package main.scala.matrix

import main.scala.Alignment
import main.scala.states.{EmissionState, _}

import scala.collection.mutable

/**
  * create a traceback matrix from our data
  */
class TracebackMatrix (dimX: Int, dimY: Int) {

  val values = Array.ofDim[EmissionState](dimX, dimY)
  (0 until dimX).foreach{x => (0 until dimY).foreach{y => values(x)(y) = Matched()}}

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
    var alignedStringA = new mutable.ArrayBuffer[Char]()
    var alignedStringB = new mutable.ArrayBuffer[Char]()

    // find the best final score
    var currentBestMatrix = Matched().str
    EmissionState.knownStates.foreach{state =>
      if (matFunction(EmissionState.stringToState(state)).get(indexA,indexB) < matFunction(EmissionState.stringToState(state)).get(indexA,indexB))
        currentBestMatrix = state
    }

    // while we're not back to square 0,0...
    while (!(indexA == 0 && indexB == 0)) {
      /*println(currentBestMatrix + "(" + indexA + "," + indexB + ") " +
        tracebackFunc(EmissionState.stringToState(currentBestMatrix)).get(indexA, indexB) + " " +
        currentBestMatrix + " " + alignedStringA.result.reverse.mkString("") + " " +
        alignedStringB.result.reverse.mkString("") + " size " + tracebackFunc(EmissionState.stringToState(currentBestMatrix)).get(indexA, indexB))
*/
      val nextMatrix = tracebackFunc(EmissionState.stringToState(currentBestMatrix)).get(indexA,indexB)
      (tracebackFunc(EmissionState.stringToState(currentBestMatrix)).get(indexA, indexB)) match {
        case xCont:EmissionState if indexA == 0 => {
          alignedStringA ++= "-" * xCont.distance
          alignedStringB ++= sequenceB.slice(indexB - xCont.distance,indexB).reverse
          indexB -= xCont.distance
        } case xCont:EmissionState if indexB == 0 => {
          alignedStringA ++= sequenceA.slice(indexA - xCont.distance,indexA).reverse
          alignedStringB ++=  "-" * xCont.distance
          indexA -= xCont.distance
        } case Matched(x) => {
          alignedStringA ++= sequenceA.slice(indexA - x,indexA).reverse
          alignedStringB ++= sequenceB.slice(indexB - x,indexB).reverse
          indexA -= x
          indexB -= x
        } case GapA(x) => {
          alignedStringA ++=  "-" * x
          alignedStringB ++= sequenceB.slice(indexB - x,indexB).reverse
          indexB -= x
        } case GapB(x) => {
          alignedStringA ++= sequenceA.slice(indexA - x,indexA).reverse
          alignedStringB ++= "-" * x
          indexA -= x
        } case (x) => {
          throw new IllegalStateException("Unmatched " + x)
        }
      }
      currentBestMatrix = nextMatrix.str
    }

    new Alignment {
      def global = true

      def getStart = 0

      def getStop = sequenceA.size

      def getAlignmentString = (alignedStringA.result().mkString("").reverse, alignedStringB.result().mkString("").reverse)

      def getScore = {
        EmissionState.knownStates.map{state => matFunction(EmissionState.stringToState(state)).get(sequenceA.size, sequenceB.size)}.max
      }

      def getSeqA = sequenceA

      def getSeqB = sequenceB
    }
  }
}
