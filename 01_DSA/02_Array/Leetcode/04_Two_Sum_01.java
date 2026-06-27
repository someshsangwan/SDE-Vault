//https://leetcode.com/problems/two-sum/description/
//sol 1 nested loop O(n2)
//sol 2 )  sort array and use two pointer (o(nlogn)) time com and o(1) space
// sol 3) use map -> time and space both o(n);

class Solution {
    public int[] twoSum(int[] nums, int target) {
        int ans[]=new int[2];
        Map<Integer,Integer>map=new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int rem=target-nums[i];
            if(map.containsKey(rem)){
                ans[0]=map.get(rem);
                ans[1]=i;
                return ans;
            }
            else{
                map.put(nums[i],i);
            }
        }
        return ans;
    }
}
