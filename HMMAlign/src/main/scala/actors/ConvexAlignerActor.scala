package main.scala.actors
import scala.io._
import java.io._

import scala.collection.mutable._
import scala.sys.process._
import java.util.zip._

import main.scala.dp.ConvexDP
import main.scala.fasta.Fasta
import akka.routing.SmallestMailboxPool
import akka.routing.SmallestMailboxPool
import scala.util.Random
import akka.NotUsed
import akka.event.Logging
import akka.actor._
import akka.routing.Broadcast
import main.scala.AlignmentConfig

final case class AlignTo(config: AlignmentConfig, readName: String, referenceName: String, read: String, reference: String, replyTo: ActorRef)

final case class AlignedToPair(read1: AlignTo, read2: AlignTo, replyTo: ActorRef)

final case class Aligned(readAlignedName: String, refAlignedName: String, readAligned: String, refAligned: String)

final case class AlignedPair(read1: Aligned, read2: Aligned)

final case class Close()

class ConvexAlignerActor extends Actor {

  override def receive: Receive = {
    case message: AlignTo => {
      val nmw = new ConvexDP(message.read, message.reference, message.config.matchScore, -1.0 * message.config.mismatchScore, message.config.gapOpenCost, message.config.gapExtensionCost)
      val align = nmw.alignment

      message.replyTo ! Aligned(message.readName, message.referenceName, align.getAlignmentString._1, align.getAlignmentString._2)
    }
    case message: AlignedToPair => {
      val nmw1 = new ConvexDP(message.read1.read, message.read1.reference, message.read1.config.matchScore, -1.0 * message.read1.config.mismatchScore, message.read1.config.gapOpenCost, message.read1.config.gapExtensionCost)
      val nmw2 = new ConvexDP(message.read2.read, message.read2.reference, message.read2.config.matchScore, -1.0 * message.read2.config.mismatchScore, message.read2.config.gapOpenCost, message.read2.config.gapExtensionCost)

      val align1 = nmw1.alignment
      val align2 = nmw2.alignment
      val ret1 = Aligned(message.read1.readName, message.read1.referenceName, align1.getAlignmentString._1, align1.getAlignmentString._2)
      val ret2 = Aligned(message.read2.readName, message.read2.referenceName, align2.getAlignmentString._1, align2.getAlignmentString._2)
      message.replyTo ! AlignedPair(ret1, ret2)
    }
  }
}

class OutputWriter(filename: String) extends Actor {
  val output = new PrintWriter(filename)

  override def receive: Receive = {
    case message: Aligned => {
      output.write(">" + message.refAlignedName + "\n" + message.refAligned + "\n")
      output.write(">" + message.readAlignedName + "\n" + message.readAligned + "\n")
    }
    case message: AlignedPair => {
      output.write(">" + message.read1.refAlignedName + "\n" + message.read1.refAligned + "\n")
      output.write(">" + message.read1.readAlignedName + "\n" + message.read1.readAligned + "\n")

      output.write(">" + message.read2.refAlignedName + "\n" + message.read2.refAligned + "\n")
      output.write(">" + message.read2.readAlignedName + "\n" + message.read2.readAligned + "\n")
    }
    case clse: Close => {
      println("Closing writer")
      output.close()
    }
  }
}