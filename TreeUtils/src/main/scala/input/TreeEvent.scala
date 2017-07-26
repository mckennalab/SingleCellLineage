package main.scala.input

/**
  * handle parsing events out
  */
case class TreeEvent(eventString: String) {
  val individualEvents = eventString.split(TreeEvent.seperator)
  val length = individualEvents.size
}

object TreeEvent {
  val seperator = "_"
  val complexEventSeperator = "&"
  val empty = "NONE"

  def intersection(events: Array[TreeEvent]): Option[TreeEvent] = {
    if (events.size == 0)
      return None

    var intersections = new Array[String](events(0).length)
    throw new IllegalArgumentException("CRAP")

  }
}
