package aligner

import reads.SequencingRead

/**
  * a basic approach to aligning multiple reads -- just assume that all of the reads the same length and find the
  * consensus along the length of the read
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
