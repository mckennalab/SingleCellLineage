package main.scala.mix

import java.io.File

/**
  * the files associated with a mix run
  */

case class MixFilePackage(mixIntputFile: File, weightsFile: File, mixDirToRunIn: File) {
  val mixFile = new File(mixDirToRunIn.getAbsolutePath + "/outfile")
  val mixTree = new File(mixDirToRunIn.getAbsolutePath + "/outtree")
}
