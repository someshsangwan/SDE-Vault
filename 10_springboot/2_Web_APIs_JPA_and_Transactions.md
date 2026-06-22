# Chapter 2 — Web APIs, Data Access & Transactions

> **Status:** ✅ Completed
> **Goal:** Master how Spring MVC handles HTTP requests end-to-end, how to build REST APIs, how to persist data with Spring Data JPA (including Hibernate's most dangerous traps), and how `@Transactional` guarantees atomicity (with its own hidden traps).

---

## 📝 Notes

### 1. Building REST APIs with Spring MVC

Spring MVC is the web framework built into Spring Boot. At its core, every HTTP request is handled by a **Controller** class.

#### Essential Annotations

| Annotation | Binds to | Example |
|:---|:---|:---|
| `@RestController` | Class — marks as REST controller | `@RestController` |
| `@RequestMapping("/api/v1")` | Class/Method — sets base URL path | `@RequestMapping("/users")` |
| `@GetMapping("/{id}")` | Method — handles `GET` requests | Fetch a resource by ID |
| `@PostMapping` | Method — handles `POST` requests | Create a new resource |
| `@PutMapping("/{id}")` | Method — handles `PUT` requests | Replace a resource |
| `@PatchMapping("/{id}")` | Method — handles `PATCH` requests | Partially update a resource |
| `@DeleteMapping("/{id}")` | Method — handles `DELETE` requests | Delete a resource |
| `@PathVariable` | Parameter — binds URL template variable | `/users/{id}` → `Long id` |
| `@RequestParam` | Parameter — binds query string parameter | `/users?page=2` → `int page` |
| `@RequestBody` | Parameter — deserializes JSON request body into Java object | `POST /users` with JSON body |
| `@ResponseStatus` | Method — sets the HTTP status code | `@ResponseStatus(HttpStatus.CREATED)` |

#### A Complete REST Controller Example

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/v1/users/42
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    // GET /api/v1/users?page=0&size=10
    @GetMapping
    public ResponseEntity<List<UserDto>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.findAll(page, size));
    }

    // POST /api/v1/users  (body: {"name":"Somesh","email":"s@s.com"})
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    // DELETE /api/v1/users/42
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

> [!NOTE]
> **`@Controller` vs. `@RestController`:**
> `@Controller` is for traditional MVC apps that return HTML view names (e.g., `"templates/home.html"`). `@RestController` = `@Controller` + `@ResponseBody`, which tells Spring to serialize the return value directly to JSON/XML in the HTTP response body. For REST APIs, always use `@RestController`.

---

### 2. The Spring MVC Request Lifecycle — What Really Happens

This is a deep-dive interviewers love to ask about. Every HTTP request passes through this pipeline:

```
Browser / API Client
        │
        │  HTTP Request (GET /api/v1/users/42)
        ▼
┌────────────────────┐
│  DispatcherServlet │  ← The FRONT CONTROLLER. Single entry point for all requests.
│  (Front Controller)│    Spring registers this servlet automatically.
└────────────────────┘
        │
        │  "Which controller method handles /api/v1/users/42 GET?"
        ▼
┌────────────────────┐
│   HandlerMapping   │  ← Scans all @RequestMapping annotations.
│                    │    Returns: "UserController.getUser(Long id)"
└────────────────────┘
        │
        ▼
┌────────────────────────────┐
│  HandlerAdapter +          │  ← Invokes the controller method.
│  ArgumentResolvers         │    Extracts @PathVariable, @RequestBody,
│                            │    @RequestParam from the raw HTTP request.
└────────────────────────────┘
        │
        ▼
┌────────────────────┐
│  Your Controller   │  ← getUser(42L) executes, calls service → repo → DB
│  Method            │    Returns: UserDto object
└────────────────────┘
        │
        ▼
┌────────────────────────────┐
│  HttpMessageConverter      │  ← For @RestController: Serializes UserDto
│  (e.g., Jackson)           │    → JSON string using Jackson ObjectMapper
│                            │    For @Controller: ViewResolver finds template
└────────────────────────────┘
        │
        │  HTTP Response (200 OK, body: {"id":42,"name":"Somesh"})
        ▼
Browser / API Client
```

> [!NOTE]
> **Interview Key Point:** The `DispatcherServlet` is the single front-controller for all requests. It delegates to specialized components (`HandlerMapping`, `HandlerAdapter`, `HttpMessageConverter`) — it never handles business logic itself.

---

### 3. Spring Data JPA — Data Persistence Made Simple

**JPA (Java Persistence API)** is a specification for mapping Java objects to database tables. **Hibernate** is the most popular implementation of JPA. **Spring Data JPA** wraps Hibernate and adds a repository abstraction so you can query the database with almost zero SQL boilerplate.

#### Step 1 — Map an Entity

```java
@Entity                          // Marks this class as a JPA-managed database table
@Table(name = "users")           // Optional: specify exact table name
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO-INCREMENT primary key
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    // Getters, setters, constructors (or use Lombok @Data)
}
```

#### Step 2 — Create a Repository Interface

```java
// Spring Data generates the full implementation at runtime — no SQL needed!
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring generates: SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Spring generates: SELECT * FROM users WHERE name LIKE ? AND active = ?
    List<User> findByNameContainingAndActiveTrue(String nameFragment);

    // Custom JPQL query when derived query names get too complex
    @Query("SELECT u FROM User u WHERE u.createdAt > :date AND u.role = :role")
    List<User> findRecentByRole(@Param("date") LocalDate date, @Param("role") String role);
}
```

`JpaRepository<User, Long>` gives you for free:
- `findById(Long id)`, `findAll()`, `save(User user)`, `delete(User user)`, `count()`, `existsById(Long id)`, and pagination with `findAll(Pageable pageable)`.

---

### 4. The Three Critical JPA/Hibernate Traps 🪤

These are the most dangerous mistakes developers make with Hibernate in production. Interviewers love asking about all three.

---

#### 🪤 Trap 1: `spring.jpa.hibernate.ddl-auto=update` in Production

This setting controls whether Hibernate automatically modifies your database schema on application startup.

| Value | Behaviour | Environment |
|:---|:---|:---|
| `create` | Drop all tables and recreate from entities | Local dev only (destroys all data!) |
| `create-drop` | Create on startup, drop on shutdown | Tests only |
| `update` | Attempt to ALTER tables to match entities | **NEVER in production** ❌ |
| `validate` | Verify tables match entities, fail fast if not | Staging (safe) |
| `none` | Do nothing | **Production** ✅ |

> [!CAUTION]
> **`ddl-auto=update` in production is a career-limiting move.** Hibernate's `ALTER TABLE` logic is naive — it can add columns but **cannot** safely rename columns, change types, or handle complex migrations. It will silently produce corrupt schemas or fail mid-migration. Use a versioned migration tool instead.

**The correct approach for production:**

```yaml
# application.properties (Production)
spring.jpa.hibernate.ddl-auto=none
spring.flyway.enabled=true
```

Use **Flyway** or **Liquibase** to manage schema changes as versioned migration scripts:
```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL
);

-- V2__add_created_at_column.sql
ALTER TABLE users ADD COLUMN created_at TIMESTAMP DEFAULT NOW();
```

Flyway runs these scripts in order, tracks which have been applied in a `flyway_schema_history` table, and never re-runs a completed migration.

---

#### 🪤 Trap 2: `LazyInitializationException` — The Session-Closed Trap

By default, JPA loads associated collections (e.g., `@OneToMany`) **lazily** — meaning the data is NOT fetched from the database until you access the collection. The problem is that this lazy loading requires an **active Hibernate session**, which is typically open only during a transaction.

```java
@Entity
public class Order {
    @Id
    private Long id;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY) // Default = LAZY
    private List<OrderItem> items;
}

// In your controller:
@GetMapping("/orders/{id}")
public OrderDto getOrder(@PathVariable Long id) {
    Order order = orderRepository.findById(id).orElseThrow();
    // ⚠️ The @Transactional in orderRepository.findById() has already committed here!
    // The Hibernate session is CLOSED.
    order.getItems().size(); // ❌ LazyInitializationException — session is closed!
    return mapper.toDto(order);
}
```

**Three ways to fix it:**

**Fix 1 — Fetch Join (JPQL):** Eagerly fetch inside the transaction with a single optimized query.
```java
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findByIdWithItems(@Param("id") Long id);
```

**Fix 2 — `@EntityGraph`:** Declaratively specify which associations to load eagerly per query.
```java
@EntityGraph(attributePaths = {"items"})
Optional<Order> findById(Long id);
```

**Fix 3 — Map to DTO inside the service (best practice):** Never expose your entity outside the service layer. Map to a DTO while the session is still open.
```java
@Service
public class OrderService {
    @Transactional
    public OrderDto getOrder(Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        // Session is open here — safe to access lazy collections
        return new OrderDto(order.getId(), order.getItems().size()); // ✅
    }
}
// Controller receives OrderDto — no Hibernate entity, no session dependency
```

---

#### 🪤 Trap 3: The N+1 Query Problem

The N+1 problem is a performance bug where fetching $N$ parent records triggers an additional $N$ SQL queries to load their associated children — $N+1$ total queries instead of 1.

```java
// Scenario: Load all orders and print the item count for each

List<Order> orders = orderRepository.findAll(); // Query 1: SELECT * FROM orders (returns N=50 orders)

for (Order order : orders) {
    // For EACH of the 50 orders, Hibernate fires a separate query:
    // Query 2: SELECT * FROM order_items WHERE order_id = 1
    // Query 3: SELECT * FROM order_items WHERE order_id = 2
    // ...
    // Query 51: SELECT * FROM order_items WHERE order_id = 50
    System.out.println(order.getItems().size()); // ❌ 50 extra queries!
}
// Total: 51 queries instead of 1 or 2!
```

**Fix — Fetch Join:** Load everything in a single query with a JOIN.
```java
// In repository:
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.items")
List<Order> findAllWithItems();

// Now the loop fires 0 extra queries — all data was loaded at once ✅
// Total: 1 query (SELECT orders JOIN order_items)
```

> [!TIP]
> **How to detect N+1 in development:** Add `spring.jpa.show-sql=true` and `spring.jpa.properties.hibernate.format_sql=true` to your `application.properties`. Inspect the console output — if you see the same query repeated in a loop with different IDs, you have an N+1 problem.

---

### 5. `@Transactional` — ACID Guarantees and Hidden Traps

`@Transactional` wraps a method in a database transaction. If the method completes successfully, the transaction **commits** (changes saved). If an exception escapes the method, the transaction **rolls back** (changes undone — as if the method never ran).

#### Basic Usage

```java
@Service
public class TransferService {

    @Transactional // Wraps the ENTIRE method in one atomic transaction
    public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();

        from.debit(amount);   // Step 1
        to.credit(amount);    // Step 2
        // If Step 2 throws — Step 1 is ROLLED BACK automatically ✅
    }
}
```

> [!NOTE]
> **Analogy — Bank Transfer:**
> Deducting ₹1000 from Account A and adding ₹1000 to Account B must be **atomic** — both succeed or both fail. `@Transactional` is the database's guarantee of this atomicity.

#### Propagation Levels (Most Common)

| Propagation | Behaviour |
|:---|:---|
| `REQUIRED` (default) | Join existing transaction if one exists; create a new one if not |
| `REQUIRES_NEW` | Always create a new transaction, suspending any existing one |
| `SUPPORTS` | Join if one exists; run non-transactionally if not |
| `NOT_SUPPORTED` | Always run non-transactionally, suspending any existing transaction |
| `MANDATORY` | Must have an existing transaction; throw exception if none exists |
| `NEVER` | Must NOT have an existing transaction; throw exception if one exists |

---

### 6. The Two Critical `@Transactional` Traps 🪤

---

#### 🪤 Trap 1: Rollback Only Happens for Unchecked Exceptions

By default, `@Transactional` **commits** the transaction even if a **checked exception** (`IOException`, `SQLException`) escapes the method. It only rolls back for **unchecked exceptions** (`RuntimeException` and `Error`).

```java
@Transactional
public void processFile(Long userId) throws IOException { // checked exception!
    userRepository.markAsProcessing(userId); // DB write #1
    externalService.readFile();              // ❌ throws IOException
    userRepository.markAsComplete(userId);   // DB write #2 (never reached)
    // Result: DB write #1 IS committed despite the exception! ❌
}

// FIX: Explicitly declare rollback for checked exceptions
@Transactional(rollbackFor = IOException.class)
public void processFile(Long userId) throws IOException {
    // Now rolls back on IOException ✅
}

// Or: Catch and re-throw as RuntimeException
@Transactional
public void processFile(Long userId) {
    try {
        userRepository.markAsProcessing(userId);
        externalService.readFile();
    } catch (IOException e) {
        throw new RuntimeException("File processing failed", e); // Triggers rollback ✅
    }
}
```

---

#### 🪤 Trap 2: The Self-Call Proxy Trap (Most Misunderstood)

`@Transactional` works via **Spring AOP (Aspect Oriented Programming)**. Spring wraps your bean in a **proxy object**. When external code calls a `@Transactional` method, it goes through the proxy, which starts/commits/rolls back the transaction.

The trap: if a method calls **another method on the same class**, it bypasses the proxy entirely and calls the method directly. Transaction annotations on the inner method are **completely ignored**.

```java
@Service
public class OrderService {

    public void createOrderBatch(List<CreateOrderRequest> requests) {
        for (CreateOrderRequest req : requests) {
            createOrder(req); // ❌ Direct (this.createOrder) — bypasses proxy!
            // @Transactional on createOrder is IGNORED here!
        }
    }

    @Transactional // This is IGNORED when called from createOrderBatch above!
    public void createOrder(CreateOrderRequest req) {
        orderRepository.save(new Order(req));
        inventoryService.reserve(req.getProductId());
        // If inventoryService.reserve() throws, the save() is NOT rolled back! ❌
    }
}
```

**Fixes:**

**Fix 1 — Move to a separate class (cleanest):** Extract the transactional method to a separate Spring bean. External calls go through the proxy.
```java
@Service
public class OrderCreationService {
    @Transactional
    public void createOrder(CreateOrderRequest req) { ... } // ✅ Called via proxy
}

@Service
public class OrderBatchService {
    private final OrderCreationService orderCreationService;

    public void createOrderBatch(List<CreateOrderRequest> requests) {
        for (CreateOrderRequest req : requests) {
            orderCreationService.createOrder(req); // ✅ Goes through proxy
        }
    }
}
```

**Fix 2 — Self-inject the proxy (acceptable but awkward):**
```java
@Service
public class OrderService {
    @Autowired
    private OrderService self; // Inject the proxy of THIS bean

    public void createOrderBatch(List<CreateOrderRequest> requests) {
        for (var req : requests) {
            self.createOrder(req); // ✅ Goes through the proxy
        }
    }

    @Transactional
    public void createOrder(CreateOrderRequest req) { ... }
}
```

---

## 💡 Interview Points

* **What is the role of the `DispatcherServlet`?**
  It is Spring MVC's front controller — the single entry point for all HTTP requests. It delegates to `HandlerMapping` to find the right controller method, `HandlerAdapter` to invoke it with resolved arguments, and `HttpMessageConverter` to serialize the response.

* **What is the difference between `@PathVariable` and `@RequestParam`?**
  `@PathVariable` binds a URI template segment (e.g., `/users/{id}` → `Long id`). `@RequestParam` binds a query string parameter (e.g., `/users?page=2` → `int page`).

* **What is `LazyInitializationException` and how do you fix it?**
  Thrown when a lazy-loaded JPA association (e.g., `@OneToMany`) is accessed after the Hibernate session has closed. Fix by using a `JOIN FETCH` query or `@EntityGraph` to eagerly load the association within the transaction, or by mapping the entity to a DTO before the session closes.

* **What is the N+1 query problem?**
  Fetching $N$ parent entities and then accessing their lazy associations inside a loop fires 1 (for parents) + N (one per parent's association) = N+1 total SQL queries. Fix with a JPQL `JOIN FETCH` or `@EntityGraph` to load everything in a single query.

* **Does `@Transactional` roll back on a checked exception?**
  No. By default, it only rolls back on unchecked exceptions (`RuntimeException` and `Error`). For checked exceptions, you must explicitly declare `@Transactional(rollbackFor = MyCheckedException.class)`.

* **What is the `@Transactional` self-call trap?**
  `@Transactional` uses a Spring AOP proxy. If method A calls method B on the **same class** (a self-call via `this`), it bypasses the proxy. The transaction on method B is completely ignored. Fix by extracting method B into a separate Spring bean.

* **Why should `ddl-auto=update` never be used in production?**
  Hibernate's `update` mode cannot handle complex migrations (column renames, type changes, data migrations). It can corrupt schemas silently. Use a versioned schema migration tool like Flyway or Liquibase instead.

---

## 🧪 Worked Examples

### End-to-End: Create Order API
```java
// REQUEST: POST /api/v1/orders
// BODY: {"userId": 1, "productId": 42, "quantity": 3}

// 1. Controller receives request
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}

// 2. Service handles business logic + transaction
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional // If inventoryService.reserve() fails, order save is rolled back
    public OrderDto createOrder(CreateOrderRequest request) {
        Order order = new Order(request.getUserId(), request.getProductId(), request.getQuantity());
        Order saved = orderRepository.save(order);
        inventoryService.reserve(request.getProductId(), request.getQuantity()); // may throw
        return new OrderDto(saved.getId(), "CONFIRMED");
    }
}

// 3. Repository — zero SQL boilerplate
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
}
```

---

## 🔑 Key Annotations Summary

| Annotation | Purpose |
|:---|:---|
| `@RestController` | Marks REST controller; serializes return value to JSON |
| `@RequestMapping` | Maps class/method to URL path |
| `@GetMapping` / `@PostMapping` etc. | HTTP method-specific shorthand mappings |
| `@PathVariable` | Binds URI template variable |
| `@RequestParam` | Binds URL query parameter |
| `@RequestBody` | Deserializes JSON body to Java object |
| `@ResponseStatus` | Sets HTTP response status code |
| `@Entity` | Maps class to a database table |
| `@Id` + `@GeneratedValue` | Marks and auto-generates the primary key |
| `@OneToMany` / `@ManyToOne` | JPA relationship mappings |
| `@Transactional` | Wraps method in a database transaction |
| `@Query` | Custom JPQL or native SQL query on repository methods |
| `@EntityGraph` | Specifies which associations to eagerly load |
