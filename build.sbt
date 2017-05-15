import sbt._
import Keys._
import com.twitter.sbt._

lazy val Versions = new {
  val rogue = "2.2.0"
  val joda = "2.9.7"
  val jodaConvert = "1.8.1"
  val playJson = "2.6.0-M7"
  val reactiveMongo = "0.12.3"
  val specs2 = "3.8.9-20170417195349-7b7973e"
}

 val sharedSettings: Seq[Def.Setting[_]] = Seq(
   organization := "com.outworkers",
   version := "0.1.0",
   scalaVersion := "2.11.8",
   crossScalaVersions := Seq("2.11.8", "2.12.1"),
   resolvers ++= Seq(
     Resolver.typesafeRepo("releases"),
     Resolver.sonatypeRepo("releases")
   ),

) ++ VersionManagement.newSettings ++ Publishing.effectiveSettings ++ Defaults.coreDefaultSettings


lazy val commonSettings = Seq(
  organization := "com.outworkers",
	
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8", "2.12.1"),
  version := "0.5.0.rc2",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  sonatypeProfileName := "com.whisk",
  pomExtra in Global := {
    <url>https://github.com/whisklabs/docker-it-scala</url>
      <scm>
        <connection>scm:git:github.com/whisklabs/reactiverogue.git</connection>
        <developerConnection>scm:git:git@github.com:whisklabs/reactiverogue.git</developerConnection>
        <url>github.com/whisklabs/reactiverogue.git</url>
      </scm>
      <developers>
        <developer>
          <id>viktortnk</id>
          <name>Viktor Taranenko</name>
          <url>https://github.com/viktortnk</url>
        </developer>
      </developers>
  }
)

def module(name: String) =
  Project(name, file(name))
    .settings(commonSettings: _*)
    .settings(
      scalacOptions ++= Seq("-feature", "-deprecation")
    )

lazy val root =
  project
    .in(file("."))
    .settings(commonSettings: _*)
    .settings(publish := {}, publishLocal := {}, packagedArtifacts := Map.empty)
    .aggregate(bson, core, recordDsl)

lazy val bson =
  module("reactiverogue-bson")
    .settings(
      libraryDependencies ++= Seq("org.reactivemongo" %% "reactivemongo-bson" % ReactivemongoVer))

lazy val core =
  module("reactiverogue-core")
    .dependsOn(bson)
    .settings(libraryDependencies ++= Seq(
      "org.reactivemongo" %% "reactivemongo" % ReactivemongoVer,
      "org.reactivemongo" %% "reactivemongo-iteratees" % ReactivemongoVer,
      "org.reactivemongo" %% "reactivemongo-play-json" % ReactivemongoVer,
      "com.typesafe.play" %% "play-json" % playVer.value
    ))

lazy val recordDsl =
  module("reactiverogue-record-dsl")
    .dependsOn(core)
    .settings(libraryDependencies ++= Seq(
      "junit" % "junit" % "4.12" % "test",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test",
      "com.whisk" %% "docker-testkit-scalatest" % "0.9.1" % "test",
      "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.1" % "test"
    ))
