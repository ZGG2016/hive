# alter table或partition语句

[TOC]

## Alter Table/Partition File Format

```
0: jdbc:hive2://localhost:10000> create table ff_tb(id int) stored as textfile;

0: jdbc:hive2://localhost:10000> desc formatted ff_tb;
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
|           col_name            |                     data_type                      |                      comment                       |
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
| # Storage Information         | NULL                                               | NULL                                               |
| SerDe Library:                | org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe | NULL                                               |
| InputFormat:                  | org.apache.hadoop.mapred.TextInputFormat           | NULL                                               |
| OutputFormat:                 | org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat | NULL                                               |                                                  |
+-------------------------------+----------------------------------------------------+----------------------------------------------------+

0: jdbc:hive2://localhost:10000> alter table ff_tb set fileformat orc;

0: jdbc:hive2://localhost:10000> desc formatted ff_tb;
+-------------------------------+---------------------------------------------------+----------------------------------------------------+
|           col_name            |                     data_type                     |                      comment                       |
+-------------------------------+---------------------------------------------------+----------------------------------------------------+
| SerDe Library:                | org.apache.hadoop.hive.ql.io.orc.OrcSerde         | NULL                                               |
| InputFormat:                  | org.apache.hadoop.hive.ql.io.orc.OrcInputFormat   | NULL                                               |
| OutputFormat:                 | org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat  | NULL                                               |                                                |
+-------------------------------+---------------------------------------------------+----------------------------------------------------+
```

该操作仅更改表元数据。任何现有数据的转换都必须在 Hive 之外完成。

```sql
alter table order_table_s3 partition(deal_day="201901") set fileformat orc;
```

## Alter Table/Partition Location

```sql
ALTER TABLE table_name [PARTITION partition_spec] SET LOCATION "new location";
```

## Alter Table/Partition Touch

监测对表或分区的修改或访问。比如

更详细的描述见[官网](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/DDL%20Statements.md#1533alter-tablepartition-touch)

TODO

```sql
0: jdbc:hive2://localhost:10000> alter table order_table_s touch partition(deal_day='201903');
```

## Alter Table/Partition Protections


## Alter Table/Partition Compact

一般情况下，当使用 Hive 事务时，你不需要请求 compactions，因为系统会检测到它们的需要，并启动 compaction。

但是，如果对表关闭了 compaction，或者你想在系统不选择的时候 compact 表，那么 ALTER TABLE 可以初始化 compaction。

ALTER TABLE … COMPACT 语句可以包含一个 TBLPROPERTIES 子句，用于更改 compaction MapReduce job 的属性或覆盖其他 Hive 表的属性。

```sql
ALTER TABLE t8 COMPACT 'minor' 
WITH OVERWRITE TBLPROPERTIES ("compactor.mapreduce.map.memory.mb"="3072");  -- specify compaction map job properties

ALTER TABLE t8 COMPACT 'major'
WITH OVERWRITE TBLPROPERTIES ("tblprops.orc.compress.size"="8192");   -- change any other Hive table properties
```







---------------------
[官网](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/DDL%20Statements.md#1534alter-tablepartition-protections)