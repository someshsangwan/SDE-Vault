# Part 3: Service Discovery in Microservices (Eureka Architecture)

In a microservices system, services are constantly starting up, shutting down, scaling, or moving to different servers. This guide explains how they find each other using **Service Discovery** (focusing on **Netflix Eureka** client-server architecture) in an easy, interview-focused format.

---

## 1. Why Do We Need Service Discovery?

In a traditional monolithic application, different modules communicate via simple in-memory function calls.

In a **microservices** system, modules are split into separate services that must communicate over the network (e.g., *Order Service* calls *Payment Service*).
To call *Payment Service*, the *Order Service* needs its **IP address and port** (e.g., `http://192.168.1.15:8080`).

In cloud environments, we **cannot hardcode** these addresses because:
1. **Dynamic Scaling (Autoscaling):** If traffic spikes, we might start 5 new instances of *Payment Service* (each getting a random IP address).
2. **Dynamic Destructions (Ephemerality):** Containers or VMs can crash, restart, or upgrade at any time, changing their IP addresses instantly.
3. **Port Collisions:** When running multiple services on the same host, they use random ports.

We need a way to automatically and dynamically find the locations of our microservices.

---

## 2. What is Service Discovery? (The Phonebook Analogy)

Think of **Service Discovery** as a **Dynamic Phonebook (Service Registry)** for your services.

Instead of remembering IP addresses, services look up each other in this registry. The most popular tool for this in the Java/Spring boot world is **Netflix Eureka**.

It consists of two main parts:
1. **Eureka Server (The Registry/Phonebook):** A central server that stores the names and locations (IP and Port) of all active microservices.
2. **Eureka Client (The Service):** Any microservice (like *Order Service* or *Payment Service*) that registers itself with the Eureka Server and queries it to find other services.

```
       1. Register ("I am Order-Service at 10.0.0.5:8080")
     ┌───────────────────────────────────────────────────┐
     │                                                   │
     │               [ EUREKA SERVER ]                   │
     │              (Central Registry)                   │
     │                                                   │
     └───────────▲───────────────────────────▲───────────┘
                 │                           │
                 │ 3. Fetch Registry         │ 2. Heartbeats ("I am alive!")
                 │    (Cached Locally)       │
     ┌───────────┴──────────┐     ┌──────────┴───────────┐
     │    ORDER SERVICE     │     │   PAYMENT SERVICE    │
     │   (Eureka Client)    │     │   (Eureka Client)    │
     └──────────────────────┘     └──────────────────────┘
```

---

## 3. How API Invocation Works (Step-by-Step)

Here is exactly how a request flows in a Eureka-based architecture:

1. **Server Boot:** The **Eureka Server** starts up first.
2. **Client Registration (Register):** When **Payment Service** starts up, its Eureka Client sends a POST request to the Eureka Server with its metadata (App Name, IP address, Port).
3. **Fetch Registry:** When **Order Service** starts up, it fetches the registration list from the Eureka Server and **caches it in its local memory**.
4. **API Invocation (The Actual Request):**
   * *Order Service* needs to call *Payment Service*.
   * *Order Service* looks at its **local cache** to find the IP addresses of *Payment Service* (e.g., `10.0.0.5` and `10.0.0.6`).
   * It uses a client-side load balancer (like Spring Cloud LoadBalancer) to select one IP (e.g., `10.0.0.5`).
   * It sends the HTTP/gRPC request directly to `10.0.0.5`.
   * **Notice:** The Eureka Server is **NOT** contacted during the actual API call. The connection is direct.

---

## 4. Setting up Eureka (High-Level Overview)

To build this architecture in code (using Spring Boot as an example):

### A. Setup Eureka Server
1. Create a Spring Boot application and add the `spring-cloud-starter-netflix-eureka-server` dependency.
2. Annotate the main class with `@EnableEurekaServer`.
3. In `application.properties`, configure it to run on port `8761` and tell it not to register with itself (since it's the server):
   ```properties
   eureka.client.register-with-eureka=false
   eureka.client.fetch-registry=false
   ```

### B. Setup Eureka Client
1. Create your microservice (e.g., *Order Service*) and add the `spring-cloud-starter-netflix-eureka-client` dependency.
2. Annotate the main class with `@EnableDiscoveryClient` (or it registers automatically by default in newer versions).
3. In `application.properties`, give your service a name and point it to the Eureka Server:
   ```properties
   spring.application.name=order-service
   eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
   ```

---

## 5. Important Interview Questions & Answers

### Q1: How does the Eureka Server know whether a client is up or down?
* **Heartbeats (Renewals):** A registered client must send a "heartbeat" request (ping) to the server every **30 seconds** (default) to renew its lease.
* **Eviction:** If the Eureka Server does not receive a heartbeat for **90 seconds** (default), it assumes the client has crashed and removes it from its registry database.
* **Self-Preservation Mode:** If there is a temporary network breakdown, Eureka might suddenly lose heartbeats from many clients at once. Instead of evicting all of them (which would be disastrous), Eureka enters *Self-Preservation Mode*. It freezes evictions to protect active services from being incorrectly marked as offline due to local network issues.

### Q2: How does the Eureka Server store the registry data?
* Eureka stores all registration data **in-memory** using a nested Java **`ConcurrentHashMap`** (specifically a read-write registry lock mechanism).
* It does *not* persist this data to a physical database (like MySQL or MongoDB) because IP locations in microservices are highly temporary, and in-memory storage is extremely fast.

### Q3: What if the Eureka Server goes down? Is it a Single Point of Failure (SPOF)?
* **No, it is not a Single Point of Failure!**
* **Local Caching:** Every Eureka Client caches the registry list locally in memory. If the Eureka Server goes down, the services can still talk to each other using their local caches.
* **Peer Replication (Clustering):** In production, you run multiple Eureka Servers. They are configured as "peers" and constantly replicate registration data to each other. If one server dies, clients failover to the other peers.

### Q4: Does the client need to call the Eureka Server for every API call?
* **No!** 
* Calling the Eureka Server every time a microservice makes an API call would create a massive performance bottleneck.
* Instead, clients call the server once at startup to fetch the full registry, cache it locally, and then **poll the server periodically** (every 30 seconds by default) to fetch only the changes (deltas). All API routing is done using the local cache.

---

## 6. How Service Discovery and Service Mesh Work Together

A common beginner point of confusion is: *"If I have a Service Mesh, do I still need Service Discovery?"*

Yes, they work hand-in-hand!
* **Service Discovery** is the central **Phonebook** at the main office.
* **Service Mesh** is the network of **Personal Assistants (Sidecar Proxies)**.
  1. The **Control Plane (The Manager)** reads the Service Discovery phonebook.
  2. The Control Plane copies the lists and hands them to the **Sidecar Proxies (The Assistants)**.
  3. When a service wants to send a call, its **Sidecar Proxy** uses its local copy of the registry list to route the request directly to an active instance.
