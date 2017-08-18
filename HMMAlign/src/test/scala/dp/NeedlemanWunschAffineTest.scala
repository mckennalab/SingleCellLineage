package test.scala.dp

import main.scala.dp.{NeedlemanWunsch, NeedlemanWunschAffine}
import collection.mutable.Stack
import org.scalatest._
/**
  * Created by aaronmck on 7/23/17.
  */
class NeedlemanWunschAffineTest extends FlatSpec with Matchers {

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

    val nmw = new NeedlemanWunschAffine(seqA,seqB,1,-1,-1,0)

    nmw.alignment().getScore should be (2.0)

    /*
    println("Match")
    nmw.matchMatrix.printMatrix()
    println()
    nmw.traceM.printMatrix()
    println("gapA")
    nmw.gapAMatrix.printMatrix()
    println()
    nmw.traceGA.printMatrix()
    println("gapB")
    nmw.gapBMatrix.printMatrix()
    println()
    nmw.traceGB.printMatrix()
    */
    nmw.alignment().getAlignmentString._1 should be ("TAAAAT")
    nmw.alignment().getAlignmentString._2 should be ("TA---T")
  }

}
