//https://leetcode.com/problems/3sum/description/

//sol 1 ::- Brute force (o(N^3))

//sol 2 ::- Sort + Two Pointers  (o(nlogn)+o(n^2))

class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        // Sort to enable two-pointer technique
        Arrays.sort(nums);
        List<List<Integer>> result = new ArrayList<>();

        for (int i = 0; i < nums.length; i++) {
            // Trick 1: Skip duplicate fixed numbers
            if (i > 0 && nums[i] == nums[i - 1]) continue;

            // Early exit: if smallest number > 0, no triplet can sum to 0
            if (nums[i] > 0) break;

            // Two pointers for the remaining array
            int left = i + 1;
            int right = nums.length - 1;

            while (left < right) {
                int sum = nums[i] + nums[left] + nums[right];

                if (sum == 0) {
                    // Found a valid triplet!
                    result.add(Arrays.asList(nums[i], nums[left], nums[right]));
                    left++;
                    right--;

                    // Trick 2 & 3: Skip duplicates after finding a match
                    while (left < right && nums[left] == nums[left - 1]) left++;
                    while (left < right && nums[right] == nums[right + 1]) right--;
                }
                else if (sum < 0) {
                    // Sum too small → need bigger number → move left forward
                    left++;
                }
                else {
                    // Sum too big → need smaller number → move right backward
                    right--;
                }
            }
        }
        return result;
    }
}

