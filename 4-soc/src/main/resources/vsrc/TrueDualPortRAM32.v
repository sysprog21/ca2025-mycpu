// SPDX-License-Identifier: MIT
// True dual-port, dual-clock RAM behavioral model
// Port A: Write port (CPU clock domain)
// Port B: Read port (Pixel clock domain)

module TrueDualPortRAM32 #(
    parameter DEPTH = 6144,       // Number of 32-bit words
    parameter ADDR_WIDTH = 13     // Address width in bits
) (
    // Port A: Write port (CPU clock domain)
    input wire clka,
    input wire wea,
    input wire [ADDR_WIDTH-1:0] addra,
    input wire [31:0] dina,

    // Port B: Read port (Pixel clock domain)
    input wire clkb,
    input wire [ADDR_WIDTH-1:0] addrb,
    output reg [31:0] doutb
);

    // RAM storage
    reg [31:0] mem [0:DEPTH-1];

    // Port A: Write port
    always @(posedge clka) begin
        if (wea) begin
            mem[addra] <= dina;
        end
    end

    // Port B: Read port
    always @(posedge clkb) begin
        doutb <= mem[addrb];
    end

    // Initialize memory to zero
    integer i;
    initial begin
        for (i = 0; i < DEPTH; i = i + 1) begin
            mem[i] = 32'h0;
        end
    end

endmodule
