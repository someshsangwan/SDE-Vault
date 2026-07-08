


class Solution {
    public int longestSubarray(int[] arr, int k) {

        int i = 0, j = 0;
        int sum = 0;
        int maxLen = 0;

        while (j < arr.length) {

            sum += arr[j];

            // shrink window if sum > k
            while (sum > k) {
                sum -= arr[i];
                i++;
            }

            // check condition
            if (sum == k) {
                maxLen = Math.max(maxLen, j - i + 1);
            }

            j++;
        }

        return maxLen;
    }

    public int smallestSubarray(int[] arr, int k) {

        int i = 0, j = 0;
        int sum = 0;
        int minLen = Integer.MAX_VALUE;

        while (j < arr.length) {

            sum += arr[j];

            // shrink as much as possible
            while (sum >= k) {
                minLen = Math.min(minLen, j - i + 1);
                sum -= arr[i];
                i++;
            }

            j++;
        }

        return minLen == Integer.MAX_VALUE ? 0 : minLen;
    }
}

