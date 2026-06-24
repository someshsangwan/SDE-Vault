# Chapter 5 — IPC, File Systems, I/O & Interview Special

> Final chapter. Covers: all IPC mechanisms (pipes, message queues, shared memory,
> signals, sockets), file systems (inodes, file descriptors, allocation methods),
> disk scheduling, and the complete top-44 interview Q&A across all 5 chapters.

---

## PART A — Inter-Process Communication (IPC)

Processes have **separate memory** (unlike threads). They cannot read each other's variables.
IPC = how processes communicate and share data.

> **Backend connection:** Every microservice architecture IS IPC. REST APIs, message queues,
> Redis pub/sub — all IPC mechanisms extended across a network.

---

### 1. Pipes

A **unidirectional channel** — data flows one way. One process writes, another reads.

```
Process A  ──write──►  [ PIPE BUFFER ]  ──read──►  Process B
```

**Anonymous pipes** — between related (parent-child) processes only:
```bash
ls | grep java       # ls and grep are two processes connected by a pipe
ps aux | sort | head # three processes, two pipes in a chain
```

**Named pipes (FIFOs)** — any two processes, identified by a filename:
```bash
mkfifo mypipe           # create a named pipe
echo "hello" > mypipe   # process A writes
cat mypipe              # process B reads
```

- ✅ Simple, built into every OS.
- ❌ Unidirectional (two-way needs two pipes).
- ❌ Same machine only.

> **Backend:** The `|` pipe in your terminal IS this. You use it dozens of times a day.

---

### 2. Message Queues

A **linked list of messages stored in the kernel**. Sender posts messages, receiver reads them.
**Asynchronous** — sender doesn't wait for receiver.

```
Process A  ──post──►  [ msg1 | msg2 | msg3 ]  ──read──►  Process B
                          kernel message queue
```

- ✅ Asynchronous — sender and receiver don't need to be running simultaneously.
- ✅ Messages have types/priorities — receiver can pick specific types.
- ✅ Works between unrelated processes.
- ❌ Same machine only (kernel-managed).

> **Backend:** Kafka, RabbitMQ, SQS are this concept extended across a network.
> OS-level message queue = local version of what Kafka does across machines.

---

### 3. Shared Memory ★ Fastest IPC

Two processes **map the same physical RAM region** into their own virtual address spaces.
They read/write it directly — no kernel involvement after initial setup.

```
Process A's            Shared               Process B's
virtual space:         Physical RAM:        virtual space:
┌──────────┐           ┌──────────┐         ┌──────────┐
│ ...      │           │          │         │ ...      │
│ shared   │◄─────────►│  SHARED  │◄───────►│ shared   │
│ region   │           │  MEMORY  │         │ region   │
│ ...      │           │          │         │ ...      │
└──────────┘           └──────────┘         └──────────┘
```

- ✅ **Fastest IPC** — direct memory access, no kernel in the data path.
- ❌ **Requires synchronization** — two processes writing simultaneously = race condition
  (need mutex/semaphore from Chapter 3!).
- ❌ Same machine only.

> **Backend:** Used in high-performance systems (trading platforms) where microseconds
> matter. Redis on the same machine can use shared memory for ultra-fast data sharing.

---

### 4. Signals

A **software interrupt** sent to a process — async notification that an event occurred.
Can arrive at any time, interrupts whatever the process is doing.

| Signal | Number | Meaning | Default action |
|---|---|---|---|
| **SIGTERM** | 15 | Politely ask process to terminate | Terminate |
| **SIGKILL** | 9 | Force kill — **cannot be caught or ignored!** | Terminate immediately |
| **SIGINT** | 2 | Interrupt (Ctrl+C) | Terminate |
| **SIGSEGV** | 11 | Segmentation fault (illegal memory access) | Crash + core dump |
| **SIGHUP** | 1 | Hangup / reload config | Terminate |

```bash
kill -15 <PID>    # SIGTERM — politely ask to stop (process can clean up first)
kill -9  <PID>    # SIGKILL — force kill, no cleanup, cannot be ignored
kill -1  <PID>    # SIGHUP  — many servers use this to reload config without restart
```

> **Backend gold — Kubernetes pod shutdown:**
> 1. K8s sends **SIGTERM** (15) → app should finish in-flight requests, close DB
     >    connections, release resources = **graceful shutdown**.
> 2. If app doesn't stop within grace period → K8s sends **SIGKILL** (9) → instant kill.
     > This is why you implement shutdown hooks in your services!

```java
// Java graceful shutdown hook — handles SIGTERM
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    server.stop();      // stop accepting new requests
    dbPool.close();     // close DB connections cleanly
}));
```

---

### 5. Sockets

An **endpoint for communication** — between processes on the same machine OR different
machines over a network. The ONLY IPC mechanism that works cross-machine.

**Unix Domain Sockets** — same machine only, faster (no network stack overhead):
```bash
# PostgreSQL listens on /var/run/postgresql/.s.PGSQL.5432
# connecting locally uses this socket file, not TCP
```

**Network Sockets (TCP/UDP)** — cross-machine:
```
Process A  ──► [socket] ──TCP/IP──► [socket] ──► Process B
              port 8080               port 5432
              (your app)              (DB server)
```

- ✅ **Works across machines** — only IPC that does.
- ✅ Foundation of ALL network communication (HTTP, gRPC, WebSockets all built on sockets).
- ❌ Slower than shared memory (goes through OS network stack and buffers).

> **Backend:** Every HTTP request, every DB query, every microservice call — all sockets.
> `new Socket("db-host", 5432)` or `RestTemplate.getForObject(url)` creates a socket.

---

### IPC Comparison:

| Mechanism | Speed | Cross-machine? | Best use case |
|---|---|---|---|
| **Pipe** | Fast | No | Shell pipelines, parent-child |
| **Message Queue** | Medium | No (OS-level) | Async local communication |
| **Shared Memory** | **Fastest** | No | High-performance, same machine |
| **Signals** | Fast | No | Notifications, process control |
| **Sockets** | Medium | **Yes ✅** | Everything networked |

---

## PART B — File Systems

### What is a File System?
The OS organizes data on disk via a **file system** — an abstraction for storing, naming,
and organizing data. It manages files, directories, and metadata.

---

### Inodes (★ important)

Every file is represented on disk by an **inode** (index node).
An inode contains **all metadata about a file — except its name**.

```
Inode #4821:
┌─────────────────────────────────┐
│ File type:   regular file       │
│ Owner:       somesh.sangwan     │
│ Permissions: rw-r--r--          │
│ Size:        4096 bytes         │
│ Created:     2026-06-01         │
│ Modified:    2026-06-23         │
│ Link count:  1                  │
│ Data block pointers:            │
│   Block 1 → disk block #10042  │
│   Block 2 → disk block #10043  │
└─────────────────────────────────┘
```

The **filename** lives in the **directory**, which maps: `filename → inode number`

```
Directory /home/somesh/:
┌──────────────────────────┐
│ "app.jar"    → inode 4821│
│ "config.yml" → inode 4822│
│ "logs/"      → inode 4823│
└──────────────────────────┘
```

**Opening `/home/somesh/app.jar`:**
1. OS resolves path → finds directory entry `app.jar` → gets inode 4821.
2. Reads inode 4821 → finds data block locations on disk.
3. Reads those blocks → file data.

**Hard link vs Soft link:**
- **Hard link** — two filenames pointing to the **same inode**. Deleting one name doesn't
  delete data (inode link count just decrements). Same file, two names.
- **Soft link (symlink)** — a file containing the **path** to another file. Like a shortcut.
  Deleting the original breaks the symlink.

```bash
ls -i app.jar           # shows inode number
stat app.jar            # shows full inode info (size, permissions, timestamps, blocks)
df -i                   # shows inode usage (you CAN run out of inodes!)
ln app.jar hardlink     # create a hard link
ln -s app.jar symlink   # create a soft/symbolic link
```

---

### File Descriptors

When a program opens a file/socket/pipe, the OS returns a **file descriptor (fd)** —
a small integer representing the open resource.

```
fd 0 → stdin   (keyboard)    ← always reserved
fd 1 → stdout  (terminal)    ← always reserved
fd 2 → stderr  (errors)      ← always reserved
fd 3 → your opened file      ← first available
fd 4 → your DB socket
fd 5 → another open file
...
```

Every process has its own **fd table** (stored in PCB from Chapter 1).

```bash
lsof -p <PID>    # list all open file descriptors for a process
lsof -p $$       # your current shell's open files
ulimit -n        # max open file descriptors allowed per process
```

> **Backend:** "Too many open files" (`EMFILE`) = process hit the fd limit.
> Each open socket/file/connection = one fd. Always close connections when done.
> High-performance servers tune `ulimit -n` to allow thousands of concurrent connections.

---

### File Allocation Methods

How the file system stores file data blocks on disk:

**1. Contiguous Allocation** — file occupies consecutive disk blocks.
- ✅ Fast sequential read.
- ❌ External fragmentation, hard to grow.

**2. Linked Allocation** — each block has a pointer to the next block (like a linked list).
- ✅ No external fragmentation, grows easily.
- ❌ Random access is slow (must follow chain from start).
- ❌ One broken pointer = entire file lost.

**3. Indexed Allocation** — a special **index block** holds pointers to all data blocks.
- ✅ Fast random access.
- ✅ No external fragmentation.
- ❌ Index block overhead for small files.
- Unix **inodes use this** — the inode IS the index block!

---

## PART C — Disk & I/O Scheduling

Disk read/write requests arrive randomly. On HDDs the physical disk head must move to
each track — **disk scheduling** decides the order to minimize head movement.

> Note: SSDs have no physical head, so this matters less for them, but concepts appear
> in interviews.

### FCFS — First Come First Served
Serve in arrival order. Simple but head moves randomly — high seek time.

### SSTF — Shortest Seek Time First
Serve the request **closest to current head position** first.
- ✅ Reduces total head movement.
- ❌ **Starvation** — far requests may never be served (same concept as CPU scheduling!).

### SCAN — Elevator Algorithm ★
Head moves in one direction serving all requests in that direction. Hits the end,
reverses. Like an elevator.
```
Head at 50, moving right →: serves 60, 75, 90, 100 (end)
Reverses ←: serves 40, 20, 5
```
- ✅ No starvation, good throughput.
- ❌ Requests at far ends wait longer.

### C-SCAN — Circular SCAN
Like SCAN but only serves requests in **one direction**. Jumps back to start without
serving on the return trip → more uniform wait times.

### Comparison:

| Algorithm | Starvation? | Notes |
|---|---|---|
| **FCFS** | No | Simple, inefficient head movement |
| **SSTF** | Yes | Greedy — fast but unfair |
| **SCAN** | No | Elevator — balanced |
| **C-SCAN** | No | More uniform than SCAN |

---

## PART D — ★★★ Top 44 Interview Q&A

Rapid-fire answers for every topic across all 5 chapters.

---

### OS Basics

**Q1. What is an OS? What are its two main jobs?**
Resource manager (allocates CPU, RAM, I/O between programs) + abstraction provider
(hides hardware complexity — gives clean interfaces like files, sockets, memory).

**Q2. What is the difference between kernel mode and user mode?**
Kernel mode = full hardware access, runs OS code. User mode = restricted, runs app code.
System calls are the doorway between them. Separation protects OS from buggy apps.

**Q3. What is a system call? Give examples.**
A request from user-space to the kernel to perform a privileged operation.
Examples: `read()`, `write()`, `fork()`, `exec()`, `socket()`, `mmap()`.
Every I/O operation (file, network, DB) involves system calls — this is why I/O is slow.

**Q4. What is the difference between multiprogramming, multitasking, multiprocessing,
and multithreading?**
- Multiprogramming = multiple programs in RAM, switch on I/O block (keep CPU busy, 1 CPU).
- Multitasking = time-slicing between programs on 1 CPU (responsiveness, interactive).
- Multiprocessing = multiple CPUs/cores, true parallelism.
- Multithreading = multiple threads inside one process, sharing memory.

---

### Processes & Threads

**Q5. What is the difference between a process and a program?**
Program = passive file on disk. Process = program in execution with allocated resources
(CPU time, memory, open files). One program can become many processes.

**Q6. What are the states of a process?**
New → Ready → Running → Waiting/Blocked → Terminated.
Key transitions: Running→Waiting (I/O request, voluntarily), Waiting→Ready (I/O done),
Running→Ready (preemption — time slice expired).

**Q7. What is a PCB?**
Process Control Block — OS's "save file" for a process. Stores PID, state, program
counter (bookmark!), registers, memory pointers, open files, priority. Used to save/restore
process state during context switches.

**Q8. What is context switching? Why is it expensive?**
Saving current thread's state (into PCB) and loading another thread's state.
Expensive because: CPU does bookkeeping not real work, and CPU cache is invalidated
(new process's data not cached → cache misses).

**Q9. What is the difference between a process and a thread?**
Process = independent memory space, heavy, isolated, crash doesn't affect others.
Thread = lightweight execution inside a process, shares heap/code/data, has own
stack/registers/PC. Thread creation faster, context switch cheaper, but one crash
can kill all sibling threads.

**Q10. What do threads share vs what is private?**
Shared: heap (objects!), code, data (global/static vars), open file descriptors.
Private: stack (local variables), registers, program counter, thread ID.

**Q11. Concurrency vs Parallelism?**
Concurrency = multiple tasks making progress by rapidly switching on 1 core (illusion).
Parallelism = tasks truly running simultaneously onmultiple cores (real).

**Q12. What are zombie and orphan processes?**
Zombie = finished but parent hasn't called wait() → occupies process-table slot, wastes
resources, STAT shows 'Z'. Orphan = parent died, child still running → harmlessly adopted
by PID 1 (launchd/init).

**Q13. What does fork() return?**
Called once, returns twice: child's PID to the parent, 0 to the child, -1 on failure.

**Q14. Does the CPU schedule processes or threads?**
Threads. Process is a container — its threads are the actual schedulable units.
Modern OS schedulers maintain a ready queue of kernel threads. Seen live: your machine
had 4955 threads across 904 processes. The java process alone had 221 threads = 221
independent scheduler entries.

---

### CPU Scheduling

**Q15. What is CPU scheduling?**
Deciding which ready thread gets the CPU next. Needed because many threads are ready
simultaneously but CPU can only run one per core at a time.

**Q16. Preemptive vs non-preemptive scheduling?**
Non-preemptive: process keeps CPU until it finishes or blocks.
Preemptive: OS can forcibly take CPU away (time slice, higher priority arrival).
Modern OSes are preemptive — the Running→Ready state transition only exists here.

**Q17. What is the convoy effect?**
In FCFS, one long process blocks all short ones behind it. Like a slow truck blocking
all cars on a single-lane road. Fix: SJF or Round Robin.

**Q18. Why is Round Robin good for interactive systems?**
Everyone gets a turn within one quantum → good response time, no starvation.
Quantum too large = FCFS. Quantum too small = too much context-switching overhead.

**Q19. What is starvation? How do you fix it?**
A process waits indefinitely because higher-priority ones keep arriving.
Fix: aging — gradually increase waiting process's priority over time until it runs.

**Q20. What scheduling algorithm do real OSes use?**
Multilevel Feedback Queue (MLFQ). Processes move between queues based on behavior —
CPU-heavy gets demoted, interactive gets promoted. Adapts dynamically, no burst time needed.

---

### Synchronization

**Q21. What is a race condition?**
Two or more threads access shared data concurrently and the result depends on the
execution order → unpredictable, incorrect behavior.

**Q22. What is a critical section?**
Code that accesses shared data. Must be protected: only one thread at a time.
Three requirements: mutual exclusion, progress, bounded waiting.

**Q23. Mutex vs Semaphore?**
Mutex: binary lock WITH ownership — only acquirer can release. Protects critical section.
Semaphore: integer counter WITHOUT ownership — any thread can signal. Binary semaphore ≈
mutex but no ownership. Counting semaphore = N resources (like connection pool = N connections).

**Q24. What is the Producer-Consumer problem?**
Producer adds to bounded buffer, consumer removes. Problems: wait if full (producer),
wait if empty (consumer), mutual exclusion on buffer. Solved with 3 semaphores: mutex
(protect buffer), empty (free slots), full (filled slots).

**Q25. What is the Dining Philosophers problem?**
5 philosophers, 5 chopsticks, need both adjacent ones to eat. All grab left → circular
wait → deadlock. Models: circular resource dependency = DB transaction deadlocks.

---

### Deadlocks

**Q26. What is a deadlock?**
Two or more threads permanently stuck — each holds a resource and waits for one held by
another. No progress. No release. Forever.

**Q27. What are the 4 Coffman conditions?**
ALL must hold simultaneously:
1. **Mutual Exclusion** — resource can't be shared.
2. **Hold and Wait** — holding one resource, waiting for another.
3. **No Preemption** — resources can't be forcibly taken.
4. **Circular Wait** — circular chain of waiting threads.
   Break even ONE → deadlock impossible.
   Memory trick: **"My Hungry Neighbor Cooks"** (Mutual exclusion, Hold & wait, No preemption, Circular wait).

**Q28. Real backend example of deadlock?**
User A sends money to User B. User B sends money to User A. At the same time.
Business logic: lock sender row → lock receiver row.
- T1 (A→B): holds A's row lock, waiting for B's row lock.
- T2 (B→A): holds B's row lock, waiting for A's row lock.
  Circular wait → deadlock. DB detects and kills one transaction:
  `ERROR 1213: Deadlock found when trying to get lock; try restarting transaction`.

**Q29. How do you prevent this deadlock in code?**
Break circular wait: always lock rows in a consistent order (lower user ID first).
```java
User first  = sender.id < receiver.id ? sender : receiver;
User second = sender.id < receiver.id ? receiver : sender;
lock(first.balanceRow);   // always lower ID first
lock(second.balanceRow);
```
Both T1 and T2 now try to lock A's row first → one waits → no circular wait → no deadlock.

**Q30. What is the Banker's Algorithm?**
Deadlock avoidance: before granting a request, check if a safe sequence exists where all
threads can finish. If unsafe → deny the request. Mostly theoretical — needs max resource
needs in advance, fixed threads/resources.

**Q31. 4 strategies for handling deadlocks?**
1. Prevention — break a Coffman condition by design (lock ordering, most practical).
2. Avoidance — Banker's algorithm, stay in safe state (theoretical).
3. Detection + Recovery — detect cycles in RAG, kill victim transaction (what DBs do).
4. Ignorance (Ostrich) — do nothing, restart if it happens (what most OSes do).

**Q32. Deadlock vs Starvation?**
Deadlock: all involved threads stuck in a circle, nobody moves at all.
Starvation: one thread waits indefinitely while others progress fine — unfair scheduling.

---

### Memory Management

**Q33. Internal vs external fragmentation?**
External: free memory exists in total but scattered — no single contiguous block large
enough. Fix: paging.
Internal: allocated block bigger than requested — wasted space inside. Fix: smaller blocks.

**Q34. What is paging? How does address translation work?**
RAM split into fixed-size frames, process memory split into same-size pages, mapped via
page table. Logical address = page number + offset → look up page table → frame number →
physical address = (frame × page size) + offset. Eliminates external fragmentation.

**Q35. What is a TLB?**
CPU cache for page table entries. TLB hit = fast (no extra RAM access). TLB miss = slow
(access page table in RAM). 90-99% hit rate due to locality of reference. Makes paging
practical despite the two-memory-access overhead.

**Q36. What is virtual memory?**
OS illusion that each process has a large private address space, larger than physical RAM.
Only active pages in RAM; rest on disk. Allows running more/larger processes than RAM fits.
Implemented via demand paging. Seen live: IntelliJ claimed 403GB virtual, had 3.6GB in RAM.

**Q37. What is a page fault?**
Process accesses a page not in RAM. MMU raises interrupt → OS finds page on disk → loads
into a free frame → updates page table → restarts instruction. Expensive: disk ~100,000×
slower than RAM.

**Q38. What is LRU? Why is it preferred?**
Least Recently Used — evict the page not accessed for the longest time. Preferred because:
approximates optimal, no Belady's anomaly, same principle as Redis/cache eviction you use.

**Q39. What is Belady's Anomaly?**
With FIFO page replacement, adding more frames can increase page faults. Counter-intuitive.
LRU and Optimal are immune. Interview: "Belady's anomaly affects FIFO, not LRU."

**Q40. What is thrashing?**
System spends more time swapping pages than doing real work → CPU utilization collapses.
Cause: too many processes, each gets fewer frames than its working set needs.
Fix: reduce multiprogramming, working set model, add RAM.

---

### IPC & File Systems

**Q41. What are the main IPC mechanisms?**
Pipes (unidirectional, shell pipelines), Message Queues (async, kernel-managed),
Shared Memory (fastest, needs sync), Signals (async notifications like SIGTERM/SIGKILL),
Sockets (cross-machine, foundation of all networking).

**Q42. What is an inode?**
Data structure storing all file metadata except the filename (owner, permissions, size,
timestamps, data block pointers). Filenames live in directories mapping name → inode number.
Hard link = two names, same inode. Soft link = file containing path to another file.

**Q43. What is a file descriptor?**
Integer the OS returns when you open a file/socket/pipe. 0=stdin, 1=stdout, 2=stderr,
3+=yours. Each process has its own fd table. "Too many open files" = hit the fd limit.
Every open connection = one fd. Always close when done.

**Q44. What happens when Kubernetes sends SIGTERM to your pod?**
SIGTERM (15) = graceful shutdown request. App finishes in-flight requests, closes DB
connections, releases resources. If not stopped within grace period → SIGKILL (9) = instant
kill, no cleanup. Implement a shutdown hook to handle SIGTERM properly.

---

## 🖥️ HANDS-ON: IPC & File System Commands

```bash
# Pipes
ls | grep java                     # anonymous pipe between ls and grep
mkfifo /tmp/mypipe                 # create named pipe
echo "hi" > /tmp/mypipe &; cat /tmp/mypipe  # write/read named pipe

# Signals
kill -15 <PID>                     # SIGTERM — graceful stop
kill -9  <PID>                     # SIGKILL — force kill
kill -1  <PID>                     # SIGHUP — reload config
kill -l                            # list all signals

# File system
ls -i                              # show inode numbers
stat filename                      # full inode info (size, permissions, blocks, times)
df -i                              # inode usage per filesystem
ln file hardlink                   # create hard link (same inode)
ln -s file symlink                 # create soft/symbolic link
lsof -p <PID>                      # all open file descriptors for a process
lsof -p $$                         # your shell's open files
ulimit -n                          # max open fds allowed per process

# Sockets
lsof -i :8080                      # what process is listening on port 8080
netstat -an | grep LISTEN          # all listening sockets
ss -tlnp                           # (Linux) listening TCP sockets with process names
```

---

## ✅ Chapter 5 Summary (quick revision)

**IPC mechanisms:**
- **Pipe** — unidirectional, anonymous (parent-child) or named (any process), same machine.
- **Message Queue** — async, typed messages, kernel-managed, same machine.
- **Shared Memory** — fastest IPC, direct RAM access, needs synchronization, same machine.
- **Signals** — async software interrupts. SIGTERM=graceful, SIGKILL=force (can't catch).
- **Sockets** — only cross-machine IPC. Foundation of all networking (HTTP, DB, gRPC).

**File Systems:**
- **Inode** = file metadata (not name). Directory maps filename → inode. Hard link = same
  inode, two names. Soft link = path pointer.
- **File descriptor** = integer handle for open file/socket/pipe. 0/1/2 reserved.
  "Too many open files" = hit fd limit.
- **Allocation methods**: Contiguous (fast, fragmentation), Linked (flexible, slow random),
  Indexed (fast random, inode IS the index block).

**Disk Scheduling:**
- FCFS (simple), SSTF (greedy, starvation), SCAN/elevator (balanced), C-SCAN (uniform).

**Interview Q&A:** 44 questions covering all 5 chapters — study these before every interview.