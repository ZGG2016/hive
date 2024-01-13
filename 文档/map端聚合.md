# map端聚合

[TOC]

- hive.map.aggr

	Whether to use map-side aggregation in Hive Group By queries.

	默认为 true，Hive 将在 map task 中进行本地聚合

	这通常提供更好的效率，但要求更多的内存


```sql
create table t(id int, name string, salary double) row format delimited fields terminated by ",";

root@zgg-server:~/data# cat /root/data/t.txt
1,a,1000
1,b,2000
1,c,3000
1,d,1000
1,e,6000
1,f,2000

0: jdbc:hive2://zgg-server:10000> load data local inpath '/root/data/t.txt' into table t;

0: jdbc:hive2://zgg-server:10000> explain ANALYZE select id, sum(salary) from t group by id;
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
|             alias: t                               |
|             Statistics: Num rows: 1/6 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|             Select Operator                        |
|               expressions: id (type: int), salary (type: double) |
|               outputColumnNames: id, salary        |
|               Statistics: Num rows: 1/6 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|               Reduce Output Operator               |
|                 key expressions: id (type: int)    |
|                 null sort order: z                 |
|                 sort order: +                      |
|                 Map-reduce partition columns: id (type: int) |
|                 Statistics: Num rows: 1/6 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|                 value expressions: salary (type: double) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Group By Operator                          |
|           aggregations: sum(VALUE._col0)           |
|           keys: KEY._col0 (type: int)              |
|           mode: complete                           |
|           outputColumnNames: _col0, _col1          |
|           Statistics: Num rows: 1/1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|           File Output Operator                     |
|             compressed: false                      |
|             Statistics: Num rows: 1/1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
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


0: jdbc:hive2://zgg-server:10000> set hive.map.aggr=true;

0: jdbc:hive2://zgg-server:10000> explain ANALYZE select id, sum(salary) from t group by id;
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
|             alias: t                               |
|             Statistics: Num rows: 1/6 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|             Select Operator                        |
|               expressions: id (type: int), salary (type: double) |
|               outputColumnNames: id, salary        |
|               Statistics: Num rows: 1/6 Data size: 12 Basic stats: COMPLETE Column stats: NONE |    【√】
|               Group By Operator                    |                                                【√】
|                 aggregations: sum(salary)          |
|                 keys: id (type: int)               |
|                 minReductionHashAggr: 0.99         |
|                 mode: hash                         |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 1/1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |   【√】  实际行变成了1
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 1/1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|                   value expressions: _col1 (type: double) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Group By Operator                          |
|           aggregations: sum(VALUE._col0)           |
|           keys: KEY._col0 (type: int)              |
|           mode: mergepartial                       |
|           outputColumnNames: _col0, _col1          |
|           Statistics: Num rows: 1/1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|           File Output Operator                     |
|             compressed: false                      |
|             Statistics: Num rows: 1/1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
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


0: jdbc:hive2://zgg-server:10000> explain ANALYZE select sum(salary) from t;
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
|             alias: t                               |
|             Statistics: Num rows: 1/6 Data size: 8 Basic stats: COMPLETE Column stats: NONE |
|             Select Operator                        |
|               expressions: salary (type: double)   |
|               outputColumnNames: salary            |
|               Statistics: Num rows: 1/6 Data size: 8 Basic stats: COMPLETE Column stats: NONE |
|               Group By Operator                    |
|                 aggregations: sum(salary)          |
|                 minReductionHashAggr: 0.99         |
|                 mode: hash                         |
|                 outputColumnNames: _col0           |
|                 Statistics: Num rows: 1/1 Data size: 16 Basic stats: COMPLETE Column stats: NONE |
|                 Reduce Output Operator             |
|                   null sort order:                 |
|                   sort order:                      |
|                   Statistics: Num rows: 1/1 Data size: 16 Basic stats: COMPLETE Column stats: NONE |
|                   value expressions: _col0 (type: double) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Group By Operator                          |
|           aggregations: sum(VALUE._col0)           |
|           mode: mergepartial                       |
|           outputColumnNames: _col0                 |
|           Statistics: Num rows: 1/1 Data size: 16 Basic stats: COMPLETE Column stats: NONE |
|           File Output Operator                     |
|             compressed: false                      |
|             Statistics: Num rows: 1/1 Data size: 16 Basic stats: COMPLETE Column stats: NONE |
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


## 深度理解  [???]

[此文章详细解释了map端聚合机制](http://dev.bizo.com/2013/02/map-side-aggregations-in-apache-hive.html)

- hive.groupby.mapaggr.checkinterval

	Default Value: 100000

	Number of rows after which size of the grouping keys/aggregation classes is performed.

- hive.map.aggr.hash.min.reduction

	Default Value: 0.5  【减少量】

	Hash aggregation will be turned off if the ratio between hash table size and input rows is bigger than this number. Set to 1 to make sure hash aggregation is never turned off.

	聚合后的数据量越大，那么表示执行map端聚合的效果就不好

	聚合后的数据量/输入数据量 超过了这个数值，就表示执行map端聚合的效果没有满足要求（减少传输数据量的要求），那么就不需要执行map端聚合

	反过来看，聚合后的数据量/输入数据量 低于这个数值 就有必要执行map端聚合

	所以，值设定的越高，就更容易执行map端聚合；值设定的越低，就更不容易执行map端聚合

	所以，设定为1表示肯定要执行map端聚合；设定为0表示不需要执行map端聚合

	评估 使用的数据就是 hive.groupby.mapaggr.checkinterval 条

	但是，上述两个配置项的设定，需要是具体数据情况而定。

	【以上，根据一部分数据的聚合效果，来决定做不做map端聚合】

	-- hive4.0
	0: jdbc:hive2://zgg-server:10000> set hive.map.aggr.hash.min.reduction;
	+----------------------------------------+
	|                  set                   |
	+----------------------------------------+
	| hive.map.aggr.hash.min.reduction=0.99  |
	+----------------------------------------+

- hive.map.aggr.hash.force.flush.memory.threshold

	Default Value: 0.9

	The maximum memory to be used by map-side group aggregation hash table. If the memory usage is higher than this number, force to flush data.

- hive.map.aggr.hash.percentmemory

	Default Value: 0.5

	Portion of total memory to be used by map-side group aggregation hash table.


```sql
0: jdbc:hive2://zgg-server:10000> set hive.groupby.mapaggr.checkinterval=6;

0: jdbc:hive2://zgg-server:10000> set hive.map.aggr.hash.min.reduction=0;

-- 应该不执行map端聚合？？？
0: jdbc:hive2://zgg-server:10000> explain ANALYZE select id, sum(salary) from t group by id;
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
|             alias: t                               |
|             Statistics: Num rows: 1/6 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|             Select Operator                        |
|               expressions: id (type: int), salary (type: double) |
|               outputColumnNames: id, salary        |
|               Statistics: Num rows: 1/6 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|               Group By Operator                    |
|                 aggregations: sum(salary)          |
|                 keys: id (type: int)               |
|                 minReductionHashAggr: 0.0          |
|                 mode: hash                         |
|                 outputColumnNames: _col0, _col1    |
|                 Statistics: Num rows: 1/1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|                 Reduce Output Operator             |
|                   key expressions: _col0 (type: int) |
|                   null sort order: z               |
|                   sort order: +                    |
|                   Map-reduce partition columns: _col0 (type: int) |
|                   Statistics: Num rows: 1/1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|                   value expressions: _col1 (type: double) |
|       Execution mode: vectorized                   |
|       Reduce Operator Tree:                        |
|         Group By Operator                          |
|           aggregations: sum(VALUE._col0)           |
|           keys: KEY._col0 (type: int)              |
|           mode: mergepartial                       |
|           outputColumnNames: _col0, _col1          |
|           Statistics: Num rows: 1/1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
|           File Output Operator                     |
|             compressed: false                      |
|             Statistics: Num rows: 1/1 Data size: 12 Basic stats: COMPLETE Column stats: NONE |
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