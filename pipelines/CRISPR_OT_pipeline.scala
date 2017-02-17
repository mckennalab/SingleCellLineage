/** *********************************************
  * Requires Java 1.7. On the UW machines this means
  * running at least: "module load java/7u17", or the appropriate
  * module if there's a more recent version, to load java 1.7 into
  * your environment.  The best idea is to place this into your
  * bash/shell profile
  *
  *
  * Copyright (c) 2016, aaronmck
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
  * @date Sept, 2016
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
 * Quality control sequencing data and optionally align the data to the genome
 **/
class DNAQC extends QScript {
  qscript =>

  /** **************************************************************************
    * Required Parameters
    * ************************************************************************** */
  @Input(doc = "Tab-delimited (seperated with commas)", fullName = "input", shortName = "i", required = true)
  var input: File = _

  /** **************************************************************************
    * Optional Parameters -- control parameters for the script (alignment, etc)
    * ************************************************************************** */

  @Argument(doc = "are we runing with UMI reads? If so set this value to > 0, representing the UMI length", fullName = "umi", shortName = "umi", required = false)
  var umiData: Int = 0

  /** **************************************************************************
    * Path parameters -- where to find tools
    * ************************************************************************** */

  @Input(doc = "the location of the needleall program", fullName = "needle", shortName = "needle", required = false)
  var needlePath: File = "/net/gs/vol1/home/aaronmck/tools/bin/needleall"

  @Input(doc = "where we can find the paired-end UMI counting script", fullName = "umiScriptPE", shortName = "umiScriptPE", required = false)
  var umiPreprocessScript: File = "/net/shendure/vol10/projects/CRISPR.lineage/nobackup/codebase/scripts/convert_OT_fastq.scala"

  @Input(doc = "table generation script", fullName = "tableGen", shortName = "tableGen", required = false)
  var tableGenenerator: File = "/net/shendure/vol10/projects/CRISPR.lineage/nobackup/codebase/scripts/zip_OT_alignments.scala"

  @Input(doc = "convert a list of UMIs to cells", fullName = "umiToCell", shortName = "umiToCell", required = false)
  var umiToCellScript: File = "/net/shendure/vol10/projects/CRISPR.lineage/nobackup/codebase/scripts/process_UMIs_for_OT_screen.scala"

  /** **************************************************************************
    * Global Variables
    * ************************************************************************** */
  @Argument(doc = "the sequence we expect to see at the end of the sequence", fullName = "endPrimer", shortName = "endPrimer", required = false)
  var endPrimer: String = "TCAGATCCGT"

  // Gracefully hide Queue's output -- this is very important otherwise there's Queue output everywhere and it's a mess
  val queueLogDir: String = ".qlog/"

  /** **************************************************************************
    * Main script
    * ************************************************************************** */
  def script() {
    // read in the tear sheet and process each sample
    parseTearSheet(input).foreach(sampleObj => {

      val sampleOutput =       dirOrCreateOrFail(new File(sampleObj.outputDir + File.separator + sampleObj.sample), "sample output directory")

      // check that our basic output dir can be made
      dirOrCreateOrFail(sampleObj.outputDir, "base output directory")
      dirOrCreateOrFail(sampleOutput, "sample output directory")

      // our processed file
      val processedUMI = new File(sampleOutput + File.separator + sampleObj.sample + ".processed.fq.gz")

      // preprocess off the UMIs
      add(UMIProcessPaired(sampleObj.fastq, processedUMI))

      // the output fastq file for alignment and our table output
      val fastqUnzipped    = new File(sampleOutput + File.separator + sampleObj.sample + ".unaligned.processed.fq")
      val fastqAligned     = new File(sampleOutput + File.separator + sampleObj.sample + ".aligned.processed.fq")
      val fastqTable       = new File(sampleOutput + File.separator + sampleObj.sample + ".table")
      val sortedFastqTable = new File(sampleOutput + File.separator + sampleObj.sample + ".sorted.table")
      val cellCalls        = new File(sampleOutput + File.separator + sampleObj.sample + ".umiToCells")

      // align with needleman
      add(Gunzip(processedUMI,fastqUnzipped))
      add(NeedlemanAll(sampleObj.reference, fastqUnzipped, fastqAligned))

      // and process the final table for each
      add(ProcessAlignments(fastqAligned, fastqTable, sampleObj.pattern, sampleObj.reference + ".cutSites"))
      add(SortTable(fastqTable,sortedFastqTable))
      add(UMIToCell(sortedFastqTable,cellCalls))
    })
  }

  /** **************************************************************************
    * Helper classes and methods
    * ************************************************************************** */

  // if a directory doesn't exist, create it. Otherwise create it. if that fails, exception out.
  // return the directory as a file
  def dirOrCreateOrFail(dir: File, contentsDescription: String): File = {
    if (!dir.exists())
      if (!dir.mkdirs()) // mkdirs tries to make all the parent directories as well, if it can
        throw new IllegalArgumentException("Unable to find or create " + contentsDescription + " directory: " + dir)
      else
        println("created directory : " + dir.getAbsolutePath)
    dir
  }

  // The storage container for the data we've read from the input tear sheet
  case class SourceEntry(
    val sample: String,
    val fastq: File,
    val reference: File,
    val outputDir: File,
    val pattern: String)

  /**
    * reads in the sample tear sheet, a tab separated file with the columns listed at the top of the file
   */
  def parseTearSheet(input: File): Array[SourceEntry] = {
    val inputLines = Source.fromFile(input).getLines

    // check that the header contains the correct information
    if (inputLines.next().stripLineEnd != "sample\tfastq\treference\toutput.dir\tbarcode\tpattern")
      throw new IllegalArgumentException("Your header doesn't seem like a correctly formatted  tear sheet!")

    return inputLines.map(line => {
      val tokens = line.split( """\s+""") // Use a regex to avoid empty tokens
      try {
        (new SourceEntry(
          tokens(0), // sample
          new File(tokens(1)), // fastq
          new File(tokens(2)), // reference
          new File(tokens(3)), // output
          tokens(4)  // pattern
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
    //this.isIntermediate = false // by default delete the intermediate files, if you want to keep output from a task set this to false in the case class
  }

  /** **************************************************************************
    * Classes (non-GATK programs)
    *
    * a note here: normally we could use the built in Picard (and GATK) functions,
    * but for some reason our SGE interacts very poorly with Java, and we need to
    * build in an even higher 'buffer' of memory (the difference between the
    * requested memory to SGE and Java's Xmx parameter)
    * ************************************************************************** */

  // Needleman-Wunsch aligner for single ended (or merged) reads
  // ********************************************************************************************************
  case class NeedlemanAll(reference: File, inFastq: File, outputSam: File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc = "the merged read fastq") var fq = inFastq
    @Argument(doc = "the reference fasta/fa") var ref = reference
    @Output(doc = "the output sam file") var outSam = outputSam

    def commandLine = needlePath + " -aformat3 fasta -gapextend 0.5 -gapopen 20.0 -awidth3=5000 -asequence " + ref + " -bsequence " + fq + " -outfile " + outSam

    this.analysisName = queueLogDir + outSam + ".needle"
    this.jobName = queueLogDir + outSam + ".needle"
  }

  // Needleman-Wunsch aligner needs uncompressed input -- stupid aligner
  // ********************************************************************************************************
  case class Gunzip(inFile: File, outFile: File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc = "the merged read fastq") var inF = inFile
    @Output(doc = "the output sam file") var outF = outFile

    def commandLine = "gunzip -c " + inF + " > " + outFile

    this.analysisName = queueLogDir + inFile + ".gunzip"
    this.jobName = queueLogDir + inFile + ".gunzip"
  }

  // process the read
  // ********************************************************************************************************
  case class UMIProcessPaired(inputFastq: File, outputFastq: File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc = "input fastq") var iFQ1 = inputFastq
    @Output(doc = "the output first-read fastq file") var oFQ1 = outputFastq

    def commandLine = "~/tools/bin/scala " + umiPreprocessScript + " " + oFQ1 + " " + iFQ1

    this.analysisName = queueLogDir + inputFastq + ".UMIProcessPaired"
    this.jobName = queueLogDir + inputFastq + ".UMIProcessPaired"
  }

  // create read table from the alignments
  // ********************************************************************************************************
  case class ProcessAlignments(alignmentFile: File, outputTable: File, samplePatternStr: String, cutSitesFilePath: String) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc = "input alignment") var alignment  = alignmentFile
    @Output(doc = "output table") var table = outputTable
    @Argument(doc = "our sample pattern string") val samplePattern = samplePatternStr
    @Argument(doc = "the cutsite file") val cutSiteFile = cutSitesFilePath

    def commandLine = "~/tools/bin/scala " + tableGenenerator + " " + outputTable + " " + alignmentFile + " " + samplePattern + " " + cutSiteFile + " " + endPrimer

    this.analysisName = queueLogDir + outputTable + ".table"
    this.jobName = queueLogDir + outputTable + ".table"
  }

  // sort the output table
  // ********************************************************************************************************
  case class SortTable(inputTable: File, outputTable: File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc = "input table") var inTable  = inputTable
    @Output(doc = "output table") var outTable = outputTable

    def commandLine = "sort -k 1 " + inTable + " > " + outTable

    // set high mem limits for the sort
    this.memoryLimit = 32
    this.residentRequest = 32
    this.residentLimit = 32
    this.analysisName = queueLogDir + outTable + ".sortTable"
    this.jobName = queueLogDir + outTable + ".sortTable"
  }

  // reduce the UMI list to cells
  // ********************************************************************************************************
  case class UMIToCell(inputTable: File, outputTable: File) extends CommandLineFunction with ExternalCommonArgs {
    @Input(doc = "input table") var inTable  = inputTable
    @Output(doc = "output table") var outTable = outputTable

    def commandLine = "~/tools/bin/scala " + umiToCellScript + " " + inTable + " " + outTable

    // set high mem limits for the sort
    this.analysisName = queueLogDir + outTable + ".umiCell"
    this.jobName = queueLogDir + outTable + ".umiCell"
  }

}
