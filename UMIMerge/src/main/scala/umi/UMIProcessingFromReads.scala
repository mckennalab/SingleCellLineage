package umi

import java.io.PrintWriter

import reads.{ForwardReadOrientation, SequencingRead, SingleRankedReadContainer}
import utils.Utils

import scala.collection.mutable
import scala.collection.immutable
import scala.io.Source

/**
  * transforms that work with UMIs
  */
object UMIProcessingFromReads {


  /**
    * given UMIed reads, process per UMI, merging reads and calling events
    *
    */
  def extractSuccessfulUMICaptures(inputFile: String, umiStartPos: Int, umiLength: Int, minimumReadsPerUMI: Int): UMIContainer = {

    // setup clustered input of the fastq files
    // ------------------------------------------------------------------------------------------
    val forwardReads = Source.fromInputStream(Utils.gis(inputFile)).getLines().grouped(4)

    // our containers for forward and reverse reads
    var umiReads = new mutable.HashMap[String, SingleRankedReadContainer]()

    // --------------------------------------------------------------------------------
    // process the reads into bins of UMIs, keep fwd/rev reads together
    // --------------------------------------------------------------------------------
    print("Reading in sequences and parsing out UMIs (one dot per 100K reads, carets at 1M): ")

    var readsProcessed = 0
    forwardReads foreach { fGroup => {

      // for the forward read the UMI start position is used literally,
      // for the reverse read (when start is negitive) we go from the end of the read backwards that much. To
      // allow UMIs to start at the zero'th base on the reverse, we say the first base is one
      var umi: Option[String] = None

      umi = Some(fGroup(1).slice(umiStartPos, umiStartPos + umiLength))

      val readNoUMI = fGroup(1).slice(0, umiStartPos) + fGroup(1).slice(umiStartPos + umiLength, fGroup(1).length)
      val qualNoUMI = fGroup(3).slice(0, umiStartPos) + fGroup(3).slice(umiStartPos + umiLength, fGroup(3).length)

      if (!(umiReads contains umi.get))
        umiReads(umi.get) = new SingleRankedReadContainer(umi.get, 50)

      val fwd = SequencingRead(fGroup(0), readNoUMI, qualNoUMI, ForwardReadOrientation, umi.get)
      umiReads(umi.get).addRead(fwd, true)

      readsProcessed += 1
      if (readsProcessed % 100000 == 0)
        print(".")
      if (readsProcessed % 1000000 == 0)
        print("^")
    }
    }

    // --------------------------------------------------------------------------------
    // for each UMI -- process the collection of reads
    // --------------------------------------------------------------------------------
    var passingUMI = 0
    var totalWithUMI = 0
    var index = 1


    return UMIContainer(umiReads.filter { case (umi, reads) => reads.totalReads >= minimumReadsPerUMI })
  }


}

case class UMIContainer(umis: mutable.HashMap[String,SingleRankedReadContainer])
