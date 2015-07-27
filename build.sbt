import sbt.ExclusionRule

name := "scala-common"

organization := "com.igumnov.scala"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies += "com.igumnov" % "common" % "9.3" excludeAll(
  ExclusionRule(organization = "org.eclipse.jetty"),
  ExclusionRule(organization = "com.fasterxml.jackson"),
  ExclusionRule(organization = "org.scala-lang.modules"))

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.6.0-1"
libraryDependencies += "org.eclipse.jetty" % "jetty-server" % "9.3.1.v20150714"
libraryDependencies += "org.eclipse.jetty" % "jetty-servlet" % "9.3.1.v20150714"
libraryDependencies += "org.eclipse.jetty" % "jetty-security" % "9.3.1.v20150714"
