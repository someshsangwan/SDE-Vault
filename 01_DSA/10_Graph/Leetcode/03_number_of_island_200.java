//https://leetcode.com/problems/number-of-islands/description/
class Solution {
    public void mark (int i, int j ,char[][] grid,int m, int n){
        if(i<0 || i>=m || j<0 || j>=n || grid[i][j]!='1'){
            return;
        }
        grid[i][j]='0';
        mark(i+1,j,grid,m,n);
        mark(i-1,j,grid,m,n);
        mark(i,j+1,grid,m,n);
        mark(i,j-1,grid,m,n);
    }
    public int numIslands(char[][] grid) {
        int m=grid.length;
        int n=grid[0].length;
        int res=0;
        //boolean[] visited=new int[m][n];
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    res++;
                    mark(i,j,grid,m,n);
                }
            }
        }
        return res;
    }
}