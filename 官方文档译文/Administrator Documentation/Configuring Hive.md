# AdminManual Configuration

[TOC]

## 1、Configuring Hive

> A number of configuration variables in Hive can be used by the administrator to change the behavior for their installations and user sessions. These variables can be configured in any of the following ways, shown in the order of preference:

管理员可以使用 Hive 中的许多配置变量来更改安装和用户会话的行为。这些变量可以用以下任意一种方式配置，按优先顺序显示:

> Using the set command in the [CLI](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli) or [Beeline](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-BeelineHiveCommands) for setting session level values for the configuration variable for all statements subsequent to the set command. For example, the following command sets the scratch directory (which is used by Hive to store temporary output and plans) to /tmp/mydir for all subsequent statements:

- **在 CLI 或 Beeline 中，使用 `set` 命令设置子会话级别的配置变量值，对 `set` 命令之后的所有命令有效**。例如:

		set hive.exec.scratchdir=/tmp/mydir;

> Using the --hiveconf option of the [hive](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveCommandLineOptions) command (in the CLI) or [beeline](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-BeelineCommandOptions) command for the entire session. For example:

- **使用 `hive` 命令或 `beeline` 命令的 `--hiveconf` 选项设置，对整个会话有效**。例如：

		bin/hive --hiveconf hive.exec.scratchdir=/tmp/mydir

> In hive-site.xml. This is used for setting values for the entire Hive configuration (see [hive-site.xml and hive-default.xml.template](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-hive-site.xmlandhive-default.xml.template) below). For example:

- **在 `hive-site.xml` 中设置。这是对整个 hive 配置有效**。例如：

```xml
<property>
	<name>hive.exec.scratchdir</name>
    <value>/tmp/mydir</value>
    <description>Scratch space for Hive jobs</description>
</property>
```

> In server-specific configuration files (supported starting [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-7342)). You can set metastore-specific configuration values in hivemetastore-site.xml, and HiveServer2-specific configuration values in hiveserver2-site.xml.
The server-specific configuration file is useful in two situations:

- **在服务器特定的配置文件中设置**。你可以在 `hivemetastore-site.xml`中设置 metastore 的配置值，在 `hiveserver2-site.xml` 中设置 hiveserver2 的配置值。在服务器特定的配置文件中设置在两种情况下有用:

	- 你想要对一种类型的服务器有不同的配置。(如，在HiveServer2中启用授权，在CLI中不启用)

	- 你想要在服务器特定的配置文件中设置一个配置值。(如，仅在metastore server配置文件中设置metastore数据库密码)

> You want a different configuration for one type of server (for example – enabling authorization only in HiveServer2 and not CLI).

> You want to set a configuration value only in a server-specific configuration file (for example – setting the metastore database password only in the metastore server configuration file).

> HiveMetastore server reads hive-site.xml as well as hivemetastore-site.xml configuration files that are available in the $HIVE_CONF_DIR or in the classpath. If the metastore is being used in embedded mode (i.e., hive.metastore.uris is not set or empty) in hive commandline or HiveServer2, the hivemetastore-site.xml gets loaded by the parent process as well.
The value of hive.metastore.uris is examined to determine this, and the value should be set appropriately in hive-site.xml .
Certain [metastore configuration parameters](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-MetaStore) like hive.metastore.sasl.enabled, hive.metastore.kerberos.principal, hive.metastore.execute.setugi, and hive.metastore.thrift.framed.transport.enabled are used by the metastore client as well as server. For such common parameters it is better to set the values in hive-site.xml, that will help in keeping them consistent.

**HiveMetastore 服务器读取在 `$HIVE_CONF_DIR` 或 classpath 中的 `hivemetastore-site.xml` 和 `hive-site.xml`**。

如果 metastore 以嵌入式模式的形式(如`hive.metastore.uris`未设置或为空)，在 hive 命令行或 HiveServer2 中使用，父进程也会加载 `hivemetastore-site.xml`。

检查 `hive.metastore.uris` 的值来确定这一点，并且应该在`hive-site.xml`中适当地设置该值。

某些 metastore 配置参数被客户端和服务器使用，如`hive.metastore.sasl.enabled`、`hive.metastore.kerberos.principal`、`hive.metastore.execute.setugi`、`hive.metastore.thrift.framed.transport.enabled`。

对于这类通用的参数，最好在 `hive-site.xml` 中设置值，这将有助于保持它们的一致性。

> HiveServer2 reads hive-site.xml as well as hiveserver2-site.xml that are available in the $HIVE_CONF_DIR or in the classpath. 
If HiveServer2 is using the metastore in embedded mode, hivemetastore-site.xml also is loaded.

HiveServer2 读取在 `$HIVE_CONF_DIR` 或 classpath 中的 `hivemetastore-site.xml` 和 `hive-site.xml`。如果 HiveServer2 以嵌入式模式使用 metastore，也会加载 `hivemetastore-site.xml`。

> The order of precedence of the config files is as follows (later one has higher precedence) –
hive-site.xml -> hivemetastore-site.xml -> hiveserver2-site.xml -> '-hiveconf' commandline parameters.

配置文件的优先级顺序如下(后面的文件优先级更高)：

	hive-site.xml -> hivemetastore-site.xml -> hiveserver2-site.xml -> '-hiveconf' commandline parameters

### 1.1、hive-site.xml and hive-default.xml.template

> hive-default.xml.template contains the default values for various configuration variables that come prepackaged in a Hive distribution. In order to override any of the values, create hive-site.xml instead and set the value in that file as shown above.

`hive-default.xml.template` 包含了预先打包在 Hive 发行版中的各种配置变量的默认值。为了覆盖任意值，创建 `hive-site.xml`， 并在该文件中设置值，如上所示。

> hive-default.xml.template is located in the conf directory in your installation root, and hive-site.xml should also be created in the same directory.

`hive-default.xml.template` 在安装的根目录下的 `conf` 目录下，`hive-site.xml` 也应在相同的目录下创建。

> Please note that the template file hive-default.xml.template is not used by Hive at all (as of Hive 0.9.0) – the canonical list of configuration options is only managed in the HiveConf java class. The template file has the formatting needed for hive-site.xml, so you can paste configuration variables from the template file into hive-site.xml and then change their values to the desired configuration.

请注意，模板文件 `hive-default.xml.template` 不会被 Hive 使用(从Hive 0.9.0开始)--**规范的配置选项列表只在 `HiveConf` java 类中管理**。

模板文件中具有 `hive-site.xml` 所需的格式。这样你就可以将配置变量从模板文件中粘贴到`hive-site.xml` 中，然后将它们的值更改为所需的配置。

> In Hive releases 0.9.0 through 0.13.1, the template file does not necessarily contain all configuration options found in HiveConf.java and some of its values and descriptions might be out of date or out of sync with the actual values and descriptions. However, as of [Hive 0.14.0](https://issues.apache.org/jira/browse/HIVE-6037) the template file is generated directly from HiveConf.java and therefore it is a reliable source for configuration variables and their defaults.

在 Hiv 0.9.0 到 0.13.1 版本中，模板文件不一定包含 `HiveConf.java` 中所有的配置选项，并且它的一些值和描述可能已经过时或者与实际的值和描述不同步。然而，从 Hive 0.14.0 开始，模板文件是直接从 `HiveConf.java`  生成的，因此它是配置变量及其默认值的可靠来源。

> The administrative configuration variables are listed [below](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-ConfigurationVariables). User variables are listed in [Hive Configuration Properties](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties). As of Hive 0.14.0 you can display information about a configuration variable with the [SHOW CONF command](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-ShowConf).

下面列出了管理员配置变量。用户变量列在 Hive Configuration Properties 中。从 Hive 0.14.0 开始，你可以用 `SHOW CONF`命令显示配置变量的信息。

### 1.2、Temporary Folders

> Hive uses temporary folders both on the machine running the Hive client and the default HDFS instance. These folders are used to store per-query temporary/intermediate data sets and are normally cleaned up by the hive client when the query is finished. However, in cases of abnormal hive client termination, some data may be left behind. The configuration details are as follows:

**Hive 同时在 Hive 客户端和默认 HDFS 实例的机器上运行时，使用临时文件夹**。

这些文件夹**用于存储每个查询的临时或中间数据集**，通常在查询完成时由 hive 客户端清理。然而，在异常的 hive 客户端终止的情况下，可能会留下一些数据。配置细节如下:

- 在 HDFS 集群上，默认设置为 `/tmp/hive-<username>`，由配置变量 `hive.exec.scratchdir` 控制。

- 在客户端机器上，它被硬编码为 `/tmp/<username>`。

> On the HDFS cluster this is set to /tmp/hive-<username> by default and is controlled by the configuration variable hive.exec.scratchdir

> On the client machine, this is hardcoded to /tmp/<username>

> Note that when writing data to a table/partition, Hive will first write to a temporary location on the target table's filesystem (using hive.exec.scratchdir as the temporary location) and then move the data to the target table. This applies in all cases - whether tables are stored in HDFS (normal case) or in file systems like S3 or even NFS.

注意，**当向表/分区写入数据时，Hive 将首先写入目标表所在的文件系统上的临时位置(使用`hive.exec.scratchdir`作为临时路径)。然后将数据移动到目标表中**。这适用于所有情况--无论表存储在 HDFS(普通情况)中，还是存储在 S3 甚至 NFS 等文件系统中。

### 1.3、Log Files

> Hive client produces logs and history files on the client machine. Please see [Hive Logging](https://cwiki.apache.org/confluence/display/Hive/GettingStarted#GettingStarted-HiveLogging) for configuration details.

Hive 客户端在客户端机器上产生日志和历史文件。配置的详细内容请见 Hive Logging

> For WebHCat logs, see [Log Files](https://cwiki.apache.org/confluence/display/Hive/WebHCat+UsingWebHCat#WebHCatUsingWebHCat-LogFiles) in the [WebHCat manual](https://cwiki.apache.org/confluence/display/Hive/WebHCat).

对于 WebHCat 日志，见 WebHCat manual 中 Log Files 。

### 1.4、Derby Server Mode

> [Derby](http://db.apache.org/derby/) is the default database for the Hive metastore ([Metadata Store](https://cwiki.apache.org/confluence/display/Hive/GettingStarted#GettingStarted-MetadataStore)). To run Derby as a network server for multiple users, see [Hive Using Derby in Server Mode](https://cwiki.apache.org/confluence/display/Hive/HiveDerbyServerMode).

Derby 是 Hive metastore 的默认数据库。为了将 Derby 为多个用户作为网络服务器，见 Hive Using Derby in Server Mode。

### 1.5、Configuration Variables

> Broadly the configuration variables for Hive administration are categorized into:

一般来说，Hive管理的配置变量可以分为以下几类:

- Hive 配置变量
- Hive Metastore 配置变量
- 用来和 Hadoop 交互的配置变量
- 用于传递运行时信息的 Hive 变量

> [Hive Configuration Variables](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-HiveConfigurationVariables)

> [Hive Metastore Configuration Variables](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-HiveMetastoreConfigurationVariables)

> [Configuration Variables Used to Interact with Hadoop](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-ConfigurationVariablesUsedtoInteractwithHadoop)

> [Hive Variables Used to Pass Run Time Information](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-HiveVariablesUsedtoPassRunTimeInformation)

> Also see [Hive Configuration Properties](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties) in the [Language Manual](https://cwiki.apache.org/confluence/display/Hive/LanguageManual) for non-administrative configuration variables.

见 Language Manual 中的 Hive Configuration Properties，描述非管理员配置变量。

> Version information: Metrics

> A new Hive metrics system based on Codahale is introduced in releases 1.3.0 and 2.0.0 by [HIVE-10761](https://issues.apache.org/jira/browse/HIVE-10761). To configure it or revert to the old metrics system, see the Metrics section of [Hive Configuration Properties](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-Metrics).

在 Hive 1.3.0 和 2.0.0版本中，引入了一个新的基于 Codahale 的 Hive 度量系统。若要配置它或恢复到旧的度量系统，请参阅 Hive Configuration Properties 的 Metrics 部分。

#### 1.5.1、Hive Configuration Variables

Variable Name  |  Description  | Deafult Value
---|:---|:---
hive.ddl.output.format  |  The data format to use for DDL output (e.g. DESCRIBE table). One of "text" (for human readable text) or "json" (for a json object). (As of Hive 0.9.0.)【DDL输出(DESCRIBE table)的数据格式，人类可读的"text"的一种，或"json"】  |  text
hive.exec.script.wrapper  |  Wrapper around any invocations to script operator e.g. if this is set to python, the script passed to the script operator will be invoked as `python <script command>`. If the value is null or not set, the script is invoked as `<script command>`.【封装任何对脚本操作符调用，例如，如果这个设置为python，传递给脚本操作符的脚本将作为`python <script command>`，如果值是null或不设置，`<script command>`调用脚本。】  |  null
hive.exec.plan   |    |  null
hive.exec.scratchdir  |  This directory is used by Hive to store the plans for different map/reduce stages for the query as well as to stored the intermediate outputs of these stages.Hive 0.14.0 and later: HDFS root scratch directory for Hive jobs, which gets created with write all (733) permission. For each connecting user, an HDFS scratch directory `${hive.exec.scratchdir}/<username>` is created with ${hive.scratch.dir.permission}. 【Hive使用这个目录来存储不同的map/reduce stages的计划，以及存储这些阶段的中间输出。对于Hive 0.14.0及更高版本，用于Hive jobs的HDFS根scratch目录，使用写全部(733)权限创建。对于每个连接的用户，一个HDFS scratch目录使用`${hive.scratch.dir.permission}`创建`${hive.exec.scratchdir}/<username>`。】 |  `/tmp/<user.name>/hive` (Hive 0.8.0 and earlier)、`/tmp/hive-<user.name>` (as of Hive 0.8.1 to 0.14.0)、`/tmp/hive` (Hive 0.14.0 and later)
hive.scratch.dir.permission  |  The permission for the user-specific scratch directories that get created in the root scratch directory ${hive.exec.scratchdir}. (As of Hive 0.12.0.)【在根scratch目录`${hive.exec.scratchdir}`中创建的用户指定的scratch目录的权限】  |  700 (Hive 0.12.0 and later)
hive.exec.local.scratchdir  |  This directory is used for temporary files when Hive runs in local mode. (As of Hive 0.10.0.) 【当Hive运行在本地模式下时，用作临时目录的目录】 |  `/tmp/<user.name>`
hive.exec.submitviachild  |  Determines whether the map/reduce jobs should be submitted through a separate jvm in the non local mode.【当Hive运行在非本地模式下时，决定map/reduce jobs是否应该通过一个独立的jvm提交】  |  false - By default jobs are submitted through the same jvm as the compiler【默认在同一jvm下提交。】
hive.exec.script.maxerrsize  |  Maximum number of serialization errors allowed in a user script invoked through TRANSFORM or MAP or REDUCE constructs. 【通过TRANSFORM/MAP/REDUCE调用的用户脚本中允许的序列化错误的最大数量】 |  100000
hive.exec.compress.output  |  Determines whether the output of the final map/reduce job in a query is compressed or not.【是否压缩最终的map/reduce job的输出】  |  false
hive.exec.compress.intermediate  |  Determines whether the output of the intermediate map/reduce jobs in a query is compressed or not.【是否压缩map/reduce job的中间结果的输出】  |  false
hive.resource.use.hdfs.location	  |  Reference HDFS based files/jars directly instead of copying to session based HDFS scratch directory. (As of Hive [2.2.1](https://issues.apache.org/jira/browse/HIVE-17574).) 【直接引用基于files/jars的HDFS，而不是复制到基于HDFS scratch目录的会话。】 |  true
hive.jar.path  |  The location of hive_cli.jar that is used when submitting jobs in a separate jvm.【当在一个独立的jvm提交jobs时，使用的hive_cli.jar的路径】  |  
hive.aux.jars.path  |  The location of the plugin jars that contain implementations of user defined functions and SerDes.【包含了用户定义函数和SerDes的plugin jars的路径】  |  
hive.reloadable.aux.jars.path  |  The location of plugin jars that can be renewed (added, removed, or updated) by executing the [Beeline reload command](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-BeelineHiveCommands), without having to restart HiveServer2. These jars can be used just like the auxiliary classes in hive.aux.jars.path [for creating UDFs or SerDes](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.aux.jars.path).【可以通过执行Beeline reload命令，能被重新设置的plugin jars的路径，而不用重启HiveServer2。这些jars可以像`hive.aux.jars.path`中的辅助类一样使用。】 (As of Hive [0.14.0](https://issues.apache.org/jira/browse/HIVE-7553).)  |  
hive.partition.pruning  |  A strict value for this variable indicates that an error is thrown by the compiler in case no partition predicate is provided on a partitioned table. This is used to protect against a user inadvertently issuing a query against all the partitions of the table. 【此变量的严格值表示，如果分区表上没有提供分区谓词，编译器将抛出错误。这用于防止用户无意中对表的所有分区发出查询。】 |  nonstrict
hive.map.aggr  |  Determines whether the map side aggregation is on or not. 【决定是否进行map端的聚合】 |  true
hive.join.emit.interval  |    |  1000
hive.map.aggr.hash.percentmemory  |    |  (float)0.5
hive.default.fileformat  |  Default file format for CREATE TABLE statement. Options are TextFile, SequenceFile, RCFile, and Orc.【 CREATE TABLE语句中的默认的文件个数，可用的选项有`TextFile\SequenceFile\RCFile\Orc`】  |  TextFile
hive.merge.mapfiles  |  Merge small files at the end of a map-only job.【在仅有map的job的末尾合并小文件】  |  true
hive.merge.mapredfiles  |  Merge small files at the end of a map-reduce job.【在map-reduce的job的末尾合并小文件】  |  false
hive.merge.size.per.task  |  Size of merged files at the end of the job.【在job的末尾合并的文件大小】  |  256000000
hive.merge.smallfiles.avgsize  |  When the average output file size of a job is less than this number, Hive will start an additional map-reduce job to merge the output files into bigger files. This is only done for map-only jobs if hive.merge.mapfiles is true, and for map-reduce jobs if hive.merge.mapredfiles is true.【当job的平均输出文件大小小于这个数字时，Hive将启动一个额外的map-reduce job，将输出文件合并到更大的文件中。对于仅有map的job，只有在`hive.merge.mapfiles=true`的情况，对于map-reduce jobs，只有在`hive.merge.mapredfiles=true`的情况，才会执行此操作。】  |  16000000
hive.querylog.enable.plan.progress  |  Whether to log the plan's progress every time a job's progress is checked. These logs are written to the location specified by hive.querylog.location. (As of Hive [0.10](https://issues.apache.org/jira/browse/HIVE-3230).)【每次检查job的进度时，是否都要记录plan的进度。这些日志被写到`hive.querylog.location`指定的位置。】  |  true
hive.querylog.location  |  Directory where structured hive query logs are created. One file per session is created in this directory. If this variable set to empty string structured log will not be created.【创建结构化hive查询日志的目录。在这个目录中，为每个会话创建一个文件。如果此变量设置为空字符串，则不会创建结构化日志。】  |  `/tmp/<user.name>`
hive.querylog.plan.progress.interval  |  The interval to wait between logging the plan's progress in milliseconds. If there is a whole number percentage change in the progress of the mappers or the reducers, the progress is logged regardless of this value. The actual interval will be the ceiling of (this value divided by the value of hive.exec.counters.pull.interval) multiplied by the value of hive.exec.counters.pull.interval i.e. if it is not divide evenly by the value of hive.exec.counters.pull.interval it will be logged less frequently than specified. This only has an effect if hive.querylog.enable.plan.progress is set to true. (As of Hive [0.10](https://issues.apache.org/jira/browse/HIVE-3230).)【记录plan进度之间的等待间隔(以毫秒为单位)。如果mappers或reducers的进度发生了整数百分比的变化，则不管这个值如何，进度都会被记录下来。实际的间隔是。。。】 |  60000
hive.stats.autogather |  A flag to gather statistics automatically during the INSERT OVERWRITE command. (As of Hive [0.7.0](https://issues.apache.org/jira/browse/HIVE-1361).)【在执行`INSERT OVERWRITE`期间，自动收集统计信息的标记】 |  true
hive.stats.dbclass |  The default database that stores temporary hive statistics. Valid values are hbase and jdbc while jdbc should have a specification of the Database to use, separated by a colon (e.g. jdbc:mysql). (As of Hive [0.7.0](https://issues.apache.org/jira/browse/HIVE-1361).)【存储临时hive统计信息的默认数据库。有效值是hbase、jdbc，jdbc应该有一个指定使用的数据库，冒号划分。】 |  jdbc:derby
hive.stats.dbconnectionstring |  The default connection string for the database that stores temporary hive statistics. (As of Hive [0.7.0](https://issues.apache.org/jira/browse/HIVE-1361).)【存储临时hive统计信息的默认连接字符串】 |  jdbc:derby:;databaseName=TempStatsStore;create=true
hive.stats.jdbcdriver |  The JDBC driver for the database that stores temporary hive statistics. (As of Hive [0.7.0](https://issues.apache.org/jira/browse/HIVE-1361).) 【存储临时hive统计信息的数据库的JDBC driver】|  org.apache.derby.jdbc.EmbeddedDriver
hive.stats.reliable |  Whether queries will fail because stats cannot be collected completely accurately. If this is set to true, reading/writing from/into a partition may fail becuase the stats could not be computed accurately. (As of Hive [0.10.0](https://issues.apache.org/jira/browse/HIVE-2021).) 【判断查询是否会因为没有完全准确的收集统计信息而失败。如果是true，从一个分区读，或写入一个分区可能会失败，因为收集统计信息没有完全准确的收集】|  false
hive.enforce.bucketing |  If enabled, enforces inserts into bucketed tables to also be bucketed. (Hive 0.6.0 through Hive [1.x.x](https://issues.apache.org/jira/browse/HIVE-12331) only)【如果启用，强制插入到分桶表的表也被分桶】 |  false
hive.variable.substitute |  Substitutes variables in Hive statements which were previously set using the set command, system variables or environment variables. See [HIVE-1096](https://issues.apache.org/jira/browse/HIVE-1096) for details. (As of Hive 0.7.0.)【替换Hive语句中的变量，这些变量以前使用的set命令、系统变量或环境变量设置的。】 |  true
hive.variable.substitute.depth |  The maximum replacements the substitution engine will do. (As of Hive [0.10.0](https://issues.apache.org/jira/browse/HIVE-2021).)【替换引擎可以做的最大替换量。】 |  40
hive.vectorized.execution.enabled |  This flag controls the vectorized mode of query execution as documented in [HIVE-4160](https://issues.apache.org/jira/browse/HIVE-4160). (As of Hive [0.13.0](https://issues.apache.org/jira/browse/HIVE-5283).)【此标志控制文档中所述的查询执行的向量化模式】 |  false


#### 1.5.2、Hive Metastore Configuration Variables

> Please see [Hive Metastore Administration](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+Administration) for information about the configuration variables used to set up the metastore in local, remote, or embedded mode. Also see descriptions in the [Metastore](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-MetaStore) section of the Language Manual's [Hive Configuration Properties](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties).

见 Hive Metastore Administration ，查看本地、远程或嵌入式模式下用来设置 metastore 的配置变量的信息。在 Language Manual 的 Hive Configuration Properties 的 Metastore 部分查看描述。

> For security configuration (Hive 0.10 and later), see the [Hive Metastore Security](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-HiveMetastoreSecurity) section in the Language Manual's [Hive Configuration Properties](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties).

对于安全配置，在 Language Manual 的 Hive Configuration Properties 的 Hive Metastore Security 查看。

#### 1.5.3、Configuration Variables Used to Interact with Hadoop

Variable Name  |  Description  | Deafult Value
---|:---|:---
hadoop.bin.path  |  The location of the Hadoop script which is used to submit jobs to Hadoop when submitting through a separate JVM.【当通过一个独立的JVM提交时，用来提交jobs的hadoop脚本的路径。】  |  $HADOOP_HOME/bin/hadoop
hadoop.config.dir  |  The location of the configuration directory of the Hadoop installation. 【安装hadoop的配置目录的路径】 |  $HADOOP_HOME/conf
fs.default.name |  The default name of the filesystem (for example, localhost for `hdfs://<clustername>:8020`).For YARN this configuration variable is called fs.defaultFS.【文件系统的默认名称】 |  file:///
map.input.file |  The filename the map is reading from.【map读取的文件名】 |  null
mapred.job.tracker |  The URL to the jobtracker. If this is set to local then map/reduce is run in the local mode.【jobtracker的URL。如果这个设置为local，那么map/reduce运行在本地模式。】 |  local
mapred.reduce.tasks |  The number of reducers for each map/reduce stage in the query plan.【查询计划中，每个map/reduce stage中reducers的数量。】 |  1
mapred.job.name |  The name of the map/reduce job. 【map/reduce job的名字】|  null
mapreduce.input.fileinputformat.split.maxsize |  For splittable data this changes the portion of the data that each mapper is assigned. By default, each mapper is assigned based on the block sizes of the source files. Entering a value larger than the block size will decrease the number of splits which creates fewer mappers. Entering a value smaller than the block size will increase the number of splits which creates more mappers.【对于可切分的数据，这将改变分配给每个mapper的数据比例。默认情况下，每个mapper都是根据源文件的块大小分配的。输入一个大于块大小的值，将减少切分的数量，从而创建更少的mapper。输入一个小于块大小的值，将增加切分的数量，这会创建更多的mapper。】 |  empty
fs.trash.interval |  The interval, in minutes, after which a trash checkpoint directory is deleted. (This is also the interval between checkpoints.) The checkpoint directory is located in .Trash under the user's home directory and contains files and directories that were removed since the previous checkpoint.Any setting greater than 0 enables the trash feature of HDFS.When using the Transparent Data Encryption (TDE) feature, set this to 0 in Hadoop core-site.xml as documented in [HIVE-10978](https://issues.apache.org/jira/browse/HIVE-10978).【删除垃圾检查点目录之后的时间间隔，以分钟为单位。(这也是检查点之间的间隔。)检查点目录位于用户主目录下的`.Trash`中，包含自上一个检查点以来删除的文件和目录。任何大于0的设置都将启用HDFS的垃圾功能。当使用透明数据加密(TDE)特性时，在Hadoop core-site.xml中将其设置为0。】 | 0

#### 1.5.4、Hive Variables Used to Pass Run Time Information

Variable Name  |  Description  | Deafult Value
---|:---|:---
hive.session.id  |  The id of the Hive Session. 【Hive Session的id】 |  
hive.query.string  |  The query string passed to the map/reduce job. 【传给map/reduce job的查询字符串。】 |  
hive.query.planid  |  The id of the plan for the map/reduce stage. 【对于map/reduce stage，plan的id】 |  
hive.jobname.length  |  The maximum length of the jobname.【job名称的最大长度】  |  50
hive.table.name  |  The name of the Hive table. This is passed to the user scripts through the script operator. 【Hive表的名字。这通过脚本操作符传给用户脚本。】 |  
hive.partition.name  |  The name of the Hive partition. This is passed to the user scripts through the script operator. 【Hive分区的名字。这通过脚本操作符传给用户脚本。】 |  
hive.alias  |  The alias being processed. This is also passed to the user scripts through the script operator.【被处理的别名。这通过脚本操作符传给用户脚本。】  |

## 2、Removing Hive Metastore Password from Hive Configuration

> Support for this was added in Hive 0.14.0 with [HIVE-7634](https://issues.apache.org/jira/browse/HIVE-7634) and [HADOOP-10904](https://issues.apache.org/jira/browse/HADOOP-10904). By setting up a CredentialProvider to handle storing/retrieval of passwords, you can remove the need to keep the Hive metastore password in cleartext in the Hive configuration.

对这个的支持在 Hive 0.14.0 添加。通过设置一个 `CredentialProvider` 来处理密码的存储/接收，你可以在 Hive 配置的 cleartext 中移除保持 Hive metastore 密码的需要。

> Set up the CredentialProvider to store the Hive Metastore password, using the key javax.jdo.option.ConnectionPassword (the same key as used in the Hive configuration). For example, the following command adds the metastore password to a JCEKS keystore file at /usr/lib/hive/conf/hive.jceks:

1.使用 `javax.jdo.option.ConnectionPassword`，设置`CredentialProvider` 来存储 Hive Metastore 密码。

例如，下面的命令向 `/usr/lib/hive/conf/hive.jceks` 中的 JCEKS keystore 文件添加 metastore 密码：

	$ hadoop credential create javax.jdo.option.ConnectionPassword -provider jceks://file/usr/lib/hive/conf/hive.jceks
	Enter password: 
	Enter password again: 
	javax.jdo.option.ConnectionPassword has been successfully created.
	org.apache.hadoop.security.alias.JavaKeyStoreProvider has been updated.

> Make sure to restrict access to this file to just the user running the Hive Metastore server/HiveServer2.
See [http://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/CommandsManual.html#credential](http://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/CommandsManual.html#credential) for more information.

确保只允许运行 Hive Metastore server/HiveServer2 的用户访问这个文件。

> Update the Hive configuration to use the designated CredentialProvider. For example to use our /usr/lib/hive/conf/hive.jceks file:

2.更新 Hive 配置，以使用指定的 CredentialProvider。例如，使用我们的 `/usr/lib/hive/conf/hive.jceks` 文件：

```xml
  <!-- Configure credential store for passwords-->
  <property>
    <name>hadoop.security.credential.provider.path</name>
    <value>jceks://file/usr/lib/hive/conf/hive.jceks</value>
  </property>
```

> This configures the CredentialProvider used by [http://hadoop.apache.org/docs/current/api/org/apache/hadoop/conf/Configuration.html#getPassword(java.lang.String)](http://hadoop.apache.org/docs/current/api/org/apache/hadoop/conf/Configuration.html#getPassword(java.lang.String)), which is used by Hive to retrieve the metastore password.

> Remove the Hive Metastore password entry ([javax.jdo.option.ConnectionPassword](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-javax.jdo.option.ConnectionPassword)) from the Hive configuration. The CredentialProvider will be used instead.

3.从 Hive 配置中，移除 Hive Metastore 密码条目，使用 CredentialProvider 代替。

> Restart Hive Metastore Server/HiveServer2.

4.重启 Hive Metastore Server/HiveServer2

## 3、Configuring HCatalog and WebHCat

### 3.1、HCatalog

> Starting in Hive release 0.11.0, HCatalog is installed and configured with Hive. The HCatalog server is the same as the Hive metastore.

从 Hive 0.11.0 开始，使用 Hive 安装和配置 HCatalog。HCatalog server 和 Hive metastore 相同。

> See [Hive Metastore Administration](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+Administration) for metastore configuration properties.

- metastore 配置属性见 Hive Metastore Administration

> See [HCatalog Installation from Tarball](https://cwiki.apache.org/confluence/display/Hive/HCatalog+InstallHCat) for additional information.

- 额外的信息见 HCatalog Installation from Tarball

> For Hive releases prior to 0.11.0, see the "Thrift Server Setup" section in the HCatalog 0.5.0 document [Installation from Tarball](http://hive.apache.org/docs/hcat_r0.5.0/install.html).

### 3.2、WebHCat

> For information about configuring WebHCat, see [WebHCat Configuration](https://cwiki.apache.org/confluence/display/Hive/WebHCat+Configure).

对 WebHCat 的配置信息，见 WebHCat Configuration。