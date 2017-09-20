package main.scala.annotation

import scala.collection.mutable

/**
  * an annotation entry
  */
// some containers
case class AnnotationEntry(taxa: String,
                           sample: String,
                           count: Int,
                           proportion: Double,
                           event:Array[String],
                           additionalEntries: mutable.HashMap[String,String])
