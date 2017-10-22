name := "HMMAlign"

version := "1.0"

resolvers += Resolver.sonatypeRepo("public")

resolvers += "erichseifert.de" at "http://mvn.erichseifert.de/maven2"


libraryDependencies ++= {
  object v {
    val scalatest = "3.0.0-M5"
    val scalanlp = "0.11.2"
    val scalacheck = "1.12.3"
  }
  Seq(
    "org.scalatest"     %% "scalatest"                          % v.scalatest % "test",
    "com.github.scopt" %% "scopt" % "3.2.0",
    "org.apache.commons" % "commons-math3" % "3.5"
  )
}


// this is clearly not the way to do this:

mainClass in (Compile, packageBin) := Some("main.scala.Controller")
mainClass in (Compile, run) := Some("main.scala.Controller")
