import scala.collection.immutable._
import scala.io._
import java.io._

case class CRISPRSite(contig: String, bases: String, forwardStrand: Boolean, position: Int, cutSite: Int) {
  val sep = "\t"
  val strandOutput = if (forwardStrand) "FWD" else "RVS"
  def to_output = contig + sep + position + sep + cutSite + sep + bases + sep + strandOutput
}

object CRISPRSite {
  def header = "contig\tposition\tcutsite\tbases\tstrand\n"
}

case class CRISPRCircle(bufferSize: Int, outputFile: String) {
  val cutSiteFromEnd = 6
  var stack = new Array[Char](bufferSize)
  val output = new PrintWriter(outputFile)
  output.write(CRISPRSite.header)

  var currentPos = 0
  var contig = "UNKNOWN"

  def addLine(line: String) {line.foreach{chr => {addBase(chr)}}}

  def addBase(chr: Char) {
    stack(currentPos % bufferSize) = chr
    currentPos += 1
    if (currentPos >= bufferSize)
      checkCRISPR().foreach{ct => {
        output.write(ct.to_output + "\n")
      }}
  }

  def reset(cntig: String) {
    contig = cntig
    currentPos = 0
  }

  def checkCRISPR(): Array[CRISPRSite] = {
    val str = toTarget()
    if (str contains "N")
      return Array[CRISPRSite]()
    var ret = Array[CRISPRSite]()
    if (str(0) == 'T' && str(1) == 'T' && str(2) == 'T')
      ret :+= CRISPRSite(contig, str, true, (currentPos - bufferSize), (currentPos - cutSiteFromEnd))
    if (str(bufferSize-3) == 'A' && str(bufferSize-2) == 'A' && str(bufferSize-1) == 'A')
      ret :+= CRISPRSite(contig, reverseCompString(str), false, (currentPos - bufferSize), (currentPos - bufferSize) + cutSiteFromEnd)
    ret
  }

  def reverseComp(c: Char): Char = if (c == 'A') 'T' else if (c == 'C') 'G' else if (c == 'G') 'C' else if (c == 'T') 'A' else c
  def reverseCompString(str: String): String = str.map{reverseComp(_)}.reverse.mkString


  def toTarget(): String = stack.slice(currentPos % bufferSize,bufferSize).mkString + stack.slice(0,currentPos % bufferSize).mkString
}

val cls = CRISPRCircle(24,"/net/shendure/vol10/projects/CRISPR.lineage/nobackup/2016_10_14_OT_Cpf1/andrews_events.txt")


Source.fromFile("/net/shendure/vol10/projects/CRISPR.lineage/nobackup/2016_10_14_OT_Cpf1/targeted_exons_all_guides.fa").getLines().foreach{line => {
  if (line.startsWith(">")) {
    println("Switching to chromosome " + line)
    cls.reset(line.split(" ")(0).slice(1,line.split(" ")(0).length))
  } else {
    cls.addLine(line.toUpperCase)
  }
}}
