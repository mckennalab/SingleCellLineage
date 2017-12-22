name := "UMIMerge"

version := "1.1"

scalaVersion := "2.12.4"

resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies += "org.spire-math" %% "spire" % "0.13.0"
libraryDependencies += "org.scalanlp" %% "breeze-viz" % "0.13.2"
libraryDependencies += "org.scalanlp" %% "breeze-natives" % "0.13.2"
libraryDependencies += "org.scalanlp" %% "breeze" % "0.13.2"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.4"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"             % "3.7.2"
libraryDependencies += "ch.qos.logback"    % "logback-classic"                     % "1.2.3"
libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.0"

// the alternative here is to use
val buildTool = System.getProperty("build.tool", "umi")

val mainCl = buildTool match {
  case "umi"=> "collapse.UMIProcessing"
  case "call" => "eventcalling.DeepSeq"
  case _ => throw new IllegalStateException("Unable to match build.tool " + buildTool)
}


val mainJar = mainCl match {
  case "endogenous.UMICounter" => "UMIMerge.jar"
  case "eventcalling.DeepSeq" => "DeepSeq.jar"
  case _ => throw new IllegalStateException("Unable to match build.tool to jar for  " + mainCl)
}

mainClass := Some(mainCl)
assemblyJarName in assembly := mainJar
