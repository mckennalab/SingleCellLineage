package aligner

import reads.SequencingRead

/**
  * a basic approach to aligning multiple reads -- don't actually do it.  just assume that all of the reads the same length and
  * pass back the reads as-is.  For illumina reads from the same UMI, this generally works well and is super fast
  *
  */
object BasicAligner extends Aligner {

  /**
    * align a series of reads by the consensus base at each position
    * @param reads an array of reads
    * @param ref the reference sequence (if provided), we don't use it here
    * @param debug should we dump out debugging information
    * @return a sequence of aligned reads
    */
  override def alignTo(reads: Array[SequencingRead], ref: Option[String], debug: Boolean): Array[SequencingRead] = reads
}
