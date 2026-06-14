# Part 5: Monolith vs. Microservices & Decomposition Patterns

This is the most important set of patterns in microservices. According to Concept & Coding, around **50 interview questions** in HLD rounds come from this topic alone. Let's cover it all.

---

## 1. Monolithic Architecture (Legacy Applications)

A **monolith** is a single deployable unit where all business functionality lives together in one codebase.

For example, an online store monolith has everything inside one application:
* Order management
* Product/Inventory management
* Account & Login management
* Billing & Payment

```
         [ Single Monolithic Application ]
  ┌─────────────────────────────────────────────┐
  │  Order Module   │  Payment Module           │
  │─────────────────│───────────────────────────│
  │  Product Module │  Account Module           │
  └────────────────────────────┬────────────────┘
                               │
                               ▼
                      [ Single Database ]
```

### Disadvantages of Monolithic Architecture

#### 1. Overloaded IDE
Because all modules are in one codebase, the application can grow to **GBs in size**. Even opening the project in an IDE (like IntelliJ or Eclipse) becomes extremely slow. Developers waste time just loading the project.

#### 2. Scaling is Very Hard (and Expensive)
Suppose your *Order* module gets a spike of heavy traffic. You want to scale just that part.

In a monolith — **you cannot**. You have to spin up an **entire new copy** of the whole application (all GBs of it) just to handle extra Order traffic.

This means you are paying for and running extra copies of Payment, Product, Login etc. even though they don't need scaling. It is **not cost-efficient**.

#### 3. Very Tight Coupling (Deployment is a Nightmare)
All modules in a monolith share the same codebase. So:
* Fixing **one bug in one line** requires running the **entire regression test suite** for the whole application.
* Then you deploy the **entire application** again — even if only 1 line changed.
* A change in the Order module might accidentally break the Payment module because they share common code.

This is called being **Tightly Coupled**. Any change anywhere has the potential to break everything.

#### 4. Transaction Management is Hard
With a single shared database, managing transactions (ACID properties) is straightforward:
```
START TRANSACTION → Do operations in DB → COMMIT or ROLLBACK
```
But when the business grows and we need to split the system, transaction management becomes complex (more on this in Part 6).

---

## 2. Why Microservices? (All Disadvantages of Monolith Become Advantages)

In a **microservices architecture**, we break the single monolith into small, independent services. Each service:
* Focuses on **one business capability** only
* Has its **own codebase** and **own database**
* Can be **deployed independently**
* Can be **scaled independently**

```
  [ Order     ]  [ Payment   ]  [ Product   ]  [ Account  ]
  [ Service   ]  [ Service   ]  [ Service   ]  [ Service  ]
      │               │               │               │
  [ Order DB  ]  [Payment DB ]  [Product DB ]  [Account DB]
```

### Advantages of Microservices
| Monolith (Disadvantage) | Microservices (Advantage) |
| :--- | :--- |
| Entire app redeployed for any change | Deploy only the changed service |
| Entire app scaled even for one module | Scale only the service that needs it (cost-efficient) |
| One bug can break everything | Bug in Order Service does not affect Payment Service |
| Overloaded IDE with massive codebase | Small, fast, focused codebases |
| One technology stack for everything | Each service can use the best DB/language for its job |

### Disadvantages of Microservices (Important for Interviews!)

#### 1. Need for Proper Decomposition
Breaking a monolith into microservices must be done carefully. If services are **not loosely coupled**, you lose all the benefits.
* **Example of bad design:** If changing Service A requires changing Service B and Service C too, you have just created a **distributed monolith** — the worst of both worlds.
* **Loosely coupled** means: You can change, deploy, and scale one service without affecting any other.

#### 2. Debugging & Monitoring is Complex
In a monolith, there is one log file. In microservices, a single user request might travel through Service 1 → Service 2 → Service 3.

If Service 3 crashes:
* Team 1 sees Service 1 is reporting a failure → blames Service 2.
* Team 2 investigates → blames Service 3.
* Team 3 fixes the actual bug.

Just **finding where the error happened** across service boundaries becomes complex. This is why tools like **Distributed Tracing (Jaeger, Zipkin)** are critical.

#### 3. Transaction Management is Hard (Key Challenge)
Each service has its own database. You cannot run a single SQL transaction across multiple databases. This is the biggest technical challenge in microservices (solved by the **Saga Pattern** in Part 6).

---

## 3. The Phases of Microservices Design

Building a microservices system involves solving problems in distinct phases. Each phase has established design patterns to guide you:

```
  ┌──────────────────────────────────────────────────────────────────┐
  │                 Phases of Microservices Design                   │
  ├──────────────────┬───────────────┬──────────────────┬───────────┤
  │  DECOMPOSITION   │   DATABASE    │  COMMUNICATION   │INTEGRATION│
  │                  │               │                  │           │
  │ How do we break  │ Should each   │ How do services  │ How do we │
  │ the monolith     │ service have  │ talk to each     │ expose    │
  │ into services?   │ its own DB?   │ other?           │ services? │
  │                  │               │                  │           │
  │ • By Business    │ • DB per      │ • REST/gRPC      │• API      │
  │   Capability     │   Service     │   (Sync)         │  Gateway  │
  │ • By Sub-domain  │ • Shared DB   │ • Message Broker │• BFF      │
  │   (DDD)          │   (avoid!)    │   (Async/Kafka)  │           │
  └──────────────────┴───────────────┴──────────────────┴───────────┘
                                         + OBSERVABILITY (Logging, Tracing, Metrics)
```

---

## 4. Decomposition Patterns: How to Break the Monolith?

**Decomposition** answers the question: *"How small should a microservice be?"*

There is **no fixed size** for a microservice. For one company, an Order Management service (with 10 features) might be a "micro" service. For another, each of those 10 features might need its own service. It entirely depends on your scale and team structure.

Two patterns guide this decision:

---

### Pattern 1: Decompose by Business Capability

Break the system based on **what business function the code performs**.

An online store can be decomposed like this:

| Business Capability | Microservice Created |
| :--- | :--- |
| Managing orders (create, track, cancel) | *Order Service* |
| Managing products and stock levels | *Product/Inventory Service* |
| Managing user accounts and authentication | *Account Service* |
| Managing login sessions | *Auth/Login Service* |
| Generating bills and receipts | *Billing Service* |
| Processing credit card and UPI payments | *Payment Service* |

```
         Before (Monolith)              After (Business Capability)
     ┌──────────────────────┐         ┌────────┐ ┌─────────┐ ┌────────┐
     │ Order + Product +    │  ────►  │ Order  │ │ Product │ │Account │
     │ Account + Billing +  │         │ Svc    │ │ Svc     │ │ Svc    │
     │ Payment in one app   │         └────────┘ └─────────┘ └────────┘
     └──────────────────────┘         ┌─────────┐ ┌─────────┐
                                      │ Billing │ │Payment  │
                                      │ Svc     │ │ Svc     │
                                      └─────────┘ └─────────┘
```

**Challenge:** You need a **good, deep understanding of your business** to correctly identify business capabilities. If business boundaries are unclear or overlap, services will end up tightly coupled again.

---

### Pattern 2: Decompose by Sub-domain (Domain-Driven Design - DDD)

This pattern comes from **Domain-Driven Design (DDD)** by Eric Evans. Instead of thinking about code functions, we think about **business domains** and their boundaries.

Key DDD concepts:
* **Domain:** The overall business problem you are solving (e.g., "e-commerce").
* **Sub-domain:** A specific area within the domain (e.g., "Order Management", "Customer Support").
* **Bounded Context:** A clear boundary within which a specific model (set of terms, entities, rules) applies and makes sense.

Think of it this way: The word **"Customer"** in an *Order Service* means a person placing an order (with address, items, history). In a *Support Service*, it means a person who needs help (with tickets, issues, chat history). **Same word, different meaning** — these are different Bounded Contexts.

```
  ┌─────────────────────────┐      ┌──────────────────────────┐
  │  Order Bounded Context  │      │  Support Bounded Context  │
  │                         │      │                          │
  │ Customer = {            │      │ Customer = {             │
  │   name, address,        │      │   name, tickets,         │
  │   orderHistory          │      │   openIssues             │
  │ }                       │      │ }                        │
  └─────────────────────────┘      └──────────────────────────┘
```

DDD gives us a systematic, business-aligned way to draw service boundaries so services remain naturally loosely coupled.

---

## 5. Putting It All Together: Interview Summary

### Q1: What is the difference between Monolith and Microservices?
* **Monolith:** One large application, one DB, all features tightly coupled, harder to scale.
* **Microservices:** Split into small independent services, each with its own DB, independently deployable and scalable.

### Q2: What are the disadvantages of Microservices?
1. Services must be **properly decomposed** (otherwise you get a distributed monolith with high latency).
2. **Debugging is complex** (errors span multiple services and logs).
3. **Transaction management is hard** (no ACID across multiple databases — solved by Saga Pattern).

### Q3: What are the two main decomposition patterns?
1. **Business Capability:** Divide services by *what business function* they perform.
2. **Sub-domain / DDD:** Divide services by *bounded business contexts*, giving each its own clear vocabulary and rules.

### Q4: What is Loose Coupling and why does it matter in Microservices?
**Loose Coupling** means you can change, deploy, or scale one service without impacting any other service. This is the primary goal of microservices. If your services are **tightly coupled**, you have not truly decomposed your system — you just have a distributed monolith.
