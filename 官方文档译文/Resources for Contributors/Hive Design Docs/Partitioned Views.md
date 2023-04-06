# Partitioned Views

[TOC]

> This is a followup to [ViewDev](https://cwiki.apache.org/confluence/display/Hive/ViewDev) for adding partition-awareness to views.

这是 ViewDev 为视图添加分区感知功能的后续操作。

## 1、Use Cases

> An administrator wants to create a set of views as a table/column renaming layer on top of an existing set of base tables, without breaking any existing dependencies on those tables. To read-only users, the views should behave exactly the same as the underlying tables in every way. Among other things, this means users should be able to browse available partitions.

管理员希望在现有的一组基表之上创建一组视图作为表/列的重命名层，而不破坏这些表的任何现有依赖关系。

**对于只读用户，视图的行为应该与底层表在各个方面完全相同。这意味着用户应该能够浏览可用的分区**。

> A base table is partitioned on columns (ds,hr) for date and hour. Besides this fine-grained partitioning, users would also like to see a virtual table of coarse-grained (date-only) partitioning in which the partition for a given date only appears after all of the hour-level partitions of that day have been fully loaded.

基表按列 (ds、hr) 划分日期和小时分区。

除了这种细粒度分区之外，用户还希望看到一个粗粒度(仅限日期)分区的虚拟表，其中**给定日期的分区只在当天的所有小时级分区全部加载完毕后才出现**。

> A view is defined on a complex join+union+aggregation of a number of underlying base tables and other views, all of which are themselves partitioned. The top-level view should also be partitioned accordingly, with a new partition not appearing until corresponding partitions have been loaded for all of the underlying tables.

视图被定义在一系列底层基表和其他视图的复杂的 join+union+aggregation 之上，所有这些视图本身都是分区的。

**顶层视图也应该相应地进行分区，直到所有底层表加载完相应的分区，才会出现新的分区**。

### 1.1、Approaches

> One possible approach mentioned in [HIVE-1079](https://issues.apache.org/jira/browse/HIVE-1079) is to infer view partitions automatically based on the partitions of the underlying tables. A command such as SHOW PARTITIONS could then synthesize virtual partition descriptors on the fly. This is fairly easy to do for use case #1, but potentially very difficult for use cases #2 and #3. So for now, we are punting on this approach.

在 HIVE-1079 中提到的一种可能的方法是**根据底层表的分区自动推断视图分区**。

然后，像 SHOW PARTITIONS 这样的命令可以动态地合成虚拟分区描述符。这对于用例 #1 来说相当容易，但是对于用例 #2 和 #3 来说可能非常困难。所以现在，我们只能用这种方法。

> Instead, per [HIVE-1941](https://issues.apache.org/jira/browse/HIVE-1941), we will require users to explicitly declare view partitioning as part of CREATE VIEW, and explicitly manage partition metadata via ALTER VIEW ADD|DROP PARTITION. This allows all of the use cases to be satisfied (while placing more burden on the user, and taking up more metastore space). With this approach, there is no real connection between view partitions and underlying table partitions; it's even possible to create a partitioned view on an unpartitioned table, or to have data in the view which is not covered by any view partition. One downside here is that a UI will not be able to show last access time and physical information such as file size when browsing available partitions. (In theory, stats could work via an explicit ANALYZE, but analyzing a view would need some work.)

相反，对于 HIVE-1941，我们将要求用户**显式地声明视图分区，作为 CREATE VIEW 的一部分，并通过 ALTER VIEW ADD|DROP PARTITION 显式地管理分区元数据**。

这允许满足所有的用例(同时给用户增加了更多的负担，并占用了更多的元存储空间)。

使用这种方法，视图分区和底层表分区之间没有真正的联系；甚至可以在未分区的表上创建分区视图，或者在视图中包含没有被任何视图分区覆盖的数据。

这样做的一个缺点是，用户界面在浏览可用分区时，不能显示最后访问时间和文件大小等物理信息。(理论上，统计数据可以通过明确的分析工作，但分析一个视图需要一些工作。)

### 1.2、Syntax

```sql
CREATE VIEW [IF NOT EXISTS] view_name [(column_name [COMMENT column_comment], ...) ]
[COMMENT table_comment]
[PARTITIONED ON (col1, col2, ...)]
[TBLPROPERTIES ...]
AS SELECT ...
 
ALTER VIEW view_name ADD [IF NOT EXISTS] partition_spec partition_spec ...
 
ALTER VIEW view_name DROP [IF EXISTS] partition_spec, partition_spec, ...
 
partition_spec:
  : PARTITION (partition_col = partition_col_value, partition_col = partiton_col_value, ...)
```
> Notes:

> Whereas CREATE TABLE uses PARTITIONED BY, CREATE VIEW uses PARTITIONED ON. This difference is intentional because in CREATE TABLE, the PARTITIONED BY clause specifies additional column definitions which are appended to the non-partitioning columns. With CREATE VIEW, the PARTITIONED ON clause references (by name) columns already produced by the view definition. Only column names appear in PARTITIONED ON; no types etc. However, to match the CREATE TABLE convention of trailing partitioning columns, the columns referenced by the PARTITIONED ON clause must be the last columns in the view definition, and their order in the PARTITIONED ON clause must match their order in the view definition.

CREATE TABLE 使用 PARTITIONED BY，CREATE VIEW 使用 PARTITIONED ON。

这种差异是有意为之的，因为在 CREATE TABLE 中，PARTITIONED BY 子句指定追加到非分区列上额外的列定义。

使用 CREATE VIEW，PARTITIONED ON 子句引用(按名称)的列已经由视图定义产生。

只有列名出现在 PARTITIONED ON，没有类型等等。

但是，为了匹配最后分区列的 CREATE TABLE 约定，PARTITIONED ON 子句引用的列必须是视图定义中的最后一列，并且它们在 PARTITIONED ON 子句中的顺序必须与它们在视图定义中的顺序相匹配。

> The ALTER VIEW ADD/DROP partition syntax is identical to ALTER TABLE, except that it is illegal to specify a LOCATION clause.

ALTER VIEW ADD/DROP partition 的语法与 ALTER TABLE 相同，只是指定 LOCATION 子句是非法的。

> Other ALTER TABLE commands which operate on partitions (e.g. TOUCH/ARCHIVE) are not supported. (But maybe we need to support TOUCH?)

不支持对分区操作其他的 ALTER TABLE 命令(例如 TOUCH/ARCHIVE)。(但也许我们需要支持触摸？)

### 1.3、Metastore

> When storing view partition descriptors in the metastore, Hive omits the storage descriptor entirely. This is because there is no data associated with the view partition, so there is no need to keep track of partition-level column descriptors for table schema evolution, nor a partition location.

在 metastore 中存储视图分区描述符时，Hive 会完全忽略存储描述符。

这是因为没有与视图分区相关联的数据，所以不需要跟踪表模式演化的分区级列描述符，也不需要跟踪分区位置。

### 1.4、Strict Mode

> Hive strict mode (enabled with hive.mapred.mode=strict) prevents execution of queries lacking a partition predicate. This only applies to base table partitions. What does this mean?

**Hive 严格模式（使用 `hive.mapred.mode=strict` 启用）禁止执行没有分区谓词的查询**。

**这只适用于基表分区**。这是什么意思？

> Suppose you have table T1 partitioned on C1, and view V1 which selects FROM T1 WHERE C1=5. Then a query such as SELECT * FROM V1 will succeed even in strict mode, since the predicate inside of the view constrains C1.

假设你有一个表 T1 ，在 C1 上分区，视图 V1 从 T1 中选择 C1=5 创建。然后，像 `SELECT * FROM V1` 这样的查询即使在严格模式下也会成功，因为视图内的谓词约束 C1。

> Likewise, suppose you have view V2 which selects from T1 (with no WHERE clause) and is partitioned on C2. Then a query such as SELECT * FROM V2 WHERE C2=3 will fail; even though the view partition column is constrained, there is no predicate on the underlying T1's partition column C1.

同样，假设你有一个视图 V2，它从 T1 中选择(没有 WHERE 子句)，并且在 C2 上分区。然后，像 `SELECT * FROM V2 WHERE C2=3`这样的查询将失败，即使视图分区列受到约束，但在底层 T1 的分区列 C1 上没有谓词。

### 1.5、View Definition Changes

> Currently, changing a view definition requires dropping the view and recreating it. This implies dropping and recreating all existing partitions as well, which could be very expensive.

目前，更改视图定义需要删除视图并重新创建它。

这意味着还要删除并重新创建所有现有的分区，这可能非常昂贵。

> This implies that followup support for [CREATE OR REPLACE VIEW](https://issues.apache.org/jira/browse/HIVE-1078) is very important, and that it needs to preserve existing partitions (after validating that they are still compatible with the new view definition).

这意味着后续对 CREATE 或 REPLACE VIEW 的支持是非常重要的，它需要保留现有的分区(在验证它们仍然与新的视图定义兼容之后)。

## 2、Hook Information

> Although there is currently no connection between the view partition and underlying table partitions, Hive does provide dependency information as part of the hook invocation for ALTER VIEW ADD PARTITION. It does this by compiling an internal query of the form

虽然目前视图分区和底层表分区之间没有联系，但是 Hive 确实为 ALTER VIEW ADD PARTITION 的钩子调用提供了依赖信息。它通过编译这种形式的内部查询来实现这一点：

```sql
SELECT * FROM view_name
WHERE view_partition_col1 = 'val1' AND view_partition_col=2 = 'val2' ...
```

> and then capturing the table/partition inputs for this query and passing them on to the ALTER VIEW ADD PARTITION hook results.

然后捕获这个查询的表/分区输入，并将它们传递给 ALTER VIEW ADD PARTITION 钩子结果。

> This allows applications to track the dependencies themselves. In the future, Hive will automatically populate these dependencies into the metastore as part of [HIVE-1073](https://issues.apache.org/jira/browse/HIVE-1073).

这允许应用程序跟踪依赖项本身。将来，Hive 会自动将这些依赖项作为 Hive -1073 的一部分填充到 metastore 中。