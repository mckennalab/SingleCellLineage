package aligner

import reads.SequencingRead

import scala.collection.mutable.ArrayBuffer

/**
  * Created by aaronmck on 11/21/16.
  */
class NeedlemanWunsch(reference: String) {
  val ref = reference

  def align(read: String, debug: Boolean = false): NWAlignmentResult = {
    val aligned = NeedlemanWunsch.alignTwoStrings(ref, read)
    NWAlignmentResult(aligned._1,aligned._2)
  }
}

object NeedlemanWunsch {
  def alignTwoStrings(str1: String,
                      str2: String,
                      matchVal: Double = 5.0,
                      mismatchVal: Double = -4.0,
                      gap: Double = -10.0,
                      printMatrix: Boolean = false): Tuple2[String, String] = {

    // our data matrix -- an extra row at the beginning for initialized values
    // ordering is our choice so str1 is rows, str2 is columns e.g. (row, column)
    val scores = Array.fill[Double](str1.size + 1, str2.size + 1)(0.0)
    val traceback = Array.fill[TraceBack](str1.size + 1, str2.size + 1)(Diag)

    // initialize the values for the axes
    scores(0)(0) = 0.0
    (1 until str1.size).foreach { index => scores(index)(0) = scores(index - 1)(0) + gap }
    (1 until str2.size).foreach { index => scores(0)(index) = scores(0)(index - 1) + gap }

    // now fill in the score matrix
    (1 until str1.size).foreach { index1 => {
      (1 until str2.size).foreach { index2 => {
        val matchedScore = scores(index1 - 1)(index2 - 1) + (if (str1(index1 - 1) == str2(index2 - 1)) matchVal else mismatchVal)
        val deletionScore = scores(index1 - 1)(index2) + gap
        val insertionScore = scores(index1)(index2 - 1) + gap
        (matchedScore, deletionScore, insertionScore) match {
          case (m, d, i) if m >= d && m >= i => {
            scores(index1)(index2) = matchedScore
            traceback(index1)(index2) = Diag
          }
          case (m, d, i) if d >= m && d >= i => {
            scores(index1)(index2) = deletionScore
            traceback(index1)(index2) = Left
          }
          case (m, d, i) if i >= m && i >= d => {
            scores(index1)(index2) = insertionScore
            traceback(index1)(index2) = Up
          }
        }
      }
      }
    }
    }

    // now trackback the best scores
    val refBuffer = ArrayBuffer.empty[String]
    val readBuffer = ArrayBuffer.empty[String]
    var index1tb = str1.size
    var index2tb = str2.size

    while (index1tb > 0 && index2tb > 0) {
      traceback(index1tb)(index2tb) match {
        case Diag => {
          index1tb -= 1
          index2tb -= 1
          refBuffer += str1(index1tb).toString
          readBuffer += str2(index2tb).toString
        }
        case Left => {
          index1tb -= 1
          index2tb -= 0
          refBuffer += str1(index1tb).toString
          readBuffer += "-" // str2(index2tb)
        }
        case Up => {
          index1tb -= 0
          index2tb -= 1
          refBuffer += "-" // str1(index1tb)
          readBuffer += str2(index2tb).toString
        }
      }
    }

    if (index1tb > 0)
      (index1tb until 0 by -1).foreach{ind => {
        index1tb -= 1
        refBuffer += str1(index1tb).toString
        readBuffer += "-" // str2(index2tb)
      }}
    else if (index2tb > 0)
      (index2tb until 0 by -1).foreach{ind => {
        index2tb -= 1
        refBuffer += "-" // str1(index1tb)
        readBuffer += str2(index2tb).toString
      }}

    return ((refBuffer.mkString("").reverse, readBuffer.mkString("").reverse))
  }
}

sealed trait TraceBack

case object Up extends TraceBack

case object Left extends TraceBack

case object Diag extends TraceBack

case class NWAlignmentResult(referenceAlignment: String, queryAlignment: String)