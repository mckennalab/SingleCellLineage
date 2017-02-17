name := "UMIMerge"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("public")

resolvers += "erichseifert.de" at "http://mvn.erichseifert.de/maven2"


libraryDependencies ++= {
  object v {
    val scalatest = "3.0.0-M5"
    val scalanlp = "0.11.2"
    val scalacheck = "1.12.3"
  }
  Seq(
    "org.scalactic"     %% "scalactic"                          % v.scalatest,
    "org.scalanlp"      %% "breeze"                             % v.scalanlp,
    // native libraries are not included by default. add this if you want them (as of 0.7)
    // native libraries greatly improve performance, but increase jar sizes.
    // It also packages various blas implementations, which have licenses that may or may not
    // be compatible with the Apache License. No GPL code, as best I know.
    "org.scalanlp"      %% "breeze-natives"                     % v.scalanlp,
    // the visualization library is distributed separately as well.
    // It depends on LGPL code.
    "org.scalanlp"      %% "breeze-viz"                         % v.scalanlp,
    "org.scalatest"     %% "scalatest"                          % v.scalatest % "test",
    "org.spire-math"    %% "debox"                              % "0.7.3",
    "com.typesafe.scala-logging" %% "scala-logging"             % "3.5.0",
    "ch.qos.logback"    % "logback-classic"                     % "1.1.7",
    // "com.googlecode.matrix-toolkits-java" % "mtj"               % "1.0.4"
  "com.github.scopt" %% "scopt" % "3.2.0",
  "org.apache.commons" % "commons-math3" % "3.5"
  )
}


// this is clearly not the way to do this:

//mainClass in (Compile, packageBin) := Some("eventcalling.DeepSeq")
//mainClass in (Compile, run) := Some("eventcalling.DeepSeq")

//mainClass in (Compile, packageBin) := Some("pacbio.PacBioInverter")
//mainClass in (Compile, run) := Some("pacbio.PacBioInverter")

mainClass in (Compile, packageBin) := Some("collapse.UMIProcessing")
mainClass in (Compile, run) := Some("collapse.UMIProcessing")

//mainClass in (Compile, packageBin) := Some("endogenous.UMICounter")
//mainClass in (Compile, run) := Some("endogenous.UMICounter")
