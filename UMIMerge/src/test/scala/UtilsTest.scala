import aligner.{Aligner, Waterman}
import reads.SequencingRead
import utils.Utils
import utils._
import org.scalatest.junit.JUnitSuite

import org.scalatest._
/**
  * Created by aaronmck on 12/6/16.
  */
class UtilsTest extends FlatSpec with Matchers {
  "containsFWDPrimerByAlignment" should "find a full match primer" in {
    val read =     "ATCGGCGTAATATA"
    val primer =  "ATCGGCG"

    Utils.containsFWDPrimerByAlignment(read,primer,0) should be (true)
  }

  "containsBothPrimerByAlignment" should "find primers on both ends of a read" in {
    val read1 =     "ATCGGCGTATTTTTTATAGGGGGGTA"
    val read2 =     "TTTTATAGGGGGGTAATATA"
    val primer1 =  "ATCGGCG"
    val primer2 =  "AATATA"

    Utils.containsBothPrimerByAlignment(read1,read2,primer1,primer2,0) should be ((true,true))
  }

  "containsBothPrimerByAlignment" should "find primers on two ends of a read" in {
    val read =     "ATCGGCGGGGGGGGGGGGTAATATAGGG"
    val primer1 =  "ATCGGCG"
    val primer2 =  "AATATAGGG"

    Utils.containsBothPrimerByAlignment(read,primer1,primer2,0) should be ((true,true))
  }

  // containsFWDPrimerByAlignment(read: String, primer: String, allowedMismatches: Int)
  "containsFWDPrimerByAlignment" should "find primers on two ends of a read" in {
    val read =     "--------------------------------------------------------------------------------TTGGTAGTCGTGTCTCGAGGTCGAGAATTCGTTCAATCACCACCCCCGGCCGCAGGGCCATTAGCTTCCTTCTCCAGCCGCTGTGCTAATTCCCACCACCTCCTGCCGTTGTGTCAAAGTCCTCCATCTGCAGCCGCAGTGA-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------"
    val primer =  "TTGGTAGTCG"

    Utils.containsFWDPrimerByAlignment(read,primer,0) should be (true)
  }
}
