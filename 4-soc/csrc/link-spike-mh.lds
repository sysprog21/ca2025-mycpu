OUTPUT_ARCH( "riscv" )
ENTRY(_start)

SECTIONS
{
  . = 0x80000000;
  .text : { *(.text.init) *(.text.startup) *(.text) }
  .data ALIGN(0x1000) : {
    *(.data*)
    *(.rodata*)
    *(.srodata*)
  }
  .sdata : {
    __sdata_start = .;
    PROVIDE(__global_pointer$ = . + 0x800);
    *(.sdata*)
  }
  .sbss : {
    __sbss_start = .;
    *(.sbss*)
    *(.scommon)
    . = ALIGN(4);
    __sbss_end = .;
    __sdata_end = .;
  }
  .tohost ALIGN(0x1000) : {
    . = ALIGN(8);
    tohost = .;
    QUAD(0);
    fromhost = .;
    QUAD(0);
  }
  . = 0x80100000;
  .bss ALIGN(0x1000) : {
    . = ALIGN(4);
    __bss_start = .;
    *(.bss*)
    *(COMMON)
    . = ALIGN(4);
    __bss_end = .;
  }
  _end = .;
}
