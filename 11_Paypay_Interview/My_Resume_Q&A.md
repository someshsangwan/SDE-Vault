# PayPay Securities Prep: Technical & Experience Master Sheet

This master document consolidates your project architectures, file mappings, and the 33-question preparation bank into a single, comprehensive reference sheet for your upcoming interview.

---

## Part 1: Project Architecture & Codebase File Mapping

### A. Buddy App (Spring Boot 3.5.6 + PostgreSQL + Redis + WebSockets)

```
                       [React Native App]
                               |
            +------------------+------------------+
            | (HTTP REST)                         | (STOMP WebSockets)
            v                                     v
   [JwtAuthenticationFilter]             [JwtChannelInterceptor]
            |                                     |
    [UserController]                     [ChatWebSocketController]
            |                                     |
     [UserService]                         [ChatService]
            |                                     |
    [UserRepository]                       [ChatMessageRepository]
            |                                     |
    +-------+-------+-----------------------------+-------+
                    | (JPA / Native PostGIS SQL)
                    v
    [PostgreSQL (buddy_user_locations_tbl)] <-> [GIST Index]
```

#### Key Class File Map:
* **[JwtChannelInterceptor.java](file:///Users/somesh_mac/Desktop/buddy-app-backend/src/main/java/com/buddy/businesslogic/config/JwtChannelInterceptor.java)**: Intercepts WebSocket STOMP packets at the channel layer to authenticate connections using JWT headers.
* **[ChatWebSocketController.java](file:///Users/somesh_mac/Desktop/buddy-app-backend/src/main/java/com/buddy/businesslogic/controller/ChatWebSocketController.java)**: Routes real-time chat, typing indicators, and read receipts via WebSockets.
* **[UserLocationRepository.java](file:///Users/somesh_mac/Desktop/buddy-app-backend/src/main/java/com/buddy/businesslogic/repository/UserLocationRepository.java)**: Performs native spatial SQL queries using PostGIS functions.
* **[UserLocation.java](file:///Users/somesh_mac/Desktop/buddy-app-backend/src/main/java/com/buddy/businesslogic/entity/UserLocation.java)**: Mapped entity storing `latitude` and `longitude` as `Double` values.
* **[User.java](file:///Users/somesh_mac/Desktop/buddy-app-backend/src/main/java/com/buddy/businesslogic/entity/User.java)**: The main user profile mapping.
* **[RefreshTokenService.java](file:///Users/somesh_mac/Desktop/buddy-app-backend/src/main/java/com/buddy/businesslogic/security/service/RefreshTokenService.java)**: Handles database persistence and rotation logic for sessions.

---

### B. btpApp (React Native Expo + Firebase Realtime DB)

```
                    [React Native UI: MainScreen]
                                |
             +------------------+------------------+
             |                                     |
             v                                     v
     [CheckingSafety]                       [DataMaintainer]
             |                                     |
    [CalculateDistance]             (Writes ONLY on coordinate diff)
             |                                     v
     (Throttled HTTP GET)                  (HTTP PUT Request)
             |                                     |
             +------------------+------------------+
                                |
                                v
               [Firebase Realtime Database REST API]
```

#### Key Class File Map:
* **[MainScreen.js](file:///Users/somesh_mac/Desktop/btpApp/component/MainScreen.js)**: Requests foreground and background permissions, tracks active coordinate updates using `Location.watchPositionAsync`.
* **[CalculateDistance.js](file:///Users/somesh_mac/Desktop/btpApp/component/CalculateDistance.js)**: Performs proximity calculations using the Haversine formula.
* **[CheckingSafety.js](file:///Users/somesh_mac/Desktop/btpApp/component/CheckingSafety.js)**: Translates distances into warning states (`Safe`, `Alert`, `Run`).
* **[FetchingData.js](file:///Users/somesh_mac/Desktop/btpApp/component/FetchingData.js)**: Handles throttled HTTP GET requests to Firebase.
* **[dataMaintenance.js](file:///Users/somesh_mac/Desktop/btpApp/component/dataMaintenance.js)**: Handles delta-based coordinate updates to Firebase.

---

## Part 2: The 33-Question Preparation Bank

### Category A: Rakuten Pay Experience

#### 1. Walk me through the ATM cash charge API integration — what was your specific role and what were the biggest technical challenges?
* **My Role**: Core backend engineer in charge of building the secure API integration layer connecting Lawson Bank and Seven Bank host ATM systems with our Spring Boot wallet ledger.
* **Technical Challenges**:
  * **Idempotency**: ATMs retry requests on connection dropouts. We configured unique database constraint keys linked to the banks' transaction IDs to guarantee that retried calls returned the cached transaction status rather than duplicating wallet credits.
  * **mTLS & Payload Signing**: Enforced strict security using Mutual TLS (mTLS) with bank-specific client certificates and SHA-256 with RSA request signatures to guarantee non-repudiation.
  * **Timeout Coordination**: Bank requests timeout in 2-3 seconds. We implemented dynamic transaction polling and reverse fallback commands to sync states reliably.

#### 2. How did you design the Gift Send Recovery batch? What happens if the batch itself fails mid-execution?
* **Design**: Written in PHP/Symfony, orchestrated and triggered on a 1-minute cron loop using Google Cloud Composer (Apache Airflow). It queries all database transactions stuck in a `PENDING` state older than 30 seconds, runs an enquiry API call to the external Online Payment Provider (OPP), and updates the database records based on the result.
* **Failure Mid-Execution**:
  * **Idempotence**: Every step of the recovery command is idempotent. If the batch fails mid-way, the next Airflow task run picks up the remaining `PENDING` transactions.
  * **Transactional Safety**: Local JPA database writes are wrapped in transactional blocks; status is only updated from `PENDING` to `COMPLETED`/`FAILED` once confirmation succeeds.
  * **Alerting**: If a transaction remains un-reconciled after 3 attempts, it is sent to a Dead Letter Queue (DLQ) and triggers a PagerDuty alert.

#### 3. Why PHP/Symfony for the batch instead of Java? Was that your choice or existing codebase?
* **Answer**: 
  > *"It was an architectural constraint of the existing codebase. While our core transaction path is built in Java/Spring Boot, the system uses PHP/Symfony for secondary administrative batches and reporting tasks. Using Symfony allowed us to reuse existing database helper scripts, models, and container configuration templates, reducing delivery time while satisfying our recovery target (under 3 minutes)."*

#### 4. How does Google Cloud Composer (Airflow) orchestrate your batch — what does the DAG look like?
* **DAG Layout**:
  1. `SensorTask`: Periodically queries the database queue or runs on a 1-minute interval.
  2. `FetchPendingTask`: Reads the IDs of pending transactions.
  3. `ReconcileTask (Dynamic Task Mapping)`: Airflow dynamically spawns task instances to process transactions in parallel. Each task runs: `bin/console app:recover-gift <transactionId>`.
  4. `MonitoringTask`: Sends telemetry data to Datadog and triggers a Slack notification if error ratios exceed 5%.

#### 5. What secret detection tools did you use in CI/CD? How do you handle false positives?
* **Secret Detection Tool**: We integrated **Gitleaks** as a static analysis stage in our GitLab CI/CD pipelines. It scans every git diff for high-entropy strings, API keys, and credentials.
* **Handling False Positives**:
  * **Signature Ignore List**: We use a `.gitleaksignore` file containing specific SHA-256 hashes of the false positive strings (keeping raw credentials out of the source repository).
  * **Excluded Folders**: We configure the Gitleaks matcher to bypass mock secrets inside test directories like `src/test/resources/`.

#### 6. How did you reduce rollback time from 45 min to 5 min? Walk me through the pipeline design.
* **The Problem**: Rollbacks were executed by checking out the previous stable git commit and rebuilding the code, which took 45 minutes.
* **New Pipeline Design**:
  * **Semantic Image Tagging**: We tag production Docker images in ECR with their semantic release version (e.g., `prod-v1.4.1`) instead of overriding a single `latest` tag.
  * **GitOps Continuous Deployment**: We utilize ArgoCD/Helm for deployments.
  * **Container Image Swap**: The rollback pipeline simply updates our Helm configuration to point back to the target stable image tag (e.g., `v1.4.1`). Kubernetes pulls the cached image from ECR and performs a rolling update in under 5 minutes without rebuilding any code.

#### 7. What does your on-call incident response process look like? Give me an example of an incident you handled.
* **Incident Response Process**: Production alert thresholds in Datadog/Kibana automatically trigger PagerDuty notifications to the active on-call engineer. If unacknowledged in 15 minutes, it escalates to the secondary.
* **Example Incident**:
  > *"During a holiday sales rush, our Lawson Bank API endpoint suffered a spike in 504 Gateway Timeouts. I was paged. 
  > 1. I inspected Grafana and identified a database connection pool depletion.
  > 2. I analyzed database thread states and found a blocked transaction on our ledger tables caused by an unindexed analytics query.
  > 3. I manually killed the blocked thread, scaled read-replicas, and deployed a hotfix adding the missing index. The system stabilized within 10 minutes."*

#### 8. In Gatling, how did you model 2× peak traffic? What bottlenecks did you discover?
* **Modeling Traffic**: We modeled a closed-loop scenario in Gatling scripting, ramping up active concurrent users over 15 minutes to simulate a massive holiday campaign surge.
* **Bottlenecks Discovered**:
  * **Connection Pool Exhaustion**: The HikariCP database pool was capped too low (10). We increased it to 50 based on database CPU profiles.
  * **JPA N+1 Queries**: One of our transaction lookup APIs made N additional queries to fetch user preferences. We resolved this by implementing `@EntityGraph` to fetch profiles in a single query.

#### 9. What does “removing an intermediary layer” mean architecturally? What risks did that refactor carry?
* **Meaning**: We bypassed a legacy gateway API proxy layer, allowing our Spring Boot microservices to connect directly to the payment provider's API.
* **Risks & Mitigation**:
  * **API Contract Incompatibilities**: The legacy proxy did some data mapping. *Mitigation*: We wrote contract tests using Spring Cloud Contract to ensure the payload formats matched the provider's API.
  * **Network Security**: Opening direct traffic paths. *Mitigation*: We updated our AWS security groups and routing rules to permit outgoing connections only to the payment provider's specific allowlisted domain endpoints.

---

### Category B: Buddy Project Architecture

#### 10. How does your bounding-box geo query work? Why not PostGIS or a dedicated geo index?
* **Original Design (Resume)**: I initially used a bounding-box query using standard SQL `BETWEEN` latitude and longitude ranges.
  * **Why**: It was a fast MVP implementation that didn't require complex GIS database setups.
* **Upgrade to PostGIS (Codebase)**: As the app grew, I migrated it to **PostGIS** in `UserLocationRepository.java` using `ST_DWithin` and `ST_Distance` cast to `::geography` points.
  * **Why**: Bounding-box queries calculate a square, whereas search radius needs to be a circle. PostGIS supports **GIST spatial indexing**, which scales logarithmically, whereas range queries on raw double columns trigger slow table scans. We optimized the raw double database fields (`latitude` and `longitude` in `UserLocation.java`) using a **Functional GIST index**:
  ```sql
  CREATE INDEX idx_user_spatial ON buddy_user_locations_tbl 
  USING GIST (ST_MakePoint(location_lng, location_lat)::geography);
  ```

#### 11. How did you handle WebSocket connection scaling — what happens when your server restarts?
* **WebSocket Scaling**: WebSockets maintain stateful TCP connections. To scale horizontally, we configured **Redis Pub/Sub** via Spring's messaging broker relay. If User A is connected to Server 1 and User B is connected to Server 2, Server 1 publishes the message to Redis, and Server 2 picks it up and routes it to User B.
* **Server Restarts**: Active WebSocket connections drop. The client (React Native) is built with an **automatic reconnection loop** (using exponential backoff). Upon reconnecting, the client triggers an HTTP REST request (`/api/chat/unread`) to fetch any messages missed during the downtime.

#### 12. Walk me through your JWT refresh-token rotation. What happens if a refresh token is stolen?
* **Rotation**: When a client requests a new access token, the server:
  1. Validates the incoming refresh token against `buddy_refresh_tokens_tbl`.
  2. Deletes the old refresh token.
  3. Flushes the transaction (`entityManager.flush()`) to commit the delete instantly, preventing constraint violations on the `buddy_id` unique constraint.
  4. Saves a new UUID-based refresh token and returns it to the client.
* **Token Theft Detection**: If an attacker steals a refresh token and uses it after the legitimate user has already rotated it, the server receives a token that has already been deleted. We flag this as a **security breach**, identify the associated user ID, delete all active refresh tokens for that user session, and force a complete re-login.

#### 13. Why Redis for online presence? How do you handle stale presence data?
* **Why Redis**: User heartbeats happen every 30 seconds. Writing this frequency of updates to PostgreSQL would cause disk bloating and high CPU usage. Redis handles this in-memory with sub-millisecond write latency.
* **Handling Stale Data**: When a user connects to WebSockets, we set a key `user:presence:{userId} = "online"` in Redis with a **TTL of 60 seconds**. The mobile client sends a light ping frame every 30 seconds. If the user closes the app, loses network connection, or crashes, the ping stops, the key expires in Redis, and they automatically appear offline.

#### 14. Why did you proxy LocationIQ server-side instead of using client-side API calls?
* **Answer**: 
  > *"To protect our API keys and prevent abuse. If we made geocoding API calls directly from the React Native mobile app, we would have to ship our LocationIQ API secret key inside the application bundle. Attackers can easily decompile mobile APKs/IPAs and steal secret keys, using our quota for their own projects. By proxying the request through our Spring Boot backend (`/api/location/geocode`), we keep the API key securely stored in our AWS Secrets Manager."*

#### 15. With 15+ tables and 70+ endpoints, how did you manage schema migrations?
* **Answer**: 
  > *"I used **Flyway** schema migration. Every database change (creating tables, adding indexes like our GIST spatial index, or modifying constraints) is written as a versioned SQL script (e.g., `V1__init_schema.sql`, `V2__add_spatial_index.sql`) inside `src/main/resources/db/migration`. 
  > When the Spring Boot application boots, Flyway runs automatically, checks the `schema_version` table, and applies any pending migrations. This ensures our local development, staging, and production databases remain perfectly in sync."*

---

### Category C: DSA & Problem Solving

#### 16. You’ve solved 1000+ problems — what’s the hardest category for you? Why?
* **Answer**: 
  > *"The hardest category for me is **Dynamic Programming on Trees** or complex **Segment Trees**. 
  > While standard DP on arrays is relatively straightforward once you define the state transitions, DP on tree structures requires calculating sub-tree states recursively and aggregating them at the parent nodes, which is highly complex to debug. Similarly, Segment Trees with lazy propagation require high precision in range updates. I master these by breaking them down into base cases and sketching state trees on paper before writing code."*

#### 17. Given your payments background, design a rate limiter for an API.
* **Design (Token Bucket Algorithm using Redis)**:
  * For each IP/User ID, we store a hash key in Redis: `rate:limit:{userId}` containing two fields: `tokens` (available tokens) and `last_updated` (timestamp).
  * **Algorithm**:
    1. When a request arrives, fetch the hash from Redis.
    2. Calculate replenished tokens based on elapsed time since `last_updated` (e.g., refilling 10 tokens per second).
    3. If `tokens >= 1`, decrement tokens by 1, update `last_updated` to now, save to Redis, and allow the request.
    4. If `tokens < 1`, reject the request with HTTP `429 Too Many Requests`.
  * **Concurrency**: We execute this logic using a **Redis Lua Script** to ensure the read-calculate-write steps are completely atomic and avoid race conditions under high concurrent API calls.

#### 18. Classic problems: sliding window, two pointers, BFS/DFS, dynamic programming.
* *(Note: Your coding test involved String & Map problems. Ensure you can explain code details on String matching, HashMap lookups, and time complexities like O(N) or O(N log N) during the call).*

---

### Category D: System Design

#### 19. Design a payment processing system at scale.
* **Core Components**:
  ```
  [Client] -> [API Gateway] -> [Payment Gateway Svc] -> [Idempotency Filter]
                                        |
                            [Kafka Transaction Log]
                                  /         \
                 [Ledger Double-Entry]   [Acquirer Integration (Banks)]
  ```
* **Key Design Points**:
  * **Ledger Database**: Use a distributed SQL database like **TiDB** or Aurora PostgreSQL to handle transactional ACID writes with strong consistency.
  * **Asynchronous Processing**: Route payment messages via Kafka to decouple API ingestion from bank integration, ensuring we can handle checkout surges.
  * **Reconciliation Engine**: An offline batch service that runs daily to reconcile internal ledger balances with the settlement reports provided by credit card companies and banks, generating correction entries for discrepancies.

#### 20. How would you design a real-time chat system like Buddy but for millions of users?
* **Core Components**:
  1. **WebSocket Gateway Layer**: A cluster of lightweight, stateless servers (e.g., Netty/Spring WebFlux) that handle active TCP connections.
  2. **Connection Registry**: A distributed key-value store (Redis) mapping `userId -> gateway_server_ip`.
  3. **Message Router**: A Kafka cluster that routes messages. When User A sends a message to User B, the backend checks Redis to find User B's active server IP and routes the message to that specific server to push to User B.
  4. **Chat History Store**: Use a NoSQL columnar database like Cassandra or ScyllaDB, optimized for high-write, low-latency key-value queries where the partition key is `roomId`.

#### 21. How would you handle idempotency in payment APIs?
* **Answer**: 
  > *"We enforce idempotency by requiring clients to send a unique `Idempotency-Key` (typically a UUIDv4) in the HTTP headers.
  > 
  > 1. When the request hits our gateway, we try to insert the key into Redis with a TTL of 24 hours using `SETNX` (Set if Not Exists) to acquire a lock.
  > 2. If `SETNX` returns 1, we execute the payment transaction in the database. Once completed, we save the response status in Redis and release the lock.
  > 3. If `SETNX` returns 0, it means a duplicate request is in progress or completed. We wait/read the response payload from Redis and return it to the user without calling the payment gateway again."*

#### 22. Design a rollback system for distributed microservices.
* **Design (Saga Pattern with Compensating Transactions)**:
  * In a distributed system (e.g., Order Service, Inventory Service, Payment Service), we cannot use standard database transactions (no two-phase commits due to latency).
  * **Choreography-based Saga**:
    1. `Order Service` creates order (`PENDING`) -> Emits `OrderCreatedEvent`.
    2. `Payment Service` deducts balance -> Emits `PaymentChargedEvent`.
    3. `Inventory Service` attempts to reserve items -> Fails (Out of stock) -> Emits `InventoryReservationFailedEvent`.
    4. **Compensation**: `Payment Service` listens to `ReservationFailed`, refunds the payment, and emits `PaymentRefundedEvent`. `Order Service` listens and sets order to `CANCELLED`.

---

### Category E: Java / Spring Boot

#### 23. How does Spring Boot’s transaction management work? Have you used `@Transactional` across microservices?
* **Spring Boot Transaction Management**: Spring uses AOP (Aspect-Oriented Programming). When `@Transactional` is annotated, Spring wraps the bean in a dynamic proxy that starts a database transaction before entering the method and commits it after exiting (or rolls back if a `RuntimeException` is thrown).
* `@Transactional` **Across Microservices**: 
  * **Answer**: *"No, `@Transactional` is local to a single JVM and database connection pool. It cannot manage transactions across network boundaries. To manage transactions across microservices, we must use patterns like the Saga Pattern (orchestrated or choreographed) or the Transactional Outbox pattern."*

#### 24. What’s the difference between `@RestController` and `@Controller`?
* **`@Controller`**: A standard Spring MVC annotation used to declare web controllers that return dynamic HTML views (e.g., Thymeleaf, JSP).
* **`@RestController`**: A convenience annotation that combines `@Controller` and `@ResponseBody`. It indicates that the methods return domain objects serialized directly into HTTP JSON responses, making it the default choice for REST APIs.

#### 25. How does Spring Security integrate with JWT?
* **Answer**: 
  > *"We create a custom security filter class (`JwtAuthenticationFilter` extending `OncePerRequestFilter`). We configure it in our `SecurityFilterChain` bean to run *before* the standard `UsernamePasswordAuthenticationFilter`.*
  > 
  > *For every request, our filter extracts the JWT from the `Authorization: Bearer` header, validates it using our token provider, loads the user details from the database, builds a `UsernamePasswordAuthenticationToken` principal, and saves it in Spring's `SecurityContextHolder.getContext().setAuthentication(auth)`."*
* **WebSocket Special Integration**: For WebSockets, standard HTTP filters do not protect messages after the handshake. We implement a custom `JwtChannelInterceptor` (which extends `ChannelInterceptor`) to intercept the STOMP `CONNECT` frame and extract the token. This method must **never throw exceptions**, as throwing an error breaks the channel negotiation, causing silent 10-second client timeouts.

#### 26. How do you handle connection pool exhaustion in production?
* **Detection**: Monitor HikariCP metrics in Datadog (Active connections, Pending/Waiting connections, Connection acquisition timeout exceptions).
* **Remediation**:
  1. **Optimize Slow Queries**: Identify and add missing database indexes.
  2. **Shorten Transaction Scopes**: Ensure `@Transactional` is only placed on methods that perform database writes. Avoid placing it on APIs that perform long-running external HTTP calls.
  3. **Increase Pool Size**: If CPU/RAM capacity allows, safely increase `maximum-pool-size` in `application.properties`.

---

### Category F: Behavioral (STAR format)

#### 27. Tell me about a time you owned a project end-to-end.
* **Situation**: At Rakuten Pay, we had high operational overhead resolving manual transaction errors during gift-sending API timeouts.
* **Task**: I volunteered to design and build an automated recovery system to reconcile these failures.
* **Action**: I wrote the recovery logic in PHP/Symfony, set up the Google Cloud Composer (Apache Airflow) workflows, wrote unit and contract tests, and pushed it through production release.
* **Result**: We reduced manual incident tickets for gift timeouts to zero, reducing recovery times from hours to under 3 minutes.

#### 28. Describe a production incident you resolved — what did you do and what did you learn?
* **Situation**: The Lawson Bank ATM endpoint experienced gateway timeouts.
* **Task**: Stabilize the database connection pool.
* **Action**: Analyzed active threads, identified a locked transaction on our wallet balance table due to a slow-running reporting query, killed the lock manually, and scaled read-replicas.
* **Learning**: Never run complex analytics or reporting queries against the primary write database. Keep transaction scopes small.

#### 29. Have you ever disagreed with a technical decision? What happened?
* **Situation**: A senior engineer wanted to use 2PC (Two-Phase Commit) to coordinate transactions between our billing microservice and third-party gateways.
* **Task**: Resolve the trade-off.
* **Action**: I argued that 2PC introduces blocking states and increases latency exponentially if external networks lag. I built a quick prototype demonstrating how a choreographed Saga pattern with Kafka was more resilient.
* **Result**: The team agreed to use the Saga pattern, avoiding potential cascading outages during network hiccups.

#### 30. How do you prioritize when you have both feature work and on-call duties?
* **Answer**: 
  > *"When on-call, my primary duty is system stability. I suspend high-focus feature development and dedicate my time to monitoring dashboard alerts, responding to pages, and resolving bugs. 
  > If the shift is quiet, I work on non-blocking technical debt (e.g., updating dependencies or improving logging metrics). This ensures that on-call engineers are always fully available to respond to production incidents immediately without impacting feature timelines."*

---

### Category G: General / Career Path

#### 31. Why are you looking to leave Rakuten Pay?
* **Answer**: 
  > *"I have had a great experience at Rakuten Pay learning how to build secure, high-concurrency payment APIs and ATM integrations. However, I am looking for a new challenge where I can take more ownership over the system design of a rapidly growing investment application. PayPay Securities' mission to democratize investing and their use of modern technologies like TiDB at high scale aligns perfectly with where I want to grow my skills next."*

#### 32. Where do you see yourself in 3 years?
* **Answer**: 
  > *"In 3 years, I see myself as a senior technical leader at PayPay Securities, owning the architecture of core transactional ledger services. I want to help lead major system migrations, mentor junior backend engineers, and contribute to scaling our infrastructure to support the next generation of retail investors in Japan."*

#### 33. You have an EE background — how did you transition to software engineering?
* **Answer**: 
  > *"During my Electrical Engineering degree at IIT Ropar, I took courses in Data Structures, Algorithms, and computer architecture. I fell in love with software because of the immediate feedback loop: you write code, compile it, and see it impact users instantly. I spent my university years solving over 1,000 algorithmic problems on platforms like LeetCode and building full-stack applications. This passion helped me land my role at Rakuten Pay, where I successfully applied my engineering problem-solving skills to real-world fintech challenges."*
