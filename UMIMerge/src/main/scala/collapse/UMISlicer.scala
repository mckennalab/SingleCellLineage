package collapse

trait UMISlicer {
  def slice(read1: String, read1Quals: String, read2: Option[String], read2Quals: Option[String]): SlicedReadSet
}


object UMISlicer {
  def getSlicer(umiPosition: Int, umiLength: Int): UMISlicer = {
    if (umiPosition >= 0)
      return new ForwardSlicer(umiPosition,umiLength)
    else
      return new ReverseSlicer((-1 * umiPosition) - 1, umiLength)
  }
}

/**
  * a sliced-out UMI representation
  * @param read1 the first read, with the UMI sliced out (if it was in the first read)
  * @param umi the UMI sequence
  * @param read2 the second read, if it exists, with the UMI sliced out (if it was in the first read)
  */
case class SlicedReadSet(read1: String, read1Qual: String, umi: String, umiQual: String, read2: Option[String], read2Qual: Option[String])

/**
  * slice UMIs from forward (R1) reads
  * @param startPosition the starting position
  * @param length the length of the UMI
  */
class ForwardSlicer(startPosition: Int, length: Int) extends UMISlicer {
  val startPos = startPosition
  val len = length

  override def slice(read1: String, read1Quals: String, read2: Option[String], read2Quals: Option[String]): SlicedReadSet = {
    assert(read1.size >= startPos + len, "The read length of " + read1.size + " is too short to extract a " + len + "-base UMI at position " + startPos)

    val umi = read1.slice(startPos,startPos + length)
    val umiQual = read1Quals.slice(startPos,startPos + length)
    val remaining = read1.slice(0,startPos) + read1.slice(startPos + length, read1.length)
    val remainingQual = read1Quals.slice(0,startPos) + read1Quals.slice(startPos + length, read1.length)

    SlicedReadSet(remaining,remainingQual,umi,umiQual, read2, read2Quals)
  }
}

/**
  * slice UMIs from the reverse read (R2)
  * @param startPositionRev the starting position on the reverse read
  * @param length the length of the UMI
  */
class ReverseSlicer(startPositionRev: Int, length: Int) extends UMISlicer {
  val startPosRev = startPositionRev
  val len = length

  override def slice(read1: String, read1Quals: String, read2: Option[String], read2Quals: Option[String]): SlicedReadSet = {
    assert(read2.isDefined,"We need a read 2 to split out a UMI from read 2")
    val read2Seq = read2.get
    val read2Qual = read2Quals.get
    assert(read2Seq.size >= startPosRev + len, "The read length of " + read1.size + " is too short to extract a " + len + "-base UMI at position " + startPosRev)

    val umi = read2Seq.slice(startPosRev,startPosRev + length)
    val umiQual = read2Qual.slice(startPosRev ,startPosRev + length)
    val remaining = read2Seq.slice(0,startPosRev) + read2Seq.slice(startPosRev + length, read1.length)
    val remainingQual = read2Qual.slice(0,startPosRev) + read2Qual.slice(startPosRev + length, read1.length)

    SlicedReadSet(read1,read1Quals,umi,umiQual, Some(remaining), Some(remainingQual))
  }
}

