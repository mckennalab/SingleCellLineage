package pacbio

import java.io.{File, PrintWriter}

import aligner.{AlignmentManager, NeedlemanWunsch}
import utils.CutSites
import stats._
import utils._
import reads.{ReadPair, ReadPairParser, RefReadPair, UnmergedReadParser}

import scala.collection.mutable._
import scala.io._

case class PBConfig(fasta: Option[File] = None,
                      outFile: Option[File] = None,
                      primers: Option[File] = None)


/**
  * Created by aaronmck on 11/21/16.
  */
object PacBioInverter extends App {
  // parse the command line arguments
  val parser = new scopt.OptionParser[PBConfig]("DeepSeq") {
    head("DeepSeq", "1.0")

    // *********************************** Inputs *******************************************************
    opt[File]("fasta") valueName ("<file>") required() action { (x, c) => c.copy(fasta = Some(x)) } text ("the input fasta file")
    opt[File]("outFile") valueName ("<file>") required() action { (x, c) => c.copy(outFile = Some(x)) } text ("output file")
    opt[File]("primers") valueName ("<file>") required() action { (x, c) => c.copy(primers = Some(x)) } text ("the primers file")

    // some general command-line setup stuff
    note("process aligned reads from non-UMI samples\n")
    help("help") text ("prints the usage information you see here")
  }

  // *********************************** Run *******************************************************
  // run the actual read processing -- our argument parser found all of the parameters it needed
  parser.parse(args, PBConfig()) map {
    config: PBConfig => {
      val primerLines = Source.fromFile(config.primers.get.getAbsolutePath).getLines().toArray
      if (primerLines.size != 2)
        throw new IllegalStateException("Unable to parse the correct number of primers, we found: " + primerLines.size)

      var readname : Option[String] = None
      var readbases = ""

      val outFasta = new PrintWriter(config.outFile.get.getAbsolutePath)

      val nwaForw = new NeedlemanWunsch(primerLines(0))
      val nwaRevC = new NeedlemanWunsch(Utils.reverseComplement(primerLines(1)))

      Source.fromFile(config.fasta.get.getAbsolutePath).getLines().foreach{line => {
        if ((line startsWith ">") && !readname.isEmpty) {
          val alignedForward = nwaForw.align(readbases.slice(0,primerLines(0).size))
          val alignedReverse = nwaForw.align(readbases.slice(0,primerLines(1).size))

          val matchForward = Utils.editDistance(alignedForward.referenceAlignment,alignedForward.queryAlignment)
          val matchReverse = Utils.editDistance(alignedReverse.referenceAlignment,alignedReverse.queryAlignment)

          if ((matchForward.toDouble / primerLines(0).size.toDouble) <= (matchReverse.toDouble / primerLines(1).size.toDouble))
            outFasta.write(">" + readname.get + "\n" + readbases + "\n")
          else
            outFasta.write(">" + readname.get + "\n" + Utils.reverseComplement(readbases) + "\n")
          readname = Some(line.slice(1,line.size))
          readbases = ""
        } else if (line startsWith ">") {
          readname = Some(line.slice(1,line.size))
          readbases = ""
        } else {
          readbases += line
        }
      }}

      if (!readname.isEmpty) {
        val alignedForward = nwaForw.align(readbases.slice(0,primerLines(0).size))
        val alignedReverse = nwaForw.align(readbases.slice(0,primerLines(1).size))

        val matchForward = Utils.editDistance(alignedForward.referenceAlignment,alignedForward.queryAlignment)
        val matchReverse = Utils.editDistance(alignedReverse.referenceAlignment,alignedReverse.queryAlignment)

        if ((matchForward.toDouble / primerLines(0).size.toDouble) <= (matchReverse.toDouble / primerLines(1).size.toDouble))
          outFasta.write(">" + readname.get + "\n" + readbases + "\n")
        else
          outFasta.write(">" + readname.get + "_INVERTED\n" + Utils.reverseComplement(readbases) + "\n")
      }

      outFasta.close()
    }
  } getOrElse {
    println("Unable to parse the command line arguments you passed in, please check that your parameters are correct")
  }

}