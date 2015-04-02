name := """aws-dashboard"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "bootstrap" % "3.1.1-2",
  "org.webjars" % "jquery" % "2.1.3",
  "com.amazonaws" % "aws-java-sdk" % "1.9.14",
  "com.typesafe.akka" %% "akka-actor" % "2.3.9"
)

unmanagedResourceDirectories in Compile <+= baseDirectory( _ / "aws")
