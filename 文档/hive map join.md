# hive map join

[TOC]

hive4.0

[点这里](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Joins.md) 查看 join 基础

[点这里](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Join%20Optimization.md) 查看 join 优化

## map join

如果被连接的表中只有一个表较小，则连接可以作为 map only job 执行

默认是开启map join

```sql
0: jdbc:hive2://localhost:10000> set hive.auto.convert.join;
+------------------------------+
|             set              |
+------------------------------+
| hive.auto.convert.join=true  |
+------------------------------+

0: jdbc:hive2://localhost:10000> set hive.auto.convert.join=false;

0: jdbc:hive2://localhost:10000> explain SELECT a.v1, b.v1 FROM a JOIN b ON (a.k1 = b.k1);  
...
|       Reduce Operator Tree:                        |
|         Join Operator                              |
|           condition map:                           |
|                Inner Join 0 to 1                   |
|           keys:                                    |
|             0 _col0 (type: int)                    |
|             1 _col0 (type: int)                    |


-- 应该使用配置项，而不是hint
--0: jdbc:hive2://localhost:10000> set hive.auto.convert.join=true;

-- 默认true，表示忽略mapjoin hint /*+ MAPJOIN(b) */
0: jdbc:hive2://localhost:10000> set hive.ignore.mapjoin.hint=false;

0: jdbc:hive2://localhost:10000> explain SELECT a.v1, b.v1 FROM a JOIN b ON (a.k1 = b.k1);  
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-4 is a root stage                          |
|   Stage-3 depends on stages: Stage-4               |
|   Stage-0 depends on stages: Stage-3               |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-4                                   |
|     Map Reduce Local Work                          |
|       Alias -> Map Local Tables:                   |
|         $hdt$_0:a                                  |
|           Fetch Operator                           |
|             limit: -1                              |
|       Alias -> Map Local Operator Tree:            |
|         $hdt$_0:a                                  |
|           TableScan                                |
|             alias: a                               |
|             filterExpr: k1 is not null (type: boolean) |
|             Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: k1 is not null (type: boolean) |
|               Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: k1 (type: int), v1 (type: int) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|                 HashTable Sink Operator            |
|                   keys:                            |
|                     0 _col0 (type: int)            |
|                     1 _col0 (type: int)            |
|                                                    |
|   Stage: Stage-3                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan                                |
|             alias: b                               |
|             filterExpr: k1 is not null (type: boolean) |
|             Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: k1 is not null (type: boolean) |
|               Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: k1 (type: int), v1 (type: int) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|                 Map Join Operator                  |
|                   condition map:                   |
|                        Inner Join 0 to 1           |
|                   keys:                            |
|                     0 _col0 (type: int)            |
|                     1 _col0 (type: int)            |
|                   outputColumnNames: _col1, _col3  |
|                   Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|                   Select Operator                  |
|                     expressions: _col1 (type: int), _col3 (type: int) |
|                     outputColumnNames: _col0, _col1 |
|                     Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|                     File Output Operator           |
|                       compressed: false            |
|                       Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|                       table:                       |
|                           input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                           output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                           serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|       Execution mode: vectorized                   |
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

限制是不能执行 a FULL/RIGHT OUTER JOIN b

```sql
0: jdbc:hive2://localhost:10000> explain SELECT a.v1, b.v1 FROM a full JOIN b ON (a.k1 = b.k1);  
...
|       Reduce Operator Tree:                        |
|         Join Operator                              |
|           condition map:                           |
|                Full Outer Join 0 to 1              |
|           keys:                                    |
|             0 _col0 (type: int)                    |
|             1 _col0 (type: int)                    |

0: jdbc:hive2://zgg-server:10000> explain SELECT a.v1, b.v1 FROM a right JOIN b ON (a.k1 = b.k1);
...
|           TableScan                                |
|             alias: a
...
|                 HashTable Sink Operator            |
|                   keys:                            |
|                     0 _col0 (type: int)            |
|                     1 _col0 (type: int)            |
...
|               Map Join Operator                    |
|                 condition map:                     |
|                      Right Outer Join 0 to 1       |

-- 但在执行的时候出错
0: jdbc:hive2://zgg-server:10000>  SELECT a.v1, b.v1 FROM a right outer JOIN b ON (a.k1 = b.k1);
ERROR : Execution failed with exit status: 1
ERROR : Obtaining error information
ERROR : 
Task failed!
Task ID:
  Stage-4

Logs:

ERROR : /tmp/root/hive.log
ERROR : FAILED: Execution Error, return code 1 from org.apache.hadoop.hive.ql.exec.mr.MapredLocalTask
```

对于内连接，在 join 的每个 map/reduce 阶段，序列中的最后一个表通过 reducers 流动，而其他表被缓存。【下例中，表a被广播缓存，表b在各自的内存中join表a】

因此，通过组织表，将最大的表出现在序列的最后，它有助于减少 reducer 中为连接键的特定值而缓冲行所需的内存。【把被广播的表（小表）放前面，大表放后面】

```sql
0: jdbc:hive2://zgg-server:10000> explain SELECT a.v1, b.v1 FROM a JOIN b ON (a.k1 = b.k1);
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-4 is a root stage                          |
|   Stage-3 depends on stages: Stage-4               |
|   Stage-0 depends on stages: Stage-3               |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-4                                   |
|     Map Reduce Local Work                          |
|       Alias -> Map Local Tables:                   |
|         $hdt$_0:a                                  |
|           Fetch Operator                           |
|             limit: -1                              |
|       Alias -> Map Local Operator Tree:            |
|         $hdt$_0:a                                  |
|           TableScan                                |
|             alias: a                               |
|             filterExpr: k1 is not null (type: boolean) |
|             Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: k1 is not null (type: boolean) |
|               Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: k1 (type: int), v1 (type: int) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|                 HashTable Sink Operator            |
|                   keys:                            |
|                     0 _col0 (type: int)            |
|                     1 _col0 (type: int)            |
|                                                    |
|   Stage: Stage-3                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan                                |
|             alias: b                               |
|             filterExpr: k1 is not null (type: boolean) |
|             Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: k1 is not null (type: boolean) |
|               Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: k1 (type: int), v1 (type: int) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|                 Map Join Operator                  |
|                   condition map:                   |
|                        Inner Join 0 to 1           |
|                   keys:                            |
|                     0 _col0 (type: int)            |
|                     1 _col0 (type: int)            |
|                   outputColumnNames: _col1, _col3  |
|                   Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|                   Select Operator                  |
|                     expressions: _col1 (type: int), _col3 (type: int) |
|                     outputColumnNames: _col0, _col1 |
|                     Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|                     File Output Operator           |
|                       compressed: false            |
|                       Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|                       table:                       |
|                           input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                           output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                           serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|       Execution mode: vectorized                   |
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

对于内连接，对于多表join：

对于 `SELECT a.v1, b.v1, c.v1 FROM a JOIN b ON (a.k1 = b.k1) JOIN c ON (c.k1 = b.k1);` 语句：

- 上述语句在一个 map/reduce job 中执行，那么表 a 和表 b 的键的特定值被（广播）缓存在 reducers 的内存中。然后，对于从 c 检索到的每一行，使用缓存的行进行计算连接。

- 上述语句在两个 map/reduce jobs 中执行。第一个 job 连接 a 和 b，并（广播）缓存 a 的值，同时 b 的值在 reducer 中流动。其中的第二个 job （广播）缓存第一个连接的结果，同时 c 的值在 reducer 中流动。【？？？在下例中，广播缓存的是c】

```sql

0: jdbc:hive2://zgg-server:10000> explain SELECT a.v1, b.v1, c.v1 FROM a JOIN b ON (a.k1 = b.k1) JOIN c ON (c.k1 = b.k1);
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-7 is a root stage                          |
|   Stage-5 depends on stages: Stage-7               |
|   Stage-0 depends on stages: Stage-5               |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-7                                   |
|     Map Reduce Local Work                          |
|       Alias -> Map Local Tables:                   |
|         $hdt$_1:a                                  |
|           Fetch Operator                           |
|             limit: -1                              |
|         $hdt$_2:c                                  |
|           Fetch Operator                           |
|             limit: -1                              |
|       Alias -> Map Local Operator Tree:            |
|         $hdt$_1:a                                  |
|           TableScan                                |
|             alias: a                               |
|             filterExpr: k1 is not null (type: boolean) |
|             Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: k1 is not null (type: boolean) |
|               Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: k1 (type: int), v1 (type: int) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|                 HashTable Sink Operator            |
|                   keys:                            |
|                     0 _col0 (type: int)            |
|                     1 _col0 (type: int)            |
|         $hdt$_2:c                                  |
|           TableScan                                |
|             alias: c                               |
|             filterExpr: k1 is not null (type: boolean) |
|             Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: k1 is not null (type: boolean) |
|               Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: k1 (type: int), v1 (type: int) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|                 HashTable Sink Operator            |
|                   keys:                            |
|                     0 _col0 (type: int)            |
|                     1 _col0 (type: int)            |
|                                                    |
|   Stage: Stage-5                                   |
|     Map Reduce                                     |
|       Map Operator Tree:                           |
|           TableScan                                |
|             alias: b                               |
|             filterExpr: k1 is not null (type: boolean) |
|             Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|             Filter Operator                        |
|               predicate: k1 is not null (type: boolean) |
|               Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|               Select Operator                      |
|                 expressions: k1 (type: int), v1 (type: int) |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|                 Map Join Operator                  |
|                   condition map:                   |
|                        Inner Join 0 to 1           |
|                   keys:                            |
|                     0 _col0 (type: int)            |
|                     1 _col0 (type: int)            |
|                   outputColumnNames: _col0, _col1, _col3 |
|                   Statistics: Num rows: 6 Data size: 72 Basic stats: COMPLETE Column stats: COMPLETE |
|                   Map Join Operator                |
|                     condition map:                 |
|                          Inner Join 0 to 1         |
|                     keys:                          |
|                       0 _col0 (type: int)          |
|                       1 _col0 (type: int)          |
|                     outputColumnNames: _col1, _col3, _col5 |
|                     Statistics: Num rows: 6 Data size: 72 Basic stats: COMPLETE Column stats: COMPLETE |
|                     Select Operator                |
|                       expressions: _col3 (type: int), _col1 (type: int), _col5 (type: int) |
|                       outputColumnNames: _col0, _col1, _col2 |
|                       Statistics: Num rows: 6 Data size: 72 Basic stats: COMPLETE Column stats: COMPLETE |
|                       File Output Operator         |
|                         compressed: false          |
|                         Statistics: Num rows: 6 Data size: 72 Basic stats: COMPLETE Column stats: COMPLETE |
|                         table:                     |
|                             input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                             output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                             serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|       Execution mode: vectorized                   |
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

对于外连接类型，广播缓存的表是：

- left join: 右侧表
- right join: 左侧表
- full outer join: 根本不能转换为 map-join

可以将 inner join 转换为 map-join，但是 outer join 不能，因为只有当需要流动的表以外的表能够适合于大小配置（下面的noconditionaltask配置）时，才能转换 outer join

```sql
0: jdbc:hive2://zgg-server:10000> explain SELECT a.v1, b.v1 FROM a left JOIN b ON (a.k1 = b.k1);
...
|           TableScan                                |
|             alias: b                               |
...
|                 HashTable Sink Operator            |
|                   keys:                            |
|                     0 _col0 (type: int)            |
|                     1 _col0 (type: int)            |
|                                                    |
...
+----------------------------------------------------+

0: jdbc:hive2://zgg-server:10000> explain SELECT a.v1, b.v1 FROM a right JOIN b ON (a.k1 = b.k1);
...
|           TableScan                                |
|             alias: a                               |
...
|                 HashTable Sink Operator            |
|                   keys:                            |
|                     0 _col0 (type: int)            |
|                     1 _col0 (type: int)            |
|                                                    |


0: jdbc:hive2://zgg-server:10000> explain SELECT a.v1, b.v1 FROM a full JOIN b ON (a.k1 = b.k1);
...
|           TableScan                                |
|             alias: a                               |
|             Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|             Select Operator                        |
|               expressions: k1 (type: int), v1 (type: int) |
|               outputColumnNames: _col0, _col1      |
|               Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|               Reduce Output Operator               |
|                 key expressions: _col0 (type: int) |
|                 null sort order: z                 |
|                 sort order: +                      |
|                 Map-reduce partition columns: _col0 (type: int) |
|                 Statistics: Num rows: 3 Data size: 24 Basic stats: COMPLETE Column stats: COMPLETE |
|                 value expressions: _col1 (type: int) |
|           TableScan                                |
|             alias: b                               |
|             Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|             Select Operator                        |
|               expressions: k1 (type: int), v1 (type: int) |
|               outputColumnNames: _col0, _col1      |
|               Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|               Reduce Output Operator               |
|                 key expressions: _col0 (type: int) |
|                 null sort order: z                 |
|                 sort order: +                      |
|                 Map-reduce partition columns: _col0 (type: int) |
|                 Statistics: Num rows: 6 Data size: 48 Basic stats: COMPLETE Column stats: COMPLETE |
|                 value expressions: _col1 (type: int) |
|       Reduce Operator Tree:                        |
|         Join Operator                              |
|           condition map:                           |
|                Full Outer Join 0 to 1              |
|           keys:                                    |
|             0 _col0 (type: int)                    |
|             1 _col0 (type: int)                    |
```

mapjoin不支持的一些特性（下面仅列出部分）

```sql
-- Union Followed by a MapJoin
-- MapJoin Followed by Union
select a.*,b.* from tb_ratings a join tb_users_2 b on a.uid=b.uid where b.uid=1
union all
select a.*,b.* from tb_ratings a join tb_users_2 b on a.uid=b.uid where b.uid=2;
...
ERROR : Execution failed with exit status: 1
ERROR : Obtaining error information
ERROR : 
Task failed!
Task ID:
  Stage-8

Logs:

ERROR : /tmp/root/hive.log
ERROR : FAILED: Execution Error, return code 1 from org.apache.hadoop.hive.ql.exec.mr.MapredLocalTask
INFO  : Completed executing command(queryId=root_20240113022228_cd0ac22b-6aa3-471c-8f1c-108414695bc4); Time taken: 5.464 seconds
Error: Error while compiling statement: FAILED: Execution Error, return code 1 from org.apache.hadoop.hive.ql.exec.mr.MapredLocalTask (state=08S01,code=1)
```

```sql
-- Reduce Sink (Group By/Join/Sort By/Cluster By/Distribute By) Followed by MapJoin
select uid, avg(rating) from (select a.*,b.* from tb_ratings a join tb_users_2 b on a.uid=b.uid) t group by uid;

-- MapJoin Followed by MapJoin
select firstjoin.*, c.* FROM
  (select a.uid, b.gender, a.mid, a.rating
      FROM tb_ratings a JOIN tb_users_2 b ON a.uid=b.uid) firstjoin 
  JOIN tb_users_2 c ON (firstjoin.uid=c.uid);                                           
```

- hive.smalltable.filesize or hive.mapjoin.smalltable.filesize

	Default Value: 25000000
	
	Added In: Hive 0.7.0 with HIVE-1642: hive.smalltable.filesize (replaced by hive.mapjoin.smalltable.filesize in Hive 0.8.1)
	
	Added In: Hive 0.8.1 with HIVE-2499: hive.mapjoin.smalltable.filesize
	
	The threshold (in bytes) for the input file size of the small tables; if the file size is smaller than this threshold, it will try to convert the common join into map join.

	小表的输入文件大小的阈值。

	如果文件大小小于这个阈值，将尝试将普通join转为map join

- hive.auto.convert.join.noconditionaltask

	Default Value: true
	
	Whether Hive enables the optimization about converting common join into mapjoin based on the input file size. If this parameter is on, and the sum of size for n-1 of the tables/partitions for an n-way join is smaller than the size specified by `hive.auto.convert.join.noconditionaltask.size`, the join is directly converted to a mapjoin (there is no conditional task).

	根据输入文件大小，启用普通join转为mapjoin

	只有 hive.auto.convert.join.noconditionaltask 和 hive.auto.convert.join 同时为 true 时，才执行 mapjoin

- hive.auto.convert.join.noconditionaltask.size

	Default Value: 10000000

	If `hive.auto.convert.join.noconditionaltask` is off, this parameter does not take effect. However, if it is on, and the sum of size for n-1 of the tables/partitions for an n-way join is smaller than this size, the join is directly converted to a mapjoin (there is no conditional task). The default is 10MB.

	假设参与join的表(或分区)有N个，如果启用 `hive.auto.convert.join.noconditionaltask` 参数，并且有N-1个表(或分区)的大小总和小于 `hive.auto.convert.join.noconditionaltask.size` 参数指定的值，那么会直接将join转为Map join

理解 hive.auto.convert.join.noconditionaltask.size 配置项

```sql
0: jdbc:hive2://zgg-server:10000> set hive.auto.convert.join.noconditionaltask.size=10;

0: jdbc:hive2://zgg-server:10000> explain select b.uid, a.mid FROM tb_ratings a JOIN tb_users_2 b ON a.uid=b.uid;
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-5 is a root stage , consists of Stage-6, Stage-7, Stage-1 |
|   Stage-6 has a backup stage: Stage-1              |
|   Stage-3 depends on stages: Stage-6               |
|   Stage-7 has a backup stage: Stage-1              |
|   Stage-4 depends on stages: Stage-7               |
|   Stage-1      【Join Operator】                    |
|   Stage-0 depends on stages: Stage-3, Stage-4, Stage-1 |
....

0: jdbc:hive2://zgg-server:10000> explain select b.uid, a.mid FROM tb_ratings a JOIN tb_users_2 b ON a.uid=b.uid;
INFO  : Starting task [Stage-5:CONDITIONAL] in serial mode
INFO  : Stage-6 is selected by condition resolver.
INFO  : Stage-7 is filtered out by condition resolver.
INFO  : Stage-1 is filtered out by condition resolver.
INFO  : Starting task [Stage-6:MAPREDLOCAL] in serial mode
ERROR : Execution failed with exit status: 1
ERROR : Obtaining error information
ERROR : 
Task failed!
Task ID:
  Stage-6

Logs:

ERROR : /tmp/root/hive.log
ERROR : FAILED: Execution Error, return code 1 from org.apache.hadoop.hive.ql.exec.mr.MapredLocalTask
ERROR : ATTEMPT: Execute BackupTask: org.apache.hadoop.hive.ql.exec.mr.MapRedTask
INFO  : Launching Job 2 out of 3
INFO  : Starting task [Stage-1:MAPRED] in serial mode
...
```

## Sort-Merge-Bucket (SMB) joins

[先阅读这里](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Join%20Optimization.md#2322auto-conversion-to-smb-map-join)

如果要连接的表在连接列上进行分桶，并且一个表中的桶数是另一个表中桶数的倍数，那么这些桶可以相互连接。

```sql
0: jdbc:hive2://localhost:10000> show create table order_table_buckets_2;
+----------------------------------------------------+
|                   createtab_stmt                   |
+----------------------------------------------------+
| CREATE EXTERNAL TABLE `order_table_buckets_2`(     |
|   `order_id` int,                                  |
|   `product_name` string,                           |
|   `price` int)                                     |
| PARTITIONED BY (                                   |
|   `deal_day` string)                               |
| CLUSTERED BY (                                     |
|   order_id)                                        |
| SORTED BY (                                        |
|   price ASC)                                       |
| INTO 4 BUCKETS           
...

0: jdbc:hive2://localhost:10000> show create table order_table_buckets;
+----------------------------------------------------+
|                   createtab_stmt                   |
+----------------------------------------------------+
| CREATE EXTERNAL TABLE `order_table_buckets_2`(     |
|   `order_id` int,                                  |
|   `product_name` string,                           |
|   `price` int)                                     |
| PARTITIONED BY (                                   |
|   `deal_day` string)                               |
| CLUSTERED BY (                                     |
|   order_id)                                        |
| SORTED BY (                                        |
|   price ASC)                                       |
| INTO 4 BUCKETS           
...

-- 设置这个属性，那么处理 A 的桶 1 的 mapper 只会获取 b 的桶 1
0: jdbc:hive2://localhost:10000> set hive.optimize.bucketmapjoin = true;

0: jdbc:hive2://localhost:10000> select a.*,b.* from order_table_buckets a join order_table_buckets_2 b on a.order_id=b.order_id;
+-------------+-----------------+----------+-------------+-------------+-----------------+----------+-------------+
| a.order_id  | a.product_name  | a.price  | a.deal_day  | b.order_id  | b.product_name  | b.price  | b.deal_day  |
+-------------+-----------------+----------+-------------+-------------+-----------------+----------+-------------+
| 9           | book            | 30       | 201901      | 9           | book            | 30       | 201901      |
| 11          | book4           | 45       | 201901      | 11          | book4           | 45       | 201901      |
| 13          | book6           | 145      | 201901      | 13          | book6           | 145      | 201901      |
| 8           | liquor          | 150      | 201901      | 8           | liquor          | 150      | 201901      |
| 1           | bicycle         | 1000     | 201901      | 1           | bicycle         | 1000     | 201901      |
| 4           | tv              | 3000     | 201901      | 4           | tv              | 3000     | 201901      |
| 6           | banana          | 8        | 201901      | 6           | banana          | 8        | 201901      |
| 10          | book2           | 40       | 201901      | 10          | book2           | 40       | 201901      |
| 7           | milk            | 70       | 201901      | 7           | milk            | 70       | 201901      |
| 2           | truck           | 20000    | 201901      | 2           | truck           | 20000    | 201901      |
| 5           | apple           | 10       | 201901      | 5           | apple           | 10       | 201901      |
| 12          | book5           | 75       | 201901      | 12          | book5           | 75       | 201901      |
| 3           | cellphone       | 2000     | 201901      | 3           | cellphone       | 2000     | 201901      |
+-------------+-----------------+----------+-------------+-------------+-----------------+----------+-------------+

```

如果要连接的表在连接列上进行了排序和存储，并且它们具有相同数量的桶，则可以执行 sort-merge join。对应的桶在 mapper 上相互连接

```sql
0: jdbc:hive2://localhost:10000> set hive.input.format=org.apache.hadoop.hive.ql.io.BucketizedHiveInputFormat;

0: jdbc:hive2://localhost:10000> set hive.optimize.bucketmapjoin = true;

0: jdbc:hive2://localhost:10000> set hive.optimize.bucketmapjoin.sortedmerge = true;

select a.*,b.* from order_table_buckets a join (select * from order_table_buckets_2 order by order_id)b on a.order_id=b.order_id;
```

