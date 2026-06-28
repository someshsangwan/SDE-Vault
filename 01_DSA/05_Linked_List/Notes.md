# LinkedList – Complete Notes (Java | LeetCode | Interview Prep)

---

## 📌 What is a Linked List?

A Linked List is a **linear data structure** where elements (nodes) are connected using pointers.

* Each node contains:

    * Data
    * Reference to next node

---

## 🧱 Node Structure (Java) ⭐

```java
class Node {
    int val;
    Node next;

    // Constructor
    Node(int val) {
        this.val = val;
        this.next = null;
    }
}
```

---

## 🧠 Types of Linked Lists

1. **Singly Linked List**
2. **Doubly Linked List**
3. **Circular Linked List**

---

## 🔗 Basic Operations

### 1. Traversal

```java
Node temp = head;
while (temp != null) {
    System.out.print(temp.val + " -> ");
    temp = temp.next;
}
```

---

### 2. Insert at Beginning

```java
Node newNode = new Node(val);
newNode.next = head;
head = newNode;
```

---

### 3. Insert at End

```java
Node newNode = new Node(val);

if (head == null) return newNode;

Node temp = head;
while (temp.next != null) {
    temp = temp.next;
}
temp.next = newNode;
```

---

### 4. Delete a Node (by value)

```java
if (head == null) return null;

if (head.val == val) return head.next;

Node temp = head;
while (temp.next != null && temp.next.val != val) {
    temp = temp.next;
}

if (temp.next != null) {
    temp.next = temp.next.next;
}
```

---

## ⚡ Key Patterns (VERY IMPORTANT)

---

### 1. Reverse Linked List ⭐

```java
Node prev = null;
Node curr = head;

while (curr != null) {
    Node next = curr.next;
    curr.next = prev;
    prev = curr;
    curr = next;
}

return prev;
```

---

### 2. Slow & Fast Pointer (Tortoise-Hare) ⭐⭐⭐

```java
Node slow = head;
Node fast = head;

while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;
}
```

---

### 3. Detect Cycle (Floyd’s Algorithm) ⭐

```java
while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;

    if (slow == fast) return true;
}
return false;
```

---

### 4. Find Middle Node ⭐

```java
return slow;
```

---

### 5. Merge Two Sorted Lists ⭐

```java
Node dummy = new Node(-1);
Node temp = dummy;

while (l1 != null && l2 != null) {
    if (l1.val < l2.val) {
        temp.next = l1;
        l1 = l1.next;
    } else {
        temp.next = l2;
        l2 = l2.next;
    }
    temp = temp.next;
}

temp.next = (l1 != null) ? l1 : l2;

return dummy.next;
```

---

### 6. Remove Nth Node from End ⭐

```java
Node dummy = new Node(0);
dummy.next = head;

Node fast = dummy;
Node slow = dummy;

for (int i = 0; i <= n; i++) {
    fast = fast.next;
}

while (fast != null) {
    fast = fast.next;
    slow = slow.next;
}

slow.next = slow.next.next;

return dummy.next;
```

---

### 7. Reverse Linked List II ⭐⭐

* Reverse between given positions (left → right)

---

## 🔑 How to Recognize LinkedList Problems

* Modifying **next pointers**
* No random access needed
* Patterns:

    * Reverse
    * Merge
    * Cycle
    * Middle
    * Kth node

---

## 🧩 Most Asked Interview Questions

### 🟢 Easy

* Reverse Linked List ⭐
* Merge Two Sorted Lists ⭐

### 🟡 Medium

* Linked List Cycle ⭐⭐⭐
* Remove Nth Node ⭐⭐⭐
* Reorder List ⭐⭐⭐
* Add Two Numbers ⭐⭐⭐
* Intersection of Lists ⭐⭐⭐

### 🔴 Hard

* Reverse Nodes in K Group ⭐⭐⭐
* Merge K Lists ⭐⭐⭐
* Copy List with Random Pointer ⭐⭐⭐

---

## ⚠️ Common Mistakes

* Losing reference (`next`)
* Null pointer errors
* Not using dummy node

---

## 💡 Pro Tips

* Always track:

    * prev
    * curr
    * next
* Draw diagrams
* Use dummy node for edge cases

---

## 🧠 Mental Model

```text
Break → Rewire → Move
```

---

## 🎯 Summary

* LinkedList = pointer manipulation
* Master:

    * Reverse
    * Slow/Fast pointer
    * Dummy node

---

**Practice daily — this topic becomes easy only with repetition.**
