# System Design (HLD) — 30 Core Concepts

> **Source:** AlgoMaster "30 System Design Concepts" video + my own interview-depth additions.
> **Mental model for the whole video:** follow a single request on its journey — from a *client*, across the *network*, through *proxies/load balancers*, into *servers*, down to *databases/caches*, and back. Almost every concept below is a station on that journey or a technique to make one station faster / more reliable / more scalable.

**The 3 goals everything serves:** ⚡ **Latency** (fast) · 📈 **Scalability** (handles growth) · 🛡️ **Reliability/Availability** (doesn't fall over). When answering a design question, tie every choice back to one of these three.

---

## Map of the 30 concepts

```
                     THE REQUEST'S JOURNEY
  ┌─────────┐   DNS    ┌──────────┐   HTTP/S   ┌──────────────┐
  │ CLIENT  │────(3)──▶│  PROXY/  │───(6)─────▶│ LOAD BALANCER│
  │ (1)     │  IP (2)  │ REV PROXY│  API (7)   │    (13)      │
  └─────────┘          │   (4)    │ REST/GraphQL└──────┬───────┘
       ▲   latency(5)  └──────────┘   (8,9)            │
       │                                    ┌──────────┴──────────┐
       │                                    ▼                     ▼
       │                              ┌───────────┐        ┌───────────┐
       │  WebSockets(23) Webhooks(24) │  SERVER   │ ...    │  SERVER   │  ← Horizontal
       │  Msg Queue(26)  Rate lim(27) │ (scale:   │        │           │    scaling (12)
       │  API GW(28) Idempotency(29)  │  V↑11 H→12)        └───────────┘
       │  Microservices(25)           └─────┬─────┘
       │                                    │
       │        ┌───────────────────────────┼──────────────────────┐
       ▼        ▼                            ▼                       ▼
  ┌────────┐ ┌───────┐              ┌─────────────────┐     ┌──────────────┐
  │  CDN   │ │ CACHE │              │   DATABASE      │     │ BLOB STORAGE │
  │  (22)  │ │ (18)  │              │ SQL vs NoSQL(10,11)   │  S3 (21)     │
  └────────┘ └───────┘              │ Index(14) Repl(15)    └──────────────┘
                                    │ Shard(16) VPart(17)   │
   CAP theorem (30) governs         │ Denormalize(19)       │
   the whole distributed picture    └─────────────────┘
```

---

# PART A — Networking & Communication (concepts 1–9)

## 1. Client–Server Architecture
The foundation of almost every web app.

```
  CLIENT                                    SERVER
 (browser,     ── request (store/get/modify) ─▶  (always-on machine
  mobile app,  ◀── response (data or error) ──   waiting for requests)
  frontend)
```
- **Client** initiates requests. **Server** runs continuously, processes them, returns a response.
- Everything below is about making this loop *fast, scalable, reliable*.

## 2. IP Address
> "Phone number for a server." Every publicly deployed server has a unique IP.

- Client needs an **address** to reach a server. Computers identify each other by **IP address** (e.g. `93.184.216.34`).
- Problem: humans can't remember IPs → leads to **DNS**.

## 3. DNS (Domain Name System)
> The internet's phone book: maps human-friendly **domain names** → **IP addresses**.

```
 you type "algomaster.io"
        │
        ▼
   ┌─────────┐  "what's the IP for algomaster.io?"   ┌────────────┐
   │ BROWSER │ ─────────────────────────────────────▶│ DNS SERVER │
   │         │ ◀──────────── 93.184.x.x ─────────────│            │
   └─────────┘                                        └────────────┘
        │  now connect directly to that IP
        ▼
     SERVER
```
- Try it: `ping algomaster.io` shows the resolved IP.
- **Interview tie-in:** DNS can also do load balancing (return different IPs) and geo-routing (nearest data center).

## 4. Proxy vs Reverse Proxy
A middleman between client and server. **Which side it hides tells you which one it is.**

```
 FORWARD PROXY (hides the CLIENT)          REVERSE PROXY (hides the SERVERS)
  client ─▶ [proxy] ─▶ internet ─▶ server   client ─▶ [rev proxy] ─▶ backend servers
            hides your IP / location                   routes by rules, hides topology
```
| | Forward Proxy | Reverse Proxy |
|---|---|---|
| Sits in front of | the **client** | the **server(s)** |
| Hides | client's identity/IP | backend structure |
| Use cases | privacy, corporate filtering, caching | load balancing, SSL termination, caching, security (Nginx, HAProxy) |

- A **load balancer (13)** is a type of reverse proxy.

## 5. Latency
> The **round-trip delay** between request and response. Biggest cause: **physical distance**.

```
 User in India ──────────── request ──────────▶ Server in New York
               ◀──────────── response ──────────
               (data travels halfway around the world → high latency)
```
- **Fix:** deploy across **multiple data centers** worldwide → users hit the *nearest* one. (Also see CDN (22).)
- **Interview vocab:** *latency* = delay per request; *throughput* = requests/sec. You often trade one for the other.

## 6. HTTP / HTTPS
The rules (protocol) for client–server communication over the web.

```
 REQUEST                                  RESPONSE
 ┌────────────────────┐                   ┌────────────────────┐
 │ Header: method,     │  ──────────────▶ │ Status code (200,   │
 │  browser, cookies   │                   │  404, 500...)       │
 │ Body: form data etc │  ◀────────────── │ Body: data or error │
 └────────────────────┘                   └────────────────────┘
```
- **HTTP flaw:** sends data in **plain text** → anyone intercepting can read it.
- **HTTPS = HTTP + SSL/TLS encryption.** Even if intercepted, data can't be read or altered. Non-negotiable for fintech.

## 7. APIs (Application Programming Interfaces)
> HTTP just *transfers* data; it doesn't define **structure/format**. APIs are the contract that does.

```
 CLIENT ─request─▶ API (on server) ─▶ talks to DB / other services
        ◀─────────  returns structured response (JSON or XML)
```
- API = the middleman so clients don't worry about low-level details.
- Two dominant styles → **REST (8)** and **GraphQL (9)**.

## 8. REST API
> The most widely used style. Rules for structured HTTP communication.

- **Stateless** — every request is independent (server keeps no session between calls).
- Everything is a **resource** (users, orders, products).
- Standard HTTP methods:

| Method | Action | Example |
|---|---|---|
| `GET` | read | `GET /users/42` |
| `POST` | create | `POST /orders` |
| `PUT` | update | `PUT /users/42` |
| `DELETE` | remove | `DELETE /orders/9` |

- ✅ Simple, scalable, **easy to cache**.
- ❌ **Over-fetching** (returns more than needed) / **under-fetching** (need many calls). E.g. user profile + recent posts = multiple round trips.

## 9. GraphQL
> Introduced by Facebook (2015). Client asks for **exactly** what it needs — nothing more, nothing less.

```
 REST:  GET /user/42   +   GET /user/42/posts   (2 round trips, extra fields)
 GraphQL:  one query  ─▶  { user(id:42){name, posts{ title } } }
           server returns ONLY those fields, in ONE request
```
- ✅ Solves over/under-fetching; one endpoint; great for complex nested data.
- ❌ More **server-side processing**; **harder to cache** than REST; complexity.
- **Rule of thumb:** REST for simple CRUD/public APIs; GraphQL for rich, client-driven data needs.

---

# PART B — Data Storage & Scaling (concepts 10–19)

## 10–11. Databases: SQL vs NoSQL
Small data → a variable/file in memory. Real apps → a dedicated **database** server (secure, consistent, durable).

```
 CLIENT ─▶ SERVER ─▶ DATABASE ─▶ fetch/store ─▶ back to client
```

| | **SQL (Relational)** | **NoSQL** |
|---|---|---|
| Schema | strict, predefined | flexible / none |
| Structure | tables + relationships | key-value, document, graph, wide-column |
| Guarantees | **ACID** (strong consistency) | high scalability & performance |
| Best for | banking, structured relational data | huge scale, flexible/evolving data |
| Examples | MySQL, PostgreSQL | MongoDB, Cassandra, DynamoDB, Redis |

- **ACID** = Atomicity, Consistency, Isolation, Durability. **Critical for fintech** — you can't half-complete a money transfer.
- **Decision rule:** need structure + strong consistency → **SQL**. Need scale + flexible schema → **NoSQL**. Many apps use **both**.

## 11 (scaling intro). Vertical Scaling ("Scaling Up")
> Make **one machine** more powerful: add CPU / RAM / storage.

- ✅ Quick, simple, no code change.
- ❌ **Hard ceiling** (max hardware); ❌ cost grows **exponentially**; ❌ **single point of failure** (that one box dies → whole system down).

## 12. Horizontal Scaling ("Scaling Out")
> Add **more machines** and share the load.

```
        ┌── Server A
 traffic├── Server B    more servers = more capacity
        └── Server C    one dies → others take over (fault tolerant)
```
- ✅ Near-unlimited growth, ✅ fault tolerance.
- ❌ New problem: *which server does a client talk to?* → **Load Balancer (13)**.
- **Interview line:** "Vertical = bigger box (quick, capped). Horizontal = more boxes (scalable, fault-tolerant, needs a load balancer)."

## 13. Load Balancer
> A reverse proxy that distributes requests across backend servers, and reroutes away from unhealthy ones.

```
 clients ─▶ ┌───────────────┐ ─▶ Server 1 (healthy)
            │ LOAD BALANCER │ ─▶ Server 2 (healthy)
            └───────────────┘ ─X  Server 3 (down → skipped)
```
**Load-balancing algorithms:**
- **Round-robin** — rotate through servers in order.
- **Least connections** — send to the server with fewest active connections.
- **IP hashing** — hash client IP → same client sticks to same server (session affinity).

## 14. Database Indexing
> Like a book's index — jump straight to the data instead of scanning every page.

```
 Without index: scan ALL rows        With index: B-tree lookup → pointer → row
 (full table scan, O(n))             (O(log n))
```
- An index stores **column values + pointers** to the actual rows.
- Index columns that are **frequently queried**: primary keys, foreign keys, WHERE-clause columns.
- ⚠️ **Trade-off:** speeds up **reads**, slows down **writes** (index must update on every change) and uses storage. → Only index hot columns.

## 15. Replication
> Keep **copies** of the DB across servers to scale **reads** and improve availability.

```
        writes            async copy
 app ─────────▶ PRIMARY ───────────▶ Read Replica 1 ─▶ reads
                (leader)   ───────────▶ Read Replica 2 ─▶ reads
                           ───────────▶ Read Replica 3 ─▶ reads
```
- **Primary** handles all **writes**; **read replicas** handle **reads**, kept in sync.
- ✅ Scales **read-heavy** apps; ✅ availability — if primary fails, a replica is **promoted** to new primary.
- ⚠️ **Replication lag** → replicas can be slightly stale (eventual consistency). Matters for fintech reads-after-write.

## 16. Sharding (Horizontal Partitioning)
> Split the DB into smaller pieces (**shards**) across servers, divided by **rows**. For scaling **writes** & huge data.

```
 Users table (terabytes)  ──shard key = user_id──▶
   Shard A: user_id 1..1M     Shard B: 1M..2M     Shard C: 2M..3M
```
- Each shard holds a **subset**; distributed by a **shard key** (e.g. `user_id`).
- ✅ Reduces load per shard; ✅ scales reads **and** writes.
- ⚠️ Hard part: choosing a good shard key (avoid **hotspots**), cross-shard queries/joins get painful.
- **= Horizontal partitioning** (splits by **rows**).

## 17. Vertical Partitioning
> Split a table by **columns** — when the problem is too many columns, not too many rows.

```
 users(profile, login_history, billing)  ──split──▶
   user_profile │ user_login │ user_billing   (query scans only what it needs)
```
- ✅ Faster queries (scan fewer columns), ✅ less disk I/O.
- Contrast: **Sharding = by rows; Vertical partitioning = by columns.**

## 18. Caching
> Store frequently accessed data in **memory** (fast) instead of hitting the DB/disk (slow) every time. (Redis, Memcached.)

**Cache-aside pattern (most common):**
```
 request ─▶ check CACHE ──hit──▶ return instantly ✅
                 │
                miss
                 ▼
            read DB ─▶ store in cache ─▶ return
            (next time it's a hit)
```
- **TTL (Time To Live):** expire entries so stale data isn't served forever.
- **Interview follow-ups:** eviction policies (**LRU**, LFU), write strategies (write-through vs write-back), thundering herd / cache stampede.

## 19. Denormalization
> Deliberately **duplicate** data to avoid expensive **joins**. Trade storage for read speed.

```
 Normalized:  users ⨝ orders   (JOIN on every read — slower as data grows)
 Denormalized: user_orders table  (user details + latest orders together → no join)
```
- Normalization reduces redundancy but adds **joins**; denormalization removes joins by combining tables.
- ✅ Faster reads — used in **read-heavy** apps.
- ❌ More storage, and **updates get harder** (must update duplicated copies everywhere).

---

# PART C — Distributed Systems & Real-Time (concepts 20–30)

## 20 → 30. CAP Theorem
> In a distributed system you can only have **2 of 3**: **C**onsistency, **A**vailability, **P**artition tolerance.

```
        C  Consistency (every read sees the latest write)
       / \
      /   \
     A─────P
 Availability   Partition tolerance
 (always responds)  (works despite network splits)
```
- Network partitions **will** happen → **P is mandatory**. So the real choice is:
    - **CP** — stay consistent, sacrifice availability during a partition (e.g. banking, most SQL setups).
    - **AP** — stay available, tolerate stale data (e.g. social feeds, DNS, Cassandra/DynamoDB).
- **Fintech instinct:** money → lean **CP** (correctness over uptime for the write path).

## 21. Blob Storage (e.g. Amazon S3)
> For large **unstructured** files (images, videos, PDFs) that DBs handle poorly.

```
 Bucket / container
   ├── image.png   → unique URL
   ├── video.mp4   → unique URL   (retrieve & serve over the web)└── report.pdf  → unique URL
```
- ✅ Scalability, ✅ pay-as-you-go, ✅ automatic replication, ✅ easy URL access.
- Common combo: store the file in blob storage, store just the **URL** in your database.

## 22. CDN (Content Delivery Network)
> A global network of servers that caches content **close to users** for faster delivery.

```
 Origin (California)                    User in India
        │                                    ▲
        │ push/pull content                  │ served from NEAREST edge
        ▼                                     │
   ┌── CDN edge (Mumbai) ───────────────────┘
   ├── CDN edge (Frankfurt)
   └── CDN edge (Tokyo)
```
- Serves static assets (HTML, JS, images, video) from the closest **edge server** → less buffering, faster loads.
- Solves the "streaming a video hosted across the world is slow" problem. Complements **Latency (5)**.

## 23. WebSockets
> **Persistent, two-way** connection for **real-time** apps. Replaces inefficient HTTP polling.

```
 HTTP polling (bad):  client ─?─▶ server, ─?─▶, ─?─▶  (repeated, mostly empty)
 WebSocket (good):    client ⇄══════ persistent open connection ══════⇄ server
                      server can PUSH anytime; client sends anytime
```
- Use for: live chat, stock dashboards, multiplayer games, live notifications.
- vs polling: no wasted bandwidth / server load from empty responses.

## 24. Webhooks
> **Server-to-server** notification: "**don't call us, we'll call you**" when an event happens.

```
 1. Your app registers a webhook URL with the provider (e.g. payment gateway)
 2. Event occurs (payment succeeds)
 3. Provider sends HTTP POST ─▶ your webhook URL, with event details
```
- Replaces wasteful **polling an API** to check "did it happen yet?"
- Classic fintech use: **payment gateway → your server** on payment success/failure. (Pair with **Idempotency (29)** — providers retry webhooks!)

## 25. Microservices
> Break a **monolith** (one big codebase) into small, independent services.

```
 MONOLITH                        MICROSERVICES
 ┌────────────────┐              ┌────────┐ ┌────────┐ ┌────────┐
 │ auth+orders+   │      ──▶     │ Auth   │ │ Orders │ │Payments│
 │ payments+...   │              │ svc+DB │ │ svc+DB │ │ svc+DB │
 └────────────────┘              └────────┘ └────────┘ └────────┘
 hard to scale/deploy            each: single responsibility,
                                 own DB, scale & deploy independently
```
- Communicate via **APIs** or **message queues (26)**.
- ✅ Independent scaling/deployment, fault isolation.
- ❌ Operational complexity, distributed transactions, network overhead.

## 26. Message Queues
> **Asynchronous** communication between services — decouples producer from consumer. (Kafka, RabbitMQ, SQS.)

```
 PRODUCER ─put msg─▶ [ QUEUE holds messages ] ─▶ CONSUMER pulls & processes
```
- **Synchronous** (wait for immediate response) doesn't scale; a queue lets requests be processed **without blocking**.
- ✅ Decouples services, ✅ smooths load spikes (buffer), ✅ prevents overload of internal services.
- Enables retries, and (with idempotency) reliable processing.

## 27. Rate Limiting
> Cap how many requests a client can make in a time window — protects **public** APIs from abuse/overload.

```
 client quota: 100 req/min
 req 1..100 ✅   req 101 within the minute ─▶ 429 Too Many Requests ❌
```
- Stops bots/DDoS from consuming all resources and degrading service for real users.
- **Algorithms:** fixed window, sliding window, **token bucket** (know these names).

## 28. API Gateway
> Single **entry point** for all client requests in a microservices system. Handles cross-cutting concerns so services don't each reimplement them.

```
 clients ─▶ ┌─────────────┐ ─▶ Auth service
            │ API GATEWAY │ ─▶ Orders service
            │ auth, rate  │ ─▶ Payments service
            │ limit, log, │
            │ routing     │
            └─────────────┘
```
- Does: **authentication, rate limiting, logging, monitoring, request routing**.
- ✅ Simplifies management, improves security & scalability; clients don't touch services directly.

## 29. Idempotency
> A repeated request produces the **same result** as making it **once**. Essential where retries/duplicates happen (fintech!).

```
 Each request → unique ID (idempotency key)
   ┌─ seen this ID before?  ── yes ─▶ ignore duplicate, return prior result
   └─                          no  ─▶ process normally, record the ID
```
- Scenario: user refreshes the payment page → 2 charge requests arrive → only **one** charge happens.
- Directly maps to your **T4 fintech prep**: idempotency keys prevent double-charge/double-trade. Pair with **webhooks (24)** and **message queues (26)**, which retry by design.

---

## Quick-reference cheat sheet

| # | Concept | One-liner | Solves |
|---|---------|-----------|--------|
| 1 | Client–Server | request/response loop | foundation |
| 2 | IP Address | server's phone number | addressing |
| 3 | DNS | domain → IP | human-friendly names |
| 4 | Proxy / Reverse Proxy | middleman (hides client / servers) | privacy / routing |
| 5 | Latency | round-trip delay | speed |
| 6 | HTTP/HTTPS | comm protocol (+TLS encryption) | secure transport |
| 7 | API | structured client↔server contract | interoperability |
| 8 | REST | stateless, resources, HTTP verbs | simple CRUD |
| 9 | GraphQL | fetch exactly what you need | over/under-fetching |
| 10 | SQL DB | tables, schema, ACID | consistency |
| 11 | NoSQL DB | flexible schema, scalable | scale/flexibility |
| 11b | Vertical Scaling | bigger machine | quick capacity |
| 12 | Horizontal Scaling | more machines | scale + fault tolerance |
| 13 | Load Balancer | distributes traffic | HS routing |
| 14 | Indexing | fast lookup table | read speed |
| 15 | Replication | copies for reads/failover | read scale + availability |
| 16 | Sharding | split by rows | write scale + big data |
| 17 | Vertical Partitioning | split by columns | narrow queries |
| 18 | Caching | data in memory + TTL | read speed |
| 19 | Denormalization | duplicate to avoid joins | read speed |
| 20 | CAP Theorem | pick 2 of C/A/P | distributed trade-off |
| 21 | Blob Storage | large files, URLs (S3) | unstructured data |
| 22 | CDN | edge servers near users | latency for static content |
| 23 | WebSockets | persistent 2-way | real-time client↔server |
| 24 | Webhooks | event push server→server | avoid polling |
| 25 | Microservices | small independent services | scale/deploy independently |
| 26 | Message Queue | async producer/consumer | decoupling |
| 27 | Rate Limiting | cap requests/window | abuse/overload |
| 28 | API Gateway | single entry point | cross-cutting concerns |
| 29 | Idempotency | same result on retry | duplicate safety |

---

## Fintech interview angles (why these 30 matter for *your* rounds)
- **ACID + SQL (10)** and **CAP → CP (20)** — you'll justify strong consistency for money.
- **Idempotency (29)** — the single most-asked fintech design point: no double-charge.- **Message queues (26)** + **webhooks (24)** — payment async flows, and why they need idempotency.
- **Replication (15)** vs **sharding (16)** — scaling a ledger/transactions DB; replication lag vs read-after-write.
- **Rate limiting (27)** + **API gateway (28)** — protecting payment endpoints.
- **Optimistic locking / audit logging** (from your roadmap T4) build directly on these.

---

## My own questions (add as they come up)
<!-- Somesh: jot clarifications/questions here as we drill each concept -->