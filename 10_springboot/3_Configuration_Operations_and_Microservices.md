# Chapter 3 — Configuration, Operations, Security & Microservices

> **Status:** ✅ Completed
> **Goal:** Master Spring Boot's external configuration system, validation, global error handling, production monitoring with Actuator, testing strategies, JWT-based security, and how Spring Boot fits into a microservices ecosystem with Spring Cloud.

---

## 📝 Notes

### 1. External Configuration & Profiles

Hard-coding configuration values (database URLs, API keys, port numbers) is a cardinal sin in production development. Spring Boot provides a rich, layered system for externalizing configuration.

#### `application.properties` / `application.yml`

```yaml
# application.yml — base configuration
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: admin
    # Never commit real passwords — use environment variables instead!
    password: ${DB_PASSWORD}   # Reads from environment variable DB_PASSWORD
  jpa:
    hibernate:
      ddl-auto: validate

app:
  jwt:
    secret: ${JWT_SECRET}
    expiry-minutes: 60
```

#### Injecting Configuration Values

```java
// Option 1: @Value — Simple, for single values
@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiry-minutes:30}") // ← 30 is the DEFAULT if not set
    private int expiryMinutes;
}

// Option 2: @ConfigurationProperties — Preferred for groups of related values
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private int expiryMinutes;
    // Getters and setters required (or Lombok @Data)
}

@Service
public class JwtService {
    private final JwtProperties jwtProperties;
    public JwtService(JwtProperties jwtProperties) { this.jwtProperties = jwtProperties; }
    // Access: jwtProperties.getSecret(), jwtProperties.getExpiryMinutes()
}
```

> [!TIP]
> **Prefer `@ConfigurationProperties` over `@Value` when** you have many related configuration properties (e.g., `app.mail.*`). `@ConfigurationProperties` binds them all at once to a typed POJO, gives you IDE completion, and validates constraints with `@Validated`.

#### Spring Profiles — Environment-Specific Configuration

Profiles allow you to switch configurations between environments (local, staging, production) without code changes.

```
application.yml          ← Always loaded (base config)
application-local.yml    ← Loaded when profile = local
application-staging.yml  ← Loaded when profile = staging
application-prod.yml     ← Loaded when profile = prod
```

```yaml
# application-local.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb  # In-memory H2 for local dev
  jpa:
    hibernate:
      ddl-auto: create-drop  # Reset DB on each restart locally
logging:
  level:
    root: DEBUG              # Verbose logging locally

# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://prod-db-host:5432/proddb
  jpa:
    hibernate:
      ddl-auto: none         # Flyway handles schema changes
logging:
  level:
    root: WARN               # Only warnings and errors in production
```

**Activate a profile:**
```bash
# Via environment variable (recommended in containers/Kubernetes)
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar

# Via JVM argument
java -jar app.jar --spring.profiles.active=prod

# In @Test classes
@ActiveProfiles("local")
@SpringBootTest
class UserServiceTest { ... }
```

#### Configuration Override Precedence (Highest → Lowest)

```
1. Command-line arguments:  java -jar app.jar --server.port=9090
2. Environment variables:   SERVER_PORT=9090
3. application-{profile}.yml (active profile file)
4. application.yml (base file)
5. @PropertySource annotations
6. Default values in @Value("${prop:DEFAULT}")
```

> [!IMPORTANT]
> Environment variables always override `application.yml`. This is the mechanism Kubernetes and Docker use to inject secrets and environment-specific config into containers — the application's `application.yml` is the template; the runtime environment provides the actual secret values.

---

### 2. Request Validation with `@Valid`

Never trust user input. Spring Boot integrates with **Bean Validation (JSR-380)** to validate incoming `@RequestBody` objects declaratively.

```java
// 1. Annotate the DTO with constraint annotations
public class CreateUserRequest {

    @NotBlank(message = "Name must not be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a valid email address")
    private String email;

    @Min(value = 18, message = "User must be at least 18 years old")
    @Max(value = 120, message = "User age must be realistic")
    private int age;

    @NotNull(message = "Role is required")
    private UserRole role;
}

// 2. Add @Valid to the controller parameter — triggers validation before the method runs
@PostMapping("/users")
public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
}
// If validation fails, Spring automatically throws MethodArgumentNotValidException
// before your controller method even executes.
```

| Annotation | What it validates |
|:---|:---|
| `@NotNull` | Field is not `null` |
| `@NotBlank` | String is not null, not empty, and not all whitespace |
| `@NotEmpty` | Collection/String is not null and not empty |
| `@Size(min, max)` | String length or collection size is within range |
| `@Min(value)` / `@Max(value)` | Numeric value is above/below limit |
| `@Email` | String is a valid email format |
| `@Pattern(regexp)` | String matches a regular expression |
| `@Positive` / `@Negative` | Number is strictly positive/negative |
| `@Future` / `@Past` | Date/time is in the future/past |

---

### 3. Global Exception Handling with `@ControllerAdvice`

Without global error handling, Spring returns a generic, ugly error response. `@RestControllerAdvice` lets you define a **centralized** error handling class that intercepts specific exception types across ALL controllers and transforms them into clean, consistent API error responses.

```java
@RestControllerAdvice // Applies to ALL controllers globally
public class GlobalExceptionHandler {

    // Handle validation failures (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        return Map.of(
            "status", 400,
            "error", "Validation Failed",
            "details", errors,
            "timestamp", Instant.now()
        );
    }

    // Handle resource not found
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(EntityNotFoundException ex) {
        return Map.of(
            "status", 404,
            "error", "Resource Not Found",
            "message", ex.getMessage()
        );
    }

    // Catch-all for unexpected errors
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGenericError(Exception ex) {
        log.error("Unhandled exception", ex);
        return Map.of(
            "status", 500,
            "error", "Internal Server Error",
            "message", "An unexpected error occurred. Please try again later."
        );
    }
}
```

> [!NOTE]
> **`@ControllerAdvice` vs. `@RestControllerAdvice`:**
> `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`. It automatically serializes the return values of `@ExceptionHandler` methods to JSON/XML. Use `@RestControllerAdvice` for REST APIs.

---

### 4. Spring Boot Actuator — Production Observability

Actuator exposes production-ready HTTP endpoints for monitoring and managing your running application — without writing a single line of monitoring code.

```yaml
# application.yml — expose all endpoints (careful in production!)
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers
  endpoint:
    health:
      show-details: when_authorized
```

#### Key Actuator Endpoints

| Endpoint | Method | Purpose |
|:---|:---|:---|
| `/actuator/health` | GET | Liveness/Readiness check — used by Kubernetes probes and load balancers |
| `/actuator/info` | GET | App metadata: version, build time, git commit hash |
| `/actuator/metrics` | GET | Micrometer metrics: JVM heap, GC, HTTP request duration, DB pool stats |
| `/actuator/metrics/{name}` | GET | Specific metric (e.g., `/actuator/metrics/jvm.memory.used`) |
| `/actuator/loggers` | GET/POST | View and **dynamically change** log levels without restart |
| `/actuator/beans` | GET | All beans registered in the application context |
| `/actuator/env` | GET | ⚠️ All environment variables and config properties (may expose secrets!) |
| `/actuator/heapdump` | GET | ⚠️ Downloads full JVM heap dump (for memory leak analysis, but huge file!) |

> [!CAUTION]
> **Never expose `env`, `heapdump`, or `threaddump` publicly.** These endpoints can leak secrets, environment variables, and sensitive application internals. Always secure Actuator endpoints behind authentication or restrict them to an internal network port.

```yaml
# Secure approach: run actuator on a separate internal port
management:
  server:
    port: 8081    # Only accessible inside the cluster, not exposed externally
  endpoints:
    web:
      exposure:
        include: health,info,metrics  # Only safe endpoints
```

---

### 5. Testing Strategy — Slices, Not Just Full Context

Loading the full `@SpringBootTest` context (which starts the entire application) for every test is slow and wasteful. Spring Boot provides **test slices** — lightweight test contexts that load only the beans needed for a specific layer.

#### Three Core Testing Strategies

**Strategy 1: Unit Test (No Spring)**
```java
class UserServiceTest {
    // No Spring annotations — pure Java unit test
    private UserRepository mockRepo = Mockito.mock(UserRepository.class);
    private UserService userService = new UserService(mockRepo); // Constructor injection FTW

    @Test
    void shouldThrowWhenUserNotFound() {
        when(mockRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.findById(99L));
    }
}
```

**Strategy 2: `@WebMvcTest` — Web Layer Slice**
```java
@WebMvcTest(UserController.class)  // Only loads UserController + MVC infra, NO service or DB beans
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc; // Simulates HTTP requests without starting a real server

    @MockBean  // Creates a Mockito mock and registers it as a Spring bean
    private UserService userService;

    @Test
    void shouldReturn200WhenUserFound() throws Exception {
        when(userService.findById(1L)).thenReturn(new UserDto(1L, "Somesh"));

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Somesh"));
    }

    @Test
    void shouldReturn400WhenBodyInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}")) // blank name — validation should fail
                .andExpect(status().isBadRequest());
    }
}
```

**Strategy 3: `@DataJpaTest` — Repository Layer Slice**
```java
@DataJpaTest // Loads only JPA entities, repositories, and an embedded H2 database
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager; // Helper to set up test data

    @Test
    void shouldFindUserByEmail() {
        entityManager.persist(new User("Somesh", "somesh@test.com"));
        entityManager.flush();

        Optional<User> found = userRepository.findByEmail("somesh@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Somesh");
    }
}
```

| Strategy | Annotation | Speed | Loads |
|:---|:---|:---|:---|
| Unit Test | None | ⚡ Fastest | Nothing (plain Java) |
| Web Layer Slice | `@WebMvcTest` | 🔥 Fast | MVC stack only (no DB, no services) |
| Data Layer Slice | `@DataJpaTest` | 🔥 Fast | JPA/Hibernate + embedded H2 only |
| Full Integration | `@SpringBootTest` | 🐢 Slow | Entire application context |

> [!TIP]
> **The Testing Pyramid for Spring Boot:** Write many unit tests, fewer `@WebMvcTest` / `@DataJpaTest` slice tests, and only a small number of full `@SpringBootTest` integration tests. This balances speed and confidence.

---

### 6. Security Basics & JWT Flow

#### Authentication vs. Authorization

| Concept | Question | Example |
|:---|:---|:---|
| **Authentication** | *Who are you?* | Verifying username/password, validating a JWT |
| **Authorization** | *What are you allowed to do?* | Checking if the authenticated user has the `ROLE_ADMIN` role |

#### JWT (JSON Web Token) — Stateless Authentication

JWT enables stateless authentication: the server does not store sessions. Every request carries a self-contained token that proves the user's identity.

```
Structure of a JWT (3 parts, Base64-encoded, separated by dots):
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzb21lc2giLCJyb2xlIjoiVVNFUiIsImV4cCI6MTcyMDAwMDAwMH0.AbCdEfGh...

HEADER.PAYLOAD.SIGNATURE
  │        │        └── HMAC-SHA256(Base64(header) + "." + Base64(payload), SECRET_KEY)
  │        └────────── {"sub":"somesh","role":"USER","exp":1720000000}
  └─────────────────── {"alg":"HS256","typ":"JWT"}
```

**JWT Authentication Flow:**
```
1. Client sends: POST /auth/login  {"username":"somesh","password":"secret"}
        │
        ▼
2. Server verifies credentials against DB.
   If valid → generates JWT token signed with server's SECRET_KEY.
        │
        ▼
3. Server responds: {"token": "eyJhbGci..."}
        │
        ▼
4. Client stores token (localStorage / memory) and sends it with every request:
   Authorization: Bearer eyJhbGci...
        │
        ▼
5. Server receives request → JwtFilter extracts token from header →
   Verifies signature using SECRET_KEY → Extracts user info from payload →
   Sets authentication in SecurityContext
        │
        ▼
6. Controller method executes with the authenticated user context.
   No database session lookup needed — the token IS the authentication. ✅
```

```java
// Simplified JWT Filter
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.validateAndParseClaims(token);
                String username = claims.getSubject();
                // Set auth in Spring Security context so controllers can access it
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, getAuthorities(claims));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
```

> [!WARNING]
> **JWT Security Gotchas:**
> 1. **Never store the JWT secret in `application.yml` committed to Git.** Always use environment variables (`${JWT_SECRET}`).
> 2. **Short expiry + refresh tokens:** JWTs cannot be invalidated server-side (stateless). Use short access token expiry (15 min) and a separate refresh token mechanism.
> 3. **Always validate the signature and expiry.** Never decode a JWT without verifying the signature.

---

### 7. Microservices with Spring Cloud

When you break a monolith into microservices, you need infrastructure to solve inter-service communication, configuration management, and resilience. Spring Cloud provides ready-made solutions.

#### Core Spring Cloud Components

| Component | Problem Solved | Key Annotation/Class |
|:---|:---|:---|
| **Spring Cloud Gateway** | API Gateway — single entry point for all microservices, handles routing, rate limiting, auth | `@EnableGateway`, YAML routes config |
| **Spring Cloud Eureka** | Service Discovery — microservices register their location; others discover them by name | `@EnableEurekaServer`, `@EnableDiscoveryClient` |
| **Spring Cloud Config** | Centralized configuration server — one Git repo/Vault for all microservice configs | `@EnableConfigServer`, `spring.config.import=configserver:` |
| **OpenFeign** | Declarative HTTP client — call other microservices using an annotated interface | `@FeignClient`, `@EnableFeignClients` |
| **Resilience4j** | Resilience patterns — Circuit Breaker, Rate Limiter, Retry, Bulkhead | `@CircuitBreaker`, `@Retry`, `@RateLimiter` |

```
Client → [API Gateway] → routes to appropriate service
                         ↕ looks up location in [Eureka Service Registry]
         [Config Server] ← all services pull config from here on startup
         [Order Service] → calls [Inventory Service] via OpenFeign
                         → wrapped in Circuit Breaker (Resilience4j)
```

---

### 8. Resilience4j Circuit Breaker — Protecting Against Cascading Failure

The Circuit Breaker pattern prevents a failing downstream service from taking down your entire system.

**The Three States:**

```
                 failure rate > threshold
    ┌─────────────────────────────────────────┐
    │                                         ▼
 CLOSED                                     OPEN
(All traffic flows;               (All calls FAIL FAST immediately
 failures counted)                 → fallback executed. No real calls
    ▲                              to the failing service.)
    │                                         │
    │     test calls succeed                  │  wait timeout (e.g., 30s)
    │                                         ▼
    └──────────────────────────────────── HALF-OPEN
                                    (Allow limited test calls;
                                     if they succeed → CLOSED,
                                     if they fail → OPEN again)
```

```java
@Service
public class ProductService {

    private final InventoryClient inventoryClient;

    // If inventoryClient.checkStock fails too often → circuit opens → fallback is called
    @CircuitBreaker(name = "inventory", fallbackMethod = "getStockFallback")
    @Retry(name = "inventory", fallbackMethod = "getStockFallback") // Retry 3x before tripping
    public StockInfo getStock(Long productId) {
        return inventoryClient.checkStock(productId); // Remote HTTP call
    }

    // Fallback — called when circuit is OPEN or all retries exhausted
    public StockInfo getStockFallback(Long productId, Exception e) {
        log.warn("Inventory service unavailable for product {}. Returning default.", productId);
        return new StockInfo(productId, 0, "UNKNOWN"); // Graceful degradation ✅
    }
}
```

```yaml
# application.yml — Resilience4j configuration
resilience4j:
  circuit-breaker:
    instances:
      inventory:
        sliding-window-size: 10         # Count last 10 calls
        failure-rate-threshold: 50      # Open circuit if 50%+ calls fail
        wait-duration-in-open-state: 30s  # Stay OPEN for 30 seconds
        permitted-number-of-calls-in-half-open-state: 3  # 3 test calls in HALF-OPEN
  retry:
    instances:
      inventory:
        max-attempts: 3                 # Retry up to 3 times
        wait-duration: 500ms            # Wait 500ms between retries
```

---

## 💡 Interview Points

* **What is the difference between `@Value` and `@ConfigurationProperties`?**
  `@Value` injects individual property values directly. `@ConfigurationProperties` binds a group of related properties (with a common prefix) to a structured POJO, providing IDE autocompletion, type safety, and `@Validated` constraint support. Prefer `@ConfigurationProperties` for related config groups.

* **How does Spring handle configuration override between `application.yml` and environment variables?**
  Environment variables have higher precedence than `application.yml`. Spring Boot converts `SERVER_PORT=9090` to `server.port=9090` using relaxed binding, so an env var always wins over the properties file. This is how Kubernetes and Docker inject runtime secrets.

* **What is `@WebMvcTest` and when do you use it?**
  A Spring test slice that loads only the web layer (controllers, filters, `MockMvc`) without starting a full application context or connecting to a database. Use it to test controller request mapping, validation, JSON serialization, and `@ControllerAdvice` exception handling in isolation.

* **What is the difference between Authentication and Authorization?**
  Authentication answers "Who are you?" (verifying identity via credentials or a JWT). Authorization answers "What are you allowed to do?" (checking if the authenticated identity has the required permissions/roles to access a resource).

* **Why is JWT considered stateless authentication?**
  The server stores no session data. The JWT token itself encodes user identity and claims (payload) and is cryptographically signed (signature). The server only needs its secret key to verify the token — no database session lookup is required.

* **What is a Circuit Breaker and its three states?**
  A Circuit Breaker wraps remote service calls to prevent cascading failures. **Closed**: traffic flows normally, failures are tracked. **Open**: failures exceeded threshold — all calls fail immediately to the fallback without contacting the failing service. **Half-Open**: after a wait, a few test calls are allowed; if they succeed the circuit closes, if they fail it re-opens.

* **What does `@ControllerAdvice` do?**
  It defines a global exception handling class whose `@ExceptionHandler` methods intercept specific exceptions thrown from any `@Controller` or `@RestController` across the entire application. It centralizes error handling and ensures consistent API error responses.

---

## 🧪 Worked Examples

### Testing a Controller with Validation
```java
// Test: POST /api/v1/users with invalid body returns 400
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;

    @Test
    void createUser_whenNameBlank_returns400() throws Exception {
        String invalidBody = """
            {
                "name": "",
                "email": "somesh@test.com",
                "age": 25
            }
            """;

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details[0]").value("name: Name must not be blank"));
    }
}
```

### Full Profile-Based Configuration Setup
```
src/main/resources/
├── application.yml         ← Base config (shared across all environments)
├── application-local.yml   ← Local dev overrides (H2, DEBUG logging)
├── application-staging.yml ← Staging overrides (PostgreSQL, WARN logging)
└── application-prod.yml    ← Prod overrides (PostgreSQL, Flyway, ERROR logging)
```

```bash
# Run locally (uses H2, DEBUG logs)
SPRING_PROFILES_ACTIVE=local java -jar app.jar

# Run in production (uses PostgreSQL, ERROR logs, Flyway migrations)
SPRING_PROFILES_ACTIVE=prod DB_PASSWORD=secret JWT_SECRET=mysecret java -jar app.jar
```

---

## 🔑 Key Annotations Summary

| Annotation | Purpose |
|:---|:---|
| `@Value("${prop.key}")` | Inject a single configuration property value |
| `@ConfigurationProperties(prefix = "app")` | Bind a group of properties to a typed POJO |
| `@Profile("prod")` | Only register this bean when the `prod` profile is active |
| `@Valid` | Trigger Bean Validation on the annotated method parameter |
| `@NotBlank`, `@Email`, `@Min`, etc. | Bean Validation constraint annotations |
| `@RestControllerAdvice` | Global exception handler for all REST controllers |
| `@ExceptionHandler(MyEx.class)` | Handles specific exception type within a `@ControllerAdvice` |
| `@MockBean` | Creates a Mockito mock and registers it as a Spring bean in test context |
| `@WebMvcTest(Ctrl.class)` | Web layer test slice (no DB, no service beans) |
| `@DataJpaTest` | JPA layer test slice (embedded H2, no web layer) |
| `@SpringBootTest` | Full application context integration test |
| `@CircuitBreaker(name="x")` | Wraps method with Resilience4j circuit breaker |
| `@FeignClient(name="service")` | Declares a declarative HTTP client for inter-service calls |
