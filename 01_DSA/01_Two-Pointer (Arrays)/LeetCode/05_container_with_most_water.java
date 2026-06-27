//https://leetcode.com/problems/container-with-most-water/description/


//sol 1:- brute force o(n^2) :- nested loop and check eevry position

//sol 2:- 2 pointer (o(n))

class Solution {
    public int maxArea(int[] height) {
        int i=0;
        int j=height.length-1;
        int res=0;
        while(i<j){
            int small=Math.min(height[i],height[j]);
            res=Math.max(res,(j-i)*small);
            if(height[i]<height[j]){
                i++;
            }
            else{
                j--;
            }
        }
        return res;

    }
}