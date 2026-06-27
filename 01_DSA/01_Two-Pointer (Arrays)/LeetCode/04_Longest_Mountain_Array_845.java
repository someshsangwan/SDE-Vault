//https://leetcode.com/problems/longest-mountain-in-array/description/


class Solution {
    public int longestMountain(int[] arr) {
        int n=arr.length;
        if(n<3){
            return 0;
        }
        int res=0;
        //we start from 1 and finsih at n-1 becuase peek element cant be in start and end
        for(int i=1;i<n-1;i++){
            if(arr[i]>arr[i-1] && arr[i]>arr[i+1]){
                int l=i;
                int r=i;
                while(l>0 && arr[l]>arr[l-1]){
                    l=l-1;
                }
                while(r<n-1 && arr[r]>arr[r+1]){
                    r=r+1;
                }
                res=Math.max(res,r-l+1);
            }
        }
        return res;

    }
}