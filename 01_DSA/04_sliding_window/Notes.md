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
/ FIxed-
// 1)Max/Min subarray of size K;
// 2) 1st -ve in every window of size k
// 3)count occurance of anagram
// 4)max of all subarray of size k;
// 5)max of min of every window size;

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

// Variable
// 1) longest/smallest subarray of sum k
// 2) longest subarray/sub string of k distinct
// 3) length of longest substring No repeated character
// 4) Pick toy
// 5) Minimum substring Window   // most important/ Hard problem of sliding window ---