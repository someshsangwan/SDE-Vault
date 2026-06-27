//https://leetcode.com/problems/contains-duplicate/

//sol 1-> use nested loop-> time complxity is o(n^2)
//sol 2 -> use set --> time complaxity o(n)
class Solution {
    public boolean containsDuplicate(int[] nums) {
        int n=nums.length;
        Set<Integer>set=new HashSet<>();
        for(int i=0;i<n;i++){
            set.add(nums[i]);
        }
        if(set.size()!=n){
            return true;
        }
        return false;
    }
}