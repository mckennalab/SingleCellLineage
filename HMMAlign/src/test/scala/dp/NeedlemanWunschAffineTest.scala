package test.scala.dp

import main.scala.dp.{NeedlemanWunsch, NeedlemanWunschAffine}
import collection.mutable.Stack
import org.scalatest._
import matchers.should._
import org.scalatest.flatspec._
/**
  * Created by aaronmck on 7/23/17.
  */
class NeedlemanWunschAffineTest extends AnyFlatSpec with Matchers {
  /*
  "Needleman Wunsch Affine" should "correcly align two sequences" in {
    val seqA = "AAA"
    val seqB = "A"

    val nmw = new NeedlemanWunschAffine(seqA,seqB,1,-1,-1,0)

    nmw.alignment().getScore should be (0.0)
    nmw.alignment().getAlignmentString._1 should be ("AAA")
    nmw.alignment().getAlignmentString._2 should be ("--A")
  }

  "Needleman Wunsch Affine" should "correcly align two longer sequences" in {
    val seqA = "AAAAAAAAA"
    val seqB = "A"

    val nmw = new NeedlemanWunschAffine(seqA,seqB,1,-1,-1,0)

    nmw.alignment().getScore should be (0.0)
    nmw.alignment().getAlignmentString._1 should be ("AAAAAAAAA")
    nmw.alignment().getAlignmentString._2 should be ("--------A")
  }

  "Needleman Wunsch Affine" should "correcly align two anchored longer sequences" in {
    val seqA = "TAAAAT"
    val seqB = "TAT"

    val nmw = new NeedlemanWunschAffine(seqA,seqB,10,-5,-10,-1)

    nmw.alignment().getScore should be (18.0)
    nmw.alignment().getAlignmentString._1 should be ("TAAAAT")
    nmw.alignment().getAlignmentString._2 should be ("TA---T")
  }

  "Needleman Wunsch Affine" should "correcly align single-end anchored longer sequences" in {
    val seqA = "TAAAAT"
    val seqB = "TA"

    val nmw = new NeedlemanWunschAffine(seqA,seqB,10,-5,-10,-1)

    nmw.alignment().getScore should be (7.0)
    nmw.alignment().getAlignmentString._1 should be ("TAAAAT")
    nmw.alignment().getAlignmentString._2 should be ("TA----")
  }

  "Needleman Wunsch Affine" should "correcly align far-end anchored longer sequences" in {
    val seqA = "AT"
    val seqB = "TAAAAAAT"

    val nmw = new NeedlemanWunschAffine(seqA,seqB,10,-5,-10,-1)

    nmw.alignment().getScore should be (5.0)
    nmw.alignment().getAlignmentString._1 should be ("------AT")
    nmw.alignment().getAlignmentString._2 should be ("TAAAAAAT")
  }

  "Needleman Wunsch Affine" should "put in two gaps when needed" in {
    val seqA = "TAGG"
    val seqB = "TAAAAGGAAT"

    val nmw = new NeedlemanWunschAffine(seqA,seqB,10,-5,-8,-1)

    nmw.traceBest.printMatrix()
    println()
    nmw.matchMatrix.printMatrix()
    println()
    nmw.insertAMatrix.printMatrix()
    println()
    nmw.insertBMatrix.printMatrix()
    nmw.alignment().getScore should be (20.0)
    nmw.alignment().getAlignmentString._2 should be ("TAAAAGGAAT")
    nmw.alignment().getAlignmentString._1 should be ("T---AGG---")

  }*/

}
