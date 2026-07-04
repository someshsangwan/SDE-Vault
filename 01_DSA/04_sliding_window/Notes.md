# Sliding Window — Complete Notes (LeetCode / Interview Prep)

---

## 📌 What is Sliding Window?

Sliding Window is a **two-pointer technique** for processing **contiguous subarrays / substrings** efficiently. You maintain a window `[i..j]` (both inclusive) and slide it across the data, **reusing the previous window's computation** instead of recomputing from scratch.

**The core insight:** windows `[0..3]` and `[1..4]` share 3 elements. Brute force recomputes all 4 elements of every window → O(n·k) or O(n²). Sliding window just **adds the new element and removes the old one** → O(1) per step → **O(n) total**.

```
arr = [2, 5, 1, 8, 2, 9], k = 3
[2, 5, 1] sum = 8
   [5, 1, 8] sum = 8 - 2 + 8 = 14   ← reuse! subtract leaving, add entering
      [1, 8, 2] sum = 14 - 5 + 2 = 11
```

**Why O(n) even with a nested while loop:** each element enters the window once (`j++`) and leaves at most once (`i++`). Total pointer movements ≤ 2n. This is *amortized* analysis — the inner loop's total work across the whole run is bounded, even if one iteration shrinks a lot.

---

## ⚡ When to Use? (Pattern Recognition Triggers)

* Problem involves a **subarray / substring** (must be contiguous — if the problem says *subsequence*, sliding window usually does NOT apply)
* Asked for: **longest / shortest / count / max / min** of some contiguous segment
* Keywords: "substring", "subarray", "contiguous", "window", "consecutive"
* Constraints: n up to 10⁵–10⁶ → O(n²) brute force will TLE → you need O(n)

**When it does NOT work:** the window trick needs *monotonic* behavior — expanding must move the condition in one direction, shrinking in the other. Classic trap: **"subarray sum = k" with negative numbers** — adding an element might *decrease* the sum, so shrinking logic breaks. Use **prefix sum + HashMap** there instead (LC 560). Sliding window for sums is safe only when all numbers are ≥ 0.

---

## 🧠 Type 1: Fixed Size Window (size `k` given)

### Template

```java
int i = 0, j = 0;
while (j < n) {
    // 1. include arr[j] in window calculation

    if (j - i + 1 < k) {          // window not full yet → just grow
        j++;
    } else if (j - i + 1 == k) {  // window exactly k → process, then slide
        // 2. process window / update answer

        // 3. remove arr[i] from window calculation
        i++;
        j++;
    }
}
```

### ⚠️ Why `< k` and NOT `<= k` (common confusion — I asked this myself)

`j - i + 1` = current window size. The two branches split two different jobs:
- `< k` → "window still too small, keep growing"
- `== k` → "window full — process it, then move BOTH pointers" (size stays k forever after)

The window **does** reach size k — via the `== k` branch. Trace with `arr = [a,b,c,d,e]`, k=3:

| step | i | j | size | branch | action |
|---|---|---|---|---|---|
| 1 | 0 | 0 | 1 | `< 3` | j→1 |
| 2 | 0 | 1 | 2 | `< 3` | j→2 |
| 3 | 0 | 2 | **3** | `== 3` | process `[a,b,c]`, i→1, j→3 |
| 4 | 1 | 3 | 3 | `== 3` | process `[b,c,d]`, i→2, j→4 |
| 5 | 2 | 4 | 3 | `== 3` | process `[c,d,e]` ✅ |

If you write `<= k` instead: at step 3, size 3 takes the *expand* branch without processing → size becomes 4 → **neither branch matches → `j` never moves → infinite loop.** Not just skipped windows — a hang.

### Worked example: Max sum subarray of size k

```java
public int maxSum(int[] arr, int k) {
    int i = 0, j = 0, sum = 0, max = Integer.MIN_VALUE;
    while (j < arr.length) {
        sum += arr[j];                    // include arr[j]
        if (j - i + 1 < k) {
            j++;
        } else {                          // size == k
            max = Math.max(max, sum);     // process
            sum -= arr[i];                // remove arr[i]
            i++; j++;
        }
    }
    return max;
}
```
O(n) time, O(1) space.

### Fixed-window problem list
1. Max/Min sum subarray of size k
2. First negative number in every window of size k → **Deque of indices** (front = oldest negative still inside window)
3. Count occurrences of anagram → **int[26] frequency + match counter**
4. Maximum of all subarrays of size k (LC 239, Sliding Window Maximum) → **monotonic Deque** (see §DS section)
5. Max of min of every window size

---

## 🧠 Type 2: Variable Size Window (Most Important 🔥)

Window grows and shrinks based on a **condition**. This is where longest/shortest/count problems live.

### The universal mantra — burn this in:

> ### 👉 **Expand → Validate → Shrink → Update answer**

Every variable-window problem is these 4 steps in a loop:
1. **Expand:** pull `arr[j]` into the window (update sum/map/set).
2. **Validate:** is the window still legal? (sum ≤ k? distinct ≤ k? no duplicates?)
3. **Shrink:** while it's illegal, evict `arr[i]` from the left (`i++`) until legal again.
4. **Update answer:** record window size / count, then `j++`.

### Template

```java
int i = 0, j = 0;
while (j < n) {
    // 1. EXPAND: include arr[j] (update sum / map / set)

    while (/* 2. VALIDATE fails: window is invalid */) {
        // 3. SHRINK: remove arr[i] (downdate sum / map / set)
        i++;
    }

    // 4. UPDATE ANSWER (window [i..j] is valid here)
    // ans = Math.max(ans, j - i + 1);

    j++;
}
```

### ⚠️ Longest vs Shortest — WHERE you update the answer flips:

* **Longest** valid window (template above): shrink until valid, **then** update → you record the *biggest* valid window ending at j.
* **Shortest** valid window (e.g., min subarray with sum ≥ target, LC 209; Min Window Substring): update **inside** the shrink loop — the moment the window *becomes* valid, record it, then keep shrinking to try for even smaller:

```java
while (/* window IS valid */) {
    ans = Math.min(ans, j - i + 1);   // record BEFORE shrinking away validity
    // remove arr[i];
    i++;
}
```

Mixing these two up is the #1 variable-window bug.

### Worked example: Longest substring with at most K distinct characters

```java
public int longestKDistinct(String s, int k) {
    Map<Character, Integer> freq = new HashMap<>();
    int i = 0, j = 0, ans = 0;
    while (j < s.length()) {
        freq.merge(s.charAt(j), 1, Integer::sum);        // EXPAND

        while (freq.size() > k) {                        // VALIDATE fails
            char left = s.charAt(i);
            freq.merge(left, -1, Integer::sum);          // SHRINK
            if (freq.get(left) == 0) freq.remove(left);  // ← don't forget! size() must be honest
            i++;
        }

        ans = Math.max(ans, j - i + 1);                  // UPDATE (longest → after shrink)
        j++;
    }
    return ans;
}
```

### Variable-window problem list
1. Longest/smallest subarray with sum k (positives only!) → **running int sum**
2. Longest subarray/substring with k distinct → **HashMap freq** (above)
3. Longest substring without repeating characters (LC 3) → **HashSet** or Map of last index
4. Fruit into baskets / pick toy (LC 904) → k-distinct with k=2
5. **Minimum Window Substring (LC 76)** → HashMap + match counter — the hardest, rehearse separately
6. Count subarrays with exactly K distinct (LC 992) → trick: `exactly(K) = atMost(K) − atMost(K−1)`

---

## 🧰 Which Data Structure Inside the Window? (decide BEFORE coding)

The window pointers are always the same — what changes per problem is the **bookkeeping structure** that answers "is my window valid?" in O(1). Choose by what the condition needs to know:

| Window condition asks… | Use | Example problems |
|---|---|---|
| running sum / average | plain `int`/`long` | max sum size k, min len sum ≥ target |
| "how many of each element?" (frequency) | **HashMap<Character,Integer>** (or `int[26]` for a–z — faster) | k distinct, anagrams, min window substring |
| "have I seen this element?" (uniqueness) | **HashSet** | longest substring w/o repeating chars |
| "how many *distinct*?" | HashMap + its `size()` | at-most-K-distinct family |
| "current max/min of the window?" | **Monotonic Deque** (`ArrayDeque` of *indices*) | sliding window maximum, first negative in window |
| "count windows matching a target multiset?" | freq array + **`matched` counter** | anagram count, LC 76, LC 567 |

### HashMap pattern (frequency tracking)
- Expand: `map.merge(c, 1, Integer::sum)`
- Shrink: `map.merge(c, -1, Integer::sum)`, and **`map.remove(c)` when count hits 0** — otherwise `map.size()` (your distinct count) lies.
- Prefer `int[26]` + an `int distinct` counter when keys are lowercase letters — same logic, no boxing, interviewers like it.

### HashSet pattern (uniqueness)
For "no repeats" windows: try to add `s.charAt(j)`; while it's already in the set, remove `s.charAt(i)` and `i++`. The set IS the window's contents.

### Monotonic Deque pattern (window max — LC 239, know this cold)
Problem: max of every window of size k, in O(n). A max-heap works but is O(n log n) and stale values linger. The Deque trick keeps **indices** of useful candidates in **decreasing value order** — front is always the current max:

```java
public int[] maxSlidingWindow(int[] nums, int k) {
    Deque<Integer> dq = new ArrayDeque<>();   // stores INDICES, values decreasing
    int[] ans = new int[nums.length - k + 1];
    for (int j = 0; j < nums.length; j++) {
        // evict from BACK: smaller elements can never be max while nums[j] is alive
        while (!dq.isEmpty() && nums[dq.peekLast()] <= nums[j]) dq.pollLast();
        dq.offerLast(j);
        // evict from FRONT: index fell out of the window's left edge
        if (dq.peekFirst() <= j - k) dq.pollFirst();
        // window full → front index holds the max
        if (j >= k - 1) ans[j - k + 1] = nums[dq.peekFirst()];
    }
    return ans;
}
```

Why **indices, not values**: you need the index to know when the front has slid out of the window. Each index is pushed once and popped once → O(n).

---

## 🧨 Common Pitfalls (self-inflicted wounds checklist)

1. **`<= k` in the fixed template** → infinite loop (see trace above).
2. **Longest vs shortest answer placement** — after the shrink loop vs inside it.
3. **Forgetting `map.remove(key)` at count 0** → `map.size()` overcounts distinct → window shrinks forever / wrong answer.
4. **`if` instead of `while` for shrinking** — one new element can invalidate the window by more than one step; `if` leaves it invalid.
5. **Negative numbers + sum condition** → sliding window invalid; switch to prefix sum + HashMap (LC 560).
6. **Deque of values instead of indices** → can't detect when the max leaves the window.
7. **Subsequence vs subarray** — read the problem twice; window = contiguous only.
8. **Off-by-one on window size** — it's always `j - i + 1` (inclusive ends). Say it out loud while coding.

---

## 📊 Complexity Summary

| Variant | Time | Space |
|---|---|---|
| Fixed, sum/count only | O(n) | O(1) |
| Variable + HashMap/Set | O(n) | O(min(n, alphabet)) — e.g. O(26) for letters |
| Monotonic Deque max/min | O(n) | O(k) |

Interview-ready line: *"Each element enters and leaves the window at most once, so the two pointers do ≤ 2n moves total — O(n) amortized, even with the nested while."*

---

## ✅ Self-Check Questions (answer without looking)

1. In the fixed template, why does `<= k` cause an infinite loop and not just a wrong answer?
2. You're solving "shortest subarray with sum ≥ target" — where does `ans = min(...)` go, and why?
3. Longest-K-distinct: what goes wrong if you decrement a char's count to 0 but leave it in the map?
4. Why can't sliding window solve "subarray sum equals k" when the array has negatives? What replaces it?
5. Sliding Window Maximum: why must the Deque store indices, and why is the whole thing O(n) despite the inner while?
6. Given "longest substring with at most 2 kinds of fruit" — which template + which DS, in 5 seconds?

*(Answers: 1 — at size k the expand branch fires without processing, size hits k+1, then neither branch matches and j freezes. 2 — inside the shrink-while, because the window is valid there and every shrink step might be a new minimum. 3 — `map.size()` still counts the zombie key → distinct count inflated → shrink loop never exits / skips valid windows. 4 — adding an element may decrease the sum, so validity isn't monotonic → prefix-sum + HashMap of "prefix seen before". 5 — indices let you check `front <= j - k` to evict expired maxes; each index pushed/popped once → total work ≤ 2n. 6 — variable window, HashMap freq with size ≤ 2 → it's longest-K-distinct with k=2.)*

---

## 🔗 Related
- [[01_DSA/03_Two-Pointer/Notes|Two-Pointer]] — sliding window is two-pointer with a maintained window state
- Prefix Sum + HashMap — the fallback when negatives break the window
- LC must-solve set: **3, 76, 209, 239, 424, 438, 560 (contrast!), 567, 904, 992, 1004**