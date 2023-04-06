# LanguageManual Indexing

[TOC]

## 1、Indexing Is Removed since 3.0

> There are alternate options which might work similarily to indexing:

**还有其他选项作用可能与索引类似**:

- 使用自动重写的物化视图可能产生非常相似的结果。Hive 2.3.0 增加了对物化视图的支持。

- 使用柱状文件格式(Parquet, ORC)：他们可以做选择性扫描；它们甚至可能跳过整个文件/块。

> Materialized views with automatic rewriting can result in very similar results.  [Hive 2.3.0](https://issues.apache.org/jira/browse/HIVE-14249) adds support for materialzed views.
> Using columnar file formats ([Parquet](https://cwiki.apache.org/confluence/display/Hive/Parquet), [ORC](https://orc.apache.org/docs/indexes.html)) – they can do selective scanning; they may even skip entire files/blocks.

> Indexing has been removed in version 3.0 ([HIVE-18448](https://issues.apache.org/jira/browse/HIVE-18448)).

索引已在 3.0 版本中被删除。

## 2、Overview of Hive Indexes

> The goal of Hive indexing is to improve the speed of query lookup on certain columns of a table. Without an index, queries with predicates like 'WHERE tab1.col1 = 10' load the entire table or partition and process all the rows. But if an index exists for col1, then only a portion of the file needs to be loaded and processed.

Hive 索引的目的是提高对表中某些列的查询查找的速度。**如果没有索引，则使用像 `WHERE tab1.col1 = 10` 这样的谓词进行查询会加载整个表或分区，并处理所有行**。但是，如果存在 col1 的索引，那么只需要加载和处理文件的一部分。

> The improvement in query speed that an index can provide comes at the cost of additional processing to create the index and disk space to store the index.

索引可以提供的查询速度的提高是以创建索引和存储索引的磁盘空间的额外处理为代价的。

> Versions:Hive indexing was added in version 0.7.0, and bitmap indexing was added in version 0.8.0.

Hive 索引在 0.7.0 版本中添加，位图索引在 0.8.0 版本中添加。

## 3、Indexing Resources

> Documentation and examples of how to use Hive indexes can be found here:

如何使用 Hive 索引的文档和示例：

- [Indexes](https://cwiki.apache.org/confluence/display/Hive/IndexDev) – design document (lists indexing JIRAs with current status, starting with [HIVE-417](https://issues.apache.org/jira/browse/HIVE-417))

- [Create/Drop/Alter Index](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-Create/Drop/AlterIndex) – [HiveQL Language Manual DDL](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL)

- [Show Indexes](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-ShowIndexes) – HiveQL Language Manual DDL

- [Bitmap indexes](https://cwiki.apache.org/confluence/display/Hive/IndexDev+Bitmap) – added in Hive version 0.8.0 ([HIVE-1803](https://issues.apache.org/jira/browse/HIVE-1803))

- [Indexed Hive](http://www.slideshare.net/NikhilDeshpande/indexed-hive) – overview and examples by Prafulla Tekawade and Nikhil Deshpande, October 2010

- [Tutorial: SQL-like join and index with MapReduce using Hadoop and Hive](http://asheeshgarg.blogspot.com/2012/04/sql-like-join-and-index-with-mr-using.html) – blog by Ashish Garg, April 2012

### 3.1、Configuration Parameters for Hive Indexes

> The [Configuration Properties](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-Indexing) document describes parameters that configure Hive indexes.

Configuration Properties 文档描述了配置 Hive 索引的参数。

## 4、Simple Examples

> This section gives some indexing examples adapted from the Hive test suite.

本节给出了一些来自 Hive 测试套件的索引示例。

> Case sensitivity:In Hive 0.12.0 and earlier releases, the index name is case-sensitive for CREATE INDEX and DROP INDEX statements. However, ALTER INDEX requires an index name that was created with lowercase letters (see [HIVE-2752](https://issues.apache.org/jira/browse/HIVE-2752)). This bug is fixed in Hive [0.13.0](https://issues.apache.org/jira/browse/HIVE-2752) by making index names case-insensitive for all HiveQL statements. For releases prior to 0.13.0, the best practice is to use lowercase letters for all index names.

区分大小写

在 Hive 0.12.0 和更早的版本中，CREATE index 和 DROP index 语句的索引名是区分大小写的。

但是，ALTER INDEX 需要一个用小写字母创建的索引名。在 Hive 0.13.0 中，修复了这个bug，索引名对所有 HiveQL 语句不区分大小写。对于 0.13.0 之前的版本，最佳实践是对所有索引名使用小写字母。

Create/build, show, and drop index:

```sql
CREATE INDEX table01_index ON TABLE table01 (column2) AS 'COMPACT';
SHOW INDEX ON table01;
DROP INDEX table01_index ON table01;
```

Create then build, show formatted (with column names), and drop index:

```sql
CREATE INDEX table02_index ON TABLE table02 (column3) AS 'COMPACT' WITH DEFERRED REBUILD;
ALTER INDEX table02_index ON table2 REBUILD;
SHOW FORMATTED INDEX ON table02;
DROP INDEX table02_index ON table02;
```

Create bitmap index, build, show, and drop:

```sql
CREATE INDEX table03_index ON TABLE table03 (column4) AS 'BITMAP' WITH DEFERRED REBUILD;
ALTER INDEX table03_index ON table03 REBUILD;
SHOW FORMATTED INDEX ON table03;
DROP INDEX table03_index ON table03;
```

Create index in a new table:

```sql
CREATE INDEX table04_index ON TABLE table04 (column5) AS 'COMPACT' WITH DEFERRED REBUILD IN TABLE table04_index_table;
```

Create index stored as RCFile:

```sql
CREATE INDEX table05_index ON TABLE table05 (column6) AS 'COMPACT' STORED AS RCFILE;
```

Create index stored as text file:

```sql
CREATE INDEX table06_index ON TABLE table06 (column7) AS 'COMPACT' ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' STORED AS TEXTFILE;
```

Create index with index properties:

```sql
CREATE INDEX table07_index ON TABLE table07 (column8) AS 'COMPACT' IDXPROPERTIES ("prop1"="value1", "prop2"="value2");
```

Create index with table properties:

```sql
CREATE INDEX table08_index ON TABLE table08 (column9) AS 'COMPACT' TBLPROPERTIES ("prop3"="value3", "prop4"="value4");
```

Drop index if exists:

```sql
DROP INDEX IF EXISTS table09_index ON table09;
```

Rebuild index on a partition:

```sql
ALTER INDEX table10_index ON table10 PARTITION (columnX='valueQ', columnY='valueR') REBUILD;
```