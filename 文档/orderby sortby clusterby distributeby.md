# orderby sortby clusterby distributeby

先阅读[这里](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Sort%20Distribute%20Cluster%20Order%20By.md)

## orderby sortby

order by 和 sort by 的区别就是：

- order by 保证了输出数据的全局有序。

- sort by 保证了每个 reducer 中的行有序。

若超过了一个 reducer ，sort by 的结果可能是部分有序。

----------------------------------------------------------------

order by: there has to be one reducer to sort the final output

```sql
-- 只起了一个 reducer，那么所有数据都进入到这里排序
0: jdbc:hive2://zgg-server:10000> select * from ratings order by rating_time limit 3;
....
INFO  : MapReduce Jobs Launched: 
INFO  : Stage-Stage-1: Map: 3  Reduce: 1   Cumulative CPU: 46.03 sec   HDFS Read: 678310327 HDFS Write: 199 HDFS EC Read: 0 SUCCESS
INFO  : Total MapReduce CPU Time Spent: 46 seconds 30 msec
INFO  : Completed executing command(queryId=root_20240118110123_63d3f7bc-e7a8-4f8b-b147-1bb3e622892b); Time taken: 113.827 seconds
+--------------+--------------+-----------------+----------------------+
| ratings.uid  | ratings.mid  | ratings.rating  | ratings.rating_time  |
+--------------+--------------+-----------------+----------------------+
| 99254        | 527          | 4.0             | 1000000065           |
| 99254        | 318          | 5.0             | 1000000065           |
| 62636        | 4053         | 3.0             | 1000000188           |
+--------------+--------------+-----------------+----------------------+

0: jdbc:hive2://zgg-server:10000> explain select * from ratings order by rating_time limit 3;
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
|           TableScan                                |
|             alias: ratings                         |
|             Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|             Select Operator                        |
|               expressions: uid (type: int), mid (type: int), rating (type: double), rating_time (type: string) |
|               outputColumnNames: _col0, _col1, _col2, _col3 |
|               Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|               Reduce Output Operator               |
|                 key expressions: _col3 (type: string) |
|                 null sort order: z                 |
|                 sort order: +                      |
|                 Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|                 TopN Hash Memory Usage: 0.1        |
|                 value expressions: _col0 (type: int), _col1 (type: int), _col2 (type: double) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Select Operator                            |
|           expressions: VALUE._col0 (type: int), VALUE._col1 (type: int), VALUE._col2 (type: double), KEY.reducesinkkey0 (type: string) |
|           outputColumnNames: _col0, _col1, _col2, _col3 |
|           Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|           Limit                                    |
|             Number of rows: 3                      |
|             Statistics: Num rows: 3 Data size: 330 Basic stats: COMPLETE Column stats: COMPLETE |
|             File Output Operator                   |
|               compressed: false                    |
|               Statistics: Num rows: 3 Data size: 330 Basic stats: COMPLETE Column stats: COMPLETE |
|               table:                               |
|                   input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                   output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                   serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|                                                    |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: 3                                     |
|       Processor Tree:                              |
|         ListSink                                   |
|                                                    |
+----------------------------------------------------+
```

Hive uses the columns in SORT BY to sort the rows before feeding the rows to a reducer. 

```sql
0: jdbc:hive2://zgg-server:10000> select * from ratings sort by rating_time limit 3;
...
INFO  : MapReduce Jobs Launched: 
INFO  : Stage-Stage-1: Map: 3  Reduce: 3   Cumulative CPU: 41.53 sec   HDFS Read: 678321469 HDFS Write: 587 HDFS EC Read: 0 SUCCESS
INFO  : Stage-Stage-2: Map: 1  Reduce: 1   Cumulative CPU: 3.61 sec   HDFS Read: 10341 HDFS Write: 199 HDFS EC Read: 0 SUCCESS
INFO  : Total MapReduce CPU Time Spent: 45 seconds 140 msec
INFO  : Completed executing command(queryId=root_20240118110354_7f3d37f2-250f-45bb-b480-ede0ea096e9d); Time taken: 132.183 seconds
+--------------+--------------+-----------------+----------------------+
| ratings.uid  | ratings.mid  | ratings.rating  | ratings.rating_time  |
+--------------+--------------+-----------------+----------------------+
| 99254        | 527          | 4.0             | 1000000065           |
| 99254        | 318          | 5.0             | 1000000065           |
| 62636        | 4053         | 3.0             | 1000000188           |
+--------------+--------------+-----------------+----------------------+

0: jdbc:hive2://zgg-server:10000> explain select * from ratings sort by rating_time limit 3;
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-1 is a root stage                          |
|   Stage-2 depends on stages: Stage-1               |
|   Stage-0 depends on stages: Stage-2               |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-1                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan                                |
|             alias: ratings                         |
|             Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|             Select Operator                        |
|               expressions: uid (type: int), mid (type: int), rating (type: double), rating_time (type: string) |
|               outputColumnNames: _col0, _col1, _col2, _col3 |
|               Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|               Reduce Output Operator               |
|                 key expressions: _col3 (type: string) |
|                 null sort order: z                 |
|                 sort order: +                      |
|                 Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|                 TopN Hash Memory Usage: 0.1        |
|                 value expressions: _col0 (type: int), _col1 (type: int), _col2 (type: double) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Limit                                      |
|           Number of rows: 3                        |
|           Statistics: Num rows: 3 Data size: 330 Basic stats: COMPLETE Column stats: COMPLETE |
|           Select Operator                          |
|             expressions: VALUE._col0 (type: int), VALUE._col1 (type: int), VALUE._col2 (type: double), KEY.reducesinkkey0 (type: string) |
|             outputColumnNames: _col0, _col1, _col2, _col3 |
|             Statistics: Num rows: 3 Data size: 330 Basic stats: COMPLETE Column stats: COMPLETE |
|             File Output Operator                   |
|               compressed: false                    |
|               table:                               |
|                   input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                   output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                   serde: org.apache.hadoop.hive.serde2.lazybinary.LazyBinarySerDe |
|                                                    |
|   Stage: Stage-2                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan                                |
|             Reduce Output Operator                 |
|               key expressions: _col3 (type: string) |
|               null sort order: z                   |
|               sort order: +                        |
|               Statistics: Num rows: 3 Data size: 330 Basic stats: COMPLETE Column stats: COMPLETE |
|               TopN Hash Memory Usage: 0.1          |
|               value expressions: _col0 (type: int), _col1 (type: int), _col2 (type: double) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Limit                                      |
|           Number of rows: 3                        |
|           Statistics: Num rows: 3 Data size: 330 Basic stats: COMPLETE Column stats: COMPLETE |
|           Select Operator                          |
|             expressions: VALUE._col0 (type: int), VALUE._col1 (type: int), VALUE._col2 (type: double), KEY.reducesinkkey0 (type: string) |
|             outputColumnNames: _col0, _col1, _col2, _col3 |
|             Statistics: Num rows: 3 Data size: 330 Basic stats: COMPLETE Column stats: COMPLETE |
|             File Output Operator                   |
|               compressed: false                    |
|               Statistics: Num rows: 3 Data size: 330 Basic stats: COMPLETE Column stats: COMPLETE |
|               table:                               |
|                   input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                   output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                   serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|                                                    |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: 3                                     |
|       Processor Tree:                              |
|         ListSink                                   |
|                                                    |
+----------------------------------------------------+

```

在 Hive 2.1.0 及之后的版本，可以在 order by 子句指定 null 放置的位置

- 对于 ASC ，默认是 NULLS FIRST

- 对于 DESC ，默认是 NULLS LAST

```sql
select * from ratings order by desc NULLS FIRST;
```

其他特性见 [严格模式文档]()

## clusterby distributeby

cluster by 等同于 distribute by 和 sort by

Hive 使用 distribute by 在 reducers 间分发行数据。具有相同 distribute by 的列的行数据进入同一个 reducer。它并不保证根据分发的 key 进行分类或排序。

```sql
select * from ratings cluster by uid;
select * from ratings distribute by uid sort by uid;
```

```sql
0: jdbc:hive2://zgg-server:10000> explain select * from ratings cluster by uid;
...
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
|           TableScan                                |
|             alias: ratings                         |
|             Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|             Select Operator                        |
|               expressions: uid (type: int), mid (type: int), rating (type: double), rating_time (type: string) |
|               outputColumnNames: _col0, _col1, _col2, _col3 |
|               Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|               Reduce Output Operator               |
|                 key expressions: _col0 (type: int) |
|                 null sort order: a                 |
|                 sort order: +                      |
|                 Map-reduce partition columns: _col0 (type: int) |
|                 Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|                 value expressions: _col1 (type: int), _col2 (type: double), _col3 (type: string) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Select Operator                            |
|           expressions: KEY.reducesinkkey0 (type: int), VALUE._col0 (type: int), VALUE._col1 (type: double), VALUE._col2 (type: string) |
|           outputColumnNames: _col0, _col1, _col2, _col3 |
|           Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |


0: jdbc:hive2://zgg-server:10000> explain select * from ratings distribute by uid;
...
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
|           TableScan                                |
|             alias: ratings                         |
|             Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|             Select Operator                        |
|               expressions: uid (type: int), mid (type: int), rating (type: double), rating_time (type: string) |
|               outputColumnNames: _col0, _col1, _col2, _col3 |
|               Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|               Reduce Output Operator               |
|                 null sort order:                   |
|                 sort order:                        |
|                 Map-reduce partition columns: _col0 (type: int) |
|                 Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
|                 value expressions: _col0 (type: int), _col1 (type: int), _col2 (type: double), _col3 (type: string) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Select Operator                            |
|           expressions: VALUE._col0 (type: int), VALUE._col1 (type: int), VALUE._col2 (type: double), VALUE._col3 (type: string) |
|           outputColumnNames: _col0, _col1, _col2, _col3 |
|           Statistics: Num rows: 25000096 Data size: 2750010560 Basic stats: COMPLETE Column stats: COMPLETE |
```

[官网](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+SortBy)