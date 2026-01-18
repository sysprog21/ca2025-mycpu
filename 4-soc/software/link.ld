OUTPUT_ARCH( "riscv" )
ENTRY( _start )

SECTIONS
{
  /* * Set the text segment start address to 0x1000.
   * This matches 'EntryAddress = 0x1000' in Parameters.scala.
   */
  . = 0x00001000;

  .text : {
    *(.text.init)
    *(.text)
  }

  .data : {
    *(.data)
  }

  .bss : {
    *(.bss)
  }

  /* * Define the Stack Pointer Top.
   * Total RAM is 2MB (0x200000). We set the stack top at 1MB (0x100000)
   */
  . = 0x00100000;
  _stack_top = .;
}