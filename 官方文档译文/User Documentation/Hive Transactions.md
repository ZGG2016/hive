# Hive Transactions

[TOC]

> Hive 3 Warning. Any transactional tables created by a Hive version prior to Hive 3 require Major Compaction to be run on every partition before upgrading to 3.0.  More precisely, any partition which has had any update/delete/merge statements executed on it since the last Major Compaction, has to undergo another Major Compaction.  No more update/delete/merge may happen on this partition until after Hive is upgraded to Hive 3.

Hive 3 警告

在 Hive 3 之前的版本中，创建的任何事务表都需要，在升级到 3.0 之前，在每个分区上运行 Major Compaction。

更准确地说，自上次 Major Compaction 以来在其上执行过 update/delete/merge 语句的任何分区，都必须进行另一次 Major Compaction。

在 Hive 升级到 Hive 3 之前，这个分区不会再发生 update/delete/merge。

## 1、ACID and Transactions in Hive

### 1.1、What is ACID and why should you use it?

> ACID stands for four traits of database transactions:  Atomicity (an operation either succeeds completely or fails, it does not leave partial data), Consistency (once an application performs an operation the results of that operation are visible to it in every subsequent operation), [Isolation](https://en.wikipedia.org/wiki/Isolation_(database_systems)) (an incomplete operation by one user does not cause unexpected side effects for other users), and Durability (once an operation is complete it will be preserved even in the face of machine or system failure).  These traits have long been expected of database systems as part of their transaction functionality.  

ACID 表示数据库事务的四个特征：

- 原子性(操作要么完全成功要么失败，它没有留下部分数据)，

- 一致性(一旦应用程序执行一个操作，该操作的结果对每个后续的操作是可见的)，

- 隔离性(由一个用户的一个不完整的操作不会为其他用户引起意想不到的副作用)，

- 持久性(一旦一个操作完成后它将被保留下来，即使机器或系统故障)。

长期以来，人们一直期望数据库系统具有这些特性，并将其作为事务功能的一部分。

> Up until Hive 0.13, atomicity, consistency, and durability were provided at the partition level.  Isolation could be provided by turning on one of the available locking mechanisms ([ZooKeeper](http://zookeeper.apache.org/) or in memory).  With the addition of transactions in [Hive 0.13](https://issues.apache.org/jira/browse/HIVE-5317) it is now possible to provide full ACID semantics at the row level, so that one application can add rows while another reads from the same partition without interfering with each other.

直到 Hive 0.13，**原子性、一致性和持久性都是在分区级别上提供的**。

**隔离性可以通过打开其中一个可用的锁机制(ZooKeeper或内存)来提供**。

随着 Hive 0.13 中事务的增加，现在**可以在行级别提供完整的 ACID 语义**，这样一个应用程序可以添加行，而另一个应用程序可以从同一个分区读取，而不会相互干扰。

> Transactions with ACID semantics have been added to Hive to address the following use cases:

Hive 中添加了带有 ACID 语义的事务，以解决以下问题：

> 1.Streaming ingest of data.  Many users have tools such as [Apache Flume](http://flume.apache.org/), [Apache Storm](https://storm.incubator.apache.org/), or [Apache Kafka](http://kafka.apache.org/) that they use to stream data into their Hadoop cluster.  While these tools can write data at rates of hundreds or more rows per second, Hive can only add partitions every fifteen minutes to an hour.  Adding partitions more often leads quickly to an overwhelming number of partitions in the table.  These tools could stream data into existing partitions, but this would cause readers to get dirty reads (that is, they would see data written after they had started their queries) and leave many small files in their directories that would put pressure on the NameNode.  With this new functionality this use case will be supported while allowing readers to get a consistent view of the data and avoiding too many files.

1.数据的流摄取。

许多用户使用 Apache Flume、Apache Storm 或 Apache Kafka 等工具将数据流入 Hadoop 集群。

这些工具可以以每秒数百行或更快的速度写入数据，而 Hive 只能每 15 分钟到 1 小时增加一个分区。更频繁地添加分区会很快导致表中出现大量分区。

这些工具可以将数据流入现有的分区中，但这会导致读取器读取脏数据(也就是说，他们会在开始查询之后看到写入的数据)，并在他们的目录中留下许多小文件，这会给 NameNode 带来压力。

有了这个新功能，将支持这个用例，同时允许读者获得一致的数据视图，并避免过多的文件。

> 2.Slow changing dimensions.  In a typical star schema data warehouse, dimensions tables change slowly over time.  For example, a retailer will open new stores, which need to be added to the stores table, or an existing store may change its square footage or some other tracked characteristic.  These changes lead to inserts of individual records or updates of records (depending on the strategy chosen).  Starting with 0.14, Hive is able to support this.

2.缓慢变化维度。

在典型的星型模式数据仓库中，维度表会随着时间缓慢更改。

例如，零售商将开设新的商店，这些商店需要添加到 stores 表中，或者现有的商店可能会更改其面积或其他一些可跟踪的特征。这些更改将导致插入单个记录或更新记录(取决于所选择的策略)。从 0.14 开始，Hive 就能够支持这个功能。

> 3.Data restatement.  Sometimes collected data is found to be incorrect and needs correction.  Or the first instance of the data may be an approximation (90% of servers reporting) with the full data provided later.  Or business rules may require that certain transactions be restated due to subsequent transactions (e.g., after making a purchase a customer may purchase a membership and thus be entitled to discount prices, including on the previous purchase).  Or a user may be contractually required to remove their customer’s data upon termination of their relationship.  Starting with Hive 0.14 these use cases can be supported via [INSERT](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML), [UPDATE](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-Update), and [DELETE](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-Delete).

3.数据重述。

有时，发现收集的数据是不正确的，需要纠正。或者数据的第一个实例可能是一个近似值(90%的服务器报告)，稍后提供完整的数据。

或者业务规则可能要求由于后续事务而重新申明某些事务(例如，在购买之后，客户可能会购买会员资格，因此有权享受折扣价格，包括之前的购买)。

或者，根据合同，用户可能被要求在客户关系终止时删除他们的数据。

从 Hive 0.14 开始，这些用例可以通过 INSERT、UPDATE 和 DELETE 支持。

> 4.Bulk updates using [SQL MERGE](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-Merge) statement.

4.使用 SQL MERGE 语句进行批量更新。

### 1.2、Limitations

> BEGIN, COMMIT, and ROLLBACK are not yet supported.  All language operations are auto-commit.  The plan is to support these in a future release.

还不支持 BEGIN、COMMIT 和 ROLLBACK。所有语言操作都是自动提交的。

我们计划在未来的版本中支持这些功能。

> Only [ORC file format](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+ORC) is supported in this first release.  The feature has been built such that transactions can be used by any storage format that can determine how updates or deletes apply to base records (basically, that has an explicit or implicit row id), but so far the integration work has only been done for ORC.

第一个版本只支持 ORC 文件格式。

已经构建了这样的特性，即任何可以确定如何将更新或删除应用于基本记录(基本上，具有显式或隐式行id)的存储格式都可以使用事务，但到目前为止，集成工作仅针对 ORC 完成。

> By default transactions are configured to be off.  See the [Configuration](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions#HiveTransactions-Configuration) section below for a discussion of which values need to be set to configure it.

默认情况下，事务配置为关闭的。请参阅下面的 Configuration 部分，了解需要设置哪些值来配置它。

> Tables must be [bucketed](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL+BucketedTables) to make use of these features.  Tables in the same system not using transactions and ACID do not need to be bucketed. External tables cannot be made ACID tables since the changes on external tables are beyond the control of the compactor ([HIVE-13175](https://issues.apache.org/jira/browse/HIVE-13175)).

为了使用这些特性，表必须被分桶。

同一个系统中，不使用事务和 ACID 的表不需要进行分桶。不能将外部表创建为 ACID 表，因为外部表上的更改超出了 compactor 的控制范围。

> Reading/writing to an ACID table from a non-ACID session is not allowed. In other words, the Hive transaction manager must be set to org.apache.hadoop.hive.ql.lockmgr.DbTxnManager in order to work with ACID tables.

不允许从非 ACID 会话中读写 ACID 表。

换句话说，Hive 事务管理器必须设置为 `org.apache.hadoop.hive.ql.lockmgr.DbTxnManager` 来处理 ACID 表。

> At this time only snapshot level isolation is supported.  When a given query starts it will be provided with a consistent snapshot of the data.  There is no support for dirty read, read committed, repeatable read, or serializable.  With the introduction of BEGIN the intention is to support snapshot isolation for the duration of transaction rather than just a single query.  Other isolation levels may be added depending on user requests.

目前只支持快照级别隔离。

当开始一个给定的查询时，将为它提供一个数据的一致性快照。不支持脏读、读提交、可重复读或可序列化。引入 BEGIN 的目的是支持事务期间的快照隔离，而不仅仅是单个查询。可以根据用户请求添加其他隔离级别。

> The existing ZooKeeper and in-memory lock managers are not compatible with transactions.  There is no intention to address this issue.  See [Basic Design](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions#HiveTransactions-BasicDesign) below for a discussion of how locks are stored for transactions.

现有的 ZooKeeper 和内存锁管理器与事务不兼容。我们无意解决这个问题。有关如何为事务存储锁的讨论，请参阅下面的 Basic Design。

> ~~Schema changes using ALTER TABLE is NOT supported for ACID tables. [HIVE-11421](https://issues.apache.org/jira/browse/HIVE-11421) is tracking it.~~  Fixed in 1.3.0/2.0.0.

ACID 表不支持使用 ALTER TABLE 更改模式。HIVE-11421 正在追踪它。在 1.3.0/2.0.0 固定。

> Using Oracle as the Metastore DB and "datanucleus.connectionPoolingType=BONECP" may generate intermittent "No such lock.." and "No such transaction..." errors.  Setting "datanucleus.connectionPoolingType=DBCP" is recommended in this case. 

使用 Oracle 作为 Metastore DB，`datanucleus.connectionPoolingType=BONECP`可能会产生间歇性的 "No such lock.." 和 "No such transaction..." 错误。

在这种情况下，建议使用 "datanucleus.connectionPoolingType=DBCP"。

> [LOAD DATA...](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-Loadingfilesintotables) statement is not supported with transactional tables.  (This was not properly enforced until [HIVE-16732](https://issues.apache.org/jira/browse/HIVE-16732))

事务表不支持 `LOAD DATA...` 语句。(这直到 HIVE-16732 才被严格执行)

### 1.3、Streaming APIs

> Hive offers APIs for streaming data ingest and streaming mutation:

Hive 提供了流数据摄取和流变化的 APIs:

- [Hive HCatalog Streaming API](https://cwiki.apache.org/confluence/display/Hive/Streaming+Data+Ingest)
- [Hive Streaming API](https://cwiki.apache.org/confluence/display/Hive/Streaming+Data+Ingest+V2) (Since Hive 3)
- [HCatalog Streaming Mutation API](https://cwiki.apache.org/confluence/display/Hive/HCatalog+Streaming+Mutation+API) (available in Hive 2.0.0 and later)

> A comparison of these two APIs is available in the [Background](https://cwiki.apache.org/confluence/display/Hive/HCatalog+Streaming+Mutation+API#HCatalogStreamingMutationAPI-Background) section of the Streaming Mutation document.

流变化文档的 Background 部分可以对这两种 APIs 进行比较。

### 1.4、Grammar Changes

> INSERT...VALUES, UPDATE, and DELETE have been added to the SQL grammar, starting in [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-5317).  See [LanguageManual DML](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML) for details.

从 Hive 0.14 开始，SQL 语法中已经添加了 `INSERT...VALUES`、`UPDATE` 和 `DELETE`。详细信息请参见 LanguageManual DML。

> Several new commands have been added to Hive's DDL in support of ACID and transactions, plus some existing DDL has been modified.  

为了支持 ACID 和事务，Hive 的 DDL 中增加了几个新的命令，并且修改了一些现有的 DDL。

> A new command SHOW TRANSACTIONS has been added, see [Show Transactions](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-ShowTransactions) for details.

添加了一个新的命令 `SHOW TRANSACTIONS`，有关详细信息，请参阅 SHOW TRANSACTIONS。

> A new command SHOW COMPACTIONS has been added, see [Show Compactions](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-ShowCompactions) for details.

添加了一个新的命令 `SHOW COMPACTIONS`，有关详细信息，请参阅 SHOW COMPACTIONS。

> The SHOW LOCKS command has been altered to provide information about the new locks associated with transactions.  If you are using the ZooKeeper or in-memory lock managers you will notice no difference in the output of this command.  See [Show Locks](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-ShowLocks) for details.

`SHOW LOCKS` 命令已被修改，以提供有关与事务关联的新锁的信息。

如果你使用的是 ZooKeeper 或内存锁管理器，则不会注意到该命令的输出结果有什么不同。详情请参见 Show Locks。

> A new option has been added to ALTER TABLE to request a compaction of a table or partition.  In general users do not need to request compactions, as the system will detect the need for them and initiate the compaction.  However, if [compaction is turned off](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions#HiveTransactions-TableProperties) for a table or a user wants to compact the table at a time the system would not choose to, ALTER TABLE can be used to initiate the compaction.  See [Alter Table/Partition Compact](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AlterTable/PartitionCompact) for details.  This will enqueue a request for compaction and return.  To watch the progress of the compaction the user can use SHOW COMPACTIONS.

向 ALTER TABLE 添加了一个新选项，以请求表或分区的 compaction。

一般来说，用户不需要请求 compaction，因为系统会检测到他们的需求并启动 compaction。

但是，如果对表关闭了 compaction，或者用户希望在系统不选择的时候 compact 表，那么可以使用 ALTER table 来初始化 compaction。详见 Alter Table/Partition Compact。

这将使 compaction 请求进入队列并返回。要查看 compaction 的进度，用户可以使用 SHOW compact。

> A new command ABORT TRANSACTIONS has been added, see [Abort Transactions](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AbortTransactions) for details.

添加了一个新的命令 `ABORT TRANSACTIONS`，有关详细信息，请参阅 ABORT TRANSACTIONS。

### 1.5、Basic Design

> HDFS does not support in-place changes to files.  It also does not offer read consistency in the face of writers appending to files being read by a user.  In order to provide these features on top of HDFS we have followed the standard approach used in other data warehousing tools.  Data for the table or partition is stored in a set of base files.  New records, updates, and deletes are stored in delta files.  A new set of delta files is created for each transaction (or in the case of streaming agents such as Flume or Storm, each batch of transactions) that alters a table or partition.  At read time the reader merges the base and delta files, applying any updates and deletes as it reads.

HDFS 不支持对文件的就地更改。它也不提供在写入器追加用户正在读取的文件时的读取一致性。

为了在 HDFS 之上提供这些特性，我们遵循了其他数据仓库工具中使用的标准方法。

表或分区的数据存储在一组基本文件中。新记录、更新和删除存储在增量文件中。

每个修改表或分区的事务(或者在流式代理，如Flume或Storm，的情况下，每批事务)都会创建一组新的增量文件。

读取器在读取时合并基本文件和增量文件，并在读取时应用任何更新和删除。

#### 1.5.1、Base and Delta Directories

> Previously all files for a partition (or a table if the table is not partitioned) lived in a single directory.  With these changes, any partitions (or tables) written with an ACID aware writer will have a directory for the base files and a directory for each set of delta files.  Here is what this may look like for an unpartitioned table "t":

以前，一个分区(或者一个表，如果该表没有分区)的所有文件都位于一个目录中。

通过这些更改，使用支持 ACID 的写入器编写的任何分区(或表)都将拥有一个用于基本文件的目录和一个用于每个增量文件集的目录。

对于未分区表 t，可能是这样的：

> Filesystem Layout for Table "t"

```sh
hive> dfs -ls -R /user/hive/warehouse/t;
drwxr-xr-x   - ekoifman staff          0 2016-06-09 17:03 /user/hive/warehouse/t/base_0000022
-rw-r--r--   1 ekoifman staff        602 2016-06-09 17:03 /user/hive/warehouse/t/base_0000022/bucket_00000
drwxr-xr-x   - ekoifman staff          0 2016-06-09 17:06 /user/hive/warehouse/t/delta_0000023_0000023_0000
-rw-r--r--   1 ekoifman staff        611 2016-06-09 17:06 /user/hive/warehouse/t/delta_0000023_0000023_0000/bucket_00000
drwxr-xr-x   - ekoifman staff          0 2016-06-09 17:07 /user/hive/warehouse/t/delta_0000024_0000024_0000
-rw-r--r--   1 ekoifman staff        610 2016-06-09 17:07 /user/hive/warehouse/t/delta_0000024_0000024_0000/bucket_00000
```

#### 1.5.2、Compactor

> Compactor is a set of background processes running inside the Metastore to support ACID system.  It consists of Initiator, Worker, Cleaner, AcidHouseKeeperService and a few others.

Compactor 是一组运行在 Metastore 中的后台进程，用于支持 ACID 系统。

它由 Initiator、Worker、Cleaner、AcidHouseKeeperService 和其他一些组件组成。

##### 1.5.2.1、Delta File Compaction

> As operations modify the table more and more delta files are created and need to be compacted to maintain adequate performance.  There are two types of compactions, minor and major.

对表的修改越多，创建的增量文件越多，需要 compacted 文件以保持足够的性能。

有两种 compactions，minor 和 major。

> Minor compaction takes a set of existing delta files and rewrites them to a single delta file per bucket.

- Minor compaction 接收一组现有的增量文件，并对每个桶，将其重写为单个增量文件。

> Major compaction takes one or more delta files and the base file for the bucket and rewrites them into a new base file per bucket.  Major compaction is more expensive but is more effective.

- Major compaction 接收一个或多个增量文件和桶的基本文件，并对每个桶，将它们重写为新的基本文件。Major compaction 更昂贵，但更有效。

> All compactions are done in the background and do not prevent concurrent reads and writes of the data.  After a compaction the system waits until all readers of the old files have finished and then removes the old files.

所有的 compactions 都是在后台完成的，不会阻止并发的数据读写。compaction 之后，系统会等待，直到旧文件的所有读取器都完成，然后删除旧文件。

##### 1.5.2.2、Initiator

> This module is responsible for discovering which tables or partitions are due for compaction.  This should be enabled in a Metastore using [hive.compactor.initiator.on](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.compactor.initiator.on).  There are several properties of the form `*.threshold` in "New Configuration Parameters for Transactions" table below that control when a compaction task is created and which type of compaction is performed.  Each compaction task handles 1 partition (or whole table if the table is unpartitioned).  If the number of consecutive compaction failures for a given partition exceeds hive.compactor.initiator.failed.compacts.threshold, automatic compaction scheduling will stop for this partition.  See Configuration Parameters table for more info.

这个模块负责发现哪些表或分区需要 compaction。

这应该在 Metastore 中使用 `hive.compactor.initiator.on` 启用。

在下面的 "New Configuration Parameters for Transactions" 表中，有几个 `*.threshold` 形式的属性，控制着何时创建 compaction 任务，以及执行哪种类型的 compaction。

每个 compaction 任务处理 1 个分区(如果表未分区，则处理整个表)。

如果给定分区的连续 compaction 失败次数超过 `hive.compactor.initiator.failed.compacts.threshold`，则该分区的自动 compaction 调度将停止。

##### 1.5.2.3、Worker

> Each Worker handles a single compaction task.  A compaction is a MapReduce job with name in the following form: <hostname>-compactor-<db>.<table>.<partition>.  Each worker submits the job to the cluster (via [hive.compactor.job.queue](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.compactor.job.queue) if defined) and waits for the job to finish.  [hive.compactor.worker.threads](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.compactor.worker.threads) determines the number of Workers in each Metastore.  The total number of Workers in the Hive Warehouse determines the maximum number of concurrent compactions.

每个 Worker 处理一个 compaction 任务。compaction 是一种 MapReduce job，其名称形式为 `<hostname>-compactor-<db>.<table>.<partition>`。

每个 worker 向集群提交 job(如果定义了，则通过 `hive.compactor.job.queue`)，并等待 job 完成。

`hive.compactor.worker.threads` 确定每个 Metastore 中的 Workers 数量。Hive 仓库中的 worker 总数决定了并发 compactions 的最大数量。

##### 1.5.2.4、Cleaner

> This process is a process that deletes delta files after compaction and after it determines that they are no longer needed.

该进程在 compaction 后删除增量文件，并在确定不再需要它们之后删除增量文件。

##### 1.5.2.5、AcidHouseKeeperService

> This process looks for transactions that have not heartbeated in [hive.txn.timeout](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.txn.timeout) time and aborts them.  The system assumes that a client that initiated a transaction stopped heartbeating crashed and the resources it locked should be released.

这个进程查找在 `hive.txn.timeout` 内没有接收到心跳的事务，并中止它们。

系统假设初始化事务的客户端停止心跳崩溃，它锁定的资源应该被释放。

##### 1.5.2.6、SHOW COMPACTIONS

> This commands displays information about currently running compaction and recent history (configurable retention period) of compactions.  This history display is available since [HIVE-12353](https://issues.apache.org/jira/browse/HIVE-12353).

这个命令显示关于当前运行的 compaction（可配置的保留期）和 compaction 的最近历史记录的信息。此历史记录显示从 HIVE-12353 开始可用。

> Also see [LanguageManual DDL#ShowCompactions](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-ShowCompactions) for more information on the output of this command and [NewConfigurationParametersforTransactions](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions#HiveTransactions-NewConfigurationParametersforTransactions)/Compaction History for configuration properties affecting the output of this command.  The system retains the last N entries of each type: failed, succeeded, attempted (where N is configurable for each type).

另外，请参阅 LanguageManual DDL#ShowCompactions 以获得关于该命令输出的更多信息，以及 NewConfigurationParametersforTransactions/Compaction History，以了解影响该命令输出的配置属性。

系统保留每种类型的最后 N 个表项：failed、succeeded、attempt(N对每种类型来说是可配置的)。

#### 1.5.3、Transaction/Lock Manager

> A new logical entity called "transaction manager"  was added which incorporated previous notion of "database/table/partition lock manager" (hive.lock.manager with default of org.apache.hadoop.hive.ql.lockmgr.zookeeper.ZooKeeperHiveLockManager). The transaction manager is now additionally responsible for managing of transactions locks. The default DummyTxnManager emulates behavior of old Hive versions: has no transactions and uses hive.lock.manager property to create lock manager for tables, partitions and databases. A newly added DbTxnManager manages all locks/transactions in Hive metastore with DbLockManager (transactions and locks are durable in the face of server failure). This means that previous behavior of locking in ZooKeeper is not present anymore when transactions are enabled. To avoid clients dying and leaving transaction or locks dangling, a heartbeat is sent from lock holders and transaction initiators to the metastore on a regular basis.  If a heartbeat is not received in the configured amount of time, the lock or transaction will be aborted.

添加了一个新的逻辑实体 "transaction manager"，它包含了以前“数据库/表/分区锁管理器”的概念(`hive.lock.manager`默认为`org.apache.hadoop.hive.ql.lockmgr.zookeeper.ZooKeeperHiveLockManager`)。

事务管理器现在还负责管理事务锁。默认的 DummyTxnManager 模拟旧 Hive 版本的行为：没有事务，使用 `hive.lock.manager` 属性为表、分区和数据库创建锁管理器。

新增加的 DbTxnManager 通过 DbLockManager 管理 Hive metastore 中的所有锁/事务(事务和锁在服务器故障时是持久的)。这意味着当启用事务时，ZooKeeper 中先前的锁行为不再存在。

为了避免客户端死亡并留下事务或锁悬空，会定期从锁持有者和事务启动器向 metastore 发送心跳。如果在配置的时间内没有接收到心跳，锁或事务将被中止。

> As of [Hive 1.3.0](https://issues.apache.org/jira/browse/HIVE-12529), the length of time that the DbLockManger will continue to try to acquire locks can be controlled via [hive.lock.numretires](http://configuration%20properties/#hive.lock.numretires) and [hive.lock.sleep.between.retries](http://configuration%20properties/#hive.lock.sleep.between.retries).  When the DbLockManager cannot acquire a lock (due to existence of a competing lock), it will back off and try again after a certain time period.  In order to support short running queries and not overwhelm the metastore at the same time, the DbLockManager will double the wait time after each retry.  The initial back off time is 100ms and is capped by hive.lock.sleep.between.retries.  hive.lock.numretries is the total number of times it will retry a given lock request.  Thus the total time that the call to acquire locks will block (given values of 100 retries and 60s sleep time) is (100ms + 200ms + 400ms + ... + 51200ms + 60s + 60s + ... + 60s) = 91m:42s:300ms.

从 Hive 1.3.0 开始，DbLockManger 继续尝试获取锁的时间长度可以通过 `hive.lock.numretires` 和 `hive.lock.sleep.between.retries` 来控制。

当 DbLockManager 无法获得一个锁(由于存在一个竞争锁)时，它将后退，并在一段时间后再次尝试。

为了支持短时间运行的查询，同时不会淹没 metastore, DbLockManager 会在每次重试后将等待时间加倍。

初始回退时间是 100ms，并由 `hive.lock.sleep.between.retries` 来限制。

`hive.lock.numretries` 是重试给定锁请求的总次数。因此，获取锁的调用被阻塞的总时间(给定100次重试和60秒睡眠时间)是(100ms + 200ms + 400ms +…+ 5120ms + 60s + 60s +…+ 60s) = 91m:42s:300ms。

> More [details](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-ShowLocks) on locks used by this Lock Manager.

关于此 Lock Manager 使用的锁的更多细节。

> Note that the lock manager used by DbTxnManager will acquire locks on all tables, even those without "transactional=true" property.  By default, Insert operation into a non-transactional table will acquire an exclusive lock and thus block other inserts and reads.  While technically correct, this is a departure from how Hive traditionally worked (i.e. w/o a lock manger).  For backwards compatibility, [hive.txn.strict.locking.mode](http://configuration%20properties/#hive.txn.strict.locking.mode) (see table below) is provided which will make this lock manager acquire shared locks on insert operations on non-transactional tables.  This restores previous semantics while still providing the benefit of a lock manager such as preventing table drop while it is being read.  Note that for transactional tables, insert always acquires share locks since these tables implement MVCC architecture at the storage layer and are able to provide strong read consistency (Snapshot Isolation) even in presence of concurrent modification operations.

注意，DbTxnManager 使用的锁管理器将获取所有表上的锁，即使是那些没有 “transactional=true” 属性的表。

默认情况下，插入到非事务性表中的操作将获得一个排他锁，从而阻塞其他插入和读取操作。虽然技术上是正确的，但这背离了 Hive 传统的工作方式(即不使用锁槽)。

为了向后兼容，请使用`hive.txn.strict.locking.mode`(见下表)将使这个锁管理器在非事务性表的插入操作上获得共享锁。这恢复了以前的语义，同时仍然提供了锁管理器的好处，比如在读取表时防止表被删除。

请注意，对于事务性表，插入总是会获得共享锁，因为这些表在存储层实现了 MVCC 架构，并且能够提供强读一致性(快照隔离)，即使存在并发的修改操作。

### 1.6、Configuration

> Minimally, these configuration parameters must be set appropriately to turn on transaction support in Hive:

最低限度地，这些配置参数必须适当地设置以打开 Hive 中的事务支持：

> Client Side

客户端

- [hive.support.concurrency](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.support.concurrency) – true
- [hive.enforce.bucketing](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.enforce.bucketing) – true (Not required as of [Hive 2.0](https://issues.apache.org/jira/browse/HIVE-12331))
- [hive.exec.dynamic.partition.mode](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.exec.dynamic.partition.mode) – nonstrict
- [hive.txn.manager](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.txn.manager) – org.apache.hadoop.hive.ql.lockmgr.DbTxnManager

> Server Side (Metastore)

服务端(Metastore)

- [hive.compactor.initiator.on](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.compactor.initiator.on) – true (See table below for more details)
- [hive.compactor.worker.threads](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.compactor.worker.threads) – a positive number on at least one instance of the Thrift metastore service

> The following sections list all of the configuration parameters that affect Hive transactions and compaction.  Also see [Limitations](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions#HiveTransactions-Limitations) above and [Table Properties](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions#HiveTransactions-TableProperties) below.

以下部分列出了影响 Hive 事务和 compaction 的所有配置参数。还请参阅上面的 Limitations 和下面的 Table Properties。

#### 1.6.1、New Configuration Parameters for Transactions

> A number of new configuration parameters have been added to the system to support transactions.

系统中添加了许多新的配置参数来支持事务。

配置见原文：[https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions#HiveTransactions-NewConfigurationParametersforTransactions](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions#HiveTransactions-NewConfigurationParametersforTransactions)

#### 1.6.2、Configuration Values to Set for INSERT, UPDATE, DELETE

> In addition to the new parameters listed above, some existing parameters need to be set to support INSERT ... VALUES, UPDATE, and DELETE.

Configuration Key                  | Must be set to
---|:---
[hive.support.concurrency](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.support.concurrency)	       | true (default is false)
[hive.enforce.bucketing](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.enforce.bucketing)	           | true (default is false) (Not required as of [Hive 2.0](https://issues.apache.org/jira/browse/HIVE-12331))
[hive.exec.dynamic.partition.mode](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.exec.dynamic.partition.mode)   | nonstrict (default is strict)

#### 1.6.3、Configuration Values to Set for Compaction

> If the data in your system is not owned by the Hive user (i.e., the user that the Hive metastore runs as), then Hive will need permission to run as the user who owns the data in order to perform compactions.  If you have already set up HiveServer2 to impersonate users, then the only additional work to do is assure that Hive has the right to impersonate users from the host running the Hive metastore.  This is done by adding the hostname to `hadoop.proxyuser.hive.hosts` in Hadoop's core-site.xml file.  If you have not already done this, then you will need to configure Hive to act as a proxy user.  This requires you to set up keytabs for the user running the Hive metastore and add `hadoop.proxyuser.hive.hosts` and `hadoop.proxyuser.hive.groups` to Hadoop's core-site.xml file.  See the Hadoop documentation on secure mode for your version of Hadoop (e.g., for Hadoop 2.5.1 it is at [Hadoop in Secure Mode](http://hadoop.apache.org/docs/r2.5.1/hadoop-project-dist/hadoop-common/SecureMode.html)).

如果你的系统中的数据不属于 Hive 用户(即运行 Hive metastore 的用户)，那么 Hive 将需要权限来作为拥有数据的用户运行，以便执行 compactions。

如果你已经设置了 HiveServer2 来模拟用户，那么唯一需要做的就是确保 Hive 有权限模拟运行 Hive metastore 的主机上的用户。

这是通过向 hadoop 的 core-site.xml 文件中的 `hadoop.proxyuser.hive.hosts` 添加主机名来实现的。

如果你还没有这样做，那么你需要将 Hive 配置为代理用户。这需要你为运行 Hive metastore 的用户设置 keytab，并添加 `hadoop.proxyuser.hive.hosts` 和 `hadoop.proxyuser.hive.groups` 到 Hadoop 的 core-site.xml 文件。

### 1.7、Table Properties

> If a table is to be used in ACID writes (insert, update, delete) then the table property `"transactional=true"` must be set on that table, starting with [Hive 0.14.0](https://issues.apache.org/jira/browse/HIVE-8290). Note, once a table has been defined as an ACID table via `TBLPROPERTIES ("transactional"="true")`, it cannot be converted back to a non-ACID table, i.e., changing `TBLPROPERTIES ("transactional"="false")` is not allowed. Also, [hive.txn.manager](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.txn.manager) must be set to `org.apache.hadoop.hive.ql.lockmgr.DbTxnManager` either in hive-site.xml or in the beginning of the session before any query is run. Without those, inserts will be done in the old style; updates and deletes will be prohibited prior to HIVE-11716.  Since HIVE-11716 operations on ACID tables without DbTxnManager are not allowed.  However, this does not apply to Hive 0.13.0.

如果一个表要用于 ACID 写操作(insert、update、delete)，那么必须在该表上设置 “transactional=true” 属性，从 Hive 0.14.0 开始。

注意，一旦表通过 `TBLPROPERTIES ("transactional"="true")` 定义为 ACID 表，它就不能被转换回非 ACID 表，也就是说，更改 `TBLPROPERTIES ("transactional"="false")` 是不允许的。

同时，`hive.txn。manager` 必须设置为 `org.apache.hadoop.hive.ql.lockmgr.DbTxnManager`，可以是在 hive-site.xml 中，也可以是在任何查询运行之前的会话开始时。

没有这些，插入将以旧的方式进行；更新和删除将被禁止，在 HIVE-11716 之前。因为 HIVE-11716 在没有 DbTxnManager 的情况下是不允许对 ACID 表进行操作的。然而，这并不适用于 Hive 0.13.0。

> If a table owner does not wish the system to automatically determine when to compact, then the table property "NO_AUTO_COMPACTION" can be set.  This will prevent all automatic compactions.  Manual compactions can still be done with [Alter Table/Partition Compact](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AlterTable/PartitionCompact) statements.

如果表所有者不希望系统自动决定何时进行 compact，那么可以设置表属性 “NO_AUTO_COMPACTION”。

这将阻止所有的自动 compactions。手动 compactions 仍然可以使用 Alter Table/Partition Compact 语句。

> Table properties are set with the TBLPROPERTIES clause when a table is created or altered, as described in the [Create Table](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTable) and [Alter Table](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AlterTableProperties) Properties sections of Hive Data Definition Language. The "transactional" and "NO_AUTO_COMPACTION" table properties are case-sensitive in Hive releases 0.x and 1.0, but they are case-insensitive starting with release 1.1.0 [(]HIVE-8308](https://issues.apache.org/jira/browse/HIVE-8308)).

在创建或修改表时，通过 TBLPROPERTIES 子句设置表属性，如 Hive 数据定义语言的 Create Table 和 Alter Table properties 章节所述。

在 Hive 版本 0.x 和 1.0 中，“transactional” 和 “NO_AUTO_COMPACTION” 表属性是区分大小写的。但从版本 1.1.0 开始，它们是不区分大小写的。

> More compaction related options can be set via TBLPROPERTIES as of [Hive 1.3.0 and 2.1.0](https://issues.apache.org/jira/browse/HIVE-13354). They can be set at both table-level via [CREATE TABLE](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-Create/Drop/TruncateTable), and on request-level via [ALTER TABLE/PARTITION COMPACT](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AlterTable/PartitionCompact).  These are used to override the Warehouse/table wide settings.  For example, to override an MR property to affect a compaction job, one can add "compactor.<mr property name>=<value>" in either CREATE TABLE statement or when launching a compaction explicitly via ALTER TABLE.  The "<mr property name>=<value>" will be set on JobConf of the compaction MR job.   Similarly, "tblprops.<prop name>=<value>" can be used to set/override any table property which is interpreted by the code running on the cluster.  Finally, "compactorthreshold.<prop name>=<value>" can be used to override properties from the "New Configuration Parameters for Transactions" table above that end with ".threshold" and control when compactions are triggered by the system.  Examples:

从 Hive 1.3.0 和 2.1.0 开始，更多的 compaction 相关选项可以通过 TBLPROPERTIES 设置。

它们可以通过 CREATE TABLE 在表级别设置，也可以通过 ALTER TABLE/PARTITION COMPACT 在请求级设置。这些用于覆盖仓库/表宽设置。

例如，要覆盖 MR 属性以影响 compaction job，可以添加 `compactor.<mr property name>=<value>` 属性，可以在 CREATE TABLE 语句中或通过 ALTER TABLE 显式启动 compaction 时。

`<mr property name>=<value>` 将在 compaction MR job 的 JobConf 中设置。同样，`tblprops.<prop name>=<value>` 可以用来设置/覆盖任何由运行在集群上的代码解释的表属性。

最后，当系统触发 compactions 时，`compactorthreshold.<prop name>=<value>` 可以用来覆盖 "New Configuration Parameters for transaction" 表中以 ".threshold" 结尾的属性，并控制。

例子:

> Example: Set compaction options in TBLPROPERTIES at table level

在表级别，在 TBLPROPERTIES 中设置 compaction 选项。

```sql
CREATE TABLE table_name (
  id                int,
  name              string
)
CLUSTERED BY (id) INTO 2 BUCKETS STORED AS ORC
TBLPROPERTIES ("transactional"="true",
  "compactor.mapreduce.map.memory.mb"="2048",     -- specify compaction map job properties
  "compactorthreshold.hive.compactor.delta.num.threshold"="4",  -- trigger minor compaction if there are more than 4 delta directories
  "compactorthreshold.hive.compactor.delta.pct.threshold"="0.5" -- trigger major compaction if the ratio of size of delta files to
                                                                   -- size of base files is greater than 50%
);
```

> Example: Set compaction options in TBLPROPERTIES at request level

在请求级别，在 TBLPROPERTIES 中设置 compaction 选项。

```sql
ALTER TABLE table_name COMPACT 'minor' 
   WITH OVERWRITE TBLPROPERTIES ("compactor.mapreduce.map.memory.mb"="3072");  -- specify compaction map job properties
ALTER TABLE table_name COMPACT 'major'
   WITH OVERWRITE TBLPROPERTIES ("tblprops.orc.compress.size"="8192");         -- change any other Hive table properties
```

## 2、Talks and Presentations

> Transactional Operations In Hive by Eugene Koifman at Dataworks Summit 2017, San Jose, CA, USA

- [Slides](https://www.slideshare.net/Hadoop_Summit/transactional-sql-in-apache-hive)
- [Video](https://www.youtube.com/watch?v=Rk8irGDjpuI&feature=youtu.be)

> DataWorks Summit 2018, San Jose, CA, USA - Covers Hive 3 and ACID V2 features

- [Slides](https://www.slideshare.net/Hadoop_Summit/transactional-operations-in-apache-hive-present-and-future-102803358)
- [Video](https://www.youtube.com/watch?v=GyzU9wG0cFQ&t=834s)
