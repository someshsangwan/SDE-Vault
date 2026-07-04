# Chapter 7 — Generics & Type Erasure

> Shorter chapter, two star questions: **type erasure** and **PECS (`? extends` vs `? super`)**. Everything else is supporting cast.

**Related:** [[01_OOP_Fundamentals]] · [[02_Collections_Framework]] (generic arrays trap in the hand-rolled HashMap)

---

## Words used in this chapter (plain meanings)

| Word | Plain meaning |
|------|---------------|
| **Generic** | A class/method with a type *parameter*: `List<T>` — "a list of *something*, you tell me what" |
| **Type parameter** | The placeholder: `T`, `K`, `V`, `E` |
| **Type erasure** | The compiler *deletes* generic types after checking — at runtime `List<String>` is just `List` |
| **Wildcard** | `?` = "some unknown type" |
| **Bounded** | Restricting the placeholder: `T extends Number` = "T, but must be a Number" |
| **Invariant** | `List<Dog>` is NOT a `List<Animal>`, even though Dog is an Animal |

---

## 1. Why generics exist

Before Java 5, collections held `Object` — every read needed a cast, and wrong casts exploded **at runtime**:

```java
// BEFORE (raw) — compiles fine, dies in production:
List list = new ArrayList();
list.add("hello");
list.add(42);                                  // nothing stops you
String s = (String) list.get(1);               // 💥 ClassCastException at RUNTIME

// AFTER (generic) — the same bug becomes a compile error:
List<String> list2 = new ArrayList<>();
list2.add("hello");
list2.add(42);                                 // ❌ won't compile — caught NOW, not at 3am
String s2 = list2.get(0);                      // no cast needed
```

**One-liner:** *generics move type errors from runtime to compile time.*

### Writing your own
```java
// Generic class — T is decided by whoever creates it:
class Pair<K, V> {
    private final K key;
    private final V value;
    Pair(K key, V value) { this.key = key; this.value = value; }
    K getKey() { return key; }
    V getValue() { return value; }
}
Pair<String, Integer> p = new Pair<>("age", 25);

// Generic METHOD — <T> declared before the return type:
static <T> T firstOrNull(List<T> list) {
    return list.isEmpty() ? null : list.get(0);
}
```

### Bounded type parameters — "T, but with abilities"
```java
// T must be Comparable, so we're allowed to call compareTo:
static <T extends Comparable<T>> T max(List<T> list) {
    T best = list.get(0);
    for (T x : list) if (x.compareTo(best) > 0) best = x;
    return best;
}
// multiple bounds: <T extends Number & Comparable<T>>  (class first, then interfaces)
```

---

## 2. ★ Type Erasure — what generics really are

**The compiler checks your types, then DELETES them.** At runtime, `List<String>` and `List<Integer>` are the exact same class: `List`. The generics exist only at compile time.

```java
List<String>  a = new ArrayList<>();
List<Integer> b = new ArrayList<>();
System.out.println(a.getClass() == b.getClass());   // true! Both are just ArrayList
```

Under the hood the compiler: replaces `T` with `Object` (or its bound: `T extends Number` → `Number`), and inserts the casts for you at every read site. Your pre-Java-5 code still exists — the compiler just writes it for you, safely.

**Why erasure? Backwards compatibility** — Java 5 generics had to run on old JVMs and interoperate with old raw-type code. (C# chose the other path: real "reified" generics at runtime.)

### ⭐ The consequences (each is a follow-up question)

```java
class Box<T> {
    T value;
    void make()   { value = new T(); }        // ❌ 1. can't instantiate T (which constructor? T is gone at runtime)
    boolean is(Object o) { return o instanceof T; }   // ❌ 2. can't instanceof T (T doesn't exist at runtime)
    T[] arr = new T[10];                      // ❌ 3. can't create generic arrays
    Class<T> cls = T.class;                   // ❌ 4. no T.class
}

// 5. can't overload on generic type — both erase to process(List):
void process(List<String> l)  { }
void process(List<Integer> l) { }             // ❌ compile error: same erasure

// 6. static members can't use the CLASS's T (statics are shared; T differs per instance)
```

**Workarounds worth knowing:**
```java
// pass the Class object explicitly ("type token"):
static <T> T create(Class<T> type) throws Exception { return type.getDeclaredConstructor().newInstance(); }

// generic array — the cast trick from our hand-rolled HashMap ([[02_Collections_Framework]] §6):
@SuppressWarnings("unchecked")
T[] arr = (T[]) new Object[10];
```

---

## 3. ★ Invariance, and why arrays are the odd one out

Even though `Dog extends Animal`:
```java
List<Animal> animals = new ArrayList<Dog>();   // ❌ does NOT compile — generics are INVARIANT
```
Why? If it compiled, you could do `animals.add(new Cat())` — and your `List<Dog>` now contains a Cat. Compile-time safety gone.

**But arrays allow it (covariant) — and pay the price at runtime:**
```java
Animal[] animals = new Dog[3];       // ✅ compiles (arrays are covariant)
animals[0] = new Cat();              // 💥 ArrayStoreException at RUNTIME
```
**Interview one-liner:** *"Arrays are covariant and fail at runtime; generics are invariant and fail at compile time — generics learned from arrays' mistake."*

---

## 4. ★★ Wildcards & PECS — the #1 generics question

Invariance is safe but annoying — a method taking `List<Animal>` rejects your `List<Dog>`. Wildcards fix that:

```java
// ? extends Animal — "a list of Animal OR ANY SUBTYPE" (List<Dog> ✅, List<Cat> ✅)
static double totalWeight(List<? extends Animal> list) {
    double sum = 0;
    for (Animal a : list) sum += a.getWeight();   // READING as Animal: safe ✅
    // list.add(new Dog());                        // WRITING: ❌ forbidden! (it might be a List<Cat>)
    return sum;
}

// ? super Dog — "a list of Dog OR ANY SUPERTYPE" (List<Animal> ✅, List<Object> ✅)
static void addDogs(List<? super Dog> list) {
    list.add(new Dog());                          // WRITING a Dog: safe ✅ (any supertype-list accepts a Dog)
    // Dog d = list.get(0);                       // READING: ❌ only as Object (it might be List<Object>)
}
```

### PECS — **P**roducer **E**xtends, **C**onsumer **S**uper
- The collection **produces** values for you (you read from it) → `? extends T`.
- The collection **consumes** values from you (you write into it) → `? super T`.
- You do both → no wildcard, exact `List<T>`.

**The JDK itself is the best example** (say this one):
```java
public static <T> void copy(List<? super T> dest, List<? extends T> src)
//                          ↑ consumer: super        ↑ producer: extends
```
Also `Collections.sort(List<T>, Comparator<? super T>)` — a `Comparator<Animal>` can sort a `List<Dog>`.

### The three lists (rapid-fire)
| | Can hold | You can add |
|--|----------|-------------|
| `List<Object>` | anything | anything |
| `List<?>` | *some specific* unknown type | **nothing** (except null) — you don't know what it holds |
| raw `List` | anything, no checks | anything — all safety off (legacy only, never write it) |

`List<?>` isn't useless — it means "I only need size/iterate/clear, works for every list type."

---

## 5. Practical bits you'll actually type

```java
// The comparator generics you already use (Chapter 2 §10):
Comparator<Txn> byAmount = Comparator.comparing(Txn::getAmount);

// A generic Response wrapper (every Spring project has one):
class ApiResponse<T> {
    int code;
    T data;
    static <T> ApiResponse<T> ok(T data) { ... }
}
ApiResponse<List<UserDto>> resp = ApiResponse.ok(users);

// Bounded generic in a repository interface — Spring Data itself:
interface JpaRepository<T, ID> { T save(T entity); Optional<T> findById(ID id); }
```

**The diamond `<>`** — `new HashMap<String, List<Txn>>()` → `new HashMap<>()`; compiler infers from the left side. And `var map = new HashMap<String, Integer>()` flips where the type is written.

---

## ⭐ Quick Revision — Likely Interview Questions

1. What problem do generics solve? (runtime cast errors → compile-time errors)
2. What is type erasure? Why did Java choose it? (backwards compatibility)
3. Prove erasure in one line. (`new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass()` → true)
4. Name 4 things erasure forbids (new T(), instanceof T, T.class, generic arrays, overload by type argument).
5. How did our hand-rolled HashMap create its Node array? (cast trick + @SuppressWarnings)
6. Is `List<Dog>` a `List<Animal>`? Why not — what could go wrong?
7. Arrays vs generics variance — which fails at compile time vs runtime? (ArrayStoreException)
8. Explain PECS with `Collections.copy`'s signature.
9. `? extends T`: why can you read but not add? `? super T`: why the reverse?
10. `List<Object>` vs `List<?>` vs raw `List`?
11. Write a generic `max` method with a bounded type parameter.
12. Why can't static fields use the class's type parameter?
