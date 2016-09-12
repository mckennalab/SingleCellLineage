package reads

/**
  * Created by aaronmck on 9/11/16.
  */
class PeekFilelineIterator(iter: Iterator[String]) extends Iterator[String] {

  var nextString: Option[String] = if (iter.hasNext) Some(iter.next()) else None

  override def hasNext: Boolean = nextString isDefined

  override def next(): String = {
    val ret = nextString.get
    nextString = if (iter.hasNext) Some(iter.next()) else None
    return ret
  }

  def peek: String = nextString.get
}
