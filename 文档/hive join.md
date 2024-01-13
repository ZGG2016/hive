# hive 的 join

hive4.0

先 [点这里](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Joins.md) 查看 join 基础，[点这里](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Join%20Optimization.md) 查看 join 优化

------------------------------------
```sql
CREATE TABLE a (k1 int, k2 int, v1 int, v2 int);
CREATE TABLE b (k1 int, k2 int, v1 int, v2 int);
CREATE TABLE c (k1 int, k2 int, v1 int, v2 int);

insert into a values(1, 2, 3, 4),(5, 6, 7, 8),(9, 10, 11, 12);
insert into b values(1, 2, 3, 4),(5, 6, 7, 8),(9, 10, 11, 12);
insert into c values(1, 2, 3, 4),(5, 6, 7, 8),(9, 10, 11, 12);
```

从 Hive 0.13.0 开始，在 join conditions 中，Hive 试图根据 Join 的输入来解析非限定列引用。如果一个未限定的列引用解析为属于多个表，Hive 会将其标记为一个模糊引用。 【不指定字段属于哪个表】

```sql
-- 如果k1字段仅存在a中 或 k2字段仅存在b中就可以
0: jdbc:hive2://localhost:10000> select a.v1, b.v1 from a join b on k1=k2;
Error: Error while compiling statement: FAILED: SemanticException [Error 10008]: Line 1:35 Ambiguous table alias 'k1' (state=42000,code=10008)
```

从 Hive 2.2.0 开始，支持 ON 子句中的复杂表达式。在此之前，Hive 不支持非等号条件的 join conditions

```sql
0: jdbc:hive2://localhost:10000> SELECT a.k1,b.k1 FROM a LEFT OUTER JOIN b ON (a.k1 <> b.k1);
+-------+-------+
| a.k1  | b.k1  |
+-------+-------+
| 1     | 9     |
| 1     | 5     |
| 5     | 1     |
| 5     | 9     |
| 9     | 1     |
| 9     | 5     |
+-------+-------+
```

如果在 join 子句中使用每个表的相同的列，那么 Hive 将多表 join 转成一个 map/reduce job，否则就是多个 job   [???]

```sql
SELECT a.v1, b.v1, c.v1 FROM a JOIN b ON (a.k1 = b.k1) JOIN c ON (c.k1 = b.k1);
INFO  : Total jobs = 2
INFO  : Launching Job 1 out of 2
...
INFO  : The url to track the job: http://zgg-server:8088/proxy/application_1705117733491_0006/
INFO  : Starting Job = job_1705117733491_0006, Tracking URL = http://zgg-server:8088/proxy/application_1705117733491_0006/
...
INFO  : Ended Job = job_1705117733491_0006
INFO  : Launching Job 2 out of 2
...
INFO  : The url to track the job: http://zgg-server:8088/proxy/application_1705117733491_0007/
INFO  : Starting Job = job_1705117733491_0007, Tracking URL = http://zgg-server:8088/proxy/application_1705117733491_0007/
...


SELECT a.v1, b.v1, c.v1 FROM a JOIN b ON (a.k1 = b.k1) JOIN c ON (c.k1 = b.k2);
```



join 出现在 WHERE 子句之前。

因此，如果要限制 join 的输出，在 WHERE 子句中限制，否则应该放在 join 子句中。

```sql
set hive.auto.convert.join=false;

create table tb_ratings_2(uid int, mid int, rating double);
insert into tb_ratings_2 values(1,608,4.0),(1,1246,4.0),(2,1357,5.0),(2,3068,4.0);

0: jdbc:hive2://localhost:10000> select * from tb_ratings_2;
+-------------------+-------------------+----------------------+
| tb_ratings_2.uid  | tb_ratings_2.mid  | tb_ratings_2.rating  |
+-------------------+-------------------+----------------------+
| 1                 | 608               | 4.0                  |
| 1                 | 1246              | 4.0                  |
| 2                 | 1357              | 5.0                  |
| 2                 | 3068              | 4.0                  |
+-------------------+-------------------+----------------------+

create table tb_users_2 like tb_users;
insert into tb_users_2 select * from tb_users limit 3;

0: jdbc:hive2://localhost:10000> select * from tb_users_2;
+-----------------+--------------------+-----------------+------------------------+---------------------+
| tb_users_2.uid  | tb_users_2.gender  | tb_users_2.age  | tb_users_2.occupation  | tb_users_2.zipcode  |
+-----------------+--------------------+-----------------+------------------------+---------------------+
| 1               | F                  | 1               | 10                     | 48067               |
| 2               | M                  | 56              | 16                     | 70072               |
| 3               | M                  | 25              | 15                     | 55117               |
+-----------------+--------------------+-----------------+------------------------+---------------------+

0: jdbc:hive2://localhost:10000> select a.*, b.* from tb_users_2 a left join tb_ratings_2 b on a.uid=b.uid where a.gender='M';
+--------+-----------+--------+---------------+------------+--------+--------+-----------+
| a.uid  | a.gender  | a.age  | a.occupation  | a.zipcode  | b.uid  | b.mid  | b.rating  |
+--------+-----------+--------+---------------+------------+--------+--------+-----------+
| 3      | M         | 25     | 15            | 55117      | NULL   | NULL   | NULL      |
| 2      | M         | 56     | 16            | 70072      | 2      | 1357   | 5.0       |
| 2      | M         | 56     | 16            | 70072      | 2      | 3068   | 4.0       |
+--------+-----------+--------+---------------+------------+--------+--------+-----------+
0: jdbc:hive2://zgg-server:10000> explain select a.*, b.* from tb_users_2 a left join tb_ratings_2 b on a.uid=b.uid where a.gender='M';
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-1 is a root stage                          |
|   Stage-0 depends on stages: Stage-1               |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-1                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan      【过滤了】                            |
|             alias: a                               |
|             filterExpr: (gender = 'M') (type: boolean) |
|             Statistics: Num rows: 3 Data size: 549 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: (gender = 'M') (type: boolean) |
|               Statistics: Num rows: 2 Data size: 366 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: uid (type: int), age (type: int), occupation (type: string), zipcode (type: int) |
|                 outputColumnNames: _col0, _col1, _col2, _col3 |
|                 Statistics: Num rows: 2 Data size: 196 Basic stats: COMPLETE Column stats: COMPLETE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 2 Data size: 196 Basic stats: COMPLETE Column stats: COMPLETE |
|                   value expressions: _col1 (type: int), _col2 (type: string), _col3 (type: int) |
|           TableScan     【没有过滤】                           |
|             alias: b                               |
|             filterExpr: uid is not null (type: boolean) |
|             Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: uid is not null (type: boolean) |
|               Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: uid (type: int), mid (type: int), rating (type: double) |
|                 outputColumnNames: _col0, _col1, _col2 |
|                 Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|                   value expressions: _col1 (type: int), _col2 (type: double) |
|       Reduce Operator Tree:                        |
|         Join Operator                              |
|           condition map:                           |
|                Left Outer Join 0 to 1              |
|           keys:                                    |
|             0 _col0 (type: int)                    |
|             1 _col0 (type: int)                    |
|           outputColumnNames: _col0, _col1, _col2, _col3, _col4, _col5, _col6 |
|           Statistics: Num rows: 3 Data size: 342 Basic stats: COMPLETE Column stats: COMPLETE |
|           Select Operator                          |
|             expressions: _col0 (type: int), 'M' (type: string), _col1 (type: int), _col2 (type: string), _col3 (type: int), _col4 (type: int), _col5 (type: int), _col6 (type: double) |
|             outputColumnNames: _col0, _col1, _col2, _col3, _col4, _col5, _col6, _col7 |
|             Statistics: Num rows: 3 Data size: 597 Basic stats: COMPLETE Column stats: COMPLETE |
|             File Output Operator                   |
|               compressed: false                    |
|               Statistics: Num rows: 3 Data size: 597 Basic stats: COMPLETE Column stats: COMPLETE |
|               table:                               |
|                   input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                   output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                   serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|                                                    |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: -1                                    |
|       Processor Tree:                              |
|         ListSink                                   |
|                                                    |
+----------------------------------------------------+

0: jdbc:hive2://localhost:10000> select a.*, b.* from tb_users_2 a left join tb_ratings_2 b on a.uid=b.uid and a.gender='M';
+--------+-----------+--------+---------------+------------+--------+--------+-----------+
| a.uid  | a.gender  | a.age  | a.occupation  | a.zipcode  | b.uid  | b.mid  | b.rating  |
+--------+-----------+--------+---------------+------------+--------+--------+-----------+
| 1      | F         | 1      | 10            | 48067      | NULL   | NULL   | NULL      |
| 3      | M         | 25     | 15            | 55117      | NULL   | NULL   | NULL      |
| 2      | M         | 56     | 16            | 70072      | 2      | 1357   | 5.0       |
| 2      | M         | 56     | 16            | 70072      | 2      | 3068   | 4.0       |
+--------+-----------+--------+---------------+------------+--------+--------+-----------+
0: jdbc:hive2://localhost:10000> explain select a.*, b.* from tb_users_2 a left join tb_ratings_2 b on a.uid=b.uid and a.gender='M';
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-1 is a root stage                          |
|   Stage-0 depends on stages: Stage-1               |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-1                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan        【没有过滤】                        |
|             alias: a                               |
|             Statistics: Num rows: 3 Data size: 549 Basic stats: COMPLETE Column stats: COMPLETE |
|             Select Operator                        |
|               expressions: uid (type: int), gender (type: string), age (type: int), occupation (type: string), zipcode (type: int), (gender = 'M') (type: boolean) |
|               outputColumnNames: _col0, _col1, _col2, _col3, _col4, _col5 |
|               Statistics: Num rows: 3 Data size: 561 Basic stats: COMPLETE Column stats: COMPLETE |
|               Reduce Output Operator               |
|                 key expressions: _col0 (type: int) |
|                 null sort order: z                 |
|                 sort order: +                      |
|                 Map-reduce partition columns: _col0 (type: int) |
|                 Statistics: Num rows: 3 Data size: 561 Basic stats: COMPLETE Column stats: COMPLETE |
|                 value expressions: _col1 (type: string), _col2 (type: int), _col3 (type: string), _col4 (type: int), _col5 (type: boolean) |
|           TableScan         【没有过滤】                       |
|             alias: b                               |
|             filterExpr: uid is not null (type: boolean) |
|             Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: uid is not null (type: boolean) |
|               Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: uid (type: int), mid (type: int), rating (type: double) |
|                 outputColumnNames: _col0, _col1, _col2 |
|                 Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|                   value expressions: _col1 (type: int), _col2 (type: double) |
|       Reduce Operator Tree:                        |
|         Join Operator                              |
|           condition map:                           |
|                Left Outer Join 0 to 1              |
|           filter predicates:                       |
|             0 {VALUE._col4}                        |
|             1                                      |
|           keys:                                    |
|             0 _col0 (type: int)                    |
|             1 _col0 (type: int)                    |
|           outputColumnNames: _col0, _col1, _col2, _col3, _col4, _col6, _col7, _col8 |
|           Statistics: Num rows: 4 Data size: 796 Basic stats: COMPLETE Column stats: COMPLETE |
|           Select Operator                          |
|             expressions: _col0 (type: int), _col1 (type: string), _col2 (type: int), _col3 (type: string), _col4 (type: int), _col6 (type: int), _col7 (type: int), _col8 (type: double) |
|             outputColumnNames: _col0, _col1, _col2, _col3, _col4, _col5, _col6, _col7 |
|             Statistics: Num rows: 4 Data size: 796 Basic stats: COMPLETE Column stats: COMPLETE |
|             File Output Operator                   |
|               compressed: false                    |
|               Statistics: Num rows: 4 Data size: 796 Basic stats: COMPLETE Column stats: COMPLETE |
|               table:                               |
|                   input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                   output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                   serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|                                                    |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: -1                                    |
|       Processor Tree:                              |
|         ListSink                                   |
|                                                    |
+----------------------------------------------------+

```
对于分区表，

```sql
create table tb_ratings_p(uid int, mid int, rating double) partitioned by(rating_time string);
insert into tb_ratings_p partition(rating_time='2022') values(1,608,4.0),(1,1246,4.0),(2,1357,5.0),(2,3068,4.0);
insert into tb_ratings_p partition(rating_time='2023') values(1,608,4.0),(1,1246,4.0),(2,1357,5.0),(2,3068,4.0);

0: jdbc:hive2://localhost:10000> select * from tb_ratings_p;
+-------------------+-------------------+----------------------+---------------------------+
| tb_ratings_p.uid  | tb_ratings_p.mid  | tb_ratings_p.rating  | tb_ratings_p.rating_time  |
+-------------------+-------------------+----------------------+---------------------------+
| 1                 | 608               | 4.0                  | 2022                      |
| 1                 | 1246              | 4.0                  | 2022                      |
| 2                 | 1357              | 5.0                  | 2022                      |
| 2                 | 3068              | 4.0                  | 2022                      |
| 1                 | 608               | 4.0                  | 2023                      |
| 1                 | 1246              | 4.0                  | 2023                      |
| 2                 | 1357              | 5.0                  | 2023                      |
| 2                 | 3068              | 4.0                  | 2023                      |
+-------------------+-------------------+----------------------+---------------------------+


create table tb_users_p(uid int, gender string, age int, occupation int, zipcode int) partitioned by(year string);
insert into tb_users_p partition(year='2022') select * from tb_users where uid in (1,2,3);
insert into tb_users_p partition(year='2023') select * from tb_users where uid in (1,2,3);

0: jdbc:hive2://localhost:10000> select * from tb_users_p;
+-----------------+--------------------+-----------------+------------------------+---------------------+------------------+
| tb_users_p.uid  | tb_users_p.gender  | tb_users_p.age  | tb_users_p.occupation  | tb_users_p.zipcode  | tb_users_p.year  |
+-----------------+--------------------+-----------------+------------------------+---------------------+------------------+
| 1               | F                  | 1               | 10                     | 48067               | 2022             |
| 2               | M                  | 56              | 16                     | 70072               | 2022             |
| 3               | M                  | 25              | 15                     | 55117               | 2022             |
| 1               | F                  | 1               | 10                     | 48067               | 2023             |
| 2               | M                  | 56              | 16                     | 70072               | 2023             |
| 3               | M                  | 25              | 15                     | 55117               | 2023             |
+-----------------+--------------------+-----------------+------------------------+---------------------+------------------+


-- 你将过滤掉所有没有有效的 b.key 的连接输出行，因此你已经超越了 LEFT OUTER 要求 
0: jdbc:hive2://localhost:10000> select a.*, b.* from tb_users_p a left join tb_ratings_p b on a.uid=b.uid where a.year='2022' and b.rating_time='2022';
+--------+-----------+--------+---------------+------------+---------+--------+--------+-----------+----------------+
| a.uid  | a.gender  | a.age  | a.occupation  | a.zipcode  | a.year  | b.uid  | b.mid  | b.rating  | b.rating_time  |
+--------+-----------+--------+---------------+------------+---------+--------+--------+-----------+----------------+
| 1      | F         | 1      | 10            | 48067      | 2022    | 1      | 608    | 4.0       | 2022           |
| 1      | F         | 1      | 10            | 48067      | 2022    | 1      | 1246   | 4.0       | 2022           |
| 2      | M         | 56     | 16            | 70072      | 2022    | 2      | 1357   | 5.0       | 2022           |
| 2      | M         | 56     | 16            | 70072      | 2022    | 2      | 3068   | 4.0       | 2022           |
+--------+-----------+--------+---------------+------------+---------+--------+--------+-----------+----------------+

0: jdbc:hive2://zgg-server:10000> explain select a.*, b.* from tb_users_p a left join tb_ratings_p b on a.uid=b.uid where a.year='2022' and b.rating_time='2022';
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-1 is a root stage                          |
|   Stage-0 depends on stages: Stage-1               |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-1                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan           【过滤了】                    |
|             alias: a                               |
|             filterExpr: ((year = '2022') and uid is not null) (type: boolean) |
|             Statistics: Num rows: 3 Data size: 303 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: uid is not null (type: boolean) |
|               Statistics: Num rows: 3 Data size: 303 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: uid (type: int), gender (type: string), age (type: int), occupation (type: int), zipcode (type: int) |
|                 outputColumnNames: _col0, _col1, _col2, _col3, _col4 |
|                 Statistics: Num rows: 3 Data size: 303 Basic stats: COMPLETE Column stats: COMPLETE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 3 Data size: 303 Basic stats: COMPLETE Column stats: COMPLETE |
|                   value expressions: _col1 (type: string), _col2 (type: int), _col3 (type: int), _col4 (type: int) |
|           TableScan       【过滤了】                            |
|             alias: b                               |
|             filterExpr: ((rating_time = '2022') and uid is not null) (type: boolean) |
|             Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: uid is not null (type: boolean) |
|               Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: uid (type: int), mid (type: int), rating (type: double) |
|                 outputColumnNames: _col0, _col1, _col2 |
|                 Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|                   value expressions: _col1 (type: int), _col2 (type: double) |
|       Reduce Operator Tree:                        |
|         Join Operator                              |
|           condition map:                           |
|                Inner Join 0 to 1                   |
|           keys:                                    |
|             0 _col0 (type: int)                    |
|             1 _col0 (type: int)                    |
|           outputColumnNames: _col0, _col1, _col2, _col3, _col4, _col5, _col6, _col7 |
|           Statistics: Num rows: 4 Data size: 468 Basic stats: COMPLETE Column stats: COMPLETE |
|           Select Operator                          |
|             expressions: _col0 (type: int), _col1 (type: string), _col2 (type: int), _col3 (type: int), _col4 (type: int), '2022' (type: string), _col5 (type: int), _col6 (type: int), _col7 (type: double), '2022' (type: string) |
|             outputColumnNames: _col0, _col1, _col2, _col3, _col4, _col5, _col6, _col7, _col8, _col9 |
|             Statistics: Num rows: 4 Data size: 1172 Basic stats: COMPLETE Column stats: COMPLETE |
|             File Output Operator                   |
|               compressed: false                    |
|               Statistics: Num rows: 4 Data size: 1172 Basic stats: COMPLETE Column stats: COMPLETE |
|               table:                               |
|                   input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                   output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                   serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|                                                    |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: -1                                    |
|       Processor Tree:                              |
|         ListSink                                   |
|                                                    |
+----------------------------------------------------+


-- 将 a.year='2022' and b.rating_time='2022' 放到 on 子句上
0: jdbc:hive2://localhost:10000> select a.*, b.* from tb_users_p a left join tb_ratings_p b on a.uid=b.uid and a.year='2022' and b.rating_time='2022';
+--------+-----------+--------+---------------+------------+---------+--------+--------+-----------+----------------+
| a.uid  | a.gender  | a.age  | a.occupation  | a.zipcode  | a.year  | b.uid  | b.mid  | b.rating  | b.rating_time  |
+--------+-----------+--------+---------------+------------+---------+--------+--------+-----------+----------------+
| 1      | F         | 1      | 10            | 48067      | 2023    | NULL   | NULL   | NULL      | NULL           |
| 1      | F         | 1      | 10            | 48067      | 2022    | 1      | 1246   | 4.0       | 2022           |
| 1      | F         | 1      | 10            | 48067      | 2022    | 1      | 608    | 4.0       | 2022           |
| 2      | M         | 56     | 16            | 70072      | 2023    | NULL   | NULL   | NULL      | NULL           |
| 2      | M         | 56     | 16            | 70072      | 2022    | 2      | 3068   | 4.0       | 2022           |
| 2      | M         | 56     | 16            | 70072      | 2022    | 2      | 1357   | 5.0       | 2022           |
| 3      | M         | 25     | 15            | 55117      | 2023    | NULL   | NULL   | NULL      | NULL           |
| 3      | M         | 25     | 15            | 55117      | 2022    | NULL   | NULL   | NULL      | NULL           |
+--------+-----------+--------+---------------+------------+---------+--------+--------+-----------+----------------+

0: jdbc:hive2://zgg-server:10000> explain select a.*, b.* from tb_users_p a left join tb_ratings_p b on a.uid=b.uid and a.year='2022' and b.rating_time='2022';
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-1 is a root stage                          |
|   Stage-0 depends on stages: Stage-1               |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-1                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan    【没有过滤】                            |
|             alias: a                               |
|             Statistics: Num rows: 6 Data size: 1710 Basic stats: COMPLETE Column stats: COMPLETE |
|             Select Operator                        |
|               expressions: uid (type: int), gender (type: string), age (type: int), occupation (type: int), zipcode (type: int), year (type: string), (year = '2022') (type: boolean) |
|               outputColumnNames: _col0, _col1, _col2, _col3, _col4, _col5, _col6 |
|               Statistics: Num rows: 6 Data size: 1734 Basic stats: COMPLETE Column stats: COMPLETE |
|               Reduce Output Operator               |
|                 key expressions: _col0 (type: int) |
|                 null sort order: z                 |
|                 sort order: +                      |
|                 Map-reduce partition columns: _col0 (type: int) |
|                 Statistics: Num rows: 6 Data size: 1734 Basic stats: COMPLETE Column stats: COMPLETE |
|                 value expressions: _col1 (type: string), _col2 (type: int), _col3 (type: int), _col4 (type: int), _col5 (type: string), _col6 (type: boolean) |
|           TableScan        【过滤了】                            |
|             alias: b                               |
|             filterExpr: ((rating_time = '2022') and uid is not null) (type: boolean) |
|             Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: uid is not null (type: boolean) |
|               Statistics: Num rows: 4 Data size: 64 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: uid (type: int), mid (type: int), rating (type: double), '2022' (type: string) |
|                 outputColumnNames: _col0, _col1, _col2, _col3 |
|                 Statistics: Num rows: 4 Data size: 416 Basic stats: COMPLETE Column stats: COMPLETE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 4 Data size: 416 Basic stats: COMPLETE Column stats: COMPLETE |
|                   value expressions: _col1 (type: int), _col2 (type: double), _col3 (type: string) |
|       Reduce Operator Tree:                        |
|         Join Operator                              |
|           condition map:                           |
|                Left Outer Join 0 to 1              |
|           filter predicates:                       |
|             0 {VALUE._col5}                        |
|             1                                      |
|           keys:                                    |
|             0 _col0 (type: int)                    |
|             1 _col0 (type: int)                    |
|           outputColumnNames: _col0, _col1, _col2, _col3, _col4, _col5, _col7, _col8, _col9, _col10 |
|           Statistics: Num rows: 11 Data size: 4071 Basic stats: COMPLETE Column stats: COMPLETE |
|           Select Operator                          |
|             expressions: _col0 (type: int), _col1 (type: string), _col2 (type: int), _col3 (type: int), _col4 (type: int), _col5 (type: string), _col7 (type: int), _col8 (type: int), _col9 (type: double), _col10 (type: string) |
|             outputColumnNames: _col0, _col1, _col2, _col3, _col4, _col5, _col6, _col7, _col8, _col9 |
|             Statistics: Num rows: 11 Data size: 4071 Basic stats: COMPLETE Column stats: COMPLETE |
|             File Output Operator                   |
|               compressed: false                    |
|               Statistics: Num rows: 11 Data size: 4071 Basic stats: COMPLETE Column stats: COMPLETE |
|               table:                               |
|                   input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                   output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                   serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|                                                    |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: -1                                    |
|       Processor Tree:                              |
|         ListSink                                   |
|                                                    |
+----------------------------------------------------+
```

LEFT SEMI JOIN 以一种高效的方式实现了不相关的 IN/EXISTS 子查询语义

```sql
-- 再往b表中插入数据
insert into b values(1, 2, 3, 4),(5, 6, 7, 8),(9, 10, 11, 12);

select a.* from a where a.k1 in (select k1 from b);
select a.* from a where exists (select k1 from b where a.k1=b.k1);
select a.* from a left semi join b on a.k1=b.k1; 
+-------+-------+-------+-------+
| a.k1  | a.k2  | a.v1  | a.v2  |
+-------+-------+-------+-------+
| 1     | 2     | 3     | 4     |
| 5     | 6     | 7     | 8     |
| 9     | 10    | 11    | 12    |
+-------+-------+-------+-------+

-- 右边的表只能在连接条件(on-子句)中引用，而不能在 WHERE- 或 SELECT- 子句中引用
0: jdbc:hive2://zgg-server:10000> select a.v1, b.v1 from a left semi join b on a.k1=b.k1;
Error: Error while compiling statement: FAILED: SemanticException [Error 10004]: Line 1:13 Invalid table alias or column reference 'b': (possible column names are: k1, k2, v1, v2) (state=42000,code=10004)


-- 在b表找到两行匹配的数据，都会匹配输出
-- 0: jdbc:hive2://zgg-server:10000> select a.* from a join b on a.k1=b.k1;
0: jdbc:hive2://zgg-server:10000> select a.* from a left join b on a.k1=b.k1;
+-------+-------+-------+-------+
| a.k1  | a.k2  | a.v1  | a.v2  |
+-------+-------+-------+-------+
| 1     | 2     | 3     | 4     |
| 1     | 2     | 3     | 4     |
| 5     | 6     | 7     | 8     |
| 5     | 6     | 7     | 8     |
| 9     | 10    | 11    | 12    |
| 9     | 10    | 11    | 12    |
+-------+-------+-------+-------+
```

----------------------------
其他参考：

[https://www.cnblogs.com/wqbin/p/10270384.html](https://www.cnblogs.com/wqbin/p/10270384.html)

[https://www.bilibili.com/video/BV1EZ4y1G7iL?p=107](https://www.bilibili.com/video/BV1EZ4y1G7iL?p=107)
