package endogenous

import java.io.{File, PrintWriter}

import aligner.BasicAligner
import collapse.UMIMerger
import collapse.UMIProcessing._
import reads._
import umi.UMIProcessingFromReads
import utils.Utils

import scala.collection.mutable
import scala.io.Source

/**
  * Given a library of reads tagged with UMIs, count the occurances of each,
  */
object UMICounter extends App {

  val NOTAREALFILENAME = "/0192348102jr10234712930h8j19p0hjf129-348h512935"
  // please don't make a file with this name
  val NOTAREALFILE = new File(NOTAREALFILENAME)

  // parse the command line arguments
  val parser = new scopt.OptionParser[Config]("UMIMerge") {
    head("UMIMerge", "1.0")

    // *********************************** Inputs *******************************************************
    opt[File]("inputCaptured") required() valueName ("<file>") action { (x, c) => c.copy(inputCaptured = Some(x)) } text ("read file with our possibly edited hits")
    opt[File]("inputLibrary") required() valueName ("<file>") action { (x, c) => c.copy(inputLibrary = Some(x)) } text ("our library background hits")
    opt[File]("umiCounts") required() valueName ("<file>") action { (x, c) => c.copy(outputUMIStats = Some(x)) } text ("the counts of each UMI in the data")
    opt[Int]("minimumUMIReads") action { (x, c) => c.copy(minimumUMIReads = x) } text ("the minimum number of reads that each UMI should have to be considered signal and not noise")

    opt[Int]("umiStart") required() action { (x, c) => c.copy(umiStartPos = x) } text ("the start position, zero based, of our UMIs")
    opt[Int]("umiLength") required() action { (x, c) => c.copy(umiLength = x) } text ("the length of our UMIs")

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

    // read the input libraries
    // ------------------------------------------------------------------------------------------
    val libraryUMIs = UMIProcessingFromReads.extractSuccessfulUMICaptures(config.inputLibrary.get.getAbsolutePath,
      config.umiStartPos,
      config.umiLength,
      config.minimumUMIReads)


    // make a consensus of the library UMIs

    // for each read, check the UMI match, check the differences, and output the results
    //*******

    // merge the two sets of UMIs and see how many overlap in each
    // --------------------------------------------------------------------------------
    val outputUMIData: Option[PrintWriter] = if (config.outputUMIStats.get.getAbsolutePath != NOTAREALFILE.getAbsolutePath)
      Some(new PrintWriter(config.outputUMIStats.get.getAbsolutePath))
    else None
    /*
    val allUMIs = capturedUMIs.umis.keySet ++ libraryUMIs.umis.keySet

    outputUMIData.get.write("umi\tcaptured\tlibrary\n")

    allUMIs.foreach{case(umi) => {
      val captured = if (capturedUMIs.umis contains umi) capturedUMIs.umis(umi).totalReads else 0
      val library = if (libraryUMIs.umis contains umi) libraryUMIs.umis(umi).totalReads else 0
      outputUMIData.get.write(umi + "\t" + captured + "\t" + library + "\n")
    }}

    outputUMIData.get.close()
    */
    throw new IllegalStateException("CODE NOT DONE")
  }

}

case class Config(inputCaptured: Option[File] = None,
                  inputLibrary: Option[File] = None,
                  outputUMIStats: Option[File] = None,
                  umiLength: Int = 10,
                  umiStartPos: Int = 0,
                  minimumUMIReads: Int = 250)
