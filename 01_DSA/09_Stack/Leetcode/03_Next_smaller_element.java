//https://www.geeksforgeeks.org/problems/immediate-smaller-element1142/1

//given an array find next smaller element for every elemt
//Input: arr[] = [4, 8, 5, 2, 25]
//Output: [2, 5, 2, -1, -1]
//brute orce _> nested loop
//optimized -> use stack

class Solution {
    static ArrayList<Integer> nextSmallerEle(int[] arr) {
        // code here
        ArrayList<Integer>ans=new ArrayList<>();
        int n=arr.length;
        Deque<Integer>st=new LinkedList<>();
        st.push(-1);
        for(int i=n-1;i>=0;i--){
            int x=arr[i];
            while(st.peek()>=x){
                st.pop();
            }
            ans.add(st.peek());
            st.push(x);
        }
        Collections.reverse(ans);
        return ans;
    }
}