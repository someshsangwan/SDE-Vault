//https://leetcode.com/problems/reorder-list/description/

/**
 * Definition for singly-linked list.
 * public class ListNode {
 * int val;
 * ListNode next;
 * ListNode() {}
 * ListNode(int val) { this.val = val; }
 * ListNode(int val, ListNode next) { this.val = val; this.next = next; }
 * }
 */
class Solution {
    public void reorderList(ListNode head) {
        // Base Case: If the list is empty or has only 1 node, no reordering is needed
        if (head == null || head.next == null) {
            return;
        }

        // Step 1: Find the middle of the linked list 🐢🐇
        ListNode slow = head;
        ListNode fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }

        // Step 2: Reverse the second half of the list in-place 🔃
        ListNode prev = null;
        ListNode curr = slow.next;
        slow.next = null; // Split the list into two separate halves
        ListNode next;

        while (curr != null) {
            next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }

        // Step 3: Interleave the first half and the reversed second half ✂️🤝
        ListNode left = head;
        curr = prev; // Head of the reversed second half

        while (curr != null) {
            ListNode temp1 = left.next; // Save next node of first half
            ListNode temp2 = curr.next; // Save next node of second half

            // Connect left node to right node
            left.next = curr;
            // Connect right node to saved next left node
            curr.next = temp1;

            // Move pointers forward for the next iteration
            left = temp1;
            curr = temp2;
        }
    }
}