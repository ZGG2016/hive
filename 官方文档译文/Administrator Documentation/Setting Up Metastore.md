# AdminManual Metastore Administration

This page only documents the MetaStore in Hive 2.x and earlier. For 3.x and later releases please see [AdminManual Metastore 3.0 Administration](https://github.com/ZGG2016/hive-website/blob/master/Administrator%20Documentation/AdminManual%20Metastore%203.0%20Administration.md)

## 1、Introduction

> All the metadata for Hive tables and partitions are accessed through the Hive Metastore. Metadata is persisted using [JPOX](http://www.datanucleus.org/) ORM solution (Data Nucleus) so any database that is supported by it can be used by Hive. Most of the commercial relational databases and many open source databases are supported. See the list of [supported databases](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+Administration#AdminManualMetastoreAdministration-SupportedBackendDatabasesforMetastore) in section below.

Hive 表和分区的所有元数据都是通过 Hive Metastore 访问的。

元数据使用 JPOX ORM 方法(Data Nucleus)进行持久化，因此 Hive 可以使用它支持的任何数据库。支持大多数商业关系数据库和许多开源数据库。

请参阅下面一节中支持的数据库列表。

> You can find an E/R diagram for the metastore [here](https://issues.apache.org/jira/secure/attachment/12471108/HiveMetaStore.pdf).

可以在这里找到元存储的 E/R 图。

> There are 2 different ways to setup the metastore server and metastore database using different Hive configurations:

使用不同的 Hive 配置，有两种不同的方式来设置元存储服务器和元存储数据库:

> Configuration options for **metastore database** where metadata is persisted:

元数据持久化的元存储数据库的配置选项:

- Local/Embedded Metastore Database (Derby)
- Remote Metastore Database

> Configuration options for **metastore server**:

元存储服务器的配置选项:

- Local/Embedded Metastore Server 
- Remote Metastore Server

### 1.1、Basic Configuration Parameters

> The relevant configuration parameters are shown here. (Non-metastore parameters are described in [Configuring Hive](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration). Also see the Language Manual's [Hive Configuration Properties](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties), including [Metastore](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-MetaStore) and [Hive Metastore Security](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-HiveMetastoreSecurity).)

相关的配置参数显示在这里。

(非元存储参数请参见 Configuring Hive。

也可以查看语言手册中的 Hive Configuration Properties，包括 Metastore 和 Hive Metastore Security。)

> Also see hivemetastore-site.xml documentation under [Configuring Hive](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration).

也可参阅 Configuring Hive 下的 hivemetastore-site.xml 文档。

Configuration Parameter          |    Description
---|:---
hive.metastore.local             |    local or remote metastore (removed as of Hive [0.10](https://issues.apache.org/jira/browse/HIVE-2585): If hive.metastore.uris is empty local mode is assumed, remote otherwise)【本地或远程元存储（在Hive 0.10移除：如果`hive.metastore.uris`是空，就是本地模式，否则是远程模式。）】
hive.metastore.uris              |    Hive connects to one of these URIs to make metadata requests to a remote Metastore (comma separated list of URIs)【Hive连接这些URI中的一个，来向远程元存储发起元数据请求（逗号分隔的URIs的列表）】
hive.metastore.warehouse.dir     |    URI of the default location for native tables【原生表的默认路径的URI】
javax.jdo.option.ConnectionDriverName   |  JDBC Driver class name for the data store which contains metadata【包含元数据的数据存储的JDBC驱动程序类名】
javax.jdo.option.ConnectionURL   |    JDBC connection string for the data store which contains metadata【包含元数据的数据存储的JDBC连接字符串】

> The Hive metastore is stateless and thus there can be multiple instances to achieve High Availability. Using hive.metastore.uris it is possible to specify multiple remote metastores. Hive will use the first one from the list by default but will pick a random one on connection failure and will try to reconnect.

Hive 元存储是无状态的，因此可以有多个实例来实现高可用性。使用 `hive.metastore.uris` 可以指定多个远程元存储。Hive 将默认使用列表中的第一个，但在连接失败时随机选择一个，并尝试重新连接。

### 1.2、Additional Configuration Parameters

> The following metastore configuration parameters were carried over from old documentation without a guarantee that they all still exist. See the HiveConf Java class for current Hive configuration options, and see the [Metastore](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-MetaStore) and [Hive Metastore Security](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-HiveMetastoreSecurity) sections of the Language Manual's [Hive Configuration Properties](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties) for user-friendly descriptions of the metastore parameters.

下面的元存储配置参数是从旧文档中继承过来的，并没有保证它们都仍然存在。参考 HiveConf Java 类了解当前的 Hive 配置选项，参考语言手册的 Hive Configuration Properties 的 Metastore 和 Hive Metastore Security 部分了解对用户友好的 Metastore 参数的描述。


Configuration Parameter     |    Description   |  Default Value
---|:---|:---
hive.metastore.metadb.dir   |  The location of filestore metadata base directory. (Functionality removed in 0.4.0 with [HIVE-143](https://issues.apache.org/jira/browse/HIVE-143).) 【文件存储元数据基本目录的位置】  |
hive.metastore.rawstore.impl|  Name of the class that implements the `org.apache.hadoop.hive.metastore.rawstore` interface. This class is used to store and retrieval of raw metadata objects such as table, database. (Hive 0.8.1 and later.)【实现`org.apache.hadoop.hive.metastore.rawstore`接口的类名。该类用于存储和检索原始元数据对象，如表、数据库。】  |
org.jpox.autoCreateSchema   |  Creates necessary schema on startup if one doesn't exist. (The schema includes tables, columns, and so on.) Set to false after creating it once.【如果不存在一个schema，那么在启用时创建一个。（schema包含表、列等）创建一次后设置为false。】  |
org.jpox.fixedDatastore     |   Whether the datastore schema is fixed. 【数据存储schema是否是固定的】 |
datanucleus.autoStartMechanism | Whether to initialize on startup. 【是否在启动时初始化】   | 
hive.metastore.ds.connection.url.hook | Name of the hook to use for retriving the JDO connection URL. If empty, the value in `javax.jdo.option.ConnectionURL` is used as the connection URL. (Hive 0.6 and later.)【用于检索JDO连接URL的钩子的名称。如果为空，则`javax.jdo.option.ConnectionURL`中的值被用作连接URL。】   |
hive.metastore.ds.retry.attempts  |  The number of times to retry a call to the backing datastore if there were a connection error.(Hive 0.6 through 0.12; removed in 0.13.0 – use `hive.hmshandler.retry.attempts` instead.)【如果出现连接错误，重试调用备份数据存储的次数。】    |  1
hive.metastore.ds.retry.interval   |  The number of miliseconds between datastore retry attempts.(Hive 0.6 through 0.12; removed in 0.13.0 – use `hive.hmshandler.retry.interval` instead.)  在数据存储重试尝试间的毫秒数  |   1000
hive.metastore.server.min.threads  |   Minimum number of worker threads in the Thrift server's pool.(Hive 0.6 and later.) 【在Thrift server's pool中worker线程的最小数量】   |  200
hive.metastore.server.max.threads  |   Maximum number of worker threads in the Thrift server's pool.(Hive 0.6 and later.) 【在Thrift server's pool中worker线程的最大数量】   |  100000 since Hive 0.8.1
hive.metastore.filter.hook   |  Metastore hook class for further filtering the metadata read results on client side.([Hive 1.1.0](https://issues.apache.org/jira/browse/HIVE-8612) and later.)【元存储钩子类，用于在客户端进一步过滤元数据读结果。】   |   org.apache.hadoop.hive.metastore.DefaultMetaStoreFilterHookImpl
hive.metastore.port   |   Hive metastore listener port.([Hive 1.3.0](https://issues.apache.org/jira/browse/HIVE-9365) and later.) 【hive元存储监听的端口】 |  9083

### 1.3、Data Nucleus Auto Start

**Configuring datanucleus.autoStartMechanism is highly recommended**

强烈建议配置`datanucleus.autoStartMechanism`

> Configuring auto start for data nucleus is highly recommended. See [HIVE-4762](https://issues.apache.org/jira/browse/HIVE-4762) for more details.

```xml
<property>
   <name>datanucleus.autoStartMechanism</name>
   <value>SchemaTable</value>
 </property>
```

### 1.4、Default Configuration

> The default configuration sets up an embedded metastore which is used in unit tests and is described in the next section. More practical options are described in the subsequent sections.

默认配置设置嵌入式元存储，用于单元测试，下一节将对此进行描述。更实用的选项将在后面的章节中描述。

## 2、Local/Embedded Metastore Database (Derby)

> An embedded metastore database is mainly used for unit tests. Only one process can connect to the metastore database at a time, so it is not really a practical solution but works well for unit tests.

嵌入式的元存储数据库主要用于单元测试。一次只能有一个进程连接元存储数据库，所以这不是一个实际的解决方案，但对于单元测试来说工作得很好。

> For unit tests Local/Embedded Metastore Server configuration for the metastore server is used in conjunction with embedded database.

对于单元测试，元存储服务器的本地/嵌入式元存储服务器配置与嵌入式数据库一起使用。

> Derby is the default database for the embedded metastore.

Derby 是嵌入式元存储的默认数据库。

Config Param                    |    Config Value     |    Comment
---|:---|:---
javax.jdo.option.ConnectionURL  | `jdbc:derby:;databaseName=../build/test/junit_metastore_db;create=true` |  Derby database located at hive/trunk/build...【Derby数据库位于`hive/trunk/build...`】
javax.jdo.option.ConnectionDriverName | org.apache.derby.jdbc.EmbeddedDriver  |  Derby embeded JDBC driver class.
hive.metastore.warehouse.dir  | file://${user.dir}/../build/ql/test/data/warehouse | Unit test data goes in here on your local filesystem.【单元测试数据在本地文件系统中。】

> If you want to run Derby as a network server so the metastore can be accessed from multiple nodes, see [Hive Using Derby in Server Mode](https://cwiki.apache.org/confluence/display/Hive/HiveDerbyServerMode).

如果你希望将 Derby 作为网络服务器运行，以便可以从多个节点访问元存储，请参阅 Hive Using Derby in server Mode。

## 3、Remote Metastore Database

> In this configuration, you would use a traditional standalone RDBMS server. The following example configuration will set up a metastore in a MySQL server. This configuration of metastore database is recommended for any real use.

在这种配置中，你将使用传统的 standalone RDBMS 服务器。

下面的示例配置将在 MySQL 服务器中建立一个元存储。对于任何实际使用，建议使用元存储数据库的这种配置。

Config Param                    |    Config Value     |    Comment
---|:---|:---
javax.jdo.option.ConnectionURL  |  `jdbc:mysql://<host name>/<database name>`?createDatabaseIfNotExist=true  |  metadata is stored in a MySQL server【元数据存储在mysql服务器】
javax.jdo.option.ConnectionDriverName|  com.mysql.jdbc.Driver | MySQL JDBC driver class
javax.jdo.option.ConnectionUserName  |  `<user name>`   | user name for connecting to MySQL server【连接mysql服务器的用户名】
javax.jdo.option.ConnectionPassword  | `<password>`     | password for connecting to MySQL server【连接mysql服务器的密码】

## 4、Local/Embedded Metastore Server

> In local/embedded metastore setup, the metastore server component is used like a library within the Hive Client. Each Hive Client will open a connection to the database and make SQL queries against it. Make sure that the database is accessible from the machines where Hive queries are executed since this is a local store. Also make sure the JDBC client library is in the classpath of Hive Client. This configuration is often used with HiveServer2 (to use embedded metastore only with HiveServer2 add "--hiveconf hive.metastore.uris=' '" in command line parameters of the hiveserver2 start command or use hiveserver2-site.xml (available in Hive 0.14)).

在本地/嵌入式元存储设置中，元存储服务器组件像 Hive 客户端中的库一样使用。

每个 Hive 客户端将打开一个到数据库的连接，并对其进行 SQL 查询。确保数据库可以从执行 Hive 查询的机器上访问，因为这是一个本地存储。

还要确保 JDBC 客户端库在 Hive 客户端的类路径中。此配置通常与 HiveServer2 一起使用(嵌入式元存储仅与 HiveServer2 一起使用，在 hiveserver2 的启动命令的命令行参数中添加 "--hiveconf hive.metastore.uris=' '" 或使用 hiveserver2-site.xml)。

Config Param           |    Config Value     |    Comment
---|:---|:---
hive.metastore.uris    |   not needed because this is local store【这里不需要，因为是本地存储】  |  
hive.metastore.local   |       true          |   this is local store (removed in Hive 0.10, see [configuration description](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+Administration#AdminManualMetastoreAdministration-BasicConfigurationParameters) section)【这是本地存储】
hive.metastore.warehouse.dir  |   `<base hdfs path>`  |   Points to default location of non-external Hive tables in HDFS.【指向hdfs中非外部hive表的默认路径】

## 5、Remote Metastore Server

> In remote metastore setup, all Hive Clients will make a connection to a metastore server which in turn queries the datastore (MySQL in this example) for metadata. Metastore server and client communicate using [Thrift](http://incubator.apache.org/thrift) Protocol. Starting with Hive 0.5.0, you can start a Thrift server by executing the following command:

在远程元存储设置中，所有 Hive 客户端都会连接到元存储服务器，客户端依次查询数据存储(本例中是MySQL)的元数据。元存储服务器和客户端使用 Thrift 协议通信。

从 Hive 0.5.0 开始，可以执行以下命令启动 Thrift 服务:

	hive --service metastore

> In versions of Hive earlier than 0.5.0, it's instead necessary to run the Thrift server via direct execution of Java:

在 Hive 0.5.0 之前的版本中，有必要通过 Java 直接运行 Thrift 服务:

	$JAVA_HOME/bin/java  -Xmx1024m -Dlog4j.configuration=file://$HIVE_HOME/conf/hms-log4j.properties -Djava.library.path=$HADOOP_HOME/lib/native/Linux-amd64-64/ -cp $CLASSPATH org.apache.hadoop.hive.metastore.HiveMetaStore

> If you execute Java directly, then JAVA_HOME, HIVE_HOME, HADOOP_HOME must be correctly set; CLASSPATH should contain Hadoop, Hive (lib and auxlib), and Java jars.

如果你直接指向 Java，那么 `JAVA_HOME`、`HIVE_HOME`、`HADOOP_HOME`必须正确的设置。

CLASSPATH 应该包含 Hadoop、Hive(lib and auxlib) 和 Java jars。

### 5.1、Server Configuration Parameters

> The following example uses a Remote Metastore Database.

下面的示例使用一个远程元存储数据库：

Config Param           |    Config Value     |    Comment
---|:---|:---
javax.jdo.option.ConnectionUserName  | `<user name>`  | user name for connecting to MySQL server【连接mysql服务器的用户名】
javax.jdo.option.ConnectionURL       |  `jdbc:mysql://<host name>/<database name>?createDatabaseIfNotExist=true` |  metadata is stored in a MySQL server【元数据存储在mysql服务器】
javax.jdo.option.ConnectionPassword  |  `<password>`  | password for connecting to MySQL server【连接mysql服务器的密码】
javax.jdo.option.ConnectionDriverName | com.mysql.jdbc.Driver | MySQL JDBC driver class
hive.metastore.warehouse.dir  |  `<base hdfs path>`  | default location for Hive tables.【hive表的默认路径】
hive.metastore.thrift.bind.host	| `<host_name>`	  |  Host name to bind the metastore service to. When empty, "localhost" is used. This configuration is available Hive 4.0.0 onwards.【要绑定元存储服务器的主机名。当为空时，使用“localhost”。此配置在Hive4.0.0以后可用。】

> From Hive 3.0.0 ([HIVE-16452](https://issues.apache.org/jira/browse/HIVE-16452)) onwards the metastore database stores a GUID which can be queried using the Thrift API get_metastore_db_uuid by metastore clients in order to identify the backend database instance. This API can be accessed by the HiveMetaStoreClient using the method getMetastoreDbUuid().

从 Hive 3.0.0 开始，元存储数据库存储一个 GUID，这个 GUID 可以被元存储客户端通过 Thrift API get_metaore_db_uuid 查询，来识别后端数据库实例。

HiveMetaStoreClient 可以使用 getmetaoredbuuid() 方法访问这个 API。

### 5.2、Client Configuration Parameters

Config Param           |    Config Value     |    Comment
---|:---|:---
hive.metastore.uris  |  `thrift://<host_name>:<port>`  |  host and port for the Thrift metastore server. If hive.metastore.thrift.bind.host is specified, host should be same as that configuration. Read more about this in dynamic service discovery configuration parameters.【Thrift 元存储服务器的主机和端口。如果指定了`hive.metastore.thrift.bind.host`，则主机应与该配置一致。在动态服务发现配置参数中了解更多信息。】
hive.metastore.local |  false  | Metastore is remote.  Note: This is no longer needed as of Hive 0.10.  Setting hive.metastore.uri is sufficient.【元存储是远程的】
hive.metastore.warehouse.dir  | `<base hdfs path>`  | Points to default location of non-external Hive tables in HDFS.【指向hdfs中非外部hive表的默认路径】

### 5.3、Dynamic Service Discovery Configuration Parameters

> From Hive 4.0.0 (HIVE-20794) onwards, similar to HiveServer2, a ZooKeeper service can be used for dynamic service discovery of a remote metastore server. Following parameters are used by both metastore server and client.

从 Hive 4.0.0 开始，类似于 HiveServer2，ZooKeeper 服务可以用于远程元存储服务器的动态发现。

动态发现服务器和客户端都使用以下参数

Config Param           |    Config Value     |    Comment
---|:---|:---
hive.metastore.service.discovery.mode	|   service discovery mode	| When it is set to "zookeeper", ZooKeeper is used for dynamic service discovery of a remote metastore. In that case, a metastore adds itself to the ZooKeeper when it is started and removes itself when it shuts down. By default it is empty. Both the client and server should have same value for this parameter.【当设置为“zookeeper”时，zookeeper用于远程元存储的动态服务发现。在这种情况下，元存储在启动时将自己添加到ZooKeeper中，在关闭时删除自己。默认是空。对于这个参数，客户端和服务器应该具有相同的值】
hive.metastore.uris	 | `<host_name>:<port>`,`<host_name>:<port>`, ... |	One or more host and port pairs of ZooKeeper servers forming a ZooKeeper ensemble. Used when hive.metastore.service.discovery.mode is set to "zookeeper". The configuration is not used by server otherwise. If all the servers are using the same port you may specify the port using hive.metastore.zookeeper.client.port instead of specifying it with every server separately. Both the client and server should have same value for this parameter.【构成ZooKeeper集成的一个或多个ZooKeeper服务器的主机和端口对。在`hive.metastore.service.discovery.mode`设置为“zookeeper”时使用。否则该配置不会被服务器使用。如果所有的服务器使用相同的端口，你可以使用`hive.metastore.zookeeper.client.port`指定端口，而不是为每个服务器单独指定。对于这个参数，客户端和服务器应该具有相同的值。】
hive.metastore.zookeeper.client.port | `<port>`  |	Port number when same port number is used by all the ZooKeeper servers in the ensemble. Both the client and server should have same value for this parameter.【当集成中的所有ZooKeeper服务器使用相同的端口号时的端口号。对于这个参数，客户端和服务器应该具有相同的值。】
hive.metastore.zookeeper.namespace  |  `<namespace name>`  |  The parent node under which all ZooKeeper nodes for metastores are created.【创建所有元存储的ZooKeeper节点的父节点。】
hive.metastore.zookeeper.session.timeout  |  `<time in milliseconds>`	|   ZooKeeper client's session timeout (in milliseconds). The client is disconnected if a heartbeat is not sent in the timeout.【ZooKeeper客户端会话超时时间(毫秒)。如果在超时时间内没有发送心跳，客户端将断开连接。】
hive.metastore.zookeeper.connection.timeout  |  `<time in seconds>`	  |  ZooKeeper client's connection timeout in seconds. Connection timeout * hive.metastore.zookeeper.connection.max.retries with exponential backoff is when curator client deems connection is lost to zookeeper.【ZooKeeper客户端连接超时时间(秒)。。。。】
hive.metastore.zookeeper.connection.max.retries | `<number>`  | Max number of times to retry when connecting to the ZooKeeper server.【当连接ZooKeeper服务器时的最大重试次数。】
hive.metastore.zookeeper.connection.basesleeptime  | `<time in milliseconds>`  |  Initial amount of time (in milliseconds) to wait between retries when connecting to the ZooKeeper server when using ExponentialBackoffRetry policy.【使用ExponentialBackoffRetry策略连接ZooKeeper服务器时，在两次重试之间等待的初始时间(毫秒)。】

> If you are using MySQL as the datastore for metadata, put MySQL jdbc libraries in HIVE_HOME/lib before starting Hive Client or HiveMetastore Server.

如果你使用 MySQL 作为元数据的数据存储，请在启动 Hive Client 或 Hive metastore Server 之前将 MySQL jdb 库放在 HIVE_HOME/lib 中。

> To change the metastore port, use this hive command:

修改元存储端口，使用 hive 命令:

	hive --service metastore -p <port_num>

## 6、Supported Backend Databases for Metastore

DataBase   |   Minimum Supported Version   |  Name for Parameter  | See Also
---|:---|:---|:---
MySQL	   |            5.6.17	           |         mysql	      |
Postgres   |            9.1.13	           |         postgres     |
Oracle	   |              11g	           |         oracle	      |[hive.metastore.orm.retrieveMapNullsAsEmptyStrings](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.metastore.orm.retrieveMapNullsAsEmptyStrings)
MS SQL Server |	         2008 R2	       |          mssql	      |

## 7、Metastore Schema Consistency and Upgrades

> Version:Introduced in Hive 0.12.0. See [HIVE-3764](https://issues.apache.org/jira/browse/HIVE-3764).

> Hive now records the schema version in the metastore database and verifies that the metastore schema version is compatible with Hive binaries that are going to accesss the metastore. Note that the Hive properties to implicitly create or alter the existing schema are disabled by default. Hive will not attempt to change the metastore schema implicitly. When you execute a Hive query against an old schema, it will fail to access the metastore.

Hive 现在会在元存储数据库中记录 schema 版本，并验证元存储 schema 版本是否与将要访问该元存储的 Hive 二进制文件兼容。

注意，默认情况下，用于隐式创建或更改现有 schema 的 Hive 属性是禁用的。Hive 不会隐式修改元存储架构。当你对 schema 执行 Hive 查询时，将无法访问元存储。

> To suppress the schema check and allow the metastore to implicitly modify the schema, you need to set a configuration property hive.metastore.schema.verification to false in hive-site.xml.

要禁止 schema 检查，并允许元存储隐式地修改 schema，需要在 hive-site.xml 中设置一个配置属性`hive.metastore.schema.verification` 为 false。

> Starting in release 0.12, Hive also includes an off-line schema tool to initialize and upgrade the metastore schema. Please refer to the details [here](https://cwiki.apache.org/confluence/display/Hive/Hive+Schema+Tool).

从 0.12 版开始，Hive 还包含了一个离线 schema 工具，用于初始化和升级元存储 schema。详情请参阅此处。