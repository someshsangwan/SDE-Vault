# Scalability

> **Status:** Learning · **Started:** 2026-05-10
> **Source:** _System Design Interview Handbook_ (AlgoMaster), Fundamentals §1
> **Why first:** every other System Design topic — load balancers, caching, sharding, message queues — exists *because of* scalability needs. Understand this and the rest becomes "how do we achieve scalability."

---

## 1. The Book's Definition

> "Scalability is the property of a system to handle a growing amount of load by adding resources to the system. A system that can continuously evolve to support a growing amount of work is **scalable**."

Three words to internalize:
- **growing load** — more requests, more data, more users, or more features
- **adding resources** — money/hardware/instances *can* fix it
- **continuously** — it's not a one-time fix; the system keeps working as load grows further

A system that hits a hard wall (cannot scale further regardless of money) is **not scalable**, even if it currently performs well.

---

## 2. Scalability vs Performance — Don't Confuse Them

| | Performance | Scalability |
|---|---|---|
| **Question it answers** | "How fast for one user?" | "How well as load grows?" |
| **Measured by** | Latency, response time | How throughput / latency change with N users |
| **Example fix** | Faster query, better algorithm | Add more servers, shard the DB |
| **Trade-off** | Often improves single-user latency | Often *worsens* single-user latency (network hops, replication delay) but enables 1000× more users |

A system can be **fast but not scalable** (one beefy server, fast responses, dies at 10k users) or **slow but scalable** (every request takes 500ms, but handles 100M users gracefully).

---

## 3. The Three Axes of Scale (AKF Cube — interview gold)

When someone says "we need to scale," ask **which dimension?** Typically one of:

| Axis | What grows | Example | Common solution |
|---|---|---|---|
| **Load (traffic)** | Requests per second | Black Friday spike: 1k → 100k RPS | More app servers behind a load balancer |
| **Data** | Storage size | 1 GB → 100 TB of payment records | Sharding, archival, columnar storage |
| **Users / Tenants** | Distinct user accounts or organizations | SaaS going from 100 → 10M tenants | Multi-tenancy strategy, partition by tenant |
| **Geographic** | Distance to users | Tokyo-only → global product | CDN, multi-region deployment, edge compute |
| **Functional / team** | # of features and devs | 1 monolith → 50 microservices | Service decomposition, independent deploys |

In an interview, **"how do we scale this?"** is too vague. Pin down which axis is hurting first.

---

## 4. Vertical vs Horizontal Scaling (preview of Trade-offs §1)

The two fundamental scaling strategies. The book covers this in detail later; here's the summary you need now.

### Vertical (scale up)
**Make the existing machine bigger.** More CPU, more RAM, more disk, faster network card.

```
Before:  [ 4 vCPU, 16 GB RAM ]   →   After:  [ 32 vCPU, 256 GB RAM ]
```

✅ Pros: simple (no code changes), no distributed-system complexity, lower latency (everything in-process).
❌ Cons:
- **Hard physical ceiling** — you can't keep buying bigger boxes forever (AWS x1e.32xlarge tops out around 4 TB RAM).
- **Single point of failure** — that one beefy box goes down → site is down.
- **Expensive non-linearly** — a 2× bigger machine costs significantly more than 2 smaller ones.
- **Downtime for upgrades** — usually requires a restart.

### Horizontal (scale out)
**Add more machines.** Distribute the load across them.

```
Before:  [ 1 server ]   →   After:  [ Server ] [ Server ] [ Server ] [ Server ]
                                            ↑
                                      Load Balancer
```

✅ Pros:
- **Near-limitless ceiling** (if your architecture allows it).
- **Fault tolerance** — one server dies, others keep serving.
- **Linear cost** (roughly) — 4 commodity servers cost ~4× one.
- **Rolling deploys** — update one server at a time, no downtime.
❌ Cons:
- **Distributed-system complexity**: coordination, consistency, network partitions.
- **App must be designed for it** — see [[03-stateful-vs-stateless]] (state must move out of the app).
- **More infra to manage** — load balancers, service discovery, health checks.

### The real-world hybrid

Almost every production system does **both**. Pick a reasonably large machine (vertical) and run several of them behind a load balancer (horizontal). At Rakuten Pay scale, you'd run *many* moderately-sized JVM instances rather than one mega-instance.

---

## 5. Why Most Modern Systems Default to Horizontal

Three reasons:

1. **Commodity hardware is cheaper per unit of work.** 4 small machines beat 1 big machine on $/RPS in most cases.
2. **Failures are routine, not exceptional.** Cloud VMs reboot, get evicted, hit hardware faults. Horizontal scaling assumes this and survives it; vertical scaling pretends it won't happen.
3. **Cloud-native tooling assumes horizontal.** Kubernetes, autoscaling groups, load balancers — all designed for "many small instances."

The corollary is the **stateless service mantra** (book Trade-off §3): if your app holds state in local memory (sessions, cache, user data), you cannot horizontally scale it without losing that state on every request that hits a different instance. That's why we externalize state to Redis / DB / object storage.

---

## 6. Spring Boot — How a Java Service Actually Scales

Concretely, what does scaling a Spring Boot payment service look like?

### Step 1: Vertical, until it hurts
- Tune JVM heap (`-Xmx`, GC settings).
- Tune Tomcat thread pool (`server.tomcat.threads.max`).
- Tune HikariCP connection pool to match DB capacity.
- Optimize hot queries, add DB indexes.
- Profile with async-profiler / JFR; fix the worst offenders.

This buys you 2–10× depending on where you started.

### Step 2: Make it stateless
- Move HTTP sessions out of the JVM heap → Spring Session + Redis.
- Move local caches → Redis or Memcached.
- No "this user is currently on server X" assumptions.

Now any request can hit any instance.

### Step 3: Horizontal — run N instances behind a load balancer
- Containerize (Docker) → run on Kubernetes, ECS, or auto-scaling group.
- Front them with an ALB / NGINX / API Gateway.
- Configure health checks (`/actuator/health`) so the LB removes dead instances.
- Configure HPA (Horizontal Pod Autoscaler) on CPU or custom metrics → instances scale up automatically.

### Step 4: Scale the database (the hardest part)
The app tier is easy to scale. The DB usually isn't. Common moves:
- **Read replicas** — multiple read-only DB copies; route reads to them, writes to primary.
- **Connection pooling at scale** — too many app instances × HikariCP = DB connection exhaustion. Use PgBouncer / RDS Proxy.
- **Vertical scale the DB** for as long as you can.
- **Sharding** when one DB can't hold the data — see [[13-database-sharding]].
- **Caching aggressively** — Redis in front of DB reads — see [[11-caching]].

### Step 5: Decompose
When the monolith itself becomes the bottleneck (deploys are scary, teams stepping on each other), split into **microservices** — each scales independently. Now you're scaling on the *functional* axis.

---

## 7. The Common Bottlenecks (in order you usually hit them)

When a system fails to scale, it's almost always one of these:

1. **CPU on the app tier** — easy fix: add more app instances.
2. **DB connection pool exhausted** — fix: pool tuning + read replicas + caching.
3. **DB CPU/IO** — fix: indexes, query optimization, read replicas, sharding.
4. **Locks / synchronized blocks in the app** — fix: reduce critical sections, lock-free data structures, partition the contended resource.
5. **Network bandwidth between services** — fix: gRPC instead of REST, caching, batching.
6. **Thread pool starvation** — too few threads for blocking calls — fix: tune pool, virtual threads (Java 21), or go reactive.
7. **External dependencies** (3rd-party APIs, card networks) — fix: timeouts, retries with backoff, circuit breakers, async + queues.

In an interview, when asked "what would break first as load increases?" — the **DB** is almost always the right first answer.

---

## 8. Rakuten Pay Concrete Example

**Scenario:** Rakuten Pay sees 10× traffic on Black Friday checkout.

| Layer | What was good for 1× | What breaks at 10× | Fix |
|---|---|---|---|
| Mobile app / CDN | OK | OK (CDN absorbs static) | Already horizontal |
| API Gateway | 1 instance | Throughput cap | Auto-scale, more instances |
| Auth service | 4 instances | Auth cache hit rate drops, CPU spikes | Bigger Redis, more instances |
| Payment orchestrator | 6 instances | Tomcat pools full, queue depth grows | Add instances; consider Loom virtual threads |
| Fraud check service | External SaaS | Rate limit | Negotiate higher limits; pre-compute; circuit breaker |
| Card network gateway | Fixed throughput | Becomes the bottleneck | Queue + async retry; fall back to "we'll confirm shortly" UX |
| Payment DB (RDS Postgres) | Primary handles all | CPU 100%, connection waits | Read replicas; cache lookups in Redis |
| Audit log | Synchronous insert per txn | DB write contention | Move to Kafka → async consumer writes batches |

This is the kind of layered analysis interviewers want when they say "imagine traffic 10×s."

---

## 9. How to Measure Scalability — Metrics That Matter

Interviewers love when you talk in numbers, not adjectives.

| Metric | What it tells you |
|---|---|
| **Throughput (RPS / TPS)** | How much work the system completes per second |
| **Latency p50 / p95 / p99** | The shape of user experience — p99 = the worst 1% of users |
| **Saturation** | How close each resource is to its limit (CPU %, conn pool used, queue depth) |
| **Error rate** | % of requests failing (5xx, timeouts) |
| **Headroom** | (max capacity − current load). When this hits zero, you fall over. |

**The Universal Scalability Law (Gunther)** captures something important:

```
   Throughput grows roughly linearly with N (concurrency)
   ...until contention dominates...
   ...then it plateaus...
   ...then it actually *decreases* due to coordination cost.
```

Adding more capacity past a certain point makes things *worse* if your system has too much shared state / coordination. This is why "just add more servers" can fail — and why statelessness matters.

---

## 10. Anti-Patterns (Things That Look Like Scaling but Aren't)

- **Putting a cache in front without invalidation strategy** — temporarily faster, eventually serves stale data and someone files a bug.
- **Sharding too early** — operational complexity that doesn't pay off until you actually have data-volume problems.
- **Scaling the app tier when the DB is the bottleneck** — adds DB load with zero throughput gain.
- **Synchronous fan-out without timeouts** — one slow downstream service grinds N upstream instances to a halt (cascading failure).
- **No circuit breaker on external calls** — a misbehaving 3rd party can saturate your thread pool.

---

## 11. Self-Check Questions

1. A system handles 1k RPS comfortably. At 5k RPS, p99 latency goes from 100ms → 5s. Is the system "not scalable"? What's the precise question to ask before answering?
2. You doubled the number of app servers but throughput only went up 30%. Name three plausible causes.
3. Why does horizontal scaling almost require statelessness? What state are we talking about?
4. If your DB is the bottleneck at 10× load, list 4 different mitigations in *increasing order of complexity*.
5. What's the difference between scaling **load**, scaling **data**, and scaling **users**? Give a system where each is the dominant problem.
6. Why might "throwing more servers at it" actually decrease throughput?
7. In a microservices architecture, which service do you scale first when overall traffic spikes? How do you know?

---

## 12. Linked Notes

- [[02-availability]] — uptime guarantees; tightly coupled to scalability (a scaled system that's frequently down isn't useful)
- [[03-latency-vs-throughput]] — the two metrics that define how "scalable" feels in practice
- [[04-cap-theorem]] — the unavoidable trade-off when scaling stateful systems horizontally
- [[05-load-balancers]] — the mechanism that makes horizontal scaling possible
- [[11-caching]] — the cheapest scalability win for read-heavy workloads
- [[13-database-sharding]] — scaling the data axis
- Trade-offs §1: [[t-01-vertical-vs-horizontal]]
- Trade-offs §3: [[t-03-stateful-vs-stateless]]

---

## 13. Revision Schedule

- [ ] Day +1: re-explain scalability vs performance in 60 seconds
- [ ] Day +3: redraw the Rakuten Pay 10× scenario table from memory
- [ ] Day +7: answer all self-check questions
- [ ] Day +14: connect this note to availability + load balancers (read those notes)
- [ ] Day +30: be able to give a 5-minute "how to scale a web service" walkthrough on a whiteboard