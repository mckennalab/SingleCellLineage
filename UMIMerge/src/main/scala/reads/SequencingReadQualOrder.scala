package reads

/**
  * Created by aaronmck on 9/11/16.
  */
object SequencingReadQualOrder extends Ordering[SequencingRead] {
  def compare(a:SequencingRead, b:SequencingRead) = a.averageQual() compare b.averageQual()
}
