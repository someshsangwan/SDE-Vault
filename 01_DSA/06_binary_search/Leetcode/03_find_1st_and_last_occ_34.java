//https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/description/



class Solution {
    public int firstoccurance(int[] nums, int target){
        int lo = 0, hi = nums.length - 1;
        int ans = -1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (nums[mid] == target) {
                ans = mid;      // Found it, but keep looking left
                hi = mid - 1;
            } else if (nums[mid] > target) {
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }
        return ans;

    }
    public int lastoccurance(int[] nums, int target){
        int lo=0;
        int hi=nums.length-1;
        int ans=-1;
        while(lo<=hi){
            int mid =lo+(hi-lo)/2;
            if(nums[mid]==target){
                ans =mid;
                lo=mid+1;
            }
            else if(nums[mid]>target){
                hi=mid-1;
            }
            else{
                lo=mid+1;
            }
        }
        return ans;

    }

    public int[] searchRange(int[] nums, int target) {
        int[] ans =new int[2];
        ans[0]=firstoccurance(nums,target);
        ans[1]=lastoccurance(nums,target);
        return ans;

    }
}