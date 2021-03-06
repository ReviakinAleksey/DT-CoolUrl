import sbt.Keys._
import sbt._

object BuildSettings {
  val buildOrganization = "com.cool.url"
  val buildVersion = "0.1.0"
  val buildSbtVersion = "0.13.5"
  val buildScalaVersion = "2.11.2"

  val buildSettings = Seq(
    organization := buildOrganization,
    version := buildVersion,
    sbtVersion := buildSbtVersion,
    scalaVersion := buildScalaVersion
  )
}


object Dependencies {
  val logging = "com.typesafe.scala-logging" %% "scala-logging" % "3.0.0"
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val slick = "com.typesafe.slick" %% "slick" % "2.1.0"
  val c3p0 = "com.mchange" % "c3p0" % "0.9.2.1"
  val scalaJackson = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.3"
  val unfilteredNetty = "net.databinder" %% "unfiltered-netty-server" % "0.8.2"
  val unfilteredDirectives = "net.databinder" %% "unfiltered-directives" % "0.8.2"
  val postgresql = "org.postgresql" % "postgresql" % "9.3-1102-jdbc41"
  val guava =  "com.google.guava" % "guava" % "15.0"
  val scalatest = "org.scalatest" %% "scalatest" % "2.2.1" % "test"
}

object CoolUrl extends Build {

  import BuildSettings._
  import Dependencies._


  val commonDeps = Seq(
    logback,
    logging,
    slick,
    c3p0,
    scalaJackson,
    unfilteredNetty,
    unfilteredDirectives,
    postgresql,
    guava,
    scalatest
  )


  lazy val main = Project(
    id = "main",
    base = file("."),
    settings = buildSettings ++ Seq(libraryDependencies ++= commonDeps) ++
      Seq(parallelExecution in Test := false) ++
      Seq(testOptions in Test := Seq(Tests.Filter(s => s.endsWith("MainSpec"))))
  )
}