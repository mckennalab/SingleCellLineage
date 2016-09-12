package endogenous

import java.io.{File, PrintWriter}

import aligner.BasicAligner
import collapse.{UMIMerger}
import collapse.UMIProcessing._
import reads._
import utils.Utils

import scala.collection.mutable
import scala.io.Source

/**
  * Given a library of reads tagged with UMIs, count the occurances of each,
  */
class UMICounter extends App {

  val NOTAREALFILENAME = "/0192348102jr10234712930h8j19p0hjf129-348h512935"
  // please don't make a file with this name
  val NOTAREALFILE = new File(NOTAREALFILENAME)

  // parse the command line arguments
  val parser = new scopt.OptionParser[Config]("UMIMerge") {
    head("UMIMerge", "1.0")

    // *********************************** Inputs *******************************************************
    opt[File]("inputFileReads1") required() valueName ("<file>") action { (x, c) => c.copy(inputFileReads1 = Some(x)) } text ("first read file ")
    opt[File]("inputFileReads2") valueName ("<file>") action { (x, c) => c.copy(inputFileReads2 = Some(x)) } text ("second reads file")
    opt[File]("outputFastq1") required() valueName ("<file>") action { (x, c) => c.copy(outputFastq1 = Some(x)) } text ("the output stats file")
    opt[File]("outputFastq2") valueName ("<file>") action { (x, c) => c.copy(outputFastq2 = Some(x)) } text ("the output stats file")
    opt[File]("umiCounts") required() valueName ("<file>") action { (x, c) => c.copy(outputUMIStats = Some(x)) } text ("the counts of each UMI in the data")
    opt[File]("primersEachEnd") required() valueName ("<file>") action { (x, c) => c.copy(primersEachEnd = Some(x)) } text ("the file containing the amplicon primers requred to be present, one per line, two lines total")
    opt[Int]("minimumUMIReads") action { (x, c) => c.copy(minimumUMIReads = x) } text ("the minimum number of reads that each UMI should have to be considered signal and not noise")
    opt[Int]("minimumSurvivingUMIReads") action { (x, c) => c.copy(minimumSurvivingUMIReads = x) } text ("the minimum number of reads that each UMI should have post filtering")
    opt[Int]("downsampleSize") action { (x, c) => c.copy(downsampleSize = x) } text ("the maximum number of top-reads we'll store for any UMI")
    opt[Int]("primerMismatches") action { (x, c) => c.copy(primerMismatches = x) } text ("how many mismatches are allowed in primer regions")
    opt[String]("primersToCheck") action { (x, c) => c.copy(primersToCheck = x) } text ("should we check both primers, or just one? Or none?")
    opt[Boolean]("processSingleReads") action { (x, c) => c.copy(processSingleReads = x) } text ("process single reads instead of paired end")


    opt[Int]("umiStart") required() action { (x, c) => c.copy(umiStartPos = x) } text ("the start position, zero based, of our UMIs")
    opt[Int]("umiLength") required() action { (x, c) => c.copy(umiLength = x) } text ("the length of our UMIs")
    opt[String]("samplename") required() action { (x, c) => c.copy(samplename = x) } text ("the sample name of this run")


    // some general command-line setup stuff
    note("processes reads with UMIs into merged reads\n")
    help("help") text ("prints the usage information you see here")
  }

  // *********************************** Run *******************************************************
  // run the actual read processing -- our argument parser found all of the parameters it needed
  parser.parse(args, Config()) map {
    config: Config => {
      processUMICounts(config)
    }
  } getOrElse {
    println("Unable to parse the command line arguments you passed in, please check that your parameters are correct")
  }


  /**
    * given UMIed reads, process per UMI, merging reads and calling events
    *
    * @param config our config object
    */
  def processUMICounts(config: Config): Unit = {

    // setup clustered input of the fastq files
    // ------------------------------------------------------------------------------------------
    val forwardReads = Source.fromInputStream(Utils.gis(config.inputFileReads1.get.getAbsolutePath)).getLines().grouped(4)

    val primers = Source.fromFile(config.primersEachEnd.get.getAbsolutePath).getLines().map { line => line }.toList
    if (primers.length != 2)
      throw new IllegalStateException("You should only provide a primer file with two primers")

    // our containers for forward and reverse reads
    var umiReads = new mutable.HashMap[String, SingleRankedReadContainer]()

    // --------------------------------------------------------------------------------
    // process the reads into bins of UMIs, keep fwd/rev reads together
    // --------------------------------------------------------------------------------
    print("Reading in sequences and parsing out UMIs (one dot per 100K reads, carets at 1M): ")

    var readsProcessed = 0
    forwardReads foreach { fGroup => {

      // for the forward read the UMI start position is used literally,
      // for the reverse read (when start is negitive) we go from the end of the read backwards that much. To
      // allow UMIs to start at the zero'th base on the reverse, we say the first base is one
      var umi: Option[String] = None

      umi = Some(fGroup(1).slice(config.umiStartPos, config.umiStartPos + config.umiLength))

      val readNoUMI = fGroup(1).slice(0, config.umiStartPos) + fGroup(1).slice(config.umiStartPos + config.umiLength, fGroup(1).length)
      val qualNoUMI = fGroup(3).slice(0, config.umiStartPos) + fGroup(3).slice(config.umiStartPos + config.umiLength, fGroup(3).length)

      val containsForward = Utils.editDistance(readNoUMI.slice(0, primers(0).length), primers(0)) <= config.primerMismatches

      if (!(umiReads contains umi.get))
        umiReads(umi.get) = new SingleRankedReadContainer(umi.get, config.downsampleSize)

      val fwd = SequencingRead(fGroup(0), readNoUMI, qualNoUMI, ForwardReadOrientation, umi.get)
      umiReads(umi.get).addRead(fwd, containsForward)

      readsProcessed += 1
      if (readsProcessed % 100000 == 0)
        print(".")
      if (readsProcessed % 1000000 == 0)
        print("^")
    }}

    // --------------------------------------------------------------------------------
    // for each UMI -- process the collection of reads
    // --------------------------------------------------------------------------------
    var passingUMI = 0
    var totalWithUMI = 0
    var index = 1

    var histogram = Array[Int](5000)

    val outputUMIData: Option[PrintWriter] = if (config.outputUMIStats.get.getAbsolutePath != NOTAREALFILE.getAbsolutePath)
      Some(new PrintWriter(config.outputUMIStats.get.getAbsolutePath))
    else None

    if (outputUMIData.isDefined)
      outputUMIData.get.write("umi\ttotal\n")

    println("\n\nTotal UMIs to process: " + umiReads.size)

    umiReads.foreach { case (umi, reads) => {
      if (reads.size() >= 5000)
        histogram(5000) = histogram(5000) + 1
      else
        histogram(reads.size()) = histogram(reads.size()) + 1

      if (index % 1000 == 0) {
        println("INFO: Processed " + index + " umis so far")
      }
      index += 1
    }
    }

    if (outputUMIData.isDefined)
      outputUMIData.get.close()

  }

}

case class Config(inputFileReads1: Option[File] = None,
                  inputFileReads2: Option[File] = None,
                  outputFastq1: Option[File] = None,
                  outputFastq2: Option[File] = None,
                  outputUMIStats: Option[File] = None,
                  umiLength: Int = 10,
                  umiStartPos: Int = 0,
                  primersEachEnd: Option[File] = None,
                  samplename: String = "TEST",
                  minimumUMIReads: Int = 10,
                  minimumSurvivingUMIReads: Int = 6,
                  umiInForwardRead: Boolean = true,
                  downsampleSize: Int = 40,
                  primersToCheck: String = "BOTH",
                  primerMismatches: Int = 7,
                  processSingleReads: Boolean = false)
