# HiveReplicationDevelopment

[TOC]

## 1、Introduction

> Replication in the context of databases and warehouses is the process of duplication of entities from one warehouse to another. This can be at the broader level of an entire database, or at a smaller level such as a table or partition. The goal of replication is to have a replica which changes whenever the base entity changes.

在数据库和仓库环境中的复制是将实体从一个仓库复制到另一个仓库的过程。这可以是在整个数据库的更广泛的级别，也可以是如表或分区的更小的级别。

复制的目标是拥有一个随基本实体更改而更改的副本。

> In Hive, replication (introduced in [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-7973)) focuses on disaster recovery, using a lazy, primary-copy model. It uses notifications and export/import statements to implement an API for replication that can then be executed by other tools such as [Falcon](https://falcon.apache.org/).

在 Hive 中，复制主要关注灾难恢复，使用惰性的主拷贝模型。

它使用 notifications 和 export/import 语句来实现用于复制的 API，然后可以由其他工具(如Falcon)执行。

> See [Hive Replication](https://falcon.apache.org/) for usage information.

> Version 2 of Hive Replication. This document describes the initial version of Hive Replication. A second version is also available: see [HiveReplicationv2Development](https://cwiki.apache.org/confluence/display/Hive/HiveReplicationv2Development) for details.

Hive 复制的版本 2。

本文档介绍了 Hive Replication 的初始版本。还有第二个版本可用:有关详细信息，请参阅 HiveReplicationv2Development。

### 1.1、Purposes of Replication

> Replication is the process of making a duplicate copy of some object such as a database, table, or partition. There are two main use cases for replication: disaster recovery and load balancing.

复制是对某些对象(如数据库、表或分区)进行复制的过程。

复制有两个主要用例：灾难恢复和负载平衡。

#### 1.1.1、Disaster Recovery

- 允许在灾难(如服务器无法恢复地崩溃)或自然灾害(如火灾和地震)后恢复数据。

- 在没有损失的情况下，优先考虑数据/元数据的安全性。

- 可用性的速度并不那么重要。

- 热-冷备份通常就足够了。

> Allows recovery of data after a disaster such as a server crashing unrecoverably or natural disasters such as fires and earthquakes.
> Prioritizes the safety of the data/metadata with no loss.
> Speed of availability is not as important.
> Hot-cold backup is often sufficient.

#### 1.1.2、Load Balancing

- 允许跨多个系统分割用户，以管理比系统通常能够处理的负载的更大负载。

- 优先考虑速度和访问。

- 在大多数情况下趋向于只读。

- 热备份通常是可取的。

> Allows the splitting of users across multiple systems to manage a heavier load than the system would normally be able to handle.
> Prioritizes speed and access.
> Tends to be read-only for most uses.
> Hot-warm backup is usually desirable.

### 1.2、Replication Taxonomy

> Replication systems are frequently classified by their transaction source (“where”) and their synchronization strategy (“when”) [1].

复制系统通常按照它们的事务源（地点）和同步策略（时间）分类。

#### 1.2.1、Transaction Source

##### 1.2.1.1、Primary-Copy

- 从主单向复制到副本。

- 优点:简单的并发控制(无需因为复制的需要而引入锁)。

- 缺点:不适合非读的负载均衡。

> Unilateral copy from primary to replica.
> Pros: Simple concurrency control (no locks introduced by the need for replication).
> Cons: Poor for non-read load balancing.

##### 1.2.1.2、Update-Anywhere

- 允许双向更新。

- 优点:有利于负载均衡。

- 缺点:复杂的并发控制。

> Allows bidirectional updates.
> Pros: Good for load balancing.
> Cons: Complex concurrency control.

#### 1.2.2、Synchronization Strategy

##### 1.2.2.1、Eager

- 创建一个单独的连续事件流。

- 优点:保证很强的一致性。

- 缺点:响应时间长，可能存在性能问题，因为要求在当前事件的复制已暂存之前不处理任何事件。

> Creates a singular sequential stream of events.
> Pros: Guarantees strong consistency.
> Cons: Possible performance problems with long response times due to the requirement that no event may be processed until the replication of the current event has been staged.

##### 1.2.2.2、Lazy

- 允许对事件进行重新排序，以优化资源的使用。

- 优点:快速响应用户时间。

- 缺点:在目标上可能存在陈旧数据和临时不一致。

> Allows some reordering of events to optimize use of resources.
> Pros: Quick response time to user.
> Cons: Can have stale data on destination and temporary inconsistency.

## 2、Design

### 2.1、Taxonomy Design Choices

> Replication in Hive uses lazy, primary-copy replication for a number of reasons as discussed below.

Hive 中的复制使用惰性的 primary-copy 复制，原因如下所述

#### 2.1.1、Primary-Copy vs Update-Anywhere

> Since replication in Hive focuses on disaster recovery, the read-only load balancing offered by primary-copy replication is sufficient. The cost of complex concurrency control in update-anywhere replication is too high. Additionally, primary-copy replication can be isolated to be unidirectional per object, which allows for scenarios where two different objects can be replicated in different directions if necessary. This might be sufficient for some load-balancing requirements.

由于 Hive 中的复制侧重于灾难恢复，primary-copy 复制提供的只读的负载均衡就足够了。在 update-anywhere 中，复杂的并发控制的成本太高。【？？？】

此外，可以将 primary-copy 隔离为每个对象的单向复制，这允许在必要时向不同方向复制两个不同的对象。对于某些负载平衡需求，这可能已经足够了。

#### 2.1.2、Eager vs Lazy

> Eager replication requires a guaranteed delta log for every update on the primary. This poses a problem when [external tables](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-ExternalTables) are used in Hive. External tables allow data to be managed completely outside of Hive, and therefore may not provide Hive with a complete and accurate delta log. With ACID tables, such a log does exist and will be made use of in future development, but currently replication in Hive works only with traditional tables.

即时复制需要保证主服务器上的每次更新都有增量日志。当在 Hive 中使用外部表时，会产生一个问题。

外部表允许在 Hive 之外完全管理数据，因此可能不会为 Hive 提供完整和准确的增量日志。

在 ACID 表中，这样的日志是存在的，并且在将来的开发中会用到，但是目前 Hive 中的复制只适用于传统的表。

> Instead, Hive uses lazy replication. Unlike eager replication, lazy replication is asynchronous and non-blocking which allows for better resource utilization. This is prioritized in Hive with the acknowledgement that there is some complexity involved with events being processed out of order, including destructive events such as DROP.

相反，Hive 使用的是惰性复制。与即时复制不同，惰性复制是异步和非阻塞的，这允许更好地利用资源。这在 Hive 中是优先的，因为有一些复杂的事件被乱序处理，包括像 DROP 这样的破坏性事件。

### 2.2、Other Design Choices

> In addition to the taxonomy choices listed above, a number of other factors influenced the design of replication in Hive:

除了上面列出的分类选择，还有许多其他因素影响了 Hive 中复制的设计:

> Hive’s primary need for replication is disaster recovery.

- Hive 对复制的主要需求是灾难恢复。

> Hive supports a notion of export/import. There are already some implementations of manual replication and time-automated replication with Perl/Python scripts using export/import and [DistCp](https://hadoop.apache.org/docs/r1.2.1/distcp2.html).

- Hive 支持 export/import 的概念。使用 export/import 和 DistCp 的 Perl/Python 脚本已经实现了一些手动复制和时间自动复制。

> Hive already has the ability to send notifications to trigger actions based on an event, such as an [Oozie](http://oozie.apache.org/) job triggered by a partition being loaded.

- Hive 已经具备了发送通知来触发基于事件的操作的能力，比如一个被加载的分区触发的 Oozie job。

> UI is not intended as there are other data management tools (such as [Falcon](https://falcon.apache.org/)) that already cover data movement and include some form of feed replication. Hive should allow these tools to plug into the replication system. Hive handles the logic and provides APIs for external tools to handle the mechanics (execution, data management, user interaction).

- UI 并不适用于此，因为已经有其他数据管理工具(如Falcon)涵盖了数据移动，并包含某种形式的提要复制。Hive 应该允许这些工具插入到复制系统中。Hive 处理逻辑，并为外部工具提供 APIs 来处理机制(执行、数据管理、用户交互)。

> Timestamp/feed-based replication is solved by other tools and therefore Hive does not need to re-solve this problem.

- 基于时间戳/feed的复制是通过其他工具解决的，因此 Hive 不需要解决这个问题。

> HDFS replication and backing metastore database replication are also possible using broad DFS copies and native replication tools of backing databases and therefore Hive does not need to re-solve these problems. Additionally, attempting to solve replication by these methods would lead to multiple sources of truth and causes

- HDFS 复制和备份 metastore 数据库复制也可以使用广泛的 DFS 副本和备份数据库的本地复制工具，因此 Hive 不需要解决这些问题。此外，试图通过这些方法来解决复制问题将导致多个真实源，并导致同步问题，而且不容易实现更细粒度的方法(也就是说，它们不容易只支持热表或分区的复制)。

### 2.3、Basic Approach

> Hive already supports [EXPORT and IMPORT commands](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+ImportExport) which can be used to dump out tables, DistCp them to another cluster, and import/create from that. A mechanism which automates exports/imports would establish a base on which replication could be developed. With the aid of HiveMetaStoreEventHandler mechanisms, such automation can be developed to generate notifications when certain changes are committed to the metastore and then translate those notifications to export actions, DistCp actions, and import actions.

Hive 已经支持 EXPORT 和 IMPORT 命令，这些命令可以用来转储表，将它们 DistCp 到另一个集群，并从该集群 import/create。

一种使 exports/imports 自动化的机制将建立一个可以开发复制的基础。在 HiveMetaStoreEventHandler 机制的帮助下，可以开发这种自动化，在某些更改提交到 metastore 时生成通知，然后将这些通知转换为 export 操作、DistCp 操作和 import 操作。

> This already partially exists with the [notification system](https://cwiki.apache.org/confluence/display/Hive/HCatalog+Notification) that is part of the hcatalog-server-extensions jar. Initially, this was developed to be able to trigger a JMS notification, which an Oozie workflow could use to start off actions keyed on the finishing of a job that used HCatalog to write to a table. While this currently lives under HCatalog, the primary reason for its existence has a scope well past HCatalog alone and can be used as-is without the use of [HCatalog IF/OF](https://cwiki.apache.org/confluence/display/Hive/HCatalog+InputOutput). This can be extended with the help of a library which does that aforementioned translation of notifications to actions.

这已经部分存在于作为 hcatalog-server-extensions jar 一部分的通知系统中。

最初，开发它是为能够触发 JMS 通知，Oozie 工作流可以使用 JMS 通知来启动操作，以完成使用 HCatalog 向表写入的作业。

虽然它目前存在于 HCatalog 下，但它存在的主要原因是它的作用域远远超过了 HCatalog 本身，并且可以在不使用 HCatalog IF/OF 的情况下按现状使用。在上述将通知转换为行动的库的帮助下，可以扩展这一功能。

## 3、Implementation

> Hive’s implementation of replication combines the notification event subsystem with a mapping of an event to an actual task to be performed that helps automate the replication process. Hive provides an API for replication which other tools can call to handle the execution aspects of replication.

Hive 复制的实现将通知事件子系统与将要执行的实际任务的映射结合在一起，这有助于实现复制过程的自动化。

Hive 提供了一个用于复制的 API，其他工具可以调用该 API 来处理复制的执行方面。

### 3.1、Events

> Events are triggered when any change happens to the warehouse. Hive provides an API for tools to read these events. Each event has an associated event sequence ID (monotonically increasing). Once read, these events are then translated to replication tasks by means of a ReplicationTaskFactory.

当仓库发生任何更改时，将触发事件。

Hive 为工具提供了可以读取这些事件的 API。每个事件都有一个相关的事件序列 ID(单调递增)。一旦读取，这些事件就会通过 ReplicationTaskFactory 转换成复制任务。

> Thus, the base replication mechanism in Hive is not tied exclusively to the export/import (EXIM) mechanism described previously, which instead serves only as a default event-to-task mapping mechanism. It is possible for an organization or a project integrating with Hive to plug in some other ReplicationTaskFactory implementation to achieve the same goal, which would allow modifications such as using additional bookkeeping or security aspects or using a different mechanism to move data from one warehouse to another. It is also theoretically possible to use a different implementation to achieve some other form of replication, even across database technologies, such as Hive --> MySQL, for instance. However, this document will focus on the details of the EXIM mechanism as implemented by EximReplicationTaskFactory.

因此，Hive 中的基本复制机制并不是只与前面描述的 export/import (EXIM) 机制绑定在一起，它只是作为一个默认的事件到任务的映射机制。

它可能是一个组织或一个项目结合 Hive 插入其他 ReplicationTaskFactory 实现实现同样的目标，这将允许修改如使用额外的簿记或安全方面或使用不同的机制来将数据从一个仓库移动到另一个。

理论上也可以使用不同的实现实现其他形式的复制，甚至跨数据库技术，例如 Hive --> MySQL。然而，本文将重点关注 EximReplicationTaskFactory 实现的 EXIM 机制的细节。

> Execution of events occurs after the mapping from event to task is completed. All aspects of execution, such as managing appropriate credentials, optimum cluster use, and scheduling, are outside of the scope of Hive and are therefore performed by replication tools such as HiveDR in Falcon.

事件的执行发生在事件到任务的映射完成之后。执行的所有方面，比如管理适当的凭证、最优的集群使用和调度，都不在 Hive 的范围之内，因此都是由复制工具执行的，比如 Falcon 中的 HiveDR。

### 3.2、Event IDs, State IDs, and Sequencing of Exports/Imports

> As mentioned above, each event is tagged with an event sequence ID. In addition to event IDs, exports are tagged with the current state ID of the primary warehouse at the time when the export occurs (which will be a later time than the event itself) since replication is done asynchronously. This allows replication to determine if the state of the object at the destination is newer or older than that being exported from the source and make a decision accordingly at import time as to whether or not to accept the import. If the destination’s current state of the object is newer than the state of the object being exported from the source, it will not copy that change. This allows for robustness in the face of replication event repeats or restarts after failure.

如前所述，每个事件用一个事件序列 ID 标记。除了事件 IDs，export 使用当 export 出现时的主仓库的当前的状态 ID 标记(将在比事件本身稍后的时间)，因为复制操作是异步完成的。

这允许复制确定目标对象的状态是否比从源导出的对象的状态更新或更旧，并在导入时相应地做出是否接受导入的决定。

如果目标对象的当前状态比从源导出的对象的状态更新，则不会复制该更改。这允许在复制事件重复或失败后重新启动时具有健壮性。

### 3.3、Handling of Events

> Each event is handled differently depending on its event type, which is a combination of the object (database, table, partition) and the operation (create, add, alter, insert, drop). Each event may include a source command, a copy, and a destination command. The following chart describes the ten event types and how they are handled with descriptions below.

每个事件的处理方式取决于其事件类型，它对象(数据库、表、分区)和操作(创建、添加、更改、插入、删除)的组合。

每个事件可以包括一个源命令、一个复制和一个目标命令。下面的图表描述了十种事件类型，以及如何处理它们。

Event  |   Source Command  | Needs Copy?  |  Destination Command
---|:---|:---|:---
AddPartition | (multi) EXPORT … FOR REPLICATION  |  Yes | (multi) IMPORT
AlterDatabase| (not implemented) | (not implemented) | (not implemented)
AlterPartition | EXPORT … FOR METADATA REPLICATION | Yes (metadata only) | IMPORT
AlterTable | EXPORT … FOR METADATA REPLICATION | Yes (metadata only) | IMPORT
CreateDatabase | No-op | No | No-op
CreateTable  | EXPORT … FOR REPLICATION  | Yes | IMPORT
DropDatabase | No-op | No | DROP DATABASE CASCADE
DropPartition | No-op | No | (multi) ALTER TABLE … DROP PARTITION(…) FOR REPLICATION(‘id’)
DropTable | No-op | No | DROP TABLE … FOR REPLICATION(‘id’)
Insert | EXPORT … FOR REPLICATION | Yes (dumb copy) | IMPORT

**CreateDatabase**

> For security reasons, Hive does not currently allow the replication of a database before it exists since that may enable a replication of private data unintentionally. Accordingly, everything is set to no-op. However, this may be revisited in the future and therefore the implementation includes an API point.

出于安全原因，Hive 目前不允许在数据库存在之前复制数据库，因为这可能会在无意中启用私有数据的复制。因此，一切都被设置为 no-op。然而，这在将来可能会被重新访问，因此实现包括一个 API 点。

**DropDatabase**

> Drop does not require any action on the source side or any copying. On the destination side, it requires a drop database cascade. Although this is a dangerous command, the current requirement for a database to already exist before it can be replicated mitigates the risk by not allowing any replication definitions to be set up before the object exists and therefore this is considered an intended action.

删除不需要在源端执行任何操作或进行任何复制。在目标端，它需要 drop database cascade。尽管这是一个危险的命令，但当前对数据库的要求是在复制之前已经存在，这可以降低风险，因为在对象存在之前不允许设置任何复制定义，因此这被认为是一个预期的操作。

**AlterDatabase**

> This has not currently been implemented as Hive considers a database to be a container object. The only meaningful alteration would therefore be the HDFS location, which has no need for replication. Therefore there is no API point for this.

目前还没有实现，因为 Hive 认为数据库是一个容器对象。因此，唯一有意义的更改是 HDFS 的位置，它不需要复制。因此没有 API 点。

**CreateTable**

> This requires an export on the source side, a copying of data, and an import on the destination side. This import will only execute if the import’s state ID is newer than the replica object’s state ID. Otherwise, it will be a no-op.

这需要源端导出、数据复制和目标端导入。只有当导入的状态 ID 比复制对象的状态 ID 更新时，该导入才会执行。否则，它将是一个 no-op。

**DropTable**

> Drop does not require any action on the source side or any copying. On the destination side, the table will be dropped if the state ID included in the drop command is newer than the state ID of the replica object. Otherwise, it will be a no-op.

删除不需要在源端执行任何操作或进行任何复制。在目标端，如果 drop 命令中包含的状态 ID 比复制对象的状态 ID 更新，则该表将被删除。否则，它将是一个 no-op。

**AlterTable**

> This requires the export, copying, and import of metadata only. The import will only execute if its state ID is newer than the replica object’s state ID. Otherwise, it will be a no-op.

这只需要元数据的导出、复制和导入。只有当它的状态 ID 比复制对象的状态 ID 更新时，导入才会执行。否则，它将是一个 no-op。

**AddPartition**

> Multiple partitions can be added atomically, resulting in multiple export commands bundled into one event and a copying of the data. The imports will only execute if their state ID is newer than the replica object’s state ID. Otherwise, it will be a no-op.

可以自动添加多个分区，从而导致多个导出命令捆绑到一个事件中，并复制数据。只有当导入的状态 ID 比复制对象的状态 ID 更新时，导入才会执行。否则，导入将是一个 no-op。

**DropPartition**

> Drop does not require any action on the source side or any copying. On the destination side, there can be multiple commands for one event, as with AddPartition. These are only executed if their state ID is newer than the replica object’s state ID. Otherwise, it will be a no-op.

删除不需要在源端执行任何操作或进行任何复制。在目标端，一个事件可以有多个命令，就像使用 AddPartition 一样。只有当它们的状态 ID 比复制对象的状态 ID 更新时，它们才会被执行。否则，它将是一个 no-op。

**AlterPartition**

> This requires the export, copying, and import of metadata only. Unlike AddPartition and DropPartition, AlterPartition only modifies one partition at a time. The import will only execute if its state ID is newer than the replica object’s state ID. Otherwise, it will be a no-op.

这只需要元数据的导出、复制和导入。与 AddPartition 和 DropPartition 不同，AlterPartition 一次只修改一个分区。只有当它的状态 ID 比复制对象的状态 ID 更新时，导入才会执行。否则，它将是一个 no-op。

**Insert**

> This requires the export, copying, and import of data (not metadata). Currently it is a dumb copy of the entire object (rather than applying only the changes made), although this is an area to improve and optimize in the future. The import will only execute if its state ID is newer than the replica object’s state ID. Otherwise, it will be a no-op.

这需要数据的导出、复制和导入(不是元数据)。目前，它只是整个对象的一个 dumb 副本(而不是仅应用所做的更改)，尽管这是未来需要改进和优化的领域。只有当它的状态 ID 比复制对象的状态 ID 更新时，导入才会执行。否则，它将是一个 no-op。

## 4、Future Features

> In the future, additional work should be done in the following areas:

今后，还应在以下方面进行更多的工作：

> Enable replication to work with ACID tables, especially with relation to load-balancing use cases. Load-balancing might also be needed only for specific hot tables, allowing for a more surgical use of replication.

- 允许复制与 ACID 表一起工作，特别是与负载均衡相关的情况。负载均衡也可能只需要用于特定的热表，从而允许更精确地使用复制。

> Limit unnecessary copies by allowing for event nullification/collapsing.

- 通过允许事件 nullification/collapsing 来限制不必要的副本。

> Look at storing events generated on message queues like [Kafka](http://kafka.apache.org/), or on-disk on HDFS, rather than in the metastore.

- 将事件存储在 Kafka 这样的消息队列上，或者存储在 HDFS 的磁盘上，而不是存储在 metastore 中。

> Support replication of more Hive objects such as roles, users, etc.

- 支持更多 Hive 对象的复制，如角色、用户等。

> [HIVE-7973](https://issues.apache.org/jira/browse/HIVE-7973) tracks progress on developing replication in Hive.

## 5、References

[1] Kemme, B., et al., "Database Replication: A Tutorial," in Replication: Theory and Practice, B. Charron-Bost et al., Eds. Berlin, Germany: Springer, 2010, pp. 219-252.