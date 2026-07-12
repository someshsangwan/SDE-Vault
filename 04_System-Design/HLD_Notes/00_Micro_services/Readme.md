# Microservices Architecture

A microservices architecture is a design pattern in which a single application is composed of many loosely coupled, independently deployable, and small services. Each service runs in its own process and communicates with others using lightweight protocols (often HTTP/REST, gRPC, or messaging queues).

---

## 📚 Parts List
* **[Part 1: API Gateway and Microservices Architecture](./01_API_Gateway_and_Microservices_Architecture.md)**
* **[Part 2: Service Mesh and its Architecture | How Microservices Communicate?](./02_Service_Mesh_and_its_Architecture.md)**
* **[Part 3: Service Discovery in Microservices](./03_Service_Discovery.md)**
* **[Part 4: Synchronous vs. Asynchronous Communication](./04_Sync_vs_Async_Communication.md)**
* **[Part 5: Monolith vs. Microservices & Decomposition Patterns](./05_Monolith_vs_Microservices_and_Decomposition_Patterns.md)**
* **[Part 6: Strangler Pattern, Saga Pattern & CQRS](./06_Strangler_Saga_and_CQRS.md)**
* *(More parts to be added later)*

---

## 1. Core Principles
* **Single Responsibility:** Each service focuses on a single business capability.
* **Loose Coupling:** Services are independent; changes in one should not require changes in another.
* **Autonomy:** Each service can be developed, deployed, scaled, and maintained independently, often using its own technology stack and database.
* **Data Sovereignty:** Each service owns its database (Database-per-service pattern) to avoid tight coupling at the data tier.

---

## 2. Key Microservices Patterns
### Integration Patterns
* **API Gateway:** A single entry point for all clients. Handles routing, authentication, rate limiting, and protocol translation.
* **Backend for Frontends (BFF):** A variation of the API Gateway tailored to specific client types (e.g., mobile vs. web).

### Data & Transaction Patterns
* **Database per Service:** Every microservice manages its own database.
* **Saga Pattern:** Manages distributed transactions via a sequence of local transactions. Each step updates data within a single service and triggers the next step or a compensating transaction on failure.
* **CQRS (Command Query Responsibility Segregation):** Separates read operations (Queries) from write operations (Commands) to optimize performance and scalability.
* **Event Sourcing:** Persists the state of a business entity as a sequence of state-changing events.

### Cross-Cutting Patterns
* **Service Discovery:** Enables services to find and communicate with each other dynamically (e.g., using Consul, Eureka, or Kubernetes DNS).
* **Circuit Breaker:** Prevents cascading failures by stopping calls to a failing service and returning fallback responses (e.g., using Resilience4j).
* **Sidecar Pattern:** Deploys a helper component alongside the service to handle common infrastructure tasks (e.g., Envoy proxy in a service mesh).

---

## 3. Communication Mechanics
* **Synchronous:**
  * **HTTP/REST:** Standard, human-readable, widely used.
  * **gRPC / Protocol Buffers:** Highly performant, binary serialization, great for internal service-to-service communication.
* **Asynchronous:**
  * **Message Brokers:** Apache Kafka, RabbitMQ, or AWS SQS/SNS for event-driven decoupled communication.

---

## 4. Observability & Monitoring
Since debugging a distributed system is complex, microservices require:
* **Distributed Tracing:** Tracking a request as it flows through multiple services (e.g., Jaeger, Zipkin, OpenTelemetry).
* **Centralized Logging:** Aggregating logs from all containers into a single searchable store (e.g., ELK Stack, Splunk).
* **Metrics & Alerting:** Monitoring system health, throughput, and error rates (e.g., Prometheus and Grafana).
