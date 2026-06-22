# 🍃 Spring Boot Mastery — Backend Interview Notes

> **Goal:** Master Spring Boot core concepts, API lifecycle flows, database persistence traps, transaction management, global configuration, observability, testing slices, and microservices for backend developer interview rounds.

---

## 📚 Chapters

| # | Chapter | Focus | Status |
|---|---------|-------|--------|
| 1 | [Spring Core & Inversion of Control (IoC)](1_Spring_Core_and_IoC.md) | Spring vs. Spring Boot, IoC & Dependency Injection (DI) styles, Bean Scopes, Bean Lifecycle, Constructor Injection, Stereotypes, and `@Component` vs `@Bean`. | ✅ Completed |
| 2 | [Web APIs, Data Access & Transactions](2_Web_APIs_JPA_and_Transactions.md) | REST APIs, MVC request lifecycle (DispatcherServlet), Spring Data JPA, Hibernate traps (ddl-auto, LazyInitializationException, N+1 queries), and `@Transactional` (rollbacks, self-call proxy trap). | ✅ Completed |
| 3 | [Configuration, Operations & Microservices](3_Configuration_Operations_and_Microservices.md) | Profiles, validation (`@Valid`), global error handling (`@ControllerAdvice`), Actuator, Testing Slices (`@WebMvcTest`, `@DataJpaTest`), Security basics & JWT flow, Spring Cloud, and Resilience4j. | ✅ Completed |

---

## 🗺️ How this works
- **Chapter 1** establishes the core Spring mechanics. You must understand how Spring manages memory, instantiates classes, wires dependencies, and controls lifecycles before discussing web layers or databases.
- **Chapter 2** covers the functional application layer—how a HTTP request travels from the client through the MVC front controller (`DispatcherServlet`), interacts with the database (JPA/Hibernate), and guarantees atomicity using database transactions.
- **Chapter 3** dives into configuration, testing, observability (production metrics), authentication/authorization, and distributed system architectures (microservices and resilience design).

---

## 💡 Top 10 High-Yield Interview Questions

If you only have 30 minutes before your interview, make sure you can answer these ten questions in two sentences each:
1. **Spring vs. Spring Boot:** Spring Boot is Spring plus auto-configuration, starters, an embedded server (Tomcat), and Actuator for production-ready metrics.
2. **IoC vs. DI:** Inversion of Control is the design principle of outsourcing object creation to the framework; Dependency Injection is the mechanism used to supply those dependencies to an object.
3. **`@Bean` vs. `@Component`:** Use `@Component` on classes you own to register them via automatic component scanning. Use `@Bean` on configuration class methods to register third-party classes you do not own.
4. **Constructor vs. Field Injection:** Constructor injection is recommended because it allows dependencies to be `final` (immutable), ensures the object cannot exist half-built (mandatory dependencies), and allows unit testing without starting a Spring context.
5. **Bean Scope:** The default scope is `singleton` (one instance per container). Therefore, all Spring services and repositories must be completely stateless to prevent data leakage between concurrent requests.
6. **`@Transactional`:** Wraps a method in a database transaction. By default, it commits on normal completion and rolls back *only* on unchecked exceptions (`RuntimeException` and `Error`).
7. **`@Controller` vs. `@RestController`:** `@RestController` is a meta-annotation that combines `@Controller` and `@ResponseBody`. It automatically serializes return values directly into the HTTP response body as JSON/XML instead of looking for a HTML view template.
8. **Hibernate LazyInitializationException:** Thrown when your code touches a lazy-loaded collection after the database session/transaction has already closed. Fix it by fetching the collection inside the transaction (e.g., using a fetch join) or mapping to a DTO before returning.
9. **The N+1 Query Problem:** Occurs when you fetch a list of $N$ parent objects (1 query) and then iterate through them, triggering an additional query for each parent's lazy collection ($N$ queries), leading to $N+1$ total queries. Resolve it using a Join Fetch or `@EntityGraph`.
10. **Circuit Breaker:** A design pattern (implemented via Resilience4j) that protects services from cascading failure by wrapping remote network calls. It has three states: **Closed** (traffic flows), **Open** (fails fast immediately without calling the failing service), and **Half-Open** (sends trial calls to verify if the service is healthy again).

---

## ✅ Progress Log
- **2026-06-22** — Completed all 3 chapters of Spring Boot Mastery notes. Covers IoC/DI, JPA traps (N+1, LazyInitializationException, ddl-auto), `@Transactional` proxy trap, external configuration & profiles, validation, global error handling, Actuator, testing slices, JWT auth flow, and Spring Cloud microservices with Resilience4j circuit breaker.
