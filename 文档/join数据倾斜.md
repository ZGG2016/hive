# join数据倾斜


- hive.optimize.skewjoin

	Default Value: false

	Whether to enable skew join optimization.  (Also see `hive.optimize.skewjoin.compiletime` .)

	启用倾斜join优化（原理见 `hive.optimize.skewjoin.compiletime` 描述）

- hive.skewjoin.key

	Default Value: 100000

	Determine if we get a skew key in join. If we see more than the specified number of rows with the same key in join operator, we think the key as a skew join key.

	如果key对应的行的数量超过这个值，就将其认为是倾斜key

- hive.skewjoin.mapjoin.map.tasks

	Default Value: 10000

	Determine the number of map task used in the follow up map join job for a skew join. It should be used together with `hive.skewjoin.mapjoin.min.split` to perform a fine grained control.

	对于倾斜join，在接下来的 map join 作业中使用的 map 任务的数量。

- hive.skewjoin.mapjoin.min.split

	Default Value: 33554432

	Determine the number of map task at most used in the follow up map join job for a skew join by specifying the minimum split size. It should be used together with `hive.skewjoin.mapjoin.map.tasks` to perform a fine grained control.

	根据这里指定最小的分片大小，来决定最多使用的 map 任务的数量

- hive.optimize.skewjoin.compiletime

	Default Value: false

	Whether to create a separate plan for skewed keys for the tables in the join. This is based on the skewed keys stored in the metadata. At compile time, the plan is broken into different joins: one for the skewed keys, and the other for the remaining keys. And then, a union is performed for the two joins generated above. So unless the same skewed key is present in both the joined tables, the join for the skewed key will be performed as a map-side join.

	是否为倾斜key创建一个独立了的计划。它根据存储在源数据中的倾斜key决定。

	在编译期间，计划被划分为两个join: 一个是倾斜key的join，一个是剩余key的join。然后在union这两个join的结果。

	除非同一倾斜key同时存在两个表中，否则倾斜key的join执行map端join

	The main difference between this paramater and `hive.optimize.skewjoin` is that this parameter uses the skew information stored in the metastore to optimize the plan at compile time itself. If there is no skew information in the metadata, this parameter will not have any effect.

    这个参数和 `hive.optimize.skewjoin` 的区别是：

    这个参数使用存储在 metastore 中的倾斜信息在编译期间优化计划。如果 metastore 中没有倾斜信息，那么这个参数就不起作用。

	Both `hive.optimize.skewjoin.compiletime` and `hive.optimize.skewjoin` should be set to true. (Ideally, `hive.optimize.skewjoin` should be renamed as `hive.optimize.skewjoin.runtime` , but for backward compatibility that has not been done.)

	这两个参数都应该设为 true. 

	（理想情况下，`hive.optimize.skewjoin` 应该重命名为 `hive.optimize.skewjoin.runtime`, 但为了向后兼容，尚未完成）

	If the skew information is correctly stored in the metadata, `hive.optimize.skewjoin.compiletime` will change the query plan to take care of it, and `hive.optimize.skewjoin` will be a no-op.

	如果倾斜信息正确地存储在 metadata 中，`hive.optimize.skewjoin.compiletime` 将改变查询计划，而 `hive.optimize.skewjoin` 将不会有任何操作。

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

0: jdbc:hive2://zgg-server:10000> select count(1) from movies;
+--------+
|  _c0   |
+--------+
| 62424  |
+--------+
```

启用

```sql
0: jdbc:hive2://zgg-server:10000> set hive.skewjoin.key=60000;

0: jdbc:hive2://zgg-server:10000> set hive.optimize.skewjoin=true;

0: jdbc:hive2://zgg-server:10000> set hive.auto.convert.join=false;

0: jdbc:hive2://zgg-server:10000> select b.title, a.rating from ratings a join movies b on a.mid=b.mid;
...
INFO  : Total jobs = 2
INFO  : Launching Job 1 out of 2
...

0: jdbc:hive2://zgg-server:10000> explain select b.title, a.rating from ratings a join movies b on a.mid=b.mid;
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-1 is a root stage                          |
|   Stage-4 depends on stages: Stage-1 , consists of Stage-5 |
|   Stage-5                                          |
|   Stage-3 depends on stages: Stage-5               |
|   Stage-0 depends on stages: Stage-3               |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-1                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan                                |
|             alias: a                               |
|             filterExpr: mid is not null (type: boolean) |
|             Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|             Filter Operator                        |
|               predicate: mid is not null (type: boolean) |
|               Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|               Select Operator                      |
|                 expressions: mid (type: int), rating (type: double) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|                   value expressions: _col1 (type: double) |
|           TableScan                                |
|             alias: b                               |
|             filterExpr: mid is not null (type: boolean) |
|             Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|             Filter Operator                        |
|               predicate: mid is not null (type: boolean) |
|               Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|               Select Operator                      |
|                 expressions: mid (type: int), title (type: string) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|                   value expressions: _col1 (type: string) |
|       Reduce Operator Tree:                        |
|         Join Operator                              |
|           condition map:                           |
|                Inner Join 0 to 1                   |
|           handleSkewJoin: true         【这里】      |
|           keys:                                    |
|             0 _col0 (type: int)                    |
|             1 _col0 (type: int)                    |
|           outputColumnNames: _col1, _col3          |
|           Statistics: Num rows: 1 Data size: 13 Basic stats: COMPLETE Column stats: NONE |
|           Select Operator                          |
|             expressions: _col3 (type: string), _col1 (type: double) |
|             outputColumnNames: _col0, _col1        |
|             Statistics: Num rows: 1 Data size: 13 Basic stats: COMPLETE Column stats: NONE |
|             File Output Operator                   |
|               compressed: false                    |
|               Statistics: Num rows: 1 Data size: 13 Basic stats: COMPLETE Column stats: NONE |
|               table:                               |
|                   input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                   output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                   serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|                                                    |
|   Stage: Stage-4                                   |
|     Conditional Operator                           |
|                                                    |
|   Stage: Stage-5                                   |
|     Map Reduce Local Work                          |
|       Alias -> Map Local Tables:                   |
|         1                                          |
|           Fetch Operator                           |
|             limit: -1                              |
|       Alias -> Map Local Operator Tree:            |
|         1                                          |
|           TableScan                                |
|             HashTable Sink Operator                |
|               keys:                                |
|                 0 reducesinkkey0 (type: int)       |
|                 1 reducesinkkey0 (type: int)       |
|                                                    |
|   Stage: Stage-3                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan                                |
|             Map Join Operator                      |
|               condition map:                       |
|                    Inner Join 0 to 1               |
|               keys:                                |
|                 0 reducesinkkey0 (type: int)       |
|                 1 reducesinkkey0 (type: int)       |
|               outputColumnNames: _col1, _col3      |
|               Select Operator                      |
|                 expressions: _col3 (type: string), _col1 (type: double) |
|                 outputColumnNames: _col0, _col1    |
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
|                 Statistics: Num rows: 1 Data size: 13 Basic stats: COMPLETE Column stats: NONE |
|                 File Output Operator               |
|                   compressed: false                |
|                   Statistics: Num rows: 1 Data size: 13 Basic stats: COMPLETE Column stats: NONE |
|                   table:                           |
|                       input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                       output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                       serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|       Local Work:                                  |
|         Map Reduce Local Work                      |
|                                                    |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: -1                                    |
|       Processor Tree:                              |
|         ListSink                                   |
|                                                    |
+----------------------------------------------------+
```

不启用

```sql
0: jdbc:hive2://zgg-server:10000> set hive.optimize.skewjoin=false;

0: jdbc:hive2://zgg-server:10000> select b.title, a.rating from ratings a join movies b on a.mid=b.mid;
...
INFO  : Total jobs = 1
INFO  : Launching Job 1 out of 1
...

0: jdbc:hive2://zgg-server:10000> explain select b.title, a.rating from ratings a join movies b on a.mid=b.mid;
INFO  : Compiling command(queryId=root_20240114064610_9a2ba483-3c60-4fce-8f36-04549629e466): explain select b.title, a.rating from ratings a join movies b on a.mid=b.mid
INFO  : No Stats for default@ratings, Columns: rating, mid
INFO  : No Stats for default@movies, Columns: mid, title
INFO  : Semantic Analysis Completed (retrial = false)
INFO  : Created Hive schema: Schema(fieldSchemas:[FieldSchema(name:Explain, type:string, comment:null)], properties:null)
INFO  : Completed compiling command(queryId=root_20240114064610_9a2ba483-3c60-4fce-8f36-04549629e466); Time taken: 2.166 seconds
INFO  : Concurrency mode is disabled, not creating a lock manager
INFO  : Executing command(queryId=root_20240114064610_9a2ba483-3c60-4fce-8f36-04549629e466): explain select b.title, a.rating from ratings a join movies b on a.mid=b.mid
INFO  : Starting task [Stage-3:EXPLAIN] in serial mode
INFO  : Completed executing command(queryId=root_20240114064610_9a2ba483-3c60-4fce-8f36-04549629e466); Time taken: 1.44 seconds
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
|             alias: a                               |
|             filterExpr: mid is not null (type: boolean) |
|             Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|             Filter Operator                        |
|               predicate: mid is not null (type: boolean) |
|               Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|               Select Operator                      |
|                 expressions: mid (type: int), rating (type: double) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|                   value expressions: _col1 (type: double) |
|           TableScan                                |
|             alias: b                               |
|             filterExpr: mid is not null (type: boolean) |
|             Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|             Filter Operator                        |
|               predicate: mid is not null (type: boolean) |
|               Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|               Select Operator                      |
|                 expressions: mid (type: int), title (type: string) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|                   value expressions: _col1 (type: string) |
|       Reduce Operator Tree:                        |
|         Join Operator                              |
|           condition map:                           |
|                Inner Join 0 to 1                   |
|           keys:                                    |
|             0 _col0 (type: int)                    |
|             1 _col0 (type: int)                    |
|           outputColumnNames: _col1, _col3          |
|           Statistics: Num rows: 1 Data size: 13 Basic stats: COMPLETE Column stats: NONE |
|           Select Operator                          |
|             expressions: _col3 (type: string), _col1 (type: double) |
|             outputColumnNames: _col0, _col1        |
|             Statistics: Num rows: 1 Data size: 13 Basic stats: COMPLETE Column stats: NONE |
|             File Output Operator                   |
|               compressed: false                    |
|               Statistics: Num rows: 1 Data size: 13 Basic stats: COMPLETE Column stats: NONE |
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

set hive.optimize.skewjoin.compiletime=true;

```sql
0: jdbc:hive2://zgg-server:10000> set hive.optimize.skewjoin.compiletime=true;

0: jdbc:hive2://zgg-server:10000> set hive.optimize.skewjoin=false;

0: jdbc:hive2://zgg-server:10000> explain select b.title, a.rating from ratings a join movies b on a.mid=b.mid;
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
|             alias: a                               |
|             filterExpr: mid is not null (type: boolean) |
|             Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|             Filter Operator                        |
|               predicate: mid is not null (type: boolean) |
|               Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|               Select Operator                      |
|                 expressions: mid (type: int), rating (type: double) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|                   value expressions: _col1 (type: double) |
|           TableScan                                |
|             alias: b                               |
|             filterExpr: mid is not null (type: boolean) |
|             Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|             Filter Operator                        |
|               predicate: mid is not null (type: boolean) |
|               Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|               Select Operator                      |
|                 expressions: mid (type: int), title (type: string) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 1 Data size: 188 Basic stats: COMPLETE Column stats: NONE |
|                   value expressions: _col1 (type: string) |
|       Reduce Operator Tree:                        |
|         Join Operator                              |
|           condition map:                           |
|                Inner Join 0 to 1                   |
|           keys:                                    |
|             0 _col0 (type: int)                    |
|             1 _col0 (type: int)                    |
|           outputColumnNames: _col1, _col3          |
|           Statistics: Num rows: 1 Data size: 13 Basic stats: COMPLETE Column stats: NONE |
|           Select Operator                          |
|             expressions: _col3 (type: string), _col1 (type: double) |
|             outputColumnNames: _col0, _col1        |
|             Statistics: Num rows: 1 Data size: 13 Basic stats: COMPLETE Column stats: NONE |
|             File Output Operator                   |
|               compressed: false                    |
|               Statistics: Num rows: 1 Data size: 13 Basic stats: COMPLETE Column stats: NONE |
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

[参考理解](https://blog.csdn.net/weixin_46389691/article/details/132620702)