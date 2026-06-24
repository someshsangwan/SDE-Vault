# Chapter 3 — Synchronization & Deadlocks

> Interview-focused notes. Covers: race conditions, critical section, mutex, semaphore,
> mutex vs semaphore, classic problems (producer-consumer, reader-writer, dining philosophers),
> deadlocks (4 Coffman conditions, prevention, avoidance, detection & recovery),
> deadlock vs starvation, and real production debugging tools.

---

## PART A — The Problem: Why Do We Need Synchronization?

Threads **share the heap** (from Chapter 1). Two threads can read and write the
**same variable/object at the same time**. This is powerful but dangerous.

### The Bank Account Race Condition

```
balance = 1000

Thread 1 (deposit +500):        Thread 2 (ATM withdraw -200):
  read balance  → 1000            read balance  → 1000
  add 500       → 1500            subtract 200  → 800
  write balance = 1500            write balance = 800   ← overwrites Thread 1!
```

**Expected:** 1300 (1000 + 500 - 200)
**Actual:** 800 or 1500 depending on timing → **money appears or disappears!**

This is a **Race Condition** — result depends on the timing/order of thread execution,
which the OS controls, not you.

> **Backend connection:** This is exactly why you've seen `synchronized`, `ReentrantLock`,
> `AtomicInteger`, `ConcurrentHashMap` in Java. Every one of those exists to solve race
> conditions on shared data.

---

## PART B — Race Condition & Critical Section

### Race Condition
When **two or more threads** access shared data **concurrently** and the result depends
on the **order of execution** → unpredictable, incorrect behavior.

### Critical Section
The piece of code that **accesses shared data** — the dangerous zone.
Only **one thread should be inside the critical section at a time**.

```
┌─────────────────────────────────┐
│         Thread's code           │
│   int x = localVar;  // safe    │
│                                 │
│ ┌─────────────────────────────┐ │
│ │      CRITICAL SECTION       │ │  ← only ONE thread allowed here at a time
│ │  balance = balance + 500;   │ │
│ └─────────────────────────────┘ │
│                                 │
│   System.out.println(x); // safe│
└─────────────────────────────────┘
```

### Three requirements for solving the critical section problem:
1. **Mutual Exclusion** — only one thread inside critical section at a time.
2. **Progress** — if no thread is inside, a waiting thread must eventually get in (no deadlock on entry).
3. **Bounded Waiting** — a waiting thread must eventually get in, can't wait forever (no starvation).

---

## PART C — Mutex (Mutual Exclusion Lock)

A **mutex** is a lock. Before entering the critical section, a thread **acquires** the lock.
When done, it **releases** it. Any other thread trying to acquire a locked mutex →
**blocks** (goes to Waiting state) until the lock is free.

```
Thread 1:  acquire(lock) ──► [critical section] ──► release(lock)
Thread 2:  acquire(lock) ──► BLOCKED ────────────────────────────► [critical section] ──► release(lock)
                                        (unblocked when T1 releases)
```

> Analogy: A **toilet with one key**. One person takes the key, goes in, locks the door.
> Everyone else waits outside. Person comes out, hangs key back — next person grabs it.

### In Java:
```java
// Option 1 — synchronized keyword (built-in mutex)
synchronized(this) {
    balance = balance + 500;  // critical section — only one thread at a time
}

// Option 2 — explicit ReentrantLock
ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    balance = balance + 500;
} finally {
    lock.unlock();  // ALWAYS release in finally — or you'll deadlock forever!
}
```

**Key property:** Only the thread that **acquired** the lock can **release** it. It's owned.

---

## PART D — Semaphore

A semaphore is more general than a mutex. Instead of binary (locked/unlocked), it has
an **integer counter** controlling how many threads can access a resource simultaneously.

Two operations:
- **wait() / P() / down()** → decrement counter. If counter goes negative → block.
- **signal() / V() / up()** → increment counter. If threads waiting → wake one up.

### Binary Semaphore (value = 0 or 1) — behaves like mutex, initial value = 1
```
Initial: S = 1

Thread 1: wait(S)   → S=0  → enters critical section
Thread 2: wait(S)   → S=-1 → BLOCKED
Thread 1: signal(S) → S=0  → Thread 2 wakes up, enters
Thread 2: signal(S) → S=1
```

### Counting Semaphore (value = N) — controls access to a pool of N resources

Example: DB connection pool with 5 connections:
```
Initial: S = 5

Thread 1: wait(S) → S=4 → gets a connection
Thread 2: wait(S) → S=3 → gets a connection
Thread 3: wait(S) → S=2 → gets a connection
Thread 4: wait(S) → S=1 → gets a connection
Thread 5: wait(S) → S=0 → gets a connection
Thread 6: wait(S) → S=-1 → BLOCKED (no connections left, must wait)

Thread 1 done: signal(S) → S=0 → Thread 6 wakes up, gets the released connection
```

> **Backend connection:** A **connection pool** (HikariCP, c3p0) is literally a counting
> semaphore. Max pool size = initial semaphore value. When all connections are in use,
> new requests block — exactly `wait()` blocking when S < 0.

---

## PART E — Mutex vs Semaphore (★ top interview question)

| | **Mutex** | **Semaphore** |
|---|---|---|
| Value | Binary (locked/unlocked) | Integer (0 to N) |
| Purpose | Mutual exclusion (one at a time) | Limit concurrent access to N resources |
| Ownership | Yes — only acquirer can release | No — any thread can signal |
| Use case | Protect a critical section | Control access to a pool of resources |
| Analogy | Toilet with one key | Parking lot with N spaces |

**The ownership difference is crucial:**
- **Mutex** — Thread A locks it, Thread A MUST unlock it. Like your house key.
- **Semaphore** — Thread A does wait(), Thread B can do signal(). Like a traffic light.

> **One-liner:** *"A mutex is for mutual exclusion (one thread at a time, owned by acquirer).
> A semaphore is a signaling mechanism for controlling access to N resources (no ownership)."*

---

## PART F — Classic Synchronization Problems

### 1. Producer-Consumer Problem (Bounded Buffer)

**Setup:** Producer thread creates items → puts in a **shared buffer** (size N).
Consumer thread takes items out.

**Problems:**
- Producer must **wait** if buffer is **full**.
- Consumer must **wait** if buffer is **empty**.
- Both must not access buffer **simultaneously** (race condition).

**Solution using semaphores:**
```
mutex = 1    (binary — protects buffer access)
empty = N    (counting — tracks empty slots)
full  = 0    (counting — tracks filled slots)

PRODUCER:                          CONSUMER:
  wait(empty)  ← wait for space     wait(full)   ← wait for item
  wait(mutex)  ← lock buffer        wait(mutex)  ← lock buffer
  [add item]                        [remove item]
  signal(mutex) ← unlock            signal(mutex) ← unlock
  signal(full)  ← item added        signal(empty) ← space freed
```

> **Backend = this IS your message queue.** Kafka, RabbitMQ, Redis queues are all
> producer-consumer. The "consumer must wait if empty" is why your consumer blocks on `poll()`.

---

### 2. Reader-Writer Problem

**Setup:** Shared resource (e.g., a database).
- Multiple readers can read **simultaneously** (safe).
- A writer needs **exclusive access** — no readers or writers while writing.

**Rules:**
- Readers + Readers ✅ — simultaneously OK.
- Reader + Writer ❌ — must not overlap.
- Writer + Writer ❌ — must not overlap.

**Two variants:**
- **Readers preference** — new readers jump ahead of waiting writers → writers may starve.
- **Writers preference** — once writer is waiting, no new readers → readers may starve.

> **Backend connection:** Java's `ReentrantReadWriteLock` is exactly this.
> Used in caches — reads are frequent, writes are rare, so allow concurrent reads.

---

### 3. Dining Philosophers Problem

**Setup:** 5 philosophers at a round table. 5 chopsticks between them (one between each pair).
To eat, a philosopher needs BOTH left and right chopstick.

```
        P1
    C5      C1
  P5          P2
    C4      C2
        P3
       C3
```

**The problem:** All 5 pick up LEFT chopstick simultaneously →
everyone holds one, everyone waits for the other → **nobody eats → DEADLOCK**.

This is the classic model of **circular resource waiting**.

**Solutions:**
1. Allow max **4 philosophers** to try at once (at least one always gets both).
2. **Odd philosophers pick left first, even pick right first** (breaks symmetry).
3. Pick up **both chopsticks atomically** (treat as one operation with a mutex).

> **Backend connection:** This is exactly a DB deadlock. Transaction A locks Users table,
> waits for Orders. Transaction B locks Orders, waits for Users. Circular wait → deadlock.
> That's why DBs have deadlock detection and kill one transaction (the "victim philosopher").

---

## PART G — Deadlocks

### What is a Deadlock?
Two or more threads **stuck forever**, each waiting for a resource held by another —
none can proceed, none will release what they hold.

### Real Backend Example — Money Transfer Deadlock

This is a **real scenario** in payment systems. Here's the business logic:

```java
// Transfer money: lock sender's balance row, then lock receiver's balance row
void transfer(User sender, User receiver, int amount) {
    lock(sender.balanceRow);    // Step 1: lock sender
    lock(receiver.balanceRow);  // Step 2: lock receiver
    sender.balance -= amount;
    receiver.balance += amount;
    unlock(receiver.balanceRow);
    unlock(sender.balanceRow);
}
```

Now two simultaneous transactions happen:

```
Transaction 1: User A sends money to User B
  Step 1: lock(User A's balance row)  ✅ acquired
  Step 2: lock(User B's balance row)  ⏳ waiting...

Transaction 2: User B sends money to User A  (at the same time!)
  Step 1: lock(User B's balance row)  ✅ acquired
  Step 2: lock(User A's balance row)  ⏳ waiting...
```

```
T1 (A→B):  holds lock on A's row ──────► waiting for B's row ──┐
T2 (B→A):  holds lock on B's row ──────► waiting for A's row ──┘
                    ↑                              ↑
              (held by T2)                   (held by T1)
                         CIRCULAR WAIT → DEADLOCK
```

**Neither transaction can proceed. Both are stuck forever.**

In production, the DB detects this cycle and you see:
```
ERROR 1213 (40001): Deadlock found when trying to get lock; try restarting transaction
```

The DB kills one transaction (the "victim"), rolls it back, lets the other complete.
Your application should catch this error and **retry the transaction**.

---

## PART H — The 4 Coffman Conditions (★★★ most asked)

A deadlock can occur **if and only if ALL four conditions hold simultaneously**.
Break even ONE → deadlock is impossible.

### Condition 1: Mutual Exclusion
At least one resource is **non-shareable** — only one thread can use it at a time.
- In our example: a DB row lock can only be held by one transaction at a time.

### Condition 2: Hold and Wait
A thread **holds at least one resource** AND is **waiting to acquire more**.
- In our example: T1 holds A's row lock AND is waiting for B's row lock.

### Condition 3: No Preemption
Resources **cannot be forcibly taken away** — only voluntarily released.
- In our example: the OS can't rip A's row lock from T1 mid-execution.

### Condition 4: Circular Wait
A **circular chain** exists: each thread waits for a resource held by the next.
- In our example: T1 waits for B's lock (held by T2), T2 waits for A's lock (held by T1).

```
T1 ──waits for──► T2
▲                  │
└──waits for───────┘     circular chain → deadlock
```

> **Memory trick: "My Hungry Neighbor Cooks"**
> **M**utual exclusion → **H**old & wait → **N**o preemption → **C**ircular wait

---

## PART I — Handling Deadlocks (4 Strategies)

```
1. Prevention   → make deadlock IMPOSSIBLE (break a Coffman condition by design)
2. Avoidance    → smart runtime decisions to never enter an unsafe state
3. Detection    → let it happen, detect it, then recover
4. Ignorance    → pretend it doesn't exist (used more than you'd think!)
```

---

### Strategy 1: Prevention — Break a Coffman Condition

| Condition to break | How | Downside |
|---|---|---|
| **Mutual Exclusion** | make resources shareable (read-only data) | not always possible |
| **Hold and Wait** | request ALL resources upfront before starting | wasteful, low utilization |
| **No Preemption** | forcibly take resources back if thread can't proceed | only works for saveable resources |
| **Circular Wait** | impose ordering on resources — always acquire in the same order | **most practical!** |

**Breaking Circular Wait — the most practical and used in production:**

Assign a number to every lock. Threads must **always acquire locks in ascending order**.

```
User A row = lock #1001  (lower ID)
User B row = lock #1002  (higher ID)

Rule: always lock the lower user ID first.

Transaction 1 (A→B): lock(A=1001) then lock(B=1002)  ✅ ascending order
Transaction 2 (B→A): lock(A=1001) then lock(B=1002)  ✅ same order! (NOT B first)
                          ↑
          BOTH transactions now acquire locks in the same order
          → circular wait is impossible → no deadlock
```

**Fixed transfer code:**
```java
void transfer(User sender, User receiver, int amount) {
    // Always lock the lower user ID first — regardless of who is sender/receiver
    User first  = sender.id < receiver.id ? sender : receiver;
    User second = sender.id < receiver.id ? receiver : sender;

    lock(first.balanceRow);   // always lower ID first
    lock(second.balanceRow);  // always higher ID second

    sender.balance -= amount;
    receiver.balance += amount;

    unlock(second.balanceRow);
    unlock(first.balanceRow);
}
```

Now if A→B and B→A happen simultaneously, **both transactions try to lock A(1001) first**.
One gets it, the other waits — no circular wait → no deadlock.

> **Backend gold:** This is a real rule in production payment code. If your service acquires
> multiple DB locks, always lock rows/tables in a consistent fixed order (e.g., by primary key).
> Every senior dev knows this. When an interviewer asks "how do you prevent deadlocks in your
> code?" — this is the answer with a real example.

---

### Strategy 2: Avoidance — Banker's Algorithm

Instead of breaking conditions by design, **dynamically decide at runtime** whether granting
a resource request is safe. If it might lead to deadlock → deny the request (thread waits).

**Banker's Algorithm** (Dijkstra, 1965): named after a bank that never gives out cash if
it can't satisfy maximum possible withdrawals of all customers.

#### Key concept: Safe State vs Unsafe State
- **Safe state** = there exists a **safe sequence** in which all threads can finish without deadlock.
- **Unsafe state** = no such sequence exists → deadlock is *possible*.
- Algorithm keeps system always in a **safe state** by refusing unsafe requests.

#### Data structures (N threads, M resource types):
```
Available[M]      — how many of each resource are currently free
Max[N][M]         — max resources each thread will EVER need
Allocation[N][M]  — what each thread currently holds
Need[N][M]        — what each thread still needs  (= Max − Allocation)
```

#### Safety check (simplified):
```
Work = Available
For each thread i (in some order):
  If Need[i] <= Work:
    Work = Work + Allocation[i]  (simulate it finishing, releasing its resources)
    Mark it done
If all threads can finish this way → SAFE STATE ✅
Else → UNSAFE ❌
```

**Why mostly theoretical:**
- ❌ Must know maximum resource need in advance (rarely known in real systems).
- ❌ Number of threads/resources must be fixed.
- ❌ Too much overhead for real-time systems.
- Real systems (like databases) use **detection + recovery** instead.

---

### Strategy 3: Detection & Recovery

Let deadlocks happen, then **detect and fix** them.

#### Detection — Resource Allocation Graph (RAG)
Draw a graph:
- **Circles** = threads, **Squares** = resources
- **Request edge** (thread → resource) = thread waiting for this resource
- **Assignment edge** (resource → thread) = resource is held by this thread

**Deadlock = a CYCLE in this graph.**

```
Money transfer deadlock as a RAG:

  T1 ──request──► [B's row] ──assigned──► T2
  ▲                                        │
  └──────────assigned── [A's row] ◄──request┘

Cycle exists → DEADLOCK CONFIRMED
```

#### Recovery — how to break the deadlock:
1. **Kill a transaction** — terminate one deadlocked thread/transaction, roll it back.
   → DB engines do this: pick a "victim" (cheapest to rollback), kill it, let others proceed.
2. **Preempt a resource** — forcibly take a resource from one thread, give to another.
3. **Rollback** — roll thread back to a safe checkpoint before it acquired the resources.

> **This is what your DB does:** MySQL, PostgreSQL, Oracle constantly run deadlock detection.
> When a cycle is detected, they pick the cheapest transaction to kill.
> You get: `ERROR 1213: Deadlock found when trying to get lock; try restarting transaction`
> Your application should catch this and **retry the transaction**.

---

### Strategy 4: Ignorance — Ostrich Algorithm

**Just ignore the possibility of deadlock.** If it happens, restart the app/process.

Sounds absurd — but **most general-purpose OSes (Linux, Windows, macOS) use this** for most
resources because:
- Deadlocks in practice are rare.
- Prevention/avoidance overhead is constant and expensive.
- Kill/restart a process is cheap and acceptable.

Named "Ostrich Algorithm" — like an ostrich burying its head in the sand.

---

## PART J — Deadlock vs Starvation (don't confuse them)

| | **Deadlock** | **Starvation** |
|---|---|---|
| What | threads wait forever, **mutually blocking each other** | a thread waits forever because **others keep getting priority** |
| Cause | circular resource dependency | scheduling bias (priority, SJF) |
| Others making progress? | **No** — everyone is stuck | **Yes** — others run fine, just not this one |
| Fix | prevention / avoidance / detection | aging (boost waiting thread's priority over time) |

> Deadlock = the whole group is stuck in a circle.
> Starvation = one person keeps getting skipped while everyone else moves forward.

---

## 🖥️ HANDS-ON: Deadlock Detection Tools (production)

### Java — detect deadlocked threads in a running JVM
```bash
jstack <PID>                           # full thread dump of every thread
jstack <PID> | grep -A 20 'deadlock'  # filter for deadlock section
```

Output when deadlock found:
```
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock <0x...> (User B's balance row lock)
  which is held by "Thread-2"
"Thread-2":
  waiting to lock <0x...> (User A's balance row lock)
  which is held by "Thread-1"
```

> `jstack` is one of the most useful production debugging tools for a backend dev.
> If your service ever hangs or freezes → `jstack <PID>` immediately shows deadlocks.

```bash
lsof -p <PID>     # files/sockets/locks held by a process
strace -p <PID>   # system calls — see exactly what it's blocked waiting on (Linux)
```

### Database — inspect locks and deadlocks
```sql
-- MySQL: see last deadlock
SHOW ENGINE INNODB STATUS;

-- MySQL: current lock waits
SELECT * FROM information_schema.INNODB_LOCKS;

-- PostgreSQL: see all current locks
SELECT * FROM pg_locks;

-- PostgreSQL: see what queries are waiting and why
SELECT pid, wait_event, wait_event_type, query
FROM pg_stat_activity
WHERE wait_event IS NOT NULL;
```

---

## ✅ Chapter 3 Summary (quick revision)

**Synchronization:**
- **Race condition** — result depends on thread timing → incorrect behavior.
- **Critical section** — code accessing shared data, needs protection.
- 3 requirements: **Mutual Exclusion, Progress, Bounded Waiting**.
- **Mutex** — binary lock WITH ownership. (`synchronized`, `ReentrantLock`).
- **Semaphore** — integer counter, binary or counting (N resources), NO ownership.
- **Key difference:** mutex has ownership (acquirer must release); semaphore doesn't.

**Classic Problems:**
- **Producer-Consumer** — bounded buffer, 3 semaphores (mutex + empty + full) = message queues.
- **Reader-Writer** — concurrent reads OK, exclusive write = `ReentrantReadWriteLock`.
- **Dining Philosophers** — circular resource wait → deadlock model = DB transaction deadlocks.

**Deadlocks:**
- **4 Coffman conditions** (ALL must hold): Mutual Exclusion, Hold & Wait, No Preemption, Circular Wait.
- **Real example:** A sends to B + B sends to A simultaneously → each locks sender first →
  T1 holds A's lock waiting for B's, T2 holds B's lock waiting for A's → deadlock.
- **Fix (prevention):** Always lock rows in consistent order (lower user ID first) →
  breaks circular wait → no deadlock possible.
- **4 strategies:**
    - **Prevention** — break a Coffman condition by design. Most practical: lock ordering.
    - **Avoidance** — Banker's algorithm, safe state checks(mostly theoretical).
  - **Detection + Recovery** — detect cycles in RAG, kill victim transaction (what DBs do).
  - **Ignorance** — Ostrich algorithm (what most OSes do for rare cases).
- **Deadlock vs Starvation** — circular block vs scheduling skip.
- **Tools:** `jstack` (Java), `SHOW ENGINE INNODB STATUS` (MySQL), `pg_locks` (PostgreSQL).