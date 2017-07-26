import aligner.AlignmentManager
import org.scalatest.{FlatSpec, Matchers}
import reads.{Deletion, Match}
import utils.CutSites
import umi.BitEncoding
/**
  * Created by aaronmck on 2/26/17.
  */
class BitEncoderTest extends FlatSpec with Matchers {
  "BitEncoder" should "find a basic distance correctly" in {
    val bencoder = new BitEncoding(24)
    //                                         XXXXXXXX X XXXXX X X
    val encode1 = bencoder.bitEncodeString("AAATTTTCTGGATAAGGAGGGATG")
    val encode2 = bencoder.bitEncodeString("AAACAGCTACAACATATGCGCACG")

    bencoder.mismatches(encode1,encode2) should be(16)
  }

  // TACTATTGTCTAGACACTGTCCGA

  "BitEncoder" should "encode and decode correctly" in {
    val bencoder = new BitEncoding(24)
    val known = "TACTATTGTCTAGACACTGTCCGA"
    val encode1 = bencoder.bitEncodeString(known)
    val decode = bencoder.bitDecodeString(encode1).str

    (known) should be(decode)
  }
}