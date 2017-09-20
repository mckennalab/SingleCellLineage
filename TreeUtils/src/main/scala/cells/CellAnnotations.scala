package main.scala.cells

import java.io._

import main.scala.node.RichNode

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

class CellAnnotations(cellFile: File) {

  // load up the annotation file
  val cells = Source.fromFile(cellFile).getLines()

  // find the target events
  val tokens = cells.next().split("\t").zipWithIndex.filter{case(tks,i) => tks contains "target"}.map{case(t,i) => (t.stripPrefix("target").toInt,i)}.toMap
  val maxToken = tokens.keys.max


  tokens.foreach{case(index,pos) => println(index + " i ---> p " + pos)}
  println("Max token " + maxToken)

  // get the cells loaded up
  val cellBuffer = new ArrayBuffer[CellAnnotation]()
  println("loading annotations from " + cellFile.getAbsolutePath)

  cells.foreach{cl => {
    val sp = cl.split("\t")
    if (sp(1) == "PASS" && sp.size >= maxToken) {
      try {
        val eventString = (0 until maxToken).map { e => sp(tokens(e + 1)) }.mkString("-")
        cellBuffer += CellAnnotation(sp(0), eventString, sp(2).toInt)
      } catch {
        case e: Exception => {println("Failed on line " + sp.mkString("*")); throw e}
      }
    } else {
      println("dropping cell " + sp(0))
    }
  }}

  var allCells = cellBuffer.toArray

  /**
    * find cells that match the cell ID
    * @param eventString the event string to look up
    */
  def findMatchingCells(eventString: String): Array[CellAnnotation] = {
    allCells.filter{cell => {
      if (cell.eventString == eventString) {
        if (cell.isMatchedToTerminalNode)
          println("Cell " + cell.name + " is already matched!!")
        cell.isMatchedToTerminalNode = true
        true
      } else false
    }}
  }

  /**
    * print any cells we haven't matched yet
    */
  def printUnmatchedCells(): Unit = {
    allCells.foreach{cell =>
      if (!cell.isMatchedToTerminalNode)
        println("Cell " + cell.name + " with event string " + cell.eventString + " IS UNMATCHED!")
    }
  }

}

case class CellAnnotation(name: String, eventString: String, clade: Int = -1) {
  var isMatchedToTerminalNode = false
}