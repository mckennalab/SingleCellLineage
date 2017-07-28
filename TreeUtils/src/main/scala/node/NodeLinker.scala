package main.scala.node

import main.scala.mix.Edge

/**
  * A simple container that stores links between nodes in a tree
  */
class NodeLinker {
  private var arrayOfConnections = Array[Edge]()

  def addEdge(edge: Edge): Unit = {
    // check that we don't already have this edge in our graph
    arrayOfConnections.foreach { existingEdge => {
      if (existingEdge.from == edge.from && existingEdge.to == edge.to)
        throw new IllegalStateException("That edge already exists in the tree!: " + edge.toFancyString + " and existing " + existingEdge.toFancyString)
    }
    }
    arrayOfConnections :+= edge
  }

  def addEdges(linker: NodeLinker): Unit = {
    // check that we don't already have this edge in our graph
    linker.arrayOfConnections.foreach { edge => {
      arrayOfConnections.foreach { existingEdge => {
        if (existingEdge.from == edge.from && existingEdge.to == edge.to)
          throw new IllegalStateException("That edge already exists in the tree!: " + edge.toFancyString + " and existing " + existingEdge.toFancyString)
      }}
      arrayOfConnections :+= edge
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
      if (edge.from == from && edge.to == to)
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


}
