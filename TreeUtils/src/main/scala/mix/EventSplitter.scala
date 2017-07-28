package main.scala.mix

import java.io.File

import main.scala.annotation.AnnotationsManager
import main.scala.node.RichNode
import main.scala.stats.Event

import scala.collection.mutable.HashMap

/**
  * split events into a pre and post pile, generate trees seperately for each, and
  * put the whole tree back together
  *
  * @param mixDir            the data directory
  * @param eventContainer    a container of events
  * @param firstXSites       the number of sites in the first half of the experiment
  * @param sample            the sample name
  * @param annotationMapping a mapping of nodes to their annotations
  */
object EventSplitter {

  // TODO: preserve the underlying name
  def splitInTwo(mixDir: File,
                 eventContainer: EventContainer,
                 firstXSites: Int,
                 sample: String,
                 annotationMapping: AnnotationsManager): RichNode = {

    // split the events into the root for the first X sites,
    // and sub-tree for individual nodes
    val sites = (0 until firstXSites).map {
      id => (id, EventContainer.wildcard)
    }.toArray

    val rootTreeContainer = EventContainer.subset(eventContainer, sites, eventContainer.eventToNumber, sample)


    // run the root tree, and fetch the results
    println("Processing the root tree...")
    val (rootNodeAndConnection,linker) = MixRunner.mixOutputToTree(MixRunner.runMix(mixDir, rootTreeContainer._1), rootTreeContainer._1, annotationMapping, "root")

    // now for each subtree, make a tree using just those events to be grafted onto the root tree
    // and graft the children onto the appropriate node
    val childToTree = new HashMap[String, RichNode]()

    rootTreeContainer._2.foreach {
      case (internalNodeName, children) => {
        if (children.size > 1) {
          val subset = EventContainer.subsetByChildren(eventContainer, children, internalNodeName)

          println("Processing the " + internalNodeName + " tree...")
          val (childNode, childLinker) = MixRunner.mixOutputToTree(MixRunner.runMix(mixDir, subset), subset, annotationMapping, internalNodeName)

          childToTree(internalNodeName) = childNode
          linker.addEdges(childLinker)

          rootNodeAndConnection.graftToName(internalNodeName, childToTree(internalNodeName))
        }
      }
    }

    // post-process the final tree
    MixRunner.postProcessTree(rootNodeAndConnection, linker, eventContainer, annotationMapping)

    rootNodeAndConnection
  }
}