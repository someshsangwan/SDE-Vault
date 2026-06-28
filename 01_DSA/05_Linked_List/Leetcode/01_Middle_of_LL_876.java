//https://leetcode.com/problems/middle-of-the-linked-list/description/

//sol1 :- traversal the LL , calculate the length of LL and then again traversal LL till half of length and print that node
// sol2 :- slow and fast pointer approach

class Solution {
    public ListNode middleNode(ListNode head) {
        ListNode slow =head;
        ListNode fast=head;
        while(fast!=null && fast.next!=null ){
            slow=slow.next;
            fast=fast.next.next;
        }
        return slow;

    }
}