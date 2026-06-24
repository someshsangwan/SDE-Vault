# Chapter 4 — Memory Management & Virtual Memory

> Interview-focused notes. Covers: address binding, contiguous allocation, fragmentation,
> paging, TLB, segmentation, virtual memory, demand paging, page faults, page replacement
> algorithms (FIFO/Optimal/LRU/LFU), Belady's anomaly, thrashing, and live macOS commands
> to inspect memory usage.

---

## PART A — The Core Problem

Physical RAM is limited. Multiple programs need memory simultaneously. Four problems:

1. **Where in RAM does each program go?** → Memory allocation
2. **Programs think they own all memory** — but they share it → Isolation
3. **RAM smaller than all programs combined** → Virtual memory
4. **Memory gets fragmented over time** → Fragmentation

The OS solves all of these.

---

## PART B — Address Binding (how code finds memory)

When you write `int x = 5`, your program refers to a memory address for `x`.
**When** is that address decided?

### 1. Compile Time
Compiler hardcodes exact physical addresses. Program must always load at those addresses.
- Problem: if those addresses are taken → crash. Old, inflexible.

### 2. Load Time
OS decides where to put the program when it's loaded. Loader adjusts all addresses.
- Better, but program can't move after loading.

### 3. Execution Time (Runtime) ★ — what modern OSes do
Addresses are translated **dynamically while the program runs**.
- Program uses **logical addresses** (what it thinks).
- Hardware (MMU) translates to **physical addresses** (actual RAM) on the fly.

```
Your program uses:                  Hardware translates to:
logical address 0x0042   ──(MMU)──► physical address 0x8F42
(what code sees)                    (actual RAM location)
```

> **MMU (Memory Management Unit)** = hardware chip between CPU and RAM doing address
> translation on every single memory access — millions of times per second.

---

## PART C — Contiguous Memory Allocation

Simplest idea: give each process a **single unbroken chunk** of RAM.

```
RAM:
┌──────────┐  0
│  OS      │
├──────────┤
│ Process1 │  solid block
├──────────┤
│ Process2 │  solid block
├──────────┤
│  FREE    │
└──────────┘
```

Simple, but causes **fragmentation** over time as processes come and go.

---

## PART D — Fragmentation (★ interview favourite)

### External Fragmentation
Free memory exists in **total** but is **scattered in small holes** — no single hole is
big enough for the new process.

```
RAM after some processes finish:
┌──────────┐
│  FREE    │  200MB  ← not contiguous
├──────────┤
│ Process2 │
├──────────┤
│  FREE    │  150MB  ← not contiguous
├──────────┤
│ Process4 │
├──────────┤
│  FREE    │  100MB  ← not contiguous
└──────────┘

Total free = 450MB. New process needing 400MB CANNOT fit — no single block is 400MB.
That wasted scattered space = EXTERNAL fragmentation.
```

### Internal Fragmentation
A process is given **more memory than it needs** — wasted space inside the allocation.

```
Process asks for 18KB.
OS allocates in fixed 8KB units → gives 24KB (3 blocks).
Wasted: 24 - 18 = 6KB sitting idle inside → INTERNAL fragmentation.
```

### Side by side:

| | External Fragmentation | Internal Fragmentation |
|---|---|---|
| Where | between allocations (holes outside) | inside an allocation |
| Cause | variable-size allocations, processes come and go | fixed-size allocation units |
| Fix | compaction (shuffle memory), paging | smaller allocation units |

> **Backend connection:** JVM GC compaction phase literally moves objects to eliminate
> external fragmentation. `-XX:+UseG1GC` specifically handles heap fragmentation.
> `StackOverflowError` = stack fragmented/overflowed. `OutOfMemoryError` = heap full.

---

## PART E — Paging (★★★ the real solution)

**Paging** solves external fragmentation by abandoning the "contiguous block" requirement.

### The Big Idea
- Split physical RAM into fixed-size chunks → **frames**
- Split each process's memory into same-size chunks → **pages**
- Map pages to frames — **pages don't need to be in consecutive frames**

```
Process memory (pages):          Physical RAM (frames):
┌──────────┐                     Frame 0: [OS      ]
│  Page 0  │ ──────────────────► Frame 1: [Page 2  ]
├──────────┤                     Frame 2: [Page 0  ] ← Page 0 lands here
│  Page 1  │ ──────────────────► Frame 3: [OS      ]
├──────────┤  (not contiguous)   Frame 4: [Page 1  ] ← Page 1 lands here
│  Page 2  │ ──────────────────► Frame 5: [FREE    ]
└──────────┘
```

OS maintains a **Page Table** per process mapping page number → frame number:
```
Page Table:
  Page 0  →  Frame 2
  Page 1  →  Frame 4
  Page 2  →  Frame 1
```

### Address Translation with Paging

Logical address = **page number + offset**

```
Logical address: [  page number  |  offset  ]
                         ↓
                   look up page table
                         ↓
                 [ frame number  |  offset  ]  =  physical address
```

**Example** (page size = 4KB = 4096 bytes):
```
Logical address = 8300
Page number  = 8300 / 4096 = 2       (page 2)
Offset       = 8300 % 4096 = 108     (byte 108 within the page)
Page table:   Page 2 → Frame 5
Physical address = (5 × 4096) + 108 = 20588
```

- ✅ **No external fragmentation** — any free frame holds any page.
- ❌ **Internal fragmentation** still exists (last page of a process may not be full).
- ❌ **Page table can be huge** — 32-bit process with 4KB pages = 1 million page table entries!

---

## PART F — TLB (Translation Lookaside Buffer)

**Problem:** Every memory access with paging needs **two** memory accesses:
1. Look up page table → get frame number
2. Access actual data at physical address

This doubles memory access time!

**Solution: TLB** — a small, ultra-fast **cache inside the CPU** storing recent page→frame mappings.

```
CPU wants address → check TLB first
        │
        ├── TLB HIT  (mapping cached) → frame number instantly ✅ FAST (~10ns)
        │
        └── TLB MISS (not cached)     → go to page table in RAM → get mapping
                                         → store in TLB → retry  ❌ SLOW (~100ns)
```

```
TLB (tiny, lives in CPU — ~64 entries):
┌──────────────────────┐
│ Page 2  → Frame 5    │
│ Page 0  → Frame 2    │
│ Page 7  → Frame 11   │
│ ...                  │
└──────────────────────┘
```

**Effective Access Time (EAT):**
```
EAT = (hit rate × TLB time) + (miss rate × page table time)

Example: hit rate=90%, TLB=10ns, page table=100ns
EAT = (0.9 × 10) + (0.1 × 100) = 9 + 10 = 19ns  (vs 100ns without TLB — 5× faster!)
```

Programs tend to access the same pages repeatedly (**locality of reference**) →
TLB hit rates are typically **90–99%** in practice.

---

## PART G — Segmentation

Alternative to paging. Divide a process into **logical segments of variable size**
that match how the programmer thinks about memory.

```
Segment 0: Code     (600 bytes)
Segment 1: Stack    (200 bytes)
Segment 2: Heap     (1000 bytes)
Segment 3: Data     (300 bytes)
```

Logical address = `segment number + offset`
OS keeps a **segment table** mapping each segment to its base + limit in RAM.

| | **Paging** | **Segmentation** |
|---|---|---|
| Division unit | fixed-size pages | variable-size logical segments |
| Fragmentation | internal only | external only |
| Programmer visible? | No (transparent) | Yes (matches code structure) |
| Used in practice | ✅ dominant today | mostly historical |

> Modern systems use **paging** (sometimes paging + segmentation together).
> Pure segmentation = mostly exam questions now.

---

## PART H — Virtual Memory (★★★ the big one)

Everything above assumed the **entire process fits in RAM**. What if it doesn't?

**Virtual Memory** = OS creates the **illusion** that each process has a huge private
address space, even if physical RAM is much smaller.

> Key insight: **Not all of a process needs to be in RAM at the same time.**
> Only the parts currently being used need to be loaded. The rest sits on disk.

```
Virtual address space (huge — process thinks it has all this):
┌──────────────────────────────────────────────────────┐
│ Code │ Data │ Heap ──────────────► │ ◄──────── Stack │
└──────────────────────────────────────────────────────┘
           ↕ only active pages actually in RAM
Physical RAM (smaller):          Disk (swap space):
┌─────────────────┐    ◄────►    ┌────────────────────┐
│ some pages here │              │ rest of pages here  │
└─────────────────┘              └────────────────────┘
```

This means:
- You can run a **4GB process on a 2GB RAM machine**.
- You can run **many more processes** than would fit in RAM simultaneously.

### RSS vs VSZ — seeing virtual memory live:
```bash
ps -axo pid,rss,vsz,pmem,comm | sort -nrk2 | head -10
```

Real output from this machine:
```
IntelliJ (idea):
  VSZ = 422,624,352 KB ≈ 403 GB  ← virtual address space claimed
  RSS =   3,742,592 KB ≈ 3.6 GB  ← actually IN physical RAM right now

java process:
  VSZ = 428,192,064 KB ≈ 408 GB  ← claims 408GB virtual
  RSS =   1,717,024 KB ≈ 1.6 GB  ← only 1.6GB actually in RAM
```

- **VSZ (Virtual Size)** = total virtual address space claimed (may include disk-backed pages).
- **RSS (Resident Set Size)** = pages actually IN RAM right now.

> IntelliJ claims 403GB virtual but only 3.6GB is in RAM — this IS demand paging working.
> System-wide: `VM: 382T vsize` — 382 terabytes of virtual space on a ~47GB RAM machine!

---

## PART I — Demand Paging & Page Faults

**Demand paging** = load a page into RAM **only when actually needed**, not upfront.

### Page Fault — what happens when a needed page isn't in RAM:

```
1. CPU tries to access a logical address
2. MMU checks page table → page NOT in RAM (invalid bit set)
3. MMU raises a PAGE FAULT (hardware interrupt)
4. OS page fault handler takes over:
      a. Find the page on disk (swap space)
      b. Find a free frame in RAM (or evict a page — see replacement below)
      c. Load the page from disk into that frame
      d. Update page table (set valid bit, set frame number)
5. Restart the instruction that caused the fault
6. This time → page is in RAM → proceeds normally
```

```
Process running
     │
     ▼
access page X ──► in RAM? ──YES──► proceed normally ✅ (fast)
                      │
                     NO
                      │
                      ▼
                 PAGE FAULT → find on disk → load to RAM → retry ❌ (slow)
```

**Page fault is expensive** — disk is ~100,000× slower than RAM → minimize faults!

---

## PART J — Page Replacement Algorithms (★★ exam + interview)

When a page fault occurs and RAM is **full**, OS must **evict** a page to make room.
Which page to evict? → page replacement algorithms.

### 1. FIFO — First In First Out
Evict the page that has been in RAM the **longest** (oldest loaded).

```
Reference string: 1 2 3 4 1 2 5 1 2 3 4 5    (frames = 3)

1: [1  -  -]  fault
2: [1  2  -]  fault
3: [1  2  3]  fault
4: [4  2  3]  fault  (evict 1, oldest)
1: [4  1  3]  fault  (evict 2, oldest)
2: [4  1  2]  fault  (evict 3, oldest)
5: [5  1  2]  fault  (evict 4, oldest)
            ...
Total = 9 faults
```

- ✅ Simple to implement.
- ❌ Evicts the oldest, not the least useful — an old page might be heavily used.
- ❌ Suffers from **Belady's Anomaly**.

---

### 2. Optimal (OPT)
Evict the page that **will not be used for the longest time in the future**.

- ✅ **Theoretically best** — minimum possible page faults.
- ❌ **Impossible in practice** — requires knowing the future.
- Used only as a **benchmark** to compare other algorithms against.

---

### 3. LRU — Least Recently Used ★ most used in practice
Evict the page that **hasn't been used for the longest time**.

Based on **locality of reference** — recently used pages will likely be used again soon.

```
Reference string: 7 0 1 2 0 3 0 4 2 3 0 3 2   (frames = 3)

7: [7  -  -]  fault
0: [7  0  -]  fault
1: [7  0  1]  fault
2: [2  0  1]  fault  (evict 7, least recently used)
0: [2  0  1]  hit    (0 is in RAM)
3: [2  0  3]  fault  (evict 1, LRU)
0: [2  0  3]  hit
4: [4  0  3]  fault  (evict 2, LRU)
...
```

- ✅ **Close to optimal** in practice.
- ✅ Does **NOT** suffer from Belady's anomaly.
- ❌ Expensive to implement perfectly (need to track exact last-use time for every page).
- Real OSes use **approximations** (reference bits, clock/second-chance algorithm).

> **Backend connection — LRU is everywhere you've used caching:**
> - Redis: `maxmemory-policy allkeys-lru`
> - CPU cache eviction
> - Browser cache
> - CDN cache
    > "Evict the least recently used item" — you've configured this hundreds of times.
    > Same algorithm, just applied to cache entries instead of RAM pages.

---

### 4. LFU — Least Frequently Used
Evict the page with the **fewest total accesses**.

- ✅ Makes sense — rarely used pages should leave.
- ❌ A page heavily used in the past but not recently will stay forever.
- ❌ Counter overhead per page.

---

### Algorithm Comparison:

| Algorithm | Evict which page | Practical? | Belady's anomaly? |
|---|---|---|---|
| **FIFO** | oldest loaded | Yes | YES ❌ |
| **Optimal** | longest future wait | No (needs future) | No |
| **LRU** | least recently used | Yes (approx) | No ✅ |
| **LFU** | least frequently used | Sometimes | No |

---

## PART K — Belady's Anomaly (★ interview trap)

**Common sense:** more RAM frames → fewer page faults. Always, right?

**Belady's Anomaly:** with **FIFO**, adding more frames can **increase** page faults!

```
Reference string: 1 2 3 4 1 2 5 1 2 3 4 5

3 frames → 9 page faults
4 frames → 10 page faults  ← MORE frames, MORE faults! 😱
```

- Only affects **FIFO** (and a few other algorithms).
- **LRU and Optimal do NOT suffer from Belady's anomaly** — more frames always means
  equal or fewer faults.

> **Interview trap:** "Does LRU suffer from Belady's anomaly?" → **No.**
> "Which algorithm suffers from Belady's anomaly?" → **FIFO.**

---

## PART L — Thrashing (★★ must know)

**Thrashing** = system spends more time **swapping pages in/out of disk** than
**actually running processes**. CPU utilizationcollapses. Machine feels completely frozen.

### How it happens:
```
Too many processes in RAM
  → each process gets too few frames
  → every memory access causes a page fault
  → OS constantly loading pages from disk, evicting others
  → CPU busy doing page swaps, not actual work
  → CPU utilization drops to near 0%
  → system feels completely frozen/hung
```

```
CPU utilization
100% |         ★
     |       /
     |      /
     |     /  ← more processes = better utilization (initially)
  0% |    /___________________________
     |                                ← THRASHING: utilization collapses
     └────────────────────────────────► number of processes in memory
```

### Why it happens:
Each process has a **working set** — the pages it's actively using right now.
If OS gives it fewer frames than its working set → constant page faults → thrashing.

### Fixes:
1. **Working set model** — track each process's active pages, give it at least that many frames.
2. **Reduce multiprogramming** — swap out entire processes to give remaining ones more frames.
3. **Add more RAM** — the real-world fix.

> **Backend connection:** You've felt thrashing if you ran too many services on a low-RAM
> machine and everything ground to a halt. Linux starts using swap (disk), disk I/O hits
> 100%, nothing responds.
> `free -h` → if `swap used` is high and increasing → your system is approaching thrashing.

---

## 🖥️ HANDS-ON: Inspecting Memory on macOS

### System-wide memory:
```bash
top -l 1 | grep PhysMem    # RAM used/free
top -l 1 | grep "VM:"      # virtual memory + swap activity
```

Real output from this machine:
```
PhysMem: 43G used (3649M wired, 6447M compressor), 4214M unused
VM: 382T vsize, 595348 swapins, 727739 swapouts
    ↑ 382 TERABYTES virtual space on ~47GB RAM machine = virtual memory!
    swapins/swapouts = pages moved between RAM and disk (lifetime totals)
```

### Per-process memory:
```bash
ps -axo pid,rss,vsz,pmem,comm | sort -nrk2 | head -10
```
- **RSS** = Resident Set Size = pages **actually in RAM** right now (KB)
- **VSZ** = Virtual Size = total **virtual address space** claimed (KB)
- **%MEM** = percentage of physical RAM this process uses

### Checking for thrashing (Linux — your prod servers):
```bash
free -h                    # RAM + swap overview
vmstat 1 5                 # memory + swap I/O every 1s, 5 times
                           # watch 'si' (swap in) and 'so' (swap out) columns
                           # if si/so are high and non-zero → thrashing warning
cat /proc/meminfo          # detailed memory breakdown
```

### Full command cheat-sheet:

| Goal | Command |
|---|---|
| System RAM + swap | `top -l 1 \| grep -E "PhysMem\|VM:"` |
| Top memory hogs (RSS) | `ps -axo pid,rss,vsz,pmem,comm \| sort -nrk2 \| head -10` |
| Live memory monitor | `top` then press `o`, type `mem` to sort by memory |
| (Linux) RAM overview | `free -h` |
| (Linux) swap activity | `vmstat 1 5` (watch si/so columns) |
| (Linux) per-process | `ps axo pid,rss,vsz,pmem,comm --sort=-rss \| head -10` |
| (Linux) detailed | `cat /proc/meminfo` |

---

## ✅ Chapter 4 Summary (quick revision)

- **Address binding** — compile / load / **runtime** (modern). MMU does live translation.
- **Contiguous allocation** — simple but causes fragmentation.
- **External fragmentation** — free space scattered in holes (fix: paging).
- **Internal fragmentation** — allocated block bigger than needed (fix: smaller pages).
- **Paging** — fixed pages mapped to frames via page table. No external fragmentation.
    - Logical address = page number + offset → look up page table → physical address.
- **TLB** — CPU cache for page table lookups. 90%+ hit rate. Makes paging fast.
    - TLB hit → fast. TLB miss → go to RAM page table → slow.
- **Segmentation** — variable-size logical segments. Mostly historical.
- **Virtual memory** — illusion of large address space. Only active pages in RAM.
    - VSZ (claimed virtual space) vs RSS (actually in RAM). Saw 382TB virtual on 47GB machine!
- **Demand paging** — load pages only when needed. Page fault = page missing from RAM.
    - Page fault: MMU raises interrupt → OS loads page from disk → restart instruction.
- **Page replacement algorithms:**
    - **FIFO** — evict oldest. Simple. Suffers from **Belady's anomaly**.
    - **Optimal** — evict longest future wait. Best possible. Impossible in practice.
    - **LRU** — evict least recently used. Best practical. No Belady's. = cache eviction you know.
    - **LFU** — evict least frequently used.
- **Belady's anomaly** — FIFO only: more frames can cause MORE faults. LRU is immune.
- **Thrashing** — too few frames per process → constant page faults → CPU utilization collapses.
    - Fix: working set model, reduce multiprogramming, add RAM.