# DynamicPartitions

[TOC]

## 1、Documentation

This is the [design](https://cwiki.apache.org/confluence/display/Hive/DesignDocs) document for dynamic partitions in Hive. Usage information is also available:

- [Tutorial: Dynamic-Partition Insert](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-Dynamic-PartitionInsert)
- [Hive DML: Dynamic Partition Inserts](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-DynamicPartitionInserts)
- [HCatalog Dynamic Partitioning](https://cwiki.apache.org/confluence/display/Hive/HCatalog+DynamicPartitions)
	- [Usage with Pig](https://cwiki.apache.org/confluence/display/Hive/HCatalog+DynamicPartitions#HCatalogDynamicPartitions-UsagewithPig)
	- [Usage from MapReduce](https://cwiki.apache.org/confluence/display/Hive/HCatalog+DynamicPartitions#HCatalogDynamicPartitions-UsagefromMapReduce)

References:

- [Original design doc](https://issues.apache.org/jira/secure/attachment/12437909/dp_design.txt)
- [HIVE-936](https://issues.apache.org/jira/browse/HIVE-936)

## 2、Terminology

> Static Partition (SP) columns: in DML/DDL involving multiple partitioning columns, the columns whose values are known at COMPILE TIME (given by user).

- **静态分区(SP)列:在涉及多个分区列的 DML/DDL 中，这些列的值在编译时已知(由用户给出)**。

> Dynamic Partition (DP) columns: columns whose values are only known at EXECUTION TIME.

- **动态分区(DP)列:在执行时才知道其值的列**。

## 3、Syntax

> DP columns are specified the same way as it is for SP columns – in the partition clause. The only difference is that DP columns do not have values, while SP columns do. In the partition clause, we need to specify all partitioning columns, even if all of them are DP columns.

DP 列的**指定方式与 SP 列的指定方式相同：在 partition 子句中。唯一的区别是 DP 列没有值，而 SP 列有**。在 partition 子句中，我们需要指定所有分区列，即使它们都是 DP 列。

> In INSERT ... SELECT ... queries, the dynamic partition columns must be specified last among the columns in the SELECT statement and in the same order in which they appear in the PARTITION() clause.

**在 `INSERT ... SELECT ...` 查询时，动态分区列必须在 SELECT 语句中的最后一个列中指定，且顺序与它们在 PARTITION() 子句中出现的顺序相同**。

> all DP columns – only allowed in nonstrict mode. In strict mode, we should throw an error. e.g.,

- 所有的 DP 列：只允许在非严格模式下。在严格模式下，应当抛出一个错误。如：

```sql
INSERT OVERWRITE TABLE T PARTITION (ds, hr)
SELECT key, value, ds, hr FROM srcpart WHERE ds is not null and hr>10;
```

> mixed SP & DP columns. e.g.,

- 混合 SP & DP 列，如：

```sql
INSERT OVERWRITE TABLE T PARTITION (ds='2010-03-03', hr)
SELECT key, value, /*ds,*/ hr FROM srcpart WHERE ds is not null and hr>10;
```

> SP is a subpartition of a DP: should throw an error because partition column order determins directory hierarchy. We cannot change the hierarchy in DML. e.g.,

- SP 是 DP 的子分区:应该抛出错误，因为分区列顺序决定了目录层次结构。我们不能改变 DML 中的层次结构。如：

```sql
-- throw an exception
INSERT OVERWRITE TABLE T PARTITION (ds, hr = 11)
SELECT key, value, ds/*, hr*/ FROM srcpart WHERE ds is not null and hr=11;
```

> multi-table insert. e.g.,

- 多表插入，如：

```sql
FROM S
INSERT OVERWRITE TABLE T PARTITION (ds='2010-03-03', hr)
SELECT key, value, ds, hr FROM srcpart WHERE ds is not null and hr>10
INSERT OVERWRITE TABLE R PARTITION (ds='2010-03-03', hr=12)
SELECT key, value, ds, hr from srcpart where ds is not null and hr = 12;
```

> CTAS – syntax is a little bit different from CTAS on non-partitioned tables, since the schema of the target table is not totally derived from the select-clause. We need to specify the schema including partitioning columns in the create-clause. e.g.,

- CTAS：语法与非分区表上的 CTAS 有一点不同，因为目标表的 schema 不是完全从 select 子句派生的。我们需要在 create-子句中指定包含分区列的模式。如：

【CTAS：CREATE TABLE AS SELECT】

```sql
CREATE TABLE T (key int, value string) PARTITIONED BY (ds string, hr int) AS
SELECT key, value, ds, hr+1 hr1 FROM srcpart WHERE ds is not null and hr>10;
```

> The above example shows the case of all DP columns in CTAS. If you want put some constant for some partitioning column, you can specify it in the select-clause. e.g,

上面的例子显示了 CTAS 中所有 DP 列的情况。

如果你想为某些分区列添加某个常数，可以在 select 子句中指定它。如：

```sql
CREATE TABLE T (key int, value string) PARTITIONED BY (ds string, hr int) AS
SELECT key, value, "2010-03-03", hr+1 hr1 FROM srcpart WHERE ds is not null and hr>10;
```

## 4、Design

> In SemanticAnalyser.genFileSinkPlan(), parse the input and generate a list of SP and DP columns. We also generate a mapping from the input ExpressionNode to the output DP columns in FileSinkDesc.

- 在 `SemanticAnalyser.genFileSinkPlan()`中，解析输入，并生成 SP 和 DP 列的列表。我们还生成了一个从输入 ExpressionNode 到 FileSinkDesc 中的输出 DP 列的映射。

> We also need to keep a HashFunction class in FileSinkDesc to evaluate partition directory names from the input expression value.

- 我们还需要在 FileSinkDesc 中保留一个 HashFunction 类，以便根据输入表达式值计算分区目录名

> In FileSinkOperator, setup the input -> DP mapping and Hash in initOp(). and determine the output path in processOp() from the mapping.

- 在 FileSinkOperator 中，在 initOp() 中，设置 input -> DP 映射和哈希。并从映射中确定 processOp() 中的输出路径。

> ObjectInspector setup?

- ObjectInspector 设置?

> MoveTask: since DP columns represent a subdirectory tree, it is possible to use one MoveTask at the end to move the results from the temporary directory to the final directory.

- MoveTask:由于 DP 列表示一个子目录树，所以可以在末尾使用 MoveTask 将结果从临时目录移动到最终目录。

> post exec hook for replication: remove all existing data in DP before creating new partitions. We should make sure replication hook recognize all the modified partitions.

- post exec hook for replication：在创建新分区之前，删除 DP 中的所有现有数据。我们应该确保复制钩子识别所有被修改的分区。

> metastore support: since we are creating multiple parititions in a DML, metastore should be able to create all these partitions. Need to investigate.

- metastore支持：由于我们在 DML 中创建多个分区，metastore 应该能够创建所有这些分区。需要调查。

## 5、Design issues

> 1) Data type of the dynamic partitioning column: 

（1）动态分区列的数据类型:

> A dynamic partitioning column could be the result of an expression. For example:

动态分区列可以是表达式的结果。例如:

```sql
-- part_col is partitioning column
create table T as select a, concat("part_", part_col) from S where part_col is not null;
```

> Although currently there is not restriction on the data type of the partitioning column, allowing non-primitive columns to be partitioning column probably doesn't make sense. The dynamic partitioning column's type should be derived from the expression. The data type has to be able to be converted to a string in order to be saved as a directory name in HDFS.

虽然目前对分区列的数据类型没有限制，但是允许非基本类型列作为分区列可能没有意义。

动态分区列的类型应该从表达式派生。数据类型必须能够转换为字符串，以便在 HDFS 中保存为目录名。

> 2) Partitioning column value to directory name conversion:

> After converting column value to string, we still need to convert the string value to a valid directory name. Some reasons are:

（2）分区列值到目录名的转换:

在将列值转换为字符串之后，我们仍然需要将字符串值转换为有效的目录名。一些原因是:

- 字符串的长度在理论上是无限的，但是 HDFS/local FS 目录名的长度是有限的。
- 字符串值可以包含 FS 路径名中保留的特殊字符(如`/`或`..`)。
- 我们应该为分区列 ObjectInspector 做什么?

> string length is unlimited in theory, but HDFS/local FS directory name length is limited.
> string value could contains special characters that is reserved in FS path names (such as '/' or '..').
> what should we do for partition column ObjectInspector?

> We need to define a UDF (say hive_qname_partition(T.part_col)) to take a primitive typed value and convert it to a qualified partition name.

我们需要定义一个 UDF(比如 `hive_qname_partition(T.part_col)`)来接受一个基本类型的值，并将其转换为限定的分区名。

> 3) Due to 2), this dynamic partitioning scheme qualifies as a hash-based partitioning scheme, except that we define the hash function to be as close as
the input value. We should allow users to plugin their own UDF for the partition hash function. Will file a follow up JIRA if there is sufficient interests.

（3）由于（2），该动态分区方案属于基于哈希的分区方案，只不过我们将哈希函数定义为接近于输入的值。我们应该允许用户为分区哈希函数插件自己的 UDF。如果有足够的兴趣，将提交 JIRA 跟进。

> 4) If there are multiple partitioning columns, their order is significant since that translates to the directory structure in HDFS: partitioned by (ds string, dept int) implies a directory structure of ds=2009-02-26/dept=2. In a DML or DDL involving partitioned table, So if a subset of partitioning columns are specified (static), we should throw an error if a dynamic partitioning column is lower. Example:

（4）如果有多个分区列，它们的顺序是重要的，因为这转换为 HDFS 的目录结构：`partitioned by (ds string, dept int)` 意味着目录结构为 `ds=2009-02-26/dept=2`。在涉及分区表的 DML 或 DDL 中，如果指定了分区列的子集(静态)，那么如果动态分区列较低，则应该抛出错误。例子:

```sql
create table nzhang_part(a string) partitioned by (ds string, dept int);
insert overwrite nzhang_part (dept=1) select a, ds, dept from T where dept=1 and ds is not null;
```