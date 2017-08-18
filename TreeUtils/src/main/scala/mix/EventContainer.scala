package main.scala.mix

import main.scala.stats.Event

import scala.collection.mutable.HashMap





/**
  * storage for a subset of events over specific targets
  *
  * @param mevents          the subsetted events
  * @param eventContainer   the original events container
  * @param eventsToChildren the mapping of new ID to old ID
  */
case class SubsettedEventContainer(mevents: Array[Event],
                                   eventContainer: EventContainer,
                                   eventsToChildren: HashMap[String, Array[String]]) extends EventContainer {

  def sample: String = eventContainer.sample

  def events: Array[Event] = mevents

  def eventToCount: HashMap[String, Int] = eventContainer.eventToCount

  def numberToEvent: HashMap[Int, String] = eventContainer.numberToEvent

  def eventToSites: HashMap[String, Set[Int]] = eventContainer.eventToSites

  def eventToNumber: HashMap[String, Int] = eventContainer.eventToNumber

  def numberOfTargets: Int = eventContainer.numberOfTargets
}


