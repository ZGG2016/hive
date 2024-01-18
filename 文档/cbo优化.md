# cbo优化

根据统计信息，选择代价最小的执行计划

- hive.cbo.enable

	Default Value: false in `Hive 0.14.*`; true in Hive 1.1.0 and later ( HIVE-8395 )

	When true, the [cost based optimizer](https://cwiki.apache.org/confluence/display/Hive/Cost-based+optimization+in+Hive), which uses the Calcite framework, will be enabled.

	启用基于成本的优化器


其他相关配置项 [点这里]()


```sql
0: jdbc:hive2://zgg-server:10000> set hive.compute.query.using.stats=true;
0: jdbc:hive2://zgg-server:10000> set hive.stats.fetch.column.stats=true;
0: jdbc:hive2://zgg-server:10000> ANALYZE TABLE ratings COMPUTE STATISTICS FOR COLUMNS;

0: jdbc:hive2://zgg-server:10000> desc extended ratings;
...
parameters:{numRows=25000096, rawDataSize=628260795, totalSize=678260987, COLUMN_STATS_ACCURATE={\"BASIC_STATS\":\"true\",\"COLUMN_STATS\":{\"mid\":\"true\",\"rating\":\"true\",\"rating_time\":\"true\",\"uid\":\"true\"}}, numFiles=1, 
...

0: jdbc:hive2://zgg-server:10000> desc formatted ratings rating;
+------------------------+----------------------------------------------------+
|    column_property     |                       value                        |
+------------------------+----------------------------------------------------+
| col_name               | rating                                             |
| data_type              | double                                             |
| min                    | 0.5                                                |
| max                    | 5.0                                                |
| num_nulls              | 1                                                  |
| distinct_count         | 10                                                 |
| avg_col_len            |                                                    |
| max_col_len            |                                                    |
| num_trues              |                                                    |
| num_falses             |                                                    |
| bit_vector             |                                                    |
| comment                | from deserializer                                  |
| COLUMN_STATS_ACCURATE  | {\"BASIC_STATS\":\"true\",\"COLUMN_STATS\":{\"mid\":\"true\",\"rating\":\"true\",\"rating_time\":\"true\",\"uid\":\"true\"}} |
+------------------------+----------------------------------------------------+

-- TODO 为什么会执行mr job
0: jdbc:hive2://zgg-server:10000> select max(rating) from ratings;
...
INFO  : Query ID = root_20240116132700_c3d699eb-98d1-4a25-9887-39c52dac7e9d
INFO  : Total jobs = 1
INFO  : Launching Job 1 out of 1
...
INFO  : MapReduce Total cumulative CPU time: 28 seconds 510 msec
INFO  : Ended Job = job_1705404643694_0018
INFO  : MapReduce Jobs Launched: 
INFO  : Stage-Stage-1: Map: 3  Reduce: 1   Cumulative CPU: 28.51 sec   HDFS Read: 678313756 HDFS Write: 103 HDFS EC Read: 0 SUCCESS
INFO  : Total MapReduce CPU Time Spent: 28 seconds 510 msec
INFO  : Completed executing command(queryId=root_20240116132700_c3d699eb-98d1-4a25-9887-39c52dac7e9d); Time taken: 72.499 seconds
+------+
| _c0  |
+------+
| 5.0  |
+------+
1 row selected (72.784 seconds)
```