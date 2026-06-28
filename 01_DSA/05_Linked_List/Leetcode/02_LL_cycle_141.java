//https://leetcode.com/problems/linked-list-cycle/description/


// you can make visited array kind of things

//sol 2 :- slow and fast pointer .. if there is cycle then both pointer will meet sometime ...
public class Solution {
    public boolean hasCycle(ListNode head) {
        ListNode slow=head;
        ListNode fast=head;

        while(fast!=null && fast.next!=null){
            slow=slow.next;
            fast=fast.next.next;
            if(slow==fast){
                return true;
            }
        }
        return false;
    }
}