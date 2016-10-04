import scala.io._
import java.io._
import scala.collection.mutable._
import scala.util.matching.Regex

def reverseComp(c: Char): Char = if (c == 'A') 'T' else if (c == 'C') 'G' else if (c == 'G') 'C' else if (c == 'T') 'A' else c
def reverseCompString(str: String): String = str.map{reverseComp(_)}.reverse.mkString
def editDistance(str1: String, str2: String): Int = str1.toUpperCase.zip(str2.toUpperCase).map{case(s1,s2) => if (s1 == s2) 0 else 1}.sum

// command line parameters
val fastaFile     = Source.fromFile(args(0)).getLines()
val outFasta      = new PrintWriter(args(1))
val primers       = Source.fromFile(args(2)).getLines().toArray
val fwdPrimer     = primers(0)
val revCompPrimer = reverseCompString(primers(1))

println(fwdPrimer)
println(revCompPrimer)

var readname : Option[String] = None
var readbases = ""


fastaFile.foreach{line => {
  if ((line startsWith ">") && !readname.isEmpty) {
    val matchForward = editDistance(readbases.slice(0,fwdPrimer.size),fwdPrimer)
    val matchReverse = editDistance(readbases.slice(0,revCompPrimer.size),revCompPrimer)

    if ((matchForward.toDouble / fwdPrimer.size.toDouble) <= (matchReverse.toDouble / revCompPrimer.size.toDouble))
      outFasta.write(">" + readname.get + "\n" + readbases + "\n")
    else
      outFasta.write(">" + readname.get + "\n" + reverseCompString(readbases) + "\n")
    readname = Some(line.slice(1,line.size))
    readbases = ""
  } else if (line startsWith ">") {
    readname = Some(line.slice(1,line.size))
    readbases = ""
  } else {
    readbases += line
  }
}}

if (!readname.isEmpty) {
    val matchForward = editDistance(readbases.slice(0,fwdPrimer.size),fwdPrimer)
    val matchReverse = editDistance(readbases.slice(0,revCompPrimer.size),revCompPrimer)

    if ((matchForward.toDouble / fwdPrimer.size.toDouble) <= (matchReverse.toDouble / revCompPrimer.size.toDouble))
      outFasta.write(">" + readname.get + "\n" + readbases + "\n")
    else
      outFasta.write(">" + readname.get + "\n" + reverseCompString(readbases) + "\n")
}

outFasta.close()
