//https://leetcode.com/problems/symmetric-tree/description/


class Solution {
    private boolean check(TreeNode p , TreeNode q){
        if(p==null && q==null){
            return true;
        }
        if( (p==null && q!=null) || (p!=null && q==null) ){
            return false;
        }
        boolean left=check(p.left,q.right);
        boolean right=check(p.right,q.left);
        boolean value= (p.val==q.val);
        if(left && right && value) return true;
        else return false;
    }
    public boolean isSymmetric(TreeNode root) {
        if(root==null || (root.left==null && root.right==null)){
            return true;
        }
        return check(root.left,root.right);

    }
}