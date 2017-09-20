package main.scala.mix

import scala.collection.mutable.ArrayBuffer

/**
  * store the relationships between a parent and child node in the tree
  */
case class Edge(from: String, to: String, treeRoot: String) {
  var chars = new ArrayBuffer[Char]()

  def addChars(inputString: String): Unit = {
    inputString.foreach{char => if (char != ' ') chars += char}
  }

  def toFancyString = "(*(From: " + from + ", to: " + to + "," + chars.mkString("") + " root node: " + treeRoot + ")*)"
}
