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

  // a score that prevents use of the cell -- should be smaller than anything we can produce through alignment
  val reallyLow = -10000000.0

  // our alignments
  val matchMatrix = new ScoreMatrix(sequenceA.size + 1, sequenceB.size + 1)
  val insertAMatrix = new ScoreMatrix(sequenceA.size + 1, sequenceB.size + 1)
  val insertBMatrix = new ScoreMatrix(sequenceA.size + 1, sequenceB.size + 1)

  // initialize the scoring matrices
  (0 until sequenceA.size + 1).foreach{ index1 => {
    matchMatrix.set(index1,0,gapOpen + (gapExtend * (index1 - 1)))
    insertAMatrix.set(index1,0,gapOpen + (gapExtend * (index1 - 1)))
    insertBMatrix.set(index1,0,reallyLow)
  }}
  (0 until sequenceB.size + 1).foreach{ index2 => {
    matchMatrix.set(0,index2,gapOpen + (gapExtend * (index2 - 1)))
    insertAMatrix.set(0,index2,reallyLow)
    insertBMatrix.set(0,index2,gapOpen + (gapExtend * (index2 - 1)))
  }}
  matchMatrix.set(0,0,0.0)
  insertAMatrix.set(0,0,reallyLow)
  insertBMatrix.set(0,0,reallyLow)

  // traceback the best path
  val traceBest = new TracebackMatrix(sequenceA.size + 1, sequenceB.size + 1)


  // fill in the score matrix
  (1 until sequenceA.size + 1).foreach { indexA => {
    (1 until sequenceB.size + 1).foreach { indexB => {

      val matchedScore = if (sequenceA(indexA - 1) == sequenceB(indexB - 1)) matchScore else mismatchScore

      { // match matrix setup
        val matchScores = Array[Double](
          matchMatrix.get(indexA - 1, indexB - 1) + (matchedScore),
          insertAMatrix.get( indexA - 1, indexB - 1) + (matchedScore),
          insertBMatrix.get( indexA - 1, indexB - 1) + (matchedScore))

        val max = matchScores.max
        val index = matchScores.indexOf(max)
        matchMatrix.set(indexA, indexB, max)
      }

      { // gapB matrix -- we do the opposite of the Durbin book, as they keep track of insertions, we keep track of deletions
        val gapBScores = Array[Double](
          matchMatrix.get(indexA - 1, indexB) + (gapOpen),
          insertBMatrix.get(indexA - 1, indexB) + (gapExtend))

        val max = gapBScores.max
        val index = gapBScores.indexOf(max)
        insertBMatrix.set(indexA, indexB, max)
      }

      { // gapA matrix-- we do the opposite of the Durbin book, as they keep track of insertions, we keep track of deletions
        val gapAScores = Array[Double](
          matchMatrix.get(indexA, indexB - 1 ) + (gapOpen),
          insertAMatrix.get( indexA, indexB - 1) + (gapExtend))

        val max = gapAScores.max
        val index = gapAScores.indexOf(max)
        insertAMatrix.set(indexA, indexB, max)
      }

      // now record the best traceback at this location
      val traceDir = Array[Double](matchMatrix.get(indexA, indexB),insertAMatrix.get(indexA, indexB),insertBMatrix.get(indexA, indexB))
      val index = traceDir.indexOf(traceDir.max) match {case 0 => Matched; case 1 => GapA; case 2 => GapB}
      traceBest.set(indexA,indexB,index)
    }}
  }}

  val emissionMap: Map[EmissionState,ScoreMatrix] = EmissionState.knownStates.map{state => state match {
    case GapA => (GapA,insertAMatrix) // insertAMatrix)
    case GapB => (GapB,insertBMatrix) // insertBMatrix)
    case Matched => (Matched,matchMatrix)
    case _ => throw new IllegalStateException("Unknown emission state: " + state)
  }}.toMap

  val traceMap: Map[EmissionState,TracebackMatrix] = EmissionState.knownStates.map{state => state match {
    case GapA => (GapA,traceBest)
    case GapB => (GapB,traceBest)
    case Matched => (Matched,traceBest)
    case _ => throw new IllegalStateException("Unknown emission state: " + state)
  }}.toMap

  def emissionMapping(state: EmissionState): ScoreMatrix = emissionMap(state)
  def traceMapping(state: EmissionState): TracebackMatrix = traceMap(state)

  override def alignment: Alignment = TracebackMatrix.tracebackGlobalAlignment(sequenceA,sequenceB,emissionMapping,traceMapping)
}
