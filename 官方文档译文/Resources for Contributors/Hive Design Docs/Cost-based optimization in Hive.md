# Cost-based optimization in Hive

[TOC]

## Abstract

Apache Hadoop is a framework for the distributed processing of large data sets using clusters of computers typically composed of commodity hardware. Over last few years Apache Hadoop has become the de facto platform for distributed data processing using commodity hardware. Apache Hive is a popular SQL interface for data processing using Apache Hadoop.

User submitted SQL query is converted by Hive to physical operator tree which is optimized and converted to Tez Jobs and is then executed on Hadoop cluster. Distributed SQL query processing in Hadoop differs from conventional relational query engine when it comes to handling of intermediate result sets. Hive query processing often requires sorting and reassembling of intermediate result set; this is called shuffling in Hadoop parlance.

Most of the existing query optimizations in Hive are about minimizing shuffling cost. Currently user would have to submit an optimized query to Hive with right join order for query to be executed efficiently. Logical optimizations in Hive are limited to filter push down, projection pruning and partition pruning. Cost based logical optimizations can significantly improve Apache Hive’s query latency and ease of use.

hive 中大部分的查询优化都是关于如何最小化 shuffle 成本。目前，用户必须使用正确的 join 顺序向 hive 提交优化查询，才能有效执行查询。

hive 中的逻辑优化主要在谓词下推、投影裁剪和分区裁剪。基于成本的逻辑优化能显著改善查询延迟和易用性。

Join reordering and join algorithm selection are few of the optimizations that can benefit from a cost based optimizer. Cost based optimizer would free up user from having to rearrange joins in the right order or from having to specify join algorithm by using query hints and configuration options. This can potentially free up users to model their reporting and ETL needs close to business process without having to worry about query optimizations.

join 重排和 join 算法选择少数可以从基于成本的优化获益的优化方案。基于成本的优化器能够使用户从重排 join 和使用 hint 和配置项指定 join 算法中解放出来。

这可能可以让用户在接近业务流程的情况下对他们的报告和 ETL 需求进行建模，而不必担心查询优化。

Calcite is an open source cost based query optimizer and query execution framework. Calcite currently has more than fifty query optimization rules that can rewrite query tree, and an efficient plan pruner that can select cheapest query plan in an optimal manner. In this paper we discuss how Calcite can be used to introduce Cost Based Logical Optimizer (CBO) in to Apache Hive.

Calcite 是一个开源的基于成本的查询优化器和查询执行框架。当前，Calcite 具有超过 50 个能重写查询树的查询优化规则，和能选择最低成本的查询计划的高效计划裁剪。

CBO will be introduced in to Hive in a Phased manner. In the first phase, Calcite would be used to reorder joins and to pick right join algorithm so as to reduce query latency. Table cardinality and Boundary statistics will be used for this cost based optimizations.

将 CBO 以分阶段的方式引入 hive 中。在第一阶段，Calcite 将重排 join，并选择正确的 join 算法。并会使用表基数和边界统计信息。

## 1. INTRODUCTION

Hive is a data-warehousing infrastructure on top of Apache Hadoop. Hive takes advantage of Hadoop’s massive scale out and fault tolerance capabilities for data storage and processing on commodity hardware. Hive is designed to enable easy data summarization, ad-hoc querying and analysis of large volumes of data. Hive SQL is the declarative query language, which enables users familiar with SQL to do ad-hoc querying, summarization and data analysis easily.

In past Hadoop jobs tended to have high latencies and incurred substantial overheads in job submission and scheduling. As a result - latency for Hive queries was generally very high even when data sets involved were very small. As a result Hive was typically used for ETL and not much for interactive queries. With Hadoop2 and Tez the overheads for job submission and job scheduling have gone down significantly. In Hadoop version one, the jobs that could be executed could only be Map-Reduce Jobs.  With Hadoop2 and Tez that limitation no longer apply.

In Hadoop the output of mapper is sorted and sometimes persisted on the local disk of the mapper. This sorted mapper output is then send to appropriate reducer which then combines sorted results from different mappers.  While executing multiple map-reduce jobs where output of one job needs to be consumed by the successive map-reduce job, the output of preceding map-reduce job needs to be persisted into HDFS; this persistence is costly as the data needs to be copied to other nodes based on the replication factor of HDFS.

Hive on top of Hadoop version 1 often had to submit multiple map-reduce jobs to complete query processing. This Map-Reduce job pipeline degraded performance, as the intermediate result set now needs to be persisted to fault tolerant HDFS. Also submitting jobs and scheduling them were relatively costly operations. With Hadoop2 and Tez the cost of job submission and scheduling is minimized. Also Tez does not restrict the job to be only Map followed by Reduce; this implies all of the query execution can be done in a single job without having to cross job boundaries. This would result in a significant cost savings, as the intermediate result sets need not be persisted to HDFS or to even local disk.

Query optimizations in a relational query engine can be broadly classified as logical query optimizations and physical query optimizations. Logical query optimizations generally refer to query optimizations that can be derived based on relational algebra independent of the physical layer in which query is executed. Physical query optimizations are query optimizations that are cognizant of physical layer primitives. For Hive, the physical layer implies Map-Reduce and Tez primitives.

关系查询引擎中的查询优化可以大致分为逻辑查询优化和物理查询优化。 

逻辑查询优化通常是指可以基于关系代数导出的查询优化，与执行查询的物理层无关。

物理查询优化是识别物理层原语的查询优化。 

对于 Hive，物理层意味着 Map-Reduce 和 Tez 原语。

Currently logical query optimizations in Hive can be broadly categorized as follows:

当前 hive 中的逻辑查询优化大概分为如下几类：

- Projection Pruning
- Deducing Transitive Predicates
- Predicate Push down
- Merging of Select-Select, Filter-Filter in to single operator
- Multi-way Join
- Query Rewrite to accommodate for Join skew on some column values

Physical optimizations in Hive can be broadly classified as follows:

当前 hive 中的物理查询优化大概分为如下几类：

- Partition Pruning
- Scan pruning based on partitions and bucketing
- Scan pruning if query is based on sampling
- Apply Group By on the map side in some cases
- Perform Join on the Mapper
- Optimize Union so that union can be performed on map side only
- Decide which table to stream last, based on user hint, in a multi way join
- Remove unnecessary reduce sink operators
- For queries with limit clause, reduce the number of files that needs to be scanned for the table.
- For queries with Limit clause, restrict the output coming from mapper by restricting what Reduce Sink operator generates.
- Reduce the number of Tez jobs needed for answering user submitted SQL query
- Avoid Map-Reduce jobs in case of simple fetch query
- For simple fetch queries with aggregation, perform aggregation without Map-Reduce tasks
- Rewrite Group By Query to use index table instead of original table
- Use Index scans when predicates above table scan is equality predicates and columns in predicate have indexes on it.

In Hive most of the optimizations are not based on the cost of query execution. Most of the optimizations do not rearrange the operator tree except for filter push down and operator merging.  Most of the operator tree mutation is for removing reduce-sink and reducer operator.  Listed below are some of optimization decisions that can benefit from a CBO:

在 hive 中，大多数优化并不是基于查询执行的成本。 除了过滤下推和操作符合并之外，大多数优化不会重新排列操作符树。 大多数操作符树变异是为了删除 reduce-sink 和 reducer 操作符。 

下面列出了一些可以从 CBO 中受益的优化决策：

- How to order Join
- What algorithm to use for a given Join
- Should the intermediate result be persisted or should it be recomputed on operator failure.
- The degree of parallelism at any operator (specifically number of reducers to use).
- Semi Join selection

Calcite is an open source, Apache Licensed, query planning and execution framework. Many pieces of Calcite are derived from Eigenbase Project. Calcite has optional JDBC server, query parser and validator, query optimizer and pluggable data source adapters. One of the available Calcite optimizer is a cost based optimizer based on volcano paper. Currently different pieces of Calcite is used in following projects/products:

- Apache Drill
- Cascading (Lingual)
- Lucid DB
- Mondrian/Pentaho

！[CalciteArchitectureOverview.jpg](./images/CalciteArchitectureOverview.jpg)

Calcite currently has over fifty cost based optimization rules. Some of the prominent cost based optimization rules are listed below:

- Push Join through Union
- Push Filter past Table Function
- Join Reordering
- Semi Join selection
- Push Aggregate through Union
- Pull Aggregate through Union
- Pull Constants through Aggregate
- Merge Unions

In this document we propose to use Calcite’s cost based optimizer, Volcano, to perform Cost Based Optimizations in Hive. We propose to implement Calcite based CBO in a phased manner. Note here that proposal is to use Calcite’s optimizer only and nothing else. Listed below are the envisioned stages of introducing CBO in to Hive using Calcite:

在本文档中，我们建议使用 Calcite 的基于成本的优化器 Volcano 在 Hive 中执行基于成本的优化。 

我们建议分阶段实施基于 Calcite 的 CBO。 请注意，这里的建议是仅使用 Calcite 的优化器，而不使用其他任何东西。 

下面列出了使用 Calcite 将 CBO 引入 Hive 的设想阶段：

- Phase1 – Join Reordering & Join algorithm Selection
	- Table cardinality and boundary statistics will be used to compute operator cardinality.
	- Hive operator tree will be converted to Calcite operator tree.
	- Volcano optimizer in Calcite will be used to rearrange joins and to pick the join algorithm.
	- Optimized Calcite operator tree would be converted back to Hive AST and will be executed as before. So all of the Hive’s existing optimizations would run on top of Calcite optimized SQL.

- Phase2 – Add support for Histograms, use other optimizations in Calcite
	- Introduce space efficient histograms
	- Change operator cardinality computation to use histograms
	- Register additional optimization rules available in Calcite like the ones listed above.

- Phase3 – Code restructuring so that Calcite generates optimized Hive Operator tree
	- Unlike phase1 Hive AST would be directly translated into Calcite operator tree.
	- Optimize Calcite operator tree using Volcano optimizer.
	- Convert optimized Calcite operator tree back into Hive Operator tree. This is unlike phase1 where optimized Calcite operator tree is converted to Hive AST.

## 2. RELATED WORK

## 3. BACKGROUND

### Hive Query optimization issues

Hive converts user specified SQL statement to AST that is then used to produce physical operator tree. All of the query optimizations are performed on the physical operator tree. Hive keeps semantic info separate from query operator tree. Semantic info is extracted during plan generation that is then looked up often during down stream query optimizations. Adding new query optimizations into Hive is often made difficult by the lack of proper logical query plan and due to Semantic info and query tree separation.

Hive 将用户指定的 SQL 语句转换为 AST，然后用于生成物理运算符树。 所有查询优化都在物理运算符树上执行。 

Hive 将语义信息与查询运算符树分开。 在计划生成期间提取语义信息，然后在下游查询优化期间经常查找这些信息。 由于缺乏适当的逻辑查询计划以及语义信息和查询树的分离，向 Hive 添加新的查询优化通常会变得困难。

### TEZ

Apache Tez generalizes the MapReduce paradigm to execute a complex DAG (directed acyclic graph) of tasks. Refer to the following link for more info.

http://hortonworks.com/blog/apache-tez-a-new-chapter-in-hadoop-data-processing/

### Join algorithms in Hive

Hive only supports equi-Join currently. Hive Join algorithm can be any of the following:

#### Multi way Join

If multiple joins share the same driving side join key then all of those joins can be done in a single task.

Example: `(R1 PR1.x=R2.a  - R2) PR1.x=R3.b - R3) PR1.x=R4.c - R4`

All of the join can be done in the same reducer, since R1 will already be sorted based on join key x.

#### Common Join

Use Mappers to do the parallel sort of the tables on the join keys, which are then passed on to reducers. All of the tuples with same key is given to same reducer. A reducer may get tuples for more than one key. Key for tuple will also include table id, thus sorted output from two different tables with same key can be recognized. Reducers will merge the sorted stream to get join output.

使用映射器对连接键上的表进行并行排序，然后将其传递给 reducer。 所有具有相同键的元组都被赋予相同的 reducer。 一个 reducer 可能会获得多个键的元组。 元组的键还将包括表 ID，因此可以识别具有相同键的两个不同表的排序输出。 reducer 将合并排序流以获得连接输出。

#### Map Join

Useful for star schema joins, this joining algorithm keeps all of the small tables (dimension tables) in memory in all of the mappers and big table (fact table) is streamed over it in the mapper. This avoids shuffling cost that is inherent in Common-Join. For each of the small table (dimension table) a hash table would be created using join key as the hash table key.

对于星型模式联接很有用，此联接算法将所有小表（维度表）保留在所有 mappers 的内存中，并且大表（事实表）在 mapper 中流式传输。 这避免了 Common-Join 固有的洗牌成本。 对于每个小表（维度表），将使用连接键作为哈希表键来创建哈希表。

#### Bucket Map Join

If the joining keys of map-join are bucketed then instead of keeping whole of small table (dimension table) in every mapper, only the matching buckets will be kept. This reduces the memory footprint of the map-join.

如果 map-join 的连接键是分桶的，那么不是在每个 mapper 中保留整个小表（维度表），而是只保留匹配的桶。 这减少了映射连接的内存占用。

#### SMB Join

This is an optimization on Bucket Map Join; if data to be joined is already sorted on joining keys then hash table creation is avoided and instead a sort merge join algorithm is used.

这是对 Bucket Map Join 的优化； 如果要连接的数据已经在连接键上排序，则避免创建哈希表，而是使用排序合并连接算法。

#### Skew Join

If the distribution of data is skewed for some specific values, then join performance may suffer since some of the instances of join operators (reducers in map-reduce world) may get over loaded and others may get under utilized. On user hint, hive would rewrite a join query around skew value as union of joins.

如果数据的分布对于某些特定值是倾斜的，则连接性能可能会受到影响，因为连接运算符的某些实例（map-reduce 世界中的reducers）可能会过载，而其他实例可能会得到充分利用。 根据用户提示，hive 会将围绕倾斜值的连接查询重写为连接并集。

Example `R1 PR1.x=R2.a  - R2` with most data distributed around x=1 then this join may be rewritten as `(R1 PR1.x=R2.a and PR1.x=1    - R2)`` union all `(R1 PR1.x=R2.a and PR1.x<>1    - R2)``

## 4. Implementation details

CBO will be introduced in to Hive in three different phases. Following provides high-level overview of these phases: 

### Phase 1

![Phase1.jpg](./images/Phase1.jpg)

Statistics:

- Table Cardinality
- Column Boundary Stats: min, max, avg, number of distinct values
 
Cost Based Optimizations:

- Join ordering
- Join Algorithm
 
Restrictions:

- Calcite CBO will be used only for select expressions
- Calcite CBO won’t be used if select expression contains any of the following operators:
	- Sort By

Hive supports both total ordering (order by) and partial ordering (sort by). Partial ordering cannot be represented in relational algebra and SQL. In future we may represent Sort By as a table function.

- Map/Reduce/Transform

Hive allows users to specify map/reduce/transform operator in the sql; data would be transformed using provided mapper/reducer scripts. There is no direct translation for these operators to relational algebra. We may represent them as table function in future.

- Cluster By/Distribute By

Cluster By and Distribute By are used mainly with the [Transform/Map-Reduce Scripts](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Transform). But, it is sometimes useful in SELECT statements if there is a need to partition and sort the output of a query for subsequent queries. Cluster By is a short-cut for both Distribute By and Sort By. Hive uses the columns in Distribute By to distribute the rows among reducers. All rows with the same Distribute By columns will go to the same reducer. However, Distribute By does not guarantee clustering or sorting properties on the distributed keys.

- Table Sample

The TABLESAMPLE clause allows the users to write queries for samples of the data instead of the whole table. The TABLESAMPLE clause can be added to any table in the FROM clause. In future we may represent Table Sample as table function.

- Lateral Views

Lateral view is used in conjunction with user-defined table generating functions such as explode(). UDTF generates one or more output rows for each input row. A lateral view first applies the UDTF to each row of base table and then joins resulting output rows to the input rows to form a virtual table having the supplied table alias.

- UDTF (Table Functions)
- PTF (Partitioned Table Functions)
 

Calcite related enhancements:

- Introduce Operators to represent hive relational operators. Table Scan, Join, Union, Select, Filter, Group By, Distinct, Order By. These operators would implement a calling convention with physical cost for each of these operators.
- Introduce rules to convert Joins from CommonJoin to MapJoin, MapJoin to BucketJoin, BucketJoin to SMBJoin, CommonJoin to SkewJoin.
- Introduce rule to merge joins so that a single join operator will represent multi-way join (similar to MergedJoin in Hive).
- Merged-Join in Hive will be translated to MultiJoinRel in Calcite.

### Phase 2

![Phase2.jpg](./images/Phase2.jpg)

Statistics:

- Histograms

Cost Based Optimizations:

- Join ordering based on histograms
- Join Algorithm – histograms are used for estimating join selectivity
- Take advantage of additional optimizations in Calcite. The actual rules to use is TBD.

### Phase 3

![Phase3.jpg](./images/Phase3.jpg)

### Configuration

The configuration parameter [hive.cbo.enable](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.cbo.enable) determines whether cost-based optimization is enabled or not.

### Proposed Cost Model

Hive employs parallel-distributed query execution using Hadoop cluster. This implies for a given query operator tree different operators could be running on different nodes. Also same operator may be running in parallel on different nodes in the cluster, processing different partitions of the original relation. This parallel execution model induces high I/O and CPU costs. Hive query execution cost tends to be more I/O centric due to following reasons.

Hive 使用 Hadoop 集群进行并行分布式查询执行。

这意味着对于给定的查询操作符树，不同的操作符可以在不同的节点上运行。同样的操作符也可以在集群中的不同节点上并行运行，处理原始关系的不同分区。 

这种并行执行模型会导致较高的 I/O 和 CPU 成本。 由于以下原因，Hive 查询执行成本往往更以 I/O 为中心。

- Shuffling cost

Data needed by an operator from its child operator in query tree requires assembling data from all instances of child operator. This data then needs to be sorted and chopped up so that a partition of the relation is presented to an instance of the operator.

查询树中的操作符所需的来自其子操作符的数据需要组装来自子操作符的所有实例的数据。 然后需要对这些数据进行排序和切碎，以便将关系的分区呈现给操作符的实例。

This shuffling cost involves cost of writing intermediate result set to local file system, cost of reading data back from local file system, and cost of transferring intermediate result set to the node that is operating child processor. In addition to I/O cost, shuffling also requires sorting of data that should be counted towards CPU cost.

这种 shuffling 成本包括将中间结果集写入本地文件系统的成本、从本地文件系统读回数据的成本以及将中间结果集传输到正在操作子处理器的节点的成本。 除了 I/O 成本之外，混洗还需要对数据进行排序，这些数据应计入 CPU 成本。

- HDFS Read/Write is costly

Reading and writing data to HDFS is more costly compared to local FS. In Map-Reduce framework Table Scan would typically read data from HDFS and would write data to HDFS when switching between two Map-Reduce jobs. In Tez all of the operators should work with in a single Tez Job and hence we shouldn’t have to pay the cost of writing intermediate result set to HDFS.

与本地 FS 相比，向 HDFS 读取和写入数据的成本更高。 在 Map-Reduce 框架中，表扫描通常会从 HDFS 读取数据，并在两个 Map-Reduce 作业之间切换时将数据写入 HDFS。 在 Tez 中，所有操作符都应该在单个 Tez 作业中使用，因此我们不必支付将中间结果集写入 HDFS 的成本。

Cost based optimizations in Hive will keep track of cost in terms of

- CPU usage
- IO Usage
- Cardinality
- Average size of the tuple in the relation.
 
Average size of the tuple in relation and cardinality of the relation will be used to estimate resources needed to hold a relation in memory. Memory needed to hold a relation is used to decide whether certain join algorithms, like Map/Bucket Join, can be used.

关系中元组的平均大小和关系的基数将用于估计在内存中保存关系所需的资源。 保存关系所需的内存用于决定是否可以使用某些连接算法，例如 Map/Bucket Join。

Volcano optimizer in Calcite compares cost of two equivalent query plans by getting cost of each operator in the tree and summing them up to find cumulative cost. Plan with lowest cumulative cost is picked as the best plan to execute the query. “VolcanoCost” is the Java class representing cost for Calcite’s Volcano optimizer. “VolcanoCost” comparison operators seem to take into consideration only the number of rows to decide if one cost is less than other.

Calcite 中的 Volcano 优化器通过获取树中每个操作符的成本并将它们相加以找到累积成本来比较两个等效查询计划的成本。 

选择具有最低累积成本的计划作为执行查询的最佳计划。 “VolcanoCost” 是代表 Calcite 火山优化器成本的 Java 类。 “VolcanoCost” 比较操作符似乎只考虑行数来确定一个成本是否小于其他成本。

For Hive we want to consider CPU and IO usage first before comparing cardinality.

对于 Hive，我们希望在比较基数之前首先考虑 CPU 和 IO 使用情况。

We propose to introduce a new “RelOptCost” implementation “HiveVolcanoCost” derived from “VolcanoCost”. “HiveVolcanoCost” will keep CPU, I/O, Cardinality, and average size of tuple. Cost comparison algorithm will give importance to CPU and IO cost before paying attention to cardinality. CPU and IO cost will be stored in nano seconds granularity. Following is the pseudo code for “RelOptCost.isLe” function:

我们建议引入一种新的 “RelOptCost” 实现 “HiveVolcanoCost”，该实现源自 “VolcanoCost”。 “HiveVolcanoCost” 将保留 CPU、I/O、基数和元组的平均大小。 成本比较算法在关注基数之前会优先考虑 CPU 和 IO 成本。 CPU 和 IO 成本将以纳秒粒度存储。 以下是 “RelOptCost.isLe” 函数的伪代码：

```
Class HiveVolcanoCost extends VolcanoCost {

Double m_sizeOfTuple;

 

               @Override

                  public boolean isLe(RelOptCost other) {

                                    VolcanoCost that = (VolcanoCost) other;

                                    if (((this.dCpu + this.dIo) < (that.dCpu + that.dIo))

                                                                        || ((this.dCpu + this.dIo) == (that.dCpu + that.dIo)

                                                            && this.dRows <= that.dRows)) {

                                                      return true;

                                    } else {

                                                      return false;

                                    }

                  }

}
```

 
**Design Choice:**

In the absence of histograms, cardinality can be assumed to follow uniform distribution on the distinct values. With this approach cardinality/distinct computation could always follow same code path (Histograms). Alternative is to use heuristics when histograms are not available (like the one described by Hector Garcia Molina, Jeffrey D. Ullman and Jennifer Widom in “Database Systems”). Currently Hive statistics doesn’t keep track of the distinct values for an attribute (only the number of distinct values is kept); however from min, max,number of distinct values, and table cardinality uniformly distributed histogram can be constructed. Following describes formulas for a uniform histogram construction.

在没有直方图的情况下，可以假设基数遵循不同值的均匀分布。 通过这种方法，cardinality/distinct 可以始终遵循相同的代码路径（直方图）。 

另一种方法是在直方图不可用时使用启发式方法（如 Hector Garcia Molina、Jeffrey D. Ullman 和 Jennifer Widom 在“数据库系统”中描述的方法）。 

目前，Hive 统计信息不跟踪属性的不同值（仅保留不同值的数量）； 然而，根据 min, max,number of distinct values, and table cardinality，可以构建均匀分布的直方图。 下面描述了统一直方图构造的公式。

Number of buckets = (Max-Min)/No of distinct values.

Bucket width = (Max- Min)/ Number of buckets.

Bucket Height = Table cardinality/ Number of buckets.

In this paper for the cost formula, in the absence of histogram, we will follow heuristics described by Hector Garcia Molina, Jeffrey D. Ullman and Jennifer Widom in the book “Database Systems”.

**Following are the cost variables that will be used in cost computation:**

- Hr - This is the cost of Reading 1 byte from HDFS in nano seconds.
- Hw - This is the cost of Writing 1 byte to HDFS in nano seconds.
- Lr - This is the cost of Reading 1 byte from Local FS in nano seconds.
- Lw - This is the cost of writing 1 byte to Local FS in nano seconds.
- NEt – This is the average cost of transferring 1 byte over network in the Hadoop cluster from any node to any node; expressed in nano seconds.
- T(R) - This is the number of tuples in the relation.
- Tsz – Average size of the tuple in the relation
- V(R, a) –The number of distinct values for attribute a in relation R
- CPUc – CPU cost for a comparison in nano seconds
 

**Assumptions:**

- Relative cost of Disk, HDFS, and Network read/write with each other is more important than the exact true cost.
- We assume uniform cost regardless of hardware type, locality, and size of data involved in I/O, type of I/O scatter/gather vs. sequential read/write. This is obviously over simplification, but - we are more interested in relative cost of operations.
- This cost model ignores the number of disk blocks that needs to be read/written from/to and instead look at number of bytes that needs to be read/written. This is an obvious oversimplification of I/O cost.
- This cost model also ignores storage layout, column store vs. others.
- We assume all of the tuples to be of uniform size.
- No colocation of tasks is assumed and hence we consider network transfer cost.
- For CPU cost, only comparison cost is considered. It is assumed that each comparison will cost 1 nano second.
- Each vertex in Tez is a different process
- HDFS read is assumed to be 1.5 times of local disk read and HDFS write is assumed to be 10 times of local disk write.
 

**Following are the assumed values for cost variables:**

- CPUc = 1 nano sec
- NEt = 150 * CPUc nano secs
- Lw = 4 * NEt
- Lr = 4 * NEt
- Hw = 10 * Lw
- Hr = 1.5 * Lr
 

**Table Scan**

- T(R) = Consult Metadata to get cardinality;

- Tsz = Consult Metadata to get average tuple size;

- V(R, a) = Consult Metadata

- CPU Usage = 0;

- IO Usage = Hr * T(R) * Tsz

**Common Join**

- T(R) = Join Cardinality estimation

- Tsz = Consult Metadata to get average tuple size based on join schema;

- CPU Usage = Sorting Cost for each of the relation + Merge Cost for sorted stream

 	= (T(R1) * log T(R1) * CPUc + T(R2) * log T(R2) * CPUc + … + T(Rm) * log T(Rm) * CPUc) + (T(R1) + T(R2) + …+ T(Rm)) * CPUc nano seconds;

- IO Usage = Cost of writing intermediate result set in to local FS for shuffling + Cost of reading from local FS for transferring to Join operator node + Cost of transferring mapped output to Join operator node

	= Lw * (T(R1) * Tsz1 + T(R2) * Tsz2 + …+ T(Rm) * Tszm)  + Lr * (T(R1) * Tsz1 + T(R2) * Tsz2 + …+ T(Rm) * Tszm) + NEt *  (T(R1) * Tsz1 + T(R2) * Tsz2 + … + T(Rm) * Tszm)

`R1, R2… Rm` is the relations involved in join.

`Tsz1, Tsz2… Tszm` are the average size of tuple in relations `R1, R2…Rm`.


**Map Join**

- Number of Rows = Join Cardinality estimation

- Size of tuple = Consult Metadata to get average tuple size based on join schema

- CPU Usage =HashTable Construction cost + Cost of Join

	= (T(R2) + …+ T(Rm)) + (T(R1) + T(R2) + …+ T(Rm)) * CPUc nano seconds

- IO Usage = Cost of transferring small tables to Join Operator Node * Parallelization of the join

	= NEt *  (T(R2) * Tsz2 + … + T(Rm) * Tszm) * number of mappers

`R1, R2… Rm` is the relations involved in join and R1 is the big table that will be streamed.

`Tsz2… Tszm` are the average size of tuple in relations `R1, R2…Rm`.

 

**Bucket Map Join**

- Number of Rows = Join Cardinality estimation

- Size of tuple = Consult Metadata to get average tuple size based on join schema;

- CPU Usage =Hash Table Construction cost + Cost of Join

	= (T(R2) + …+ T(Rm)) * CPUc + (T(R1) + T(R2) + …+ T(Rm)) * CPUc nano seconds

- IO Usage = Cost of transferring small tables to Join * Parallelization of the join

	= NEt *  (T(R2) * Tsz2 + … + T(Rm) * Tszm) * number of mappers

`R1, R2… Rm` is the relations involved in join.

`Tsz2… Tszm` are the average size of tuple in relations `R2…Rm`.


**SMB Join**

- Number of Rows = Join Cardinality estimation

- Size of tuple = Consult Metadata to get average tuple size based on join schema;

- CPU Usage = Cost of Join

	= (T(R1) + T(R2) + …+ T(Rm)) * CPUc nano seconds

- IO Usage = Cost of transferring small tables to Join * Parallelization of the join

	= NEt *  (T(R2) * Tsz2 + … + T(Rm) * Tszm) * number of mappers

`R1, R2… Rm` is the relations involved in join.

`Tsz2… Tszm` are the average size of tuple in relations `R2…Rm`.

 

**Skew Join**

Query will be rewritten as union of two joins. We will have a rule to rewrite query tree for skew join. Rewritten query will use the cost model for the join and union operators. 

**Distinct/Group By**

- Number of Rows = Based on Group-By selectivity = V(R, a,b,c..) where a,b,c are the group by keys

- Size of tuple = Consult Metadata to get average tuple size based on schema

- CPU Usage = Cost of Sorting + Cost of categorizing into group

	= (T(R) * log T(R) + T(R)) * CPUc nano seconds;

- IO Usage = Cost of writing intermediate result set in to local FS for shuffling + Cost of reading from local FS for transferring to GB reducer operator node + Cost of transferring data set to GB Node

	= Lw * T(R) * Tsz + Lr * T(R) * Tsz + NEt * T(R) * Tsz


**Union All**

- Number of Rows = Number of Rows from left + Number of Rows from right

- Size of tuple = avg of (avg size of left, avg size of right)

- CPU Usage = 0

- IO Usage = Cost of writing intermediate result set in to HDFS + Cost of reading from HDFS for transferring to UNION mapper node + Cost of transferring data set to Mapper Node

	= (T(R1) * Tsz1  + T(R2) * Tsz2) * Hw + (T(R1) * Tsz1 + T(R2) * Tsz2) * Hr + (T(R1) * Tsz1 + T(R2) * Tsz2) * NEt

`R1, R2` is the relations involved in join.

`Tsz1, Tsz2` is the average size of tuple in relations `R1, R2`.

**Filter/Having**

- Number of Rows = Filter Selectivity * Number of Rows from Child

- Size of tuple = size of tuple from child operator

- CPU Usage = T(R) * CPUc nano seconds

- IO Usage = 0

**Select**

- Number of Rows = Number of Rows from Child

- Size of tuple = size of tuple from child operator

- CPU Usage = 0

- IO Usage = 0

**Filter Selectivity**

Without Histogram

- Equality Predicates where one side is a literal = 1/V(R, A)
- Equality Predicate when both sides are not literals = 1/max (V(R, A), V(R, B))
- In Equality Predicates (Less/Greater than) = 1/3
- Not Equal = (V(R, A) -1)/V(R, A)
- OR Condition = n*(1 –(1-m1/n)(1-m2/n)) where n is the total number of tuples from child and m1 and m2 is the expected number of tuples from each part of the disjunction predicate.
- AND condition = product of selectivity of individual leaf predicates in the conjunctive predicate
 

**Join Cardinality (without Histogram)**

- Inner Join = Product of cardinalities of child relations * Join selectivity
- One side Outer Join = max (Selectivity of filter rejecting non joined inner tuples * (Product of cardinalities of child relations), Cardinality of Outer side)

Example: C(R1  a=b R2) = max (C(R2.b=R1.a C(R1 X R2)), C(R1))

- Full Outer Join = max (Selectivity of filter rejecting non joined inner tuples * (Product of cardinalities of child relations), sum of cardinalities of left and right relation)
= C(R1 a=b R2) = max (C(R1.a=R1.b C(R1 X R2)), C(R1) + C(R2))

- For multi-way join algorithm, join would be decomposed to number of different joins for cardinality and cost estimation.
 

Join Selectivity (without Histogram)

- Single Attribute  = 1/max (V(R1, a), V(R2, a))  where join predicate is R1.a=R2.a

- Multiple Attributes = 1/(max (V(R1, a), V(R2, a))   * max (V(R1, b), V(R2, b)) ) where join predicate is R1.a=R2.a and R1.b = R2.b
 

**Distinct Estimation**

Inner Join - Distinct Estimation

- V(J, a) = V(R1, a) if attribute a comes only from one side of join; J is the relation representing Join output and R1 one of the relation involved.
- V(J, a) = max(V(R1, a), V(R2, a)) if attribute a is present in both sides of join; J is the relation representing Join output and R1, R2 are the relations involved in join.
- V(J, a) = V(R1, a) if attribute a comes only from one side of join; J is the relation representing Join output output and R1 is the relation from which attribute “a” comes from.
- V(J, a) = V(Ro, a) where Ro is the relation for the outer side; J is the relation representing Join output and Ro is the outer relation of the join.
- V(J, a) = V(R1, a) if attribute “a” comes only from one side of join; J is the relation representing Join output and R1 is the relation from which attribute a comes from.
- V(J, a) = max (V(R1, a), V(R2, a)) where J is the relation representing Join output and R1, R2 are the relations involved in join.
- V(U, a) = max (V(R1, a), V(R2, a)) where U is the relation representing Union All output and R1, R2 are the relations involved in union.

**One sided Outer Join - Distinct Estimation**

**Full Outer Join - Distinct Estimation**

**Union All - Distinct Estimation**
 
**GB - Distinct Estimation**

- V(G, a) = V(R, a) where G is the relation representing Group-By output and R is the child relation of Group-By. It is assumed attribute “a” is part of grouping keys.

- V(G, a,b,c) = max (V(R,a), V(R,b), V(R,c)) where G is the relation representing Group-By output and R is the child relation of Group-By. It is assumed attribute “a”, “b”, “c” is part of grouping keys.
 
**Filter - Distinct Estimation**

- V(F, a) = V(R, a) where F is the relation representing filter output and R is the child relation of the filter.

## 5. Phase 1 – Work Items

- Walk the Hive OP tree and make sure that OP tree doesn’t contain any op that cannot be translated into Calcite (Lateral Views, PTF, Cubes & Rollups, Multi Table Insert).
- Walk the Hive OP tree and introduce cast functions to make sure that all of the comparisons (implicit & explicit) are strictly type safe (the type must be same on both sides).
- Implement Calcite operators specific to Hive that would do cost computation and cloning.
- Convert the Hive OP tree to Calcite OP tree.
	- Convert Hive Types to Calcite types
	- Convert Hive Expressions to Calcite expressions
	- Convert Hive Operator to Calcite operator
	- Handle Hidden Columns
	- Handle columns introduced by ReduceSink (for shuffling)
	- Handle Join condition expressions stashed in Reducesink op
	- Handle filters in Join Conditions
	- Convert Hive Semi Join to Calcite
	- Attach cost to operators
	- Alias the top-level query projections to query projections that user expects.
- Optimize the Calcite OP tree using Volcano Optimizer.
	- Implement Rules to convert Joins to Hive Join algorithms.
		- Common Join -> Map Join
		- Map Join -> Bucket Map Join
		- Common Join -> Bucket Map Join
		- Bucket Map Join ->  SMB Join
		- Common Join -> Skew Join
- Walk the Optimized Calcite OP tree and introduce derived tables to convert OP tree to SQL.
	- Generate unique table (including derived table) aliases
- Walk the OP tree and convert in to AST.
	- Stash Join algorithm in AST as query Hints
- Modify Plan Generator to pay attention to Calcite query hints
- Rerun Hive optimizer and generate the execution plan (this second pass would not invoke Calcite optimizer).

## Open Issues

CBO needs to differentiate between types of IPC (Durable-Local-FS vs, Durable-HDFS, Memory vs. Streaming) 

## Reference