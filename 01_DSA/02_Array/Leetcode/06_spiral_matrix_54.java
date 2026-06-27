//https://leetcode.com/problems/spiral-matrix/description/

// just do what problem is aksing to do
// go right , go down , go left , go up ... then repeat for inner layers
// but make sure you maintain the boundaries (we need to keep track of boundaries like once you finish 1 right then need to increment this boundary )

class Solution {
    public List<Integer> spiralOrder(int[][] matrix) {
        int top = 0, bottom = matrix.length - 1;
        int left = 0, right = matrix[0].length - 1;
        List<Integer> spiral = new ArrayList<>();

        while (top <= bottom && left <= right) {
            for (int i = left; i <= right; i++)
                spiral.add(matrix[top][i]);
            top++;

            for (int j = top; j <= bottom; j++)
                spiral.add(matrix[j][right]);
            right--;

            if (top <= bottom) {
                for (int k = right; k >= left; k--)
                    spiral.add(matrix[bottom][k]);
                bottom--;
            }

            if (left <= right) {
                for (int l = bottom; l >= top; l--)
                    spiral.add(matrix[l][left]);
                left++;
            }
        }

        return spiral;
    }
}