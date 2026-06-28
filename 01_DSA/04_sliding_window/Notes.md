# Sliding Window – Complete Notes (LeetCode / Interview Prep)

---

## 📌 What is Sliding Window?

Sliding Window is a **two-pointer technique** used to process **subarrays / substrings efficiently** by maintaining a window (range) and adjusting it dynamically.

Instead of recomputing results for every subarray → we **reuse previous computation**.

---

## ⚡ When to Use Sliding Window?

Use it when:

* Problem involves **subarray / substring**
* Asked for:

    * Longest / shortest / count
    * Continuous segment
* Keywords:

    * "substring"
    * "subarray"
    * "contiguous"
    * "window"
* Constraints:

    * Array/String size is large → brute force O(n²) not acceptable

---

## 🧠 Types of Sliding Window

### 1. Fixed Size Window

* Window size `k` is given
* Move window by 1 step

#### Template

```java
int i = 0, j = 0;
while (j < n) {
    // include arr[j]

    if (j - i + 1 < k) {
        j++;
    } else if (j - i + 1 == k) {
        // process window

        // remove arr[i]
        i++;
        j++;
    }
}
```

---

### 2. Variable Size Window (Most Important 🔥)

* Window expands and shrinks based on condition

#### Template

```java
int i = 0, j = 0;
while (j < n) {

    // include arr[j]

    while (condition breaks) {
        // remove arr[i]
        i++;
    }

    // update answer

    j++;
}
```

---

## 🔑 How to Recognize Sliding Window

Ask yourself:

1. Is it about **contiguous elements**?
2. Can I use **two pointers (i, j)**?
3. Can I **expand and shrink window**?
4. Is brute force giving O(n²)?

If YES → Sliding Window likely.

---

## 🧩 Common Patterns

### 1. Maximum / Minimum Sum Subarray of Size K

* Fixed window

---

### 2. Longest Substring Without Repeating Characters

* Variable window
* Use HashSet / Map

---

### 3. Count Subarrays with Sum = K

* Prefix sum + window (sometimes hybrid)

---

### 4. Longest Substring with K Unique Characters

* Map + shrinking logic

---

### 5. Minimum Window Substring (Hard 🔥)

* Expand → satisfy condition
* Shrink → optimize window

---

## 📊 Data Structures Used

| Use Case           | DS        |
| ------------------ | --------- |
| Unique characters  | Set       |
| Frequency tracking | HashMap   |
| Fixed numeric ops  | Variables |

---

## 🧠 Mental Model

```
Expand → until valid
Shrink → until optimal
Repeat
```

---

## 🚨 Common Mistakes

* Forgetting to shrink window
* Wrong condition in while loop
* Not updating answer at correct time
* Off-by-one errors (`j - i + 1`)

---

## 🏆 Most Asked Sliding Window Questions

### 🟢 Easy

* Maximum Sum Subarray of Size K
* Average of Subarrays of Size K

---

### 🟡 Medium (VERY IMPORTANT)

* Longest Substring Without Repeating Characters ⭐
* Longest Repeating Character Replacement ⭐
* Permutation in String ⭐
* Find All Anagrams in a String ⭐
* Maximum Number of Vowels in Substring of Size K

---

### 🔴 Hard (Interview Level)

* Minimum Window Substring ⭐⭐⭐
* Sliding Window Maximum ⭐⭐⭐
* Subarrays with K Different Integers ⭐⭐⭐

---

## 💡 Pro Tips

* Always write **i (start), j (end)**
* Expand first, then shrink
* Use **while loop for shrinking**, not if
* Practice patterns, not just questions

---

## 🔁 Practice Order (Recommended)

1. Fixed window problems
2. Variable window basic
3. HashMap-based window
4. Hard problems (minimum window type)

---

## 🧪 Example Walkthrough (Core Idea)

Problem: Longest substring without repeating chars

```
Input: "abcabcbb"

Step:
i=0, j=0 → expand
Use set

If duplicate:
  remove from left (i++)
Else:
  update max length
```

---

## 🎯 Summary

* Sliding Window = optimize brute force
* Works on contiguous data
* Two types: Fixed & Variable
* Master shrinking logic → key to solving hard problems

---

## 🚀 Next Step

After this, learn:

* Two Pointer (non-window)
* Prefix Sum
* Monotonic Queue (for sliding max)

---

**Consistency > Speed. Practice daily 2–3 problems.**
