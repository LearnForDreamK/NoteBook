# SQL语句练习

平常CURD居多，复杂SQL不一定写得出来，再学习强化一下。

## 175.组合两个表

```mysql
# Write your MySQL query statement below
SELECT FirstName,LastName,City,State FROM
Person AS a LEFT JOIN Address AS b
ON a.PersonId = b.PersonId
```

## 176.第二高的薪水

```mysql
#想法是先排序筛选出前2大的 然后再从前2大的取出最小的
SELECT MIN(SecondHighestSalary) AS SecondHighestSalary FROM 
(SELECT Salary AS SecondHighestSalary FROM Employee ORDER BY Salary DESC LIMIT 2) as c
#上面的SQL在表条目小于2时会返回不必要的结果，利用limit的性质
SELECT Salary AS SecondHighestSalary FROM Employee ORDER BY Salary DESC LIMIT 1,1
#需要无值的时候返回Null，DISTINCT是为了记录不同薪资
SELECT (SELECT DISTINCT Salary FROM Employee ORDER BY Salary DESC LIMIT 1,1) SecondHighestSalary
#其实还可以用IFNULL(查询子句,NULL) 来解决空值问题
```

## 177.第N高的薪水

```mysql
CREATE FUNCTION getNthHighestSalary(N INT) RETURNS INT
BEGIN
  #limit处N只能为正整数 所以在这里-1
  SET N=N-1;
  RETURN (
      # Write your MySQL query statement below.
      SELECT IFNULL(
          (SELECT DISTINCT Salary FROM Employee ORDER BY Salary DESC LIMIT N,1) 
      ,NULL)
  );
END
```

## 178.分数排名

```mysql
#按分数排序查询出所有的分数,排名即是大于当前分的数量
SELECT Score,(
    SELECT count(DISTINCT Score) FROM Scores where Score >= S.Score
) AS 'Rank' FROM Scores AS S ORDER BY Score DESC; 

#dense_rank()和rank()区别是前者出现相同的元素会按顺序排名 1 1 2 后者会 1 1 3
select Score, dense_rank() over(Order By Score desc) 'Rank' FROM Scores;
```

## 180.连续出现的数字

```mysql
#刚开始考虑错了，按Id分组 查询每组超过三个元素的组，发现是要连续的！下面错误的
SELECT COUNT(*) as c FROM Logs GROUP BY Num HAVING c >= 3

#联合三张表,每张表找到一个Num相同但id不相同的行
SELECT DISTINCT a.Num AS ConsecutiveNums FROM Logs a ,Logs b ,Logs c WHERE a.Num = b.Num AND 
b.Num = c.Num AND a.Id+1 = b.Id AND b.Id+1 = c.Id

#妙妙屋解法 依靠 (列1,列2) in (SELECT * FROM TB)解决
SELECT  DISTINCT Num AS ConsecutiveNums FROM Logs WHERE (Id+1,Num) in (SELECT * FROM Logs) AND (Id+2,Num) in (SELECT * FROM Logs)

```

## 181.收入超过经理的员工

```mysql
#最容易想的笨比解法
SELECT Name AS Employee From Employee e WHERE Salary > (SELECT Salary FROM Employee WHERE Id = e.ManagerId)
#连接查询
#连接查询
SELECT e1.Name AS Employee FROM Employee e1 INNER JOIN Employee e2 ON e1.ManagerId = e2.Id AND e1.Salary > e2.Salary
```

