# Getting Started

[TOC]

## 1、Installation and Configuration

<font color="grey">You can install a stable release of Hive by downloading a tarball, or you can download the source code and build Hive from that.</font>

可以下载 tar 包来安装 Hive 稳定版本，或者下载二进制源码，自己编译。

**Running HiveServer2 and Beeline**

### 1.1、Requirements

- Java 1.7

	Hive1.2 及更新版本要求 Java1.7 或更新版本。Hive0.14 到 1.1 要求 Java1.6，建议用户使用 Java1.8。

- Hadoop2.x(首选)，1.x(在Hive2.0.0 及更新版本不再支持)

	Hive0.13 之前的版本也支持 Hadoop0.20.x、0.23.x。

- Hive 通常用于生产的 Linux 和 Windows 环境中。Mac 是一种常用的开发环境。本文中的说明适用于 Linux 和 Ma c。在 Windows 上使用它需要稍微不同的步骤。

<font color="grey">Note:  Hive versions 1.2 onward require Java 1.7 or newer. Hive versions 0.14 to 1.1 work with Java 1.6 as well. Users are strongly advised to start moving to Java 1.8 (see HIVE-8607).</font>  

<font color="grey">Hadoop 2.x (preferred), 1.x (not supported by Hive 2.0.0 onward).</font>

<font color="grey">Hive versions up to 0.13 also supported Hadoop 0.20.x, 0.23.x.</font>

<font color="grey">Hive is commonly used in production Linux and Windows environment. Mac is a commonly used development environment. The instructions in this document are applicable to Linux and Mac. Using it on Windows would require slightly different steps.</font>

### 1.2、Installing Hive from a Stable Release

<font color="grey">Start by downloading the most recent stable release of Hive from one of the Apache download mirrors (see [Hive Releases](https://hive.apache.org/downloads.html)).</font>

<font color="grey">Next you need to unpack the tarball. This will result in the creation of a subdirectory named hive-x.y.z (where x.y.z is the release number):</font>

下载 Hive 的稳定版本，再解压，会产生子目录 `hive-x.y.z`：

	$ tar -xzvf hive-x.y.z.tar.gz

<font color="grey">Set the environment variable HIVE_HOME to point to the installation directory:</font>

配置环境变量：

	$ cd hive-x.y.z
	$ export HIVE_HOME={{pwd}}

<font color="grey">Finally, add $HIVE_HOME/bin to your PATH:</font>

	$ export PATH=$HIVE_HOME/bin:$PATH

### 1.3、Building Hive from Source

<font color="grey">The Hive GIT repository for the most recent Hive code is located here: git clone https://git-wip-us.apache.org/repos/asf/hive.git (the master branch).</font>

<font color="grey">All release versions are in branches named "branch-0.#" or "branch-1.#" or the upcoming "branch-2.#", with the exception of release 0.8.1 which is in "branch-0.8-r2". Any branches with other names are feature branches for works-in-progress. See Understanding Hive Branches for details.</font>

<font color="grey">As of 0.13, Hive is built using Apache Maven.</font>

#### 1.3.1、Compile Hive on master

<font color="grey">To build the current Hive code from the master branch:</font>

	  $ git clone https://git-wip-us.apache.org/repos/asf/hive.git
	  $ cd hive
	  $ mvn clean package -Pdist [-DskipTests -Dmaven.javadoc.skip=true]
	  $ cd packaging/target/apache-hive-{version}-SNAPSHOT-bin/apache-hive-{version}-SNAPSHOT-bin
	  $ ls
	  LICENSE
	  NOTICE
	  README.txt
	  RELEASE_NOTES.txt
	  bin/ (all the shell scripts)
	  lib/ (required jar files)
	  conf/ (configuration files)
	  examples/ (sample input and query files)
	  hcatalog / (hcatalog installation)
	  scripts / (upgrade scripts for hive-metastore)

<font color="grey">Here, {version} refers to the current Hive version.</font>

<font color="grey">If building Hive source using Maven (mvn), we will refer to the directory "/packaging/target/apache-hive-{version}-SNAPSHOT-bin/apache-hive-{version}-SNAPSHOT-bin" as <install-dir> for the rest of the page.</font>

#### 1.3.2、Compile Hive on branch-1

<font color="grey">In branch-1, Hive supports both Hadoop 1.x and 2.x.  You will need to specify which version of Hadoop to build against via a Maven profile.  To build against Hadoop 1.x use the profile hadoop-1; for Hadoop 2.x use hadoop-2.  For example to build against Hadoop 1.x, the above mvn command becomes:</font>

	$ mvn clean package -Phadoop-1,dist

#### 1.3.3、Compile Hive Prior to 0.13 on Hadoop 0.20

<font color="grey">Prior to Hive 0.13, Hive was built using Apache Ant.  To build an older version of Hive on Hadoop 0.20:</font>

	  $ svn co http://svn.apache.org/repos/asf/hive/branches/branch-{version} hive
	  $ cd hive
	  $ ant clean package
	  $ cd build/dist
	  # ls
	  LICENSE
	  NOTICE
	  README.txt
	  RELEASE_NOTES.txt
	  bin/ (all the shell scripts)
	  lib/ (required jar files)
	  conf/ (configuration files)
	  examples/ (sample input and query files)
	  hcatalog / (hcatalog installation)
	  scripts / (upgrade scripts for hive-metastore)

<font color="grey">If using Ant, we will refer to the directory "build/dist" as <install-dir>.</font>

#### 1.3.4、Compile Hive Prior to 0.13 on Hadoop 0.23

<font color="grey">To build Hive in Ant against Hadoop 0.23, 2.0.0, or other version, build with the appropriate flag; some examples below:</font>

	  $ ant clean package -Dhadoop.version=0.23.3 -Dhadoop-0.23.version=0.23.3 -Dhadoop.mr.rev=23
	  $ ant clean package -Dhadoop.version=2.0.0-alpha -Dhadoop-0.23.version=2.0.0-alpha -Dhadoop.mr.rev=23

### 1.4、Running Hive

<font color="grey">Hive uses Hadoop, so:</font>

Hive 需要使用 Hadoop ，所以：

- you must have Hadoop in your path OR
- export HADOOP_HOME=<hadoop-install-dir>

<font color="grey">In addition, you must use below HDFS commands to create /tmp and /user/hive/warehouse (aka hive.metastore.warehouse.dir) and set them chmod g+w before you can create a table in Hive.</font>

另外，还需要使用如下命令创建目录 `/tmp` 和 `/user/hive/warehouse`(即`hive.metastore.warehouse.dir`)，然后设置权限 `chmod g+w` ：

	  $ $HADOOP_HOME/bin/hadoop fs -mkdir       /tmp
	  $ $HADOOP_HOME/bin/hadoop fs -mkdir       /user/hive/warehouse
	  $ $HADOOP_HOME/bin/hadoop fs -chmod g+w   /tmp
	  $ $HADOOP_HOME/bin/hadoop fs -chmod g+w   /user/hive/warehouse

<font color="grey">You may find it useful, though it's not necessary, to set HIVE_HOME:</font>

设置 Hive 的环境变量：

	$ export HIVE_HOME=<hive-install-dir>

#### 1.4.1、Running Hive CLI

<font color="grey">To use the Hive [command line interface](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli) (CLI) from the shell:</font>

使用如下命令，启动 Hive 命令行：

	$ $HIVE_HOME/bin/hive

#### 1.4.2、Running HiveServer2 and Beeline

<font color="grey">Starting from Hive 2.1, we need to run the schematool command below as an initialization step. For example, we can use "derby" as db type.</font>

从 Hive2.1 开始，允许如下 `schematool` 命令，来初始化。这里使用 "derby" 作为数据库类型：

	$ $HIVE_HOME/bin/schematool -dbType <db type> -initSchema

<font color="grey">[HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2) (introduced in Hive 0.11) has its own CLI called [Beeline](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients). HiveCLI is now deprecated in favor of Beeline, as it lacks the multi-user, security, and other capabilities of HiveServer2.  To run HiveServer2 and Beeline from shell:</font>

**HiveServer2(Hive0.11 引入)有自己的命令行界面，称为 `Beeline`** 。

HiveCLI 现在已被弃用，取而代之的是 `Beeline` ，因为它缺乏 HiveServer2 的多用户、安全性和其他功能。

从 shell 运行 HiveServer2 和 `Beeline` :

	$ $HIVE_HOME/bin/hiveserver2

	$ $HIVE_HOME/bin/beeline -u jdbc:hive2://$HS2_HOST:$HS2_PORT

<font color="grey">Beeline is started with the JDBC URL of the HiveServer2, which depends on the address and port where HiveServer2 was started.  By default, it will be (localhost:10000), so the address will look like jdbc:hive2://localhost:10000.</font>

<font color="grey">Or to start Beeline and HiveServer2 in the same process for testing purpose, for a similar user experience to HiveCLI:</font>

**Beeline 启动需要设置 HiveServer2 的 JDBC URL**，该 URL 依赖于 HiveServer2 的地址和端口。

默认情况下，它是 `(localhost:10000)`，因此地址看起来像 `jdbc:hive2://localhost:10000`。

或为了测试目的，在相同的进程中启动 Beeline 和 HiveServer2 ，以获得与 HiveCLI 类似的用户体验:

	$ $HIVE_HOME/bin/beeline -u jdbc:hive2://

#### 1.4.3、Running HCatalog

<font color="grey">To run the HCatalog server from the shell in Hive release 0.11.0 and later:</font>

从 0.11.0 版本开始，可以在 shell 中运行 `HCatalog server` :

	$ $HIVE_HOME/hcatalog/sbin/hcat_server.sh

<font color="grey">To use the HCatalog command line interface (CLI) in Hive release 0.11.0 and later:</font>

从 0.11.0 版本开始，可以使用 HCatalog 的命令行界面：

	$ $HIVE_HOME/hcatalog/bin/hcat

<font color="grey">For more information, see [HCatalog Installation from Tarball](https://cwiki.apache.org/confluence/display/Hive/HCatalog+InstallHCat) and [HCatalog CLI](https://cwiki.apache.org/confluence/display/Hive/HCatalog+InstallHCat) in the [HCatalog manual](https://cwiki.apache.org/confluence/display/Hive/HCatalog).</font>

#### 1.4.4、Running WebHCat (Templeton)

<font color="grey">To run the WebHCat server from the shell in Hive release 0.11.0 and later:</font>

从 0.11.0 版本开始，可以在 shell 中运行 `WebHCat server` :

	$ $HIVE_HOME/hcatalog/sbin/webhcat_server.sh

<font color="grey">For more information, see [WebHCat Installation](https://cwiki.apache.org/confluence/display/Hive/WebHCat+InstallWebHCat) in the [WebHCat manual](https://cwiki.apache.org/confluence/display/Hive/WebHCat).</font>

### 1.5、Configuration Management Overview

<font color="grey">Hive by default gets its configuration from <install-dir>/conf/hive-default.xml

The location of the Hive configuration directory can be changed by setting the HIVE_CONF_DIR environment variable.

Configuration variables can be changed by (re-)defining them in <install-dir>/conf/hive-site.xml

Log4j configuration is stored in <install-dir>/conf/hive-log4j.properties

Hive configuration is an overlay on top of Hadoop – it inherits the Hadoop configuration variables by default.</font>

- Hive 默认从 `<install-dir>/conf/hive-default.xml` 中获取配置。

- 可以通过设置 `HIVE_CONF_DIR` 来改变配置文件的目录。

- 配置文件中的变量可以在 `<install-dir>/conf/hive-site.xml` 中重新定义。

- Log4j 配置文件存储在 `<install-dir>/conf/hive-log4j.properties`。

- Hive 配置文件是覆盖在 Hadoop 之上的，默认继承 Hadoop 配置变量。

- Hive 配置可以通过以下方式操作:

	- 编辑 `hive-site.xml` ，并在其中定义任何想要的变量(包括 Hadoop 变量)

	- 使用 set 命令(请参阅下一节)

	- 调用 Hive 命令(已废弃)，Beeline 或 HiveServer2 使用语法:

		- $ bin/hive --hiveconf x1=y1 --hiveconf x2=y2 //设置变量x1和x2分别为y1和y2

		- $ bin/hiveserver2 --hiveconf x1=y1 --hiveconf x2=y2 //将服务端变量x1和x2分别设置为y1和y2

		- bin/beeline --hiveconf x1=y1 --hiveconf x2=y2 //将客户端变量x1和x2分别设置为y1和y2。

- 将 `HIVE_OPTS` 环境变量设置为 `--hiveconf x1=y1 --hiveconf x2=y2`，其作用与上面相同。

<font color="grey">Hive configuration can be manipulated by:

Editing hive-site.xml and defining any desired variables (including Hadoop variables) in it

Using the set command (see next section)

Invoking Hive (deprecated), Beeline or HiveServer2 using the syntax:

$ bin/hive --hiveconf x1=y1 --hiveconf x2=y2  //this sets the variables x1 and x2 to y1 and y2 respectively

$ bin/hiveserver2 --hiveconf x1=y1 --hiveconf x2=y2  //this sets server-side variables x1 and x2 to y1 and y2 respectively

$ bin/beeline --hiveconf x1=y1 --hiveconf x2=y2  //this sets client-side variables x1 and x2 to y1 and y2 respectively.

Setting the HIVE_OPTS environment variable to "--hiveconf x1=y1 --hiveconf x2=y2" which does the same as above.</font>

### 1.6、Runtime Configuration

<font color="grey">Hive queries are executed using map-reduce queries and, therefore, the behavior of such queries can be controlled by the Hadoop configuration variables.
The HiveCLI (deprecated) and Beeline command 'SET' can be used to set any Hadoop (or Hive) configuration variable. For example:</font>

因为 Hive 查询底层执行的是 MapReduce ，所以可以通过控制 Hadoop 的配置文件中的变量，来控制 Hive 查询的行为。

使用  HiveCLI(已废弃)和 Beeline  命令 `SET` 来设置 Hadoop (or Hive) 的变量：

    beeline> SET mapred.job.tracker=myhost.mycompany.com:50030;
    beeline> SET -v;

后者显示所有当前设置。没有 `-v` 选项，只显示与基本 Hadoop 配置不同的变量。

<font color="grey">The latter shows all the current settings. Without the -v option only the variables that differ from the base Hadoop configuration are displayed.</font>

### 1.7、Hive, Map-Reduce and Local-Mode

<font color="grey">Hive compiler generates map-reduce jobs for most queries. These jobs are then submitted to the Map-Reduce cluster indicated by the variable:</font>

对大多数查询来说，Hive 编译器会生成 map-reduce jobs。然后这些 jobs 被提交到变量所指示的 Map-Reduce 集群：

	mapred.job.tracker

虽然这个变量通常指向一个具有多节点的 Map-Reduce 集群，但 Hadoop 也提供了一个极好的选项，可以在用户的本地工作站运行 map-reduce jobs。

这对于在小数据集上运行查询非常有用，在这种情况下，本地模式执行通常比在大型集群快得多。

数据直接从 HDFS 透明地访问。相反，本地模式只运行一个 reducer ，处理较大的数据集时会非常慢，

<font color="grey">While this usually points to a map-reduce cluster with multiple nodes, Hadoop also offers a nifty option to run map-reduce jobs locally on the user's workstation. This can be very useful to run queries over small data sets – in such cases local mode execution is usually significantly faster than submitting jobs to a large cluster. Data is accessed transparently from HDFS. Conversely, local mode only runs with one reducer and can be very slow processing larger data sets.</font>

<font color="grey">Starting with release 0.7, Hive fully supports local mode execution. To enable this, the user can enable the following option:</font>

从版本 0.7 开始，Hive 完全支持本地模式执行。用户需要启用下面的选项：

	hive> SET mapreduce.framework.name=local;

另外，`mapred.local.dir` 应指向本地机器的一个有效路径(如`/tmp/<username>/mapred/local`)。

否则，用户将得到一个分配本地磁盘空间的异常。

<font color="grey">In addition, mapred.local.dir should point to a path that's valid on the local machine (for example /tmp/<username>/mapred/local). (Otherwise, the user will get an exception allocating local disk space.)</font>

<font color="grey">Starting with release 0.7, Hive also supports a mode to run map-reduce jobs in local-mode automatically. The relevant options are hive.exec.mode.local.auto, hive.exec.mode.local.auto.inputbytes.max, and hive.exec.mode.local.auto.tasks.max:</font>

从版本 0.7 开始，Hive 也支持自动地在本地运行 map-reduce jobs。相关选项有 `hive.exec.mode.local.auto` 、`hive.exec.mode.local.auto.inputbytes.max` 和 `hive.exec.mode.local.auto.tasks.max`：

	hive> SET hive.exec.mode.local.auto=false;

注意，这个特征默认是不启用的。

如果启用了，且满足下面的阈值，Hive 会分析每个查询中每个 map-reduce job 的大小，然后本地运行它：

- job 的总输入大小小于`hive.exec.mode.local.auto.inputbytes.max` (默认128MB)

- map-tasks 的总数量小于`hive.exec.mode.local.auto.tasks.max`(默认4)

- reduce-tasks 的总数量等于1或0

<font color="grey">Note that this feature is disabled by default. If enabled, Hive analyzes the size of each map-reduce job in a query and may run it locally if the following thresholds are satisfied:</font>

<font color="grey">The total input size of the job is lower than:
`hive.exec.mode.local.auto.inputbytes.max` (128MB by default)</font>

<font color="grey">The total number of map-tasks is less than: `hive.exec.mode.local.auto.tasks.max` (4 by default)</font>

<font color="grey">The total number of reduce tasks required is 1 or 0.</font>

因此，对于小规模数据集上的查询，或具有多个 map-reduce jobs 的查询，这些查询的后续 job 的输入要小得多(因为前一个 job 中进行了 reduce/filtering)，job 可以在本地运行。

请注意，Hadoop 服务器节点的运行环境和运行 Hive 客户端的机器间可能存在差异(因为 jvm 版本或软件库不同)。在本地模式下运行时，这可能会导致意外的行为/错误。

还要注意，本地模式执行是在一个单独的子 jvm (Hive 客户端的)中执行的。

如果用户愿意，这个子 jvm 的最大内存量可以通过 `hive.mapred.local.mem` 来控制。默认情况下为0，在这种情况下，Hive 让 Hadoop 决定子 jvm 的默认内存限制。

<font color="grey">So for queries over small data sets, or for queries with multiple map-reduce jobs where the input to subsequent jobs is substantially smaller (because of reduction/filtering in the prior job), jobs may be run locally.</font>

<font color="grey">Note that there may be differences in the runtime environment of Hadoop server nodes and the machine running the Hive client (because of different jvm versions or different software libraries). This can cause unexpected behavior/errors while running in local mode. Also note that local mode execution is done in a separate, child jvm (of the Hive client). If the user so wishes, the maximum amount of memory for this child jvm can be controlled via the option hive.mapred.local.mem. By default, it's set to zero, in which case Hive lets Hadoop determine the default memory limits of the child jvm.</font>

### 1.8、Hive Logging

<font color="grey">Hive uses log4j for logging. By default logs are not emitted to the console by the CLI. The default logging level is WARN for Hive releases prior to 0.13.0. Starting with Hive 0.13.0, the default logging level is INFO.</font>

**Hive 使用 log4j 进行日志记录**。默认情况下，CLI 不会将日志发送到控制台。

对于 0.13.0 之前的 Hive，默认的日志记录级别是 WARN。从 0.13.0 开始，默认的日志记录级别是 INFO。

<font color="grey">The logs are stored in the directory /tmp/<user.name>:</font>

日志存储在如下目录：

	/tmp/<user.name>/hive.log

注意：本地模式下，0.13.0 之前的 Hive，日志文件名称是 ".log" ，而不是 "hive.log"。

<font color="grey">Note: In [local mode](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82706478#GettingStarted-Hive,Map-ReduceandLocal-Mode), prior to Hive 0.13.0 the log file name was ".log" instead of "hive.log". This bug was fixed in release 0.13.0 (see [HIVE-5528](https://issues.apache.org/jira/browse/HIVE-5528) and [HIVE-5676](https://issues.apache.org/jira/browse/HIVE-5676)).
To configure a different log location, set hive.log.dir in $HIVE_HOME/conf/hive-log4j.properties. Make sure the directory has the sticky bit set (chmod 1777 <dir>).</font>

**在 `$HIVE_HOME/conf/hive-log4j.properties` 中设置 `hive.log.dir` 来配置一个不同的日志路径：**

	hive.log.dir=<other_location>

<font color="grey">If the user wishes, the logs can be emitted to the console by adding the arguments shown below:</font>

通过添加如下参数，日志可以提交到控制台：

	bin/hive --hiveconf hive.root.logger=INFO,console  //for HiveCLI (deprecated)
	
	bin/hiveserver2 --hiveconf hive.root.logger=INFO,console

<font color="grey">Alternatively, the user can change the logging level only by using:</font>

改变日志输出等级：

	bin/hive --hiveconf hive.root.logger=INFO,DRFA //for HiveCLI (deprecated)

	bin/hiveserver2 --hiveconf hive.root.logger=INFO,DRFA

<font color="grey">Another option for logging is TimeBasedRollingPolicy (applicable for Hive 1.1.0 and above, [HIVE-9001](https://issues.apache.org/jira/browse/HIVE-9001)) by providing DAILY option as shown below:</font>

可以通过 `DAILY` 选项来设置 `TimeBasedRollingPolicy `:

	bin/hive --hiveconf hive.root.logger=INFO,DAILY //for HiveCLI (deprecated)

	bin/hiveserver2 --hiveconf hive.root.logger=INFO,DAILY

注意：通过 'set' 命令设置 `hive.root.logger` 不会改变日志属性，因为它们是在初始化时确定的。

<font color="grey">Note that setting hive.root.logger via the 'set' command does not change logging properties since they are determined at initialization time.</font>

<font color="grey">Hive also stores query logs on a per Hive session basis in /tmp/<user.name>/, but can be configured in [hive-site.xml](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration) with the [hive.querylog.location](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.querylog.location) property.  Starting with Hive 1.1.0, [EXPLAIN EXTENDED](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Explain#LanguageManualExplain-EXPLAINSyntax) output for queries can be logged at the INFO level by setting the [hive.log.explain.output](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.log.explain.output) property to true.</font>

在每个 Hive session ，Hive 也会在 `/tmp/<user.name>/` 中存储查询日志，但可以在 `hive-site.xml` 中配置属性 `hive.querylog.location` 来改变。

从 Hive 1.1.0 开始，通过设置 `hive.log.explain.output=true`，可以将`EXPLAIN EXTENDED` 的输出以 INFO 的级别记录。

<font color="grey">Logging during Hive execution on a Hadoop cluster is controlled by Hadoop configuration. Usually Hadoop will produce one log file per map and reduce task stored on the cluster machine(s) where the task was executed. The log files can be obtained by clicking through to the Task Details page from the Hadoop JobTracker Web UI.</font>

Hive 在 Hadoop 集群上执行期间，日志记录是由 Hadoop 配置控制的。通过 Hadoop 对每个 map 和 reduce 任务产生一个日志文件，存储在运行该任务的节点上。

可以通过点击 Hadoop JobTracker Web UI 中的 Task Details 页来获取日志文件。

<font color="grey">When using local mode (using mapreduce.framework.name=local), Hadoop/Hive execution logs are produced on the client machine itself. Starting with release 0.6 – Hive uses the hive-exec-log4j.properties (falling back to hive-log4j.properties only if it's missing) to determine where these logs are delivered by default. The default configuration file produces one log file per query executed in local mode and stores it under /tmp/<user.name>. The intent of providing a separate configuration file is to enable administrators to centralize execution log capture if desired (on a NFS file server for example). Execution logs are invaluable for debugging run-time errors.</font>

当使用本地模式时(`mapreduce.framework.name=local`)，会在客户端机器上产生 Hadoop/Hive 执行日志。

**从 0.6 版本开始，Hive 默认使用 `hive-exec-log4j.properties` 来决定这些执行日志被传输到什么地方(只有缺失它时，才会使用`hive-log4j.properties`)**。

对在本地模式下执行的一个查询，默认配置文件会产生一个日志文件，存储在 `/tmp/<user.name>`。

提供单独配置文件的目的是让管理员能够根据需要集中执行日志捕获(例如，在 NFS 文件服务器上)。执行日志对于调试运行时错误非常有用。

<font color="grey">For information about WebHCat errors and logging, see [Error Codes and Responses]((https://cwiki.apache.org/confluence/display/Hive/WebHCat+UsingWebHCat#WebHCatUsingWebHCat-ErrorCodesandResponses)) and [Log Files](https://cwiki.apache.org/confluence/display/Hive/WebHCat+UsingWebHCat#WebHCatUsingWebHCat-LogFiles) in the [WebHCat manual](https://cwiki.apache.org/confluence/display/Hive/WebHCat).</font>

<font color="grey">Error logs are very useful to debug problems. Please send them with any bugs (of which there are many!) to hive-dev@hadoop.apache.org.</font>

<font color="grey">From Hive 2.1.0 onwards (with [HIVE-13027](https://issues.apache.org/jira/browse/HIVE-13027)), Hive uses Log4j2's asynchronous logger by default. Setting [hive.async.log.enabled](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.async.log.enabled) to false will disable asynchronous logging and fallback to synchronous logging. Asynchronous logging can give significant performance improvement as logging will be handled in a separate thread that uses the LMAX disruptor queue for buffering log messages. Refer to [https://logging.apache.org/log4j/2.x/manual/async.html](https://logging.apache.org/log4j/2.x/manual/async.html) for benefits and drawbacks.</font>

**从 Hive 2.1.0 开始，Hive 默认使用 Log4j2 的异步日志记录器。**

**设置 `hive.async.log=false` 将禁用异步日志记录并退回到同步日志记录。**

异步日志记录可以**显著提高性能**，因为日志记录将在单独的线程中处理，该线程使用 LMAX 干扰器队列缓冲日志消息。

#### 1.8.1、HiveServer2 Logs

<font color="grey">HiveServer2 operation logs are available to clients starting in Hive 0.14. See [HiveServer2 Logging](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-HiveServer2Logging) for configuration.</font>

从 Hive 0.14 开始，HiveServer2 操作日志对客户端是可用的。

#### 1.8.2、Audit Logs

<font color="grey">Audit logs are logged from the Hive metastore server for every metastore API invocation.</font>

<font color="grey">An audit log has the function and some of the relevant function arguments logged in the metastore log file. It is logged at the INFO level of log4j, so you need to make sure that the logging at the INFO level is enabled (see [HIVE-3505](https://issues.apache.org/jira/browse/HIVE-3505)). The name of the log entry is "HiveMetaStore.audit".</font>

<font color="grey">Audit logs were added in Hive 0.7 for secure client connections ([HIVE-1948](https://issues.apache.org/jira/browse/HIVE-1948)) and in Hive 0.10 for non-secure connections ([HIVE-3277](https://issues.apache.org/jira/browse/HIVE-3277); also see [HIVE-2797](https://issues.apache.org/jira/browse/HIVE-2797)).</font>

对于每个 metastore API 调用，审计日志会在 Hive metastore server 记录下来。

一个审计日志有函数和一些相关的函数参数，记录在 metastore 日志文件。它是 log4j 的 INFO 级别的日志记录，因此需要确保 INFO 级别上的日志记录是启用的。日志条目的名称是 `HiveMetaStore.audit`。

在 Hive 0.7 中，为安全客户端连接，添加了审计日志，在 Hive 0.10 中为非安全连接。

#### 1.8.3、Perf Logger

<font color="grey">In order to obtain the performance metrics via the PerfLogger, you need to set DEBUG level logging for the PerfLogger class ([HIVE-12675](https://issues.apache.org/jira/browse/HIVE-12675)). This can be achieved by setting the following in the log4j properties file.</font>

为了通过 PerfLogger 获得性能度量，需要为 PerfLogger 类设置 DEBUG 级别日志记录。这可以通过在 log4j 文件中设置以下内容来实现：

	log4j.logger.org.apache.hadoop.hive.ql.log.PerfLogger=DEBUG

如果日志程序级别已经通过 `hive.root.logger` 设置为 DEBUG 的话。以上设置就不再要求，为了查看性能日志。

<font color="grey">If the logger level has already been set to DEBUG at root via hive.root.logger, the above setting is not required to see the performance logs.</font>

## 2、DDL Operations

<font color="grey">The Hive DDL operations are documented in [Hive Data Definition Language](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL).</font>

### 2.1、Creating Hive Tables

	hive> CREATE TABLE pokes (foo INT, bar STRING);

创建一个表 pokes ，具有两列，一列是 integer，一列是 string。

<font color="grey">creates a table called pokes with two columns, the first being an integer and the other a string.</font>

	hive> CREATE TABLE invites (foo INT, bar STRING) PARTITIONED BY (ds STRING);

创建一个表 invites ，具有两列和一个ds分区列。

分区列是一个虚拟列。它不是数据本身的一部分，而是从加载特定数据集的分区派生而来的。

<font color="grey">creates a table called invites with two columns and a partition column called ds. The partition column is a virtual column. It is not part of the data itself but is derived from the partition that a particular dataset is loaded into.</font>

默认情况下，假定表为文本输入格式，分隔符为 `^A(ctrl-a)`。

<font color="grey">By default, tables are assumed to be of text input format and the delimiters are assumed to be ^A(ctrl-a).</font>

### 2.2、Browsing through Tables

	hive> SHOW TABLES;

列出所有表

<font color="grey">lists all the tables.</font>

	hive> SHOW TABLES '.*s';

列出所有结尾是 's' 的表。模式匹配遵循 Java 正则表达式。

<font color="grey">lists all the table that end with 's'. The pattern matching follows Java regular expressions. Check out this link for documentation [http://java.sun.com/javase/6/docs/api/java/util/regex/Pattern.html](http://java.sun.com/javase/6/docs/api/java/util/regex/Pattern.html).</font>

	hive> DESCRIBE invites;

展示列的列表

<font color="grey">shows the list of columns.</font>

### 2.3、Altering and Dropping Tables

<font color="grey">Table names can be [changed](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-RenameTable) and columns can be [added or replaced](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-Add/ReplaceColumns):</font>

表名称可以改变，列可以增加或替换：

	hive> ALTER TABLE events RENAME TO 3koobecaf;
	hive> ALTER TABLE pokes ADD COLUMNS (new_col INT);
	hive> ALTER TABLE invites ADD COLUMNS (new_col2 INT COMMENT 'a comment');
	hive> ALTER TABLE invites REPLACE COLUMNS (foo INT, bar STRING, baz INT COMMENT 'baz replaces new_col2');

<font color="grey">Note that REPLACE COLUMNS replaces all existing columns and only changes the table's schema, not the data. The table must use a native SerDe. REPLACE COLUMNS can also be used to drop columns from the table's schema:</font>

`REPLACE COLUMNS` 替换了所有已存在的列，仅改变了表结构，而没有改变数据。表必须使用原生的 SerDe。`REPLACE COLUMNS` 也可以用来删除表结构中的列：

	hive> ALTER TABLE invites REPLACE COLUMNS (foo INT COMMENT 'only keep the first column');

<font color="grey">Dropping tables:</font>

删除表：

	hive> DROP TABLE pokes;

### 2.4、Metadata Store

<font color="grey">Metadata is in an embedded Derby database whose disk storage location is determined by the Hive configuration variable named javax.jdo.option.ConnectionURL. By default this location is ./metastore_db (see conf/hive-default.xml).</font>

Metadata 是在集成的 Derby 数据库中，它的磁盘存储路径是通过 `javax.jdo.option.ConnectionURL` 设置的。默认位置是 `./metastore_db`

<font color="grey">Right now, in the default configuration, this metadata can only be seen by one user at a time.</font>

现在，在默认的配置文件中，同一时间，这个元数据仅可以被一个用户看到。

<font color="grey">Metastore can be stored in any database that is supported by JPOX. The location and the type of the RDBMS can be controlled by the two variables javax.jdo.option.ConnectionURL and javax.jdo.option.ConnectionDriverName. Refer to JDO (or JPOX) documentation for more details on supported databases. The database schema is defined in JDO metadata annotations file package.jdo at src/contrib/hive/metastore/src/model.</font>

Metastore 可以存储在 JPOX 支持的任意数据库。

路径和 RDBMS 的类型通过 `javax.jdo.option.ConnectionURL` 和 `javax.jdo.option.ConnectionDriverName` 设置。

在 `src/contrib/hive/metastore/src/model` 中的 JDO 元数据注释文件包 `package.jdo` 中定义数据库 schema。

<font color="grey">In the future, the metastore itself can be a standalone server.</font>

未来，metastore 本身可以是独立的服务器。

如果你想运行 metastore 作为一个网络服务器，这样它就可以从多个节点访问，查看 `Hive Using Derby in Server Mode`

<font color="grey">If you want to run the metastore as a network server so it can be accessed from multiple nodes, see [Hive Using Derby in Server Mode](https://cwiki.apache.org/confluence/display/Hive/HiveDerbyServerMode).</font>

## 3、DML Operations

<font color="grey">The Hive DML operations are documented in [Hive Data Manipulation Language](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML).</font>

<font color="grey">Loading data from flat files into Hive:</font>

载入文件到 Hive:

```sh
hive> LOAD DATA LOCAL INPATH './examples/files/kv1.txt' OVERWRITE INTO TABLE pokes;
```

载入的文件包含两列，由 ctrl-a 划分。 

'LOCAL' 表示文件是本地文件系统上的。如果不写 'LOCAL'，表示文件是 HDFS 上的。

'OVERWRITE' 表示会删除表中已存在的数据。如果不写，那么数据文件就会追加到已存在的数据集。

<font color="grey">Loads a file that contains two columns separated by ctrl-a into pokes table. 'LOCAL' signifies that the input file is on the local file system. If 'LOCAL' is omitted then it looks for the file in HDFS.</font>

<font color="grey">The keyword 'OVERWRITE' signifies that existing data in the table is deleted. If the 'OVERWRITE' keyword is omitted, data files are appended to existing data sets.</font>

注意：

- load 命令的执行不会对架构执行任何数据验证。

- 如果文件在 hdfs 中，它会被移动到 hive 控制的文件系统命名空间中。

Hive 目录的根目录由 `hive-default.xml` 中的 `hive.metastore.warehouse.dir` 选项指定。

建议用户创建表前先创建这个目录。

<font color="grey">NO verification of data against the schema is performed by the load command.</font>

<font color="grey">If the file is in hdfs, it is moved into the Hive-controlled file system namespace.
The root of the Hive directory is specified by the option hive.metastore.warehouse.dir in hive-default.xml. We advise users to create this directory before trying to create tables via Hive.</font>
 
```sh
hive> LOAD DATA LOCAL INPATH './examples/files/kv2.txt' OVERWRITE INTO TABLE invites PARTITION (ds='2008-08-15');
	
hive> LOAD DATA LOCAL INPATH './examples/files/kv3.txt' OVERWRITE INTO TABLE invites PARTITION (ds='2008-08-08');
```
上述两个 load 语句将数据载入到表的两个不同的分区中。表必须是根据ds分区的，语句才能执行成功。

<font color="grey">The two LOAD statements above load data into two different partitions of the table invites. Table invites must be created as partitioned by the key ds for this to succeed.</font>

```sh
hive> LOAD DATA INPATH '/user/myname/kv2.txt' OVERWRITE INTO TABLE invites PARTITION (ds='2008-08-15');
```
上述命令将载入到 HDFS 文件/目录中的数据载入到表中。

注意，从 HDFS 载入数据将导致移动文件/目录。因此，该操作几乎是瞬间完成的。

<font color="grey">The above command will load data from an HDFS file/directory to the table.
Note that loading data from HDFS will result in moving the file/directory. As a result, the operation is almost instantaneous.</font>

## 4、SQL Operations

<font color="grey">The Hive query operations are documented in [Select](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Select).</font>

### 4.1、Example Queries

<font color="grey">Some example queries are shown below. They are available in build/dist/examples/queries.</font>

<font color="grey">More are available in the Hive sources at ql/src/test/queries/positive.</font>

下列查询在 `build/dist/examples/queries` 。更多查询在 `ql/src/test/queries/positive`。

#### 4.1.1、SELECTS and FILTERS

```sh
hive> SELECT a.foo FROM invites a WHERE a.ds='2008-08-15';
```

<font color="grey">selects column 'foo' from all rows of partition ds=2008-08-15 of the invites table. The results are not stored anywhere, but are displayed on the console.</font>

<font color="grey">Note that in all the examples that follow, INSERT (into a Hive table, local directory or HDFS directory) is optional.</font>

下列的所有示例，INSERT 是可选地。

```sh
hive> INSERT OVERWRITE DIRECTORY '/tmp/hdfs_out' SELECT a.* FROM invites a WHERE a.ds='2008-08-15';
```
查询结果在 '/tmp/hdfs_out' 目录下的文件中。

<font color="grey">selects all rows from partition ds=2008-08-15 of the invites table into an HDFS directory. The result data is in files (depending on the number of mappers) in that directory.</font>

注意：如果有分区列，则使用 `*` 选择。也可以在 projection 子句中指定。

<font color="grey">NOTE: partition columns if any are selected by the use of `*`. They can also be specified in the projection clauses.</font>

<font color="grey">Partitioned tables must always have a partition selected in the WHERE clause of the statement.</font>

分区表必须要在 WHERE 子句中选择一个分区。

```sh
hive> INSERT OVERWRITE LOCAL DIRECTORY '/tmp/local_out' SELECT a.* FROM pokes a;
```

<font color="grey">selects all rows from pokes table into a local directory.</font>

```sh
hive> INSERT OVERWRITE TABLE events SELECT a.* FROM profiles a;
	
hive> INSERT OVERWRITE TABLE events SELECT a.* FROM profiles a WHERE a.key < 100;
	
hive> INSERT OVERWRITE LOCAL DIRECTORY '/tmp/reg_3' SELECT a.* FROM events a;
	
hive> INSERT OVERWRITE DIRECTORY '/tmp/reg_4' select a.invites, a.pokes FROM profiles a;
	
hive> INSERT OVERWRITE DIRECTORY '/tmp/reg_5' SELECT COUNT(*) FROM invites a WHERE a.ds='2008-08-15';
	
hive> INSERT OVERWRITE DIRECTORY '/tmp/reg_5' SELECT a.foo, a.bar FROM invites a;
	
hive> INSERT OVERWRITE LOCAL DIRECTORY '/tmp/sum' SELECT SUM(a.pc) FROM pc1 a;
```

没有包含 HIVE-287 的 Hive 版本，使用 `COUNT(1)` ，而不是 `COUNT(*)`。

<font color="grey">selects the sum of a column. The avg, min, or max can also be used. Note that for versions of Hive which don't include [HIVE-287](https://issues.apache.org/jira/browse/HIVE-287), you'll need to use COUNT(1) in place of `COUNT(*)`.</font>

#### 4.1.2、GROUP BY

```sh
hive> FROM invites a INSERT OVERWRITE TABLE events SELECT a.bar, count(*) WHERE a.foo > 0 GROUP BY a.bar;

hive> INSERT OVERWRITE TABLE events SELECT a.bar, count(*) FROM invites a WHERE a.foo > 0 GROUP BY a.bar;
```
<font color="grey">Note that for versions of Hive which don't include [HIVE-287](https://issues.apache.org/jira/browse/HIVE-287), you'll need to use COUNT(1) in place of `COUNT(*)`.</font>

#### 4.1.3、JOIN

```sh
hive> FROM pokes t1 JOIN invites t2 ON (t1.bar = t2.bar) INSERT OVERWRITE TABLE events SELECT t1.bar, t1.foo, t2.foo;
```

#### 4.1.4、MULTITABLE INSERT

```sql
	FROM src
	  INSERT OVERWRITE TABLE dest1 SELECT src.* WHERE src.key < 100
	  INSERT OVERWRITE TABLE dest2 SELECT src.key, src.value WHERE src.key >= 100 and src.key < 200
	  INSERT OVERWRITE TABLE dest3 PARTITION(ds='2008-04-08', hr='12') SELECT src.key WHERE src.key >= 200 and src.key < 300
	  INSERT OVERWRITE LOCAL DIRECTORY '/tmp/dest4.out' SELECT src.value WHERE src.key >= 300;	
```

#### 4.1.5、STREAMING

```sh
hive> FROM invites a INSERT OVERWRITE TABLE events SELECT TRANSFORM(a.foo, a.bar) AS (oof, rab) USING '/bin/cat' WHERE a.ds > '2008-08-09';
```

<font color="grey">This streams the data in the map phase through the script /bin/cat (like Hadoop streaming).</font>

<font color="grey">Similarly – streaming can be used on the reduce side (please see the [Hive Tutorial](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-Custommap%2Freducescripts) for examples).</font>

## 5、Simple Example Use Cases

### 5.1、MovieLens User Ratings

<font color="grey">First, create a table with tab-delimited text file format:</font>

建表：

```sql
CREATE TABLE u_data (
  userid INT,
  movieid INT,
  rating INT,
  unixtime STRING)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE;
```

<font color="grey">Then, download the data files from MovieLens 100k on the [GroupLens datasets](http://grouplens.org/datasets/movielens/) page (which also has a README.txt file and index of unzipped files):</font>

下载数据文件：

	wget http://files.grouplens.org/datasets/movielens/ml-100k.zip

or:

	curl --remote-name http://files.grouplens.org/datasets/movielens/ml-100k.zip

<font color="grey">Note:  If the link to [GroupLens datasets](http://grouplens.org/datasets/movielens/) does not work, please report it on [HIVE-5341](https://issues.apache.org/jira/browse/HIVE-5341) or send a message to the user@hive.apache.org mailing list.</font>

<font color="grey">Unzip the data files:</font>

解压数据文件：

	unzip ml-100k.zip

<font color="grey">And load u.data into the table that was just created:</font>

载入 u.data 到表中：

```sql
LOAD DATA LOCAL INPATH '<path>/u.data'
OVERWRITE INTO TABLE u_data;
```

<font color="grey">Count the number of rows in table u_data:</font>

统计 u_data 的行数：

```sql
SELECT COUNT(*) FROM u_data;
```

<font color="grey">Note that for older versions of Hive which don't include [HIVE-287](https://issues.apache.org/jira/browse/HIVE-287), you'll need to use COUNT(1) in place of `COUNT(*)`.</font>

<font color="grey">Now we can do some complex data analysis on the table u_data:</font>

<font color="grey">Create weekday_mapper.py:</font>

```python
import sys
import datetime

for line in sys.stdin:
  line = line.strip()
  userid, movieid, rating, unixtime = line.split('\t')
  weekday = datetime.datetime.fromtimestamp(float(unixtime)).isoweekday()
  print '\t'.join([userid, movieid, rating, str(weekday)])
```

<font color="grey">Use the mapper script:</font>

```sql
CREATE TABLE u_data_new (
  userid INT,
  movieid INT,
  rating INT,
  weekday INT)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t';

add FILE weekday_mapper.py;

INSERT OVERWRITE TABLE u_data_new
SELECT
  TRANSFORM (userid, movieid, rating, unixtime)
  USING 'python weekday_mapper.py'
  AS (userid, movieid, rating, weekday)
FROM u_data;

SELECT weekday, COUNT(*)
FROM u_data_new
GROUP BY weekday;
```

<font color="grey">Note that if you're using Hive 0.5.0 or earlier you will need to use COUNT(1) in place of `COUNT(*)`.</font>

### 5.2、Apache Weblog Data

<font color="grey">The format of Apache weblog is customizable, while most webmasters use the default.
For default Apache weblog, we can create a table with the following command.</font>

Apache weblog 的格式是可定制的，而大多数 web 管理员使用默认格式。

对于默认的 Apache weblog，我们可以使用以下命令创建一个表。

<font color="grey">More about RegexSerDe can be found here in [HIVE-662](https://issues.apache.org/jira/browse/HIVE-662) and [HIVE-1719](https://issues.apache.org/jira/browse/HIVE-1719).</font>

```sql
CREATE TABLE apachelog (
  host STRING,
  identity STRING,
  user STRING,
  time STRING,
  request STRING,
  status STRING,
  size STRING,
  referer STRING,
  agent STRING)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.RegexSerDe'
WITH SERDEPROPERTIES (
  "input.regex" = "([^]*) ([^]*) ([^]*) (-|\\[^\\]*\\]) ([^ \"]*|\"[^\"]*\") (-|[0-9]*) (-|[0-9]*)(?: ([^ \"]*|\".*\") ([^ \"]*|\".*\"))?"
)
STORED AS TEXTFILE;
```