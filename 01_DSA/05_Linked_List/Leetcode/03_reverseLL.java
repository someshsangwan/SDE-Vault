//https://leetcode.com/problems/reverse-linked-list/description/

class Solution {
    public ListNode reverseList(ListNode head) {

        ListNode curr=head;
        ListNode prev=null;
        ListNode ford;
        while(curr!=null){
            ford=curr.next;
            curr.next=prev;
            prev=curr;
            curr=ford;

        }
        return prev;

    }
}