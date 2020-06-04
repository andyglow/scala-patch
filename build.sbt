import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype._
import ReleaseTransformations._

// https://github.com/xerial/sbt-sonatype/issues/71
publishTo in ThisBuild := sonatypePublishTo.value

lazy val commons = Seq(
  organization := "com.github.andyglow",

  homepage := Some(new URL("http://github.com/andyglow/scala-patch")),

  startYear := Some(2019),

  organizationName := "andyglow",

  scalaVersion := "2.13.2",

  scalacOptions ++= {
    val options = Seq(
      "-encoding", "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xfatal-warnings",
      "-Xlint",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Xfuture")

    // WORKAROUND https://github.com/scala/scala/pull/5402
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) => options.map {
        case "-Xlint"               => "-Xlint:-unused,_"
        case "-Ywarn-unused-import" => "-Ywarn-unused:imports,-patvars,-privates,-locals,-params,-implicits"
        case other                  => other
      }
      case Some((2, n)) if n >= 13  => options.filterNot { opt =>
        opt == "-Yno-adapted-args" || opt == "-Xfuture"
      } :+ "-Xsource:2.13"
      case _             => options
    }
  },

  scalacOptions in (Compile,doc) ++= Seq(
    "-groups",
    "-implicits",
    "-no-link-warnings"),

  licenses := Seq(("LGPL-3.0", url("http://opensource.org/licenses/LGPL-3.0"))),

  sonatypeProfileName := "com.github.andyglow",

  publishMavenStyle := true,

  sonatypeProjectHosting := Some(
    GitHubHosting(
      "andyglow",
      "scala-patch",
      "andyglow@gmail.com")),

  scmInfo := Some(
    ScmInfo(
      url("https://github.com/andyglow/scala-patch"),
      "scm:git@github.com:andyglow/scala-patch.git")),

  developers := List(
    Developer(
      id    = "andyglow",
      name  = "Andriy Onyshchuk",
      email = "andyglow@gmail.com",
      url   = url("https://ua.linkedin.com/in/andyglow"))),

  releasePublishArtifactsAction := PgpKeys.publishSigned.value,

  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _), enableCrossBuild = true),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _), enableCrossBuild = true),
    pushChanges),

  libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % Test
)

lazy val core = (project in file("core")).settings(
  commons,
  name := "scala-patch-core",
  scalacOptions ++= Seq(
    "-language:implicitConversions",
    "-language:higherKinds"))

lazy val macros = (project in file("macros")).dependsOn(core).settings(
  commons,
  name := "scala-patch-macros",
  scalacOptions ++= Seq(
    "-language:experimental.macros"),
  libraryDependencies ++= Seq(
    (scalaVersion apply ("org.scala-lang" % "scala-reflect" % _ % Compile)).value.withSources.withJavadoc))

lazy val examples = (project in file("examples")).dependsOn(core, macros).settings(
  commons,
  name := "scala-patch-examples"
)

lazy val root = (project in file("."))
  .aggregate(core, macros, examples)
  .settings(
    commons,
    publish / skip := true,
    publishArtifact := false,
    aggregate in update := false,
    name := "scala-patch")
