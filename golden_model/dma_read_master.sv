// Module: DMA Read Master (AXI4 Full)
// Description: Fetches descriptors and reads data from source memory.

module dma_read_master (
    input  logic        aclk,
    input  logic        aresetn,

    // Control Interface
    input  logic        start,
    input  logic [63:0] head_ptr,
    output logic        done,
    output logic        error,

    // AXI4 Read Master Interface
    output logic [63:0] araddr,
    output logic [7:0]  arlen,
    output logic [2:0]  arsize,
    output logic [1:0]  arburst,
    output logic        arvalid,
    input  logic        arready,

    input  logic [63:0] rdata, // Assuming 64-bit bus
    input  logic        rlast,
    input  logic        rvalid,
    output logic        rready
);

    // State Machine Definitions
    typedef enum logic [2:0] {
        IDLE,
        FETCH_DESC,       // Read descriptor from memory
        DECODE_DESC,      // Extract Src/Dst/Len
        CALC_ALIGN,       // Handle unaligned addresses
        READ_DATA,        // Main data burst
        WAIT_COMPLETE     // Wait for write channel
    } state_t;

    state_t current_state, next_state;

    // Internal Registers for Descriptor
    logic [63:0] desc_src_addr;
    logic [63:0] desc_dst_addr;
    logic [31:0] desc_len;
    logic [63:0] desc_next_ptr;

    // FSM Sequential Logic
    always_ff @(posedge aclk or negedge aresetn) begin
        if (!aresetn)
            current_state <= IDLE;
        else
            current_state <= next_state;
    end

    // FSM Combinational Logic
    always_comb begin
        next_state = current_state;
        case (current_state)
            IDLE: begin
                if (start) next_state = FETCH_DESC;
            end

            FETCH_DESC: begin
                // Logic to issue AXI Read for Descriptor
                // TODO: Implement AR handshake
                if (rvalid && rlast) next_state = DECODE_DESC;
            end

            DECODE_DESC: begin
                if (desc_len == 0) next_state = IDLE; // End of chain
                else next_state = CALC_ALIGN;
            end

            CALC_ALIGN: begin
                // Check 4KB boundary crossing
                next_state = READ_DATA;
            end

            READ_DATA: begin
                // Loop until all bytes transferred
                if (rlast) next_state = WAIT_COMPLETE;
            end

            WAIT_COMPLETE: begin
                next_state = IDLE; // Simplified for skeleton
            end
        endcase
    end

    // Logic Stubs
    assign arburst = 2'b01; // INCR
    assign arsize  = 3'b011; // 8 bytes (64-bit)

endmodule