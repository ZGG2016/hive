# hive物化视图

[TOC]

## Create Materialized View

```sql
0: jdbc:hive2://zgg-server:10000> select * from t2;
+--------+----------+--------------+
| t2.id  | t2.name  | t2.deal_day  |
+--------+----------+--------------+
| 1      | aa       | 20230110     |
| 1      | aa       | 20230110     |
| 2      | bb       | 20230110     |
| 3      | cc       | 20230110     |
| 4      | dd       | 20230110     |
| 1      | aa       | 20230110     |
| 2      | bb       | 20230110     |
| 3      | cc       | 20230110     |
| 4      | dd       | 20230110     |
+--------+----------+--------------+

-- By default, materialized views are enabled to be used by the query optimizer for automatic rewriting when they are created.

create materialized view IF NOT EXISTS mv 
DISABLE REWRITE
comment 'materialized view test'
partitioned on(deal_day)
CLUSTERED ON (id) 
-- DISTRIBUTED ON (id) SORTED ON (id)
  ROW FORMAT delimited fields terminated by '\t'
  stored as textfile
as select * from t2 where deal_day='20230110';

0: jdbc:hive2://zgg-server:10000> desc formatted mv;
+-----------------------------------------------+----------------------------------------------------+-----------------------------+
|                   col_name                    |                     data_type                      |           comment           |
+-----------------------------------------------+----------------------------------------------------+-----------------------------+
| id                                            | int                                                |                             |
| name                                          | string                                             |                             |
|                                               | NULL                                               | NULL                        |
| # Partition Information                       | NULL                                               | NULL                        |
| # col_name                                    | data_type                                          | comment                     |
| deal_day                                      | string                                             |                             |
|                                               | NULL                                               | NULL                        |
| # Detailed Table Information                  | NULL                                               | NULL                        |
| Database:                                     | default                                            | NULL                        |
| OwnerType:                                    | USER                                               | NULL                        |
| Owner:                                        | root                                               | NULL                        |
| CreateTime:                                   | Fri Dec 29 08:17:33 UTC 2023                       | NULL                        |
| LastAccessTime:                               | Fri Dec 29 08:18:21 UTC 2023                       | NULL                        |
| Retention:                                    | 0                                                  | NULL                        |
| Location:                                     | hdfs://zgg-server:8020/user/hive/warehouse/mv      | NULL                        |
| Table Type:                                   | MATERIALIZED_VIEW                                  | NULL                        |
| Table Parameters:                             | NULL                                               | NULL                        |
|                                               | COLUMN_STATS_ACCURATE                              | {\"BASIC_STATS\":\"true\"}  |
|                                               | bucketing_version                                  | 2                           |
|                                               | comment                                            | materialized view test      |
|                                               | materializedview.distribute.columns                | [\"id\"]                    |
|                                               | materializedview.sort.columns                      | [\"id\"]                    |
|                                               | numFiles                                           | 1                           |
|                                               | numPartitions                                      | 1                           |
|                                               | numRows                                            | 9                           |
|                                               | rawDataSize                                        | 36                          |
|                                               | totalSize                                          | 45                          |
|                                               | transient_lastDdlTime                              | 1703837901                  |
|                                               | NULL                                               | NULL                        |
| # Storage Information                         | NULL                                               | NULL                        |
| SerDe Library:                                | org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe | NULL                        |
| InputFormat:                                  | org.apache.hadoop.mapred.TextInputFormat           | NULL                        |
| OutputFormat:                                 | org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat | NULL                        |
| Compressed:                                   | No                                                 | NULL                        |
| Num Buckets:                                  | -1                                                 | NULL                        |
| Bucket Columns:                               | []                                                 | NULL                        |
| Sort Columns:                                 | []                                                 | NULL                        |
|                                               | NULL                                               | NULL                        |
| # Materialized View Information               | NULL                                               | NULL                        |
| Original Query:                               | select * from t2 where deal_day='20230110'         | NULL                        |
| Expanded Query:                               | SELECT `id`, `name`, `deal_day` FROM (select `t2`.`id`, `t2`.`name`, `t2`.`deal_day` from `default`.`t2` where `t2`.`deal_day`='20230110') `mv` | NULL                        |
| Rewrite Enabled:                              | No                                                 | NULL                        |
| Outdated for Rewriting:                       | Unknown                                            | NULL                        |
|                                               | NULL                                               | NULL                        |
| # Materialized View Source table information  | NULL                                               | NULL                        |
| Table name                                    | I/U/D since last rebuild                           | NULL                        |
| hive.default.t2                               | 0/0/0                                              | NULL                        |
+-----------------------------------------------+----------------------------------------------------+-----------------------------+


0: jdbc:hive2://zgg-server:10000> select * from mv;
+--------+----------+--------------+
| mv.id  | mv.name  | mv.deal_day  |
+--------+----------+--------------+
| 1      | aa       | 20230110     |
| 1      | aa       | 20230110     |
| 1      | aa       | 20230110     |
| 2      | bb       | 20230110     |
| 2      | bb       | 20230110     |
| 3      | cc       | 20230110     |
| 3      | cc       | 20230110     |
| 4      | dd       | 20230110     |
| 4      | dd       | 20230110     |
+--------+----------+--------------+

-- 向t2插入一条记录
0: jdbc:hive2://zgg-server:10000> select * from t2;
+--------+-----------+--------------+
| t2.id  |  t2.name  | t2.deal_day  |
+--------+-----------+--------------+
| 1      | aa        | 20230110     |
| 1      | aa        | 20230110     |
| 2      | bb        | 20230110     |
| 3      | cc        | 20230110     |
| 4      | dd        | 20230110     |
| 1      | aa        | 20230110     |
| 2      | bb        | 20230110     |
| 3      | cc        | 20230110     |
| 4      | dd        | 20230110     |
| 5      | zhangsan  | 20230110     |
+--------+-----------+--------------+

-- 未同步更新
0: jdbc:hive2://zgg-server:10000> select * from mv;
+--------+----------+--------------+
| mv.id  | mv.name  | mv.deal_day  |
+--------+----------+--------------+
| 1      | aa       | 20230110     |
| 1      | aa       | 20230110     |
| 1      | aa       | 20230110     |
| 2      | bb       | 20230110     |
| 2      | bb       | 20230110     |
| 3      | cc       | 20230110     |
| 3      | cc       | 20230110     |
| 4      | dd       | 20230110     |
| 4      | dd       | 20230110     |
+--------+----------+--------------+
```

```
Error: Error while compiling statement: FAILED: SemanticException Automatic rewriting for materialized view cannot be enabled if the materialized view uses non-transactional tables (state=42000,code=40000)
```

## Drop Materialized View

移除物化视图的数据和元数据

```sql
0: jdbc:hive2://zgg-server:10000> drop materialized view mv;
```

## Alter Materialized View

Once a materialized view has been created, the optimizer will be able to exploit its definition semantics to automatically rewrite incoming queries using materialized views, and hence, accelerate query execution. 

By default, materialized views are enabled for rewriting at creation time.

```sql
0: jdbc:hive2://zgg-server:10000> alter materialized view mv enable REWRITE;
```

## Show Materialized Views

```sql
0: jdbc:hive2://zgg-server:10000> show materialized views in default;
+----------+------------------+-----------------+----------------------+
| mv_name  | rewrite_enabled  |      mode       | incremental_rebuild  |
+----------+------------------+-----------------+----------------------+
| mv       | No               | Manual refresh  | Unknown              |
| mv_2     | No               | Manual refresh  | Unknown              |
| mv_3     | No               | Manual refresh  | Unknown              |
+----------+------------------+-----------------+----------------------+
```

## Alter Materialized Views ... rebuild

在前面的例子中，在原表t2插入了一条数据，但是没有同步到物化视图mv中，此时就需要rebuild

```sql
0: jdbc:hive2://zgg-server:10000> alter materialized view mv rebuild;
0: jdbc:hive2://zgg-server:10000> select * from mv;
+--------+-----------+--------------+
| mv.id  |  mv.name  | mv.deal_day  |
+--------+-----------+--------------+
| 1      | aa        | 20230110     |
| 1      | aa        | 20230110     |
| 1      | aa        | 20230110     |
| 2      | bb        | 20230110     |
| 2      | bb        | 20230110     |
| 3      | cc        | 20230110     |
| 3      | cc        | 20230110     |
| 4      | dd        | 20230110     |
| 4      | dd        | 20230110     |
| 5      | zhangsan  | 20230110     |
+--------+-----------+--------------+
```

Current implementation only supports incremental rebuild when there were INSERT operations over the source tables, while UPDATE and DELETE operations will force a full rebuild of the materialized view.

To execute incremental maintenance, following conditions should be met:

- The materialized view should only use transactional tables, either micromanaged or ACID.

- If the materialized view definition contains a Group By clause, the materialized view should be stored in an ACID table, since it needs to support MERGE operation. For materialized view definitions consisting of Scan-Project-Filter-Join, this restriction does not exist.  

A rebuild operation acquires an exclusive write lock over the materialized view, i.e., for a given materialized view, only one rebuild operation can be executed at a given time.

## 理解 ENABLE REWRITE

```sql
create table t3(id int, name string)
STORED AS ORC
TBLPROPERTIES ("transactional"="true");

insert into t3 values(1,'aa'),(2,'bb'),(3,'cc');

create table t4(id int, price double)
partitioned by(deal_day string)
STORED AS ORC
TBLPROPERTIES ("transactional"="true");

insert into t4 partition(deal_day='20230110') values(1,1000.10),(2,201.12),(3,223.23),(1,232.10),(2,341.12);

-- 报错Error while compiling statement: FAILED: Execution Error, return code 1 from org.apache.hadoop.hive.ql.exec.mr.MapredLocalTask
-- 取消map join:  SET hive.auto.convert.join=false;
create materialized view mv2
stored as orc 
as 
select t3.id, t3.name, sum(price) amount
from t3, t4
where t3.id=t4.id and t4.deal_day='20230110' 
group by t3.id, t3.name;

-- 计算总金额
0: jdbc:hive2://zgg-server:10000> select id, name, amount from mv2;
+-----+-------+---------+
| id  | name  | amount  |
+-----+-------+---------+
| 1   | aa    | 1232.2  |
| 2   | bb    | 542.24  |
| 3   | cc    | 223.23  |
+-----+-------+---------+
3 rows selected (0.499 seconds)


0: jdbc:hive2://zgg-server:10000> select id, name, sum(price) amount from t3, t4 where t3.id=t4.id and t4.deal_day='20230110' group by t3.id, t3.name;
INFO  : Compiling command(queryId=root_20231229095844_2385a5fb-dc55-4159-a957-59d615aff2f4): select id, name, sum(price) amount from t3, t4 where t3.id=t4.id and t4.deal_day='20230110' group by t3.id, t3.name
INFO  : Semantic Analysis Completed (retrial = false)
INFO  : Created Hive schema: Schema(fieldSchemas:[FieldSchema(name:id, type:int, comment:null), FieldSchema(name:name, type:string, comment:null), FieldSchema(name:amount, type:double, comment:null)], properties:null)
INFO  : Completed compiling command(queryId=root_20231229095844_2385a5fb-dc55-4159-a957-59d615aff2f4); Time taken: 0.891 seconds
INFO  : Operation QUERY obtained 1 locks
INFO  : Executing command(queryId=root_20231229095844_2385a5fb-dc55-4159-a957-59d615aff2f4): select id, name, sum(price) amount from t3, t4 where t3.id=t4.id and t4.deal_day='20230110' group by t3.id, t3.name
INFO  : Completed executing command(queryId=root_20231229095844_2385a5fb-dc55-4159-a957-59d615aff2f4); Time taken: 0.268 seconds
+-----+-------+---------+
| id  | name  | amount  |
+-----+-------+---------+
| 1   | aa    | 1232.2  |
| 2   | bb    | 542.24  |
| 3   | cc    | 223.23  |
+-----+-------+---------+
3 rows selected (1.283 seconds)


0: jdbc:hive2://zgg-server:10000> Explain select id, name, sum(price) amount from t3, t4 where t3.id=t4.id and t4.deal_day='20230110' group by t3.id, t3.name;
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-0 is a root stage                          |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: -1                                    |
|       Processor Tree:                              |
|         TableScan                                  |
|           alias: default.mv2   【√】                 |
|           Statistics: Num rows: 3 Data size: 294 Basic stats: COMPLETE Column stats: COMPLETE |
|           Select Operator                          |
|             expressions: id (type: int), name (type: string), amount (type: double) |
|             outputColumnNames: _col0, _col1, _col2 |
|             Statistics: Num rows: 3 Data size: 294 Basic stats: COMPLETE Column stats: COMPLETE |
|             ListSink                               |
|                                                    |
+----------------------------------------------------+
17 rows selected (1.207 seconds)


create materialized view mv3
DISABLE REWRITE
stored as orc 
as 
select sum(price) amount
from t4
where deal_day='20230110';

Explain select sum(price) amount from t4 where deal_day='20230110';

-- 执行了 mr job
0: jdbc:hive2://zgg-server:10000> select sum(price) amount from t4 where deal_day='20230110';
INFO  : Query ID = root_20231229095806_adb7e9e8-bc3e-4177-8603-c513095dc222
INFO  : Total jobs = 1
...
+----------+
|  amount  |
+----------+
| 1997.67  |
+----------+
1 row selected (34.734 seconds)

create materialized view mv4
stored as orc 
as 
select sum(price) amount
from t4
where deal_day='20230110';

-- 未执行 mr job
0: jdbc:hive2://zgg-server:10000> select sum(price) amount from t4 where deal_day='20230110';
INFO  : Compiling command(queryId=root_20231229100323_79bb8519-3175-4c99-aac5-6004434bc464): select sum(price) amount from t4 where deal_day='20230110'
INFO  : Semantic Analysis Completed (retrial = false)
INFO  : Created Hive schema: Schema(fieldSchemas:[FieldSchema(name:amount, type:double, comment:null)], properties:null)
INFO  : Completed compiling command(queryId=root_20231229100323_79bb8519-3175-4c99-aac5-6004434bc464); Time taken: 0.164 seconds
INFO  : Operation QUERY obtained 1 locks
INFO  : Executing command(queryId=root_20231229100323_79bb8519-3175-4c99-aac5-6004434bc464): select sum(price) amount from t4 where deal_day='20230110'
INFO  : Completed executing command(queryId=root_20231229100323_79bb8519-3175-4c99-aac5-6004434bc464); Time taken: 0.105 seconds
+----------+
|  amount  |
+----------+
| 1997.67  |
+----------+
1 row selected (0.42 seconds)
```

-------------------------------

更多详细描述见

官网：[物化视图语法](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-Create/Drop/AlterMaterializedView) | [物化视图](https://cwiki.apache.org/confluence/display/Hive/Materialized+views) 

翻译： [物化视图语法](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/DDL%20Statements.md) | [物化视图](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Materialized%20views.md) 