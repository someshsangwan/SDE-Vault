# Chapter 1 — Object-Oriented Programming (OOP) in Java

> Base note shared with [[01_OOPS_Basics]] (LLD). Sections 9+ are Java-interview additions.

---

## 1. Class

**Class** — A user-defined blueprint or template for creating objects. It defines the **properties (attributes)** and **behaviors (methods)** that objects created from it will have.

## 2. Object

An **object** is an **instance** of a class.

**Real-life example:** Consider `Human` as a class — it has properties like `name`, `age`, `height`, and behaviors like `walk()`, `eat()`, `sleep()`. You and me are objects of this class.

```java
class Human {
    // Properties
    String name;
    int age;
    double height;

    // Behaviors
    void walk()  { System.out.println(name + " is walking."); }
    void eat()   { System.out.println(name + " is eating."); }
    void sleep() { System.out.println(name + " is sleeping."); }
}

public class Main {
    public static void main(String[] args) {
        Human somesh = new Human();
        somesh.name = "John";
        somesh.age = 25;
        somesh.height = 5.9;

        System.out.println(somesh.name + " is " + somesh.age + " years old and " + somesh.height + " feet tall.");
        somesh.walk();
        somesh.eat();
    }
}
```

### ⭐ INTERVIEW EXTRA — Class vs Object in one line
> "A class is the **blueprint**; an object is a **real instance** living in memory."
> When `new Human()` runs: memory is allocated on the **heap**, fields get **default values** (`null`, `0`, `0.0`, `false`), the constructor runs, and a **reference** to that heap object is returned.

---

## 3. Variables & Methods

### Types of Variables
| Type | Where declared | Lifetime / Scope |
|------|----------------|------------------|
| **Instance** | Inside class, outside methods | One copy **per object** (heap) — e.g. `name`, `age` |
| **Local** | Inside a method/block | Only inside that method/block (stack) |
| **Static** | With `static` keyword | **One copy shared** by all objects (method area) — e.g. `species` |

### Types of Methods
- **Instance methods** — belong to an object, can access instance variables. e.g. `walk()`, `eat()`.
- **Static methods** — belong to the class, not any object. e.g. a common message for all humans.
- **Getter / Setter** — controlled access to private variables. e.g. `getName()`, `setName()`.

### Memory of Static Variables
- Stored in the **method area** of JVM memory; **one address** shared by all objects → memory-efficient & consistent.
- Created **once** when the class is loaded.
- Instance variables → **heap**, unique per object. Static variables exist **independently of objects**.

### ⭐ INTERVIEW EXTRA — JVM Memory Model (one diagram to remember)
```
            JVM Memory
 ┌─────────────┬──────────────┬───────────────┐
 │   STACK     │     HEAP     │  METHOD AREA  │
 │ (per thread)│ (objects +   │ (class info,  │
 │ local vars, │ instance vars)│ static vars,  │
 │ refs, calls │              │ method code)  │
 └─────────────┴──────────────┴───────────────┘
```
- **Stack:** local variables and object *references*. One stack per thread.
- **Heap:** the actual objects + their instance variables. Shared, garbage-collected.
- **Method Area:** class metadata, static variables, method bytecode.

> Common Q: *"Where is the object stored vs the reference?"* → object on **heap**, reference variable on **stack**.

---

## 4. Constructor

A **constructor** is a special method used to **initialize objects**. Called automatically when an object is created.

**Key points:**
- **Name:** same as the class name.
- **No return type** (not even `void`).
- **Purpose:** initialize instance variables.
- **Types:** Default (provided by Java if none defined) / Parameterized (accepts arguments).

```java
class Human {
    String name;
    int age;

    Human() {                 // Non-parameterized
        name = "Somesh";
        age = 18;
    }

    Human(String name, int age) {  // Parameterized
        this.name = name;
        this.age = age;
    }

    void display() { System.out.println(name + " is " + age + " years old."); }
}
```

### ⭐ INTERVIEW EXTRA — `this`, `super`, and constructor chaining
- **`this`** → refers to the *current object*. Used to disambiguate (`this.name = name`) and to call another constructor in the same class: `this(...)`.
- **`super`** → refers to the *parent class*. `super()` calls the parent constructor; `super.method()` calls the parent's method.
- **Rule:** the **first line** of every constructor is an implicit `super()` call (unless you write `this(...)` or `super(...)` yourself). This is why parent constructors always run before child constructors.

```java
class Animal {
    Animal() { System.out.println("Animal constructor"); }
}
class Dog extends Animal {
    Dog() {
        // super(); // implicitly here
        System.out.println("Dog constructor");
    }
}
// new Dog()  ->  "Animal constructor" then "Dog constructor"
```

> **Default constructor gotcha:** Java gives you a free no-arg constructor **only if you declare no constructor at all**. The moment you add a parameterized one, the no-arg constructor is gone unless you write it yourself.

---

## 5. Packages & Import

### Package
A collection of related classes, interfaces, and sub-packages. Acts like a **folder** — organizes code, avoids naming conflicts, provides access protection & namespace management.
- Built-in: `java.util`, `java.io`, etc.
- Two classes can share a name if they're in **different packages**.
```java
package package_name;
```

### Import
```java
import package_name.ReferExpireProc;  // a specific class
import package_name.*;                 // all classes in the package
```

---

## 6. Access Modifiers

| Modifier | Within Class | Within Package | Subclass (other pkg) | Non-subclass (other pkg) |
|----------|:---:|:---:|:---:|:---:|
| `public`    | ✅ | ✅ | ✅ | ✅ |
| `protected` | ✅ | ✅ | ✅ | ❌ |
| *default*   | ✅ | ✅ | ❌ | ❌ |
| `private`   | ✅ | ❌ | ❌ | ❌ |

> Mnemonic: **public → protected → default → private** = widest → narrowest visibility.

---

## 7. Non-Access Modifiers

### `abstract`
- Used for abstraction. An **abstract class** can have abstract methods (no body) **and** concrete methods (with body).
- You **cannot instantiate** an abstract class — extend it and override its abstract methods, then instantiate the subclass.
- You **can** create an **anonymous inner class** that extends an abstract class and implements its methods on the spot:

```java
abstract class AbstractClass {
    abstract String myName();
}
public class Main {
    public static void main(String[] args) {
        AbstractClass obj1 = new AbstractClass() {
            @Override String myName() { return "Pallav Raj"; }
        };
        System.out.println(obj1.myName()); // Pallav Raj
    }
}
```

### `final`
- **Final variable** — value can't change once assigned (constant); must be initialized at declaration or in constructor.
- **Final method** — cannot be overridden by subclasses.
- **Final class** — cannot be inherited.

### `static`
- Members belong to the **class**, not an object. Can apply to variables, methods, and **blocks**.
- **Static block** runs **once** when the class is loaded.

```java
class Example {
    static int count = 0;
    static void displayCount() { System.out.println("Count: " + count); }
    static { System.out.println("Static block executed."); count = 10; }
}
```
Output:
```
Static block executed.
Initial Count: 10
Count: 10
Count: 20
```

---

## 8. The Four Pillars of OOP

### Pillar 1 — Abstraction
Hiding implementation details, showing only essential features. Focus on **what** an object does, not **how**.

Achieved via:
- **Abstract classes** → *partial* abstraction (abstract + concrete methods).
- **Interfaces** → *100%* abstraction (only abstract methods before Java 8; default/static methods from Java 8+).

**Real-life:** driving a car — you use start/stop/accelerate without knowing the engine internals.

```java
interface Animal {
    void sound();
    void eat();
}
class Dog implements Animal {
    public void sound() { System.out.println("Dog barks."); }
    public void eat()   { System.out.println("Dog eats bones."); }
}
```

#### Abstract Class vs Interface
| Feature | Abstract Class | Interface |
|---------|----------------|-----------|
| Methods | Abstract + concrete | Only abstract (pre-8); default/static from Java 8 |
| Variables | Instance variables allowed | Only `public static final` (constants) |
| Inheritance | Extend **one** abstract class | Implement **multiple** interfaces |
| Access modifiers | Any | Methods implicitly `public` |

#### ⭐ INTERVIEW EXTRA — Which one do I pick?
- **Interface** = a *contract / capability* → "CAN-DO". e.g. `Comparable`, `Flyable`, `PaymentMethod`. Use when unrelated classes share a behavior, or you need multiple inheritance of type.
- **Abstract class** = a *partial base* with shared state/code → "IS-A". e.g. `AbstractShape` with a shared `color` field + a common `describe()`.
- **Rule of thumb in LLD:** **"Program to an interface, not an implementation."** Default to interfaces for your top-level abstractions; reach for an abstract class only when subclasses share real code/state.

### Pillar 2 — Encapsulation
Wrapping data + methods into one unit (class) and **restricting direct access**.
- **Data hiding:** make variables `private`.
- **Access control:** expose `get`/`set` with validation.
- **Security:** prevents unauthorized modification.
- **Real-life:** a capsule wraps medicine, exposing only what's needed.

```java
class Employee {
    private String name;
    private int age;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) {
        if (age > 0) this.age = age;
        else System.out.println("Age must be positive.");
    }
}
```
> ⭐ The validation inside `setAge` is the *whole point* of encapsulation — the object protects its own **invariants** (rules that must always stay true).

### Pillar 3 — Inheritance
A child class inherits properties/methods of a parent class → code reuse + parent-child hierarchy.
- Child inherits all **non-private** members.
- Child can add its own members or **override** parent methods.

```java
class Animal { void eat() { System.out.println("This animal eats food."); } }
class Dog extends Animal { void bark() { System.out.println("Dog barks."); } }
```

**Q: Is multiple inheritance possible in Java?**
- **Not with classes** → avoids the **Diamond Problem** (ambiguity when two parents have the same method).
- **Yes with interfaces** (since interfaces declare, not implement — and if there's a default-method clash, the child must override).

```java
interface Parent1 { void display(); }
interface Parent2 { void display(); }
class Child implements Parent1, Parent2 {
    public void display() { System.out.println("Child's implementation"); }
}
```

#### ⭐ INTERVIEW EXTRA — Composition over Inheritance (BIG for LLD)
This is one of the most-rewarded ideas in LLD interviews.

- **Inheritance = IS-A:** `Dog IS-A Animal`. Use only for true specialization.
- **Composition = HAS-A:** `Car HAS-A Engine`. The class *holds a reference* to another object and delegates work to it.

```java
// Composition: Car HAS-A Engine
class Engine { void start() { System.out.println("Engine started"); } }
class Car {
    private Engine engine = new Engine();   // HAS-A
    void start() { engine.start(); }         // delegate
}
```

**Why interviewers prefer composition:**
- Inheritance is **rigid** — it locks you into one hierarchy and tightly couples child to parent (a parent change can break children = *fragile base class*).
- Composition is **flexible** — you can swap the composed object at runtime (this is literally how the **Strategy pattern** works).
- Guideline: **"Favor composition over inheritance."** Ask *"is it truly IS-A?"* If you're inheriting just to reuse code, prefer composition.

### Pillar 4 — Polymorphism
"Many forms" — one entity behaves in multiple ways.

**1. Compile-time (Static) — Method Overloading:** same method name, different parameters (count/type/order). Resolved at **compile time**.
```java
int add(int a, int b);
int add(int a, int b, int c);
double add(double a, double b);
```

**2. Run-time (Dynamic) — Method Overriding:** subclass redefines a parent method; the actual method is chosen at **runtime** based on the object type.
```java
Animal animal;
animal = new Dog(); animal.sound(); // Dog barks.
animal = new Cat(); animal.sound(); // Cat meows.
```

#### ⭐ INTERVIEW EXTRA — Overloading vs Overriding (classic question)
| | Overloading | Overriding |
|--|-------------|------------|
| When resolved | Compile time | Runtime |
| Where | Same class | Parent ↔ child |
| Signature | Must **differ** (params) | Must be **same** |
| Return type | Can differ | Same (or covariant) |
| Also called | Static / early binding | Dynamic / late binding |

**Upcasting & dynamic dispatch:** `Animal a = new Dog();` is *upcasting*. The reference type (`Animal`) decides **what methods you can call**; the object type (`Dog`) decides **which version actually runs**. This runtime selection is **dynamic method dispatch** — the engine behind run-time polymorphism and most design patterns.

> Note: `static`, `final`, and `private` methods **cannot be overridden** (they're bound at compile time).

---

## ⭐ INTERVIEW EXTRA — `equals()`, `hashCode()`, `toString()`
Every class implicitly extends `Object`, which gives these. You override them in LLD all the time:
- **`toString()`** — readable representation, great for debugging/logging.
- **`equals()`** — logical equality (e.g. two `Money` objects with same amount). Default `==` only checks reference identity.
- **`hashCode()`** — must be consistent with `equals()`. **Contract:** equal objects MUST have equal hashCodes (else they break in `HashMap`/`HashSet`).

```java
class Point {
    int x, y;
    Point(int x, int y){ this.x=x; this.y=y; }
    @Override public boolean equals(Object o){
        if (this==o) return true;
        if (!(o instanceof Point)) return false;
        Point p=(Point)o; return x==p.x && y==p.y;
    }
    @Override public int hashCode(){ return java.util.Objects.hash(x, y); }
    @Override public String toString(){ return "Point("+x+","+y+")"; }
}
```

---
---

# 🆕 Java-Interview Additions (beyond the LLD note)

## 9. Java is ALWAYS Pass-by-Value

The single most misunderstood Java question. Java passes **everything by value** — but for objects, the *value being copied is the reference*.

```java
void changePrimitive(int x)      { x = 100; }              // caller unaffected
void mutateObject(Human h)       { h.name = "Changed"; }   // caller SEES this
void reassignObject(Human h)     { h = new Human(); }      // caller unaffected!

int a = 5;
Human somesh = new Human();
somesh.name = "Somesh";

changePrimitive(a);        // a is still 5
mutateObject(somesh);      // somesh.name is now "Changed"
reassignObject(somesh);    // somesh still points to the SAME object
```

**The one-liner to say in an interview:**
> "Java is always pass-by-value. For objects, the value passed is a **copy of the reference** — so a method can *mutate* the object the caller sees, but *reassigning* the parameter has no effect on the caller."

---

## 10. Method Overriding — the Exact Rules

Interviewers love these edge cases because most candidates only know the happy path.

| Rule | Detail |
|------|--------|
| **Signature** | Method name + parameters must be identical |
| **Return type** | Same, or a **covariant** (subtype) return — `Animal getPet()` can be overridden as `Dog getPet()` |
| **Access modifier** | Can be **same or wider**, never narrower — `protected` → `public` ✅, `public` → `protected` ❌ (compile error) |
| **Checked exceptions** | Can throw **same, fewer, or narrower** checked exceptions — never new/broader ones. Unchecked exceptions: no restriction |
| **`static` methods** | Not overridden — they are **hidden** (method hiding). Resolved by *reference type*, not object type |
| **`private` methods** | Not inherited at all, so "overriding" one just creates an unrelated new method |
| **`final` methods** | Compile error if you try |

```java
class Parent {
    protected Animal getPet() throws IOException { ... }
    static void greet() { System.out.println("Parent"); }
}
class Child extends Parent {
    @Override
    public Dog getPet() throws FileNotFoundException { ... } // ✅ wider access, covariant return, narrower exception
    static void greet() { System.out.println("Child"); }     // ⚠️ hiding, NOT overriding
}

Parent p = new Child();
p.greet();   // prints "Parent" — static = reference type decides!
```

> **Always use `@Override`.** It's not decoration — it makes the compiler verify you actually overrode something (catches typos like `equals(Point o)` instead of `equals(Object o)`).

---

## 11. Constructor Edge Cases (rapid-fire round)

- **Can a constructor be `private`?** ✅ Yes — that's how **Singleton** and factory methods (`LocalDate.of(...)`) work. It blocks outside instantiation.
- **Can a constructor be `final`, `static`, or `abstract`?** ❌ No. Constructors aren't inherited (nothing to override → `final` meaningless), always run on an instance (`static` meaningless), and must have a body (`abstract` meaningless).
- **Can a constructor call another constructor?** ✅ `this(...)` — must be the **first statement**, so you can't have both `this(...)` and `super(...)`.
- **Can a constructor be overloaded?** ✅ Yes. **Overridden?** ❌ No (not inherited).
- **What if the parent has no no-arg constructor?** Child constructors **must** explicitly call `super(args)` or the code won't compile — classic trick question.

**Object initialization order** (asked as "what prints?"):
```
1. Parent static blocks     (once, at class load)
2. Child static blocks      (once, at class load)
3. Parent instance blocks + parent constructor
4. Child instance blocks + child constructor
```

---

## 12. `final` vs `finally` vs `finalize` (classic filter question)

| | What it is | Where |
|--|-----------|-------|
| `final` | Keyword — constant variable / non-overridable method / non-extendable class | Declarations |
| `finally` | Block that **always runs** after `try/catch` (even on return/exception) — used for cleanup | Exception handling |
| `finalize()` | Method the GC *might* call before reclaiming an object. **Deprecated since Java 9, removed in 18** — never rely on it; use `try-with-resources` / `AutoCloseable` | `Object` class |

> Bonus trap: *"When does `finally` NOT run?"* → `System.exit()`, JVM crash, or the thread is killed. A `return` inside `finally` also silently swallows exceptions — flag it as a code smell.

---

## 13. Wrapper Classes, Autoboxing & the Integer Cache

Every primitive has a wrapper (`int` → `Integer`, etc.). Java auto-converts between them:
- **Autoboxing:** `Integer x = 5;` (primitive → object)
- **Unboxing:** `int y = x;` (object → primitive)

### ⚠️ The Integer cache trap (very frequently asked)
```java
Integer a = 127, b = 127;
System.out.println(a == b);      // true  — cached!

Integer c = 128, d = 128;
System.out.println(c == d);      // false — new objects!
System.out.println(c.equals(d)); // true  — always compare wrappers with equals()
```
Java caches `Integer` objects for **-128 to 127** (also `Byte`, `Short`, `Long`, `Character` 0–127, `Boolean`). Inside the range, autoboxing returns the *same cached object*, so `==` happens to work; outside it, `==` compares different references.

**Rules to state:**
1. Never compare wrapper objects with `==` — use `.equals()`.
2. Unboxing a `null` wrapper throws **`NullPointerException`** (`int x = (Integer) null;`) — a real production bug pattern, e.g. a nullable `Integer` from a DB mapped to `int`.
3. Prefer primitives in hot paths — boxing allocates objects (GC pressure).

---

## 14. Nested Classes — static nested vs inner vs anonymous

| Type | Declared as | Holds outer instance ref? | Use case |
|------|-------------|:---:|----------|
| **Static nested** | `static class Node` inside a class | ❌ No | Helper tied to the class, not an instance — e.g. `Map.Entry`, your `Node` inside a `LinkedList` impl |
| **Inner (non-static)** | `class Inner` inside a class | ✅ Yes (implicit `Outer.this`) | Needs access to outer instance state — e.g. `Iterator` implementations |
| **Local** | Inside a method | ✅ (and effectively-final locals) | Rare; scoped helpers |
| **Anonymous** | `new Interface() { ... }` | ✅ | One-off implementations; mostly replaced by **lambdas** for functional interfaces |

```java
class Outer {
    private int data = 10;
    static class StaticNested { }              // new Outer.StaticNested()
    class Inner { int get() { return data; } } // outer.new Inner()
}
```

> **Interview point:** a non-static inner class secretly holds a reference to its outer instance → it can cause **memory leaks** (the outer object can't be GC'd while the inner one lives, e.g. in a long-lived listener/thread). Default to **static nested** unless you need the outer state — this is Effective Java Item 24.

---

## 15. Association vs Aggregation vs Composition (HAS-A, precisely)

All three are "HAS-A", differing in **ownership/lifetime coupling**:

| Relationship | Meaning | Lifetime | Example |
|--------------|---------|----------|---------|
| **Association** | Objects just know each other | Independent | `Doctor` ↔ `Patient` |
| **Aggregation** | Whole–part, but part survives alone (weak HAS-A) | Part outlives whole | `Team` has `Player`s — delete team, players remain |
| **Composition** | Whole *owns* part (strong HAS-A) | Part dies with whole | `House` has `Room`s — delete house, rooms are gone |

```java
class Team {                       // Aggregation — players injected from outside
    private List<Player> players;
    Team(List<Player> players) { this.players = players; }
}
class House {                      // Composition — rooms created & owned inside
    private final List<Room> rooms = new ArrayList<>();
    House() { rooms.add(new Room("kitchen")); }
}
```

> Rakuten Pay analogy: a `Payment` HAS-A `Transaction` record (**composition** — a transaction can't exist without its payment), while a `User` HAS-A `PaymentMethod` (**aggregation** — the card exists independently and can be attached to another account).

---

## ⭐ Quick Revision — Likely Interview Questions
1. Difference between class and object?
2. Abstract class vs interface — when to use each?
3. Why no multiple inheritance with classes? What's the Diamond Problem?
4. Overloading vs overriding?
5. Composition vs inheritance — which to prefer and why?
6. Where are static vs instance variables stored?
7. Can you override a static / final / private method? (No — static is *hidden*, not overridden.)
8. equals–hashCode contract?
9. What does `this` / `super` do? Constructor chaining order?
10. What is dynamic method dispatch?
11. Is Java pass-by-value or pass-by-reference? Prove it with code.
12. What is a covariant return type? Can an override narrow the access modifier?
13. Can a constructor be private / final / static? Why or why not?
14. `final` vs `finally` vs `finalize`?
15. Why does `Integer a = 127; Integer b = 127; a == b` print `true` but fail for `128`?
16. Static nested class vs inner class — which should you default to and why?
17. Association vs aggregation vs composition?
18. What prints first — static block, instance block, or constructor? In what order across parent/child?

---

**Related:** [[02_Collections_Framework]] · [[03_Multithreading_Concurrency]] · [[04_JVM_Memory_GC]] · [[08_String_Immutability]]