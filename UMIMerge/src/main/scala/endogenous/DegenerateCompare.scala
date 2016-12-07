package endogenous

/**
  * transform a read into a record of the degeneracy,
  */
class DegenerateCompare(degenerateSequence: String, offsetIntoRead: Int) {
  val degenSeq = degenerateSequence
  val offset = offsetIntoRead

  // find the N positions in the string -- these are our degenerate bases
  val degeneratePositions = degenSeq.zipWithIndex.map{case(ch,index) => if (ch == 'N') index else -1}.filter{case(ind) => ind >= 0}

  def readToStat(cellRead: String, libraryRead: String): DegenStat = {
    val cellSubStr = cellRead.slice(offset,offset + degenerateSequence.size)
    val libSubStr = cellRead.slice(offset,offset + degenerateSequence.size)

    var degenBases = 0
    var mismatchedBases = 0
    var matches = 0
    cellSubStr.zip(libSubStr).zipWithIndex.foreach{case((cellBase,libBase),index) => {
      if (cellBase == libBase) {
        matches += 1
      } else {
        if (degeneratePositions contains index) {
          degenBases += 1
        } else {
          mismatchedBases += 1
        }
      }
    }}

    DegenStat(degenBases, mismatchedBases, matches, matches.toDouble / degenerateSequence.size.toDouble, libSubStr)
  }
}

case class DegenStat(degenerateBases: Int, mismatchedOtherBases: Int, mathces: Int, matchLibrary: Double, libRep: String)