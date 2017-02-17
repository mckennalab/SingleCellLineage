package collapse


import com.monsanto.stats.tables.clustering.{CRP, ModelParams, RealRandomNumGen, TopicVectorInput}
import reads.RankedReadContainer
import umi.{BitEncoding, StringCount}

import scala.annotation.switch
import scala.collection.mutable

/**
  * cluster UMIs
  */
class UmiClustering(umiSize: Int, umis: mutable.HashMap[String, RankedReadContainer], initialCountThreshold: Int) {

  val bitEncoder = new BitEncoding(umiSize)

  var currentThreshold = initialCountThreshold

  var allUmisBuilder = mutable.ArrayBuilder.make[Long]
  umis.foreach { case (umiString, reads) => {
    val encoding = bitEncoder.bitEncodeString(umiString)
    allUmisBuilder += encoding
  }}

  // now sort the list
  val allUmis = allUmisBuilder.result()
  scala.util.Sorting.quickSort(allUmis)

  // now partition UMIs into discrete bins, and assign a collection of UMIs to a consensus sequence
  var stillBinning = true
  var index = 0


  while (index < allUmis.size) {
    var lookAheadPointer = index
    var currentClump = mutable.ArrayBuilder.make[Long]
  }


}
