package reads

import scala.collection._
import utils._

/**
 * holds a sequencing read
 */
case class SequencingRead(name: String, bases: String, quals: String, readOrientation: ReadDirection, umi: String, cigar: Option[String] = None, position: Int = 0) {
  if (bases.length != quals.length && quals != "*")
    throw new IllegalArgumentException("Read " + name + " has an unequal base and qual string!\n" + bases + "\n" + quals + "\n")

  val intQuals = quals.map{qual => Utils.phredCharToQscore(qual)}.toArray
  def length = bases.length
  var reverseCompAlign = false


  // reads can store metadata in their names, a series of double underscore tags at the end of the read like __number->10
  var metaData = Array[Tuple2[String,String]]()

  if (name contains "__") {
    name.split("__").foreach{possibleTag => {
      if (possibleTag contains "->") {
        val tagSplit = possibleTag.split("->")
        if (tagSplit.size == 2)
          metaData :+= (tagSplit(0),tagSplit(1))
      }
    }}
  }


  /**
   * find the last base in the string before a series of dashes
    *
    * @return an integer of the last position in a read that's not a dash
   */
  def trueEnd(): Int = {
    ((bases.length - 1).until(-1,-1)).foreach{i => if (bases(i) != '-') return i}
    return -1
  }

  /**
   * slice a read at a position, producing a read with appropriate qual scores
    *
    * @param fromPos the base to start at, inclusive
   * @param toPos the base to end at, inclusive
   * @return a new SequencingRead representing the sliced-down version
   */
  def slice(fromPos: Int, toPos: Int): SequencingRead = {
    if (fromPos < 0)
      throw new IllegalArgumentException("from position of " + fromPos + " < 0")
    if (toPos > bases.length)
      throw new IllegalArgumentException("to position of " + toPos + " > readlength of " + toPos)
    if (fromPos >= toPos)
      throw new IllegalArgumentException("from position of " + fromPos + " greater than or equal to toPos of " + toPos)

    SequencingRead(name, bases.slice(fromPos, toPos),quals.slice(fromPos, toPos), readOrientation, umi)
  }

  /**
   * filter the reads down by a specific combination of quality score drop over a window
    *
    * @param windowSize the sliding window to filter over
   * @param minWindowQual the qual score we have to achive over a window to keep the rest of the read
   */
  def qualityThresholdRead(windowSize: Int = 5, minWindowQual: Double = 10): SequencingRead = {
    val cutPos = intQuals.toArray.sliding(windowSize).zipWithIndex.map{case(basesInWindow,index) => {
      if (basesInWindow.sum / windowSize.toDouble < minWindowQual) {
        // println("for read " + bases + " found a window with qual of " +  (basesInWindow.sum / windowSize.toDouble) + " at position " + index)
        index
      } else
        0
    }}.filter(x => x != 0).toArray
    println("quals = " + (quals.zip(bases).zipWithIndex.map{case((ql,bs),index) => index + "--" + bs + "," + Utils.phredCharToQscore(ql)}.mkString(";")))
    println("for read " + bases + " the cut pos is " + cutPos.mkString(","))
    if (cutPos.size > 0) {
      println(cutPos(0))
      val cutMinusWindow = math.max(0, cutPos(0) - minWindowQual)
      SequencingRead(name, bases.slice(0, cutPos(0)), quals.slice(0, cutPos(0)), readOrientation, umi)
    } else
      SequencingRead(name, bases, quals, readOrientation, umi)
  }

  /**
   * find the 'distance' between this read and another
    *
    * @param read2 the second sequencing read to consider
   * @return a double, the mismatched bases, normalized by the *longer* of the two read lengths
   */
  def distance(read2: SequencingRead): Double =
    (bases.zip(read2.bases).map{case(b1,b2) => if (b1 == b2) 0 else 1}.sum.toDouble + math.abs(bases.length - read2.bases.length)) /
      math.max(bases.length,read2.bases.length).toDouble

  /**
   *
   * @return the average quality score value
   */
  def averageQual(): Double = intQuals.sum.toDouble / intQuals.length.toDouble

  /**
   * does the read contain the name primer?
    *
    * @param primer the primer sequence (please make sure the reverse complement is done for reverse reads)
   * @param window the window of bases added to the primer length when searching the beginning of the read
   * @return true if the read starts with the primer
   */
  def startsWithPrimer(primer: String, window: Int = 5): Boolean = (bases.slice(0,primer.length + 5) contains primer)

  /**
   *
   * @return a string representing the read in fastq format
   */
  def toFastqString(umi: String, rev: Boolean, index: Int, stripFromFront: Int): String = {
    val plusOrMinus = readOrientation match {
      case ForwardReadOrientation => "+"
      case ReverseReadOrientation => "+" // nevermind, this is not imporant
      case ReferenceRead => "+" // ehh lets assume
      case ConsensusRead => "+" // ehh lets assume
    }
    if (!rev)
      "@NS500488:132:H7JCTAFXX:1:11101:8025:" + index + " " + umi + "_" + name + "\n" + bases.slice(stripFromFront,bases.length) + "\n" + plusOrMinus + "\n" + quals.slice(stripFromFront,quals.length)
    else
      "@NS500488:132:H7JCTAFXX:1:11101:8025:" + index + " " + umi + "_" + name + "\n" + Utils.reverseComplement(bases.slice(stripFromFront,bases.length)) + "\n" + plusOrMinus + "\n" + quals.slice(stripFromFront,quals.length)
  }


  /**
   * find the first and last non-deletion character in a read -- useful when we align to the reference
   * and we want to remove cruft like reading into adapters, etc
    *
    * @return a tuple pair of the first non-dash base and last non-dash base (zero-indexed)
   */
  def firstAndLastActualBases(): Tuple2[Int,Int] = {
    var firstNonDash = 0
    var lastNonDash = bases.length - 1


    while(firstNonDash < bases.length & bases(firstNonDash) == '-')
      firstNonDash += 1
    while(lastNonDash > 0 & bases(lastNonDash) == '-')
      lastNonDash -= 1
    (firstNonDash,lastNonDash)
  }

}

object SequencingRead {
  /**
   * produce a reverse complement of a read, taking some care to correspond the bases and quals
    *
    * @param sequencingRead the sequencing read to reverse
   * @return a sequencingread object that represents the reverse read
   */
  def reverseComplement(sequencingRead: SequencingRead): SequencingRead = {
    //name: String, bases: String, quals: String, forwardRead: Boolean, umi: String
    val newBases = Utils.reverseComplement(sequencingRead.bases.toUpperCase())
    val newQuals = sequencingRead.quals.reverse

    SequencingRead(sequencingRead.name,newBases,newQuals,sequencingRead.readOrientation,sequencingRead.umi)
  }

  /**
   * strip the insertions out of a read, most likely for output
    *
    * @param sequencingRead the input read
   * @return a sequencing read representation of the read with insertions stripped out
   */
  def stripDownToJustBases(sequencingRead: SequencingRead): SequencingRead = {
    //name: String, bases: String, quals: String, readOrientation: ReadDirection, umi: String
    var bases = ""
    var quals = ""
    for (i <- 0 until sequencingRead.bases.length) {
      if (sequencingRead.bases(i) != '-') {
        bases += sequencingRead.bases(i)
        quals += sequencingRead.quals(i)
      }
    }

    SequencingRead(sequencingRead.name,bases,quals,sequencingRead.readOrientation,sequencingRead.umi)
  }

  /**
   * this function is for creating reads during testing
    *
    * @param name read name
   * @param bases bases as a string
   * @return a seq read with quality H
   */
  def readFromNameAndSeq(name: String, bases: String): SequencingRead = {
    SequencingRead(name,bases,"H"*bases.length,ForwardReadOrientation,"UNKNOWN")
  }

  /**
   * this is for testing
    *
    * @param name read name
   * @param bases bases as a string
   * @param qualBase as a string
   * @return a seq read with quality H
   */
  def readFromNameAndSeq(name: String, bases: String, qualBase: String): SequencingRead = {
    SequencingRead(name,bases,qualBase*bases.length,ForwardReadOrientation,"UNKNOWN")
  }

  /**
    * given a set of reads, aggregate the meta data from the reads into a summary which can be output
    * @param reads
    * @return
    */
  def aggregateMetaData(reads: Array[SequencingRead]): String = {
    val mapping = new mutable.HashMap[String,mutable.HashMap[String,Int]]()

    reads.foreach{case(read) => {
      read.metaData.foreach{case(metaName,metaValue) => {
        val annotationValue = mapping.getOrElse(metaName,new mutable.HashMap[String,Int]())
        annotationValue(metaValue) = annotationValue.getOrElse(metaValue,0) + 1
        mapping(metaName) = annotationValue
      }}
    }}

    // now compose a string representation
    return mapping.map{case(hashKey,hashContainer) => {
      hashKey + "->" + hashContainer.map{case(hashValue,count) => hashValue + "=" + count}.mkString(";")
    }}.mkString("__")
  }
}