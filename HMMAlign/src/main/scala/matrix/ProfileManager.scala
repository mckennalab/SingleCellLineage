package main.scala.matrix

import main.scala.emission.Emission
import main.scala.transistion.TransitionModel

/**
  * manage our profile HMM aligner
  */
class ProfileManager(sequenceA: String, sequenceB: String) {

  // our data storage
  val insertions = new ScoreMatrix(sequenceA.size, sequenceA.size)
  val deletions  = new ScoreMatrix(sequenceA.size, sequenceA.size)
  val matches    = new ScoreMatrix(sequenceA.size, sequenceA.size)

  // our traceback matrix
  val traceback  = new TracebackMatrix(sequenceA.size, sequenceA.size)

  // get the transition model
  val transModel = new TransitionModel()

  // and the emission model
  val emit = new Emission()

  /**
    * forward algorithm
    */
  def forward(): Unit = {

  }


}
