package main.scala.annotation

import java.io.File

import main.scala.mix.EventContainer
import main.scala.node.RichNode

import scala.collection.mutable.HashMap
import scala.io.Source

/**
  * load up all the annotation files, and make a set of mappings
  * for each node name to a set of annotations.  On request,
  * annotate each node with it's appropriate info
  */
class AnnotationsManager(evtContainer: EventContainer) {
  val seperator = "\t"
  val eventSeperator = "_"

  val annotationMapping = new HashMap[String,AnnotationEntry]()
  val cladeMapping = new HashMap[String,CladeEntry]()
  val sampleTotals = new HashMap[String,Int]()

  var eventDefinitionsToColors : Option[HashMap[String,Array[String]]] = None

  evtContainer.events.foreach{evt => {
    println("adding event with " + evt.events.size)
    annotationMapping(evt.name) = AnnotationEntry("UNKOWN",evt.name,evt.count,evt.proportion,evt.events)
    cladeMapping(evt.name) = CladeEntry(evtContainer.sample,"ALL","black")
  }}


  // *******************
  // deal with the optional clade assignment color matching
  // TODO: put optional clade identities back in

  /**
    * lookup the clade color for this event; if there isn't one return black, our default
    *
    * @param node the event node
    * @return a color string
    */
  def setNodeColor(node: RichNode, parentNode: Option[RichNode]): Tuple2[String,String] = {
    if (!eventDefinitionsToColors.isDefined)
      return ("nodecolor","black")

    var assigned_colors = Array[String]()
    eventDefinitionsToColors.get.foreach{case(color,arrayOfEvents) => {
      val containCount = arrayOfEvents.map{case(chkEvt) => if (node.parsimonyEvents contains chkEvt) 1 else 0}.sum

      var parentContains = 0
      if (parentNode.isDefined)
        parentContains = arrayOfEvents.map{case(chkEvt) => if (parentNode.get.parsimonyEvents contains chkEvt) 1 else 0}.sum

      // the second part of this expression is to deal with Jamie's clade choices
      if (containCount == arrayOfEvents.size && (node.children.size > 0 || parentContains == arrayOfEvents.size)) {
        assigned_colors :+= color
      }
    }}

    // check for conflict, only assign if it's one color
    if (assigned_colors.size == 1)
      return ("nodecolor",assigned_colors(0))
    return ("nodecolor","black")
  }

}
