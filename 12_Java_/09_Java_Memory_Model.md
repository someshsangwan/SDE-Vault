# Chapter 9 — Java Memory Model (JMM) & happens-before

> The "senior" chapter — the theory under Chapter 3. You don't need all of it daily, but ONE question from here appears in almost every SDE2 loop: **double-checked locking** (§5). Read Chapter 3 first.

**Related:** [[03_Multithreading_Concurrency]] (§4–§6 are the practice; this is the why) · [[08_String_Immutability]] (final fields)

---

## Words used in this chapter (plain meanings)

| Word | Plain meaning |
|------|---------------|
| **Reordering** | Compiler/CPU running your statements in a different order than you wrote them |
| **JMM** | The rulebook saying which reorderings/cachings are allowed, and what a thread is guaranteed to see |
| **happens-before (HB)** | The JMM's guarantee: "if A happens-before B, then B SEES everything A did" |
| **Memory barrier** | A fence instruction that blocks reordering/caching across it (what volatile compiles to) |
| **Safe publication** | Handing an object to another thread so it sees the object FULLY built |

---

## 1. The uncomfortable truth: your code doesn't run in the order you wrote it

For speed, the compiler, the JIT, and the CPU all **reorder** instructions and **cache** values — as long as the result looks the same *to a single thread*. Multi-threaded code sees behind the curtain:

```java
// two threads, four variables, all start 0:
int x = 0, y = 0, r1 = 0, r2 = 0;

Thread A:  x = 1;   r1 = y;
Thread B:  y = 1;   r2 = x;
```
Intuition says at least one of r1/r2 must be 1. **Reality: `r1 == 0 && r2 == 0` is possible** — each thread's write may be reordered after its read, or sit in a store buffer the other core hasn't seen. Run it a million times and you'll catch it.

**Within one thread you never notice** (the JVM preserves single-thread illusion — "as-if-serial"). Across threads, all bets are off — *unless* you create a happens-before edge.

---

## 2. happens-before — the only guarantee that exists

**Definition (memorize):** if action A *happens-before* action B, then B is guaranteed to **see** everything A wrote, and A can't be reordered after B.

No HB edge between two accesses to the same data (one a write) = a **data race** = anything can happen (Chapter 3's stale `stopped` flag).

### The HB rules you must know (each one is a tool from Chapter 3!)

| # | Rule | Plain words | Chapter 3 tool |
|---|------|-------------|----------------|
| 1 | **Program order** | within ONE thread, line N happens-before line N+1 | (single-thread sanity) |
| 2 | **Monitor lock** | unlocking a lock HB everything after a later lock of the SAME lock | `synchronized` (§5 whiteboard rule) |
| 3 | **Volatile** | a volatile WRITE happens-before every later volatile READ of that variable | `volatile` (§6) |
| 4 | **Thread start** | `t.start()` HB everything inside t | thread sees what you set up before starting it |
| 5 | **Thread join** | everything inside t HB `t.join()` returning | after join you see all of t's writes |
| 6 | **Transitivity** | A HB B and B HB C → A HB C | this is what makes rule 3 powerful |

**Transitivity is the magic.** The volatile write publishes MORE than the volatile variable itself:
```java
int data = 0;                     // plain, not volatile!
volatile boolean ready = false;

Thread A:  data = 42;             // (1)  program order: (1) HB (2)
           ready = true;          // (2)  volatile write
Thread B:  if (ready) {           // (3)  volatile read: (2) HB (3)
               print(data);       // (4)  transitivity: (1) HB (4) → GUARANTEED to print 42
           }
```
One volatile flag safely publishes the whole batch of plain writes done before it. This exact pattern is how lock-free queues hand over objects.

---

## 3. What volatile really does (deeper than Chapter 3)

Two services, not one:
1. **Visibility** — reads/writes go to main memory (Chapter 3's story).
2. **Ordering** — it's a **memory barrier**: writes above a volatile write can't slide below it; reads below a volatile read can't float above it.

Number 2 is why the `data/ready` example works — and why double-checked locking breaks without it:

---

## 4. Reminder of what it still doesn't give you

`volatile` ≠ atomicity (`count++` still broken — Chapter 3 §6). JMM gives *ordering and visibility* guarantees; for read-modify-write you still need CAS/locks (§7 of Chapter 3).

---

## 5. ★★ Double-Checked Locking — THE JMM interview question

Goal: a lazy singleton that doesn't pay the lock cost on every access.

```java
// ❌ BROKEN without volatile (the famous bug):
class Config {
    private static Config instance;              // ← missing volatile!

    static Config getInstance() {
        if (instance == null) {                  // check 1 (no lock — fast path)
            synchronized (Config.class) {
                if (instance == null)            // check 2 (with lock)
                    instance = new Config();     // ← the dangerous line
            }
        }
        return instance;
    }
}
```

**Why broken?** `instance = new Config()` is really 3 steps: ① allocate memory ② run constructor ③ point `instance` at it. The JIT/CPU may **reorder ② and ③**. Thread A does ①③ — and *then* Thread B hits check 1, sees `instance != null` (step ③ done!), skips the lock, and uses a **half-constructed object** — fields still default values. Rare, unreproducible, production-only. The nastiest kind of bug.

**Fix — one word:**
```java
private static volatile Config instance;   // volatile forbids the ②③ reorder (write barrier)
                                            // and check 1's read now has HB with the write
```

**The better answers (say these next):**
```java
// Holder idiom — lazy AND lock-free, JVM class-loading is thread-safe by spec:
class Config {
    private Config() {}
    private static class Holder { static final Config INSTANCE = new Config(); }
    static Config getInstance() { return Holder.INSTANCE; }   // Holder loads on first call
}

// Or Effective Java's favorite: enum singleton (serialization-safe too)
enum Config { INSTANCE; }
```
**Interview flow:** show DCL → explain the reorder bug → fix with volatile → *"but in practice I'd use the holder idiom or an enum."* That sequence is a complete senior answer.

---

## 6. final fields — immutability's secret weapon

The JMM gives `final` fields a special guarantee: **if an object is properly constructed (`this` doesn't escape the constructor), every thread sees its final fields correctly initialized — no volatile, no locks.**

```java
class Txn {
    private final String id;              // final → safely published with the object
    private int retries;                  // non-final → another thread may see 0!
    Txn(String id) { this.id = id; this.retries = 3; }
}
```
This is why immutable objects ([[08_String_Immutability]] §4) are *automatically* thread-safe to share by any means — the JMM itself protects them. One more reason "make it immutable" is the first answer to most concurrency questions.

**The escape trap:** the guarantee dies if the constructor leaks `this` (registers itself as a listener, stores `this` in a static, starts a thread with `this`) before finishing.

---

## 7. Safe publication — the checklist

"Publish" = make an object created by one thread visible to others. The safe ways (each = an HB edge from the table):

1. **static initializer / holder idiom** — class loading is safe (rule of the JVM spec)
2. **volatile field** (or `AtomicReference`)
3. **synchronized** — same lock on both sides
4. **concurrent collections** — putting into a `ConcurrentHashMap`/`BlockingQueue` publishes safely (their docs promise the HB edge)
5. **final field** of a properly constructed object
6. `Thread.start()` / handing to an `ExecutorService`

Unsafe: a plain non-volatile field. (That's just §1's reordering demo with objects.)

---

## ⭐ Quick Revision — Likely Interview Questions

1. What is the JMM in one sentence? Why can't you reason with "the code runs in written order"?
2. In the two-thread x/y demo — can both reads see 0? Why?
3. Define happens-before. What is a data race?
4. List 5 happens-before rules and the Chapter-3 tool each one powers.
5. The `data/ready` volatile-flag example — why is plain `data` guaranteed visible? (transitivity)
6. What TWO things does volatile give? What does it still not give? (ordering+visibility; not atomicity)
7. Write double-checked locking. Why is it broken without volatile? (constructor/assignment reorder → half-built object)
8. What's better than DCL? (holder idiom, enum) Why is the holder thread-safe? (class loading)
9. What special guarantee do final fields have? When does it break? (this-escape)
10. Name 4 safe-publication techniques.
11. Why are immutable objects automatically thread-safe? (final field semantics + no state changes)
