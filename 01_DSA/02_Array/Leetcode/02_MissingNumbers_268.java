//https://leetcode.com/problems/missing-number/

// sol 1->sort array and if index value is not equal index -> means missing -> return index value -1 -> Time-> o(nlogn)

//sol 2 -> use math -> sum of first n natural number (n*(n+1)/2);

class Solution {
    public int missingNumber(int[] nums) {
        int sum =0;
        int n=nums.length;
        for(int i=0;i<n;i++){
            sum+=nums[i];
        }
        // this sum is from array elements ->
        //now lets calculate from formula
        int expected=(n*(n+1))/2;
        return expected-sum;
    }
}



