# Managed vs. External Tables

[TOC]

> Hive fundamentally knows two different types of tables:

Hive 主要有两种不同类型的表：

- Managed (Internal)
- External

## 1、Introduction

> This document lists some of the differences between the two but the fundamental difference is that Hive assumes that it owns the data for managed tables. That means that the data, its properties and data layout will and can only be changed via Hive command. The data still lives in a normal file system and nothing is stopping you from changing it without telling Hive about it. If you do though it violates invariants and expectations of Hive and you might see undefined behavior.

本文档列出了这两类表间的一些区别，但**最根本的区别是受管表的数据由 Hive 管理**。

**这意味着数据、其属性和数据布局只能通过 Hive 命令更改**。

数据存在于一个正常的文件系统中，可以在不通知 Hive 的情况下修改数据。但是，如果你这样做，它会违背 Hive 的不变量和期望，你可能会看到未定义的行为。

> Another consequence is that data is attached to the Hive entities. So, whenever you change an entity (e.g. drop a table) the data is also changed (in this case the data is deleted). This is very much like with traditional RDBMS where you would also not manage the data files on your own but use a SQL-based access to do so.

【受管表】另一个后果是数据被附加到 Hive 实体上。因此，无论是什么时候更改一个实体(例如删除一个表)，数据也会更改(在这种情况下，数据被删除)。

这很像传统的 RDBMS，不需要自己管理数据文件，而是使用基于 SQL 的访问来管理。

> For external tables Hive assumes that it does not manage the data.

对于外部表，Hive 不管理它的数据。

> Managed or external tables can be identified using the [DESCRIBE FORMATTED table_name](https://cwiki.apache.org/confluence/display/Hive/Managed+vs.+External+Tables#Managedvs.ExternalTables-DescribeTable/View/Column) command, which will display either MANAGED_TABLE or EXTERNAL_TABLE depending on table type.

可以**使用 `DESCRIBE FORMATTED table_name` 命令识别外部表和受管表**，输出要么展示 MANAGED_TABLE，要么展示 EXTERNAL_TABLE，这取决于表的类型。

> [Statistics](https://cwiki.apache.org/confluence/display/Hive/StatsDev) can be managed on internal and external tables and partitions for query optimization. 

可以在内部和外部表（分区）上管理统计信息，以便进行查询优化。

## 2、Feature comparison

> This means that there are lots of features which are only available for one of the two table types but not the other. This is an incomplete list of things:

这意味着有很多特性只对其中一种表可用，而对另一种不可用。这是一个不完整的清单:

- ARCHIVE/UNARCHIVE/TRUNCATE/MERGE/CONCATENATE 仅对受管表有效

- DROP 删除受管表的数据，而只能删除外部表的元数据

- ACID/Transactional 仅对受管表有效

- 查询结果缓存仅对受管表有效

- 外部表上只允许 RELY 约束

- 一些物化视图特性只在受管表上工作

> ARCHIVE/UNARCHIVE/TRUNCATE/MERGE/CONCATENATE only work for managed tables
> DROP deletes data for managed tables while it only deletes metadata for external ones
> ACID/Transactional only works for managed tables
> [Query Results Caching](https://issues.apache.org/jira/browse/HIVE-18513) only works for managed tables
> Only the RELY constraint is allowed on external tables
> Some Materialized View features only work on managed tables

## 3、Managed tables

> A managed table is stored under the [hive.metastore.warehouse.dir](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.metastore.warehouse.dir) path property, by default in a folder path similar to /user/hive/warehouse/databasename.db/tablename/. The default location can be overridden by the location property during table creation. If a managed table or partition is dropped, the data and metadata associated with that table or partition are deleted. If the PURGE option is not specified, the data is moved to a trash folder for a defined duration.

受管表存储在 `hive.metastore.warehouse.dir` 路径下，默认是 `/user/hive/warehouse/databasename.db/tablename/` 目录下。

**在创建表期间，可以使用 location 属性覆盖默认值**。

如果删除了受管表或分区，则与该表或分区关联的数据和元数据将被删除。

如果未指定 PURGE 选项，数据将在定义的持续时间内移动到回收站文件夹中。

> Use managed tables when Hive should manage the lifecycle of the table, or when generating temporary tables.

当需要 Hive 管理表生命周期时，或者在生成临时表时，使用受管表。

## 4、External tables

> An external table describes the metadata / schema on external files. External table files can be accessed and managed by processes outside of Hive. External tables can access data stored in sources such as Azure Storage Volumes (ASV) or remote HDFS locations. If the structure or partitioning of an external table is changed, an [MSCK REPAIR TABLE table_name](https://cwiki.apache.org/confluence/display/Hive/Managed+vs.+External+Tables#Managedvs.ExternalTables-RecoverPartitions(MSCKREPAIRTABLE)) statement can be used to refresh metadata information.

外部表描述外部文件上的元数据/模式。

**Hive 外的进程可以访问和管理外部表文件**。

外部表可以访问存储在像 Azure Storage Volumes (ASV) 或远程 HDFS 位置中的数据。

当外部表的结构或分区发生变化时，可以使用 MSCK REPAIR table table_name 语句来刷新元数据信息。

> Use external tables when files are already present or in remote locations, and the files should remain even if the table is dropped.

当文件已经存在或在远程位置时，使用外部表，即使表被删除，文件也应该保留。