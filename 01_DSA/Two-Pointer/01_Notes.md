# Two Pointers Pattern

> **Pattern family:** Linear scan / array & string · **Prerequisites:** arrays, loops, sorting
> **Sources synthesized:** GeeksForGeeks (correctness proof, problem types), Coding Nexus / Medium (the "lockers" analogy), Dev.to (variant naming, comparison with sliding window), plus Java/JVM context for Rakuten Pay relevance.

---

## 1. TL;DR

Two Pointers is a technique that uses **two indices moving through a data structure based on a condition**, replacing a brute-force O(n²) nested loop with a single O(n) pass. It's possible whenever the data has **order or structure we can exploit** to safely skip work without re-examining it.

---

## 2. The Core Idea

### 2.1 The "Lockers" Analogy

> Imagine you and a friend stand at opposite ends of a long row of lockers. Each locker has a number. You're looking for **two lockers whose numbers sum to a target**. Instead of opening every pair (O(n²)), you each step toward the middle, *adjusting based on what you see*: if your combined number is too big, the friend on the high end takes a step back; if too small, you on the low end step forward. You meet in the middle having checked at most n lockers — O(n).

That's the entire pattern. The data has structure (the lockers are sorted), so each step is **informed** — you discard a whole class of candidates with one move, not just one cell.

### 2.2 Worked Example — Pair sum in a sorted array

```
arr = [1, 3, 5, 8, 11, 15],   target = 14
       L                    R
       1 + 15 = 16  > 14    →  R--

arr = [1, 3, 5, 8, 11, 15]
       L                R
       1 + 11 = 12  < 14    →  L++

arr = [1, 3, 5, 8, 11, 15]
          L             R
          3 + 11 = 14   ✓
```

Each pointer visits each index at most once → **O(n) total work**, vs O(n²) brute force.

### 2.3 The Cost of Brute Force

```java
// Brute force — every pair, O(n²)
for (int i = 0; i < n; i++) {
    for (int j = i + 1; j < n; j++) {
        if (arr[i] + arr[j] == target) return new int[]{i, j};
    }
}
```

Two pointers replaces the **inner loop** with a directional decision based on the comparison. That decision is only safe when the data is *ordered* — which is why two pointers and "is the array sorted?" go hand in hand.

---

## 3. Pattern Recognition — When to Reach for Two Pointers

This is the most important section. **Most candidates lose two-pointer questions not because they can't write the code, but because they don't *recognize* when to apply it.** Internalize these triggers.

### 3.1 🚩 Strong signals (almost certainly two pointers)

| Signal in problem | Why two pointers fits |
|---|---|
| Array is **sorted** (or can be sorted without losing problem meaning) | Lets you make safe directional decisions by comparing pointer values |
| Need to **find a pair / triplet / quadruplet** with a given sum or condition | Each pointer holds a candidate value; multi-pointer extends naturally |
| Need to **compare from both ends** (palindrome, container, water trapping) | Convergent flavor |
| **In-place** array modification ("modify in O(1) extra space") | Chasing/fast-slow flavor — slow tracks write position, fast scans |
| **Remove duplicates** / **rearrange** in a sorted/structured array | Chasing flavor |
| **Linked list cycle / middle / nth-from-end** | Floyd's slow-fast on linked list |
| **Merge two sorted arrays/lists** | Parallel two-arrays flavor |
| **Substring with a property** (longest/shortest with k distinct chars, etc.) | Sliding window — a *variant* of two pointers; see [[#7-two-pointers-vs-sliding-window]] |

### 3.2 🟡 Soft signals (consider two pointers)

- Brute force is O(n²) and constraints (n ≤ 10⁵ or higher) make that too slow → linear pass needed.
- Asked for **O(1) extra space** → in-place pointer manipulation.
- Problem talks about "pairs," "windows," "ranges," "subarrays."
- The data structure has a **monotonic property** you can exploit (sorted, all-positive prefix sums, etc.).

### 3.3 🚫 Anti-signals (don't force two pointers)

- Unsorted data with no exploitable structure → use HashMap instead (e.g., classic Two Sum on unsorted array — `HashMap<value, index>` is O(n); two pointers requires O(n log n) sort).
- Need access to non-adjacent elements without a monotonic relationship.
- Problem inherently requires graph/tree traversal.
- You'd lose the original index information by sorting (and that information matters for the answer).

### 3.4 📌 The mental flowchart

```
Is the input array / string / linked list?
   │
   ├─ Yes ─→ Sorted (or sortable without losing meaning)?
   │          │
   │          ├─ Yes ─→ Looking for pairs / triplets / sum / comparison?  → CONVERGENT
   │          │         Removing / rearranging in-place?                  → CHASING (fast/slow)
   │          │         Substring with a property?                        → SLIDING WINDOW
   │          │         Merging two sorted streams?                        → PARALLEL
   │          │
   │          └─ No  ─→ Linked list?  → CHASING (Floyd's cycle / middle)
   │                    Else? Use HashMap / sort first / different pattern
   │
   └─ No  ─→ Probably not two pointers
```

---

## 4. The Three Variants

Different sources name these differently. I use these names because they describe pointer **motion**:

- **Convergent** — pointers start far apart, move toward each other (a.k.a. "opposite ends")
- **Chasing** — pointers start at the same end, one moves faster (a.k.a. "fast/slow," "same direction")
- **Parallel** — one pointer per array, advancing independently

### 4.1 Variant A — Convergent (Opposite Ends)

Pointers start at the two ends and move toward each other.

**Use when:** sorted array; finding pair/triplet matching a condition; palindrome check; container/area maximization.

**Java template:**

```java
public int[] twoPointersConvergent(int[] arr, int target) {
    int left = 0, right = arr.length - 1;

    while (left < right) {
        int sum = arr[left] + arr[right];
        if (sum == target) {
            return new int[]{left, right};
        } else if (sum < target) {
            left++;   // need a larger value
        } else {
            right--;  // need a smaller value
        }
    }
    return new int[]{-1, -1};
}
```

**Loop invariant:** the answer (if it exists) lies in the open window `(left, right)`. Every move shrinks the window without missing the answer.

**Classic problems:** LC 167 (Two Sum II), LC 125 (Valid Palindrome), LC 11 (Container With Most Water), LC 42 (Trapping Rain Water).

---

### 4.2 Variant B — Chasing (Same Direction / Fast-Slow)

Both pointers start at the same end. `fast` scans; `slow` lags behind, marking a write position or a slower traversal.

**Use when:** in-place modification, removing duplicates, partitioning, linked-list cycle detection, finding the middle of a linked list.

**Java template — in-place dedup of sorted array:**

```java
public int removeDuplicates(int[] nums) {
    if (nums.length == 0) return 0;
    int slow = 0;  // last position written
    for (int fast = 1; fast < nums.length; fast++) {
        if (nums[fast] != nums[slow]) {
            slow++;
            nums[slow] = nums[fast];  // overwrite
        }
    }
    return slow + 1;  // length of deduped portion
}
```

**Java template — linked list cycle (Floyd's algorithm):**

```java
public boolean hasCycle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;            // 1 step
        fast = fast.next.next;       // 2 steps
        if (slow == fast) return true;
    }
    return false;
}
```

**Java template — middle of linked list:**

```java
public ListNode middleNode(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    return slow;  // when fast reaches end, slow is at middle
}
```

**Loop invariant:** everything before `slow` is the "valid" prefix (deduped, partitioned, traversed); `fast` is exploring ahead.

---

### 4.3 Variant C — Parallel (Two Arrays)

Each pointer indexes a *different* array. They advance independently based on comparisons.

**Use when:** merging sorted arrays, comparing two streams, intersection of sorted lists, "is one array a subsequence of another?"

**Java template — merge two sorted arrays:**

```java
public int[] merge(int[] a, int[] b) {
    int[] out = new int[a.length + b.length];
    int i = 0, j = 0, k = 0;
    while (i < a.length && j < b.length) {
        if (a[i] <= b[j]) {
            out[k++] = a[i++];
        } else {
            out[k++] = b[j++];
        }
    }
    while (i < a.length) out[k++] = a[i++];
    while (j < b.length) out[k++] = b[j++];
    return out;
}
```

**Loop invariant:** `out[0..k-1]` is sorted and contains the smallest `k` elements seen so far across both inputs.

---

## 5. Multi-Pointer Extensions (3 and 4 pointers)

Convergent two pointers extend cleanly to **k-Sum** problems. The trick: fix one pointer with an outer loop, then run a normal convergent two-pointer on the remaining range.

**3Sum template:**

```java
public List<List<Integer>> threeSum(int[] nums) {
    Arrays.sort(nums);
    List<List<Integer>> result = new ArrayList<>();
    for (int i = 0; i < nums.length - 2; i++) {
        if (i > 0 && nums[i] == nums[i - 1]) continue;  // skip duplicates for i

        int left = i + 1, right = nums.length - 1;
        int target = -nums[i];

        while (left < right) {
            int sum = nums[left] + nums[right];
            if (sum == target) {
                result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                while (left < right && nums[left] == nums[left + 1]) left++;
                while (left < right && nums[right] == nums[right - 1]) right--;
                left++; right--;
            } else if (sum < target) {
                left++;
            } else {
                right--;
            }
        }
    }
    return result;
}
```

**4Sum:** wrap one more outer loop. Total cost: O(n³) for 3Sum, O(n³) for 4Sum (the sort is the cheap part).

**Why this works:** sorting + the two-pointer invariant lets us skip entire ranges of impossible pairs, turning an O(n^k) brute force into O(n^(k-1)).

---

## 6. Why It Works — Correctness Proof

The reason two pointers is *correct* (not just fast) is the **monotonicity invariant**:

> Every pointer move **eliminates at least one candidate from the answer space**, and the discarded candidates are **provably not part of any optimal solution.**

### 6.1 Convergent — case-by-case proof for "pair sum = target" on sorted array

Claim: at each step, the unchecked search space `(left, right)` still contains the answer (if it exists).

**Case 1 — `arr[left] + arr[right] < target` → we increment `left`.**
We must show no pair `(left, k)` for `k ≤ right` could have summed to `target`.
Since the array is sorted, `arr[k] ≤ arr[right]` for all `k ≤ right`. So `arr[left] + arr[k] ≤ arr[left] + arr[right] < target`. None of those pairs reach the target. Discarding the entire row at index `left` is safe.

**Case 2 — `arr[left] + arr[right] > target` → we decrement `right`.**
Symmetric. For any `k ≥ left`, `arr[k] ≥ arr[left]`, so `arr[k] + arr[right] ≥ arr[left] + arr[right] > target`. Safe to discard the entire column at index `right`.

**Case 3 — `arr[left] + arr[right] == target`.**
Found it. Return.

Each iteration either returns or shrinks the window by 1. Window starts at size n − 1, so at most n − 1 iterations. **O(n).**

### 6.2 Chasing — invariant for in-place dedup

Invariant: after iteration, `nums[0..slow]` contains every distinct value seen in `nums[0..fast]`, in original order, with no duplicates.

**Proof:** initially `slow = 0`, `fast = 1`. Invariant holds vacuously.
- If `nums[fast] == nums[slow]`: duplicate, do nothing. Invariant preserved.
- If `nums[fast] != nums[slow]`: new distinct value. Increment `slow`, write `nums[fast]` to `nums[slow]`. Invariant preserved.

When `fast` reaches the end, `nums[0..slow]` is exactly the deduped prefix.

### 6.3 Parallel — invariant for merge

Invariant: after each iteration, `out[0..k-1]` is sorted and contains exactly the `k` smallest elements among `a[0..i-1] ∪ b[0..j-1]`.

**Proof:** at each step we pick the smaller of `a[i]` and `b[j]` (both candidates for being the next-smallest unconsumed element). Sorting in both inputs guarantees no smaller element exists earlier in either array (we've already consumed them).

> **If you can't articulate why a pointer move is safe, your solution is probably buggy.** Walk through the invariant before submitting.

---

## 7. Two Pointers vs Sliding Window

These are siblings and often confused. **Sliding window is a *specialization* of two pointers**, where both pointers move in the same direction and the *gap between them* (the window) is what carries meaning.

| | Two Pointers | Sliding Window |
|---|---|---|
| **Pointer motion** | Various (convergent, chasing, parallel) | Both same direction, `right` always advances first |
| **What you track** | The *values* at the pointers | The state of the *window* between them |
| **Typical question** | "Find a pair / triplet meeting condition" | "Longest / shortest contiguous subarray with property X" |
| **Data requirement** | Often sorted | Order matters but no sorting needed |
| **State updates** | Local (just compare values) | Maintain rolling state (sum, count, hashmap of window) |

**Heuristic:**
> If the question is about a **single pair or triplet of elements** → Two Pointers.
> If the question is about a **contiguous subarray/substring with a property** → Sliding Window.

Sliding window has its own dedicated note: [[sliding-window]] (separate topic, not covered here).

---

## 8. Complexity Cheat Sheet

| Variant / scenario | Time | Space |
|---|---|---|
| Convergent on already-sorted array | O(n) | O(1) |
| Sort + convergent (e.g., 3Sum on unsorted input) | O(n²) (3Sum) | O(1) extra (in-place sort) |
| Chasing in-place modification | O(n) | O(1) |
| Floyd's cycle detection | O(n) | O(1) |
| Merge two sorted arrays | O(n + m) | O(n + m) for the output |
| 4Sum (sort + 2 outer loops + convergent) | O(n³) | O(1) extra |

**Key trade-off:** if you must sort first to use two pointers, you pay O(n log n). For "find a pair with sum k" on unsorted input, a HashMap is O(n) and beats sorting + two pointers asymptotically. **Always evaluate both before committing.**

---

## 9. Java-Specific Considerations

1. **Use `int` indices, not `Integer`** — avoids autoboxing overhead in tight loops.
2. **For strings, prefer `char[] s.toCharArray()`** if you'll do many `charAt(i)` calls — array access is slightly faster and reads more naturally for pointer code.
3. **Avoid `List.get(i)` on `LinkedList`** — that's O(n) per call. If you got a `List<Integer>`, convert to array first: `list.stream().mapToInt(Integer::intValue).toArray()`.
4. **For pairs/triplets, return `int[]` or `List<List<Integer>>`** based on the problem signature.
5. **`Arrays.sort(int[])` is O(n log n)** — Dual-Pivot Quicksort for primitives, TimSort for objects. Modifies in place.
6. **Integer overflow on sums:** for problems with large values (`Integer.MAX_VALUE` adjacent), use `long`:
   ```java
   long sum = (long) arr[left] + arr[right];
   ```
7. **Watch immutability of `String`:** can't modify in place. Use `char[]` for character-level pointer work, then `new String(chars)` at the end.

---

## 10. Common Pitfalls

| Pitfall | Fix |
|---|---|
| Off-by-one: `while (left <= right)` vs `while (left < right)` | Use `<` when both indices must be **distinct** (pair); `<=` when same index can be valid (palindrome center, search) |
| Forgetting to skip duplicates in 3Sum-style problems | After finding a match, `while (left < right && arr[left] == arr[left+1]) left++;` |
| Moving both pointers blindly when only one is "wrong" | Move only the pointer whose value caused the mismatch |
| Sorting when the problem requires preserving original indices | Sort a list of `(value, originalIndex)` pairs, or use HashMap instead |
| Integer overflow on large `int` sums | Use `long` for the addition |
| Missing null/empty checks | `if (arr == null || arr.length < 2) return ...;` at the top |
| Using two pointers on **unsorted** array without sorting first | The algorithm silently returns wrong answers — always check sortedness assumption |
| Forgetting that sorting destroys original order | If indices in the answer must be original-array indices, sort `(value, index)` pairs |

---

## 11. Linked Patterns

- [[sliding-window]] — both pointers same direction, the *window* carries meaning. Sibling pattern; many problems blur the line.
- [[binary-search]] — also exploits sortedness; sometimes solves the same problem in O(log n) instead of O(n).
- [[fast-slow-linked-list]] — the linked-list flavor of chasing two pointers.
- [[hashmap-pattern]] — the alternative when data is unsorted and sorting would cost too much.
- [[sorting]] — frequent prerequisite; understand stability & in-place vs not.

---

## 12. Problem Catalog

Fill in as you solve. Backlinks from each `[[LC-...]]` solution note auto-populate here too.

| # | Problem | Variant | Difficulty | My time | Notes |
|---|---|---|---|---|---|
| 167 | [[LC-167-Two-Sum-II]] | Convergent | Easy | _ min | _ |
| 26 | [[LC-26-Remove-Duplicates]] | Chasing | Easy | _ min | _ |
| 27 | [[LC-27-Remove-Element]] | Chasing | Easy | _ min | _ |
| 283 | [[LC-283-Move-Zeroes]] | Chasing | Easy | _ min | _ |
| 88 | [[LC-88-Merge-Sorted-Array]] | Parallel (reverse) | Easy | _ min | _ |
| 125 | [[LC-125-Valid-Palindrome]] | Convergent | Easy | _ min | _ |
| 344 | [[LC-344-Reverse-String]] | Convergent | Easy | _ min | _ |
| 11 | [[LC-11-Container-With-Most-Water]] | Convergent | Medium | _ min | _ |
| 15 | [[LC-15-3Sum]] | Sort + convergent | Medium | _ min | _ |
| 16 | [[LC-16-3Sum-Closest]] | Sort + convergent | Medium | _ min | _ |
| 18 | [[LC-18-4Sum]] | Sort + convergent | Medium | _ min | _ |
| 75 | [[LC-75-Sort-Colors]] | Chasing (Dutch flag) | Medium | _ min | _ |
| 142 | [[LC-142-Linked-List-Cycle-II]] | Chasing (LL Floyd) | Medium | _ min | _ |
| 141 | [[LC-141-Linked-List-Cycle]] | Chasing (LL) | Easy | _ min | _ |
| 876 | [[LC-876-Middle-of-Linked-List]] | Chasing (LL) | Easy | _ min | _ |
| 19 | [[LC-19-Remove-Nth-Node-From-End]] | Chasing (LL, gap) | Medium | _ min | _ |
| 42 | [[LC-42-Trapping-Rain-Water]] | Convergent | Hard | _ min | _ |

---

## 13. Self-Check Quiz

Answer in your own words. If you can't, re-read the relevant section.

1. Why does two pointers achieve O(n) instead of O(n²)? Answer in terms of pointer-move invariants and what's eliminated each step.
2. When would HashMap **beat** two pointers for "find a pair with sum = k"? Be specific about input properties.
3. In 3Sum, why do we sort first if the answer doesn't depend on original array order?
4. In Floyd's cycle detection, why is `2x` the right speed for fast — could we use `3x`? What changes if we do?
5. For "Remove Duplicates from Sorted Array," what does the `slow` pointer mean at the end of the loop?
6. State the loop invariant for the convergent two-pointer "pair sum = target" algorithm.
7. Give one situation where you would *not* use two pointers even though the array is sorted.
8. Two pointers vs sliding window — give one problem each that's clearly one or the other.

---

## 14. Revision Tips

- **First revision:** after solving 2–3 problems, re-read sections 3 (Pattern Recognition) and 6 (Why It Works) — those are the parts that don't stick from coding alone.
- **Second revision:** explain the three variants out loud in 5 minutes, no notes. If you get stuck, the gap reveals what to revisit.
- **Third revision (pre-interview):** review the **Pitfalls** table and re-skim the **Problem Catalog** — recognize each problem in 10 seconds and recall its variant.
- **Long-term:** any time you encounter a new problem that fits two pointers, add it to the Problem Catalog. The catalog *is* your moat.