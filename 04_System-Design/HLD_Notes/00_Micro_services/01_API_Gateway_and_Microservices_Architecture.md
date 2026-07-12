# Part 1: API Gateway and Microservices Architecture

This guide explains the role, mechanics, and scaling strategies of an API Gateway in a microservices architecture, drawing from the core concepts of Concept & Coding's system design course.

---

## 1. What is an API Gateway?
An **API Gateway** is a reverse proxy that serves as the single entry point for all client requests. It accepts incoming API requests from clients (mobile, web, third-party) and routes them to the correct backend microservice based on the API endpoint (URL path).

```
  Client ────────► [ API Gateway ]
                          │
         ┌────────────────┼────────────────┐
         ▼ (/invoice)     ▼ (/order)       ▼ (/sales)
  [ Invoice Service ]  [ Order Service ]  [ Sales Service ]
```

### The Key Difference: API Gateway vs. Load Balancer

| Feature | API Gateway | Load Balancer |
| :--- | :--- | :--- |
| **Routing Intelligence** | **Application Layer (Layer 7):** Understands URL paths, headers, and request contents. Decides *which microservice* to route to (e.g., `/invoice` vs `/order`). | **Transport/Application Layer (Layer 4/7):** Does not route between different services; it only distributes traffic between multiple instances of the *same* microservice. |
| **Capabilities** | Auth, Rate Limiting, API Composition, Protocol Translation, Service Discovery, Caching. | Strictly traffic distribution, health checking of instances, SSL offloading. |
| **Position in Path** | Receives request first (Entry point of the system). | Sits behind the API Gateway, fronting each individual microservice group. |

---

## 2. Core Capabilities of an API Gateway

Apart from basic routing, an API Gateway is an intelligent hub that handles multiple cross-cutting concerns:

### A. API Composition (Aggregation)
In a microservices architecture, a single frontend page may need data from multiple microservices (e.g., Product Details, Invoice Info, Ratings, Recommendations).
* **The Problem:** Having the client query each microservice directly causes excessive network roundtrips, increases latency, and wastes client bandwidth (especially on mobile devices with low bandwidth).
* **The Solution:** The client makes a single call (e.g., `/api/myorder`) to the API Gateway. The API Gateway orchestrates calls to the necessary backend services, aggregates the results, and returns a single response tailored to the client device.
  * **Device-specific payloads:** For a **Mobile Client**, the Gateway might only query Product & Invoice services. For a **PC Client** with more bandwidth and screen space, it queries Product, Invoice, Ratings, and Recommendation services.
  * **Real-world Example:** Netflix heavily utilizes API Composition at the Gateway level to optimize requests for various client devices.

```
                  ┌──────────────────────┐
                  │     API Gateway      │
                  │  (API Composition)   │
                  └─┬───┬───────────┬───┬┘
       ┌────────────┘   │           │   └────────────┐
       ▼                ▼           ▼                ▼
[ Product Ser. ]  [ Invoice Ser. ]  [ Ratings Ser. ]  [ Recs Ser. ]
```

### B. Authentication & Authorization
Instead of duplicating authentication logic in every microservice, the API Gateway centralizes security at the edge:
1. The client initially obtains an access token (e.g., JWT, OAuth 2.0) from the Authorization Server.
2. The client passes this token in the header of subsequent requests to the API Gateway.
3. The API Gateway validates the token.
4. Once validated, the Gateway forwards the request to downstream microservices, injecting user context headers (e.g., `X-User-Id`, `X-User-Role`) so downstream services don't have to re-authenticate the request.

### C. Rate Limiting & Throttling
Protects backend systems from being overwhelmed by traffic spikes or malicious actors.
* **Burst Limit:** The maximum concurrent requests the gateway can handle at a peak instant before rejecting traffic with a `429 Too Many Requests` error.
* **Throttling:** Fine-grained rules to limit requests based on specific criteria, such as:
  * **Endpoint limits:** `/api/invoice` can be called at most 10 times per minute.
  * **User-based limits:** Limit a specific authenticated user.
  * **IP-based blocking:** Block or restrict specific IP addresses.
* **API Queues:** Rather than immediately dropping excess requests during a spike (burst), the Gateway can hold requests in an API Queue (waiting area) until processing bandwidth becomes available. This helps mitigate **Thundering Herd** problems.

### D. Service Discovery Integration
Microservices scale up/down dynamically, meaning their IP addresses and ports change constantly.
* **Service Registry:** A registry (like Netflix Eureka, Consul, or ZooKeeper) keeps track of active microservice instances.
* **Registration Methods:**
  1. *Self-Registration:* Instances register themselves when scaling up and deregister when shutting down.
  2. *Active Health Checks:* The Service Discovery mechanism runs health checks to prune dead instances from the registry.
* **Gateway Role:** When a request for `/order` arrives, the API Gateway queries the Service Discovery tool to get the network location (IP/Port) of an active Order service instance (or its load balancer) and routes the request there.

### E. Request/Response Transformation & Caching
* **Transformation:** Modifies request headers, query parameters, or body payloads to match downstream requirements, or formats downstream responses to suit client needs.
* **Caching:** Cache responses for static or semi-static data (e.g., product catalogs) at the gateway edge to prevent unnecessary downstream calls.

---

## 3. High-Scale Architecture: Handling Millions of Requests per Second

How can an API Gateway be a "single entry point" without becoming a single point of failure (SPOF) or a scaling bottleneck?

In production, the architecture is distributed across multiple tiers, regions, and availability zones:

```
                              [ Client Browser / Mobile ]
                                           │
                                           ▼
                       ┌───────────────────────────────────────┐
                       │  DNS-Based Load Balancer (Route 53)   │
                       └───────────┬───────────────┬───────────┘
                                   │               │
                     ┌─────────────┘               └─────────────┐
                     ▼ (Geo / Latency Routing)                   ▼
             ┌───────────────┐                           ┌───────────────┐
             │   REGION 1    │                           │   REGION 2    │
             │   (Mumbai)    │                           │   (Chennai)   │
             └───────┬───────┘                           └───────┬───────┘
                     │                                           │
         ┌───────────┴───────────┐                   ┌───────────┴───────────┐
         ▼ (Availability Zone 1) ▼ (AZ 2)            ▼ (Availability Zone 1) ▼ (AZ 2)
  ┌──────────────┐        ┌──────────────┐    ┌──────────────┐        ┌──────────────┐
  │ API Gateway  │        │ API Gateway  │    │ API Gateway  │        │ API Gateway  │
  │  (Instance)  │        │  (Instance)  │    │  (Instance)  │        │  (Instance)  │
  └──────┬───────┘        └──────┬───────┘    └──────┬───────┘        └──────┬───────┘
         │                       │                   │                       │
         ▼                       ▼                   ▼                       ▼
  ┌──────────────┐        ┌──────────────┐    ┌──────────────┐        ┌──────────────┐
  │ Microservice │        │ Microservice │    │ Microservice │        │ Microservice │
  │Load Balancer │        │Load Balancer │    │Load Balancer │        │Load Balancer │
  └──────┬───────┘        └──────┬───────┘    └──────┬───────┘        └──────┬───────┘
         │                       │                   │                       │
   ┌─────┴─────┐           ┌─────┴─────┐       ┌─────┴─────┐           ┌─────┴─────┐
   ▼           ▼           ▼           ▼       ▼           ▼       ▼           ▼
[Inst 1]    [Inst 2]    [Inst 1]    [Inst 2] [Inst 1]    [Inst 2] [Inst 1]    [Inst 2]
```

### Step-by-Step Flow:
1. **Tier 1: DNS-Based Load Balancing**
   * The client resolves the API domain (e.g., `api.xyz.com`) using a DNS-based load balancer (such as **AWS Route 53** or **Azure Traffic Manager**).
   * This DNS load balancer dynamically directs the client to the closest or lowest-latency **Region** (e.g., Mumbai vs. Chennai). 
   * It also handles compliance routing (e.g., user data from a certain country must go to a specific region).
   * *Note:* DNS is **not** a single point of failure because it is highly hierarchical and cached locally across the internet.
2. **Tier 2: Region and Availability Zone (AZ) Failover**
   * Within each Region, there are multiple Availability Zones (AZs) representing isolated physical data centers.
   * Multiple instances of the **API Gateway** run in these zones and scale horizontally based on traffic.
   * If an entire Availability Zone goes down, the API Gateway instances in other AZs continue handling the traffic.
3. **Tier 3: Service Discovery and Microservice Load Balancers**
   * The active API Gateway instance receives the request, resolves the downstream path via Service Discovery, and forwards it to the dedicated **Load Balancer** for that specific microservice.
   * This local load balancer (e.g., Invoice Service Load Balancer) distributes the traffic evenly across the active instances of that microservice within the Availability Zone.
