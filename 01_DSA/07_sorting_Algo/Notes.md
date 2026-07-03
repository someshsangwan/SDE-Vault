# Sorting Algorithms — DSA Interview Notes

> Source: Confluence page *Somesh_7_Sorting_algorithms*. Covers 6 core algorithms + stability & "which to pick" cheat sheet.

## Quick Reference Table

| Algorithm      | Best        | Average     | Worst       | Space     | Stable? |
|----------------|-------------|-------------|-------------|-----------|---------|
| Bubble Sort    | O(n)        | O(n²)       | O(n²)       | O(1)      | Yes     |
| Selection Sort | O(n²)       | O(n²)       | O(n²)       | O(1)      | No      |
| Insertion Sort | O(n)        | O(n²)       | O(n²)       | O(1)      | Yes     |
| Merge Sort     | O(n log n)  | O(n log n)  | O(n log n)  | O(n)      | Yes     |
| Quick Sort     | O(n log n)  | O(n log n)  | O(n²)       | O(log n)  | No      |
| Heap Sort      | O(n log n)  | O(n log n)  | O(n log n)  | O(1)      | No      |

---

## 1) Bubble Sort

A simple comparison-based algorithm. It repeatedly steps through the list, compares **adjacent elements**, and swaps them if they are in the wrong order. Repeated until the list is sorted.

**Time Complexity**
- Best (already sorted): **O(n)**
- Average: **O(n²)**
- Worst (reverse sorted): **O(n²)**

```java
public class BubbleSort {

    // Main function to perform Bubble Sort
    public static void bubbleSort(int[] array) {
        int n = array.length;

        for (int i = 0; i < n - 1; i++) {
            // Flag to detect if any swap happened this pass
            boolean swapped = false;

            // Last i elements are already in place
            for (int j = 0; j < n - 1 - i; j++) {
                // Swap adjacent elements if they are in the wrong order
                if (array[j] > array[j + 1]) {
                    int temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;
                    swapped = true;
                }
            }

            // If no swaps occurred, the array is already sorted -> O(n) best case
            if (!swapped) {
                break;
            }
        }
    }

    // Helper function to print the array
    public static void printArray(int[] array) {
        for (int value : array) {
            System.out.print(value + " ");
        }
        System.out.println();
    }

    // Main method to test the Bubble Sort implementation
    public static void main(String[] args) {
        int[] array = {5, 1, 4, 2, 8};

        System.out.println("Original Array:");
        printArray(array);

        bubbleSort(array);

        System.out.println("Sorted Array:");
        printArray(array);
    }
}
```

---

## 2) Selection Sort

Divides the array into a **sorted part** and an **unsorted part**. Repeatedly selects the smallest (or largest) element from the unsorted part and swaps it with the first element of the unsorted part.

**Time Complexity**
- Best: **O(n²)**
- Average: **O(n²)**
- Worst: **O(n²)**

> Note: always O(n²) because it scans the whole unsorted portion each pass, regardless of initial order.

```java
public class SelectionSort {

    // Main function to perform Selection Sort
    public static void selectionSort(int[] array) {
        int n = array.length;

        for (int i = 0; i < n - 1; i++) {
            // Assume the current position holds the smallest element
            int minIndex = i;

            // Find the smallest element in the unsorted part
            for (int j = i + 1; j < n; j++) {
                if (array[j] < array[minIndex]) {
                    minIndex = j;
                }
            }

            // Swap the found minimum with the first unsorted element
            int temp = array[minIndex];
            array[minIndex] = array[i];
            array[i] = temp;
        }
    }

    // Helper function to print the array
    public static void printArray(int[] array) {
        for (int value : array) {
            System.out.print(value + " ");
        }
        System.out.println();
    }

    // Main method to test the Selection Sort implementation
    public static void main(String[] args) {
        int[] array = {64, 25, 12, 22, 11};

        System.out.println("Original Array:");
        printArray(array);

        selectionSort(array);

        System.out.println("Sorted Array:");
        printArray(array);
    }
}
```

---

## 3) Insertion Sort

Builds the sorted array one element at a time. Takes each element from the unsorted part and **inserts it into its correct position** in the sorted part.

**Time Complexity**
- Best (already sorted): **O(n)**
- Average: **O(n²)**
- Worst (reverse sorted): **O(n²)**

```java
public class InsertionSort {

    // Main function to perform Insertion Sort
    public static void insertionSort(int[] array) {
        int n = array.length;

        // Start from the second element (first element is trivially "sorted")
        for (int i = 1; i < n; i++) {
            int key = array[i];   // Element to be inserted into the sorted part
            int j = i - 1;

            // Shift elements greater than key one position to the right
            while (j >= 0 && array[j] > key) {
                array[j + 1] = array[j];
                j--;
            }

            // Place the key in its correct position
            array[j + 1] = key;
        }
    }

    // Helper function to print the array
    public static void printArray(int[] array) {
        for (int value : array) {
            System.out.print(value + " ");
        }
        System.out.println();
    }

    // Main method to test the Insertion Sort implementation
    public static void main(String[] args) {
        int[] array = {12, 11, 13, 5, 6};

        System.out.println("Original Array:");
        printArray(array);

        insertionSort(array);

        System.out.println("Sorted Array:");
        printArray(array);
    }
}
```

---

## 4) Merge Sort

A **divide-and-conquer** algorithm that splits the array into smaller subarrays, sorts them, and merges them back together. One of the most efficient sorts at **O(n log n)** in all cases.

**Time Complexity**
- Best: **O(n log n)**
- Average: **O(n log n)**
- Worst: **O(n log n)**

```java
public class MergeSort {

    // Main function to sort an array using Merge Sort
    public static void mergeSort(int[] array, int left, int right) {
        if (left < right) {
            // Find the middle point
            int mid = left + (right - left) / 2;

            // Recursively sort the left half
            mergeSort(array, left, mid);

            // Recursively sort the right half
            mergeSort(array, mid + 1, right);

            // Merge the two halves
            merge(array, left, mid, right);
        }
    }

    // Function to merge two sorted subarrays
    private static void merge(int[] array, int left, int mid, int right) {
        // Sizes of the two subarrays
        int n1 = mid - left + 1;
        int n2 = right - mid;

        // Create temporary arrays
        int[] leftArray= new int[n1];
        int[] rightArray = new int[n2];

        // Copy data to temporary arrays
        for (int i = 0; i < n1; i++) {
            leftArray[i] = array[left + i];
        }
        for (int j = 0; j < n2; j++) {
            rightArray[j] = array[mid + 1 + j];
        }

        // Merge the temporary arrays back into the original array
        int i = 0, j = 0; // Initial indexes of left and right subarrays
        int k = left;     // Initial index of the merged array

        while (i < n1 && j < n2) {
            if (leftArray[i] <= rightArray[j]) {
                array[k] = leftArray[i];
                i++;
            } else {
                array[k] = rightArray[j];
                j++;
            }
            k++;
        }

        // Copy remaining elements of leftArray, if any
        while (i < n1) {
            array[k] = leftArray[i];
            i++;
            k++;
        }

        // Copy remaining elements of rightArray, if any
        while (j < n2) {
            array[k] = rightArray[j];
            j++;
            k++;
        }
    }

    // Helper function to print the array
    public static void printArray(int[] array) {
        for (int value : array) {
            System.out.print(value + " ");
        }
        System.out.println();
    }

    // Main method to test the Merge Sort implementation
    public static void main(String[] args) {
        int[] array = {38, 27, 43, 3, 9, 82, 10};

        System.out.println("Original Array:");
        printArray(array);

        mergeSort(array, 0, array.length - 1);

        System.out.println("Sorted Array:");
        printArray(array);
    }
}
```

---

## 5) Quick Sort

A highly efficient **divide-and-conquer** algorithm. Selects a **pivot** element and partitions the other elements into two subarrays — smaller than the pivot on the left, greater on the right — then recursively sorts the subarrays.

**How Quick Sort Works**
1. **Choose a Pivot** — commonly the last element, first element, or a random element.
2. **Partition** — rearrange so all elements smaller than the pivot are on the left, all greater on the right.
3. **Recursively Sort** — apply the same process to the left and right subarrays.

> Worst case is **O(n²)** (e.g. already-sorted input with a poor pivot choice); average is **O(n log n)**.

```java
public class QuickSort {

    // Main function to perform Quick Sort
    public static void quickSort(int[] array, int low, int high) {
        if (low < high) {
            // Partition the array and get the pivot index
            int pivotIndex = partition(array, low, high);

            // Recursively sort elements before and after the pivot
            quickSort(array, low, pivotIndex - 1);
            quickSort(array, pivotIndex + 1, high);
        }
    }

    // Function to partition the array
    private static int partition(int[] array, int low, int high) {
        int pivot = array[high]; // Choose the last element as the pivot
        int i = low - 1;         // Index of the smaller element

        for (int j = low; j < high; j++) {
            // If the current element is smaller than or equal to the pivot
            if (array[j] <= pivot) {
                i++;
                // Swap array[i] and array[j]
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
        }

        // Swap the pivot element with the element at i+1
        i = i + 1;
        int temp = array[i];
        array[i] = array[high];
        array[high] = temp;

        return i; // Return the pivot index
    }

    // Helper function to print the array
    public static void printArray(int[] array) {
        for (int value : array) {
            System.out.print(value + " ");
        }
        System.out.println();
    }

    // Main method to test the Quick Sort implementation
    public static void main(String[] args) {
        int[] array = {10, 7, 8, 9, 1, 5};

        System.out.println("Original Array:");
        printArray(array);

        quickSort(array, 0, array.length - 1);

        System.out.println("Sorted Array:");
        printArray(array);
    }
}
```

---

## 6) Heap Sort

**Algorithm**
1. Build a **Max-Heap** from the input array (using the `heapify` function).
2. Swap the root (largest element) with the last element of the heap.
3. Reduce the heap size by 1 and heapify the root to restore the heap property.
4. Repeat until the heap size is 1.

```java
public class HeapSort {

    // Main function to perform Heap Sort
    public static void heapSort(int[] array) {
        int n = array.length;

        // Step 1: Build a Max-Heap
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(array, n, i);
        }

        // Step 2: Extract elements from the heap
        for (int i = n - 1; i > 0; i--) {
            // Move current root (largest element) to the end
            int temp = array[0];
            array[0] = array[i];
            array[i] = temp;

            // Restore the heap property for the reduced heap
            heapify(array, i, 0);
        }
    }

    // Function to heapify a subtree rooted at index i
    private static void heapify(int[] array, int n, int i) {
        int largest = i;        // Initialize largest as root
        int left = 2 * i + 1;   // Left child
        int right = 2 * i + 2;  // Right child

        // If left child is larger than root
        if (left < n && array[left] > array[largest]) {
            largest = left;
        }

        // If right child is larger than largest so far
        if (right < n && array[right] > array[largest]) {
            largest = right;
        }

        // If largest is not root
        if (largest != i) {
            // Swap array[i] with array[largest]
            int temp = array[i];
            array[i] = array[largest];
            array[largest] = temp;

            // Recursively heapify the affected subtree
            heapify(array, n, largest);
        }
    }

    // Helper function to print the array
    public static void printArray(int[] array) {
        for (int value : array) {
            System.out.print(value + " ");
        }
        System.out.println();
    }

    // Main method to test the Heap Sort implementation
    public static void main(String[] args) {
        int[] array = {12, 11, 13, 5, 6, 7};

        System.out.println("Original Array:");
        printArray(array);

        heapSort(array);

        System.out.println("Sorted Array:");
        printArray(array);
    }
}
```

---

## Which Sorting Algorithm Is Best?

| Algorithm  | Best       | Average    | Worst      | Space      | Stable? |
|------------|------------|------------|------------|------------|---------|
| Quick Sort | O(n log n) | O(n log n) | O(n²)      | O(n log n) | No      |
| Merge Sort | O(n log n) | O(n log n) | O(n log n) | O(n)       | Yes     |
| Heap Sort  | O(n log n) | O(n log n) | O(n log n)| O(1)       | No      |

**When to use what:**
- **Small dataset** → Insertion, Bubble, Selection are fine (O(n²)).
- **Large dataset** → Quick Sort and Merge Sort (O(n log n)).
- **Memory-constrained environments** → Heap Sort and Quick Sort (O(n log n)) — they need very little extra memory compared to Merge Sort.

---

## What Does "Stable" Mean?

A **stable** sorting algorithm maintains the **relative order of equal elements** in the input array after sorting. If two elements have the same value in the input, their relative order stays the same in the output.

**Example**

Input Array: `[(4, A), (2, B), (4, C), (3, D)]`
(first number = value, letter = label)

Sorted by value (ascending) with a **stable** algorithm:

`[(2, B), (3, D), (4, A), (4, C)]`

Notice the two elements with value `4` — `(4, A)` and `(4, C)` — appear in the **same relative order** as in the input. That's stability.