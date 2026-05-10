# Two Pointers Pattern

> **Status:** Learning · **Started:** 2026-05-10
> **Pattern family:** Linear scan / array & string
> **Prerequisite:** comfortable with arrays, basic loops, sorting

---

## 1. TL;DR

Two Pointers is a technique that uses **two indices moving through a data structure** based on some condition, replacing a brute-force O(n²) nested loop with a single O(n) pass — possible whenever the data has **order or structure we can exploit** to safely skip work.

---

## 2. The Core Idea

Imagine you're searching for a pair of numbers in a sorted array that sum to `target`.

Brute force: try every pair → O(n²).

But the array is **sorted**. So if you put one finger on the first element and one on the last:
- If `arr[left] + arr[right] > target` → the right value is too big → move `right` left.
- If `arr[left] + arr[right] < target` → the left value is too small → move `left` right.
- Each move **eliminates an entire row or column** of possibilities, not just one cell.

That's the magic: each pointer movement is *informed* — it discards a whole class of candidates at once.

```
arr = [1, 3, 5, 8, 11, 15],  target = 14
       L                  R
       1 + 15 = 16 > 14  → R--

arr = [1, 3, 5, 8, 11, 15]
       L              R
       1 + 11 = 12 < 14  → L++

arr = [1, 3, 5, 8, 11, 15]
          L           R
          3 + 11 = 14 ✓
```

Both pointers together visit every index at most once → **O(n)**.

---

## 3. Pattern Recognition — When to Reach for Two Pointers

This is the most important section. Memorize these triggers.

### 🚩 Strong signals (almost certainly two pointers)

| Signal in problem | Why two pointers fits |
|---|---|
| Array is **sorted** (or you can sort it) | Lets you make decisions by comparing pointer values |
| Need to **find a pair / triplet / quadruplet** with some sum/condition | Each pointer holds a candidate value |
| Need to **compare from both ends** (palindrome, container, water trapping) | Opposite-ends flavor |
| **In-place** array modification ("modify in O(1) extra space") | Fast/slow flavor — slow tracks write position, fast scans |
| **Remove duplicates** / **rearrange** in a sorted/structured array | Fast/slow flavor |
| **Linked list cycle / middle / nth-from-end** | Fast/slow flavor on linked list |
| **Merge two sorted arrays/lists** | Two-arrays flavor |
| Need to find **longest/shortest substring with a property** | Sliding window (a *variant* of two pointers — see [[sliding-window]]) |

### 🟡 Soft signals (consider two pointers)

- Brute force is O(n²) and the constraints (n ≤ 10⁵) make that too slow → look for a linear pass
- Asked for **O(1) extra space** — points to in-place pointer manipulation
- Problem talks about "pairs", "windows", "ranges", "subarrays"

### 🚫 Anti-signals (don't force two pointers)

- Unsorted data with no exploitable structure → use HashMap instead
- Need access to non-adjacent elements that don't have a monotonic relationship
- Problem inherently requires graph/tree traversal

### 📌 The mental flowchart

```
Is the input array/string/linked list?
   │
   ├─ Yes → Is it sorted (or sortable without losing problem meaning)?
   │         │
   │         ├─ Yes → "Looking for pairs/sum/comparison"? → Opposite-ends two pointers
   │         │        "Removing/rearranging in-place"?    → Fast/slow two pointers
   │         │        "Substring with property"?          → Sliding window variant
   │         │
   │         └─ No  → Linked list?  → Fast/slow (cycle/middle)
   │                  HashMap/sort first?
   │
   └─ No → Probably not two pointers
```

---

## 4. The Three Flavors

### Flavor A — Opposite Ends (Converging Pointers)

Pointers start at the two ends and move toward each other.

**Use when:** sorted array, finding pair/triplet matching a condition, palindrome check.

**Java template:**

```java
public int[] twoPointersOppositeEnds(int[] arr, int target) {
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

**Loop invariant:** the answer (if it exists) lies in the window `[left, right]`. Every move shrinks the window without missing the answer.

---

### Flavor B — Fast/Slow (Same Direction)

Both pointers start at the same end. `fast` scans through the data; `slow` marks a write position or lags behind.

**Use when:** in-place modification, removing duplicates, partitioning, linked-list cycle/middle detection.

**Java template (in-place dedup of sorted array):**

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

**Java template (linked list cycle — Floyd's algorithm):**

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

**Loop invariant:** everything before `slow` is the "valid" prefix (deduped, partitioned, etc.); `fast` is exploring.

---

### Flavor C — Two Arrays (Parallel Scan)

Each pointer indexes a *different* array. They advance independently based on comparisons.

**Use when:** merging sorted arrays, comparing two streams, intersection of sorted lists.

**Java template (merge two sorted arrays into a third):**

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

## 5. Why It Works — The Invariant

The reason two pointers is correct (not just fast) is the **monotonicity invariant**:

> Every pointer move **eliminates at least one candidate from the answer space**, and the discarded candidates are **provably not part of any optimal solution**.

For opposite-ends (Two Sum II): if `arr[L] + arr[R] < target`, we know **no pair `(L, k)` for any `k ≤ R` can reach the target** (because all `arr[k] ≤ arr[R]`). So we can safely discard the entire row at index `L` and advance `L`.

If you can't articulate *why* a pointer move is safe, your two-pointers solution is probably buggy.

---

## 6. Complexity Cheat Sheet

| Flavor | Time | Space |
|---|---|---|
| Opposite-ends on sorted array | O(n) | O(1) |
| Sort-then-two-pointers (e.g. 3Sum) | O(n²) | O(1) extra |
| Fast/slow in-place modification | O(n) | O(1) |
| Floyd's cycle detection | O(n) | O(1) |
| Merge two sorted arrays | O(n+m) | O(n+m) for output |

**Key trade-off:** if you have to sort first to use two pointers, you pay O(n log n) sort cost, which can sometimes lose to an O(n) HashMap approach. Always evaluate both.

---

## 7. Java-Specific Considerations

1. **Use `int` indices, not `Integer`** — avoids autoboxing in tight loops.
2. **For strings, prefer `char[] s.toCharArray()`** if you'll do many `charAt(i)` calls — `charAt` is fine but array access is slightly faster and more idiomatic for pointer manipulation.
3. **Avoid `List.get(i)` on `LinkedList`** — that's O(n). If you got a `List<Integer>`, convert to array first if you'll use indices: `list.stream().mapToInt(Integer::intValue).toArray()`.
4. **For pairs/triplets, return `int[]` or `List<List<Integer>>`** based on the problem signature.
5. **`Arrays.sort(arr)`** is O(n log n) — Dual-Pivot Quicksort for primitives, TimSort for objects. Modifies in place.

---

## 8. Common Pitfalls

| Pitfall | Fix |
|---|---|
| Off-by-one: `while (left <= right)` vs `while (left < right)` | Use `<` when both indices need to be distinct (pair); `<=` when same index can be valid (palindrome center) |
| Forgetting to skip duplicates in 3Sum-style problems | After finding a match, `while (left < right && arr[left] == arr[left+1]) left++;` |
| Modifying both pointers in the same step incorrectly | Move only the pointer that's "wrong" — never both speculatively |
| Sorting when problem requires preserving original indices | Sort an array of `(value, originalIndex)` pairs instead, or use HashMap |
| Integer overflow on `arr[left] + arr[right]` for large ints | Use `long` for the sum: `long sum = (long) arr[left] + arr[right];` |
| Forgetting null/empty checks | `if (arr == null || arr.length < 2) return ...;` at the top |

---

## 9. Linked Patterns

- [[sliding-window]] — a special case of fast/slow where the gap matters more than the indices themselves
- [[binary-search]] — also exploits sortedness; sometimes a problem can be solved either way, and binary search can be faster (O(log n)) when you only need one lookup
- [[fast-slow-linked-list]] — the linked-list-flavored version of two pointers
- [[hashmap-pattern]] — the alternative when you can't sort

---

## 10. Problem Catalog

Fill in as you solve. Backlinks from each `[[LC-...]]` solution note will auto-populate here too.

| # | Problem | Flavor | Difficulty | My time | Notes |
|---|---|---|---|---|---|
| 167 | [[LC-167-Two-Sum-II]] | Opposite-ends | Easy | _ min | _ |
| 26 | [[LC-26-Remove-Duplicates]] | Fast/slow | Easy | _ min | _ |
| 15 | [[LC-15-3Sum]] | Sort + opposite-ends | Medium | _ min | _ |
| 11 | [[LC-11-Container-With-Most-Water]] | Opposite-ends | Medium | _ min | _ |
| 125 | [[LC-125-Valid-Palindrome]] | Opposite-ends | Easy | _ min | _ |
| 283 | [[LC-283-Move-Zeroes]] | Fast/slow | Easy | _ min | _ |
| 88 | [[LC-88-Merge-Sorted-Array]] | Two-arrays (reverse) | Easy | _ min | _ |
| 42 | [[LC-42-Trapping-Rain-Water]] | Opposite-ends | Hard | _ min | _ |
| 16 | [[LC-16-3Sum-Closest]] | Sort + opposite-ends | Medium | _ min | _ |
| 18 | [[LC-18-4Sum]] | Sort + opposite-ends | Medium | _ min | _ |
| 141 | [[LC-141-Linked-List-Cycle]] | Fast/slow (LL) | Easy | _ min | _ |
| 142 | [[LC-142-Linked-List-Cycle-II]] | Fast/slow (LL) | Medium | _ min | _ |
| 876 | [[LC-876-Middle-of-Linked-List]] | Fast/slow (LL) | Easy | _ min | _ |

---

## 11. Self-Check Quiz (revisit before interviews)

1. Why does two pointers achieve O(n) instead of O(n²)? (Answer in terms of pointer-move invariants.)
2. When would HashMap beat two pointers for "find a pair with sum k"? (Hint: think about input properties.)
3. In 3Sum, why do we sort first if the original array order doesn't matter for the answer?
4. In Floyd's cycle detection, why is `2x` the right speed for fast — could we use `3x`?
5. For "Remove Duplicates from Sorted Array", what's the role of the `slow` pointer at the end of the loop?

---

## 12. Revision Schedule

- [ ] First-pass solve (today): LC 167, 26, 15
- [ ] Day +3: re-solve LC 167, 26, 15 from blank file (target: half the time)
- [ ] Day +7: solve LC 11, 283
- [ ] Day +14: solve LC 42 (hard)
- [ ] Day +30: full pattern review — explain pattern in 5 minutes without notes