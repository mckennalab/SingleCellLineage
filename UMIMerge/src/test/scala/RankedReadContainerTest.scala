package test.scala

import utils._
import org.scalatest.junit.JUnitSuite

import org.scalatest._
import reads._

/**
 * test that our sorting read container actually works
 */
class RankedReadContainerTest extends FlatSpec with Matchers {
  val readName = "TestRead1"

  "A RankedReadContainer" should "sort two reads by their average length correctly" in {
    val read1 = SequencingRead.readFromNameAndSeq("test1","AAAAAA","H")
    val read1R = SequencingRead.readFromNameAndSeq("test1","AAAAAA","H")

    val read2 = SequencingRead.readFromNameAndSeq("test1","AAAAA","A")
    val read2R = SequencingRead.readFromNameAndSeq("test1","AAAAA","A")
    val rankedContainer1 = SortedReadPair(read1,read1R,true,true)
    val rankedContainer2 = SortedReadPair(read2,read2R,true,true)

    ((rankedContainer1 compare rankedContainer2) < 0) should be (true)
  }

  "A RankedReadContainer" should "handle ranking reads correctly" in {
    val read1 = SequencingRead.readFromNameAndSeq("test1","TTTTTT","H")
    val read1R = SequencingRead.readFromNameAndSeq("test1","TTTTTT","H")

    val read2 = SequencingRead.readFromNameAndSeq("test1","AAAAA","A")
    val read2R = SequencingRead.readFromNameAndSeq("test1","AAAAA","A")

    val container = new RankedReadContainer("test",1)

    val rankedContainer1 = SortedReadPair(read1,read1R,true,true)
    val rankedContainer2 = SortedReadPair(read2,read2R,true,true)

    container.addBundle(rankedContainer1)
    container.addBundle(rankedContainer2)
    container.pQ.size should be (1)

    container.addBundle(rankedContainer1)
    container.addBundle(rankedContainer1)
    container.pQ.size should be (1)

    (container.pQ.dequeue().toReadSet)(0).bases should be ("TTTTTT")
    container.pQ.size should be (0)

    val read3 = SequencingRead.readFromNameAndSeq("test1","AAAAAAAA","A")
    val read3R = SequencingRead.readFromNameAndSeq("test1","AAAAAAAA","A")
    val rankedContainer3 = SortedReadPair(read3,read3R,true,true)

    container.addBundle(rankedContainer1)
    container.addBundle(rankedContainer3)
    container.pQ.size should be (1)

    (container.pQ.dequeue().toReadSet)(0).bases should be ("AAAAAAAA")
    container.pQ.size should be (0)
  }
}
