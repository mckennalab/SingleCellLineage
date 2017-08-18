package main.scala.dp

import main.scala.{Aligner, Alignment}
import main.scala.matrix.{ScoreMatrix, TracebackMatrix}
import main.scala.states.{EmissionState, GapA, GapB, Matched}

/**
  * A
  */
class NeedlemanWunschAffine(sequenceA: String,
                            sequenceB: String,
                            matchScore: Double,
                            mismatchScore: Double,
                            gapOpen: Double,
                            gapExtend: Double) extends Aligner {

  // a score that prevents use of the cell
  val reallyLow = -10000000.0

  // our alignments
  val matchMatrix = new ScoreMatrix(sequenceA.size + 1, sequenceB.size + 1)
  val gapAMatrix = new ScoreMatrix(sequenceA.size + 1, sequenceB.size + 1)
  val gapBMatrix = new ScoreMatrix(sequenceA.size + 1, sequenceB.size + 1)

  (0 until sequenceA.size + 1).foreach{ index1 => {
    matchMatrix.set(index1,0,gapOpen + (gapExtend * (index1 - 1)))
    gapAMatrix.set(index1,0,gapOpen + (gapExtend * (index1 - 1)))
    gapBMatrix.set(index1,0,reallyLow)
  }}
  (1 until sequenceB.size + 1).foreach{ index2 => {
    matchMatrix.set(0,index2,gapOpen + (gapExtend * (index2 - 1)))
    gapAMatrix.set(0,index2,reallyLow)
    gapBMatrix.set(0,index2,gapOpen + (gapExtend * (index2 - 1)))
  }}
  matchMatrix.set(0,0,0.0)

  val traceM = new TracebackMatrix(sequenceA.size + 1, sequenceB.size + 1)
  val traceGA = new TracebackMatrix(sequenceA.size + 1, sequenceB.size + 1)
  val traceGB = new TracebackMatrix(sequenceA.size + 1, sequenceB.size + 1)


  // fill in the score matrix
  (1 until sequenceA.size + 1).foreach { indexA => {
    (1 until sequenceB.size + 1).foreach { indexB => {

      val matchedScore = if (sequenceA(indexA - 1) == sequenceB(indexB - 1)) matchScore else mismatchScore

      { // match matrix setup
        val matchScores = Array[Double](
          matchMatrix.get(indexA - 1, indexB - 1) + (matchedScore),
          gapAMatrix.get( indexA - 1, indexB - 1) + (matchedScore),
          gapBMatrix.get( indexA - 1, indexB - 1) + (matchedScore))

        val max = matchScores.max
        val index = matchScores.indexOf(max)
        matchMatrix.set(indexA, indexB, max)
        traceM.set(indexA,indexB,index match {case 0 => Matched; case 1 => GapA; case 2 => GapB})
      }

      { // gapB matrix -- we do the opposite of the Durbin book, as they keep track of insertions, we keep track of deletions
        val gapBScores = Array[Double](
          matchMatrix.get(indexA - 1, indexB) + (gapOpen),
          gapBMatrix.get( indexA - 1, indexB) + (gapExtend))

        val max = gapBScores.max
        val index = gapBScores.indexOf(max)
        gapBMatrix.set(indexA, indexB, max)
        traceGB.set(indexA,indexB,index match {case 0 => Matched; case 1 => GapB})
      }

      { // gapA matrix-- we do the opposite of the Durbin book, as they keep track of insertions, we keep track of deletions
        val gapAScores = Array[Double](
          matchMatrix.get(indexA, indexB - 1) + (gapOpen),
          gapAMatrix.get( indexA, indexB - 1) + (gapExtend))

        val max = gapAScores.max
        val index = gapAScores.indexOf(max)
        gapAMatrix.set(indexA, indexB, max)
        traceGA.set(indexA,indexB,index match {case 0 => Matched; case 1 => GapA})
      }
    }}
  }}

  val emissionMap: Map[EmissionState,ScoreMatrix] = EmissionState.knownStates.map{state => state match {
    case GapA => (GapA,matchMatrix) // gapAMatrix)
    case GapB => (GapB,matchMatrix) // gapBMatrix)
    case Matched => (Matched,matchMatrix)
    case _ => throw new IllegalStateException("Uknown emmission state: " + state)
  }}.toMap

  val traceMap: Map[EmissionState,TracebackMatrix] = EmissionState.knownStates.map{state => state match {
    case GapA => (GapA,traceGA)
    case GapB => (GapB,traceGB)
    case Matched => (Matched,traceM)
    case _ => throw new IllegalStateException("Uknown emmission state: " + state)
  }}.toMap

  def emissionMapping(state: EmissionState): ScoreMatrix = emissionMap(state)
  def traceMapping(state: EmissionState): TracebackMatrix = traceMap(state)

  override def alignment: Alignment = TracebackMatrix.tracebackGlobalAlignment(sequenceA,sequenceB,emissionMapping,traceMapping)
}
