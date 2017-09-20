package main.scala.stats

/**
  * parsed editing outcome events from a stats file
  */
case class Event(events: Array[String], eventNumbers: Array[Int], count: Int, proportion: Double, sample: String, name: String) {
  val builder = new StringBuilder()

  // make a padded version of the name
  val paddedName = name + (0 until (10 - name.length)).map{i => " "}.mkString("")

  // make a binary representation of the events
  def toMixString(index: Int): Tuple2[String,Boolean] = {
    var isWT = true
    val ret = paddedName + (1 until index).map{ind => {
      if (eventNumbers contains ind) {
        isWT = false
        "1"
      } else {
        "0"
      }
    }}.mkString("")
    return (ret,isWT)
  }

  def prettyString(): String = {
    events.mkString("-") + " " + eventNumbers.mkString("-") + " " + count + " " + proportion + sample + " " + name
  }
}