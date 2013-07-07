import sbt._
import Keys._

object Belt extends Build {
  val sharedSettings = Seq(
    organization := "com.mosesn",
    name := "belt",
    scalaVersion := "2.10.2",
    crossScalaVersions := Seq("2.9.3", "2.10.2")
  )

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
