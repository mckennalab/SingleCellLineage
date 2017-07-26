package main.scala.matrix

import main.scala.states._

/**
  * Created by aaronmck on 7/17/17.
  */
class TracebackMatrix (dimX: Int, dimY: Int) {

  val values = Array.ofDim[EmissionState](dimX, dimY)

  def set(rowPos: Int, colPos: Int, value: EmissionState) {values(rowPos)(colPos) = value}
  def get(rowPos: Int, colPos: Int) : EmissionState = values(rowPos)(colPos)

}

