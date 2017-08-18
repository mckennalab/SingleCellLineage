
package main.scala.states

sealed trait EmissionState { def str: String = "UNK" }
object EmissionState {
  val knownStates = Array[EmissionState](Matched,GapA,GapB)
}
// it's Matched instead of Match to avoid name conflicts with scala match
case object Matched extends EmissionState { override def str = "M" }

case object GapA extends EmissionState { override def str = "A" }

case object GapB extends EmissionState { override def str = "B" }

