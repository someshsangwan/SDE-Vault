//https://www.geeksforgeeks.org/problems/first-negative-integer-in-every-window-of-size-k3345/1

//Given an array arr[]  and a positive integer k, find the first negative integer for each and every window(contiguous subarray) of size k.
//
//Note: If a window does not contain a negative integer, then return 0 for that window.

class Solution {
    static List<Integer> firstNegInt(int arr[], int k) {
        // code here
        List<Integer>res= new ArrayList<>();
        Queue<Integer> q = new LinkedList<>();
        int j=0;
        int i=0;
        int n=arr.length;
        while(j<n){
            if(arr[j]<0){
                q.add(arr[j]);
            }
            if(j-i+1<k){
                j++;
            }
            else{
                if(q.size()==0){
                    res.add(0);
                }
                else{
                    res.add(q.peek());
                    if(arr[i]==q.peek()){
                        q.poll();
                    }
                }
                i++;
                j++;
            }
        }
        return res;

    }
}