# Hive Replication

[TOC]

## 1、Overview

> Hive Replication builds on the [metastore event](https://cwiki.apache.org/confluence/display/Hive/HCatalog+Notification) and [ExIm](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+ImportExport) features to provide a framework for replicating Hive metadata and data changes between clusters. There is no requirement for the source cluster and replica to run the same Hadoop distribution, Hive version, or metastore RDBMS. The replication system has a fairly 'light touch', exhibiting a low degree of coupling and using the Hive-metastore Thrift service as an integration point. However, the current implementation is not an 'out of the box' solution. In particular it is necessary to provide some kind of orchestration service that is responsible for requesting replication tasks and executing them.

Hive Replication 建立在 metastore event 和 ExIm 特性的基础上，提供了一个在集群间复制 Hive 元数据和数据更改的框架。

源集群和副本不需要运行相同的 Hadoop 发行版、Hive 版本或 metastore RDBMS。

复制系统具有相当“轻触性”，表现出低耦合度，并使用 Hive-metastore Thrift 服务作为集成点。

然而，当前的实现并不是一个“开箱即用”的解决方案。特别是，有必要提供某种业务流程服务，该服务负责请求复制任务并，执行它们。

> See [HiveReplicationDevelopment](https://cwiki.apache.org/confluence/display/Hive/HiveReplicationDevelopment) for information on the design of replication in Hive.

关于 Hive 中复制设计的信息，请参阅 HiveReplicationDevelopment。

> A more advanced replication mechanism is being implemented in Hive to address some of the limitations of this mode. See [HiveReplicationv2Development](https://cwiki.apache.org/confluence/display/Hive/HiveReplicationv2Development) for details.

Hive 中正在实现一种更高级的复制机制，以解决这种模式的一些限制。有关详细信息，请参阅 HiveReplicationv2Development。

## 2、Potential Uses

- 灾难恢复集群。

- 将数据复制到云中进行外部;处理。

> Disaster recovery clusters.
> Copying data into clouds for off-premise processing.

## 3、Prerequisites

- 你必须在你的复制源头上运行 Hive 1.1.0 或更高版本(为了支持 DbNotificationListener)。

- 你必须在复制目的地运行 Hive 0.8.0 或更高版本(为了支持 IMPORT)。

- 你需要 Hive 1.2.0 或更高版本的 JAR 依赖来实例化和执行 ReplicationTasks。这不是集群的要求；它仅用于编排复制的服务。

- 你首先需要源集群上的管理员权限，才能向 metastore 数据库写入通知。

> You must be running Hive 1.1.0 or later at your replication source (for DbNotificationListener support).
> You must be running Hive 0.8.0 or later at your replication destination (for IMPORT support).
> You'll require Hive 1.2.0 or later JAR dependencies to instantiate and execute ReplicationTasks. This is not a cluster requirement; it is needed only for the service orchestrating the replication.
> You will initially require administration privileges on the source cluster to enable the writing of notifications to the metastore database.

## 4、Limitations

> While the metastore events feature allows the sinking of notifications to anything implementing MetaStoreEventListener, the implementation of the replication feature can only source events from the metastore database and hence the DbNotificationListener must be used.

- 虽然 metastore events 特性允许将通知发送给任何实现了 MetaStoreEventListener 的对象，但复制特性的实现只能从 metastore 数据库中获取事件源，因此必须使用 DbNotificationListener。

> Data appended to tables or partitions using the HCatalogWriters will not be automatically replicated as they do not currently generate metastore notifications ([HIVE-9577](https://issues.apache.org/jira/browse/HIVE-9577)). This is likely only a consideration if data is being written to table by processes outside of Hive.

- 使用 HCatalogWriters 追加到表或分区的数据不会被自动复制，因为它们目前不会生成 metastore 通知。这可能只有在数据被 Hive 之外的进程写入表时才需要考虑。

## 5、Configuration

> To configure the persistence of metastore notification events it is necessary to set the following hive-site.xml properties on the source cluster. A restart of the metastore service will be required for the settings to take effect.

要配置 metastore 通知事件的持久性，有必要在源集群上设置以下 hive-site.xml 属性。

需要重启 metastore 服务才能使设置生效。

> hive-site.xml Configuration for Replication

```xml
<property>
  <name>hive.metastore.event.listeners</name>
  <value>org.apache.hive.hcatalog.listener.DbNotificationListener</value>
</property>
<property>
  <name>hive.metastore.event.db.listener.timetolive</name>
  <value>86400s</value>
</property>
```

> The system uses the org.apache.hive.hcatalog.api.repl.exim.EximReplicationTaskFactory by default. This uses EXPORT and IMPORT commands to capture, move, and ingest the metadata and data that need to be replicated. However, it is possible to provide custom implementations by setting the hive.repl.task.factory Hive configuration property.

系统默认使用 `org.apache.hive.hcatalog.api.repl.exim.EximReplicationTaskFactory`。

它使用 EXPORT 和 IMPORT 命令来捕获、移动和摄取需要复制的元数据和数据。但是，可以通过设置 `hive.repl.task.factory` 来提供定制的实现。

## 6、Typical Mode of Operation

> With the metastore event configuration in place on the source cluster, the NOTIFICATION_LOG table in the metastore will be populated with events on the successful execution of metadata operations such as CREATE, ALTER, and DROP.

- 在源集群上有了 metastore event 配置后，metastore 中的 NOTIFICATION_LOG 表将在成功执行如 CREATE、ALTER 和 DROP 等元数据操作时填充事件。

> These events can be read and converted into ReplicationTasks using org.apache.hive.hcatalog.api.HCatClient.getReplicationTasks(long, int, String, String).

- 这些事件可以使用 `org.apache.hive.hcatalog.api.HCatClient.getReplicationTasks(long, int, String, String)` 读取并转换为 ReplicationTasks。

> ReplicationTasks encapsulate a set of commands to execute on the source Hive instance (typically to export data) and another set to execute on the replica instance (typically to import data). The commands are provided as Hive SQL strings.

- ReplicationTasks 封装了一组命令，以在源 Hive 实例上执行(通常用于导出数据)，另一组命令在复制实例上执行(通常用于导入数据)。命令以 Hive SQL 字符串的形式提供。

> The ReplicationTask also serves as a place where database and table name mappings can be declared and StagingDirectoryProvider implementations configured for the resolution of paths at both the source and destination:

- ReplicationTask 还可以作为声明数据库和表名映射的地方，并配置 StagingDirectoryProvider 实现，以解析源和目标的路径:

	- org.apache.hive.hcatalog.api.repl.ReplicationTask.withDbNameMapping(Function<String, String>)
	- org.apache.hive.hcatalog.api.repl.ReplicationTask.withTableNameMapping(Function<String, String>)
	- org.apache.hive.hcatalog.api.repl.ReplicationTask.withSrcStagingDirProvider(StagingDirectoryProvider)
	- org.apache.hive.hcatalog.api.repl.ReplicationTask.withDstStagingDirProvider(StagingDirectoryProvider)

> The Hive SQL commands provided by the tasks must then be executed against the source Hive and then the destination (aka the replica). One way of doing this is to open up a JDBC connection to the respective HiveServer and submit the task's Hive SQL queries.

- 任务提供的 Hive SQL 命令必须先在源 Hive 执行，然后再对目标 Hive(也就是副本)执行。其中一种方法是打开到各自 HiveServer 的 JDBC 连接，并提交任务的 Hive SQL 查询。

> It is necessary to maintain the position within the notification log so that replication tasks are applied only once. This can be achieved by maintaining a record of the last successfully executed event's id (task.getEvent().getEventId()) and providing this as an offset when sourcing the next batch of events.

- 有必要维护通知日志中的位置，以便只应用一次复制任务。这可以通过维护上次成功执行的事件id的记录(task.getEvent().getEventId())，并在查找下一批事件时将其作为偏移量来实现。

> To avoid losing or missing events that require replication, it may be wise to poll for replication tasks at a frequency significantly greater than derived from the hive.metastore.event.db.listener.timetolive property. If notifications are not consumed in a timely manner they may be purged from the table before they can be actioned by the replication service.

- 为了避免丢失或缺失要求的复制的事件，明智的做法是以比从 `hive.metastore.event.db.listener.timetolive` 派生的频率高得多的频率轮询复制任务。如果通知没有及时使用，则可能在复制服务对其进行操作之前从表中清除它们。

## 7、Replication to AWS/EMR/S3

> At this time it is not possible to replicate to tables on EMR that have a path location in S3. This is due to a bug in the dependency of the IMPORT command in the EMR distribution (checked in AMI-4.2.0). Also, if using the EximReplicationTaskFactory you may need to add the relevant S3 protocols to your Hive configurations:

此时，不可能复制到 EMR 上在 S3 中具有路径位置的表。这是由于 EMR 发行版中的 IMPORT 命令依赖的一个 bug(在AMI-4.2.0中检出)。同样，如果使用 EximReplicationTaskFactory，你可能需要在你的 Hive 配置中添加相关的 S3 协议:

> HiveConf Configuration for ExIm on S3

```xml
<property>
  <name>hive.exim.uri.scheme.whitelist</name>
  <value>hdfs,s3a</value>
</property>
```