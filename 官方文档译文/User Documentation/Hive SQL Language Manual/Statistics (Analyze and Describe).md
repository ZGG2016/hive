# StatsDev

[TOC]

> This document describes the support of statistics for Hive tables (see [HIVE-33](http://issues.apache.org/jira/browse/HIVE-33)).

本文档描述 Hive 表的统计信息。

## 1、Motivation

> Statistics such as the number of rows of a table or partition and the histograms of a particular interesting column are important in many ways. One of the key use cases of statistics is query optimization. Statistics serve as the input to the cost functions of the optimizer so that it can compare different plans and choose among them. Statistics may sometimes meet the purpose of the users' queries. Users can quickly get the answers for some of their queries by only querying stored statistics rather than firing long-running execution plans. Some examples are getting the quantile of the users' age distribution, the top 10 apps that are used by people, and the number of distinct sessions.

统计信息，比如表或分区的行数和特定列的直方图，在很多方面都很重要。

统计信息的一个关键用例是查询优化。统计信息**作为成本函数的输入，使优化器可以比较不同的计划，并进行选择**。

统计信息有时可以满足用户查询的目的。用户可以通过**只查询存储的统计信息，而不是触发长时间运行的执行计划，来快速获得某些查询的答案**。

有些例子是关于用户年龄分布的分位数，人们使用的前 10 个应用，以及不同的会话数。

## 2、Scope

### 2.1、Table and Partition Statistics

> The first milestone in supporting statistics was to support table and partition level statistics. Table and partition statistics are now stored in the Hive Metastore for either newly created or existing tables. The following statistics are currently supported for partitions:

支持统计信息的第一个里程碑是支持表和分区级的统计。现在，**表和分区统计信息存储在 Hive Metastore 中，无论是新创建的表还是现有的表**。分区目前支持以下统计信息:

- Number of rows 行数
- Number of files 文件数
- Size in Bytes 字节大小

> For tables, the same statistics are supported with the addition of the number of partitions of the table.

对于表，通过添加表的分区数来支持相同的统计信息。

> Version: Table and partition statistics

版本：表和分区统计信息

Hive 0.7.0 中添加了表和分区级别的统计。

> Table and partition level statistics were added in Hive 0.7.0 by [HIVE-1361](https://issues.apache.org/jira/browse/HIVE-1361).

### 2.2、Column Statistics

> The second milestone was to support column level statistics. See [Column Statistics in Hive](https://cwiki.apache.org/confluence/display/Hive/Column+Statistics+in+Hive) in the Design Documents.

第二个里程碑是支持列级别的统计信息。见 Design Documents 中的 Column Statistics in Hive。

> Version: Column statistics

版本：列统计信息。

列级别的统计信息在 Hive 0.10.0 中添加。

> Column level statistics were added in Hive 0.10.0 by [HIVE-1362](https://issues.apache.org/jira/browse/HIVE-1362).

### 2.3、Top K Statistics

> [Column level top K statistics](https://cwiki.apache.org/confluence/display/Hive/Top+K+Stats) are still pending; see [HIVE-3421](https://issues.apache.org/jira/browse/HIVE-3421).

## 3、Quick overview

Description   |     Stored in    |  Collected by  |  Since
---|:---|:---|:---
Number of partition the dataset consists of【数据集包含的分区数】	 | Fictional metastore property: **numPartitions**  |  	computed during displaying the properties of a partitioned table【在显示分区表的属性时计算】  |  [Hive 2.3](https://issues.apache.org/jira/browse/HIVE-16315)
Number of files the dataset consists of【数据集包含的文件数】	 |  Metastore table property: **numFiles**	| Automatically during Metastore operations【在Metastore操作期间自动执行】	 |
Total size of the dataset as its seen at the filesystem level【数据集在文件系统级的总大小】  | Metastore table property: **totalSize**  | Automatically during Metastore operations【在Metastore操作期间自动执行】	 |
Uncompressed size of the dataset【数据集的未压缩的大小】  |	Metastore table property:**rawDataSize**  |  Computed, these are the basic statistics. Calculated automatically when hive.stats.autogather is enabled.Can be collected manually by: ANALYZE TABLE ... COMPUTE STATISTICS【经过计算，这些是基本的统计数据。当启用`hive.stats.autogather`自动计算。可以手工收集通过：`ANALYZE TABLE ... COMPUTE STATISTICS`】  |  [Hive 0.8](https://issues.apache.org/jira/browse/HIVE-2185)
Number of rows the dataset consist of 数据集包含的行数】 |  Metastore table property: **numRows** |  Computed, these are the basic statistics. Calculated automatically when [`hive.stats.autogather`](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.stats.autogather) is enabled.Can be collected manually by: `ANALYZE TABLE ... COMPUTE STATISTICS`【经过计算，这些是基本的统计数据。当启用`hive.stats.autogather`自动计算。可以手工收集通过：`ANALYZE TABLE ... COMPUTE STATISTICS`】  | 
Column level statistics【列级别的统计信息】  |  Metastore; `TAB_COL_STATS` table  |  Computed, Calculated automatically when `hive.stats.column.autogather` is enabled.Can be collected manually by: `ANALYZE TABLE ... COMPUTE STATISTICS FOR COLUMNS`


## 4、Implementation

> The way the statistics are calculated is similar for both newly created and existing tables.

对于新创建的表和现有的表，统计信息的计算方法是相似的。

> For newly created tables, the job that creates a new table is a MapReduce job. During the creation, every mapper while copying the rows from the source table in the FileSink operator, gathers statistics for the rows it encounters and publishes them into a Database (possibly MySQL). At the end of the MapReduce job, published statistics are aggregated and stored in the MetaStore.

对于新创建的表，创建新表的 job 是一个 MapReduce job。在创建过程中，当在 FileSink 操作符中从源表复制行时，每个 mapper 都会收集所遇到行的统计信息，并将它们发布到数据库(可能是MySQL)中。在 MapReduce job 的最后，发布的统计信息被聚合，并存储在 MetaStore 中。

> A similar process happens in the case of already existing tables, where a Map-only job is created and every mapper while processing the table in the TableScan operator, gathers statistics for the rows it encounters and the same process continues.

对于已经存在的表，也会发生类似的过程，创建一个 Map-only job ，当在 TableScan 操作符中处理表时，每个 mapper 收集它遇到的行的统计信息，并继续执行相同的过程。

> It is clear that there is a need for a database that stores temporary gathered statistics. Currently there are two implementations, one is using MySQL and the other is using HBase. There are two pluggable interfaces IStatsPublisher and IStatsAggregator that the developer can implement to support any other storage. The interfaces are listed below:

很明显，需要一个数据库来存储临时收集的统计信息。目前有两种实现，一种是使用 MySQL，另一种是使用 HBase。开发者可以实现两个可插入接口 istatsppublisher 和 IStatsAggregator 来支持任何其他存储。

接口如下:

```java
package org.apache.hadoop.hive.ql.stats;
 
import org.apache.hadoop.conf.Configuration;
 
/**
 * An interface for any possible implementation for publishing statics.
 */
 
public interface IStatsPublisher {
 
  /**
 * This method does the necessary initializations according to the implementation requirements.
   */
  public boolean init(Configuration hconf);
 
  /**
 * This method publishes a given statistic into a disk storage, possibly HBase or MySQL.
   *
 * rowID : a string identification the statistics to be published then gathered, possibly the table name + the partition specs.
   *
 * key : a string noting the key to be published. Ex: "numRows".
   *
 * value : an integer noting the value of the published key.
 * */
  public boolean publishStat(String rowID, String key, String value);
 
  /**
 * This method executes the necessary termination procedures, possibly closing all database connections.
   */
  public boolean terminate();
 
}
```
```java
package org.apache.hadoop.hive.ql.stats;
 
import org.apache.hadoop.conf.Configuration;
 
/**
 * An interface for any possible implementation for gathering statistics.
 */
 
public interface IStatsAggregator {
 
  /**
 * This method does the necessary initializations according to the implementation requirements.
   */
  public boolean init(Configuration hconf);
 
  /**
 * This method aggregates a given statistic from a disk storage.
 * After aggregation, this method does cleaning by removing all records from the disk storage that have the same given rowID.
   *
 * rowID : a string identification the statistic to be gathered, possibly the table name + the partition specs.
   *
 * key : a string noting the key to be gathered. Ex: "numRows".
   *
 * */
  public String aggregateStats(String rowID, String key);
 
  /**
 * This method executes the necessary termination procedures, possibly closing all database connections.
   */
  public boolean terminate();
 
}
```

## 5、Usage

### 5.1、Configuration Variables

> See [Statistics](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-Statistics) in [Configuration Properties](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties) for a list of the variables that configure Hive table statistics. [Configuring Hive](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-ConfiguringHive) describes how to use the variables.

配置 Hive 表统计信息的变量列表见 Configuration Properties 中的 Statistics。

Configuring Hive 描述了如何使用变量

### 5.2、Newly Created Tables

> For newly created tables and/or partitions (that are populated through the INSERT OVERWRITE command), statistics are automatically computed by default. The user has to explicitly set the boolean variable hive.stats.autogather to false so that statistics are not automatically computed and stored into Hive MetaStore.

**对于新创建的表和/或分区(通过 `INSERT OVERWRITE`命令填充数据)，默认情况下会自动计算统计信息**。用户必须显式地设置布尔变量 `hive.stats.autogather` 为 false，以便统计信息不会自动计算并存储到 Hive MetaStore 中。

	set hive.stats.autogather=false;

> The user can also specify the implementation to be used for the storage of temporary statistics setting the variable hive.stats.dbclass. For example, to set HBase as the implementation of temporary statistics storage (the default is jdbc:derby or fs, depending on the Hive version) the user should issue the following command:

用户还可以通过**设置变量 `hive.stats.dbclass` 来指定用于存储临时统计信息的实现**。

例如，设置 HBase 为临时统计存储的实现(默认为 jdbc:derby 或 fs，取决于 Hive 版本)，用户应该发出以下命令:

	set hive.stats.dbclass=hbase;

> In case of JDBC implementations of temporary stored statistics (ex. Derby or MySQL), the user should specify the appropriate connection string to the database by setting the variable hive.stats.dbconnectionstring. Also the user should specify the appropriate JDBC driver by setting the variable hive.stats.jdbcdriver.

对于临时存储统计数据的 JDBC 实现(例如Derby或MySQL)，用户应该通过**设置变量 `hive.stats.dbconnectionstring` 来指定连接到数据库的字符串**。

用户还应该通过**设置变量 `hive.stats.jdbcdriver` 来指定适当的 JDBC 驱动程序**。

	set hive.stats.dbclass=jdbc:derby;
	set hive.stats.dbconnectionstring="jdbc:derby:;databaseName=TempStatsStore;create=true";
	set hive.stats.jdbcdriver="org.apache.derby.jdbc.EmbeddedDriver";

> Queries can fail to collect stats completely accurately. There is a setting hive.stats.reliable that fails queries if the stats can't be reliably collected. This is false by default.

查询可能无法完全准确地收集统计数据。如果不能可靠地收集统计数据，可能存在 `hive.stats.reliable` 设置会导致查询失败。这默认为 false。

### 5.3、Existing Tables – ANALYZE

> For existing tables and/or partitions, the user can issue the ANALYZE command to gather statistics and write them into Hive MetaStore. The syntax for that command is described below:

对于已经存在的表和/或分区，用户可以使用 ANALYZE 命令来收集统计信息，并将其写入 Hive MetaStore。

该命令的语法描述如下:

	ANALYZE TABLE [db_name.]tablename [PARTITION(partcol1[=val1], partcol2[=val2], ...)]  -- (Note: Fully support qualified table name since Hive 1.2.0, see HIVE-10007.)
	  COMPUTE STATISTICS 
	  [FOR COLUMNS]          -- (Note: Hive 0.10.0 and later.)
	  [CACHE METADATA]       -- (Note: Hive 2.1.0 and later.)
	  [NOSCAN];

> When the user issues that command, he may or may not specify the partition specs. If the user doesn't specify any partition specs, statistics are gathered for the table as well as all the partitions (if any). If certain partition specs are specified, then statistics are gathered for only those partitions. When computing statistics across all partitions, the partition columns still need to be listed. As of [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-10007), Hive fully supports qualified table name in this command. User can only compute the statistics for a table under current database if a non-qualified table name is used.

当用户使用该命令时，他可能指定，也可能不指定分区。如果用户没有指定任何分区，则会收集表以及所有分区(如果有的话)的统计信息。

如果指定了某些分区，则只收集这些分区的统计信息。

在跨所有分区计算统计信息时，仍然需要列出分区列。

从 Hive 1.2.0 开始，Hive 在此命令中完全支持限定表名。如果使用非限定表名，用户只能计算当前数据库下表的统计信息。

> When the optional parameter NOSCAN is specified, the command won't scan files so that it's supposed to be fast. Instead of all statistics, it just gathers the following statistics:

当指定了可选参数 NOSCAN 时，该命令不会扫描文件，因此它应该是快速的。与所有统计信息不同，它只收集了以下统计数据:

- Number of files
- Physical size in bytes

> Version 0.10.0: FOR COLUMNS.As of [Hive 0.10.0](https://issues.apache.org/jira/browse/HIVE-1362), the optional parameter FOR COLUMNS computes column statistics for all columns in the specified table (and for all partitions if the table is partitioned). See [Column Statistics in Hive](https://cwiki.apache.org/confluence/display/Hive/Column+Statistics+in+Hive) for details.To display these statistics, use DESCRIBE FORMATTED [db_name.]table_name column_name [PARTITION (partition_spec)].

从 Hive 0.10.0 开始，可选参数 FOR COLUMNS 计算指定表中的所有列的列统计(如果表已经分区，则计算所有分区的列统计)。详细信息请参见 Column Statistics in Hive。

要显示这些统计信息，请使用 `DESCRIBE FORMATTED [db_name.]table_name column_name [PARTITION (partition_spec)]`

## 6、Examples

> Suppose table Table1 has 4 partitions with the following specs:

假设表 Table1 有四个分区：

- Partition1: (ds='2008-04-08', hr=11)
- Partition2: (ds='2008-04-08', hr=12)
- Partition3: (ds='2008-04-09', hr=11)
- Partition4: (ds='2008-04-09', hr=12)

> and you issue the following command:

执行如下命令：

	ANALYZE TABLE Table1 PARTITION(ds='2008-04-09', hr=11) COMPUTE STATISTICS;

> then statistics are gathered for partition3 (ds='2008-04-09', hr=11) only.

那么，就会仅收集 partition3 (ds='2008-04-09', hr=11) 的统计信息。

> If you issue the command:

执行如下命令：

	ANALYZE TABLE Table1 PARTITION(ds='2008-04-09', hr=11) COMPUTE STATISTICS FOR COLUMNS;

> then column statistics are gathered for all columns for partition3 (ds='2008-04-09', hr=11). This is available in Hive 0.10.0 and later.

那么就会收集 partition3 (ds='2008-04-09', hr=11) 分区的所有列的列统计信息。在 Hive 0.10.0 及其以后的版本可用。

> If you issue the command:

执行如下命令：

	ANALYZE TABLE Table1 PARTITION(ds='2008-04-09', hr) COMPUTE STATISTICS;

> then statistics are gathered for partitions 3 and 4 only (hr=11 and hr=12).

那么，仅收集 partitions 3 和 4 (hr=11 and hr=12) 分区的统计信息。

> If you issue the command:

执行如下命令：

	ANALYZE TABLE Table1 PARTITION(ds='2008-04-09', hr) COMPUTE STATISTICS FOR COLUMNS;

> then column statistics for all columns are gathered for partitions 3 and 4 only (Hive 0.10.0 and later).

那么，仅收集 partitions 3 和 4 分区的所有列的列统计信息。在 Hive 0.10.0 及其以后的版本可用。

> If you issue the command:

执行如下命令：

	ANALYZE TABLE Table1 PARTITION(ds, hr) COMPUTE STATISTICS;

> then statistics are gathered for all four partitions.

那么，收集所有四个分区的统计信息。

> If you issue the command:

执行如下命令：

	ANALYZE TABLE Table1 PARTITION(ds, hr) COMPUTE STATISTICS FOR COLUMNS;

> then column statistics for all columns are gathered for all four partitions (Hive 0.10.0 and later).

那么，收集所有四个分区的所有列的列统计信息。在 Hive 0.10.0 及其以后的版本可用。

> For a non-partitioned table, you can issue the command:

对于一个非分区表，执行如下命令：

	ANALYZE TABLE Table1 COMPUTE STATISTICS;

> to gather statistics of the table.

来收集表的统计信息。

> For a non-partitioned table, you can issue the command:

对于一个非分区表，执行如下命令：

	ANALYZE TABLE Table1 COMPUTE STATISTICS FOR COLUMNS;

> to gather column statistics of the table (Hive 0.10.0 and later).

来收集表的列统计信息。在 Hive 0.10.0 及其以后的版本可用。

> If Table1 is a partitioned table,  then for basic statistics you have to specify partition specifications like above in the analyze statement. Otherwise a semantic analyzer exception will be thrown.

如果表 Table1 是一个分区表，那么对于基本的统计信息，你必须在 analyze 语句中指定分区。否则抛出 semantic analyzer exception。 

> However for column statistics, if no partition specification is given in the analyze statement, statistics for all partitions are computed.

然而，对于列统计信息，如果没有在 analyze 语句中指定分区，就会计算所有分区的统计信息。

> You can view the stored statistics by issuing the [DESCRIBE](http://wiki.apache.org/hadoop/Hive/LanguageManual/DDL?highlight=(describe)#Describe_Partition) command. Statistics are stored in the Parameters array. Suppose you issue the analyze command for the whole table Table1, then issue the command:

可以执行 DESCRIBE 命令来浏览存储的统计信息。统计信息存储在 Parameters 数组中。在整个表 Table1 执行 analyze 命令：

	DESCRIBE EXTENDED TABLE1;

> then among the output, the following would be displayed:

输出如下：

	... , parameters:{numPartitions=4, numFiles=16, numRows=2000, totalSize=16384, ...}, ....

> If you issue the command:

执行如下命令：

	DESCRIBE EXTENDED TABLE1 PARTITION(ds='2008-04-09', hr=11);

> then among the output, the following would be displayed:

输出如下：

	... , parameters:{numFiles=4, numRows=500, totalSize=4096, ...}, ....

> If you issue the command:

执行如下命令：

	desc formatted concurrent_delete_different partition(ds='tomorrow') name;

> the output would look like this:

输出如下：

	+-----------------+--------------------+-------+-------+------------+-----------------+--------------+--------------+------------+-------------+------------+----------+
	|    col_name     |     data_type      |  min  |  max  | num_nulls  | distinct_count  | avg_col_len  | max_col_len  | num_trues  | num_falses  | bitvector  | comment  |
	+-----------------+--------------------+-------+-------+------------+-----------------+--------------+--------------+------------+-------------+------------+----------+
	| col_name        | name               | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	| data_type       | varchar(50)        | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	| min             |                    | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	| max             |                    | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	| num_nulls       | 0                  | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	| distinct_count  | 2                  | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	| avg_col_len     | 5.0                | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	| max_col_len     | 5                  | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	| num_trues       |                    | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	| num_falses      |                    | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	| bitVector       |                    | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	| comment         | from deserializer  | NULL  | NULL  | NULL       | NULL            | NULL         | NULL         | NULL       | NULL        | NULL       | NULL     |
	+-----------------+--------------------+-------+-------+------------+-----------------+--------------+--------------+------------+-------------+------------+----------+

> If you issue the command:

执行如下命令：

	ANALYZE TABLE Table1 PARTITION(ds='2008-04-09', hr) COMPUTE STATISTICS NOSCAN;

> then statistics, number of files and physical size in bytes are gathered for partitions 3 and 4 only.

那么，仅收集 partitions 3 和 4 分区的统计信息：文件数和物理大小，字节大小。

### 6.1、ANALYZE TABLE <table1> CACHE METADATA

> Feature not implemented. Hive Metastore on HBase was discontinued and removed in Hive 3.0.0. See [HBaseMetastoreDevelopmentGuide]()

功能没有实现。HBase 上的 Hive Metastore 停止使用，在 Hive 3.0.0 中被移除。见 HBaseMetastoreDevelopmentGuide

> When Hive metastore is configured to use HBase, this command explicitly caches file metadata in HBase metastore.  

当 Hive metastore 配置为使用 HBase 时，该命令会显式缓存文件元数据到 HBase metastore 中。

> The goal of this feature is to cache file metadata (e.g. ORC file footers) to avoid reading lots of files from HDFS at split generation time, as well as potentially cache some information about splits (e.g. grouping based on location that would be good for some short time) to further speed up the generation and achieve better cache locality with consistent splits.

这个功能的目的是缓存文件元数据(例如ORC file footers)，以避免在分裂生成时间从 HDFS 阅读大量的文件，以及潜在的缓存一些分裂的信息(如基于位置分组，这在一些短时间内有利的)，进一步加快生成和使用一致的分裂实现更好的缓存位置。

	ANALYZE TABLE Table1 CACHE METADATA;

> See feature details in [HBase Metastore Split Cache]() and ([HIVE-12075]())

## 7、Current Status (JIRA)