
// Function to check if two strings are anagrams
static boolean areAnagram(String s1, String s2) {

    int[] cnt = new int[26];

    int n1 = s1.length();
    int n2 = s2.length();

    for (int i = 0; i < n1; i++)
        cnt[s1.charAt(i) - 'a']++;

    for (int i = 0; i < n2; i++)
        cnt[s2.charAt(i) - 'a']--;

    for (int i = 0; i < 26; i++) {
        if (cnt[i] != 0)
            return false;
    }

    return true;
}