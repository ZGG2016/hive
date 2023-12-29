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

这个功能在 Hive 2.0.0 中被移除。此功能被 Hive 提供的几种安全选项之一所替代。

```sql
ALTER TABLE table_name [PARTITION partition_spec] ENABLE|DISABLE NO_DROP [CASCADE];

ALTER TABLE table_name [PARTITION partition_spec] ENABLE|DISABLE OFFLINE;
```

- 启用 NO_DROP 可以防止表被删除
- 如果表中的任何一个分区启用了 NO_DROP，那么该表也不能被删除。
- 启用 OFFLINE 可以防止查询表或分区中的数据，但仍然可以访问其元数据。
- 如果一个表启用了 NO_DROP，那么分区可能会被删除，但是使用了 NO_DROP CASCADE 的分区也不能被删除，除非 drop partition 命令指定了 IGNORE PROTECTION。

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

## Alter Table/Partition Concatenate

如果表或分区包含许多小的 RCFiles 或 ORC 文件，那么上面的命令将把它们合并到更大的文件中。在 RCFile 的情况下，合并发生在块的级别，而对于 ORC 文件，合并发生在条带级别，因此避免了解压和解码数据的开销。 

```sql
0: jdbc:hive2://zgg-server:10000> alter table t6 concatenate;
```

```
Error: Error while compiling statement: FAILED: SemanticException [Error 30034]: Concatenate/Merge can only be performed on managed tables (state=42000,code=30034)
```

## Alter Table/Partition Update columns

同步 serde 存储模式信息到 metastore

```sql
root@zgg-server:~# cat data/user.avsc 
{"namespace": "example.avro",
 "type": "record",
 "name": "User",
 "fields": [
     {"name": "name", "type": "string"},
     {"name": "favorite_color", "type": ["string", "null"]}
 ]
}
-- 将 user.avsc 上传到hdfs
CREATE TABLE `user`
  PARTITIONED BY (ds string)
  ROW FORMAT SERDE
  'org.apache.hadoop.hive.serde2.avro.AvroSerDe'
  STORED AS INPUTFORMAT
  'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat'
  OUTPUTFORMAT
  'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat'
  TBLPROPERTIES (
    'avro.schema.url'='/in/user.avsc');

mysql> select * from COLUMNS_V2;
+-------+---------+------------------+-----------+-------------+
| CD_ID | COMMENT | COLUMN_NAME      | TYPE_NAME | INTEGER_IDX |
+-------+---------+------------------+-----------+-------------+
|   106 |         | favorite_color   | string    |           1 |
|   106 |         | name             | string    |           0 |
+-------+---------+------------------+-----------+-------------+


-- 修改user.avsc内容后，再次上传到hdfs
root@zgg-server:~# hadoop fs -cat /in/user.avsc
{"namespace": "example.avro",
 "type": "record",
 "name": "User",
 "fields": [
     {"name": "lastname", "type": "string"},
     {"name": "favorite_color", "type": ["string", "null"]}
 ]
}

-- 只有执行此语句后，mysql中的元数据才会更新
0: jdbc:hive2://zgg-server:10000> alter table `user` update columns;

mysql> select * from COLUMNS_V2;
+-------+---------+------------------+-----------+-------------+
| CD_ID | COMMENT | COLUMN_NAME      | TYPE_NAME | INTEGER_IDX |
+-------+---------+------------------+-----------+-------------+
|   107 |         | favorite_color   | string    |           1 |
|   107 |         | lastname         | string    |           0 |
+-------+---------+------------------+-----------+-------------+
```

---------------------

[官网](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AlterEitherTableorPartition)

[官网翻译](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/DDL%20Statements.md)