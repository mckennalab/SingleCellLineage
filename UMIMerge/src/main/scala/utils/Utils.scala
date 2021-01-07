package utils

import java.io._
import java.util.zip._

import aligner.NeedlemanWunsch

/**
 * Created by aaronmck on 10/22/15.
 */
object Utils {

  def phredCharToDouble(ch: Char): Double = math.pow(10, ((ch.toInt - 32) / 10.0) * -1.0)

  def phredCharToQscore(ch: Char): Int = (ch.toInt - 32)

  def probabilityOfErrorToPhredInt(vl: Double): Int = math.round(-10.0 * math.log10(vl)).toInt

  def probabilityOfErrorToPhredChar(vl: Double): Char = (probabilityOfErrorToPhredInt(vl) + 33).toChar

  // read in compressed input streams with scala source commands
  def gis(s: String) = new GZIPInputStream(new BufferedInputStream(new FileInputStream(s)))

  // complement a base as a character
  def compBase(b: Char): Char = b match {
    case 'A' => 'T'
    case 'C' => 'G'
    case 'G' => 'C'
    case 'T' => 'A'
    case 'a' => 't'
    case 'c' => 'g'
    case 'g' => 'c'
    case 't' => 'a'
    case _ => 'N'
  }

  // reverse complement a string of DNA bases
  def reverseComplement(str: String) = str.map { t => compBase(t) }.reverse.mkString("")


  // quality filter reads.  Find any window of n bases that have a quality score less than X, cut and drop
  // the rest of the read.  returns the remaining read string
  def qualityControlRead(read: String, qualString: String, windowSize: Int = 5, minWindowQual: Double = 25): String = {
    val cutPos = qualString.toArray.sliding(windowSize).zipWithIndex.map { case (bases, index) => {
      if (bases.map { b => phredCharToQscore(b) }.sum / windowSize.toDouble < minWindowQual)
        index
      else
        0
    }
    }.filter(x => x != 0).toArray

    if (cutPos.size > 0)
      read.slice(0, cutPos(0))
    else
      read
  }

  /**
   * helper method: does the read start with the primer of interest?
   *
   * @param read              the read sequence
   * @param primer            the primer sequence
   * @param allowedMismatches the number of mismatches allowed to consider it a TRUE
   * @return true if mismatches <= allowedMismatches
   */
  def containsFWDPrimerByAlignment(read: String, primer: String, allowedMismatches: Int): Boolean = {
    //println("ALLOWED " + allowedMismatches + " PRIMER " + primer  + " READ " + read + " ==> " + editDistanceByAlignment(primer,read,true))
    editDistanceByAlignment(primer, read, true) <= allowedMismatches
  }

  /**
   * helper method: does the read start with the primer of interest?
   *
   * @param read              the read sequence
   * @param primer            the primer sequence, the reverse complement of which will match our read
   * @param allowedMismatches the number of mismatches allowed to consider it a TRUE
   * @return true if mismatches <= allowedMismatches
   */
  def containsREVCompPrimerByAlignment(read: String, primer: String, allowedMismatches: Int): Boolean = {
    editDistanceByAlignment(Utils.reverseComplement(primer), read, true) <= allowedMismatches
  }

  /**
   * helper method: does the read start with the primer of interest? IMPORTANT: this assumes the primer and read will
   * be a match without reverse complimenting
   *
   * @param read              the read sequence
   * @param primer            the primer sequence
   * @param allowedMismatches the number of mismatches allowed to consider it a TRUE
   * @return true if mismatches <= allowedMismatches
   */
  def readEndsWithPrimerExistingDirection(read: String, primer: String, allowedMismatches: Int): Boolean = {
    //println("REVERSE!!!!")
    val filteredRead = read.filter(bs => bs != '-').mkString("")
    editDistanceByAlignment(primer, filteredRead.slice(filteredRead.length - primer.length, filteredRead.length), false) <= allowedMismatches
  }

  /**
   * helper method: do our paired-end reads start and end with the primer of interest?
   *
   * @param read1             the first read seq
   * @param read2             second read
   * @param primer1           our first primer
   * @param primer2           our second primer, the reverse complement of which will match our read 2
   * @param allowedMismatches true if mismatches <= allowedMismatches
   * @return a tuple for each read, where true means the # of mismatches <= allowedMismatches
   */
  def containsFWDandREVCompByAlignment(read1: String, read2: String, primer1: String, primer2: String, allowedMismatches: Int): Tuple2[Boolean, Boolean] = {
    (containsFWDPrimerByAlignment(read1, primer1, allowedMismatches), containsREVCompPrimerByAlignment(read2, primer2, allowedMismatches))
  }

  /**
   * helper method: do our paired-end reads start and end with the primer of interest?
   *
   * NOTE: This is used when reads are oriented in the same direction as the reference (both on the forward strand)
   *
   * @param read1             the first read seq
   * @param read2             second read
   * @param primer1           our first primer
   * @param primer2           our second primer, the reverse complement of which will match our read 2
   * @param allowedMismatches true if mismatches <= allowedMismatches
   * @return a tuple for each read, where true means the # of mismatches <= allowedMismatches
   */
  def containsBothPrimerByAlignmentReoriented(read1: String, read2: String, primer1: String, primer2: String, allowedMismatches: Int): Tuple2[Boolean, Boolean] = {
    (containsFWDPrimerByAlignment(read1, primer1, allowedMismatches), readEndsWithPrimerExistingDirection(read2, primer2, allowedMismatches))
  }

  /**
   * helper method: do our paired-end reads start and end with the primer of interest?
   *
   * NOTE: This is used when reads are oriented as they come off a Illumina sequencer (forward and reverse strands)
   *
   * @param read1             the first read seq
   * @param read2             second read
   * @param primer1           our first primer
   * @param primer2           our second primer, the reverse complement of which will match our read 2
   * @param allowedMismatches true if mismatches <= allowedMismatches
   * @return a tuple for each read, where true means the # of mismatches <= allowedMismatches
   */
  def containsBothPrimerByAlignment(read1: String, read2: String, primer1: String, primer2: String, allowedMismatches: Int): Tuple2[Boolean, Boolean] = {
    (containsFWDPrimerByAlignment(read1, primer1, allowedMismatches), containsFWDPrimerByAlignment(read2, Utils.reverseComplement(primer2), allowedMismatches))
  }

  /**
   * helper method: does a MERGED read contain both primers?
   *
   * @param read              the sequencing read
   * @param primer1           our first primer
   * @param primer2           second primer, normal complement! (read 5' to 3')
   * @param allowedMismatches true if mismatches <= allowedMismatches
   * @return a tuple for each read, where true means the # of mismatches <= allowedMismatches
   */
  def containsBothPrimerByAlignment(read: String, primer1: String, primer2: String, allowedMismatches: Int): Tuple2[Boolean, Boolean] = {
    (containsFWDPrimerByAlignment(read, primer1, allowedMismatches), readEndsWithPrimerExistingDirection(read, primer2, allowedMismatches))
  }


  /**
   * perform an alignment using Needleman-Wunsch between two strings and find their edit distance
   *
   * @param alignTo   the sequence to align to
   * @param alignWith sequence to align with
   * @return the edit distance (int)
   */
  def editDistanceByAlignment(alignTo: String, alignWith: String, subsetToAlignToLength: Boolean): Int = {

    // filter gaps out of each sequence before setting up the alignment
    val reference = alignTo.filter(bs => bs != '-').mkString("")
    val query = alignWith.filter(bs => bs != '-').mkString("")

    val alignment = new NeedlemanWunsch(reference)

    val subsetQuery = if (subsetToAlignToLength) query.slice(0, reference.length) else query
    val nwResult = alignment.align(subsetQuery)
    //println(nwResult.queryAlignment,nwResult.referenceAlignment)
    Utils.editDistance(nwResult.referenceAlignment, nwResult.queryAlignment)
  }

  /**
   * compute the edit distance of two base strings
   *
   * @param seq1 the first sequence
   * @param seq2 the second string
   * @return the edit distance
   * @throws IllegalStateException if strings are of unequal length
   */
  def editDistance(seq1: String, seq2: String): Int = {
    if (seq1.size != seq2.size)
      throw new IllegalStateException("Unable to compare edit distances for unequal strings: " + seq1 + " AND " + seq2)
    seq1.toUpperCase().zip(seq2.toUpperCase).map { case (s1, s2) => if (s1 == s2) 0 else 1 }.sum
  }

  /**
   * convert char to a Phred quality score
   *
   * @param char the character to convert
   * @return a Double of the Phred (0-~40)
   */
  def charToPhredQual(c: Char): Double = {
    (c.toInt - 33).toDouble
  }

  /**
   * remove any low-quality score region within the string
   *
   * @param str                 the sequence string
   * @param quals               the string of quals
   * @param minPhredAverageQual the min phred score (averaged)
   * @param windowSize          the window size to scan over
   * @return the sliced string
   */
  def filterReadBySlidingWindow(str: String, quals: String, minPhredAverageQual: Double, windowSize: Int): String = {
    val phreds = quals.map { g => charToPhredQual(g) }
    val averages = (0 until (phreds.length - windowSize)).map { index => phreds.slice(index, index + windowSize).sum / windowSize }
    val firstLowScore = averages.indexWhere(p => p <= minPhredAverageQual)

    if (firstLowScore >= 0)
      str.slice(0, firstLowScore)
    else
      str
  }
}
