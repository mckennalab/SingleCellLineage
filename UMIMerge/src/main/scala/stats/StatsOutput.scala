package stats

import java.io._

/**
 * this class handles outputting a statistics file for runs of the umi/non-umi amplicon sequencing
 */
class StatsOutput(outputFile: File, targetCount: Int, outputReadStrings: Boolean = true) {

  // setup the output file
  val output = new PrintWriter(outputFile.getAbsolutePath)

  // output the header
  output.write(StatsOutput.standardHeaderItems(outputReadStrings) +
    StatsOutput.sepString +
    StatsOutput.createTargetString(targetCount) +
    StatsOutput.endline)

  /**
   * output a stats entry object to the stats file
    *
    * @param container a filled out container with read statistics
   */
  def outputStatEntry(container: StatsContainer): Unit = {
    output.write(container.outputString(outputReadStrings) + StatsOutput.endline)
  }

  def close() = output.close()
}


/**
 * our static methods for generating non-stateful aspects of the stats file
 */
object StatsOutput {
  def sepString = "\t"

  def endline = "\n"

  // return the standard header items that all stats files have
  val standardHeaderItemsArray = Array[String]("readName", "keep", "conflict", "hasForwardPrimer", "hasReversePrimer"
    , "umi", "merged", "mergedReadLen", "read1len",
    "read2len", "finalReadCount1", "finalReadCount2", "matchRate1",
    "matchRate2", "alignedBases1", "alignedBases2")

  // if we're outputing read alignment strings we include these entries
  val standardHeaderItemsArrayAddition = Array[String]("fwdRead","revRead","mergedRead","fwdReadRef","revReadRef","mergedReadRef")

  // get the output header strings
  def standardHeaderItems(outputReadStrings: Boolean) = {
    if (outputReadStrings) {
      (standardHeaderItemsArray ++ standardHeaderItemsArrayAddition).mkString(sepString)
    } else {
      (standardHeaderItemsArray).mkString(sepString)
    }
  }

  // create the target specific strings at the top of the file
  def createTargetString(numberOfTargets: Int): String = {
    (1 until (numberOfTargets + 1)).map { i => "target" + i }.toArray[String] ++
      (1 until (numberOfTargets + 1)).map { i => "sequence" + i }.toArray[String]
  }.mkString(sepString)
}

