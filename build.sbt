import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype._
import ReleaseTransformations._

// https://github.com/xerial/sbt-sonatype/issues/71
ThisBuild / publishTo := sonatypePublishTo.value

ThisBuild / versionScheme := Some("pvp")

lazy val scalaV = settingKey[ScalaVer]("Current Scala Version")

lazy val commons = Seq(
  organization                           := "com.github.andyglow",
  homepage                               := Some(new URL("http://github.com/andyglow/scala-patch")),
  startYear                              := Some(2019),
  organizationName                       := "andyglow",
  scalaVersion                           := (ScalaVer.fromEnv getOrElse ScalaVer._212).full,
  crossScalaVersions                     := ScalaVer.values.map(_.full),
  scalaV                                 := ScalaVer.fromString(scalaVersion.value) getOrElse ScalaVer._212,
  scalacOptions                          := CompilerOptions(scalaV.value),
  Compile / unmanagedSourceDirectories ++= {
    val bd                                   = baseDirectory.value
    def extraDirs(suffix: String): Seq[File] = Seq(bd / "src" / "main" / s"scala$suffix")
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, y)) if y <= 12 => extraDirs("-2.12-")
      case Some((2, y)) if y >= 13 => extraDirs("-2.13+")
      case _                       => Nil
    }
  },
  Test / unmanagedSourceDirectories ++= {
    val bd                                   = baseDirectory.value
    def extraDirs(suffix: String): Seq[File] = Seq(bd / "src" / "test" / s"scala$suffix")
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, y)) if y <= 12 => extraDirs("-2.12-")
      case Some((2, y)) if y >= 13 => extraDirs("-2.13+")
      case _                       => Nil
    }
  },
  Compile / doc / scalacOptions ++= Seq("-groups", "-implicits", "-no-link-warnings"),
  licenses                               := Seq(("LGPL-3.0", url("http://opensource.org/licenses/LGPL-3.0"))),
  sonatypeProfileName                    := "com.github.andyglow",
  publishMavenStyle                      := true,
  sonatypeProjectHosting                 := Some(GitHubHosting("andyglow", "scala-patch", "andyglow@gmail.com")),
  scmInfo                                := Some(
    ScmInfo(url("https://github.com/andyglow/scala-patch"), "scm:git@github.com:andyglow/scala-patch.git")
  ),
  developers                             := List(
    Developer(
      id = "andyglow",
      name = "Andriy Onyshchuk",
      email = "andyglow@gmail.com",
      url = url("https://ua.linkedin.com/in/andyglow")
    )
  ),
  releaseCrossBuild                      := true,
  releasePublishArtifactsAction          := PgpKeys.publishSigned.value,
  releaseProcess                         := Seq[ReleaseStep](
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
    pushChanges
  ),
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.13" % Test
)

lazy val core = (project in file("modules/core"))
  .enablePlugins(spray.boilerplate.BoilerplatePlugin)
  .settings(
    commons,
    name := "scala-gpl",
    scalacOptions ++= Seq("-language:implicitConversions", "-language:higherKinds")
  )

lazy val macros = (project in file("modules/macros"))
  .dependsOn(core)
  .settings(
    commons,
    name := "scala-gpl-macros",
    scalacOptions ++= Seq("-language:experimental.macros"),
    libraryDependencies ++= Seq(
      (scalaVersion apply ("org.scala-lang" % "scala-reflect" % _ % Compile)).value.withSources.withJavadoc
    )
  )

lazy val generic =
  (project in file("modules/generic")).dependsOn(core, macros).settings(commons, name := "scala-gpl-generic")

lazy val texts = (project in file("modules/texts"))
  .dependsOn(core)
  .settings(
    commons,
    name                                         := "scala-gpl-patch-text",
    libraryDependencies += "org.bitbucket.cowwoc" % "diff-match-patch" % "1.2"
  )

lazy val structuredMatchers = (project in file("modules/scalatest-structured-matchers"))
  .dependsOn(core, generic)
  .settings(
    commons,
    name                                   := "scalatest-structured-matchers",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.13"
  )

lazy val examples = (project in file("examples"))
  .dependsOn(core, generic)
  .settings(commons, name := "scala-gpl-examples", publish / skip := true, publishArtifact := false)

lazy val root = (project in file("."))
  .aggregate(core, macros, generic, texts, structuredMatchers, examples)
  .settings(
    commons,
    publish / skip     := true,
    publishArtifact    := false,
    update / aggregate := false,
    name               := "scala-gpl-root"
  )
