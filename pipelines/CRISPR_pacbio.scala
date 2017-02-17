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
    * Data Parameters
    * ************************************************************************** */
  @Input(doc = "Tab-delimited tear sheet containing sample information", fullName = "input", shortName = "i", required = true)
  var input: File = _

  @Argument(doc = "the experiment name, used to generate the base output folder on the web server", fullName = "expName", shortName = "expName", required = true)
  var experimentalName: String = ""

  /** **************************************************************************
    * Optional Parameters -- control parameters for the script (alignment, etc)
    * ************************************************************************** */

  @Argument(doc = "are we runing with UMI reads? If so, set this value to > 0, representing the UMI length", fullName = "umiLength", shortName = "umiLength", required = false)
  var umiLength: Int = 10

  @Argument(doc = "where does the UMI start in the read", fullName = "umiStart", shortName = "umiStart", required = false)
  var umiStart: Int = 0

  @Input(doc = "where to put the web files", fullName = "web", shortName = "web", required = false)
  var webSite: File = "/net/shendure/vol2/www/content/members/aaron/staging/"

  /** **************************************************************************
    * Path parameters -- where to find tools
    * ************************************************************************** */
  @Input(doc = "The path to a functional installation of the scala tool", fullName = "scala", shortName = "scala", required = false)
  var scalaPath: File = new File("/net/gs/vol1/home/aaronmck/tools/bin/scala")

  @Input(doc = "Invert the pacbio reads", fullName = "invert", shortName = "invert", required = false)
  var invertPacbio: File = new File("/net/shendure/vol10/projects/CRISPR.lineage/nobackup/bin/PacBioInverter.jar")

  @Input(doc = "the script to analyze the per site edits", fullName = "writeweb", shortName = "wwwrite", required = false)
  var writeWebFiles: File = "/net/gs/vol1/home/aaronmck/source/sandbox/aaron/projects/CRISPR/scripts/write_web_files.scala"

  @Input(doc = "the location of the needleall program", fullName = "needle", shortName = "needle", required = false)
  var needlePath: File = "/net/gs/vol1/home/aaronmck/tools/bin/needleall"

  @Input(doc = "the path the javascript conversion script", fullName = "JSTable", shortName = "JSTable", required = false)
  var toJSTableScript: File = "/net/shendure/vol10/projects/CRISPR.lineage/nobackup/codebase/scripts/stats_to_javascript_tables2.scala"

  @Argument(doc = "move the data over to the web location", fullName = "webpub", shortName = "webpub", required = false)
  var toWebPublishScript = "/net/shendure/vol10/projects/CRISPR.lineage/nobackup/codebase/scripts/push_to_web_location.scala"

  @Argument(doc = "move the data over to the web location", fullName = "invertScript", shortName = "invertScript", required = false)
  var checkInversionScript = "/net/shendure/vol10/projects/CRISPR.lineage/nobackup/codebase/scripts/check_pacbio_inversion.scala"

  @Argument(doc = "where to find the EDA file for alignment with NW", fullName = "eda", shortName = "eda", required = false)
  var edaMatrix = "/net/shendure/vol10/projects/CRISPR.lineage/nobackup/reference_data/EDNAFULL"

  @Argument(doc = "do we want to highlight certain regions in the plot? define intervals here", fullName = "highlight", shortName = "highlight", required = false)
  var regionsFile: Option[File] = None
  
  @Argument(doc = "how many bases to consider around the cutsite when calling events", fullName = "cutWindow", shortName = "cutWindow", required = false)
  var cutsiteWindow = 3

  @Argument(doc = "Do we want to check primers on both ends, one end, or neither", fullName = "primersToUse", shortName = "primersToUse", required = false)
  var primersToCheck: String = "BOTH"

  @Argument(doc = "Do we want to check primers on both ends, one end, or neither", fullName = "minRead", shortName = "minRead", required = false)
  var minLength: Int = 10

  @Argument(doc = "Do we want to convert UNKNOWN calls into NONE calls?", fullName = "unknownToNone", shortName = "unknownToNone", required = false)
  var convertUnknownsToNone: Boolean = false

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

      // **************************** Setup files and directories ******************************************

      // create the per-sample output directories, verifying that they either exist already or can be made, also make the base web dir, it's ok that this is
      // duplicated, as don't try to make it if it exists
      val webLoc =             webSite + File.separator + sampleObj.sample + File.separator
      val sampleOutput =       dirOrCreateOrFail(new File(sampleObj.outputDir + File.separator + sampleObj.sample), "sample output directory")
      val sampleWebLocation =  dirOrCreateOrFail(new File(webSite +  File.separator + experimentalName + File.separator + sampleTag), "our output web publishing directory")

      val primersFile = new File(sampleObj.reference.getAbsolutePath + ".primers")
      val cutSites = new File(sampleObj.reference.getAbsolutePath + ".cutSites")
      val orientedFasta = new File(sampleOutput + File.separator + sampleTag + ".oriented.fasta")
      val orientedPassedFasta = new File(sampleOutput + File.separator + sampleTag + ".oriented.passed.fasta")
      val orientedPassedFastaCalls = new File(sampleOutput + File.separator + sampleTag + ".oriented.passed.fasta.calls")
      val alignedFasta = new File(sampleOutput + File.separator + sampleTag + ".aligned.fasta")
      val alignedStats = new File(sampleOutput + File.separator + sampleTag + ".aligned.stats")
      
      val perBaseEventFile = new File(sampleOutput + File.separator + sampleTag + ".perBase")
      val topReadFile = new File(sampleOutput + File.separator + sampleTag + ".topReadEvents")
      val topReadFileNew = new File(sampleOutput + File.separator + sampleTag + ".topReadEventsNew")
      val topReadCount = new File(sampleOutput + File.separator + sampleTag + ".topReadCounts")
      val allReadCount = new File(sampleOutput + File.separator + sampleTag + ".allReadCounts")

      add(InvertPacbio(sampleObj.fastq, orientedFasta, primersFile))
      add(CheckForLargeInversions(sampleObj.reference,orientedFasta,orientedPassedFastaCalls,orientedPassedFasta, 100))

      add(PerformAlignment(sampleObj.reference, orientedPassedFasta, alignedFasta))

      
      add(ReadsToStats(alignedFasta,
        alignedStats,
        cutSites,
        primersFile,
        sampleObj.sample))

      add(ToJavascriptTables(alignedStats, cutSites, sampleObj.reference, perBaseEventFile, topReadFile, topReadCount,allReadCount, topReadFileNew))
      add(ToWebPublish(sampleWebLocation, perBaseEventFile, topReadFileNew, topReadCount, cutSites, allReadCount))
    })
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
    val reference: File,
    val outputDir: File,
    val fastq: File,
    val barcode: String)

  /**
    * reads in the sample tear sheet, a tab separated file with the columns listed at the top of the file
   */
  def parseTearSheet(input: File): Array[SourceEntry] = {
    val inputLines = Source.fromFile(input).getLines

    // check that the header contains the correct information
    if (inputLines.next().stripLineEnd != "sample\treference\toutput\tfastq\tbarcode")
      throw new IllegalArgumentException("Your header doesn't seem like a correctly formatted  tear sheet!")

    return inputLines.map(line => {
      val tokens = line.split( """\s+""") // Use a regex to avoid empty tokens
      try {
        (new SourceEntry(
          tokens(0),           // sample
          new File(tokens(1)), // reference
          new File(tokens(2)), // output
          new File(tokens(3)), // fastq
          tokens(4)            // barcode
        ))
      }
      catch {
        case e: java.lang.ArrayIndexOutOfBoundsException => {
          println("\n\n****\nunable to find all the needed columns from the input file line: " + line + "\n***\n"); throw e
        }
      }
    }).toArray
  }

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

    var command = scalaPath + " -J-Xmx6g " + toJSTableScript + " " + stats + " " + perBase + " " + topR + " " + topReadC + " " + allReadC + " " + cuts + " " + perBaseES + " " + convertUnknownsToNone + " " + ref
    if (regionsFile.isDefined)
      command += " " + regionsFile.get.getAbsolutePath

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

    var cmd = scalaPath + " -J-Xmx1g " + toWebPublishScript + " " + webL + " " + perBase + " " + topR + " " + topReadC + " " + cuts + " " + allReads
    if (regionsFile.isDefined) {
      cmd += " " + regionsFile.get.getAbsolutePath
    }
    def commandLine = cmd

    this.analysisName = queueLogDir + perBase + ".web"
    this.jobName = queueLogDir + perBase + ".web"
    this.memoryLimit = 2
    this.isIntermediate = false
  }

  // call out the alignment task to
  // ********************************************************************************************************
  case class PerformAlignment(reference: File,
    inFastq: File,
    outputFasta: File) extends CommandLineFunction with ExternalCommonArgs {

    @Argument(doc = "the reference fasta/fa") var ref = reference
    @Input(doc = "the reads fastq") var inFQ = inFastq
    @Output(doc = "the output paired fasta file") var outFasta = outputFasta

    def commandLine = needlePath + " -datafile " + edaMatrix + " -snucleotide1 -snucleotide2 -aformat3 fasta -gapextend 0.5 -gapopen 10.0 -asequence " + ref + " -bsequence " + inFQ + " -outfile " + outFasta

    this.analysisName = queueLogDir + outFasta + ".aligner"
    this.jobName = queueLogDir + outFasta + ".aligner"
  }

  // call out the alignment task to
  // ********************************************************************************************************
  case class CheckForLargeInversions(reference: File,orientedFasta: File, inversionResults: File, orientedPassedFasta: File, readLenThreshold: Int) extends CommandLineFunction with ExternalCommonArgs {
    @Argument(doc = "the reference fasta/fa") var ref = reference
    @Argument(doc = "the inversion results") var invResults = inversionResults
    @Argument(doc = "the inversion threshold") var rlThresh = readLenThreshold
    @Input(doc = "the oriented fasta file") var orFasta = orientedFasta
    @Output(doc = "the reads passing the inversion check") var orPassFasta = orientedPassedFasta

    def commandLine = scalaPath + " -J-Xmx2g " + checkInversionScript + " " + ref + " " + orFasta + " " + invResults + " " + orPassFasta + " " + rlThresh

    this.analysisName = queueLogDir + orPassFasta + ".checkLI"
    this.jobName = queueLogDir + orPassFasta + ".checkLI"
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

  //
  // gzip a bunch of fastqs into a single gzipped fastq
  // ********************************************************************************************************
  case class InvertPacbio(inFasta: File, outFasta: File, primersFile: File) extends ExternalCommonArgs {
    @Input(doc = "the read fasta") var inFa = inFasta
    @Input(doc = "the primers file") var primers = primersFile
    @Output(doc = "the output fasta") var outfa = outFasta

    def commandLine = "java -Xmx4g -jar " + invertPacbio + " --fasta " + inFasta + " --outFile " + outFasta + " --primers " + primers

    this.analysisName = queueLogDir + outfa + ".invert"
    this.jobName = queueLogDir + outfa + ".invert"
  }


  /**
   * Process the UMIs, merging and aligning reads down to a single, high-quality concensus per UMI
   *
  // ********************************************************************************************************
  case class UMIProcessingPaired(inMergedReads1: File, inMergedReads2: File, outputFASTA1: File, outputFASTA2: File,
    umiCutOff: Int, primersFile: File, sampleName: String, umiCountsFile: File) extends CommandLineFunction with ExternalCommonArgs {

    @Input(doc = "input reads (fwd)") var inReads1 = inMergedReads1
    @Input(doc = "input reads (rev)") var inReads2 = inMergedReads2
    @Output(doc = "output fasta for further alignment (fwd)") var outFASTA1 = outputFASTA1
    @Output(doc = "output fasta for further alignment (rev)") var outFASTA2 = outputFASTA2
    @Output(doc = "output a counts of the reads behind each UMI") var outUMIs = umiCountsFile
    @Argument(doc = "how many UMIs do we need to initial have to consider merging them") var umiCut = umiCutOff
    @Argument(doc = "the primers file; one line per primer that we expect to have on each end of the resulting merged read") var primers = primersFile
    @Argument(doc = "the sample name") var sample = sampleName

    var cmdString = scalaPath + " -J-Xmx23g /net/shendure/vol10/projects/CRISPR.lineage/nobackup/bin/UMIMerge.jar "
    cmdString += " --inputFileReads1 " + inReads1 + " --inputFileReads2 " + inReads2 + " --outputFastq1 " + outFASTA1 + " --outputFastq2 " + outFASTA2
    cmdString += " --primersEachEnd " + primers + " --samplename " + sample
    cmdString += " --umiStart " + umiStart + " --minimumUMIReads " + minimumUMIReads + " --minimumSurvivingUMIReads " + minimumSurvivingUMIReads
    cmdString += " --umiCounts " + outUMIs + " --umiLength " + umiLength + " --primerMismatches " + maxAdaptMismatch
    cmdString += " --primersToCheck " + primersToCheck

    if (processSingleReads)
      cmdString += " --processSingleReads true "

    var cmd = cmdString

    this.memoryLimit = 32
    this.residentRequest = 32
    this.residentLimit = 32

    def commandLine = cmd
    this.isIntermediate = false
    this.analysisName = queueLogDir + inReads1 + ".umis"
    this.jobName = queueLogDir + inReads1 + ".umis"
  }*/

  case class ReadsToStats(inputMerged: File, outputStats: File, cutSites: File, primersEachEnd: File, sampleName: String) extends CommandLineFunction with ExternalCommonArgs {

    @Input(doc = "input reads (rev)") var merged = inputMerged
    @Input(doc = "the cutsite locations") var cutSiteFile = cutSites
    @Output(doc = "output statistics file for containing information about the UMI merging process") var outStat = outputStats
    @Argument(doc = "the primers file; one line per primer that we expect to have on each end of the resulting merged read") var primers = primersEachEnd
    @Argument(doc = "the sample name") var sample = sampleName

    var cmdString = "java -jar -Xmx2g /net/shendure/vol10/projects/CRISPR.lineage/nobackup/bin/ReadToStats.jar "
    cmdString += " --inputMerged " + inputMerged + " --cutSites "
    cmdString += cutSiteFile + " --outputStats "
    cmdString += outStat + " --primersEachEnd " + primers + " --sample "
    cmdString += sample + " --primerMismatches 10"
    cmdString += " --primersToCheck " + primersToCheck + " --cutsiteWindow " + cutsiteWindow

    var cmd = cmdString

    this.memoryLimit = 6
    this.residentRequest = 6
    this.residentLimit = 6

    def commandLine = cmd
    this.isIntermediate = false
    this.analysisName = queueLogDir + outStat + ".calls"
    this.jobName = queueLogDir + outStat + ".calls"
  }

}
