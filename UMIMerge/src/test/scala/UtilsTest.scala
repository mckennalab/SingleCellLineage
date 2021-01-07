import org.scalatest._
import utils.Utils
/**
  * Created by aaronmck on 12/6/16.
  */
class UtilsTest extends FlatSpec with Matchers {
  "containsFWDPrimerByAlignment" should "find a full match primer" in {
    val read =     "ATCGGCGTAATATA"
    val primer =  "ATCGGCG"

    Utils.containsFWDPrimerByAlignment(read,primer,0) should be (true)
  }

  "containsBothPrimerByAlignment" should "find primers on both ends of each read" in {
    val read1 =     "ATCGGCGTATTTTTTATAGGGGGGTA"
    val read2 =     "TTTTATAGGGGGGTAATATA"
    val primer1 =  "ATCGGCG"
    val primer2 =  "ATAAAA"

    Utils.containsBothPrimerByAlignment(read1,read2,primer1,primer2,0) should be ((true,true))
  }

  "containsBothPrimerByAlignment" should "find primers on both ends of each read with read data" in {
    val read1 =     "ACTCAGATCTCGAGCTCAAGCTTCCCCTCGCTGGAGCTCAGCTCTCCTCAAATGGCTCTGCAGCGAGCACCACTTGGCTCCAGCCGGGTCCGACGCTATCGCCTGAACTCTGTCTTGAACCACTGTGCTCAGGGGAGAGC"
    val read2 =     "ATCGTACCATGGCTGCCATTTGCGCCGCCCGGCAACTCATACTTACCTGGCAGGGGAGATACCATGATCAAGAAGGTGGTTCACCCAGGGCGAGGCTTGGCCATTGCACTCCGGCCACGCTGACCCCTGCGAATTCCCCAAATGTGGGA"
    val primer1 =  "ACTCAGATCTCGAGCTCAAG"
    val primer2 =  "AATGGCAGCCATGGTACGAT"

    Utils.containsBothPrimerByAlignment(read1,read2,primer1,primer2,0) should be ((true,true))
  }

  "containsBothPrimerByAlignment" should "find primers on two ends of a read" in {
    val read =     "ATCGGCGGGGGGGGGGGGTAATATAGGG"
    val primer1 =  "ATCGGCG"
    val primer2 =  "AATATAGGG"

    Utils.containsBothPrimerByAlignment(read,primer1,primer2,0) should be ((true,true))
  }

  "containsBothPrimerByAlignment" should "find primers on one of Molly's reads" in {
    val read =     "TCGAACTGAGTCCAGACACCCGGACGCCACCCGTCCCTACCGCAACCGGCCCAGTCCCACCACCCCTCTCACCGCCGGAAGCTGAACTGACTCGTCCG"
    val primer1 =  "TCGAACTGAGTCCAGACACCCGGACGCCATT"
    val primer2 =  "CCGCCGGAAGCTGAACTGACTCGTCCG"

    Utils.containsBothPrimerByAlignment(read, primer1, primer2, 2) should be ((true,true))
  }


  "containsBothPrimerByAlignment" should "find primers on one of Molly's reads with dashes" in {
    val read =     "--------------TCGAACTGAGTCCAGACACCCGGACGCCACCCGTCCCTACCGCAACCGGCCCAGTCCCACCACCCCTCTCACCGCCGGAAGCTGAACTGACTCGTCCG--------------"
    val primer1 =  "TCGAACTGAGTCCAGACACCCGGACGCCATT"
    val primer2 =  "CCGCCGGAAGCTGAACTGACTCGTCCG"

    Utils.containsBothPrimerByAlignment(read, primer1, primer2, 2) should be ((true,true))
  }

  "containsBothPrimerByAlignment" should "find primers on one of Molly's reads even with shifting errors" in {
    val read =     "TCGAACTGAGTCCAGACACCCGGACGCCACCCGTCCCTACCGCAACCGGCCCAGTCCCACCACCCCTCTCACCGCCGGAAGCTGAACTGACTCGTCCG"
    val primer1 =  "CGAACTGAGTCCAGACACCCGGACGCCACC"
    val primer2 =  "CCGCTGGAAGCTGAACTGACTCGTCCT"

    Utils.containsBothPrimerByAlignment(read, primer1, primer2, 2) should be ((true,true))
  }

  "containsBothPrimerByAlignment" should "find only one primer on one of Molly's reads, the other is wrong" in {
    val read =     "TCGAACTGAGTCCAGACACCCGGACGCCACCCGTCCCTACCGCAACCGGCCCAGTCCCACCACCCCTCTCACCGCCGGAAGCTGAACTGACTCGTCCG"
    val primer1 =  "AGAACAAAGTCCAGACACCCGGACGCCACC"
    val primer2 =  "CCGCCGGAAGCTGAACTGACTCGTCCG"

    Utils.containsBothPrimerByAlignment(read, primer1, primer2, 2) should be ((false,true))
  }

  "containsBothPrimerByAlignment" should "find no primers on one of Molly's reads" in {
    val read =     "TCGAACTGAGTCCAGACACCCGGACGCCACCCGTCCCTACCGCAACCGGCCCAGTCCCACCACCCCTCTCACCGCCGGAAGCTGAACTGACTCGTCCG"
    val primer1 =  "AGAACAAAGTCCAGACACCCGGACGCCACC"
    val primer2 =  "CCGCCGGAAGCTGAACTGACTCGAAAA"

    Utils.containsBothPrimerByAlignment(read, primer1, primer2, 2) should be ((false,false))
  }

  // containsFWDPrimerByAlignment(read: String, primer: String, allowedMismatches: Int)
  "containsFWDPrimerByAlignment" should "find primers on two ends of a read" in {
    val read =     "--------------------------------------------------------------------------------TTGGTAGTCGTGTCTCGAGGTCGAGAATTCGTTCAATCACCACCCCCGGCCGCAGGGCCATTAGCTTCCTTCTCCAGCCGCTGTGCTAATTCCCACCACCTCCTGCCGTTGTGTCAAAGTCCTCCATCTGCAGCCGCAGTGA-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------"
    val primer =  "TTGGTAGTCG"

    Utils.containsFWDPrimerByAlignment(read,primer,0) should be (true)
  }

  "filterReadBySlidingWindow" should "remove nothing in a high quality string" in {
    val read =     "AAAAATTTTTAAAAATTTTTAAAAATTTTTAAAAATTTTTAAAAATTTTT"
    val quals =    "HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH"

    (Utils.filterReadBySlidingWindow(read, quals, 20, 5)) should be (read)

  }

  "filterReadBySlidingWindow" should "remove a low qual region" in {
    val read =     "AAAAATTTTTAAAAATTTTTAAAAATTTTTAAAAATTTTTAAAAATTTTT"
    val quals =    "HHHHHHHHHHHHHHHHHHHHHHHHHLLLLZ/////HHHHHHHHHHHHHHH"

    val retRead = Utils.filterReadBySlidingWindow(read, quals, 20, 5)
    (retRead) should be ("AAAAATTTTTAAAAATTTTTAAAAATTTTT")
  }

  "filterReadBySlidingWindow" should "remove a low qual region before a lower qual region" in {
    val read =     "AAAAATTTTTAAAAATTTTTAAAAATTTTTAAAAATTTTTAAAAATTTTT"
    val quals =    "HHHHHHHHHHHHHHHHHHHHHHHHHHLLLZ/////HHHHHHHHH!!!!!!"

    val retRead = Utils.filterReadBySlidingWindow(read, quals, 20, 5)
    (retRead) should be ("AAAAATTTTTAAAAATTTTTAAAAATTTTT")
  }
}
