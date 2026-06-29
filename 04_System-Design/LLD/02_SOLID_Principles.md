# Chapter 2 — SOLID Principles in Java

---

## Introduction to SOLID

The **SOLID** principles are a set of five design principles in object-oriented programming first introduced by Robert C. Martin (Uncle Bob). 
Their primary goal is to make software designs more **understandable, flexible, readable, and maintainable**.

Before diving in, understand the two pillars of clean design:
- **Cohesion:** How closely related and focused the responsibilities of a single module/class are. We aim for **High Cohesion**.
- **Coupling:** The degree of direct dependency between modules/classes. We aim for **Loose Coupling** (changing one module shouldn't break another).

---

## 1. Single Responsibility Principle (SRP)

> **"A class should have one, and only one, reason to change."**  
> In other words, a class should have only one job or focus, serving a single actor or stakeholder.

### ❌ The Bad Code (Violates SRP)
Here, the `Invoice` class does three things: calculates the price, prints the invoice (presentation logic), and saves it to the database (persistence logic). If database schema changes or print layout changes, this class must be modified.
```java
class Invoice {
    private int amount;

    public Invoice(int amount) {
        this.amount = amount;
    }

    public int calculateTotal() {
        return amount + (amount * 18 / 100); // 18% GST
    }

    public void printInvoice() {
        System.out.println("Invoice Total: " + calculateTotal());
    }

    public void saveToDatabase() {
        System.out.println("Saving invoice to DB...");
    }
}
```

### ✅ The Good Code (Follows SRP)
We split the responsibilities into three distinct, highly cohesive classes:
```java
class Invoice {
    private int amount;

    public Invoice(int amount) {
        this.amount = amount;
    }

    public int calculateTotal() {
        return amount + (amount * 18 / 100);
    }
}

class InvoicePrinter {
    public void print(Invoice invoice) {
        System.out.println("Invoice Total: " + invoice.calculateTotal());
    }
}

class InvoiceRepository {
    public void save(Invoice invoice) {
        System.out.println("Saving invoice to database...");
    }
}
```

### ⭐ INTERVIEW EXTRA — SRP Gotcha
> SRP does **not** mean a class should only have one method. It means the class should serve exactly **one actor**. For example, a `UserSession` class might have `login()`, `logout()`, and `refreshSession()` methods. This is fine because all these methods serve the same actor and business responsibility (session management).

---

## 2. Open/Closed Principle (OCP)

> **"Software entities (classes, modules, functions) should be open for extension, but closed for modification."**  
> You should be able to add new functionality without changing existing, working code.

### ❌ The Bad Code (Violates OCP)
If we want to add a new payment method (like UPI or Crypto), we have to modify the `processPayment` method inside the existing `PaymentProcessor` class, introducing risks of breaking existing methods.
```java
class PaymentProcessor {
    public void processPayment(String type, double amount) {
        if (type.equals("CreditCard")) {
            System.out.println("Processing credit card payment of $" + amount);
        } else if (type.equals("PayPal")) {
            System.out.println("Processing PayPal payment of $" + amount);
        }
    }
}
```

### ✅ The Good Code (Follows OCP)
By introducing an interface, we can add new payment methods simply by creating new classes that implement `PaymentMethod`. We never need to touch the existing payment controller code.
```java
interface PaymentMethod {
    void process(double amount);
}

class CreditCardPayment implements PaymentMethod {
    public void process(double amount) {
        System.out.println("Processing credit card payment of $" + amount);
    }
}

class PayPalPayment implements PaymentMethod {
    public void process(double amount) {
        System.out.println("Processing PayPal payment of $" + amount);
    }
}

// Open for Extension: We can add UPI payment without modifying existing code
class UPIPayment implements PaymentMethod {
    public void process(double amount) {
        System.out.println("Processing UPI payment of $" + amount);
    }
}

class PaymentService {
    public void process(PaymentMethod method, double amount) {
        method.process(amount); // Polymorphism in action
    }
}
```

---

## 3. Liskov Substitution Principle (LSP)

> **"Objects of a superclass should be replaceable with objects of its subclasses without affecting the correctness of the program."**  
> If Class `B` is a subclass of Class `A`, we should be able to pass `B` to any method expecting `A` without breaking that method's behavior.

### ❌ The Bad Code (Violates LSP)
A common violation occurs when a subclass implements an inherited method by throwing an exception because it cannot support that behavior.
```java
class Bird {
    public void fly() { System.out.println("Flying in the sky."); }
}

class Sparrow extends Bird {}

class Ostrich extends Bird {
    @Override
    public void fly() {
        throw new UnsupportedOperationException("Ostriches cannot fly!");
    }
}
```
If we write a method `runFlySimulation(Bird bird)` that calls `bird.fly()`, passing an `Ostrich` will crash the application, violating LSP.

### ✅ The Good Code (Follows LSP)
We refactor the hierarchy. Not all birds can fly, so we separate the `fly()` capability into its own interface or intermediate class.
```java
class Bird {
    public void eat() { System.out.println("Eating food."); }
}

interface Flyable {
    void fly();
}

class Sparrow extends Bird implements Flyable {
    public void fly() { System.out.println("Sparrow flying."); }
}

class Ostrich extends Bird {
    // Ostriches only inherit eat(), no flying logic here
}
```

### ⭐ INTERVIEW EXTRA — The Square-Rectangle Problem
This is the most common LLD interview question for LSP.
* **Problem:** A `Square` extends `Rectangle`. If `Rectangle` has `setWidth(w)` and `setHeight(h)`, a `Square` must set *both* dimensions to keep them equal. 
* **Violation:** If a client function takes a `Rectangle` reference, sets the width to 10 and height to 20, and asserts that the area is 200, it will fail if passed a `Square` (where setting height to 20 makes both width and height 20, area = 400).
* **Fix:** Avoid inheritance here. `Rectangle` and `Square` are distinct geometric classes. Use a common interface `Shape` with a `getArea()` method.

---

## 4. Interface Segregation Principle (ISP)

> **"Clients should not be forced to depend on interfaces they do not use."**  
> Instead of one large, bloated interface, design multiple, smaller, and specific interfaces.

### ❌ The Bad Code (Violates ISP)
Here, the `Worker` interface is bloated. A `Robot` implements it but is forced to provide empty implementations for `eat()` because robots don't eat.
```java
interface Worker {
    void work();
    void eat();
}

class HumanWorker implements Worker {
    public void work() { System.out.println("Working..."); }
    public void eat()  { System.out.println("Eating lunch..."); }
}

class RobotWorker implements Worker {
    public void work() { System.out.println("Working..."); }
    public void eat()  { /* Empty, violates ISP */ }
}
```

### ✅ The Good Code (Follows ISP)
By splitting the interface into smaller interfaces, each class only implements what it actually needs.
```java
interface Workable {
    void work();
}

interface Eatable {
    void eat();
}

class HumanWorker implements Workable, Eatable {
    public void work() { System.out.println("Working..."); }
    public void eat()  { System.out.println("Eating..."); }
}

class RobotWorker implements Workable {
    public void work() { System.out.println("Working..."); }
}
```

---

## 5. Dependency Inversion Principle (DIP)

> **"High-level modules should not depend on low-level modules. Both should depend on abstractions. Abstractions should not depend on details. Details should depend on abstractions."**  
> Basically: program to interfaces, not concrete implementations.

### ❌ The Bad Code (Violates DIP)
The high-level class `Car` directly depends on the concrete, low-level class `WiredEngine`. If we want to change `Car` to use a `WirelessEngine` or `ElectricEngine`, we have to modify the `Car` constructor.
```java
class WiredEngine {
    public void start() { System.out.println("Wired engine started."); }
}

class Car {
    private WiredEngine engine; // Direct coupling to concrete details

    public Car() {
        this.engine = new WiredEngine();
    }

    public void drive() {
        engine.start();
    }
}
```

### ✅ The Good Code (Follows DIP)
We introduce an abstraction (`Engine` interface) and inject the dependency (Dependency Injection).
```java
interface Engine {
    void start();
}

class WiredEngine implements Engine {
    public void start() { System.out.println("Wired engine started."); }
}

class ElectricEngine implements Engine {
    public void start() { System.out.println("Electric engine started."); }
}

class Car {
    private Engine engine; // Depend on abstraction

    // Constructor injection: Engine is injected from outside
    public Car(Engine engine) {
        this.engine = engine;
    }

    public void drive() {
        engine.start();
    }
}
```

### ⭐ INTERVIEW EXTRA — DIP vs DI vs IoC
| Concept | Definition | Example |
|---------|------------|---------|
| **DIP (Principle)** | A design guideline that high-level modules should depend on interfaces/abstractions, not concrete classes. | "Car depends on `Engine` interface, not `WiredEngine` class." |
| **DI (Pattern)** | A technique for passing dependencies into an object (e.g. constructor/setter) rather than creating them inside. | `new Car(new ElectricEngine())` |
| **IoC (Container)** | Inverting control of object lifecycle and creation, delegating it to a framework/container. | Spring framework managing beans and injecting dependencies automatically. |

---

## ⭐ Quick Revision — Likely Interview Questions

1. **What is the difference between SRP and ISP?**
   - SRP is about a class's responsibility and cohesion (single actor to change).
   - ISP is about interface design (reducing bloated interfaces so clients aren't forced to implement unused methods).
2. **How does OCP differ from LSP?**
   - OCP focuses on adding features without changing existing code.
   - LSP ensures that child classes do not violate/break the contract established by their parent classes.
3. **What is the "Diamond Problem" and how is it related to interface inheritance?**
   - The Diamond Problem occurs when a class inherits from two classes that define the same method with different implementations. Java interfaces resolve this by forcing the subclass to override the conflicting default method.
4. **Why is inheriting `Square` from `Rectangle` an LSP violation?**
   - A `Rectangle` allows changing width and height independently. A `Square` cannot allow independent changes without breaking its invariants (width == height). Thus, a `Square` cannot be safely substituted where a `Rectangle` is expected.
5. **How does Dependency Injection relate to the Dependency Inversion Principle?**
   - DI is the concrete pattern/mechanism used to satisfy the DIP principle (decoupling high-level classes from low-level classes by injecting abstractions).
