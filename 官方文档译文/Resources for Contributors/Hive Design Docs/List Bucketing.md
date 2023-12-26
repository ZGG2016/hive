# List Bucketing

[TOC]

## 1、Goal

> The top level problem is as follows:

最顶层的问题是：

> There are many tables of the following format:

存在很多下列格式的表：

	create table T(a, b, c, ....., x) partitioned by (ds);

> and the following queries need to be performed efficiently:

并且，需要高效执行下列查询：

	select ... from T where x = 10;

> The cardinality of 'x' is in 1000's per partition of T. Moreover, there is a skew for the values of 'x'. In general, there are `~10` values of 'x' which have a very large skew, and the remaining values of 'x' have a small cardinality. Also, note that this mapping (values of 'x' with a high cardinality) can change daily.

> The above requirement can be solved in the following ways:

上述要求可通过以下方式解决:

### 1.1、Basic Partitioning

> Create a partition per value of 'x'.

为每个 'x' 值创建一个分区：

- create table T(a,b,c, .......) partitioned by (ds, x);

> Advantages
> Existing Hive is good enough.

- 优点

	- 已存在的 Hive 足够了

> Disadvantages
> HDFS scalability: Number of files in HDFS increases.
> HDFS scalability: Number of intermediate files in HDFS increases. For example if there are 1000 mappers and 1000 partitions, and each mapper gets at least 1 row for each key, we will end up creating 1 million intermediate files.
> Metastore scalability: Will the metastore scale with the number of partitions.

- 缺点

	- HDFS 扩展性：HDFS 中的文件的数量增加。
	- HDFS 扩展性：HDFS 中的中间文件的数量增加。例如，如果有 1000 个 mappers 任务和 1000 个分区，对每个键，每个 mapper 至少获得 1 行，最后将创建 1 百万的中间文件。
	- Metastore 扩展性：metastore 会随着分区数量的增加而扩展。

### 1.2、List Bucketing

> The basic idea here is as follows: Identify the keys with a high skew. Have one directory per skewed key, and the remaining keys go into a separate directory. This mapping is maintained in the metastore at a table or partition level, and is used by the Hive compiler to do input pruning. The list of skewed keys is stored at the table level. (Note that initially this list can be supplied by the client periodically, and eventually it can be updated when a new partition is being loaded.)

基本思想如下：识别具有高倾斜度的键。

每个倾斜键有一个目录，其余键进入一个单独的目录。这个映射在一个表或分区级别的 metastore 中维护，并被 Hive 编译器用来做输入修剪。

倾斜键列表存储在表级别。(注意，这个列表最初可以由客户端定期提供，最终可以在加载新分区时更新它。)

> For example, the table maintains the list of skewed keys for 'x': 6, 20, 30, 40. When a new partition is being loaded, it will create 5 directories (4 directories for skewed keys + 1 default directory for all the remaining keys). The table/partition that got loaded will have the following mapping: 6,20,30,40,others. This is similar to hash bucketing currently, where the bucket number determines the file number. Since the skewed keys need not be consecutive, the entire list of skewed keys need be stored in each table/partition.

例如，该表维护 'x' 的倾斜键列表：6、20、30、40。

当加载一个新分区时，它将创建 5 个目录(倾斜键的 4 个目录，和其余所有键的 1 个默认目录)。

加载的表/分区将有以下映射：6、20、30、40、others。这类似于当前的哈希分桶，其中桶数决定了文件数。

由于倾斜键不必是连续的，所以整个倾斜键列表需要存储在每个表/分区中。

> When a query of the form

下面形式的查询

	select ... from T where ds = '2012-04-15' and x = 30;

> is issued, the Hive compiler will only use the directory corresponding to x=30 for the map-reduce job.

上述查询执行，Hive 编译器将仅使用 x=30 的目录。

> For a query of the form

下面形式的查询

	select ... from T where ds = '2012-04-15' and x = 50;

> the Hive compiler will only use the file corresponding to x=others for the map-reduce job.

Hive 编译器将仅使用 x=others 的目录。

> This approach is good under the following assumptions:

在下面的假设下，这种方法很好:

- 每个分区的倾斜键占总数据的很大比例。在上面的例子中，如果倾斜键(6、20、30和40)只占数据的一小部分(比如20%)，那么 x=50 形式的查询仍然需要扫描剩下的数据(`~80%`)。

- 每个分区的倾斜键的数量相当少。这个列表存储在 metastore 中，所以在 metastore 中，每个分区存储 100 万个倾斜键是没有意义的。

> Each partition's skewed keys account for a significant percentage of the total data. In the above example, if the skewed keys (6,20,30 and 40) only occupy a small percentage of the data (say 20%), the queries of the form x=50 will still need to scan the remaining data (`~80%`).

> The number of skewed keys per partition is fairly small. This list is stored in the metastore, so it does not make sense to store 1 million skewed keys per partition in the metastore.

> This approach can be extended to the scenario when there are more than one clustered key. Say we want to optimize the queries of the form

这种方法可以扩展到有多个聚集键的场景。假设我们想优化下面形式的查询

	select ... from T where x = 10 and y = 'b';

> Extend the above approach. For each skewed value of (x,y), store the file offset. So, the metastore will have the mapping like: (10, 'a') -> 1, (10, 'b') -> 2, (20, 'c') -> 3, (others) -> 4.

扩展上面的方法。对于每个倾斜的 (x,y) 值，存储文件的偏移量。因此，metastore 的映射如下：(10, 'a') -> 1, (10, 'b') -> 2, (20, 'c') -> 3, (others) -> 4.

> A query with all the clustering keys specified can be optimized easily. However, queries with some of the clustering keys specified:

具有所有指定的聚集键的查询可以很容易地进行优化。然而，使用一些指定的聚集键进行查询:

	select ... from T where x = 10;
	select ... from T where y = 'b';

> can only be used to prune very few directories. It does not really matter if the prefix of the clustering keys is specified or not. For example for x=10, the Hive compiler can prune the file corresponding to (20, 'c'). And for y='b', the files corresponding to (10, 'a') and (20, 'c') can be pruned. Hashing for others does not really help, when the complete key is not specified.

只能用于修剪极少数的目录。

聚集键的前缀是否指定并不重要。

例如，对于 x=10, Hive 编译器可以修剪对应 (20，'c') 的文件。对于 y='b'，对应于 (10，'a') 和 (20，'c') 的文件可以被修剪。当没有指定完整键时，为其他对象的哈希并没有真正的帮助。

> This approach does not scale in the following scenarios:

这种方法在以下场景中无法扩展：

- 倾斜键的数目非常大。这为 metastore 的可伸缩性带来了一个问题。

- 在大多数情况下，聚集键的数量大于一个，并且在查询中，没有指定所有聚集键。

> The number of skewed keys is very large. This creates a problem for metastore scalability.

> In most of the cases, the number of clustered keys is more than one, and in the query, all the clustered keys are not specified.

### 1.3、Skewed Table vs. List Bucketing Table

> Skewed Table is a table which has skewed information.

- 倾斜表是一个有倾斜信息的表。

> List Bucketing Table is a skewed table. In addition, it tells Hive to use the list bucketing feature on the skewed table: create sub-directories for skewed values.

- 列桶表是一个倾斜的表。此外，它告诉 Hive 在倾斜表上使用列桶特性：为倾斜值创建子目录。

> A normal skewed table can be used for skewed join, etc. (See [the Skewed Join Optimization design document](https://cwiki.apache.org/confluence/display/Hive/Skewed+Join+Optimization).) You don't need to define it as a list bucketing table if you don't use the list bucketing feature.

正常的倾斜表可以用于倾斜连接，等等。如果不使用列桶特性，则不需要将其定义为列桶表。

### 1.4、List Bucketing Validation

> Mainly due to its sub-directory nature, list bucketing can't coexist with some features.

由于列桶的子目录性质，导致和某些特性无法共存。

#### 1.4.1、DDL

> Compilation error will be thrown if list bucketing table coexists with

如果列桶表和下面的特性共存，会抛出编译错误：

- normal bucketing (clustered by, tablesample, etc.)
- external table
- "load data ..."
- CTAS (Create Table As Select) queries

#### 1.4.2、DML

> Compilation error will be thrown if list bucketing table coexists with

如果列桶表和下面的特性共存，会抛出编译错误：

- "insert into"
- normal bucketing (clustered by, tablesample, etc.)
- external table
- non-RCfile due to merge
- non-partitioned table

> Partitioning value should not be the same as a default list bucketing directory name.

分区值不应该和默认的列桶目录名称相同。

#### 1.4.3、Alter Table Concatenate

> Compilation error will be thrown if list bucketing table coexists with

如果列桶表和下面的特性共存，会抛出编译错误：

- non-RCfile
- external table for alter table

### 1.5、Hive Enhancements

> Hive needs to be extended to support the following:

Hive 需要扩展支持下面的特性：

#### 1.5.1、Create Table

	CREATE TABLE <T> (SCHEMA) SKEWED BY (keys) ON ('c1', 'c2') [STORED AS DIRECTORIES];

> The table will be a skewed table. Skewed information will be created for all partitions.

表将是一个倾斜表。将创建所有的分区的倾斜信息。

例如：

- create table T (c1 string, c2 string) skewed by (c1) on ('x1') stored as directories;
- create table T (c1 string, c2 string, c3 string) skewed by (c1, c2) on (('x1', 'x2'), ('y1', 'y2')) stored as directories;

> 'STORED AS DIRECTORIES' is an optional parameter. It tells Hive that it is not only a skewed table but also the list bucketing feature should apply: create sub-directories for skewed values.

`STORED AS DIRECTORIES` 是一个可选参数。告诉 Hive 它不仅是一个倾斜表，而且应该 应用列桶特性：为倾斜值创建子目录。

#### 1.5.2、Alter Table

##### 1.5.2.1、Alter Table Skewed

	ALTER TABLE <T> (SCHEMA) SKEWED BY  (keys) ON ('c1', 'c2') [STORED AS DIRECTORIES];

> The above is supported in table level only and not partition level.

上面的语句仅在表级别支持，不在分区级别。

> It will

它将：

- 将一个表从非倾斜表转换为倾斜表，或者
- 修改一个倾斜表的倾斜列的名称 和/或 倾斜值

> convert a table from a non-skewed table to a skewed table, or else
> alter a skewed table's skewed column names and/or skewed values.

> It will impact

它将：

- 影响在 alter 语句之后创建的分区
- 但，不影响 alter 语句之前创建的分区

> partitions created after the alter statement
> but not partitions created before the alter statement.

##### 1.5.2.2、Alter Table Not Skewed

	ALTER TABLE <T> (SCHEMA) NOT SKEWED;

> The above will

上面将

- 关闭表的倾斜特性
- 让表不倾斜
- 关闭列桶特性，因为列桶表也是一个倾斜表

> turn off the "skewed" feature from a table
> make a table non-skewed
> turn off the "list bucketing" feature since a list bucketing table is a skewed table also.

> It will impact

它将：

- 影响在 alter 语句之后创建的分区
- 但，不影响 alter 语句之前创建的分区

> partitions created after the alter statement
> but not partitions created before the alter statement.

##### 1.5.2.3、Alter Table Not Stored as Directories

	ALTER TABLE <T> (SCHEMA) NOT STORED AS DIRECTORIES;

> The above will

上面将

- 关闭列桶
- 不会关闭表的倾斜特性，因为一个倾斜表可以是一个正常的倾斜表，而没有倾斜特性。

> turn off "list bucketing"
> not turn off the "skewed" feature from table since a "skewed" table can be a normal "skewed" table without list bucketing.

##### 1.5.2.4、Alter Table Set Skewed Location

	ALTER TABLE <T> (SCHEMA) SET SKEWED LOCATION (key1="loc1", key2="loc2");

> The above will change the list bucketing location map.

上面的语句将改变列桶路径映射。

#### 1.5.3、Design

> When such a table is being loaded, it would be good to create a sub-directory per skewed key. The infrastructure similar to dynamic partitions can be used.

当加载这样一个表时，最好为每个倾斜键创建一个子目录。可以使用类似于动态分区的基础结构。

> Alter table <T> partition <P> concatenate; needs to be changed to merge files per directory.

`Alter table <T> partition <P> concatenate;` 需要更改，以合并每个目录的文件。

## 2、Implementation

> Version information. List bucketing was added in Hive 0.10.0 and 0.11.0.

> [HIVE-3026](https://issues.apache.org/jira/browse/HIVE-3026) is the root JIRA ticket for the list bucketing feature.  It has links to additional JIRA tickets which implement list bucketing in Hive, including: 

- [HIVE-3554](https://issues.apache.org/jira/browse/HIVE-3554):  Hive List Bucketing – Query logic (release 0.10.0)
- [HIVE-3649](https://issues.apache.org/jira/browse/HIVE-3649):  Hive List Bucketing - enhance DDL to specify list bucketing table (release 0.10.0)
- [HIVE-3072](https://issues.apache.org/jira/browse/HIVE-3072):  Hive List Bucketing - DDL support (release 0.10.0)
- [HIVE-3073](https://issues.apache.org/jira/browse/HIVE-3073):  Hive List Bucketing - DML support (release 0.11.0)

> For more information, see [Skewed Tables in the DDL document](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-SkewedTables).


原文讨论部分：[https://cwiki.apache.org/confluence/display/Hive/ListBucketing](https://cwiki.apache.org/confluence/display/Hive/ListBucketing)
