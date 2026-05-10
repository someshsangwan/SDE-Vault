# Process vs Thread

> **Status:** Learning · **Started:** 2026-05-10
> **Why this matters:** every Spring Boot app is one **process** with hundreds of **threads**. Understanding the boundary is the foundation for concurrency, deadlocks, scaling, and reasoning about prod incidents.

---

## 1. TL;DR

| | Process | Thread |
|---|---|---|
| **What it is** | An instance of a running program | A unit of execution **inside** a process |
| **Memory** | Own address space (code, data, heap, stack) | **Shares** code, data, heap with siblings; has **own stack + registers + program counter** |
| **Created by** | OS (`fork()`, `exec()`, JVM start) | OS / runtime (e.g., `new Thread()`) |
| **Communication** | IPC (pipes, sockets, shared memory, signals) | Just read/write shared memory (but **needs synchronization**) |
| **Switching cost** | Expensive (MMU/page table swap, TLB flush) | Cheap (registers + stack pointer) |
| **Failure isolation** | One process crash ≠ another crashes | One thread's uncaught exception can crash the whole process |
| **Java mapping** | One JVM = one OS process | Each `Thread` object → one OS thread (HotSpot, since Java 1.3) |

---

## 2. What is a Process?

A **process** is an abstraction the OS gives to a running program. When you start a Spring Boot app:

```bash
java -jar payment-service.jar
```

…the OS creates a new process. That process gets:

1. **Its own virtual address space** — a private 64-bit address range. Its memory is isolated from other processes; the OS uses the **MMU (Memory Management Unit)** and **page tables** to translate virtual → physical addresses.
2. **A process ID (PID)** — unique integer assigned by the kernel.
3. **Resources**: open file descriptors, network sockets, environment variables.
4. **At least one thread** — the "main thread". A process always has ≥ 1 thread; threads exist *inside* a process.

### Process memory layout (Linux/macOS)

```
High addresses
┌──────────────────┐
│      Stack       │  ← grows downward; per-thread function call frames
│        ↓         │
│        ↑         │
│       Heap       │  ← grows upward; malloc / new go here
├──────────────────┤
│       BSS        │  ← uninitialized globals
├──────────────────┤
│      Data        │  ← initialized globals
├──────────────────┤
│    Text (Code)   │  ← machine instructions, read-only
└──────────────────┘
Low addresses
```

In Java terms:
- **Text** = JVM bytecode + JIT-compiled native code
- **Heap** = JVM heap (where all `new MyClass()` objects live)
- **Stack** = each Java thread has its own stack with method frames + local variables

### Inspecting processes

```bash
ps -ef | grep java        # list processes (no thread detail)
ps -eLf | grep java       # list processes + their threads (note the L flag)
top -pid <PID>            # live view
lsof -p <PID>             # files & sockets opened by process
```

---

## 3. What is a Thread?

A **thread** is a single sequence of execution within a process. Multiple threads in one process share most things — they only have private:

- **Program counter (PC)** — where in the code they currently are
- **Registers** — CPU register state at this instant
- **Stack** — their own function call stack (so two threads can call the same method without their local variables colliding)

Everything else — heap, code, file descriptors, env — is **shared**.

```
        ┌──────────── Process ────────────┐
        │  Heap  │  Code  │  Globals │ FDs │   ← shared
        ├────────┼────────┼──────────┼─────┤
        │ Stack1 │ Stack2 │  Stack3  │ ... │   ← per-thread
        │  PC1   │  PC2   │   PC3    │     │
        │  Regs1 │  Regs2 │   Regs3  │     │
        └────────┴────────┴──────────┴─────┘
          Thread A  Thread B  Thread C
```

### Why threads exist (the value proposition)

1. **Concurrency on multi-core CPUs** — true parallelism. 8-core CPU → up to 8 threads running simultaneously.
2. **Hide I/O latency** — while one thread waits on a DB query, another handles a different request.
3. **Cheap to create** — typically ~1 ms vs ~10–50 ms for a process.
4. **Cheap to switch** — context switch ≈ 1–5 µs vs 10–100 µs for processes (no MMU/page-table swap).

### The cost: shared memory means race conditions

Because threads share heap, **two threads writing to the same `HashMap` simultaneously can corrupt it** (causing infinite loops in older JDKs). This is the entire reason synchronization (locks, `synchronized`, `ConcurrentHashMap`, `volatile`, `AtomicInteger`) exists.

---

## 4. The Java/JVM Perspective

This is where it gets concrete for our work.

### Each JVM = exactly one OS process

`java -jar app.jar` → one process. If you scale by running 4 JVMs on one box, that's 4 processes.

### Java threads are OS threads (mostly)

In HotSpot JVM (the default), since Java 1.3, the mapping is **1:1 platform thread to OS thread**. So:

```java
Thread t = new Thread(() -> System.out.println("hi"));
t.start();
```

…actually creates a real OS thread (`pthread_create` on Linux/macOS). This is why creating millions of threads is *not* free — each takes ~1 MB of stack space and consumes a kernel slot.

### Project Loom: Virtual Threads (Java 21+)

Java 21 (LTS) introduced **virtual threads** — millions of cheap threads multiplexed onto a small pool of platform (OS) threads by the JVM scheduler.

```java
Thread.startVirtualThread(() -> { /* runs on a virtual thread */ });
// or
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(myTask);
}
```

**Why this matters for backend engineers:** the old "thread pool of 200" pattern (where you sized the pool to your peak parallel I/O) is being replaced by "one virtual thread per request, blocking I/O is fine." Spring Boot 3.2+ supports virtual threads via `spring.threads.virtual.enabled=true`.

### Inspecting JVM threads

```bash
jps                              # list JVMs by PID
jstack <PID>                     # full thread dump (every thread, its stack, its state)
jcmd <PID> Thread.print          # equivalent
```

Thread states you'll see in a dump:
- `RUNNABLE` — actively running or ready to run
- `BLOCKED` — waiting on a `synchronized` lock held by another thread
- `WAITING` / `TIMED_WAITING` — `wait()`, `Object.wait()`, `LockSupport.park()`, `Thread.sleep()`
- `TERMINATED` — done

---

## 5. The Tomcat / Spring Boot Threading Model (Critical for Rakuten Pay)

When a request arrives at a Spring Boot app, here's what happens thread-wise:

```
HTTP request
    ↓
[Acceptor thread] (1 thread, accepts new connections)
    ↓
[Worker thread pool] (default: 200 threads in Tomcat)
    ↓
DispatcherServlet → @RestController.method()
    ↓
DB call (blocks worker thread until response)
    ↓
HTTP response
    ↓
Worker thread returns to pool
```

Key points:

1. **One thread per request** (in classic blocking model) — if 201 requests arrive simultaneously and the pool has 200 threads, the 201st waits in a queue.
2. **Pool size tuning matters**: `server.tomcat.threads.max=400` in `application.properties`. Too small → request queueing → high p99 latency. Too large → memory pressure (each thread = ~1 MB stack), CPU thrashing.
3. **Blocking I/O is the bottleneck**: a thread waiting on a DB query is doing nothing useful but holding 1 MB of stack.
4. **`@Async` methods** run on a separate thread pool (`TaskExecutor`) — useful for fire-and-forget like sending emails.
5. **Reactive (Spring WebFlux)** uses a much smaller event-loop thread pool — but requires non-blocking code throughout, including the DB driver (R2DBC).
6. **Virtual threads (Java 21 + Boot 3.2)** sidestep all of this — let the JVM multiplex blocking calls cheaply.

### Real Rakuten Pay scenario

> A payment request comes in. Worker thread picks it up, calls the fraud service (200ms), then the user balance service (50ms), then the card network (800ms), then writes to DB (30ms), then responds. Total: 1.08 seconds where the thread is **mostly idle**, holding 1 MB of stack and one of 200 pool slots.

This is why payment platforms care about either (a) async/reactive code or (b) virtual threads — to stop wasting platform threads on idle waits.

---

## 6. IPC vs Shared Memory — How They Communicate

### Threads (in same process): shared memory

```java
class Counter {
    private int count = 0;  // on the heap → shared
    public synchronized void increment() { count++; }
}
```

Both threads see the same `count`. The `synchronized` keyword (or `AtomicInteger`, `LongAdder`, etc.) prevents races.

### Processes: must use IPC

Two separate processes can't see each other's memory. They communicate via:

| IPC mechanism | Use case | Java API |
|---|---|---|
| **Pipes** | Parent-child stream | `ProcessBuilder` |
| **TCP/HTTP sockets** | Cross-machine + same-machine | `java.net.Socket`, RestTemplate, OkHttp |
| **Unix domain sockets** | Same-machine, fast | NIO `UnixDomainSocketAddress` |
| **Shared memory** | Fastest, same-machine | `MappedByteBuffer` (memory-mapped files) |
| **Message queues** | Decoupled async | Kafka, RabbitMQ |
| **Signals** | Lifecycle (kill, reload) | `kill -HUP <pid>` |

In microservices: **service A and service B are two processes**. They communicate via HTTP/gRPC/Kafka — that's all just IPC over the network.

---

## 7. Context Switch Cost — Why It Matters

A **context switch** = the OS pauses one execution unit and resumes another.

- **Thread → thread (same process)**: save/restore registers + stack pointer. ~1–5 µs.
- **Process → process**: above PLUS swap page tables, flush TLB, possibly swap caches. ~10–100 µs.

A server doing 100k context switches per second can spend significant CPU just on switching. That's why:

- Excessive thread counts hurt throughput (more switches than work).
- Reactive/event-loop frameworks (Netty, Vert.x) minimize switches by handling many connections per thread.
- `pidstat -w 1` shows switch rate — useful diagnostic.

---

## 8. Failure Isolation

| Scenario | Effect |
|---|---|
| Thread T uncaught `RuntimeException` | T dies. Other threads in same JVM **continue**. The pool may replace T. |
| Thread T calls `System.exit(0)` or hits OOM | **Entire JVM dies.** All threads gone. |
| Process P crashes (segfault, kill -9) | Just P. Other processes (other microservices) **unaffected**. |

This is the strongest argument for microservices over monolith: **process boundaries provide failure isolation**. A bug in the recommendation service can't take down the payment service if they're separate processes.

---

## 9. Self-Check Questions

Try to answer in your own words. If you can't, re-read the section.

1. Two threads in the same JVM both call `myMap.put(k, v)`. What can go wrong, and why specifically?
2. Why is creating a thread cheaper than creating a process? Name the specific resource that doesn't have to be allocated.
3. You see `BLOCKED` in a thread dump. What is that thread waiting for, and what is it *not* waiting for (vs `WAITING`)?
4. A Tomcat pool has 200 threads. A traffic spike sends 500 simultaneous requests, each doing a 1-second DB call. What happens to requests 201–500? What metric would you look at to confirm?
5. What's the difference between `ps -ef` and `ps -eLf`?
6. Why does `synchronized` exist if threads can just share memory?
7. If you wanted two Spring Boot services to share data, would you put them in the same JVM or different JVMs? Argue both sides.
8. What's the value of virtual threads (Loom) if Java threads are already cheap to create compared to processes?

---

## 10. Common Interview Traps

- **"How many threads can a Java app have?"** — depends on stack size and OS limits. ~10k platform threads is realistic before memory pressure. Virtual threads: millions.
- **"Process vs Thread — which is better?"** — wrong question. They solve different problems. Threads for in-process concurrency; processes for isolation.
- **"Are Java threads OS threads?"** — yes, since Java 1.3 (HotSpot). Green threads (M:N user-space) are **gone** in mainstream JVMs but coming back via Loom virtual threads.
- **"What's a fiber/coroutine?"** — user-space threads scheduled by the runtime, not the OS. Loom virtual threads are this.

---

## 11. Linked Notes

- [[concurrency-and-locks]] — `synchronized`, `ReentrantLock`, `volatile` (next OS topic)
- [[deadlocks]] — what happens when threads wait on each other forever
- [[memory-management]] — virtual memory, paging (gives more depth on why process switch is expensive)
- [[java-thread-pools]] — `ExecutorService`, `ForkJoinPool`, virtual thread executors
- [[spring-boot-threading]] — Tomcat tuning, `@Async`, WebFlux, Loom integration

---

## 12. Revision Schedule

- [ ] Day +1: re-explain process vs thread to yourself in 2 minutes (no notes)
- [ ] Day +3: hand-draw the memory layout diagram and the Tomcat threading model
- [ ] Day +7: answer all 8 self-check questions written down
- [ ] Day +30: be able to debate "why microservices?" using the failure-isolation argument