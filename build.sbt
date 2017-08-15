organization := "io.github.a-fistful-of-code"

version := "0.1.0"

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

scalariformSettings

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/a-fistful-of-code/pre-canned</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:a-fistful-of-code/pre-canned.git</url>
      <connection>scm:git@github.com:a-fistful-of-code/pre-canned.git</connection>
    </scm>
    <developers>
      <developer>
        <id>theon</id>
        <name>Ian Forsey</name>
        <url>http://theon.github.io</url>
      </developer>
    </developers>)