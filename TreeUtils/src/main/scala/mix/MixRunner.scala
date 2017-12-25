package main.scala.mix

import scala.io._
import java.io._
import java.lang.ProcessBuilder

import scala.concurrent._
import scala.collection.JavaConversions._
import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.Files

import beast.util.TreeParser
import main.scala.MixConfig
import main.scala.annotation.AnnotationsManager
import main.scala.node.{BestTree, NodeLinker, RichNode}


/**
  * Control the mix program
  */
object MixRunner {

  var mixLocation = new File("/net/shendure/vol10/projects/CRISPR.lineage/nobackup/bin/phylip-fast/phylip-3.696/exe/mix")

  /**
    * run the mix program
    *
    * @param mixPackage the mix file package
    * @return the process return code
    */
  private def lowLevelProcessesMix(mixPackage: MixFilePackage): Int = {

    val mixprogram = List[String]("mix")
    val pb = new ProcessBuilder(mixprogram)

    pb.directory(mixPackage.mixDirToRunIn)
    pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)

    val process = pb.start()

    val stdin = process.getOutputStream();
    val writer = new BufferedWriter(new OutputStreamWriter(stdin))

    val a = Array(mixPackage.mixIntputFile, "P", "W", "4", "5", "Y", mixPackage.weightsFile).foreach { s =>
      writer.write((s + "\n"))
      writer.flush()
    }

    process.waitFor()

    writer.close()

    process.exitValue()
  }

  /**
    * Take the mix data and run the PHYLIP mix program on the data
    *
    * @param mixPackage the mix file package
    */
  private def executeMix(mixPackage: MixFilePackage) {

    // copy the mix file
    val dest = new File(mixPackage.mixDirToRunIn + "/mix")
    println(dest.getAbsolutePath)
    dest.createNewFile
    new FileOutputStream(dest) getChannel() transferFrom(new FileInputStream(mixLocation) getChannel, 0, Long.MaxValue)

    // run and assert that we didn't fail
    assert(lowLevelProcessesMix(mixPackage) == 0)
  }

  /**
    * run MIX on a subset of the tree
    *
    * @param runDir        the run location for mix
    * @param readEventsObj the read events container
    * @return a MixFilePackage describing the results of the MIX run
    */
  def runMix(runDir: File, readEventsObj: EventContainer): MixFilePackage = {
    // setup the files we use when runnning MIX
    val mixInput = new File(runDir.getAbsolutePath + File.separator + "mixInput")
    val mixWeights = new File(runDir.getAbsolutePath + File.separator + "mixWeights")

    val mixPackage = MixFilePackage(mixInput, mixWeights, runDir)

    println("Writing the mix data to disk, containing " + readEventsObj.events.size + " events")
    EventIO.writeMixPackage(mixPackage, readEventsObj)

    // now run mix for the tree as a whole
    println("Running mix...")
    MixRunner.executeMix(mixPackage)
    mixPackage
  }

  /**
    * create a tree from the MIX output
    *
    * @param mixFilePackage    the object containing MIX output paths
    * @param readEventsObj     the events used to generate this
    * @param annotationMapping the annotations
    * @param rootName          the name of the root node
    * @return a rich node representing the root of the new tree
    */
  def mixOutputToTree(mixFilePackage: MixFilePackage,
                      readEventsObj: EventContainer,
                      annotationMapping: AnnotationsManager,
                      rootName: String,
                      defaultColor: String = "black"): Tuple2[RichNode, NodeLinker] = {

    // find the best tree from the mix output
    println("Loading best tree...")
    val bestTreeContainer = BestTree(mixFilePackage.mixTree)

    // ------------------------------------------------------------
    // parse out the data from the mix (PHYLIP) output
    // ------------------------------------------------------------
    val mixParser = new MixParser(mixFilePackage.mixFile.getAbsolutePath, readEventsObj, bestTreeContainer.maxIndex, rootName)

    // load our tree
    //println("Best Tree " + bestTreeContainer.bestTreeString)
    val treeParser = new TreeParser(bestTreeContainer.bestTreeString, false, true, true, bestTreeContainer.maxIndex)

    // cleanup the mix output file
    println("Delete mixfile: " + mixFilePackage.mixFile)
    println("Delete mixtree: " + mixFilePackage.mixTree)

    Files.delete(mixFilePackage.mixFile.toPath)
    Files.delete(mixFilePackage.mixTree.toPath)

    // return the rich node representation of this tree
    (RichNode(treeParser.getRoot, annotationMapping, None, readEventsObj.numberOfTargets, defaultColor), mixParser.activeTree)
  }

  /**
    * post-process the tree to update annotations, names, and other artifacts to make the final polished tree
    *
    * @param rootNode          the root node to apply this all too
    * @param linker            provides a link lookup table between nodes
    * @param readEventsObj     the original events
    * @param annotationMapping the annotation manager which looks up the correct annotations for individual nodes
    */
  def postProcessTree(rootNode: RichNode, linker: NodeLinker, readEventsObj: EventContainer, annotationMapping: AnnotationsManager): RichNode = {

    // reassign the names
    val rootName = RichNode.recAssignNames(rootNode, linker)

    // now apply the parsimony results to the root of the tree (recursively walking down the nodes)
    //RichNode.applyParsimonyGenotypes(rootNode, linker, readEventsObj)
    RichNode.backAssignGenotypes(rootNode)

    // check that the nodes we assigned are consistent
    //RichNode.recCheckNodeConsistency(rootNode)

    RichNode.fixGraftedColors(rootNode, "red")

    // count nodes before
    println("before collapsing nodes " + rootNode.countSubNodes())

    // collapse nodes from the root
    ParsimonyCollapser.collapseNodes(rootNode)

    // sort the nodes
    RichNode.reorderChildrenByAlleleString(rootNode)

    // add gray lines to branches where we're going to two identical alleles with different tissue sources
    RichNode.assignBranchColors(rootNode)

    // the updated numbers
    println("after collapsing nodes " + rootNode.countSubNodes())

    // assign the colors to the nodes
    //RichNode.applyFunction(rootNode, annotationMapping.setNodeColor)

    rootNode
  }

}