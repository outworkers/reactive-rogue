import sbt._
import Keys._
import com.twitter.sbt._

object ReactiveRogue extends Build {

  val publishSettings : Seq[sbt.Project.Setting[_]] = Seq(
    publishTo := Some("newzly releases" at "http://maven.newzly.com/repository/internal"),
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => true }
  )

   val sharedSettings: Seq[sbt.Project.Setting[_]] = Seq(
     organization := "com.websudos",
     version := "0.0.1",
     scalaVersion := "2.10.4",
     resolvers ++= Seq(
      "Sonatype repo"                    at "https://oss.sonatype.org/content/groups/scala-tools/",
      "Sonatype releases"                at "https://oss.sonatype.org/content/repositories/releases",
      "Sonatype snapshots"               at "https://oss.sonatype.org/content/repositories/snapshots",
      "Sonatype staging"                 at "http://oss.sonatype.org/content/repositories/staging",
      "Java.net Maven2 Repository"       at "http://download.java.net/maven/2/",
      "Websudos snapshots"               at "http://maven.websudos.co.uk/ext-release-snapshots",
      "Websudos repository"              at "http://maven.websudos.co.uk/ext-release-local",
      "Twitter Repository"               at "http://maven.twttr.com"
     ),
     unmanagedSourceDirectories in Compile <<= (scalaSource in Compile)(Seq(_)),
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
  ) ++ net.virtualvoid.sbt.graph.Plugin.graphSettings ++ VersionManagement.newSettings ++ publishSettings

  lazy val reactiveRogue = Project(
    id = "reactiverogue",
    base = file("."),
    settings = Project.defaultSettings ++ sharedSettings
  ).aggregate(
    mongodb,
    record,
    core,
    dsl
  )

  lazy val mongodb = Project(
    id = "reactiverogue-mongodb",
    base = file("reactiverogue-mongodb"),
    settings = Project.defaultSettings ++ sharedSettings
  ).settings(
    name := "reactiverogue-mongodb",
    libraryDependencies ++= Seq(
      "joda-time"             %  "joda-time"              % "2.1",
      "org.joda"              %  "joda-convert"           % "1.4",
      "com.typesafe.play"     %% "play-json"              % "2.2.2",
      "org.reactivemongo"     %% "play2-reactivemongo"    % "0.10.2",
      "org.reactivemongo"     %% "reactivemongo"          % "0.10.0"
    )
  )

  lazy val record = Project(
    id = "reactiverogue-record",
    base = file("reactiverogue-record"),
    settings = Project.defaultSettings ++ sharedSettings
  ).settings(
    name := "reactiverogue-record",
    libraryDependencies ++= Seq(
      "com.typesafe.play"     %% "play-json"              % "2.2.1",
      "org.reactivemongo"     %% "play2-reactivemongo"    % "0.10.2"
    )
  ).dependsOn(
    mongodb
  )

  lazy val core = Project(
    id = "reactiverogue-core",
    base = file("reactiverogue-core"),
    settings = Project.defaultSettings ++ sharedSettings
  ).settings(
    name := "reactiverogue-core",
    libraryDependencies ++= Seq(
       "com.foursquare"       %% "rogue-field"            % "2.2.0" intransitive(),
       "com.foursquare"       %% "rogue-index"            % "2.2.0" intransitive()
    )
  ).dependsOn(
    mongodb
  )

  lazy val dsl = Project(
    id = "reactiverogue-record-dsl",
    base = file("reactiverogue-record-dsl"),
    settings = Project.defaultSettings ++ sharedSettings
  ).settings(
    name := "reactiverogue-record-dsl",
    libraryDependencies ++= Seq(
      "junit"                 % "junit"                   % "4.5"         % "test, provided",
      "com.novocode"          % "junit-interface"         % "0.6"         % "test, provided",
      "org.specs2"            %% "specs2"                 % "1.12.3"      % "test, provided"
    )
  ).dependsOn(
    core,
    record
  )

}



