// convert a run where the UMI is read out as an indexed run to a more traditional run where
// we prepend it to the front of the first read
import java.io._
import scala.io._

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

def gos(s: String) = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(s)))
def gis(s: String) = new DataInputStream(new GZIPInputStream(new FileInputStream(s)))

val inputReads = Source.fromInputStream(gis(args(0))).getLines().grouped(4)
val inputUMI = args(1).split(",").map{fl => {
  Source.fromInputStream(gis(fl)).getLines().grouped(4)
}}
val cutLength = args(3).toInt

val outputReads = new PrintWriter(gos(args(2)))

inputReads.foreach{case(read) => {

  var umiSequences = ""
  var umiQuals = ""
  inputUMI.foreach{umiFl => {
    val umi = umiFl.next
    umiSequences +=  umi(1).slice(0,cutLength)
    umiQuals +=  umi(3).slice(0,cutLength)
  }}

  outputReads.write(read(0) + "\n" + umiSequences + read(1) + "\n" + read(2) + "\n" + umiQuals + read(3) + "\n")
}}

outputReads.close()
