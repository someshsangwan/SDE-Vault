//https://leetcode.com/problems/invert-binary-tree/description/

// this question is not that muc hard it looks hard but i think its easiest question i have seen on tree


class Solution {
    public void invert(TreeNode root){
        if(root==null){
            return;
        }
        TreeNode temp=root.left;
        root.left=root.right;
        root.right=temp;
        invert(root.left);
        invert(root.right);

    }
    public TreeNode invertTree(TreeNode root) {
        if(root==null || (root.left==null && root.right==null)){
            return root;
        }
        invert(root);
        return root;


    }
}