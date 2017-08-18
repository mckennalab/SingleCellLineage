package main.scala.dp

import main.scala.{Aligner, Alignment}
import main.scala.matrix.{ScoreMatrix, TracebackMatrix}
import main.scala.states.{EmissionState, _}

import scala.collection.mutable

/**
  * The most basic dynamic programming alignment -- the Needleman Wunsch global alignment with linear indel score.
  */
class NeedlemanWunsch(sequenceA: String, sequenceB: String, matchScore: Double, mismatchScore: Double, delScore: Double) extends Aligner {

  val matrix = new ScoreMatrix(    sequenceA.size + 1, sequenceB.size + 1)
  val trace  = new TracebackMatrix(sequenceA.size + 1, sequenceB.size + 1)

  (1 until sequenceA.size + 1).foreach{ index1 => matrix.set(index1,0,matrix.get(index1 - 1, 0) + delScore)}
  (1 until sequenceB.size + 1).foreach{ index2 => matrix.set(0,index2,matrix.get(0, index2 - 1) + delScore)}

  // fill in the score matrix
  (1 until sequenceA.size + 1).foreach{ index1 => {
    (1 until sequenceB.size + 1).foreach { index2 => {

      val matchedScore = if (sequenceA(index1 - 1) == sequenceB(index2 - 1)) matchScore else mismatchScore

      val scores = Array[Double](
        matrix.get(index1 - 1, index2 - 1) + (matchedScore),
        matrix.get(index1, index2 - 1) + delScore,
        matrix.get(index1 - 1, index2) + delScore)

      val max = scores.max
      val index = scores.indexOf(max)

      matrix.set(index1, index2, max)

      index match {
        case 0 => trace.set(index1, index2, Matched)
        case 1 => trace.set(index1, index2, GapA)
        case 2 => trace.set(index1, index2, GapB)
      }
    }}
  }}

  val emissionMap = EmissionState.knownStates.map{state => (state,matrix)}.toMap
  val traceMap = EmissionState.knownStates.map{state => (state,trace)}.toMap
  def emissionMapping(state: EmissionState): ScoreMatrix = emissionMap(state)
  def traceMapping(state: EmissionState): TracebackMatrix = traceMap(state)

  override def alignment: Alignment = TracebackMatrix.tracebackGlobalAlignment(sequenceA,sequenceB,emissionMapping,traceMapping)
}
