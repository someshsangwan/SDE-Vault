//https://leetcode.com/problems/find-all-numbers-disappeared-in-an-array/description/

// sol 1-> use set ,put all element in set
// set only accept uniques number
// then go through a loop from 1 to <=n , check if that number is in set or not if not then its missing .. add in answer array
// time complexity will be o(n) and also space o(n)


//sol 2 .. o(1) space and o(n) time complexity

//smart move ....

class Solution {
    public List<Integer> findDisappearedNumbers(int[] nums) {
        for (int i = 0; i < nums.length; i++) {
            int idx = Math.abs(nums[i]) - 1;
            if (nums[idx] > 0) {
                nums[idx] = -nums[idx];
            }
        }

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] > 0) {
                result.add(i + 1);
            }
        }
        return result;
    }
}