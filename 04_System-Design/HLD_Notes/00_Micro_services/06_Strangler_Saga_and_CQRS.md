# Part 6: Strangler Pattern, Saga Pattern & CQRS

This note covers three very important microservices design patterns from the data management phase:
1. **Strangler Pattern** — How to safely migrate a monolith to microservices
2. **Saga Pattern** — How to handle transactions across multiple databases
3. **CQRS** — How to handle cross-database queries

---

## 1. Strangler Pattern (Migrating Monolith → Microservices)

### When is it used?
When you already have an existing monolith and you want to **gradually refactor** it into microservices — without stopping traffic or risking a big-bang migration.

### The Problem with a Direct Migration
You **cannot** convert an entire monolith to microservices in one shot. The monolith can have hundreds of flows, and converting everything at once carries enormous risk. Any bug will crash the entire system.

### The Solution: The Strangler Pattern
Place a **Controller (like an API Gateway or Proxy)** in front of both the old monolith and the new microservice. This controller decides what percentage of traffic goes where.

```
         All Requests
              │
              ▼
       ┌─────────────────┐
       │   CONTROLLER    │
       │  (Traffic Split)│
       └──────┬──────────┘
              │
   ┌──────────┴──────────────┐
   │ 90%                     │ 10%
   ▼                         ▼
[ Old Monolith ]       [ New Microservice ]
```

### How the Migration Works Step-by-Step:
1. **Start small:** A specific flow in the monolith is converted into a new microservice.
2. **Route 10% traffic** to the new microservice. Monitor for bugs or failures.
3. **If the microservice fails:** Roll back immediately by sending 0% traffic to it. Fix the bug and restart.
4. **If it's stable:** Increase traffic to 20%, 30%, 50%... slowly over time.
5. **Once 100% traffic** is served by the microservice, the monolith's corresponding module can be removed.
6. Repeat this for each module until the monolith is completely "strangled" (phased out).

```
   Phase 1:   [Monolith 100%]  [Microservice 0%]   ← Start
   Phase 2:   [Monolith 90%]   [Microservice 10%]  ← Test
   Phase 3:   [Monolith 50%]   [Microservice 50%]  ← Growing confidence
   Phase 4:   [Monolith 0%]    [Microservice 100%] ← Complete!
   Phase 5:   [Monolith DELETED]                   ← Done!
```

**Key benefit:** You have a **safety switch** at every stage. If anything breaks, you can redirect traffic back to the monolith instantly.

---

## 2. Database Patterns: Shared vs. Database-per-Service

Before understanding Saga, we need to understand why we use separate databases for each service.

### Option A: Shared Database (Avoid in Microservices!)
All microservices share a single common database.

```
   ┌────────────┐  ┌────────────┐  ┌────────────┐
   │  Service 1 │  │  Service 2 │  │  Service 3 │
   └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
         │               │               │
         └───────────────┼───────────────┘
                         ▼
                 [ Shared Database ]
```

**Why this fails at scale:**
1. **Scaling is wasteful:** If Service 2 gets heavy traffic and needs a bigger database, you have to scale the entire shared database (which serves all services) even though only Service 2 needs it.
2. **Schema changes are dangerous:** If Service 3 wants to delete a column, it must check if Service 1 and Service 2 also use it. This creates tight coupling at the database level.
3. **One team blocks another:** Service 1's heavy query can lock the database and starve Service 2.

**When it's okay to use:** Only for small applications or early-stage startups where simplicity matters more than scale.

---

### Option B: Database-per-Service ✅ (Recommended)
Every service owns and controls its own dedicated database. No other service can access another service's database directly.

```
   ┌────────────┐  ┌────────────┐  ┌────────────┐
   │  Service 1 │  │  Service 2 │  │  Service 3 │
   └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
         ▼               ▼               ▼
      [ DB 1 ]        [ DB 2 ]        [ DB 3 ]
    (SQL/Postgres)   (MongoDB)      (Cassandra)
```

**Rule:** If Service 1 needs data from Service 3's database, it must **call Service 3's API** — it cannot query Service 3's database directly.

**Benefits:**
1. ✅ Each service can use the database that best fits its needs (SQL, NoSQL, etc.)
2. ✅ Schema changes in one service do not affect any other service.
3. ✅ Each database can be independently scaled based on its own traffic needs.

**Two new challenges this creates:**
1. **Cross-service transactions** → Solved by **Saga Pattern**
2. **Cross-database joins** → Solved by **CQRS**

---

## 3. The Saga Pattern (Distributed Transactions)

### The Problem
In a monolith with a single database, ACID transactions are simple:
```sql
START TRANSACTION;
UPDATE balance SET amount = amount - 10 WHERE user = 'PersonA';
INSERT INTO payment_history VALUES ('PersonA', 'PersonB', 10);
COMMIT; -- If any step fails, EVERYTHING rolls back automatically
```

In microservices with separate databases, a single transaction might span multiple services:

**Example:** Person A pays Person B ₹10
1. **Balance Service** → Deduct ₹10 from Person A's balance (DB 1)
2. **Payment Service** → Record the payment history (DB 2)

What if step 1 succeeds but **step 2 fails**?
* Person A's balance is already reduced by ₹10.
* But there is no record of the payment.
* The data is now **inconsistent**!

You cannot use a single SQL `ROLLBACK` because they are on different databases.

### The Solution: Saga Pattern
**Saga = A sequence of local transactions, where each failure triggers a compensating transaction to undo previous steps.**

```
Step 1: Balance Service deducts ₹10 → publishes "Balance Updated" event ✅
Step 2: Payment Service records payment → FAILS ❌
        → publishes "Payment Failed" event (Compensating Event)
Step 3: Balance Service listens, adds ₹10 back (Compensating Transaction) ↩️
        → Data is back to consistent state
```

### Real Example Walkthrough (Person A pays Person B ₹10):

**Happy Path (Success):**
```
Person A ─ Pay ₹10 ──► [Balance Service]
                              │ Deduct ₹10 from A's DB
                              │ Publish "Balance Deducted" event
                              ▼
                        [Payment Service]
                              │ Record payment in DB
                              │ Publish "Payment Complete" event
                              ▼
                         ✅ Transaction Done!
```

**Failure Path (Payment fails):**
```
Person A ─ Pay ₹10 ──► [Balance Service]
                              │ Deduct ₹10 from A's DB ✅
                              │ Publish "Balance Deducted" event
                              ▼
                        [Payment Service]
                              │ FAILS to record ❌
                              │ Publish "Payment FAILED" event (Compensation!)
                              ▼
                        [Balance Service]
                              │ Listens to "Payment FAILED"
                              │ Adds ₹10 BACK to A's balance (Rollback!)
                              ▼
                         ✅ Data Consistent Again
```

---

## 4. Two Ways to Implement Saga

### A. Choreography (Event-Driven — No Central Brain)
Services communicate by listening to and publishing events. There is **no central coordinator**. Each service is responsible for reacting to events.

```
[Service 1] ──Publish Event──► [Event Bus / Queue]
                                       │
                           ┌───────────┼───────────┐
                           ▼           ▼           ▼
                      [Service 2] [Service 3] [Service N]
                   (Listens & acts on the event)
```

**How it works:**
1. Service 1 completes its local transaction → publishes a "Success" event to the queue.
2. Service 2 listens → processes its part → publishes its own "Success" or "Failure" event.
3. If any service publishes a "Failure" event → all previous services listen and run their compensating transactions.

**Pros:**
* ✅ Fully decoupled — no service knows about the others.
* ✅ Easy to build for simple, linear workflows.

**Cons:**
* ❌ Hard to see the full picture — there is no single place to check the overall transaction status.
* ❌ **Cyclic Dependency Risk:** Service A triggers B → B triggers C → C accidentally triggers A again. This creates infinite loops!

---

### B. Orchestration (Central Coordinator — Has a Brain)
A dedicated **Orchestrator service** manages the entire workflow. It knows what steps to execute in which order and what to do if any step fails.

```
                    [Orchestrator]
                         │
          ┌──────────────┼───────────────┐
          │              │               │
          ▼              ▼               ▼
    [Service 1]    [Service 2]    [Service 3]
    "Do Step 1"   "Do Step 2"   "Do Step 3"

  If Service 3 fails:
  Orchestrator tells Service 2: "Please undo Step 2"
  Orchestrator tells Service 1: "Please undo Step 1"
```

**How it works:**
1. Orchestrator calls Service 1. Waits for reply.
2. Service 1 succeeds → Orchestrator calls Service 2.
3. Service 2 fails → Orchestrator **directly tells** Service 1 to run its compensating (rollback) transaction.

**Pros:**
* ✅ **No cyclic dependency** — the Orchestrator strictly controls the order.
* ✅ Easy to debug — just check the Orchestrator's logs to see where the transaction stopped.
* ✅ Great for complex workflows with many steps.

**Cons:**
* ❌ Orchestrator can become a **Single Point of Failure** — must be made highly available.
* ❌ Tighter coupling — Orchestrator must know the APIs of all participant services.

---

## 5. CQRS (Command Query Responsibility Segregation)

### The Problem: Cross-Database Joins
With Database-per-Service, data is split across multiple databases. What if you need to **combine data from DB 1 and DB 2** (like joining two tables)?

In a monolith:
```sql
SELECT orders.id, users.name
FROM orders
JOIN users ON orders.user_id = users.id; -- Easy, same database
```

In microservices, `orders` is in *Order DB* and `users` is in *User DB* — **you cannot do a SQL JOIN across them**.

### The Solution: CQRS
**CQRS = Separate the "Write" (Command) database from the "Read" (Query) database.**

```
   WRITE SIDE (Commands)           READ SIDE (Queries)
   ┌──────────────────┐            ┌──────────────────────────┐
   │ Order Service    │            │   Read DB (View Store)   │
   │ [Order DB]       │──────────► │ (Pre-joined / denormed   │
   │                  │  Sync      │  data for fast reads)    │
   │ User Service     │  Events    │                          │
   │ [User DB]        │            │ e.g., MongoDB, Redis     │
   └──────────────────┘            └──────────────────────────┘
            ▲                               │
      Writes go here                  Reads come here
```

**How it works:**
1. Each service still writes to its own dedicated database (no change here).
2. Events are published whenever data changes (e.g., "Order Created", "User Updated").
3. A separate **Read DB (View Store)** listens to these events and maintains a pre-joined, denormalized copy of the data specifically for query needs.
4. All complex queries (like combining order + user data) go to this Read DB — **no cross-database joins needed**.

**Key trade-off:** The Read DB is **eventually consistent**. There may be a small delay (milliseconds) between a write and when the Read DB reflects it.

---

## 6. Key Interview Questions & Answers

### Q1: What is the Strangler Pattern and why is it used?
It is a migration strategy to gradually move traffic from a monolith to microservices using a traffic controller (proxy). It avoids risky big-bang migrations and allows you to roll back instantly if something breaks.

### Q2: What is the Saga Pattern?
A Saga is a sequence of local transactions across multiple microservices. If any step fails, compensating transactions are triggered in reverse to undo previous successful steps and restore data consistency.

### Q3: Choreography vs. Orchestration — Which to choose?
* Use **Choreography** for simple, linear workflows (2–3 services). Less infrastructure needed.
* Use **Orchestration** for complex workflows (many steps, branching logic). Easier to debug and avoids cyclic dependencies.

### Q4: What is the difference between Saga and 2PC (Two-Phase Commit)?
| | Saga | 2-Phase Commit (2PC) |
| :--- | :--- | :--- |
| **Locking** | No database locking | Locks DB rows until all agree |
| **Consistency** | Eventually Consistent | Strongly Consistent |
| **Performance** | High (async, non-blocking) | Low (blocking, slow) |
| **Failure** | Uses compensating transactions | Rollback by the coordinator |

### Q5: What is CQRS and why is it needed in microservices?
CQRS separates write operations (Commands) and read operations (Queries) into different databases. It is needed because each microservice owns its own DB, making cross-service SQL JOINs impossible. A dedicated Read DB (View Store) maintains pre-joined data for fast queries.
