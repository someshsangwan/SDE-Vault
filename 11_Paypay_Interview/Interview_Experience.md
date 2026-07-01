# PayPay Securities — Most-Asked Interview Questions & Answers

> Compiled from Glassdoor, InterviewBit, LeetCode Discuss, Medium, and first-hand write-ups (gaijineer, HackMD, InterviewCat).
> Most public data is for **PayPay** (the parent); the process and question style are identical. The **Securities** twist = brokerage / order / ledger domain instead of P2P payments.
> Related notes: [[Paypay_general_question]] · [[My_Resume_Q&A]]

---

## 0. The Interview Process (from a first-hand LeetCode write-up)

The loop is split into **5 parts, conducted separately based on your availability** (don't try to do them all in one day — it's draining):

| Part | Focus | Coding problem given |
|---|---|---|
| 1. Online Assessment | Backend online code challenge | (you've already cleared this) |
| 2. HR Screening | Expected salary, join date, background check | — |
| 3. Fundamental Knowledge | Networking, DB, Java, your experience | **Two Sum** (easy) |
| 4a. System Design | Design PayPay + third-party (bank) integration | **Find First & Last Position in Sorted Array** (binary search) |
| 4b. Algo & DS | Implement a DS / how a DS works under the hood + Java | **Number of Islands** |
| 5. Behavioral | Background, problem-solving depth, biggest mistake | — |

**Verbatim from the candidate:** *"the first session will be system design asking about designing PayPay itself, the scope will be interacting with the third party (bank). They will talk about availability, improving performance on DB write and read, how do you scale your system, and security."*

**Recurring theme across all sources:** recruiters *say* "CS fundamentals," but interviewers pivot hard to **"how would you build a payment / transaction system?"** and grill your resume. The interviewers are described as **nice**, but the bar is real — backend candidates rate the loop ~3.1/5. Two common failure points: (1) DB isolation/locking under pressure, (2) **panicking on time management in system design**.

---

## 1. Fundamental Knowledge Round — Java / Language Internals ⭐

**Q: When does an object become eligible for garbage collection?**
When no longer reachable from any GC root (thread stack, static fields, JNI refs). Triggers: reference set `null`, reassigned, out of scope, or "island of isolation" (mutually-referencing objects unreachable from roots).

**Q: How does GC work in the JVM?**
Generational hypothesis — most objects die young.
- **Young gen** (Eden + 2 Survivors) → minor GC, fast copying collector.
- **Old gen** → major/full GC, expensive.
- Collectors: **G1** (default since Java 9, region-based, low-pause), **ZGC**/**Shenandoah** (sub-ms pauses, large heaps).
- Mark → Sweep → Compact; reachability from GC roots decides liveness.
- Know `-Xms/-Xmx`, "stop-the-world" pauses, and why full GC hurts a low-latency trading path.

**Q: HashMap vs Hashtable vs ConcurrentHashMap** (asked very often)
- `HashMap`: unsynchronized, one null key + null values allowed, fast, not thread-safe.
- `Hashtable`: legacy, method-level synchronized (coarse lock), no nulls — avoid.
- `ConcurrentHashMap`: thread-safe via fine-grained locking (bucket-level CAS + `synchronized` in Java 8+, no more segments), no null keys/values — **use in prod**.

**Q: How does a HashMap work under the hood?** (they like "under the hood")
Array of buckets; `hash(key)` → index. Collisions form a linked list, which **converts to a red-black tree when a bucket exceeds 8 entries** (Java 8+) for O(log n) worst case. Load factor 0.75 → resize (double + rehash). `equals`/`hashCode` contract is what makes lookups correct.

**Q: Process vs Thread** — Process = own address space, isolated, heavy. Thread = shares process heap, own stack + PC, lightweight; shared memory is why races happen.

**Q: Race condition vs Data race**
- **Data race:** two threads touch the same memory concurrently, ≥1 write, no synchronization → undefined behavior (memory-level).
- **Race condition:** correctness depends on interleaving/timing (a logic bug). Can exist without a data race (e.g. check-then-act on synchronized-but-non-atomic ops).

**Q: Deadlock / Livelock / Starvation**
- **Deadlock:** cyclic wait; 4 Coffman conditions (mutual exclusion, hold-and-wait, no preemption, circular wait). Break one — e.g. global lock ordering.
- **Livelock:** threads keep reacting to each other, no progress (two people dodging in a corridor).
- **Starvation:** a thread never gets the lock/CPU (unfair scheduling). Fix: fair locks.

**Q: How does Spring IoC / DI work?** Container (not your code) creates and wires beans. On startup Spring scans `@Component`/`@Service`/`@Repository`, builds bean definitions, instantiates, and injects dependencies (constructor injection preferred) via `ApplicationContext`. Benefits: loose coupling, testability, centralized lifecycle/scope.

**Also be ready for:** `volatile` vs `synchronized`, immutability/`final`, `String` pool & interning, `equals`/`hashCode`, `ExecutorService`/thread pools, `CompletableFuture`.

---

## 2. Databases ⭐⭐ (heavily emphasized — fintech)

**Q: Explain ACID.**
- **Atomicity:** all-or-nothing.
- **Consistency:** valid state → valid state (invariants hold).
- **Isolation:** concurrent txns don't corrupt each other (isolation levels).
- **Durability:** committed data survives crashes (WAL/redo flushed to disk).
Ground it: *a stock buy that debits the wallet and credits shares must be atomic — never one without the other.*

**Q: Isolation levels + anomalies they prevent** (know this table cold)

| Level | Dirty read | Non-repeatable read | Phantom read |
|---|---|---|---|
| Read Uncommitted | ❌ | ❌ | ❌ |
| Read Committed | ✅ | ❌ | ❌ |
| Repeatable Read | ✅ | ✅ | ❌ (MySQL InnoDB prevents via gap locks) |
| Serializable | ✅ | ✅ | ✅ |

- **Dirty read:** read another txn's uncommitted change.
- **Non-repeatable read:** same row, two reads, different values (a committed update in between).
- **Phantom read:** same range query returns different *rows* (insert/delete in between).
Higher isolation = more correctness, less concurrency. Fintech default Read Committed; escalate to Serializable / explicit locks for money paths.

**Q: Commit vs Rollback / rollback transaction / autocommit?**
- **Commit:** persist + make durable/visible.
- **Rollback:** undo all changes since txn start (undo log).
- **Autocommit:** each statement is its own txn. For multi-step money ops, turn it OFF and wrap in an explicit txn (`@Transactional`).

**Q: Optimistic vs Pessimistic locking** (the money question)
- **Pessimistic:** `SELECT ... FOR UPDATE` locks the row; others block. Use under high contention (two orders on the same limited holding). Risk: deadlocks, lower throughput.
- **Optimistic:** no lock; add a `version` column, update `WHERE version = :old`. 0 rows updated → conflict → retry. Spring: `@Version`. Great for wallet balances at scale.
- Be ready to state **which you'd pick for a stock order** and why.

**Also:** indexing (B-tree, when used), normalization vs denormalization, SQL vs NoSQL, sharding, read replicas, JPA N+1 problem.

---

## 3. Fundamental Knowledge Round — Networking / Backend

**Q: "What happens when you type a URL and press Enter?"** ⭐ (asked almost every loop — have a crisp 3-min answer)
1. Browser/OS cache check, then **DNS** resolution (browser → OS → resolver → root → TLD → authoritative) → IP.
2. **TCP handshake** (SYN, SYN-ACK, ACK) to IP:443.
3. **TLS handshake** (cert validation, key exchange, session keys).
4. Browser sends **HTTP request** (method, headers, cookies).
5. Server path (load balancer → app server → cache/DB) processes → **HTTP response**.
6. Browser parses HTML → DOM, fetches CSS/JS/images, renders, runs JS.
Bonus: CDN, keep-alive, HTTP/2 multiplexing.

**Other topics:** OSI/TCP-IP layers, TCP vs UDP, HTTP methods & status codes, HTTPS/TLS, load balancing, Docker/K8s basics, Linux (processes, file descriptors, `grep`/`top`), CAP theorem, idempotency, at-least-once vs exactly-once.

---

## 4. Algo & Data Structures Round (LeetCode Easy→Medium)

Confirmed / representative problems (solve these in **Java**):
- **[Two Sum]** — the "easy warm-up" in the fundamentals round. HashMap one-pass, O(n).
- **[Find First and Last Position of Element in Sorted Array]** — binary search twice (leftmost + rightmost bound). Given in the system-design session.
- **[Number of Islands]** — grid DFS/BFS flood fill; know both + Union-Find variant. Given in the algo session.
- Plus reported classics: longest palindromic substring, palindrome check, is-a-SumTree, general binary search, one medium DP.

**"Under the hood" DS questions:** be ready to *implement* or explain internals of — HashMap, LinkedList, Stack/Queue, Heap/PriorityQueue, and give Big-O for operations. They may ask "which DS for scenario X and why?"

**Prep set:** Trees, Graphs, Heaps, HashMaps, DP. Practice fast **stdin/stdout parsing** (OA is HackerRank-style). The OA had 2–4 problems; one candidate cleared with 3/4 done — partial credit exists, but finish as many as you can.

---

## 5. System Design ⭐⭐⭐ (where people fail)

**The exact prompt (verbatim from a candidate): "designing PayPay itself, scope = interacting with the third party (bank) — availability, improving DB write/read performance, how you scale, and security."**
For **Securities**, expect a brokerage framing. Have these three ready (you've drafted them — good):
1. **Stock Order System** (most likely) — placement → matching/routing → execution → settlement.
2. **Wallet / Payment Ledger** — double-entry ledger, idempotency, balance consistency.
3. **Real-Time Stock Price Feed** — fan-out via WebSocket/pub-sub, low latency.

**Framework (don't panic on time — the #1 failure cause):**
1. **Clarify** requirements + scale (QPS, users, read/write ratio) — ~5 min.
2. **APIs** (`placeOrder`, `getPortfolio`, `deposit`).
3. **Data model** (accounts, holdings, orders, ledger entries).
4. **Architecture** (API GW → order service → matching → wallet/ledger → DB + cache + queue).
5. **Deep dive** on the money-critical part: **consistency & idempotency**.
6. **Scale:** read replicas, sharding by account, caching, async via Kafka. (Directly answers their "improve DB read/write" + "how do you scale.")
7. **Reliability & security:** exactly-once, audit trail, encryption, PII, reconciliation with the bank.

**Fintech concepts to name-drop (they signal domain seriousness):**
- **Double-entry ledger** (balanced debit + credit; never mutate a balance in place).
- **Idempotency keys** (retries must not double-charge/double-buy).
- **Distributed txns:** 2PC vs **Saga + compensating actions** (prefer Saga for microservices).
- **Exactly-once vs at-least-once**, outbox pattern.
- **Strong consistency** on money (not eventual).
- **Reconciliation** with bank/exchange, immutable audit logs.
- **DB read/write scaling:** primary-replica, CQRS, caching hot reads, write batching/queuing.

---

## 6. Behavioral / HR (final round)

Assesses **personal values, work approach, growth mindset**. The interviewer is described as nice — be genuine.

Reported questions:
- **What's the biggest mistake you've ever made? What lesson did you learn?** *If you could go back, what would you do to fix it?* (asked verbatim)
- Deep-dive on **how you solved a hard problem you faced** — expect follow-ups probing depth.
- What is your **biggest weakness**?
- **"What is something no one can beat you at?"**
- **Why PayPay (Securities)?** Why leave Rakuten Pay?
- Self-developed product vs **SIer**-style development — what draws you?
- **Expected salary + earliest join date** (in the HR screen).

**Prep:** STAR + the **conclusion → reason → example → conclusion** framing (favored in the Japan market). Lean on your Rakuten Pay payments background — directly relevant. Have sharp **reverse questions** about team direction, tech, and engineer ownership.

---

## 7. Quick Prep Checklist (interview on the 10th)

- [ ] **DB isolation levels + optimistic/pessimistic locking** table — explain it *in the context of a stock order*.
- [ ] 3-minute crisp **"what happens when you type a URL"** answer.
- [ ] **GC + HashMap/ConcurrentHashMap (+ under the hood) + race vs data race** — say them fluently.
- [ ] Re-solve **Two Sum, Find First & Last Position (binary search), Number of Islands** in Java — these were literally asked.
- [ ] Rehearse **Stock Order + Wallet Ledger** design end-to-end, **timed, out loud** (time mgmt is where people fail). Explicitly cover availability, DB read/write scaling, and security — the exact axes they probe.
- [ ] 3 STAR stories from Rakuten Pay (a **mistake + lesson + what you'd do differently**, a hard problem, a conflict) + "why PayPay Securities".
- [ ] Know your **expected salary + join date** for the HR screen.

---

## Sources
- [PayPay Japan | Entire interview — LeetCode Discuss](https://leetcode.com/discuss/interview-experience/1806599/PayPay-Japan-or-Entire-interview/) *(the 5-part breakdown + exact LC problems above)*
- [PayPay Japan Backend Online Code Challenge 2 — LeetCode](https://leetcode.com/discuss/interview-question/1490866/PayPay-Japan-Backend-Online-Code-Challenge-2)
- [PayPay SWE Interview — Glassdoor](https://www.glassdoor.com/Interview/PayPay-Software-Engineer-Interview-Questions-EI_IE3735809.0,6_KO7,24.htm)
- [PayPay Backend Engineer — Glassdoor](https://www.glassdoor.com/Interview/PayPay-Backend-Engineer-Interview-Questions-EI_IE3735809.0,6_KO7,23.htm)
- [PayPay Interview Questions — InterviewBit](https://www.interviewbit.com/paypay-interview-questions/)
- [PayPay Tokyo Interview Experience — Medium (Niladri)](https://medium.com/@niladribhusandalai/paypay-tokyo-interview-experience-23bf43f9e31d)
- [SWE Interview Experience with PayPay Japan — gaijineer](https://gaijineer.co/software-engineer-interview-experience-with-paypay-japan)
- [PayPay/PayPayカード SWE 面接対策 — InterviewCat](https://jobs.interviewcat.dev/blog/paypay-software-engineer-interview)