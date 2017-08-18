package main.scala.matrix

object MatrixUtils {

  def padInt(i: Int, padv: Int): String = {
    String.format("%1$" + padv + "s", i.toString)
  }

  def padDouble(i: Double, padv: Int): String = {
    String.format("%1$" + padv + "s", i.toString)
  }

  def padString(st: String, padv: Int): String = {
    String.format("%1$" + padv + "s", st)
  }
}
