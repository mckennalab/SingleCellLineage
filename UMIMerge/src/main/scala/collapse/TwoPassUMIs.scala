package collapse
import java.io.{File, PrintWriter}

import com.typesafe.scalalogging.LazyLogging
import picocli.CommandLine.{Command, Option, Parameters}
import utils.Utils

import scala.collection.mutable
import scala.io.Source

@Command(name = "umi2pass", description = Array("extract and collapse successfully captured UMIs from a set of fasta files"))
class TwoPassUMIs extends Runnable with LazyLogging {
  @Option(names = Array("-inputReads1", "--read1"), required = true, paramLabel = "FILE", description = Array("the first input read file"))
  private var read1: File = new File("UNKNOWN")

  @Option(names = Array("-inputReads2", "--read2"), required = false, paramLabel = "FILE", description = Array("the second input read file"))
  private var read2: File = new File("UNKNOWN")

  @Option(names = Array("-outputReads1", "--outread1"), required = true, paramLabel = "FILE", description = Array("the first output read file"))
  private var outRead1: File = new File("UNKNOWN")

  @Option(names = Array("-outputReads2", "--outread2"), required = false, paramLabel = "FILE", description = Array("the second output read file"))
  private var outRead2: File = new File("UNKNOWN")

  @Option(names = Array("-umiStatsFile", "--statsFile"), required = true, paramLabel = "FILE", description = Array("an output file containing stats about each captured UMI"))
  private var statsFile: File = new File("UNKNOWN")

  @Option(names = Array("-umiThrehold", "--thresh"), required = false, paramLabel = "INT", description = Array("how many reads are required to save a UMI"))
  private var umiThrehold: Int = 7

  @Option(names = Array("-umiStart", "--umiStart"), required = false, paramLabel = "INT", description = Array("Where does the UMI start in the read, starting at 0. Negative values for the reverse read, starting at -1 "))
  private var umiStart: Int = 7

  @Option(names = Array("-umiLength", "--umiLength"), required = false, paramLabel = "INT", description = Array("how long is the UMI"))
  private var umiLength: Int = 7

  @Option(names = Array("-minBaseCallRate", "--minBaseCallRate"), required = false, paramLabel = "DOUBLE", description = Array("how many bases do we need to call in a read to record it"))
  private var minBaseCallRate: Double = 0.90

  @Option(names = Array("-baseCallThresh", "--baseCallThresh"), required = false, paramLabel = "DOUBLE", description = Array("what proportion of reads need to have base X to call the consensus"))
  private var baseCallThresh: Double = 0.90

  override def run()= {

    // setup a UMI extractor
    val umiExtractor = UMISlicer.getSlicer(umiStart,umiLength)

    // read in the input file, recording UMI counts
    val umis = new mutable.LinkedHashMap[String, Int]()
    val umiMaxsize1 = new mutable.LinkedHashMap[String, Int]()
    val umiMaxsize2 = new mutable.LinkedHashMap[String, Int]()

    val readers = TwoPassUMIs.forwardRevereseReaders(read1,read2)
    readers._1.foreach{r1 => {
      val r2 = if (readers._2.isDefined) Some(readers._2.get.next()) else None
      val r2seq = if (r2.isDefined) Some(r2.get(TwoPassUMIs.baseFastqPositon)) else None
      val r2qual = if (r2.isDefined) Some(r2.get(TwoPassUMIs.qualFastqPosition)) else None

      val umi = umiExtractor.slice(r1(TwoPassUMIs.baseFastqPositon), r1(TwoPassUMIs.qualFastqPosition), r2seq, r2qual)

      val slicedSeq1 = Utils.filterReadBySlidingWindow(umi.read1,umi.read1Qual,20,5)

      umis(umi.umi) = umis.getOrElse(umi.umi,0) + 1
      umiMaxsize1(umi.umi) = math.max(umiMaxsize1.getOrElse(umi.umi,0),slicedSeq1.size)
      if (r2.isDefined) {
        val slicedSeq2 = Utils.filterReadBySlidingWindow(umi.read2.get,umi.read2Qual.get,20,5)
        umiMaxsize2(umi.umi) = math.max(umiMaxsize2.getOrElse(umi.umi, 0), slicedSeq2.size)
      }
    }}

    // get a set of collapsers for any UMI with adequate coverage
    val collapsers = findPassingUMIs(umiExtractor, umis, umiMaxsize1, umiMaxsize2)

    // output the called UMIs
    outputCalledUMIs(collapsers)
  }

  /**
    * output the called UMIs
    * @param collapsers collapser down UMIs
    */
  private def outputCalledUMIs(collapsers: mutable.LinkedHashMap[String,SequenceCounterCollection]): Unit = {
    // output consensus reads from the collapsed UMI read pairs
    val outputStats = new PrintWriter(statsFile.getAbsolutePath)
    outputStats.write("UMI\tcount\tnRateF\tnRateR\n")
    val outputFastq1File = new PrintWriter(outRead1.getAbsolutePath)
    val outputFastq2File = if (read2.exists()) Some(new PrintWriter(outRead2.getAbsolutePath)) else None

    collapsers.foreach{case(umi,collections) => {
      val read1Col = collections.read1.countsToSequence(baseCallThresh,minBaseCallRate)
      if (read1Col.string.isDefined) {
        val read1Name = "@READ1_" + collections.read1.readCount + "_" + collections.read1.readCount + "_" + umi
        val read1QualityScore = "H" * read1Col.string.get.size
        outputFastq1File.write(read1Name + "\n" + read1Col.string.get + "\n+\n" + read1QualityScore + "\n")
      }

      if (outputFastq2File.isDefined) {
        val read2Col = collections.read2.countsToSequence(baseCallThresh, minBaseCallRate)
        if (read2Col.string.isDefined) {
          val read2Name = "@READ1_" + collections.read2.readCount + "_" + collections.read2.readCount + "_" + umi
          val read2QualityScore = "H" * read2Col.string.get.size
          outputFastq2File.get.write(read2Name + "\n" + read2Col.string.get + "\n+\n" + read2QualityScore + "\n")
        }
      }
    }}

    outputFastq1File.close()
    if (outputFastq2File.isDefined)
      outputFastq2File.get.close()
  }

  /**
    * find the passing UMIs above a certain threshold
    * @param umiExtractor how we extract
    * @param knownUMIs
    * @return
    */
  private def findPassingUMIs(umiExtractor: UMISlicer,
                              knownUMIs: mutable.LinkedHashMap[String, Int],
                              umiLength1: mutable.LinkedHashMap[String, Int],
                              umiLength2: mutable.LinkedHashMap[String, Int]): mutable.LinkedHashMap[String,SequenceCounterCollection] = {
    // setup a collapser for each UMI that has more than the minimum number of reads
    val collapsers = new mutable.LinkedHashMap[String,SequenceCounterCollection]()

    val readers2 = TwoPassUMIs.forwardRevereseReaders(read1,read2)
    // take a second pass over the reads, adding data to the collapsers
    readers2._1.foreach { r1 => {
      val r2 = if (readers2._2.isDefined) Some(readers2._2.get.next()) else None
      val r2seq = if (r2.isDefined) Some(r2.get(TwoPassUMIs.baseFastqPositon)) else None
      val r2qual = if (r2.isDefined) Some(r2.get(TwoPassUMIs.qualFastqPosition)) else None

      val umi = umiExtractor.slice(r1(TwoPassUMIs.baseFastqPositon), r1(TwoPassUMIs.qualFastqPosition), r2seq, r2qual)

      if (knownUMIs(umi.umi) >= umiThrehold) {
        val slicedSeq1 = Utils.filterReadBySlidingWindow(umi.read1,umi.read1Qual,20,5)
        val slicedSeq2 = if (r2.isDefined) Some(Utils.filterReadBySlidingWindow(umi.read2.get,umi.read2Qual.get,20,5)) else None
        val collapser = collapsers.getOrElse(umi.umi, SequenceCounterCollection(slicedSeq1, slicedSeq2, umiLength1(umi.umi), umiLength2(umi.umi)))
        collapser.read1.addSequence(slicedSeq1)
        if (umi.read2.isDefined)
          collapser.read2.addSequence(slicedSeq2.get)
        collapsers(umi.umi) = collapser
      }
    }
    }

    collapsers
  }
}


object TwoPassUMIs {
  val baseFastqPositon = 1
  val qualFastqPosition = 3

  def forwardRevereseReaders(read1: File, read2: File) = {
    val forwardReads = Source.fromInputStream(Utils.gis(read1.getAbsolutePath)).getLines().grouped(4)
    val reverseReads =
      if (read2.exists())
        Some(Source.fromInputStream(Utils.gis(read2.getAbsolutePath)).getLines().grouped(4))
      else
        None
    (forwardReads,reverseReads)
  }

  def qualScore(len: Int) = {
    "H" * len
  }
}

case class SequenceCounterCollection(read1: SequenceCounter, read2: SequenceCounter)
object SequenceCounterCollection {
  def apply(len1: Int, len2: Int): SequenceCounterCollection = SequenceCounterCollection(new SequenceCounter(len1), new SequenceCounter(len2))
  def apply(s1: String, s2: scala.Option[String], len1: Int, len2: Int): SequenceCounterCollection = {
    if (s2.isDefined)
      SequenceCounterCollection(new SequenceCounter(len1), new SequenceCounter(len2))
    else
      SequenceCounterCollection(new SequenceCounter(len1), new SequenceCounter(0))
  }
}