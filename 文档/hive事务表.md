# hive事务表

[TOC]

## 限制

- 只支持 ORC 文件格式
- 表必须被分桶
- 不能将外部表创建为 ACID 表
- hive 事务管理器必须设置为 `org.apache.hadoop.hive.ql.lockmgr.DbTxnManager` 来处理 ACID 表
- 事务表不支持 LOAD DATA... 语句

## 原理

每一次对表或分区修改的事务，都会创建一个增量文件，在读取时合并基本文件和增量文件，并应用更新和删除。

对表的修改越多，创建的增量文件越多，需要 compacted 文件以保持足够的性能。

有两种类型的 compaction:

- Minor compaction 对每个桶，合并的增量文件成一个增量文件
- Major compaction 对每个桶，合并增量文件和基本文件

详细介绍 [点这里](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20Transactions.md) 查看

## 启用

```sql
set hive.support.concurrency = true;
set hive.enforce.bucketing = true;
set hive.exec.dynamic.partition.mode = nonstrict;
set hive.txn.manager = org.apache.hadoop.hive.ql.lockmgr.DbTxnManager;
set hive.compactor.initiator.on = true;
set hive.compactor.worker.threads = 1;
```

一旦表通过 `TBLPROPERTIES ("transactional"="true")` 定义为 ACID 表，它就不能被转换回非 ACID 表

```sql
create table t6(id int, name string) 
partitioned by (deal_day string) 
clustered by (id) into 2 buckets STORED AS ORC
TBLPROPERTIES ("transactional"="true",
  "compactor.mapreduce.map.memory.mb"="2048",     -- specify compaction map job properties
  "compactorthreshold.hive.compactor.delta.num.threshold"="4",  -- trigger minor compaction if there are more than 4 delta directories
  "compactorthreshold.hive.compactor.delta.pct.threshold"="0.5" -- trigger major compaction if the ratio of size of delta files to
                                                                -- size of base files is greater than 50%
);

0: jdbc:hive2://zgg-server:10000> desc formatted t6;
+-------------------------------+----------------------------------------------------+-----------------------------+
|           col_name            |                     data_type                      |           comment           |
+-------------------------------+----------------------------------------------------+-----------------------------+
| id                            | int                                                |                             |
| name                          | string                                             |                             |
|                               | NULL                                               | NULL                        |
| # Partition Information       | NULL                                               | NULL                        |
| # col_name                    | data_type                                          | comment                     |
| deal_day                      | string                                             |                             |
|                               | NULL                                               | NULL                        |
| # Detailed Table Information  | NULL                                               | NULL                        |
| Database:                     | default                                            | NULL                        |
| OwnerType:                    | USER                                               | NULL                        |
| Owner:                        | root                                               | NULL                        |
| CreateTime:                   | Wed Dec 27 13:12:19 UTC 2023                       | NULL                        |
| LastAccessTime:               | Wed Dec 27 13:12:45 UTC 2023                       | NULL                        |
| Retention:                    | 0                                                  | NULL                        |
| Location:                     | hdfs://zgg-server:8020/user/hive/warehouse/t6      | NULL                        |
| Table Type:                   | MANAGED_TABLE                                      | NULL                        |
| Table Parameters:             | NULL                                               | NULL                        |
|                               | COLUMN_STATS_ACCURATE                              | {\"BASIC_STATS\":\"true\"}  |
|                               | bucketing_version                                  | 2                           |
|                               | compactor.mapreduce.map.memory.mb                  | 2048                        |
|                               | compactorthreshold.hive.compactor.delta.num.threshold | 4                           |
|                               | compactorthreshold.hive.compactor.delta.pct.threshold | 0.5                         |
|                               | numFiles                                           | 0                           |
|                               | numPartitions                                      | 0                           |
|                               | numRows                                            | 0                           |
|                               | rawDataSize                                        | 0                           |
|                               | totalSize                                          | 0                           |
|                               | transactional                                      | true                        |
|                               | transactional_properties                           | default                     |
|                               | transient_lastDdlTime                              | 1703682765                  |
|                               | NULL                                               | NULL                        |
| # Storage Information         | NULL                                               | NULL                        |
| SerDe Library:                | org.apache.hadoop.hive.ql.io.orc.OrcSerde          | NULL                        |
| InputFormat:                  | org.apache.hadoop.hive.ql.io.orc.OrcInputFormat    | NULL                        |
| OutputFormat:                 | org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat   | NULL                        |
| Compressed:                   | No                                                 | NULL                        |
| Num Buckets:                  | 2                                                  | NULL                        |
| Bucket Columns:               | [id]                                               | NULL                        |
| Sort Columns:                 | []                                                 | NULL                        |
+-------------------------------+----------------------------------------------------+-----------------------------+


0: jdbc:hive2://zgg-server:10000> insert into t6 partition(deal_day='20231212') select 1,'zhangsan';
0: jdbc:hive2://zgg-server:10000> select * from t6;
+--------+-----------+--------------+
| t6.id  |  t6.name  | t6.deal_day  |
+--------+-----------+--------------+
| 1      | zhangsan  | 20231212     |
+--------+-----------+--------------+

-- 将 zhangsan 改为 lisi
0: jdbc:hive2://zgg-server:10000> update t6 set name='lisi' where id=1;
0: jdbc:hive2://zgg-server:10000> select * from t6;
+--------+----------+--------------+
| t6.id  | t6.name  | t6.deal_day  |
+--------+----------+--------------+
| 1      | lisi     | 20231212     |
+--------+----------+--------------+

-- 删除 id=1 这条数据
0: jdbc:hive2://zgg-server:10000> delete from t6 where id=1;
0: jdbc:hive2://zgg-server:10000> select * from t6;
+--------+----------+--------------+
| t6.id  | t6.name  | t6.deal_day  |
+--------+----------+--------------+
+--------+----------+--------------+
```

## merge into

首先启用acid

```sh
set hive.support.concurrency = true;
set hive.enforce.bucketing = true;
set hive.exec.dynamic.partition.mode = nonstrict;
set hive.txn.manager = org.apache.hadoop.hive.ql.lockmgr.DbTxnManager;
set hive.compactor.initiator.on = true;
set hive.compactor.worker.threads = 1;
set hive.auto.convert.join=false;
set hive.merge.cardinality.check=false;
```

然后创建两个表，一个作为 merge 的目标表，一个作为 merge 的源表。

请注意，目标表必须被分桶，设置启用事务，并以 orc 格式存储。

```sql
CREATE TABLE transactions(
 ID int,
 TranValue string,
 last_update_user string)
PARTITIONED BY (tran_date string)
CLUSTERED BY (ID) into 5 buckets 
STORED AS ORC TBLPROPERTIES ('transactional'='true');

CREATE TABLE merge_source(
 ID int,
 TranValue string,
 tran_date string)
STORED AS ORC;
```

然后用一些数据填充目标表和源表。 【事务表不支持 LOAD DATA... 语句】

```sql
INSERT INTO transactions PARTITION (tran_date) VALUES
(1, 'value_01', 'creation', '20170410'),
(2, 'value_02', 'creation', '20170410'),
(3, 'value_03', 'creation', '20170410'),
(4, 'value_04', 'creation', '20170410'),
(5, 'value_05', 'creation', '20170413'),
(6, 'value_06', 'creation', '20170413'),
(7, 'value_07', 'creation', '20170413'),
(8, 'value_08', 'creation', '20170413'),
(9, 'value_09', 'creation', '20170413'),
(10, 'value_10','creation', '20170413');

INSERT INTO merge_source VALUES 
(1, 'value_01', '20170410'),
(4, NULL, '20170410'),
(7, 'value_77777', '20170413'),
(8, NULL, '20170413'),
(8, 'value_08', '20170415'),
(11, 'value_11', '20170415');
```

当我们检查这两个表时，我们期望在 Merge 之后，第 1 行保持不变，第 4 行被删除(暗示了一个业务规则:空值表示删除)，第 7 行被更新，第 11 行被插入。

第 8 行涉及到将行从一个分区移动到另一个分区。当前 Merge 不支持动态更改分区值。这需要在旧分区中删除，并在新分区中插入。在实际的用例中，需要根据这个标准构建源表。

然后创建 merge 语句，如下所示。请注意，并不是所有的 3 个 WHEN 合并语句都需要存在，只有 2 个甚至 1 个 WHEN 语句也可以。我们用不同的 last_update_user 标记数据。

```sql
MERGE INTO transactions AS T 
USING merge_source AS S
ON T.ID = S.ID and T.tran_date = S.tran_date
WHEN MATCHED AND (T.TranValue != S.TranValue AND S.TranValue IS NOT NULL) THEN UPDATE SET TranValue = S.TranValue, last_update_user = 'merge_update'
WHEN MATCHED AND S.TranValue IS NULL THEN DELETE
WHEN NOT MATCHED THEN INSERT VALUES (S.ID, S.TranValue, 'merge_insert', S.tran_date);
```

作为 update 子句的一部分，set value 语句不应该包含目标表装饰器“T.”，否则将得到 SQL 编译错误。

一旦合并完成，重新检查数据就会显示数据已经按照预期进行了合并。

第 1 行没有改变；第 4 行被删除；第 7 行被更新，第 11 行被插入。第 8 行被移到了一个新的分区。

## hive4.0创建事务表

```sql
0: jdbc:hive2://localhost:10000> CREATE TRANSACTIONAL TABLE transactional_table_test(key string, value string) PARTITIONED BY(ds string) STORED AS ORC;
```