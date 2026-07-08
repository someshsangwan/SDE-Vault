//https://leetcode.com/problems/longest-palindromic-substring/description/

//Given a string s, return the longest palindromic substring in s.
//
//
//
//Example 1:
//
//Input: s = "babad"
//Output: "bab"
//Explanation: "aba" is also a valid answer.

//brute force :- o(n^2)

class Solution {
    public String longestPalindrome(String s) {
        int n = s.length();
        String ans = "";
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                String sub = s.substring(i, j + 1);
                if (isPalindrome(sub) && sub.length() > ans.length()) {
                    ans = sub;
                }
            }
        }
        return ans;
    }

    //optimized
    //DP



}