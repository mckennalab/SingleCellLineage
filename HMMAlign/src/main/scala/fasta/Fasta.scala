package main.scala.fasta

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

class Fasta(file: String) {

  val input = Source.fromFile(file).getLines()

  val readBuffer = new ArrayBuffer[Read]()

  var currentRead : Option[Read] = None

  input.foreach{line => {
    if (line startsWith ">") {
      currentRead.map{t => readBuffer += t}
      currentRead = Some(Read(line.stripPrefix(">")))
    } else {
      currentRead.map{s => s.sequence += line}
    }
  }}
  currentRead.map{s => readBuffer += s}

  println("From file " + file + " read count " + readBuffer.size)
}

case class Read(name: String) {
  var sequence = ""
}