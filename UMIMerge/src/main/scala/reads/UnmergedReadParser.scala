package reads

import java.io.File

/**
 * Created by aaronmck on 12/6/15.
 */
class UnmergedReadParser(readFiles: File) extends Iterator[ReadPair] {

  val parser = new ReadPairParser(readFiles)

  // the two reads we're returning
  var read1 : Option[RefReadPair] = None
  var read2 : Option[RefReadPair] = None

  nextReads()

  // fill the reads from our iterator
  def nextReads() {
    if (parser.hasNext) {
      read1 = Some(parser.next())
      //println("Read1 " + read1.get.read.name)
    } else
      read1 = None
    if (parser.hasNext) {
      read2 = Some(parser.next())
     // println("Read2 " + read2.get.read.name)
    }
    else
      read2 = None

  }

  override def hasNext: Boolean = read1.isDefined && read2.isDefined

  override def next(): ReadPair = {
    val ret = ReadPair(read1.get,read2.get)
    nextReads()
    return ret
  }


}
