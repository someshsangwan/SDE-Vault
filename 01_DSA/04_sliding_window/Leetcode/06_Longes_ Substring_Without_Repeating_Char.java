//https://leetcode.com/problems/longest-substring-without-repeating-characters/description/
//Given a string s, find the length of the longest substring without duplicate characters.
//Example 1:
//
//Input: s = "abcabcbb"
//Output: 3
//Explanation: The answer is "abc", with the length of 3. Note that "bca" and "cab" are also correct answers.


class Solution {
    public int lengthOfLongestSubstring(String s) {

        int i = 0, j = 0;
        int maxLen = 0;

        Set<Character> set = new HashSet<>();

        while (j < s.length()) {

            if (!set.contains(s.charAt(j))) {
                set.add(s.charAt(j));
                maxLen = Math.max(maxLen, j - i + 1);
                j++;
            } else {
                set.remove(s.charAt(i));
                i++;
            }
        }

        return maxLen;
    }
}