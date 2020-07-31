organization := "io.github.a-fistful-of-code"

val scala212Version = "2.12.11"
val scala213Version = "2.13.3"

scalaVersion := scala213Version

crossScalaVersions := Seq(scala212Version, scala213Version)

name := "pre-canned"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

val akkaVersion = "2.6.8"
val akkaHttpVersion = "10.1.12"
val scalatestVersion = "3.2.0"

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
