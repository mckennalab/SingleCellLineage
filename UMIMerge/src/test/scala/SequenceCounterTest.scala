import collapse.SequenceCounter
import org.scalatest.{FlatSpec, Matchers}
import reads.{Consensus, SequencingRead}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class SequenceCounterTest extends FlatSpec with Matchers {
  val lng = 200
  val readCount = 20

  "SequenceCounter" should "test should collapse a identical pile of reads reads correctly" in {

    val counter = SequenceCounterTest.createReadSeq(lng,readCount)
    val concen = counter.countsToSequence(0.9,0.9)

    concen.string.isDefined should be (true)
    concen.count should be (readCount)
    concen.nProp should be (0.0)
  }

  "SequenceCounter" should "should collapse discordant bases to Ns reads correctly" in {

    val counter = SequenceCounterTest.createReadSeq(lng,readCount)
    // make the first base 50/50 A/C
    counter.counts(0)(0) = readCount/2
    counter.counts(0)(1) = readCount/2
    counter.counts(0)(2) = 0
    counter.counts(0)(3) = 0

    val concen = counter.countsToSequence(0.9,0.9)
    concen.string.isDefined should be (true)
    concen.count should be (readCount)
    concen.nProp should be (1.0/lng.toDouble)
  }

  "SequenceCounter" should "should collapse two discordant bases to Ns reads correctly" in {

    val counter = SequenceCounterTest.createReadSeq(lng,readCount)
    // make the first base 50/50 A/C
    counter.counts(0)(0) = readCount/2
    counter.counts(0)(1) = readCount/2
    counter.counts(0)(2) = 0
    counter.counts(0)(3) = 0

    val secondPos = 10
    counter.counts(secondPos)(0) = readCount/2
    counter.counts(secondPos)(1) = readCount/2
    counter.counts(secondPos)(2) = 0
    counter.counts(secondPos)(3) = 0


    val concen = counter.countsToSequence(0.9,0.9)
    concen.string.isDefined should be (true)
    concen.count should be (readCount)
    concen.nProp should be (2.0/lng.toDouble)
  }


  "SequenceCounter" should "should not return a string when there's too many Ns" in {

    val counter = SequenceCounterTest.createReadSeq(lng,readCount)
    // make the first base 50/50 A/C
    (0 until (lng/10)).foreach{index => {
      counter.counts(index)(0) = readCount / 2
      counter.counts(index)(1) = readCount / 2
      counter.counts(index)(2) = 0
      counter.counts(index)(3) = 0
    }}

    val concen = counter.countsToSequence(0.9,0.1)
    concen.string.isDefined should be (false)
  }
  "SequenceCounter" should "should  return a string when there's just below the limit of Ns" in {

    val counter = SequenceCounterTest.createReadSeq(lng,readCount)
    // make the first base 50/50 A/C
    (0 until (lng/10) - 1).foreach{index => {
      counter.counts(index)(0) = readCount / 2
      counter.counts(index)(1) = readCount / 2
      counter.counts(index)(2) = 0
      counter.counts(index)(3) = 0
    }}

    val concen = counter.countsToSequence(0.9,0.1)
    concen.string.isDefined should be (true)
  }

  "SequenceCounter" should "call an A base correctly" in {

    val counts = Array[Int](100,0,0,0,0)
    val called = SequenceCounter.pileupToCall(counts,1)
    (called) should be ('A')
  }

  "SequenceCounter" should "call an C base correctly" in {

    val counts = Array[Int](0,100,0,0,0)
    val called = SequenceCounter.pileupToCall(counts,1)
    (called) should be ('C')
  }

  "SequenceCounter" should "call an G base correctly" in {

    val counts = Array[Int](0,0,100,0,0)
    val called = SequenceCounter.pileupToCall(counts,1)
    (called) should be ('G')
  }

  "SequenceCounter" should "call an T base correctly" in {

    val counts = Array[Int](0,0,0,100,0)
    val called = SequenceCounter.pileupToCall(counts,1)
    (called) should be ('T')
  }
  "SequenceCounter" should "call an ambiguous base correctly" in {

    val counts = Array[Int](0,0,50,50,0)
    val called = SequenceCounter.pileupToCall(counts,1)
    (called) should be ('N')
  }
}

object SequenceCounterTest {

  def createReadSeq(length: Int, count: Int): SequenceCounter = {

    val readBase = SequenceCounterTest.generateFakeRead(length)
    val counter = new SequenceCounter(length)

    (0 until count).foreach(c => counter.addSequence(readBase))
    counter
  }

  def generateFakeRead(length: Int): String = {
    (0 until length).map(index => randomBase()).mkString("")
  }

  def randomBase(): Char = {
    val rand = new Random()
    val rd = rand.nextInt(4)
    rd match {
      case 0 => 'A'
      case 1 => 'C'
      case 2 => 'G'
      case 3 => 'T'
      case _ => 'N'
    }
  }
}