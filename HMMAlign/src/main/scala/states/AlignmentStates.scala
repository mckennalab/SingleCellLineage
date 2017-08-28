
package main.scala.states

trait EmissionState { def str: String = "UNK" }


// it's Matched instead of Match to avoid name conflicts with scala match
case class Matched_(distance: Int = 1) extends EmissionState { override def str = "M" }

case class GapA_(distance: Int = 1) extends EmissionState { override def str = "A" }

case class GapB_(distance: Int = 1) extends EmissionState { override def str = "B" }

object Matched {
  def apply() = Matched_(1)
}
object GapA {
  def apply() = GapA_(1)
}
object GapB {
  def apply() = GapB_(1)
}

object EmissionState {
  val knownStates = Array[EmissionState](Matched,GapA,GapB)
}