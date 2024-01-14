# groupby数据倾斜

- hive.groupby.skewindata

	Default Value: false

	Whether there is skew in data to optimize group by queries.

	启用

```sql
0: jdbc:hive2://zgg-server:10000> select mid, count(1) cnt from ratings group by mid order by cnt asc;
| 209151  | 1    |
| 209153  | 1    |
| 209155  | 1    |
| 209157  | 1    |
| 209159  | 1    |
| 209163  | 1    |
| 209169  | 1    |
...
| 110    | 59184  |
| 527    | 60411  |
| 480    | 64144  |
| 260    | 68717  |
| 2571   | 72674  |
| 593    | 74127  |
| 296    | 79672  |
| 318    | 81482  |
| 356    | 81491  |
+---------+------+

0: jdbc:hive2://zgg-server:10000> set hive.groupby.skewindata=true;

-- 起两个job
0: jdbc:hive2://zgg-server:10000> select mid, count(rating) cnt from ratings group by mid;
...
INFO  : Total jobs = 2
INFO  : Launching Job 1 out of 2
...

0: jdbc:hive2://zgg-server:10000> explain select mid, count(rating) cnt from ratings group by mid;
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
|             Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|             Select Operator                        |
|               expressions: mid (type: int)         |
|               outputColumnNames: mid               |
|               Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|               Group By Operator                    |
|                 aggregations: count(rating)        |
|                 keys: mid (type: int)              |
|                 minReductionHashAggr: 0.99         |
|                 mode: hash                         |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: rand() (type: double) |   【使用随机数作为分区列，进行重新分区】
|                   Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|                   value expressions: _col1 (type: bigint) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Group By Operator                          |
|           aggregations: count(VALUE._col0)         |
|           keys: KEY._col0 (type: int)              |
|           mode: partials        【shuffle到reducer后，进行分区内聚合】                   |
|           outputColumnNames: _col0, _col1          |
|           Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|           File Output Operator                     |
|             compressed: false                      |
|             table:                                 |
|                 input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                 output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                 serde: org.apache.hadoop.hive.serde2.lazybinary.LazyBinarySerDe |
|                                                    |
|   Stage: Stage-2                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan                                |
|               key expressions: _col0 (type: int)   |
|               null sort order: z                   |
|               sort order: +                        |
|               Map-reduce partition columns: _col0 (type: int) |
|               Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|               value expressions: _col1 (type: bigint) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Group By Operator                          |
|           aggregations: count(VALUE._col0)         |
|           keys: KEY._col0 (type: int)              |
|           mode: final     【最终的聚合】              |
|           outputColumnNames: _col0, _col1          |
|           Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|           File Output Operator                     |
|             compressed: false                      |
|             Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|             table:                                 |
|                 input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                 output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                 serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|                                                    |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: -1                                    |
|       Processor Tree:                              |
|         ListSink                                   |
|                                                    |
+----------------------------------------------------+
```

```sql
-- 关闭后，再看执行计划
0: jdbc:hive2://zgg-server:10000> set hive.groupby.skewindata=false;

-- 只起一个job
0: jdbc:hive2://zgg-server:10000> select mid, count(rating) cnt from ratings group by mid;
...
INFO  : Total jobs = 1
INFO  : Launching Job 1 out of 1
...

0: jdbc:hive2://zgg-server:10000> explain select mid, count(rating) cnt from ratings group by mid;
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
|             Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|             Select Operator                        |
|               expressions: mid (type: int)         |
|               outputColumnNames: mid               |
|               Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|               Group By Operator                    |
|                 aggregations: count(rating)        |
|                 keys: mid (type: int)              |
|                 minReductionHashAggr: 0.99         |
|                 mode: hash                         |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |  【使用mid列分区】
|                   Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|                   value expressions: _col1 (type: bigint) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Group By Operator                          |
|           aggregations: count(VALUE._col0)         |
|           keys: KEY._col0 (type: int)              |
|           mode: mergepartial                       |
|           outputColumnNames: _col0, _col1          |
|           Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|           File Output Operator                     |
|             compressed: false                      |
|             Statistics: Num rows: 1 Data size: 4 Basic stats: COMPLETE Column stats: NONE |
|             table:                                 |
|                 input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                 output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                 serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|                                                    |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: -1                                    |
|       Processor Tree:                              |
|         ListSink                                   |
|                                                    |
+----------------------------------------------------+
```

[参考理解](https://blog.csdn.net/weixin_46389691/article/details/132620702)