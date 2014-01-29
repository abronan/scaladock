organization := "com.scaladock"

name := "scaladock"

version := "0.0.1-SNAPSHOT"

description := "Scala client library for the Docker remote API"

licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

scalaVersion := "2.10.2"

crossScalaVersions := Seq("2.9.3", "2.10.2")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8")

resolvers ++= Seq(
  "Scala Tools Repo Releases" at "http://scala-tools.org/repo-releases",
  "sonatype-public" at "https://oss.sonatype.org/content/groups/public",
  "Typesafe Repo Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "spray repo" at "http://repo.spray.io"
)

libraryDependencies := Seq(
  "org.scalaz" %% "scalaz-core" % "7.0.5",
  "org.scalatest" % "scalatest_2.10" % "2.0.RC2" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.play" %% "play" % "2.2.0",
  "org.apache.commons" % "commons-compress" % "1.6",
  "org.scalaj" %% "scalaj-http" % "0.3.12",
  "net.liftweb" %% "lift-json" % "2.5.1",
  "com.jsuereth" %% "scala-arm" % "1.3",
  "com.decodified" %% "scala-ssh" % "0.6.4",
  "ch.qos.logback" % "logback-classic" % "1.0.13"
)

publishMavenStyle := true

publishArtifact in Test := false

publishTo <<= version {
  v: String =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := {
  x => false
}

pomExtra := (
  <url>http://github.com/abronan/scaladock</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:abronan/scaladock.git</url>
      <connection>scm:git:git@github.com:abronan/scaladock.git</connection>
    </scm>
    <developers>
      <developer>
        <id>abronan</id>
        <name>Alexandre Beslic</name>
        <url>http://github.com/abronan</url>
      </developer>
    </developers>
  )
