package main.scala.matrix

import main.scala.Alignment
import main.scala.states._

import scala.collection.mutable

/**
  * create a traceback matrix from our data
  */
class TracebackMatrix (dimX: Int, dimY: Int) {

  val values = Array.ofDim[EmissionState](dimX, dimY)
  (0 until dimX).foreach{x => (0 until dimY).foreach{y => values(x)(y) = Matched}}

  def set(rowPos: Int, colPos: Int, value: EmissionState) {values(rowPos)(colPos) = value}
  def get(rowPos: Int, colPos: Int) : EmissionState = values(rowPos)(colPos)


  def printMatrix(pad: Int = 7): Unit = {
    println(MatrixUtils.padInt(-1, pad) + (0 until dimX).map { i => MatrixUtils.padInt(i, pad) }.mkString(""))
    (0 until dimY).foreach { indexB => {
      print(MatrixUtils.padInt(indexB, pad))
      (0 until dimX).foreach { indexA => {
        print(MatrixUtils.padString(values(indexA)(indexB).str, pad))
      }
      }
      println()
    }
    }
  }
}

object TracebackMatrix {

  /**
    * given a set of traceback matrices, find the highest scoring global alignment
    * @param sequenceA the first sequence to align
    * @param sequenceB the second sequence
    * @param matFunction a mapping from emission type to the scoring matrix
    * @param tracebackFunc the mapping from emission state to traceback matrix
    * @return an alignment object
    */
  def tracebackGlobalAlignment(sequenceA: String,
                               sequenceB: String,
                               matFunction: (EmissionState => ScoreMatrix),
                               tracebackFunc: (EmissionState => TracebackMatrix)): Alignment = {

    // get pointers to the end of the sequence
    var indexA = sequenceA.size
    var indexB = sequenceB.size

    // make storage for the aligned string results
    var alignedStringA = mutable.ArrayBuilder.make[Char]()
    var alignedStringB = mutable.ArrayBuilder.make[Char]()

    // find the best final score
    var currentBestMatrix: EmissionState = Matched
    EmissionState.knownStates.foreach{state =>
      if (matFunction(state).get(indexA,indexB) < matFunction(state).get(indexA,indexB))
        currentBestMatrix = state
    }

    // while we're not back to square 0,0...
    while (!(indexA == 0 && indexB == 0)) {
      //println(currentBestMatrix + "(" + indexA + "," + indexB + ") " + tracebackFunc(currentBestMatrix).get(indexA, indexB) + " " + currentBestMatrix + " " + alignedStringA.result.reverse.mkString("") + " " + alignedStringB.result.reverse.mkString(""))
      val nextMatrix = tracebackFunc(currentBestMatrix).get(indexA,indexB)
      (tracebackFunc(currentBestMatrix).get(indexA, indexB)) match {
        case (x) if indexA == 0 => {
          alignedStringA += '-'
          alignedStringB += sequenceB(indexB - 1)
          indexB -= 1
        } case (x) if indexB == 0 => {
          alignedStringA += sequenceA(indexA - 1)
          alignedStringB += '-'
          indexA -= 1
        } case (Matched) => {
          alignedStringA += sequenceA(indexA - 1)
          alignedStringB += sequenceB(indexB - 1)
          indexA -= 1
          indexB -= 1
        } case (GapA) => {
          alignedStringA += '-'
          alignedStringB += sequenceB(indexB - 1)
          indexB -= 1
        } case (GapB) => {
          alignedStringA += sequenceA(indexA - 1)
          alignedStringB += '-'
          indexA -= 1
        } case (x) => {
          throw new IllegalStateException("Unmatched " + x)
        }
      }
      currentBestMatrix = nextMatrix
    }

    new Alignment {
      def global = true

      def getStart = 0

      def getStop = sequenceA.size

      def getAlignmentString = (alignedStringA.result().mkString("").reverse, alignedStringB.result().mkString("").reverse)

      def getScore = {
        EmissionState.knownStates.map{state => matFunction(state).get(sequenceA.size, sequenceB.size)}.max
      }

      def getSeqA = sequenceA

      def getSeqB = sequenceB
    }
  }
}
