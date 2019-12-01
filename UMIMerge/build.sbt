name := "UMIMerge"

version := "1.2"

scalaVersion := "2.12.4"

resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies += "info.picocli" % "picocli" % "3.8.1"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.4"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"             % "3.7.2"
libraryDependencies += "ch.qos.logback"    % "logback-classic"                     % "1.2.3"
libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.0"


mainClass := Some("Main")
assemblyJarName in assembly := "MergeAndCall.jar"
