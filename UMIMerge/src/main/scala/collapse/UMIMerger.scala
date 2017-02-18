package collapse

import java.io._

import aligner.Aligner
import reads._

import reads.SequencingRead

/**
  * handles the process of merging down read with matching UMIs
  */
object UMIMerger {

  def mergeTogether(umi: String,
                    readsF: List[SequencingRead],
                    readsR: List[SequencingRead],
                    readsFCount: Int,
                    readsRCount: Int,
                    outputFastq1: PrintWriter,
                    outputFastq2: PrintWriter,
                    primers: List[String],
                    sample: String,
                    minSurvivingReads: Int,
                    index: Int,
                    aligner: Aligner): UMIMergePairResult = {


    // some constants we should probably bubble-up
    val minReadLength = 30
    val minMeanQualScore = 30.0
    val debug = false

    // use MSA to align all the reads
    val preparedFWD = Consensus.prepareConsensus(readsF.toArray, minReadLength, minMeanQualScore)
    val preparedREV = Consensus.prepareConsensus(readsR.toArray, minReadLength, minMeanQualScore)

    if (preparedFWD.size > 1 && preparedREV.size > 1) {

      val mergedF = aligner.alignTo(preparedFWD, None)
      val mergedR = aligner.alignTo(preparedREV, None)

      // remove the reads that are a really poor match
      val fwdCleanedUp = Consensus.removeMismatchedReads(mergedF)
      val revCleanedUp = Consensus.removeMismatchedReads(mergedR)

      if (fwdCleanedUp.size > 1 && revCleanedUp.size > 1) {

        // make a consensus from the remaining 'good' reads
        val fwdConsensus = SequencingRead.stripDownToJustBases(Consensus.consensus(fwdCleanedUp, "ConsensusFWD"))
        val revConsensus = SequencingRead.stripDownToJustBases(Consensus.consensus(revCleanedUp, "ConsensusREV"))

        revConsensus.reverseCompAlign = true

        outputFastq1.write(fwdConsensus.toFastqString(umi + "FWD" + "_" + fwdCleanedUp.size + "_" + readsFCount, false, index, 0) + "\n")
        outputFastq2.write(revConsensus.toFastqString(umi + "REV" + "_" + revCleanedUp.size + "_" + readsRCount, false, index, 0) + "\n")

        return UMIMergePairResult(fwdConsensus.bases, revConsensus.bases, readsF.size, readsR.size, fwdCleanedUp.size, revCleanedUp.size)
      }

    }
    return UMIMergePairResult(SequencingRead.stripDownToJustBases(Consensus.consensus(preparedFWD)).bases,
      SequencingRead.stripDownToJustBases(Consensus.consensus(preparedREV)).bases,
      readsF.size, readsR.size, 0, 0)
  }

  def mergeTogetherSingleReads(umi: String,
                               readsF: List[SequencingRead],
                               readsFCount: Int,
                               outputFastq1: PrintWriter,
                               primers: List[String],
                               sample: String,
                               minSurvivingReads: Int,
                               index: Int,
                               aligner: Aligner): UMIMergeResult = {


    // some constants we should probably push up
    val minReadLength = 30
    val minMeanQualScore = 30.0
    val debug = false

    // use MSA to align all the reads
    val preparedFWD = Consensus.prepareConsensus(readsF.toArray, minReadLength, minMeanQualScore)

    if (preparedFWD.size > 1) {

      val mergedF = aligner.alignTo(preparedFWD, None)

      // remove the reads that are a really poor match
      val fwdCleanedUp = Consensus.removeMismatchedReads(mergedF, 0.9, minReadLength)

      if (fwdCleanedUp.size > 1) {

        // make a consensus from the remaining 'good' reads
        val fwdConsensus = SequencingRead.stripDownToJustBases(Consensus.consensus(fwdCleanedUp, "ConsensusFWD"))

        if (debug)
          if (readsFCount > minSurvivingReads)
            println("WRITING candidate " + fwdCleanedUp.size + " umi " + umi)

        outputFastq1.write(fwdConsensus.toFastqString(umi + "FWD" + "_" + fwdCleanedUp.size + "_" + readsFCount, false, index, 0) + "\n")
        return UMIMergeResult(fwdConsensus.bases, readsF.size, fwdCleanedUp.size)
      } else {
        if (debug)
          if (readsFCount > minSurvivingReads)
            println("Dropping candidate " + fwdCleanedUp.size + " umi " + umi)
      }

    } else {
      if (debug)
        if (readsFCount > minSurvivingReads)
          println("Dropping poor candidate " + preparedFWD.size + " umi " + umi)
    }

    if (debug)
      if (readsFCount > minSurvivingReads)
        println("DIdn't even try poor candidate " + preparedFWD.size + " umi " + umi + " readsFCount " + readsFCount)

    return UMIMergeResult(SequencingRead.stripDownToJustBases(Consensus.consensus(preparedFWD)).bases, readsF.size, 0)

  }

  def getFailureStatus(readsKept: Double, fwdMatchBase: Double, revMatchBase: Option[Double], fwdPrimer: Boolean, revPrimer: Boolean): String = {
    var failureReason = ""
    if (readsKept < 0.5) failureReason += "notEnoughReadsRemaining;"
    if (fwdMatchBase < 0.85) failureReason += "tooManyForwardMismatches;"
    if (revMatchBase.getOrElse(1.0) < 0.85) failureReason += "tooManyForwardMismatches;"
    if (!fwdPrimer) failureReason += "forwardPrimerMissing;"
    if (!revPrimer) failureReason += "reversePrimerMissing;"

    if (failureReason == "")
      failureReason = "PASS"
    failureReason
  }

}

case class UMIMergePairResult(read1Consensus: String, read2Consensus: String, read1InputCount: Int, read2InputCount: Int, read1SurvivingCount: Int, read2SurvivingCount: Int)

case class UMIMergeResult(readConsensus: String, readInputCount: Int, readSurvivingCount: Int)