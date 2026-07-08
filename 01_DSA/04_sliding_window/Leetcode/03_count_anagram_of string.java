//https://www.geeksforgeeks.org/problems/count-occurences-of-anagrams5839/1
//Given a word pat and a text txt. Return the count of the occurrences of anagrams of the word in the text.

class Solution {



    int search(String pat, String txt) {
        int res = 0;
        Map<Character, Integer> map = new HashMap<>();
        // Step 1: build frequency map
        for (char c : pat.toCharArray()) {
            map.put(c, map.getOrDefault(c, 0) + 1);
        }

        int count = map.size(); // number of unique chars to match
        int k = pat.length();

        int i = 0, j = 0;

        while (j < txt.length()) {

            // Step 2: acquire (expand window)
            char ch = txt.charAt(j);
            if (map.containsKey(ch)) {
                map.put(ch, map.get(ch) - 1);

                if (map.get(ch) == 0) {
                    count--;
                }
            }

            // Step 3: window size < k
            if (j - i + 1 < k) {
                j++;
            }
            else {
                // Step 4: window size == k
                if (count == 0) {
                    res++;
                }

                // Step 5: release (shrink window)
                char leftChar = txt.charAt(i);

                if (map.containsKey(leftChar)) {
                    if (map.get(leftChar) == 0) {
                        count++;
                    }
                    map.put(leftChar, map.get(leftChar) + 1);
                }

                i++;
                j++;
            }
        }

        return res;
    }
}