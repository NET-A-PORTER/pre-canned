organization := "com.netaporter"

version := "0.0.1"

scalaVersion := "2.10.3"

name := "Pre-canned"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

val akka = "2.2.3"
val spray = "1.2.0"

libraryDependencies ++=
  "com.typesafe.akka" %% "akka-actor" % akka ::
  "io.spray" % "spray-can" % spray ::
  "io.spray" % "spray-http" % spray ::
  Nil

libraryDependencies ++=
  "io.spray" % "spray-client" % spray % "test" ::
  "com.typesafe.akka" %% "akka-testkit" % akka % "test" ::
  "org.scalatest" %% "scalatest" % "2.0" % "test" ::
  Nil

scalariformSettings