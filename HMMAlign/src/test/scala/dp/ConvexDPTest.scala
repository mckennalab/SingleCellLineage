package test.scala.dp

import main.scala.dp.{ConvexDP, NeedlemanWunschAffine}
import org.scalatest._

class ConvexDPTest extends FlatSpec with Matchers {

  "ConvexDPTest" should "correctly align two sequences" in {
    val seqA = "AAA"
    val seqB = "A"

    val nmw = new ConvexDP(seqA,seqB,1,-1,10.0,1.0)

    //nmw.alignment().getScore should be (0.0)
    nmw.alignment().getAlignmentString._1 should be ("AAA")
    nmw.alignment().getAlignmentString._2 should be ("A--")
  }

    "ConvexDPTest" should "correctly align two longer sequences" in {
      val seqA = "AAAAAAAAA"
      val seqB = "A"

      val nmw = new ConvexDP(seqA,seqB,1,-1,10.0,1.0)

      //nmw.alignment().getScore should be (0.0)
      nmw.alignment().getAlignmentString._1 should be ("AAAAAAAAA")
      nmw.alignment().getAlignmentString._2 should be ("A--------")
    }


    "ConvexDPTest" should "match a longer complete match correctly" in {
      val seqA = "ACTATGGAGTCGTCAGCAGTACTACTGAC"
      val seqB = "ACTATGGAGTCGTCAGCAGTACTACTGAC"


      val nmw = new ConvexDP(seqA,seqB,3,-2,10.0,1.0)
      //nmw.matrix.printMatrix()
      //nmw.trace.printMatrix()
      val align = nmw.alignment

      println(align.getAlignmentString._1)
      println(align.getAlignmentString._2)
      align.getAlignmentString._1 should be ("ACTATGGAGTCGTCAGCAGTACTACTGAC")
      align.getAlignmentString._2 should be ("ACTATGGAGTCGTCAGCAGTACTACTGAC")

    }

  "ConvexDPTest" should "find a small gap correctly" in {
    val seqA = "GTCGT--GCAGT".filter{st => st != '-'}.mkString("")
    val seqB = "GTCGTCAGCAGT"


    val nmw = new ConvexDP(seqA,seqB,3,-2,10.0,1.0)
    //nmw.matrix.printMatrix()
    //nmw.trace.printMatrix()
    val align = nmw.alignment

    println(align.getAlignmentString._1)
    println(align.getAlignmentString._2)
    align.getAlignmentString._1 should be ("GTCGT--GCAGT")
    align.getAlignmentString._2 should be ("GTCGTCAGCAGT")

  }


  "ConvexDPTest" should "find a two small gaps correctly" in {
    val seqA = "ACTATG--GAGTCGT--GCAGT".filter{st => st != '-'}.mkString("")
    val seqB = "ACTATGTTGAGTCGTCAGCAGT"


    val nmw = new ConvexDP(seqA,seqB,3,-2,10.0,1.0)
    //nmw.matrix.printMatrix()
    //nmw.trace.printMatrix()
    val align = nmw.alignment

    println(align.getAlignmentString._1)
    println(align.getAlignmentString._2)
    align.getAlignmentString._1 should be ("ACTATG--GAGTCGT--GCAGT")
    align.getAlignmentString._2 should be ("ACTATGTTGAGTCGTCAGCAGT")

  }

  "ConvexDPTest" should "align without forcing a match at the end" in {
    val seqA = "ACTA------------------".filter{st => st != '-'}.mkString("")
    val seqB = "ACTATGTTGAGTCGTCAGCAGT"


    val nmw = new ConvexDP(seqA,seqB,3,-2,10.0,1.0)
    //nmw.matrix.printMatrix()
    //nmw.trace.printMatrix()
    val align = nmw.alignment

    println(align.getAlignmentString._1)
    println(align.getAlignmentString._2)
    align.getAlignmentString._1 should be ("ACTA------------------")
    align.getAlignmentString._2 should be ("ACTATGTTGAGTCGTCAGCAGT")
  }

  "ConvexDPTest" should "find a large gap correctly" in {
    val seqA = "AC------------------GT".filter{st => st != '-'}.mkString("")
    val seqB = "ACTATGTTGAGTCGTCAGCAGT"


    val nmw = new ConvexDP(seqA,seqB,3,-2,10.0,1.0)
    //nmw.matrix.printMatrix()
    //nmw.trace.printMatrix()
    val align = nmw.alignment

    println(align.getAlignmentString._1)
    println(align.getAlignmentString._2)
    align.getAlignmentString._1 should be ("AC------------------GT")
    align.getAlignmentString._2 should be ("ACTATGTTGAGTCGTCAGCAGT")
  }


  "ConvexDPTest" should "not stick bases on the end " in {
    val seqA = "TGTCATGGAGTCGACTGCACGACAGTAA".filter{st => st != '-'}.mkString("")
    val seqB = "TGTCATGGAGTCGACTGCACGACAGTCGACTATGGAGTCGCGAGCGCTATGAGCGACTATGGGAATTCTCGACCTCGAGACAAATGGCAGCCCGG"


    val nmw = new ConvexDP(seqA,seqB,3,-2,10.0,1.0)
    //nmw.matrix.printMatrix()
    //nmw.trace.printMatrix()
    val align = nmw.alignment

    println(align.getAlignmentString._1)
    println(align.getAlignmentString._2)
    align.getAlignmentString._1 should be ("TGTCATGGAGTCGACTGCACGACAGTAA-------------------------------------------------------------------")
    align.getAlignmentString._2 should be ("TGTCATGGAGTCGACTGCACGACAGTCGACTATGGAGTCGCGAGCGCTATGAGCGACTATGGGAATTCTCGACCTCGAGACAAATGGCAGCCCGG")
  }

  "ConvexDPTest" should "correctly score the gap cost" in {
    ConvexDP.scoreDistance(1,10,1) should be (10)
    ConvexDP.scoreDistance(2,10,1) should be (10.69315 +- 0.0001)
  }
/*

  "ConvexDPTest" should "correctly align two anchored longer sequences" in {
    //val sA = "-------------------------------------------------------------------------------------------------------------GAGCTCAAGCTTCGGACAGCAGTATCATGGA----------------------------------------------------GTCTATGGAGTCGACAGCAGTGTGTGAGTCGAGAGCATAGACA---------------------------------TCGATGGGTATGGAGTCGACAGAGATATCATG--GTATGGAGTCGACAGCAGTATCTGCTGTCATGGAGTCGACTGCACGAC------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------CTAGATCATTACCCTGTTATCCCTATACGAAATTAAGCGTACGAATGGCCCATCTGGCCTGTGTTTCAGACACCAGGGAGTCTCTGCTCACGTTTC"
    val seqA = "-------------------------------------------------------------------------------------------------------------GAGCTCAAGCTTCGGACAGCAGTATCATCGAGAAAGCTAGCGCCCTCGCCCTCGATCTATGGAGTCGAGAGCGCGCTCGTCGACTATGGAGTCGTCAGCAGTACTACTGACGATGGAGTCGACAGCAGTGTGT---------GAGTCGAGAGCATAGACA------------TCGACTACAGTCGCTACGACTATGGAGTCGACAGAGATATCATGCAGTATGGAGTCGACAGCAGTATCTGCTGTCATGGAGTCGACTGCACGACAGTAA-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------".filter{st => st != '-'}.mkString("")
    val seqB = "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTGATACGGCGACCACCGAGATCTACACGGCATTCCTGCTGAACCGCTCTTCCGATCTNNNNNNNNNNNNNNNNNNNTCGAGCTCAAGCTTCGGACAGCAGTATCATCGACTATGGAGTCGAGAGCGCGCTCGTCGACTATGGAGTCGTCAGCAGTACTACTGACGATGGAGTCGACAGCAGTGTGTGAGTCTATGGAGTCGAGAGCATAGACATCGAGTATGGAGTCGACTACAGTCGCTACGACTATGGAGTCGACAGAGATATCATGCAGTATGGAGTCGACAGCAGTATCTGCTGTCATGGAGTCGACTGCACGACAGTCGACTATGGAGTC--------GCGAGCGCTATGAGCGACTATGGGAATTCTCGACCTCGAG---ACAAATGGCAGCCC----GG---GGGATCCAAACTCGAGAAA------GCTAGCAAACCATGGTACGATGATGATCCAGACATGATAAGATACATTGATGAGTTTGGACAAACCACAACTAGAATGCAGTGAAAAAAATGCTTTATTTGTGAAATTTGTGATGCTATTGCTTTATTTGTAACCATTATAAGCTGCAATAAACAAGTTAACAACAACAATTGCATTCATTTTATGTTTCAGGTTCAGGGGGAGGTGTGGGAGGTTTTTTAAAGCAAGTAAAACCTCTACAAATGTGGTATGGCTGATTATGATCCTCTAGATCAGATCTCTTGTTTATTGCAGCTTATAATGGTTACAAATAAAGCAATAGCATCACAAATTTCACAAATAAAGCATTTTTTTCACTGCATTCTAGTTGTGGTTTGTCCAAACTCATCAATGTATCTTATCATGTCTGGATCTACGTAATACGACTCACTATAGTTCTAGAGGCTCGAGAGGGGCCGCTTTACTTGTACAGCTCGTCCATGCCGAGAGTGATCCCGGCGGCGGTCACGAACTCCAGCAGGACCATGTGATCGCGCTTCTCGTTGGGGTCTTTGCTCAGGGCGGACTGGGTGCTCAGGTAGTGGTTGTCGGGCAGCAGCACGGGGCCGTCGCCGATGGGGGTGTTCTGCTGGTAGTGGTCGGCGAGCTGCACGCTGCCGTCCTCGATGTTGTGGCGGATCTTGAAGTTCACCTTGATGCCGTTCTTCTGCTTGTCGGCCATGATATAGACGTTGTGGCTGTTGTAGTTGTACTCCAGCTTGTGCCCCAGGATGTTGCCGTCCTCCTTGAAGTCGATGCCCTTCAGCTCGATGCGGTTCACCAGGGTGTCGCCCTCGAACTTCACCTCGGCGCGGGTCTTGTAGTTGCCGTCGTCCTTGAAGAAGATGGTGCGCTCCTGGACGTAGCCTTCGGGCATGGCGGACTTGAAGAAGTCGTGCTGCTTCATGTGGTCGGGGTAGCGGCTGAAGCACTGCACGCCGTAGGTCAGGGTGGTCACGAGGGTGGGCCAGGGCACGGGCAGCTTGCCGGTGGTGCAGATGAACTTCAGGGTCAGCTTGCCGTAGGTGGCATCGCCCTCGCCCTCGCCGGACACGCTGAACTTGTGGCCGTTTACGTCGCCGTCCAGCTCGACCAGGATGGGCACCACCCCGGTGAACAGCTCCTCGCCCTTGCTCACCATGGTCACTGTCTGCTTTGCTGTTGGTCTGGGCTCCTGGGTCACTGGCTTACTAATGGAGTCTTTATGTATGAGGACTCTTATCAATTGTTCTTCTATAAAGGTCTGCAGTGTTTCTGTTCGTCCCCTACATGGACACCCAGAGCCTCCTAAATACAGGAGCCCTGATAACTGCACAAGTGCTCAGATTCCAGCAGGGTGGAAAATGAGATAAAGTGTGCAGATGGGGAGGGGGACGTGAATGAGAGATTTGAGGGATGAAAAGGATGGATGAACGCATTGAAAATAGCCCCTTTCACAAAATAATACCAGTAAATTGTCATATAATTAATGACCAGACCTTTACCTGTAAATGATGTGATGTTCATATACAGAAGAATGCCAACAATTTACAGATGATTTTACAAATTCTTAATTTTCCTTTTCTAAATGGATTTGTTCACACATTATCTCTTCAAAGCAATTGAATTTTCTGGAAAAGATTGGCTGTGTGTGTAAAAGGTGCTACTGAGGAATTTAATGTGACATGGAATGAAGCAAACAGCAGCCTAAGACAGGATGGCAGGAAAAATGTCACATGTTTAAAATAAGAGTGAAACCAACGACCTGGACTTATAGAGACTGTGTGTCCTTAGGCAAAGCTCTTGTACAAACAGTGCTTGTGACTGTGAAATTCGGGGTTTGCCTGGATTGTGTTAAATGTGTGCTTGGATGTATCATAAAAAAGAATCGGTTCTCTGTATTTGACATTTTCTGTAATTTATCTAAAGTTACAACTGGCTATGCCTGATTTTTATCACTATTGAATCAATCATTTTAAAGAATGTTAAGTCTGCTGATTTGGAGAGCTCATTTACATTTATTTTGAATGTCTCTTATTTAACACAACTGATTTAAGCTTTAGATCTAATCTGATCATTCAGGCGCGCCAACACGTGAATACCACGCGAGGCCTTAGGGATAACAGGGTAATACGCGTGATCTGCGAAGATACGGCCACGGGTGCTCTTGATCCTGTGGCTGATTTTGGACTGTGCTGCTCGCAGCTGCTGATGAATCACATACTTCCTCCATTTTCTTCCACTGATTGACTGTTATAATTTCCCTAATTTCCAGGTCAAGGTGCTGTGCATTGTGGTAATAGATGTGACATGACGTCACTTCCAAAGGACCAATGAACATGTCTGACCAATTTCATATAATGTGAAAACGATTTTCATAGGCAGAATAAATAACATTTAAATTAAACTGGGCATCAGCGCAATTCAATTGGTTTGGTAATAGCAAGGGAAAATAGAATGAAGTGATCTCCAAAAAATAAGTACTTTTTGACTGTAAATAAAATTGTAAGGAGTAAAAAGTACTTTTTTTTCTAAAAAAATGTAATTAAGTAAAAGTAAAAGTATTGATTTTTAATTGTACTCAAGTAAAGTAAAAATCCCCAAAAATAATACTTAAGTACAGTAATCAAGTAAAATTACTCAAGTACTTTACACCTCTGGTTCTTGACCCCCTACCTTCAGCAAGCCCAGCAGATCCACTAGTTCTAGAG".filter{st => st != '-'}.mkString("")

    val nmw = new ConvexDP(seqA,seqB,3,-2,60.0,1.0)
    val align = nmw.alignment

    println(align.getAlignmentString._1)
    println(align.getAlignmentString._2)
    align.getAlignmentString._1 should be ("TAAAAT")
    align.getAlignmentString._2 should be ("T----T")
  }


  "ConvexDPTest" should "correctly align two anchored longer sequences" in {
    //val sA = "-------------------------------------------------------------------------------------------------------------GAGCTCAAGCTTCGGACAGCAGTATCATGGA----------------------------------------------------GTCTATGGAGTCGACAGCAGTGTGTGAGTCGAGAGCATAGACA---------------------------------TCGATGGGTATGGAGTCGACAGAGATATCATG--GTATGGAGTCGACAGCAGTATCTGCTGTCATGGAGTCGACTGCACGAC------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------CTAGATCATTACCCTGTTATCCCTATACGAAATTAAGCGTACGAATGGCCCATCTGGCCTGTGTTTCAGACACCAGGGAGTCTCTGCTCACGTTTC"
    val seqA = "-------------------------------------------------------------------------------------------------------------GAGCTCAAGCTTCGGACAGCAGTATC-------ATGGAGT---------------------------------------------CTATGGAGTCGACAGCAGTGTGT---------GAGTCGAGAGCATAGACATCG---ATGG---------------------GTATGGAGTCGACAGAGATATCATG--GTATGGAGTCGACAGCAGTATCTGCTGTCATGGAGTCGACTGCACGAC------CTA--GA-TCATTACCCTGTTATCCCTAT-----AC----GAAATT---------AAGCGTACGAATG---GCCCATCTGGCCTGTGTTTCAGACACCAGGGAGTCTCTGCT-------CACGTTTC-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------".filter{st => st != '-'}.mkString("")
    val seqB = "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTGATACGGCGACCACCGAGATCTACACGGCATTCCTGCTGAACCGCTCTTCCGATCTNNNNNNNNNNNNNNNNNNNTCGAGCTCAAGCTTCGGACAGCAGTATCATCGACTATGGAGTCGAGAGCGCGCTCGTCGACTATGGAGTCGTCAGCAGTACTACTGACGATGGAGTCGACAGCAGTGTGTGAGTCTATGGAGTCGAGAGCATAGACATCGAGTATGGAGTCGACTACAGTCGCTACGACTATGGAGTCGACAGAGATATCATGCAGTATGGAGTCGACAGCAGTATCTGCTGTCATGGAGTCGACTGCACGACAGTCGACTATGGAGTC--------GCGAGCGCTATGAGCGACTATGGGAATTCTCGACCTCGAG---ACAAATGGCAGCCC----GG---GGGATCCAAACTCGAGAAA------GCTAGCAAACCATGGTACGATGATGATCCAGACATGATAAGATACATTGATGAGTTTGGACAAACCACAACTAGAATGCAGTGAAAAAAATGCTTTATTTGTGAAATTTGTGATGCTATTGCTTTATTTGTAACCATTATAAGCTGCAATAAACAAGTTAACAACAACAATTGCATTCATTTTATGTTTCAGGTTCAGGGGGAGGTGTGGGAGGTTTTTTAAAGCAAGTAAAACCTCTACAAATGTGGTATGGCTGATTATGATCCTCTAGATCAGATCTCTTGTTTATTGCAGCTTATAATGGTTACAAATAAAGCAATAGCATCACAAATTTCACAAATAAAGCATTTTTTTCACTGCATTCTAGTTGTGGTTTGTCCAAACTCATCAATGTATCTTATCATGTCTGGATCTACGTAATACGACTCACTATAGTTCTAGAGGCTCGAGAGGGGCCGCTTTACTTGTACAGCTCGTCCATGCCGAGAGTGATCCCGGCGGCGGTCACGAACTCCAGCAGGACCATGTGATCGCGCTTCTCGTTGGGGTCTTTGCTCAGGGCGGACTGGGTGCTCAGGTAGTGGTTGTCGGGCAGCAGCACGGGGCCGTCGCCGATGGGGGTGTTCTGCTGGTAGTGGTCGGCGAGCTGCACGCTGCCGTCCTCGATGTTGTGGCGGATCTTGAAGTTCACCTTGATGCCGTTCTTCTGCTTGTCGGCCATGATATAGACGTTGTGGCTGTTGTAGTTGTACTCCAGCTTGTGCCCCAGGATGTTGCCGTCCTCCTTGAAGTCGATGCCCTTCAGCTCGATGCGGTTCACCAGGGTGTCGCCCTCGAACTTCACCTCGGCGCGGGTCTTGTAGTTGCCGTCGTCCTTGAAGAAGATGGTGCGCTCCTGGACGTAGCCTTCGGGCATGGCGGACTTGAAGAAGTCGTGCTGCTTCATGTGGTCGGGGTAGCGGCTGAAGCACTGCACGCCGTAGGTCAGGGTGGTCACGAGGGTGGGCCAGGGCACGGGCAGCTTGCCGGTGGTGCAGATGAACTTCAGGGTCAGCTTGCCGTAGGTGGCATCGCCCTCGCCCTCGCCGGACACGCTGAACTTGTGGCCGTTTACGTCGCCGTCCAGCTCGACCAGGATGGGCACCACCCCGGTGAACAGCTCCTCGCCCTTGCTCACCATGGTCACTGTCTGCTTTGCTGTTGGTCTGGGCTCCTGGGTCACTGGCTTACTAATGGAGTCTTTATGTATGAGGACTCTTATCAATTGTTCTTCTATAAAGGTCTGCAGTGTTTCTGTTCGTCCCCTACATGGACACCCAGAGCCTCCTAAATACAGGAGCCCTGATAACTGCACAAGTGCTCAGATTCCAGCAGGGTGGAAAATGAGATAAAGTGTGCAGATGGGGAGGGGGACGTGAATGAGAGATTTGAGGGATGAAAAGGATGGATGAACGCATTGAAAATAGCCCCTTTCACAAAATAATACCAGTAAATTGTCATATAATTAATGACCAGACCTTTACCTGTAAATGATGTGATGTTCATATACAGAAGAATGCCAACAATTTACAGATGATTTTACAAATTCTTAATTTTCCTTTTCTAAATGGATTTGTTCACACATTATCTCTTCAAAGCAATTGAATTTTCTGGAAAAGATTGGCTGTGTGTGTAAAAGGTGCTACTGAGGAATTTAATGTGACATGGAATGAAGCAAACAGCAGCCTAAGACAGGATGGCAGGAAAAATGTCACATGTTTAAAATAAGAGTGAAACCAACGACCTGGACTTATAGAGACTGTGTGTCCTTAGGCAAAGCTCTTGTACAAACAGTGCTTGTGACTGTGAAATTCGGGGTTTGCCTGGATTGTGTTAAATGTGTGCTTGGATGTATCATAAAAAAGAATCGGTTCTCTGTATTTGACATTTTCTGTAATTTATCTAAAGTTACAACTGGCTATGCCTGATTTTTATCACTATTGAATCAATCATTTTAAAGAATGTTAAGTCTGCTGATTTGGAGAGCTCATTTACATTTATTTTGAATGTCTCTTATTTAACACAACTGATTTAAGCTTTAGATCTAATCTGATCATTCAGGCGCGCCAACACGTGAATACCACGCGAGGCCTTAGGGATAACAGGGTAATACGCGTGATCTGCGAAGATACGGCCACGGGTGCTCTTGATCCTGTGGCTGATTTTGGACTGTGCTGCTCGCAGCTGCTGATGAATCACATACTTCCTCCATTTTCTTCCACTGATTGACTGTTATAATTTCCCTAATTTCCAGGTCAAGGTGCTGTGCATTGTGGTAATAGATGTGACATGACGTCACTTCCAAAGGACCAATGAACATGTCTGACCAATTTCATATAATGTGAAAACGATTTTCATAGGCAGAATAAATAACATTTAAATTAAACTGGGCATCAGCGCAATTCAATTGGTTTGGTAATAGCAAGGGAAAATAGAATGAAGTGATCTCCAAAAAATAAGTACTTTTTGACTGTAAATAAAATTGTAAGGAGTAAAAAGTACTTTTTTTTCTAAAAAAATGTAATTAAGTAAAAGTAAAAGTATTGATTTTTAATTGTACTCAAGTAAAGTAAAAATCCCCAAAAATAATACTTAAGTACAGTAATCAAGTAAAATTACTCAAGTACTTTACACCTCTGGTTCTTGACCCCCTACCTTCAGCAAGCCCAGCAGATCCACTAGTTCTAGAG".filter{st => st != '-'}.mkString("")

    val nmw = new ConvexDP(seqA,seqB,3,-2,10.0,1.0)
    val align = nmw.alignment

    println(align.getAlignmentString._1)
    println(align.getAlignmentString._2)
    align.getAlignmentString._1 should be ("TAAAAT")
    align.getAlignmentString._2 should be ("T----T")
  }*/

}