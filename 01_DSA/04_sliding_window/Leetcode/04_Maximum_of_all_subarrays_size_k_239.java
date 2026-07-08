//https://leetcode.com/problems/sliding-window-maximum/description/
//You are given an array of integers nums, there is a sliding window of size k which is moving from the very left of
//the array to the very right. You can only see the k numbers in the window. Each time the sliding window moves right
// by one position
//Input: nums = [1,3,-1,-3,5,3,6,7], k = 3
//Output: [3,3,5,5,6,7]


//sliding window +deque

//brute force :- nested loop ... o(n^2)
//optimized -> sliding window +deque

class Solution {
    public int[] maxSlidingWindow(int[] nums, int k) {

        Deque<Integer> dq = new LinkedList<>();
        List<Integer> res = new ArrayList<>();

        int i = 0, j = 0;

        while (j < nums.length) {

            // Step 1: remove smaller elements from back
            while (!dq.isEmpty() && nums[dq.peekLast()] < nums[j]) {
                dq.pollLast();
            }

            // Step 2: add current index
            dq.offerLast(j);

            // Step 3: window size < k
            if (j - i + 1 < k) {
                j++;
            }
            else {
                // Step 4: window size == k
                res.add(nums[dq.peekFirst()]);

                // Step 5: remove out-of-window element
                if (dq.peekFirst() == i) {
                    dq.pollFirst();
                }

                i++;
                j++;
            }
        }

        // convert list to array
        int[] ans = new int[res.size()];
        for (int x = 0; x < res.size(); x++) {
            ans[x] = res.get(x);
        }

        return ans;
    }
}