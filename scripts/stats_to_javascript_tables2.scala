// generate the files needed for the d3 plots
import scala.io._
import java.io._
import scala.collection.mutable._
import scala.math._

// -----------------------------------------------------------------------------
// a container for HMIDs
// -----------------------------------------------------------------------------
sealed trait IndelType {
  val toInt = -1
  def toStr(): String
}

case object Deletion extends IndelType { override val toInt = 1; def toStr(): String = "D"}
case object Match extends IndelType { override val toInt = 0; def toStr(): String = "M"}
case object Insertion extends IndelType { override val toInt = 2; def toStr(): String = "I"}
case object NoneType extends IndelType { override val toInt = 3; def toStr(): String = "NONE"}
case object Scar extends IndelType { override val toInt = 4; def toStr(): String = "S"}

// various data storage classes we have
case class Cutsite(sequence: String, start: Int, cutsite:Int)
case class Reference(name: String, sequence: String, primer1: String, primer2:String)

// HMID events
case class HMID(events: Array[Event]) {
  val stringRep = events.map{evt => evt.toStringRep()}.mkString("_")
  var count = 0
  def isWT(): Boolean = events.map{evt => if (evt.classOf == NoneType) 0 else 1}.sum == 0

  // for the histogram in the D3 plots
  def eventToPerBase(referenceLength: Int): Array[Int] = {
    val eventInts = Array.fill[Int](referenceLength)(0)
    events.foreach{evt => {
      (evt.position until (evt.position + evt.size)).foreach{pos => {
        if (pos >= 0 && pos < referenceLength)
          eventInts(pos) = evt.classOf.toInt
      }}
    }}
    return eventInts
  }

  // do we have a non-WT event over the target interval?
  def activeEventOverlap(start: Int, end: Int): Boolean = {
    events.map{evt =>
      if (((evt.position >= start && evt.position <= end) ||
        ((evt.position + evt.size) >= start && (evt.position + evt.size)<= end)) ||
        (evt.position <= start && (evt.position + evt.size) >= end))
        1 else 0
    }.sum > 0
  }

  // convert the events to a format that the D3 plotting understands
  def eventToETPB(referenceLength: Int): Array[ETPB] = {
    var eventPBs = Array[ETPB]()

    events.foreach{evt => {
      if (evt.classOf != NoneType) {

        // if the type is not NONE, pad check to see if we need to pad with NONEs to
        // 1) the beginning if we're the first event, or
        // 2) pad to the last event if there's a gap, then add the event
        if (eventPBs.size == 0 && evt.position > 0)
          eventPBs :+= ETPB(0, evt.position, 0)
        else if (eventPBs.size > 0 && eventPBs(eventPBs.size - 1).stop < evt.position)
          eventPBs :+= ETPB(eventPBs(eventPBs.size - 1).stop, evt.position, 0)

        eventPBs :+= ETPB(evt.position, evt.position + evt.size, evt.classOf.toInt)
      }
    }}

    // pad to the end: if we didn't have any non-NONE events, pad for the whole read with a NONE
    if (eventPBs.size > 0 && eventPBs(eventPBs.size - 1).stop < referenceLength) {
      eventPBs :+= ETPB(eventPBs(eventPBs.size - 1).stop, referenceLength, 0)
    } else if (eventPBs.size == 0) {
      eventPBs :+= ETPB(0, referenceLength, 0)
    }
    return eventPBs
  }
}

// a simple case class for our intervals
case class ETPB(start: Int, stop: Int, event: Int)

// -----------------------------------------------------------------------------
// our main event class, which handles individual event entries
// -----------------------------------------------------------------------------
case class Event(site: Int, size: Int, classOf: IndelType, bases: Option[String], position: Int) {
  def toStringRep(): String = if (classOf == NoneType) "NONE" else size + classOf.toStr() + "+" + position + (if (bases.isDefined) ("+" + bases.get) else "")
}

object Event {
  def toEvent(substr: String, site: Int): Event = {
    val tokens = substr.split("\\+")
    if (tokens.length == 1)
      return Event(site,0,NoneType,None,-1)

    val size = tokens(0).slice(0,tokens(0).length -1).toInt
    val typeOf = tokens(0).slice(tokens(0).length-1,tokens(0).length) match {
      case "D" => Deletion
      case "I" => Insertion
      case "M" => Match
      case "S" => Scar
      case _ => throw new IllegalStateException("Unknown type >" + tokens(0).slice(tokens(0).length-1,tokens(0).length) + "<")
    }
    Event(site,size, typeOf,if(tokens.length == 3) Some(tokens(2)) else None, tokens(1).toInt)
  }
}

// -----------------------------------------------------------------------------
// a container for HMIDs
// -----------------------------------------------------------------------------
class StatsFile(inputFile: String, unknownsAsNone: Boolean) {
  val statsFile = Source.fromFile(inputFile).getLines()
  val hmidCounts = new HashMap[String,HMID]()
  val header = statsFile.next().split("\t")
  
  // setup a bunch of of ways to index the target names
  val numberOfTargets = header.filter{tk => tk contains "target"}.foldLeft(0)((b,a) =>
    if (b > a.stripPrefix("target").toInt) b else a.stripPrefix("target").toInt)

  println("Number of targets " + numberOfTargets)

  val targetStrings = (1 until (numberOfTargets + 1)).map{"target" + _}
  val targetToPosition = targetStrings.map{case(tg) => (tg,header.indexOf(tg))}.toMap
  val targetToNumber = targetStrings.map{case(tg) => (tg,tg.slice(tg.length-1,tg.length).toInt)}.toMap

  // process all lines in the file
  statsFile.foreach{line => {
    if ((line contains "PASS") && !(line contains "WT") && (!(line contains "UNKNOWN") || unknownsAsNone)) { 
      val (newHMIDString,newHMID) = lineToHMID(line, unknownsAsNone)
      val replacementHMID = hmidCounts.getOrElse(newHMIDString,newHMID)
      replacementHMID.count += 1
      hmidCounts(newHMIDString) = replacementHMID
    } else {
      // println("Dropping line: " + line)
    }
  }}

  val totalHMIDs = hmidCounts.map{case(str,id) => id.count}.sum
  val sortedEvents = hmidCounts.toSeq.sortBy(_._2.count).toArray.reverse
  println("total stat file events to process: " + totalHMIDs)

  /** process a line into an HMID **/
  def lineToHMID(line: String, unknownsToNone: Boolean = true): Tuple2[String,HMID] = {
    val spl = line.split("\t")

    val events = new ArrayBuffer[Event]()
    val seenEvents = new HashMap[String,Boolean]()
    val tokens = new ArrayBuffer[String]()

    targetStrings.foreach{case(tg) => {
      val toAdd = if (spl(targetToPosition(tg)) == "UNKNOWN" && unknownsToNone) "NONE" else spl(targetToPosition(tg))
      tokens += toAdd
      spl(targetToPosition(tg)).split("\\&").foreach{
        subevt => {
          val converteSubEvt = if (subevt == "UNKNOWN" && unknownsToNone) "NONE" else subevt
          if (!(seenEvents contains subevt)) {
            events += Event.toEvent(converteSubEvt,targetToNumber(tg)) // -- make sure we only propigate NONEs, not UNKNOWNs
            seenEvents(subevt) = true
          } 
        }
      }
    }}
    (tokens.mkString("_"),HMID(events.toArray))
  }

}
// -----------------------------------------------------------------------------
// store the cutsites
// -----------------------------------------------------------------------------
class CutSiteContainer(cutSiteFile:String) {
  val csFile = Source.fromFile(cutSiteFile).getLines()
  val header = csFile.next()

  val sites = csFile.map{case(line) => {
    val sp = line.split("\t")
    Cutsite(sp(0), sp(1).toInt, sp(2).toInt)
  }}.toArray

}

// -----------------------------------------------------------------------------
// process the input files
// -----------------------------------------------------------------------------
if (!(args.size == 9 || args.size == 10))
  throw new IllegalArgumentException("Not the correct number of arguments, we want 9 or 10, we saw " + args.size)

val unknownsAsNone = args(7).toUpperCase match {
  case "TRUE" => true
  case "FALSE" => false
  case _ => throw new IllegalArgumentException("Unable to determine the unknown reads -> true parameter")
}

val topXevents = 500
val statsObj = new StatsFile(args(0),unknownsAsNone)
val cutSites = new CutSiteContainer(args(5))
val referenceLength = Source.fromFile(args(8)).getLines().drop(1).map{line => line.size}.sum

case class HighlightRegion(start: Int, end: Int, color: String)
var highlighedRegions: Option[Array[HighlightRegion]] = None
if (args.size == 10) {
  highlighedRegions = Some(Source.fromFile(args(9)).getLines().drop(1).map{line => {
    val sp = line.split("\t")
    HighlightRegion(sp(1).toInt,sp(2).toInt,sp(3))
  }}.toArray)
}
// -----------------------------------------------------------------------------
// now create output files
// -----------------------------------------------------------------------------
val perBaseEvents = new PrintWriter(args(2))
val occurances    = new PrintWriter(args(1))
val readCounts    = new PrintWriter(args(3))
val allEventsF    = new PrintWriter(args(4))
val perBaseEventsNew = new PrintWriter(args(6))
// -----------------------------------------------------------------------------
// first output all of the events
// -----------------------------------------------------------------------------
allEventsF.write("event\tarray\tcount\tproportion\n")
statsObj.sortedEvents.zipWithIndex.foreach{case((hmid,hmidEvents),index) =>
  allEventsF.write(hmid + "\t" + index + "\t" + hmidEvents.count + "\t" + (hmidEvents.count.toDouble / statsObj.totalHMIDs.toDouble) + "\n")
}
allEventsF.close()

// -----------------------------------------------------------------------------
// now output the top events
// -----------------------------------------------------------------------------
val wt_colors = Array[String]("#888888", "#00FF00")

readCounts.write("event\tarray\tproportion\trawCount\tWT\thighlightMembership\n")
statsObj.sortedEvents.slice(0,topXevents).zipWithIndex.foreach{case((hmid,hmidEvents),index) => {
  var color = if(hmidEvents.isWT) wt_colors(1) else wt_colors(0)
  var highlightMembership = Array[String]()
  highlighedRegions.foreach{ray => ray.foreach{region => {
    if (hmidEvents.activeEventOverlap(region.start,region.end)) {
      color = region.color
      highlightMembership :+= region.start + "-" + region.end + "_" + region.color.stripPrefix("#").stripPrefix("\"").stripSuffix("\"")
    }
  }}}

  val hlString = if (highlightMembership.size == 0) "NONE" else highlightMembership.mkString(",")

  readCounts.write(hmid + "\t" + index + "\t" + (hmidEvents.count.toDouble / statsObj.totalHMIDs.toDouble) + "\t" + hmidEvents.count + "\t" + color + "\t" + hlString + "\n")
}}
readCounts.close()

// -----------------------------------------------------------------------------
// output the top events as a melted string of 0s, 1s, and 2s (encoded indels)
// -----------------------------------------------------------------------------
perBaseEvents.write("array\tposition\tevent\n")
statsObj.sortedEvents.slice(0,topXevents).zipWithIndex.foreach{case((hmid,hmidEvents),index) => {
  hmidEvents.eventToPerBase(referenceLength).zipWithIndex.foreach{case(event,subIndex) =>
    perBaseEvents.write(index + "\t" + subIndex + "\t" + event + "\n")
  }
}}
perBaseEvents.close()

// -----------------------------------------------------------------------------
// create a simple table of the events
// -----------------------------------------------------------------------------

perBaseEventsNew.write("array\tstart\tend\tevent\n")
statsObj.sortedEvents.slice(0,topXevents).zipWithIndex.foreach{case((hmid,hmidEvents),index) => {
  hmidEvents.eventToETPB(referenceLength).zipWithIndex.foreach{case(etpb,subIndex) =>
    perBaseEventsNew.write(index + "\t" + math.max(0,etpb.start) + "\t" + math.min(referenceLength-1,etpb.stop) + "\t" + etpb.event + "\n")
  }
}}
perBaseEventsNew.close()

// -----------------------------------------------------------------------------
// output per-base information
// -----------------------------------------------------------------------------
val insertionCounts = Array.fill[Int](referenceLength)(0)
val deletionCounts = Array.fill[Int](referenceLength)(0)
val scarCounts = Array.fill[Int](referenceLength)(0)

var totalReads = 0
statsObj.sortedEvents.foreach{case(hmid,hmidEvents) => {
  hmidEvents.eventToPerBase(referenceLength).zipWithIndex.foreach{case(event,index) => event match {
    case Deletion.toInt => deletionCounts(index) += hmidEvents.count
    case Insertion.toInt => insertionCounts(index) += hmidEvents.count
    case Scar.toInt => scarCounts(index) += hmidEvents.count
    case _ => {}
  }}
  totalReads += hmidEvents.count
}}

occurances.write("index\tmatch\tinsertion\tdeletion\tscar\n")
insertionCounts.zip(deletionCounts).zipWithIndex.foreach{case((ins,del),index) => {
  val matchProp = 1.0 - ((ins + del).toDouble / totalReads.toDouble)
  occurances.write(index + "\t" + matchProp + "\t" + (ins.toDouble/totalReads.toDouble) + "\t" + (del.toDouble/totalReads.toDouble) + "\t" + (scarCounts(index).toDouble/totalReads.toDouble) + "\n")
}}
occurances.close()
