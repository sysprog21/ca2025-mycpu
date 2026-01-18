// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import java.nio.file.Files
import java.nio.file.Paths

import chiseltest.VerilatorBackendAnnotation
import chiseltest.WriteVcdAnnotation
object VerilatorEnabler {
  val annos = if (sys.env.contains("Path")) {
    if (
      sys.env
        .getOrElse("Path", "")
        .split(";")
        .exists(path => {
          Files.exists(Paths.get(path, "verilator"))
        })
    ) {
      Seq(VerilatorBackendAnnotation)
    } else {
      Seq()
    }
  } else {
    if (
      sys.env
        .getOrElse("PATH", "")
        .split(":")
        .exists(path => {
          Files.exists(Paths.get(path, "verilator"))
        })
    ) {
      Seq(VerilatorBackendAnnotation)
    } else {
      Seq()
    }
  }
}

object WriteVcdEnabler {
  val annos = if (sys.env.contains("WRITE_VCD")) {
    Seq(WriteVcdAnnotation)
  } else {
    Seq()
  }
}

object TestAnnotations {
  val annos = VerilatorEnabler.annos ++ WriteVcdEnabler.annos
}
