import sbt._
import Keys._

object Belt extends Build {
  val sharedSettings = Seq(
    organization := "com.mosesn",
    name := "belt",
    scalaVersion := "2.10.2",
    crossScalaVersions := Seq("2.9.3", "2.10.2"),
    scalacOptions += "-deprecation",
    publishMavenStyle := true,
    publishTo <<= version { (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
              Some("snapshots" at nexus + "content/repositories/snapshots")
        else
              Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false },
    publishArtifact in Test := false,
    pomExtra := (
      <url>http://github.com/mosesn/belt</url>
      <licenses>
        <license>
          <name>MIT License</name>
          <url>http://opensource.org/licenses/mit-license.php</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:mosesn/belt.git</url>
        <connection>scm:git:git@github.com:mosesn/belt.git</connection>
      </scm>
      <developers>
        <developer>
          <id>mosesn</id>
          <name>Moses Nakamura</name>
          <url>https://github.com/mosesn</url>
        </developer>
      </developers>
    )
  )

  lazy val belt = Project(
    id = "belt",
    base = file(".")
  )
    .aggregate(core)
    .settings(sharedSettings: _*)
    .settings(publishArtifact := false)

  def project(name: String) = {
    val string = "belt-%s" format name
    Project(
      id = string,
      base = file(string)
    )
      .settings(sharedSettings: _*)
  }

  lazy val core = project("core")
}
