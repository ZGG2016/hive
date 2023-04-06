# Column Statistics in Hive

[TOC]

## 1、Introduction

> This document describes changes to a) HiveQL, b) metastore schema, and c) metastore Thrift API to support column level statistics in Hive. Please note that the document doesn’t describe the changes needed to persist histograms in the metastore yet.

本文档描述了 HiveQL、metastore 模式、metastore Thrift API 的变更，以支持 Hive 中的列级统计信息。

请注意，文档还没有描述在 metastore 中保存直方图所需的更改。

> Version information. Column statistics are introduced in Hive 0.10.0 by [HIVE-1362](https://issues.apache.org/jira/browse/HIVE-1362). This is the design document. Column statistics auto gather is introduced in Hive 2.3 by [HIVE-11160](https://issues.apache.org/jira/browse/HIVE-11160). This is also the design document.

版本信息

列统计信息在 Hive 0.10.0 中引入。

列统计信息自动收集在 Hive 2.3 中引入。

> For general information about Hive statistics, see [Statistics in Hive](https://cwiki.apache.org/confluence/display/Hive/StatsDev). For information about top K statistics, see [Column Level Top K Statistics](https://cwiki.apache.org/confluence/display/Hive/Top+K+Stats).

关于 Hive 统计信息的详细信息请参见 Statistics in Hive。

有关 top K 统计信息，请参见 Column Level Top K Statistics。

## 2、HiveQL changes

> HiveQL currently supports the [analyze command](https://cwiki.apache.org/confluence/display/Hive/StatsDev#StatsDev-ExistingTables) to compute statistics on tables and partitions. HiveQL’s analyze command will be extended to trigger statistics computation on one or more column in a Hive table/partition. The necessary changes to HiveQL are as below,

HiveQL 目前支持 analyze 命令来计算表和分区的统计信息。

HiveQL 的 analyze 命令将被扩展，来触发 Hive 表/分区中的一个或多个列的统计计算。

对 HiveQL 的必要修改如下:

	analyze table t [partition p] compute statistics for [columns c,...];

> Please note that table and column aliases are not supported in the analyze statement.

注意，analyze 语句中不支持表和列别名。

> To view column stats :

查看列数据:

	describe formatted [table_name] [column_name];

## 3、Metastore Schema

> To persist column level statistics, we propose to add the following new tables,

为了持久化列级统计信息，我们建议添加如下新表：

```sql
CREATE TABLE TAB_COL_STATS
(
CS_ID NUMBER NOT NULL,
TBL_ID NUMBER NOT NULL,
COLUMN_NAME VARCHAR(128) NOT NULL,
COLUMN_TYPE VARCHAR(128) NOT NULL,
TABLE_NAME VARCHAR(128) NOT NULL,
DB_NAME VARCHAR(128) NOT NULL,

LOW_VALUE RAW,
HIGH_VALUE RAW,
NUM_NULLS BIGINT,
NUM_DISTINCTS BIGINT,

BIT_VECTOR, BLOB,  /* introduced in HIVE-16997 in Hive 3.0.0 */

AVG_COL_LEN DOUBLE,
MAX_COL_LEN BIGINT,
NUM_TRUES BIGINT,
NUM_FALSES BIGINT,
LAST_ANALYZED BIGINT NOT NULL)
```
```sql
ALTER TABLE COLUMN_STATISTICS ADD CONSTRAINT COLUMN_STATISTICS_PK PRIMARY KEY (CS_ID);

ALTER TABLE COLUMN_STATISTICS ADD CONSTRAINT COLUMN_STATISTICS_FK1 FOREIGN KEY (TBL_ID) REFERENCES TBLS (TBL_ID) INITIALLY DEFERRED ;
```

```sql
CREATE TABLE PART_COL_STATS
(
CS_ID NUMBER NOT NULL,
PART_ID NUMBER NOT NULL,

DB_NAME VARCHAR(128) NOT NULL,
COLUMN_NAME VARCHAR(128) NOT NULL,
COLUMN_TYPE VARCHAR(128) NOT NULL,
TABLE_NAME VARCHAR(128) NOT NULL,
PART_NAME VARCHAR(128) NOT NULL,

LOW_VALUE RAW,
HIGH_VALUE RAW,
NUM_NULLS BIGINT,
NUM_DISTINCTS BIGINT,

BIT_VECTOR, BLOB,  /* introduced in HIVE-16997 in Hive 3.0.0 */

AVG_COL_LEN DOUBLE,
MAX_COL_LEN BIGINT,
NUM_TRUES BIGINT,
NUM_FALSES BIGINT,
LAST_ANALYZED BIGINT NOT NULL)
```
```sql
ALTER TABLE COLUMN_STATISTICS ADD CONSTRAINT COLUMN_STATISTICS_PK PRIMARY KEY (CS_ID);

ALTER TABLE COLUMN_STATISTICS ADD CONSTRAINT COLUMN_STATISTICS_FK1 FOREIGN KEY (PART_ID) REFERENCES PARTITIONS (PART_ID) INITIALLY DEFERRED;
```

## 4、Metastore Thrift API

> We propose to add the following Thrift structs to transport column statistics:

我们建设添加如下 Thrift 结构体，来传输列统计信息：

```
struct BooleanColumnStatsData {
1: required i64 numTrues,
2: required i64 numFalses,
3: required i64 numNulls
}

struct DoubleColumnStatsData {
1: required double lowValue,
2: required double highValue,
3: required i64 numNulls,
4: required i64 numDVs,

5: optional string bitVectors

}

struct LongColumnStatsData {
1: required i64 lowValue,
2: required i64 highValue,
3: required i64 numNulls,
4: required i64 numDVs,

5: optional string bitVectors
}

struct StringColumnStatsData {
1: required i64 maxColLen,
2: required double avgColLen,
3: required i64 numNulls,
4: required i64 numDVs,

5: optional string bitVectors
}

struct BinaryColumnStatsData {
1: required i64 maxColLen,
2: required double avgColLen,
3: required i64 numNulls
}

struct Decimal {
1: required binary unscaled,
3: required i16 scale
}

struct DecimalColumnStatsData {
1: optional Decimal lowValue,
2: optional Decimal highValue,
3: required i64 numNulls,
4: required i64 numDVs,
5: optional string bitVectors
}

struct Date {
1: required i64 daysSinceEpoch
}

struct DateColumnStatsData {
1: optional Date lowValue,
2: optional Date highValue,
3: required i64 numNulls,
4: required i64 numDVs,
5: optional string bitVectors
}

union ColumnStatisticsData {
1: BooleanColumnStatsData booleanStats,
2: LongColumnStatsData longStats,
3: DoubleColumnStatsData doubleStats,
4: StringColumnStatsData stringStats,
5: BinaryColumnStatsData binaryStats,
6: DecimalColumnStatsData decimalStats,
7: DateColumnStatsData dateStats
}

struct ColumnStatisticsObj {
1: required string colName,
2: required string colType,
3: required ColumnStatisticsData statsData
}

struct ColumnStatisticsDesc {
1: required bool isTblLevel, 
2: required string dbName,
3: required string tableName,
4: optional string partName,
5: optional i64 lastAnalyzed
}

struct ColumnStatistics {
1: required ColumnStatisticsDesc statsDesc,
2: required list<ColumnStatisticsObj> statsObj;
}
```

> We propose to add the following Thrift APIs to persist, retrieve and delete column statistics:

我们建设添加如下 Thrift APIs，来持久化、接收和删除列统计信息：

```
bool update_table_column_statistics(1:ColumnStatistics stats_obj) throws (1:NoSuchObjectException o1, 
2:InvalidObjectException o2, 3:MetaException o3, 4:InvalidInputException o4)
bool update_partition_column_statistics(1:ColumnStatistics stats_obj) throws (1:NoSuchObjectException o1, 
2:InvalidObjectException o2, 3:MetaException o3, 4:InvalidInputException o4)

ColumnStatistics get_table_column_statistics(1:string db_name, 2:string tbl_name, 3:string col_name) throws
(1:NoSuchObjectException o1, 2:MetaException o2, 3:InvalidInputException o3, 4:InvalidObjectException o4) 
ColumnStatistics get_partition_column_statistics(1:string db_name, 2:string tbl_name, 3:string part_name,
4:string col_name) throws (1:NoSuchObjectException o1, 2:MetaException o2, 
3:InvalidInputException o3, 4:InvalidObjectException o4)

bool delete_partition_column_statistics(1:string db_name, 2:string tbl_name, 3:string part_name, 4:string col_name) throws 
(1:NoSuchObjectException o1, 2:MetaException o2, 3:InvalidObjectException o3, 
4:InvalidInputException o4)
bool delete_table_column_statistics(1:string db_name, 2:string tbl_name, 3:string col_name) throws 
(1:NoSuchObjectException o1, 2:MetaException o2, 3:InvalidObjectException o3, 
4:InvalidInputException o4)
```

> Note that delete_column_statistics is needed to remove the entries from the metastore when a table is dropped. Also note that currently Hive doesn’t support drop column.

注意，当一个表被删除时，需要 delete_column_statistics 来从 metastore 中删除条目。还要注意的是，目前 Hive 不支持删除列。

> Note that in V1 of the project, we will support only scalar statistics. Furthermore, we will support only static partitions, i.e., both the partition key and partition value should be specified in the analyze command. In a following version, we will add support for height balanced histograms as well as support for dynamic partitions in the analyze command for column level statistics.

注意，在项目的 V1 中，我们将只支持标量统计信息。

此外，我们将只支持静态分区，也就是说，分区键和分区值都应该在 analyze 命令中指定。

在下一个版本中，我们将添加对高度平衡直方图的支持，以及对列级统计的 analyze 命令中的动态分区的支持。