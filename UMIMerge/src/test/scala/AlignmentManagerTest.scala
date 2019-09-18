import java.io.File

import aligner._
import org.scalatest.{FlatSpec, Matchers}
import reads._
import utils.{CutSites, IndividualCutSite}

import scala.collection.mutable

/**
 * Created by aaronmck on 11/18/15.
 */
class AlignmentManagerTest extends FlatSpec with Matchers {
  val readName = "TestRead1"
  val debug = false

  "Alignment manager" should "find basic deletion correctly" in {
    val ref =     "AAATAAAAT"
    val readFwd = "AAAA-AAAA"
    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls = AlignmentManager.callEdits(ref,readFwd,1,cutSites,false,true)
    testCalls._1.length should be (3)
    testCalls._1(0).cigarCharacter should be (Match)
    testCalls._1(0).readBase should be ("AAAA")
    testCalls._1(0).refBase should be ("AAAT")
    testCalls._1(0).refBase should be ("AAAT")

    testCalls._1(1).cigarCharacter should be (Deletion)
    testCalls._1(1).readBase should be ("-")
    testCalls._1(1).refBase should be ("A")

    testCalls._1(2).cigarCharacter should be (Match)
    testCalls._1(2).readBase should be ("AAAA")
    testCalls._1(2).refBase should be ("AAAT")
  }

  "Alignment manager" should "find multibase deletion correctly" in {
    val ref =     "AAATAAAAA"
    val readFwd = "AAAA---AA"
    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls = AlignmentManager.callEdits(ref,readFwd,1,cutSites,false,true)
    testCalls._1.length should be (3)
    testCalls._1(0).cigarCharacter should be (Match)
    testCalls._1(0).readBase should be ("AAAA")
    testCalls._1(0).refBase should be ("AAAT")

    testCalls._1(1).cigarCharacter should be (Deletion)
    testCalls._1(1).readBase should be ("---")
    testCalls._1(1).refBase should be ("AAA")

    testCalls._1(2).cigarCharacter should be (Match)
    testCalls._1(2).readBase should be ("AA")
    testCalls._1(2).refBase should be ("AA")
  }

  "Alignment manager" should "find multi-deletions correctly" in {
    val ref =     "AAAAAAAAA"
    val readFwd = "--AA---AA"
    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls = AlignmentManager.callEdits(ref,readFwd,1,cutSites,false,true)
    testCalls._1.length should be (3)

    testCalls._1(0).cigarCharacter should be (Match)
    testCalls._1(0).readBase should be ("AA")
    testCalls._1(0).refBase should be ("AA")
    testCalls._1(0).refPos should be (2)

    testCalls._1(1).cigarCharacter should be (Deletion)
    testCalls._1(1).readBase should be ("---")
    testCalls._1(1).refBase should be ("AAA")
    testCalls._1(1).refPos should be (4)

    testCalls._1(2).cigarCharacter should be (Match)
    testCalls._1(2).readBase should be ("AA")
    testCalls._1(2).refBase should be ("AA")
    testCalls._1(2).refPos should be (7)
  }

  "Alignment manager" should "find basic insertion correctly" in {
    val ref =     "AAAT-AAAA"
    val readFwd = "AAAATAAAA"
    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls = AlignmentManager.callEdits(ref,readFwd,1,cutSites,false,true)
    testCalls._1.length should be (3)
    testCalls._1(0).cigarCharacter should be (Match)
    testCalls._1(0).readBase should be ("AAAA")
    testCalls._1(0).refBase should be ("AAAT")
    testCalls._1(0).refPos should be (0)

    testCalls._1(1).cigarCharacter should be (Insertion)
    testCalls._1(1).readBase should be ("T")
    testCalls._1(1).refBase should be ("-")
    testCalls._1(1).refPos should be (4)

    testCalls._1(2).cigarCharacter should be (Match)
    testCalls._1(2).readBase should be ("AAAA")
    testCalls._1(2).refBase should be ("AAAA")
    testCalls._1(2).refPos should be (4)
  }

  "Alignment manager" should "find offsets correctly" in {
    val ref =     "AAATAAAAA"
    val readFwd = "----TAAAA"

    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls = AlignmentManager.callEdits(ref,readFwd,1,cutSites,false,true)
    testCalls._1.length should be (1)
    testCalls._1(0).cigarCharacter should be (Match)
    testCalls._1(0).readBase should be ("TAAAA")
    testCalls._1(0).refBase should be  ("AAAAA")
    testCalls._1(0).refPos should be (4)

  }

  "Alignment manager" should "merge an event and a non-event correctly" in {
    val ref1 =     "AAAATAAAA"
    val readFwd1 = "AAAATAAAA"

    val ref2 =     "AAAATAAAA"
    val readFwd2 = "AAAAT-AAA"

    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7)))
    val testCalls1 = AlignmentManager.callEdits(ref1,readFwd1,1,cutSites,false,true)
    val testCalls2 = AlignmentManager.callEdits(ref2,readFwd2,1,cutSites,false,true)

    testCalls1._1.length should be (1)
    testCalls2._1.length should be (3)

    val combined = AlignmentManager.editsToCutSiteCalls(List[List[Alignment]](testCalls1._1,testCalls2._1),List[List[String]](testCalls1._2,testCalls2._2),cutSites)
    combined._2.size should be (1)
    combined._2(0) should be ("WT_1D+5")
  }

  "Alignment manager" should "merge an dual-event and a non-event correctly" in {
    val ref1 =     "AAAATAAAAAAATAAAAA"
    val readFwd1 = "AAAATAAAAAAATTAAAA"

    //              012345678901234567
    val ref2 =     "AAAATAAAAAAAATAAAA"
    val readFwd2 = "AAAAT-AAAAAAAT--AA"

    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7),(12,14,16)))
    val testCalls1 = AlignmentManager.callEdits(ref1,readFwd1,1,cutSites,false,true)
    val testCalls2 = AlignmentManager.callEdits(ref2,readFwd2,1,cutSites,false,true)

    testCalls1._1.length should be (1)
    testCalls2._1.length should be (5)


    val combined = AlignmentManager.editsToCutSiteCalls(List[List[Alignment]](testCalls1._1,testCalls2._1),List[List[String]](testCalls1._2,testCalls2._2),cutSites)
    combined._2.size should be (2)
    //println(combined._2.mkString(")("))
    combined._2(0) should be ("WT_1D+5")
    combined._2(1) should be ("WT_2D+14")
  }

  "Alignment manager" should "handle two reads with a shared, complex event correctly" in {
    val ref1 =     "AAATAAAAAAAATAAAAA"
    val readFwd1 = "AAAT-T-AAAAATAAAAA"

    //              012345678901234567
    val ref2 =     "AAATAAAAAAAATAAAAA"
    val readFwd2 = "AAAT-T-AAAAATAAAAA"

    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((2,5,8)))
    val testCalls1 = AlignmentManager.callEdits(ref1,readFwd1,1,cutSites,false,true)
    val testCalls2 = AlignmentManager.callEdits(ref2,readFwd2,1,cutSites,false,true)

    testCalls1._1.length should be (5)
    testCalls2._1.length should be (5)

    val combined = AlignmentManager.editsToCutSiteCalls(List[List[Alignment]](testCalls1._1,testCalls2._1),List[List[String]](testCalls1._2,testCalls2._2),cutSites)
    combined._2.size should be (1)
    combined._2(0) should be ("1D+4&1D+6")
  }

  "Alignment manager" should "merge an a collision correctly" in {
    val ref1 =     "AAAATAAAAAAAATAAAA"
    val readFwd1 = "AAAAT-AAAAAAAT--AA"

    //              012345678901234567
    val ref2 =     "AAAATAAAAAAAATAAAA"
    val readFwd2 = "AAAAT-AAAAAAAT--AA"

    val cutSites = CutSites.fromIntervals(Array[Tuple3[Int,Int,Int]]((3,5,7),(12,14,16)))
    val testCalls1 = AlignmentManager.callEdits(ref1,readFwd1,1,cutSites,false,true)
    val testCalls2 = AlignmentManager.callEdits(ref2,readFwd2,1,cutSites,false,true)

    testCalls1._1.length should be (5)
    testCalls2._1.length should be (5)

    val combined = AlignmentManager.editsToCutSiteCalls(List[List[Alignment]](testCalls1._1,testCalls2._1),List[List[String]](testCalls1._2,testCalls2._2),cutSites)
    combined._2.size should be (2)
    //println(combined._2.mkString(")("))
    combined._2(0) should be ("1D+5")
    combined._2(1) should be ("2D+14")
  }

  "Alignment manager" should "call a real scar from real data correctly" in {
    val ref1 =     "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTAATGATACGGCGACCACCGAGATCTACACNNNNNNNNCTAAATGGCTGTGAGAGAGCTCAGNNNNNNNNNNTAGTGTATGTGCAGTGAGCCCCTTTTCCTCTAACTGAAAGAAGGAAAAAAAAATGGAACCCAAAATATTCTACATAGTTTCCATGTCACAGCCAGGGCTGGGCAGTCTCCTGTTATTTCTTTTAAAATAAATATATCATTTAAATGCATAAATAAGCAAACCCTGCTCGGGAATGGGAGGGAGAGTCTCTGGAGTCCACCCCTTCTCGGCCCTGGCTCTGCAGATAGTGCTATCAAAGCCCTGACAGAGCCCTGCCCATTGCTGGGCCTTGGAGTGAGTCAGCCTAGTAGAGAGGCAGGGCAAGCCATCTCATAGCTGCTGAGTGGGAGAGAGAAAAGGGCTCATTGTCTATAAACTCAGGTCATGGCTATTCTTATTCTCACACTAAGAAAAAGAATGAGATGTCTACATATACCCTGCGTCCCCTCTTGTGTACTGGGGTCCCCAAGAGCTCTCTAAAAGTGATGGCAAAGTCATTGCGCTAGATGCCATCCCATCTATTATAAACCTGCATTTGTCTCCACACACCAGTCATGGACGGTTTGGAGCGAGATTGATAAAGTNNNNNNNNNNTGAACGCTTATCTCGTATGCCGTCTTCTGCTTGAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    val readFwd1 = "-----------------------------------------------------------------------------------------------------TAGTGTATGTGCAGTGAGCCCCTTTTCCTCTAACTGAAAGAAGGAAAAAAAAATGGAACCCAAAATATTCTACATAGTTTCC-TGTCACAGCagacGCTGGGCAGTCTCCTGTTATTTCTTTTAAAATAAATATATCATTTAAATGCATAAATAAGCAAACCCTGCTCGGGAATGGGAGGGAGAGTCTCTGGAGTCCACCCCTTCTCGGCCCTGGCTCTGCAGATAGTGCTATCAAAGCCCTGACAGAGCCCTGCCCATTGCTGGGCCTTGGAGTGAGTCA----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------"

    //println("BIG TEST TIME!!")
    val cutSites = CutSites.fromFile(new File("./test_files/MGR.fa.cutSites"),3)
    val testCalls = AlignmentManager.callEdits(ref1,readFwd1,1,cutSites,false,true)

    //println(testCalls._3(0))
    //println(testCalls._3(0))
    //println(testCalls._2(0))
    val scarsHopefully = AlignmentManager.findScar(0, testCalls._3(0), testCalls._2(0), cutSites.sites(0).cutPosition - cutSites.sites(0).startPos)
    //println(scarsHopefully.isDefined)
    scarsHopefully should not be None
    scarsHopefully.get.cigarCharacter should be (Scar)
  }

  "Alignment manager" should "process basic scars correctly" in {
    // we should find a 4 base scar from this sequence
    val ref1 =     "AAAAAAAAAAAAAAAAAA"
    val readFwd1 = "AAAATTTAAAAAAAAAAA"

    val testCalls1 = AlignmentManager.findScar(0, ref1, readFwd1, 5, 3, 10, 0.70)

    testCalls1 should not be None
    testCalls1.get.readBase should be ("ATTT")
    testCalls1.get.refBase should be ("AAAA")
    testCalls1.get.refPos should be (3)
    testCalls1.get.cigarCharacter should be (Scar)
  }

  "Alignment manager" should "process a 50/50 basic scars correctly" in {
    // we should find a 4 base scar from this sequence
    val ref1 =     "AAAAAAAAAAAAAAAAAA"
    val readFwd1 = "AAAATATAAAAAAAAAAA"

    val testCalls1 = AlignmentManager.findScar(0, ref1, readFwd1, 5, 2, 10, 0.50)

    testCalls1 should not be None
    testCalls1.get.readBase should be ("ATAT")
    testCalls1.get.refBase should be ("AAAA")
    testCalls1.get.refPos should be (3)
    testCalls1.get.cigarCharacter should be (Scar)
  }

  "Alignment manager" should "work with real data4" in {
    val ref =     "TCGTCGGCAGCGTCAGATGTGTATAAGAGACAGNNNNNNNNNNCTTCCTCCAGCTCTTCAGCTCGTCTCTCCAGCAGTTCCCCCGAGTCTGCACCTCCCCAGAAGTCCTCCAGTCCAAACGCTGCTGTCCAGTCTGGCCCGGCGACGGCTCTGTGTGCGGCGTCCAGTCAGGTCGAGGGTTCTGTCAGGACGTCCTGGTGTCCGACCTTCCCAACGGGCCGCAGTATCCTCACTCAGGAGTGGACGATCGAGAGCGATGGCCTTTAGTGTTTTACAACCAAACCTGCCAGTGCGCCGGAAACTACATGGGGTTTGATTGCGGCGAATGCAAGTTCGGCTTCTTCGGTGCCAACTGCGCAGAGAGACGCGAGTCTGTGCGCAGAAATATATTCCAGCTGTCCACTACCGAGAGGCAGAGGTTCATCTCGTACCTAAATCTGGCCAAAACCACCATAAGCCCCGATTATATGATCGTAACAGGAACGTACGCGCAGATGAACAACGGCTCCACGCCAATGTTCGCCAACATCAGTGTGTACGATTTATTCGTGTGGATGCATTATTACGTGTCCCGGGACGCTCTGCTCGGTGGGCCTGGGAATGTGTGGGCTGATAGATCGGAAGAGCACACGTCTGAACT"
    val readFwd =  "CTTCCTCCAGCTCTTCAGCTCGTCTCTCCAGCAGTTCCCCCGAGTCTGCACCACCCAAACGCTGCTGTCCAGTCTGGCCCGGCGACGGCTCCGTGTGCGGCGTCCAGTCAGGTCGAGGGTTCTGTCAGGACGTCCTGGTGTCCGACCTTCCCAACGGGCCGCAGTATCCTCACTCAGTGGACGATCGAGAGCGATGGCCTTTAGTGTTAACACC"

    val readRev = "ATCAGCCCACACATTCCCAGGCCCACCGAGCAGAGCGTCCCGGGACACGTAATAATGCATCCACATAAATCGTACACACTGATGTTGGCGAACATTGGCGTGGAGATTGTTC"

    val fRead = Aligner.SequencingReadFromNameBases("fwd",readFwd)
    val rRead = Aligner.SequencingReadFromNameBases("rev",readRev)

    val cutsSiteObj = CutSites.fromFile(new File("test_files/TYRFull.fasta.cutSites"), 3)

    rRead.reverseCompAlign = true
    //println(AlignmentManager.cutSiteEvents("testUMI", ref, fRead, rRead, cutsSiteObj, 10, true)._3.mkString("<->"))
  }

  "Alignment manager" should "recover the wild type sequence when there is no event called" in {
    val fakeReferenceBases1 = Alignment(/*val refPos: Int */ 5, /*refBase: String*/ "AAAAAAAAAA", /*readBase: String*/ "----------", /*cigarCharacter: String*/ Match)
    val fakeReferenceBases2 = Alignment(/*val refPos: Int */ 5, /*refBase: String*/ "AAAAAAAAAA", /*readBase: String*/ "AAAAAAAAAA", /*cigarCharacter: String*/ Match)

    val cutsites = new CutSites()
    cutsites.cutSites(0) = 10
    cutsites.startSites(0) = 10
    cutsites.sites :+= IndividualCutSite("TestRef", 0, 5, 10, 15, 20)

    val edits = AlignmentManager.editsToCutSiteCalls(List[List[Alignment]](List[Alignment](fakeReferenceBases1,fakeReferenceBases2)), List[List[String]](List[String]("AAAAAAAAAA")), cutsites)
    //println("-------------------->>>>>>>>")
    //println("-->>" + edits._2.mkString(",") + "<<--" + edits._3.mkString(",") + "<<--")
  }
}
