# 剑指offer

> 练习剑指offer的题目

## 09.用两个栈实现队列

栈可以用LinkedList代替，最差情况下使用Stack，我这里使用数组代替栈。

```java
class CQueue {
    //入栈
    private List<Integer> input;
    //输出栈
    private List<Integer> output;

    public CQueue() {
        input = new ArrayList<>();
        output = new ArrayList<>();
    }
    public void appendTail(int value) {
        input.add(value);
    } 
    public int deleteHead() {
        //
        int size = output.size();
        if(size <= 0){
            //逆序遍历第一个数组加入第二个
            for(int j = input.size()-1 ; j >=0 ;j--){
                output.add(input.get(j));
            }
            input.clear();
        }       
        return output.size() > 0 ? output.remove(output.size()-1) : -1;
    }
}
```

## 10.斐波那契数列

```java
class Solution {
    public int fib(int n) {
        return getFibByDP(n);
    }
    /**低效递归,可以用备忘录优化 */
    private int getFibByRecursion(int n){
        if(n <= 0){
            return 0;
        }
        if(n <= 2){
            return 1;
        }
        return getFibByRecursion(n-1) + getFibByRecursion(n-2);
    }
    private int getFibByDP(int n){
        if(n <= 2){
            return getFibByRecursion(n);
        }
        int dp1 = 1 , dp2 = 1;
        for(int i = 3 ; i <= n ; i++){
            int tmp = (dp1 + dp2) % 1000000007;
            dp1 = dp2;
            dp2 = tmp;
        }
        return dp2;
    }
}
```

## 03.数组中的重复数字

```java
class Solution {
    /**
    思路1 排序，排序后遍历 logn + n
    思路2 hash，加入集合 n
    思路3 原地hash
     */
    public int findRepeatNumber(int[] nums) {
        //原地hash ，把数组中的数字和数组索引对应的位置一一对应
        for(int i = 0 ; i < nums.length ; i++){
            //
            if(nums[i] == i){
                continue;
            }
            //重复元素
            if(nums[nums[i]] == nums[i]){
                return nums[i];
            }
            //普通交换
            int tmp = nums[nums[i]];
            nums[nums[i]] = nums[i];
            nums[i] = tmp;
        }
        return -1;       
    }
}
```

## 04.二维数组中的查找

```java
class Solution {
    /**
    排序
    思路1 先判断target是否在目标行内，然后二分法查 时间复杂度 n * logn
    思路2 把矩阵转一下 是一个二叉搜索树
     */
    public boolean findNumberIn2DArray(int[][] matrix, int target) {
        if(matrix == null || matrix.length == 0){
            return false;
        }
        //二叉树搜索树（可以从左下或右上开始
        int maxcolumn = matrix[0].length , maxrow = matrix.length , i = 0 ,j = maxcolumn-1;
        while(j >= 0 && i < maxrow){
            if(matrix[i][j] > target){
                j--;
            }else if(matrix[i][j] < target){
                i++;
            }else{
                return true;
            }
        }
        return false;
    }
}
```

10.II 青蛙跳台阶

```java
class Solution {
    public int numWays(int n) {
        //青蛙在任何一个台阶 f(n) = f(n-1) + f(n-2)
        if(n <= 0){
            return 1;
        }
        if(n == 1){
            return 1;
        }
        if(n == 2){
            return 2;
        }
        int dp1 = 1 , dp2 = 2;
        for(int i = 3 ;i <= n ;i++){
            int tmp = (dp1 + dp2)%1000000007;
            dp1 = dp2;
            dp2 = tmp;
        }
        return dp2;
    }
}
```

## 07.重建二叉树



## 12.矩阵中的路径

```java
    public boolean exist(char[][] board, String word) {
        //回溯        
        for(int i = 0 ; i < board.length ;i++){
            for(int j = 0; j < board[0].length ;j++){
                if(findAns(board,word.toCharArray(),0,i,j)){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean findAns(char[][] board , char[] word , int currentIndex , int b1 , int b2){
        if(currentIndex == word.length){
            return true;
        }
        if(b1 < 0 || b1 > board.length-1 || b2 < 0 || b2 > board[0].length-1){
            return false;
        }
        if(board[b1][b2] == '-'){
            return false;
        }

        if(board[b1][b2] == word[currentIndex]){
            char tmp = board[b1][b2];
            board[b1][b2] = '-';
            //上下左右
            boolean res = findAns(board,word,currentIndex+1,b1,b2-1) ||
            findAns(board,word,currentIndex+1,b1,b2+1) ||
            findAns(board,word,currentIndex+1,b1-1,b2) ||
            findAns(board,word,currentIndex+1,b1+1,b2) ;
            board[b1][b2] = tmp;
            return res;
        }
        return false;
    }
```

