# PayPay Securities (PPSEC) — Technical Interview Prep (Detailed)

> Compiled from Glassdoor, InterviewBit, LeetCode Discuss, Medium, and first-hand write-ups (gaijineer, HackMD, InterviewCat).
> Most public interview data is for **PayPay** (the parent) — the process and question style are the same. The **Securities** twist: brokerage / stock order / ledger domain instead of P2P payments. Ground every example in *"a user buys a stock in the app"*.
> Related notes: [[Paypay_general_question]] · [[My_Resume_Q&A]]

---

## 0. What HR Actually Told You (read this first)

> *"This will be a technical interview. The primary objective is to evaluate your foundational technical skills, specifically focusing on basic Computer Science concepts such as **databases, data structures, and algorithms**. There is also a possibility of a **live coding** session. We may also ask you to share your **reasons for applying to PPSEC**."*

So the priority order for this round is:

1. **Databases** (§2) — expect the most depth here, it's fintech.
2. **Data Structures** (§3) — "how does X work under the hood" style.
3. **Algorithms + Live Coding** (§4, §5) — LeetCode Easy→Medium in Java.
4. **Why PPSEC** (§8) — have a 60–90 second answer ready.

Java/JVM (§6) and networking (§7) are secondary for *this* round but PayPay interviewers historically drift into them, so keep them warm. System design (§9) is likely a *later* round — skim it, don't cram it now.

### What is PayPay Securities? (know your target company)

- **PayPay Securities (PayPay証券 / "PPSEC")** is the **online stock brokerage** arm of the PayPay group. PayPay's ecosystem = PayPay app (payments), PayPay Card, PayPay Bank, and PayPay Securities — about **73 million registered app users** (as of March 2026).
- Its pitch: **investing for beginners** — buy Japanese and US stocks in **small / fractional amounts** (from ~¥100 or ¥1,000) directly inside the PayPay app ("PayPay資産運用" — asset management mini-app). Deposits come straight from your PayPay balance, so the barrier to a first-ever stock purchase is tiny.
- Parent **PayPay Corporation** is a SoftBank Group company and **listed on Nasdaq (ticker PAYP) in March 2026**, raising ~$880M — so the group is in aggressive-growth, "financial super-app" mode, and Securities is one of its growth engines.
- Engineering reality behind that product: order placement, price feeds, fractional-share accounting, a money ledger, strict consistency, and Japan FSA-regulated audit requirements. That's why they grill **databases + transactions** so hard.

---

## 1. The Interview Process (from a first-hand LeetCode write-up)

The loop is split into **5 parts, conducted separately based on your availability** (don't try to do them all in one day — it's draining):

| Part | Focus | Coding problem given |
|---|---|---|
| 1. Online Assessment | Backend online code challenge | (you've already cleared this) |
| 2. HR Screening | Expected salary, join date, background check | — |
| 3. Fundamental Knowledge | DB, DS, algorithms, Java, your experience — **← you are here** | **Two Sum** (easy) |
| 4a. System Design | Design the product + third-party (bank/exchange) integration | **Find First & Last Position in Sorted Array** |
| 4b. Algo & DS | Implement a DS / how a DS works under the hood, in Java | **Number of Islands** |
| 5. Behavioral | Background, problem-solving depth, biggest mistake | — |

**Recurring theme across all sources:** recruiters *say* "CS fundamentals," but interviewers often pivot to **"how would you build a transaction system?"** and grill your resume. Interviewers are described as **nice**, but the bar is real. Two common failure points: (1) DB isolation/locking under pressure, (2) panicking on time management.

---

## 2. Databases ⭐⭐⭐ (HR named this first — go deepest here)

### 2.0 Absolute basics (they may start here to warm you up)

**Q: What is a database? What is a DBMS?**
A **database** is an organized collection of data stored so it can be found, changed, and protected efficiently. A **DBMS** (Database Management System — MySQL, PostgreSQL, Oracle) is the software that manages it: it parses your SQL, finds the data on disk, handles many users at once without them corrupting each other, and survives crashes. Without a DBMS you'd be writing raw files and reinventing locking, indexing, and crash recovery yourself.

**Q: What is a relational database?**
Data lives in **tables** (rows and columns), and tables **relate** to each other through keys. Example for PPSEC:

- `users(user_id, name, email)`
- `accounts(account_id, user_id, cash_balance)` — `user_id` here points back to `users`
- `orders(order_id, account_id, symbol, qty, price, status)`
- `holdings(account_id, symbol, qty)`

The "relation" is: an order belongs to an account, an account belongs to a user. You reconstruct the full picture with **JOINs**.

**Q: Primary key vs Foreign key vs Unique key — what's the difference?**
- **Primary key (PK):** the column that uniquely identifies each row. Rules: unique + never NULL + one per table. `order_id` in `orders`.
- **Foreign key (FK):** a column that stores another table's PK, creating the relationship. `orders.account_id` is an FK referencing `accounts.account_id`. The DB can enforce it — you can't create an order for an account that doesn't exist ("referential integrity").
- **Unique key:** also enforces uniqueness, but you can have several per table and (in most DBs) it allows NULL. `users.email` — not the identifier of the row, but no two users may share it.

Baby version: PK = *"this row's ID card"*. FK = *"a note saying which other row I belong to"*. Unique = *"no duplicates allowed here, but it's not the ID card"*.

**Q: Explain the JOIN types.**
A JOIN combines rows from two tables where a condition matches. Picture `accounts` (left) and `orders` (right):

- **INNER JOIN:** only rows that match on both sides. *Accounts that have at least one order* — accounts with zero orders disappear from the result.
- **LEFT JOIN:** every left row, matched right rows where they exist, `NULL`s where they don't. *All accounts, with their orders if any* — an account with no orders still appears, with NULL order columns. This is how you find "accounts that never traded": `LEFT JOIN orders ... WHERE orders.order_id IS NULL`.
- **RIGHT JOIN:** mirror of LEFT (rarely used — people just swap table order and use LEFT).
- **FULL OUTER JOIN:** everything from both sides, NULLs where no match. (MySQL doesn't support it natively; PostgreSQL does.)

```sql
SELECT a.account_id, o.symbol, o.qty
FROM accounts a
LEFT JOIN orders o ON o.account_id = a.account_id;
```

**Q: WHERE vs HAVING?**
Both filter, but at different stages. **WHERE filters rows before grouping; HAVING filters groups after `GROUP BY`.**

```sql
-- accounts whose total BUY amount this year exceeds ¥1,000,000
SELECT account_id, SUM(qty * price) AS total
FROM orders
WHERE side = 'BUY' AND created_at >= '2026-01-01'   -- row-level filter first
GROUP BY account_id
HAVING SUM(qty * price) > 1000000;                   -- group-level filter after
```

You *cannot* put `SUM(...) > 1000000` in WHERE — the sum doesn't exist yet at that stage.

**Q: DELETE vs TRUNCATE vs DROP?**
- **DELETE:** removes rows one by one, can have a `WHERE`, is a normal transaction (**can be rolled back**), fires triggers. Slow on huge tables.
- **TRUNCATE:** wipes *all* rows instantly by deallocating the pages. No WHERE, usually can't be rolled back (DDL in MySQL), resets auto-increment. Fast.
- **DROP:** deletes the entire table — data *and* structure. The table no longer exists.

Baby version: DELETE = erasing lines from a notebook page. TRUNCATE = ripping the page out. DROP = throwing the notebook away.

**Q: What is normalization? And why would you ever denormalize?**
**Normalization** = organizing tables so each fact is stored **exactly once**, to avoid update anomalies. The first three normal forms, simply:

- **1NF:** every cell holds one value — no comma-separated lists. (Not `symbols = "AAPL,TSLA"` in one column; one row per holding.)
- **2NF:** every non-key column depends on the *whole* primary key. If the PK is `(account_id, symbol)`, don't also store `account_owner_name` there — it depends only on `account_id`, so it belongs in `accounts`.
- **3NF:** non-key columns don't depend on *other non-key columns*. Don't store both `symbol` and `company_name` in `orders` — `company_name` depends on `symbol`, so it belongs in a `stocks` table.

Why it matters: if `company_name` is copied into a million order rows and the company renames itself, you must update a million rows and might miss some → inconsistent data.

**Denormalization** = deliberately re-copying data to make reads faster (fewer JOINs). Example: store a precomputed `portfolio_value` on the account instead of summing holdings on every screen load. Trade-off: faster reads, but now you must keep the copy in sync. Rule of thumb: **normalize for correctness first, denormalize only for a measured read bottleneck.**

---

### 2.1 Transactions & ACID (the #1 fintech question — own this)

**Q: What is a transaction?**
A group of SQL statements that the database treats as **one indivisible unit**: either *all* of them take effect, or *none* do. Marked by `BEGIN` … `COMMIT` (keep) or `ROLLBACK` (undo everything).

The canonical PPSEC example — a user buys 1 share of Apple for ¥30,000:

```sql
BEGIN;
UPDATE accounts SET cash_balance = cash_balance - 30000 WHERE account_id = 42;
UPDATE holdings SET qty = qty + 1 WHERE account_id = 42 AND symbol = 'AAPL';
INSERT INTO orders (account_id, symbol, qty, price, status) VALUES (42, 'AAPL', 1, 30000, 'FILLED');
COMMIT;
```

If the server crashes after the first UPDATE but before COMMIT, the user must **not** end up with money gone and no share. The transaction guarantees both-or-neither.

**Q: Explain ACID.** (Say each letter, define it, give the brokerage example.)

- **A — Atomicity:** *all or nothing.* All statements in the transaction succeed together or are all undone. How: the DB keeps an **undo log**; on failure it replays the log backwards. Example: debit cash + credit shares — never one without the other.
- **C — Consistency:** the transaction moves the DB from one *valid* state to another valid state — every rule (constraints, FKs, "balance ≥ 0", "total money in system unchanged") holds before and after. The DB enforces declared constraints; your application logic enforces business invariants.
- **I — Isolation:** concurrent transactions don't see each other's half-finished work. It should *look like* transactions ran one after another, even though they overlap. Controlled by **isolation levels** (next question — this is the follow-up 90% of the time).
- **D — Durability:** once the DB says "committed", the data survives a power cut. How: the change is first written to a **write-ahead log (WAL / redo log)** and flushed to disk *before* COMMIT returns. On restart, the DB replays the log. Example: order confirmation shown to the user must never vanish because a server died.

**Q: Commit vs Rollback? What is autocommit?**
- **COMMIT** = "make it permanent and visible to others."
- **ROLLBACK** = "undo everything since BEGIN" (via the undo log).
- **Autocommit** (the default in MySQL/JDBC) = every single statement is silently its own tiny transaction. Fine for one-off reads; **dangerous for multi-step money operations** — if step 2 fails, step 1 is already committed. So for the buy-a-stock flow you turn autocommit off and wrap the steps explicitly — in Spring, that's what `@Transactional` on the service method does for you (opens a transaction, commits on success, rolls back on a runtime exception).

---

### 2.2 Isolation levels & anomalies (know this cold — it's *the* PayPay question)

First understand the three **anomalies** (bugs caused by concurrency), each as a story with two people:

**Dirty read** — *reading someone's unsaved draft.*
Transaction A updates a stock price to ¥500 but hasn't committed. Transaction B reads ¥500 and shows it to a user. A then rolls back — ¥500 *never officially existed*, but B acted on it. B read "dirty" (uncommitted) data.

**Non-repeatable read** — *the same row changes between your two looks.*
Transaction B reads account 42's balance: ¥10,000. Meanwhile transaction A commits a deposit. B reads the *same row* again inside the *same transaction*: now ¥50,000. B's two reads disagree — it can't "repeat" its read. Bad when B is doing math across multiple reads (e.g., a risk check then a debit).

**Phantom read** — *the same query returns different rows.*
Transaction B runs `SELECT COUNT(*) FROM orders WHERE symbol='AAPL'` → 10 rows. A commits a *new* AAPL order. B reruns the identical query → 11 rows. No existing row changed — a **new row appeared like a phantom**. (Non-repeatable = a row's *values* changed; phantom = the *set of rows* changed.)

Now the four **isolation levels** — each one is just "which anomalies do I tolerate in exchange for speed":

| Level | Dirty read | Non-repeatable read | Phantom read | Plain-English behavior |
|---|---|---|---|---|
| Read Uncommitted | ❌ possible | ❌ possible | ❌ possible | You can see others' uncommitted work. Almost never used. |
| Read Committed | ✅ prevented | ❌ possible | ❌ possible | You only see committed data, but re-reads may differ. **PostgreSQL/Oracle default.** |
| Repeatable Read | ✅ prevented | ✅ prevented | ❌ possible* | Snapshot of the data when your txn started; your reads are stable. **MySQL InnoDB default** (*InnoDB actually blocks most phantoms too, via gap locks — nice bonus point to mention). |
| Serializable | ✅ prevented | ✅ prevented | ✅ prevented | As if transactions ran strictly one-by-one. Safest, slowest. |

The trade-off in one sentence: **higher isolation = more correctness, less concurrency (more locking/waiting)**. Fintech pattern: run the system at Read Committed / Repeatable Read for throughput, and protect the *money-critical paths* with explicit locking or Serializable.

**How the DB implements this (bonus depth):** modern DBs use **MVCC** (Multi-Version Concurrency Control) — instead of blocking readers, the DB keeps old versions of rows, and each transaction reads the version that was current at its snapshot time. That's why "readers don't block writers and writers don't block readers" in PostgreSQL/InnoDB.

---

### 2.3 Locking — optimistic vs pessimistic (the money question)

The problem both solve: **two transactions want to change the same row at the same time.** Classic PPSEC case — account 42 has ¥10,000 cash, and two buy orders of ¥8,000 each arrive simultaneously. Without protection: both read balance=10,000, both check "10,000 ≥ 8,000 ✓", both subtract → balance = −6,000. The user spent money they didn't have. This bug is called a **lost update / check-then-act race**.

**Pessimistic locking** — *assume conflict will happen, so lock first.*

```sql
BEGIN;
SELECT cash_balance FROM accounts WHERE account_id = 42 FOR UPDATE;  -- 🔒 row locked
-- check balance, then:
UPDATE accounts SET cash_balance = cash_balance - 8000 WHERE account_id = 42;
COMMIT;  -- 🔓 lock released
```

`FOR UPDATE` locks the row; the second transaction's `SELECT ... FOR UPDATE` **blocks and waits** until the first commits — then it sees balance=2,000, check fails, order rejected. Correct!
- ✅ Use when conflicts are **likely** (hot rows: a popular stock's inventory, one account being hammered).
- ❌ Cost: waiting reduces throughput, and if two transactions lock rows in opposite order you get **deadlocks** (fix: always acquire locks in a consistent global order, e.g. by ascending account_id).

**Optimistic locking** — *assume conflict is rare; don't lock, but detect it at write time.*
Add a `version` column. Read the row and remember its version. When you update, demand the version hasn't moved:

```sql
UPDATE accounts
SET cash_balance = 2000, version = version + 1
WHERE account_id = 42 AND version = 7;   -- 7 = the version I read
```

If someone else updated the row first, the version is now 8, your WHERE matches **0 rows** → you know you lost the race → **retry** the whole read-check-write (or fail the request). In JPA/Hibernate this is just a `@Version` field — 0 rows updated throws `OptimisticLockException`.
- ✅ Use when conflicts are **rare** (most accounts aren't being written concurrently): no waiting, no deadlocks, scales beautifully.
- ❌ Cost: under *high* contention you retry constantly and waste work.

**Which for a stock order? (be ready to commit to an answer):**
"For the **cash balance debit** I'd use pessimistic (`SELECT ... FOR UPDATE`) — it's the correctness-critical hot path and I want the second writer to *wait and see the true balance*, not retry-loop. For low-contention updates like user profile or watchlist, optimistic with `@Version` is cheaper. And in a real ledger I'd avoid in-place balance mutation entirely — append **double-entry ledger rows** and derive the balance — which sidesteps most of this." *(That last sentence is a senior-signal.)*

---

### 2.4 Indexing (guaranteed topic)

**Q: What is an index and why does it make queries fast?**
Like the index at the back of a textbook: instead of reading all 500 pages to find "ACID" (a **full table scan**, O(n)), you look it up in the sorted index and jump straight to the page. A DB index is a separate, **sorted** structure that maps column value → row location, letting the DB find rows in **O(log n)**.

**Q: How does a B-tree index work under the hood?**
Databases use a **B+ tree**: a short, *very wide* tree. Internal nodes hold sorted ranges ("keys < 100 go left, 100–500 middle, > 500 right"); **leaf nodes hold the actual keys + row pointers, and the leaves are linked left-to-right like a linked list.** Why this shape:
1. Each node is one disk page (~16KB) holding hundreds of keys, so the tree is only **3–4 levels deep even for millions of rows** → a lookup = 3–4 page reads.
2. The linked leaves make **range scans** cheap: for `WHERE price BETWEEN 100 AND 200`, find 100, then just walk right along the leaf chain.
That's why B+ trees serve `=`, `<`, `>`, `BETWEEN`, `ORDER BY`, and prefix `LIKE 'abc%'` — while a hash index only serves `=`.

**Q: If indexes are so great, why not index every column?**
Every **write** (INSERT/UPDATE/DELETE) must also update every index on the table → writes get slower, and each index eats disk/RAM. Index the columns you actually filter/join/sort on. For `orders`, that's `account_id`, `(symbol, created_at)`, maybe `status` — not the free-text memo field.

**Q: When does the DB *ignore* your index?** (favorite follow-up)
- Function on the column: `WHERE YEAR(created_at) = 2026` — the index stores raw dates, not YEAR() results. Rewrite as a range: `created_at >= '2026-01-01' AND created_at < '2027-01-01'`.
- Leading wildcard: `LIKE '%pay'` (sorted order is useless if the prefix is unknown).
- Low selectivity: `WHERE status = 'FILLED'` when 95% of rows are FILLED — a scan is genuinely cheaper, the optimizer knows it.
- **Composite index, skipped left column:** an index on `(symbol, created_at)` is like a phone book sorted by *last name, then first name*. `WHERE symbol='AAPL' AND created_at>...` ✅. `WHERE created_at>...` alone ❌ — you can't use a phone book to search by first name. This is the **leftmost-prefix rule**.

**Q: Clustered vs non-clustered index?**
- **Clustered:** the table's rows are *physically stored in the index order* — the leaf nodes ARE the rows. One per table (it *is* the table). In InnoDB, the primary key is the clustered index.
- **Non-clustered (secondary):** a separate structure whose leaves hold the indexed value + a pointer (in InnoDB: the PK value), requiring a second lookup into the clustered index to fetch the full row.
- **Covering index** bonus: if the index itself contains every column the query needs (`SELECT symbol, created_at ... WHERE symbol=?` on index `(symbol, created_at)`), the second lookup is skipped entirely.

**Q: How do you find and fix a slow query?**
Run `EXPLAIN <query>` — the DB shows its plan: full scan or index? which index? estimated rows? Typical fixes, in order: add/fix an index for the WHERE/JOIN columns → rewrite the query so the index is usable (see above) → SELECT only needed columns → paginate → then bigger guns (denormalize, cache, read replica).

---

### 2.5 SQL vs NoSQL + the questions around it

**Q: SQL vs NoSQL — differences and when to use which?**
- **SQL (relational — MySQL, PostgreSQL):** fixed schema, tables + JOINs, strong ACID transactions, vertical scaling first. Pick when data is relational and **correctness is non-negotiable** — accounts, orders, ledger. *All the money at PPSEC lives here.*
- **NoSQL** families: **document** (MongoDB — flexible JSON blobs), **key-value** (Redis — caching, sessions), **wide-column** (Cassandra — huge write volume), **graph** (Neo4j). Pick for flexible schemas, massive horizontal scale, and access patterns that don't need cross-record transactions — e.g. price-tick history, notifications, session cache.
- Honest framing: it's not either/or — a brokerage uses PostgreSQL/MySQL for money **and** Redis for hot quotes **and** maybe Cassandra/ClickHouse for tick history.

**Q: What is the N+1 problem?** (JPA/Hibernate — you use Spring, they may probe)
You load 100 accounts (1 query), then access `account.getOrders()` on each — lazy loading fires **100 more queries**. 1+N total, and the DB dies a death of a thousand cuts. Fix: fetch in one go — `JOIN FETCH` in JPQL, `@EntityGraph`, or batch fetching. Detect it by logging SQL in dev and watching for repeated identical queries.

**Q: What is a deadlock in the DB and how do you handle it?**
Txn A locks row 1 and wants row 2; txn B locks row 2 and wants row 1 — both wait forever. The DB detects the cycle and **kills one** (you get a deadlock exception). Prevention: lock rows in a **consistent global order** (e.g., always lower account_id first in a transfer), keep transactions short, index your WHERE clauses so you lock rows not ranges. Handling: catch and **retry** the victim transaction — deadlock errors are expected noise in a busy OLTP system, not a crash.

**Also skim:** read replicas & replication lag (you might read your own write as stale — route read-after-write to primary), sharding by account_id (and why cross-shard transactions hurt), connection pooling (HikariCP — why "open a connection per request" kills the DB).

---

## 3. Data Structures ⭐⭐ (HR named this second)

For each DS, be able to say: **what it is → how it's laid out in memory → Big-O of each operation → when you'd pick it**. "Under the hood" is the house style.

### 3.0 The Big-O cheat table (memorize)

| Structure | Access | Search | Insert | Delete | Notes |
|---|---|---|---|---|---|
| Array | O(1) | O(n) | O(n) | O(n) | insert/delete = shift elements |
| ArrayList (amortized) | O(1) | O(n) | O(1) end / O(n) middle | O(n) | doubles capacity when full |
| LinkedList | O(n) | O(n) | O(1)* | O(1)* | *if you already hold the node |
| Stack / Queue / Deque | — | — | O(1) | O(1) | push/pop, offer/poll |
| HashMap | — | O(1) avg | O(1) avg | O(1) avg | O(log n) worst (treeified bucket) |
| Heap (PriorityQueue) | O(1) peek | O(n) | O(log n) | O(log n) poll | min at root |
| BST (balanced, TreeMap) | — | O(log n) | O(log n) | O(log n) | sorted order for free |

### 3.1 Array vs LinkedList (the classic opener)

**Array / `ArrayList`:** one contiguous block of memory. `arr[i]` is instant — the computer literally computes `base_address + i × element_size` and jumps there: **O(1) access**. But inserting in the middle means shifting everything after it right: **O(n)**. `ArrayList` = a resizable array: when full it allocates a new array ~1.5–2× bigger and copies — occasionally O(n), but **amortized O(1)** appends (the expensive copy is rare enough that the average stays constant).

**LinkedList:** nodes scattered in memory, each holding `value + next` (+ `prev` if doubly-linked). To reach index 5 you must hop node-to-node: **O(n) access**. But if you're already *at* a node, splicing one in/out is just re-pointing two references: **O(1)**.

**Real talk for the interview:** in modern Java you almost always use `ArrayList` — contiguous memory is **CPU-cache-friendly** (the next element is already loaded), while LinkedList nodes cause a cache miss per hop. `LinkedList` earns its keep mainly as a **Deque** or when a design needs O(1) splice with a held node reference — e.g., an **LRU cache** = HashMap + doubly-linked list (that's LeetCode *LRU Cache*, a realistic live-coding ask).

### 3.2 HashMap under the hood ⭐ (asked at PayPay very often — be fluent)

Walk through what happens on `map.put("AAPL", 150)`:

1. Java calls `"AAPL".hashCode()` → an int, e.g. `2000560`. HashMap then *spreads* it (XORs high bits into low bits) so weak hash functions still distribute well.
2. Bucket index = `hash & (capacity − 1)` — a fast bitwise modulo (works because capacity is always a **power of 2**). Say index 12.
3. **Bucket 12 empty?** → place a new Node(key, value) there. Done.
4. **Bucket 12 occupied?** — a **collision** (two different keys, same index — inevitable by pigeonhole). Walk the chain: for each node, if `hash` matches **and** `equals()` says the keys are the same key → **overwrite** value; otherwise append a new node to the chain.
5. **Java 8+ twist:** if one bucket's chain exceeds **8 nodes** (and table size ≥ 64), the list converts to a **red-black tree** → that bucket's worst case drops from O(n) to **O(log n)**. (Defends against hash-flooding attacks too.)
6. **Resize:** when `size > capacity × 0.75` (the **load factor**), capacity doubles and every entry is rehashed into the new table — O(n), amortized away. 0.75 is the tuned sweet spot between wasted space and collision rate.

`get(key)` = steps 1–2, then walk the bucket comparing with `equals()`.

**The follow-up trap — the `equals`/`hashCode` contract:** *equal objects MUST have equal hashCodes.* If you override `equals` but not `hashCode`, two "equal" keys can hash to **different buckets** — you `put` under one and `get` under the other returns `null`. Also: **never mutate a field of an object being used as a key** — its hashCode changes, and it's now sitting in the wrong bucket, effectively lost.

**HashMap vs Hashtable vs ConcurrentHashMap:**
- `HashMap` — not thread-safe; allows one null key. Single-threaded / externally-synchronized use.
- `Hashtable` — legacy (Java 1.0); every method `synchronized` on one lock → threads serialize. No nulls. **Don't use; say "legacy" in the interview.**
- `ConcurrentHashMap` — thread-safe with **fine-grained** concurrency: reads are lock-free (volatile), writes CAS into empty buckets or `synchronized` on just the **first node of that one bucket** (Java 8+; the old "16 segments" design is gone). No nulls (a null `get` would be ambiguous: absent or stored-null?). **The production choice.** Note: per-operation safety ≠ atomic compound ops — use `compute`/`merge`/`putIfAbsent`, not `get`-then-`put`.

### 3.3 Stack & Queue

- **Stack** — LIFO, like a stack of plates: `push`/`pop`/`peek`, all O(1). *Where you meet it:* the **call stack** (why infinite recursion → `StackOverflowError`), undo history, matching brackets (LC *Valid Parentheses*), DFS-without-recursion, expression evaluation.
- **Queue** — FIFO, like a convenience-store line: `offer`/`poll`/`peek`, O(1). *Where:* **BFS**, producer-consumer buffers, and conceptually every message queue (Kafka) — and a brokerage matching engine processes orders **in arrival order** = a queue, often price-time priority = priority queues per price level.
- **Deque** (`ArrayDeque`) — O(1) at both ends. Idiomatic Java: use `ArrayDeque` for **both** stacks and queues, not `java.util.Stack` (legacy, synchronized) and not `LinkedList`.

### 3.4 Heap / PriorityQueue

A heap answers one question repeatedly and fast: **"what's the current min (or max)?"**
- It's a **complete binary tree** with the rule: every parent ≤ its children (min-heap). Only root is guaranteed globally smallest — it is *not* fully sorted.
- Stored inside a plain array, no node objects: children of index `i` live at `2i+1` and `2i+2`.
- `peek` O(1). `offer`: append at the end, **sift up** (swap with parent while smaller) — O(log n). `poll`: take root, move last element to root, **sift down** — O(log n).
- Java: `PriorityQueue<Integer> pq = new PriorityQueue<>();` (min-heap by default); max-heap: `new PriorityQueue<>(Comparator.reverseOrder())` or `(a,b) -> b - a`.
- *When:* "top K" problems (keep a size-K min-heap → O(n log K)), merging K sorted lists, Dijkstra, schedulers — and an **order book**: best bid = max-heap of buys, best ask = min-heap of sells. Perfect PPSEC talking point.

### 3.5 Trees — BST, balanced trees, and friends

- **Binary tree:** each node ≤ 2 children. Know the traversals: **inorder** (left-root-right — yields **sorted order** in a BST), preorder, postorder, and **level-order** (BFS with a queue).
- **BST rule:** left subtree < node < right subtree → search by comparing and going left/right, O(log n)… **only if balanced**. Insert 1,2,3,4,5 in order and you get a linked list in disguise — O(n). That's *why* self-balancing trees exist.
- **Red-black tree:** a BST that rebalances itself on every insert/delete via rotations, guaranteeing O(log n) always. You don't implement it — you *name where Java uses it*: `TreeMap`/`TreeSet`, and HashMap's treeified buckets.
- **TreeMap vs HashMap** (nice compare-contrast answer): HashMap O(1) but no order; TreeMap O(log n) but keys always sorted + range queries (`firstKey`, `headMap`, `ceilingKey`). *A price-ordered order book side is exactly a TreeMap.*
- **B+ tree** — see §2.4; connecting "trees" to "DB index" in one interview is a strong move.
- **Trie** (bonus): character-tree for prefix search — stock **ticker autocomplete** ("AA…" → AAPL, AAL) — O(prefix length) lookup.

### 3.6 Graphs

- **What:** nodes + edges; directed or undirected, weighted or not. Model anything relational: payment flows, service dependency graphs, social follows.
- **Representations:** **adjacency list** (`Map<Node, List<Node>>` — the default, O(V+E) space) vs **adjacency matrix** (V×V grid — O(1) edge check but O(V²) space; only for dense/small graphs).
- **BFS** — queue, explores in rings outward; finds **shortest path in unweighted graphs**; O(V+E).
- **DFS** — recursion or explicit stack, dives deep first; for connectivity, cycle detection, flood fill, topological sort; O(V+E).
- Always carry a **visited set** or you'll loop forever on a cycle — saying this unprompted signals experience.
- A **2D grid is a graph**: each cell is a node, neighbors are up/down/left/right. That's exactly *Number of Islands* (§4.4) — which PayPay actually asked.

### 3.7 "Which data structure would you use for…?" (their favorite question format)

| Scenario | Answer | Why |
|---|---|---|
| Fast lookup by key (session by user ID) | HashMap | O(1) average |
| Uniqueness check (processed order IDs) | HashSet | O(1) contains |
| Keys in sorted order / range queries (order book prices) | TreeMap | O(log n), sorted iteration |
| Top-K / always need min or max (best bid/ask) | Heap / PriorityQueue | O(log n) insert, O(1) peek |
| Process in arrival order (incoming orders) | Queue | FIFO fairness |
| Undo / nesting / most-recent-first | Stack | LIFO |
| LRU cache | HashMap + doubly-linked list | O(1) get + O(1) move-to-front |
| Prefix search (ticker autocomplete) | Trie | O(len) per lookup |
| Fast random access, iterate a lot | ArrayList | O(1) index, cache-friendly |

---

## 4. Algorithms + Confirmed Problems ⭐⭐ (HR named this third)

### 4.0 Big-O in one breath (they may literally ask "what is Big-O?")

Big-O describes **how running time (or memory) grows as input n grows**, ignoring constants — it's about the *shape* of growth, not the milliseconds. The ladder, fastest to slowest: **O(1)** (HashMap get) → **O(log n)** (binary search — halving) → **O(n)** (one pass) → **O(n log n)** (good sorts) → **O(n²)** (nested loops) → **O(2ⁿ)** (naive subsets/fib). Rules of thumb: drop constants and lower-order terms (O(2n+10) = O(n)); sequential steps add (keep the bigger), nested loops multiply. For n = 10⁶: O(n²) = 10¹² steps ≈ minutes — too slow; that's how you sanity-check an approach before coding it.

### 4.1 Binary search (their pet topic — a confirmed problem is binary search)

**Idea:** in a **sorted** array, compare with the middle; the answer can only be in one half — discard the other. Halving repeatedly → **O(log n)** (a million elements = ~20 comparisons).

```java
int lo = 0, hi = arr.length - 1;
while (lo <= hi) {
    int mid = lo + (hi - lo) / 2;   // not (lo+hi)/2 → avoids int overflow
    if (arr[mid] == target) return mid;
    else if (arr[mid] < target) lo = mid + 1;   // target is in the right half
    else hi = mid - 1;                          // target is in the left half
}
return -1;
```

The three classic bugs (say them out loud while coding — free points): overflow in mid; `<=` vs `<` in the loop condition; forgetting `+1`/`−1` → infinite loop.

### 4.2 Sorting — what to know (full detail in [[Sort_Algo_Notes]] if linked)

Know the comparison table and *why*, not every implementation:

| Algorithm | Avg | Worst | Space | Stable? | One-liner |
|---|---|---|---|---|---|
| Bubble/Insertion | O(n²) | O(n²) | O(1) | ✅ | insertion is great on tiny/nearly-sorted input |
| Merge sort | O(n log n) | O(n log n) | O(n) | ✅ | split in half, sort halves, merge |
| Quick sort | O(n log n) | O(n²) rare | O(log n) | ❌ | partition around a pivot |
| Heap sort | O(n log n) | O(n log n) | O(1) | ❌ | build heap, pop repeatedly |

**Stable** = equal elements keep their original relative order — *matters in finance:* sorting orders by price must not scramble the time-priority of same-price orders.
**Java trivia they like:** `Arrays.sort(int[])` = dual-pivot quicksort (primitives — stability meaningless); `Arrays.sort(Object[])` / `Collections.sort` = **TimSort** (merge-sort-based, stable, exploits already-sorted runs). And "why is sorting O(n log n) at best?" → comparison sorts must distinguish n! orderings → log₂(n!) ≈ n log n comparisons minimum.

### 4.3 The pattern toolkit (what actually shows up at Easy→Medium)

- **HashMap for lookup** — "have I seen this before?" in O(1). (Two Sum, duplicates, anagrams.)
- **Two pointers** — sorted array or string from both ends. (Palindrome check — *reported at PayPay*.)
- **Sliding window** — "longest/shortest substring/subarray with property X."
- **BFS/DFS** — grids and graphs. (Number of Islands — *confirmed*.)
- **Binary search** — sorted anything, or "minimize the maximum" answers. (*Confirmed*.)
- **Simple DP** — one medium DP was reported; know climbing-stairs / house-robber style 1-D DP.

### 4.4 The three confirmed problems, solved & explained

**① Two Sum** (asked in *this* fundamentals round) — find indices of two numbers summing to `target`.

Brute force = try every pair, O(n²). The insight: while scanning, for each `x` ask **"have I already seen `target − x`?"** — a HashMap answers that in O(1):

```java
public int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> seen = new HashMap<>(); // value → index
    for (int i = 0; i < nums.length; i++) {
        int need = target - nums[i];              // the partner we need
        if (seen.containsKey(need))
            return new int[]{seen.get(need), i};  // partner seen earlier → done
        seen.put(nums[i], i);                     // remember me for future elements
    }
    return new int[]{};                           // per problem statement, unreachable
}
```

One pass, **O(n) time, O(n) space**. Note the order — check *then* put — which also handles `target = 2×x` with duplicate values correctly (you never match an element with itself).

**② Find First and Last Position of Element in Sorted Array** (LC 34, confirmed) — sorted array, find the first and last index of `target` in **O(log n)**.

Sorted + O(log n) demanded → binary search, but twice: once **biased left** (keep searching left even after a hit) to find the first occurrence, once **biased right** for the last:

```java
public int[] searchRange(int[] nums, int target) {
    return new int[]{ bound(nums, target, true), bound(nums, target, false) };
}

private int bound(int[] nums, int target, boolean first) {
    int lo = 0, hi = nums.length - 1, ans = -1;
    while (lo <= hi) {
        int mid = lo + (hi - lo) / 2;
        if (nums[mid] == target) {
            ans = mid;                            // record hit, but keep going…
            if (first) hi = mid - 1;              // …left for the FIRST one
            else       lo = mid + 1;              // …right for the LAST one
        }
        else if (nums[mid] < target) lo = mid + 1;
        else hi = mid - 1;
    }
    return ans;
}
```

Two O(log n) searches = **O(log n) total, O(1) space**. Edge cases to mention: empty array, target absent → `[-1,-1]` (handled — `ans` stays −1).

**③ Number of Islands** (LC 200, confirmed) — grid of `'1'` land / `'0'` water; count islands (groups of 1s connected up/down/left/right).

Mental model: scan every cell; when you hit unvisited land, that's a **new island** — count it, then **flood-fill** (DFS) the whole island to mark it visited so you never count it again. Marking = overwrite `'1'` with `'0'` (sink it), which doubles as the visited set:

```java
public int numIslands(char[][] grid) {
    int count = 0;
    for (int r = 0; r < grid.length; r++)
        for (int c = 0; c < grid[0].length; c++)
            if (grid[r][c] == '1') {   // new, unvisited island found
                count++;
                sink(grid, r, c);      // erase it entirely
            }
    return count;
}

private void sink(char[][] g, int r, int c) {
    if (r < 0 || r >= g.length || c < 0 || c >= g[0].length || g[r][c] != '1')
        return;                        // off-grid or water/already-sunk → stop
    g[r][c] = '0';                     // mark visited by sinking
    sink(g, r + 1, c);  sink(g, r - 1, c);
    sink(g, r, c + 1);  sink(g, r, c - 1);
}
```

**O(rows × cols) time** (each cell touched a constant number of times), **O(rows × cols) worst-case stack** (all-land grid → deep recursion). Strong follow-up answers: BFS with an `ArrayDeque` avoids stack overflow on huge grids; "don't mutate the input" → separate `boolean[][] visited`; **Union-Find** solves it too and handles the dynamic "islands appear over time" variant (LC 305).

**Also reported at PayPay:** longest palindromic substring (expand around center), palindrome check (two pointers), is-a-SumTree, plain binary search, one medium DP.

---

## 5. Live Coding — how to behave in the room

HR explicitly warned about live coding. The evaluation is ~50% communication:

1. **Restate the problem** in your own words + confirm with an example. Catches misunderstandings for free.
2. **Ask clarifying questions:** empty input? duplicates? negatives? sorted? expected size of n (drives target complexity)? return value vs print?
3. **Say the brute force first**, with its Big-O — *then* improve. "O(n²) works, but with a HashMap we can do one pass in O(n)" is exactly the sentence they want to hear. Never dive into code silently.
4. **Narrate while typing.** Silence reads as being stuck.
5. **Test before declaring done:** trace one normal case *line by line* out loud, then edge cases (empty, size 1, all-same, target absent).
6. State **time and space complexity** unprompted at the end.
7. If stuck: say what you're stuck on, work a tiny example by hand on "paper", or ask for a hint — asking beats freezing.
8. **Java hygiene** since you're presenting as a Java backend engineer: real generics (`Map<Integer, Integer>`), `ArrayDeque` not `Stack`, meaningful names, know `StringBuilder` for string building in a loop.

---

## 6. Java / JVM — likely probes given your Java profile

**Q: When does an object become eligible for garbage collection?**
When it's **no longer reachable** from any *GC root*. GC roots = places the JVM can always see: local variables on live thread stacks, static fields, JNI references. If no chain of references leads from a root to your object, it's garbage — even if objects point at *each other* ("island of isolation": A↔B reference each other, but nothing from a root reaches either → both collected; this is why JVM GC is reachability-based, not reference-counting).

**Q: How does GC work in the JVM?** (build the answer in layers)
1. **Premise — the generational hypothesis:** most objects die young (a request's temp objects are garbage milliseconds later), so the heap is split by age.
2. **Young generation** = Eden + two Survivor spaces. New objects go to Eden; when Eden fills → **minor GC**: copy the few *survivors* to a Survivor space, wipe Eden wholesale. Fast, because copying a few live objects beats scanning many dead ones.
3. Objects surviving several minor GCs are **promoted** to the **old generation**; when it fills → **major/full GC** — much more expensive.
4. Mechanics: **mark** (walk references from GC roots, flag live objects) → **sweep** (reclaim the rest) → **compact** (defragment).
5. Collectors: **G1** (default since Java 9 — region-based, targets pause goals), **ZGC/Shenandoah** (sub-millisecond pauses on huge heaps).
6. The catch — **stop-the-world pauses**: application threads freeze during (parts of) GC. Land it in domain: *a multi-second full GC in an order-execution path means orders miss a moving market price — so a trading system tunes for pause time (ZGC/G1) over raw throughput.*

**Q: Process vs Thread?**
A **process** = a running program with its **own private memory space** (your Spring Boot app). A **thread** = an execution lane *inside* a process; threads **share the heap** but each has its own stack and program counter. Processes are isolated and expensive to create/switch; threads are cheap — and the shared heap is precisely *why* thread bugs (races) exist and processes don't have that problem.

**Q: Race condition vs Data race?** (subtle — nailing it impresses)
- **Data race** — *memory-level*: two threads touch the same variable concurrently, at least one writing, with **no synchronization**. Undefined behavior; `counter++` from two threads (it's read-modify-write, not atomic) losing increments.
- **Race condition** — *logic-level*: correctness depends on **timing/interleaving**. Classic: `if (balance >= amount) { withdraw(amount); }` — even if each step is individually synchronized (no data race!), two threads can both pass the check before either withdraws → overdraft. **Check-then-act must be atomic as a unit** — one lock around both, or `SELECT ... FOR UPDATE` when the state lives in the DB (same disease as §2.3, different organ).

**Q: Deadlock / Livelock / Starvation?**
- **Deadlock:** circular waiting — A holds lock 1 wants 2, B holds 2 wants 1; nobody moves. Requires all four **Coffman conditions** (mutual exclusion, hold-and-wait, no preemption, circular wait) — break any one; the practical fix is a **global lock ordering** (always lock the lower account-ID first in a transfer).
- **Livelock:** threads stay *active* but make no progress — endlessly reacting to each other, like two people side-stepping the same way in a corridor forever. Fix: randomized backoff.
- **Starvation:** one thread never gets the resource because others always win (unfair lock, priorities). Fix: fair locks (`new ReentrantLock(true)`), fair scheduling.

**Q: `volatile` vs `synchronized`?**
- `volatile` guarantees **visibility** (a write is immediately seen by other threads — no core-local caching) and ordering, but **not atomicity** — `volatile int counter; counter++` is still broken (read-modify-write).
- `synchronized` guarantees **both**: mutual exclusion + visibility at monitor exit/enter.
- Rule: `volatile` for a simple flag one thread writes and others read (`volatile boolean shutdown`); `synchronized`/`ReentrantLock` for compound actions; `AtomicInteger` (CAS) for counters.

**Q: How does Spring IoC / DI work?**
**Inversion of Control:** you don't `new` your dependencies — the Spring **container** creates and wires them ("don't call us, we'll call you"). At startup, component scanning finds `@Component/@Service/@Repository/@Controller`, builds bean definitions, instantiates beans, resolves the dependency graph in order, and **injects** dependencies — **constructor injection preferred** (dependencies explicit, final, trivially testable with mocks). `@Autowired` on fields works but hides dependencies and hurts testability. One runtime follow-up: default bean scope is **singleton** — one shared instance → your beans should be **stateless**, or you've built a race condition (ties back to threading, and they like cross-topic candidates).

**Also warm:** `String` immutability & the string pool, `equals`/`hashCode` contract (§3.2), `final`, `ExecutorService`/thread pools (why not raw `new Thread()` per task), `CompletableFuture` for async composition, checked vs unchecked exceptions (+ `@Transactional` rolls back on unchecked by default — nice DB×Java crossover).

---

## 7. Networking / Backend — secondary for this round, keep warm

**Q: "What happens when you type a URL and press Enter?"** ⭐ (asked in almost every PayPay loop — keep a crisp 3-minute version)
1. **DNS:** browser/OS cache first, else resolver → root → TLD (`.com`) → authoritative server → the IP address.
2. **TCP handshake** with that IP on port 443: SYN → SYN-ACK → ACK (both sides agree a connection exists).
3. **TLS handshake:** server presents its certificate, browser validates it against trusted CAs, they agree on session keys → everything after is encrypted.
4. **HTTP request:** method, path, headers, cookies.
5. **Server side:** load balancer → app server → cache/DB → builds the **HTTP response** (status code + body).
6. **Render:** parse HTML → DOM, fetch CSS/JS/images (repeating the above), execute JS, paint.
Bonus mentions: CDN, connection keep-alive, HTTP/2 multiplexing.

**Rapid-fire one-liners to have loaded:**
- **TCP vs UDP:** TCP = reliable, ordered, connection-based (handshake, retransmit) — APIs, order placement. UDP = fire-and-forget datagrams, faster, may drop/reorder — live price *ticks* can tolerate a lost update (the next tick supersedes it), an *order* cannot.
- **HTTP status classes:** 2xx success (200/201), 3xx redirect (301/304), 4xx *client's* fault (400/401 unauthenticated/403 forbidden/404/409 conflict/429 rate-limited), 5xx *server's* fault (500/502/503/504).
- **Idempotency** ⭐: same request applied twice = effect of once. GET/PUT/DELETE idempotent by contract; POST isn't — so payment/order APIs take an **idempotency key**: client sends a unique key, server stores it, a retry with the same key returns the saved response instead of buying the stock twice. *Say this in a brokerage context — it's the single highest-value networking concept for PPSEC.*
- **HTTP vs HTTPS:** HTTPS = HTTP over TLS — encryption (no snooping), integrity (no tampering), authentication (server identity).
- **REST basics:** resources as URLs, verbs as methods, stateless requests.

---

## 8. Why PPSEC? ⭐ (HR explicitly said they'll ask — script it)

Structure it Japan-style: **conclusion → reasons → example → conclusion**, 60–90 seconds. Build from these true ingredients:

1. **Domain continuity, one step deeper:** *"At Rakuten Pay I've spent two years building payment backends in Java/Spring Boot — money movement, idempotency, transactional correctness. Securities is the same discipline with the bar raised: orders against a moving market, ledgers, settlement, regulatory audit. It's the natural next depth level for my exact skill set."*
2. **Mission you can honestly endorse:** PPSEC's whole product is **lowering the barrier to investing** for ordinary people in Japan — fractional US/JP stocks from tiny amounts, right inside an app 73M+ people already use, in a country pushing household savings toward investment (new NISA). If that genuinely resonates, say it plainly.
3. **Growth-stage engineering:** the PayPay group just listed on **Nasdaq (PAYP, March 2026)** and is building out the "financial super-app" — payments + card + bank + securities. Joining Securities now = building core systems while they scale, not maintaining finished ones.
4. **Why leave Rakuten Pay?** (the implicit twin question — never badmouth): *"Rakuten Pay taught me production payments engineering. I'm not leaving payments — I'm moving to the part of fintech I want to go deep in: investment infrastructure, and PPSEC is where that's being built at scale in Japan."*

Also prepare **reverse questions** (you'll be asked "any questions for us?"): e.g. *"How is the boundary drawn between PPSEC's backend and the main PayPay app for the 資産運用 flow?"* / *"What does the team own end-to-end — order path, ledger, market data?"* / *"How do engineers here handle FSA-driven requirements — is compliance a separate team or built into the dev process?"* These show you researched the *Securities* business specifically, not just PayPay.

---

## 9. System Design (likely a LATER round — skim now, cram after you pass this one)

**The exact prompt from a past candidate: "design PayPay itself, scope = interacting with the third party (bank) — availability, DB read/write performance, scaling, security."** For Securities expect a brokerage framing. Have three sketches ready:
1. **Stock Order System** (most likely) — placement → validation → matching/routing → execution → settlement.
2. **Wallet / Payment Ledger** — double-entry, idempotency, balance consistency.
3. **Real-Time Price Feed** — WebSocket/pub-sub fan-out, low latency.

**Framework (time management is the #1 reported failure — practice out loud, timed):**
Clarify requirements & scale (~5 min) → APIs (`placeOrder`, `getPortfolio`) → data model (accounts, holdings, orders, ledger entries) → architecture (API GW → order service → matching → ledger → DB + cache + queue) → deep-dive the money-critical part (**consistency & idempotency**) → scale (read replicas, shard by account, cache, Kafka async) → reliability & security (audit trail, encryption, reconciliation with bank/exchange).

**Fintech vocabulary that signals domain seriousness:** double-entry ledger (never mutate a balance in place) · idempotency keys · Saga with compensating actions over 2PC · outbox pattern · exactly-once vs at-least-once · strong consistency on money paths · reconciliation & immutable audit logs · CQRS/read replicas for the read path.

---

## 10. Behavioral / HR (final round — for reference)

Assesses **personal values, work approach, growth mindset**. Reported questions:
- **"What's the biggest mistake you've ever made? What lesson did you learn? If you could go back, what would you do differently?"** (asked verbatim)
- Deep-dive on **a hard problem you solved** — expect follow-ups probing real depth.
- Biggest weakness? · **"What is something no one can beat you at?"**
- Why PPSEC / why leave Rakuten Pay? (→ §8)
- Expected salary + earliest join date (HR screen).

Prep: STAR + **conclusion → reason → example → conclusion** framing. Lean on Rakuten Pay payments war stories — they're directly relevant currency here.

---

## 11. Quick Prep Checklist (interview on the 10th)

**Priority = exactly what HR listed:**
- [ ] **DB:** ACID with the buy-a-stock example → isolation levels table + all 3 anomalies as stories → optimistic vs pessimistic with the two-orders-¥8,000 example → B+ tree index + when indexes get ignored. Explain each *out loud, in the context of a stock order*.
- [ ] **DS:** HashMap `put()` walkthrough (hash → bucket → collision → treeify → resize) + `equals`/`hashCode` trap · heap mechanics · array vs linked list · the "which DS for scenario X" table.
- [ ] **Algo:** re-solve **Two Sum, First & Last Position, Number of Islands** in Java from scratch — these were literally asked. Binary search bugs (overflow, off-by-one) nameable on demand.
- [ ] **Live coding drill:** one Easy + one Medium, *talking the whole time* — restate, clarify, brute force + Big-O, improve, code, trace a test, state complexity.
- [ ] **Why PPSEC:** 60–90s answer rehearsed (conclusion → reasons → example → conclusion) + 2–3 reverse questions about the Securities business.
- [ ] Warm: GC in layers, race vs data race, `volatile` vs `synchronized`, Spring DI, URL-to-page in 3 min, idempotency-key story.
- [ ] Know your **expected salary + join date**.

---

## Sources

**Interview experiences (PayPay group):**
- [PayPay Japan | Entire interview — LeetCode Discuss](https://leetcode.com/discuss/interview-experience/1806599/PayPay-Japan-or-Entire-interview/) *(the 5-part breakdown + exact LC problems)*
- [PayPay Japan Backend Online Code Challenge 2 — LeetCode](https://leetcode.com/discuss/interview-question/1490866/PayPay-Japan-Backend-Online-Code-Challenge-2)
- [PayPay SWE Interview — Glassdoor](https://www.glassdoor.com/Interview/PayPay-Software-Engineer-Interview-Questions-EI_IE3735809.0,6_KO7,24.htm)
- [PayPay Backend Engineer — Glassdoor](https://www.glassdoor.com/Interview/PayPay-Backend-Engineer-Interview-Questions-EI_IE3735809.0,6_KO7,23.htm)
- [PayPay Interview Questions — InterviewBit](https://www.interviewbit.com/paypay-interview-questions/)
- [PayPay Tokyo Interview Experience — Medium (Niladri)](https://medium.com/@niladribhusandalai/paypay-tokyo-interview-experience-23bf43f9e31d)
- [SWE Interview Experience with PayPay Japan — gaijineer](https://gaijineer.co/software-engineer-interview-experience-with-paypay-japan)
- [PayPay/PayPayカード SWE 面接対策 — InterviewCat](https://jobs.interviewcat.dev/blog/paypay-software-engineer-interview)

**PPSEC / company facts:**
- [PayPay Securities Company Profile — PitchBook](https://pitchbook.com/profiles/company/170720-20)
- [PayPay Corporation (PAYP) — Yahoo Finance](https://finance.yahoo.com/quote/PAYP/)
- [PayPay IPO overview — Capital.com](https://capital.com/en-int/learn/ipo/paypay-ipo)
- [PayPay: Japan's Emerging Financial Super App — Seeking Alpha](https://seekingalpha.com/article/4914396-paypay-japans-emerging-financial-super-app)