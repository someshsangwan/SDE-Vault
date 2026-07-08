//*
//Given a string you need to print the size of the longest possible substring that has exactly K unique characters.
//If there is no possible substring then print -1
//
//Input:
//S = "aabacbebebe", K = 3
//Output: 7
//Explanation: "cbebebe" is the longest
//substring with K distinct characters.

class Solution {
    public int longestKSubstr(String s, int k) {

        int i = 0, j = 0;
        int maxLen = -1;

        Map<Character, Integer> map = new HashMap<>();

        while (j < s.length()) {

            char ch = s.charAt(j);
            map.put(ch, map.getOrDefault(ch, 0) + 1);

            // Case 1: less than k unique → expand
            if (map.size() < k) {
                j++;
            }

            // Case 2: exactly k unique → update answer
            else if (map.size() == k) {
                maxLen = Math.max(maxLen, j - i + 1);
                j++;
            }

            // Case 3: more than k unique → shrink
            else {
                while (map.size() > k) {
                    char leftChar = s.charAt(i);
                    map.put(leftChar, map.get(leftChar) - 1);

                    if (map.get(leftChar) == 0) {
                        map.remove(leftChar);
                    }
                    i++;
                }
                j++;
            }
        }

        return maxLen;
    }
}