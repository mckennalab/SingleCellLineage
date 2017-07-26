
package main.scala.states

sealed trait EmissionState { def str: String }

// it's Matched instead of Match to avoid name conflicts with scala match
case object Matched extends EmissionState { def str = "M" }

case object GapA extends EmissionState { def str = "I" }

case object GapB extends EmissionState { def str = "D" }

