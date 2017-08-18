package test.scala.dp

import main.scala.dp.{ConvexDP, NeedlemanWunschAffine}
import org.scalatest._

class ConvexDPTest extends FlatSpec with Matchers {
/*
  "ConvexDPTest" should "correcly align two sequences" in {
    val seqA = "AAA"
    val seqB = "A"

    val nmw = new ConvexDP(seqA,seqB,1,-1,10.0)

    //nmw.alignment().getScore should be (0.0)
    nmw.alignment().getAlignmentString._1 should be ("AAA")
    nmw.alignment().getAlignmentString._2 should be ("--A")
  }

  "ConvexDPTest" should "correcly align two longer sequences" in {
    val seqA = "AAAAAAAAA"
    val seqB = "A"

    val nmw = new ConvexDP(seqA,seqB,1,-1,10.0)

    //nmw.alignment().getScore should be (0.0)
    nmw.alignment().getAlignmentString._1 should be ("AAAAAAAAA")
    nmw.alignment().getAlignmentString._2 should be ("--------A")
  }
*/
  "ConvexDPTest" should "correcly align two anchored longer sequences" in {
    val seqA = "TAAAAT"
    val seqB = "TT"

    val nmw = new ConvexDP(seqA,seqB,1,-1,10.0)

    println("Match")
    nmw.matrix.printMatrix()
    println()
    nmw.trace.printMatrix()
    println("gapA")
    nmw.matrix.printMatrix()
    println()
    nmw.trace.printMatrix()
    println("gapB")
    nmw.matrix.printMatrix()
    println()
    nmw.trace.printMatrix()

    nmw.alignment().getAlignmentString._1 should be ("TAAAAT")
    nmw.alignment().getAlignmentString._2 should be ("TA---T")
  }

}