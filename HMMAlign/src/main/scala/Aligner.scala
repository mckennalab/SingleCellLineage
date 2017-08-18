package main.scala

/**
  * the interface for producing alignments
  */
trait Aligner {
  def alignment(): Alignment
}
