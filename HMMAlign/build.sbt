name := "HMMAlign"

version := "1.2"

resolvers += Resolver.sonatypeRepo("public")

resolvers += "erichseifert.de" at "https://mvn.erichseifert.de/maven2"

scalaVersion := "2.13.1"

val AkkaVersion = "2.6.13"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion


libraryDependencies ++= {
  object v {
    val scalatest = "3.2.2"
    val scalanlp = "0.11.2"
    val scalacheck = "1.12.3"
  }
  Seq(
    "org.scalatest"     %% "scalatest" % v.scalatest % "test",
     "org.scalatest" %% "scalatest-flatspec" % "3.2.2" % "test",
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",
    "org.apache.commons" % "commons-math3" % "3.5"
  )
}


// this is clearly not the way to do this:

mainClass in (Compile, packageBin) := Some("main.scala.Controller")
mainClass in (Compile, run) := Some("main.scala.Controller")
