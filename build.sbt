import sbt._
import sbt.Keys._

lazy val commons = Seq(
  publishMavenStyle := true,
  scalaVersion := "2.13.2",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % Test)

lazy val core = (project in file("core")).settings(
  commons,
  name := "scala-diff-core",
  scalacOptions ++= Seq(
    "-language:higherKinds"))

lazy val macros = (project in file("macros")).dependsOn(core).settings(
  commons,
  name := "scala-diff-macro",
  scalacOptions ++= Seq(
    "-language:experimental.macros"),
  libraryDependencies ++= Seq(
    (scalaVersion apply ("org.scala-lang" % "scala-reflect" % _ % Compile)).value.withSources.withJavadoc))

lazy val examples = (project in file("examples")).dependsOn(core, macros).settings(
  commons,
  name := "scala-diff-examples"
)

lazy val root = (project in file("."))
  .aggregate(core, macros, examples)
  .settings(
    commons,
    crossScalaVersions := Nil,
    publish / skip := true,
    publishArtifact := false,
    aggregate in update := false,
    name := "scala-diff")
