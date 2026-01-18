organization := "edu.ncku"

version := "2.0"

name := "4-soc"

scalaVersion := "2.12.17"

val chiselVersion = "3.6.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:reflectiveCalls",
  "-Xcheckinit",
  "-P:chiselplugin:genBundleElements"
)

libraryDependencies ++= Seq(
  "edu.berkeley.cs" %% "chisel3" % chiselVersion,
  "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test"
)

addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full)