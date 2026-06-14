# Part 5: Microservices Data Management & The Saga Pattern

In a monolith, data consistency is simple because there is only one database. In a microservices architecture, managing data across multiple independent databases is one of the most challenging problems. 

This note explains the shift from monolith to microservices data design, why shared databases fail, and how the **Saga Pattern** solves the distributed transaction problem.

---

## 1. Recapping Monolith vs. Microservices

Before diving into databases, let's understand the trade-offs that drive this transition:

### Monolithic Architecture (Legacy/Single Unit)
All business modules (Order, Payment, Inventory) run in a single application and share **one database**.
* **Disadvantages:**
  1. **Overloaded IDE:** The code footprint is massive (often GBs), making local development tools slow.
  2. **Expensive Scaling:** If only the *Order* feature has high traffic, you must clone the entire monolithic application on new servers, wasting memory and CPU on other modules.
  3. **Slow Deployments:** A single-line bug fix requires running the entire regression test suite and redeploying the entire codebase.
  4. **Tight Coupling:** Code modules easily cross-call each other, leading to spaghetti dependencies.

### Microservices Architecture
The monolith is split into small, autonomous, loosely coupled services.
* **Advantages:** Independent scaling, faster deployments, isolated debugging.
* **Disadvantages:** Network latency, monitoring complexity, and **data consistency**.

---

## 2. Database-per-Service: Why Share is a Bad Idea

In a microservices design, each service must have its **own dedicated database** (Database-per-Service pattern). 

```
   [ Shared Database (Monolith) ]                [ Database-per-Service (Microservices) ]
       ┌─────────────────────┐                         ┌─────────────┐   ┌─────────────┐
       │   Single Database   │                         │  Order DB   │   │ Payment DB  │
       └─────▲─────────▲─────┘                         └──────▲──────┘   └──────▲──────┘
             │         │                                      │                 │
       ┌─────┴───┐ ┌───┴─────┐                         ┌──────┴──────┐   ┌──────┴──────┐
       │  Order  │ │ Payment │                         │Order Service│   │Payment Serv.│
       │ Service │ │ Service │                         └─────────────┘   └─────────────┘
       └─────────┘ └─────────┘
```

### Why we do NOT use a Shared Database in Microservices:
1. **Tight Coupling at Schema Level:** If *Order Service* updates a table schema, it can silently break the *Payment Service* that reads from the same table.
2. **Resource Lockups:** If *Order Service* locks a table for a heavy database operation, it blocks the *Payment Service* from functioning, defeating the purpose of independent services.
3. **Database Fit:** Different services need different types of databases. A *Catalog Service* might prefer a NoSQL document store (MongoDB), while a *Financial Ledger Service* requires a relational SQL database (PostgreSQL).

---

## 3. The Distributed Transaction Problem

In a monolith with a SQL database, creating an order is simple:
```sql
START TRANSACTION;
UPDATE Inventory SET quantity = quantity - 1 WHERE item_id = 101;
INSERT INTO Orders (id, status) VALUES (1, 'PENDING');
INSERT INTO Payments (order_id, amount) VALUES (1, 100);
COMMIT; -- If any line fails, everything automatically rolls back (ACID)
```

In microservices:
1. **Order Service** updates its local *Order DB*.
2. **Inventory Service** updates its local *Inventory DB*.
3. **Payment Service** updates its local *Payment DB*.

Because they are separate databases, **you cannot run a single SQL transaction across them.** If the *Payment Service* fails at the end, how do we roll back the changes that were already saved in the *Order DB* and *Inventory DB*?

---

## 4. The Solution: The Saga Pattern

A **Saga** is a sequence of local transactions. Instead of locking all databases at once, a Saga executes local transactions sequentially across services.

### Compensating Transactions (The "Rollback" Action)
If a step in the Saga fails, the system must undo the previous successful steps. Because we cannot execute a traditional database rollback, we run **Compensating Transactions** in reverse order to cancel out the changes.

#### Example: Buying a Product
* **Normal Flow:**
  1. *Order Service* creates an order (`PENDING`).
  2. *Payment Service* charges the credit card.
  3. *Inventory Service* reserves the item.
  4. *Order Service* updates order status to `COMPLETED`.
* **Failure Flow (Payment Fails):**
  1. *Order Service* creates an order (`PENDING`).
  2. *Payment Service* tries to charge card and **fails**.
  3. **Compensating Transaction Triggered:** *Order Service* runs a local transaction to change the order status to `CANCELLED` (or deletes it).

---

## 5. Saga Architectures: Choreography vs. Orchestration

There are two primary ways to design a Saga: **Choreography** (Event-Driven) and **Orchestration** (Central Manager).

### Pattern A: Choreography (Event-Driven / Decentralized)
There is no central coordinator. Services listen to events and perform their tasks independently.

```
  [Order Service] ─── (Order Created Event) ───► [Payment Service]
         ▲                                              │
         │                                     (Payment Done Event)
         │                                              ▼
  [Order Complete] ◄── (Inventory Reserved Event) ── [Inventory Service]
```

* **How it works:**
  1. *Order Service* creates a pending order and publishes an `Order Created` event.
  2. *Payment Service* listens to this event, charges the user, and publishes a `Payment Done` event.
  3. *Inventory Service* listens to `Payment Done`, updates stock, and publishes an `Inventory Reserved` event.
  4. *Order Service* listens to `Inventory Reserved` and completes the order.
* **Pros:** 
  * Highly decoupled (services only watch events).
  * Easy to build for simple workflows (2 to 3 services).
* **Cons:**
  * Hard to track the overall status of a transaction (no single place to see what is happening).
  * Risk of **cyclical dependencies** (Service A triggers B, which triggers C, which accidentally triggers A again).

---

### Pattern B: Orchestration (Central Coordinator)
A dedicated coordinator service (the **Orchestrator**) tells each participant service what to do.

```
                      ┌─── 1. Charge Card? ───► [Payment Service]
                      │◄── 2. Payment OK ──────┘
  [Order Service] ──► │
   (Orchestrator)     ├─── 3. Reserve Stock? ─► [Inventory Service]
                      │◄── 4. Stock Reserved ──┘
                      ▼
               Complete Order
```

* **How it works:**
  1. *Order Service* acts as the Orchestrator. It creates the order.
  2. The Orchestrator calls *Payment Service* to charge the card.
  3. Once Payment replies `Success`, the Orchestrator calls *Inventory Service* to reserve the item.
  4. If inventory fails, the Orchestrator sends a message to *Payment Service* saying: *"Please refund the money"* (Compensating Transaction).
* **Pros:**
  * **No Cyclical Dependencies:** The workflow is strictly controlled in one place.
  * **Easier to Debug:** You can look at the Orchestrator's database to see the exact state of any pending transaction.
  * **Great for Complex Workflows:** Easy to add new steps without modifying other services.
* **Cons:**
  * **Single Point of Failure (SPOF):** If the Orchestrator crashes mid-transaction, you must write logic to recover its state upon restart.
  * **Tighter Coupling:** The Orchestrator must know the API of all participant services.

---

## 6. Key Interview Questions & Answers

### Q1: What happens if a Compensating (Rollback) Transaction fails?
* **Answer:** This is a major edge case. If a compensating transaction fails (e.g., trying to refund a user but the bank API is down), the system is left in an **inconsistent state**.
* **How to fix it:**
  1. **Automatic Retries:** The Orchestrator (or event consumer) must retry the compensating transaction with exponential backoff.
  2. **Dead Letter Queue (DLQ) & Alerting:** If retries fail repeatedly, the message is placed in a DLQ, and an alert is sent for **manual intervention** (support team fixes it manually).

### Q2: Is Saga a 2-Phase Commit (2PC)?
* **Answer:** No. 
  * **2-Phase Commit (2PC)** is a synchronous protocol that locks databases until all participants agree to commit. It provides strict consistency but is slow and blocks database resources (leads to performance bottlenecks).
  * **Saga** is asynchronous. It does not lock database rows across services, meaning data is **eventually consistent**. If a step fails, it corrects it later via compensation. It is much more scalable.

### Q3: How do we handle read requests while a Saga is running? (Lack of Isolation)
* **Answer:** Sagas lack isolation (other users can see data changes *before* the Saga fully completes). E.g., a user might see an item is reserved, but the transaction fails 5 seconds later and becomes unreserved.
* **How to handle it:** Use the **Semantic Lock** pattern. When a transaction starts, set a status field (e.g., `Order Status = PENDING_PAYMENT`). Clients reading this data know it is not finalized yet and can treat it accordingly in the UI.
