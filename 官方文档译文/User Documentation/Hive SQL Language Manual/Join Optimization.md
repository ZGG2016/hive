# Join Optimization

[TOC]

> For a general discussion of Hive joins including syntax, examples, and restrictions, see the [Joins](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Joins) wiki doc.

## 1、Improvements to the Hive Optimizer

> Version:The join optimizations described here were added in Hive version 0.11.0. See [HIVE-3784](https://issues.apache.org/jira/browse/HIVE-3784) and related JIRAs.

> This document describes optimizations of Hive's query execution planning to improve the efficiency of joins and reduce the need for user hints.

本文档描述了 Hive 查询执行计划的优化，来提高 join 的效率，减少了对用户 hints 的需求。

> Hive automatically recognizes various use cases and optimizes for them. Hive 0.11 improves the optimizer for these cases:

Hive 会自动识别各种用例，并进行优化。针对这些情况，Hive 0.11 改进了优化器:

- join 的一端装入内存，在新的优化中:

	- 该端作为散列表加载到内存中
	- 只需要扫描较大的表
	- 事实表占用较小的内存

- 星形架构 joins

- 许多情况下不再需要 hints。

- Map joins 由优化器自动拾取。

> Joins where one side fits in memory. In the new optimization:
> that side is loaded into memory as a hash table
> only the larger table needs to be scanned
> fact tables have a smaller footprint in memory
> Star-schema joins
> Hints are no longer needed for many cases.
> Map joins are automatically picked up by the optimizer.

## 2、Star Join Optimization

> A simple schema for decision support systems or data warehouses is the star schema, where events are collected in large fact tables, while smaller supporting tables (dimensions) are used to describe the data.

决策支持系统或数据仓库的一个简单模型是星型模型，其中事件收集在大型事实表中，而较小的支持表(维)用于描述数据。

> The [TPC DS](http://www.tpc.org/tpcds/) is an example of such a schema. It models a typical retail warehouse where the events are sales and typical dimensions are date of sale, time of sale, or demographic of the purchasing party. Typical queries aggregate and filter fact tables along properties in the dimension tables.

TPC DS 就是这种模型的一个例子。它对一个零售仓库建模，其中的事件是销售，典型的维度是销售日期、销售时间或采购方。典型查询根据维度表中的属性聚合和筛选事实表。

### 2.1、Star Schema Example

```sql
Select count(*) cnt
From store_sales ss
     join household_demographics hd on (ss.ss_hdemo_sk = hd.hd_demo_sk)
     join time_dim t on (ss.ss_sold_time_sk = t.t_time_sk)
     join store s on (s.s_store_sk = ss.ss_store_sk)
Where
     t.t_hour = 8
     t.t_minute >= 30
     hd.hd_dep_count = 2
order by cnt;
```

### 2.2、Prior Support for MAPJOIN

> Hive supports MAPJOINs, which are well suited for this scenario – at least for dimensions small enough to fit in memory. Before release 0.11, a MAPJOIN could be invoked either through an optimizer hint:

**Hive 支持 MAPJOINs，非常适合这种场景：至少维度表足够小可以装入内存**。在版本 0.11 前，MAPJOIN 可以**通过优化器 hint 调用**：

```sql
select /*+ MAPJOIN(time_dim) */ count(*) from
store_sales join time_dim on (ss_sold_time_sk = t_time_sk)
```

> or via auto join conversion:

或者**通过自动 join 转换**：

```sql
set hive.auto.convert.join=true;

select count(*) from
store_sales join time_dim on (ss_sold_time_sk = t_time_sk)
```

> The default value for hive.auto.convert.join was false in Hive 0.10.0.  Hive 0.11.0 changed the default to true (HIVE-3297). Note that hive-default.xml.template incorrectly gives the default as false in Hive 0.11.0 through 0.13.1.

在 Hive 0.10.0 中，`hive.auto.convert.join` 的默认值为 false。Hive 0.11.0 修改了默认值为 true。注意，在 Hive 0.11.0 到 0.13.1 中，`hive-default.xml.template` 错误地将默认值设置为 false。

> MAPJOINs are processed by loading the smaller table into an in-memory hash map and matching keys with the larger table as they are streamed through. The prior implementation has this division of labor:

**MAPJOINs 的处理方式是将较小的表加载到内存哈希表中，并在它们流过时，与较大的表匹配键**。

之前的实现有这样的分工:

- 本地工作：

	- 在本地机器上，通过标准的表扫描，从源读取记录。
	- 在内存中构建哈希表。
	- 写哈希表到本地磁盘。
	- 把哈希表上传到dfs。

- map 任务

	- 从本地磁盘(分布式缓存)读取哈希表到内存。
	- 和哈希表匹配记录的键。
	- 组合匹配和写入到输出

- 没有 reduce 任务

> Local work:
> read records via standard table scan (including filters and projections) from source on local machine
> build hashtable in memory
> write hashtable to local disk
> upload hashtable to dfs
> add hashtable to distributed cache
> Map task
> read hashtable from local disk (distributed cache) into memory
> match records' keys against hashtable
> combine matches and write to output
> No reduce task

#### 2.2.1、Limitations of Prior Implementation

> The MAPJOIN implementation prior to Hive 0.11 has these limitations:

Hive 0.11 之前的 MAPJOIN 实现有以下限制:

> The mapjoin operator can only handle one key at a time; that is, it can perform a multi-table join, but only if all the tables are joined on the same key. (Typical star schema joins do not fall into this category.)

- mapjoin 操作符一次只能处理一个键；也就是说，它可以执行多表连接，但前提是所有表都是按同一个键连接的。(典型的星型模式连接不属于这一类。)

> Hints are cumbersome for users to apply correctly and auto conversion doesn't have enough logic to consistently predict if a MAPJOIN will fit into memory or not.

- 对于用户来说，正确地应用 Hints 是很麻烦的，而且自动转换没有足够的逻辑来一致地预测 MAPJOIN 是否适合内存。

> A chain of MAPJOINs is not coalesced into a single map-only job, unless the query is written as a cascading sequence of `mapjoin(table, subquery(mapjoin(table, subquery....)`. Auto conversion never produces a single map-only job.

- 一个 MAPJOINs 链不会合并成一个单一的 map-only job，除非查询被写成一个级联序列的 `mapjoin(table, subquery(mapjoin(table, subquery....))`。自动转换从来不会产生单一的 map-only job。

> The hashtable for the mapjoin operator has to be generated for each run of the query, which involves downloading all the data to the Hive client machine as well as uploading the generated hashtable files.

- 每次运行查询都必须生成 mapjoin 操作符的哈希表，这涉及到下载所有数据到 Hive 客户端机器，以及上传生成的哈希表文件。

### 2.3、Enhancements for Star Joins

> The optimizer enhancements in Hive 0.11 focus on efficient processing of the joins needed in star schema configurations. The initial work was limited to star schema joins where all dimension tables after filtering and projecting fit into memory at the same time. Scenarios where only some of the dimension tables fit into memory are now implemented as well ([HIVE-3996](https://issues.apache.org/jira/browse/HIVE-3996)).

在 Hive 0.11 中的优化器增强集中在星型模型配置中所需的 joins 的高效处理上。

最初的工作仅限于星型模型连接，其中过滤和投射后的所有维度表都能同时放入内存。现在也实现了只有一些维度表适合内存的场景。

> The join optimizations can be grouped into three parts:

**join 优化可以分为三部分**:

- 当使用 maphint 时，在单个 map-only job 中执行操作树中的 mapjoin 链。

- 将优化扩展到自动转换的情况(在优化时生成适当的备份计划)。

- 完全在任务端生成内存中的哈希表。(未来的工作)。

> Execute chains of mapjoins in the operator tree in a single map-only job, when maphints are used.

> Extend optimization to the auto-conversion case (generating an appropriate backup plan when optimizing).

> Generate in-memory hashtable completely on the task side. (Future work.)

> The following sections describe each of these optimizer enhancements.

下面几节描述了这些优化器增强。

#### 2.3.1、Optimize Chains of Map Joins

> The following query will produce two separate map-only jobs when executed:

当执行时，下面的查询将产生两个独立的 map-only jobs：

```sql
select /*+ MAPJOIN(time_dim, date_dim) */ count(*) from
store_sales 
join time_dim on (ss_sold_time_sk = t_time_sk) 
join date_dim on (ss_sold_date_sk = d_date_sk)
where t_hour = 8 and d_year = 2002
```

> It is likely, though, that for small dimension tables the parts of both tables needed would fit into memory at the same time. This reduces the time needed to execute this query dramatically, as the fact table is only read once instead of reading it twice and writing it to HDFS to communicate between the jobs.

但是，对于小的维度表来说，两个表所需的部分很可能同时放入内存中。这大大减少了执行查询所需的时间，因为事实表只需要读一次，而不是读两次，再写到 HDFS，以便在 jobs 之间进行通信。

##### 2.3.1.1、Current and Future Optimizations

- Merge `M*-MR` patterns into a single MR.
- Merge `MJ->MJ` into a single MJ when possible.
- Merge `MJ*` patterns into a single Map stage as a chain of MJ operators. (Not yet implemented.)

> If [hive.auto.convert.join](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.auto.convert.join) is set to true the optimizer not only converts joins to mapjoins but also merges MJ* patterns as much as possible.

**如果 `hive.auto.convert.join` 设为了 true，优化器不仅将 joins 转换为 mapjoins，而且尽可能的合并 `MJ*` 模式**。【？？？】

#### 2.3.2、Optimize Auto Join Conversion

> When auto join is enabled, there is no longer a need to provide the map-join hints in the query. The auto join option can be enabled with two configuration parameters:

**当启用自动 join 时，就不再需要在查询中提供 map-join hints**。自动 join 选项可以通过两个配置参数启用:

	set hive.auto.convert.join.noconditionaltask = true;
	set hive.auto.convert.join.noconditionaltask.size = 10000000;

> The default for [hive.auto.convert.join.noconditionaltask](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.auto.convert.join.noconditionaltask) is true which means auto conversion is enabled. (Originally the default was false – see [HIVE-3784](https://issues.apache.org/jira/browse/HIVE-3784) – but it was changed to true by [HIVE-4146](https://issues.apache.org/jira/browse/HIVE-4146) before Hive 0.11.0 was released.)

**`hive.auto.convert.join.noconditionaltask` 的默认值为 true，表示启用自动转换**。(最初默认值为 false ，但在 Hive 0.11.0 发布之前，它被 Hive-4146 更改为 true。)

> The [size configuration](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.auto.convert.join.noconditionaltask.size) enables the user to control what size table can fit in memory. This value represents the sum of the sizes of tables that can be converted to hashmaps that fit in memory. Currently, n-1 tables of the join have to fit in memory for the map-join optimization to take effect. There is no check to see if the table is a compressed one or not and what the potential size of the table can be. The effect of this assumption on the results is discussed in the next section.

**大小配置 允许用户控制多大的表可以装入内存。这个值表示可以转换为装入内存的 hashmaps 的表大小的总和**。

目前，**为了使 map-join 优化生效，join 的 n-1 个表必须能装入内存**。不需要检查表是否被压缩，以及表的潜在大小是多少。

下一节将讨论这个假设对结果的影响。

> For example, the previous query just becomes:

```sql
select count(*) from
store_sales 
join time_dim on (ss_sold_time_sk = t_time_sk)
join date_dim on (ss_sold_date_sk = d_date_sk)
where t_hour = 8 and d_year = 2002
```

> If time_dim and date_dim fit in the size configuration provided, the respective joins are converted to map-joins. If the sum of the sizes of the tables can fit in the configured size, then the two map-joins are combined resulting in a single map-join. This reduces the number of MR-jobs required and significantly boosts the speed of execution of this query. This example can be easily extended for multi-way joins as well and will work as expected.

如果 time_dim 和 date_dim 符合所提供的大小配置，则将各自的 joins 转换为 map-join。如果表大小的总和能够适应配置的大小，那么两个 map-joins 将合并到一个 map-join 中。

这减少了所需 MR-jobs 的数量，并显著提高了该查询的执行速度。这个示例也可以很容易地扩展为多路 joins，并且可以像预期的那样工作。

> Outer joins offer more challenges. Since a map-join operator can only stream one table, the streamed table needs to be the one from which all of the rows are required. For the left outer join, this is the table on the left side of the join; for the right outer join, the table on the right side, etc. This means that even though an inner join can be converted to a map-join, an outer join cannot be converted. An outer join can only be converted if the table(s) apart from the one that needs to be streamed can be fit in the size configuration. A full outer join cannot be converted to a map-join at all since both tables need to be streamed.

Outer joins 提供了更多的挑战。

**由于 map-join 操作符只能流动一个表，所以流动的表必须是所有行都需要的表。对于 left outer join，这是 join 左侧的表；对于 right outer join，右边的表，等等**。

这意味着，尽管可以将 inner join 转换为 map-join，outer join 不能转换。只有当需要流动的表以外的表能够适合于大小配置时，才能转换 outer join。

由于两个表都需要流化，所以 full outer join 根本不能转换为 map-join。

> Auto join conversion also affects the sort-merge-bucket joins.

**自动 join 转换也会影响 sort-merge-bucket joins**。

> Version 0.13.0 and later:Hive 0.13.0 introduced [hive.auto.convert.join.use.nonstaged](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.auto.convert.join.use.nonstaged) with a default of false ([HIVE-6144](https://issues.apache.org/jira/browse/HIVE-6144) ).

> For conditional joins, if the input stream from a small alias can be directly applied to the join operator without filtering or projection, then it does not need to be pre-staged in the distributed cache via a MapReduce local task. Setting hive.auto.convert.join.use.nonstaged to true avoids pre-staging in those cases.

对于条件 joins，如果来自一个小别名的输入流可以直接应用到 join 操作符，而不需要过滤或投影，那么它就不需要通过 MapReduce 本地任务在分布式缓存中 pre-staged。

设置 `hive.auto.convert.join.use.nonstaged` 为 true 避免了这些情况下的 pre-staging。

###### 2.3.2.1、Current Optimization

> Group as many MJ operators as possible into one MJ.

将尽可能多的 MJ 操作符组成一个 MJ。

> As Hive goes through the conversion to map-joins for join operators based on the configuration flags, an effort is made at the end of these conversions to group as many together as possible. Going through in a sequence, if the sum of the sizes of the tables participating in the individual map-join operators is within the limit configured by the noConditionalTask.size flag, these MJ operators are combined together. This ensures more speedup with regard to these queries.

当 Hive 根据配置标志将 join 操作符转换为 map-joins 时，在这些转换结束时，会努力将尽可能多的操作符组合在一起。

如果参与各个 map-join 操作符的表的大小之和在 `noConditionalTask.size` 配置的限制内，则按顺序执行。这些 MJ 操作符组合在一起。这确保了这些查询的加速。

##### 2.3.2.2、Auto Conversion to SMB Map Join

> Sort-Merge-Bucket (SMB) joins can be converted to SMB map joins as well. SMB joins are used wherever the tables are sorted and bucketed. The join boils down to just merging the already sorted tables, allowing this operation to be faster than an ordinary map-join. However, if the tables are partitioned, there could be a slow down as each mapper would need to get a very small chunk of a partition which has a single key.

Sort-Merge-Bucket (SMB) joins 也可以转换为 SMB map joins。在对表排序和分桶的地方使用 SMB joins。

这个 join 归结为合并的已经排序的表，允许这个操作比普通的 map-join 更快。

但是，如果表是分区的，那么可能会出现慢下来的情况，因为每个 mapper 都需要从一个只有一个键的分区中获取非常小的块。

> The following configuration settings enable the conversion of an SMB to a map-join SMB:

通过以下配置设置，可以将 SMB 转换为 map-join SMB:

	set hive.auto.convert.sortmerge.join=true;
	set hive.optimize.bucketmapjoin = true;
	set hive.optimize.bucketmapjoin.sortedmerge = true;

> There is an option to set the big table selection policy using the following configuration:

有一个选项可以使用以下配置来设置大表选择策略:

	set hive.auto.convert.sortmerge.join.bigtable.selection.policy 
		= org.apache.hadoop.hive.ql.optimizer.TableSizeBasedBigTableSelectorForAutoSMJ;

> By default, the selection policy is average partition size. The big table selection policy helps determine which table to choose for only streaming, as compared to hashing and streaming.

默认情况下，选择策略为平均分区大小。与哈希和流相比，大表选择策略帮助确定选择哪个表流动。

> The available selection policies are:

可用的选择策略有:

	org.apache.hadoop.hive.ql.optimizer.AvgPartitionSizeBasedBigTableSelectorForAutoSMJ (default)
	org.apache.hadoop.hive.ql.optimizer.LeftmostBigTableSelectorForAutoSMJ
	org.apache.hadoop.hive.ql.optimizer.TableSizeBasedBigTableSelectorForAutoSMJ

> The names describe their uses. This is especially useful for the fact-fact join (query 82 in the [TPC DS](http://www.tpc.org/tpcds/) benchmark).

这些名称描述了它们的用途。这对于事实-事实连接特别有用。

###### 2.3.2.2.1、SMB Join across Tables with Different Keys

> If the tables have differing number of keys, for example Table A has 2 SORT columns and Table B has 1 SORT column, then you might get an index out of bounds exception.

**如果表有不同数量的键，例如表 A 有 2 个排序列，表 B 有 1 个排序列，那么你可能会得到一个索引越界异常**。

> The following query results in an index out of bounds exception because emp_person let us say for example has 1 sort column while emp_pay_history has 2 sort columns.

下面的查询会导致索引越界异常，因为 emp_person 有 1 个排序列，而 emp_pay_history 有 2 个排序列。

Error Hive 0.11:

```sql
SELECT p.*, py.*
FROM emp_person p INNER JOIN emp_pay_history py
ON p.empid = py.empid
```

> This works fine.

> Working query Hive 0.11

```sql
SELECT p.*, py.*
FROM emp_pay_history py INNER JOIN emp_person p
ON p.empid = py.empid
```

#### 2.3.3、Generate Hash Tables on the Task Side

> Future work will make it possible to generate in-memory hashtables completely on the task side.

未来的工作将完全在任务端生成内存中的哈希表成为可能。

##### 2.3.3.1、Pros and Cons of Client-Side Hash Tables

> Generating the hashtable (or multiple hashtables for multitable joins) on the client machine has drawbacks. (The client machine is the host that is used to run the Hive client and submit jobs.)

在客户端机器上生成哈希表(或用于多表连接的多个哈希表)有缺点。(客户端机器是运行 Hive 客户端，并提交作业的主机。)

> Data locality: The client machine typically is not a data node. All the data accessed is remote and has to be read via the network.

- 数据局部性:客户端机器通常不是数据节点。所有访问的数据都是远程的，必须通过网络读取。

> Specs: For the same reason, it is not clear what the specifications of the machine running this processing will be. It might have limitations in memory, hard drive, or CPU that the task nodes do not have.

- 规格:出于同样的原因，目前还不清楚运行此处理的机器的规格是什么。它可能有任务节点不具备的内存、硬盘驱动器或 CPU 限制。

> HDFS upload: The data has to be brought back to the cluster and replicated via the distributed cache to be used by task nodes.

- HDFS上传:数据被带回集群，通过分布式缓存复制给任务节点使用。

> Pre-processing the hashtables on the client machine also has some benefits:

在客户端机器上预处理哈希表也有一些好处:

> What is stored in the distributed cache is likely to be smaller than the original table (filter and projection).

- 存储在分布式缓存中的数据可能比原始表(过滤器和投影)要小。

> In contrast, loading hashtables directly on the task nodes using the distributed cache means larger objects in the cache, potentially reducing opportunities for using MAPJOIN.

- 相反，使用分布式缓存直接在任务节点上加载哈希表意味着缓存中的对象更大，潜在地减少了使用 MAPJOIN 的机会。

##### 2.3.3.2、Task-Side Generation of Hash Tables

> When the hashtables are generated completely on the task side, all task nodes have to access the original data source to generate the hashtable. Since in the normal case this will happen in parallel it will not affect latency, but Hive has a concept of storage handlers and having many tasks access the same external data source (HBase, database, etc.) might overwhelm or slow down the source.

当哈希表完全在任务端生成时，所有任务节点都必须访问原始数据源，来生成散列表。因为在正常情况下，这是并行发生的，它不会影响延迟，但 Hive 有一个存储处理程序的概念，当多个任务访问同一个外部数据源(HBase，数据库等)时，可能会使源崩溃或变慢。

###### 2.3.3.2.1、Further Options for Optimization

- 1.在维表上增加副本因子
- 2.使用分布式缓存存储维表

> Increase the replication factor on dimension tables.
> Use the distributed cache to hold dimension tables.