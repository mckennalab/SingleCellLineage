package main.scala.mix

import main.scala.stats.Event

import scala.collection.mutable.HashMap

/**
  * the basic event container trait. everything we need for creating a tree from events
  */
trait EventContainer {
  def sample: String
  def events: Array[Event]
  def eventToCount: HashMap[String, Int]
  def numberToEvent: HashMap[Int, String]
  def eventToNumber: HashMap[String, Int]
  def numberOfTargets: Int
}

/**
  * store all the relevant information about the editing outcomes for MIX
  *
  * @param mevents        the events as an array
  * @param meventToCount  the unique individual site events to their counts
  * @param mnumberToEvent the index of a single event to it's string representation
  * @param meventToNumber convert an event to it's index
  */
class EventContainerImpl(msample: String,
                     mevents: Array[Event],
                     meventToCount: HashMap[String, Int],
                     mnumberToEvent: HashMap[Int, String],
                     meventToNumber: HashMap[String, Int],
                     mnumberOfTargets: Int) extends EventContainer {

  def sample: String = msample
  def events: Array[Event] = mevents
  def eventToCount: HashMap[String, Int] = meventToCount
  def numberToEvent: HashMap[Int, String] = mnumberToEvent
  def eventToNumber: HashMap[String, Int] = meventToNumber
  def numberOfTargets: Int = mnumberOfTargets

}

/**
  * storage for a subset of events over specific targets
  * @param mevents        the subsetted events
  * @param eventContainer the original events container
  * @param eventsToChildren   the mapping of new ID to old ID
  */
case class SubsettedEventContainer(mevents: Array[Event],
                                   eventContainer: EventContainer,
                                   eventsToChildren: HashMap[String, Array[String]]) extends EventContainer {

  def sample: String = eventContainer.sample
  def events: Array[Event] = mevents
  def eventToCount: HashMap[String, Int] = eventContainer.eventToCount
  def numberToEvent: HashMap[Int, String] = eventContainer.numberToEvent
  def eventToNumber: HashMap[String, Int] = eventContainer.eventToNumber
  def numberOfTargets: Int = eventContainer.numberOfTargets
}

object EventContainer {
  /**
    * this function takes an event container and subsets it based on specified events over specified sites. It only
    * looks at / outputs specified sites, so if you want a site included regardless of content add it as a wildcard
    *
    * @param container the event container to generate a subset for
    * @param sites     the sites that we look at and the specified genotype (wildcard for anything)
    * @return an event container
    */
  def subset(container: EventContainer, sites: Array[Tuple2[Int, String]], eventsToIDs: HashMap[String,Int], sample: String): EventContainer = {

    val strToken = "_"
    // our subset of events
    var newEvents = Array[String]()
    val eventsToChildren = new HashMap[String,Array[String]]()
    val eventsToCounts = new HashMap[String,Int]()
    val eventsToProps = new HashMap[String,Double]()

    // run over all the events in the previous container, making
    container.events.foreach { event => {
      var valid = true
      var newEvents = Array[String]()

      // create the new list of events
      sites.foreach{case(site,str) => {
        if (event.events(site) == str || str == wildcard)
          newEvents :+= event.events(site)
        else
          valid = false
      }}

      // if we're valid, add it to the pile with our id for later reconstruction
      if (valid) {
        val newTag = newEvents.mkString(strToken)
        newEvents :+= newTag
        var eventArray = eventsToChildren.getOrElse(newTag, Array[String]())
        eventsToChildren(newTag) = eventArray +: event.name
        eventsToCounts(newTag) = eventsToCounts.getOrElse(newTag,0) + event.count
        eventsToProps(newTag) = eventsToProps.getOrElse(newTag,0) + event.proportion
      }
    }}

    SubsettedEventContainer(eventsToChildren.map{case(evt,ids) => Event(evt.split(strToken),
      evt.split(strToken).map{st => eventsToIDs(st)},
      eventsToCounts(evt),
      eventsToProps(evt),
      sample,
      evt)}.toArray,container,eventsToChildren)
  }

  val wildcard = "*"
}
