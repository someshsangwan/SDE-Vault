//https://leetcode.com/problems/how-many-numbers-are-smaller-than-the-current-number/description/

//nested loop -> o(n2)


//optimized sol -> sort array so you easily find smaller number than current and add it in Hashmap if its not there
// o(nlogn) and space o(n)

class Solution {
    public int[] smallerNumbersThanCurrent(int[] nums) {
        int n=nums.length;
        int ans[]= new int[n];
        Map<Integer,Integer>map =new HashMap<>();
        int temp[]= new int[n];
        for(int i=0;i<n;i++){
            temp[i]=nums[i];
        }
        Arrays.sort(temp);
        for(int i=0;i<n;i++){
            if(!map.containsKey(temp[i])){
                int num=temp[i];
                int smaller=0;
                if(i>0){
                    smaller=i;
                }
                map.put(num,smaller);
            }
        }
        for(int i=0;i<n;i++){
            ans[i]=map.get(nums[i]);
        }
        return ans;

    }
}