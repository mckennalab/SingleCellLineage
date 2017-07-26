package main.scala.matrix

class ScoreMatrix(dimX: Int, dimY: Int) {

  val values = Array.ofDim[Double](dimX, dimY)

  def set(rowPos: Int, colPos: Int, value: Double) {
    values(rowPos)(colPos) = value
  }

  def get(rowPos: Int, colPos: Int): Double = values(rowPos)(colPos)

  def printMatrix(pad: Int = 7): Unit = {
    println(ScoreMatrix.padInt(-1, pad) + (0 until dimX).map { i => ScoreMatrix.padInt(i, pad) }.mkString(""))
    (0 until dimY).foreach { indexB => {
      print(ScoreMatrix.padInt(indexB, pad))
      (0 until dimX).foreach { indexA => {
        print(ScoreMatrix.padDouble(values(indexA)(indexB), pad))
      }
      }
      println()
    }
    }
  }
}

object ScoreMatrix {

  def padInt(i: Int, padv: Int): String = {
    String.format("%1$" + padv + "s", i.toString)
  }

  def padDouble(i: Double, padv: Int): String = {
    String.format("%1$" + padv + "s", i.toString)
  }

  def matchInitialization(rows: Int, cols: Int): ScoreMatrix = {
    val ret = new ScoreMatrix(rows, cols)
    ret.set(0, 0, 1.0)
    ret
  }

  def initializeMatrices(rows: Int, cols: Int): MatrixPack = {
    val matched = matchInitialization(rows, cols)
    MatrixPack(matched, new ScoreMatrix(rows, cols), new ScoreMatrix(rows, cols), new TracebackMatrix(rows, cols))
  }
}

case class MatrixPack(matchMatrix: ScoreMatrix,
                      insertMatrix: ScoreMatrix,
                      deletionMatrix: ScoreMatrix,
                      tracebackMatrix: TracebackMatrix)