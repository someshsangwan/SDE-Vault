//https://leetcode.com/problems/maximum-subarray/description/

//brute force :- nested loop (o(n^2))
//optimized :- Kedane algo o(n) .....if sum becomes negative at any point ...no need to care abt previous one

//Given an integer array nums, find the subarray with the largest sum, and return its sum.
class Solution {
    public int maxSubArray(int[] nums) {
        int n= nums.length;
        int sum=0;
        int maxxsum=Integer.MIN_VALUE;
        for(int i=0;i<n;i++){
            sum=sum+nums[i];
            if(sum>maxxsum){
                maxxsum=sum;
            }
            if(sum<0){
                sum=0;
            }

        }
        return maxxsum;

    }
}