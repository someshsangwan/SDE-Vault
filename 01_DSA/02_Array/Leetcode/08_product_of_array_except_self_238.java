//https://leetcode.com/problems/product-of-array-except-self/description/


//sol 1:- iterate array two times : i=0 to n then j=0 to n (i==j) skip ..... o(n^2)
//sol 2:- o(n) + o(n) space and o(n) time compkext (you need to clculate pre and suffix prod of array)

//sol 3

//we don't actually need seperate array to store prefix product and suffix products,
//we can do all the approach discussed in method 2 directly onto our final answer array.

class Solution {
    public int[] productExceptSelf(int[] nums) {
        int n = nums.length;
        int ans[] = new int[n];
        Arrays.fill(ans, 1);
        int curr = 1;
        for(int i = 0; i < n; i++) {
            ans[i] *= curr;
            curr *= nums[i];
        }
        curr = 1;
        for(int i = n - 1; i >= 0; i--) {
            ans[i] *= curr;
            curr *= nums[i];
        }
        return ans;
    }
}