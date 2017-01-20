
import aligner._
import org.scalatest.{FlatSpec, Matchers}
import reads._
import utils.{CutSites, IndividualCutSite}

import scala.collection.mutable

/**
  * Created by aaronmck on 11/21/16.
  */
class NeedlemanWunschTest extends FlatSpec with Matchers {
  val reference = "ATCGATCGGGG"

  "Needleman Wunsch" should "do a basic alignment correctly" in {
    val nwa = new NeedlemanWunsch(reference)
    val read1 = "ATCATCG"

    val alignment = nwa.align(read1)
    alignment.referenceAlignment should be ("ATCGATCGGGG")
    alignment.queryAlignment should be ("ATC-ATC---G")
  }

  "Needleman Wunsch" should "do another basic alignment correctly" in {
    val nwa = new NeedlemanWunsch(reference)
    val read1 = "AATCGATCGGGGG"

    val alignment = nwa.align(read1)
    alignment.referenceAlignment should be ("-ATCGATC-GGGG")
    alignment.queryAlignment should be ("AATCGATCGGGGG")
  }

  "Needleman Wunsch" should "do another (2) basic alignment correctly" in {
    val nwa = new NeedlemanWunsch(reference)
    val read1 = "AATCGATGGGG"

    val alignment = nwa.align(read1)
    alignment.referenceAlignment should be ("-ATCGATCGGGG")
    alignment.queryAlignment should be ("AATCGAT-GGGG")
  }

  "Needleman Wunsch" should "work on a trivial primer alignment" in {
    val part1 = "TCGAACTGAGTCCAGACACCCGGACGCCACC"
    val part2 = "GAACTGAGTCCAGACACCCGGACGCCACC"
    val nwa = new NeedlemanWunsch(part1)
    val alignment = nwa.align(part2)

    alignment.referenceAlignment should be ("TCGAACTGAGTCCAGACACCCGGACGCCACC")
        alignment.queryAlignment should be ("--GAACTGAGTCCAGACACCCGGACGCCACC")
  }

  def readAlignments(fasta: String): Unit = {

  }

  case class AlignmentSet(alignedRead: String, alignedRef: String) {
    def read = alignedRead.filter{case(c) => c != '-'}.mkString("")
    def ref = alignedRef.filter{case(c) => c != '-'}.mkString("")
  }
}

