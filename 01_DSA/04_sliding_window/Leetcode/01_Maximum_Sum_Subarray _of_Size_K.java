//Max Sum Subarray of size K
//https://www.geeksforgeeks.org/problems/max-sum-subarray-of-size-k5313/1

//Given an array of integers arr[]  and a number k. Return the maximum sum of a subarray of size k.

//brute force :- nested loop (o(n^2))
//optimized :- sliding window .....


class Solution {
    public int maxSubarraySum(int[] arr, int k) {
        int i=0;
        int j=0;
        int n=arr.length;
        int sum=0;
        int res=0;
        while(j<n){
            sum=sum+arr[j];
            if(j-i+1<k){ // window is still small
                j++;

            }
            else{ // window is equal to k
                res=Math.max(res,sum); //process the ressult
                sum=sum-arr[i]; //remove i .
                i++;
                j++;

            }
        }
        return res;

    }
}