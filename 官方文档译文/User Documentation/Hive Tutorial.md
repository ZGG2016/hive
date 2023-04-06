# Hive Tutorial

[TOC]

## 1、Concepts

### 1.1、What Is Hive

> Hive is a data warehousing infrastructure based on [Apache Hadoop](http://hadoop.apache.org/). Hadoop provides massive scale out and fault tolerance capabilities for data storage and processing on commodity hardware.

Hive 是一个**基于 Apache Hadoop 的数据仓库**基础设施。

Hadoop 为商业硬件上的存储和处理数据提供了大规模的向外扩展和容错能力。

> Hive is designed to enable easy data summarization, ad-hoc querying and analysis of large volumes of data. It provides SQL which enables users to do ad-hoc querying, summarization and data analysis easily. At the same time, Hive's SQL gives users multiple places to integrate their own functionality to do custom analysis, such as User Defined Functions (UDFs).  

Hive 可以方便地对大量数据进行汇总、ad-hoc 查询和分析。

它**提供了 SQL，使用户能够轻松地进行 ad-hoc 查询、汇总和数据分析**。同时，Hive 的 SQL 给用户提供了多个地方来**集成他们自己的功能**来做自定义分析，比如用户定义函数(UDFs)。

### 1.2、What Hive Is NOT

> Hive is not designed for online transaction processing.  It is best used for traditional data warehousing tasks.

Hive **不是为在线事务处理而设计的。它最适合用于传统的数据仓库任务**。【联机分析处理OLAP】

### 1.3、Getting Started

> For details on setting up Hive, HiveServer2, and Beeline, please refer to the GettingStarted guide.

关于 Hive、HiveServer2 和 Beeline 的详细设置，请参考 GettingStarted guide。

> Books about Hive lists some books that may also be helpful for getting started with Hive.

关于 Hive 的书中列出了一些可能对开始使用 Hive 有帮助的书。

> In the following sections we provide a tutorial on the capabilities of the system. We start by describing the concepts of data types, tables, and partitions (which are very similar to what you would find in a traditional relational DBMS) and then illustrate the capabilities of Hive with the help of some examples.

在下面的章节中，我们将提供关于该系统功能的教程。我们首先描述数据类型、表和分区的概念(与传统关系 DBMS 非常相似)，然后通过一些例子说明 Hive 的功能。

### 1.4、Data Units

> In the order of granularity - Hive data is organized into:

hive 数据有如下组织形式：

> Databases: Namespaces function to avoid naming conflicts for tables, views, partitions, columns, and so on.  Databases can also be used to enforce security for a user or group of users.

- **数据库**：

	名称空间函数，以避免表、视图、分区、列等的命名冲突。

	数据库还可以用于为一个用户或一组用户实施安全性。

> Tables: Homogeneous units of data which have the same schema. An example of a table could be page_views table, where each row could comprise of the following columns (schema):

> timestamp—which is of INT type that corresponds to a UNIX timestamp of when the page was viewed.
userid —which is of BIGINT type that identifies the user who viewed the page.
page_url—which is of STRING type that captures the location of the page.
referer_url—which is of STRING that captures the location of the page from where the user arrived at the current page.
IP—which is of STRING type that captures the IP address from where the page request was made.

- **表**：

	具有相同 schema 的同种数据单元。

	一个示例就是 page_views 表，表中的每行都由下面的列组成：

		- timestamp：INT类型，页面浏览时间
		- userid：BIGINT类型，
		- page_url：STRING类型，
		- referer_url：STRING类型，
		- IP：STRING类型，

> Partitions: Each Table can have one or more partition Keys which determines how the data is stored. Partitions—apart from being storage units—also allow the user to efficiently identify the rows that satisfy a specified criteria; for example, a date_partition of type STRING and country_partition of type STRING. Each unique value of the partition keys defines a partition of the Table. For example, all "US" data from "2009-12-23" is a partition of the page_views table. Therefore, if you run analysis on only the "US" data for 2009-12-23, you can run that query only on the relevant partition of the table, thereby speeding up the analysis significantly. Note however, that just because a partition is named 2009-12-23 does not mean that it contains all or only data from that date; partitions are named after dates for convenience; it is the user's job to guarantee the relationship between partition name and data content! Partition columns are virtual columns, they are not part of the data itself but are derived on load.

- **分区**：

	每个表有一个或多个分区 key ，这些 key 决定了数据如何被存储。

	**除了作为存储单元之外，分区还允许用户有效地标识满足指定条件的行**【过滤掉一部分数据，提高查询效率】。例如，STRING 类型的 date_partition 和 STRING 类型的 country_partition。

	**分区 key 的每个唯一值对应表的一个分区**。例如，从 "2009-12-23" 开始的所有 "US" 下的数据都是 page_views 表的一个分区中的数据。因此，如果基于 "US" 下 2009-12-23 的数据分析，你可以只在相关分区下执行查询。从而，可以提高分析效率。

	然而，**仅仅因为一个分区命名为 2009-12-23，并不意味着它包含或仅包含该日期的所有数据，分区以日期命名是为了方便。**

	保证分区名和数据内容之间的关系是用户的工作！**分区列是虚拟列，它们不是数据本身的一部分**，而是在加载时派生的。

> Buckets (or Clusters): Data in each partition may in turn be divided into Buckets based on the value of a hash function of some column of the Table. For example the page_views table may be bucketed by userid, which is one of the columns, other than the partitions columns, of the page_view table. These can be used to efficiently sample the data.

- **分桶**:

	**通过计算表的某些列的 hash 值，分区中的数据再被划分到桶中。这可以被用来高效地抽样数据。**

	例如，page_views 表根据 userid 分桶，**userid 是 page_view 表的列之一，而不是分区列**。

> Note that it is not necessary for tables to be partitioned or bucketed, but these abstractions allow the system to prune large quantities of data during query processing, resulting in faster query execution.

**分区或分桶并不是必要的**。但是这些抽象允许系统在查询处理期间删除大量数据，从而加快查询的执行。

### 1.5、Type System

> Hive supports primitive and complex data types, as described below. See [Hive Data Types](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types) for additional information.

Hive 支持基本和复杂数据类型，如下所述。有关更多信息，请参阅 Hive Data Types。

#### 1.5.1、Primitive Types

> Types are associated with the columns in the tables. The following Primitive types are supported:

在表中，类型和列相关联，下面是支持的基本数据类型：

- Integers

	- TINYINT—1 byte integer
	- SMALLINT—2 byte integer
	- INT—4 byte integer
	- BIGINT—8 byte integer

- Boolean type
	
	- BOOLEAN—TRUE/FALSE

- Floating point numbers

	- FLOAT—single precision
	- DOUBLE—Double precision

- Fixed point numbers

	- DECIMAL—a fixed point value of user defined scale and precision

- String types

	- STRING—sequence of characters in a specified character set
	- VARCHAR—sequence of characters in a specified character set with a maximum length
	- CHAR—sequence of characters in a specified character set with a defined length

- Date and time types

	- TIMESTAMP — A date and time without a timezone ("LocalDateTime" semantics)
	- TIMESTAMP WITH LOCAL TIME ZONE — A point in time measured down to nanoseconds ("Instant" semantics)
	- DATE—a date

- Binary types

	- BINARY—a sequence of bytes

> The Types are organized in the following hierarchy (where the parent is a super type of all the children instances):

这些类型按以下层次结构组织(父实例是所有子实例的超类型)：

- Type
	- Primitive Type
		- Number
			- DOUBLE
				- FLOAT
					- BIGINT
						- INT
							- SMALLINT
								- TINYINT

				- STRING

			- BOOLEAN

> This type hierarchy defines how the types are implicitly converted in the query language. Implicit conversion is allowed for types from child to an ancestor. So when a query expression expects type1 and the data is of type2, type2 is implicitly converted to type1 if type1 is an ancestor of type2 in the type hierarchy. Note that the type hierarchy allows the implicit conversion of STRING to DOUBLE.

这种类型层次结构定义了如何在查询语言中隐式地转换类型。

**允许从子类型到祖先类型的隐式转换**。

因此，当查询表达式期望类型1且数据为类型2时，如果在类型层次结构中，类型1是类型2的祖先，则类型2将隐式转换为类型1。

注意，类型层次结构允许隐式地将 STRING 转换为 DOUBLE。

> Explicit type conversion can be done using the cast operator as shown in the [#Built In Functions](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInFunctions) section below.

**显式类型转换可以使用强制转换操作符完成**，如下面的 #Built in Functions 一节所示。

#### 1.5.2、Complex Types

> Complex Types can be built up from primitive types and other composite types using:

复杂类型可以从基本类型和其他组合类型构建：

> Structs: the elements within the type can be accessed using the DOT (.) notation. For example, for a column c of type STRUCT {a INT; b INT}, the a field is accessed by the expression c.a

- Structs：类型中的元素可以**使用点号访问**。

	例如，c 列的类型是 `STRUCT {a INT; b INT}`，通过 `c.a` 访问字段。

> Maps (key-value tuples): The elements are accessed using ['element name'] notation. For example in a map M comprising of a mapping from 'group' -> gid the gid value can be accessed using M['group']

- Maps（键值元组）：**使用 `['element name']` 访问元素**。

	例如，映射 M 由 `'group' -> gid` 组成，可以通过 `M['group']` 访问 gid 值。

> Arrays (indexable lists): The elements in the array have to be in the same type. Elements can be accessed using the [n] notation where n is an index (zero-based) into the array. For example, for an array A having the elements ['a', 'b', 'c'], A[1] retruns 'b'.

- Arrays（可索引的列表）：数组中的元素必须具有相同的类型。可以**使用 `[n]` 来访问元素，n 是索引（从0开始）**。

	例如，数组 A 有元素 `['a', 'b', 'c']`，那么 A[1] 将返回 'b'。

> Using the primitive types and the constructs for creating complex types, types with arbitrary levels of nesting can be created. For example, a type User may comprise of the following fields:

**使用基本类型和用于创建复杂类型的构造，可以创建具有任意嵌套级别的类型**。例如，一个类型用户可能包含以下字段:

- gender—which is a STRING.
- active—which is a BOOLEAN.

#### 1.5.3、Timestamp

> Timestamps have been the source of much confusion, so we try to document the intended semantics of Hive.

Timestamps 一直是很多困惑的根源，所以我们试图记录计划的 Hive 的语义。

	Timestamp ("LocalDateTime" semantics)

> Java's "LocalDateTime" timestamps record a date and time as year, month, date, hour, minute, and seconds without a timezone. These timestamps always have those same values regardless of the local time zone.

Java 的 “LocalDateTime” 时间戳将日期和时间记录为年、月、日、时、分和秒，而没有时区。

无论本地时区是什么，这些时间戳总是具有相同的值。【和时区无关】

> For example, the timestamp value of "2014-12-12 12:34:56" is decomposed into year, month, day, hour, minute and seconds fields, but with no time zone information available. It does not correspond to any specific instant. It will always be the same value regardless of the local time zone. Unless your application uses UTC consistently, timestamp with local time zone is strongly preferred over timestamp for most applications. When users say an event is at 10:00, it is always in reference to a certain timezone and means a point in time, rather than 10:00 in an arbitrary time zone.

例如，“2014-12-12 12:34:56” 的时间戳值被分解为年、月、日、小时、分钟和秒字段，但是没有时区信息可用。

它**不对应于任何特定的时刻。它将始终是相同的值，无论当地时区是什么**。除非你的应用程序一致使用 UTC，否则具有当地时区的时间戳对于大多数应用程序来说都比时间戳更受欢迎。

当用户说一个事件发生在 10:00 时，它总是与某个时区有关，意思是一个时间点，而不是任意时区中的 10:00。

	Timestamp with local time zone ("Instant" semantics)

> Java's "Instant" timestamps define a point in time that remains constant regardless of where the data is read. Thus, the timestamp will be adjusted by the local time zone to match the original point in time.

Java 的 "Instant" 时间戳定义了一个无论从何处读取数据都保持不变的时间点。因此，**这个时间戳将根据当地时区调整，以匹配原始的时间点**。

Type | Value in America/Los_Angeles | Value in America/New York
---|:---|:---
timestamp | 2014-12-12 12:34:56 | 2014-12-12 12:34:56
timestamp with local time zone | 2014-12-12 12:34:56 | 2014-12-12 15:34:56

**Comparisons with other tools**

见原文：[https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions)

### 1.6、Built In Operators and Functions

> The operators and functions listed below are not necessarily up to date. ([Hive Operators and UDFs](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF) has more current information.) In [Beeline](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-Beeline%E2%80%93NewCommandLineShell) or the Hive [CLI](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-Beeline%E2%80%93NewCommandLineShell), use these commands to show the latest documentation:

下面列出的操作符和函数不一定是最新的。

在 Beeline 或 Hive CLI 中，使用这些命令显示最新的文档:

```sql
SHOW FUNCTIONS;
DESCRIBE FUNCTION <function_name>;
DESCRIBE FUNCTION EXTENDED <function_name>;
```

> Case-insensitive. All Hive keywords are case-insensitive, including the names of Hive operators and functions.

不区分大小写。**所有 Hive 关键字不区分大小写**，包括 Hive 操作符和函数的名称。

----------------------------------------------

```sql
hive> show functions;
OK
!
!=
$sum0
%
&
*
+
.....
abs
and
array

hive> describe function array;
OK
array(n0, n1...) - Creates an array with the given elements 

hive> describe function extended array;
OK
array(n0, n1...) - Creates an array with the given elements 
Function class:org.apache.hadoop.hive.ql.udf.generic.GenericUDFArray
Function type:BUILTIN
```

----------------------------------------------

#### 1.6.1、Built In Operators

> Relational Operators:The following operators compare the passed operands and generate a TRUE or FALSE value, depending on whether the comparison between the operands holds or not.

- 关系操作符：下面的操作符比较传入的操作数，生成一个 TRUE 或 FALSE 值，这取决于操作数之间的比较是否有效。

	操作符表格见原文：[https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions)

> Arithmetic Operators:The following operators support various common arithmetic operations on the operands. All of them return number types.

- 算术操作符：以下操作符支持对操作数进行各种常见的算术操作。它们都返回 number 类型。

	操作符表格见原文：[https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions)

> Logical Operators:The following operators provide support for creating logical expressions. All of them return boolean TRUE or FALSE depending upon the boolean values of the operands.

- 逻辑操作符：以下操作符提供了对创建逻辑表达式的支持。它们都返回布尔值 TRUE 或 FALSE，这取决于操作数的布尔值。

	操作符表格见原文：[https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions)

> Operators on Complex Types:The following operators provide mechanisms to access elements in Complex Types

- 在复杂类型上的操作符：下面的操作符提供了访问复杂类型中元素的机制。

	操作符表格见原文：[https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions)

#### 1.6.2、Built In Functions

> Hive supports the following built in functions:([Function list in source code: FunctionRegistry.java](http://svn.apache.org/viewvc/hive/trunk/ql/src/java/org/apache/hadoop/hive/ql/exec/FunctionRegistry.java?view=markup))

- Hive 支持下面的内建函数：

	函数表格见原文：[https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions)

> The following built in aggregate functions are supported in Hive:

- Hive 中支持下面内建的聚合函数：

	函数表格见原文：[https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-BuiltInOperatorsandFunctions)

### 1.7、Language Capabilities

> [Hive's SQL](https://cwiki.apache.org/confluence/display/Hive/LanguageManual) provides the basic SQL operations. These operations work on tables or partitions. These operations are:

Hive 的 SQL 提供了基本的 SQL 操作。这些操作在表或分区上工作。这些操作是:

- 能够使用 WHERE 子句从表中过滤行。

- 能够使用 SELECT 子句从表中选择某些列。

- 能够在两个表之间进行 equi-joins。

- 能够对存储在一个表中的数据的多个 “group by” 列上的聚合进行评估。

- 能够将查询结果存储到另一个表中。

- 能够将表的内容下载到本地目录(例如，nfs)。

- 能够将查询结果存储在 hadoop dfs 目录中。

- 能够管理表和分区(创建、删除和修改)。

- 能够插入自定义脚本的语言选择自定义映射/减少作业。

> Ability to filter rows from a table using a WHERE clause.
> Ability to select certain columns from the table using a SELECT clause.
> Ability to do equi-joins between two tables.
> Ability to evaluate aggregations on multiple "group by" columns for the data stored in a table.
> Ability to store the results of a query into another table.
> Ability to download the contents of a table to a local (for example,, nfs) directory.
> Ability to store the results of a query in a hadoop dfs directory.
> Ability to manage tables and partitions (create, drop and alter).
> Ability to plug in custom scripts in the language of choice for custom map/reduce jobs.

## 2、Usage and Examples

> NOTE: Many of the following examples are out of date.  More up to date information can be found in the [LanguageManual](https://cwiki.apache.org/confluence/display/Hive/LanguageManual).

注意：下面的许多例子是过时的。可以在 LanguageManual 查找到更多信息。

> The following examples highlight some salient features of the system. A detailed set of query test cases can be found at [Hive Query Test Cases](http://svn.apache.org/viewvc/hive/trunk/ql/src/test/queries/clientpositive/) and the corresponding results can be found at [Query Test Case Results](http://svn.apache.org/viewvc/hive/trunk/ql/src/test/results/clientpositive/).

下面的例子突出了该系统的一些显著特征。

详细的查询测试用例集可以在 Hive Query Test Cases 中找到，相应的结果可以在 Query Test Case Results 中找到。

- Creating, Showing, Altering, and Dropping Tables
- Loading Data
- Querying and Inserting Data

### 2.1、Creating, Showing, Altering, and Dropping Tables

> See [Hive Data Definition Language](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL) for detailed information about creating, showing, altering, and dropping tables.

见 Hive Data Definition Language 查看更多关于创建、展示、修改和删除表的信息。

#### 2.1.1、Creating Tables

> An example statement that would create the page_view table mentioned above would be like:

创建上面提到的 page_view 表的示例语句如下:

```sql
CREATE TABLE page_view(viewTime INT, userid BIGINT,
                page_url STRING, referrer_url STRING,
                ip STRING COMMENT 'IP Address of the User')
COMMENT 'This is the page view table'
PARTITIONED BY(dt STRING, country STRING)
STORED AS SEQUENCEFILE;
```

> In this example, the columns of the table are specified with the corresponding types. Comments can be attached both at the column level as well as at the table level. Additionally, the partitioned by clause defines the partitioning columns which are different from the data columns and are actually not stored with the data. When specified in this way, the data in the files is assumed to be delimited with ASCII 001(ctrl-A) as the field delimiter and newline as the row delimiter.

在本例中，表的列使用相应的类型指定。

可以在列级别和表级别添加注释。

此外，partitioned by 子句定义了分区列，分区列与数据列不同，实际上并不与数据一起存储。

当以这种方式指定时，假设文件中的数据以 ASCII 001(ctrl-A)作为字段分隔符，以换行分隔符作为行分隔符。

> The field delimiter can be parametrized if the data is not in the above format as illustrated in the following example:

如果数据不是上述格式，则可以参数化字段分隔符：

```sql
CREATE TABLE page_view(viewTime INT, userid BIGINT,
                page_url STRING, referrer_url STRING,
                ip STRING COMMENT 'IP Address of the User')
COMMENT 'This is the page view table'
PARTITIONED BY(dt STRING, country STRING)
ROW FORMAT DELIMITED
        FIELDS TERMINATED BY '1'
STORED AS SEQUENCEFILE;
```

> The row delimintor currently cannot be changed since it is not determined by Hive but Hadoop delimiters.

行分隔符目前不能更改，因为它不是由 Hive 决定的，而是 Hadoop 决定的。

> It is also a good idea to bucket the tables on certain columns so that efficient sampling queries can be executed against the data set. If bucketing is absent, random sampling can still be done on the table but it is not efficient as the query has to scan all the data. The following example illustrates the case of the page_view table that is bucketed on the userid column:

在某些列上对表进行分桶是个好主意，以便对数据集执行有效的抽样查询。

如果不使用分桶，对表的表随机抽样仍然可以做，但不是有效的查询，必须扫描所有的数据。

下面的例子说明了 page_view 表在 userid 列上分桶的情况:

```sql
CREATE TABLE page_view(viewTime INT, userid BIGINT,
                page_url STRING, referrer_url STRING,
                ip STRING COMMENT 'IP Address of the User')
COMMENT 'This is the page view table'
PARTITIONED BY(dt STRING, country STRING)
CLUSTERED BY(userid) SORTED BY(viewTime) INTO 32 BUCKETS
ROW FORMAT DELIMITED
        FIELDS TERMINATED BY '1'
        COLLECTION ITEMS TERMINATED BY '2'
        MAP KEYS TERMINATED BY '3'
STORED AS SEQUENCEFILE;
```

> In the example above, the table is clustered by a hash function of userid into 32 buckets. Within each bucket the data is sorted in increasing order of viewTime. Such an organization allows the user to do efficient sampling on the clustered column—n this case userid. The sorting property allows internal operators to take advantage of the better-known data structure while evaluating queries with greater efficiency.

在上面的例子中，表被 userid 的哈希函数聚集到 32 个桶中。在每个桶中，数据按 viewTime 的递增顺序排序。

这样的组织允许用户在聚集列上进行有效的抽样，在本例中为 userid。

排序属性允许内部操作符利用已知的数据结构，同时以更高的效率计算查询。

```sql
CREATE TABLE page_view(viewTime INT, userid BIGINT,
                page_url STRING, referrer_url STRING,
                friends ARRAY<BIGINT>, properties MAP<STRING, STRING>
                ip STRING COMMENT 'IP Address of the User')
COMMENT 'This is the page view table'
PARTITIONED BY(dt STRING, country STRING)
CLUSTERED BY(userid) SORTED BY(viewTime) INTO 32 BUCKETS
ROW FORMAT DELIMITED
        FIELDS TERMINATED BY '1'
        COLLECTION ITEMS TERMINATED BY '2'
        MAP KEYS TERMINATED BY '3'
STORED AS SEQUENCEFILE;
```

> In this example, the columns that comprise of the table row are specified in a similar way as the definition of types. Comments can be attached both at the column level as well as at the table level. Additionally, the partitioned by clause defines the partitioning columns which are different from the data columns and are actually not stored with the data. The CLUSTERED BY clause specifies which column to use for bucketing as well as how many buckets to create. The delimited row format specifies how the rows are stored in the hive table. In the case of the delimited format, this specifies how the fields are terminated, how the items within collections (arrays or maps) are terminated, and how the map keys are terminated. STORED AS SEQUENCEFILE indicates that this data is stored in a binary format (using hadoop SequenceFiles) on hdfs. The values shown for the ROW FORMAT and STORED AS clauses in the above, example represent the system defaults.

在本例中，由表行组成的列的指定方式与类型的定义类似。

可以在列级和表级附加注释。

此外，partitioned by 子句定义了分区列，分区列与数据列不同，实际上并不与数据一起存储。

CLUSTERED BY 子句指定使用哪个列进行分桶以及创建多少个桶。

分隔的行格式指定了行在 hive 表中的存储方式。

对于分隔格式，它指定如何终止字段，集合(数组或映射)中的项如何终止，以及如何终止映射键。

STORED AS SEQUENCEFILE 表示该数据以二进制格式(使用hadoop SequenceFiles)存储在 hdfs 上。

上面示例中显示的 ROW FORMAT 和 STORED AS 子句的值表示系统默认值。

> Table names and column names are case insensitive.

表名和列名不区分大小写。

### 2.1.2、Browsing Tables and Partitions

	SHOW TABLES;

> To list existing tables in the warehouse; there are many of these, likely more than you want to browse.

列出仓库中已存在的表；有很多，可能比你想浏览的要多。

	SHOW TABLES 'page.*';

> To list tables with prefix 'page'. The pattern follows Java regular expression syntax (so the period is a wildcard).

列出以 “page” 为前缀的表。该模式遵循 Java 正则表达式语法(因此句点是通配符)。

	SHOW PARTITIONS page_view;

> To list partitions of a table. If the table is not a partitioned table then an error is thrown.

列出一个表的分区。如果表不是一个分区表，那么就抛出一个错误。

	DESCRIBE page_view;

> To list columns and column types of table.

列出表的列和列类型。

	DESCRIBE EXTENDED page_view;

> To list columns and all other properties of table. This prints lot of information and that too not in a pretty format. Usually used for debugging.

列出表的列和所有其他的属性。这打印很多信息，那也不是一个好看的格式。通常用于调试。

	DESCRIBE EXTENDED page_view PARTITION (ds='2008-08-08');

> To list columns and all other properties of a partition. This also prints lot of information which is usually used for debugging.

列出一个分区的列和所有其他的属性。这打印很多信息，那也不是一个好看的格式。通常用于调试。

### 2.1.3、Altering Tables

> To rename existing table to a new name. If a table with new name already exists then an error is returned:

将已存在的表重命名为一个新名字。如果具有新名字的表已存在，就返回一个错误：

	ALTER TABLE old_table_name RENAME TO new_table_name;

> To rename the columns of an existing table. Be sure to use the same column types, and to include an entry for each preexisting column:

重命名一个已存在表的列。确保使用相同的列类型，包含一个预先存在的列的入口

	ALTER TABLE old_table_name REPLACE COLUMNS (col1 TYPE, ...);

> To add columns to an existing table:

给一个已存在的表添加列：

	ALTER TABLE tab1 ADD COLUMNS (c1 INT COMMENT 'a new int column', c2 STRING DEFAULT 'def val');

> Note that a change in the schema (such as the adding of the columns), preserves the schema for the old partitions of the table in case it is a partitioned table. All the queries that access these columns and run over the old partitions implicitly return a null value or the specified default values for these columns.

注意，模式中的更改(如添加列)将保留表的旧分区的模式，以防它是一个分区表。

访问这些列，并在旧分区上运行的所有查询都会隐式地为这些列返回空值或指定的默认值。

> In the later versions, we can make the behavior of assuming certain values as opposed to throwing an error in case the column is not found in a particular partition configurable.

在以后的版本中，我们可以采用假设某些值的行为，而不是在某个特定分区中找不到可配置的列时抛出错误。

### 2.1.4、Dropping Tables and Partitions

> Dropping tables is fairly trivial. A drop on the table would implicitly drop any indexes(this is a future feature) that would have been built on the table. The associated command is:

删除表非常简单。对表进行删除操作将隐式地删除在表上构建的任何索引(这是将来的特性)。相关的命令是:

	DROP TABLE pv_users;

> To dropping a partition. Alter the table to drop the partition.

删除一个分区。修改表以删除分区。

	ALTER TABLE pv_users DROP PARTITION (ds='2008-08-08')

> Note that any data for this table or partitions will be dropped and may not be recoverable.

注意，该表或分区的任何数据都将被删除，并且可能无法恢复。

### 2.2、Loading Data

> There are multiple ways to load data into Hive tables. The user can create an external table that points to a specified location within [HDFS](http://hadoop.apache.org/common/docs/current/hdfs_design.html). In this particular usage, the user can copy a file into the specified location using the HDFS put or copy commands and create a table pointing to this location with all the relevant row format information. Once this is done, the user can transform the data and insert them into any other Hive table. For example, if the file /tmp/pv_2008-06-08.txt contains comma separated page views served on 2008-06-08, and this needs to be loaded into the page_view table in the appropriate partition, the following sequence of commands can achieve this:

将数据加载到 Hive 表中有多种方式。

用户可以创建一个外部表，该表指向 HDFS 中的指定位置。在这种特殊的用法中，用户可以使用 HDFS 的 put 或 copy 命令将文件复制到指定的位置，并创建一个指向该位置的表，其中包含所有相关的行格式信息。

一旦完成，用户就可以转换数据，并将它们插入到任何其他 Hive 表中。例如，如果文件 `/tmp/pv_2008-06-08.txt` 包含了 2008-06-08 上的以逗号分隔的页面视图，并且需要将其加载到 page_view 表的相应分区中，下面的命令序列可以实现这一点:

```sql
CREATE EXTERNAL TABLE page_view_stg(viewTime INT, userid BIGINT,
                page_url STRING, referrer_url STRING,
                ip STRING COMMENT 'IP Address of the User',
                country STRING COMMENT 'country of origination')
COMMENT 'This is the staging page view table'
ROW FORMAT DELIMITED FIELDS TERMINATED BY '44' LINES TERMINATED BY '12'
STORED AS TEXTFILE
LOCATION '/user/data/staging/page_view';
 
hadoop dfs -put /tmp/pv_2008-06-08.txt /user/data/staging/page_view
 
FROM page_view_stg pvs
INSERT OVERWRITE TABLE page_view PARTITION(dt='2008-06-08', country='US')
SELECT pvs.viewTime, pvs.userid, pvs.page_url, pvs.referrer_url, null, null, pvs.ip
WHERE pvs.country = 'US';
```

> This code results in an error due to LINES TERMINATED BY limitation

由于 LINES TERMINATED BY 限制，这个代码会导致错误

	FAILED: SemanticException 6:67 LINES TERMINATED BY only supports newline '\n' right now. Error encountered near token ''12''

> See  `[HIVE-5999](https://issues.apache.org/jira/browse/HIVE-5999) - Allow other characters for LINES TERMINATED BY OPEN`  `[HIVE-11996](https://issues.apache.org/jira/browse/HIVE-11996) - Row Delimiter other than '\n' throws error in Hive. OPEN`
 
> In the example above, nulls are inserted for the array and map types in the destination tables but potentially these can also come from the external table if the proper row formats are specified.

在上面的示例中，将为目标表中的 array 和 map 类型插入 nulls，但如果指定了适当的行格式，这些 nulls 也可能来自外部表。

> This method is useful if there is already legacy data in HDFS on which the user wants to put some metadata so that the data can be queried and manipulated using Hive.

如果 HDFS 中已经有一些遗留数据，用户想要在这些数据上放一些元数据，这样就可以使用 Hive 来查询和操作这些数据，那么这种方法非常有用。

> Additionally, the system also supports syntax that can load the data from a file in the local files system directly into a Hive table where the input data format is the same as the table format. If /tmp/pv_2008-06-08_us.txt already contains the data for US, then we do not need any additional filtering as shown in the previous example. The load in this case can be done using the following syntax:

此外，系统还支持将本地文件系统中文件的数据直接加载到 Hive 表中，并且输入的数据格式与表格式相同。

如果 `/tmp/pv_2008-06-08_us.txt` 已经包含了 US 的数据，那么我们不需要像前面的例子中所示的任何额外的过滤。在这种情况下，可以使用以下语法进行加载:

	LOAD DATA LOCAL INPATH /tmp/pv_2008-06-08_us.txt INTO TABLE page_view PARTITION(date='2008-06-08', country='US')

> The path argument can take a directory (in which case all the files in the directory are loaded), a single file name, or a wildcard (in which case all the matching files are uploaded). If the argument is a directory, it cannot contain subdirectories. Similarly, the wildcard must match file names only.

path 参数可以接受一个目录(在这种情况下，该目录中的所有文件都被加载)、一个单个文件名或一个通配符(在这种情况下，所有匹配的文件都被上传)。

如果参数是目录，则不能包含子目录。类似地，通配符必须只匹配文件名。

> In the case that the input file /tmp/pv_2008-06-08_us.txt is very large, the user may decide to do a parallel load of the data (using tools that are external to Hive). Once the file is in HDFS - the following syntax can be used to load the data into a Hive table:

如果输入文件 `/tmp/pv_2008-06-08_us.txt` 非常大，用户可能会决定并行加载数据(使用 Hive 外部的工具)。一旦文件在 HDFS 中，可以使用以下语法将数据加载到 Hive 表中：

	LOAD DATA INPATH '/user/data/pv_2008-06-08_us.txt' INTO TABLE page_view PARTITION(date='2008-06-08', country='US')

> It is assumed that the array and map fields in the input.txt files are null fields for these examples.

对于这些例子，假设 input.txt 文件中的 array 和 map 字段是空字段。

> See [Hive Data Manipulation Language](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML) for more information about loading data into Hive tables, and see [External Tables](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-ExternalTables) for another example of creating an external table.

### 2.3、Querying and Inserting Data

> The Hive query operations are documented in [Select](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Select), and the insert operations are documented in [Inserting data into Hive Tables from queries](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-InsertingdataintoHiveTablesfromqueries) and [Writing data into the filesystem from queries](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-Writingdataintothefilesystemfromqueries).

Hive 查询操作记录在 Select 中，insert 操作记录在 Inserting data into Hive Tables from queries 和 Writing data into the filesystem from queries 中。

#### 2.3.1、Simple Query

> For all the active users, one can use the query of the following form:

对于所有活跃用户，可以使用下面形式的查询：

```sql
INSERT OVERWRITE TABLE user_active
SELECT user.*
FROM user
WHERE user.active = 1;
```

> Note that unlike SQL, we always insert the results into a table. We will illustrate later how the user can inspect these results and even dump them to a local file. You can also run the following query in [Beeline](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-Beeline%E2%80%93NewCommandLineShell) or the Hive [CLI](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli):

注意，与 SQL 不同，我们总是将结果插入表中。我们稍后将演示用户如何检查这些结果，甚至将它们转储到本地文件。

你也可以在 Beeline 或 Hive 命令行中执行如下查询:

```sql
SELECT user.*
FROM user
WHERE user.active = 1;
```

> This will be internally rewritten to some temporary file and displayed to the Hive client side.

这将在内部被重写到一些临时文件，并显示到 Hive 客户端。

#### 2.3.2、Partition Based Query

> What partitions to use in a query is determined automatically by the system on the basis of where clause conditions on partition columns. For example, in order to get all the page_views in the month of 03/2008 referred from domain xyz.com, one could write the following query:

在查询中，使用什么分区由系统根据分区列上的 where 子句条件自动决定。

例如，为了从域名 xyz.com 获得所有在 2008 年 3 月的 page_views，可以写以下查询：

```sql
INSERT OVERWRITE TABLE xyz_com_page_views
SELECT page_views.*
FROM page_views
WHERE page_views.date >= '2008-03-01' AND page_views.date <= '2008-03-31' AND
      page_views.referrer_url like '%xyz.com';
```

> Note that page_views.date is used here because the table (above) was defined with PARTITIONED BY(date DATETIME, country STRING) ; if you name your partition something different, don't expect .date to do what you think!

注意，这里使用 page_views.date 是因为上面的表是用 `PARTITIONED BY(date DATETIME, country STRING)` 定义的；如果你给你的分区起了不同的名字，别指望 `.date` 会像你想的那样!

#### 2.3.3、Joins

> In order to get a demographic breakdown (by gender) of page_view of 2008-03-03 one would need to join the page_view table and the user table on the userid column. This can be accomplished with a join as shown in the following query:

为了获得 2008-03-03 的 page_view 的人口统计分类(按性别)，需要在 userid 列上连接 page_view 表和 user 表。这可以通过 join 来实现，如下面的查询所示:

```sql
INSERT OVERWRITE TABLE pv_users
SELECT pv.*, u.gender, u.age
FROM user u JOIN page_view pv ON (pv.userid = u.id)
WHERE pv.date = '2008-03-03';
```

> In order to do outer joins the user can qualify the join with LEFT OUTER, RIGHT OUTER or FULL OUTER keywords in order to indicate the kind of outer join (left preserved, right preserved or both sides preserved). For example, in order to do a full outer join in the query above, the corresponding syntax would look like the following query:

为了进行外部连接，用户可以使用 LEFT OUTER、RIGHT OUTER 或 FULL OUTER 关键字来限定连接，以表明外部连接的类型(左保留、右保留或两边保留)。

例如，为了在上面的查询中执行 full outer join，相应的语法看起来像下面的查询:

```sql
INSERT OVERWRITE TABLE pv_users
SELECT pv.*, u.gender, u.age
FROM user u FULL OUTER JOIN page_view pv ON (pv.userid = u.id)
WHERE pv.date = '2008-03-03';
```

> In order check the existence of a key in another table, the user can use LEFT SEMI JOIN as illustrated by the following example.

为了检查另一个表中是否存在一个键，用户可以使用 LEFT SEMI JOIN，如下面的例子所示。

```sql
INSERT OVERWRITE TABLE pv_users
SELECT u.*
FROM user u LEFT SEMI JOIN page_view pv ON (pv.userid = u.id)
WHERE pv.date = '2008-03-03';
```

> In order to join more than one tables, the user can use the following syntax:

为了连接多个表，用户可以使用以下语法：

```sql
INSERT OVERWRITE TABLE pv_friends
SELECT pv.*, u.gender, u.age, f.friends
FROM page_view pv JOIN user u ON (pv.userid = u.id) JOIN friend_list f ON (u.id = f.uid)
WHERE pv.date = '2008-03-03';
```

> Note that Hive only supports [equi-joins](http://en.wikipedia.org/wiki/Join_(SQL)#Equi-join). Also it is best to put the largest table on the rightmost side of the join to get the best performance.

Hive 只支持 equi-joins。另外，最好将最大的表放在连接的最右边，以获得最佳性能。

#### 2.3.4、Aggregations

> In order to count the number of distinct users by gender one could write the following query:

为了按性别计算不同用户的数量，可以编写以下查询：

```sql
INSERT OVERWRITE TABLE pv_gender_sum
SELECT pv_users.gender, count (DISTINCT pv_users.userid)
FROM pv_users
GROUP BY pv_users.gender;
```

> Multiple aggregations can be done at the same time, however, no two aggregations can have different DISTINCT columns .e.g while the following is possible

多个聚合可以同时进行，但是，两个聚合不能有不同的列。例如，以下是可能的：

```sql
INSERT OVERWRITE TABLE pv_gender_agg
SELECT pv_users.gender, count(DISTINCT pv_users.userid), count(*), sum(DISTINCT pv_users.userid)
FROM pv_users
GROUP BY pv_users.gender;
```

> however, the following query is not allowed

但是，下面的查询是不允许的

```sql
INSERT OVERWRITE TABLE pv_gender_agg
SELECT pv_users.gender, count(DISTINCT pv_users.userid), count(DISTINCT pv_users.ip)
FROM pv_users
GROUP BY pv_users.gender;
```

#### 2.3.5、Multi Table/File Inserts

> The output of the aggregations or simple selects can be further sent into multiple tables or even to hadoop dfs files (which can then be manipulated using hdfs utilities). For example, if along with the gender breakdown, one needed to find the breakdown of unique page views by age, one could accomplish that with the following query:

聚合或简单选择的输出可以进一步发送到多个表中，甚至发送到 hadoop dfs 文件中(然后可以使用 hdfs 实用程序操作这些文件)。

例如，如果在性别分类的同时，需要找到按年龄划分的唯一页面浏览量，可以通过以下查询来实现：

```sql
FROM pv_users
INSERT OVERWRITE TABLE pv_gender_sum
    SELECT pv_users.gender, count_distinct(pv_users.userid)
    GROUP BY pv_users.gender
 
INSERT OVERWRITE DIRECTORY '/user/data/tmp/pv_age_sum'
    SELECT pv_users.age, count_distinct(pv_users.userid)
    GROUP BY pv_users.age;
```

> The first insert clause sends the results of the first group by to a Hive table while the second one sends the results to a hadoop dfs files.

第一个 insert 子句将第一个 group by 的结果发送到 Hive 表中，而第二个 insert 子句将结果发送到 hadoop dfs 文件中。

#### 2.3.6、Dynamic-Partition Insert

> In the previous examples, the user has to know which partition to insert into and only one partition can be inserted in one insert statement. If you want to load into multiple partitions, you have to use multi-insert statement as illustrated below.

在前面的例子中，用户必须知道要插入哪个分区，并且在一条 insert 语句中只能插入一个分区。

如果想要加载到多个分区，必须使用多条 insert 语句，如下所示。

```sql
FROM page_view_stg pvs
INSERT OVERWRITE TABLE page_view PARTITION(dt='2008-06-08', country='US')
       SELECT pvs.viewTime, pvs.userid, pvs.page_url, pvs.referrer_url, null, null, pvs.ip WHERE pvs.country = 'US'
INSERT OVERWRITE TABLE page_view PARTITION(dt='2008-06-08', country='CA')
       SELECT pvs.viewTime, pvs.userid, pvs.page_url, pvs.referrer_url, null, null, pvs.ip WHERE pvs.country = 'CA'
INSERT OVERWRITE TABLE page_view PARTITION(dt='2008-06-08', country='UK')
       SELECT pvs.viewTime, pvs.userid, pvs.page_url, pvs.referrer_url, null, null, pvs.ip WHERE pvs.country = 'UK';
```

> In order to load data into all country partitions in a particular day, you have to add an insert statement for each country in the input data. This is very inconvenient since you have to have the priori knowledge of the list of countries exist in the input data and create the partitions beforehand. If the list changed for another day, you have to modify your insert DML as well as the partition creation DDLs. It is also inefficient since each insert statement may be turned into a MapReduce Job.

为了将某一天的数据加载到所有 country 分区中，必须在输入数据中为每个国家添加 insert 一条语句。

这非常不方便，因为你必须事先知道输入数据中存在的国家列表，并预先创建分区。

如果列表某天更改了，则必须修改 insert DML 和创建分区 DML。这也是低效的，因为每个 insert 语句都可能变成 MapReduce Job。

> [Dynamic-partition insert](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-DynamicPartitionInserts) (or multi-partition insert) is designed to solve this problem by dynamically determining which partitions should be created and populated while scanning the input table. This is a newly added feature that is only available from version 0.6.0. In the dynamic partition insert, the input column values are evaluated to determine which partition this row should be inserted into. If that partition has not been created, it will create that partition automatically. Using this feature you need only one insert statement to create and populate all necessary partitions. In addition, since there is only one insert statement, there is only one corresponding MapReduce job. This significantly improves performance and reduce the Hadoop cluster workload comparing to the multiple insert case.

动态分区插入(或多分区插入)是为了解决这个问题而设计的，它在扫描输入表时动态地确定应该创建和填充哪些分区。

这是一个新添加的特性，仅在 0.6.0 版本中可用。

在动态分区插入中，将计算输入列的值，以确定应该将该行插入哪个分区。如果还没有创建该分区，它将自动创建该分区。

使用这个特性，你只需要一条 insert 语句就可以创建和填充所有必要的分区。另外，由于只有一条 insert 语句，所以对应的 MapReduce job 也只有一个。

与多次插入相比，这显著提高了性能并减少了 Hadoop 集群的工作负载。

> Below is an example of loading data to all country partitions using one insert statement:

下面是一个使用 insert 语句将数据加载到所有 country 分区的例子:

```sql
FROM page_view_stg pvs
INSERT OVERWRITE TABLE page_view PARTITION(dt='2008-06-08', country)
       SELECT pvs.viewTime, pvs.userid, pvs.page_url, pvs.referrer_url, null, null, pvs.ip, pvs.country
```

> There are several syntactic differences from the multi-insert statement:

与多条 insert 语句有几个语法上的区别:

> country appears in the PARTITION specification, but with no value associated. In this case, country is a dynamic partition column. On the other hand, ds has a value associated with it, which means it is a static partition column. If a column is dynamic partition column, its value will be coming from the input column. Currently we only allow dynamic partition columns to be the last column(s) in the partition clause because the partition column order indicates its hierarchical order (meaning dt is the root partition, and country is the child partition). You cannot specify a partition clause with (dt, country='US') because that means you need to update all partitions with any date and its country sub-partition is 'US'.

- country 出现在 PARTITION 规范中，但没有关联的值。

在本例中，country 是一个动态分区列。另一方面，ds 有一个与之相关联的值，这意味着它是一个静态分区列。

如果一个列是动态分区列，那么它的值将来自输入列。

目前，我们只允许动态分区列是 partition 子句中的最后一列，因为分区列的顺序表明了它的层次顺序(意味着 dt 是根分区，而 country 是子分区)。

你不能用 (dt, country='US') 指定分区子句，因为这意味着你需要用任一日期更新所有分区，而它的 country 子分区是 'US'。

> An additional pvs.country column is added in the select statement. This is the corresponding input column for the dynamic partition column. Note that you do not need to add an input column for the static partition column because its value is already known in the PARTITION clause. Note that the dynamic partition values are selected by ordering, not name, and taken as the last columns from the select clause.

- 在 select 语句中添加一个额外的 pvs.country 列。

这是动态分区列的相应输入列。

注意，你不需要为静态 partition 列添加输入列，因为它的值在 PARTITION 子句中已知。

注意，动态分区值是按照顺序选择的，而不是名称，并作为 select 子句的最后一列。

> Semantics of the dynamic partition insert statement:

动态分区插入语句的语义:

> When there are already non-empty partitions exists for the dynamic partition columns, (for example, country='CA' exists under some ds root partition), it will be overwritten if the dynamic partition insert saw the same value (say 'CA') in the input data. This is in line with the 'insert overwrite' semantics. However, if the partition value 'CA' does not appear in the input data, the existing partition will not be overwritten.

- 当动态分区列已经存在非空的分区时(例如，country='CA' 存在于某些 ds 根分区下)，如果动态分区插入在输入数据中遇到相同的值(比如'CA')，那么它将被覆盖。这符合 'insert overwrite' 语义。但是，如果分区值 'CA' 没有出现在输入数据中，则不会覆盖现有分区。

> Since a Hive partition corresponds to a directory in HDFS, the partition value has to conform to the HDFS path format (URI in Java). Any character having a special meaning in URI (for example, '%', ':', '/', '#') will be escaped with '%' followed by 2 bytes of its ASCII value.

- 一个 Hive 分区对应 HDFS 中的一个目录，分区值必须符合 HDFS 的路径格式(Java为URI)。任何在 URI 中有特殊含义的字符(例如'%'，':'，'/'，'#')将用 '%' 后跟它的 ASCII 值的 2 个字节进行转义。

> If the input column is a type different than STRING, its value will be first converted to STRING to be used to construct the HDFS path.

- 如果输入列不是 STRING 类型，则首先将其值转换为 STRING，用于构建 HDFS 路径。

> If the input column value is NULL or empty string, the row will be put into a special partition, whose name is controlled by the hive parameter hive.exec.default.partition.name. The default value is HIVE_DEFAULT_PARTITION{}. Basically this partition will contain all "bad" rows whose value are not valid partition names. The caveat of this approach is that the bad value will be lost and is replaced by HIVE_DEFAULT_PARTITION{} if you select them Hive. JIRA HIVE-1309 is a solution to let user specify "bad file" to retain the input partition column values as well.

- 如果输入的列值为 NULL 或空字符串，行将被放入一个特殊的分区，分区的名称由 `hive.exec.default.partition.name` 参数控制。默认值为 `HIVE_DEFAULT_PARTITION{}`。基本上，这个分区将包含“坏”行，行的值不是有效的分区名。注意，如果选择它们，“坏”值将丢失，并由 `HIVE_DEFAULT_PARTITION{}` 替换。JIRA HIVE-1309 是一个让用户指定 “坏文件” 来保留输入分区列值的解决方案。

> Dynamic partition insert could potentially be a resource hog in that it could generate a large number of partitions in a short time. To get yourself buckled, we define three parameters:

- 动态分区插入可能会占用大量资源，因为它可能会在很短的时间内生成大量分区。我们定义了三个参数：

	- hive.exec.max.dynamic.partitions.pernode（默认值是100）：是每个 mapper 或 reducer 可以创建的最大动态分区。如果一个 mapper 或 reducer 创建的值超过这个阈值，一个致命错误将从 mapper/reducer(通过计数器)引发，整个 job 将被杀死。

	- hive.exec.max.dynamic.partitions（默认值为1000）：是一个 DML 可以创建的动态分区的总数。如果每个 mapper/reducer 都没有超过这个限制，但是动态分区的总数却超过了，那么在 job 结束时，中间数据被移动到最终目的地之前，会引发一个异常。

	- hive.exec.max.created.files（默认值为100000）是所有 mappers 和 reducers 创建的最大文件总数。这是通过在创建新文件时由每个 mapper/reducer 更新 Hadoop 计数器来实现的。如果总数超过了 hive.exec.max.created.files 时，一个致命错误将被抛出，job 将被终止。

> hive.exec.max.dynamic.partitions.pernode (default value being 100) is the maximum dynamic partitions that can be created by each mapper or reducer. If one mapper or reducer created more than that the threshold, a fatal error will be raised from the mapper/reducer (through counter) and the whole job will be killed.

> hive.exec.max.dynamic.partitions (default value being 1000) is the total number of dynamic partitions could be created by one DML. If each mapper/reducer did not exceed the limit but the total number of dynamic partitions does, then an exception is raised at the end of the job before the intermediate data are moved to the final destination.

> hive.exec.max.created.files (default value being 100000) is the maximum total number of files created by all mappers and reducers. This is implemented by updating a Hadoop counter by each mapper/reducer whenever a new file is created. If the total number is exceeding hive.exec.max.created.files, a fatal error will be thrown and the job will be killed.

> Another situation we want to protect against dynamic partition insert is that the user may accidentally specify all partitions to be dynamic partitions without specifying one static partition, while the original intention is to just overwrite the sub-partitions of one root partition. We define another parameter hive.exec.dynamic.partition.mode=strict to prevent the all-dynamic partition case. In the strict mode, you have to specify at least one static partition. The default mode is strict. In addition, we have a parameter hive.exec.dynamic.partition=true/false to control whether to allow dynamic partition at all. The default value is false prior to Hive 0.9.0 and true in Hive 0.9.0 and later.

我们希望防止动态分区插入的另一种情况是，用户可能会意外地将所有分区指定为动态分区，而没有指定一个静态分区，而最初的目的只是覆盖一个根分区的子分区。

我们定义另一个参数 `hive.exec.dynamic.partition.mode=strict`，防止出现全部动态分区的情况。在严格模式下，必须指定至少一个静态分区。默认模式为严格的。

此外，我们还有一个参数 `hive.exec.dynamic.partition=true/false` 来控制是否允许动态分区。Hive 0.9.0 之前的默认值为 false, Hive 0.9.0 之后的默认值为 true。

> In Hive 0.6, dynamic partition insert does not work with hive.merge.mapfiles=true or hive.merge.mapredfiles=true, so it internally turns off the merge parameters. Merging files in dynamic partition inserts are supported in Hive 0.7 (see JIRA HIVE-1307 for details).

在 Hive 0.6 中，动态分区插入不能与 `hive.merge.mapfiles=true` 或 `hive.merge.mapredfiles=true` 一起使用。因此它在内部关闭了合并参数。

Hive 0.7 支持在动态分区插入中合并文件。

> Troubleshooting and best practices:

故障排除和最佳实践：

> As stated above, there are too many dynamic partitions created by a particular mapper/reducer, a fatal error could be raised and the job will be killed. The error message looks something like:

如上所述，一个特定的 mapper/reducer 创建的动态分区太多了，可能会引发一个致命错误，job 将被终止。错误信息是这样的：

```sh
    beeline> set hive.exec.dynamic.partition.mode=nonstrict;
    beeline> FROM page_view_stg pvs
          INSERT OVERWRITE TABLE page_view PARTITION(dt, country)
                 SELECT pvs.viewTime, pvs.userid, pvs.page_url, pvs.referrer_url, null, null, pvs.ip,
                        from_unixtimestamp(pvs.viewTime, 'yyyy-MM-dd') ds, pvs.country;
...
2010-05-07 11:10:19,816 Stage-1 map = 0%,  reduce = 0%
[Fatal Error] Operator FS_28 (id=41): fatal error. Killing the job.
Ended Job = job_201005052204_28178 with errors
...
```

> The problem of this that one mapper will take a random set of rows and it is very likely that the number of distinct (dt, country) pairs will exceed the limit of hive.exec.max.dynamic.partitions.pernode. One way around it is to group the rows by the dynamic partition columns in the mapper and distribute them to the reducers where the dynamic partitions will be created. In this case the number of distinct dynamic partitions will be significantly reduced. The above example query could be rewritten to:

这样做的问题是，一个 mapper 将取一个随机的行集，并且很可能不同的(dt, country)对的数量将超过 `hive.exec.max.dynamic.partitions.pernode` 的限制。

解决这个问题的一种方法是按照 mapper 中的动态分区列对行进行分组，并将它们分发到将在其中创建动态分区的 reducers 中。

在这种情况下，不同动态分区的数量将显著减少。上面的示例查询可以重写为:

```sh
beeline> set hive.exec.dynamic.partition.mode=nonstrict;
beeline> FROM page_view_stg pvs
      INSERT OVERWRITE TABLE page_view PARTITION(dt, country)
             SELECT pvs.viewTime, pvs.userid, pvs.page_url, pvs.referrer_url, null, null, pvs.ip,
                    from_unixtimestamp(pvs.viewTime, 'yyyy-MM-dd') ds, pvs.country
             DISTRIBUTE BY ds, country;
```

> This query will generate a MapReduce job rather than Map-only job. The SELECT-clause will be converted to a plan to the mappers and the output will be distributed to the reducers based on the value of (ds, country) pairs. The INSERT-clause will be converted to the plan in the reducer which writes to the dynamic partitions.

该查询将生成一个 MapReduce job，而不是 Map-only job。

SELECT 子句将被转换为 mappers 的计划，并根据 (ds, country)对的值将输出分配给 reducers。

INSERT 子句将被转换为 reducer 中的计划，用于写入动态分区。

> Additional documentation:

- [Design Document for Dynamic Partitions](https://cwiki.apache.org/confluence/display/Hive/DynamicPartitions)

	- [Original design doc](https://issues.apache.org/jira/secure/attachment/12437909/dp_design.txt)

	- [HIVE-936](https://issues.apache.org/jira/browse/HIVE-936)

- [Hive DML: Dynamic Partition Inserts](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-DynamicPartitionInserts)

- [HCatalog Dynamic Partitioning](https://cwiki.apache.org/confluence/display/Hive/HCatalog+DynamicPartitions)

	- [Usage with Pig](https://cwiki.apache.org/confluence/display/Hive/HCatalog+DynamicPartitions#HCatalogDynamicPartitions-UsagewithPig)

	- [Usage from MapReduce](https://cwiki.apache.org/confluence/display/Hive/HCatalog+DynamicPartitions#HCatalogDynamicPartitions-UsagefromMapReduce)

#### 2.3.7、Inserting into Local Files

> In certain situations you would want to write the output into a local file so that you could load it into an excel spreadsheet. This can be accomplished with the following command:

在某些情况下，可能希望将输出写入到本地文件，以便将其加载到 excel 表格中。这可以通过以下命令完成:

```sql
INSERT OVERWRITE LOCAL DIRECTORY '/tmp/pv_gender_sum'
SELECT pv_gender_sum.*
FROM pv_gender_sum;
```

#### 2.3.8、Sampling

> The sampling clause allows the users to write queries for samples of the data instead of the whole table. Currently the sampling is done on the columns that are specified in the CLUSTERED BY clause of the CREATE TABLE statement. In the following example we choose 3rd bucket out of the 32 buckets of the pv_gender_sum table:

抽样子句允许用户对数据的样本(而不是整个表)编写查询。

目前，抽样是在 CREATE TABLE 语句的 CLUSTERED BY 子句中指定的列上进行的。

在下面的例子中，我们从 pv_gender_sum 表的 32 个桶中选择第三个桶：

```sql
INSERT OVERWRITE TABLE pv_gender_sum_sample
SELECT pv_gender_sum.*
FROM pv_gender_sum TABLESAMPLE(BUCKET 3 OUT OF 32);
```

> In general the TABLESAMPLE syntax looks like:

一般情况下，TABLESAMPLE 语法如下:

```sql
TABLESAMPLE(BUCKET x OUT OF y)
```

> y has to be a multiple or divisor of the number of buckets in that table as specified at the table creation time. The buckets chosen are determined if bucket_number module y is equal to x. So in the above example the following tablesample clause

y 必须是该表在创建表时指定的桶数的倍数或除数。

如果 bucket_number 模块 y 等于 x，则确定所选择的桶。所以在上面的例子中，下面的 tablesample 子句

```sql
TABLESAMPLE(BUCKET 3 OUT OF 16)
```

> would pick out the 3rd and 19th buckets. The buckets are numbered starting from 0.

选出第3和第19个桶。桶从0开始编号。

> On the other hand the tablesample clause

另一方面，tablesample 子句

```sql
TABLESAMPLE(BUCKET 3 OUT OF 64 ON userid)
```

> would pick out half of the 3rd bucket.

会取出第三个桶的一半。

#### 2.3.9、Union All

> The language also supports union all, for example, if we suppose there are two different tables that track which user has published a video and which user has published a comment, the following query joins the results of a union all with the user table to create a single annotated stream for all the video publishing and comment publishing events:

语言还支持 union all，

例如，如果我们假设有两个不同的表，跟踪用户发布的一个视频，和用户发表的评论。

下面的查询使用 user 表 join 了 union all 的结果，来创建一个带注释的所有视频发布和评论发布事件流:

```sql
INSERT OVERWRITE TABLE actions_users
SELECT u.id, actions.date
FROM (
    SELECT av.uid AS uid
    FROM action_video av
    WHERE av.date = '2008-06-03'
 
    UNION ALL
 
    SELECT ac.uid AS uid
    FROM action_comment ac
    WHERE ac.date = '2008-06-03'
    ) actions JOIN users u ON(u.id = actions.uid);
```

#### 2.3.10、Array Operations

> Array columns in tables can be as follows:

表中的数组列可以如下:

```sql
CREATE TABLE array_table (int_array_column ARRAY<INT>);
```

> Assuming that pv.friends is of the type ARRAY<INT> (i.e. it is an array of integers), the user can get a specific element in the array by its index as shown in the following command:

假设 pv.friends 的类型为 ARRAY<INT>(即它是一个整型数组)，用户可以通过数组的索引获取数组中的特定元素，如下所示:

```sql
SELECT pv.friends[2]
FROM page_views pv;
```

> The select expression gets the third item in the pv.friends array.

select 表达式获得 pv.friends 数组中的第三项。

> The user can also get the length of the array using the size function as shown below:

用户也可以使用 size 函数获取数组的长度，如下所示:

```sql
SELECT pv.userid, size(pv.friends)
FROM page_view pv;
```

#### 2.3.11、Map (Associative Arrays) Operations

> Maps provide collections similar to associative arrays. Such structures can only be created programmatically currently. We will be extending this soon. For the purpose of the current example assume that pv.properties is of the type map<String, String> i.e. it is an associative array from strings to string. Accordingly, the following query:

映射提供了类似于关联数组的集合。目前只能以编程方式创建此类结构。我们很快就会扩展这个。

对于当前的例子，假设 pv.properties 类型为 map<String, String>，即它是一个从字符串到字符串的关联数组。因此，以下查询：

```sql
INSERT OVERWRITE page_views_map
SELECT pv.userid, pv.properties['page type']
FROM page_views pv;
```

> can be used to select the 'page_type' property from the page_views table.

可用于从 page_views 表中选择 'page_type' 属性。

> Similar to arrays, the size function can also be used to get the number of elements in a map as shown in the following query:

与数组类似，size 函数也可以用于获取 map 中元素的数量，如下所示:

```sql
SELECT size(pv.properties)
FROM page_view pv;
```

#### 2.3.12、Custom Map/Reduce Scripts

> Users can also plug in their own custom mappers and reducers in the data stream by using features natively supported in the Hive language. for example, in order to run a custom mapper script - map_script - and a custom reducer script - reduce_script - the user can issue the following command which uses the TRANSFORM clause to embed the mapper and the reducer scripts.

用户还可以使用 Hive 语言本地支持的特性，在数据流中插入自己的自定义 mappers 和 reducers。

例如，为了运行一个自定义 mapper 脚本 map_script 和一个自定义 reducer 脚本 reduce_script，用户可以发出以下命令，该命令使用 TRANSFORM 子句来嵌入 mapper 和 reducer 脚本。

> Note that columns will be transformed to string and delimited by TAB before feeding to the user script, and the standard output of the user script will be treated as TAB-separated string columns. User scripts can output debug information to standard error which will be shown on the task detail page on hadoop.

注意，在提供给用户脚本之前，列将被转换为字符串，并以制表符分隔，用户脚本的标准输出将被视为制表符分隔的字符串列。

用户脚本可以将调试信息输出到标准错误，这些错误将显示在 hadoop 上的 task detail 页面上。

```sql
FROM (
     FROM pv_users
     MAP pv_users.userid, pv_users.date
     USING 'map_script'
     AS dt, uid
     CLUSTER BY dt) map_output
 
 INSERT OVERWRITE TABLE pv_users_reduced
     REDUCE map_output.dt, map_output.uid
     USING 'reduce_script'
     AS date, count;
```

> Sample map script (weekday_mapper.py )

抽样 map 脚本：

```python
import sys
import datetime
 
for line in sys.stdin:
  line = line.strip()
  userid, unixtime = line.split('\t')
  weekday = datetime.datetime.fromtimestamp(float(unixtime)).isoweekday()
  print ','.join([userid, str(weekday)])
```

> Of course, both MAP and REDUCE are "syntactic sugar" for the more general select transform. The inner query could also have been written as such:

当然，MAP 和 REDUCE 对于更一般的 select 转换来说都是“语法糖”。内部查询也可以这样写:

```sql
SELECT TRANSFORM(pv_users.userid, pv_users.date) USING 'map_script' AS dt, uid CLUSTER BY dt FROM pv_users;
```

> Schema-less map/reduce: If there is no "AS" clause after "USING map_script", Hive assumes the output of the script contains 2 parts: key which is before the first tab, and value which is the rest after the first tab. Note that this is different from specifying "AS key, value" because in that case value will only contains the portion between the first tab and the second tab if there are multiple tabs.

无模式的 map/reduce：

如果 USING map_script 后没有 AS 子句，Hive 假设脚本输出包含两个部分：key 在第一个制表符前，value 在第一个制表符后。

注意，这与指定 “AS key, value” 不同，因为在这种情况下，如果有多个制表符，value 将只包含第一个制表符和第二个制表符之间的部分。

> In this way, we allow users to migrate old map/reduce scripts without knowing the schema of the map output. User still needs to know the reduce output schema because that has to match what is in the table that we are inserting to.

通过这种方式，我们允许用户在不知道 map 输出模式的情况下迁移旧的 map/reduce 脚本。

用户仍然需要知道 reduce 输出模式，因为它必须与我们要插入的表中的内容相匹配。

```sql
FROM (
    FROM pv_users
    MAP pv_users.userid, pv_users.date
    USING 'map_script'
    CLUSTER BY key) map_output
 
INSERT OVERWRITE TABLE pv_users_reduced
 
    REDUCE map_output.dt, map_output.uid
    USING 'reduce_script'
    AS date, count;
```

> Distribute By and Sort By: Instead of specifying "cluster by", the user can specify "distribute by" and "sort by", so the partition columns and sort columns can be different. The usual case is that the partition columns are a prefix of sort columns, but that is not required.

Distribute By 和 Sort By：用户可以指定 Distribute By 和 Sort By，而不是指定 cluster By，因此分区列和排序列可以不同。

通常情况下，分区列是排序列的前缀，但这不是必需的。

```sql
FROM (
    FROM pv_users
    MAP pv_users.userid, pv_users.date
    USING 'map_script'
    AS c1, c2, c3
    DISTRIBUTE BY c2
    SORT BY c2, c1) map_output
 
INSERT OVERWRITE TABLE pv_users_reduced
 
    REDUCE map_output.c1, map_output.c2, map_output.c3
    USING 'reduce_script'
    AS date, count;
```  

#### 2.3.1、Co-Groups

> Amongst the user community using map/reduce, cogroup is a fairly common operation wherein the data from multiple tables are sent to a custom reducer such that the rows are grouped by the values of certain columns on the tables. With the UNION ALL operator and the CLUSTER BY specification, this can be achieved in the Hive query language in the following way. Suppose we wanted to cogroup the rows from the actions_video and action_comments table on the uid column and send them to the 'reduce_script' custom reducer, the following syntax can be used by the user:

在使用 map/reduce 的用户社区中，cogroup 是一种相当常见的操作，其中来自多个表的数据被发送到一个自定义的 reducer，这样行就按表上某些列的值分组。

通过 UNION ALL 操作符和 CLUSTER BY 规范，Hive 查询语言可以通过以下方式实现这一点。

假设我们想在 uid 列上对 actions_video 和 action_comments 表中的行进行分组，并将它们发送到 'reduce_script' 自定义 reducer，用户可以使用以下语法:

```sql
FROM (
     FROM (
             FROM action_video av
             SELECT av.uid AS uid, av.id AS id, av.date AS date
 
            UNION ALL
 
             FROM action_comment ac
             SELECT ac.uid AS uid, ac.id AS id, ac.date AS date
     ) union_actions
     SELECT union_actions.uid, union_actions.id, union_actions.date
     CLUSTER BY union_actions.uid) map
 
 INSERT OVERWRITE TABLE actions_reduced
     SELECT TRANSFORM(map.uid, map.id, map.date) USING 'reduce_script' AS (uid, id, reduced_val);
```
