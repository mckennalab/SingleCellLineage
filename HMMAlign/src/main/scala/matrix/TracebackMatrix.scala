package main.scala.matrix

import main.scala.Alignment
import main.scala.states._

import scala.collection.mutable

/**
  * Created by aaronmck on 7/17/17.
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

    //println(sequenceA + " " + sequenceB)
    while (!(indexA == 0 && indexB == 0)) {
      tracebackFunc(currentBestMatrix).get(indexA, indexB) match {
        case x if indexA == 0 | x == GapA => {
          println("GapA - " + sequenceB(indexB - 1) + " seqA " + alignedStringA.result().mkString("") + " seqB " + alignedStringB.result().mkString(""))
          alignedStringA += '-'
          alignedStringB += sequenceB(indexB - 1)
          indexB -= 1
        }
        case x if indexB == 0 | x == GapB => {
          println("GapB " + sequenceA(indexA - 1) + " - " + " seqA " + alignedStringA.result().mkString("") + " seqB " + alignedStringB.result().mkString(""))
          alignedStringA += sequenceA(indexA - 1)
          alignedStringB += '-'
          indexA -= 1
        }
        case Matched => {
          println("Matched " + sequenceB(indexB - 1) + " " + sequenceA(indexA - 1) + " seqA " + alignedStringA.result().mkString("") + " seqB " + alignedStringB.result().mkString(""))
          alignedStringA += sequenceA(indexA - 1)
          alignedStringB += sequenceB(indexB - 1)
          indexA -= 1
          indexB -= 1
        }
      }
      currentBestMatrix = tracebackFunc(currentBestMatrix).get(indexA,indexB)
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
