// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import riscv.Parameters

/**
 * Advanced Hazard Detection and Control Unit: Maximum optimization
 *
 * Most sophisticated hazard detection supporting early branch resolution
 * in ID stage with comprehensive forwarding support. Achieves best
 * performance through aggressive optimization.
 *
 * Key Enhancements:
 * - Early branch resolution: Branches resolved in ID stage (not EX)
 * - ID-stage forwarding: Enables immediate branch operand comparison
 * - Complex hazard detection: Handles jump dependencies and multi-stage loads
 *
 * ============================================================================
 * TIMING DIAGRAMS FOR HAZARD SCENARIOS
 * ============================================================================
 *
 * Legend:
 *   [XXX]  = Instruction in pipeline stage
 *   [bub]  = Bubble (NOP inserted due to stall/flush)
 *   [hold] = Stage held (instruction not advancing)
 *   --->   = Data forwarding path
 *
 * ----------------------------------------------------------------------------
 * 1. LOAD-USE HAZARD (1-cycle stall)
 * ----------------------------------------------------------------------------
 *
 *   Cycle:     1       2       3       4       5       6
 *   IF:      [LW]    [ADD]   [hold]  [next]  [...]   [...]
 *   ID:              [LW]    [ADD]   [ADD]   [next]  [...]
 *   EX:                      [LW]    [bub]   [ADD]   [next]
 *   MEM:                             [LW]──> [bub]   [ADD]
 *   WB:                                 │    [LW]    [bub]
 *                                       │
 *                            Cycle 3: ADD held in ID, bubble in EX
 *                            Cycle 4: LW in MEM forwards to ADD in EX
 *
 *   Code:  LW  x1, 0(x2)    ; load x1
 *          ADD x3, x1, x4   ; uses x1 → 1-cycle stall, then MEM→EX forward
 *
 * ----------------------------------------------------------------------------
 * 2. BRANCH HAZARD WITH EX DEPENDENCY (suppress + re-evaluate)
 * ----------------------------------------------------------------------------
 *
 *   Cycle:     1       2       3       4       5       6
 *   IF:      [ADD]   [BEQ]   [hold]  [tgt]   [...]   [...]
 *   ID:              [ADD]   [BEQ]   [BEQ]   [tgt]   [...]
 *   EX:                      [ADD]   [bub]   [BEQ]   [tgt]
 *   MEM:                             [ADD]──>        [BEQ]
 *   WB:                                 │    [ADD]   [...]
 *                                       │
 *                            Cycle 3: BEQ held in ID (branch_hazard=true)
 *                            Cycle 4: ADD in MEM forwards to BEQ in ID
 *                                     BEQ re-evaluates with correct value
 *
 *   Code:  ADD x1, x2, x3   ; compute x1
 *          BEQ x1, x4, label ; uses x1 → hold 1 cycle, forward from MEM→ID
 *
 *   Note: Branch stays in ID and re-evaluates with forwarded value.
 *         No id_flush needed (unlike load-use which flushes EX).
 *
 * ----------------------------------------------------------------------------
 * 3. JAL/JALR HAZARD (3-stage detection, critical for sw ra)
 * ----------------------------------------------------------------------------
 *
 *   JAL computes rd=PC+4 in WB stage, NOT in EX. Forwarding from MEM gives
 *   wrong value (ALU result = jump target address, not return address).
 *
 *   Cycle:     1       2       3       4       5       6       7
 *   IF:      [JAL]   [ADDI]  [SW]    [hold]  [hold]  [next]  [...]
 *   ID:              [JAL]   [ADDI]  [SW]    [SW]    [SW]    [next]
 *   EX:                      [JAL]   [ADDI]  [bub]   [bub]   [SW]
 *   MEM:                             [JAL]   [ADDI]  [bub]   [bub]
 *   WB:                                      [JAL]──>[ADDI]  [bub]
 *                                               │
 *                            Cycles 4-5: SW held in ID waiting for JAL
 *                            Cycle 5: JAL in WB, PC+4 now available
 *                            Cycle 6: SW proceeds with correct ra value
 *
 *   Code:  JAL  ra, func      ; ra = PC+4 (computed in WB)
 *          ADDI sp, sp, -16   ; doesn't use ra, OK
 *          SW   ra, 12(sp)    ; reads ra → MUST wait for JAL to reach WB
 *
 *   Detection points:
 *   - jal_jalr_hazard_ex:  JAL in EX,  SW in ID → stall 2 cycles
 *   - jal_jalr_hazard_mem: JAL in MEM, SW in ID → stall 1 cycle
 *   - jal_jalr_hazard_wb:  JAL in WB,  SW in ID → stall 1 cycle (reg delay)
 *
 * ----------------------------------------------------------------------------
 * 4. STORE-LOAD HAZARD (same address, sequential access)
 * ----------------------------------------------------------------------------
 *
 *   Cycle:     1       2       3       4       5       6
 *   IF:      [SW]    [LW]    [hold]  [next]  [...]   [...]
 *   ID:              [SW]    [LW]    [LW]    [next]  [...]
 *   EX:                      [SW]    [bub]   [LW]    [next]
 *   MEM:                             [SW]    [bub]   [LW]
 *   WB:                                      [SW]    [bub]   [LW]
 *                                             │
 *                            Cycle 3: LW held, SW completing in MEM
 *                            Cycle 4: SW done, LW proceeds
 *                            Without stall, LW would read stale data
 *
 *   Code:  SW a0, -20(s0)    ; store pointer to stack
 *          LW a5, -20(s0)    ; load from same address → must wait
 *
 * ----------------------------------------------------------------------------
 * 5. CONTROL HAZARD - BRANCH TAKEN (1-cycle penalty with early resolution)
 * ----------------------------------------------------------------------------
 *
 *   Cycle:     1       2       3       4       5
 *   IF:      [BEQ]   [wrong] [tgt]   [...]   [...]
 *   ID:              [BEQ]   [bub]   [tgt]   [...]
 *   EX:                      [bub]   [bub]   [tgt]
 *   MEM:                             [bub]   [bub]
 *   WB:                                      [bub]
 *                             │
 *                    Cycle 2: BEQ resolves in ID, flushes IF
 *                             [wrong] becomes [bub] in ID next cycle
 *                    Cycle 3: Target instruction fetched
 *
 *   Code:  BEQ x1, x2, label  ; branch taken, resolved in ID
 *          ADD x3, x4, x5     ; <-- flushed (wrong path)
 *   label: SUB x6, x7, x8     ; fetched after 1-cycle penalty
 *
 *   Note: Branch resolves in ID (not EX), so only IF is flushed.
 *         This gives 1-cycle penalty vs 2 if resolved in EX.
 *
 * ----------------------------------------------------------------------------
 * 6. BTB/RAS CORRECT PREDICTION (0-cycle penalty)
 * ----------------------------------------------------------------------------
 *
 *   Cycle:     1       2       3       4       5
 *   IF:      [BEQ]   [tgt]   [...]   [...]   [...]
 *   ID:              [BEQ]   [tgt]   [...]   [...]
 *   EX:                      [BEQ]   [tgt]   [...]
 *   MEM:                             [BEQ]   [tgt]
 *   WB:                                      [BEQ]
 *                             │
 *                    Cycle 2: BEQ resolves in ID, confirms BTB prediction
 *                             prediction_correct=true suppresses if_flush
 *                             No bubble - [tgt] already in IF!
 *
 *   With BTB hit and correct prediction:
 *   - IF stage already fetched from predicted target
 *   - ID stage confirms prediction matches actual target
 *   - prediction_correct signal suppresses if_flush
 *
 * ----------------------------------------------------------------------------
 * 7. LOAD-TO-BRANCH HAZARD (branch operand depends on load)
 * ----------------------------------------------------------------------------
 *
 *   Cycle:     1       2       3       4       5       6
 *   IF:      [LW]    [BEQ]   [hold]  [tgt]   [...]   [...]
 *   ID:              [LW]    [BEQ]   [BEQ]   [tgt]   [...]
 *   EX:                      [LW]    [bub]   [BEQ]   [tgt]
 *   MEM:                             [LW]──> [bub]   [BEQ]
 *   WB:                                 │    [LW]    [bub]
 *                                       │
 *                            Cycle 3: BEQ held, LW in MEM
 *                            Cycle 4: LW result forwards MEM→ID to BEQ
 *
 *   Code:  LW  x1, 0(x2)    ; load x1
 *          BEQ x1, x4, label ; uses x1 → 1-cycle stall for MEM→ID forward
 *
 *   Similar to load-use but branch held in ID (not flushed) for
 *   re-evaluation with forwarded load result
 *
 * ============================================================================
 * FORWARDING PRIORITY AND DATA PATHS
 * ============================================================================
 *
 *   Priority order (newest data wins):
 *   1. MEM stage result (1 cycle old)
 *   2. WB stage result  (2 cycles old)
 *   3. Register file    (committed value)
 *
 *                    +--------+     +--------+
 *                    |  MEM   |---->|   ID   | (ID-stage forwarding for branches)
 *                    +--------+     +--------+
 *                         |              ^
 *                         |              |
 *                         v              |
 *                    +--------+     +--------+
 *                    |   WB   |---->|   EX   | (EX-stage forwarding for ALU)
 *                    +--------+     +--------+
 *
 * ============================================================================
 *
 * Hazard Types and Resolution:
 * 1. Control Hazards:
 *    - Branch taken in ID → flush only IF stage (1 cycle penalty)
 *    - Jump in ID → may need stall if operands not ready
 *
 * 2. Data Hazards:
 *    - Load-use for ALU → 1 cycle stall
 *    - Load-use for branch → 1-2 cycle stall depending on stage
 *    - Jump register dependencies → stall until operands ready
 *
 * Performance Impact:
 * - CPI ~1.05-1.2 (best achievable)
 * - Branch penalty reduced to 1 cycle (0 with BTB/RAS)
 * - Minimal stalls through aggressive forwarding
 *
 * @note Most complex control logic but best performance
 * @note Requires ID-stage forwarding paths for full benefit
 */
class Control extends Module {
  val io = IO(new Bundle {
    val jump_flag               = Input(Bool())                                     // id.io.if_jump_flag
    val jump_instruction_id     = Input(Bool())                                     // id.io.ctrl_jump_instruction           //
    val rs1_id                  = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id.io.regs_reg1_read_address
    val rs2_id                  = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id.io.regs_reg2_read_address
    val memory_read_enable_ex   = Input(Bool())                                     // id2ex.io.output_memory_read_enable
    val rd_ex                   = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // id2ex.io.output_regs_write_address
    val memory_read_enable_mem  = Input(Bool())                                     // ex2mem.io.output_memory_read_enable   //
    val rd_mem                  = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // ex2mem.io.output_regs_write_address   //
    val memory_write_enable_ex  = Input(Bool())                                     // id2ex.io.output_memory_write_enable
    val memory_write_enable_mem = Input(Bool())                                     // ex2mem.io.output_memory_write_enable
    val regs_write_enable_ex    = Input(Bool())                                     // id2ex.io.output_regs_write_enable
    val regs_write_source_ex    = Input(UInt(2.W))                                  // id2ex.io.output_regs_write_source
    val regs_write_source_mem   = Input(UInt(2.W))                                  // ex2mem.io.output_regs_write_source
    // WB stage signals for JAL/JALR hazard detection
    val regs_write_source_wb = Input(UInt(2.W))                                  // mem2wb.io.output_regs_write_source
    val rd_wb                = Input(UInt(Parameters.PhysicalRegisterAddrWidth)) // mem2wb.io.output_regs_write_address

    val if_flush = Output(Bool())
    val id_flush = Output(Bool())
    val pc_stall = Output(Bool())
    val if_stall = Output(Bool())
    // Signal to suppress branch decision when RAW hazard exists
    // Without this, branch comparison uses stale forwarded value from wrong pipeline stage
    val branch_hazard = Output(Bool())
    // Signal indicating JAL/JALR hazard - must NOT be suppressed by mem_stall
    // JAL/JALR hazards require immediate id_flush even during memory operations
    val jal_jalr_hazard = Output(Bool())
  })

  // Initialize control signals to default (no stall/flush) state
  io.if_flush        := false.B
  io.id_flush        := false.B
  io.pc_stall        := false.B
  io.if_stall        := false.B
  io.branch_hazard   := false.B
  io.jal_jalr_hazard := false.B

  // Detect RAW hazard: branch/jump in ID needs value from EX stage (not yet computed)
  // When this is true, forwarding would get wrong value from MEM stage
  val ex_hazard_for_branch = io.jump_instruction_id &&
    io.rd_ex =/= 0.U &&
    (io.rd_ex === io.rs1_id || io.rd_ex === io.rs2_id)

  // ============ Store-Load Hazard Detection ============
  // When a store is in MEM stage and a load is in EX stage, we must stall the load.
  // Without this, the load could start reading from memory before the store writes,
  // returning stale data (0) instead of the just-stored value.
  //
  // Example triggering this hazard:
  //   SW a0, -20(s0)   [MEM] - storing pointer to stack
  //   LW a5, -20(s0)   [EX]  - about to load same address → gets stale data!
  //
  // Fix: Stall when store in MEM and load in EX. The MemoryAccess state machine
  // will handle the actual memory transaction timing, but we need to prevent
  // the load from starting until the store completes.
  val store_load_hazard = io.memory_write_enable_mem && io.memory_read_enable_ex

  // ============ JAL/JALR Hazard Detection ============
  // JAL/JALR compute rd=PC+4 in WriteBack stage, NOT EX stage.
  // Therefore, forwarding from MEM stage gives the WRONG value (ALU result, not PC+4).
  // We must stall until JAL/JALR reaches WB where the correct value is computed.
  //
  // Example triggering this hazard:
  //   JAL ra, func   [EX]  - will write ra=PC+4 in WB, but MEM forwards ALU result (jump target)
  //   ADDI sp, sp, -16 [ID] - doesn't read ra, OK
  //   SW ra, 12(sp)   [IF→ID next cycle] - reads ra, needs correct PC+4 value
  //
  // When sw reaches ID and jal is in MEM (not WB yet), forwarding from MEM gives wrong value.
  // Solution: Stall when EX has JAL/JALR and ID reads the destination register.
  val jal_jalr_hazard_ex = (io.regs_write_source_ex === RegWriteSource.NextInstructionAddress) &&
    io.rd_ex =/= 0.U &&
    (io.rd_ex === io.rs1_id || io.rd_ex === io.rs2_id)

  // ============ JAL/JALR Hazard in MEM Stage ============
  // Similar to EX stage, but for when JAL/JALR has advanced to MEM.
  // The PC+4 is still not available until WB. If ID reads rd_mem, we must stall.
  //
  // Example triggering this hazard:
  //   JAL ra, func    [MEM] - will write ra=PC+4 in WB
  //   ADDI sp, sp, -X [EX]  - doesn't read ra
  //   SW ra, Y(sp)    [ID]  - reads ra, needs correct PC+4 value
  //
  // Without this stall, forwarding from MEM gives wrong value (ALU result/jump target).
  val jal_jalr_hazard_mem = (io.regs_write_source_mem === RegWriteSource.NextInstructionAddress) &&
    io.rd_mem =/= 0.U &&
    (io.rd_mem === io.rs1_id || io.rd_mem === io.rs2_id)

  // ============ JAL/JALR Hazard in WB Stage ============
  // CRITICAL: Due to pipeline register delay, when JAL/JALR enters WB at a rising edge,
  // mem2wb.io.output still reflects the PREVIOUS cycle's values until after combinational
  // logic settles. If ID tries to read from forwarding at this exact cycle, it gets the
  // wrong rd_wb (from the bubble/previous instruction, not JAL).
  //
  // Example timing:
  //   Cycle N:   JAL in MEM, hazard_mem detected, stall/flush
  //   Cycle N+1: JAL enters WB (at rising edge), but mem2wb output shows bubble
  //              Forwarding sees rd_wb=0 (bubble), no match, captures stale register value
  //
  // Solution: Also detect when JAL/JALR is in WB stage. This gives the pipeline one more
  // cycle to propagate JAL's values through mem2wb before allowing dependent instructions.
  val jal_jalr_hazard_wb = (io.regs_write_source_wb === RegWriteSource.NextInstructionAddress) &&
    io.rd_wb =/= 0.U &&
    (io.rd_wb === io.rs1_id || io.rd_wb === io.rs2_id)

  // Complex hazard detection for early branch resolution in ID stage
  when(
    // ============ Complex Hazard Detection Logic ============
    // This condition detects multiple hazard scenarios requiring stalls:

    // --- Condition 1: EX stage hazards (1-cycle dependencies) ---
    ((io.jump_instruction_id || io.memory_read_enable_ex) && // Either:
      // - Jump in ID needs register value, OR
      // - Load in EX (load-use hazard)
      io.rd_ex =/= 0.U &&                                 // Destination is not x0
      (io.rd_ex === io.rs1_id || io.rd_ex === io.rs2_id)) // Destination matches ID source
    //
    // Examples triggering Condition 1:
    // a) Jump dependency: ADD x1, x2, x3 [EX]; JALR x0, x1, 0 [ID] → stall
    // b) Load-use: LW x1, 0(x2) [EX]; ADD x3, x1, x4 [ID] → stall
    // c) Load-branch: LW x1, 0(x2) [EX]; BEQ x1, x4, label [ID] → stall

      || // OR

        // --- Condition 2: MEM stage load with jump dependency (2-cycle) ---
        (io.jump_instruction_id &&                              // Jump instruction in ID
          io.memory_read_enable_mem &&                          // Load instruction in MEM
          io.rd_mem =/= 0.U &&                                  // Load destination not x0
          (io.rd_mem === io.rs1_id || io.rd_mem === io.rs2_id)) // Load dest matches jump source
        //
        // Example triggering Condition 2:
        // LW x1, 0(x2) [MEM]; NOP [EX]; JALR x0, x1, 0 [ID]
        // Even with forwarding, load result needs extra cycle to reach ID stage

        || // OR

        // --- Condition 3: Store-load hazard ---
        store_load_hazard
        // Store in MEM stage, load in EX stage - must wait for store to complete

        || // OR

        // --- Condition 4: JAL/JALR hazard in EX (PC+4 computed in WB, not EX) ---
        jal_jalr_hazard_ex
        // JAL/JALR in EX stage, ID reads destination - must wait for WB

        || // OR

        // --- Condition 5: JAL/JALR hazard in MEM (still not in WB) ---
        jal_jalr_hazard_mem
        // JAL/JALR in MEM stage, ID reads destination - must wait for WB

        || // OR

        // --- Condition 6: JAL/JALR hazard in WB (pipeline register delay) ---
        jal_jalr_hazard_wb
        // JAL/JALR in WB stage, but mem2wb output not yet stable for forwarding
  ) {
    // Stall action: Insert bubble and freeze pipeline
    //
    // Hazards requiring id_flush (insert bubble in id2ex):
    // 1. Load-use hazard: Load in EX, dependent instruction in ID
    // 2. JAL/JALR hazard: JAL/JALR in EX or MEM, ID reads destination register
    //
    // For JAL/JALR hazards: At the moment of detection, the correct PC+4 value
    // isn't available yet for forwarding (JAL hasn't reached MEM/WB). Without
    // id_flush, id2ex captures the STALE register file value instead of waiting
    // for the correct forwarded value. This was the root cause of the sw ra bug
    // where the wrong return address (from a previous call) was saved.
    //
    // Branch hazards do NOT need id_flush - the branch instruction proceeds and
    // re-evaluates with correct forwarded values once EX moves to MEM.
    val is_load_use_hazard = io.memory_read_enable_ex &&
      io.rd_ex =/= 0.U &&
      (io.rd_ex === io.rs1_id || io.rd_ex === io.rs2_id)
    // JAL/JALR hazard: must flush to prevent capturing stale register file value
    // Includes WB stage hazard due to pipeline register delay
    val is_jal_jalr_hazard = jal_jalr_hazard_ex || jal_jalr_hazard_mem || jal_jalr_hazard_wb
    io.id_flush := is_load_use_hazard || is_jal_jalr_hazard
    io.pc_stall := true.B // Freeze PC (don't fetch next instruction)
    io.if_stall := true.B // Freeze IF/ID (hold fetched instruction)
    // Suppress branch decision when there's EX or MEM hazard for branch
    // This prevents using wrong forwarded value from MEM for branch comparison
    // Also suppress when MEM-stage load hasn't completed (Condition 2 dependency)
    val mem_hazard_for_branch = io.jump_instruction_id &&
      io.memory_read_enable_mem &&
      io.rd_mem =/= 0.U &&
      (io.rd_mem === io.rs1_id || io.rd_mem === io.rs2_id)
    io.branch_hazard := ex_hazard_for_branch || mem_hazard_for_branch
    // Export JAL/JALR hazard for PipelinedCPU to bypass mem_stall suppression
    io.jal_jalr_hazard := is_jal_jalr_hazard

  }.elsewhen(io.jump_flag) {
    // ============ Control Hazard (Branch Taken) ============
    // Branch resolved in ID stage - only 1 cycle penalty
    // Only flush IF stage (not ID) since branch resolved early
    io.if_flush := true.B // Flush IF/ID: discard wrong-path fetch
    // Note: No ID flush needed - branch already resolved in ID!
    // This is the key optimization: 1-cycle branch penalty vs 2-cycle
  }
}
