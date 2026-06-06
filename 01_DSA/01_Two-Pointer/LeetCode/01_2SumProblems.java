class Solution {

    //Given a 1-indexed array of integers numbers that is already sorted in non-decreasing order, find two numbers such that they add up to a specific target number.
    public int[] twoSum(int[] numbers, int target) {
        int i=0;
        int j=numbers.length-1;
        while(i<j){
            if(numbers[i]+numbers[j]==target){
                return new int[]{i+1,j+1};  //1-indexed array;

            }
            if(numbers[i]+numbers[j]<target){
                i++;
            }
            else{
                j--;
            }
        }
        // no pair exist
        return new int[]{-1,-1};
    }
}