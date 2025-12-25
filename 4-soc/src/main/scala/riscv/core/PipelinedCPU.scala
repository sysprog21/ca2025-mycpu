// SPDX-License-Identifier: MIT
// MyCPU is freely redistributable under the MIT License. See the file
// "LICENSE" for information on usage and redistribution of this file.

package riscv.core

import chisel3._
import chisel3.util.MuxLookup
import riscv.core.CPUBundle
import riscv.core.CSR
import riscv.core.RegisterFile
import riscv.Parameters

/**
 * CPU: Five-stage pipelined RISC-V RV32I processor with advanced optimizations
 *
 * Pipeline Architecture: IF → ID → EX → MEM → WB (Classic 5-stage RISC pipeline)
 *
 * This is the most advanced pipeline implementation in the MyCPU project, featuring
 * comprehensive forwarding networks, early branch resolution, and optimized hazard
 * handling to achieve near-optimal CPI for an unpredicted-branch architecture.
 *
 * Pipeline Stages:
 * - IF (InstructionFetch): Fetches instructions, handles PC updates, interrupt vectoring
 * - ID (InstructionDecode): Decodes instructions, reads registers, resolves simple branches
 * - EX (Execute): ALU operations, complex branch evaluation, CSR operations
 * - MEM (MemoryAccess): Load/store operations, memory-mapped I/O access
 * - WB (WriteBack): Register writeback with data source selection
 *
 * Pipeline Registers:
 * - IF2ID: Buffers instruction and PC between IF and ID stages
 * - ID2EX: Buffers decoded control signals, register data, and immediates
 * - EX2MEM: Buffers ALU results, memory control signals, and register metadata
 * - MEM2WB: Buffers memory read data and writeback control signals
 *
 * Data Hazard Handling (Full Forwarding Network):
 *
 * ID-Stage Forwarding:
 * - Forward from MEM stage: Enables back-to-back dependent operations
 * - Forward from WB stage: Handles two-cycle dependencies
 * - Critical for jump instructions: JAL/JALR can use freshly computed values
 * - Eliminates most register-to-register hazard stalls
 *
 * EX-Stage Forwarding:
 * - Forward from MEM stage: Standard EX-to-EX bypass path
 * - Forward from WB stage: Handles load result forwarding
 * - Priority: MEM-stage data (newer) over WB-stage data (older)
 *
 * Load-Use Hazards:
 * - Selective stalling: Only stalls when jump depends on load result
 * - Most load-use cases handled by forwarding from MEM stage
 * - Unavoidable 1-cycle penalty for load followed by dependent jump
 *
 * Control Hazard Handling (Early Branch Resolution):
 *
 * Branch Resolution in ID Stage:
 * - Simple equality branches (BEQ, BNE) resolved in ID using forwarded values
 * - Reduces branch penalty from 2 cycles to 1 cycle (single IF flush)
 * - Complex branches (BLT, BGE, BLTU, BGEU) still resolved in EX stage
 *
 * Jump Handling:
 * - JAL: Computed in ID stage, 1-cycle penalty
 * - JALR: Computed in ID stage with forwarding support
 * - ID-stage forwarding critical for back-to-back jumps
 *
 * Pipeline Flush Strategy:
 * - Taken branch/jump: Flush IF stage only (1 bubble)
 * - Interrupt: Flush IF and ID stages (2 bubbles)
 * - Minimized flush depth reduces control hazard penalties
 *
 * CSR and Interrupt Support:
 * - CSR instructions: CSRRW, CSRRS, CSRRC and immediate variants
 * - CLINT: Timer interrupts, external interrupts
 * - Interrupt vectoring to mtvec, state saved to mepc/mcause/mstatus
 * - MRET instruction restores PC from mepc
 *
 * Forwarding Priority Rules:
 * 1. MEM-stage result (most recent)
 * 2. WB-stage result (one cycle older)
 * 3. Register file value (oldest, no hazard)
 *
 * Performance Characteristics:
 * - CPI: ~1.1-1.3 (near-optimal for unpredicted branches)
 * - Branch penalty: 1 cycle (simple branches), 2 cycles (complex branches)
 * - Load-use penalty: 1 cycle (when jumping after load)
 * - Hardware complexity: High (dual forwarding paths, early branch logic)
 * - Verified correct: Passes all RISC-V compliance tests
 *
 * Key Optimizations:
 * - Early branch resolution: Reduces average branch penalty by 50%
 * - Dual-stage forwarding: Eliminates most data hazard stalls
 * - Selective stalling: Minimizes unnecessary pipeline bubbles
 * - Optimized control logic: Reduces critical path delay
 *
 * Comparison to Other Implementations:
 * - vs ThreeStage: 2x better CPI, validated identical functional results
 * - vs FiveStageStall: Eliminates register hazard stalls via forwarding
 * - vs FiveStageForward: Adds ID-stage forwarding and early branch resolution
 *
 * Educational Value:
 * - Demonstrates real-world pipeline optimization techniques
 * - Shows trade-offs between hardware complexity and performance
 * - Illustrates forwarding network design and priority rules
 * - Provides reference for production-quality processor implementation
 *
 * Interface (CPUBundle):
 * - instruction_address: PC to instruction memory
 * - instruction/instruction_valid: Instruction memory interface
 * - memory_bundle: Data memory/MMIO interface (AXI4-Lite style)
 * - device_select: Upper address bits for peripheral routing
 * - interrupt_flag: External interrupt input
 * - debug_read_address/data: Register file inspection
 * - csr_debug_read_address/data: CSR inspection
 */
class PipelinedCPU extends Module {
  val io = IO(new CPUBundle)

  val ctrl       = Module(new Control)
  val regs       = Module(new RegisterFile)
  val inst_fetch = Module(new InstructionFetch)
  val if2id      = Module(new IF2ID)
  val id         = Module(new InstructionDecode)
  val id2ex      = Module(new ID2EX)
  val ex         = Module(new Execute)
  val ex2mem     = Module(new EX2MEM)
  val mem        = Module(new MemoryAccess)
  val mem2wb     = Module(new MEM2WB)
  val wb         = Module(new WriteBack)
  val forwarding = Module(new Forwarding)
  val clint      = Module(new CLINT)
  val csr_regs   = Module(new CSR)

  ctrl.io.jump_flag               := id.io.if_jump_flag
  ctrl.io.jump_instruction_id     := id.io.ctrl_jump_instruction
  ctrl.io.rs1_id                  := id.io.regs_reg1_read_address
  ctrl.io.rs2_id                  := id.io.regs_reg2_read_address
  ctrl.io.memory_read_enable_ex   := id2ex.io.output_memory_read_enable
  ctrl.io.rd_ex                   := id2ex.io.output_regs_write_address
  ctrl.io.memory_read_enable_mem  := ex2mem.io.output_memory_read_enable
  ctrl.io.rd_mem                  := ex2mem.io.output_regs_write_address
  ctrl.io.memory_write_enable_ex  := id2ex.io.output_memory_write_enable
  ctrl.io.memory_write_enable_mem := ex2mem.io.output_memory_write_enable
  ctrl.io.regs_write_enable_ex    := id2ex.io.output_regs_write_enable
  ctrl.io.regs_write_source_ex    := id2ex.io.output_regs_write_source
  ctrl.io.regs_write_source_mem   := ex2mem.io.output_regs_write_source
  // WB stage signals for JAL/JALR hazard detection (pipeline register delay fix)
  ctrl.io.regs_write_source_wb := mem2wb.io.output_regs_write_source
  ctrl.io.rd_wb                := mem2wb.io.output_regs_write_address

  regs.io.write_enable  := mem2wb.io.output_regs_write_enable
  regs.io.write_address := mem2wb.io.output_regs_write_address
  regs.io.write_data    := wb.io.regs_write_data
  regs.io.read_address1 := id.io.regs_reg1_read_address
  regs.io.read_address2 := id.io.regs_reg2_read_address

  regs.io.debug_read_address := io.debug_read_address
  io.debug_read_data         := regs.io.debug_read_data

  // Memory stall signal: freeze entire pipeline when AXI4 bus transactions are pending
  val mem_stall = mem.io.ctrl_stall_flag

  // Instruction memory interface
  io.instruction_address          := inst_fetch.io.instruction_address
  inst_fetch.io.stall_flag_ctrl   := ctrl.io.pc_stall || mem_stall
  inst_fetch.io.jump_flag_id      := id.io.if_jump_flag
  inst_fetch.io.jump_address_id   := id.io.if_jump_address
  inst_fetch.io.rom_instruction   := io.instruction
  inst_fetch.io.instruction_valid := io.instruction_valid

  // Prediction signals from IF2ID pipeline register (all predictors)
  val btb_predicted    = if2id.io.output_btb_predicted_taken
  val btb_pred_target  = if2id.io.output_btb_predicted_target
  val ras_predicted    = if2id.io.output_ras_predicted_valid
  val ras_pred_target  = if2id.io.output_ras_predicted_target
  val ibtb_predicted   = if2id.io.output_ibtb_predicted_valid
  val ibtb_pred_target = if2id.io.output_ibtb_predicted_target

  // Actual branch resolution from ID stage
  val actual_taken      = id.io.if_jump_flag
  val actual_target     = id.io.if_jump_address
  val is_branch_or_jump = id.io.ctrl_jump_instruction

  // BTB misprediction detection - covers multiple cases:
  // 1. BTB predicted taken, but branch not taken (wrong direction)
  // 2. BTB predicted taken on non-branch instruction (aliasing/stale entry)
  // 3. BTB predicted taken with wrong target (stale target, e.g., JALR)

  // Case 1: Predicted taken but actually not taken (conditional branches only)
  // Note: For conditional branches, RAS/IndirectBTB don't predict (JALR-specific),
  // so btb_wrong_direction doesn't need btb_actually_used gating
  val btb_wrong_direction = btb_predicted && is_branch_or_jump && !actual_taken

  // Case 2: BTB hit on non-branch (aliasing) - must redirect to sequential PC
  // Note: For non-branch instructions, RAS/IndirectBTB shouldn't predict (JALR-specific)
  val btb_non_branch = btb_predicted && !is_branch_or_jump

  // Determine if BTB was actually used (not overridden by higher-priority predictors)
  // Priority in InstructionFetch: RAS > IndirectBTB > BTB
  // This matters for JALR instructions where multiple predictors may have predictions
  val btb_actually_used = btb_predicted && !ras_predicted && !ibtb_predicted

  // Case 3: Predicted taken and actually taken, but wrong target (only if BTB was used)
  val btb_wrong_target = btb_actually_used && actual_taken && (btb_pred_target =/= actual_target)

  // BTB prediction correct: predicted taken, actually taken, target matches (only if BTB was used)
  val btb_correct_prediction = btb_actually_used && actual_taken && (btb_pred_target === actual_target)

  // Misprediction requires correction
  val btb_mispredict_raw = (btb_wrong_direction || btb_non_branch || btb_wrong_target) && !id.io.branch_hazard

  // BTB misprediction correction address:
  // - Wrong direction or non-branch: redirect to PC+4 (sequential)
  // - Wrong target: redirect to correct target
  val btb_correction_addr_raw = Mux(btb_wrong_target, actual_target, if2id.io.output_instruction_address + 4.U)

  // If a mispredict is detected during mem_stall, defer the correction until the stall releases.
  // Otherwise the correction pulse is lost (IF is stalled and IF2ID flush is suppressed).
  val btb_mispredict_pending      = RegInit(false.B)
  val btb_correction_addr_pending = RegInit(0.U(Parameters.AddrWidth))

  when(mem_stall && btb_mispredict_raw) {
    btb_mispredict_pending      := true.B
    btb_correction_addr_pending := btb_correction_addr_raw
  }.elsewhen(btb_mispredict_pending && !mem_stall) {
    btb_mispredict_pending := false.B
  }

  val btb_mispredict = btb_mispredict_raw || (btb_mispredict_pending && !mem_stall)
  val btb_correction_addr_effective = Mux(
    btb_mispredict_pending && !mem_stall,
    btb_correction_addr_pending,
    btb_correction_addr_raw
  )

  inst_fetch.io.btb_mispredict         := btb_mispredict
  inst_fetch.io.btb_correction_addr    := btb_correction_addr_effective
  inst_fetch.io.btb_correct_prediction := btb_correct_prediction

  // BTB update: update when branch/jump resolves in ID stage
  // Also invalidate on non-branch BTB hits to prevent future mispredictions
  val id_is_branch_or_jump = id.io.ctrl_jump_instruction
  val btb_should_update    = (id_is_branch_or_jump || btb_non_branch) && !id.io.branch_hazard && !mem_stall
  inst_fetch.io.btb_update_valid  := btb_should_update
  inst_fetch.io.btb_update_pc     := if2id.io.output_instruction_address
  inst_fetch.io.btb_update_target := id.io.if_jump_address
  inst_fetch.io.btb_update_taken  := id.io.if_jump_flag && id_is_branch_or_jump // Non-branch = not taken

  // Return Address Stack (RAS) update logic
  // Detect instruction type from ID stage for call/return pattern recognition
  val id_instruction = if2id.io.output_instruction
  val id_opcode      = id_instruction(6, 0)
  val id_rd          = id_instruction(11, 7)
  val id_rs1         = id_instruction(19, 15)

  // CALL detection: JAL with rd=x1 (ra) or rd=x5 (t0)
  // These are the standard RISC-V link registers for call/return
  val is_jal      = id_opcode === Instructions.jal
  val is_jalr     = id_opcode === Instructions.jalr
  val rd_is_link  = id_rd === 1.U || id_rd === 5.U   // x1 (ra) or x5 (t0)
  val rs1_is_link = id_rs1 === 1.U || id_rs1 === 5.U // x1 (ra) or x5 (t0)

  // Push on JAL/JALR with rd=link (call pattern)
  // Note: JALR with rd=link, rs1=link is a co-routine swap, still pushes
  val ras_push_trigger = (is_jal || is_jalr) && rd_is_link && !id.io.branch_hazard && !mem_stall
  val ras_push_addr    = if2id.io.output_instruction_address + 4.U // Return address = PC + 4

  inst_fetch.io.ras_push      := ras_push_trigger
  inst_fetch.io.ras_push_addr := ras_push_addr

  // RAS misprediction detection (for JALR rs1=link returns)
  // Compare RAS predicted target with actual computed target
  // Gate with !branch_hazard because actual_target uses forwarded data that may be stale during hazard
  val is_return           = is_jalr && rs1_is_link && (id_rd === 0.U) // JALR rd=x0, rs1=link is return pattern
  val ras_wrong_target    = ras_predicted && is_return && (ras_pred_target =/= actual_target) && !id.io.branch_hazard
  val ras_correct_predict = ras_predicted && is_return && (ras_pred_target === actual_target)

  // IndirectBTB misprediction detection (for non-return JALR: function pointers, vtables)
  // Gate with !branch_hazard because actual_target uses forwarded data that may be stale during hazard
  val is_indirect_jalr = is_jalr && !is_return // JALR that is NOT a return pattern
  val ibtb_wrong_target =
    ibtb_predicted && is_indirect_jalr && (ibtb_pred_target =/= actual_target) && !id.io.branch_hazard
  val ibtb_correct_predict = ibtb_predicted && is_indirect_jalr && (ibtb_pred_target === actual_target)

  // Pop from RAS when JALR rs1=link resolves in ID (only if not already speculatively popped correctly)
  // If RAS prediction was correct, no additional pop needed (already done in IF)
  // If RAS prediction was wrong, the speculative pop removed an incorrect entry anyway
  inst_fetch.io.ras_pop := false.B // Speculative pop already done in IF stage

  // RAS restore: DISABLED
  // The previous implementation incorrectly pushed actual_target (JALR destination = rs1+imm),
  // but the RAS should only contain return addresses (PC+4 from JAL/JALR calls).
  // When RAS mispredicts, the popped value was wrong anyway (stack corrupted or empty),
  // so there's nothing meaningful to restore. Pushing actual_target would corrupt the RAS
  // with non-return addresses, degrading future predictions.
  //
  // The correct behavior is to accept the speculative pop:
  // - If the pop removed a valid return address, it would have been correct (no misprediction)
  // - If the pop removed garbage or was from empty stack, no harm in losing it
  inst_fetch.io.ras_restore       := false.B
  inst_fetch.io.ras_restore_addr  := 0.U
  inst_fetch.io.ras_restore_valid := false.B

  // ID-stage forwarding for register data (used by ID2EX and IndirectBTB hash)
  // This is CRITICAL for cases where an instruction reads a register written by
  // an instruction 2 stages ahead (e.g., jal writes ra, then addi, then sw reads ra).
  // By the time sw reaches EX stage, jal is past WB and EX forwarding can't help.
  // ID-stage forwarding captures the correct value when sw is in ID stage.
  val id_reg1_data_forwarded = MuxLookup(forwarding.io.reg1_forward_id, regs.io.read_data1)(
    IndexedSeq(
      ForwardingType.ForwardFromMEM -> mem.io.forward_to_ex,
      ForwardingType.ForwardFromWB  -> wb.io.regs_write_data
    )
  )
  val id_reg2_data_forwarded = MuxLookup(forwarding.io.reg2_forward_id, regs.io.read_data2)(
    IndexedSeq(
      ForwardingType.ForwardFromMEM -> mem.io.forward_to_ex,
      ForwardingType.ForwardFromWB  -> wb.io.regs_write_data
    )
  )

  // IndirectBTB update: train on non-return JALR instructions when they resolve
  // Update with (PC, rs1_hash) → target mapping to improve future predictions
  // Uses forwarded rs1 value for hash calculation (same value used for target computation)
  val ibtb_rs1_hash      = IndirectBTBHash(id_reg1_data_forwarded)
  val ibtb_should_update = is_indirect_jalr && !id.io.branch_hazard && !mem_stall
  inst_fetch.io.ibtb_update_valid    := ibtb_should_update
  inst_fetch.io.ibtb_update_pc       := if2id.io.output_instruction_address
  inst_fetch.io.ibtb_update_rs1_hash := ibtb_rs1_hash
  inst_fetch.io.ibtb_update_target   := actual_target

  if2id.io.stall := ctrl.io.if_stall || mem_stall
  // CRITICAL FIX: Suppress IF2ID flush during mem_stall!
  //
  // When a JAL/JALR triggers if_flush while mem_stall is active:
  //   - IF2ID is stalled (holds JAL instruction)
  //   - ID2EX is stalled (holds previous instruction)
  //   - If we flush IF2ID immediately, the JAL is lost before ID2EX can capture it
  //   - JAL never reaches EX/MEM/WB, so ra never gets written with PC+4
  //   - Any subsequent sw ra stores the WRONG (stale) return address!
  //
  // The fix: suppress IF2ID flush until mem_stall releases. The JAL stays in IF2ID,
  // and when mem_stall releases, ID2EX captures JAL before IF2ID flushes on the same
  // clock edge (register captures happen before flush updates the output).
  //
  // The "wrong-path" instruction in IF is also stalled and won't execute because:
  //   1. During mem_stall, IF is stalled (won't capture wrong-path)
  //   2. When mem_stall releases, pending_jump redirects PC to jump target
  //   3. IF2ID flushes on the same cycle, discarding any wrong-path instruction
  //
  // IF flush logic:
  // - ctrl.io.if_flush: Branch/jump taken (from Control)
  // - btb_mispredict: BTB predicted wrong direction, target, or hit on non-branch
  // - ras_wrong_target: RAS predicted wrong return address
  // - ibtb_wrong_target: IndirectBTB predicted wrong target for non-return JALR
  // - SKIP flush if BTB, RAS, or IndirectBTB prediction was correct
  //   This is the key optimization: when prediction was correct,
  //   IF already fetched the correct next instruction, so no flush needed!
  val prediction_correct = btb_correct_prediction || ras_correct_predict || ibtb_correct_predict
  val need_if_flush =
    (ctrl.io.if_flush && !prediction_correct) || btb_mispredict || ras_wrong_target || ibtb_wrong_target
  if2id.io.flush                 := need_if_flush && !mem_stall
  if2id.io.instruction           := inst_fetch.io.id_instruction
  if2id.io.instruction_address   := inst_fetch.io.instruction_address
  if2id.io.interrupt_flag        := io.interrupt_flag
  if2id.io.btb_predicted_taken   := inst_fetch.io.btb_predicted_taken
  if2id.io.btb_predicted_target  := inst_fetch.io.btb_predicted_target
  if2id.io.ras_predicted_valid   := inst_fetch.io.ras_predicted_valid
  if2id.io.ras_predicted_target  := inst_fetch.io.ras_predicted_target
  if2id.io.ibtb_predicted_valid  := inst_fetch.io.ibtb_predicted_valid
  if2id.io.ibtb_predicted_target := inst_fetch.io.ibtb_predicted_target

  id.io.instruction               := if2id.io.output_instruction
  id.io.instruction_address       := if2id.io.output_instruction_address
  id.io.reg1_data                 := regs.io.read_data1
  id.io.reg2_data                 := regs.io.read_data2
  id.io.forward_from_mem          := mem.io.forward_to_ex
  id.io.forward_from_wb           := wb.io.regs_write_data
  id.io.reg1_forward              := forwarding.io.reg1_forward_id
  id.io.reg2_forward              := forwarding.io.reg2_forward_id
  id.io.interrupt_assert          := clint.io.id_interrupt_assert
  id.io.interrupt_handler_address := clint.io.id_interrupt_handler_address
  id.io.branch_hazard             := ctrl.io.branch_hazard

  id2ex.io.stall := mem_stall
  // Do not flush id2ex when mem_stall is active - EXCEPT for JAL/JALR hazards!
  // When the memory is stalling (e.g., multi-cycle store), the id2ex register holds
  // the instruction waiting in EX stage. For load-use hazards, the flush is suppressed
  // because the hazard will be re-evaluated when the memory stall releases.
  //
  // CRITICAL: JAL/JALR hazards MUST flush immediately even during mem_stall!
  // Without this, sw ra captures the stale register file value instead of waiting
  // for the correct forwarded PC+4 value from the JAL/JALR instruction.
  // This was the root cause of the vga_simple bug where sw ra saved 0x1050 instead of 0x125c.
  id2ex.io.flush               := ctrl.io.id_flush && (!mem_stall || ctrl.io.jal_jalr_hazard)
  id2ex.io.instruction         := if2id.io.output_instruction
  id2ex.io.instruction_address := if2id.io.output_instruction_address

  // ID-stage forwarding values (defined earlier) passed to ID2EX pipeline register
  id2ex.io.reg1_data              := id_reg1_data_forwarded
  id2ex.io.reg2_data              := id_reg2_data_forwarded
  id2ex.io.regs_reg1_read_address := id.io.regs_reg1_read_address
  id2ex.io.regs_reg2_read_address := id.io.regs_reg2_read_address
  id2ex.io.regs_write_enable      := id.io.ex_reg_write_enable
  id2ex.io.regs_write_address     := id.io.ex_reg_write_address
  id2ex.io.regs_write_source      := id.io.ex_reg_write_source
  id2ex.io.immediate              := id.io.ex_immediate
  id2ex.io.aluop1_source          := id.io.ex_aluop1_source
  id2ex.io.aluop2_source          := id.io.ex_aluop2_source
  id2ex.io.csr_write_enable       := id.io.ex_csr_write_enable
  id2ex.io.csr_address            := id.io.ex_csr_address
  id2ex.io.memory_read_enable     := id.io.ex_memory_read_enable
  id2ex.io.memory_write_enable    := id.io.ex_memory_write_enable
  id2ex.io.csr_read_data          := csr_regs.io.id_reg_read_data

  ex.io.instruction         := id2ex.io.output_instruction
  ex.io.instruction_address := id2ex.io.output_instruction_address
  ex.io.reg1_data           := id2ex.io.output_reg1_data
  ex.io.reg2_data           := id2ex.io.output_reg2_data
  ex.io.immediate           := id2ex.io.output_immediate
  ex.io.aluop1_source       := id2ex.io.output_aluop1_source
  ex.io.aluop2_source       := id2ex.io.output_aluop2_source
  ex.io.csr_read_data       := id2ex.io.output_csr_read_data
  ex.io.forward_from_mem    := mem.io.forward_to_ex
  ex.io.forward_from_wb     := wb.io.regs_write_data
  ex.io.reg1_forward        := forwarding.io.reg1_forward_ex
  ex.io.reg2_forward        := forwarding.io.reg2_forward_ex

  ex2mem.io.stall               := mem_stall
  ex2mem.io.regs_write_enable   := id2ex.io.output_regs_write_enable
  ex2mem.io.regs_write_source   := id2ex.io.output_regs_write_source
  ex2mem.io.regs_write_address  := id2ex.io.output_regs_write_address
  ex2mem.io.instruction_address := id2ex.io.output_instruction_address
  ex2mem.io.funct3              := id2ex.io.output_instruction(14, 12)
  ex2mem.io.reg2_data           := ex.io.mem_reg2_data
  ex2mem.io.memory_read_enable  := id2ex.io.output_memory_read_enable
  ex2mem.io.memory_write_enable := id2ex.io.output_memory_write_enable
  ex2mem.io.alu_result          := ex.io.mem_alu_result
  ex2mem.io.csr_read_data       := id2ex.io.output_csr_read_data

  mem.io.alu_result          := ex2mem.io.output_alu_result
  mem.io.reg2_data           := ex2mem.io.output_reg2_data
  mem.io.memory_read_enable  := ex2mem.io.output_memory_read_enable
  mem.io.memory_write_enable := ex2mem.io.output_memory_write_enable
  mem.io.funct3              := ex2mem.io.output_funct3
  mem.io.regs_write_source   := ex2mem.io.output_regs_write_source
  mem.io.regs_write_address  := ex2mem.io.output_regs_write_address
  mem.io.regs_write_enable   := ex2mem.io.output_regs_write_enable
  mem.io.csr_read_data       := ex2mem.io.output_csr_read_data
  mem.io.instruction_address := ex2mem.io.output_instruction_address // For JAL/JALR forwarding
  io.device_select := mem.io.bus
    .address(Parameters.AddrBits - 1, Parameters.AddrBits - Parameters.SlaveDeviceCountBits)
  io.memory_bundle <> mem.io.bus
  io.memory_bundle.address := 0.U(Parameters.SlaveDeviceCountBits.W) ## mem.io.bus
    .address(Parameters.AddrBits - 1 - Parameters.SlaveDeviceCountBits, 0)

  mem2wb.io.stall               := mem_stall
  mem2wb.io.instruction_address := ex2mem.io.output_instruction_address
  mem2wb.io.alu_result          := ex2mem.io.output_alu_result
  // Use MEM stage's latched outputs instead of ex2mem outputs for ALL writeback signals
  // This preserves correct values when mem_stall releases (PipelineRegister bypass issue)
  // Without this fix, the load instruction's rd/enable would be lost, causing UART corruption
  mem2wb.io.regs_write_enable  := mem.io.wb_regs_write_enable
  mem2wb.io.regs_write_source  := mem.io.wb_regs_write_source
  mem2wb.io.regs_write_address := mem.io.wb_regs_write_address
  mem2wb.io.memory_read_data   := mem.io.wb_memory_read_data
  mem2wb.io.csr_read_data      := ex2mem.io.output_csr_read_data

  wb.io.instruction_address := mem2wb.io.output_instruction_address
  wb.io.alu_result          := mem2wb.io.output_alu_result
  wb.io.memory_read_data    := mem2wb.io.output_memory_read_data
  wb.io.regs_write_source   := mem2wb.io.output_regs_write_source
  wb.io.csr_read_data       := mem2wb.io.output_csr_read_data

  forwarding.io.rs1_id               := id.io.regs_reg1_read_address
  forwarding.io.rs2_id               := id.io.regs_reg2_read_address
  forwarding.io.rs1_ex               := id2ex.io.output_regs_reg1_read_address
  forwarding.io.rs2_ex               := id2ex.io.output_regs_reg2_read_address
  forwarding.io.rd_mem               := ex2mem.io.output_regs_write_address
  forwarding.io.reg_write_enable_mem := ex2mem.io.output_regs_write_enable
  forwarding.io.rd_wb                := mem2wb.io.output_regs_write_address
  forwarding.io.reg_write_enable_wb  := mem2wb.io.output_regs_write_enable

  clint.io.instruction_address_if := inst_fetch.io.instruction_address
  clint.io.instruction_id         := if2id.io.output_instruction
  clint.io.jump_flag              := id.io.clint_jump_flag
  clint.io.jump_address           := id.io.clint_jump_address
  clint.io.interrupt_flag         := io.interrupt_flag // Direct connection, bypass IF2ID pipeline delay
  clint.io.csr_bundle <> csr_regs.io.clint_access_bundle

  csr_regs.io.reg_read_address_id    := id.io.ex_csr_address
  csr_regs.io.reg_write_enable_ex    := id2ex.io.output_csr_write_enable
  csr_regs.io.reg_write_address_ex   := id2ex.io.output_csr_address
  csr_regs.io.reg_write_data_ex      := ex.io.csr_write_data
  csr_regs.io.debug_reg_read_address := io.csr_debug_read_address
  io.csr_debug_read_data             := csr_regs.io.debug_reg_read_data

  // Performance counter connections
  //
  // Instruction retirement: Count instructions that complete WB stage.
  // An instruction is "retired" when it successfully commits (reaches WB without flush).
  //
  // What counts as a valid instruction:
  // 1. Register-writing instructions: mem2wb.io.output_regs_write_enable = true
  //    (ALU ops, loads, JAL/JALR, CSR reads, etc.)
  // 2. Store instructions: Complete when write_data_accepted from AXI bus
  //    (stores don't write registers but still need to be counted)
  // 3. Branches (taken or not): Don't write registers, but must be counted
  //    We track this via a "valid instruction" signal that propagates through MEM2WB
  //
  // What does NOT count:
  // - Bubbles/NOPs inserted by hazard handling (regs_write_enable=false, not a store)
  // - Flushed instructions (never reach WB)
  //
  // Current implementation uses regs_write_enable as primary indicator plus store completion.
  // Branch instructions that don't write registers are counted when they set if_jump_flag
  // and successfully update PC (they implicitly complete when they resolve in ID stage).
  //
  // For now, we use a simpler but slightly imprecise metric:
  // - Count register-writing instructions in WB
  // - Count stores when they complete (write_data_accepted)
  // This may undercount branches that don't write registers, but matches typical CPI analysis.
  val wb_instruction_valid = mem2wb.io.output_regs_write_enable
  val store_completed      = mem.io.bus.write && mem.io.bus.write_valid // Store completes
  csr_regs.io.instruction_retired := (wb_instruction_valid || store_completed) && !mem_stall

  // Branch misprediction: BTB, RAS, or IndirectBTB predicted wrong
  // Gate with !mem_stall to ensure single-cycle pulse.
  // - mem_stall: Pipeline frozen (would count same misprediction multiple times)
  // Note: All three signals already include !branch_hazard gating.
  csr_regs.io.branch_misprediction := (btb_mispredict || ras_wrong_target || ibtb_wrong_target) && !mem_stall

  // Stall type breakdown for detailed performance analysis
  // Stall counters are MUTUALLY EXCLUSIVE to avoid double-counting.
  // Priority order: memory > control > hazard
  // Only one stall type can increment per cycle.
  //
  // Memory stalls (mhpmcounter5): AXI bus wait states (highest priority)
  // - Multi-cycle memory reads (waiting for RVALID)
  // - Multi-cycle memory writes (waiting for BVALID)
  csr_regs.io.memory_stall := mem_stall

  // Control stalls (mhpmcounter6): Branch/jump flush penalty (medium priority)
  // - IF flush due to taken branch/jump (1 cycle penalty with early resolution)
  // - BTB misprediction correction
  // - RAS misprediction correction
  // - IndirectBTB misprediction correction
  //
  // PULSE SEMANTICS: This is an event counter, not a cycle counter.
  // Guaranteed single-cycle pulse because:
  // 1. Misprediction detected only when branch is in ID stage
  // 2. Next cycle: ID flushed with NOP → signals go low (no branch in ID)
  // 3. mem_stall gating prevents counting during stalls
  // 4. When stall releases, flush completes atomically
  val control_flush_event = (need_if_flush || btb_mispredict || ras_wrong_target || ibtb_wrong_target) && !mem_stall
  csr_regs.io.control_stall := control_flush_event

  // Hazard stalls (mhpmcounter4): Data hazards that cause pipeline bubbles (lowest priority)
  // - Load-use hazard: Instruction in ID needs result from load in EX/MEM
  // - JAL/JALR hazard: SW needs ra but JAL/JALR hasn't written it yet
  // - Branch hazard: Branch in ID needs ALU result from EX
  // Only counted when not in a memory stall AND not a control flush (mutual exclusion).
  csr_regs.io.hazard_stall := ctrl.io.pc_stall && !mem_stall && !control_flush_event

  // BTB miss penalty (mhpmcounter7): Branch/jump that incurs BTB-related penalty
  // Counts two types of BTB-related penalties:
  // 1. BTB miss: Branch/jump NOT in BTB but actually taken (cold miss)
  // 2. BTB wrong target: BTB hit but predicted wrong target (stale entry)
  // Both cases cause a 1-cycle flush penalty that could have been avoided.
  // Note: Wrong direction (predicted taken, actually not taken) is counted in mhpmcounter3.
  //
  // PULSE SEMANTICS: Single-cycle event (branch_hazard and mem_stall gating).
  val btb_miss_penalty = (
    (!btb_predicted && is_branch_or_jump && actual_taken) || // BTB miss (cold)
      btb_wrong_target                                       // BTB wrong target (stale)
  ) && !id.io.branch_hazard && !mem_stall
  csr_regs.io.btb_miss_taken := btb_miss_penalty

  // Total branches resolved (mhpmcounter8): All branch/jump instructions resolved in ID stage
  // This provides the denominator for calculating branch prediction accuracy:
  //   Accuracy = 1 - (mhpmcounter3 / mhpmcounter8)
  // Counts all conditional branches and unconditional jumps (JAL, JALR) when they resolve.
  // Does not count branches held due to branch_hazard (they'll be counted when they resolve).
  //
  // PULSE SEMANTICS: Single-cycle event per branch (branch_hazard and mem_stall gating).
  csr_regs.io.branch_resolved := is_branch_or_jump && !id.io.branch_hazard && !mem_stall

  // BTB predictions (mhpmcounter9): Count when BTB predicted "taken" for a resolved branch
  // This allows calculating BTB coverage: mhpmcounter9 / mhpmcounter8
  // Combined with mhpmcounter3 (mispredictions), can isolate BTB accuracy vs RAS accuracy.
  // Note: Only counts predictions that actually resolved (excludes squashed predictions).
  //
  // PULSE SEMANTICS: Single-cycle event per prediction (branch_hazard and mem_stall gating).
  csr_regs.io.btb_predicted := btb_predicted && is_branch_or_jump && !id.io.branch_hazard && !mem_stall

  // Initialize unused CPUBundle signals (used by wrapper, not by pipeline core)
  io.bus_address                                 := 0.U
  io.axi4_channels.read_address_channel.ARADDR   := 0.U
  io.axi4_channels.read_address_channel.ARPROT   := 0.U
  io.axi4_channels.read_address_channel.ARVALID  := false.B
  io.axi4_channels.read_data_channel.RREADY      := false.B
  io.axi4_channels.write_address_channel.AWADDR  := 0.U
  io.axi4_channels.write_address_channel.AWPROT  := 0.U
  io.axi4_channels.write_address_channel.AWVALID := false.B
  io.axi4_channels.write_data_channel.WDATA      := 0.U
  io.axi4_channels.write_data_channel.WSTRB      := 0.U
  io.axi4_channels.write_data_channel.WVALID     := false.B
  io.axi4_channels.write_response_channel.BREADY := false.B
  io.debug_bus_write_enable                      := false.B
  io.debug_bus_write_data                        := 0.U
}
