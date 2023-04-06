# AdminManual Metastore 3.0 Administration

原文链接：[https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+3.0+Administration](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+3.0+Administration)

## 1、Version Note

> This document applies only to the Metastore in Hive 3.0 and later releases. For Hive 0, 1, and 2 releases please see the [Metastore Administration](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+Administration) document.

本文档仅适用于 Hive 3.0 及更早版本的 Metastore。

对于 Hive 0、1 和 2 版本请见 Metastore Administration。

## 2、Introduction

> The definition of Hive objects such as databases, tables, and functions are stored in the Metastore. Depending on how the system is configured, statistics and authorization records may also be stored there. Hive, and other execution engines, use this data at runtime to determine how to parse, authorize, and efficiently execute user queries.  

**Hive 对象(如数据库、表、函数等)的定义存储在 Metastore 中**。

根据系统的配置方式，**统计信息和授权记录也可能存储在那里**。

Hive 和其他执行引擎在运行时使用这些数据来决定如何解析、授权和有效地执行用户查询。

> The Metastore persists the object definitions to a relational database (RDBMS) via [DataNucleus](http://www.datanucleus.org/), a Java JDO based Object Relational Mapping (ORM) layer. See [Supported RDBMSs](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+3.0+Administration) below for a list of supported RDBMSs that can be used.

Metastore **通过 DataNucleus 将对象定义持久化到关系数据库**(RDBMS)。

> The Metastore can be configured to embed the Apache Derby RDBMS or connect to a external RDBMS.  The Metastore itself can be embedded entirely in a user process or run as a service for other processes to connect to.  Each of these options will be discussed in turn below.

可以**将 Metastore 配置为嵌入 Apache Derby RDBMS 或连接到外部 RDBMS**。

Metastore 本身可以完全嵌入到用户进程中，也可以作为服务运行，供其他进程连接。下面将依次讨论这些选择。

### 2.1、Changes From Hive 2 to Hive 3

> Beginning in Hive 3.0, the Metastore can be run without the rest of Hive being installed.  It is provided as a separate release in order to allow non-Hive systems to easily integrate with it.  (It is, however, still included in the Hive release for convenience.)  Making the Metastore a standalone service involved changing a number of configuration parameter names and tool names.  All of the old configuration parameters and tools still work, in order to maximize backwards compatibility.  This document will cover both the old and new names.  As new functionality is added old, Hive style names will not be added.

**从 Hive 3.0 开始，Metastore 可以在不安装 Hive 其余部分的情况下运行**。

它是作为一个单独的发行版提供的，以允许非 hive 系统轻松地与它集成。(不过，为了方便起见，它仍然包含在 Hive 版本中。)

要使 Metastore 成为一个独立的服务，需要更改大量的配置参数名称和工具名称。为了最大限度地向后兼容，所有旧的配置参数和工具仍然有效。这个文档将包括旧的和新的名字。当新功能被添加到旧功能时，Hive 样式的名称将不会被添加。

> For details on using the Metastore without Hive, see [Running the Metastore Without Hive](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+3.0+Administration) below.

关于在没有 Hive 的情况下使用 Metastore 的详细信息，请参见下面的 Running the Metastore Without Hive。

## 3、General Configuration

> The metastore reads its configuration from the file metastore-site.xml.  It expects to find this file in $METASTORE_HOME/conf where $METASTORE_HOME is an environment variable.  For backwards compatibility it will also read any hive-site.xml or hive-metastoresite.xml files found in HIVE_HOME/conf.  Configuration options can also be defined on the command line (see [Starting and Stopping the Service](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+3.0+Administration) below).

metastore 从文件 `metastore-site.xml` 读取其配置。它需要在 `$METASTORE_HOME/conf` 中找到这个文件，其中 `$METASTORE_HOME` 是一个环境变量。

为了向后兼容，它还会读取 `HIVE_HOME/conf` 中的 `hive-site.xml` 或 `hive-metastoresite.xml` 文件。配置选项也可以在命令行上定义(请参阅下面启动和停止服务)。

> Configuration values specific to running the Metastore with various RDBMSs, embedded or as a service, and without Hive are discussed in the relevant sections.  The following configuration values apply to the Metastore regardless of how it is being run.  This table covers only commonly customized configuration values.  For less commonly changed configuration values see [Less Commonly Changed Configuration Parameters](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+3.0+Administration).

特定于使用各种 RDBMSs 运行 Metastore 的配置值，嵌入式的或作为服务的，以及不使用 Hive 的，将在相关章节中讨论。不管 Metastore 是如何运行的，以下配置值都将应用于该 Metastore。该表只包含常用的自定义配置值。有关不常更改的配置值，请参阅 Less Commonly Changed Configuration Parameters。

Parameter     |    Hive 2 Parameter   |   Default Value     |    Description
---|:---|:---|:---
metastore.warehouse.dir	 | hive.metastore.warehouse.dir |	 	|  URI of the default location for tables in the default catalog and database.【在默认的catalog和数据库中表默认位置的URI】
datanucleus.schema.autoCreateAll  |	datanucleus.schema.autoCreateAll  |	false  |	Auto creates the necessary schema in the RDBMS at startup if one does not exist. Set this to false after creating it once. To enable auto create also set hive.metastore.schema.verification=false. Auto creation is not recommended in production; run schematool instead.【如果没有schema，那么启动时，在RDBMS自动创建一个。一旦创建它之后，设置设个属性为false。为启用自动创建，设置`hive.metastore.schema.verification=false`。在生产中并不推荐自动创建，而是建议运行`schematool`】
metastore.schema.verification  |	hive.metastore.schema.verification	 | true	| Enforce metastore schema version consistency. When set to true: verify that version information stored in the RDBMS is compatible with the version of the Metastore jar. Also disable automatic schema migration. Users are required to manually migrate the schema after upgrade, which ensures proper schema migration. This setting is strongly recommended in production.When set to false: warn if the version information stored in RDBMS doesn't match the version of the Metastore jar and allow auto schema migration.【强制metastore schema版本一致性。当设置为true时:验证存储在RDBMS中的版本信息是否与Metastore jar的版本兼容。还要禁用自动schema迁移。升级后需要用户手动迁移schema，以保证模式迁移的正确性。强烈建议在生产中使用此设置。当设置为false时:如果存储在RDBMS中的版本信息与Metastore jar的版本不匹配，则发出警告，并允许自动模式迁移。】
metastore.hmshandler.retry.attempts  |	hive.hmshandler.retry.attempts  |	10	| The number of times to retry a call to the meastore when there is a connection error.【当出现连接错误时重试调用Metastore的次数。】
metastore.hmshandler.retry.interval  |	hive.hmshandler.retry.interval  |  2 sec  | Time between retry attempts.【在两次重新尝试之间的时间】
metastore.log4j.file  |	hive.log4j.file  |	none  |	Log4j configuration file. If unset will look for `metastore-log4j2.properties` in `$METASTORE_HOME/conf`
metastore.stats.autogather |	hive.stats.autogather	|  true	 |  Whether to automatically gather basic statistics during insert commands.【是否在插入命令时自动收集基本统计信息。】

## 4、RDBMS

### 4.1、Option 1: Embedding Derby

> The metastore can be run with [Apache Derby](https://db.apache.org/derby/) embedded.  This is the default configuration.  However, it is not intended for use beyond simple testing.  In this configuration only one client can use the Metastore and any changes are not durable beyond the life of the client (since it uses an in memory version of Derby).

metastore 可以在嵌入式 Apache Derby 中运行。这是默认配置。然而，除了简单的测试之外，它并不打算使用。在此配置中，只有一个客户端可以使用 Metastore，并且任何更改都不会超过客户端的生命周期(因为它使用 Derby 的内存版本)。

### 4.2、Option 2: External RDBMS

> For any durable, multi-user installation, an external RDBMS should be used to store Metastore objects.  The Metastore connects to an external RDBMS via JDBC.  Any jars required by the JDBC driver for your RDBMS should be placed in METASTORE_HOME/lib or explicilty passed on the command line.  The following values need to be configured to connect the Metastore to an RDBMS.  (Note:  these configuration parameters did not change between Hive 2 and 3.)

对于任何持久的多用户安装，都应该使用外部 RDBMS 来存储 Metastore 对象。

Metastore 通过 JDBC 连接到外部 RDBMS。RDBMS 的 JDBC driver 需要的任何 jar 都应该放在 `METASTORE_HOME/lib`中，或者通过命令行显式传递。

为了将 Metastore 连接到 RDBMS，需要配置以下值。(注意:这些配置参数在 Hive 2 和 3 之间没有改变。)

Configuration Parameter              |    Comment  
---|:---
javax.jdo.option.ConnectionURL       |   Connection URL for the JDBC driver
javax.jdo.option.ConnectionDriverName|   JDBC driver class
javax.jdo.option.ConnectionPassword  |   Password to connect to the RDBMS with. The Metastore uses [Hadoop's CredentialProvider API](http://hadoop.apache.org/docs/r3.0.1/api/org/apache/hadoop/security/alias/CredentialProvider.html) so this does not have to be stored in clear text in your configuration file.
javax.jdo.option.ConnectionUserName  |   User name to connect to the RDBMS with

#### 4.2.1、Supported RDBMSs

> As the Metastore uses DataNucleus to communicate with the RDBMS, theoretically any storage option supported by DataNucleus would work with the Metastore.  However, we only test and recommend the following:

由于 Metastore 使用 DataNucleus 与 RDBMS 通信，所以理论上，DataNucleus 支持的任何存储选项都可以与 Metastore 一起工作。然而，我们只测试并推荐以下方法:

RDBMS|Minimum Version|javax.jdo.option.ConnectionURL|javax.jdo.option.ConnectionUserName
---|:---|:---|:---
MS SQL Server  |	2008 R2	|  `jdbc:sqlserver://<HOST>:<PORT>;DatabaseName=<SCHEMA>`	|com.microsoft.sqlserver.jdbc.SQLServerDriver
MySQL  |  5.6.17  | `jdbc:mysql://<HOST>:<PORT>/<SCHEMA>`	| com.mysql.jdbc.Driver
MariaDB|    5.5   | `jdbc:mysql://<HOST>:<PORT>/<SCHEMA>`	| org.mariadb.jdbc.Driver
Oracle*|	11g	  | `jdbc:oracle:thin:@//<HOST>:<PORT>/xe`| oracle.jdbc.OracleDriver
Postgres|	9.1.13|	`jdbc:postgresql://<HOST>:<PORT>/<SCHEMA>` |  org.postgresql.Driver

`<HOST>` = The host the RDBMS is on.

`<PORT>` = Port the RDBMS is listening for JDBC connections on

`<SCHEMA>` = The schema (or database) that the Metastore stores its tables in.

> The Oracle values shown are for Oracle's thin JDBC client.  If you are using a different client the ConnectionURL and ConnectionDriverName values will differ.

显示的 Oracle 值是针对 Oracle thin JDBC 客户端的。如果使用不同的客户端，ConnectionURL 和 ConnectionDriverName 值将有所不同。

> Special Note:  When using Postgres you should set the configuration parameter metastore.try.direct.sql.ddl (previously hive.metastore.try.direct.sql.ddl) to false, to avoid failures in certain operations.

特别注意:当使用 Postgres 时，您应该设置配置参数 `metastore.try.direct.sql.ddl` 为false，以避免某些操作失败。

### 4.3、Installing and Upgrading the Metastore Schema

> The Metastore provides the schematool utility to work with the Metastore schema in the RDBMS.  For a full list of options see the -help option of the tool.  The following summarizes what the tool can do.  In most cases schematool can read the configuration from the metastore-site.xml file, though the configuration can also be passed as options on the command line.

Metastore 提供了 schematool 实用工具来处理 RDBMS 中的 Metastore schema。有关选项的完整列表，请参见工具的 `-help` 选项。下面总结了该工具的功能。

在大多数情况下，schematool 可以从 `metastore-site.xml` 文件读取配置，但是配置也可以作为命令行上的选项传递。

> -initSchema: install a new schema.  This should be used when first setting up a Metastore.

- -initSchema：安装一个新的 schema。这应该在第一次设置 Metastore 时使用。

> -upgradeSchema: upgrade to the newly installed version.  For 3.0, upgrades can be done from 1.2, 2.0, 2.1, 2.2, and 2.3 to 3.0.  If you need to upgrade from before 1.2, use an older version of Hive's schematool to first upgrade your schema to 1.2, then use the current Metastore version to upgrade to 3.0.

- -upgradeSchema：升级到新安装的版本。对于 3.0，可以从 1.2、2.0、2.1、2.2 和 2.3 升级到 3.0。如果需要从 1.2 之前的版本升级，请先使用 Hive 的 schematool 的旧版本将您 schema 升级到 1.2，然后使用当前的 Metastore 版本升级到 3.0。

> -createUser: create the Metastore user and schema.  This does not install the tables, it just creates the database user and schema.  This likely will not work in a production environment because you likely will not have permissions to create users and schemas.  You will likely need your DBA to do this for you.  

- -createUser：创建 Metastore 用户和 schema。这并不安装表，它只是创建数据库用户和 schema。这在生产环境中可能无法工作，因为你可能没有创建用户和 schema 的权限。可能需要 DBA 为你完成这些工作。

> -validate: check that your Metastore schema is correct for its recorded version

- -validate：检查 Metastore schema 对于其记录的版本是否正确

## 5、Running the Metastore

### 5.1、Embedded Mode

> The Metastore can be embedded directly into a process as a library.  This is often done with HiveServer2 to avoid an additional network hop for metadata operations.  It can also be done when using the Hive CLI or any other process.  This mode is the default and will be used anytime the configuration parameter metastore.uris is not set.

Metastore 可以作为库直接嵌入到进程中。这通常是用 HiveServer2 完成的，以避免元数据操作的额外网络跳。它也可以在使用 Hive CLI 或任何其他进程时完成。这种模式是默认的，将在任何没有设置 `metastore.uris` 的时候使用。

> Except in the case of HiveServer2, using this mode raises a few concerns.  First, having many clients will put a burden on the backing RDBMS since each client will have its own set of connections.  Second, every client must have read/write access to the RDBMS.  This makes it hard to properly secure the RDBMS.  Therefore embedded mode is not recommended in production with the exception of HiveServer2.

除了 HiveServer2，使用这种模式会引起一些关注。首先，有很多客户端会给后台 RDBMS 增加负担，因为每个客户端都有自己的连接集。其次，每个客户端都必须具有对 RDBMS 的读/写访问权限。这使得正确保护 RDBMS 变得很困难。因此，除了 HiveServer2 之外，不建议在生产环境中使用嵌入式模式。

### 5.2、Metastore Server

> To run the Metastore as a service, you must first configure it with a URL.

为了将 Metastore 作为一个服务运行，你必须首先使用一个 URL 配置它。

Configured On  |  Parameter | Hive 2 Parameter  |  Format   |  Default Value  |  Comment 
---|:---|:---|:---|:---|:---
Client	 |  metastore.thrift.uris  | hive.metastore.uris | `thrift://<HOST>:<PORT>[, thrift://<HOST>:<PORT>...]`  | none   |  HOST = hostname, PORT = should be set to match metastore.thrift.port on the server (which defaults to 9083. You can provide multiple servers in a comma separate list.
Server	 |  metastore.thrift.port  |  hive.metastore.port  |  integer  |  9083  |  Port Thrift will listen on.

> Once you have configured your clients, you can start the Metastore on a server using the start-metastore utility.  See the -help option of that utility for available options. There is no stop-metastore script.  You must locate the process id for the metastore and kill that process.

配置好客户端之后，就可以使用 `start-metastore` 在服务器上启动 Metastore。查看该实用程序的 `-help` 选项以获得可用的选项。没有 `stop-metastore` 脚本。你必须找到 metastore 的进程 id ，并 kill 该进程。

#### 5.2.1、High Availability

> The Metastore service is stateless.  This allows you to start multiple instances of the service to provide for high availability.  It also allows you to configure some clients to embed the metastore (e.g. HiveServer2) while still running a Metastore service for other clients.  If you are running multiple Metastore services you can put all their URIs into your client's metastore.thrift.uris value and then set metastore.thrift.uri.selection ( in Hive 2 hive.metastore.uri.selection) to RANDOM or SEQUENTIAL.  RANDOM will cause your client to randomly select one of the servers in the list, while SEQUENTIAL will cause it to start at the beginning of the list and attempt to connect to each server in order.

Metastore 服务是无状态的。这允许你可以启动服务的多个实例，以提供高可用性。

它还允许你配置一些客户端来嵌入 metastore(例如HiveServer2)，同时仍然为其他客户端运行 metastore 服务。

如果你正在运行多个 Metastore 服务，你可以把它们所有的 URIs 放入你的客户端的 `metastore.thrift.uris` 中，然后设置 `metastore.thrift.uri.selection`(在 Hive 2 中 `hive.metastore.uri.selection`)的值为 RANDOM 或 SEQUENTIAL。

RANDOM 将使你的客户端随机选择列表中的一个服务器，而 SEQUENTIAL 将使它从列表的开头开始，并尝试按顺序连接每个服务器。

#### 5.2.2、Securing the Service

TODO: Need to fill in details for setting up with Kerberos, SSL, etc.

	CLIENT_KERBEROS_PRINCIPAL, KERBEROS_*, SSL*, USE_SSL, USE_THRIFT_SASL

## 6、Running the Metastore Without Hive

> Beginning in Hive 3.0, the Metastore is released as a separate package and can be run without the rest of Hive.  This is referred to as standalone mode. 

从 Hive 3.0 开始，Metastore 作为一个单独的包发布，可以在没有 Hive 其他部分的情况下运行。这称为独立模式。

> By default the Metastore is configured for use with Hive, so a few configuration parameters have to be changed in this configuration.

默认情况下，配置的 Metastore 是与 Hive 一起使用，因此在这个配置中需要修改一些配置参数。

Configuration Parameter        |    Set to for Standalone Mode
---|:---
metastore.task.threads.always  |   org.apache.hadoop.hive.metastore.events.EventCleanerTask,org.apache.hadoop.hive.metastore.MaterializationsCacheCleanerTask
metastore.expression.proxy	|org.apache.hadoop.hive.metastore.DefaultPartitionExpressionProxy

> Currently the following features have not been tested or are known not to work with the Metastore in standalone mode:

目前，以下特性还没有经过测试，或者已知不能在独立模式下与 Metastore 一起工作:

- 如果没有 Hive，压缩器(用于ACID表)将无法运行。可以对ACID表进行读写，但不能压缩。

- 尚未在 Hive 之外测试复制。

> The compactor (for use with ACID tables) cannot be run without Hive.  ACID tables can be read and written to, but they cannot compacted.
> Replication has not been tested outside of Hive.

## 7、Performance Optimizations

### 7.1、CachedStore

> Prior to Hive 3.0 there was only a single implementation of the MetaStore API (called ObjectStore). [HIVE-16520](https://issues.apache.org/jira/browse/HIVE-16520) introduced a second implementation that can cache objects from the database in memory. This can save a significant amount of time for round trips to the database. It can be used by changing the parameter metastore.rawstore.impl to org.apache.hadoop.hive.metastore.cache.CachedStore.

在 Hive 3.0 之前，MetaStore API 只有一个实现(称为ObjectStore)。

HIVE-16520 引入了第二个实现，它可以在内存中缓存数据库中的对象。这可以节省大量往返数据库的时间。

它可以通过改变参数 `metastore.rawstore.impl` 为 `org.apache.hadoop.hive.metastore.cache.CachedStore` 来实现。

> The cache is automatically updated with new data when changes are made through this MetaStore. In a scenario where there are multiple MetaStore servers the caches can be out of date on some of them. To prevent this the CachedStore automatically refreshes the cache in a configurable frequency (default: 1 minute).

当通过这个 MetaStore 作为修改时，缓存将自动使用新数据更新。在有多个 MetaStore 服务器的场景中，其中一些服务器上的缓存可能已经过期。为了防止这种情况发生，CachedStore 会以可配置的频率(默认为1分钟)自动刷新缓存。

> Details about all properties for the CachedStore can be found on [Configuration Properties](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties) (Prefix: metastore.cached). 

关于 CachedStore 的所有属性的详细信息可以在配置属性中找到(前缀:metastore.cached)。

## 8、Less Commonly Changed Configuration Parameters

> BATCHED_RETRIEVE_*, CLIENT_CONNECT_RETRY_DELAY, FILTER_HOOK, SERDES_USING_METASTORE_FOR_SCHEMA, SERVER_*_THREADS, 

> THREAD_POOL_SIZE

> Security: EXECUTE_SET_UGI, metastore.authorization.storage.checks

> Setting up Caching: CACHED*, CATALOGS_TO_CACHE & AGGREGATE_STATS_CACHE*

> Transactions: MAX_OPEN_TXNS, TXNS_*