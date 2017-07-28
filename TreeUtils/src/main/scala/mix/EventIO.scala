package main.scala.mix

import java.io.{File, PrintWriter}

import main.scala.stats.{Event}

import scala.collection.mutable.{ArrayBuffer, HashMap, Set}
import scala.io.Source

/**
  * convert a file of event strings to an array of events
  */
object EventIO {

  /**
    * convert a 'allEvents' file into an array of Events
    * @param allEventsFile a file with the event string first, followed by the count and proportions
    * @param sample the sample name to use
    * @return an array of events
    */
  def readEventsObject(allEventsFile: File, sample: String): EventContainer = {

    val eventToNumber = new HashMap[String, Int]()
    val eventToCount = new HashMap[String, Int]()
    val numberToEvent = new HashMap[Int, String]()
    val eventToPositions = new HashMap[String, Set[Int]]()

    eventToNumber("NONE") = 0
    eventToNumber("UNKNOWN") = 0
    numberToEvent(0) = "NONE"

    var linesProcessed = 0
    var nextIndex = 1

    val builder = ArrayBuffer[Event]()

    Source.fromFile(allEventsFile).getLines().drop(1).zipWithIndex.foreach { case (line, index) => {
      val lineTks = line.split("\t")

      val eventTokens = lineTks(0).split("_")

      // count the events
      eventTokens.foreach { case (event) => eventToCount(event) = eventToCount.getOrElse(event, 0) + lineTks(2).toInt }

      eventTokens.zipWithIndex.foreach { case (evt, index) => {
        eventToPositions(evt) = eventToPositions.getOrElse(evt, Set[Int]()) + index
      }}

      val eventNumbers = eventTokens.map { evt => {
        if (eventToNumber contains evt) {
          eventToNumber(evt)
        } else {
          eventToNumber(evt) = nextIndex
          numberToEvent(nextIndex) = evt
          nextIndex += 1
          eventToNumber(evt)
        }
      }}

      val evt = Event(lineTks(0).split("_"), eventNumbers, lineTks(2).toInt, lineTks(3).toDouble, sample, "N" + linesProcessed)
      linesProcessed += 1
      builder += evt
    }
    }

    // being lazy here -> for some reason scala really wants the internal sets to be immutable and I don't care
    val eventsToPosImmut = eventToPositions.map{case(key,values) => (key,values.toSet)}

    val evtArray = builder.toArray
    new EventContainerImpl(sample,evtArray,eventToCount,eventsToPosImmut,numberToEvent,eventToNumber,evtArray(0).events.size)
  }

  /**
    * rescale occurrence values for MIX to the range of characters it accepts
    *
    * @param value the value to rescale
    * @param min   the min value observed in the data
    * @param max   the max value in the data
    * @return the scaled value as a MIX recognized character
    */
  def scaleValues(value: Int, min: Int, max: Int): Char = {
    val maxLog = math.log(max)
    val valueLog = math.log(value)
    //println(maxLog + " " + valueLog + " ")
    val ret = scala.math.round(((valueLog.toDouble - min.toDouble) / maxLog.toDouble) * (EventIO.characterArray.length.toDouble - 1.0)).toInt
    EventIO.characterArray(ret)
  }


  /**
    * write the files required by the PHYLIP mix tool
    *
    * @param mixPackage the description of the files to write
    * @return an array of events that match the events that went into the tree
    */
  def writeMixPackage(mixPackage: MixFilePackage, eventsContainer: EventContainer) = {
    val weightFile = new PrintWriter(mixPackage.weightsFile)
    val mixInputFile = new PrintWriter(mixPackage.mixIntputFile)

    // normalize the weights to the range of values we have
    val maxCount = eventsContainer.eventToCount.values.max

    // -----------------------------------------------------------------------------------
    // map the each of the events to associated weights in PHYLIP space and write to disk
    // -----------------------------------------------------------------------------------
    val weights = (1 until eventsContainer.eventToCount.size).map {
      case (index) => {
        scaleValues(eventsContainer.eventToCount(eventsContainer.numberToEvent(index)), 0, maxCount)
      }
    }.toArray

    weightFile.write(weights.mkString("") + "\n")
    weightFile.close()

    var outputBuffer = Array[String]()
    eventsContainer.events.foreach { evt => {
      val outputStr = evt.toMixString(eventsContainer.eventToCount.size)
      if (!outputStr._2) {
        outputBuffer :+= outputStr._1
      }
    }
    }

    mixInputFile.write((outputBuffer.size) + "\t" + (eventsContainer.eventToCount.size - 3) + "\n")
    outputBuffer.foreach { case (str) => {
      mixInputFile.write(str + "\n")
    }
    }

    mixInputFile.close()
  }

  // scale the values from counts to characters (0-9, A-Z)
  val characterArray = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
}




