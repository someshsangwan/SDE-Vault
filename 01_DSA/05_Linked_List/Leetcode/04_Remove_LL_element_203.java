//https://leetcode.com/problems/remove-linked-list-elements/description/

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
    public ListNode removeElements(ListNode head, int val) {
        if(head==null){
            return null;
        }
        if(head.next==null){
            if(head.val==val){
                return null;
            }
            else{
                return head;
            }
        }

        ListNode curr=head;
        while(curr!=null){
            if(curr.next!=null && curr.next.val==val){
                ListNode temp=curr;
                while(temp.next!=null && temp.next.val==val){
                    temp=temp.next;
                }
                curr.next=temp.next;
            }
            else{
                curr=curr.next;
            }
        }
        if(head.val==val){
            return head.next;
        }
        return head;


    }
}