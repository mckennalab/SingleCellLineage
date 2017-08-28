package test.scala.dp

import main.scala.dp.NeedlemanWunsch

import collection.mutable.Stack
import org.scalatest._

/**
  * Created by aaronmck on 7/23/17.
  */
class NeedlemanWunschTest extends FlatSpec with Matchers {

  "Needleman Wunsch" should "correcly align two sequences" in {
    val seqA = "AAA"
    val seqB = "ATA"

    val nmw = new NeedlemanWunsch(seqA,seqB,1,-1,-10)
    nmw.alignment().getScore should be (1.0)
    nmw.alignment().getAlignmentString._1 should be ("AAA")
    nmw.alignment().getAlignmentString._2 should be ("ATA")
  }

  "Needleman Wunsch" should "correcly align two more sequences" in {
    val seqA = "AAA"
    val seqB = "ATA"

    val nmw = new NeedlemanWunsch(seqA,seqB,5,-50,-1)

    nmw.alignment().getScore should be (8.0)
    nmw.alignment().getAlignmentString._1 should be ("AA-A")
    nmw.alignment().getAlignmentString._2 should be ("-ATA")
  }
  "Needleman Wunsch" should "correcly align two more sequences again" in {
    val seqA = "TTAAA"
    val seqB = "TTATA"

    val nmw = new NeedlemanWunsch(seqA,seqB,1,-10,-1)
    nmw.alignment().getScore should be (2.0)
    nmw.alignment().getAlignmentString._1 should be ("TTAA-A")
    nmw.alignment().getAlignmentString._2 should be ("TT-ATA")
  }
}