package peripheral

import chisel3._
import chisel3.util._
import bus._
import riscv.Parameters

class DMADescriptor extends Bundle {
  val nextPtr = UInt(32.W)
  val srcAddr = UInt(32.W)
  val dstAddr = UInt(32.W)
  val length  = UInt(32.W)
}

object DMARegisters {
  val CONTROL  = 0x00.U
  val STATUS   = 0x04.U
  val HEAD_PTR = 0x08.U
  val TAIL_PTR = 0x0C.U 
}

case class DMAParameters(
  fifoDepth: Int = 16,
  maxBurstLen: Int = 8,
  axiConfig: AXI4Config = AXI4Config()
)

class DMA(param: DMAParameters) extends Module {
  val io = IO(new Bundle {
    val slave     = Flipped(new AXI4LiteChannels(Parameters.AddrBits, Parameters.DataBits))
    val master    = new AXI4Bundle(param.axiConfig)
    val interrupt = Output(Bool())
  })

  // Registers
  val ctrlReg   = RegInit(0.U(32.W))
  val statusReg = Reg(UInt(32.W))
  val headPtr   = RegInit(0.U(32.W))
  
  val ctrlStart = ctrlReg(0)
  val ctrlIrqEn = ctrlReg(2)
  val statusBusy = RegInit(false.B)
  val statusDone = RegInit(false.B)

  // Slave Logic
  val slaveModule = Module(new AXI4LiteSlave(Parameters.AddrBits, Parameters.DataBits))
  slaveModule.io.channels <> io.slave
  val mmioOffset = slaveModule.io.bundle.address(11, 0)
  
  // Default outputs
  slaveModule.io.bundle.read_data := 0.U
  slaveModule.io.bundle.read_valid := false.B
  
  // Read Logic
  when(slaveModule.io.bundle.read) {
    slaveModule.io.bundle.read_valid := true.B
    val rData = WireDefault(0.U(32.W))
    switch(mmioOffset) {
      is(DMARegisters.CONTROL)  { rData := ctrlReg }
      is(DMARegisters.STATUS)   { rData := statusReg }
      is(DMARegisters.HEAD_PTR) { rData := headPtr }
    }
    slaveModule.io.bundle.read_data := rData
  }

  // Write Logic
  when(slaveModule.io.bundle.write) {
    switch(mmioOffset) {
      is(DMARegisters.CONTROL) { ctrlReg := slaveModule.io.bundle.write_data }
      is(DMARegisters.STATUS) {
        when(slaveModule.io.bundle.write_data(1)) { statusDone := false.B }
      }
      is(DMARegisters.HEAD_PTR) { headPtr := slaveModule.io.bundle.write_data }
    }
  }

  // Core FSM
  val currentDescAddr = RegInit(0.U(32.W))
  val descNext        = RegInit(0.U(32.W))
  val descSrc         = RegInit(0.U(32.W))
  val descDst         = RegInit(0.U(32.W))
  val descLen         = RegInit(0.U(32.W))

  val sIdle :: sFetchDesc :: sReadDescWait :: sStartTransfer :: sWaitTransfer :: sUpdatePtr :: Nil = Enum(6)
  val state = RegInit(sIdle)

  val transferStart = WireDefault(false.B)
  val transferBusy  = Wire(Bool())
  val transferDone  = Wire(Bool())

  io.interrupt := statusDone && ctrlIrqEn

  switch(state) {
    is(sIdle) {
      statusBusy := false.B
      when(ctrlStart) {
        statusBusy := true.B
        statusDone := false.B 
        currentDescAddr := headPtr
        state := sFetchDesc
      }
    }
    is(sFetchDesc) {
      state := sReadDescWait
    }
    is(sReadDescWait) { /* Sync */ }
    is(sStartTransfer) {
      transferStart := true.B
      state := sWaitTransfer
    }
    is(sWaitTransfer) {
      when(transferDone) { state := sUpdatePtr }
    }
    is(sUpdatePtr) {
      when(descNext === 0.U) {
        statusBusy := false.B
        statusDone := true.B
        ctrlReg := ctrlReg & ~1.U(32.W)
        state := sIdle
      }.otherwise {
        currentDescAddr := descNext
        state := sFetchDesc
      }
    }
  }

  // Data Mover
  val buffer = Module(new Queue(UInt(param.axiConfig.dataWidth.W), param.fifoDepth))
  buffer.io.enq.valid := false.B
  buffer.io.enq.bits  := 0.U

  val bytesLeft   = RegInit(0.U(32.W))
  val readOffset  = RegInit(0.U(32.W))
  val writeOffset = RegInit(0.U(32.W))

  // Read Engine
  val rStateIdle :: rStateAddr :: rStateData :: Nil = Enum(3)
  val rState = RegInit(rStateIdle)
  val rLen   = RegInit(0.U(8.W)) 
  val descFetchCnt = RegInit(0.U(3.W)) 

  io.master.ar.valid := false.B
  io.master.ar.addr  := 0.U
  io.master.ar.len   := 0.U
  io.master.ar.size  := 2.U 
  io.master.ar.burst := AXI4Parameters.BURST_INCR
  io.master.ar.id    := 0.U
  io.master.ar.lock  := false.B
  io.master.ar.cache := 0.U
  io.master.ar.prot  := 0.U
  io.master.ar.qos   := 0.U
  io.master.r.ready  := false.B

  switch(rState) {
    is(rStateIdle) {
      when(state === sFetchDesc) {
        descFetchCnt := 0.U
      }
      .elsewhen(state === sReadDescWait && descFetchCnt < 4.U) {
        rState := rStateAddr
        rLen := 0.U 
      }
      .elsewhen(state === sWaitTransfer && bytesLeft > 0.U && readOffset < descLen) {
        val remBytes = descLen - readOffset
        val maxBytes = (param.maxBurstLen.U * 4.U)
        val transferBytes = Mux(remBytes > maxBytes, maxBytes, remBytes)
        rState := rStateAddr
        rLen := (transferBytes >> 2) - 1.U
      }
    }
    is(rStateAddr) {
      io.master.ar.valid := true.B
      io.master.ar.len   := rLen
      when(state === sReadDescWait) {
         io.master.ar.addr := currentDescAddr + (descFetchCnt << 2)
      }.otherwise {
         io.master.ar.addr := descSrc + readOffset
      }
      when(io.master.ar.ready) { rState := rStateData }
    }
    is(rStateData) {
      when(state === sReadDescWait) {
        io.master.r.ready := true.B
        when(io.master.r.valid) {
          switch(descFetchCnt) {
            is(0.U) { descNext := io.master.r.data }
            is(1.U) { descSrc  := io.master.r.data }
            is(2.U) { descDst  := io.master.r.data }
            is(3.U) { descLen  := io.master.r.data }
          }
          descFetchCnt := descFetchCnt + 1.U
          rState := rStateIdle
        }
      }
      .otherwise {
        io.master.r.ready := buffer.io.enq.ready
        buffer.io.enq.valid := io.master.r.valid
        buffer.io.enq.bits := io.master.r.data
        when(io.master.r.valid && buffer.io.enq.ready) {
          readOffset := readOffset + 4.U
          when(io.master.r.last) { rState := rStateIdle }
        }
      }
    }
  }

  when(state === sReadDescWait && rState === rStateIdle && descFetchCnt === 4.U) {
    state := sStartTransfer
  }

  // Write Engine
  val wStateIdle :: wStateAddr :: wStateData :: wStateResp :: Nil = Enum(4)
  val wState = RegInit(wStateIdle)
  val wLen   = RegInit(0.U(8.W))

  io.master.aw.valid := false.B
  io.master.aw.addr  := 0.U
  io.master.aw.len   := 0.U
  io.master.aw.size  := 2.U
  io.master.aw.burst := AXI4Parameters.BURST_INCR
  io.master.aw.id    := 0.U
  io.master.aw.lock  := false.B
  io.master.aw.cache := 0.U
  io.master.aw.prot  := 0.U
  io.master.aw.qos   := 0.U
  
  io.master.w.valid  := false.B
  io.master.w.data   := 0.U
  io.master.w.strb   := "b1111".U 
  io.master.w.last   := false.B
  io.master.b.ready  := false.B

  when(transferStart) {
    readOffset  := 0.U
    writeOffset := 0.U
    bytesLeft   := descLen
  }

  buffer.io.deq.ready := false.B
  val wBurstCnt = RegInit(0.U(8.W))

  switch(wState) {
    is(wStateIdle) {
       when(state === sWaitTransfer && buffer.io.deq.valid && writeOffset < descLen) {
          val remBytes = descLen - writeOffset
          val maxBytes = (param.maxBurstLen.U * 4.U)
          val transferBytes = Mux(remBytes > maxBytes, maxBytes, remBytes)
          wLen := (transferBytes >> 2) - 1.U
          wState := wStateAddr
       }
    }
    is(wStateAddr) {
      io.master.aw.valid := true.B
      io.master.aw.addr  := descDst + writeOffset
      io.master.aw.len   := wLen
      when(io.master.aw.ready) {
        wState := wStateData
        wBurstCnt := 0.U
      }
    }
    is(wStateData) {
      buffer.io.deq.ready := io.master.w.ready
      io.master.w.valid   := buffer.io.deq.valid
      io.master.w.data    := buffer.io.deq.bits
      val isLast = (wBurstCnt === wLen)
      io.master.w.last := isLast
      when(io.master.w.ready && buffer.io.deq.valid) {
        writeOffset := writeOffset + 4.U
        wBurstCnt := wBurstCnt + 1.U
        when(isLast) { wState := wStateResp }
      }
    }
    is(wStateResp) {
      io.master.b.ready := true.B
      when(io.master.b.valid) { wState := wStateIdle }
    }
  }

  transferDone := (state === sWaitTransfer) && (writeOffset === descLen) && (wState === wStateIdle) && (rState === rStateIdle)
  transferBusy := (state =/= sIdle)

  statusReg := Cat(
    0.U(16.W),
    state.asUInt,
    rState.asUInt,
    0.U(6.W),
    statusDone,
    statusBusy
  )
}