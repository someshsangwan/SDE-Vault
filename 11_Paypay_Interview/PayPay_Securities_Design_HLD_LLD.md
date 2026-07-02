# PayPay Securities / Brokerage App — System Design (HLD + LLD)

> Dedicated design note for the PayPay Securities interview. Covers **HLD** (distributed architecture) and **LLD** (Java/OOP class design) for a retail stock-investment / brokerage service.
> Interviewers phrase it as *"design PayPay itself, interacting with the third party (bank) — availability, DB read/write performance, scaling, security."* Any "design a brokerage / stock investment / trading app / Robinhood / Groww" prompt maps here.
> Related: [[Interview_Experience]] · [[Paypay_general_question]]

---

## 0. The One Insight That Sets You Apart

**A retail broker is NOT a stock exchange.**

- A **stock exchange** (Tokyo Stock Exchange / NASDAQ) runs the **matching engine** + **order book** — the ultra-low-latency, stateful core that matches buy/sell orders.
- A **retail broker** (PayPay Securities, Robinhood, Groww) is a **client + router**: it does KYC, holds user funds (wallet), validates & risk-checks orders, then **routes them to the exchange or a market maker**, and reflects fills back to the user's portfolio.

So when they say "design PayPay Securities," the scope is the **broker side**: funding (bank integration), order management, portfolio, real-time prices, settlement — *not* building a matching engine. **Say this out loud early** — it shows you understand the domain and instantly scopes the problem correctly. Then offer: *"I can also go into the exchange-side matching engine if you'd like"* (deep-dive in §8).

---

## 0.5 How to Actually Drive the Interview — Progressive Build-Up + Cross-Questions ⭐⭐

Don't draw the final diagram immediately. **Start tiny, then evolve it**, narrating your reasoning. Interviewers score you on *how you get there*, and they interrupt with **"why this instead of that?"** — those are the moments that decide the round. Below is the exact sequence, with the cross-questions they'll throw and how to answer.

> Golden rule: **state a simple version → name its problem → add the next piece to fix that problem.** Every addition must be *motivated by a problem*, never "because big systems have it."

---

### STEP 0 — Clarify & scope (first 3–5 min, before drawing anything)
Say: *"Before designing, let me confirm scope."* Ask:
- Retail broker (route to exchange) or the exchange itself? → confirm **broker**.
- Which functions in scope? (I'll assume: fund via bank, place/track orders, portfolio, real-time prices.)
- Scale ballpark? (I'll assume ~10M users, read-heavy price traffic, spiky orders at market open.)

**❓ Cross-question: "Why do you care about scope first?"**
> "In a brokerage the hard part is money-correctness and third-party (exchange/bank) integration, not raw QPS. Scoping tells me where to spend my time — I don't want to over-engineer a matching engine we don't even own."

---

### STEP 1 — The naïve version (draw this first)
```
[Client] → [App Server] → [Database]
```
Say: *"Simplest thing: one service, one DB. User places an order, we write it, update their balance."* Then immediately **attack it yourself**: "This has three problems — (1) it doesn't reach any exchange, (2) balance updates race under concurrency, (3) one DB/server is a SPOF and won't scale reads."

**❓ "Why not just start with microservices?"**
> "Premature decomposition adds network calls and distributed-transaction complexity before I understand the domain. I start monolithic-conceptually, then split along the seams that actually need independent scaling/ownership."

---

### STEP 2 — Reach the outside world: bank + exchange
Add the **Order Router / Exchange Gateway** (→ TSE) and **bank integration** for funding.
Say: *"A broker's job is routing. We validate, then forward the order to the exchange over FIX and consume execution reports."*

**❓ "Why route to an exchange instead of matching orders yourself?"**
> "We're a broker, not an exchange. The matching engine + order book is the exchange's ultra-low-latency stateful core. Building our own would be re-implementing TSE, add latency, and isn't our regulatory role."

**❓ "The bank/exchange API is slow or down — what happens?"**
> "Wrap those calls in a **circuit breaker + retry + timeout** (Resilience4j, which PayPay uses). Orders queue and are processed async so a slow third party doesn't take down our order intake."

---

### STEP 3 — Make money correct (the heart of the design)
Introduce the **Wallet** with the **hold → settle → release** model, and a **double-entry ledger**.
Say: *"On submit I don't debit — I **reserve** funds. On fill I settle; on cancel I release. Balance is derived from an append-only ledger."*

**❓ "Why reserve instead of just checking balance and debiting on fill?"**
> "Between check and fill the user could place a second order and spend the same cash twice. Reserving atomically at submit prevents double-spend across pending orders."

**❓ "Two orders hit the same account balance at once — how do you stay correct?"**
> "Serialize the reserve step with a **`SELECT … FOR UPDATE`** row lock on the account (pessimistic). One user rarely fires thousands of orders/sec, so locking one account row is cheap and money-correct. Under low contention I'd use an optimistic `@Version` + retry instead."

**❓ "Why pessimistic here but optimistic elsewhere?"**
> "Match the tool to contention. High-contention, correctness-critical single row → pessimistic. Low-contention updates where conflicts are rare → optimistic avoids lock overhead."

**❓ "Why a double-entry ledger instead of a `balance` column?"**
> "Auditability and reconciliation. A single mutable balance loses history and can silently drift; an append-only debit/credit ledger is regulator-auditable and lets me reconcile against the bank/exchange. Balance becomes a checkpointed materialized value."

**❓ "Why `BigDecimal` and not `double`?"**
> "Floating point can't represent 0.1 exactly — you get rounding errors, unacceptable for money. `BigDecimal` (or integer minor-units) is exact."

---

### STEP 4 — Split into services (only now)
Split into **Order**, **Wallet/Account**, **Portfolio**, **Market Data**, **Risk/Compliance**, **Notification** behind an **API Gateway**.
Say: *"I split along ownership + scaling boundaries: price traffic scales very differently from the money path, so they shouldn't share a deployment."*

**❓ "Now Order, Wallet, Portfolio are separate DBs — how do you keep a trade atomic across them?"**
> "No distributed 2PC — it's slow and locks across services. Use the **Saga pattern**: a sequence of local transactions with **compensating actions**. If the exchange rejects after funds were held, a compensating event releases the hold."

**❓ "How do you publish the event reliably — what if the DB commit succeeds but Kafka publish fails?"**
> "That's the dual-write problem. **Transactional Outbox**: write the domain row and an outbox row in the *same* local DB transaction, then a relay ships the outbox row to Kafka. No lost events, no phantom events."

**❓ "Why Kafka and not just REST calls between services?"**
> "Decoupling + durability + replay. Async events mean Portfolio/Notification don't block the order path, a consumer crash just resumes from its offset, and I can replay for recovery/audit. Synchronous REST would couple availability and cascade failures."

---

### STEP 5 — Scale the read-heavy price path
Add **Redis** (latest price cache) + **WebSocket** fan-out + **Kafka partition per symbol**.
Say: *"Price reads outnumber order writes ~100:1. I keep the latest price in Redis for sub-ms reads and push updates over WebSockets so clients don't poll."*

**❓ "Why WebSocket instead of polling / long-polling?"**
> "Prices change constantly during market hours. Polling wastes requests and adds latency; a persistent WebSocket pushes only on change and scales to many idle connections cheaply."

**❓ "Why partition Kafka by symbol?"**
> "It preserves **ordering per symbol** (price ticks must be in order) while letting me scale horizontally across symbols. Global ordering isn't needed; per-symbol ordering is."

**❓ "Why cache in Redis — isn't a read replica enough?"**
> "A replica still costs a DB round-trip and query; Redis is an in-memory O(1) lookup for the single hottest value (latest quote). Replicas handle heavier historical/portfolio queries."

---

### STEP 6 — Scale the write path + DB read/write (their explicit ask)
Add **read replicas**, **CQRS read model** for portfolio, **shard by userId**, keep the sync write path minimal.

**❓ "How do you improve DB *reads*?"**
> "Read replicas for history/portfolio, Redis for hot values, and **CQRS** — a denormalized read model updated async from trade events so dashboard reads never touch the write DB."

**❓ "How do you improve DB *writes*?"**
> "Keep the synchronous write minimal (reserve funds + persist order); push everything else async via Kafka. **Shard by userId** so a user's money data is co-located and write load spreads across shards. The ledger is append-only, so no update-contention hotspots."

**❓ "Why shard by userId and not by symbol?"**
> "For the money/order path, a user's data must be transactionally together — sharding by user keeps a user's wallet+orders on one shard. Symbol-based partitioning is for the *market-data* path, which is a different concern."

**❓ "Won't eventual consistency in the CQRS portfolio show wrong balances?"**
> "Portfolio *display* can lag a second — acceptable. But the **spendable balance used for validation** is read strong-consistent from the wallet's source of truth, never from the eventual read model. I separate 'money truth' (strong) from 'display' (eventual)."

---

### STEP 7 — Reliability, availability, security (close strong)
Multi-AZ K8s, Aurora failover, Kafka RF≥3, idempotency keys, circuit breakers, immutable audit log, TLS/AES-256, MFA.

**❓ "How do you guarantee an order isn't placed twice on a client retry?"**
> "**Idempotency key** per request, stored uniquely — a retry with the same key returns the original result instead of creating a new order. Same idea for dedup on exchange execution reports via execution ID."

**❓ "Exactly-once processing — is that even possible?"**
> "Not true exactly-once delivery, but exactly-once **effect**: at-least-once delivery + **idempotent consumers**. For money that's what matters."

---

### The mental model to repeat
`naïve box → reach bank/exchange → make money correct → split services → scale reads → scale writes → harden`.
Each arrow is triggered by a **problem you named**, and you defend each choice against its alternative. If you do only this, you'll clear the round.

---

## 1. Requirements

### Functional
1. **Onboarding/KYC** — register, verify identity (AML/KYC — mandatory in Japan for securities).
2. **Fund management** — deposit/withdraw via linked **bank** (the "third party" they mention).
3. **Market data** — real-time & historical prices, charts, watchlists.
4. **Place orders** — market / limit / stop-loss; buy & sell; also **fractional / recurring investing** (PayPay Securities' "点セット"/tsumitate style micro-investing).
5. **Order management** — view status, modify, cancel.
6. **Portfolio** — holdings, cash balance, P&L (realized/unrealized).
7. **Settlement** — T+2 settlement, reconciliation with exchange/bank.
8. **Notifications** — order fills, price alerts.

### Non-Functional (drive the whole design)
- **Consistency for money** — a wallet balance / holding must never be wrong or double-counted. Strong consistency, not eventual, on the money path.
- **High availability** — downtime during market hours = users lose money = regulatory/reputational risk. Target 99.99%.
- **Low latency** — order submission acknowledged in ms; price feed near-real-time.
- **Durability & auditability** — every order/trade/fund movement is an immutable, regulator-auditable record (Japan FSA).
- **Scalability** — huge read-heavy price traffic + spiky order bursts (market open 9:00, news events).
- **Security** — encryption, MFA, fraud detection, PII protection.

### Back-of-envelope (say the method, not memorized numbers)
- Say ~10M users, ~1M DAU, peak maybe **10–50k orders/sec** at open, but **price-feed reads dwarf writes** (100:1+). → *Design implication: reads scale horizontally with cache + replicas; the money-write path is the scarce, correctness-critical resource.*

---

## 2. High-Level Architecture

```
                 ┌─────────── Mobile / Web Client ───────────┐
                 │  (orders via REST,  prices via WebSocket)  │
                 └───────────────────┬───────────────────────┘
                                     │
                            ┌────────▼────────┐
                            │   API Gateway   │  auth (JWT/OAuth2), rate-limit, routing
                            └────────┬────────┘
        ┌──────────────┬────────────┼─────────────┬───────────────┬──────────────┐
        ▼              ▼            ▼              ▼               ▼              ▼
 ┌────────────┐ ┌────────────┐ ┌──────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
 │  User /    │ │  Account / │ │  Order   │ │  Market    │ │ Portfolio  │ │Notification│
 │  KYC Svc   │ │Wallet Svc  │ │ Service  │ │ Data Svc   │ │  Service   │ │  Service   │
 └────────────┘ └─────┬──────┘ └────┬─────┘ └─────┬──────┘ └─────┬──────┘ └─────┬──────┘
                      │             │             │              │              │
               (bank rails)   ┌─────▼──────┐  (exchange feed) (read model)  (Kafka→push)
               ┌──────────┐   │Risk/Compli-│      ▲
               │  Bank /   │   │ ance check │      │
               │ Payment   │   └─────┬──────┘      │
               │  Gateway  │         │             │
               └──────────┘   ┌──────▼───────┐     │
                              │ Order Router /│─────┘   ← Order Gateway to
                              │  Exchange GW  │─────────► TOKYO STOCK EXCHANGE
                              └──────┬────────┘  ◄───────  (fills / execution reports)
                                     │
                              ┌──────▼───────┐
                              │  Settlement  │  T+2, reconciliation
                              └──────────────┘

  Cross-cutting:  Kafka (async events) · Redis (cache) · Aurora MySQL (txn) · S3 (audit) · DynamoDB (feed cache)
```

### Component responsibilities
| Service | Responsibility |
|---|---|
| **API Gateway** | AuthN/Z (JWT/OAuth2), rate limiting, routing, TLS termination. Stateless. |
| **User / KYC** | Registration, identity verification, AML/KYC status. |
| **Account / Wallet** | Cash balance, **fund holds/reservations**, deposits/withdrawals via bank. The money source of truth (double-entry ledger). |
| **Order Service** | Create/modify/cancel orders, order state machine, persistence. |
| **Risk / Compliance** | Pre-trade checks: sufficient funds/shares, trading limits, market hours, fraud/AML. |
| **Order Router / Exchange Gateway** | Translates internal order → exchange protocol (e.g. FIX), routes to **TSE**/market maker, consumes execution reports. |
| **Market Data** | Ingests exchange feed, normalizes, caches, fans out via WebSocket. |
| **Portfolio** | Read model of holdings + P&L; updated on fills. |
| **Settlement** | T+2 clearing, reconciliation with exchange & bank. |
| **Notification** | Fill confirmations, price alerts (Kafka-driven). |

---

## 3. Order Lifecycle (the flow they'll drill)

**Buy order, step by step:**
1. Client → API Gateway → **Order Service**: `POST /orders {symbol, qty, type, price}` with an **idempotency key**.
2. **Risk/Compliance** check: market open? valid symbol? KYC ok?
3. **Wallet**: **reserve/hold** funds = `qty × price + fee` (atomically decrement available balance, not the ledger total). If insufficient → reject.
4. Order persisted as `NEW → VALIDATED`, event published to Kafka.
5. **Order Router** sends it to the **exchange (TSE)** via FIX; state `→ ROUTED / OPEN`.
6. Exchange matches it (their matching engine); sends back an **execution report** (full/partial fill).
7. On fill: state `→ FILLED / PARTIALLY_FILLED`. Emit `TradeExecuted` event.
8. **Wallet**: convert the *hold* into a real debit (double-entry: cash −, brokerage clearing +). **Portfolio**: add the shares. Both must be atomic/consistent.
9. **Settlement** service tracks T+2 clearing with the exchange/clearinghouse.
10. **Notification** pushes confirmation; audit log written (immutable, S3).

**Order state machine (memorize):**
`NEW → VALIDATED → ROUTED → OPEN → (PARTIALLY_FILLED)* → FILLED`
with side exits: `→ REJECTED` (validation), `→ CANCELLED` (user), `→ EXPIRED` (limit order end-of-day).

**Pitfall to mention:** never mutate the wallet balance directly on order placement. Use a **two-phase money model**: *hold* on submit, *settle* on fill, *release* on cancel/reject. Prevents users double-spending the same cash across two pending orders.

---

## 4. Deep Dive — Consistency & the Money Path ⭐

This is the round-4 differentiator. Interviewers probe "how do you keep balances correct."

- **Double-entry ledger.** Every money movement = balanced debit + credit rows in an append-only `ledger_entries` table. You never `UPDATE balance = balance - x` blindly; balance is derivable/checkpointed from entries. Guarantees auditability and makes reconciliation possible.
- **Idempotency.** Client sends an idempotency key; store it uniquely. Retries (network timeouts) must not place two orders or double-debit. Exchange execution reports can arrive twice → dedupe by execution ID.
- **Concurrency on the wallet row.** Two orders racing on the same balance:
  - **Pessimistic**: `SELECT ... FOR UPDATE` on the account row → serialize. Simple, correct, but limits throughput per account (fine — one user rarely fires 1000 orders/sec).
  - **Optimistic**: `@Version` column, retry on conflict. Better under low contention.
  - *Recommendation:* pessimistic row lock on the funding account for the reserve step — money correctness > throughput here.
- **Cross-service atomicity (no distributed 2PC).** Order + Wallet + Portfolio live in different services/DBs. Use the **Saga pattern** with **compensating transactions**: if the exchange rejects after funds were held, a compensating event *releases the hold*. Drive it with Kafka + the **Transactional Outbox** pattern (write DB row + outbox event in one local txn; a relay publishes to Kafka → exactly-once-ish, no dual-write bug).
- **Exactly-once effect.** At-least-once delivery + idempotent consumers = exactly-once *effect*, which is what money needs.

---

## 5. Deep Dive — Real-Time Price Feed (read-heavy path)

### 5.0 Where do prices even come from? (how PayPay gets prices from TSE)
Before you can cache or push a price, you have to *receive* it. There are **two separate channels** to the exchange — don't confuse them:
- **Order gateway (OUT):** you *send orders* to TSE via FIX, get execution reports back. (Step 2.)
- **Market data feed (IN):** you *receive prices* — a firehose of updates flowing into you. This is what §5 is about.

**The exchange broadcasts a market-data feed.** TSE runs a system called **arrowhead** that continuously **publishes** every price change, trade, and order-book update as a real-time stream. This feed is a paid product — brokers subscribe to it. Two ways to receive it:
- **Direct feed:** connect straight to TSE's market-data servers; fastest, but you handle their raw (often binary) protocol yourself.
- **Via a market-data vendor/aggregator** (e.g. QUICK in Japan, Refinitiv, Bloomberg): they collect exchange feeds and hand you a cleaner, normalized stream. Easier, common for retail brokers.

**Multicast:** exchanges usually *broadcast* the feed like a radio station — transmit once, and every subscribed broker tunes in and receives it simultaneously (no separate copy per broker).

So the ingestion flow is: `TSE (arrowhead) ──broadcast feed──► PayPay Market Data Service ──normalize──► Kafka → Redis → WebSocket → user`. PayPay Securities, as a licensed Japanese broker, subscribes to TSE market data (direct or via a Japanese vendor like QUICK).

### 5.1 Pipeline (once prices are flowing in)
- **Ingestion**: the **Market Data Service** subscribes to the exchange feed → **normalize** (convert TSE's raw format into our clean internal format) → publish to **Kafka**, one **partition per symbol** (preserves per-symbol ordering, enables horizontal scale).
- **Cache**: latest price per symbol in **Redis** (sub-ms reads). This absorbs the 100:1 read load — clients don't hit the DB for a quote.
- **Fan-out to clients**: **WebSocket** servers (stateful, many connections) subscribe to Kafka topics and push updates. Users subscribe only to symbols on their screen → reduce fan-out.
- **Historical data**: time-series store (TimescaleDB / or DynamoDB + S3) for charts; served from read replicas, not the hot path.
- **Graceful degradation**: under extreme load, serve slightly-stale **cached** prices and prioritize the **order-execution** path over cosmetic price ticks.

### 5.2 The price flows like this
```
Exchange sends price
      │
   [Kafka]  ← one lane per stock (order kept, scales wide)
      │
      ├──► [Redis]  ← store latest price (fast reads, the "whiteboard")
      │
      └──► [WebSocket servers] ──push──► millions of user phones
```

### 5.3 Why do we need Redis if WebSocket already pushes to phones?
They *sound* redundant but solve two different problems:
- **WebSocket** streams a price **when it changes** to phones **already watching** that stock. It's a live *pipe* with **no memory** — it only fires on change and forgets everything.
- **Redis** holds the **current** price so *anyone* can ask "what's the price right now?" and get an instant answer.

WebSocket alone breaks in these very common moments:
1. **You just opened the app** — the price hasn't changed in the last few seconds, so no push is coming. What number do you show *now*? → read Redis.
2. **Order validation / risk check** — a backend service (not a phone) needs the current price to validate a market order or reserve funds. → read Redis.
3. **Portfolio valuation** — backend needs the current price of every held stock, instantly. → read Redis.
4. **A new WebSocket connection needs a starting value** — on connect, send the latest known price from Redis as a baseline, then stream changes on top.

| | Redis | WebSocket |
|---|---|---|
| Answers | "What's the price **right now**?" (a question, any time) | "The price **just changed** to X" (an event, on change) |
| Model | **Pull** — you ask, it answers | **Push** — it tells you when there's news |
| Stores state? | ✅ holds the *current* value | ❌ no memory, just a live pipe |
| Users | anyone, any moment (app load, backend checks, valuations) | phones actively watching a stock |

**One-liner:** *"Redis is the **source of truth for the current price** (pull, has memory); WebSocket is the **delivery mechanism for updates** (push, no memory). A freshly-opened app or a backend order-check reads Redis; a phone watching live gets WebSocket pushes. You need both."*

### 5.4 Do we store market prices in a DB?
Split it into two different things people call "price":

**1. Current/live price** ("Toyota is ¥3,000 *right now*") → **NOT stored durably in a DB.**
- **Disposable** — replaced by a new value in ~1 second; nobody needs the exact price 3s ago.
- **Too fast** — thousands of ticks/sec across all stocks would hammer a durable DB for data we immediately overwrite.
- **Not our data** — the exchange is the source of truth. If **Redis** loses it, the next tick from the TSE feed refills it. So it lives only in Redis (memory); no durability needed.

**2. Historical prices** ("Toyota every minute for 5 years", for charts/analytics) → **YES, stored** — but **NOT in the main transactional DB.**
- Use a **time-series DB** built for timestamped data: **TimescaleDB / InfluxDB**, or PayPay-style **DynamoDB / S3** for older data.
- Keep the price firehose *out* of Aurora/MySQL so it never competes with or endangers the money path.

| Data | Where | Durable? | Why |
|---|---|---|---|
| **Current price** | Redis (memory) | ❌ No | Disposable, re-fetchable from exchange |
| **Historical prices** (charts) | Time-series DB (Timescale / DynamoDB / S3) | ✅ Yes | Users want charts; must survive |
| **Orders, wallet, portfolio** | Transactional DB (Aurora/MySQL) | ✅ Yes | Money — must be correct & durable |

**Intuition:** current price = the *time on a clock* (read it, don't save every second); historical prices = a *logbook* (keep it, in a store built for logging); money = a *bank ledger* (sacred, separate, always durable).

**One-liner:** *"Three storage tiers by purpose: **Redis** for the disposable current price, a **time-series DB** for historical charts, and the **transactional DB** strictly for money. The live price firehose never touches the money DB."*

---

## 6. Deep Dive — Availability + DB Read/Write Scaling + Security (their exact axes)

**Availability**
- Stateless services (gateway, order, portfolio) → run N replicas on **Kubernetes**, multi-AZ. Auto-scale on CPU/queue depth.
- No single point of failure: multi-AZ Aurora with automatic failover; Kafka replication factor ≥ 3.
- **Resilience4j** (PayPay actually uses it): circuit breakers, retries, bulkheads around the bank/exchange integrations so a slow third party doesn't cascade.
- Idempotent + retryable everywhere so a pod restart is safe.

**DB read/write performance** (they specifically ask "improve DB write and read")
- **Reads:** Aurora **read replicas** for portfolio/history queries; **Redis** cache for hot reads (prices, portfolio snapshots); **CQRS** — a separate read model for portfolio, updated async from trade events, so heavy dashboard reads never touch the write path.
- **Writes:** **shard by user/account ID** (a user's money data co-located); batch/queue non-critical writes via Kafka; keep the synchronous write path minimal (reserve funds + persist order), push the rest async.
- **Hot rows:** the ledger is append-only (no update contention); balance is a periodically-checkpointed materialized value.
- Mention PayPay's real stack: **Aurora MySQL**, plus **TiDB** (distributed SQL) for horizontally-scalable transactional workloads, **DynamoDB** for feed/session data.

**Security**
- TLS everywhere; **AES-256** at rest for PII/bank details.
- **OAuth2 + JWT**, **MFA** for login and withdrawals.
- Fraud/AML monitoring (ML on trading patterns), transaction limits.
- **Immutable audit log** (append-only, S3 with object-lock) for FSA compliance.
- Least-privilege, secrets manager, network isolation between money services and edge.

---

## 7. Tech Stack — Map to PayPay's ACTUAL stack (bonus points)

Name these; it signals you did homework:
- **Language/Framework:** Java & **Kotlin** on **Spring Boot** (PayPay Securities also has some PHP legacy). Libraries: **JUnit, Resilience4j, Feign**.
- **Datastores:** **MySQL / Aurora DB**, **TiDB** (distributed), **DynamoDB**, **ElasticSearch**, **Redis**.
- **Async:** **Kafka**.
- **Infra:** **AWS**, **Kubernetes on EC2**, **Docker**, **ArgoCD** (GitOps CD).
- **Scale reality:** PayPay runs **100+ microservices across ~10 teams** — so microservice decomposition + async choreography is the expected answer, not a monolith.

---

## 8. (Optional) Exchange-Side Deep Dive — The Matching Engine

Only if the interviewer pushes toward the exchange. Key points:
- **Order book per symbol**, held **in-memory** for sub-microsecond access — the one justified **stateful, single-writer** component (everything else is stateless).
- **Price–time (FIFO) priority:** best price first; ties broken by earliest arrival.
  - Data structure: two sides. **Buy side**: max-heap / price-descending map of price levels; **Sell side**: min-heap / price-ascending. Each price level = a **FIFO queue** of orders. A `HashMap<orderId, node>` gives O(1) cancel.
  - Match = highest bid ≥ lowest ask → produce two fills (one per side).
- **Order types:** Market, Limit, Stop-Loss, Fill-or-Kill, IOC.
- **Sequencing:** an Order Manager assigns a globally increasing **sequence number** for fairness/determinism.
- **Reliability:** the engine is deterministic; recover via **event sourcing** — replay the ordered input log (Kafka) + periodic order-book **snapshots**; run a **hot standby**.

---

## 9. LLD — Java / OOP Class Design ⭐

The **Algo & DS round** may ask you to *code the domain model* or an *order book*. Here's clean, idiomatic Java.

### 9.1 Enums & core value objects
```java
public enum Side { BUY, SELL }

public enum OrderType { MARKET, LIMIT, STOP_LOSS, FILL_OR_KILL }

public enum OrderStatus {
    NEW, VALIDATED, ROUTED, OPEN,
    PARTIALLY_FILLED, FILLED,
    REJECTED, CANCELLED, EXPIRED
}

// Money: NEVER use double for money. Use BigDecimal (or long minor-units).
public record Money(BigDecimal amount, Currency currency) {
    public Money add(Money o) { return new Money(amount.add(o.amount), currency); }
    public Money subtract(Money o) { return new Money(amount.subtract(o.amount), currency); }
}
```

### 9.2 Order — with a State-pattern-friendly status
```java
public class Order {
    private final String id;                 // UUID, also idempotency anchor
    private final String userId;
    private final String symbol;
    private final Side side;
    private final OrderType type;
    private final int quantity;
    private int filledQuantity;
    private final Money limitPrice;          // null for MARKET
    private volatile OrderStatus status;
    private final Instant createdAt;

    // state transitions guarded — illegal transitions throw
    public synchronized void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next))
            throw new IllegalStateTransitionException(status, next);
        this.status = next;
    }
    public int remainingQty() { return quantity - filledQuantity; }
    // getters...
}
```
*Talk about:* immutability of identity fields, `BigDecimal` for price, guarded state transitions, `synchronized`/optimistic concurrency on mutable fields.

### 9.3 Strategy pattern for order types (extensible matching/validation)
```java
public interface OrderTypeStrategy {
    boolean isExecutable(Order order, Money marketPrice);
}
class MarketOrder implements OrderTypeStrategy {
    public boolean isExecutable(Order o, Money mkt) { return true; }        // always
}
class LimitOrder implements OrderTypeStrategy {
    public boolean isExecutable(Order o, Money mkt) {
        return o.getSide() == Side.BUY
             ? mkt.amount().compareTo(o.getLimitPrice().amount()) <= 0
             : mkt.amount().compareTo(o.getLimitPrice().amount()) >= 0;
    }
}
```
Adding a new order type = new class, no `if/else` sprawl (Open/Closed Principle).

### 9.4 Wallet — the two-phase money model
```java
public class Wallet {
    private final String userId;
    private Money available;   // spendable
    private Money reserved;    // held for open orders

    public synchronized void hold(Money amt) {          // on order submit
        if (available.amount().compareTo(amt.amount()) < 0)
            throw new InsufficientFundsException();
        available = available.subtract(amt);
        reserved  = reserved.add(amt);
    }
    public synchronized void settle(Money amt) {        // on fill
        reserved = reserved.subtract(amt);              // money leaves the account
    }
    public synchronized void release(Money amt) {       // on cancel/reject
        reserved  = reserved.subtract(amt);
        available = available.add(amt);
    }
}
```
*In prod this is backed by a double-entry ledger + `SELECT ... FOR UPDATE` / `@Version`; the in-memory version shows you understand the state model.*

### 9.5 Portfolio & Holding
```java
public class Holding {
    private final String symbol;
    private int quantity;
    private Money avgBuyPrice;      // recompute on each buy fill

    public void applyBuy(int qty, Money price) {
        Money totalCost = avgBuyPrice.multiply(quantity).add(price.multiply(qty));
        quantity += qty;
        avgBuyPrice = totalCost.divide(quantity);   // weighted avg
    }
    public Money unrealizedPnl(Money marketPrice) {
        return marketPrice.subtract(avgBuyPrice).multiply(quantity);
    }
}

public class Portfolio {
    private final Map<String, Holding> holdings = new ConcurrentHashMap<>();
    // updateOnFill(...), totalValue(marketData), etc.
}
```

### 9.6 Order Book (if asked to implement the exchange core)
```java
public class OrderBook {
    private final String symbol;
    // price -> FIFO queue of orders at that price level
    private final TreeMap<BigDecimal, Deque<Order>> buys  =
        new TreeMap<>(Comparator.reverseOrder());   // highest bid first
    private final TreeMap<BigDecimal, Deque<Order>> sells =
        new TreeMap<>();                            // lowest ask first
    private final Map<String, Order> index = new HashMap<>(); // O(1) cancel

    public List<Trade> place(Order order) {
        List<Trade> trades = new ArrayList<>();
        var opposite = order.getSide() == Side.BUY ? sells : buys;
        while (order.remainingQty() > 0 && !opposite.isEmpty()) {
            var bestEntry = opposite.firstEntry();
            if (!crosses(order, bestEntry.getKey())) break;      // no price match
            Deque<Order> q = bestEntry.getValue();
            Order resting = q.peekFirst();                        // time priority
            int fill = Math.min(order.remainingQty(), resting.remainingQty());
            trades.add(new Trade(order, resting, fill, bestEntry.getKey()));
            // ... update filledQty, pop resting if fully filled, clean empty level
        }
        if (order.remainingQty() > 0) rest(order);                // add to book
        return trades;
    }
    // crosses(), rest(), cancel(orderId) via index ...
}
```
*Complexity:* place/match ≈ O(log P + fills) where P = distinct price levels (TreeMap); cancel O(1) via the index. **Key talking point:** price = `TreeMap` (ordered), time = `Deque` (FIFO) at each level, `HashMap` for cancel — the classic price-time-priority structure.

### Design patterns to name-drop in LLD
- **Strategy** — order types / validation.
- **State** — order status transitions.
- **Observer / pub-sub** — price updates → subscribers.
- **Factory** — create orders from requests.
- **Command** — encapsulate place/cancel/modify as auditable actions.
- **Singleton** (careful) — per-symbol order book instance.

---

## 10. Common Pitfalls (say these to show maturity)
1. Using `double` for money → use `BigDecimal` / integer minor units.
2. Mutating balance directly instead of hold→settle→release + ledger.
3. Assuming a broker runs a matching engine (it routes to the exchange).
4. Dual-write bug (DB + Kafka) → use the **Transactional Outbox**.
5. Ignoring idempotency → duplicate orders on retry.
6. Distributed 2PC across services → prefer **Saga + compensation**.
7. Eventual consistency on balances → money needs strong consistency; eventual is fine for *portfolio display* only.
8. Forgetting compliance/audit — in fintech it's a foundational constraint, not an add-on.

---

## 11. Self-Check Questions
- Why is a broker's design different from an exchange's? Which part is stateful and why?
- Walk the full lifecycle of a limit buy that partially fills, then the user cancels the rest — what happens to held funds?
- Two pending orders would together overspend the wallet — how does your design prevent it? (hold model + row lock)
- How do you guarantee a network-retried order isn't placed twice?
- Order service committed, but the Kafka publish crashed — how do you avoid losing the event? (outbox)
- The exchange sends the same fill twice — what stops a double credit? (idempotent consumer / execution-id dedupe)
- How do you scale price reads 100× without touching the write DB? (Redis + WebSocket fan-out + partition per symbol)
- Which DB isolation level / locking do you use on the funding account, and why?
- Bank deposit API is timing out — how does your system stay up? (circuit breaker, async, Resilience4j)

---

## Sources
- [Design Robinhood (trading app) — System Design Handbook](https://www.systemdesignhandbook.com/guides/design-robinhood/)
- [Design a Stock Exchange System — System Design Handbook](https://www.systemdesignhandbook.com/guides/design-a-stock-exchange-system/)
- [Groww Stock Broker System Design — GitHub (SunilGudivada)](https://github.com/SunilGudivada/Data-Structures-and-Algorithms/blob/main/system-design/stock-broker-system-design-groww.md)
- [Design an Online Stock Brokerage System (OOD) — grokking-the-OOD-interview](https://github.com/tssovi/grokking-the-object-oriented-design-interview/blob/master/object-oriented-design-case-studies/design-an-online-stock-brokerage-system.md)
- [Stock Exchange System Design — System Design Newsletter (Neo Kim)](https://newsletter.systemdesign.one/p/stock-exchange-system-design)
- [Backend Engineer @ PayPay Securities — Japan Dev (tech stack)](https://japan-dev.com/jobs/paypay-securities/paypay-securities-backend-engineer-ljuyvo)
- [PayPay Tech Talks vol.1 — PayPay Inside-Out (architecture)](https://insideout.paypay.ne.jp/en/2021/01/21/techtalks-vol1-en/)


