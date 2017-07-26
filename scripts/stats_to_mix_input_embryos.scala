
// ------------------------------------------------------------------------------------------------------------------------------
// now that we have the event buffer filled in, we want to adaptively filter to a managable number of events to run mix with
val allEvents = eventsBuffer.toList

// count down until we have over 400 events
var cutOff = 0
(0 until 15).foreach{cut => {
  val total = allEvents.map{evt => if (evt.count >= cut) 1 else 0}.sum
  if (total > 300) {
    cutOff = cut
    filteringNumber.write(cut + "\n")
  }
  println(cut + "\t" + total)
  
}}
filteringNumber.close()

val newEventList = allEvents.filter(evt => evt.count >= cutOff).toArray

println("Processed " + linesProcessed + " lines with size " + newEventList.size)
val keyToTag = new HashMap[String,String]()
val keyToCount = new HashMap[String,Int]()


// scale the values from counts to characters (0-9, A-Z)
val characterArray = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

// normalize the weights to the range of values we have
val maxCount = eventToCount.values.max

// this function scales the observed counts to weights accepted by PHYLIP 
def scaleValues(value: Int, min: Int, max: Int): Char = {
  val maxLog = math.log(max)
  val valueLog = math.log(value)
  //println(maxLog + " " + valueLog + " ")
  val ret = scala.math.round(((valueLog.toDouble - min.toDouble) / maxLog.toDouble) * (characterArray.length.toDouble - 1.0)).toInt
  characterArray(ret)
}

// -----------------------------------------------------------------------------------
// map the each of the events to associated weights in PHYLIP space and write to disk
// -----------------------------------------------------------------------------------
val weights = (1 until nextIndex).map{
  case(index) => {
    scaleValues(eventToCount(numberToEvent(index)),0,maxCount)
  }
}.toArray

weightFile.write(weights.mkString("") + "\n")
weightFile.close()

// -----------------------------------------------------------------------------------
// for each event, output the name and events
// -----------------------------------------------------------------------------------

annotationFile.write("taxa\tsample\tcount\tproportion\tevent\tcellIDs\n")

var outputBuffer = Array[String]()
newEventList.toArray.foreach{evt => {
  val outputStr = evt.toMixString(nextIndex)
  if (!outputStr._2) {
    outputBuffer :+= outputStr._1
    annotationFile.write(evt.name + "\t" + evt.sample + "\t" + evt.count + "\t" + evt.proportion + "\t" + evt.events.mkString("_") + "\n")
  }
}}

annotationFile.close()

output.write((outputBuffer.size) + "\t" + (nextIndex - 3) + "\n")
outputBuffer.foreach{case(str) => {
  output.write(str + "\n")
}}

annotationFile.close()
output.close()

eventsToNumbers.write("event\tnumber\tpositions\n")
eventToNumber.foreach{case(event,number) => {
  val eventToPos = eventToPositions.getOrElse(event,Set[Int]()).mkString(",")
  eventsToNumbers.write(event + "\t" + number + "\t" + eventToPos + "\n")
}}
eventsToNumbers.close()

// we just make a single-entry file for the embryos
sampleToClade.write("sample\tclade\tcolor\n")
sampleToClade.write(sampleName + "\t1\t#9EC2E9\n")
sampleToClade.close()
