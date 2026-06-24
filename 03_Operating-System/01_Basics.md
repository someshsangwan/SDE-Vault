# Chapter 1 — OS Basics & Processes

> Interview-focused notes. Covers: what an OS is, kernel/user mode, system calls,
> types of OS, the four "multi-*" terms, processes (memory layout, states, PCB,
> context switching), process creation (fork/exec/wait), zombie & orphan processes,
> and threads/multithreading. Includes hands-on macOS commands to inspect everything live.

---

## PART A — What is an Operating System?

Hardware (CPU, RAM, disk, network card) is just dumb electronics. Your programs need
to use that hardware, but letting every program touch hardware directly = chaos
(crashes, conflicts). So we need a **manager** between apps and hardware.

**That manager is the Operating System.**

```
┌─────────────────────────────┐
│   Your Apps (Chrome, Java,  │   ← User programs
│   your backend service)     │
├─────────────────────────────┤
│     Operating System        │   ← The manager (Linux, Windows, macOS)
├─────────────────────────────┤
│   Hardware (CPU, RAM, Disk) │   ← Physical electronics
└─────────────────────────────┘
```

### The OS has two main jobs
1. **Resource Manager** — decides who gets the CPU, how much RAM each program gets,
   who can access disk. Like traffic police for hardware.
2. **Abstraction provider (Extended Machine)** — hides ugly hardware details. You write
   `file.write("hello")` and never think about disk sectors/voltages. OS does the dirty work.

> **Backend connection:** When your Spring Boot app writes a log or opens a DB socket,
> you're using OS abstractions. You never touch hardware directly — the OS does it.

---

## PART B — Kernel Mode vs User Mode

The core of the OS is the **Kernel** — the most trusted part, it can directly touch hardware.

The CPU runs in **two modes**:

| Mode | Who runs here | Power |
|------|--------------|-------|
| **User Mode** | Your apps (Chrome, your backend service) | Restricted — cannot touch hardware directly |
| **Kernel Mode** | The OS kernel | Full power — can do anything |

**Why two modes? → Safety.** If a buggy program could directly access any RAM/hardware,
one bad line could crash the whole machine or corrupt other programs. So normal apps run
in restricted **user mode**.

But apps DO need files/network... how? → They ask the OS via a **System Call**.

---

## PART C — System Calls

A **system call** is how a user program requests a service from the kernel.
It's the **doorway** from user mode into kernel mode.

Flow when your Java code does `Files.read(...)`:
```
1. Your code (USER MODE) says "I want to read this file"
2. Triggers a system call (e.g., read())
3. CPU switches USER MODE → KERNEL MODE  (a "trap")
4. Kernel reads the file from disk (it has the power)
5. Kernel hands data back to your program
6. CPU switches KERNEL MODE → USER MODE
7. Your code continues with the data
```

Common system call categories:
- **Process control:** `fork()`, `exec()`, `exit()`
- **File management:** `open()`, `read()`, `write()`, `close()`
- **Communication:** `socket()`, `send()`, `recv()`
- **Memory:** allocate/free memory

### ⭐ User-mode vs Kernel-mode call EXAMPLES (my question)

**Stays in USER MODE** (just CPU + your own memory, no OS needed — FAST):
```java
int a = 5 + 3;            // math → just CPU
String name = "somesh";   // assign variable → your own RAM
list.add(item);           // manipulate your own data structure
for (int i=0;i<10;i++){}  // loop → just CPU
if (x > y) { ... }        // comparison → just CPU
factorial(n);             // calling YOUR OWN function → CPU + stack
```

**Requires a SYSTEM CALL** (needs hardware / OS-managed resource → kernel mode):
```java
System.out.println("hi");      // print to screen → write()
Files.read(path);              // read file → open()/read()
socket.send(data);             // network → send()
new Thread().start();          // create thread → kernel
db.executeQuery("SELECT...");  // network I/O → socket calls
Thread.sleep(1000);            // ask OS to pause me
new byte[100_000_000];         // big memory request → mmap/brk
```

| What you're doing | Mode | Why |
|---|---|---|
| `2 + 2`, loops, if-else | User | Pure CPU |
| Modify a List/Map in memory | User | Your own RAM |
| println / log to console | **Kernel** | Needs the screen (hardware) |
| Read/write a file | **Kernel** | Needs the disk |
| DB query / HTTP call | **Kernel** | Needs the network |
| Create thread/process | **Kernel** | OS manages these |
| Allocate huge memory | **Kernel** | OS hands out RAM |

**Mental model:** Your program is in a sandbox (user mode). Inside the sandbox → do
anything with your toys (CPU math, own memory) = FREE & FAST. Want something OUTSIDE
(disk, network, screen, more memory)? → raise your hand and ask the OS = a **system call**.

> **Backend gold:** This is WHY CPU-bound work (calculations, in-memory) is fast, while
> I/O-bound work (DB, file, network) is slow — every I/O pays the cost of a system call +
> mode switch into the kernel and back. It's also why we use **buffering** (collect many
> log lines, write once) — to reduce the number of expensive system calls.

---

## PART D — Types of Operating Systems

- **Batch OS** — jobs collected in batches, run one after another, no interaction (punch cards).
- **Multiprogramming OS** — multiple programs in memory; when one waits for I/O, CPU runs another.
- **Multitasking / Time-sharing OS** — CPU switches between programs so fast it feels simultaneous.
- **Multiprocessing OS** — multiple CPUs/cores running tasks truly in parallel.
- **Real-time OS (RTOS)** — must respond within strict deadlines (airbags, medical, embedded).

> Key evolution idea: we kept finding ways to **keep the expensive CPU busy** instead of
> letting it idle while waiting for slow I/O.

---

## PART E — Processes

### Program vs Process (classic Q)
- **Program** = passive file on disk (`.jar`, `.exe`), does nothing until run.
- **Process** = a program **in execution** — alive, with allocated resources.
- One program → many processes (open Chrome 3× = 1 program, 3 processes).

```
Program = recipe on paper (passive) | Process = actually cooking (active, using resources)
```

> **Backend:** `app.jar` is a program. `java -jar app.jar` creates a **process**.
> Run on 3 servers → 3 processes from one program.

### Process Memory Layout (★ Stack vs Heap heavily asked)
```
 High addresses
┌──────────────┐
│    STACK     │  ← local vars, function call frames | grows DOWN ↓
│      ▼       │
│      ▲       │
│    HEAP      │  ← objects created at runtime (new/malloc) | grows UP ↑
├──────────────┤
│    DATA      │  ← global & static variables
├──────────────┤
│  TEXT (Code) │  ← compiled instructions
└──────────────┘
 Low addresses
```

| Section | Holds | Java example |
|---|---|---|
| **Text/Code** | compiled instructions | your bytecode/methods |
| **Data** | global & static vars | `static int count;` |
| **Heap** | runtime objects, dynamic memory | `new User()`, `new ArrayList<>()` |
| **Stack** | local vars, call frames | `intx` in a method, params |

**Stack vs Heap:**

| | Stack | Heap |
|---|---|---|
| Stores | local vars, function calls | objects, dynamic data |
| Managed | automatically (freed on return) | manually / Garbage Collector |
| Speed | very fast | slower |
| Size | small, limited | large |
| Overflow error | **StackOverflow** (deep recursion) | **OutOfMemory** (too many objects) |

> **Backend:** `StackOverflowError` = stack section overflowed (infinite recursion).
> `OutOfMemoryError` = heap filled up (leak). JVM `-Xmx` flag sizes the **heap**.

### Process States (★)
```
                  ┌─────────┐
       admitted   │   NEW   │
      ┌───────────└─────────┘
      ▼
┌──────────┐  scheduler picks  ┌──────────┐  exit  ┌────────────┐
│  READY   │ ────────────────► │ RUNNING  │ ─────► │ TERMINATED │
│          │ ◄──────────────── │          │        └────────────┘
└──────────┘ time slice expired└──────────┘
      ▲        (preempted)          │
      │   I/O done                  │ needs I/O
      │                             ▼
      │                       ┌──────────┐
      └───────────────────────│ WAITING  │ (blocked on disk/network/etc.)
                              └──────────┘
```
1. **New** — being created.
2. **Ready** — in RAM, ready to run, waiting for CPU (sits in a queue).
3. **Running** — executing on CPU (only one per core at a time).
4. **Waiting/Blocked** — paused, waiting for I/O (disk, network, DB).
5. **Terminated** — finished, being cleaned up.

Crucial transitions:
- **Ready → Running:** scheduler picks it.
- **Running → Ready:** time slice expired → kicked out (**preemption**).
- **Running → Waiting:** made an I/O request, gives up CPU voluntarily (no point holding it).
- **Waiting → Ready:** I/O finished, ready again (must re-queue for CPU).

> **Backend gold:** A DB query moves your thread Running → Waiting. CPU doesn't idle —
> it runs another request. Foundation of handling many concurrent users, and why
> **async/non-blocking I/O** matters: free the thread instead of leaving it in WAITING.

### PCB — Process Control Block
The process's **"ID card + save file."** One PCB per process. When the OS pauses a process
and later resumes it, the PCB is how it remembers exactly where it left off.

Contains:
- **PID** (unique id), **State**, **Program Counter** (which instruction is next — the bookmark!),
  **CPU Registers**, **Memory info**, **Open files (fds)**, **Scheduling info (priority)**.

```
┌─────────────────────┐
│        PCB          │
│ PID:           1234 │
│ State:      Running │
│ Program Counter:0x4A│ ← "resume from HERE"
│ Registers:    [...] │ ← "these were my values"
│ Memory pointers     │
│ Open files: [3,4,7] │
│ Priority:      High │
└─────────────────────┘
```
> Analogy: interrupted while reading → note page number (program counter) + bookmark +
> what you were thinking (registers). Resume seamlessly later. PCB = that bookmark + notes.

### Context Switching
Switching the CPU from one process to another:
```
Process A running
  ▼
1. SAVE A's state → into A's PCB (program counter, registers...)
  ▼
2. LOAD B's state ← from B's PCB
  ▼
Process B running (resumes where it left off)
```
**Why expensive:** during the switch the CPU does bookkeeping, NOT your actual program
(pure overhead). Also trashes CPU cache (new process's data not cached → cache misses).

> **Backend gold:** #1 reason creating thousands of threads hurts — more threads → more
> context switching → CPU switches instead of working. Hence **thread pools** (fixed thread
> count) and lightweight concurrency (async, Node event loop, Java virtual threads).

---

## PART F — Process Creation: fork / exec / wait

**Every process has a parent.** Creator = parent, new one = child → forms a family tree.
The first process is hand-created by the kernel at boot: **PID 1** (`launchd` on Mac,
`systemd`/`init` on Linux). Everything descends from it.

### fork() — clone yourself
Creates a child by **duplicating the parent** (copy of code, data, heap, stack).
Called **once**, returns **twice**:
```c
int pid = fork();
if (pid > 0)      { /* PARENT — fork returned child's PID */ }
else if (pid==0)  { /* CHILD  — fork returned 0 */ }
else              { /* fork failed, returned -1 */ }
```
```
 BEFORE              AFTER
 ┌────────┐    ┌────────┐  ┌────────┐
 │ Parent │ →  │ Parent │  │ Child  │
 │PID 100 │    │PID 100 │  │PID 101 │
 └────────┘    └────────┘  └────────┘
              returns 101  returns 0
```

### exec() — become a different program
Replaces the current process's program with a **new one** (same PID, code wiped & replaced).
A fresh fork is just a clone — usually you then `exec()` to become what you actually want.

### wait() — parent waits for child
Makes the parent **pause** until the child finishes, then collects the child's **exit status**.
```
Parent: fork() → child starts
Parent: wait() → parent SLEEPS (Waiting state)
                 child runs... exits
Parent: wakes ← gets exit status, continues
```

> **The classic combo — how your shell runs EVERY command:** type `ls` →
> 1. shell **fork()s** a child  2. child **exec()s** `ls`  3. shell **wait()s** for it,
     > then shows the prompt again. You've used fork+exec+wait thousands of times!
>
> Try: `sleep 5` → prompt freezes 5s (shell in wait()). `sleep 5 &` → prompt returns
> instantly (`&` tells shell NOT to wait).

---

## PART G — Zombie & Orphan Processes (favourite Q)

### 🧟 Zombie
A process that **finished (terminated)** but whose **parent hasn't called wait()** yet.
Its entry lingers in the process table (holding its exit status).
- Not running, not using CPU/memory — but occupies a process-table slot.
- **Problem:** if a buggy parent keeps spawning children and never wait()s, zombies pile up
  and **exhaust the process table** (can't create new processes).
- Shown as **`Z`** in STAT column (or `<defunct>`).
- **Fix:** parent must call wait(). If parent dies, PID 1 (launchd/init) adopts & reaps it.

### 👶 Orphan
A process whose **parent terminated first** while the **child is still running**.
- **Not a problem** — PID 1 (launchd/init) **adopts** it (its PPID becomes 1) and reaps it later.

### Zombie vs Orphan (one-liner)

| | Zombie 🧟 | Orphan 👶 |
|---|---|---|
| Who died? | **Child** died, parent alive | **Parent** died, child alive |
| State | child finished but not reaped | child still running |
| Problem? | Yes — wastes process-table slots | No — adopted by PID 1 |
| Cause | parent didn't call wait() | parent exited before child |

> Memory trick: **Zombie** = dead child still "haunting" the process table.
> **Orphan** = living child whose parent is gone, gets adopted.

---

## PART H — Threads & Multithreading

A **thread** = lightweight unit of execution **inside a process**. One process can have many
threads, all **sharing the same memory**. Creating a thread is far cheaper than a process.

```
┌─────────── PROCESS ───────────┐
│  CODE │ DATA │ HEAP           │ ← SHARED by all threads
│ ┌──────┐┌──────┐┌──────┐      │
│ │Thr 1 ││Thr 2 ││Thr 3 │      │
│ │stack ││stack ││stack │      │ ← each thread: OWN stack
│ │regs  ││regs  ││regs  │      │   + own registers + own PC
│ │ PC   ││ PC   ││ PC   │      │
│ └──────┘└──────┘└──────┘      │
└───────────────────────────────┘
```

### Shared vs Private (★ top Q)

| Shared among threads | Private to each thread |
|---|---|
| Code (Text) | Stack (own local vars) |
| Data (globals/statics) | Registers |
| **Heap** (objects!) | Program Counter |
| Open files / resources | Thread ID |

> **Backend:** Threads share the **heap** → two threads can hit the same object → that's why
> a shared `HashMap` can corrupt → use `ConcurrentHashMap`/`synchronized`. Local variables
> (stack) are safe — private per thread.

### Process vs Thread (#1 Q)

| | Process | Thread |
|---|---|---|
| Memory | own separate space | shares process memory |
| Creation cost | heavy | lightweight |
| Communication | hard (needs IPC) | easy (shared memory) |
| Crash impact | one crash doesn't kill others | one thread crash can kill whole process |
| Context switch | expensive | cheaper |
| Isolation | strong | weak |

> Analogy: **Process** = a house (own plot/utilities). **Threads** = roommates sharing
> kitchen/living room (heap/data) but each has own bedroom (stack). Cheap to add a roommate,
> expensive to build a house. Roommates can mess with shared stuff → need rules (locks).

### Concurrency vs Parallelism
- **Concurrency** = dealing with many tasks by **rapidly switching** on one core (illusion of simultaneous).
- **Parallelism** = actually running many tasks at the **same instant** on **multiple cores**.
```
CONCURRENCY (1 core): T1─T2─T1─T2─T1─T2   (switching fast)
PARALLELISM (2 cores):T1───────────────
                      T2───────────────  (truly simultaneous)
```

### User-level vs Kernel-level threads
- **Kernel-level:** OS schedules each thread. ✅ true parallelism, ✅ one blocking doesn't stop
  others. ❌ slower (every op is a system call).
- **User-level:** managed by a user-space library; kernel sees just one process. ✅ very fast
  create/switch. ❌ can't use multiple cores; ❌ one blocking system call blocks the WHOLE process.

> **Modern relevance:** Java **Virtual Threads** / Go **goroutines** = lightweight user-ish
> threads mapped onto a few kernel threads → millions of cheap threads without the cost of
> millions of kernel threads + context switches. Hot 2024-2026 backend interview topic.

### Multithreading models

| Model | Mapping | Note |
|---|---|---|
| Many-to-One | many user → 1 kernel | one block = all block, no true parallelism |
| One-to-One | each user → own kernel | true parallelism; many threads = costly (traditional Java) |
| Many-to-Many | many user → fewer kernel | best of both, complex (virtual threads/goroutines feel like this) |

---

## The four "multi-*" terms (★ interview trap)

| Term | What's "multi" | CPUs | Concurrency / Parallelism | Goal |
|---|---|---|---|---|
| **Multiprogramming** | programs in RAM | 1 | Concurrency (switch on I/O block) | keep CPU busy |
| **Multitasking** (time-sharing) | programs sharing CPU via timer | 1 | Concurrency (switch on time-slice) | responsiveness |
| **Multiprocessing** | CPUs/cores | **many** | **Parallelism** | true simultaneous execution |
| **Multithreading** | threads in one process | 1 or many | both | concurrency within an app |

Key distinctions to say out loud:
- **Multiprogramming vs Multitasking:** both single-CPU. Multiprogramming switches only when a
  program **blocks (I/O)**; multitasking switches on a **timer** too (time-slicing) → interactive.
- **Multitasking vs Multiprocessing:** multitasking = one CPU faking simultaneity (concurrency);
  multiprocessing = multiple CPUs actually simultaneous (parallelism).
- **Multiprocessing vs Multithreading:** multiprocessing = multiple processes/CPUs (separate
  memory); multithreading = multiple threads in one process (shared memory).

> **Backend:** Your 8-core server (**multiprocessing** hardware) runs a Java app using a thread
> pool (**multithreading**), while the OS **multitasks** between your app, the DB, nginx, etc.

---

## 🖥️ HANDS-ON: Inspecting processes & threads (macOS)

### `ps aux` — list all processes
- `a`=all users, `u`=detailed format, `x`=include background processes.

Columns: **USER | PID | %CPU | %MEM | VSZ | RSS | STAT | STARTED | TIME | COMMAND**
- **PID** = the process ID. **STAT** = the process state (maps to our theory!).

**STAT codes ↔ theory:**
| Code | Meaning |
|---|---|
| **R** | Running or Ready (runnable) |
| **S** | Sleeping/Waiting (blocked on event) — most processes! |
| **I** | Idle (long sleep) |
| **T** | Stopped |
| **Z** | **Zombie** |
| `s` | session leader |
| `+` | foreground |

> Most processes sit in **S (Waiting)** — exactly the Waiting state from theory.

### Command cheat-sheet

| Goal | Command |
|---|---|
| List all processes | `ps aux` |
| Top CPU hogs | `ps aux \| sort -nrk 3 \| head -10` |
| Top memory hogs | `ps aux \| sort -nrk 4 \| head -10` |
| **Live monitor (best)** | `top` (press `q` to quit) |
| Live, sorted by CPU | `top -o cpu` |
| One-shot top-10 by CPU | `top -l 1 -o cpu -n 10` |
| Find a process | `pgrep -l java`  or  `ps aux \| grep java` |
| Kill a process | `kill <PID>`  (force: `kill -9 <PID>`) |
| System CPU/mem summary | `top -l 1 \| head -10` |
| Parent/child (PPID) | `ps -eo pid,ppid,comm` |
| Process tree | `ps -ejH`  (or `pstree` if installed) |
| Hunt zombies | `ps aux \| grep -w Z` or `grep defunct` |
| Threads per process (sorted) | `top -l 1 -stats pid,th,command -o th -n 10` |

`sort -nrk 3` = sort **n**umeric, **r**everse, by column **3** (=%CPU). col 4 = %MEM.

### Reading the system summary (`top -l 1 | head -10`)
```
Processes: 944 total, 2 running, 942 sleeping, 5722 threads
Load Avg: 1.76, 2.40, 2.85
CPU usage: 4.13% user, 9.30% sys, 86.55% idle
PhysMem: 47G used, 642M unused
```
- **2 running** = R state (using CPU). **942 sleeping** = S/Waiting (theory confirmed!).
- **5722 threads** across 944 processes.
- **Load Avg** = avg load over 1/5/15 min; if it exceeds core count consistently → overloaded.
- **CPU usage maps to kernel vs user mode!**
    - `user` = CPU running YOUR programs (user mode)
    - `sys`  = CPU running the KERNEL (system calls — kernel mode!)
    - `idle` = doing nothing
  > That `sys%` is literally the cost of system calls we discussed.

### Parent/child example (real output)
```
PID 1     PPID 0    /sbin/launchd        ← ancestor of everything
PID 9374  PPID 1    Terminal             ← launched by launchd
PID 32483 PPID 90818 cef_server          ← spawned by IntelliJ (90818)
PID 32485 PPID 32483 cef_server Helper   ← spawned by cef_server → family tree 🌳
```
This is why Chrome/IntelliJ show many processes — main process **forks** children for
tabs/plugins/GPU.

### Threads example (real output, `top -l 1 -stats pid,th,command -o th`)
```
PID    #TH    COMMAND
0      888/12 kernel_task    ← kernel runs hundreds of threads
90884  218    java           ← ONE java process, 218 threads, one shared heap!
90818  147/4  idea
```
> That `java` process = theory made real: one process, one shared heap, 218 execution paths.
> Two of them touching the same object unsynchronized → race condition (Chapter 3).

### Linux note (prod servers)
- `ps -eLf` → one line per thread. `htop` (press `H` to toggle threads). `pstree`.
- `ps axo pid,nlwp,comm --sort=-nlwp` → threads per process.

---

## ✅ Chapter 1 Summary (quick revision)

- **OS** = resource manager + abstraction between apps & hardware.
- **Kernel vs user mode** — seen live as `sys%` vs `user%` in `top`.
- **System calls** = doorway user→kernel (I/O, threads, memory). Why I/O is slow.
- **Program** (passive file) vs **Process** (running, with resources).
- Memory layout: **Text, Data, Heap, Stack** (StackOverflow vs OutOfMemory).
- **5 states:** New→Ready→Running→Waiting→Terminated (= STAT codes in `ps`).
- **PCB** = process's save file (PID, program counter, registers...).
- **Context switch** = save one PCB, load another — costly overhead → why thread pools exist.
- **fork** (clone) / **exec** (replace program) / **wait** (parent waits) — every shell command.
- **Zombie** (dead child not reaped — problem, `Z` state) vs **Orphan** (parent died, adopted by PID 1).
- **Thread** = lightweight execution inside a process; heap shared, stack private.
- **Concurrency** (switch on 1 core) vs **Parallelism** (multiple cores).
- **User vs kernel threads**; virtual threads/goroutines = cheap many-to-many.
- The four **multi-*** terms: multiprogramming / multitasking / multiprocessing / multithreading.