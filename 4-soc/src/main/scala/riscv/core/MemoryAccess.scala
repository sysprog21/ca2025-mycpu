// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util._
import riscv.core.BusBundle
import riscv.Parameters

object MemoryAccessStates extends ChiselEnum {
  val Idle, Read, Write = Value
}

/**
 * Memory Access Stage (MEM) of the Pipeline
 *
 * Handles load/store operations with AXI4-Lite bus interface. Implements a
 * simple state machine for bus transactions with proper stall generation.
 *
 * Key Features:
 * - Load operations: LB, LBU, LH, LHU, LW with byte/halfword extraction
 * - Store operations: SB, SH, SW with byte strobes
 * - Pipeline stall generation during bus transactions
 * - Data forwarding to EX stage for load-use hazard mitigation
 * - Latched control signals to handle stall release timing
 *
 * State Machine:
 * - Idle: Monitor memory_read_enable/memory_write_enable, start transactions
 * - Read: Wait for bus.read_valid, extract data, release stall
 * - Write: Wait for bus.write_valid (BRESP), release stall
 *
 * Critical Timing:
 * - PipelineRegister is purely sequential (io.out := reg), NOT combinational
 *   bypass. During bus transactions, mem_stall keeps pipeline registers frozen,
 *   so io.funct3 and other signals remain stable.
 * - Control signals (regs_write_source, regs_write_address, regs_write_enable)
 *   are latched for MEM2WB to ensure correct writeback after read completes.
 * - forward_to_ex uses latched values ONLY while in Read state; after
 *   completion, the new instruction's values must be used.
 */
class MemoryAccess extends Module {
  val io = IO(new Bundle() {
    val alu_result          = Input(UInt(Parameters.DataWidth))                 // used as memory address
    val reg2_data           = Input(UInt(Parameters.DataWidth))
    val memory_read_enable  = Input(Bool())
    val memory_write_enable = Input(Bool())
    val funct3              = Input(UInt(3.W))
    val regs_write_source   = Input(UInt(2.W))
    val regs_write_address  = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // destination register
    val regs_write_enable   = Input(Bool())                                     // register write enable
    val csr_read_data       = Input(UInt(Parameters.DataWidth))
    val instruction_address = Input(UInt(Parameters.AddrWidth))                 // For JAL/JALR forwarding (PC+4)

    val wb_memory_read_data = Output(UInt(Parameters.DataWidth))
    val forward_to_ex       = Output(UInt(Parameters.DataWidth))
    val ctrl_stall_flag     = Output(Bool()) // stall when memory access is not finished
    // Output the correct regs_write_* for MEM2WB pipeline register
    // These preserve the load instruction's values when stall releases
    val wb_regs_write_source  = Output(UInt(2.W))
    val wb_regs_write_address = Output(UInt(Parameters.PhysicalRegisterAddrWidth))
    val wb_regs_write_enable  = Output(Bool())

    val bus = new BusBundle
  })
  val mem_address_index = io.alu_result(log2Up(Parameters.WordSize) - 1, 0).asUInt
  val mem_access_state  = RegInit(MemoryAccessStates.Idle)

  // Register to hold the loaded data across the stall release cycle
  // This ensures the data persists until MEM2WB captures it
  val latched_memory_read_data = RegInit(0.U(Parameters.DataWidth))

  // Capture control signals when entering Read state
  // Although PipelineRegister is purely sequential (io.out := reg), we still latch
  // regs_write_source/address/enable for MEM2WB because when read_valid arrives,
  // mem_stall releases and MEM2WB captures at the clock edge. The latched values
  // ensure the load instruction's writeback info is preserved for one extra cycle.
  val latched_regs_write_source  = RegInit(0.U(2.W))
  val latched_regs_write_address = RegInit(0.U(Parameters.PhysicalRegisterAddrWidth))
  val latched_regs_write_enable  = RegInit(false.B)
  // Note: latched_funct3 and latched_address_index were removed - not needed because
  // PipelineRegister is purely sequential and these signals stay stable during stall

  // Track when a read just completed so we can extend the validity of
  // latched control signals for one more cycle. This handles the case where the
  // dependent instruction arrives in EX on the cycle AFTER the load completes.
  // Without this, forward_to_ex would show the wrong value (ALU result instead of
  // loaded data) because effective_regs_write_source switches too early.
  val read_just_completed = RegInit(false.B)

  // Helper for common transaction completion logic (state machine reset only)
  def on_bus_transaction_finished() = {
    mem_access_state   := MemoryAccessStates.Idle
    io.ctrl_stall_flag := false.B
    // NOTE: read_just_completed is set ONLY in Read completion path, NOT here
    // Setting it for Write completions was a bug causing wrong wb_regs_write_source
  }

  // Clear read_just_completed on the cycle after it was set.
  // This ensures latched values are used for exactly one cycle after completion.
  // CRITICAL FIX: The original implementation had a bug where this signal would
  // not be cleared if a new memory transaction started in the same cycle, causing
  // the signal to get "stuck" high. The corrected logic is unconditional.
  when(read_just_completed) {
    read_just_completed := false.B
  }

  io.bus.request := false.B
  io.bus.read    := false.B
  io.bus.address := io.alu_result(Parameters.AddrBits - 1, log2Up(Parameters.WordSize)) ## 0.U(
    log2Up(Parameters.WordSize).W
  )
  io.bus.write_data      := 0.U
  io.bus.write_strobe    := VecInit(Seq.fill(Parameters.WordSize)(false.B))
  io.bus.write           := false.B
  io.wb_memory_read_data := latched_memory_read_data // Use latched value
  io.ctrl_stall_flag     := false.B

  // Misaligned access handling:
  // RISC-V spec allows implementation-defined behavior for misaligned accesses.
  // Current implementation supports within-word misalignment for byte and halfword:
  // - LB/LBU/SB: Always aligned (single byte), fully supported
  // - LH/LHU/SH at offset 0,1,2: Supported (bytes within same word)
  // - LH/LHU/SH at offset 3: Best-effort (returns bytes 2-3, crosses word boundary)
  // - LW/SW at offset 0: Supported (word-aligned)
  // - LW/SW at non-zero offset: Unsupported (crosses word boundary, returns partial data)
  // Cross-word-boundary accesses would require two bus transactions and are not implemented.
  // For strict compliance with exception-based handling, add misalignment trap logic.

  // State machine: handle Read/Write completion FIRST (independent of enable signals)
  // This fixes a critical bug where the state machine would get stuck if the pipeline
  // moved on (enable went low) before the bus transaction completed.
  when(mem_access_state === MemoryAccessStates.Read) {
    // In Read state: wait for read_valid, keep stalling
    io.bus.request     := true.B
    io.ctrl_stall_flag := true.B
    when(io.bus.read_valid) {
      val data = io.bus.read_data
      // Compute the processed data (byte/halfword extraction with sign extension)
      // Use io.funct3 and mem_address_index directly - PipelineRegister is purely
      // sequential (io.out := reg), NOT combinational bypass, so these signals
      // remain stable during the entire bus transaction while mem_stall is asserted.
      val processed_data = MuxLookup(
        io.funct3,
        0.U,
        IndexedSeq(
          InstructionsTypeL.lb -> MuxLookup(
            mem_address_index,
            Cat(Fill(24, data(31)), data(31, 24)),
            IndexedSeq(
              0.U -> Cat(Fill(24, data(7)), data(7, 0)),
              1.U -> Cat(Fill(24, data(15)), data(15, 8)),
              2.U -> Cat(Fill(24, data(23)), data(23, 16))
            )
          ),
          InstructionsTypeL.lbu -> MuxLookup(
            mem_address_index,
            Cat(Fill(24, 0.U), data(31, 24)),
            IndexedSeq(
              0.U -> Cat(Fill(24, 0.U), data(7, 0)),
              1.U -> Cat(Fill(24, 0.U), data(15, 8)),
              2.U -> Cat(Fill(24, 0.U), data(23, 16))
            )
          ),
          InstructionsTypeL.lh -> MuxLookup(
            mem_address_index,
            Cat(Fill(16, data(31)), data(31, 16)), // offset 3: best-effort (crosses word boundary)
            IndexedSeq(
              0.U -> Cat(Fill(16, data(15)), data(15, 0)), // bytes 0-1
              1.U -> Cat(Fill(16, data(23)), data(23, 8)), // bytes 1-2
              2.U -> Cat(Fill(16, data(31)), data(31, 16)) // bytes 2-3
            )
          ),
          InstructionsTypeL.lhu -> MuxLookup(
            mem_address_index,
            Cat(Fill(16, 0.U), data(31, 16)), // offset 3: best-effort (crosses word boundary)
            IndexedSeq(
              0.U -> Cat(Fill(16, 0.U), data(15, 0)), // bytes 0-1
              1.U -> Cat(Fill(16, 0.U), data(23, 8)), // bytes 1-2
              2.U -> Cat(Fill(16, 0.U), data(31, 16)) // bytes 2-3
            )
          ),
          InstructionsTypeL.lw -> data
        )
      )
      // Store in register for persistence after read_valid goes low
      latched_memory_read_data := processed_data
      // Also output immediately for forwarding on THIS cycle
      // Without this, the forwarding path would see the old latch value (0)
      io.wb_memory_read_data := processed_data
      // Signal that a READ just completed - used by wb_effective_regs_write_source MUX
      // to extend latched control signals for one more cycle
      read_just_completed := true.B
      on_bus_transaction_finished()
    }
  }.elsewhen(mem_access_state === MemoryAccessStates.Write) {
    // In Write state: wait for write_valid (BRESP) to complete transaction
    // CRITICAL: Must keep stall asserted until BRESP received.
    // The posted-write optimization (releasing stall on write_data_accepted)
    // was buggy: while BRESP is pending, the pipeline advances but the state
    // machine remains in Write state, causing any new load/store in MEM stage
    // to be ignored (the .otherwise block only runs in Idle state).
    // Conservative fix: stall until write completion to ensure correctness.
    io.bus.request     := true.B
    io.ctrl_stall_flag := true.B

    when(io.bus.write_valid) {
      on_bus_transaction_finished()
    }
  }.otherwise {
    // Idle state: check enable signals to start new transactions
    when(io.memory_read_enable) {
      // Start the read transaction when the bus is available
      io.ctrl_stall_flag := true.B
      io.bus.read        := true.B
      io.bus.request     := true.B
      // Capture control signals for MEM2WB when read starts
      // These are latched so that when read_valid arrives and stall releases,
      // MEM2WB can still capture the correct writeback info for the load instruction
      latched_regs_write_source  := io.regs_write_source
      latched_regs_write_address := io.regs_write_address
      latched_regs_write_enable  := io.regs_write_enable
      when(io.bus.granted) {
        mem_access_state := MemoryAccessStates.Read
      }
    }.elsewhen(io.memory_write_enable) {
      // Start the write transaction when the bus is available
      io.ctrl_stall_flag  := true.B
      io.bus.write_data   := io.reg2_data
      io.bus.write        := true.B
      io.bus.write_strobe := VecInit(Seq.fill(Parameters.WordSize)(false.B))
      when(io.funct3 === InstructionsTypeS.sb) {
        io.bus.write_strobe(mem_address_index) := true.B
        // Fix: Use ByteBits-1 for correct 8-bit slice (was ByteBits which gave 9 bits)
        io.bus.write_data := io.reg2_data(Parameters.ByteBits - 1, 0) << (mem_address_index << log2Up(
          Parameters.ByteBits
        ).U)
      }.elsewhen(io.funct3 === InstructionsTypeS.sh) {
        when(mem_address_index === 0.U) {
          // Offset 0: write to bytes 0-1
          io.bus.write_strobe(0) := true.B
          io.bus.write_strobe(1) := true.B
          io.bus.write_data      := io.reg2_data(15, 0)
        }.elsewhen(mem_address_index === 1.U) {
          // Offset 1: write to bytes 1-2
          io.bus.write_strobe(1) := true.B
          io.bus.write_strobe(2) := true.B
          io.bus.write_data      := io.reg2_data(15, 0) << 8.U
        }.elsewhen(mem_address_index === 2.U) {
          // Offset 2: write to bytes 2-3
          io.bus.write_strobe(2) := true.B
          io.bus.write_strobe(3) := true.B
          io.bus.write_data      := io.reg2_data(15, 0) << 16.U
        }.otherwise {
          // Offset 3: best-effort, write to bytes 2-3 (crosses word boundary)
          io.bus.write_strobe(2) := true.B
          io.bus.write_strobe(3) := true.B
          io.bus.write_data      := io.reg2_data(15, 0) << 16.U
        }
      }.elsewhen(io.funct3 === InstructionsTypeS.sw) {
        for (i <- 0 until Parameters.WordSize) {
          io.bus.write_strobe(i) := true.B
        }
      }
      io.bus.request := true.B
      when(io.bus.granted) {
        mem_access_state := MemoryAccessStates.Write
      }
    }
  }

  // CRITICAL: Forwarding and Writeback have DIFFERENT timing requirements!
  //
  // When a load completes (read_just_completed = true):
  // - State transitions: Read → Idle (same cycle as read_valid)
  // - mem_stall releases, allowing MEM2WB to capture at clock edge
  // - We use latched values for writeback to preserve the load's info
  //
  // For FORWARDING (forward_to_ex):
  // - Only use latched values while IN Read state (during the bus transaction)
  // - After completion, the NEW instruction is in MEM stage, so use its values
  // - This allows correct forwarding from the new instruction to EX stage
  //
  // For WRITEBACK (wb_regs_write_source):
  // - Must use latched values for one EXTRA cycle after read completes
  // - Because MEM2WB captures at clock edge AFTER read_just_completed is set
  // - Without this, MEM2WB captures the NEW instruction's regs_write_source,
  //   causing WB to use ALU result instead of loaded data!
  //
  // Bug pattern this fixes:
  // - LW completes → read_just_completed = true
  // - io.regs_write_source = FromALU (from next instruction)
  // - MEM2WB captures FromALU instead of FromMemory
  // - WB writes ALU result to register instead of loaded data!

  // For writeback: only use latched values while waiting for read_valid
  // When read_valid arrives, ex2mem still holds the LOAD so io.* are correct
  // BUG FIX: Using in_read_or_just_completed caused NEW instruction's rd to be
  // replaced by latched rd when read_just_completed was true
  val in_active_read = mem_access_state === MemoryAccessStates.Read && !io.bus.read_valid

  // For FORWARDING data source selection (forward_to_ex)
  val forward_regs_write_source = Mux(
    in_active_read,
    latched_regs_write_source,
    io.regs_write_source
  )

  val wb_effective_regs_write_source = Mux(
    in_active_read,
    latched_regs_write_source,
    io.regs_write_source
  )
  val wb_effective_regs_write_address = Mux(
    in_active_read,
    latched_regs_write_address,
    io.regs_write_address
  )
  val wb_effective_regs_write_enable = Mux(
    in_active_read,
    latched_regs_write_enable,
    io.regs_write_enable
  )

  // Forward to EX stage: Select correct data source based on instruction type
  // - Memory loads (FromMemory): forward loaded data
  // - CSR instructions (FromCSR): forward CSR read data
  // - JAL/JALR (NextInstructionAddress): forward PC+4 (return address)
  // - ALU operations (default): forward ALU result
  io.forward_to_ex := MuxLookup(forward_regs_write_source, io.alu_result)(
    Seq(
      RegWriteSource.Memory                 -> io.wb_memory_read_data,
      RegWriteSource.CSR                    -> io.csr_read_data,
      RegWriteSource.NextInstructionAddress -> (io.instruction_address + 4.U)
    )
  )

  // Pass to MEM2WB pipeline register for WB stage data source selection
  io.wb_regs_write_source  := wb_effective_regs_write_source
  io.wb_regs_write_address := wb_effective_regs_write_address
  io.wb_regs_write_enable  := wb_effective_regs_write_enable
}
