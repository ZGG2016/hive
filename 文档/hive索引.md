# hive 索引

详细描述 [点这里](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Indexes.md)

Create then build, show formatted (with column names), and drop index:

```sql
hive (tags_dat)> create index tbl_orders_2_index on table tbl_orders_2(id) as 'COMPACT' WITH DEFERRED REBUILD;
OK
Time taken: 0.328 seconds

hive (tags_dat)> ALTER INDEX tbl_orders_2_index on tbl_orders_2 rebuild;
Query ID = root_20231203101212_4dc4e002-90a8-4501-8db9-14d66639acaa
Total jobs = 1
Launching Job 1 out of 1
Number of reduce tasks not specified. Estimated from input data size: 1
In order to change the average load for a reducer (in bytes):
  set hive.exec.reducers.bytes.per.reducer=<number>
In order to limit the maximum number of reducers:
  set hive.exec.reducers.max=<number>
In order to set a constant number of reducers:
  set mapreduce.job.reduces=<number>
Starting Job = job_1701568640867_0004, Tracking URL = http://bigdata-cdh01.itcast.cn:8088/proxy/application_1701568640867_0004/
Kill Command = /export/servers/hadoop/bin/hadoop job  -kill job_1701568640867_0004
Hadoop job information for Stage-1: number of mappers: 1; number of reducers: 1
2023-12-03 10:12:20,500 Stage-1 map = 0%,  reduce = 0%
2023-12-03 10:12:33,179 Stage-1 map = 100%,  reduce = 0%, Cumulative CPU 8.45 sec
2023-12-03 10:12:39,405 Stage-1 map = 100%,  reduce = 100%, Cumulative CPU 11.44 sec
MapReduce Total cumulative CPU time: 11 seconds 440 msec
Ended Job = job_1701568640867_0004
Loading data to table tags_dat.tags_dat__tbl_orders_2_tbl_orders_2_index__
Table tags_dat.tags_dat__tbl_orders_2_tbl_orders_2_index__ stats: [numFiles=1, numRows=120125, totalSize=12607109, rawDataSize=12486984]
MapReduce Jobs Launched: 
Stage-Stage-1: Map: 1  Reduce: 1   Cumulative CPU: 11.44 sec   HDFS Read: 93732211 HDFS Write: 12607227 SUCCESS
Total MapReduce CPU Time Spent: 11 seconds 440 msec
OK
Time taken: 26.642 seconds

hive (tags_dat)> show formatted index on tbl_orders_2;
OK
idx_name	tab_name	col_names	idx_tab_name	idx_type	comment
idx_name            	tab_name            	col_names           	idx_tab_name        	idx_type            	comment             
	 	 	 	 	 
	 	 	 	 	 
tbl_orders_2_index  	tbl_orders_2        	id                  	tags_dat__tbl_orders_2_tbl_orders_2_index__	compact             	
Time taken: 0.054 seconds, Fetched: 4 row(s)

hive (tags_dat)> drop index tbl_orders_2_index on tbl_orders_2;
OK
Time taken: 0.196 seconds
```

Create bitmap index, build, show, and drop:

```sql
hive (tags_dat)> create index tbl_orders_2_index on table tbl_orders_2(id) as 'bitmap' WITH DEFERRED REBUILD;
OK
Time taken: 0.162 seconds

hive (tags_dat)> ALTER INDEX tbl_orders_2_index on tbl_orders_2 rebuild;

hive (tags_dat)> show formatted index on tbl_orders_2;
OK
idx_name	tab_name	col_names	idx_tab_name	idx_type	comment
idx_name            	tab_name            	col_names           	idx_tab_name        	idx_type            	comment             
	 	 	 	 	 
	 	 	 	 	 
tbl_orders_2_index  	tbl_orders_2        	id                  	tags_dat__tbl_orders_2_tbl_orders_2_index__	bitmap              	
Time taken: 0.037 seconds, Fetched: 4 row(s)
```

Create index in a new table:  【idx_tab_name字段】

```sql
hive (tags_dat)> create index tbl_orders_2_index on table tbl_orders_2(id) as 'bitmap' WITH DEFERRED REBUILD in table tbl_orders_2_index_table;
OK
Time taken: 0.125 seconds
hive (tags_dat)> show formatted index on tbl_orders_2;
OK
idx_name	tab_name	col_names	idx_tab_name	idx_type	comment
idx_name            	tab_name            	col_names           	idx_tab_name        	idx_type            	comment             
	 	 	 	 	 
	 	 	 	 	 
tbl_orders_2_index  	tbl_orders_2        	id                  	tbl_orders_2_index_table	bitmap              	
Time taken: 0.024 seconds, Fetched: 4 row(s)
```
