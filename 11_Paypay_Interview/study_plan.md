# PayPay Securities Interview: 8-Day Study Plan

With your interview scheduled for **next Wednesday (July 8, 2026)**, here is a day-by-day checklist to structure your preparation across the three requested business topics and core technical requirements.

---

## 📅 Study Schedule & Checklist

### [ ] Day 1 (Wednesday, July 1): Understand PayPay Securities' Product
* **Goal**: Understand what you are building and who you are building it for.
* **Tasks**:
  * [ ] Read the *Service Overview* section in [preparation_guide.md](file:///Users/somesh_mac/Desktop/SDE-Vault/11_Paypay_Interview/preparation_guide.md).
  * [ ] Research **Fractional Share Trading** (how brokers let retail users buy $1 / 100-Yen worth of expensive shares like Apple or Toyota).
  * [ ] Understand the relationship between **Point Investment (Point Unyo)** and the actual **PayPay Securities brokerage account**.
  * [ ] Familiarize yourself with how it operates as a **Mini-App** inside the main PayPay payment app.

### [ ] Day 2 (Thursday, July 2): Japan's Fintech Industry & Trends
* **Goal**: Build context on why PayPay Securities is positioned for explosive growth.
* **Tasks**:
  * [ ] Study the **New NISA (2024)** program. Understand why the shift from *"savings to investment"* is a major national policy in Japan.
  * [ ] Read about cashless payments in Japan. Note that while Japan was traditionally cash-dominated, it is targeting an **80% cashless ratio**, with **QR/Code payments** (led by PayPay) growing the fastest.
  * [ ] Think about how wealth-tech integrates with mobile payments to capture first-time retail investors.

### [ ] Day 3 (Friday, July 3): Culture & Behavioral Fit (PayPay 5 Senses)
* **Goal**: Align your experiences with PayPay's flat, global, fast-paced startup environment.
* **Tasks**:
  * [ ] Review the **"PayPay 5 Senses"** in [preparation_guide.md](file:///Users/somesh_mac/Desktop/SDE-Vault/11_Paypay_Interview/preparation_guide.md).
  * [ ] For each sense, write down 1-2 scenarios from your past roles using the **STAR method** (Situation, Task, Action, Result):
    * **Speed**: A time you had to deliver a feature rapidly or make a quick engineering decision.
    * **No Ego / Team**: A time you collaborated across teams, mentored someone, or handled constructive feedback.
    * **Sincerity / Professionalism**: A time you dealt with a critical bug, production incident, or financial calculations where precision was paramount.
    * **Self-Driven / Purpose**: A project where you took end-to-end ownership without close supervision.

### [ ] Day 4 (Saturday, July 4): System Design — Wallet Sync & Consistency
* **Goal**: Prepare for backend architectural discussions involving payments and transactional consistency.
* **Tasks**:
  * [ ] Review your notes on **[Payment System](file:///Users/somesh_mac/Desktop/SDE-Vault/04_System-Design/HLD_Notes/26.%20Payment%20System/README.md)** and **[Digital Wallet](file:///Users/somesh_mac/Desktop/SDE-Vault/04_System-Design/HLD_Notes/27.%20%20Digital%20Wallet/README.md)**.
  * [ ] Focus on the **Saga Pattern** (orchestrated vs. choreographed) for achieving eventual consistency between the PayPay Payment database and the Securities database.
  * [ ] Understand **Idempotent API design** and event processing using Kafka (e.g., using message dedup keys to prevent charging a user twice).
  * [ ] Study **Double-Entry Bookkeeping** principles to maintain accurate user ledger balances.

### [ ] Day 5 (Sunday, July 5): System Design — Fractional Trading & Databases
* **Goal**: Focus on the specific system mechanics of a retail broker app.
* **Tasks**:
  * [ ] Review your notes on **[Stock Exchange](file:///Users/somesh_mac/Desktop/SDE-Vault/04_System-Design/HLD_Notes/28.%20Stock%20Exchange/README.md)**. Note the differences: a stock exchange matches buyers/sellers, but a *retail broker* (PayPay Securities) handles inventory and internal allocation.
  * [ ] Design the flow of the **Internalization Engine**: how the broker holds whole stocks in inventory and registers fractions to user accounts.
  * [ ] Research **TiDB** (which PayPay uses). Understand how it achieves both horizontal scalability (like NoSQL) and strong ACID consistency (using Raft-based storage engines).

### [ ] Day 6 (Monday, July 6): Java / Spring Boot Concurrency & Locks
* **Goal**: Brush up on low-level coding and framework concepts that senior engineers ask.
* **Tasks**:
  * [ ] Review your Spring Boot notes in [10_springboot/](file:///Users/somesh_mac/Desktop/SDE-Vault/10_springboot).
  * [ ] Study **Optimistic locking** (using `@Version` in JPA) vs. **Pessimistic locking** (e.g., `SELECT ... FOR UPDATE`). Explain when to use which when updating a user's cash balance.
  * [ ] Brush up on thread safety in Java (e.g., `ConcurrentHashMap`, `Atomic` variables, thread pools) and Spring's transaction propagation levels (`@Transactional(propagation = ...)`).

### [ ] Day 7 (Tuesday, July 7): Pitch Refinement & Practice
* **Goal**: Polish your oral delivery and make sure you sound natural and confident.
* **Tasks**:
  * [ ] Practice speaking your answer to: **"Why PayPay Securities?"** and **"Why Japan/Fintech?"**.
  * [ ] Practice introducing your past projects. Keep it to a high-level summary of the business goal, the tech stack, the architecture challenges you solved, and the final impact.
  * [ ] Review your DSA test solutions (the String & Map problems you solved on Sunday) in case they ask you to walk through your code or suggest optimizations.

### [ ] Day 8 (Wednesday, July 8): Interview Day!
* **Goal**: Perform your best!
* **Tasks**:
  * [ ] Join the video call 5 minutes early.
  * [ ] Keep a glass of water handy.
  * [ ] Speak clearly, highlight your interest in high-scale systems, and ask engaging questions at the end (e.g., about their transition to TiDB, handling trade spikes, or team structure).
