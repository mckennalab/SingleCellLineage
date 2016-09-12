package reads

import scala.collection.mutable


/**
  * evaluate reads on the fly, limiting our container to a specified size of high-quality reads, but recording the total numbers we saw
  */
class SingleRankedReadContainer(umi: String, maxSize: Int) {
  var totalReads = 0
  var totalPassedReads = 0
  var noPrimer1 = 0
  var noPrimer2 = 0

  val maxSz = maxSize
  var pQ = new mutable.PriorityQueue[SortedRead]()

  def size() = pQ.size

  /**
    * add a read pair our sorting container, dropping low-quailty reads if we've exceeded our storage capacity
    *
    * @param seq             the first sequence read
    * @param containsPrimer1 does the first read contain the primer?
    */
  def addRead(seq: SequencingRead, containsPrimer1: Boolean): Unit = {
    totalReads += 1

    if (containsPrimer1) {
      totalPassedReads += 1
      pQ += SortedRead(seq)
      while (pQ.size > maxSz)
        pQ.dequeue()
    } else {
      if (!containsPrimer1)
        noPrimer1 += 1
    }
  }

  /**
    * make a paired array set from the reads
    *
    * @return the paired set
    */
  def toPairedFWDREV(): Tuple2[Array[SequencingRead], Array[SequencingRead]] = {
    val readFWD = new mutable.ArrayBuffer[SequencingRead]()
    val readREV = new mutable.ArrayBuffer[SequencingRead]()

    pQ.foreach { readPair => {
      readFWD += readPair.read1
    }
    }
    return (readFWD.toArray, readREV.toArray)
  }
}

// a case class container for pairs of reads -- we use this to sort by length, given that reads are generally quality trimmed
case class SortedRead(read1: SequencingRead, rankByQual: Boolean = true) extends Ordered[SortedRead] {

  // here we have a choice, we can either rank by the average qual over the read, or the average length
  val totalAverageQual = (read1.averageQual() * read1.length) / (read1.length).toDouble
  val totalAverageLength = (read1.length) / (2.0)

  // choose what to rank by -- qual or length
  val rankVal = if (rankByQual) totalAverageQual else totalAverageLength

  // compare the events in reverse order -- we want to drop sorted reads in anti qual order
  def compare(that: SortedRead): Int =
    if (this.rankVal == that.rankVal)
      this.read1.length - that.read1.length
    else
      that.rankVal.toInt - this.rankVal.toInt

}