/** *********************************************
  * Requires Java 1.7. On the UW machines this means
  * running at least: "module load java/7u17", or the appropriate
  * module if there's a more recent version, to load java 1.7 into
  * your environment.  The best idea is to place this into your
  * bash/shell profile
  *
  *
  * Copyright (c) 2014, 2015, 2016, aaronmck
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
  * @author Aaron
  * @date June, 2015
  *
  */

package org.broadinstitute.gatk.queue.qscripts

import org.broadinstitute.gatk.queue.extensions.gatk._
import org.broadinstitute.gatk.queue.QScript
import org.broadinstitute.gatk.queue.extensions.picard._
import org.broadinstitute.gatk.queue.util.QScriptUtils
import org.broadinstitute.gatk.queue.function.ListWriterFunction
import org.broadinstitute.gatk.utils.commandline.Hidden
import org.broadinstitute.gatk.utils.commandline

import collection.JavaConversions._
import htsjdk.samtools.SAMFileReader
import htsjdk.samtools.SAMFileHeader.SortOrder
import scala.io.Source
import scala.collection.immutable._
import java.io.File

/**
  * Given amplicon reads from a CRISPR experiment, align and call events over those cut sites.
  *
  * PLEASE READ:
  *
  * - This pipeline assumes your reference file, say myref.fa, has the following additional files in the same dir:
  * - myref.fa.cutSites <-- contains the CRISPR target seq, position start, the cutsite position
  * - myref.fa.primers <-- two lines, containing the positive strand primers on both ends of your amplicon.
  *
 **/
class DNAQC extends QScript {
  qscript =>

  /** **************************************************************************
    * the base locations for the jar files and the scripts directory
    * ************************************************************************** */
  @Input(doc = "Where to find binary files we need", fullName = "binaryLoc", shortName = "b", required = true)
  var binaryLoc: File = new File("/app/bin/")

  @Input(doc = "Where to find script files we need", fullName = "scriptLoc", shortName = "s", required = true)
  var scriptLoc: File = new File("/app/sc_GESTALT/scripts/")

  /** **************************************************************************
    * Data Parameters
    * ************************************************************************** */
  @Input(doc = "Tab-delimited tear sheet containing sample information", fullName = "input", shortName = "i", required = true)
  var input: File = _

  @Input(doc = "where to put aggregated stats", fullName = "aggLocation", shortName = "agg", required = true)
  var aggregateLocation: File = _

  @Argument(doc = "the experiment name, used to generate the base output folder on the web server", fullName = "expName", shortName = "expName", required = true)
  var experimentalName: String = ""

  /** **************************************************************************
    * Optional Parameters -- control parameters for the script (alignment, etc)
    * ************************************************************************** */

  @Argument(doc = "do we NOT want to use Trimmomatic to clean the reads of bad sequence?", fullName = "dontTrim", shortName = "dontTrim", required = false)
  var dontTrim: Boolean = false

  @Argument(doc = "the minimum quality score we accept; less than this we trim off the ends, or trim when we see a window averaging this quality score", fullName = "trimQual", shortName = "trimQual", required = false)
  var trimQual: Int = 10

  @Argument(doc = "how many bases to average over in our sliding window cleaning using Trimmomatic", fullName = "trimWindow", shortName = "trimWindow", required = false)
  var trimWindow: Int = 5

  @Argument(doc = "are we runing with UMI reads? If so, set this value to > 0, representing the UMI length", fullName = "umiLength", shortName = "umiLength", required = false)
  var umiLength: Int = 10

  @Argument(doc = "where does the UMI start in the read", fullName = "umiStart", shortName = "umiStart", required = false)
  var umiStart: Int = 0

  @Argument(doc = "the number of UMIs required to call a successful UMI capture event, if you're using UMIs", fullName = "minimumUMIReads", shortName = "minimumUMIReads", required = false)
  var minimumUMIReads = 10

  @Argument(doc = "the number of surviving reads required to call a successful UMI capture event, if you're using UMIs", fullName = "minimumSurvivingUMIReads", shortName = "minimumSurvivingUMIReads", required = false)
  var minimumSurvivingUMIReads = 6

@Argument(doc = "maximum number of mismatches in the adapter sequences to allow", fullName = "maxAdapterMismatches", shortName = "maxAdapterMismatches", required = false)
  var maxAdaptMismatch = 3

  @Argument(doc = "what proportion of the match and mismatches bases must be a match for the alignment to be considered a PASS in the stats file (default 0.80)", fullName = "matchProportion", shortName = "matchProportion", required = false)
  var matchProportion = 0.80

  @Argument(doc = "what is the minimum number of matched bases to consider a read alignment PASSing? (default 25) ", fullName = "matchCount", shortName = "matchCount", required = false)
  var matchCount = 15

  @Input(doc = "where to put the web files", fullName = "web", shortName = "web", required = false)
  var webSite: File = "/var/www/html"

  /** **************************************************************************
    * Path parameters -- where to find tools
    * ************************************************************************** */
  @Input(doc = "The path to a functional installation of the scala tool", fullName = "scala", shortName = "scala", required = false)
  var scalaPath: File = new File("/usr/bin/scala")

  @Input(doc = "The path to the Trimmomatic adapters file", fullName = "trimadapters", shortName = "tad", required = false)
  var adaptersFile: File = new File("/app/Trimmomatic-0.36/adapters/TruSeq3-PE.fa")

  @Input(doc = "The path to the jar file for trimmomatic", fullName = "trim", shortName = "tm", required = false)
  var trimmomaticName: File = "trimmomatic.jar"

  @Input(doc = "The filename to the barcode splitter", fullName = "maulpath", shortName = "mlp", required = false)
  var maulName: File = "Maul.jar"

  @Input(doc = "The filename of the UMI merger", fullName = "maul", shortName = "maul", required = false)
  var umiName: File = "UMIMerge.jar"

  @Input(doc = "The filename of the calling har", fullName = "Caller", shortName = "Caller", required = false)
  var callerName: File = "DeepSeq.jar"

  @Input(doc = "The path to the seqprep tool", fullName = "seqprep", shortName = "sprep", required = false)
  var seqPrepPath: File = "/net/gs/vol1/home/aaronmck/tools/bin/SeqPrep"

  @Input(doc = "The path to the flash tool", fullName = "flash", shortName = "flash", required = false)
  var flashPath: File = "/usr/local/bin/flask"

  @Input(doc = "the script to analyze the edits made to a crispr target", fullName = "edits", shortName = "edits", required = false)
  var crisprPath: File = "/app/sc_GESTALT/scripts/analyze_diversity_of_edits.scala"

  @Input(doc = "the script to analyze the per site edits", fullName = "writeweb", shortName = "wwwrite", required = false)
  var writeWebFiles: File = "/app/sc_GESTALT/scripts/write_web_files.scala"

  @Input(doc = "the location of the needleall program", fullName = "needle", shortName = "needle", required = false)
  var needlePath: File = "/usr/bin/needleall"

  @Input(doc = "the location of the alignment script", fullName = "alignScript", shortName = "alignScript", required = false)
  var alignmentScripts: File = "/app/sc_GESTALT/scripts/align_merged_unmerged_reads.scala"

  @Input(doc = "the path the javascript conversion script", fullName = "JSTable", shortName = "JSTable", required = false)
  var toJSTableScript: File = "/app/sc_GESTALT/scripts/stats_to_javascript_tables2.scala"

  @Argument(doc = "zip two reads together", fullName = "ZipReads", shortName = "ZipReads", required = false)
  var zipReadsPath = "/app/sc_GESTALT/scripts/zip_two_read_files.scala"

  @Argument(doc = "move the data over to the web location", fullName = "webpub", shortName = "webpub", required = false)
  var toWebPublishScript = "/app/sc_GESTALT/scripts/push_to_web_location.scala"

  @Argument(doc = "aggregate stats files (UMIed reads) together", fullName = "stats", shortName = "stats", required = false)
  var aggregateScripts = "/app/sc_GESTALT/scripts/aggregate_stats.scala"

  @Argument(doc = "script that takes an independent UMI file and puts it on the front of read 1", fullName = "umiToFront", shortName = "umiToFront", required = false)
  var convertUMIFile = "/app/sc_GESTALT/scripts/beths_UMI_to_standard_UMI.scala"

  @Argument(doc = "analyize stats file and generate a bunch of plots", fullName = "plotsScript", shortName = "plotsScript", required = false)
  var statsFileAnalysis = "/app/sc_GESTALT/scripts/stats_file_analysis.py"

  @Argument(doc = "the first adapter sequence", fullName = "adaptOne", shortName = "adaptOne", required = false)
  var adapterOne = "GACCTCGAGACAAATGGCAG"

  @Argument(doc = "the second adapter sequence", fullName = "adaptTwo", shortName = "adaptTwo", required = false)
  var adapterTwo = "CGAAGCTTGAGCTCGAGATCTG"

  @Argument(doc = "where to start trimming in the first read", fullName = "trimStart", shortName = "trimStart", required = false)
  var trimStart: Int = 0

  @Argument(doc = "where to stop trimming in the first read", fullName = "trimStop", shortName = "trimStop", required = false)
  var trimStop: Int = 0

  @Argument(doc = "the cost of a gap open", fullName = "gapopen", shortName = "gapopen", required = false)
  var gapopen: Double = 10

  @Argument(doc = "the cost of a gap extend", fullName = "gapextend", shortName = "gapextend", required = false)
  var gapextend: Double = 0.5

  @Argument(doc = "where to find the EDA file for alignment with NW", fullName = "eda", shortName = "eda", required = false)
  var edaMatrix = "/app/sc_GESTALT/tear_sheet_examples/EDNAFULL.Ns_are_zero"

  @Argument(doc = "which aligner to use", fullName = "aligner", shortName = "aligner", required = false)
  var aligner = "needle" // this is the only aligner installed on the Docker instance

  @Argument(doc = "Do we want to check primers on both ends, one end, or neither", fullName = "primersToUse", shortName = "primersToUse", required = false)
  var primersToCheck: String = "BOTH"

  @Argument(doc = "Do we want to check primers on both ends, one end, or neither", fullName = "minRead", shortName = "minRead", required = false)
  var minLength: Int = 20

  @Argument(doc = "Use one of index runs as a UMI, and add it to read 1", fullName = "umiIndex", shortName = "umiIndex", required = false)
  var umiIndex: String = "NONE" // "index1" or "index2" are options

  @Argument(doc = "Do we want to convert UNKNOWN calls into NONE calls?", fullName = "unknownToNone", shortName = "unknownToNone", required = false)
  var convertUnknownsToNone: Boolean = false

  @Argument(doc = "do we want to do the whole web publishing thing?", fullName = "dontweb", shortName = "dontweb", required = false)
  var dontWebPublish: Boolean = false

  /** **************************************************************************
    * Global Variables
    * ************************************************************************** */

  // Gracefully hide Queue's output -- this is very important otherwise there's Queue output everywhere and it's a mess
  val queueLogDir: String = ".qlog/"

  // ** **************************************************************************
  // * Main script entry point
  // * **************************************************************************
  def script() {
    var statsFiles = List[File]()

    val sampleWebBase =      dirOrCreateOrFail(new File(webSite +  File.separator + experimentalName), "our output web publishing directory")
    val aggReportDir =       dirOrCreateOrFail(new File(webSite +  File.separator + experimentalName + File.separator + "aggregateHMIDReport"), "our report output directory")
    val aggWebTreeLocation = dirOrCreateOrFail(new File(webSite +  File.separator + experimentalName + File.separator + "tree"), "our output tree directory")

    // read in the tear sheet and process each sample
    parseTearSheet(input).foreach(sampleObj => {

      // the sample name we'll use for generating files
      val sampleTag = sampleObj.sample

      // check that our basic output dir can be made
      dirOrCreateOrFail(sampleObj.outputDir, "base output directory")

      // are we running in double or single ended mode?
      if ((sampleObj.fastq1.toUpperCase != "NA" && !sampleObj.fastq1.exists()) || (sampleObj.fastq2.toUpperCase != "NA" && !sampleObj.fastq2.exists())) {
        throw new IllegalStateException("Unable to find one of the read fastqs from the sample " + sampleObj.sample + " please check the tear sheet: fq1 = " + sampleObj.fastq1 + " fq2 = " + sampleObj.fastq2)
      }

      // are we using zero, one, or two barcodes?
      val oneBarcode = sampleObj.fastqBarcode1.exists() && !sampleObj.fastqBarcode1.exists()
      val dualBarcode = sampleObj.fastqBarcode1.exists() && sampleObj.fastqBarcode2.exists()
      val pairedEnd = sampleObj.fastq2.exists()

      // **************************** Setup files and directories ******************************************


      // create the per-sample output directories, verifying that they either exist already or can be made, also make the base web dir, it's ok that this is
      // duplicated, as don't try to make it if it exists
      val webLoc =             webSite + File.separator + sampleObj.sample + File.separator
      val sampleOutput =       dirOrCreateOrFail(new File(sampleObj.outputDir + File.separator + sampleObj.sample), "sample output directory")
      val sampleWebLocation =  dirOrCreateOrFail(new File(webSite +  File.separator + experimentalName + File.separator + sampleTag), "our output web publishing directory")

      // our barcode split files
      var barcodeSplit1      = new File(sampleOutput + File.separator + sampleTag + ".barcodeSplit.fastq1.fq.gz")
      var barcodeSplit2      = new File(sampleOutput + File.separator + sampleTag + ".barcodeSplit.fastq2.fq.gz")
      var barcodeSplitIndex1 = new File(sampleOutput + File.separator + sampleTag + ".barcodeSplit.indexFastq1.fq.gz")
      var barcodeSplitIndex2 = new File(sampleOutput + File.separator + sampleTag + ".barcodeSplit.indexFastq2.fq.gz")

      val samUnmergedFasta = new File(sampleOutput + File.separator + sampleTag + ".UM.fasta")
      val samMergedFasta = new File(sampleOutput + File.separator + sampleTag + ".M.fasta")

      var mergedReads = new File(sampleOutput + File.separator + "out.extendedFrags.fastq")
      var mergedReadUnzipped = new File(sampleOutput + File.separator + sampleTag + ".merged.fq")
      var unmergedUnzipped = new File(sampleOutput + File.separator + sampleTag + ".unmerged.fq")

      
      var inputFiles = if (pairedEnd) List[File](sampleObj.fastq1,sampleObj.fastq2) else List[File](sampleObj.fastq1)

      var processedFastqs = if (pairedEnd) List[File](barcodeSplit1,barcodeSplit2) else List[File](barcodeSplit1)
      val barcodeConfusion = new File(sampleOutput + File.separator + sampleTag + ".barcodeConfusion")
      val barcodeStats = new File(sampleOutput + File.separator + sampleTag + ".barcodeStats")
      val overlapFile = new File(sampleOutput + File.separator + sampleTag + ".readOverlap")


      // ************************************** handle the barcodes **************************************
      val barcodes: Map[Int,String] = (Array[String](sampleObj.barcode1,sampleObj.barcode2)).zipWithIndex.map{case(barcode,index) => (index + 1,barcode)}.toMap

      (sampleObj.fastqBarcode1,sampleObj.fastqBarcode2) match {
        // we have no barcode 1 and no barcode 2, just pass them through
        case (f1, f2) if !sampleObj.fastqBarcode1.exists() && !sampleObj.fastqBarcode2.exists() && (trimStop - trimStart > 0)  => {
          val barcodeInputs = List[File]()
          val barcodeSplitFiles = List[File]()
          println("HERE")
          add(Maul(inputFiles, barcodeInputs, barcodes, processedFastqs, barcodeSplitFiles, barcodeStats, barcodeConfusion,overlapFile, trimStart, trimStop))
        }
        case (f1, f2) if !sampleObj.fastqBarcode1.exists() && !sampleObj.fastqBarcode2.exists()  => {
          processedFastqs = inputFiles
        }

        // we have barcode 1, but no barcode 2
        case (f1, f2) if (sampleObj.fastqBarcode1.exists() && !sampleObj.fastqBarcode2.exists()) => {
          val barcodeInputs = List[File](sampleObj.fastqBarcode1)
          val barcodeSplitFiles = List[File](barcodeSplitIndex1)

          add(Maul(inputFiles, barcodeInputs, barcodes, processedFastqs, barcodeSplitFiles, barcodeStats, barcodeConfusion,overlapFile, trimStart, trimStop))
        }
        // we have no barcode 1, but a barcode 2
        case (f1, f2) if (!sampleObj.fastqBarcode1.exists() && sampleObj.fastqBarcode2.exists()) => {
          val barcodeInputs = List[File](sampleObj.fastqBarcode2)
          val barcodeSplitFiles =  List[File](barcodeSplitIndex2)

          add(Maul(inputFiles, barcodeInputs, barcodes, processedFastqs, barcodeSplitFiles, barcodeStats, barcodeConfusion,overlapFile, trimStart, trimStop))
        }
        // we have no barcode 1, but a barcode 2
        case (f1, f2) if (sampleObj.fastqBarcode1.exists() && sampleObj.fastqBarcode2.exists()) => {
          val barcodeInputs = List[File](sampleObj.fastqBarcode1,sampleObj.fastqBarcode2)
          val barcodeSplitFiles =  List[File](barcodeSplitIndex1,barcodeSplitIndex2)

          add(Maul(inputFiles, barcodeInputs, barcodes, processedFastqs, barcodeSplitFiles, barcodeStats, barcodeConfusion,overlapFile, trimStart, trimStop))

        }
      }
      inputFiles = processedFastqs

      // ************************************** split the input files **************************************
      val cleanedFastqs = List[File](
        new File(sampleOutput + File.separator + "out.notCombined_1.fastq"),
        new File(sampleOutput + File.separator + "out.notCombined_2.fastq"))

      val toAlignFastq1 = new File(sampleOutput + File.separator + sampleTag + ".fwd.fastq")
      val toAlignFastq2 = new File(sampleOutput + File.separator + sampleTag + ".rev.fastq")
      val toAlignStats = new File(sampleOutput + File.separator + sampleTag + ".stats")
      val toAligUMICounts = new File(sampleOutput + File.separator + sampleTag + ".umiCounts")

      val perBaseEventFile = new File(sampleOutput + File.separator + sampleTag + ".perBase")
      val topReadFile = new File(sampleOutput + File.separator + sampleTag + ".topReadEvents")
      val topReadFileNew = new File(sampleOutput + File.separator + sampleTag + ".topReadEventsNew")
      val topReadCount = new File(sampleOutput + File.separator + sampleTag + ".topReadCounts")
      val allReadCount = new File(sampleOutput + File.separator + sampleTag + ".allReadCounts")
      val cutSites = new File(sampleObj.reference + ".cutSites")
      val unpairedReads = List[File](new File(sampleOutput + File.separator + sampleTag + ".unpaired1.fq.gz"), new File(sampleOutput + File.separator + sampleTag + ".unpaired2.fq.gz"))

      // *******************************************************************************************
      // If they've encoded the UMI into one of the index reads (Beth's approach), handle that
      // *******************************************************************************************
      var umiTemp = inputFiles

      umiIndex.toUpperCase match {
        case "NONE" => {
          umiTemp = inputFiles
        } 
        case "INDEX1" => {
          println("INDEX1")
          val outputFirstRead = new File(sampleOutput + File.separator + sampleTag + ".withUMI.fq.gz")
          add(addUMIToReads(umiTemp(0), barcodeSplitIndex1, outputFirstRead))
          umiTemp = List[File](outputFirstRead,inputFiles(1))
        }
        case "INDEX2" => {
          println("INDEX2")
          val outputFirstRead = new File(sampleOutput + File.separator + sampleTag + ".withUMI.fq.gz")
          add(addUMIToReads(umiTemp(0), barcodeSplitIndex2, outputFirstRead ))
          umiTemp = List[File](outputFirstRead,inputFiles(1))
        }
        case _ => throw new IllegalStateException("Unknown UMI index: " + umiIndex)
      }

      var cleanedTmp = if (pairedEnd)
        List[File](new File(sampleOutput + File.separator + sampleTag + ".cleanedInter1.fq.gz"),new File(sampleOutput + File.separator + sampleTag + ".cleanedInter2.fq.gz"))
      else
        List[File](new File(sampleOutput + File.separator + sampleTag + ".cleanedInter1.fq.gz"))

      if (!dontTrim) { // yes a double negitive...sorry: if we want to trim, do this
        add(Trimmomatic(umiTemp,adaptersFile,cleanedTmp,unpairedReads))
      } else {
        // just pass through the input files as 'cleaned'
        cleanedTmp = umiTemp
      }

      // *******************************************************************************************
      // if we're a UMI run, we divert here to merge the reads by UMIs, and put these merged reads back into the pipeline
      // *******************************************************************************************
      if (sampleObj.UMIed) {
        // do we have a second read? handle Bushra's case here
        if (cleanedTmp.size == 2) {
          val toAlignFastq1 = new File(sampleOutput + File.separator + sampleTag + ".umi.fwd.fastq")
          val toAlignFastq2 = new File(sampleOutput + File.separator + sampleTag + ".umi.rev.fastq")

          // collapse the reads by their UMI and output FASTA files with the remaining quality reads
          add(UMIProcessingPaired(
            cleanedTmp(0),
            Some(cleanedTmp(1)),
            toAlignFastq1,
            Some(toAlignFastq2),
            10,
            new File(sampleObj.reference + ".primers"),
            sampleObj.sample,
          toAligUMICounts))

          cleanedTmp = List[File](toAlignFastq1,toAlignFastq2)

        } else if (cleanedTmp.size == 1) {
          val toAlignFastq1 = new File(sampleOutput + File.separator + sampleTag + ".umi.fwd.fastq")

          // collapse the reads by their UMI and output FASTA files with the remaining quality reads
          add(UMIProcessingPaired(
            cleanedTmp(0),
            None,
            toAlignFastq1,
            None,
            10,
            new File(sampleObj.reference + ".primers"),
            sampleObj.sample,
          toAligUMICounts))

          cleanedTmp = List[File](toAlignFastq1)
        }
      }

      // again handle single vs paired end reads
      if (pairedEnd) {
        add(Flash(cleanedTmp, cleanedFastqs, mergedReads, sampleOutput + File.separator))
        add(ZipReadFiles(cleanedFastqs(0), cleanedFastqs(1), unmergedUnzipped))
        add(PerformAlignment(sampleObj.reference,mergedReads, unmergedUnzipped, samMergedFasta, samUnmergedFasta))

        add(ReadsToStats(samUnmergedFasta,
        samMergedFasta,
        toAlignStats,
        cutSites,
        new File(sampleObj.reference + ".primers"),
        sampleObj.sample))

      statsFiles :+= toAlignStats
      } else {
        var newTempMerged = cleanedTmp(0)
        if (cleanedTmp(0).getAbsolutePath.endsWith("gz")) {
          newTempMerged = cleanedTmp(0).getAbsolutePath + ".unzipped"
          add(Gunzip(cleanedTmp(0), newTempMerged))
        }
        add(PerformAlignment(sampleObj.reference, mergedReads, newTempMerged, samMergedFasta, samUnmergedFasta))

        // we've swapped things here, treating the unmerged (single reads) as the merged, and given them a blank unmerged
        add(ReadsToStats(samMergedFasta,
        samUnmergedFasta,
        toAlignStats,
        cutSites,
        new File(sampleObj.reference + ".primers"),
        sampleObj.sample))

      statsFiles :+= toAlignStats
      }

      
      if (!dontWebPublish) {
        add(ToJavascriptTables(toAlignStats, cutSites, sampleObj.reference, perBaseEventFile, topReadFile, topReadCount, allReadCount, topReadFileNew))
        add(ToWebPublish(sampleWebLocation, perBaseEventFile, topReadFileNew, topReadCount, cutSites, allReadCount))
      }
    })

    // agg. all of the stats together into a single file
    val mergedStatsFile = new File(aggregateLocation + File.separator + "merged.stats")
    val mergedUMIInfo = new File(aggregateLocation + File.separator + "merged.info")
    add(AggregateStatsFiles(statsFiles, mergedStatsFile, mergedUMIInfo))
  }

  /** **************************************************************************
    * Helper classes and methods
    * ************************************************************************** */

  // if a directory doesn't exist, create it. Otherwise just return the dir. If anything fails, exception out.
  def dirOrCreateOrFail(dir: File, contentsDescription: String): File = {
    if (!dir.exists()) {
      println("Trying to make dir " + dir)
      if (!dir.mkdirs()) // mkdirs tries to make all the parent directories as well, if it can
        throw new IllegalArgumentException("Unable to find or create " + contentsDescription + " directory: " + dir)
      else
        println("created directory : " + dir.getAbsolutePath)
    } else {
      //println("directory exists! " + dir)
    }
    dir
  }

  // The storage container for the data we've read from the input tear sheet
  case class SourceEntry(
    val sample: String,
    val UMIed: Boolean,
    val reference: File,
    val outputDir: File,
    val fastq1: File,
    val fastq2: File,
    val fastqBarcode1: File,
    val fastqBarcode2: File,
    val barcode1: String,
    val barcode2: String)

  // a read group, as defined in a sam/bam file
  case class ReadGroup(val id: String, // readgroup id
    val lb: String, // the library name
    val pl: String, // platform name: almost always ILLUMINA
    val pu: String, // a platform unit, should be unique to this exact sample+run combination
    val sm: String, // the sample name
    val cn: String, // sequencing center
    val ds: String)

  // a description of the sequencing data

  /**
    * reads in the sample tear sheet, a tab separated file with the columns listed at the top of the file
   */
  def parseTearSheet(input: File): Array[SourceEntry] = {
    val inputLines = Source.fromFile(input).getLines

    // check that the header contains the correct information
    if (inputLines.next().stripLineEnd != "sample\tumi\treference\toutput.dir\tfastq1\tfastq2\tbarcode.fastq1\tbarcode.fastq2\tbarcode1\tbarcode2")
      throw new IllegalArgumentException("Your header doesn't seem like a correctly formatted  tear sheet!")

    return inputLines.map(line => {
      val tokens = line.split( """\s+""") // Use a regex to avoid empty tokens
      try {
        (new SourceEntry(
          tokens(0), // sample
          tokens(1).toBoolean, // case control status: true if control, else a name of a sample for the control
          new File(tokens(2)), // reference
          new File(tokens(3)), // output
          new File(tokens(4)), // fastq1
          new File(tokens(5)), // fastq2
          new File(tokens(6)), // barcode fastq1
          new File(tokens(7)), // barcode fastq2
          tokens(8), // barcode1
          tokens(9) // barcode2
        ))
      }
      catch {
        case e: java.lang.ArrayIndexOutOfBoundsException => {
          println("\n\n****\nunable to find all the needed columns from the input file line: " + line + "\n***\n"); throw e
        }
      }
    }).toArray
  }

   /** Turn a source entry into a read group.  The mapping is rather simple */
  def sourceToReadGroup(source: SourceEntry): ReadGroup = ReadGroup(id = source.sample, lb = source.sample, pl = "ILLUMINA", pu = source.sample, sm = source.sample, cn = "UW", ds = source.sample)

  /** **************************************************************************
    * traits that get tacked onto runnable objects
    * ************************************************************************** */

  // General arguments to non-GATK tools
  trait ExternalCommonArgs extends CommandLineFunction {
    this.memoryLimit = 6 // set this a bit high, there's a weird java / SGE interactions that require higher mem reservations
    this.residentRequest = 6
    this.residentLimit = 6
    this.isIntermediate = false // by default delete the intermediate files, if you want to keep output from a task set this to false in the case class
  }

  /** **************************************************************************
    * Classes (non-GATK programs)
    *
    * a note here: normally we could use the built in Picard (and GATK) functions,
    * but for some reason our SGE interacts very poorly with Java, and we need to
    * build in an even higher 'buffer' of memory (the difference between the
    * requested memory to SGE and Java's Xmx parameter)
    * ************************************************************************** */

   // ********************************************************************************************************
  case class ToJavascriptTables(statsFile: File, cutSites: File, reference: File, perBaseEvents: File, topReads: File, topReadCounts: File, allEventCounts: File, perBaseEventsNew: File) extends ExternalCommonArgs {
    @Input(doc = "the input stats file") var stats = statsFile
    @Input(doc = "the cut sites information file") var cuts = cutSites
    @Input(doc = "the reference file") var ref = reference
    @Output(doc = "per base event counts") var perBase = perBaseEvents
    @Output(doc = "the top reads and their base by base events") var topR = topReads
    @Output(doc = "the the counts for all the top reads") var topReadC = topReadCounts
    @Output(doc = "the the counts for all the reads") var allReadC = allEventCounts
    @Output(doc = "new per base event style") var perBaseES = perBaseEventsNew

    var command = scalaPath + " -J-Xmx6g " + scriptLoc + "/" + toJSTableScript + " " + stats + " " + perBase + " " + topR + " " + topReadC + " " + allReadC + " " + cuts + " " + perBaseES + " " + convertUnknownsToNone + " " + ref

    def commandLine = command

    this.analysisName = queueLogDir + stats + ".toJS"
    this.jobName = queueLogDir + stats + ".toJS"
    this.memoryLimit = 8
    this.residentRequest = 8
    this.residentLimit = 8
  }

  // ********************************************************************************************************
  case class ToWebPublish(webLocation: File, perBaseEvents: File, topReads: File, topReadCounts: File, cutSites: File, allReadCount: File) extends ExternalCommonArgs {
    @Input(doc = "per base event counts") var webL = webLocation
    @Input(doc = "per base event counts") var perBase = perBaseEvents
    @Input(doc = "the top reads and their base by base events") var topR = topReads
    @Input(doc = "the counts for all the top reads") var topReadC = topReadCounts
    @Input(doc = "the cutsites") var cuts = cutSites
    @Input(doc = "all HMIDs") var allReads = allReadCount

    def commandLine = scalaPath + " -J-Xmx1g " + scriptLoc + "/" + toWebPublishScript + " " + webL + " " + perBase + " " + topR + " " + topReadC + " " + cuts + " " + allReads

    this.analysisName = queueLogDir + perBase + ".web"
    this.jobName = queueLogDir + perBase + ".web"
    this.memoryLimit = 2
    this.isIntermediate = false
  }

  // ********************************************************************************************************
  case class Maul(inputFastqs: List[File],
    barcodeFiles: List[File],
    barcodes: Map[Int,String],
    outputFastqs: List[File],
    outputIndexFastqs: List[File],
    barcodeStats: File,
    barcodeConfusion: File,
    overlap: File,
    trimStart: Int,
  trimStop: Int) extends ExternalCommonArgs {

    @Input(doc = "the input fastqs") var inputFQs = inputFastqs
    @Output(doc = "the output fastq files") var outputFQs = outputFastqs
    @Output(doc = "the output index fastq files") var outputIndexFQs = outputIndexFastqs
    @Argument(doc = "the output barcode counts") var outStats = barcodeStats
    @Argument(doc = "the output barcode confusion file") var outBCC = barcodeConfusion
    @Argument(doc = "the output overlap file") var outOver = overlap
    @Argument(doc = "where to start trimming from the reads") var tStart = trimStart
    @Argument(doc = "where to stop trimming from the reads") var tStop = trimStop


    //if ((barcodeFiles.isDefined) && barcodeFiles.size != outputIndexFastqs.size)
    if (barcodeFiles.size != outputIndexFastqs.size)
      throw new IllegalStateException("index input and output files have to be the same size")

    // the base command to run
    var baseCmd = "java -Xmx4g -jar " + binaryLoc + "/" + maulName + " --inFQ1 " + inputFQs(0) + " --outFQ1 " + outputFQs(0)

    // figure out if we're running with one or two barcodes
    if (inputFastqs.length == 2) baseCmd += " --inFQ2 " + inputFQs(1) + " --outFQ2 " + outputFQs(1)

    // if we have an initial barecode
    barcodes.foreach{case(index,barcode) => index match {
      case 1 => baseCmd += " --barcodes1 " + barcode
      case 2 => baseCmd += " --barcodes2 " + barcode
      case _ => throw new IllegalStateException("Unknown barcode")
    }}

    if (tStop - tStart > 0) {
      baseCmd += " --trimStart " + tStart + " --trimStop " + tStop
    }

    //if (barcodeFiles.isDefined)
    barcodeFiles.size match {
      case 0 => {}
      case 1 => baseCmd += " --inBarcodeFQ1 " + barcodeFiles(0) + " --outFQIndex1 " + outputIndexFQs(0)
      case 2 => baseCmd += " --inBarcodeFQ1 " + barcodeFiles(0) + " --inBarcodeFQ2 " + barcodeFiles(1) + " --outFQIndex1 " + outputIndexFQs(0) + " --outFQIndex2 " + outputIndexFQs(1)
    }
   
    def commandLine = baseCmd + " --barcodeStatsFile " + outStats + " --barcodeStatsFileUnknown " + barcodeConfusion
    this.isIntermediate = false
    this.memoryLimit = 8
  }

  // call out the alignment task to
  // ********************************************************************************************************
  case class PerformAlignment(reference: File,
    inFastqMerged: File,
    inFastqPairs: File,
    outputMergedFasta: File,
    outputPairedFasta: File) extends CommandLineFunction with ExternalCommonArgs {

    @Input(doc = "the merged read fastq") var mergedFQ = inFastqMerged
    @Input(doc = "the paired reads fastq") var pairedFQ = inFastqPairs

    @Argument(doc = "the reference fasta/fa") var ref = reference

    @Output(doc = "the output merged fasta file") var outMergedFasta = outputMergedFasta
    @Output(doc = "the output paired fasta file") var outPairedFasta = outputPairedFasta

    def commandLine = scalaPath + " " + scriptLoc + "/" + alignmentScripts + " " + edaMatrix + " " + aligner + " " + mergedFQ + " " + pairedFQ + " " + ref + " " + outMergedFasta + " " + outPairedFasta + " " + gapopen + " " + gapextend

    this.analysisName = queueLogDir + outputMergedFasta + ".aligner"
    this.jobName = queueLogDir + outputMergedFasta + ".aligner"
  }

  // Needleman-Wunsch aligner needs uncompressed input -- stupid aligner
  // ********************************************************************************************************
  case class Gunzip(inFile: File, outFile: File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc = "the merged read fastq") var inF = inFile
    @Output(doc = "the output sam file") var outF = outFile

    def commandLine = "gunzip -c " + inF + " > " + outFile

    this.analysisName = queueLogDir + outF + ".gunzip"
    this.jobName = queueLogDir + outF + ".gunzip"
  }

  // interleave the two read files to better keep track of the reads post alignment
  // ********************************************************************************************************
  case class ZipReadFiles(inRead1File: File, inRead2File: File, outFile: File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc = "the read 1") var inF1 = inRead1File
    @Input(doc = "the read 2") var inF2 = inRead2File
    @Output(doc = "the output sam file") var outF = outFile

    def commandLine = scalaPath + " " + scriptLoc + "/" + zipReadsPath + " " + inF1 + " " + inF2 + " " + outF

    this.analysisName = queueLogDir + outF + ".gunzip"
    this.jobName = queueLogDir + outF + ".gunzip"
  }

  // aggregate stats flies down to a single file
  // ********************************************************************************************************
  case class AggregateStatsFiles(inputFiles: List[File], outputStatsFile: File, outputUmiStatsFile: File)
    extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc = "the umi input file") var inputFls = inputFiles
    @Output(doc = "the output merged stats file") var outStats = outputStatsFile
    @Output(doc = "the output umi information") var outUMIStats = outputUmiStatsFile

    def commandLine = scalaPath + " -J-Xmx8g " + scriptLoc + "/" + aggregateScripts + " " + inputFls.mkString(",") + " " + outStats + " " + outUMIStats

    this.analysisName = queueLogDir + outStats + ".outStats"
    this.jobName = queueLogDir + outStats + ".outStats"
    this.isIntermediate = false
  }

  // move the UMI to the front of the first read
  // ********************************************************************************************************
 case class addUMIToReads(readFile: File, barcodeFile: File, outputReadsAndBarcodes: File)
      extends CommandLineFunction with ExternalCommonArgs {

    @Input(doc = "read file") var reads = readFile
    @Input(doc = "barcodes") var barcodes = barcodeFile
    @Output(doc = "the output umi + reads file") var output = outputReadsAndBarcodes

    def commandLine = scalaPath + " -J-Xmx8g " + scriptLoc + "/" + convertUMIFile + " " + reads + " " + barcodes + " " + output

    this.analysisName = queueLogDir + output + ".umiOnReads"
    this.jobName = queueLogDir + output + ".umiOnReads"
    this.isIntermediate = false
  }

  // gzip a bunch of fastqs into a single gzipped fastq
  // ********************************************************************************************************
  case class ConcatFastqs(inFastqs: List[File], outFastq: File) extends ExternalCommonArgs {
    @Input(doc = "the read fastqs") var fqs = inFastqs
    @Output(doc = "the output compressed read file") var outfq = outFastq

    def commandLine = "gzip -c " + fqs.mkString(" ") + " > " + outfq

    this.analysisName = queueLogDir + outFastq + ".concatFastqs"
    this.jobName = queueLogDir + outFastq + ".concatFastqs"
  }


  /**
   * Trimmomatic -- http://www.usadellab.org/cms/?page=trimmomatic
   *
   * trims off adapter sequence and low quality regions in paired end sequencing data
   */
  // ********************************************************************************************************
  case class Trimmomatic(inFastqs: List[File],
    adapters: File,
    outs: List[File],
    outputUnpaired: List[File]
  ) extends CommandLineFunction with ExternalCommonArgs {

    @Input(doc = "input FASTAs") var fqs = inFastqs
    @Argument(doc = "the adapters file") var adp = adapters

    @Output(doc = "output fastas (corrected)") var fqOuts = outs
    @Output(doc = "output fastas for failed reads") var fqOutsUnpaired = outputUnpaired

    // PUSH up for Molly
    // the parameters string to tack onto the end, specifying:
    var appended = "ILLUMINACLIP:" + adapters.getAbsolutePath() + ":2:30:12" // where to find the illumina adapter file
    appended += " LEADING:" + trimQual // trim off leading bases with a quality less than X
    appended += " TRAILING:" + trimQual // trim off trailing bases with a quality less than X
    appended += " SLIDINGWINDOW:" + trimWindow + ":" + trimQual // remove bases with a quality less than Y in a sliding window of size X
    appended += " MINLEN:" + minLength // the resulting reads must have a length of at least X

    // setup the memory limits, high for trimmomatic (java + SGE = weird memory issues...)
    this.memoryLimit = 4
    this.residentRequest = 4
    this.residentLimit = 4

    // CMD command issued, and the hidden queue output file names
    var cmd = "java -Xmx3g -jar " + binaryLoc + "/" + trimmomaticName + (if (fqs.length == 2) " PE" else " SE") + " -phred33 -threads 1 " + fqs.mkString(" ")
    if (fqs.length == 2) cmd += " " + fqOuts(0) + " " + fqOutsUnpaired(0) + " " + fqOuts(1) + " " + fqOutsUnpaired(1) + " " + appended
    else cmd += " " + fqOuts(0) + " " + " " + appended

    def commandLine = cmd
    this.isIntermediate = false
    this.analysisName = queueLogDir + fqs(0) + ".trimmomatic"
    this.jobName = queueLogDir + fqs(0) + ".trimmomatic"

  }

  /**
   * Process the UMIs, merging and aligning reads down to a single, high-quality concensus per UMI
   */
  // ********************************************************************************************************
  case class UMIProcessingPaired(inMergedReads1: File, inMergedReads2: Option[File], outputFASTA1: File, outputFASTA2: Option[File],
    umiCutOff: Int, primersFile: File, sampleName: String, umiCountsFile: File) extends CommandLineFunction with ExternalCommonArgs {

    @Input(doc = "input reads (fwd)") var inReads1 = inMergedReads1
    @Output(doc = "output fasta for further alignment (fwd)") var outFASTA1 = outputFASTA1
    @Output(doc = "output a counts of the reads behind each UMI") var outUMIs = umiCountsFile
    @Argument(doc = "how many UMIs do we need to initial have to consider merging them") var umiCut = umiCutOff
    @Argument(doc = "the primers file; one line per primer that we expect to have on each end of the resulting merged read") var primers = primersFile
    @Argument(doc = "the sample name") var sample = sampleName

    var cmdString = scalaPath + " -J-Xmx7g " + binaryLoc + "/" + umiName
    cmdString += " --inputFileReads1 " + inReads1 + " --outputFastq1 " + outFASTA1

    if (inMergedReads2.isDefined)
      cmdString += " --inputFileReads2 " + inMergedReads2.get + " --outputFastq2 " + outputFASTA2.get

    cmdString += " --primersEachEnd " + primers + " --samplename " + sample
    cmdString += " --umiStart " + umiStart + " --minimumUMIReads " + minimumUMIReads + " --minimumSurvivingUMIReads " + minimumSurvivingUMIReads
    cmdString += " --umiCounts " + outUMIs + " --umiLength " + umiLength + " --primerMismatches " + maxAdaptMismatch
    cmdString += " --primersToCheck " + primersToCheck

    var cmd = cmdString

    this.memoryLimit = 32
    this.residentRequest = 32
    this.residentLimit = 32

    def commandLine = cmd
    this.isIntermediate = false
    this.analysisName = queueLogDir + inReads1 + ".umis"
    this.jobName = queueLogDir + inReads1 + ".umis"
  }

  //--inputUnmerged --inputMerged --outputStats --cutSites --primersEachEnd --sample test
  case class ReadsToStats(inputUnmerged: File,
    inputMerged: File,
    outputStats: File,
    cutSites: File,
    primersEachEnd: File,
    sampleName: String) extends CommandLineFunction with ExternalCommonArgs {

    @Input(doc = "input reads (fwd)") var unmerged = inputUnmerged
    @Input(doc = "input reads (rev)") var merged = inputMerged
    @Input(doc = "the cutsite locations") var cutSiteFile = cutSites
    @Output(doc = "output statistics file for containing information about the UMI merging process") var outStat = outputStats
    @Argument(doc = "the primers file; one line per primer that we expect to have on each end of the resulting merged read") var primers = primersEachEnd
    @Argument(doc = "the sample name") var sample = sampleName

    var cmdString = "java -jar -Xmx2g " + binaryLoc + "/" + callerName
    cmdString += " --inputUnmerged " + inputUnmerged + " --inputMerged " + inputMerged + " --cutSites "
    cmdString += cutSiteFile + " --outputStats "
    cmdString += outStat + " --primersEachEnd " + primers + " --sample "
    cmdString += sample + " --primerMismatches " + maxAdaptMismatch
    cmdString += " --primersToCheck " + primersToCheck 
    cmdString += " --requiredMatchingProp " + matchProportion
    cmdString += " --requiredRemainingBases " + matchCount

    var cmd = cmdString

    this.memoryLimit = 6
    this.residentRequest = 6
    this.residentLimit = 6

    def commandLine = cmd
    this.isIntermediate = false
    this.analysisName = queueLogDir + outStat + ".calls"
    this.jobName = queueLogDir + outStat + ".calls"
  }

  /**
   * seqprep -- trimmomatic's main competition, does read merging as well
   *
   * trims off adapter sequence and low quality regions in paired end sequencing data, merges on reqest
   */
  // ********************************************************************************************************
  case class SeqPrep(inFastqs: List[File], outs: List[File], outputMerged: File, adapterOne: String, adapterTwo: String) extends CommandLineFunction with ExternalCommonArgs {

    @Input(doc = "input FASTAs") var fqs = inFastqs
    @Output(doc = "output fastas (corrected)") var fqOuts = outs
    @Output(doc = "output merged reads") var merged = outputMerged

    if (inFastqs.length != 2 || outs.length != 2)
      throw new IllegalArgumentException("Seqprep can only be run on paired end sequencing! for input files " + inFastqs.mkString(", "))

    this.memoryLimit = 4
    this.residentRequest = 4
    this.residentLimit = 4

    // o -> minimum overlap, n -> faction of bases that must match in overlap
    var cmd = seqPrepPath + " -f " + fqs(0) + " -r " + fqs(1) + " -1 " + fqOuts(0) + " -2 " + fqOuts(1) + " -s " + merged + " -A " + adapterOne + " -B " + adapterTwo

    def commandLine = cmd
    this.isIntermediate = false
      this.analysisName = queueLogDir + fqs(0) + ".seqprep"
    this.jobName = queueLogDir + fqs(0) + ".seqprep"
  }

  /**
   * flash -- seems to do a much better job with read overlap assembly than SeqPrep or Trimmomatic
   * this is one of those tools where you can't specify the output file only the directory, so our
   * output file names have to be correctly formatted for what flash will output
   */
  // ********************************************************************************************************
  case class Flash(inFastqs: List[File], outs: List[File], outputMerged: File, outputDir: File) extends CommandLineFunction with ExternalCommonArgs {

    @Input(doc = "input FASTAs") var fqs = inFastqs
    @Output(doc = "output unmerged files") var fqOuts = outs
    @Output(doc = "output merged reads") var merged = outputMerged
    @Argument(doc = "the output directory") var outputDr = outputDir

    if (inFastqs.length != 2 || outs.length != 2)
      throw new IllegalArgumentException("Seqprep can only be run on paired end sequencing! for input files " + inFastqs.mkString(", "))

    this.memoryLimit = 4
    this.residentRequest = 4
    this.residentLimit = 4

    var cmd = flashPath + " --min-overlap 30 --max-mismatch-density 0.02 --output-directory=" + outputDr + " " + fqs(0) + " " + fqs(1)

    def commandLine = cmd
    this.isIntermediate = false
      this.analysisName = queueLogDir + fqs(0) + ".flash"
    this.jobName = queueLogDir + fqs(0) + ".flash"
  }
}
