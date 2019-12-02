package eventcalling

import java.io._

import aligner.{AlignmentManager, NeedlemanWunsch}
import com.typesafe.scalalogging.LazyLogging
import pacbio.PacBioInverter
import picocli.CommandLine.{Command, Option}
import utils.CutSites
import stats._
import utils._
import reads.{ReadPair, ReadPairParser, RefReadPair, UnmergedReadParser}

import scala.collection.mutable._
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
case class DeepConfig(
                      primersToCheck: String = "BOTH",
                      samplename: String = "TEST")


@Command(name = "DeepSeq", description = Array("Call CRISPR outcome events from aligned sequences"))
class DeepSeq extends Runnable with LazyLogging {
  @Option(names = Array("-inputFileUnmerged", "--inputFileUnmerged"), required = true, paramLabel = "FILE", description = Array("the unmerged input read file"))
  private var inputFileUnmerged = new File("UNKNOWN")

  @Option(names = Array("-inputMerged", "--inputMerged"), required = true, paramLabel = "FILE", description = Array("the merged input read file"))
  private var inputMerged: File = new File("UNKNOWN")

  @Option(names = Array("-outputStats", "--outputStats"), required = true, paramLabel = "FILE", description = Array("the output stats file with our per-read or UMI details"))
  private var outputStats: File = new File("UNKNOWN")

  @Option(names = Array("-primersEachEnd", "--primersEachEnd"), required = true, paramLabel = "FILE", description = Array("the expected primers on each end of the read"))
  private var primersEachEnd: File = new File("UNKNOWN")

  @Option(names = Array("-cutSites", "--cutSites"), required = true, paramLabel = "FILE", description = Array("the cutsites file"))
  private var cutSites: File = new File("UNKNOWN")

  @Option(names = Array("-primerMismatches", "--primerMismatches"), required = false, paramLabel = "INT", description = Array("how many mismatches are allowed in a primer"))
  private var primerMismatches: Int = 0

  @Option(names = Array("-primersToCheck", "--primersToCheck"), required = false, paramLabel = "STRING", description = Array("which ends of the read should we check for the matching primers"))
  private var primersToCheck: String = "BOTH"

  @Option(names = Array("-sample", "--sample"), required = false, paramLabel = "STRING", description = Array("our sample name"))
  private var samplename: String = "TEST"

  @Option(names = Array("-cutsiteWindow", "--cutsiteWindow"), required = false, paramLabel = "INT", description = Array("how far from the cutsite will an indel be included"))
  private var cutsiteWindow: Int = 3

  @Option(names = Array("-umiStart", "--umiStart"), required = false, paramLabel = "INT", description = Array("Where does the UMI start in the read, starting at 0. Negative values for the reverse read, starting at -1 "))
  private var umiStart: Int = 7

  @Option(names = Array("-requiredMatchingProp", "--requiredMatchingProp"), required = false, paramLabel = "INT", description = Array("what proportion of the match/mismatch bases must be matches?"))
  private var requiredMatchingProportion: Double = 0.85

  @Option(names = Array("-requiredRemainingBases", "--requiredRemainingBases"), required = false, paramLabel = "DOUBLE", description = Array("how many bases do we need to call in a read to record it"))
  private var requiredRemainingBases: Int = 25

  @Option(names = Array("-callScars", "--callScars"), required = false, paramLabel = "FLAG", description = Array("should we call mismatched bases as a CRISPR scar"))
  private var callScars: Boolean = false



  val NOTAREALFILENAME = "/0192348102jr10234712930h8j19p0hjf129-348h512935"
  // please don't make a file with this name
  val NOTAREALFILE = new File(NOTAREALFILENAME)

  override def run()= {
    alignedReads()
  }

  /**
   * Process the merged and paired reads into the output stats file
    *
    * @param config our config object
   *
   */
  def alignedReads(): Unit = {
    val cutsSiteObj = CutSites.fromFile(cutSites, cutsiteWindow)

    val outputStatsFile = new StatsOutput(outputStats,cutsSiteObj.size)

    val primers = Source.fromFile(primersEachEnd.getAbsolutePath).getLines().map { line => line }.toList
    if (primers.length != 2)
      throw new IllegalStateException("You should only provide a primer file with two primers")

    // store our reference and reads
    var refName = ""
    var readName = ""
    var refString = ArrayBuilder.make[String]
    var readString = ArrayBuilder.make[String]
    var inRead = false

    val mergedReadIterator = new ReadPairParser(inputMerged)

    println("traversing merged reads...")
    mergedReadIterator.foreach { pair => {
      println("Processing merged read..")
      printMergedRead(cutsSiteObj, outputStatsFile, pair, primers)
    }
    }

    // now process unpaired reads
    if (!inputFileUnmerged.equals(NOTAREALFILE)) {
      val firstReadIterator = new UnmergedReadParser(inputFileUnmerged)

      println("traversing unmerged reads...")
      firstReadIterator.foreach { twoReads => {
        println("Read pair")
        printPairedRead(cutsSiteObj,
          outputStatsFile,
          twoReads,
          primers)
      }
      }
    }
    outputStatsFile.close()
  }

  /**
   * cycle through the merged reads, creating alignments, and dumping information out to the stats file
    *
    * @param cutsSiteObj the cut sites
   * @param outputStatsFile the stats file we're dumping output to
   * @param mergedRead the merged read - aligned read and the matching reference string
   * @param primers the primers to look for on each end of the fragment
   */
  def printMergedRead(cutsSiteObj: CutSites,
                      outputStatsFile: StatsOutput,
                      mergedRead: RefReadPair,
                      primers: List[String]): Unit = {

    val (containsFwdPrimer, containsRevPrimer) = primersToCheck match {
      case "BOTH" => Utils.containsBothPrimerByAlignment(mergedRead.read.bases, primers(0), primers(1),primerMismatches)
      case "FORWARD" => (Utils.containsFWDPrimerByAlignment(mergedRead.read.bases,primers(0),primerMismatches),true)
      case "REVERSE" => (true,Utils.readEndsWithPrimerExistingDirection(mergedRead.read.bases,primers(1),primerMismatches))
      case "NONE" => (true,true)
      case _ => throw new IllegalArgumentException("Unable to parse primer configuration state: " + primerMismatches)
    }


    val baseLen = mergedRead.read.bases.map { case (ch) => if (ch == '-') 0 else 1 }.sum

    val callEvents = AlignmentManager.cutSiteEvent(mergedRead, cutsSiteObj, callScars)

    val pass = (containsFwdPrimer && containsRevPrimer && callEvents.matchingRate >= requiredMatchingProportion &&
      callEvents.matchingBaseCount >= (requiredRemainingBases * 2) && !(callEvents.alignments.mkString("") contains "WT_")) && !callEvents.collision

    outputStatsFile.outputStatEntry(new StatsContainer(mergedRead.read.name, pass, callEvents.collision, containsFwdPrimer, containsRevPrimer,
      false, true, baseLen, -1, 1, 1, callEvents.matchingRate, -1.0, callEvents.matchingBaseCount, -1,
      callEvents.alignments, callEvents.basesOverTargets, None, None, Some(mergedRead.read.bases), None, None, Some(mergedRead.reference.bases)))
  }

  def containsAlignedPrimer(mergedRead: RefReadPair, primer: String, checkPrimers: String, mismatches: Int, revComp: Boolean): Boolean = {
    val alignment = if (!revComp) new NeedlemanWunsch(primer) else new NeedlemanWunsch(Utils.reverseComplement(primer))

    val readStr = mergedRead.read.bases.filter(bs => bs != '-').mkString("")
    if ((checkPrimers == "BOTH" || checkPrimers == "FORWARD") && readStr.size >= primer.length) {
        val alignedForward = alignment.align(readStr.slice(0, primer.size))
        Utils.editDistance(alignedForward.referenceAlignment, alignedForward.queryAlignment) <= mismatches
    } else if (readStr.size < primer.length)
      false
    else
      true
  }

  /**
   *
   * @param cutsSiteObj cut sites in our target design
   * @param outputStatsFile the stats output object
   * @param readPairs the read pairs
   * @param primers the primers we expect on each side of the read
   */
  def printPairedRead(cutsSiteObj: CutSites,
                      outputStatsFile: StatsOutput,
                      readPairs: ReadPair,
                      primers: List[String]): Unit = {

    println("Processing " + readPairs.pair1.aligned)

    val (containsFwdPrimer, containsRevPrimer) = primersToCheck match {
      case "BOTH" => Utils.containsBothPrimerByAlignment(readPairs.pair1.read.bases,readPairs.pair2.read.bases, primers(0), primers(1),primerMismatches)
      case "FORWARD" => (Utils.containsFWDPrimerByAlignment(readPairs.pair1.read.bases,primers(0),primerMismatches),true)
      case "REVERSE" => (true,Utils.containsREVCompPrimerByAlignment(readPairs.pair2.read.bases,primers(1),primerMismatches))
      case "NONE" => (true,true)
      case _ => throw new IllegalArgumentException("Unable to parse primer configuration state: " + primerMismatches)
    }

    val base1Len = readPairs.pair1.read.bases.map { case (ch) => if (ch == '-') 0 else 1 }.sum
    val base2Len = readPairs.pair2.read.bases.map { case (ch) => if (ch == '-') 0 else 1 }.sum

    val callEvents = AlignmentManager.cutSiteEventsPair(readPairs.pair1, readPairs.pair2, cutsSiteObj, false, callScars)

    val pass = containsFwdPrimer && containsRevPrimer &&
      callEvents.matchingRate1 >= requiredMatchingProportion && callEvents.matchingRate2 >= requiredMatchingProportion &&
      callEvents.matchingBaseCount1 >= requiredRemainingBases && callEvents.matchingBaseCount2 >= requiredRemainingBases &&
      !(callEvents.alignments.mkString("") contains "WT_") &&
      !callEvents.collision

    outputStatsFile.outputStatEntry(
      new StatsContainer(readPairs.pair1.read.name,
        pass,
        callEvents.collision,
        containsFwdPrimer,
        containsRevPrimer,
        false,
        false,
        base1Len,
        base2Len,
        1,
        1,
        callEvents.matchingRate1,
        callEvents.matchingRate2,
        callEvents.matchingBaseCount1,
        callEvents.matchingBaseCount2,
        callEvents.alignments,
        callEvents.basesOverTargets,
        Some(readPairs.pair1.read.bases),
        Some(readPairs.pair2.read.bases),
        None,
        Some(readPairs.pair1.reference.bases),
        Some(readPairs.pair2.reference.bases),
        None))

  }

  def containsPrimer(primer: String, readStrFWD: String): Boolean = {
    val containsFwdPrimer = if ((primersToCheck == "BOTH" || primersToCheck == "FORWARD") && readStrFWD.size >= primer.length)
      Utils.editDistance(readStrFWD.slice(0, primer.length), primer) <= primerMismatches
    else if (readStrFWD.size < primer.length)
      false
    else true
    containsFwdPrimer
  }
}
