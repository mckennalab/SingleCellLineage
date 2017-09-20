package main.scala.mix

import main.scala.node.RichNode

/**
  * methods to collapse a parsimony tree down consistently
  */
object ParsimonyCollapser {

  /**
    * Given a node (usually the root), start collapsing down matching internal nodes, multifurcating
    * previously bifurcating nodes as we go
    *
    * @param node the node to start our collapsing at, we only consider it and it's children
    */
  def collapseNodes(node: RichNode) {
    var stillRefining = true

    while(stillRefining) {
      var newChildren = Array[RichNode]()
      stillRefining = false
      node.children.foreach { case (child) => {
        // check to see if the grandchildren are grafted on -- in that case we don't want to destroy this node
        val gchildsGrafted = child.children.foldLeft(false)((a,b) => b.graftedNode | a)
        if (gchildsGrafted == false && child.children.size > 0 && child.parsimonyGenotypeDistance(node) == 0) { // old logic
          println("****Collapsing out node " + child.name)
          // reconnect all the children-of-the-child nodes
          child.children.foreach(chd => newChildren :+= chd)
          stillRefining = true
          if (child.graftedNode)
            node.nodeColor = "green"
          else
            node.nodeColor = "yellow"
        } else {
          newChildren :+= child
        }
      }
      }
      node.children = newChildren
    }

    // depth first here -- update child list,
    // have them update their children lists,
    // and finally update annotations recursively
    node.children.foreach { chd => collapseNodes(chd) }
    node.resetChildrenAnnotations()
  }
}
