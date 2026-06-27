//https://leetcode.com/problems/best-time-to-buy-and-sell-stock/description/

class Solution {
    public int maxProfit(int[] prices) {
        int n=prices.length;
        int curr=prices[0];
        int ans=0;
        for(int i=1;i<n;i++){
            ans=Math.max(ans,prices[i]-curr);
            if(prices[i]<curr){
                curr=prices[i];
            }
        }
        return ans;
    }
}