import scala.io._
import java.io._
import java.util.zip._
import scala.collection.mutable._

// load the various files we've made in the process, and generate a series of summary of
// tables that we can display with the GESTALT rundown information

val raw_read_one = args(0)
val cleaned_read_one = args(1)
val merged_reads = args(2)
val unmerged_reads = args(3)

val umi_counts = args(4)
val stats_file = args(5)

// setup the output file
val outputStatsFile = new PrintWriter(args(6))
outputStatsFile.write("stat\tkey\tvalue\n")

// read in compressed input streams with scala source commands
def gis(s: String) = new GZIPInputStream(new BufferedInputStream(new FileInputStream(s)))

// trace the path of reads from the input to the output, where we lost them, etc
var readLengths = 0L
var readCount = if (raw_read_one endsWith ".gz")
  Source.fromInputStream(gis(raw_read_one)).getLines().grouped(4).map{readSet => {
    readLengths += readSet(1).size
    1
  }}.sum
else
  Source.fromFile(raw_read_one).getLines().grouped(4).map{readSet => {
    readLengths += readSet(1).size
    1
  }}.sum

val averageReadLengths = readLengths.toDouble / readCount.toDouble
outputStatsFile.write("averageRawReadLength\taverageRawReadLength\t" + averageReadLengths + "\n")
outputStatsFile.write("rawReadCount\trawReadCount\t" + readCount + "\n")

// count clean reads
var cReadLengths = 0L
var cReadCount =
  if ((new File(cleaned_read_one)).exists) {
    if (cleaned_read_one endsWith ".gz")
      Source.fromInputStream(gis(cleaned_read_one)).getLines().grouped(4).map{readSet => {
        readLengths += readSet(1).size
        1
      }}.sum
    else
      Source.fromFile(cleaned_read_one).getLines().grouped(4).map{readSet => {
        readLengths += readSet(1).size
        1
      }}.sum
  } else 1


val cAverageReadLengths = cReadLengths.toDouble / cReadCount.toDouble
outputStatsFile.write("averageCleanReadLength\taverageCleanReadLength\t" + cAverageReadLengths + "\n")
outputStatsFile.write("cleanReadCount\tcleanReadCount\t" + cReadCount + "\n")


// count the merged and unmerged reads
val mergedReadCount = Source.fromFile(merged_reads).getLines().filter{line => line.startsWith(">")}.size / 2
val unmergedReadCount = Source.fromFile(unmerged_reads).getLines().filter{line => line.startsWith(">")}.size / 4
outputStatsFile.write("mergedReadCounts\tmergedReadCounts\t" + mergedReadCount + "\n")
outputStatsFile.write("unmergedReadCounts\tunmergedReadCounts\t" + unmergedReadCount + "\n")

if ((new File(umi_counts)).exists) {
  var passingUMICount = 0
  var failedUMICount = 0
  var totalReadSum = 0L
  var passingUMIReads = 0L
  var failedUMIreads = 0L
  val readsPerUMI = new LinkedHashMap[Int,Int]()
  val pReadsPerUMI = new LinkedHashMap[Int,Int]()
  val umireadsPerUMI = new LinkedHashMap[Int,Int]()
  val umipReadsPerUMI = new LinkedHashMap[Int,Int]()
  val maxCount = 500

  (1 until (maxCount + 1)).foreach{value => {
    readsPerUMI(value) = 0
    pReadsPerUMI(value) = 0
    umireadsPerUMI(value) = 0
    umipReadsPerUMI(value) = 0
  }}

  Source.fromFile(umi_counts).getLines().drop(1).foreach{line => {
    val sp = line.split("\t")
    if (sp(8) == "NOTENOUGHREADS") {
      failedUMICount += 1
      failedUMIreads += sp(2).toInt

      val totalCount = math.min(sp(2).toInt,maxCount)
      readsPerUMI(totalCount) = readsPerUMI.getOrElse(totalCount,0) + 1
      pReadsPerUMI(totalCount) = pReadsPerUMI.getOrElse(totalCount,0) + 1
    } else { 
      passingUMICount += 1

      var totalCount = sp(2).toInt
      passingUMIReads += totalCount
      totalCount = math.min(totalCount,maxCount)
      umireadsPerUMI(totalCount) = umireadsPerUMI.getOrElse(totalCount,0) + 1
      umipReadsPerUMI(totalCount) = umipReadsPerUMI.getOrElse(totalCount,0) + 1
    }
    totalReadSum += sp(2).toInt
  }}

  outputStatsFile.write("passingUMICount\tpassingUMICount\t" + passingUMICount + "\n")
  outputStatsFile.write("failedUMICount\tfailedUMICount\t" + failedUMICount + "\n")
  outputStatsFile.write("totalReadSum\ttotalReadSum\t" + totalReadSum + "\n")
  outputStatsFile.write("passingUMIReads\tpassingUMIReads\t" + passingUMIReads + "\n")
  outputStatsFile.write("failedUMIreads\tfailedUMIreads\t" + failedUMIreads + "\n")

  readsPerUMI.foreach{case(key,value) => outputStatsFile.write("readsPerUMI\t" + key + "\t" + value + "\n")}
  pReadsPerUMI.foreach{case(key,value) => outputStatsFile.write("passingReadsPerUMI\t" + key + "\t" + value + "\n")}
  umireadsPerUMI.foreach{case(key,value) => outputStatsFile.write("successfulReadsPerUMI\t" + key + "\t" + value + "\n")}
  umipReadsPerUMI.foreach{case(key,value) => outputStatsFile.write("successfulPassingReadsPerUMI\t" + key + "\t" + value + "\n")}
}

var passingStats = 0
var failedStats = 0
Source.fromFile(stats_file).getLines().drop(1).foreach{line => {
  if (line contains "PASS")
    passingStats += 1
  else
    failedStats += 1
}}
outputStatsFile.write("passingStatEntries\tpassingStatEntries\t" + passingStats + "\n")
outputStatsFile.write("failedStatEntries\tfailedStatEntries\t" + failedStats + "\n")

outputStatsFile.close()



