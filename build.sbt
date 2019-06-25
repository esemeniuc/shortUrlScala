name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.12.8"

lazy val akkaVersion = "2.6.0-M2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",

  "com.typesafe.akka" %% "akka-http" % "10.1.8",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion // or whatever the latest version is

)
//fork in run := true
enablePlugins(JavaAppPackaging)
