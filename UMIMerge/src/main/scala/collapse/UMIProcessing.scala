package collapse

import java.io._

import aligner.{BasicAligner, NeedlemanWunsch}
import utils.Utils
import reads._

import scala.annotation.switch
import scala.collection.{Iterator, mutable}
import scala.io._

/**
 * created by aaronmck on 2/13/14
 *
 * Copyright (c) 2014, aaronmck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 2.  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 *
 */
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



object UMIProcessing extends App {
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
        umiAnalysis(config)
    }
  } getOrElse {
    println("Unable to parse the command line arguments you passed in, please check that your parameters are correct")
  }



  /**
   * given UMIed reads, process per UMI, merging reads and calling events
    *
    * @param config our config object
   */
  def umiAnalysis(config: Config): Unit = {


    // our output files
    val outputFastq1File = new PrintWriter(config.outputFastq1.get)
    val outputFastq2File: Option[PrintWriter] = if (config.outputFastq2.isDefined) Some(new PrintWriter(config.outputFastq2.get)) else None

    // setup clustered input of the fastq files
    // ------------------------------------------------------------------------------------------
    val forwardReads = Source.fromInputStream(Utils.gis(config.inputFileReads1.get.getAbsolutePath)).getLines().grouped(4)
    val reverseReads: Option[ scala.collection.Iterator[Seq[String] ]] = if (config.inputFileReads2.isDefined) Some(Source.fromInputStream(Utils.gis(config.inputFileReads2.get.getAbsolutePath)).getLines().grouped(4)) else None
    val hasReverseReads = reverseReads.isDefined // for conveinence

    val primers = Source.fromFile(config.primersEachEnd.get.getAbsolutePath).getLines().map { line => line }.toList
    if (primers.length != 2)
      throw new IllegalStateException("You should only provide a primer file with two primers")

    // our containers for forward and reverse reads
    var umiReads = new mutable.HashMap[String, RankedReadContainer]()

    var tooFewReadsUMI = 0
    var downsampledUMI = 0
    var justRightUMI = 0

    // --------------------------------------------------------------------------------
    // process the reads into bins of UMIs, keep fwd/rev reads together
    // --------------------------------------------------------------------------------
    print("Reading in sequences and parsing out UMIs (one dot per 100K reads, carets at 1M): ")

    var readsProcessed = 0
    forwardReads foreach { fGroup => {

      val rGroup: Option[Seq[String]] = if (hasReverseReads) Some(reverseReads.get.next()) else None

      // for the forward read the UMI start position is used literally,
      // for the reverse read (when start is negative) we go from the end of the read backwards that much.
      // for instance to allow UMIs to start at the zero'th base on the reverse, they would have provided -1 as the input
      var umi: Option[String] = None

      if (config.umiStartPos >= 0) {

        umi = Some(fGroup(1).slice(config.umiStartPos, config.umiStartPos + config.umiLength))

        val readNoUMI = fGroup(1).slice(0, config.umiStartPos) + fGroup(1).slice(config.umiStartPos + config.umiLength, fGroup(1).length)
        val qualNoUMI = fGroup(3).slice(0, config.umiStartPos) + fGroup(3).slice(config.umiStartPos + config.umiLength, fGroup(3).length)

        val (containsForward, containsReverse) = config.primersToCheck match {
          case "BOTH" if hasReverseReads => Utils.containsFWDandREVCompByAlignment(readNoUMI,(rGroup.get)(1), primers(0), primers(1),config.primerMismatches)
          case "BOTH" if !( hasReverseReads )=> throw new IllegalStateException("Unable to check for both primers in single-ended reads, please set the primer check parameter to FORWARD")
          case "FORWARD" => (Utils.containsFWDPrimerByAlignment(readNoUMI,primers(0),config.primerMismatches),true)
          case "REVERSE" if hasReverseReads => (true,Utils.containsREVCompPrimerByAlignment((rGroup.get)(1),primers(1),config.primerMismatches))
          case "REVERSE" if !( hasReverseReads ) => (true,Utils.containsREVCompPrimerByAlignment((rGroup.get)(1),primers(1),config.primerMismatches))
          case "NONE" => (true,true)
          case _ => throw new IllegalArgumentException("Unable to parse primer configuration state: " + config.primerMismatches)
        }

        if (!(umiReads contains umi.get))
          umiReads(umi.get) = new RankedReadContainer(umi.get, config.downsampleSize, hasReverseReads)

        val fwd = SequencingRead(fGroup(0), readNoUMI, qualNoUMI, ForwardReadOrientation, umi.get)
        val rev: Option[SequencingRead] = if (hasReverseReads) Some(SequencingRead((rGroup.get)(0), (rGroup.get)(1), (rGroup.get)(3), ReverseReadOrientation, umi.get)) else None

        if (hasReverseReads)
          umiReads(umi.get).addBundle(SortedReadPair(fwd, rev.get, containsForward, containsReverse))
        else
          umiReads(umi.get).addBundle(SortedRead(fwd, containsForward))
      }


      else {
        require(rGroup.isDefined, "We can't parse UMIs off of the reverse read if the reverse read isnt' defined")

        val umiStartPos = math.abs(config.umiStartPos).toInt - 1
        umi = Some((rGroup.get)(1).slice(umiStartPos, umiStartPos + config.umiLength))

        val readTwoNoUMI = (rGroup.get)(1).slice(0, umiStartPos) + (rGroup.get)(1).slice(umiStartPos + config.umiLength, (rGroup.get)(1).length)
        val qualNoUMI = (rGroup.get)(3).slice(0, umiStartPos) + (rGroup.get)(3).slice(umiStartPos + config.umiLength, (rGroup.get)(3).length)

        val (containsForward, containsReverse) = config.primersToCheck match {
          case "BOTH" => Utils.containsFWDandREVCompByAlignment(fGroup(1),readTwoNoUMI, primers(0), primers(1),config.primerMismatches)
          case "FORWARD" => (Utils.containsFWDPrimerByAlignment(fGroup(1),primers(0),config.primerMismatches),true)
          case "REVERSE" => (true,Utils.containsREVCompPrimerByAlignment(readTwoNoUMI,primers(1),config.primerMismatches))
          case "NONE" => (true,true)
          case _ => throw new IllegalArgumentException("Unable to parse primer configuration state: " + config.primerMismatches)
        }

        if (!(umiReads contains umi.get))
          umiReads(umi.get) = new RankedReadContainer(umi.get,config.downsampleSize, hasReverseReads)

        val fwd = SequencingRead(fGroup(0), fGroup(1), fGroup(3), ForwardReadOrientation, umi.get)
        val rev = SequencingRead((rGroup.get)(0), readTwoNoUMI, qualNoUMI, ReverseReadOrientation, umi.get)

        umiReads(umi.get).addBundle(SortedReadPair(fwd, rev, containsForward, containsReverse))
      }

      readsProcessed += 1
      if (readsProcessed % 100000 == 0)
        print(".")
      if (readsProcessed % 1000000 == 0)
        print("^")
    }
    }


    var passingUMI = 0
    var totalWithUMI = 0


    // --------------------------------------------------------------------------------
    // take the collection of UMIs and cluster them down to a core set of UMIs, and
    // output an error rate based on that clustering
    // --------------------------------------------------------------------------------
    umiReads = UmiClustering.mergeAndConvertUMIS(config.umiLength, umiReads, hasReverseReads, config.downsampleSize)


    // --------------------------------------------------------------------------------
    // for each UMI -- process the collection of reads
    // --------------------------------------------------------------------------------
    var index = 1
    val outputUMIData : Option[PrintWriter] = if (config.outputUMIStats.get.getAbsolutePath != NOTAREALFILE.getAbsolutePath)
      Some(new PrintWriter(config.outputUMIStats.get.getAbsolutePath)) else None

    if (outputUMIData.isDefined)
      outputUMIData.get.write("umi\toneOrTwoReads\ttotalCount\tpassCount\tmissingPrimer1\tmissingPrimer2\tsurviving1\tsurviving2\tsequence\n")

    println("\n\nTotal UMIs to process: " + umiReads.size)

    umiReads.foreach { case (umi, reads) => {
      val greaterThanMinimumReads = reads.size >= config.minimumUMIReads

      if (greaterThanMinimumReads) {
        val readBundle = reads.result()

        (readBundle._2: @switch) match {
          case 2 => {
            val res = UMIMerger.mergeTogether(umi,
              readBundle._1(0),
              readBundle._1(1),
              reads.totalPassedReads,
              reads.totalPassedReads,
              outputFastq1File,
              outputFastq2File.get,
              primers,
              config.samplename,
              config.minimumSurvivingUMIReads,
              index,
              BasicAligner)

            outputUMIData.get.write(umi + "\t2\t" + reads.totalReads + "\t" +
              reads.totalPassedReads + "\t" + reads.noPrimer1 + "\t" +
              reads.noPrimer2 + "\t" + res.read1SurvivingCount + "\t" +
              res.read2SurvivingCount + "\t" + res.read1Consensus + ";" + res.read2Consensus + "\n")

          }
          case 1 => {
            val res = UMIMerger.mergeTogetherSingleReads(umi,
              readBundle._1(0),
              reads.totalPassedReads,
              outputFastq1File,
              primers,
              config.samplename,
              config.minimumSurvivingUMIReads,
              index,
              BasicAligner)

            outputUMIData.get.write(umi + "\t1\t" + reads.totalReads + "\t" +
              reads.totalPassedReads + "\t" + reads.noPrimer1 + "\t" +
              reads.noPrimer2 + "\t" + res.readSurvivingCount + "\tNA\t" + res.readConsensus + ";NA\n")

          }
        }

      } else {
        outputUMIData.get.write(umi + "\t1\t" + reads.totalReads + "\t" +
          reads.totalPassedReads + "\t" + reads.noPrimer1 + "\t" + reads.noPrimer2 +
          "\tNA\tNA\tNOTENOUGHREADS\n")
      }


      if (index % 1000 == 0) {
        println("INFO: Processed " + index + " umis so far")
      }
      index += 1
    }
    }

    if (outputUMIData.isDefined)
      outputUMIData.get.close()
    outputFastq1File.close()
    if (outputFastq2File.isDefined) outputFastq2File.get.close()
  }
}
