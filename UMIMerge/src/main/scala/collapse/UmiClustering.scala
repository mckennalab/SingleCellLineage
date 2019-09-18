package collapse


//import com.monsanto.stats.tables.clustering.{CRP, ModelParams, RealRandomNumGen, TopicVectorInput}
import reads.RankedReadContainer
import umi.{BitEncoding, StringCount}
import com.typesafe.scalalogging._

import scala.annotation.switch
import scala.collection.mutable
import scala.math.exp
/**
  * cluster UMIs to fix issues with high noise in some samples, especially 10X data
  */
object UmiClustering extends LazyLogging {

  def mergeAndConvertUMIS(umiSize: Int,
                          umis: mutable.HashMap[String, RankedReadContainer],
                          pairedReads: Boolean,
                          maxReadContainerSize: Int): mutable.HashMap[String, RankedReadContainer] = {

    require(umiSize <= 24, "The umi size must be less than 25")

    val toleratedCollisionRate = 0.50 // 5%

    // how many errors will we tolerate? this should be a function of the number of bases
    // and the total UMI space -- basically the birthday problem
    val toleratedErrors = 1 // UmiClustering.maxMismatchAtCollisionProbablility(toleratedCollisionRate, umis.size.toDouble, umiSize)

    logger.info("We're going to allow " + toleratedErrors + " errors with a collision rate of " + toleratedCollisionRate + " and a UMI size of " + umiSize)

    val bitEncoder = new BitEncoding(umiSize)

    var umiEncodings = mutable.ArrayBuilder.make[Long]
    var umiStringsToEncodings = new mutable.HashMap[Long, String]()

    umis.foreach { case (umiString, reads) => {
      if (reads.size < 1)
        println("umiString " + umiString + " has only " + reads.size)
      else {
        val encoding = bitEncoder.bitEncodeStringWithNs(umiString, reads.size())
        umiEncodings += encoding
        umiStringsToEncodings(encoding) = umiString
      }
    }
    }

    // now sort the list
    val allUmis = umiEncodings.result()

    // now partition UMIs into discrete bins, and assign a collection of UMIs to a consensus sequence
    var index = 0

    val mergedUmis = new mutable.HashMap[Long, List[Long]]()

    logger.info("Making a consensus for " + allUmis.size + " umis... ")

    while (index < allUmis.size) {
      var best_consensus = bitEncoder.bitEncodeStringWithNs(Array.fill[Char](umiSize)('N').mkString(""), 1)
      var bestDist = umiSize

      mergedUmis.keys.foreach{case(mergedUMI) => {
        if (bitEncoder.mismatches(mergedUMI, allUmis(index)) < bestDist) {
          bestDist = bitEncoder.mismatches(mergedUMI, allUmis(index))
          best_consensus = mergedUMI
        }
      }}

      if (bestDist <= toleratedErrors) {
        val newList = allUmis(index) :: mergedUmis(best_consensus)
        val newConsensus = bitEncoder.consensus(newList)

        // do we need to replace the value?
        if (bitEncoder.mismatches(newConsensus,best_consensus) > 0) {
          mergedUmis.remove(best_consensus)
        }
        mergedUmis(newConsensus) = newList

      } else {
        mergedUmis(allUmis(index)) = List[Long](allUmis(index))
      }

      index += 1
      if (index % 10000 == 0) logger.info("Merged " + index + " UMIs so far into " + mergedUmis.size + " consensus UMIS")
    }


    // ok now transform the input set into the output set,
    val ret = new mutable.HashMap[String, RankedReadContainer]()

    // for each merged set, get the backing reads and put into a common read container
    logger.info("Doing a final pass of merging...")
    var postCollisions = 0
    mergedUmis.foreach{case(consensus,fromBins) => {
      val newBin = bitEncoder.bitDecodeString(consensus).str
      val readContainer = new RankedReadContainer(newBin,maxReadContainerSize,pairedReads)

      fromBins.foreach{bin => umis(umiStringsToEncodings(bin)).pQ.foreach{rd => readContainer.addBundle(rd)}}

      if (ret contains newBin)
        postCollisions += 1
      ret(newBin) = readContainer
    }}

    logger.info("Found " + ret.size + " merged UMIs from an original " + allUmis.size + " umis... (post-collisions = " + postCollisions + ")")

    ret
  }

  def birthdayApproximation(sizeOfDataSet: Double, setSize: Double): Double = {
    1.0 - exp( (-1.0 * sizeOfDataSet * (sizeOfDataSet - 1)) / (2 * setSize) )
  }

  def birthdayApproxForD(prob: Double, sizeOfDataSet: Double): Double = {
    2.0 * ((1.0 / prob) * (sizeOfDataSet * (sizeOfDataSet - 1.0)))
  }

  /**
    * given:
    * @param prob the upper bound probability of a collision we will allow
    * @param setSize the number of UMIs we see in our data
    * @param currentBases the totally umi size, in base positions
    * @return a number of errors we can allow and still have that collision rate
    */
  def maxMismatchAtCollisionProbablility(prob: Double, setSize: Double, currentBases: Int): Int = {
    val requiredSpaceSize = birthdayApproxForD(prob,setSize)
    println(math.log(requiredSpaceSize)/math.log(4.0))
    val requiredBases = math.ceil(math.log(requiredSpaceSize)/math.log(4.0)).toInt
    math.max(1,currentBases - requiredBases)
  }
}
