/*
Copyright (C) 2021-2023 National Cheng Kung University, Taiwan.
All rights reserved.
*/

/**
 * Ring buffer is a fixed-size queue, implemented as a table of
 * pointers. Head and tail pointers are modified atomically, allowing
 * concurrent access to it. It has the following features:
 * - FIFO (First In First Out)
 * - Maximum size is fixed; the pointers are stored in a table.
 * - Lockless implementation.
 *
 * The ring buffer implementation is not preemptable.
 */

#include <stdint.h>
#include <stddef.h>
#include <stdbool.h>

#ifndef CACHE_LINE_SIZE
#define CACHE_LINE_SIZE 64
#endif
#define RING_COUNT (1u << 6)   // 64 entries (power of 2)
#define EINVAL 22
#define ENOBUFS 105
#define ENOENT  2
#define EDQUOT  122
// return -ENOBUFS etc.
typedef long ssize_t;



/* Compile-time check helper (C11). If not C11, replace with your own. */
#ifndef static_assert
#define static_assert _Static_assert
#endif

/* One cache-line worth of padding after a 32-bit field. */
#define PAD_AFTER_U32 (CACHE_LINE_SIZE - sizeof(uint32_t))

/* Producer/consumer blocks:
 *  - head is the only live field in its cache line (LR/SC target).
 *  - tail/mask/size/watermark share the next cache line (non-LR/SC line).
 *  - whole blocks are cache-line aligned.
 *
 * This minimizes “reservation breakage” caused by unrelated stores in the
 * same reservation granule/cache line as head.
 */

typedef struct __attribute__((aligned(CACHE_LINE_SIZE))) {
    /* Cache line 0: LR/SC target only */
    volatile uint32_t head;
    char _pad_head[PAD_AFTER_U32];

    /* Cache line 1: non-LR/SC metadata */
    volatile uint32_t tail;      /* written at end, read by other side */
    uint32_t mask;               /* read-only after init */
    uint32_t size;               /* read-only after init */
    uint32_t watermark;          /* read-only after init */
    char _pad_meta[CACHE_LINE_SIZE - 4 * sizeof(uint32_t)];
} ring_prod_t;

typedef struct __attribute__((aligned(CACHE_LINE_SIZE))) {
    /* Cache line 0: LR/SC target only */
    volatile uint32_t head;
    char _pad_head[PAD_AFTER_U32];

    /* Cache line 1: non-LR/SC metadata */
    volatile uint32_t tail;
    uint32_t mask;
    uint32_t size;
    char _pad_meta[CACHE_LINE_SIZE - 3 * sizeof(uint32_t)];
} ring_cons_t;

typedef struct {
    ring_prod_t prod;
    ring_cons_t cons;

    /* Ring array aligned to cache line */
    void *ring[] __attribute__((aligned(CACHE_LINE_SIZE)));
} ringbuf_t;

/* Layout assertions: head lines must be isolated and aligned. */
static_assert(sizeof(ring_prod_t) % CACHE_LINE_SIZE == 0, "prod size not multiple of cache line");
static_assert(sizeof(ring_cons_t) % CACHE_LINE_SIZE == 0, "cons size not multiple of cache line");

static_assert(offsetof(ring_prod_t, head) == 0, "prod.head not at block start");
static_assert(offsetof(ring_prod_t, tail) == CACHE_LINE_SIZE, "prod.tail not at next cache line");

static_assert(offsetof(ring_cons_t, head) == 0, "cons.head not at block start");
static_assert(offsetof(ring_cons_t, tail) == CACHE_LINE_SIZE, "cons.tail not at next cache line");

static_assert(((offsetof(ringbuf_t, prod.head) % CACHE_LINE_SIZE) == 0), "ringbuf.prod.head not cache-line aligned");
static_assert(((offsetof(ringbuf_t, cons.head) % CACHE_LINE_SIZE) == 0), "ringbuf.cons.head not cache-line aligned");


#ifndef __compiler_barrier
#define __compiler_barrier()             \
    do {                                 \
        asm volatile("" : : : "memory"); \
    } while (0)
#endif

static void *ringbuf_memset(void *dst, int value, size_t len)
{
    unsigned char *p = (unsigned char *) dst;

    while (len--) {
        *p++ = (unsigned char) value;
    }

    return dst;
}


static inline uint32_t mul_u32(uint32_t a, uint32_t b) {
    uint32_t r = 0;
    while (b) {
        if (b & 1) r += a;
        a <<= 1;
        b >>= 1;
    }
    return r;
}

#define RINGBUF_BYTES(count) \
        (((sizeof(ringbuf_t) + (count) * sizeof(void*) + (CACHE_LINE_SIZE - 1)) / CACHE_LINE_SIZE) * CACHE_LINE_SIZE)

static unsigned char ring_storage[RINGBUF_BYTES(RING_COUNT)]
    __attribute__((aligned(CACHE_LINE_SIZE)));

/* true if x is a power of 2 */
#define IS_POWEROF2(x) ((((x) -1) & (x)) == 0)
#define RING_SIZE_MASK (unsigned) (0x0fffffff) /**< Ring size mask */
#define ALIGN_CEIL(val, align) \
    (typeof(val))((val) + (-(typeof(val))(val) & ((align) -1)))

/* Calculate the memory size needed for a ring buffer.
 *
 * This function returns the number of bytes needed for a ring buffer, given
 * the number of elements in it. This value is the sum of the size of the
 * structure ringbuf and the size of the memory needed by the objects pointers.
 * The value is aligned to a cache line size.
 *
 * @param count
 *   The number of elements in the ring buffer (must be a power of 2).
 * @return
 *   - The memory size occupied by the ring buffer on success.
 *   - -EINVAL if count is not a power of 2.
 */
ssize_t ringbuf_get_memsize(const unsigned count)
{
    /* Requested size is invalid, must be power of 2, and do not exceed the
     * size limit RING_SIZE_MASK.
     */
    if ((!IS_POWEROF2(count)) || (count > RING_SIZE_MASK))
        return -EINVAL;

    ssize_t sz = sizeof(ringbuf_t) + count * sizeof(void *);
    sz = ALIGN_CEIL(sz, CACHE_LINE_SIZE);
    return sz;
}

/* Initialize a ring buffer.
 *
 * The ring size is set to *count*, which must be a power of two. Water
 * marking is disabled by default. The real usable ring size is (count - 1)
 * instead of (count) to differentiate a free ring from an empty ring buffer.
 *
 * @param r
 *   The pointer to the ring buffer structure followed by the objects table.
 * @param count
 *   The number of elements in the ring buffer (must be a power of 2).
 * @return
 *   0 on success, or a negative value on error.
 */
int ringbuf_init(ringbuf_t *r, const unsigned count)
{
    ringbuf_memset(r, 0, sizeof(*r));
    r->prod.watermark = count, r->prod.size = r->cons.size = count;
    r->prod.mask = r->cons.mask = count - 1;
    r->prod.head = r->cons.head = 0, r->prod.tail = r->cons.tail = 0;

    return 0;
}

static inline uint32_t lr_w_aq(volatile uint32_t *p)
{
    uint32_t v;
    asm volatile("lr.w.aq %0, (%1)" : "=r"(v) : "r"(p) : "memory");
    return v;
}

static inline void store_w_rl(volatile uint32_t *p, uint32_t v)
{
    uint32_t old, sc;
    do {
        asm volatile(
            "lr.w    %0, (%2)\n"
            "sc.w.rl %1, %3, (%2)\n"
            : "=&r"(old), "=&r"(sc)
            : "r"(p), "r"(v)
            : "memory"
        );
    } while (sc != 0);
}

/* CAS for head reservation (no aq/rl needed for head itself) */
static inline int cas_w(volatile uint32_t *p, uint32_t expect, uint32_t desired)
{
    uint32_t old, sc;
    // We will reuse 'old' (%0) as a temporary register for the backoff count
    // because if we are backing off, we are about to reload 'old' anyway.
    
    asm volatile(
        "0:\n"
        /* --- Load Reserved --- */
        "  lr.w    %0, (%2)\n"          
        "  bne     %0, %3, 1f\n"        

        /* --- Store Conditional --- */
        "  sc.w    %1, %4, (%2)\n"      
        "  beqz    %1, 2f\n"            // 0 means Success -> jump to end

        /* --- DETERMINISTIC BACKOFF (The Fix) --- */
        /* * Logic: delay = (mhartid + 1) * 16
         * Hart 0 waits ~16 loops.
         * Hart 1 waits ~32 loops.
         * They can NEVER collide in lockstep again.
         */
        "  csrr    %0, mhartid\n"       // Load Hart ID
        "  addi    %0, %0, 1\n"         // Add 1 (avoid 0 wait for Hart 0)
        "  slli    %0, %0, 4\n"         // Multiply by 16 (shift left 4)
        
        "3:\n"                          // Spin loop
        "  addi    %0, %0, -1\n"        
        "  bnez    %0, 3b\n"            
        "  j       0b\n"                // Retry CAS

        /* --- Failure Exit (Value mismatch) --- */
        "1:\n"
        "  li      %1, 0\n"             
        "  j       4f\n"

        /* --- Success Exit --- */
        "2:\n"
        "  li      %1, 1\n"             

        "4:\n"
        : "=&r"(old), "=&r"(sc)
        : "r"(p), "r"(expect), "r"(desired)
        : "memory"
    );
    return (int)sc;
}

/* * FIX 1: Atomic Read with Acquire 
 * Instead of 'lr.w', we use 'amoadd.w' with 0.
 * This effectively loads the value from memory atomically.
 */
static inline uint32_t amoadd_0_w_aq(volatile uint32_t *p)
{
    uint32_t v;
    // atomic_add(p, 0) returns the old value (the read value)
    asm volatile(
        "amoadd.w.aq %0, zero, (%1)" 
        : "=r"(v) 
        : "r"(p) 
        : "memory"
    );
    return v;
}

/* * FIX 2: Atomic Write with Release 
 * Instead of the lr/sc loop, we use 'amoswap.w'.
 * This unconditionally swaps the value in memory with 'v'.
 * It cannot fail, so no loop is needed.
 */
static inline void amoswap_w_rl(volatile uint32_t *p, uint32_t v)
{
    // We don't care about the old value (rd=zero), just write 'v'
    asm volatile(
        "amoswap.w.rl zero, %1, (%0)"
        : 
        : "r"(p), "r"(v) 
        : "memory"
    );
}

static inline int ringbuf_mp_enqueue_1(ringbuf_t *r, void *obj)
{
    uint32_t mask = r->prod.mask;

    while (1) {
        uint32_t head = r->prod.head;
        uint32_t cons_tail = amoadd_0_w_aq(&r->cons.tail);   // acquire observe frees
        uint32_t free_entries = mask + cons_tail - head;

        // Test if the ring is full
        if (free_entries == 0)
            return -ENOBUFS;

        uint32_t next = head + 1;

        /* LR/SC contention point: multiple producers fight on prod.head */
        if (!cas_w(&r->prod.head, head, next))
            continue;

        /* Write payload first */
        r->ring[head & mask] = obj;

        /*
         * Publish with RELEASE (no fence):
         * guarantees the ring slot store becomes visible before tail update.
         */
        while (r->prod.tail != head) { /* enforce in-order publish */ }
        store_w_rl(&r->prod.tail, next);

        return 0;
    }
}

static inline int ringbuf_mc_dequeue_1(ringbuf_t *r, void **obj_p)
{
    uint32_t mask = r->cons.mask;

    while (1) {
        uint32_t head = r->cons.head;
        uint32_t prod_tail = amoadd_0_w_aq(&r->prod.tail);   // acquire observe available

        // Test if the ring is empty  
        if ((prod_tail - head) == 0)
            return -ENOENT;

        uint32_t next = head + 1;

        /* LR/SC contention point: multiple consumers fight on cons.head */
        if (!cas_w(&r->cons.head, head, next))
            continue;

        /*
         * Because prod_tail was read with ACQUIRE, subsequent reads
         * (ring slot load) will see what producers published before tail.
         */
        void *obj = r->ring[head & mask];
        *obj_p = obj;

        /* Publish consumer progress with RELEASE */
        while (r->cons.tail != head) { /* in-order publish */ }
        store_w_rl(&r->cons.tail, next);

        return 0;
    }
}

static inline uint32_t read_mhartid(void)
{
    uint32_t x;
    asm volatile("csrr %0, mhartid" : "=r"(x));
    return x;
}

static volatile uint32_t start_count = 0;
static volatile uint32_t start_go = 0;


/* barrier_4 remains the same as your AMO fix */
static inline void barrier_4(void)
{
    /* ... your amoadd fix for start_count ... */
    int inc = 1;
    asm volatile("amoadd.w zero, %1, (%0)" :: "r"(&start_count), "r"(inc) : "memory");

    if (read_mhartid() == 0) {
        while (start_count < 4) { }
        amoswap_w_rl(&start_go, 1);           // NOW: Uses amoswap (Succeeds instantly)
    } else {
        while (amoadd_0_w_aq(&start_go) == 0) { } // NOW: Uses amoadd (Reads atomically)
    }
}

#define ITERS 5

int main(void)
{
    ringbuf_t *r = (ringbuf_t *)ring_storage;
    uint32_t id = read_mhartid();

    if (id == 0) ringbuf_init(r, RING_COUNT);
    barrier_4();

    if (id < 2) {
        /* 2 producers */
        for (uint32_t i = 0; i < ITERS; i++) {
            void *obj = (void *)(uintptr_t)((id << 28) ^ i);
            while (ringbuf_mp_enqueue_1(r, obj) != 0) { }
        }
    } else {
        /* 2 consumers */
        for (uint32_t i = 0; i < ITERS; i++) {
            void *obj;
            while (ringbuf_mc_dequeue_1(r, &obj) != 0) { }
        }
    }

    return 0;
}