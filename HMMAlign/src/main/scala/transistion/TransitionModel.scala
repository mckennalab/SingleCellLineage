package main.scala.transistion

import main.scala.states.{GapB, EmissionState, GapA, Matched}

/**
  * a model of the transitions between states in a hidden markov model
  */
class TransitionModel {

  // the transitions
  val values = Array.ofDim[Double](3, 3)

  val emissionLookup = Map(Matched().str -> 0, GapA().str -> 1, GapB().str -> 2)

  initialize()

  def transition(from: EmissionState, to: EmissionState): Double = {
    values(emissionLookup(from.str))(emissionLookup(to.str))
  }

  private def initialize(): Unit = {
    // match to all
    values(0)(0) = 0.95
    values(0)(1) = 0.01
    values(0)(2) = 0.04

    // insertion to all
    values(1)(0) = 0.10
    values(1)(1) = 0.90
    values(1)(2) = 0.00

    // deletion to all
    values(2)(0) = 0.10
    values(2)(1) = 0.00
    values(2)(2) = 0.90
  }

}
