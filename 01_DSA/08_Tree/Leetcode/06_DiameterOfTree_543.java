//https://leetcode.com/problems/diameter-of-binary-tree/description/

class Solution {
    static int ans=0;
    public int height(TreeNode root){
        if(root==null){
            return 0;
        }
        int left=height(root.left);
        int right=height(root.right);
        if(left+right>ans){
            ans=left+right;
        }

        return 1+Math.max(left,right);

    }
    public int diameterOfBinaryTree(TreeNode root) {
        if(root==null){
            return 0;
        }
        ans=0;
        height(root);
        return ans;

    }
}