// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "tw.edu.ncku"

val chiselVersion = "3.6.1"

// Root aggregate project
lazy val root = (project in file("."))
  .aggregate(common, minimal, singleCycle, mmioTrap, pipeline, soc)
  .settings(
    name := "mycpu-root"
  )

// Common shared module - hardware modules used by multiple projects
lazy val common = (project in file("common"))
  .settings(
    name := "mycpu-common",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test",
      "edu.berkeley.cs" %% "firrtl" % "1.6.0",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-feature",
      "-Xcheckinit",
      // Suppress deprecation warnings for legacy FIRRTL compiler usage
      // Shared module intentionally uses FIRRTL 1.6.0 for compatibility
      "-Wconf:cat=deprecation:s",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
  )

// 0-minimal: Minimal CPU supporting only jit.s (AUIPC, ADDI, LW, SW, JALR, ECALL)
lazy val minimal = (project in file("0-minimal"))
  .settings(
    name := "mycpu-minimal",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test",
      "edu.berkeley.cs" %% "firrtl" % "1.6.0",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-feature",
      "-Xcheckinit",
      "-Wconf:cat=deprecation:s",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
    Test / fork := true,
    Test / javaOptions += s"-Duser.dir=${(ThisBuild / baseDirectory).value}/0-minimal",
  )

// 1-single-cycle: Basic RV32I single-cycle processor
lazy val singleCycle = (project in file("1-single-cycle"))
  .dependsOn(common)
  .settings(
    name := "mycpu-single-cycle",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test",
      "edu.berkeley.cs" %% "firrtl" % "1.6.0",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-feature",
      "-Xcheckinit",
      "-Wconf:cat=deprecation:s",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
    Test / fork := true,
    Test / javaOptions += s"-Duser.dir=${(ThisBuild / baseDirectory).value}/1-single-cycle",
  )

// 2-mmio-trap: Single-cycle with MMIO peripherals and trap handling
lazy val mmioTrap = (project in file("2-mmio-trap"))
  .dependsOn(common)
  .settings(
    name := "mycpu-mmio-trap",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test",
      "edu.berkeley.cs" %% "firrtl" % "1.6.0",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-feature",
      "-Xcheckinit",
      "-Wconf:cat=deprecation:s",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
    Test / fork := true,
    Test / javaOptions += s"-Duser.dir=${(ThisBuild / baseDirectory).value}/2-mmio-trap",
  )

// 3-pipeline: Pipelined processor with forwarding
// Note: Does not depend on common due to architectural differences:
// - RegisterFile requires write forwarding for pipeline optimization
// - Parameters needs 4 implementation types (3-stage, 5-stage × 3 variants)
// - These differences are fundamental to the pipelined architecture
lazy val pipeline = (project in file("3-pipeline"))
  .settings(
    name := "mycpu-pipeline",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test",
      "edu.berkeley.cs" %% "firrtl" % "1.6.0",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-feature",
      "-Xcheckinit",
      "-Wconf:cat=deprecation:s",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
    Test / fork := true,
    Test / javaOptions += s"-Duser.dir=${(ThisBuild / baseDirectory).value}/3-pipeline",
  )

// 4-soc: System-on-Chip with AXI4-Lite bus, VGA, UART, and branch prediction
// Complete implementation without exercises - extends pipelined core with:
// - AXI4-Lite bus protocol for standardized peripheral access
// - VGA controller with 640x480@72Hz output and double-buffered framebuffer
// - UART controller with TX/RX buffering at 115200 baud
// - Branch prediction: BTB (32-entry) + RAS (4-entry) + IndirectBTB (8-entry)
lazy val soc = (project in file("4-soc"))
  .settings(
    name := "mycpu-soc",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test",
      "edu.berkeley.cs" %% "firrtl" % "1.6.0",
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-feature",
      "-Xcheckinit",
      "-Wconf:cat=deprecation:s",
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
    Test / fork := true,
    Test / javaOptions += s"-Duser.dir=${(ThisBuild / baseDirectory).value}/4-soc",
  )
