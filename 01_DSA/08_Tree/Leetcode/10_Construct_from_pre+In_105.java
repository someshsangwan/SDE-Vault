//https://leetcode.com/problems/construct-binary-tree-from-preorder-and-inorder-traversal/


class Solution {
    int preindex=0;
    public int search(int left, int right,int[] inorder,int preindexval){
        for(int i=left;i<=right;i++){
            if(inorder[i]==preindexval){
                return i;
            }
        }
        return -1;
    }
    public TreeNode helper(int[] inorder,int[] preorder, int left,int right){
        if(left>right){
            return null;
        }
        TreeNode root=new TreeNode(preorder[preindex]);
        int index=search(left,right,inorder,preorder[preindex]);
        preindex++;
        root.left=helper(inorder,preorder,left,index-1);
        root.right=helper(inorder,preorder,index+1,right);
        return root;
    }
    public TreeNode buildTree(int[] preorder, int[] inorder) {
        if(preorder.length==1){
            return new TreeNode(preorder[0]);
        }
        return helper(inorder,preorder,0,preorder.length-1);

    }
}