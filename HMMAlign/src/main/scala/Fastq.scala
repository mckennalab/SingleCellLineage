package main.scala

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

class Fastq(file: String) {

  val input = Source.fromFile(file).getLines().grouped(4)

  val readBuffer = new ArrayBuffer[Read]()

  input.foreach{line => {
      val nextRead = Read(line(0))
      nextRead.sequence = line(1)
      readBuffer += nextRead
  }}

  println("From file " + file + " read count " + readBuffer.size)
}

case class Read(name: String) {
  var sequence = ""
}