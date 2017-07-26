package main.scala.annotation

/**
  * an annotation entry
  */
// some containers
case class AnnotationEntry(taxa: String, sample: String, count: Int, proportion: Double, event:Array[String])
