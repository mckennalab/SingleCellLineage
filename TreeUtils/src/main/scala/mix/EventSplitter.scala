package main.scala.mix

import main.scala.stats.Event

import scala.collection.mutable.HashMap

/**
  * Event split -- given an input file, split the events into an initial tree
  *
  */
class EventSplitter(eventContainer: EventContainer, firstXSites: Int, sample: String) {

  // split the events into the root for the first X sites,
  // and sub-tree for individual nodes
  val sites = (0 until firstXSites).map{id => (id,EventContainer.wildcard)}.toArray

  val rootTree = EventContainer.subset(eventContainer, sites, eventContainer.eventToNumber, sample)

  // TODO: run the root tree, and fetch the results

  // TODO: now for each subtree process the tree

  // TODO: now graft the children onto the appropriate node



}


/**
  * store all the relevant information about the editing outcomes for MIX
  * @param events the events as an array
  * @param eventToCount the unique individual site events to their counts
  * @param numberToEvent the index of a single event to it's string representation
  * @param eventToNumber convert an event to it's index

case class EventContainer(sample: String,
                          events: Array[Event],
                          eventToCount : HashMap[String, Int],
                          numberToEvent: HashMap[Int,String],
                          eventToNumber: HashMap[String, Int],
                          numberOfTargets: Int)

*/

