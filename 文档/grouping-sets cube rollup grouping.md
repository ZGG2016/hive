# grouping-sets cube rollup grouping

[TOC]

## 准备数据

```sql
drop table if exists cookie;
create table cookie(month string, day string, cookieid string) 
row format delimited fields terminated by ',';

load data local inpath "/export/datas/cookie.txt" into table cookie;
hive (default)> select * from cookie;
OK
cookie.month	cookie.day	cookie.cookieid
2015-03	2015-03-10	cookie1
2015-03	2015-03-10	cookie5
2015-03	2015-03-12	cookie7
2015-04	2015-04-12	cookie3
2015-04	2015-04-13	cookie2
2015-04	2015-04-13	cookie4
2015-04	2015-04-16	cookie4
2015-03	2015-03-10	cookie2
2015-03	2015-03-10	cookie3
2015-04	2015-04-12	cookie5
2015-04	2015-04-13	cookie6
2015-04	2015-04-15	cookie3
2015-04	2015-04-15	cookie2
2015-04	2015-04-16	cookie1
Time taken: 0.094 seconds, Fetched: 14 row(s)
```

## grouping-sets

[点这里](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/%E8%99%9A%E6%8B%9F%E5%88%97.md#grouping__id) 查看

## cube

只能与 GROUP BY 一起使用。

一旦我们在一组维度上计算了一个 CUBE，我们就可以得到在这些维度上所有可能的聚合问题的答案。

【给定的a b c三个维度，各种组合的可能性】

**GROUP BY a, b, c WITH CUBE 
== GROUP BY a, b, c GROUPING SETS ((a, b, c), (a, b), (b, c), (a, c), (a), (b), (c), ())**

```sql
select month, day, 
     count(distinct cookieid) uv
from cookie
group by month, day with cube;
....
OK
month	day	uv
NULL	NULL	7
NULL	2015-03-10	4
NULL	2015-03-12	1
NULL	2015-04-12	2
NULL	2015-04-13	3
NULL	2015-04-15	2
NULL	2015-04-16	2
2015-03	NULL	5
2015-03	2015-03-10	4
2015-03	2015-03-12	1
2015-04	NULL	6
2015-04	2015-04-12	2
2015-04	2015-04-13	3
2015-04	2015-04-15	2
2015-04	2015-04-16	2
Time taken: 49.27 seconds, Fetched: 15 row(s)
```

以上语句等价于

```sql
select month, day, count(distinct cookieid) uv from cookie group by month, day union all
select month, NULL, count(distinct cookieid) uv from cookie group by month union all
select NULL, day, count(distinct cookieid) uv from cookie group by day union all
select NULL, NULL, count(distinct cookieid) uv from cookie;
```

## rollup

只能与 GROUP BY 一起使用。

用于在一个维度的层次结构级别上计算聚合。

【确定维度a，从a下钻或上钻到b，再下钻或上钻到c】

**GROUP BY a, b, c with ROLLUP == GROUP BY a, b, c GROUPING SETS ((a, b, c), (a, b), (a), ())**

从month下钻到day ↓

```sql
select month, day, 
     count(distinct cookieid) uv
from cookie
group by month, day with rollup;
...
OK
month	day	uv
NULL	NULL	7
2015-03	NULL	5
2015-03	2015-03-10	4
2015-03	2015-03-12	1
2015-04	NULL	6
2015-04	2015-04-12	2
2015-04	2015-04-13	3
2015-04	2015-04-15	2
2015-04	2015-04-16	2
Time taken: 21.089 seconds, Fetched: 9 row(s)
```

以上语句等价于

```sql
select NULL, NULL, count(distinct cookieid) uv from cookie union all
select month, NULL, count(distinct cookieid) uv from cookie group by month union all
select month, day, count(distinct cookieid) uv from cookie group by month, day;
```

从day下钻到month ↓

```sql
select day,month,  
     count(distinct cookieid) uv
from cookie
group by day,month with rollup;
...
OK
day	month	uv
NULL	NULL	7
2015-03-10	NULL	4
2015-03-10	2015-03	4
2015-03-12	NULL	1
2015-03-12	2015-03	1
2015-04-12	NULL	2
2015-04-12	2015-04	2
2015-04-13	NULL	3
2015-04-13	2015-04	3
2015-04-15	NULL	2
2015-04-15	2015-04	2
2015-04-16	NULL	2
2015-04-16	2015-04	2
Time taken: 17.227 seconds, Fetched: 13 row(s)
```

## GROUPING_ID

```sql 
0: jdbc:hive2://localhost:10000> select * from g_tb;
+------------+------------+
| g_tb.col1  | g_tb.col2  |
+------------+------------+
| 3          | NULL       |
| 1          | NULL       |
| 1          | 1          |
| 3          | 3          |
| 2          | 2          |
| 4          | 5          |
+------------+------------+
6 rows selected (0.231 seconds)

0: jdbc:hive2://localhost:10000> SELECT col1, col2, GROUPING__ID, count(*)
. . . . . . . . . . . . . . . .> FROM g_tb
. . . . . . . . . . . . . . . .> GROUP BY col1, col2 GROUPING SETS( (col1, col2), (col1), (col2), ()) order by GROUPING__ID;
+-------+-------+---------------+------+
| col1  | col2  | grouping__id  | _c3  |
+-------+-------+---------------+------+
| 1     | 1     | 0             | 1    |
| 1     | NULL  | 0             | 1    |
| 2     | 2     | 0             | 1    |
| 3     | 3     | 0             | 1    |
| 3     | NULL  | 0             | 1    |
| 4     | 5     | 0             | 1    |
| 3     | NULL  | 1             | 2    |
| 4     | NULL  | 1             | 1    |
| 1     | NULL  | 1             | 2    |
| 2     | NULL  | 1             | 1    |
| NULL  | NULL  | 2             | 2    |
| NULL  | 1     | 2             | 1    |
| NULL  | 2     | 2             | 1    |
| NULL  | 3     | 2             | 1    |
| NULL  | 5     | 2             | 1    |
| NULL  | NULL  | 3             | 6    |
+-------+-------+---------------+------+
16 rows selected (4.007 seconds)


0: jdbc:hive2://localhost:10000> SELECT col1, col2, GROUPING__ID, count(*)
. . . . . . . . . . . . . . . .> FROM g_tb
. . . . . . . . . . . . . . . .> GROUP BY col1, col2 with rollup order by GROUPING__ID;
+-------+-------+---------------+------+
| col1  | col2  | grouping__id  | _c3  |
+-------+-------+---------------+------+
| 1     | 1     | 0             | 1    |
| 1     | NULL  | 0             | 1    |
| 2     | 2     | 0             | 1    |
| 3     | 3     | 0             | 1    |
| 3     | NULL  | 0             | 1    |
| 4     | 5     | 0             | 1    |
| 1     | NULL  | 1             | 2    |
| 2     | NULL  | 1             | 1    |
| 3     | NULL  | 1             | 2    |
| 4     | NULL  | 1             | 1    |
| NULL  | NULL  | 3             | 6    |
+-------+-------+---------------+------+
11 rows selected (2.055 seconds)
```

官网原文：This function returns a bitvector corresponding to whether each column is present or not. For each column, a value of "1" is produced for a row in the result set if that column has been aggregated in that row, otherwise the value is "0". This can be used to differentiate when there are nulls in the data.

这个函数返回一个位向量，对应于每个列是否存在。

对于每一列，如果该列在一行被聚合，那么结果集中的这行会产生值 1，否则产生 0。

```
对于第一个例子，组编号的产生  

 (col1, col2)  00  -> 二进制转十进制-> 0    【两列都未被聚合了，所以为0】
 (col1)        01  -> 二进制转十进制-> 1    【col1列都未被聚合了，所以为0，col2为1】
 (col2)        10  -> 二进制转十进制-> 2    【col2列都未被聚合了，所以为0，col1为1】
 ()            11  -> 二进制转十进制-> 3    【两列都被聚合了，所以为1】
```

在hive2.3.0版本之前行为相反，对于每一列，如果该列已在该行聚合，则该函数将返回一个值0，否则该值为1。

```
对于第一个例子，组编号的产生  

 (col1, col2)  11  -> 二进制转十进制-> 3  
 (col1)        10  -> 二进制转十进制-> 2   
 (col2)        01  -> 二进制转十进制-> 1  
 ()            00  -> 二进制转十进制-> 0  
```

更详细描述查看官网

## grouping

官网原文：The grouping function indicates whether an expression in a GROUP BY clause is aggregated or not for a given row. The value 0 represents a column that is part of the grouping set, while the value 1 represents a column that is not part of the grouping set.

分组函数表示是否为给定行聚合 GROUP BY 子句中的表达式。值 0 表示属于分组集的列，而值 1 表示不属于分组集的列。

```sql
0: jdbc:hive2://localhost:10000> select month, day, 
. . . . . . . . . . . . . . . .>      count(distinct cookieid) uv,
. . . . . . . . . . . . . . . .>      grouping(month,day),
. . . . . . . . . . . . . . . .>      grouping(day,month),
. . . . . . . . . . . . . . . .>      grouping(month),
. . . . . . . . . . . . . . . .>      grouping(day)
. . . . . . . . . . . . . . . .> from cookie
. . . . . . . . . . . . . . . .> group by month, day with cube;
+----------+-------------+-----+------+------+------+------+
|  month   |     day     | uv  | _c3  | _c4  | _c5  | _c6  |
+----------+-------------+-----+------+------+------+------+
| 2015-03  | 2015-03-10  | 4   | 0    | 0    | 0    | 0    |   
| 2015-03  | 2015-03-12  | 1   | 0    | 0    | 0    | 0    |
| 2015-03  | NULL        | 5   | 1    | 2    | 0    | 1    |
| 2015-04  | 2015-04-12  | 2   | 0    | 0    | 0    | 0    |
| 2015-04  | 2015-04-13  | 3   | 0    | 0    | 0    | 0    |
| 2015-04  | 2015-04-15  | 2   | 0    | 0    | 0    | 0    |
| 2015-04  | 2015-04-16  | 2   | 0    | 0    | 0    | 0    |
| 2015-04  | NULL        | 6   | 1    | 2    | 0    | 1    |
| NULL     | 2015-03-10  | 4   | 2    | 1    | 1    | 0    |
| NULL     | 2015-03-12  | 1   | 2    | 1    | 1    | 0    |
| NULL     | 2015-04-12  | 2   | 2    | 1    | 1    | 0    |
| NULL     | 2015-04-13  | 3   | 2    | 1    | 1    | 0    |
| NULL     | 2015-04-15  | 2   | 2    | 1    | 1    | 0    |
| NULL     | 2015-04-16  | 2   | 2    | 1    | 1    | 0    |
| NULL     | NULL        | 7   | 3    | 3    | 1    | 1    |
+----------+-------------+-----+------+------+------+------+
15 rows selected (3.066 seconds)
```

理解：

```
 【？？？】
select month, day, 
     count(distinct cookieid) uv,
     grouping(month,day),
     grouping(day,month),
     grouping(month),
     grouping(day)
from cookie
group by month, day with cube;
-- GROUPING SETS( (month, day), (month), (day), ())
-- group by month, day
-- group by month, null
-- group by null, day
-- group by null, null

例1
+----------+-------------+-----+------+------+------+------+
|  month   |     day     | uv  | _c3  | _c4  | _c5  | _c6  |
+----------+-------------+-----+------+------+------+------+
| 2015-03  | 2015-03-10  | 4   | 0    | 0    | 0    | 0    | 

产生uv=4，使用了 group by month,day

grouping(month,day)  00  -> 0
grouping(day,month), 00  -> 0
grouping(month),     0   -> 0
grouping(day)        0   -> 0

例2
+----------+-------------+-----+------+------+------+------+
|  month   |     day     | uv  | _c3  | _c4  | _c5  | _c6  |
+----------+-------------+-----+------+------+------+------+
| 2015-03  | NULL        | 5   | 1    | 2    | 0    | 1    |


产生uv=5，使用了 group by month

grouping(month,day)  01  -> 1
grouping(day,month), 10  -> 2
grouping(month),     0   -> 0
grouping(day)        1   -> 1

例2
+----------+-------------+-----+------+------+------+------+
|  month   |     day     | uv  | _c3  | _c4  | _c5  | _c6  |
+----------+-------------+-----+------+------+------+------+
| NULL     | NULL        | 7   | 3    | 3    | 1    | 1    |


产生uv=7，使用了 group by null,null

grouping(month,day)  11  -> 3
grouping(day,month), 11  -> 3
grouping(month),     1   -> 1
grouping(day)        1   -> 1
```

## 优化

如果 GROUPING SETS 产生的基数很大，也就是说有很多group by子句产生，那么可以设置 `hive.new.job.grouping.set.cardinality`，基数大于这个值，那么 hive 是否需要增加一个额外的 map-reduce job。

----------------------------------
参考：

[https://www.cnblogs.com/qingyunzong/p/8798987.html](https://www.cnblogs.com/qingyunzong/p/8798987.html)

[https://blog.51cto.com/u_13446/7644265](https://blog.51cto.com/u_13446/7644265)

[官网](https://cwiki.apache.org/confluence/display/Hive/Enhanced+Aggregation%2C+Cube%2C+Grouping+and+Rollup)

[官网翻译](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Enhanced%20Aggregation,%20Cube,%20Grouping%20and%20Rollup.md)