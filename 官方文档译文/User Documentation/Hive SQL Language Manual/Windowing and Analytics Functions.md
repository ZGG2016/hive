# LanguageManual WindowingAndAnalytics

[TOC]

## 1、Enhancements to Hive QL

> Version:Introduced in Hive version 0.11.

版本：在 Hive 0.11 版本引入。

> This section introduces the Hive QL enhancements for windowing and analytics functions. See ["Windowing Specifications in HQL"](https://issues.apache.org/jira/secure/attachment/12575830/WindowingSpecification.pdf) (attached to [HIVE-4197](https://issues.apache.org/jira/browse/HIVE-4197)) for details. [HIVE-896](https://issues.apache.org/jira/browse/HIVE-896) has more information, including links to earlier documentation in the initial comments.

本节介绍 Hive QL 对窗口和分析功能的增强。详见“HQL窗口规范”(附在 HIVE-4197)。HIVE-896 有更多的信息，包括早期文档的链接。

> All of the windowing and analytics functions operate as per the SQL standard.

所有的窗口和分析函数都按照 SQL 标准操作。

> The current release supports the following functions for windowing and analytics:

当前版本支持以下窗口和分析函数：

1. Windowing functions 窗口函数

- LEAD

	- 可以可选地指定要向后偏移的行数。如果未指定，则向后偏移一行。

	- 当当前行向后扩展到窗口的末端时，返回 null。

> The number of rows to lead can optionally be specified. If the number of rows to lead is not specified, the lead is one row.

> Returns null when the lead for the current row extends beyond the end of the window.

- LAG

	- 可以可选地指定要向前偏移的行数。如果未指定，则向前偏移一行。

	- 当当前行向前扩展到窗口的开始时，返回 null。

> The number of rows to lag can optionally be specified. If the number of rows to lag is not specified, the lag is one row.

> Returns null when the lag for the current row extends before the beginning of the window.

- FIRST_VALUE

	- 这个最多接受两个参数。第一个参数是你想要第一个值的列。第二个参数（可选地）必须是布尔型，默认是 false。如果设为 true，它将跳过 null 值。

> This takes at most two parameters. The first parameter is the column for which you want the first value, the second (optional) parameter must be a boolean which is false by default. If set to true it skips null values.

- LAST_VALUE

	- 这个最多接受两个参数。第一个参数是你想要最后一个值的列。第二个参数（可选地）必须是布尔型，默认是 false。如果设为 true，它将跳过 null 值。

> This takes at most two parameters. The first parameter is the column for which you want the last value, the second (optional) parameter must be a boolean which is false by default. If set to true it skips null values.

2. The OVER clause OVER子句

> OVER with standard aggregates:

- OVER 子句和标准聚合：

	- COUNT
	- SUM
	- MIN
	- MAX
	- AVG

> OVER with a PARTITION BY statement with one or more partitioning columns of any primitive datatype.

- OVER 子句和带有任意基本数据类型的一个或多个分区列的 PARTITION BY 语句。

> OVER with PARTITION BY and ORDER BY with one or more partitioning and/or ordering columns of any datatype.

> OVER with a window specification. Windows can be defined separately in a WINDOW clause. Window specifications support the following formats:

- OVER 子句和使用任何数据类型的一个或多个分区和/或排序列的 PARTITION BY 和 ORDER BY。

	- OVER 子句和窗口规范。可以在 WINDOW 子句中单独定义窗口。窗口规范支持以下格式:
	```
	(ROWS | RANGE) BETWEEN (UNBOUNDED | [num]) PRECEDING AND ([num] PRECEDING | CURRENT ROW | (UNBOUNDED | [num]) FOLLOWING)
	(ROWS | RANGE) BETWEEN CURRENT ROW AND (CURRENT ROW | (UNBOUNDED | [num]) FOLLOWING)
	(ROWS | RANGE) BETWEEN [num] FOLLOWING AND (UNBOUNDED | [num]) FOLLOWING
	```
> When ORDER BY is specified with missing WINDOW clause, the WINDOW specification defaults to RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW.

当 ORDER BY 用缺失的 WINDOW 子句指定时，WINDOW 规范默认是 RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW。

> When both ORDER BY and WINDOW clauses are missing, the WINDOW specification defaults to ROW BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING.

当 ORDER BY 和 WINDOW 子句都缺失时，WINDOW 规范默认为 ROW BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING。

> The OVER clause supports the following functions, but it does not support a window with them (see [HIVE-4797](https://issues.apache.org/jira/browse/HIVE-4797)):Ranking functions: Rank, NTile, DenseRank, CumeDist, PercentRank.Lead and Lag functions.

**OVER 子句支持以下函数，但不支持使用它们的窗口**：

（1）排名功能:Rank, NTile, DenseRank, CumeDist, PercentRank。

（2）Lead 和 Lag 功能。

3. Analytics functions 分析函数

- RANK
- ROW_NUMBER
- DENSE_RANK
- CUME_DIST
- PERCENT_RANK
- NTILE

4. Distinct support in Hive 2.1.0 and later (see [HIVE-9534](https://issues.apache.org/jira/browse/HIVE-9534)) 

Hive 2.1.0 版本及更高版本中，支持 Distinct

> Distinct is supported for aggregation functions including SUM, COUNT and AVG, which aggregate over the distinct values within each partition. Current implementation has the limitation that no ORDER BY or window specification can be supported in the partitioning clause for performance reason. The supported syntax is as follows.

Distinct 支持聚合函数，包括 SUM、COUNT 和 AVG，它们对每个分区内的不同值进行聚合。

由于性能原因，当前的实现有一个限制，即分区子句中不支持 ORDER BY 或 window 规范。支持的语法如下所示。

```sql
COUNT(DISTINCT a) OVER (PARTITION BY c)
```

> ORDER BY and window specification is supported for distinct in Hive 2.2.0 (see [HIVE-13453](https://issues.apache.org/jira/browse/HIVE-13453)). An example is as follows.

**在 Hive 2.2.0 中，distinct 开始支持 ORDER BY 和 window 规范**。

```sql
COUNT(DISTINCT a) OVER (PARTITION BY c ORDER BY d ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING)
```

5. Aggregate functions in OVER clause support in Hive 2.1.0 and later (see [HIVE-13475](HIVE-13475)) 

Hive 2.1.0 版本及更高版本中，**在 OVER 子句中支持聚合函数**

> Support to reference aggregate functions within the OVER clause has been added. For instance, currently we can use the SUM aggregation function within the OVER clause as follows.

增加了在 OVER 子句中引用聚合函数的支持。例如，目前我们可以在 OVER 子句中使用 SUM 聚合函数，如下所示。

```sql
SELECT rank() OVER (ORDER BY sum(b))
FROM T
GROUP BY a;
```

## 2、Examples

> This section provides examples of how to use the Hive QL windowing and analytics functions in SELECT statements. See [HIVE-896](https://issues.apache.org/jira/browse/HIVE-896) for additional examples.

介绍如何在 SELECT 语句中使用 Hive QL  窗口和分析函数。

### 2.1、PARTITION BY with one partitioning column, no ORDER BY or window specification

```sql
SELECT a, COUNT(b) OVER (PARTITION BY c)
FROM T;
```

### 2.2、PARTITION BY with two partitioning columns, no ORDER BY or window specification

```sql
SELECT a, COUNT(b) OVER (PARTITION BY c, d)
FROM T;
```

### 2.3、PARTITION BY with one partitioning column, one ORDER BY column, and no window specification

```sql
SELECT a, SUM(b) OVER (PARTITION BY c ORDER BY d)
FROM T;
```

### 2.4、PARTITION BY with two partitioning columns, two ORDER BY columns, and no window 
specification

```sql
SELECT a, SUM(b) OVER (PARTITION BY c, d ORDER BY e, f)
FROM T;
```

### 2.5、PARTITION BY with partitioning, ORDER BY, and window specification

```sql
SELECT a, SUM(b) OVER (PARTITION BY c ORDER BY d ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
FROM T;

SELECT a, AVG(b) OVER (PARTITION BY c ORDER BY d ROWS BETWEEN 3 PRECEDING AND CURRENT ROW)
FROM T;

SELECT a, AVG(b) OVER (PARTITION BY c ORDER BY d ROWS BETWEEN 3 PRECEDING AND 3 FOLLOWING)
FROM T;

SELECT a, AVG(b) OVER (PARTITION BY c ORDER BY d ROWS BETWEEN CURRENT ROW AND UNBOUNDED FOLLOWING)
FROM T;
```

> There can be multiple OVER clauses in a single query. A single OVER clause only applies to the immediately preceding function call. In this example, the first OVER clause applies to COUNT(b) and the second OVER clause applies to SUM(b):

在一个查询中，可以有多个 OVER 子句。一个 OVER 子句仅适用于紧接在前面的函数调用。

在这个例子中，第一个 OVER 子句应用到 COUNT(b)，第二个 OVER 子句应用到 SUM(b)

```sql
SELECT 
 a,
 COUNT(b) OVER (PARTITION BY c),
 SUM(b) OVER (PARTITION BY c)
FROM T;
```

> Aliases can be used as well, with or without the keyword AS:

也可以使用别名，可以有 AS，也可以没有：

```sql
SELECT 
 a,
 COUNT(b) OVER (PARTITION BY c) AS b_count,
 SUM(b) OVER (PARTITION BY c) b_sum
FROM T;
```

### 2.6、WINDOW clause

```sql
SELECT a, SUM(b) OVER w
FROM T
WINDOW w AS (PARTITION BY c ORDER BY d ROWS UNBOUNDED PRECEDING);
```

### 2.7、LEAD using default 1 row lead and not specifying default value

```sql
SELECT a, LEAD(a) OVER (PARTITION BY b ORDER BY C)
FROM T;
```

### 2.8、LAG specifying a lag of 3 rows and default value of 0

```sql
SELECT a, LAG(a, 3, 0) OVER (PARTITION BY b ORDER BY C)
FROM T;
```

### 2.9、Distinct counting for each partition

```sql
SELECT a, COUNT(distinct a) OVER (PARTITION BY b)
FROM T;
```
