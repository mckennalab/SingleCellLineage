package main.scala

import main.scala.matrix.TracebackMatrix
import main.scala.states.EmissionState

/**
  * stores the result of an alignment
  */
trait Alignment {

  def global:   Boolean
  def getStart: Int
  def getStop:  Int
  def getAlignmentString: Tuple2[String,String]
  def getScore: Double
  def getSeqA:  String
  def getSeqB:  String

}
