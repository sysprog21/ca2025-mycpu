// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv

import chisel3._
import chisel3.util._

/**
 * CPU implementation variants.
 *
 * Historical note: Values 0-2 were earlier implementations (single-cycle, stall-only,
 * basic forwarding) that have been superseded. Only FiveStageFinal (3) remains as
 * the production-ready 5-stage pipelined processor with full forwarding and early
 * branch resolution.
 */
object ImplementationType {
  val FiveStageFinal = 3
}

/**
 * System-wide configuration constants for the MyCPU RISC-V processor.
 *
 * These parameters define the processor's architectural width, memory layout,
 * and peripheral configuration. Changing these values affects hardware synthesis
 * and software compatibility.
 */
object Parameters {
  // RV32I: 32-bit address and data widths
  val AddrBits  = 32
  val AddrWidth = AddrBits.W

  val InstructionBits  = 32
  val InstructionWidth = InstructionBits.W
  val DataBits         = 32
  val DataWidth        = DataBits.W
  val ByteBits         = 8
  val ByteWidth        = ByteBits.W
  val WordSize         = Math.ceil(DataBits / ByteBits).toInt // 4 bytes per word

  // Register file: 32 architectural registers (x0-x31), x0 hardwired to zero
  val PhysicalRegisters         = 32
  val PhysicalRegisterAddrBits  = log2Up(PhysicalRegisters) // 5 bits
  val PhysicalRegisterAddrWidth = PhysicalRegisterAddrBits.W

  // CSR address space: 12-bit addresses per RISC-V privileged spec
  val CSRRegisterAddrBits  = 12
  val CSRRegisterAddrWidth = CSRRegisterAddrBits.W

  // Interrupt flag width matches mip/mie CSR registers
  val InterruptFlagBits  = 32
  val InterruptFlagWidth = InterruptFlagBits.W

  // Pipeline stall control encoding (legacy, kept for compatibility)
  val HoldStateBits   = 3
  val StallStateWidth = HoldStateBits.W

  // Memory configuration: 2MB main memory, VGA framebuffer is peripheral-internal
  val MemorySizeInBytes = 2097152 // 2MB
  val MemorySizeInWords = MemorySizeInBytes / 4

  // Program entry point: 0x1000 (after reset vector area)
  val EntryAddress = 0x1000.U(Parameters.AddrWidth)

  // AXI4-Lite bus topology: single master (CPU), 8 slave address regions
  // Address decoding uses upper 3 bits: 0x00-0x1F=RAM, 0x20-0x3F=VGA, etc.
  val MasterDeviceCount    = 1
  val SlaveDeviceCount     = 8
  val SlaveDeviceCountBits = log2Up(Parameters.SlaveDeviceCount) // 3 bits

  // Default timer interval: 1 second at 100MHz clock
  val TimerDefaultLimit = 100000000
}
