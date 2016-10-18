import scala.io._
import java.io._
import scala.collection.mutable._

// the edit distance between two strings
def editDistance(str1: String, str2: String): Int = str1.zip(str2).map{case(b1,b2) => if (b1 == b2) 0 else 1}.sum
case class OTHit(bases: String, editDist: Int, count: Int)

object TreeNode {
  val bases = Array[String]("A","C","G","T")

  def baseToIndex(base:String): Int = base match {
    case "A" => 0
    case "C" => 1
    case "G" => 2
    case "T" => 3
    case _ => throw new IllegalStateException("Unable to match base " + base)
  }

  def indexToBase(index: Int): String = index match {
    case 0 => "A"
    case 1 => "C"
    case 2 => "G"
    case 3 => "T"
    case _ => throw new IllegalStateException("Unable to match index " + index)
  }
}

// tree nodes
class TreeNode(base: Char, depth: Int) {

  var count = 0

  val myBase = base
  val myDepth = depth
  val children = Array[Option[TreeNode]](None,None,None,None)

  // pop a base from the front, decide if we want to 
  def addGuide(guideString: String, guidecount: Int) {
    count += guidecount

    if (guideString.length > 0) {
      val firstBase = guideString.slice(0,1)
      val remainder = guideString.slice(1,guideString.size)

      val index = TreeNode.baseToIndex(firstBase)

      if (!children(index).isDefined)
        children(index) = Some(new TreeNode(firstBase(0),myDepth + 1))
      children(index).get.addGuide(remainder,guidecount)
    }
  }

  // do we have the specified guide in the tree: if so return the count in the tree, otherwise 0
  def containsGuide(guideString: String): Int = {
    if (guideString.size == 0)
      return count

    val remainder = guideString.slice(1,guideString.size)
    val index = TreeNode.baseToIndex(guideString.slice(0,1))
    if (!children(index).isDefined)
      return 0
    else
      children(index).get.containsGuide(remainder)
  }

  // find the set of off-target sequences for a specified guide sequence
  def findOffTargetCandidates(seqToThisPoint: String, guideString: String, maxEditCount: Int, currentHits: Int, maxHits: Int = 300): Array[OTHit] = {

    // if were here over the count, return nothing
    if (currentHits > maxEditCount)
      return Array[OTHit]()

    println("called with " + guideString)
    if (children.map{tk => tk}.size == 0) {
      if (editDistance(seqToThisPoint + myBase.toString,guideString) <= maxEditCount)
        return Array[OTHit](OTHit(seqToThisPoint + myBase.toString,editDistance(seqToThisPoint + myBase.toString,guideString),count))
      else
        return Array[OTHit]()
    }
    else {
      val nextBase = guideString.slice(0,1)
      val remainder = guideString.slice(1,guideString.size)
      val fullSeqToThisPoint = seqToThisPoint + myBase.toString
      val aHits =
        if (children(0).isDefined)
          findOffTargetCandidates(fullSeqToThisPoint, remainder, maxEditCount, if (nextBase == 'A') currentHits else currentHits + 1, maxHits)
        else Array[OTHit]()
      val cHits =
        if (children(0).isDefined)
          findOffTargetCandidates(fullSeqToThisPoint, remainder, maxEditCount, if (nextBase == 'C') currentHits else currentHits + 1, maxHits)
        else Array[OTHit]()
      val gHits =
        if (children(0).isDefined)
          findOffTargetCandidates(fullSeqToThisPoint, remainder, maxEditCount, if (nextBase == 'G') currentHits else currentHits + 1, maxHits)
        else Array[OTHit]()
      val tHits =
        if (children(0).isDefined)
          findOffTargetCandidates(fullSeqToThisPoint, remainder, maxEditCount, if (nextBase == 'T') currentHits else currentHits + 1, maxHits)
        else Array[OTHit]()
      return aHits ++ cHits ++ gHits ++ tHits
    }
  }

  def findOffTargetCandidatesList(guideString: String, maxEditCount: Int, maxHits: Int = 300): Array[OTHit] = {
    findOffTargetCandidates("", guideString: String, maxEditCount: Int, 0, maxHits)
  }
}

// our base tree node
val baseTreeNode = new TreeNode('N',0)

/*
baseTreeNode.addGuide("AGAGGAGGACGAGGACGACTGGG")
baseTreeNode.addGuide("AGAGGAGGACGAGGACGACTGGG")
println(baseTreeNode.containsGuide("AGAGGAGGACGAGGACGACTGGG"))
println(baseTreeNode.containsGuide("AGAGGAGGACGAGGACGTCTGGG"))
println(baseTreeNode.containsGuide("GGAGGAGGACGAGGACGACTGGG"))
println(baseTreeNode.findOffTargetCandidatesList("GGAGGAGGACGAGGACGACTGGG", 2))

val outputFile = "/net/shendure/vol10/projects/CRISPR.lineage/nobackup/2016_10_14_OT_Cpf1/cpf1.output.sorted.count"
val output = new PrintWriter(outputFile)
output.write("bases\tcount\n")

var lastGuide = ""
var lastGuideCount = 0
Source.fromFile(inputFile).getLines().drop(1).zipWithIndex.foreach{case(line,index) => {
  val sp = line.split("\t")
  //baseTreeNode.addGuide(sp(3))
  if (lastGuide != sp(3)) {
    if (lastGuide != "")
      output.write(lastGuide + "\t" + lastGuideCount + "\n")
    lastGuide = sp(3)
    lastGuideCount = 0
  }
  lastGuideCount += 1

  if (index % 10000000 == 0)
    println("Processed " + index + " lines")
}}

if (lastGuide != "")
  output.write(lastGuide + "\t" + lastGuideCount + "\n")

output.close()

 */
val inputFile = "/net/shendure/vol10/projects/CRISPR.lineage/nobackup/2016_10_14_OT_Cpf1/cpf1.output.sorted.count"

def checkGuide(str: String): Boolean = str.map{case(base) => if (base == 'A' || base == 'C' || base == 'G' || base == 'T') 0 else 1}.sum == 0

Source.fromFile(inputFile).getLines().drop(1).zipWithIndex.foreach{case(line,index) => {
  val sp = line.split("\t")

  if (checkGuide(sp(0)))
    baseTreeNode.addGuide(sp(0).slice(4,24),sp(1).toInt)

  if (index % 10000000 == 0)
    println("Processed " + index + " lines")
}}
