package reads

import scala.collection.mutable
import scala.annotation.switch

/**
  * evaluate reads on the fly, limiting our container to a specified size of high-quality reads, but recording the total numbers we saw
  */
class RankedReadContainer(umi: String, maxSize: Int) {
  var totalReads = 0
  var totalPassedReads = 0
  var noPrimer1 = 0
  var noPrimer2 = 0

  val maxSz = maxSize
  var pQ = new mutable.PriorityQueue[ReadBundle]()

  def size() = pQ.size


  def addBundle(readBundle: ReadBundle): Unit = {
    totalReads += 1
    var allContainsPrimer = true
    readBundle.toReadSet.zip(readBundle.containsPrimer).zipWithIndex.foreach{case((read,containsPrimer),index) => {
      (index : @switch) match {
        case 0 => {
          if (!containsPrimer)
            noPrimer1 += 1
        }
        case 1 => {
          if (!containsPrimer)
            noPrimer2 += 1
        }
      }
      allContainsPrimer = allContainsPrimer & containsPrimer
    }}


    if (allContainsPrimer) {
      // check also that we're being consistent
      val setSize = if (pQ.size > 0) pQ.head.size else readBundle.size

      if (setSize != readBundle.size)
        throw new IllegalStateException("Size of read bundle supplied doesn't match the current collection: " + readBundle.size + " doesn't equal " + setSize)

      pQ += readBundle
    }

    // if we've overflowed, drop the lowest performers
    while (pQ.size > maxSz)
      pQ.dequeue()
  }

  def result() : List[ReadBundle] = pQ.toList
}


trait ReadBundle extends Ordered[ReadBundle] {
  def toReadSet: List[SequencingRead]
  def size: Int
  def isPaired: Boolean = size == 2
  def totalAverageLength: Double
  def containsPrimer: List[Boolean]
}


// a case class container for pairs of reads -- we use this to sort by length, given that reads are generally quality trimmed
case class SortedReadPair(read1: SequencingRead, read2: SequencingRead, containsPrimer1: Boolean, containsPrimer2: Boolean) extends ReadBundle with Ordered[ReadBundle] {

  // here we have a choice, we can either rank by the average qual over the read, or the average length
  //val totalAverageQual = (read1.averageQual() * read1.length + read2.averageQual() * read2.length) / (read1.length + read2.length).toDouble
  override def totalAverageLength = (read1.length + read2.length) / (2.0)

  def size = 2

  override def toReadSet: List[SequencingRead] = List[SequencingRead](read1,read2)

  // compare the events in reverse order -- we want to drop sorted reads in reverse length order
  override def compare(that: ReadBundle): Int = {
    require(that.size == this.size)

    (that.toReadSet(0).length + that.toReadSet(1).length) - (this.read1.length + this.read2.length)
  }

  def containsPrimer = List[Boolean](containsPrimer1,containsPrimer2)
}


// a sorting container for single reads
case class SortedRead(read1: SequencingRead, containsPrimer1: Boolean) extends ReadBundle with Ordered[ReadBundle] {

  override def totalAverageLength = read1.length.toDouble

  def size = 1

  override def toReadSet: List[SequencingRead] = List[SequencingRead](read1)

  // compare the events in reverse order -- we want to drop sorted reads in reverse length order
  override def compare(that: ReadBundle): Int = {
    require(that.size == this.size)

    that.toReadSet(0).length - this.read1.length
  }

  def containsPrimer = List[Boolean](containsPrimer1)
}