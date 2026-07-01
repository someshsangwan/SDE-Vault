//https://leetcode.com/problems/first-bad-version/description/

/* The isBadVersion API is defined in the parent class VersionControl.
      boolean isBadVersion(int version); */

public class Solution extends VersionControl {
    public int firstBadVersion(int n) {
        //last occurance of target value;
        int lo=0;
        int hi=n;
        int ans=-1;
        while(lo<=hi){
            int mid =lo+(hi-lo)/2;
            if(!isBadVersion(mid)){
                ans =mid;
                lo=mid+1;

            }
            else if(isBadVersion(mid)){
                hi=mid-1;

            }
            else{
                lo=mid+1;
            }


        }
        return ans+1;
    }
}