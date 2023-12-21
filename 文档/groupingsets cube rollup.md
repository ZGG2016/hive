# groupingsets cube rollup

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

## grouping sets

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

TODO

## grouping

TODO

```sql
select month, day, 
     count(distinct cookieid) uv,
     grouping(month,day),
     grouping(day,month),
     grouping(month),
     grouping(day)
from cookie
group by month, day with cube;
```

----------------------------------
参考：

[https://www.cnblogs.com/qingyunzong/p/8798987.html](https://www.cnblogs.com/qingyunzong/p/8798987.html)

[https://blog.51cto.com/u_13446/7644265](https://blog.51cto.com/u_13446/7644265)

? [官网](https://cwiki.apache.org/confluence/display/Hive/Enhanced+Aggregation%2C+Cube%2C+Grouping+and+Rollup)

[官网翻译](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Enhanced%20Aggregation,%20Cube,%20Grouping%20and%20Rollup.md)