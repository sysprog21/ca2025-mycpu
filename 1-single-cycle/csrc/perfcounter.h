#ifndef PERFCOUNTER_H
#define PERFCOUNTER_H

typedef unsigned long long uint64_t;

void perfcounter_init(void);
uint64_t get_cycles(void);
uint64_t get_instret(void);

#endif /* PERFCOUNTER_H */