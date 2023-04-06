# Vectorized Query Execution

[TOC]

## 1、Introduction

> Vectorized query execution is a Hive feature that greatly reduces the CPU usage for typical query operations like scans, filters, aggregates, and joins. A standard query execution system processes one row at a time. This involves long code paths and significant metadata interpretation in the inner loop of execution. Vectorized query execution streamlines operations by processing a block of 1024 rows at a time. Within the block, each column is stored as a vector (an array of a primitive data type). Simple operations like arithmetic and comparisons are done by quickly iterating through the vectors in a tight loop, with no or very few function calls or conditional branches inside the loop. These loops compile in a streamlined way that uses relatively few instructions and finishes each instruction in fewer clock cycles, on average, by effectively using the processor pipeline and cache memory. A detailed design document is attached to the vectorized query execution JIRA, at [https://issues.apache.org/jira/browse/HIVE-4160](https://issues.apache.org/jira/browse/HIVE-4160).

向量化查询执行是 Hive 的一个特性，可以极大地减少典型查询操作(扫描、过滤、聚合、连接)的 CPU 使用。

标准的查询执行系统每次处理一行。这涉及到很长的代码路径和执行内部循环中重要的元数据解释。

向量化查询执行通过一次处理 1024 行数据的块简化了操作。在块中，每个列都存储为向量(一个基本数据类型的数组)。

像算术和比较这样的简单操作是通过在一个紧凑循环中快速遍历向量来完成的，在循环中不调用或很少调用函数或条件分支。

这些循环以一种流线型的方式编译，使用相对较少的指令，平均而言，通过有效地使用处理器管道和缓存内存，在更少的时钟周期内完成每条指令。

详细的设计文档附在向量化查询执行 JIRA 上，网址是 [https://issues.apache.org/jira/browse/HIVE-4160](https://issues.apache.org/jira/browse/HIVE-4160)。

## 2、Using Vectorized Query Execution

### 2.1、Enabling vectorized execution

> To use vectorized query execution, you must store your data in [ORC](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+ORC) format, and set the following variable as shown in Hive SQL (see [Configuring Hive](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-ConfiguringHive)):

为使用向量化查询执行，你必须以 ORC 格式存储数据，并设置如下变量：

	set hive.vectorized.execution.enabled = true;

> Vectorized execution is off by default, so your queries only utilize it if this variable is turned on. To disable vectorized execution and go back to standard execution, do the following:

默认向量化执行是关闭的，因此，只有当这个变量打开时，查询才会使用它。要禁用向量化执行并回到标准执行，请执行以下步骤:

	set hive.vectorized.execution.enabled = false;

> Additional configuration variables for vectorized execution are documented in [Configuration Properties – Vectorization](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-Vectorization).

其他的向量化执行的配置见 Configuration Properties – Vectorization

### 2.2、Supported data types and operations

> The following data types are currently supported for vectorized execution:

以下数据类型目前支持向量化执行:

- tinyint
- smallint
- int
- bigint
- boolean
- float
- double
- decimal
- date
- timestamp (see Limitations below)
- string

> Using other data types will cause your query to execute using standard, row-at-a-time execution.

使用其他数据类型将导致使用标准的、一次一行的执行执行查询。

> The following expressions can be vectorized when used on supported types:

使用支持的类型，下面的表达式可以被向量化:

- arithmetic: +, -, *, /, %
- AND, OR, NOT
- comparisons <, >, <=, >=, =, !=, BETWEEN, IN ( list-of-constants ) as filters
- Boolean-valued expressions (non-filters) using AND, OR, NOT, <, >, <=, >=, =, !=
- IS [NOT] NULL
- all math functions (SIN, LOG, etc.)
- string functions SUBSTR, CONCAT, TRIM, LTRIM, RTRIM, LOWER, UPPER, LENGTH
- type casts
- Hive user-defined functions, including standard and generic UDFs
- date functions (YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, UNIX_TIMESTAMP)
- the IF conditional expression

> User-defined functions are supported using a backward compatibility bridge, so although they do run vectorized, they don't run as fast as optimized vector implementations of built-in operators and functions. Vectorized filter operations are evaluated left-to-right, so for best performance, put UDFs on the right in an ANDed list of expressions in the WHERE clause. E.g., use

使用向后兼容桥，可以支持用户定义的函数，所以，尽管它们可以运行向量化的函数，但它们的运行速度不如内置操作符和函数的优化向量实现快。

向量化过滤器操作是从左到右计算的，因此为了获得最佳性能，请将 UDFs 放在 WHERE 子句中的表达式的 AND 列表的右边。例如，使用

	column1 = 10 and myUDF(column2) = "x"

instead of

	myUDF(column2) = "x" and column1 = 10

> This will allow the optimized filter to run first, potentially eliminating many rows from consideration, before running the UDF via the bridge. The UDF will only be run for rows that pass the filter on the left hand side of the AND operation.

这将允许优化的过滤器在通过桥运行 UDF 之前先运行，这可能会消除许多需要考虑的行。

UDF 将只对通过 AND 操作的左侧的过滤器的行运行。

> Using a built-in operator or function that is not supported for vectorization will cause your query to run in standard row-at-a-time mode. If a compile time or run time error occurs that appears related to vectorization, please file a [Hive JIRA](https://issues.apache.org/jira/browse/HIVE). To work around such an error, disable vectorization by setting hive.vectorized.execution.enabled to false for the specific query that is failing, to run it in standard mode.

使用不支持向量化的内置操作符或函数将导致查询以标准的、一次一行的模式运行。

如果编译时或运行时出现与矢量化相关的错误，请提交 Hive JIRA 。要解决此类错误，对于失败的特定查询，请通过设置 `hive.vectorized.execution.enabled` 为 false 来禁用向量化，以标准模式运行它。

> Vectorized support continues to be added for additional functions and expressions. If you have a request for one, please comment on this page, or open a JIRA for it.

对其他函数和表达式的矢量化支持将继续增加。

### 2.3、Seeing whether vectorization is used for a query

> You can verify which parts of your query are being vectorized using the [explain](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Explain) feature. For example, when Fetch is used in the plan instead of Map, it does not vectorize and the explain output will not include the "Vectorized execution: true" notation:

可以使用 explain 特性验证查询的哪些部分正在被向量化。

例如，当在 plan 中使用 Fetch 而不是 Map 时，它不会向量化，explain 输出也不会包含 "Vectorized execution: true":

```sql
create table vectorizedtable(state string,id int) stored as orc;
 
insert into vectorizedtable values('haryana',1);
set hive.vectorized.execution.enabled = true;
explain select count(*) from vectorizedtable;
```

> The explain output contains this:

	STAGE PLANS:
	  Stage: Stage-1
	    Map Reduce
	      Alias -> Map Operator Tree:
	        alltypesorc
	          TableScan
	            alias: vectorizedtable
	             Statistics: Num rows: 1 Data size: 95 Basic stats: COMPLETE Column stats: COMPLETE
	            Select Operator
	              Statistics: Num rows: 1 Data size: 95 Basic stats: COMPLETE Column stats: COMPLETE
	              Group By Operator
	                aggregations: count()
	                mode: hash
	                outputColumnNames: _col0
	                Statistics: Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE
	                Reduce Output Operator
	                  sort order:
	                  Statistics: Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE
	                  value expressions: _col0 (type: bigint)
	      Execution mode: vectorized
	      Reduce Operator Tree:
	        Group By Operator
	          aggregations: count(VALUE._col0)
	          mode: mergepartial
	          outputColumnNames: _col0
	          Statistics: Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE
	          File Output Operator
	            compressed: false
	            Statistics: Num rows: 1 Data size: 8 Basic stats: COMPLETE Column stats: COMPLETE
	            table:
	                input format: org.apache.hadoop.mapred.TextInputFormat
	                output format: org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat
	                serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe
	  Stage: Stage-0
	    Fetch Operator
	      limit: -1
	      Processor Tree:
	        ListSink

> The notation `Vectorized execution: true` shows that the operator containing that notation is vectorized. Absence of this notation means the operator is not vectorized, and uses the standard row-at-a-time execution path.

`Vectorized execution: true` 表明包含那个的符号的操作符被向量化。缺少这个符号意味着操作符不被向量化，而使用标准的、一次一行的执行路径。

> Note: In case you want to use vectorized execution for fetch then 

你想使用向量化的 fetch 执行的情况：

	set hive.fetch.task.conversion=none

## 3、Limitations

> Timestamps only work correctly with vectorized execution if the timestamp value is between 1677-09-20 and 2262-04-11. This limitation is due to the fact that a vectorized timestamp value is stored as a long value representing nanoseconds before/after the Unix Epoch time of 1970-01-01 00:00:00 UTC. Also see [HIVE-9862](https://issues.apache.org/jira/browse/HIVE-9862).

只有当时间戳值在 1677-09-20 和 2262-04-11 之间时，时间戳才能正确地使用向量化执行。

这种限制是由于向量化的时间戳值存储为一个长值，表示在 Unix 纪元时间 1970-01-01 00:00:00 UTC 之前或之后的纳秒。

## 4、Version Information

> Vectorized execution is available in Hive 0.13.0 and later ([HIVE-5283](https://issues.apache.org/jira/browse/HIVE-5283)).

从 Hive 0.13.0 开始可用。