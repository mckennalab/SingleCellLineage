package main.scala.node

import main.scala.mix.Edge

/**
  * A simple container that stores links between nodes in a tree
  */
class NodeLinker {
  private var arrayOfConnections = Array[Edge]()
  private var maximumMixNode = 0

  def getMaximumInternalNode = maximumMixNode

  def addEdge(edge: Edge): Unit = {
    // check that we don't already have this edge in our graph
    arrayOfConnections.foreach { existingEdge => {
      if (existingEdge.from == edge.from && existingEdge.to == edge.to)
        throw new IllegalStateException("That edge already exists in the tree!: " + edge.toFancyString + " and existing " + existingEdge.toFancyString)
    }
    }
    if (edge.to.matches(NodeLinker.intRegex) && edge.to.toInt > maximumMixNode)
      maximumMixNode = edge.to.toInt
    if (edge.from.matches(NodeLinker.intRegex) && edge.from.toInt > maximumMixNode)
      maximumMixNode = edge.from.toInt

    arrayOfConnections :+= edge
  }

  def addEdges(linker: NodeLinker): Unit = {
    // check that we don't already have this edge in our graph
    linker.arrayOfConnections.foreach { edge => {
      addEdge(edge)
    }}
  }

  def findEdge(from: String, to: String): Edge = {
    arrayOfConnections.foreach(edge =>
      if (edge.from == from && edge.to == to)
        return edge
    )
    throw new IllegalStateException("Unable to find edge")
  }

  def findEdgeWithRoot(from: String, to: String, root: String): Edge = {
    arrayOfConnections.foreach(edge =>
      if (edge.from == from && edge.to == to && edge.treeRoot == root)
        return edge
    )
    throw new IllegalStateException("Unable to find edge")
  }

  def lookupFroms(fromNode: String): List[Edge] = {
    arrayOfConnections.filter { case (mp) => mp.from == fromNode }.toList
  }

  def lookupTos(toNode: String): List[Edge] = {
    val ret = arrayOfConnections.filter { case (mp) => mp.to == toNode }.toList
    if (ret.size != 1)
      throw new IllegalStateException("Found " + ret.size + " edges for node " + toNode)
    ret
  }

  /**
    * MIX always makes internal nodes with the same names (integers starting at 1(. Here we shift those names by some amount
    * to allow multiple trees to be merged together.
    *
    * @param offset the offset to move internal nodes up by (specify 10 to move the 1 to 11).
    * @param rootName the new root name
    */
  def shiftEdges(offset: Int, rootName: String): Unit = {
    arrayOfConnections = arrayOfConnections.map{edge => {
      var fromNode = edge.from

      // our nodes all start with N, MIX nodes are whole integers
      if (fromNode.matches(NodeLinker.intRegex))
        fromNode = (fromNode.toInt + offset).toString
      else if (fromNode == "root")
        fromNode = rootName

      var toNode   = edge.to
      if (toNode.matches(NodeLinker.intRegex))
        toNode = (toNode.toInt + offset).toString
      else if (toNode == "root")
        toNode = rootName

      if (fromNode != edge.from || toNode != edge.to)
        println("renaming " + edge.toFancyString + " to " + Edge(fromNode, toNode, rootName).toFancyString)
      Edge(fromNode, toNode, rootName)
    }}
  }
}

object NodeLinker {
  val intRegex = """(\d+)"""
}