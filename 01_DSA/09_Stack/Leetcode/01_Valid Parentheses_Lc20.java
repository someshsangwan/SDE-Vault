//https://leetcode.com/problems/valid-parentheses/description/

//using stack

class Solution {
    public boolean isValid(String s) {
        Deque<Character>st=new LinkedList<>();
        int n=s.length();
        for(int i=0;i<n;i++){
            char ch=s.charAt(i);
            if(ch=='(' || ch=='{' || ch=='['){
                st.push(ch);
            }
            else{
                if(st.isEmpty()){return false;}
                char top=st.peek();
                if(ch==')' && top!='(') return false;
                if(ch=='}' && top!='{') return false;
                if(ch==']' && top!='[') return false;
                st.pop();
            }
        }
        return true;

    }
}