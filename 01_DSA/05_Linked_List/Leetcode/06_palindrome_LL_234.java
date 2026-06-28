//https://leetcode.com/problems/palindrome-linked-list/description/


//o(n) space .. put all element in array and then check 2 point if the elemt of array is palindrome .. hahah this is not godd

//find middle of LL
//then reverse the next part of middle element
// now compare the first half with seconf half if they are not same them return false else return true

class Solution {
    public boolean isPalindrome(ListNode head) {
        ListNode slow = head, fast = head, prev, temp;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        prev = slow;
        slow = slow.next;
        prev.next = null;
        while (slow != null) {
            temp = slow.next;
            slow.next = prev;
            prev = slow;
            slow = temp;
        }
        fast = head;
        slow = prev;
        while (slow != null) {
            if (fast.val != slow.val) return false;
            fast = fast.next;
            slow = slow.next;
        }
        return true;
    }
}