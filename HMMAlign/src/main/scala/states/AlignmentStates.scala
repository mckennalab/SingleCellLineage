
package main.scala.states

trait EmissionState {
  def str: String = "UNK"
  val distance: Int = 1
}

// it's Matched instead of Match to avoid name conflicts with scala match
case class Matched(override val distance: Int = 1) extends EmissionState { override def str = "M" }

case class GapA(override val distance: Int = 1) extends EmissionState { override def str = "A" }

case class GapB(override val distance: Int = 1) extends EmissionState { override def str = "B" }

object Matched {
  def apply():Matched = Matched(1)
}
object GapA {
  def apply():GapA = GapA(1)
}
object GapB {
  def apply():GapB = GapB(1)
}

object EmissionState {
  val knownStates = Array[EmissionState](Matched(),GapA(),GapB())
}