import sbt._
import Keys._
import com.twitter.sbt._

lazy val Versions = new {
  val rogue = "2.2.0"
  val joda = "2.9.7"
  val jodaConvert = "1.8.1"
  val playJson = "2.6.0-M7"
  val reactiveMongo = "0.12.3"
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
   scalacOptions ++= Seq(
       "-language:postfixOps",
       "-language:implicitConversions",
       "-language:reflectiveCalls",
       "-language:higherKinds",
       "-language:existentials",
       "-deprecation",
       "-feature",
       "-unchecked"
   )
) ++ VersionManagement.newSettings ++ Publishing.effectiveSettings ++ Defaults.coreDefaultSettings

lazy val reactiveRogue = Project(
  id = "reactiverogue",
  base = file("."),
  settings = Publishing.noPublishSettings
).aggregate(
  mongodb,
  record,
  core,
  dsl
)

lazy val mongodb = Project(
  id = "reactiverogue-mongodb",
  base = file("reactiverogue-mongodb"),
  settings = sharedSettings
).settings(
  name := "reactiverogue-mongodb",
  libraryDependencies ++= Seq(
    "joda-time"             %  "joda-time"              % Versions.joda,
    "org.joda"              %  "joda-convert"           % Versions.jodaConvert,
    "com.typesafe.play"     %% "play-json"              % Versions.playJson,
    "org.reactivemongo"     %% "reactivemongo"          % Versions.reactiveMongo
  )
)

lazy val record = Project(
  id = "reactiverogue-record",
  base = file("reactiverogue-record"),
  settings = sharedSettings
).settings(
  name := "reactiverogue-record",
  libraryDependencies ++= Seq(
    "com.typesafe.play"     %% "play-json"              % Versions.playJson
  )
).dependsOn(
  mongodb
)

lazy val core = Project(
  id = "reactiverogue-core",
  base = file("reactiverogue-core"),
  settings = sharedSettings
).settings(
  name := "reactiverogue-core"
).dependsOn(
  mongodb
)

lazy val dsl = Project(
  id = "reactiverogue-record-dsl",
  base = file("reactiverogue-record-dsl"),
  settings = sharedSettings
).settings(
  name := "reactiverogue-record-dsl",
  libraryDependencies ++= Seq(
    "junit"                 % "junit"                   % "4.5"         % Test,
    "com.novocode"          % "junit-interface"         % "0.6"         % Test,
    "org.specs2"            %% "specs2"                 % "1.12.3"      % Test
  )
).dependsOn(
  core,
  record
)
