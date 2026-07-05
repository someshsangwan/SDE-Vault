//https://leetcode.com/problems/validate-binary-search-tree/


class Solution {
    private boolean check(TreeNode root , long minval, long maxvalue){
        if(root==null){
            return true;
        }
        if(root.val<=minval || root.val>=maxvalue){
            return false;
        }
        return check(root.left,minval,root.val) && check(root.right,root.val,maxvalue);

    }
    public boolean isValidBST(TreeNode root) {
        return check(root ,Long.MIN_VALUE,Long.MAX_VALUE);

    }
}