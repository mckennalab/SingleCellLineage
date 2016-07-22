package main.scala.utils

import scala.io._
import java.io._
import scala.collection.mutable._
import scala.sys.process._
import java.util.zip._

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
    * compute the edit distance of two base strings
    *
    * @param seq1 the first sequence
    * @param seq2 the second string
    * @return the edit distance
    * @throws IllegalStateException if strings are of unequal length
    */
  def editDistance(seq1: String, seq2: String): Int = {
    if (seq1.size != seq2.size)
      throw new IllegalStateException("Unable to compare edit distances for unequal strings")
    seq1.toUpperCase().zip(seq2.toUpperCase).map { case (s1, s2) => if (s1 == s2) 0 else 1 }.sum
  }
}
