//https://leetcode.com/problems/kth-smallest-element-in-a-bst/description/

//sol 1:- in BST , we do inorder traversal then we will get sorted array so return kth index ...

//sol 2 :- if you dont want to use space o(n) .. you can manager some count interger and  decrase 1 count in every call you will return if ct=0


class Solution {
    int ans=0;
    int ct=0;
    private void check(TreeNode root, int k){
        if(root==null){
            return;
        }
        check(root.left,k);
        ct++;
        if(ct==k){
            ans=root.val;
            return;
        }
        check(root.right,k);

    }
    public int kthSmallest(TreeNode root, int k) {
        check(root,k);
        return ans;
    }
}