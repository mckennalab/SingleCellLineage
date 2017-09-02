package main.scala.dp

import main.scala.{Aligner, Alignment}
import main.scala.matrix.{ScoreMatrix, TracebackMatrix}
import main.scala.states.{EmissionState, GapA, GapB, Matched}

class ConvexDP(sequenceA: String, sequenceB: String, matchScore: Double, mismatchScore: Double, gapOpen: Double, gapExtend: Double) extends Aligner {

  val matrix = new ScoreMatrix(sequenceA.size + 1, sequenceB.size + 1)
  val trace = new TracebackMatrix(sequenceA.size + 1, sequenceB.size + 1)

  (1 until sequenceA.size + 1).foreach { index1 => matrix.set(index1, 0, -1.0 * ConvexDP.scoreDistance(index1, gapOpen, gapExtend)) }
  (1 until sequenceB.size + 1).foreach { index2 => matrix.set(0, index2, -1.0 * ConvexDP.scoreDistance(index2, gapOpen, gapExtend)) }

  (1 until sequenceA.size + 1).foreach { index1 => {
    (1 until sequenceB.size + 1).foreach { index2 => {

      val matchedScore = if (sequenceA(index1 - 1) == sequenceB(index2 - 1)) matchScore else mismatchScore

      val bestGapAIndex = matrix.maxIndex(index1, index2, false)
      val bestGapBIndex = matrix.maxIndex(index1, index2, true)

      assert(index1 - bestGapAIndex > 0)
      assert(index2 - bestGapBIndex > 0)

      val scores = Array[Double](
        matrix.get(index1 - 1, index2 - 1) + (matchedScore),
        matrix.get(bestGapAIndex, index2) - ConvexDP.scoreDistance(index1 - bestGapAIndex, gapOpen, gapExtend),
        matrix.get(index1, bestGapBIndex) - ConvexDP.scoreDistance(index2 - bestGapBIndex, gapOpen, gapExtend))

      val max = scores.max
      val index = scores.lastIndexOf(max)

      matrix.set(index1, index2, max)

      index match {
        case 0 => trace.set(index1, index2, Matched(1))
        case 1 => trace.set(index1, index2, GapB(index1 - bestGapAIndex))
        case 2 => trace.set(index1, index2, GapA(index2 - bestGapBIndex))

      }
    }
    }
  }
  }

  val emissionMap = EmissionState.knownStates.map { state => (state, matrix) }.toMap
  val traceMap = EmissionState.knownStates.map { state => (state, trace) }.toMap

  def emissionMapping(state: EmissionState): ScoreMatrix = emissionMap(state.str)

  def traceMapping(state: EmissionState): TracebackMatrix = traceMap(state.str)

  override def alignment: Alignment = TracebackMatrix.tracebackGlobalAlignment(sequenceA, sequenceB, emissionMapping, traceMapping)
}

object ConvexDP {

  def scoreDistance(distance: Int, gapOpen: Double, gapExt: Double): Double = {
    distance match {
      case 0 => throw new IllegalStateException("Shouldn't see distance")
      case 1 => gapOpen
      case x if x > 1 => gapOpen + (Math.log(distance) * gapExt.toDouble)
      case _ => throw new IllegalStateException("Unhandled distance: " + distance)
    }
  }
}