organization := "io.github.a-fistful-of-code"

val scala211Version = "2.11.11"
val scala212Version = "2.12.3"

scalaVersion := scala212Version

crossScalaVersions := Seq(scala211Version, scala212Version)

name := "pre-canned"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

val akkaVersion = "2.4.20"
val akkaHttpVersion = "10.0.9"
val scalatestVersion = "3.0.3"

libraryDependencies ++=
  "com.typesafe.akka" %% "akka-actor" % akkaVersion ::
  "com.typesafe.akka" %% "akka-stream" % akkaVersion ::
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion ::
  Nil

libraryDependencies ++=
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test" ::
  "org.scalatest" %% "scalatest" % scalatestVersion % "test" ::
  Nil

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

import ReleaseTransformations._

releaseCrossBuild := true // true if you cross-build the project for multiple Scala versions
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(releaseStepCommand("publishSigned"), enableCrossBuild = true),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(releaseStepCommand("sonatypeReleaseAll"), enableCrossBuild = true),
  pushChanges
)