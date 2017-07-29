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

  def eventToSites: HashMap[String, Set[Int]]

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
                         meventToSites: HashMap[String, Set[Int]],
                         mnumberToEvent: HashMap[Int, String],
                         meventToNumber: HashMap[String, Int],
                         mnumberOfTargets: Int) extends EventContainer {

  def sample: String = msample

  def events: Array[Event] = mevents

  def eventToCount: HashMap[String, Int] = meventToCount

  def eventToSites: HashMap[String, Set[Int]] = meventToSites

  def numberToEvent: HashMap[Int, String] = mnumberToEvent

  def eventToNumber: HashMap[String, Int] = meventToNumber

  def numberOfTargets: Int = mnumberOfTargets

}

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

object EventContainer {
  /**
    * this function takes an event container and subsets it based on specified events over specified sites. It only
    * looks at / outputs specified sites, so if you want a site included regardless of content add it as a wildcard
    *
    * @param container the event container to generate a subset for
    * @param sites     the sites that we look at and the specified genotype (wildcard for anything)
    * @return an event container
    */
  def subset(container: EventContainer, sites: Array[Tuple2[Int, String]], eventsToIDs: HashMap[String, Int], sample: String): Tuple2[EventContainer, HashMap[String, Array[String]]] = {

    val strToken = "_"
    // our subset of events
    var newEvents = Array[String]()
    val namesToEvents = new HashMap[String, String]()
    val nameToChildren = new HashMap[String, Array[String]]()
    val eventsToCounts = new HashMap[String, Int]()
    val eventsToProps = new HashMap[String, Double]()

    // run over all the events in the previous container, making
    var partialID = 0
    container.events.foreach { event => {
      var valid = true
      var newEvents = Array.fill[String](event.events.size)("NONE")

      // create the new list of events
      sites.foreach { case (site, str) => {
        val siteList = if (str == wildcard) collection.immutable.Set[Int](site) else container.eventToSites(str)

        if (event.events(site) == str || str == wildcard)
          siteList.foreach{coveredSite => newEvents(coveredSite) = event.events(coveredSite)}
        else
          valid = false
      }
      }

      // fill in the rest of

      // if we're valid, add it to the pile with our id for later reconstruction
      if (valid) {
        partialID += 1
        val newTag = newEvents.mkString(strToken)
        newEvents :+= newTag
        val name = "P" + partialID
        namesToEvents(name) = newTag

        var eventNames = nameToChildren.getOrElse(newTag, Array[String]())
        nameToChildren(name) = eventNames :+ event.name
        eventsToCounts(newTag) = eventsToCounts.getOrElse(newTag, 0) + event.count
        eventsToProps(newTag) = eventsToProps.getOrElse(newTag, 0.0) + event.proportion
      }
    }
    }


    (SubsettedEventContainer(nameToChildren.map { case (name, ids) => {
      val evt = namesToEvents(name)
      Event(evt.split(strToken),
        evt.split(strToken).map { st => eventsToIDs(st) },
        eventsToCounts(evt),
        eventsToProps(evt),
        sample,
        name)
    }
    }.toArray, container, nameToChildren), nameToChildren)
  }

  def subsetByChildren(container: EventContainer, children: Array[String], rootID: String): EventContainer = {

    // run over all the events in the previous container, making
    val newEvents = container.events.filter { event => children contains event.name}

    new EventContainerImpl(container.sample, newEvents, container.eventToCount, container.eventToSites, container.numberToEvent, container.eventToNumber, container.numberOfTargets)


  }

  val wildcard = "*"
}
