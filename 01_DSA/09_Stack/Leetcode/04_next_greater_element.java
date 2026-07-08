class Solution {
    public ArrayList<Integer> nextLargerElement(int[] arr) {
        ArrayList<Integer>ans=new ArrayList<>();
        int n=arr.length;
        Deque<Integer>st=new LinkedList<>();
        st.push(-1);
        for(int i=n-1;i>=0;i--){
            int x=arr[i];
            while(st.peek()!=-1 && st.peek()<=x){
                st.pop();
            }
            ans.add(st.peek());
            st.push(x);
        }
        Collections.reverse(ans);
        return ans;

    }
}


//other pattern
// prev smaller -- int i=0;i<n;i++       while(s.top()>=curr);
// prev greater -- int i=0;i<n;i++       while(s.top()<=curr);