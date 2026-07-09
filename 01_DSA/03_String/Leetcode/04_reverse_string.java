
public class Solution {
    public static String reverseString(String str) {
        if (str == null || str.length() <= 1) {
            return str;
        }

        char[] arr = str.toCharArray();
        int left = 0;
        int right = arr.length - 1;

        while (left < right) {
            char temp = arr[left];
            arr[left] = arr[right];
            arr[right] = temp;
            left++;
            right--;
        }

        return new String(arr);
    }
}
