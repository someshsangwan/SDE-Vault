# Binary Search — Interview Notes (Java)

> **Core idea:** Repeatedly halve a **sorted** search space. Each step throws away half → **O(log n)** time, **O(1)** space (iterative).
> If the input is sorted (or you can define a monotonic "yes/no" condition), think binary search.

---

## 1. The canonical template (find exact target)

```java
// Returns index of target, or -1 if not found.
int binarySearch(int[] a, int target) {
    int lo = 0, hi = a.length - 1;        // inclusive bounds [lo, hi]
    while (lo <= hi) {                    // <=  because bounds are inclusive
        int mid = lo + (hi - lo) / 2;     // avoids int overflow (NOT (lo+hi)/2)
        if (a[mid] == target) return mid;
        else if (a[mid] < target) lo = mid + 1;
        else hi = mid - 1;
    }
    return -1;
}
```

### The 3 things that cause 90% of binary-search bugs
1. **`mid` overflow** → always `lo + (hi - lo) / 2`, never `(lo + hi) / 2`.
2. **Loop condition** → `lo <= hi` for inclusive `[lo, hi]`; `lo < hi` for the "boundary" template below.
3. **Update step** → make sure the search space *always shrinks*, or you get an infinite loop.

---

## 2. The MOST IMPORTANT template — boundary search (first/last occurrence)

Most real interview questions aren't "find the target" — they're **"find the first/last position where a condition is true."** Master this and you can solve the majority of them.

### ⭐ MY PREFERRED STYLE — "store the answer, keep shrinking"

Same `lo <= hi` loop as the basic template (nothing new to memorize). When you hit the target, **save the index in `ans`, then deliberately keep searching** toward the side you want. This is the cleanest way to think about it. (Solves LC 34 directly.)

```java
class Solution {
    // First (leftmost) occurrence of target, or -1.
    public int firstOccurrence(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;
        int ans = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] == target) {
                ans = mid;          // found it — but keep looking LEFT
                hi = mid - 1;
            } else if (nums[mid] > target) {
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return ans;
    }

    // Last (rightmost) occurrence of target, or -1.
    public int lastOccurrence(int[] nums, int target) {
        int lo = 0, hi = nums.length - 1;
        int ans = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] == target) {
                ans = mid;          // found it — but keep looking RIGHT
                lo = mid + 1;
            } else if (nums[mid] > target) {
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return ans;
    }

    // LC 34 — Find First and Last Position of Element in Sorted Array
    public int[] searchRange(int[] nums, int target) {
        return new int[]{ firstOccurrence(nums, target),
                          lastOccurrence(nums, target) };
    }
}
```

**Why this works:** the ONLY difference between first vs last is what you do on a match:
- **First** → `hi = mid - 1` (drop the right half, hunt left for an earlier match).
- **Last** → `lo = mid + 1` (drop the left half, hunt right for a later match).

The `ans` variable remembers the best match seen so far, so you never lose it while you keep shrinking. If target never appears, `ans` stays `-1`.

### Alternative: lower/upper bound (`lo < hi`) template
Good to also recognize this classic form — it returns an *insertion point* rather than -1, which is handy for counting.

```java
// Leftmost index where a[i] >= target (lower_bound). Returns a.length if none.
int lowerBound(int[] a, int target) {
    int lo = 0, hi = a.length;            // hi = length (exclusive)
    while (lo < hi) {                     // note: <  not <=
        int mid = lo + (hi - lo) / 2;
        if (a[mid] < target) lo = mid + 1;  // condition not met → go right
        else hi = mid;                      // condition met → keep mid, go left
    }
    return lo;                            // lo == hi == answer
}
```
- For `upperBound` (first element **> target**), change `a[mid] < target` to `a[mid] <= target`.
- `upperBound - lowerBound` = **count of occurrences** of target. 🔑
- Relationship to my style: `lowerBound` == `firstOccurrence` (when target exists); `upperBound - 1` == `lastOccurrence`.

### The "predicate" mental model (binary search on answer)
Think of a boolean function `f(x)` that goes `F F F T T T` (monotonic). Binary search finds the **first T**.
This unlocks the hard problems: you're not searching an array, you're searching the *answer space*.

```java
// Generic: smallest x in [lo, hi] such that f(x) is true.
int firstTrue(int lo, int hi, Predicate<Integer> f) {
    while (lo < hi) {
        int mid = lo + (hi - lo) / 2;
        if (f.test(mid)) hi = mid;
        else lo = mid + 1;
    }
    return lo;
}
```

---

## 3. Java built-ins (know these — interviewers may allow them)

```java
// Arrays: returns index if found, else -(insertionPoint) - 1
int idx = Arrays.binarySearch(arr, key);

// Collections (sorted List)
int j = Collections.binarySearch(list, key);

// TreeMap / TreeSet give you bounds directly:
TreeMap<Integer,V> m = new TreeMap<>();
m.ceilingKey(x);   // smallest key >= x   (like lowerBound)
m.floorKey(x);     // largest key  <= x
m.higherKey(x);    // smallest key >  x
m.lowerKey(x);     // largest key  <  x
```

⚠️ In interviews, **implement it yourself** unless told otherwise — they want to see you know the mechanics.

---

## 4. Complexity
- **Time:** O(log n) — each step halves the range.
- **Space:** O(1) iterative, O(log n) recursive (call stack).
- On answer-space search: O(log(range) × cost_of_check).

---

## 5. Most-asked interview questions (ranked)

### ⭐ Tier 1 — must-do (fundamentals + boundaries)
| # | Problem | Pattern |
|---|---------|---------|
| 704 | **Binary Search** | canonical template |
| 35 | **Search Insert Position** | lower bound |
| 34 | **First & Last Position of Element** | lower + upper bound |
| 278 | **First Bad Version** | predicate / first-true |
| 69 | **Sqrt(x)** | search on answer |
| 374 | **Guess Number Higher/Lower** | classic |

### ⭐⭐ Tier 2 — very common (rotated / 2D / peak)
| # | Problem | Pattern |
|---|---------|---------|
| 33 | **Search in Rotated Sorted Array** | modified BS (find sorted half) |
| 81 | Search in Rotated Sorted Array II (dupes) | modified BS |
| 153 | **Find Minimum in Rotated Sorted Array** | boundary |
| 162 | **Find Peak Element** | BS without sorted array! |
| 74 | **Search a 2D Matrix** | treat as 1D |
| 240 | Search a 2D Matrix II | staircase |

### ⭐⭐⭐ Tier 3 — "search on answer" (the differentiator for mid-level+)
| # | Problem | Pattern |
|---|---------|---------|
| 875 | **Koko Eating Bananas** | min rate s.t. finishes in time |
|1011 | **Capacity to Ship Packages in D Days** | min capacity |
| 410 | Split Array Largest Sum | minimize max subarray sum |
| 4 | **Median of Two Sorted Arrays** | hard partition BS |
| 1482 | Minimum Days to Make Bouquets | predicate on answer |

---

## 6. Worked example — Koko Eating Bananas (LC 875)
> Koko eats `piles[i]` bananas at speed `k`/hour. Find the **minimum k** so she finishes in `h` hours.

Key insight: **not searching the array — searching the speed `k`** in `[1, max(piles)]`. `f(k)` = "can finish in h hours?" is monotonic (higher k → fewer hours). Find first-true.

```java
int minEatingSpeed(int[] piles, int h) {
    int lo = 1, hi = 0;
    for (int p : piles) hi = Math.max(hi, p);   // max speed ever needed

    while (lo < hi) {
        int k = lo + (hi - lo) / 2;
        if (hoursNeeded(piles, k) <= h) hi = k;  // fast enough → try slower
        else lo = k + 1;                         // too slow → go faster
    }
    return lo;
}

private long hoursNeeded(int[] piles, int k) {
    long hours = 0;
    for (int p : piles) hours += (p + k - 1L) / k;   // ceil(p/k)
    return hours;
}
```

This "binary search on the answer" pattern is the single highest-value one for mid-level coding rounds.

---

## 7. Interviewer follow-ups to be ready for
- *"Why `lo + (hi - lo) / 2`?"* → integer overflow when `lo + hi > Integer.MAX_VALUE`.
- *"When do you use `<=` vs `<`?"* → inclusive `[lo,hi]` uses `<=`; exclusive boundary template uses `<`.
- *"How do you handle duplicates?"* → lower/upper bound to get the range.
- *"Prove it terminates."* → the range strictly shrinks each iteration.
- *"Recursive vs iterative?"* → iterative preferred (O(1) space, no stack overflow).

---

## 8. My checklist before I say "done" in an interview
- [ ] Empty array / single element
- [ ] Target smaller than all / larger than all
- [ ] Duplicates present
- [ ] `mid` computed overflow-safe
- [ ] Search space shrinks every iteration (no infinite loop)
- [ ] Return value meaning is clear (index? boundary? insertion point?)

---

## My own questions (add as they come up)
<!-- Somesh: jot clarifications/questions here as we drill problems -->