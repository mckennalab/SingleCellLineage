package main.scala.mix

import scala.io._
import java.io._
import java.lang.ProcessBuilder

import scala.concurrent._
import scala.collection.JavaConversions._
import java.io.{File, FileInputStream, FileOutputStream}

import beast.util.TreeParser
import main.java.TreeParser
import main.scala.MixConfig
import main.scala.annotation.AnnotationsManager
import main.scala.node.{BestTree, RichNode}


/**
  * Control the mix program
  */
object MixRunner {

  val mixLocation = new File("/net/shendure/vol10/projects/CRISPR.lineage/nobackup/bin/phylip-fast/phylip-3.696/exe/mix")

  /**
    * run the mix program
    *
    * @param mixPackage the mix file package
    * @return the process return code
    */
  private def processesMix(mixPackage: MixFilePackage): Int = {

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
  def runMix(mixPackage: MixFilePackage) {

    // copy the mix file
    val dest = new File(mixPackage.mixDirToRunIn + "/mix")
    println(dest.getAbsolutePath)
    dest.createNewFile
    new FileOutputStream(dest) getChannel() transferFrom(new FileInputStream(mixLocation) getChannel, 0, Long.MaxValue)

    // run
    assert(processesMix(mixPackage) == 0)
  }

  /**
    * run MIX on a subset of the tree
    *
    * @param config        the files and configuration to run MIX
    * @param readEventsObj the read events container
    * @return a MixFilePackage describing the results of the MIX run
    */
  def processIndividualMix(config: MixConfig, readEventsObj: EventContainer): MixFilePackage = {
    // setup the files we use when runnning MIX
    val mixInput = new File(config.mixRunLocation.getAbsolutePath + File.separator + "mixInput")
    val mixWeights = new File(config.mixRunLocation.getAbsolutePath + File.separator + "mixWeights")

    val mixPackage = MixFilePackage(mixInput, mixWeights, config.mixRunLocation)

    println("Writing the mix data to disk...")
    EventIO.writeMixPackage(mixPackage, readEventsObj)

    // now run mix for the tree as a whole
    println("Running mix...")
    MixRunner.runMix(mixPackage)
    mixPackage
  }

  /**
    * create a tree from the MIX output
    *
    * @param mixFilePackage the object containing MIX output paths
    * @param readEventsObj  the events used to generate this
    * @return a rich node representing the root of the new tree
    */
  def mixOutputToTree(mixFilePackage: MixFilePackage, readEventsObj: EventContainer, annotationMapping: AnnotationsManager) = {

    // find the best tree from the mix output
    println("Loading best tree...")
    val bestTreeContainer = BestTree(mixFilePackage.mixTree)

    // ------------------------------------------------------------
    // parse out the data from the mix (PHYLIP) output
    // ------------------------------------------------------------
    val mixParser = new MixParser(mixFilePackage.mixFile.getAbsolutePath, readEventsObj, bestTreeContainer.maxIndex)

    // load our tree
    val treeParser = new TreeParser(bestTreeContainer.bestTreeString, false, true, true, bestTreeContainer.maxIndex)

    RichNode(treeParser.getRoot, annotationMapping, None, readEventsObj.numberOfTargets)
  }

}