package main.scala.matrix

class ScoreMatrix(dimX: Int, dimY: Int) {
  val mDimX = dimX
  val mDimY = dimY

  val values = Array.ofDim[Double](dimX, dimY)

  def set(rowPos: Int, colPos: Int, value: Double) {
    values(rowPos)(colPos) = value
  }

  def get(rowPos: Int, colPos: Int): Double = values(rowPos)(colPos)

  def printMatrix(pad: Int = 7): Unit = {
    println(MatrixUtils.padInt(-1, pad) + (0 until dimX).map { i => MatrixUtils.padInt(i, pad) }.mkString(""))
    (0 until dimY).foreach { indexB => {
      print(MatrixUtils.padInt(indexB, pad))
      (0 until dimX).foreach { indexA => {
        print(MatrixUtils.padDouble(values(indexA)(indexB), pad))
      }
      }
      println()
    }
    }
  }

  def maxIndex(row: Int, col:Int, byRow: Boolean): Int = {
    if (byRow)
      maxRowIndex(row,col)
    else
      maxColIndex(col,row)
  }

  private def maxRowIndex(row: Int, endCol: Int): Int = {
    var maxIndex = 0
    (0 until endCol).foreach{col => if (values(row)(col) > values(row)(maxIndex)) maxIndex = col}
    maxIndex
  }

  private def maxColIndex(col: Int, endRow: Int): Int = {
    var maxIndex = 0
    (0 until endRow).foreach{row => if (values(row)(col) > values(maxIndex)(col)) maxIndex = row}
    maxIndex
  }
}

object ScoreMatrix {

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