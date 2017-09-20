package main.scala.mix

import main.scala.node.NodeLinker

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

/**
  * Parse the output from the PHYLIP package MIX and create
  * event strings for each of the internal nodes
  */
class MixParser(mixOutput: String, eventsToNumbers: EventContainer, treeToUse: Int, rootName: String) {

  // first parse out the events to number data, and make a look-up table
  val numberToEvent = eventsToNumbers.numberToEvent
  val eventToSites = new mutable.HashMap[String,Array[Int]]()
  // now load all the lines
  eventsToNumbers.events.foreach{evt => {
    evt.events.zipWithIndex.foreach{case(evtTk,index) => {
      var curArray = eventToSites.getOrElse(evtTk,Array[Int]())
      if (!(curArray contains index))
        curArray :+= index
      eventToSites(evtTk) = curArray
    }}
  }}

  // the header line we're looking for in the file is:
  val headerLine = "From    To     Any Steps?    State at upper node"

  val inputFile = Source.fromFile(mixOutput).getLines().toArray
  println(inputFile.mkString("\n"))
  var inGenotypeSection = false
  var currentGenotype: Option[Edge] = None
  var currentTreeNumber = 0

  var activeTree = new NodeLinker()

  { // scope this so the temp. data structures go away

    // a mapping from the input tree number to it's annotations
    var treeToGenotypes = new mutable.HashMap[Int, ArrayBuffer[Edge]]()

    inputFile.foreach { line => {
      // skip blank lines to make this easier, also skip the weird sub-header line they provide
      if (line != "" && !line.contains("means same as in the node below it on tree")) {
        if (line.startsWith(headerLine)) {
          inGenotypeSection = true
          if (currentTreeNumber == treeToUse) {
            treeToGenotypes(currentTreeNumber) = new ArrayBuffer[Edge]()
          }
        }
        else if (inGenotypeSection && (line.contains("yes") || line.contains("no"))) {
          if (currentTreeNumber == treeToUse) {
            if (currentGenotype.isDefined) {
              treeToGenotypes(currentTreeNumber) += currentGenotype.get
            }
            val sp = line.trim.split(" +")
            currentGenotype = Some(Edge(sp(0), sp(1), rootName))
            currentGenotype.get.addChars(sp.slice(3, sp.size).mkString(""))
          }
        }
        else if (inGenotypeSection && line.map{chr => if (chr == ' ' || chr == '1' || chr == '.') 0 else 1}.sum > 0) {
          inGenotypeSection = false
          if (currentGenotype.isDefined) {
            treeToGenotypes(currentTreeNumber) += currentGenotype.get
            println("tree size = " + treeToGenotypes(currentTreeNumber).size)
          }

          currentGenotype = None
          currentTreeNumber += 1
        }
        else if (inGenotypeSection) {
          if (currentTreeNumber == treeToUse) {
            val sp = line.trim.split(" +")
            currentGenotype.get.addChars(sp.slice(0, sp.size).mkString(""))
          }
        }
      }

    }
    }

    // close-out the remaining tree
    if (currentGenotype.isDefined) {
      treeToGenotypes(currentTreeNumber) += currentGenotype.get
      println("tree size = " + treeToGenotypes(currentTreeNumber).size)
    }
    currentGenotype = None
    currentTreeNumber += 1

    // TODO: move this out into it's own type
    treeToGenotypes(treeToUse).toArray.foreach(edge => activeTree.addEdge(edge))
    //treeToGenotypes(treeToUse).toArray.foreach{case(edg) => println(edg.from + " -> " + edg.to)}
  }


}
