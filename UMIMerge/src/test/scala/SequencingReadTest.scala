import main.scala.utils.Utils
import org.scalatest.junit.JUnitSuite

import scala.main.{ForwardReadOrientation, SequencingRead}
import org.scalatest._

/**
 * Created by aaronmck on 10/22/15.
 */
class SequencingReadTest extends FlatSpec with Matchers {
  val readName = "TestRead1"

  "A SequencingRead" should "find its non-dash length correctly" in {
    val readSeq = "AAAAAAAA-------"
    val readQual = "HHHHHHHHHHHHHHH"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT")
    testRead.trueEnd() should be (7)
  }

  "A SequencingRead" should "not report a smaller subset when there isnt one" in {
    val readSeq = "AAAAAAAAAAAAAAA"
    val readQual = "HHHHHHHHHHHHHHH"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT")
    testRead.trueEnd() should be (readSeq.length -1)
  }

  "A SequencingRead" should "report a full dash read appropriately as zero" in {
    val readSeq =  "---------------"
    val readQual = "HHHHHHHHHHHHHHH"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT")
    testRead.trueEnd() should be (-1)
  }

  "A SequencingRead" should "slice down a read to the right size" in {
    val readSeq =  "---------------"
    val readQual = "HHHHHHHHHHHHHHH"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT").slice(0,5)
    testRead.length should be (5)
  }

  "A SequencingRead" should "should quality threshold itself down when qual drops off in a one base window" in {
    val readSeq =  "AAAAAAAAAAAAAAA"
    val readQual = "HHHHHHHHH$$$$$$"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT").qualityThresholdRead(1,10)
    testRead.bases should be ("AAAAAAAAA")
  }

  "A SequencingRead" should "should quality threshold itself down when qual drops off in a two base window" in {
    val readSeq =  "123456789123456"
    val readQual = "HHHHHHHHH$$$$$$"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT").qualityThresholdRead(2,10)
    testRead.bases should be ("123456789")
  }

  "A SequencingRead" should "should trim some real garbage" in {
    // ...
    val readSeq =  "CAATTTCCCTACTCAGATCTCGACCTCGAGACAGGTTTGGAGCGAGATTGATAAAGTGCCGACGGTCACGCTTGCGATCTCGTATGCCGTCTTCTGCTTGTAAAAAAAAACTATCTCTCCCTCTACTTATTATTCTTTATATTTTATCTCCCCCTTCACTTCACCAACATTTATCTACTAACTTAACTCAAAAGCTTTTTAATATATATTCTTAAACAAACATTATCTATTCATCTCACAATTCAACACAAGTTTA"
    val readQual = "CCCCCFFFFGGGGGGGFFFGCG,CCGFG7@FDGGGEGGGGGGGGGFGDFE<FFFFFGFFGGGGGGGGEGGGGFEFCCF<FGGGGCEF9CCFGG?FCFG9E,,B,A,@+=>+,,,,,8,,,,,,,:,,,,<,<,A,,<8,,,8,8,,,,,,,3+++,8,,,,3,,,>>++,,33,,,,,:,,,,8>,,,,,37,,,,,,,,,2,72;;2,,,,,,,,522,,/,2=,,,22,,+,+,,,25*0+4,,++2*1*++++"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT").qualityThresholdRead(5,20)
    // println("test read = " + testRead.bases)
    testRead.bases should be ("CAATTTCCCTACTCAGATCTCGACCTCGAGACAGGTTTGGAGCGAGATTGATAAAGTGCCGACGGTCACGCTTGCGATCTCGTATGCCGTCTTCTGCTTGTAAAAAA")
  }

  "A SequencingRead" should "find the reverse primer correctly in a read" in {
    val readSeq =  "TGTCTCGAGGTCGAGAATTCGGATCCTCCCCCTCGGGCCGCAGCGCCGTTCCATCACCTTCAGCCCCTGTGCCAATCCCCCACTTCCAGCCTCCGTGCCTTACCGCCTCCTCCGGCCACAGAGCCATACCTCCACTCCCAGCTGCAGTGCTTATCCACCACCTGCAGCCACAGTGCCTACCGCCCCCCCCAGCCCCCGTGCCATTCCACCACCCCTAACCCCCCTCCTAAACCTCCCCCCCCACTCCCCGAGCCCC"
    val readQual = "CCCCCGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGFGFGGGGGGGGGGGFGGGGGGFGGGGGGGGGGEGGFGGGGGGFGGGGGGGFEFGGFFGDFEDFFDEFFFDC5ECF7CC*8;EEC=DG5@FG5@>5E5CCEEFEEBB7925=EEECC2CFEGC(688?2<-66.97<6:(3(((4::42>0(-,345>?,8<:(49261(.2((-((((-(("
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT").qualityThresholdRead(3,10)
    testRead.startsWithPrimer(Utils.reverseComplement("GAATTCTCGACCTCGAGACA")) should be (true)
  }

  "A SequencingRead" should "should avoid single bad bases when in a 3-window mode" in {
    val readSeq =  "123456789123456"
    val readQual = "HH$HH$HHH$$$$$$"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT").qualityThresholdRead(3,10)
    testRead.bases should be ("123456789")
  }

  "A SequencingRead" should "should catch double bad bases when in a 3-window mode" in {
    val readSeq =  "123456789123456"
    val readQual = "HH$HH$H$H$$$$$$"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT").qualityThresholdRead(3,18)
    testRead.bases should be ("12345")
  }

  "A SequencingRead" should "should calculate a basic zero distance correctly" in {
    val readSeq =  "AATTAATTAATTAAT"
    val readQual = "HH$HH$H$H$$$$$$"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT")
    val testRead2 = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT")

    testRead.distance(testRead2) should be (0)
  }

  "A SequencingRead" should "should calculate a half-ish distance correctly" in {
    val readSeq =   "AAAAAAAATTTTTTTT"
    val readSeq2 =  "TTTTTTTTTTTTTTTT"
    val readQual =  "HH$HH$H$H$$$$$$H"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT")
    val testRead2 = SequencingRead(readName, readSeq2, readQual, ForwardReadOrientation, "TTTT")

    testRead.distance(testRead2) should be (.5)
  }

  "A SequencingRead" should "should calculate a double read length distance correctly" in {
    val readSeq =   "TTTTTTTTTTTTTTT"
    val readSeq2 =  "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTT"
    val readQual =  "HH$HH$H$H$$$$$$"
    val readQual2 =  "HH$HH$H$H$$$$$$HH$HH$H$H$$$$$$"
    val testRead = SequencingRead(readName, readSeq, readQual, ForwardReadOrientation, "TTTT")
    val testRead2 = SequencingRead(readName, readSeq2, readQual2, ForwardReadOrientation, "TTTT")

    testRead.distance(testRead2) should be (.5)
  }
}
