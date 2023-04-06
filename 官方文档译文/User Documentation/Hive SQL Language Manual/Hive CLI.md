# LanguageManual Cli

[TOC]

## 1、Hive CLI

> $HIVE_HOME/bin/hive is a shell utility which can be used to run Hive queries in either interactive or batch mode.

`$HIVE_HOME/bin/hive` 是一个 shell 工具，可以交互或批量执行 hive 查询。 

### 1.1、Deprecation in favor of Beeline CLI

> HiveServer2 (introduced in Hive 0.11) has its own CLI called [Beeline](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-Beeline%E2%80%93NewCommandLineShell), which is a JDBC client based on SQLLine.  Due to new development being focused on HiveServer2, [Hive CLI](https://issues.apache.org/jira/browse/HIVE-10304) will soon be deprecated in favor of Beeline ([HIVE-10511](https://issues.apache.org/jira/browse/HIVE-10511)).

HiveServer2 (在 Hive 0.11 中引入)有自己的 CLI 叫做 Beeline，这是一个基于S QLLine 的 JDBC 客户端。由于新的开发集中在 HiveServer2 上，Hive CLI 将很快被 Beeline 取代。

> See [Replacing the Implementation of Hive CLI Using Beeline](https://cwiki.apache.org/confluence/display/Hive/Replacing+the+Implementation+of+Hive+CLI+Using+Beeline) and [Beeline – New Command Line Shell](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-Beeline%E2%80%93NewCommandLineShell) in the HiveServer2 documentation.

#### 1.1.1、Hive Command Line Options

> To get help, run "hive -H" or "hive --help".

`hive -H` 、 `hive --help` 查看帮助。

> Usage (as it is in Hive 0.9.0):

	usage: hive
	 
	 -d,--define <key=value>          Variable substitution to apply to Hive
	                                  commands. e.g. -d A=B or --define A=B
	 -e <quoted-query-string>         SQL from command line
	 -f <filename>                    SQL from files
	 -H,--help                        Print help information
	 -h <hostname>                    Connecting to Hive Server on remote host
	    --hiveconf <property=value>   Use value for given property
	    --hivevar <key=value>         Variable substitution to apply to hive
	                                  commands. e.g. --hivevar A=B
	 -i <filename>                    Initialization SQL file
	 -p <port>                        Connecting to Hive Server on port number
	 -S,--silent                      Silent mode in interactive shell
	 -v,--verbose                     Verbose mode (echo executed SQL to the
	                                  console)

> Version information. As of Hive 0.10.0 there is one additional command line option:

版本信息：从 Hive 0.10.0 开始，有一个额外命令行选项。

	--database <dbname>      Specify the database to use

> Note: The variant "-hiveconf" is supported as well as "--hiveconf".

注意：支持`-hiveconf`、`--hivecon`

##### 1.1.1.1、Examples

> See [Variable Substitution](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+VariableSubstitution) for examples of using the hiveconf option.

使用 hiveconf 选项的示例见 Variable Substitution

> Example of running a query from the command line

从命令行运行查询

	$HIVE_HOME/bin/hive -e 'select a.col from tab1 a'

> Example of setting Hive configuration variables

设置 Hive 配置变量

	$HIVE_HOME/bin/hive -e 'select a.col from tab1 a' --hiveconf hive.exec.scratchdir=/home/my/hive_scratch  --hiveconf mapred.reduce.tasks=32

> Example of dumping data out from a query into a file using silent mode

使用静默模式将查询结果转存到文件

	$HIVE_HOME/bin/hive -S -e 'select a.col from tab1 a' > a.txt

> Example of running a script non-interactively from local disk

在非交互模式下，运行一个本地磁盘上的脚本

	$HIVE_HOME/bin/hive -f /home/my/hive-script.sql

> Example of running a script non-interactively from a Hadoop supported filesystem (starting in Hive 0.14)

在非交互模式下，运行一个 Hadoop 支持的文件系统上的脚本

	$HIVE_HOME/bin/hive -f hdfs://<namenode>:<port>/hive-script.sql
	$HIVE_HOME/bin/hive -f s3://mys3bucket/s3-script.sql 

> Example of running an initialization script before entering interactive mode

在进入交互模式前，运行一个初始化脚本

	$HIVE_HOME/bin/hive -i /home/my/hive-init.sql

##### 1.1.1.2、The hiverc File

> The CLI when invoked without the -i option will attempt to load $HIVE_HOME/bin/.hiverc and $HOME/.hiverc as initialization files.

不带 `-i` 选项调用 CLI 时，将尝试加载 `$HIVE_HOME/bin/.hiverc` 和 `$HOME/.hiverc` 作为初始化文件。

##### 1.1.1.3、Logging

> Hive uses log4j for logging. These logs are not emitted to the standard output by default but are instead captured to a log file specified by Hive's log4j properties file. By default Hive will use hive-log4j.default in the conf/ directory of the Hive installation which writes out logs to /tmp/<userid>/hive.log and uses the WARN level.

Hive 使用 log4j 进行日志记录

默认情况下，这些日志不会发送到标准输出，而是被捕获到 Hive 的 log4j 属性文件中指定的日志文件中。

默认情况下，Hive 使用 `conf/` 目录下的 hive-log4j.default 将日志写到 `/tmp/<userid>/hive.log`，使用 WARN 级别。

> It is often desirable to emit the logs to the standard output and/or change the logging level for debugging purposes. These can be done from the command line as follows:

通常希望将日志发送到标准输出和/或更改日志级别，以进行调试。

这些可以通过如下命令行完成:

	$HIVE_HOME/bin/hive --hiveconf hive.root.logger=INFO,console

> hive.root.logger specifies the logging level as well as the log destination. Specifying console as the target sends the logs to the standard error (instead of the log file).

`hive.root.logger` 指定日志记录级别以及日志目的地。

将控制台指定为日志目的地，将日志发送到标准错误(而不是日志文件)。

> See [Hive Logging in Getting Started](https://cwiki.apache.org/confluence/display/Hive/GettingStarted#GettingStarted-HiveLogging) for more information.

更多信息请参见 Hive Logging in Getting Started。

##### 1.1.1.4、Tool to Clear Dangling Scratch Directories

> See [Scratch Directory Management](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2#SettingUpHiveServer2-ScratchDirectoryManagement) in Setting Up HiveServer2 for information about scratch directories and a command-line [tool for removing dangling scratch directories](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2#SettingUpHiveServer2-ClearDanglingScratchDirTool) that can be used in the Hive CLI as well as HiveServer2.

请参阅 Setting Up HiveServer2 中的 Scratch Directory Management，了解关于 Scratch 目录的信息和一个命令行工具，用于删除悬空的 Scratch 目录，这些目录可以在 Hive CLI 和 HiveServer2 中使用。

#### 1.1.2、Hive Batch Mode Commands

> When $HIVE_HOME/bin/hive is run with the -e or -f option, it executes SQL commands in batch mode.

当运行 `$HIVE_HOME/bin/hive` 带有 `-e` 或 `-f` 选项时，它批量执行 SQL 命令：

- hive -e '<query-string>' executes the query string.
- hive -f <filepath> executes one or more SQL queries from a file.

> Version 0.14. As of Hive 0.14, <filepath> can be from one of the Hadoop supported filesystems (HDFS, S3, etc.) as well.

从 Hive 0.14 开始，`<filepath>` 可以是 Hadoop 支持的文件系统：

- $HIVE_HOME/bin/hive -f hdfs://<namenode>:<port>/hive-script.sql
- $HIVE_HOME/bin/hive -f s3://mys3bucket/s3-script.sql

> See [HIVE-7136](https://issues.apache.org/jira/browse/HIVE-7136) for more details.

#### 1.1.3、Hive Interactive Shell Commands

见原文：[https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveInteractiveShellCommands](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveInteractiveShellCommands)

##### 1.1.3.1、Hive Resources

> Hive can manage the addition of resources to a session where those resources need to be made available at query execution time. The resources can be files, jars, or archives. Any locally accessible file can be added to the session.

Hive 可以管理添加到会话的资源，这些资源需要在查询执行时可用。

这些资源可以是文件、jars 或归档文件。任何本地可访问的文件都可以添加到会话中。

> Once a resource is added to a session, Hive queries can refer to it by its name (in map/reduce/transform clauses) and the resource is available locally at execution time on the entire Hadoop cluster. Hive uses Hadoop's Distributed Cache to distribute the added resources to all the machines in the cluster at query execution time.

一旦一个资源被添加到一个会话中，Hive 查询就可以通过它的名字来引用它(在`map/reduce/transform`子句中)，并且该资源在整个 Hadoop 集群执行时本地可用。

Hive 使用 Hadoop 的分布式缓存，在查询执行时，将添加的资源分发到集群中的所有机器上。

Usage:

	ADD { FILE[S] | JAR[S] | ARCHIVE[S] } <filepath1> [<filepath2>]*

	LIST { FILE[S] | JAR[S] | ARCHIVE[S] } [<filepath1> <filepath2> ..]

	DELETE { FILE[S] | JAR[S] | ARCHIVE[S] } [<filepath1> <filepath2> ..] 

- FILE 资源只是添加到分布式缓存中。通常，这可能类似于要执行的转换脚本。

- JAR 资源也被添加到 Java 类路径中。为了引用它们包含的对象(比如UDFs)，这是必需的。

- ARCHIVE 资源作为分发它们步骤的一部分自动解档。

> FILE resources are just added to the distributed cache. Typically, this might be something like a transform script to be executed.

> JAR resources are also added to the Java classpath. This is required in order to reference objects they contain such as UDFs. See [Hive Plugins](https://cwiki.apache.org/confluence/display/Hive/HivePlugins) for more information about custom UDFs.

> ARCHIVE resources are automatically unarchived as part of distributing them.

Example:

	hive> add FILE /tmp/tt.py;
	hive> list FILES;
	/tmp/tt.py
	hive> select from networks a 
               MAP a.networkid 
               USING 'python tt.py' as nn where a.ds = '2009-01-04' limit 10;
 

> Version 1.2.0. As of [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-9664), resources can be added and deleted using [Ivy](http://ant.apache.org/ivy/) URLs of the form ivy://group:module:version?query_string.

从 Hive 1.2.0 开始，资源可以通过 Ivy URLs 的形式 `ivy://group:module:version?query_string` 来添加和删除。

- group - 模块来自哪个模块组。直接转换为 Maven groupId 或 Ivy 组织。

- module — 要加载的模块的名称。直接转化为 Maven artifactId 或 Ivy artifact。

- version - 要使用的模块的版本。任何版本或`*`(最新)或 Ivy Range 都可以使用。

> group – Which module group the module comes from. Translates directly to a Maven groupId or an Ivy Organization. 

> module – The name of the module to load. Translates directly to a Maven artifactId or an Ivy artifact.

> version – The version of the module to use. Any version or * (for latest) or an Ivy Range can be used.

> Various parameters can be passed in the query_string to configure how and which jars are added to the artifactory. The parameters are in the form of key value pairs separated by '&'.

可以在 query_string 中传递各种参数，以配置如何以及哪些 jars 添加到 artifactory。参数以键值对的形式存在，键值对用 `&` 分隔。

Usage:

	ADD { FILE[S] | JAR[S] | ARCHIVE[S] } <ivy://org:module:version?key=value&key=value&...> <ivy://org:module:version?key=value&key1=value1&...>*

	DELETE { FILE[S] | JAR[S] | ARCHIVE[S] } <ivy://org:module:version> <ivy://org:module:version>*

> Also, we can mix <ivyurl> and <filepath> in the same ADD and DELETE commands.

同样，我们可以在 ADD 和 DELETE 命令中混合 `<ivyurl>`、`<filepath>`。

	ADD { FILE[S] | JAR[S] | ARCHIVE[S] } { <ivyurl> | <filepath> } <ivyurl>* <filepath>* 

	DELETE { FILE[S] | JAR[S] | ARCHIVE[S] } { <ivyurl> | <filepath> } <ivyurl>* <filepath>*

> The different parameters that can be passed are:

可以传递的不同参数：

- exclude：以逗号分隔的 org:module 的形式的值。

- transitive：接受 true 或 false。默认值为 true。当 `transitive = true`时，所有传递的依赖项都会被下载并添加到类路径中。

- ext：默认情况下，要添加的文件的扩展名，默认是 `jar`。

- classifier：要解析的 maven 分类器。

> 1.exclude: Takes a comma separated value of the form org:module.
> 2.transitive: Takes values true or false. Defaults to true. When transitive = true, all the transitive dependencies are downloaded and added to the classpath.
> 3.ext: The extension of the file to add. 'jar' by default.
> 4.classifier: The maven classifier to resolve by.

Examples:

	hive>ADD JAR ivy://org.apache.pig:pig:0.10.0?exclude=org.apache.hadoop:avro;

	hive>ADD JAR ivy://org.apache.pig:pig:0.10.0?exclude=org.apache.hadoop:avro&transitive=false;

> The DELETE command will delete the resource and all its transitive dependencies unless some dependencies are shared by other resources. If two resources share a set of transitive dependencies and one of the resources is deleted using the DELETE syntax, then all the transitive dependencies will be deleted for the resource except the ones which are shared.

DELETE 命令将删除该资源及其所有传递的依赖，除非某些依赖被其他资源共享。

如果两个资源共享一组传递的依赖项，并且使用 DELETE 语法删除其中一个资源，那么除了共享的依赖项外，该资源的所有传递的依赖项都将被删除。

Examples:

	hive>ADD JAR ivy://org.apache.pig:pig:0.10.0
	hive>ADD JAR ivy://org.apache.pig:pig:0.11.1.15
	hive>DELETE JAR ivy://org.apache.pig:pig:0.10.0

> If A is the set containing the transitive dependencies of pig-0.10.0 and B is the set containing the transitive dependencies of pig-0.11.1.15, then after executing the above commands, A-(A intersection B) will be deleted.

如果 A 是包含 pig-0.10.0 的传递依赖的集合，B 是包含 pig-0.11.1.15 传递的依赖的集合，则执行上述命令后，A-(A intersection B) 将被删除。

> See [HIVE-9664](https://issues.apache.org/jira/browse/HIVE-9664?focusedCommentId=14338279&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-14338279) for more details.

> It is not neccessary to add files to the session if the files used in a transform script are already available on all machines in the Hadoop cluster using the same path name. For example:

如果转换脚本中使用的文件在 Hadoop 集群中使用相同的路径名的所有机器上都可用，那么就没有必要向会话添加文件。

例如:

- ... MAP a.networkid USING 'wc -l' ... 

	wc 是一个可在所有机器上使用的可执行文件。

> Here wc is an executable available on all machines.

- ... MAP a.networkid USING '/home/nfsserv1/hadoopscripts/tt.py' ... 

	可以通过在所有集群节点上相同配置的 NFS 挂载点访问 tt.py。

> Here tt.py may be accessible via an NFS mount point that's configured identically on all the cluster nodes.

> Note that Hive configuration parameters can also specify jars, files, and archives. See [Configuration Variables](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-ConfigurationVariables) for more information.

Hive 配置参数也可以指定 jars、files 和 archives。有关更多信息，请参阅 Configuration Variables。

## 2、HCatalog CLI

> Version. HCatalog is installed with Hive, starting with Hive release 0.11.0.

从 Hive 0.11.0 版本开始，HCatalog 是与 Hive 一起安装的。

> Many (but not all) hcat commands can be issued as hive commands, and vice versa. See the HCatalog [Command Line Interface](https://cwiki.apache.org/confluence/display/Hive/HCatalog+CLI) document in the [HCatalog manual](https://cwiki.apache.org/confluence/display/Hive/HCatalog) for more information.

许多(但不是所有) hcat 命令可以作为 hive 命令发出，反之亦然。