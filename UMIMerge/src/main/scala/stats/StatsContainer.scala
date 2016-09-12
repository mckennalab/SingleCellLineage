package stats

/**
  * Created by aaronmck on 9/11/16.
  */
// case class that must be filled out to output data into a stats file.  It enforces type checking
// which is nice, and formats the strings for output
class StatsContainer(name: String, keep: Boolean, isConflicted: Boolean, hasFWDPrimer: Boolean, hasREVPrimer: Boolean, isUMI: Boolean,
                          isMerged: Boolean, read1Len: Int, read2Len: Int, finalRead1Count: Int,
                          finalRead2Count: Int, matchRate1: Double, matchRate2: Double, alignedBases1: Int,
                          alignedBases2: Int, targetEvents: Array[String], targetSequences: Array[String],
                          alignedFWDRead: Option[String], alignedRVSRead: Option[String], mergedRead: Option[String],
                          alignedFWDReadRef: Option[String], alignedRVSReadRef: Option[String], mergedReadRef: Option[String]) {

  // check that the target events and target sequence counts are the same
  if (targetEvents.size != targetSequences.size)
    throw new IllegalStateException("The target event array and target sequence array are not the same length: " + targetEvents.size + " and " + targetSequences.size)

  // create an output string version of the target data
  val targetEventString = targetEvents.mkString(StatsOutput.sepString)
  val targetSequenceString = targetSequences.mkString(StatsOutput.sepString)

  // get rid of bad characters in any read name
  def convertName(readName: String): String = readName.replace(' ', '_')

  // convert the keep boolean to string
  def convertKeep(keepB: Boolean): String = if (keepB) "PASS" else "FAIL"

  // convert the merged boolean to string
  def convertMerged(mergedB: Boolean): String = if (mergedB) "MERGED" else "PAIR"

  // convert the umi boolean tag to a string
  def convertUMI(umiB: Boolean): String = if (umiB) "UMI" else "AMPLICON"

  // convert the umi boolean tag to a string
  def convertConflicted(conflicted: Boolean): String = if (conflicted) "CONFLICTED" else "CONSISTENT"

  def createReadStrings(): String = {
    val rdFwd = alignedFWDRead.getOrElse("NA")
    val rdRev = alignedRVSRead.getOrElse("NA")
    val rdMrg = mergedRead.getOrElse("NA")
    val rdFwdRef = alignedFWDReadRef.getOrElse("NA")
    val rdRevRef = alignedRVSReadRef.getOrElse("NA")
    val rdMrgRef = mergedReadRef.getOrElse("NA")
    rdFwd + StatsOutput.sepString + rdRev + StatsOutput.sepString + rdMrg + StatsOutput.sepString +
      rdFwdRef + StatsOutput.sepString + rdRevRef + StatsOutput.sepString + rdMrgRef
  }

  // our final output string
  def outputString(outputReadStrings: Boolean) =
    convertName(name) + StatsOutput.sepString +
      convertKeep(keep) + StatsOutput.sepString +
      convertConflicted(isConflicted) + StatsOutput.sepString +
      hasFWDPrimer + StatsOutput.sepString +
      hasREVPrimer + StatsOutput.sepString +
      convertUMI(isUMI) + StatsOutput.sepString +
      convertMerged(isMerged) + StatsOutput.sepString +
      read1Len + StatsOutput.sepString +
      read2Len + StatsOutput.sepString +
      finalRead1Count + StatsOutput.sepString +
      finalRead2Count + StatsOutput.sepString +
      finalRead2Count + StatsOutput.sepString +
      matchRate1 + StatsOutput.sepString +
      matchRate2 + StatsOutput.sepString +
      alignedBases1 + StatsOutput.sepString +
      alignedBases2 +
      (if (outputReadStrings) {StatsOutput.sepString + createReadStrings} else {""}) + StatsOutput.sepString +
      targetEventString + StatsOutput.sepString + targetSequenceString
}
