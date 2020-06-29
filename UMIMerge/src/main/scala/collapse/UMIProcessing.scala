package collapse

import java.io._

import aligner.BasicAligner
import reads.{ForwardReadOrientation, RankedReadContainer, ReverseReadOrientation, SequencingRead, SortedRead, SortedReadPair}
import java.io.{File, PrintWriter}

import com.typesafe.scalalogging.LazyLogging
import picocli.CommandLine.{Command, Option}
import utils.Utils

import scala.collection.mutable
import scala.io.Source


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

@Command(name = "umi2pass", description = Array("extract and collapse successfully captured UMIs from a set of fasta files"))
class UMIProcessing extends Runnable with LazyLogging {
  @Option(names = Array("-inputReads1", "--read1"), required = true, paramLabel = "FILE", description = Array("the first input read file"))
  private var read1: File = new File("UNKNOWN")

  @Option(names = Array("-inputReads2", "--read2"), required = false, paramLabel = "FILE", description = Array("the second input read file"))
  private var read2: File = new File("UNKNOWN")

  @Option(names = Array("-outputReads1", "--outread1"), required = true, paramLabel = "FILE", description = Array("the first output read file"))
  private var outRead1: File = new File("UNKNOWN")

  @Option(names = Array("-outputReads2", "--outread2"), required = false, paramLabel = "FILE", description = Array("the second output read file"))
  private var outRead2: File = new File("UNKNOWN")

  @Option(names = Array("-umiStatsFile", "--statsFile"), required = true, paramLabel = "FILE", description = Array("an output file containing stats about each captured UMI"))
  private var statsFile: File = new File("UNKNOWN")

  @Option(names = Array("-primers", "--primers"), required = true, paramLabel = "FILE", description = Array("the primers for the forward and reverse reads"))
  private var primersFile: File = new File("UNKNOWN")

  @Option(names = Array("-primersToCheck", "--primersToCheck"), required = false, paramLabel = "FILE", description = Array("what primers should we check"))
  private var primersToCheck: String = "BOTH"

  @Option(names = Array("-primerMismatches", "--primerMismatches"), required = false, paramLabel = "INT", description = Array("Number of primer mismatches allowed"))
  private var primerMismatches: Int = 7

  @Option(names = Array("-umiThrehold", "--thresh"), required = false, paramLabel = "INT", description = Array("how many reads are required to save a UMI"))
  private var umiThrehold: Int = 7

  @Option(names = Array("-umiStart", "--umiStart"), required = false, paramLabel = "INT", description = Array("Where does the UMI start in the read, starting at 0. Negative values for the reverse read, starting at -1 "))
  private var umiStart: Int = 7

  @Option(names = Array("-umiLength", "--umiLength"), required = false, paramLabel = "INT", description = Array("how long is the UMI"))
  private var umiLength: Int = 7

  @Option(names = Array("-minimumUMIReads", "--minimumUMIReads"), required = false, paramLabel = "INT", description = Array("the minimum number of input reads to output a UMI"))
  private var minimumUMIReads: Int = 4

  @Option(names = Array("-samplename", "--samplename"), required = false, paramLabel = "INT", description = Array("sample name"))
  private var samplename: String = ""

  @Option(names = Array("-minimumSurvivingUMIReads", "--minimumSurvivingUMIReads"), required = false, paramLabel = "INT", description = Array("the minimum number of final reads to output a UMI"))
  private var minimumSurvivingUMIReads: Int = 3

  @Option(names = Array("-minBaseCallRate", "--minBaseCallRate"), required = false, paramLabel = "DOUBLE", description = Array("how many bases do we need to call in a read to record it"))
  private var minBaseCallRate: Double = 0.90

  @Option(names = Array("-baseCallThresh", "--baseCallThresh"), required = false, paramLabel = "DOUBLE", description = Array("what proportion of reads need to have base X to call the consensus"))
  private var baseCallThresh: Double = 0.90

  @Option(names = Array("-downsampleSize", "--downsampleSize"), required = false, paramLabel = "INT", description = Array("How many reads are considered per UMI"))
  private var downsampleSize: Int = 40

  override def run() = {
    if (read2.exists()) {
      umiAnalysis()
    } else {
      umiAnalysisSingleEnd()
    }

  }
    /**
     * given UMIed reads, process per UMI, merging reads and calling events
     *
     */
    def umiAnalysis(): Unit = {
      // our output files
      val outputFastq1File = new PrintWriter(outRead1)
      val outputFastq2File = new PrintWriter(outRead2)

      // setup clustered input of the fastq files
      // ------------------------------------------------------------------------------------------
      val forwardReads = Source.fromInputStream(Utils.gis(read1.getAbsolutePath)).getLines().grouped(4)
      val reverseReads = Source.fromInputStream(Utils.gis(read2.getAbsolutePath)).getLines().grouped(4)

      val primers = Source.fromFile(primersFile.getAbsolutePath).getLines().map { line => line }.toList
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
        val rGroup = reverseReads.next()

        // for the forward read the UMI start position is used literally,
        // for the reverse read (when start is negitive) we go from the end of the read backwards that much.
        // for instance to allow UMIs to start at the zero'th base on the reverse, they would have provided -1 as the input
        var umi: scala.Option[String] = None

        if (umiStart >= 0) {
          umi = Some(fGroup(1).slice(umiStart, umiStart + umiLength))

          val readNoUMI = fGroup(1).slice(0, umiStart) + fGroup(1).slice(umiStart + umiLength, fGroup(1).length)
          val qualNoUMI = fGroup(3).slice(0, umiStart) + fGroup(3).slice(umiStart + umiLength, fGroup(3).length)

          val (containsForward, containsReverse) = primersToCheck match {
            case "BOTH" => Utils.containsBothPrimerByAlignment(readNoUMI, rGroup(1), primers(0), primers(1), primerMismatches)
            case "FORWARD" => (Utils.containsFWDPrimerByAlignment(readNoUMI, primers(0), primerMismatches), true)
            case "REVERSE" => (true, Utils.containsREVCompPrimerByAlignment(rGroup(1), primers(1), primerMismatches))
            case _ => throw new IllegalArgumentException("Unable to parse primer configuration state: " + primerMismatches)
          }

          if (!(umiReads contains umi.get))
            umiReads(umi.get) = new RankedReadContainer(umi.get, downsampleSize, true)

          val fwd = SequencingRead(fGroup(0), readNoUMI, qualNoUMI, ForwardReadOrientation, umi.get)
          val rev = SequencingRead(rGroup(0), rGroup(1), rGroup(3), ReverseReadOrientation, umi.get)

          umiReads(umi.get).addBundle(SortedReadPair(fwd, rev, containsForward, containsReverse))
        }
        else {
          val umiStartPos = math.abs(umiStart).toInt - 1
          umi = Some(rGroup(1).slice(umiStartPos, umiStartPos + umiLength))

          val readTwoNoUMI = rGroup(1).slice(0, umiStartPos) + rGroup(1).slice(umiStartPos + umiLength, rGroup(1).length)
          val qualNoUMI = rGroup(3).slice(0, umiStartPos) + rGroup(3).slice(umiStartPos + umiLength, rGroup(3).length)

          val (containsForward, containsReverse) = primersToCheck match {
            case "BOTH" => Utils.containsBothPrimerByAlignment(fGroup(1), readTwoNoUMI, primers(0), primers(1), primerMismatches)
            case "FORWARD" => (Utils.containsFWDPrimerByAlignment(fGroup(1), primers(0), primerMismatches), true)
            case "REVERSE" => (true, Utils.containsREVCompPrimerByAlignment(readTwoNoUMI, primers(1), primerMismatches))
            case _ => throw new IllegalArgumentException("Unable to parse primer configuration state: " + primerMismatches)
          }

          if (!(umiReads contains umi.get))
            umiReads(umi.get) = new RankedReadContainer(umi.get, downsampleSize, true)

          val fwd = SequencingRead(fGroup(0), fGroup(1), fGroup(3), ForwardReadOrientation, umi.get)
          val rev = SequencingRead(rGroup(0), readTwoNoUMI, qualNoUMI, ReverseReadOrientation, umi.get)

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
      // for each UMI -- process the collection of reads
      // --------------------------------------------------------------------------------
      var index = 1
      val outputUMIData: scala.Option[PrintWriter] = Some(new PrintWriter(statsFile.getAbsolutePath))

      outputUMIData.get.write("umi\ttotalCount\tpassCount\tmissingPrimer1\tmissingPrimer2\tsequence\n")


      println("\n\nTotal UMIs to process: " + umiReads.size)
      umiReads.foreach { case (umi, reads) => {
        val greaterThanMinimumReads = reads.size >= minimumUMIReads

        if (greaterThanMinimumReads) {
          val (readList,counts) = reads.result()

          val res = UMIMerger.mergeTogether(umi,
            readList(0),
            readList(1),
            reads.totalPassedReads,
            reads.totalPassedReads,
            outputFastq1File,
            outputFastq2File,
            primers,
            samplename,
            minimumSurvivingUMIReads,
            index,
            BasicAligner)

          outputUMIData.get.write(umi + "\t" + reads.totalReads + "\t" +
            reads.totalPassedReads + "\t" + reads.noPrimer1 + "\t" +
            reads.noPrimer2 + "\t" + res.read1SurvivingCount + "\t" + res.read2SurvivingCount + "\t" + res.read1Consensus + ";" + res.read2Consensus + "\n")

        } else {
          outputUMIData.get.write(umi + "\t" + reads.totalReads + "\t" +
            reads.totalPassedReads + "\t" + reads.noPrimer1 + "\t" +
            "NA\tNA\t" + reads.noPrimer2 + "\tNOTENOUGHREADS\n")
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
      outputFastq2File.close()
    }


    /**
     * given UMIed reads, process per UMI, merging reads and calling events
     *
     */
    def umiAnalysisSingleEnd(): Unit = {
      // our output files
      val outputFastq1File = new PrintWriter(outRead1)

      // setup clustered input of the fastq files
      // ------------------------------------------------------------------------------------------
      val forwardReads = Source.fromInputStream(Utils.gis(outRead1.getAbsolutePath)).getLines().grouped(4)

      val primers = Source.fromFile(primersFile.getAbsolutePath).getLines().map { line => line }.toList
      if (primers.length != 2)
        throw new IllegalStateException("You should only provide a primer file with two primers")

      // our containers for forward and reverse reads
      var umiReads = new mutable.HashMap[String, RankedReadContainer]()

      var tooFewReadsUMI = 0
      var downsampledUMI = 0
      var justRightUMI = 0

      // --------------------------------------------------------------------------------
      // process the reads into bins of UMIs
      // --------------------------------------------------------------------------------
      print("Reading in sequences and parsing out UMIs (one dot per 100K reads, carets at 1M): ")
      var readsProcessed = 0
      forwardReads foreach { fGroup => {

        // for the forward read the UMI start position is used literally,
        // for the reverse read (when start is negitive) we go from the end of the read backwards that much. To
        // allow UMIs to start at the zero'th base on the reverse, we say the first base is one, second is 2, etc.
        var umi: scala.Option[String] = None

        if (umiStart >= 0) {
          umi = Some(fGroup(1).slice(umiStart, umiStart + umiLength))

          val readNoUMI = fGroup(1).slice(0, umiStart) + fGroup(1).slice(umiStart + umiLength, fGroup(1).length)
          val qualNoUMI = fGroup(3).slice(0, umiStart) + fGroup(3).slice(umiStart + umiLength, fGroup(3).length)

          val containsForward = if (primersToCheck == "BOTH" || primersToCheck == "FORWARD") {
            //println("Compare " + readNoUMI.slice(0, primers(0).length) + " to " + primers(0))
            Utils.editDistance(readNoUMI.slice(0, primers(0).length), primers(0)) <= primerMismatches
          }
          else true

          if (!(umiReads contains umi.get))
            umiReads(umi.get) = new RankedReadContainer(umi.get, downsampleSize, false)

          val fwd = SequencingRead(fGroup(0), readNoUMI, qualNoUMI, ForwardReadOrientation, umi.get)

          umiReads(umi.get).addBundle(SortedRead(fwd, containsForward))
        }
        else {
          throw new IllegalStateException("Unable to pull UMIs from reverse read in single-end mode")
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
      // for each UMI -- process the collection of reads
      // --------------------------------------------------------------------------------
      var index = 1
      val outputUMIData: scala.Option[PrintWriter] = Some(new PrintWriter(statsFile.getAbsolutePath))

      if (outputUMIData.isDefined)
        outputUMIData.get.write("umi\ttotalCount\tpassCount\tmissingPrimer1\tmissingPrimer2\n")


      println("\n\nTotal UMIs to process: " + umiReads.size)
      umiReads.foreach { case (umi, reads) => {

        val greaterThanMinimumReads = reads.size >= minimumUMIReads

        if (greaterThanMinimumReads) {
          val (fwdReads, counts) = reads.result()

          val res = UMIMerger.mergeTogetherSingleReads(umi,
            fwdReads(0),
            counts,
            outputFastq1File,
            primers,
            samplename,
            minimumSurvivingUMIReads,
            index,
            BasicAligner)

          outputUMIData.get.write(umi + "\t" + reads.totalReads + "\t" +
            reads.totalPassedReads + "\t" + reads.noPrimer1 + "\t" +
            reads.noPrimer2 + res.readSurvivingCount + "\tNA\t" + "\t" + res + "\n")

        } else {
          outputUMIData.get.write(umi + "\t" + reads.totalReads + "\t" +
            reads.totalPassedReads + "\t" + reads.noPrimer1 + "\t" +
            "NA\tNA\t" + reads.noPrimer2 + "\tNOTENOUGHREADS\n")
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
    }
}