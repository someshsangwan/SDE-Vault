# Chapter 1 — Spring Core & Inversion of Control (IoC)

> **Status:** ✅ Completed
> **Goal:** Understand what Spring Boot adds over plain Spring, how the IoC container manages objects, different Dependency Injection styles, Bean scopes with thread-safety implications, Bean lifecycle hooks, and the role of each stereotype annotation.

---

## 📝 Notes

### 1. Spring vs. Spring Boot — The Big Picture

**Plain Spring** is a powerful but verbose framework. You must write `applicationContext.xml` files or Java configuration classes manually, define every bean, configure a servlet container (Tomcat), and set up dozens of dependencies that work together.

**Spring Boot** removes all of this friction with four major additions:

| Feature | What it does | Without it |
|:---|:---|:---|
| **Starters** | Pre-packaged dependency groups (e.g., `spring-boot-starter-web`) | You manually match 10+ dependency versions that are compatible |
| **Auto-configuration** | Inspects your classpath and automatically configures beans (e.g., if Jackson is present, configure `ObjectMapper`) | You write dozens of `@Bean` methods and XML config |
| **Embedded Server** | Bundles Tomcat/Jetty/Undertow inside the JAR so your app runs with `java -jar app.jar` | You deploy a WAR file to an externally installed Tomcat server |
| **Actuator** | Exposes production-ready HTTP endpoints (`/health`, `/metrics`, `/info`) | You wire your own monitoring hooks |

> [!NOTE]
> **Interview Framing:** Spring Boot is not a different framework — it is "Spring with batteries included." All Spring annotations (`@Autowired`, `@Transactional`, `@Component`) work exactly the same way. Spring Boot just removes the boilerplate configuration.

---

### 2. The IoC Container — The Heart of Spring

**The Problem Spring Solves:**

In traditional Java code, objects create their own dependencies:
```java
// Tightly coupled — UserService is responsible for creating UserRepository
public class UserService {
    private UserRepository repo = new UserRepository(); // ❌ Hard-coded dependency
}
```
This is bad because:
- You cannot swap `UserRepository` with a mock in unit tests.
- `UserService` is now tightly bound to one specific implementation.
- Object creation logic is scattered across the codebase.

**Inversion of Control (IoC):**

IoC means you *invert* the responsibility of object creation — instead of objects creating their own dependencies, you let the **framework** (the IoC Container) create and wire everything together.

```java
// Loosely coupled — Spring supplies the dependency
@Service
public class UserService {
    private final UserRepository repo; // ✅ Dependency is injected by Spring

    public UserService(UserRepository repo) {
        this.repo = repo;
    }
}
```

**The IoC Container** (also called the `ApplicationContext`) is a factory and registry that:
1. Scans your code for classes annotated with `@Component`, `@Service`, etc.
2. Instantiates these classes as managed objects called **Beans**.
3. Wires the dependencies between them automatically.
4. Manages their full lifecycle (creation → initialization → use → destruction).

> [!NOTE]
> **Analogy — Shaadi (Wedding) vs. Self-Arranged:**
> Traditionally (tight coupling), you yourself go and find your partner, arrange the venue, invite guests, etc. IoC is like a shaadi agency — you register yourself with them (annotate your class), tell them what you need (`@Autowired`), and they handle all the wiring and arrangement. You just show up when you're needed.

---

### 3. Dependency Injection (DI) — Three Styles

DI is the mechanism the IoC Container uses to supply dependencies. Spring supports three styles:

#### ✅ Constructor Injection (Recommended)
```java
@Service
public class OrderService {
    private final PaymentService paymentService;
    private final InventoryService inventoryService;

    // Spring sees this constructor and injects both beans automatically
    public OrderService(PaymentService paymentService, InventoryService inventoryService) {
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }
}
```
**Why it's recommended:**
- Dependencies can be `final` → immutable, thread-safe by default.
- The object **cannot be created** without all its required dependencies (fail-fast at startup, not at runtime).
- No Spring context needed to unit test — you can `new OrderService(mockPayment, mockInventory)` directly.

#### ⚠️ Field Injection (Discouraged)
```java
@Service
public class OrderService {
    @Autowired
    private PaymentService paymentService; // ❌ Cannot be final, hard to test
}
```
**Why it's discouraged:**
- Cannot make the field `final` → no immutability guarantee.
- Unit testing requires a Spring context or a reflection-based workaround (`ReflectionTestUtils`).
- Hides dependencies — you can't tell what an object needs by looking at its constructor.

#### Setter Injection (For Optional Dependencies)
```java
@Service
public class ReportService {
    private EmailService emailService;

    @Autowired(required = false) // emailService is optional
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }
}
```
Use when a dependency is truly optional or when you need to allow re-injection after construction.

---

### 4. What Exactly IS a Bean? — The Full Picture

The word **"bean"** is the most fundamental concept in Spring, yet it is rarely explained clearly. Here is the full picture.

#### Definition
> A **Spring Bean** is any Java object whose **entire lifecycle** (creation, wiring, use, and destruction) is managed by the Spring IoC Container (`ApplicationContext`).

The key word is **managed**. A bean is not just any object — it is an object that Spring:
- **created** (called `new` on it, or called your `@Bean` factory method)
- **configured** (injected all its dependencies)
- **tracked** (stored in an internal registry by name + type)
- **destroys** when the application shuts down (calling `@PreDestroy`)

**Non-bean vs Bean — Side by Side:**
```java
// ❌ NOT a bean — you created it manually with `new`, Spring knows nothing about it
UserService svc = new UserService(new UserRepository());

// ✅ A bean — Spring created it, wired it, and registered it
@Service
public class UserService {
    private final UserRepository repo;
    public UserService(UserRepository repo) { this.repo = repo; }
}
// Spring holds this in its container. Anywhere you write @Autowired UserService,
// Spring gives you THIS exact same instance.
```

---

#### How Does Spring Discover Beans? — Three Ways

**Way 1 — Component Scanning (`@Component` family)**
Spring scans all classes under the base package (`@SpringBootApplication` class's package) and registers any class annotated with `@Component`, `@Service`, `@Repository`, `@Controller`, or `@RestController`.

```java
@SpringBootApplication  // Scans com.myapp and all sub-packages
public class MyApp { public static void main(String[] args) { SpringApplication.run(MyApp.class, args); } }

// Spring finds this automatically because it's in com.myapp.service
@Service
public class PaymentService { ... }  // → Registered as a bean named "paymentService"
```

**Way 2 — `@Bean` Factory Methods (in `@Configuration` classes)**
For third-party classes you don't own (Jackson, RestTemplate, AWS SDK), you write a factory method:
```java
@Configuration
public class AppConfig {
    @Bean  // Spring calls this method once and stores the returned object as a bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

**Way 3 — Auto-configuration (Spring Boot Magic)**
Spring Boot ships with hundreds of `@Configuration` classes in `spring-boot-autoconfigure.jar`. When you add `spring-boot-starter-data-jpa` to your `pom.xml`, Spring Boot's auto-configuration automatically registers an `EntityManagerFactory`, `DataSource`, and `TransactionManager` bean for you — zero boilerplate.

```
auto-configuration reads: "Is HikariCP on the classpath? Yes."
→ Automatically creates: @Bean DataSource (backed by HikariCP connection pool)
→ You never wrote a single line for this
```

---

#### Bean Names — How Spring Identifies Each Bean

Every bean has a **name** (String identifier) in the container. Spring derives the name automatically:

| How bean is defined | Default name | Example |
|:---|:---|:---|
| `@Service` on `UserService` class | `userService` (camelCase of class name) | `"userService"` |
| `@Bean` method named `objectMapper()` | `objectMapper` (method name) | `"objectMapper"` |
| `@Component("mySpecialCache")` | Custom name you specify | `"mySpecialCache"` |
| `@Bean(name = "primaryDb")` | Custom name you specify | `"primaryDb"` |

```java
// You rarely need bean names, but they matter when multiple beans of the same type exist:
@Bean(name = "mysqlDataSource")
public DataSource mysqlDataSource() { ... }

@Bean(name = "redisDataSource")
public DataSource redisDataSource() { ... }

// Inject a specific one by name:
@Autowired
@Qualifier("mysqlDataSource")
private DataSource dataSource;
```

---

#### What Does the Container Actually Store? — The Bean Registry

Internally, the `ApplicationContext` maintains a **Map** of bean definitions:
```
Bean Registry (simplified):
{
  "userService"      → instance: UserService@1a2b3c  (singleton)
  "orderService"     → instance: OrderService@4d5e6f  (singleton)
  "objectMapper"     → instance: ObjectMapper@7g8h9i  (singleton)
  "paymentService"   → instance: PaymentService@0j1k2l (singleton)
}
```
When you write `@Autowired UserService userService`, Spring looks up `UserService.class` in this registry and hands you the stored instance — it does NOT call `new UserService()` again.

> [!NOTE]
> **This is why singleton beans must be stateless.** There is only ONE `UserService` instance in the entire registry. If it has mutable instance variables, all 200 concurrent HTTP request threads are sharing and mutating the same object simultaneously.

---

#### Eager vs. Lazy Bean Initialization

By default, all singleton beans are created **eagerly at application startup** — before the first request ever arrives.

```
Application starts up:
→ Creates UserService bean ✅
→ Creates OrderService bean ✅
→ Creates PaymentService bean ✅
→ Creates ReportService bean ✅  ← Even if nobody ever calls a report!
App is READY to serve requests.
```

**Pros of Eager (default):** Startup failures surface immediately (e.g., database connection fails → app won't start → you fix it before any user is affected).

**Lazy initialization** (`@Lazy`): Bean is only created on first use.
```java
@Service
@Lazy  // Created only when first injected or requested — NOT at startup
public class HeavyReportGeneratorService {
    // Expensive to initialize, rarely used → lazy is justified
}
```
**When to use `@Lazy`:** Beans that are expensive to create AND rarely used (e.g., a PDF report generator only called once a day).

> [!WARNING]
> **Don't use `@Lazy` everywhere** to speed up startup. Lazy beans delay failure detection — a misconfigured lazy bean will blow up on the first request in production, not at startup where it's easy to catch.

---

#### Beans in Real Life — What You Register vs. What You Don't

| Make it a Bean (`@Service`/`@Bean`) | Do NOT make it a Bean |
|:---|:---|
| `UserService`, `OrderService` — stateless service logic | `User`, `Order`, `Product` — JPA entities (data objects) |
| `UserRepository` — data access logic | `CreateUserRequest` — DTOs (request/response objects) |
| `JwtService`, `EmailService` — utility services | `Exception` subclasses — custom exceptions |
| `ObjectMapper`, `RestTemplate` — shared infrastructure | Plain `HashMap`, `ArrayList` — local data structures |
| `DataSource`, `EntityManagerFactory` — DB infrastructure | Any object you `new` inside a method body |

> [!NOTE]
> **Rule of thumb:** If it is a **service, repository, utility, or infrastructure object** that is shared and stateless → make it a bean. If it is a **data carrier** (DTO, entity, plain value object) → do NOT make it a bean. Entities are managed by JPA, not by Spring's IoC container.

---

### 5. Beans & Scopes

A **Bean** is any object managed by the Spring IoC container. The **scope** determines how many instances of a bean are created and how long they live.

| Scope | Instances | Lifetime | Use Case |
|:---|:---|:---|:---|
| `singleton` | **1 per container** | Application lifetime | Services, Repositories (stateless) ✅ **Default** |
| `prototype` | **New instance per injection** | Until garbage collected | Stateful helper objects, command objects |
| `request` | **1 per HTTP request** | Single HTTP request | Web-layer objects holding per-request data |
| `session` | **1 per HTTP session** | Browser session | Shopping cart, user preferences |
| `application` | **1 per `ServletContext`** | Server lifetime | Shared config readable across sessions |

> [!WARNING]
> **Thread-Safety Trap — The #1 Interview Gotcha:**
> Since `singleton` beans are shared across all threads, they MUST be completely **stateless**. If you store mutable request-specific data in an instance variable of a `@Service` class, multiple concurrent requests will overwrite each other's data.
> ```java
> @Service
> public class BadOrderService {
>     private String currentUser; // ❌ DANGER: shared across all threads!
>
>     public void processOrder(String user) {
>         this.currentUser = user; // Thread A sets "Alice"
>         // Thread B simultaneously sets "Bob" — Thread A now processes Bob's order!
>     }
> }
> ```

> [!TIP]
> **Fix:** Never store mutable state in a singleton bean. Pass data as method parameters. Use `ThreadLocal` if you absolutely must have per-thread state, or scope the bean to `request` scope.

---

### 6. Bean Lifecycle — Birth to Death

Understanding the bean lifecycle lets you run custom initialization logic (e.g., populate a cache, open a connection pool) and cleanup logic (e.g., close connections gracefully).

```
Spring Container Starts
        │
        ▼
  1. Bean instantiation (constructor is called)
        │
        ▼
  2. Dependency injection (@Autowired fields/setters filled)
        │
        ▼
  3. @PostConstruct method runs  ← YOUR HOOK (pre-use initialization)
        │
        ▼
  4. Bean is LIVE — handles requests for the app's lifetime
        │
        ▼
  5. Spring Container Shuts Down
        │
        ▼
  6. @PreDestroy method runs  ← YOUR HOOK (cleanup/close resources)
        │
        ▼
  7. Bean is garbage collected
```

```java
@Component
public class CacheService {

    private Map<String, Object> cache;

    @PostConstruct
    public void init() {
        // Runs AFTER constructor and DI are complete
        // Safe to use all injected dependencies here
        this.cache = new HashMap<>();
        System.out.println("Cache initialized — loading warm data from DB...");
        // loadFromDatabase();
    }

    @PreDestroy
    public void cleanup() {
        // Runs BEFORE Spring destroys this bean
        cache.clear();
        System.out.println("Cache flushed — graceful shutdown complete.");
    }
}
```

> [!NOTE]
> **Interview Point:** Why can't initialization logic go in the constructor? Because at the time the constructor runs, `@Autowired` dependencies have NOT been injected yet. `@PostConstruct` is guaranteed to run after all dependencies are available.

---

### 7. Stereotype Annotations — Roles within the Application Layers

Spring provides specialized versions of `@Component` to signal the **architectural role** of a class. They are functionally equivalent (all register a bean), but they carry semantic meaning and enable layer-specific processing.

```
┌────────────────────────────────────────────────────┐
│  Presentation Layer   →   @Controller / @RestController  │
├────────────────────────────────────────────────────┤
│  Business Logic Layer →   @Service                 │
├────────────────────────────────────────────────────┤
│  Data Access Layer    →   @Repository              │
├────────────────────────────────────────────────────┤
│  General Components   →   @Component               │
└────────────────────────────────────────────────────┘
```

| Annotation | Layer | Extra Behaviour |
|:---|:---|:---|
| `@Component` | Generic | Base stereotype. No extra processing. |
| `@Service` | Business Logic | No extra processing (yet). Signals intent clearly to other developers. |
| `@Repository` | Data Access | Enables **automatic exception translation** — converts raw database exceptions (e.g., `SQLIntegrityConstraintViolationException`) into Spring's `DataAccessException` hierarchy. |
| `@Controller` | Web / MVC | Marks class as a Spring MVC controller that returns view names. |
| `@RestController` | Web / REST | `@Controller` + `@ResponseBody`. Serializes return value to JSON/XML directly into HTTP response. |

---

### 8. `@Component` vs. `@Bean` — The Critical Distinction

This is one of the most frequently asked interview questions.

| | `@Component` | `@Bean` |
|:---|:---|:---|
| **Applied to** | Your own class definition | A method inside a `@Configuration` class |
| **How Spring finds it** | Classpath component scanning | Method invocation by the `@Configuration` class |
| **Use when** | You own the class source code | You do NOT own the class (third-party library) |
| **Example** | Your `UserService`, `OrderRepository` | Third-party: `ObjectMapper`, `RestTemplate`, `DataSource` |

```java
// ✅ @Component — You own this class
@Service
public class UserService { ... }

// ✅ @Bean — You do NOT own ObjectMapper (Jackson library)
@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper; // Spring manages THIS instance as a singleton bean
    }
}
```

> [!NOTE]
> **Analogy — Employee vs. Contractor:**
> `@Component` is like a full-time employee (part of your org, you manage them directly). `@Bean` is like a contractor from a vendor (external, you bring them in through a configuration/contract — the `@Configuration` class).

---

### 9. Key Annotations — Quick Reference

| Annotation | Purpose |
|:---|:---|
| `@SpringBootApplication` | Meta-annotation = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`. Entry point of the app. |
| `@Component` | Marks a class as a Spring-managed bean. |
| `@Service` | Semantic alias of `@Component` for the service/business layer. |
| `@Repository` | Semantic alias of `@Component` for the data access layer. Enables exception translation. |
| `@Controller` | Marks an MVC controller that returns view names. |
| `@RestController` | `@Controller` + `@ResponseBody`. Returns data directly as JSON/XML. |
| `@Autowired` | Tells Spring to inject a dependency. Optional on single-constructor classes. |
| `@Qualifier("name")` | Disambiguates which bean to inject when multiple implementations exist. |
| `@Primary` | Marks a bean as the default choice when multiple candidates exist for an injection point. |
| `@Configuration` | Marks a class as a source of `@Bean` method definitions. |
| `@Bean` | Declares a factory method that returns a Spring-managed bean. |
| `@Scope("prototype")` | Overrides the default singleton scope. |
| `@PostConstruct` | Marks a method to run after DI is complete (initialization hook). |
| `@PreDestroy` | Marks a method to run before the bean is destroyed (cleanup hook). |
| `@Lazy` | Delays bean creation until it is first requested. Useful for expensive objects rarely used. |

---

## 💡 Interview Points

* **What is the difference between IoC and DI?**
  IoC is the broader design *principle* — the framework controls object creation, not the objects themselves. DI is the *mechanism* through which IoC is implemented — the container "injects" dependencies from the outside rather than the class constructing them internally.

* **Why is constructor injection recommended over field injection?**
  Constructor injection enforces mandatory dependencies (the object cannot be created without them), allows fields to be `final` (immutability), and enables clean unit testing without a Spring context by using `new MyService(mockDep)`.

* **What happens if two beans of the same type exist and Spring tries to autowire one?**
  Spring throws a `NoUniqueBeanDefinitionException`. You resolve it by annotating one bean with `@Primary` (default choice) or by using `@Qualifier("beanName")` at the injection point to name the exact bean to inject.

* **What does `@SpringBootApplication` do?**
  It is a meta-annotation combining three annotations: `@Configuration` (this class defines beans), `@EnableAutoConfiguration` (auto-configure beans based on classpath), and `@ComponentScan` (scan this package and sub-packages for `@Component` classes).

* **Can you inject a `prototype`-scoped bean into a `singleton`?**
  Not directly — the prototype bean will be injected once at the singleton's construction time, effectively behaving as a singleton. The fix is to inject `ApplicationContext` and call `getBean()` each time, or use `@Lookup` annotation, or `ObjectProvider<T>`.

* **What is the difference between `@Bean` and `@Component`?**
  `@Component` is used on classes you own via component scanning. `@Bean` is used on methods inside `@Configuration` classes, typically to register third-party objects (like `ObjectMapper` or `RestTemplate`) that you cannot annotate directly.

* **What is a Spring Bean? How is it different from a regular Java object?**
  A Spring Bean is a Java object whose full lifecycle (creation, dependency injection, initialization, and destruction) is managed by the Spring IoC Container. A regular Java object created with `new` is NOT a bean — Spring has no knowledge of it and cannot inject it anywhere.

* **How does Spring discover beans?**
  Three ways: (1) **Component Scanning** — Spring scans the base package for classes annotated with `@Component`, `@Service`, `@Repository`, `@Controller`, or `@RestController`. (2) **`@Bean` methods** — methods inside `@Configuration` classes explicitly declare beans. (3) **Auto-configuration** — Spring Boot automatically registers common infrastructure beans (DataSource, ObjectMapper, etc.) based on what's on the classpath.

* **What is the default name of a bean? When does it matter?**
  By default, the bean name is the camelCase of the class name (e.g., `UserService` → `"userService"`) or the method name for `@Bean` methods. It matters when multiple beans of the same type exist — you use `@Qualifier("beanName")` to tell Spring which specific bean to inject.

* **Should JPA entities (`@Entity`) or DTOs be Spring beans?**
  No. Entities are data objects managed by JPA/Hibernate (not Spring's IoC container). DTOs are simple data carriers that are created with `new` inside methods. Only stateless service, repository, and infrastructure objects should be Spring beans.

* **What is the difference between eager and lazy bean initialization?**
  By default, all singleton beans are created eagerly at startup — before any request arrives. This means startup failures surface immediately. `@Lazy` delays creation until first use, which speeds up startup but hides configuration errors until the bean is first requested (potentially in production).

---

## 🧪 Worked Examples

### Wiring a Three-Layer Application
```java
// 1. Data Access Layer — @Repository
@Repository
public class UserRepository {
    // In real app, this extends JpaRepository
    public User findById(Long id) {
        return new User(id, "Somesh"); // Simulated DB call
    }
}

// 2. Business Logic Layer — @Service
@Service
public class UserService {
    private final UserRepository userRepository;

    // Constructor injection — recommended
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getUserName(Long id) {
        return userRepository.findById(id).getName();
    }
}

// 3. Web Layer — @RestController
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public String getUser(@PathVariable Long id) {
        return userService.getUserName(id);
    }
}
```
**Flow:** HTTP Request → `UserController` → `UserService` → `UserRepository` → Database → back up the chain.

---

### Registering a Third-Party Bean
```java
// Registering AWS S3 client — we don't own AmazonS3, so we use @Bean
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public AmazonS3 amazonS3() {
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build();
    }
}

// Now inject it anywhere using normal DI
@Service
public class FileStorageService {
    private final AmazonS3 amazonS3;

    public FileStorageService(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }
}
```

---

## 🔑 Key Commands / Annotations Summary

| Annotation | One-liner |
|:---|:---|
| `@SpringBootApplication` | Entry point: scans, auto-configures, enables `@Bean` methods in this class |
| `@Component` / `@Service` / `@Repository` | Register class as a managed bean |
| `@Autowired` | Inject a dependency (optional when only 1 constructor) |
| `@Configuration` + `@Bean` | Register third-party objects as managed beans |
| `@PostConstruct` / `@PreDestroy` | Lifecycle hooks for init and cleanup |
| `@Scope("prototype")` | Create a new instance every time the bean is requested |
| `@Primary` / `@Qualifier` | Resolve ambiguity when multiple beans of the same type exist |
