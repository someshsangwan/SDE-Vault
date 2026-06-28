//https://leetcode.com/problems/remove-nth-node-from-end-of-list/description/


//sol1 _> calculate whole length ... and then subsrtect that nth value and tranversal again to remove that node

/**
 * Definition for singly-linked list.
 * public class ListNode {
 *     int val;
 *     ListNode next;
 *     ListNode() {}
 *     ListNode(int val) { this.val = val; }
 *     ListNode(int val, ListNode next) { this.val = val; this.next = next; }
 * }
 */
class Solution {
    public ListNode removeNthFromEnd(ListNode head, int n) {
        if(head.next==null && n==1){
            return null;
        }
        if(head==null){
            return head;
        }
        int len=0;
        ListNode curr=head;
        while(curr!=null){
            len++;
            curr=curr.next;
        }
        int start=len-n;

        //edge case if its first node
        if(start==0){
            return head.next;
        }

        curr=head;
        while(start-1>0){
            curr=curr.next;
            start--;
        }
        curr.next=curr.next.next;
        return head;

    }
}