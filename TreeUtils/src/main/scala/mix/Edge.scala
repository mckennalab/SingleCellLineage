package main.scala.mix

import scala.collection.mutable.ArrayBuffer

/**
  * Created by aaronmck on 7/20/17.
  */
case class Edge(from: String, to: String, changes: Boolean, treeNumber: Int) {
  var chars = new ArrayBuffer[Char]()

  def addChars(inputString: String): Unit = {
    inputString.foreach{char => if (char != ' ') chars += char}
  }

  def toFancyString = from + "," + to + "," + treeNumber + "," + chars.mkString("")
}
