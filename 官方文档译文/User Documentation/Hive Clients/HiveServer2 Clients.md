# HiveServer2 Clients

[TOC]

> This page describes the different clients supported by [HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2).  Other documentation for HiveServer2 includes:

本页介绍 HiveServer2 支持的不同客户端，关于 HiveServer2 的其他描述文档见如下：

- [HiveServer2 Overview](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Overview)
- [Setting Up HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2)
- [Hive Configuration Properties:  HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-HiveServer2)

> Version:Introduced in Hive version 0.11. See [HIVE-2935](https://issues.apache.org/jira/browse/HIVE-2935).

版本：在 Hive 0.11 引入。

## 1、Beeline – Command Line Shell

> HiveServer2 supports a command shell Beeline that works with HiveServer2. It's a JDBC client that is based on the SQLLine CLI ([http://sqlline.sourceforge.net/](http://sqlline.sourceforge.net/)). There’s detailed [documentation](http://sqlline.sourceforge.net/#manual) of SQLLine which is applicable to Beeline as well.
 
**HiveServer2 支持 Beeline，它是一个基于 SQLLine CLI 的 JDBC 客户端**。SQLLine 有详细的文档，也适用于 Beeline。

> [Replacing the Implementation of Hive CLI Using Beeline](https://cwiki.apache.org/confluence/display/Hive/Replacing+the+Implementation+of+Hive+CLI+Using+Beeline)

使用 Beeline 替换 Hive CLI 的实现

> The Beeline shell works in both embedded mode as well as remote mode. In the embedded mode, it runs an embedded Hive (similar to [Hive CLI](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli)) whereas remote mode is for connecting to a separate HiveServer2 process over Thrift. Starting in [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-7615), when Beeline is used with HiveServer2, it also prints the log messages from HiveServer2 for queries it executes to STDERR. Remote HiveServer2 mode is recommended for production use, as it is more secure and doesn't require direct HDFS/metastore access to be granted for users.

Beeline shell 工作模式**有嵌入式模式和远程模式**。

在嵌入式模式中，它运行嵌入式 Hive (类似于 Hive CLI)，而远程模式通过 Thrift 连接到单独的 HiveServer2 进程。

从 Hive 0.14 开始，当 Beeline 与 HiveServer2 一起使用时，它**还打印来自 HiveServer2 的日志消息，用于它对 STDERR 的查询**。

**远程 HiveServer2 模式推荐在生产使用，因为它更安全，不需要为用户授予直接的 HDFS/metastore 访问权限**。

> In remote mode HiveServer2 only accepts valid Thrift calls – even in HTTP mode, the message body contains Thrift payloads.

在远程模式下，HiveServer2 只接受有效的 Thrift 调用---即使在 HTTP 模式下，消息体也包含 Thrift 有效负载。

### 1.1、Beeline Example

	% bin/beeline 
	Hive version 0.11.0-SNAPSHOT by Apache
	beeline> !connect jdbc:hive2://localhost:10000 scott tiger
	!connect jdbc:hive2://localhost:10000 scott tiger 
	Connecting to jdbc:hive2://localhost:10000
	Connected to: Hive (version 0.10.0)
	Driver: Hive (version 0.10.0-SNAPSHOT)
	Transaction isolation: TRANSACTION_REPEATABLE_READ
	0: jdbc:hive2://localhost:10000> show tables;
	show tables;
	+-------------------+
	|     tab_name      |
	+-------------------+
	| primitives        |
	| src               |
	| src1              |
	| src_json          |
	| src_sequencefile  |
	| src_thrift        |
	| srcbucket         |
	| srcbucket2        |
	| srcpart           |
	+-------------------+
	9 rows selected (1.079 seconds)


> You can also specify the connection parameters on command line. This means you can find the command with the connection string from your UNIX shell history. 

你也**可以在命令行指定连接参数**。这就意味着你能从你的 UNIX shell 历史中的连接字符串中找到命令。

	% beeline -u jdbc:hive2://localhost:10000/default -n scott -w password_file

	Hive version 0.11.0-SNAPSHOT by Apache

	Connecting to jdbc:hive2://localhost:10000/default

Beeline with NoSASL connection

> If you'd like to connect via NOSASL mode, you must specify the authentication mode explicitly:

**如果你想通过 NOSASL 模式连接，你必须明确的指定授权模式：**

	% bin/beeline
	beeline> !connect jdbc:hive2://<host>:<port>/<db>;auth=noSasl hiveuser pass 

### 1.2、Beeline Commands

Commands | Description
---|:---
`!<SQLLine command>` | List of SQLLine commands available at [http://sqlline.sourceforge.net/](http://sqlline.sourceforge.net/).`Example: !quit exits the Beeline client`.【SQLLine 命令列表】
!delimiter | Set the delimiter for queries written in Beeline. Multi-character delimiters are allowed, but quotation marks, slashes, and -- are not allowed. Defaults to ;`Usage: !delimiter $$` `Version: 3.0.0 ([HIVE-10865](https://issues.apache.org/jira/browse/HIVE-10865))`【为在Beeline编写的查询，设置分隔符。允许使用多字符分隔符，但不允许使用引号、斜杠和--。默认为;】

### 1.3、Beeline Properties

Commands | Description
---|:---
fetchsize	|   Standard JDBC enables you to specify the number of rows fetched with each database round-trip for a query, and this number is referred to as the fetch size.Setting the fetch size in Beeline overrides the JDBC driver's default fetch size and affects subsequent statements executed in the current session. 1.A value of -1 instructs Beeline to use the JDBC driver's default fetch size (default) 2.A value of zero or more is passed to the JDBC driver for each statement 3.Any other negative value will throw an Exception

**标准的 JDBC 能使你指定每次数据库往返获取的行的数值，这个数字称为 fetchsize**。

在 Beeline 中设置 fetchsize 将覆盖 JDBC 驱动程序的默认的 fetchsize，并影响在当前会话中执行的后续语句。

	1.值为 -1 表示 Beeline 使用 JDBC 驱动程序的默认 fetchsize(默认)

	2.为每个语句传递一个 0 或更多的值给 JDBC 驱动程序

	3.任何其他负数都将抛出异常

Usage: 

	!set fetchsize 200

Version: 

	4.0.0 ([HIVE-22853](https://issues.apache.org/jira/browse/HIVE-22853))	

### 1.4、Beeline Hive Commands

> Hive specific commands (same as [Hive CLI commands](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveInteractiveShellCommands)) can be run from Beeline, when the Hive JDBC driver is used.

当使用 Hive JDBC 驱动程序时，可以在 Beeline 运行特定于 Hive 的命令(与Hive CLI命令相同)。

> Use ";" (semicolon) to terminate commands. Comments in scripts can be specified using the "--" prefix.

使用 `;` 终止命令。可以使用 `--` 前缀指定脚本中的注释。

Commands | Description
---|:---
reset | Resets the configuration to the default values. 【重置默认值】
reset `<key>`	| Resets the value of a particular configuration variable (key) to the default value. Note: If you misspell the variable name, Beeline will not show an error. 【重置指定的配置变量key的默认值。注意：如果变量名拼写错误，不会报错。】
set `<key>=<value>` | Sets the value of a particular configuration variable (key). Note: If you misspell the variable name, Beeline will not show an error. 【设置指定的配置变量key的值。注意：如果变量名拼写错误，不会报错。】
set | Prints a list of configuration variables that are overridden by the user or Hive. 【打印由用户或Hive重写的配置变量列表。】
set -v | Prints all Hadoop and Hive configuration variables. 【打印Hadoop、Hive所有的配置变量】
add FILE[S] `<filepath> <filepath>*` \ add JAR[S] `<filepath> <filepath>*`\ add ARCHIVE[S] `<filepath> <filepath>*` | Adds one or more files, jars, or archives to the list of resources in the distributed cache. See [Hive Resources](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveResources) for more information.【添加一个或更多的文件、jars、archives到分布式缓存中的资源列表中。】
add FILE[S] `<ivyurl> <ivyurl>*` \ add JAR[S] `<ivyurl> <ivyurl>*` \ add ARCHIVE[S] `<ivyurl> <ivyurl>*` | As of [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-9664), adds one or more files, jars or archives to the list of resources in the distributed cache using an [Ivy](http://ant.apache.org/ivy/) URL of the form ivy://group:module:version?query_string. See [Hive Resources](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveResources) for more information.【在Hive1.2.0中，使用形式为`Ivy://group:module:version?query_string`的Ivy URL向分布式缓存中的资源列表添加一个或多个文件、jar或archives。】
list FILE[S] \ list JAR[S] \ list ARCHIVE[S] | Lists the resources already added to the distributed cache. See Hive Resources for more information. (As of Hive 0.14.0: [HIVE-7592](https://issues.apache.org/jira/browse/HIVE-7592)).【列出已被添加到分布式缓存中的资源】
list FILE[S] `<filepath>*`\ list JAR[S] `<filepath>*` \ list ARCHIVE[S] `<filepath>*` | Checks whether the given resources are already added to the distributed cache or not. See Hive Resources for more information.【检查给定的资源是否已被添加到分布式缓存中】
delete FILE[S] `<filepath>*` \ delete JAR[S] `<filepath>*` \ delete ARCHIVE[S] `<filepath>*` | Removes the resource(s) from the distributed cache. 【移除分布式缓存中的资源】
delete FILE[S] `<ivyurl> <ivyurl>*` \ delete JAR[S] `<ivyurl> <ivyurl>*` \ delete ARCHIVE[S] `<ivyurl> <ivyurl>*` | As of [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-9664), removes the resource(s) which were added using the `<ivyurl>` from the distributed cache. See Hive Resources for more information.【在Hive1.2.0中，从分布式缓存中删除使用`<ivyurl>`添加的资源。】
reload | As of [Hive 0.14.0](https://issues.apache.org/jira/browse/HIVE-7553), makes HiveServer2 aware of any jar changes in the path specified by the configuration parameter [hive.reloadable.aux.jars.path](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.reloadable.aux.jars.path) (without needing to restart HiveServer2). The changes can be adding, removing, or updating jar files.【从Hive0.14.0开始，HiveServer2可以感知到配置参数`hive.reloadable.aux.jars.path`指定的路径中的任何jar更改(不需要重启HiveServer2)。更改可以是添加、删除或更新jar文件。】
dfs `<dfs command>` | Executes a dfs command. 【执行dfs命令】
`<query string>` | Executes a Hive query and prints results to standard output.【执行hive查询，打印结果到标准输出】

### 1.5、Beeline Command Options

> The Beeline CLI supports these command line options:

Option | Description
---|:---
-u `<database URL>` | The JDBC URL to connect to. Special characters in parameter values should be encoded with URL encoding if needed.`Usage: beeline -u db_URL` 【要连接的JDBC-URL。如果需要，参数值中的特殊字符应该用URL编码进行编码。】
-r | [Reconnect](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-Reconnecting) to last used URL (if a user has previously used !connect to a URL and used !save to a beeline.properties file).`Usage: beeline -r`;Version: 2.1.0 ([HIVE-13670](https://issues.apache.org/jira/browse/HIVE-13670))【重新连接最后一个使用的URL(如果用户以前使用过`!connect`连接到一个URL，并使用`!save`保存到beeline.properties文件)】
-n `<username>` | The username to connect as.`Usage: beeline -n valid_user` 【连接的用户名。】
-p `<password>` | The password to connect as.`Usage: beeline -p valid_password`.Optional password mode:Starting Hive 2.2.0 ([HIVE-13589](https://issues.apache.org/jira/browse/HIVE-13589)) the argument for -p option is optional.`Usage : beeline -p [valid_password]`If the password is not provided after -p Beeline will prompt for the password while initiating the connection. When password is provided Beeline uses it initiate the connection without prompting.【连接的密码。可选的密码模式：从Hive2.2.0开始，`-p`参数是可选的。如果在`-p`后没有提供密码，当初始化连接时，Beeline将提示输入密码。当提供了密码，Beeline直接使用它初始化连接，不需要提示。】
-d `<driver class>` | The driver class to use.`Usage: beeline -d driver_class`【使用的驱动类】
-e `<query>` | Query that should be executed. Double or single quotes enclose the query string. This option can be specified multiple times.`Usage: beeline -e "query_string"`Support to run multiple SQL statements separated by semicolons in a single query_string:1.2.0 ([HIVE-9877](https://issues.apache.org/jira/browse/HIVE-9877)) .Bug fix (null pointer exception): 0.13.0 ([HIVE-5765](https://issues.apache.org/jira/browse/HIVE-5765)).Bug fix (--headerInterval not honored): 0.14.0 ([HIVE-7647](https://issues.apache.org/jira/browse/HIVE-7647)).Bug fix (running -e in background): 1.3.0 and 2.0.0 ([HIVE-6758](https://issues.apache.org/jira/browse/HIVE-6758)); [workaround available](https://issues.apache.org/jira/browse/HIVE-6758?focusedCommentId=13954968&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13954968) for earlier versions.【要被执行的查询语句，使用双引号或单引号将查询字符串括起来。支持在一个query_string中使用多个查询语句】
-f `<file>`	| Script file that should be executed.`Usage: beeline -f filepath`.Version: 0.12.0 ([HIVE-4268](https://issues.apache.org/jira/browse/HIVE-4268)).Note: If the script contains tabs, query compilation fails in version 0.12.0. This bug is fixed in version 0.13.0 ([HIVE-6359](https://issues.apache.org/jira/browse/HIVE-6359)).Bug fix (running -f in background): 1.3.0 and 2.0.0 ([HIVE-6758](https://issues.apache.org/jira/browse/HIVE-6758)); [workaround available](https://issues.apache.org/jira/browse/HIVE-6758?focusedCommentId=13954968&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-13954968) for earlier versions.【要被执行的脚本文件】
-i (or) --init `<file or files>` | The init files for initialization.`Usage: beeline -i /tmp/initfile`.Single file:Version: 0.14.0 ([HIVE-6561](https://issues.apache.org/jira/browse/HIVE-6561)).Multiple files:Version: 2.1.0 ([HIVE-11336](https://issues.apache.org/jira/browse/HIVE-11336)).【初始化文件】
-w (or) --password-file `<password file>` | The password file to read password from.Version: 1.2.0 ([HIVE-7175](https://issues.apache.org/jira/browse/HIVE-7175)) 【用来读取密码的密码文件】
-a (or) --authType `<auth type>` | The authentication type passed to the jdbc as an auth property.Version: 0.13.0 ([HIVE-5155](https://issues.apache.org/jira/browse/HIVE-5155))【作为身份验证属性传递给jdbc的身份验证类型】
--property-file `<file>` | File to read configuration properties from.`Usage: beeline --property-file /tmp/a`.Version: 2.2.0 ([HIVE-13964](https://issues.apache.org/jira/browse/HIVE-13964)) 【读取配置属性的文件】
--hiveconf property=value | Use value for the given configuration property. Properties that are listed in hive.conf.restricted.list cannot be reset with hiveconf (see [Restricted List and Whitelist](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-RestrictedListandWhitelist)).`Usage: beeline --hiveconf prop1=value1`.Version: 0.13.0 ([HIVE-6173](https://issues.apache.org/jira/browse/HIVE-6173))【设置指定的属性值。`hive.conf.restricted.list`列出的属性不能使用hiveconf重置】
--hivevar name=value | Hive variable name and value. This is a Hive-specific setting in which variables can be set at the session level and referenced in Hive commands or queries.`Usage: beeline --hivevar var1=value1`【hive变量的名字和值。hive的特定设置，其中变量可以在会话级别设置,并在Hive命令或查询中引用。】
--color=[true/false] | Control whether color is used for display. Default is false.`Usage: beeline --color=true`(Not supported for Separated-Value Output formats. See [HIVE-9770](https://issues.apache.org/jira/browse/HIVE-9770))【控制着是否展示颜色，默认false】
--showHeader=[true/false] | Show column names in query results (true) or not (false). Default is true.`Usage: beeline --showHeader=false`【是否展示查询结果中的列名，默认true】
--headerInterval=ROWS | The interval for redisplaying column headers, in number of rows, when outputformat is table. Default is 100.`Usage: beeline --headerInterval=50`.(Not supported for Separated-Value Output formats. See [HIVE-9770](https://issues.apache.org/jira/browse/HIVE-9770))【输出格式是表时，再次展示列headers的行间隔，默认100】
--fastConnect=[true/false]	| When connecting, skip building a list of all tables and columns for tab-completion of HiveQL statements (true) or build the list (false). Default is true.`Usage: beeline --fastConnect=false`.【在连接时，跳过为HiveQL语句的tab-completion构建所有表和列的列表(true)，或构建列表(false)。】
--autoCommit=[true/false] | Enable/disable automatic transaction commit. Default is false.`Usage: beeline --autoCommit=true`【启用或禁用自动事务提交，默认false】
--verbose=[true/false]	| Show verbose error messages and debug information (true) or do not show (false). Default is false.`Usage: beeline --verbose=true` 【是否展示详细的错误信息和调试信息】
--showWarnings=[true/false]	| Display warnings that are reported on the connection after issuing any HiveQL commands. Default is false.`Usage: beeline --showWarnings=true`.【执行完HiveQL命令后，是否展示在连接上报告的warnings信息，默认false】
--showDbInPrompt=[true/false] | Display the current database name in prompt. Default is false.`Usage: beeline --showDbInPrompt=true`.Version: 2.2.0 ([HIVE-14123](https://issues.apache.org/jira/browse/HIVE-14123))【是否在提示中展示当前数据库名称，默认false】
--showNestedErrs=[true/false]	| Display nested errors. Default is false.`Usage: beeline --showNestedErrs=true` 【是否展示嵌套错误，默认false】
--numberFormat=[pattern] | Format numbers using a [DecimalFormat](http://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html) pattern.`Usage: beeline --numberFormat="#,###,##0.00"`【使用DecimalFormat模板格式化数字】
--force=[true/false] | Continue running script even after errors (true) or do not continue (false). Default is false.`Usage: beeline--force=true` 【判断出现错误后，是否继续运行脚本，默认false】
--maxWidth=MAXWIDTH	 | The maximum width to display before truncating data, in characters, when outputformat is table. Default is to query the terminal for current width, then fall back to 80.`Usage: beeline --maxWidth=150`【当输出格式是表时，在截取数据前，以字符形式展示的最大宽度。默认是查询终端的当前宽度，然后返回到80。】
--maxColumnWidth=MAXCOLWIDTH | The maximum column width, in characters, when outputformat is table. Default is 50 in Hive version 2.2.0+ (see [HIVE-14135](https://issues.apache.org/jira/browse/HIVE-14135)) or 15 in earlier versions.`Usage: beeline --maxColumnWidth=25` 【当输出格式是表时，以字符形式，最大的列宽。Hive2.2.0+版本中，默认是50，在更早版本中，默认是15】
--silent=[true/false]	| Reduce the amount of informational messages displayed (true) or not (false). It also stops displaying the log messages for the query from HiveServer2 (Hive 0.14 and later) and the HiveQL commands (Hive 1.2.0 and later). Default is false.`Usage: beeline --silent=true`.【是否减少显示信息的数量。它还停止显示来自HiveServer2 (Hive 0.14和更高版本)和HiveQL命令(Hive 1.2.0和更高版本)的查询日志消息。】
--autosave=[true/false]	| Automatically save preferences (true) or do not autosave (false). Default is false.`Usage: beeline --autosave=true`【是否自动保存偏好设置，默认false】
--outputformat=[table/vertical/csv/tsv/dsv/csv2/tsv2] | Format mode for result display. Default is table. See [HiveServer2 Clients#Separated-Value Output Formats](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-Separated-ValueOutputFormats) below for description of recommended sv options.`Usage: beeline --outputformat=tsv`.Version: dsv/csv2/tsv2 added in 0.14.0 ([HIVE-8615](https://issues.apache.org/jira/browse/HIVE-8615)). 【结果展示的格式。默认是表。】
--truncateTable=[true/false] | If true, truncates table column in the console when it exceeds console length.Version: 0.14.0 ([HIVE-6928](https://issues.apache.org/jira/browse/HIVE-6928)) 【如果是true，当超出控制台长度时，在控制台截断表的列】
--delimiterForDSV= DELIMITER | The delimiter for delimiter-separated values output format. Default is '|' character.Version: 0.14.0 ([HIVE-7390](https://issues.apache.org/jira/browse/HIVE-7390)) 【用于分隔输出格式的分隔符。默认是'|' 】
--isolation=LEVEL | Set the transaction isolation level to TRANSACTION_READ_COMMITTED or TRANSACTION_SERIALIZABLE. See the "Field Detail" section in the Java [Connection](http://docs.oracle.com/javase/7/docs/api/java/sql/Connection.html) documentation.`Usage: beeline --isolation=TRANSACTION_SERIALIZABLE` 【设置事务隔离的等级为TRANSACTION_READ_COMMITTED或TRANSACTION_SERIALIZABLE
--nullemptystring=[true/false]	| Use historic behavior of printing null as empty string (true) or use current behavior of printing null as NULL (false). Default is false.`Usage: beeline --nullemptystring=false`.Version: 0.13.0 ([HIVE-4485](https://issues.apache.org/jira/browse/HIVE-4485))【使用历史行为打印null为空字符串，或使用当前行为打印null为空。默认false】
--incremental=[true/false] | Defaults to true from Hive 2.3 onwards, before it defaulted to false. When set to false, the entire result set is fetched and buffered before being displayed, yielding optimal display column sizing. When set to true, result rows are displayed immediately as they are fetched, yielding lower latency and memory usage at the price of extra display column padding. Setting --incremental=true is recommended if you encounter an OutOfMemory on the client side (due to the fetched result set size being large).【从Hive2.3开始的版本默认为true，之前默认为false。当为false时，将在显示之前提取并缓冲整个结果集，从而产生最佳的显示列大小调整。当为true时，结果行在获取时立即显示，从而降低延迟和内存使用，但代价是额外的显示列填充。如果在客户端遇到OutOfMemory(由于获取的结果集大小很大)，建议设置`--incremental=true` 
--incrementalBufferRows=NUMROWS	| The number of rows to buffer when printing rows on stdout, defaults to 1000; only applicable if --incremental=true and --outputformat=table.`Usage: beeline --incrementalBufferRows=1000`.Version: 2.3.0 ([HIVE-14170](https://issues.apache.org/jira/browse/HIVE-14170)).【当在标准输出打印行时，缓存行的数值，默认是1000.只有`--incremental=true`和`--outputformat=table`时才应用。】
--maxHistoryRows=NUMROWS  | The maximum number of rows to store Beeline history.Version: 2.3.0 ([HIVE-15166](https://issues.apache.org/jira/browse/HIVE-15166)) 【存储Beeline历史的最大行数】
--delimiter=;  | Set the delimiter for queries written in Beeline. Multi-char delimiters are allowed, but quotation marks, slashes, and -- are not allowed. Defaults to ;`Usage: beeline --delimiter=$$`.Version: 3.0.0 ([HIVE-10865](https://issues.apache.org/jira/browse/HIVE-10865)) 【在Beeline中，为查询设置分隔符。允许多字符分隔符，但不允许引号、斜杠和 --。默认是`;`
--convertBinaryArrayToString=[true/false]	| Display binary column data as a string using the platform's default character set.The default behavior (false) is to display binary data .using: `Arrays.toString(byte[] columnValue)`.Version: 3.0.0 ([HIVE-14786](https://issues.apache.org/jira/browse/HIVE-14786)).Display binary column data as a string using the UTF-8 character set.The default behavior (false) is to display binary data using Base64 encoding without padding.Version: 4.0.0 ([HIVE-23856](https://issues.apache.org/jira/browse/HIVE-23856)).`Usage: beeline --convertBinaryArrayToString=true`.【使用平台默认是字符集展示二进制列数据为字符串，默认的行为是展示二进制数据。使用UTF-8字符集展示二进制列数据为字符串，默认行为是使用Base64编码显示二进制数据，而不使用填充】
--help | Display a usage message.`Usage: beeline --help`

### 1.6、Output Formats

> In Beeline, the result can be displayed in different formats. The format mode can be set with the outputformat option.

Beeline 中，结果可以以不同的格式展示。

> The following output formats are supported:

支持的输出格式如下所示：

- [HiveServer2 Clients#table](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-table)
- [HiveServer2 Clients#vertical](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-vertical)
- [HiveServer2 Clients#xmlattr](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-xmlattr)
- [HiveServer2 Clients#xmlelements](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-xmlelements)
- [HiveServer2 Clients#json](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-json)
- [HiveServer2 Clients#jsonfile](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-jsonfile)
- [separated-value formats](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-Separated-ValueOutputFormats) (csv, tsv, csv2, tsv2, dsv)

#### 1.6.1、table

> The result is displayed in a table. A row of the result corresponds to a row in the table and the values in one row are displayed in separate columns in the table.
This is the default format mode.

**结果展示在表中。结果的一行对于表中的一行，一行中的值以不同列在表中展示。这是默认的格式**。

> Result of the query select id, value, comment from test_table

下面是 `select id, value, comment from test_table` 的查询结果：

	+-----+---------+-----------------+
	| id  |  value  |     comment     |
	+-----+---------+-----------------+
	| 1   | Value1  | Test comment 1  |
	| 2   | Value2  | Test comment 2  |
	| 3   | Value3  | Test comment 3  |
	+-----+---------+-----------------+

#### 1.6.2、vertical

> Each row of the result is displayed in a block of key-value format, where the keys are the names of the columns.

**结果的每一行展示在一个键值对格式的块中，keys 是列的名称**。

> Result of the query select id, value, comment from test_table

下面是 `select id, value, comment from test_table` 的查询结果：

	id       1
	value    Value1
	comment  Test comment 1

	id       2
	value    Value2
	comment  Test comment 2

	id       3
	value    Value3
	comment  Test comment 3

#### 1.6.3、xmlattr

> The result is displayed in an XML format where each row is a "result" element in the XML.The values of a row are displayed as attributes on the "result" element. The names of the attributes are the names of the columns.

**结果展示在 XML 格式中，每行是 XML 的一个结果元素。一行的值作为结果元素的属性展示。属性的名称是列名**。

> Result of the query select id, value, comment from test_table

下面是 `select id, value, comment from test_table` 的查询结果：

	<resultset>
	  <result id="1" value="Value1" comment="Test comment 1"/>
	  <result id="2" value="Value2" comment="Test comment 2"/>
	  <result id="3" value="Value3" comment="Test comment 3"/>
	</resultset>

#### 1.6.4、xmlelements

> The result is displayed in an XML format where each row is a "result" element in the XML. The values of a row are displayed as child elements of the result element.

**结果展示在 XML 格式中，每行是 XML 的一个结果元素。一行的值作为结果元素的子元素展示**。

> Result of the query select id, value, comment from test_table

下面是 `select id, value, comment from test_table` 的查询结果：

	<resultset>
	  <result>
	    <id>1</id>
	    <value>Value1</value>
	    <comment>Test comment 1</comment>
	  </result>
	  <result>
	    <id>2</id>
	    <value>Value2</value>
	    <comment>Test comment 2</comment>
	  </result>
	  <result>
	    <id>3</id>
	    <value>Value3</value>
	    <comment>Test comment 3</comment>
	  </result>
	</resultset>

#### 1.6.5、json

> (Hive 4.0) The result is displayed in JSON format where each row is a "result" element in the JSON array "resultset".

**(Hive 4.0)结果展示在 JSON 格式中，每行是 JSON 数组"resultset"中的一个结果元素**。

> Result of the query select `String`, `Int`, `Decimal`, `Bool`, `Null`, `Binary` from test_table

下面是 `select String, Int, Decimal, Bool, Null, Binary from test_table` 的查询结果：

	{"resultset":[{"String":"aaa","Int":1,"Decimal":3.14,"Bool":true,"Null":null,"Binary":"SGVsbG8sIFdvcmxkIQ"},{"String":"bbb","Int":2,"Decimal":2.718,"Bool":false,"Null":null,"Binary":"RWFzdGVyCgllZ2cu"}]}


#### 1.6.6、jsonfile

> (Hive 4.0) The result is displayed in JSON format where each row is a distinct JSON object.  This matches the expected format for a table created as JSONFILE format.

**(Hive 4.0)结果展示在 JSON 格式中，每行是一个去重了的 JSON 对象。 这与 JSONFILE 格式的表的预期格式相匹配**。

> Result of the query select `String`, `Int`, `Decimal`, `Bool`, `Null`, `Binary` from test_table

下面是 `select String, Int, Decimal, Bool, Null, Binary from test_table` 的查询结果：

	{"String":"aaa","Int":1,"Decimal":3.14,"Bool":true,"Null":null,"Binary":"SGVsbG8sIFdvcmxkIQ"}
	{"String":"bbb","Int":2,"Decimal":2.718,"Bool":false,"Null":null,"Binary":"RWFzdGVyCgllZ2cu"}

#### 1.6.7、Separated-Value Output Formats

> The values of a row are separated by different delimiters.There are five separated-value output formats available: csv, tsv, csv2, tsv2 and dsv.

**一行的值被不同的分隔符划分。有5种类型：csv, tsv, csv2, tsv2 和 dsv**

##### 1.6.7.1、csv2, tsv2, dsv

> Starting with Hive 0.14 there are improved SV output formats available, namely dsv, csv2 and tsv2. These three formats differ only with the delimiter between cells, which is comma for csv2, tab for tsv2, and configurable for dsv.

从 Hive 0.14 开始，就存在高级的 SV 输出格式，名为 dsv、csv2 和 tsv2。

这三种格式不同点在于 cells 间的分隔符。**csv2 是逗号，tsv2 是制表键，dsv 是可配置的**。

> For the dsv format, the delimiter can be set with the delimiterForDSV option. The default delimiter is '|'.Please be aware that only single character delimiters are supported.

**对于 dsv 格式，可以使用 delimiterForDSV 选项设置分隔符。默认是 '|'**。仅支持单字符分隔符。

> Result of the query select id, value, comment from test_table

下面是 `select id, value, comment from test_table` 的查询结果：

csv2

	id,value,comment
	1,Value1,Test comment 1
	2,Value2,Test comment 2
	3,Value3,Test comment 3
 
tsv2

	id	value	comment
	1	Value1	Test comment 1
	2	Value2	Test comment 2
	3	Value3	Test comment 3

dsv (the delimiter is |)

	id|value|comment
	1|Value1|Test comment 1
	2|Value2|Test comment 2
	3|Value3|Test comment 3

###### 1.6.7.1.1、Quoting in csv2, tsv2 and dsv Formats

> If quoting is not disabled, double quotes are added around a value if it contains special characters (such as the delimiter or double quote character) or spans multiple lines.Embedded double quotes are escaped with a preceding double quote.

**如果未禁用引号，如果它包含了特殊字符(如分隔符或双引号字符)，或跨越多行，则给这个添加双引号**。

内嵌双引号用前面的双引号转义。

> The quoting can be disabled by setting the disable.quoting.for.sv system variable to true. If the quoting is disabled, no double quotes are added around the values (even if they contains special characters) and the embedded double quotes are not escaped.By default, the quoting is disabled.

可以**通过设置系统变量 `disable.quoting.for.sv` 为真，来禁用引号**。

如果禁用引号，则不会在值周围添加双引号(即使它们包含特殊字符)，而且嵌入的双引号不会转义。

默认情况下，引号是禁用的。
 
> Result of the query select id, value, comment from test_table

下面是 `select id, value, comment from test_table` 的查询结果：

> csv2, quoting is enabled

csv2，启用引号：

	id,value,comment
	1,"Value,1",Value contains comma
	2,"Value""2",Value contains double quote
	3,Value'3,Value contains single quote

> csv2, quoting is disabled

csv2，禁用引号：

	id,value,comment
	1,Value,1,Value contains comma
	2,Value"2,Value contains double quote
	3,Value'3,Value contains single quote

##### 1.6.7.2、csv, tsv

> These two formats differ only with the delimiter between values, which is comma for csv and tab for tsv. The values are always surrounded with single quote characters, even if the quoting is disabled by the disable.quoting.for.sv system variable.These output formats don't escape the embedded single quotes.Please be aware that these output formats are deprecated and only maintained for backward compatibility.

这两种格式的不同点在于值之间的分隔符，**csv 是逗号，tsv 是制表键**。

值是用单引号围绕，即使设置了`isable.quoting.for.sv`，将引号被禁用。

这些输出格式不会转义嵌入的单引号。

注意：这两类输出格式被弃用了，存在仅是为了向后兼容。

> Result of the query select id, value, comment from test_table

下面是 `select id, value, comment from test_table` 的查询结果：

csv

	'id','value','comment'
	'1','Value1','Test comment 1'
	'2','Value2','Test comment 2'
	'3','Value3','Test comment 3'

tsv

	'id'	'value'	'comment'
	'1'	'Value1'	'Test comment 1'
	'2'	'Value2'	'Test comment 2'
	'3'	'Value3'	'Test comment 3'

### 1.7、HiveServer2 Logging

> Starting with Hive 0.14.0, HiveServer2 operation logs are available for Beeline clients. These parameters configure logging:

从 Hive 0.14.0 开始，对 Beeline 客户端来说，HiveServer2 操作日志是可用的。这些参数配置日志:

- [hive.server2.logging.operation.enabled](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.logging.operation.enabled)
- [hive.server2.logging.operation.log.location](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.logging.operation.log.location)
- [hive.server2.logging.operation.verbose (Hive 0.14 to 1.1)](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.logging.operation.verbose)
- [hive.server2.logging.operation.level (Hive 1.2 onward)](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.logging.operation.level)
 
> [HIVE-11488](https://issues.apache.org/jira/browse/HIVE-11488) (Hive 2.0.0) adds the support of logging queryId and sessionId to HiveServer2 log file. To enable that, edit/add %X{queryId} and %X{sessionId} to the pattern format string of the logging configuration file.

Hive-11488 (Hive 2.0.0)为 HiveServer2 日志文件添加了对 logging queryId 和 sessionId 的支持。要实现这一点，请编辑/添加 `%X{queryId}` 和 `%X{sessionId}` 到日志配置文件的模式格式字符串中。

### 1.8、Cancelling the Query

> When a user enters CTRL+C on the Beeline shell, if there is a query which is running at the same time then Beeline attempts to cancel the query while closing the socket connection to HiveServer2. This behavior is enabled only when [hive.server2.close.session.on.disconnect](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.close.session.on.disconnect) is set to true. Starting from Hive 2.2.0 ([HIVE-15626](https://issues.apache.org/jira/browse/HIVE-15626)) Beeline does not exit the command line shell when the running query is being cancelled as a user enters CTRL+C. If the user wishes to exit the shell they can enter CTRL+C for the second time while the query is being cancelled. However, if there is no query currently running, the first CTRL+C will exit the Beeline shell. This behavior is similar to how the Hive CLI handles CTRL+C.

**当用户在 Beeline shell 上输入 CTRL+C 时，如果有一个查询同时在运行，那么 Beeline 会在关闭到 HiveServer2 的套接字连接时，尝试取消该查询**。

此行为仅在`hive.server2.close.session.on.disconnect=true`时启用。

**从 Hive 2.2.0 开始，当用户输入 CTRL+C 取消正在运行的查询时，Beeline 不会退出命令行 shell**。

如果用户希望退出 shell，他们可以在取消查询时再次输入 CTRL+C。但是，如果当前没有运行查询，第一个 CTRL+C 就会退出 Beeline shell。这种行为类似于 Hive CLI 处理 CTRL+C 的方式。

> !quit is the recommended command to exit the Beeline shell.

推荐使用 `!quit` 退出 Beeline shell

### 1.9、Background Query in Terminal Script

> Beeline can be run disconnected from a terminal for batch processing and automation scripts using commands such as nohup and disown.

Beeline 可以从终端断开连接，可以使用 nohup 和 disown 等命令进行批处理和自动化脚本。

> Some versions of Beeline client may require a workaround to allow the nohup command to correctly put the Beeline process in the background without stopping it.  See [HIVE-11717](https://issues.apache.org/jira/browse/HIVE-11717), [HIVE-6758](https://issues.apache.org/jira/browse/HIVE-6758).

Beeline 客户端的一些版本可能需要一个变通方法，在不停止的情况下，使用 nohup 命令将 Beeline 进程正确地放在后台运行。

> The following environment variable can be updated:

下面的环境变量需要更新：

	export HADOOP_CLIENT_OPTS="$HADOOP_CLIENT_OPTS -Djline.terminal=jline.UnsupportedTerminal"

> Running with nohangup (nohup) and ampersand (&) will place the process in the background and allow the terminal to disconnect while keeping the Beeline process running.  
**使用 nohup 和 & 运行将进程放到后台执行，在保持 Beeline 进程运行的同时，允许断开终端连接**。

	nohup beeline --silent=true --showHeader=true --outputformat=dsv -f query.hql </dev/null > /tmp/output.log 2> /tmp/error.log &


## 2、JDBC

> HiveServer2 has a JDBC driver. It supports both embedded and remote access to HiveServer2. Remote HiveServer2 mode is recommended for production use, as it is more secure and doesn't require direct HDFS/metastore access to be granted for users.

**HiveServer2 有一个 JDBC 驱动程序。它支持对 HiveServer2 的嵌入式和远程访问**。

远程模式推荐在生产使用，因为它更安全，不需要为用户授予直接的 HDFS/metastore 访问权限。

### 2.1、Connection URLs

#### 2.1.1、Connection URL Format

> The HiveServer2 URL is a string with the following syntax:

**HiveServer2 URL 是如下形式的字符串：**

	jdbc:hive2://<host1>:<port1>,<host2>:<port2>/dbName;initFile=<file>;sess_var_list?hive_conf_list#hive_var_list

where：

> <host1>:<port1>,<host2>:<port2> is a server instance or a comma separated list of server instances to connect to (if dynamic service discovery is enabled). If empty, the embedded server will be used.

- `<host1>:<port1>,<host2>:<port2>` 是要连接的一个服务器实例，或逗号分隔的多个服务器实例(如果启用了动态服务发现)。如果是空，将使用嵌入式服务。

> dbName is the name of the initial database.

- `dbName` 是一个初始化的数据库名称。

> <file> is the path of init script file ([Hive 2.2.0](https://issues.apache.org/jira/browse/HIVE-5867) and later). This script file is written with SQL statements which will be executed automatically after connection. This option can be empty. 

- `<file>` 是初始化脚本的路径。这个脚本使用 SQL 语句写，在连接后，自动允许。这个选项可以为空。

> sess_var_list is a semicolon separated list of key=value pairs of session variables (e.g., user=foo;password=bar).

- `sess_var_list` 是一个分号划分的列表，元素是 `key=value` 形式的 session 变量。

> hive_conf_list is a semicolon separated list of key=value pairs of Hive configuration variables for this session

- `hive_conf_list` 是一个分号划分的列表，元素是 `key=value` 形式的此 session 的 hive 配置变量。

> hive_var_list is a semicolon separated list of key=value pairs of Hive variables for this session.

- `hive_var_list` 是一个分号划分的列表，元素是 `key=value` 形式的此 session 的 hive 变量。

> Special characters in sess_var_list, hive_conf_list, hive_var_list parameter values should be encoded with URL encoding if needed.

sess_var_list、hive_conf_list 和 hive_var_list 种的特殊字符应该使用 URL 编码进行编码。

#### 2.1.2、Connection URL for Remote or Embedded Mode

> The JDBC connection URL format has the prefix jdbc:hive2:// and the Driver class is org.apache.hive.jdbc.HiveDriver. Note that this is different from the old [HiveServer](https://cwiki.apache.org/confluence/display/Hive/HiveServer).

**JDBC 连接的 URL 格式有一个 `jdbc:hive2://` 前缀。驱动类是`org.apache.hive.jdbc.HiveDriver`。这不同于旧的 HiveServer。**

> For a remote server, the URL format is jdbc:hive2://<host>:<port>/<db>;initFile=<file> (default port for HiveServer2 is 10000).

- **对于远程服务，URL 格式是 `jdbc:hive2://<host>:<port>/<db>;initFile=<file>` (默认端口是10000)**

> For an embedded server, the URL format is jdbc:hive2:///;initFile=<file> (no host or port).

- **对于嵌入式服务，URL 格式是 `jdbc:hive2:///;initFile=<file>`**

> The initFile option is available in [Hive 2.2.0](https://issues.apache.org/jira/browse/HIVE-5867) and later releases.

在 Hive 2.2.0 及往后的版本，initFile 选项可用。

#### 2.1.3、Connection URL When HiveServer2 Is Running in HTTP Mode

当 HiveServer2 在 HTTP 模式下运行的 Connection URL。

**JDBC connection URL: `jdbc:hive2://<host>:<port>/<db>;transportMode=http;httpPath=<http_endpoint>`**

where:

> <http_endpoint> is the corresponding HTTP endpoint configured in [hive-site.xml](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-ConfiguringHive). Default value is cliservice.

- `<http_endpoint>` 是 hive-site.xml 中配置的对应的 HTTP 端点。默认值是 cliservice。

> Default port for HTTP transport mode is 10001.

- HTTP transport 模式的默认端口是 10001.

> Versions earlier than [0.14](https://issues.apache.org/jira/browse/HIVE-6972).In versions earlier than 0.14 these parameters used to be called [hive.server2.transport.mode](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.transport.mode) and [hive.server2.thrift.http.path](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.thrift.http.path) respectively and were part of the hive_conf_list. These versions have been deprecated in favour of the new versions (which are part of the sess_var_list) but continue to work for now.

在早于 0.14 的版本中，这些参数是 `hive.server2.transport.mode` 和 `hive.server2.thrift.http.path`，是 hive_conf_list 的一部分。这些版本已经被弃用，取而代之的是新版本(属于sess_var_list的一部分)，但是现在继续工作。

#### 2.1.4、Connection URL When SSL Is Enabled in HiveServer2

在 HiveServer2 中，启用 SSL 时的 Connection URL。

**JDBC connection URL:  `jdbc:hive2://<host>:<port>/<db>;ssl=true;sslTrustStore=<trust_store_path>;trustStorePassword=<trust_store_password>`**

where:

> <trust_store_path> is the path where client's truststore file lives.

- `<trust_store_path>` 是客户端 truststore 文件存放的路径。

> <trust_store_password> is the password to access the truststore.

- `<trust_store_password>` 是访问 truststore 的密码。

In HTTP mode:  `jdbc:hive2://<host>:<port>/<db>;ssl=true;sslTrustStore=<trust_store_path>;trustStorePassword=<trust_store_password>;transportMode=http;httpPath=<http_endpoint>`.

> For versions earlier than 0.14, see the [version note](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-HIVE-6972) above.

0.14 之前的版本，见上面的 version note。

#### 2.1.5、Connection URL When ZooKeeper Service Discovery Is Enabled

当启用了zookeeper服务发现时的Connection URL。

> ZooKeeper-based service discovery introduced in Hive 0.14.0 ([HIVE-7935](https://issues.apache.org/jira/browse/HIVE-7395)) enables high availability and rolling upgrade for HiveServer2. A JDBC URL that specifies <zookeeper quorum> needs to be used to make use of these features.

基于 ZooKeeper 的服务发现启用 HiveServer2 的高可用和滚动升级。需要**在 JDBC URL 上指定 `<zookeeper quorum>`**。 

> With further changes in Hive 2.0.0 and 1.3.0 (unreleased, [HIVE-11581](https://issues.apache.org/jira/browse/HIVE-11581)), none of the additional configuration parameters such as authentication mode, transport mode, or SSL parameters need to be specified, as they are retrieved from the ZooKeeper entries along with the hostname.

Hive 2.0.0 和 1.3.0 版本中，有了进一步的改变。**不再需要指定 `authentication mode、 transport mode、 SSL parameters` 等额外的参数。它们和主机名一起从 ZooKeeper 中检索**。 

**The JDBC connection URL: `jdbc:hive2://<zookeeper quorum>/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=hiveserver2`**.

> The <zookeeper quorum> is the same as the value of hive.zookeeper.quorum configuration parameter in hive-site.xml/hivserver2-site.xml used by HiveServer2.

`<zookeeper quorum>` 和 `hive-site.xml/hivserver2-site.xml` 中的 `hive.zookeeper.quorum` 相同。

> Additional runtime parameters needed for querying can be provided within the URL as follows, by appending it as a ?<option> as before.

**可以在 URL 中提供查询所需的其他运行时参数，如下所示:**

The JDBC connection URL: `jdbc:hive2://<zookeeper quorum>/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=hiveserver2?tez.queue.name=hive1&hive.server2.thrift.resultset.serialize.in.tasks=true` 

#### 2.1.6、Named Connection URLs

> As of Hive 2.1.0 ([HIVE-13670](https://issues.apache.org/jira/browse/HIVE-13670)), Beeline now also supports named URL connect strings via usage of environment variables. If you try to do a !connect to a name that does not look like a URL, then Beeline will attempt to see if there is an environment variable called BEELINE_URL_<name>. For instance, if you specify !connect blue, it will look for BEELINE_URL_BLUE, and use that to connect. This should make it easier for system administrators to specify environment variables for users, and users need not type in the full URL each time to connect.

**Hive 2.1.0 版本中，Beeline 也支持使用环境变量命名的URL连接字符串**。

如果你尝试使用 `!connect` 连接到一个看起来不像 URL 的名称，那么 Beeline 将尝试查看是否存在一个名为 `BEELINE_URL_<name>` 的环境变量。

例如，如果指定 `!connect blue`，它将查找 `BEELINE_URL_BLUE`，并使用它来连接。

这将使系统管理员更容易为用户指定环境变量，并且用户不需要每次都输入完整的 URL 来进行连接。

#### 2.1.7、Reconnecting

> Traditionally, !reconnect has worked to refresh a connection that has already been established. It is not able to do a fresh connect after !close has been run. As of Hive 2.1.0 (HIVE-13670), Beeline remembers the last URL successfully connected to in a session, and is able to reconnect even after a !close has been run. In addition, if a user does a !save, then this is saved in the beeline.properties file, which then allows !reconnect to connect to this saved last-connected-to URL across multiple Beeline sessions. This also allows the use of  beeline -r  from the command line to do a reconnect on startup.

通常，**`!reconnect` 用于刷新已经建立的连接。但它不能在`!close`关闭后，建立一个新的连接**。

在 Hive 2.1.0 中，Beeline 记住了会话中最后一个成功连接到的URL，并且能够在运行 `!close` 之后重新连接。

此外，如果用户执行了 `!save`，那么它将保存在 `beeline.properties` 中。然后允许 `!reconnect` 到 多个 Beeline 会话中保存的最后一个连接的URL。

这还允许从命令行使用 `beeline -r` 在启动时进行重新连接。

#### 2.1.8、Using hive-site.xml to automatically connect to HiveServer2

> As of Hive 2.2.0 ([HIVE-14063](https://issues.apache.org/jira/browse/HIVE-14063)), Beeline adds support to use the hive-site.xml present in the classpath to automatically generate a connection URL based on the configuration properties in hive-site.xml and an additional user configuration file. Not all the URL properties can be derived from hive-site.xml and hence in order to use this feature user must create a configuration file called “beeline-hs2-connection.xml” which is a Hadoop XML format file. This file is used to provide user-specific connection properties for the connection URL. Beeline looks for this configuration file in ${user.home}/.beeline/ (Unix based OS) or ${user.home}\beeline\ directory (in case of Windows). If the file is not found in the above locations Beeline looks for it in ${HIVE_CONF_DIR} location and /etc/hive/conf (check [HIVE-16335](https://issues.apache.org/jira/browse/HIVE-16335) which fixes this location from /etc/conf/hive in Hive 2.2.0) in that order. Once the file is found, Beeline uses beeline-hs2-connection.xml in conjunction with the hive-site.xml in the class path to determine the connection URL.

**在Hive 2.2.0 中，Beeline 增加了对使用类路径中的 `hive-site.xml` 的支持，以根据 `hive-site.xml` 中的配置属性和一个额外的用户配置文件自动生成一个连接 URL**。

**并非所有的 URL 属性都可以从  `hive-site.xml` 派生**，因此，为了使用这个特性，用户必须创建一个名为 `beeline-hs2-connection.xml` 的配置文件，它是一个 Hadoop xml 格式的文件。此文件用于为连接URL提供用户指定的连接属性。

Beeline 在 `${user.home}/.beeline/`(Unix) 或 `${user.home}\beeline\`(Windows) 中查找此配置文件。

如果在上面的位置没有找到文件，Beeline会在 `${HIVE_CONF_DIR}` 位置和 `/etc/hive/conf`中按顺序查找它。

找到该文件后，Beeline 将 `beeline-hs2-connection.xml` 与类路径中的 `hive-site.xml` 结合使用，以确定连接 URL。

> The URL connection properties in beeline-hs2-connection.xml must have the prefix “beeline.hs2.connection.” followed by the URL property name. For example in order to provide the property ssl the property key in the beeline-hs2-connection.xml should be “beeline.hs2.connection.ssl”. The sample beeline.hs2.connection.xml below provides the value of user and password for the Beeline connection URL. In this case the rest of the properties like HS2 hostname and port information, Kerberos configuration properties, SSL properties, transport mode, etc., are picked up using the hive-site.xml in the class path. If the password is empty beeline.hs2.connection.password property should be removed. In most cases the below configuration values in beeline-hs2-connection.xml and the correct hive-site.xml in classpath should be sufficient to make the connection to the HiveServer2.

**`beeline-hs2-connection.xml`中的 URL 连接属性必须有前缀 beeline.hs2.connection ，后跟 URL 属性名**。

例如，为了提供属性 ssl，`beeline-hs2-connection.xml` 中的属性 key 应该是 beeline.hs2.connection.ssl 。

下面的示例 `beeline-hs2-connection.xml` 提供了 Beeline 连接 URL 的用户值和密码。

在这种情况下，HS2 主机名和端口信息、Kerberos配置属性、SSL属性、传输模式等其他属性使用类路径中的 `hive-site.xml`。

如果密码为空，`beeline.hs2.connection.password` 属性应该被删除。

在大多数情况下，`beeline-hs2-connection.xml` 中的以下配置值和类路径中的正确的 `hive-site.xml` 应该足以建立到 HiveServer2 的连接。

```xml
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
	<property>
		<name>beeline.hs2.connection.user</name>
		<value>hive</value>
	</property>
	<property>
		<name>beeline.hs2.connection.password</name>
		<value>hive</value>
	</property>
</configuration>
```

> In case of properties which are present in both beeline-hs2-connection.xml and hive-site.xml, the property value derived from beeline-hs2-connection.xml takes precedence. For example in the below beeline-hs2-connection.xml file provides the value of principal for Beeline connection in a Kerberos enabled environment. In this case the property value for beeline.hs2.connection.principal overrides the value of HiveConf.ConfVars.HIVE_SERVER2_KERBEROS_PRINCIPAL from hive-site.xml as far as connection URL is concerned.

对于同时出现在 `beeline-hs2-connection.xml` 和 `hive-site.xml` 中的属性，`beeline-hs2-connection.xml` 中的属性值优先。

例如，在下面的 `beeline-hs2-connection.xml` 文件中，提供了在支持 Kerberos 的环境中进行 Beeline 连接的主体值。在本例中，`beeline.hs2.connection.principal`的属性值会覆盖 `hive-site.xml` 中的 `HiveConf.ConfVars.HIVE_SERVER2_KERBEROS_PRINCIPAL` 值。

```xml
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
	<property>
		<name>beeline.hs2.connection.hosts</name>
	  	<value>localhost:10000</value>
	</property>
	<property>
	 	<name>beeline.hs2.connection.principal</name>
	  	<value>hive/dummy-hostname@domain.com</value>
	</property>
</configuration>
```

> In case of properties beeline.hs2.connection.hosts, beeline.hs2.connection.hiveconf and beeline.hs2.connection.hivevar the property value is a comma-separated list of values. For example the following beeline-hs2-connection.xml provides the hiveconf and hivevar values in a comma separated format.

属性 `beeline.hs2.connection.hosts`, `beeline.hs2.connection.hiveconf` 和 `beeline.hs2.connection.hivevar`下，属性值是一个逗号分隔的值列表。

例如，下面的 `beeline-hs2-connection.xml` 以逗号分隔的格式提供 hiveconf 和 hivevar 值。

```xml
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
	<property>
	  	<name>beeline.hs2.connection.user</name>
	  	<value>hive</value>
	</property>
	<property>
	  	<name>beeline.hs2.connection.hiveconf</name>
	  	<value>hive.cli.print.current.db=true, hive.cli.print.header=true</value>
	</property>
	<property>
	  	<name>beeline.hs2.connection.hivevar</name>
	  	<value>testVarName1=value1, testVarName2=value2</value>
	</property>
</configuration>
```

> When the beeline-hs2-connection.xml is present and when no other arguments are provided, Beeline automatically connects to the URL generated using configuration files. When connection arguments (-u, -n or -p) are provided, Beeline uses them and does not use beeline-hs2-connection.xml to automatically connect. Removing or renaming the beeline-hs2-connection.xml disables this feature.

当存在 `beeline-hs2-connection.xml`，且没有提供其他参数时，Beeline 自动连接到使用配置文件生成的 URL。

当提供连接参数(-u、-n或-p)时，Beeline 会使用它们，而不会使用 `beeline-hs2-connection.xml` 来自动连接。

删除或重命名 `beeline-hs2-connection.xml` 将禁用此特性。

#### 2.1.9、Using beeline-site.xml to automatically connect to HiveServer2

> In addition to the above method of using hive-site.xml and beeline-hs2-connection.xml for deriving the JDBC connection URL to use when connecting to HiveServer2 from Beeline, a user can optionally add beeline-site.xml to their classpath, and within beeline-site.xml, she can specify complete JDBC URLs. A user can also specify multiple named URLs and use beeline -c <named_url> to connect to a specific URL. This is particularly useful when the same cluster has multiple HiveServer2 instances running with different configurations. One of the named URLs is treated as default (which is the URL that gets used when the user simply types beeline). An example beeline-site.xml is shown below:

从 Beeline 连接到 HiveServer2，除了使用 `beeline-hs2-connection.xml` 和 `hive-site.xml` ，用户还可以可选地将 `beeline-site.xml` 添加到类路径中，并在 `beeline-site.xml`中指定完整的JDBC URLs。

用户还可以指定多个已命名的 URL，并使用 `beeline -c <named_url>` 连接到指定的 URL。

当同一个集群有多个以不同配置运行的 HiveServer2 实例时，这特别有用。其中一个已命名的 URL 被视为默认值(即用户直接键入时使用的URL)。

`beeline-site.xml`的示例如下所示:

```xml
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
	<property>
	  	<name>beeline.hs2.jdbc.url.tcpUrl</name>
	  	<value>jdbc:hive2://localhost:10000/default;user=hive;password=hive</value>
	</property>
	 
	<property>
	  	<name>beeline.hs2.jdbc.url.httpUrl</name>
	  	<value>jdbc:hive2://localhost:10000/default;user=hive;password=hive;transportMode=http;httpPath=cliservice</value>
	</property>
	 
	<property>
	  	<name>beeline.hs2.jdbc.url.default</name>
	  	<value>tcpUrl</value>
	</property>
</configuration>
```

> In the above example, simply typing beeline opens a new JDBC connection to jdbc:hive2://localhost:10000/default;user=hive;password=hive. If both beeline-site.xml and beeline-hs2-connection.xml are present in the classpath, the final URL is created by applying the properties specified in beeline-hs2-connection.xml on top of the URL properties derived from beeline-site.xml. As an example consider the following beeline-hs2-connection.xml:

在上面的示例中，只需输入 `beeline` 就可以打开到 `jdbc:hive2://localhost:10000/default;user=hive;password=hive` 的新 JDBC 连接。

如果在类路径中同时存在 `beeline-site.xml` 和 `beeline-hs2-connection.xml`，则在来自 `beeline-site.xml`的 URL 属性之上应用 `beeline-hs2-connection.xml` 中指定的属性，从而创建最终的 URL。【参考如下示例理解】

考虑以下`beeline-hs2-connection.xml`:

```xml
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
	<property>
	  	<name>beeline.hs2.connection.user</name>
	  	<value>hive</value>
	</property>
	<property>
	  	<name>beeline.hs2.connection.password</name>
	  	<value>hive</value>
	</property>
</configuration>
```

> Consider the following beeline-site.xml:

考虑以下`beeline-site.xml`:

```xml
<?xml version="1.0"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
	<property>
	  	<name>beeline.hs2.jdbc.url.tcpUrl</name>
	  	<value>jdbc:hive2://localhost:10000/default</value>
	</property>
	 
	<property>
	  	<name>beeline.hs2.jdbc.url.httpUrl</name>
	  	<value>jdbc:hive2://localhost:10000/default;transportMode=http;httpPath=cliservice</value>
	</property>
	 
	<property>
	  	<name>beeline.hs2.jdbc.url.default</name>
	  	<value>tcpUrl</value>
	</property>
</configuration>
```

> In the above example, simply typing beeline opens a new JDBC connection to jdbc:hive2://localhost:10000/default;user=hive;password=hive. When the user types beeline -c httpUrl, a connection is opened to jdbc:hive2://localhost:10000/default;transportMode=http;httpPath=cliservice;user=hive;password=hive. 

在上面的例子中，仅需键入 `beeline`，打开一个连接到 `jdbc:hive2://localhost:10000/default;user=hive;password=hive` 的新的 JDBC 连接。

当用户键入 `beeline -c httpUrl`，打开 `jdbc:hive2://localhost:10000/default;transportMode=http;httpPath=cliservice;user=hive;password=hive`

#### 2.1.10、Using JDBC

> You can use JDBC to access data stored in a relational database or other tabular format.

你可以使用 JDBC 访问存储在关系数据库或其他表格形式中数据。

> Load the HiveServer2 JDBC driver. As of 1.2.0 applications no longer need to explicitly load JDBC drivers using Class.forName().

1.载入 HiveServer2 JDBC driver。从 1.2.0 版本开始，不再需要使用 `Class.forName()` 显示的载入 JDBC driver。

例如：

	Class.forName("org.apache.hive.jdbc.HiveDriver");

> Connect to the database by creating a Connection object with the JDBC driver. 

2.使用 JDBC driver 创建一个 Connection 对象，来连接数据库。

例如：

	Connection cnct = DriverManager.getConnection("jdbc:hive2://<host>:<port>", "<user>", "<password>");

> The default <port> is 10000. In non-secure configurations, specify a <user> for the query to run as. The <password> field value is ignored in non-secure mode.

默认的 `<port>` 是10000。在非安全配置中，为运行的查询指定一个 `<user>`。在非安全模式中，`<password>`会被忽略。

	Connection cnct = DriverManager.getConnection("jdbc:hive2://<host>:<port>", "<user>", "");

> In Kerberos secure mode, the user information is based on the Kerberos credentials.

在 Kerberos 安全模式下，用户信息基于 Kerberos 凭证。

> Submit SQL to the database by creating a Statement object and using its executeQuery() method. 

3.创建一个 Statement 对象，使用 `executeQuery()` 方法，来提交 SQL 到数据库。

例如：

	Statement stmt = cnct.createStatement();
	ResultSet rset = stmt.executeQuery("SELECT foo FROM bar");

> Process the result set, if necessary.

4.必要的话，处理结果集。

> These steps are illustrated in the sample code below.

这些步骤在下面的代码中有体现。

##### 2.1.10.1、JDBC Client Sample Code

```java
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DriverManager;
 
public class HiveJdbcClient {
  private static String driverName = "org.apache.hive.jdbc.HiveDriver";
 
  /**
   * @param args
   * @throws SQLException
   */
  public static void main(String[] args) throws SQLException {
      try {
      Class.forName(driverName);
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      System.exit(1);
    }
    //replace "hive" here with the name of the user the queries should run as
    Connection con = DriverManager.getConnection("jdbc:hive2://localhost:10000/default", "hive", "");
    Statement stmt = con.createStatement();
    String tableName = "testHiveDriverTable";
    stmt.execute("drop table if exists " + tableName);
    stmt.execute("create table " + tableName + " (key int, value string)");
    // show tables
    String sql = "show tables '" + tableName + "'";
    System.out.println("Running: " + sql);
    ResultSet res = stmt.executeQuery(sql);
    if (res.next()) {
      System.out.println(res.getString(1));
    }
       // describe table
    sql = "describe " + tableName;
    System.out.println("Running: " + sql);
    res = stmt.executeQuery(sql);
    while (res.next()) {
      System.out.println(res.getString(1) + "\t" + res.getString(2));
    }
 
    // load data into table
    // NOTE: filepath has to be local to the hive server
    // NOTE: /tmp/a.txt is a ctrl-A separated file with two fields per line
    String filepath = "/tmp/a.txt";
    sql = "load data local inpath '" + filepath + "' into table " + tableName;
    System.out.println("Running: " + sql);
    stmt.execute(sql);
 
    // select * query
    sql = "select * from " + tableName;
    System.out.println("Running: " + sql);
    res = stmt.executeQuery(sql);
    while (res.next()) {
      System.out.println(String.valueOf(res.getInt(1)) + "\t" + res.getString(2));
    }
 
    // regular hive query
    sql = "select count(1) from " + tableName;
    System.out.println("Running: " + sql);
    res = stmt.executeQuery(sql);
    while (res.next()) {
      System.out.println(res.getString(1));
    }
  }
}
```

##### 2.1.10.2、Running the JDBC Sample Code

```sh
# Then on the command-line
$ javac HiveJdbcClient.java
 
# To run the program using remote hiveserver in non-kerberos mode, we need the following jars in the classpath
# from hive/build/dist/lib
#     hive-jdbc*.jar
#     hive-service*.jar
#     libfb303-0.9.0.jar
#        libthrift-0.9.0.jar
#     log4j-1.2.16.jar
#     slf4j-api-1.6.1.jar
#    slf4j-log4j12-1.6.1.jar
#     commons-logging-1.0.4.jar
#
#
# To run the program using kerberos secure mode, we need the following jars in the classpath
#     hive-exec*.jar
#     commons-configuration-1.6.jar (This is not needed with Hadoop 2.6.x and later).
#  and from hadoop
#     hadoop-core*.jar (use hadoop-common*.jar for Hadoop 2.x)
#
# To run the program in embedded mode, we need the following additional jars in the classpath
# from hive/build/dist/lib
#     hive-exec*.jar
#     hive-metastore*.jar
#     antlr-runtime-3.0.1.jar
#     derby.jar
#     jdo2-api-2.1.jar
#     jpox-core-1.2.2.jar
#     jpox-rdbms-1.2.2.jar
# and from hadoop/build
#     hadoop-core*.jar
# as well as hive/build/dist/conf, any HIVE_AUX_JARS_PATH set, 
# and hadoop jars necessary to run MR jobs (eg lzo codec)
 
$ java -cp $CLASSPATH HiveJdbcClient
```

> Alternatively, you can run the following bash script, which will seed the data file and build your classpath before invoking the client. The script adds all the additional jars needed for using HiveServer2 in embedded mode as well.

或者，你可以运行以下 bash 脚本，它将在调用客户端之前，为数据文件创建种子并构建类路径。

该脚本还添加了在嵌入式模式下使用 HiveServer2 所需的所有附加jar。

```sh
#!/bin/bash
HADOOP_HOME=/your/path/to/hadoop
HIVE_HOME=/your/path/to/hive
 
echo -e '1\x01foo' > /tmp/a.txt
echo -e '2\x01bar' >> /tmp/a.txt
 
HADOOP_CORE=$(ls $HADOOP_HOME/hadoop-core*.jar)
CLASSPATH=.:$HIVE_HOME/conf:$(hadoop classpath)
 
for i in ${HIVE_HOME}/lib/*.jar ; do
    CLASSPATH=$CLASSPATH:$i
done
 
java -cp $CLASSPATH HiveJdbcClient
```

### 2.2、JDBC Data Types

> The following table lists the data types implemented for HiveServer2 JDBC.

Hive Type | Java Type | Specification
---|:---|:---
TINYINT   | byte  | signed or unsigned 1-byte integer
SMALLINT  | short | signed 2-byte integer
INT       | int   | signed 4-byte integer
BIGINT    | long  | signed 8-byte integer
FLOAT     | double| single-precision number (approximately 7 digits)
DOUBLE    | double| double-precision number (approximately 15 digits)
DECIMAL   | java.math.BigDecimal | fixed-precision decimal value
BOOLEAN   | boolean | a single bit (0 or 1)
STRING    | String  | character string or variable-length character string
TIMESTAMP | java.sql.Timestamp | date and time value
BINARY    | String  | binary data
**Complex Types** |     |
ARRAY     | String – json encoded | values of one data type
MAP       | String – json encoded | key-value pairs
STRUCT    | String – json encoded | structured values

### 2.3、JDBC Client Setup for a Secure Cluster

> When connecting to HiveServer2 with Kerberos authentication, the URL format is:

在有 Kerberos 授权的情况下，连接 HiveServer2，URL 的格式是：

	jdbc:hive2://<host>:<port>/<db>;principal=<Server_Principal_of_HiveServer2>

> The client needs to have a valid Kerberos ticket in the ticket cache before connecting.

在连接之前，在凭证缓存中，客户端需要有一个有效的 Kerberos 凭证。

> NOTE: If you don't have a "/" after the port number, the jdbc driver does not parse the hostname and ends up running HS2 in embedded mode . So if you are specifying a hostname, make sure you have a "/" or "/<dbname>" after the port number.

注意：如果在端口数字后，没有`/`，jdbc driver不会解析主机名，最终以嵌入式模式运行 HS2。所以，如果你指定了一个主机名，确保在端口数字后有`/`或`/<dbname>`。

> In the case of LDAP, CUSTOM or PAM authentication, the client needs to pass a valid user name and password to the JDBC connection API.

在 LDAP、CUSTOM 或 PAM 身份验证的情况下，客户端需要向 JDBC 连接 API 传递有效的用户名和密码。

> To use sasl.qop, add the following to the sessionconf part of your Hive JDBC hive connection string, e.g. jdbc:hive://hostname/dbname;sasl.qop=auth-int

为了使用 `sasl.qop`，将以下内容添加到你的 Hive JDBC hive 连接字符串的 sessionconf 部分。如，`jdbc:hive://hostname/dbname;sasl.qop=auth-int`

> For more information, see [Setting Up HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2).

#### 2.3.1、Multi-User Scenarios and Programmatic Login to Kerberos KDC

> In the current approach of using Kerberos you need to have a valid Kerberos ticket in the ticket cache before connecting. This entails a static login (using kinit, key tab or ticketcache) and the restriction of one Kerberos user per client. These restrictions limit the usage in middleware systems and other multi-user scenarios, and in scenarios where the client wants to login programmatically to Kerberos KDC.

在使用 Kerberos 的当前方法中，在连接之前，需要在凭证缓存中有一个有效的 Kerberos 凭证。这需要一个静态登录(使用 kinit、key tab 或 ticketcache)和每个客户端一个 Kerberos 用户的限制。这些限制限制了在中间件系统和其他多用户场景以及客户端希望以编程方式登录到 Kerberos KDC 的场景中的使用。

> One way to mitigate the problem of multi-user scenarios is with secure proxy users (see [HIVE-5155](https://issues.apache.org/jira/browse/HIVE-5155)). Starting in Hive 0.13.0, support for secure proxy users has two components:

**缓解多用户场景问题的一种方法是使用安全代理用户**。从 Hive 0.13.0 开始，对安全代理用户的支持有两个组件:

> Direct proxy access for privileged Hadoop users ([HIVE-5155](https://issues.apache.org/jira/browse/HIVE-5155)). This enables a privileged user to directly specify an alternate session user during the connection. If the connecting user has Hadoop level privilege to impersonate the requested userid, then HiveServer2 will run the session as that requested user.

- 特权 Hadoop 用户的直接代理访问。这允许特权用户在连接期间直接指定备用会话用户。如果连接中的用户拥有 Hadoop 级别的特权来模拟所请求的用户id，那么 HiveServer2 将作为所请求的用户运行会话。

> Delegation token based connection for Oozie ([OOZIE-1457](https://issues.apache.org/jira/browse/OOZIE-1457)). This is the common mechanism for Hadoop ecosystem components.Proxy user privileges in the Hadoop ecosystem are associated with both user names and hosts. That is, the privilege is available for certain users from certain hosts.  Delegation tokens in Hive are meant to be used if you are connecting from one authorized (blessed) machine and later you need to make a connection from another non-blessed machine. You get the delegation token from a blessed machine and connect using the delegation token from a non-blessed machine. The primary use case is Oozie, which gets a delegation token from the server machine and then gets another connection from a Hadoop task node.

- 基于委托令牌的 Oozie 连接。这是 Hadoop 生态系统组件的常见机制。Hadoop 生态系统中的代理用户特权与用户名和主机相关联。也就是说，该特权对来自特定主机的特定用户可用。如果你正在连接一台已授权(blessed)的机器，然后需要从另一台 non-blessed 机器进行连接，那么将使用 Hive 中的委托令牌。你从 blessed 机器获得委托令牌，然后使用来自 non-blessed  机器的委托令牌进行连接。主要用例是 Oozie，它从服务器机器获取委托令牌，然后从 Hadoop 任务节点获取另一个连接。

> If you are only making a JDBC connection as a privileged user from a single blessed machine, then direct proxy access is the simpler approach. You can just pass the user you need to impersonate in the JDBC URL by using the hive.server2.proxy.user=<user> parameter.

如果你只是作为特权用户从一台 blessed 机器建立 JDBC 连接，那么直接代理访问是更简单的方法。你可以在 JDBC URL 中使用 `hive.server2.proxy.user=<user>` 参数传递你需要模拟的用户。

> See examples in [ProxyAuthTest.java](https://github.com/apache/hive/blob/master/beeline/src/test/org/apache/hive/beeline/ProxyAuthTest.java).

> Support for delegation tokens with HiveServer2 binary transport mode [hive.server2.transport.mode](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.transport.mode) has been available starting 0.13.0; support for this feature with HTTP transport mode was added in [HIVE-13169](https://issues.apache.org/jira/browse/HIVE-13169), which should be part of Hive 2.1.0.

支持使用 HiveServer2 二进制传输模式的委托令牌模式从 0.13.0 开始可用；hHIVE-13169 中增加了对 HTTP 传输模式的支持，它应该是 Hive 2.1.0 的一部分。

> The other way is to use a pre-authenticated Kerberos Subject (see [HIVE-6486](https://issues.apache.org/jira/browse/HIVE-6486)). In this method, starting with Hive 0.13.0 the Hive JDBC client can use a pre-authenticated subject to authenticate to HiveServer2. This enables a middleware system to run queries as the user running the client.

另一种方法是使用预先经过身份验证的 Kerberos Subject。在这个方法中，从 Hive 0.13.0 开始，Hive JDBC 客户端可以使用一个预先认证的 subject 来认证 HiveServer2。这使得中间件系统能够以运行客户端的用户的身份运行查询。

##### 2.3.1.1、Using Kerberos with a Pre-Authenticated Subject

> To use a pre-authenticated subject you will need the following changes.

要使用预先经过身份验证的 subject，需要进行以下更改。

> Add hive-exec*.jar to the classpath in addition to the regular Hive JDBC jars (commons-configuration-1.6.jar and hadoop-core*.jar are not required).

1.除了常规的 Hive JDBC jar(不需要`commons-configuration-1.6.jar`和`hadoop-core*.jar`)之外，还将`hive-exec*.jar`添加到类路径中。

> Add auth=kerberos and kerberosAuthType=fromSubject JDBC URL properties in addition to having the “principal" url property.

2.除了拥有 “principal” URL 属性外，还添加 `auth=kerberos` 和 `kerberosAuthType=fromSubject` JDBC URL 属性。

> Open the connection in Subject.doAs().

3.在 `Subject.doAs()` 中打开连接。

> The following code snippet illustrates the usage (refer to HIVE-6486 for a complete [test case](https://issues.apache.org/jira/secure/attachment/12633984/TestCase_HIVE-6486.java)):

下面的代码片段演示了其用法：

```java
static Connection getConnection( Subject signedOnUserSubject ) throws Exception{
       Connection conn = (Connection) Subject.doAs(signedOnUserSubject, new PrivilegedExceptionAction<Object>()
           {
               public Object run()
               {
                       Connection con = null;
                       String JDBC_DB_URL = "jdbc:hive2://HiveHost:10000/default;" ||
                                              "principal=hive/localhost.localdomain@EXAMPLE.COM;" ||
                                              "kerberosAuthType=fromSubject";
                       try {
                               Class.forName(JDBC_DRIVER);
                               con =  DriverManager.getConnection(JDBC_DB_URL);
                       } catch (SQLException e) {
                               e.printStackTrace();
                       } catch (ClassNotFoundException e) {
                               e.printStackTrace();
                       }
                       return con;
               }
           });
       return conn;
}
```

### 2.4、JDBC Fetch Size

> Gives the JDBC driver a hint as to the number of rows that should be fetched from the database when more rows are needed by the client.  The default value, used for every statement, can be specified through the JDBC connection string.  This default value may subsequently be overwritten, per statement, with the JDBC API.  If no value is specified within the JDBC connection string, then the default fetch size is retrieved from the HiveServer2 instance as part of the session initiation operation.

当客户端需要更多的行时，为 JDBC 驱动程序提供应该从数据库获取的行数。

对每个语句使用来说，默认值可以通过 JDBC 连接字符串指定。这个默认值随后可能会被 JDBC API 覆盖。

如果在 JDBC 连接字符串中没有指定值，那么作为会话初始化操作的一部分，将从 HiveServer2 实例中检索默认的 fetch size。

	jdbc:hive2://<host>:<port>/<db>;fetchsize=<value>

> Hive Version 4.0

> The Hive JDBC driver will receive a preferred fetch size from the instance of HiveServer2 it has connected to.  This value is specified on the server by the hive.server2.thrift.resultset.default.fetch.size configuration.

Hive JDBC driver 将从它连接到的 HiveServer2 实例中接收一个首选 fetch size。这个值是在服务端上由 `hive.server2.thrift.resultset.default.fetch.size` 指定的。

> The JDBC fetch size is only a hint and the server will attempt to respect the client's requested fetch size though with some limits.  HiveServer2 will cap all requests at a maximum value specified by the hive.server2.thrift.resultset.max.fetch.size configuration value regardless of the client's requested fetch size.

JDBC fetch size 只是一个提示，服务器将尝试尊重客户端请求的 fetch size，尽管有一些限制。HiveServer2 将把所有请求限制在 `hive.server2.thrift.resultset.max.fetch.size` 指定的最大值。这个配置值与客户端请求的 fetch.size 无关。

> While a larger fetch size may limit the number of round-trips between the client and server, it does so at the expense of additional memory requirements on the client and server.

虽然更大的 fetch size 可能减少客户端和服务端之间的往返次数，但这样做的代价是客户端和服务端上额外的内存需求。

> The default JDBC fetch size value may be overwritten, per statement, with the JDBC API:

默认的 JDBC fetch size 值可能会被每条语句的使用 JDBC API 覆盖:

> Setting a value of 0 instructs the driver to use the fetch size value preferred by the server

- 设置值为0将指示 driver 使用服务端上首选的 fetch size 值

> Setting a value greater than zero will instruct the driver to fetch that many rows, though the actual number of rows returned may be capped by the server

- 设置一个大于0的值将指示 driver 获取许多行，尽管服务端可能会限制返回的实际行数。

> If no fetch size value is explicitly set on the JDBC driver's statement then the driver's default value is used

- 如果在 JDBC driver 的 statement 中没有显式设置 fetch size 值，则使用 driver 的默认值

> If the fetch size value is specified within the JDBC connection string, this is the default value

	如果在 JDBC 连接字符串中指定了 fetch size 值，则这是默认值。

> If the fetch size value is absent from the JDBC connection string, the server's preferred fetch size is used as the default value

	如果在 JDBC 连接字符串中没有指定 fetch size 值，则使用服务端首选的 fetch size 作为默认值。

## 3、Python Client

> A Python client driver is available on [github](https://github.com/BradRuderman/pyhs2). For installation instructions, see [Setting Up HiveServer2: Python Client Driver](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2#SettingUpHiveServer2-PythonClientDriver).

## 4、Ruby Client

> A Ruby client driver is available on github at [https://github.com/forward3d/rbhive](https://github.com/forward3d/rbhive).

## 5、Integration with SQuirrel SQL Client 

> Download, install and start the SQuirrel SQL Client from the [SQuirrel SQL website](http://squirrel-sql.sourceforge.net/).

1.下载、安装和启动 SQuirrel SQL 客户端。

> Select 'Drivers -> New Driver...' to register Hive's JDBC driver that works with HiveServer2.

2.选择 'Drivers -> New Driver...'，注册 Hive's JDBC driver

a.Enter the driver name and example URL:

	Name: Hive
	Example URL: jdbc:hive2://localhost:10000/default

> Select 'Extra Class Path -> Add' to add the following jars from your local Hive and Hadoop distribution.

3.选择 'Extra Class Path -> Add'，添加你的本地 Hive 和 Hadoop 发行版的下面的 jars：

	HIVE_HOME/lib/hive-jdbc-*-standalone.jar
	HADOOP_HOME/share/hadoop/common/hadoop-common-*.jar

> Version information:Hive JDBC standalone jars are used in Hive 0.14.0 onward ([HIVE-538](https://issues.apache.org/jira/browse/HIVE-538)); for previous versions of Hive, use `HIVE_HOME/build/dist/lib/*.jar` instead.The hadoop-common jars are for Hadoop 2.0; for previous versions of Hadoop, use `HADOOP_HOME/hadoop-*-core.jar` instead.

版本信息：Hive 0.14.0 及之后的版本使用 Hive JDBC standalone jars；对于以前的版本，使用`HIVE_HOME/build/dist/lib/*.jar`。hadoop-common jar 用于 Hadoop 2.0；对于以前版本的Hadoop，使用`HADOOP_HOME/hadoop-*-core.jar`。

> Select 'List Drivers'. This will cause SQuirrel to parse your jars for JDBC drivers and might take a few seconds. From the 'Class Name' input box select the Hive driver for working with HiveServer2:

4.选择 'List Drivers'。这将导致 SQuirrel 解析你的 jar ，以获取 JDBC drivers，并且可能需要几秒钟的时间。从 'Class Name' 输入框中选择为 HiveServer2 工作的 Hive drivers:

	org.apache.hive.jdbc.HiveDriver

> Click 'OK' to complete the driver registration. 

5.点击 'OK'，完成 driver 注册。

> Select 'Aliases -> Add Alias...' to create a connection alias to your HiveServer2 instance.

6.选择 'Aliases -> Add Alias...'，创建 HiveServer2 实例的连接别名。

> Give the connection alias a name in the 'Name' input box.

a.在 'Name' 输入框，给连接别名指定一个名字。

> Select the Hive driver from the 'Driver' drop-down.

b.从 'Driver' 下拉框中，选择 Hive driver

> Modify the example URL as needed to point to your HiveServer2 instance.

c.根据需要修改示例 URL ，以指向你的 HiveServer2 实例。

> Enter 'User Name' and 'Password' and click 'OK' to save the connection alias.

d.输入“用户名”和“密码”，然后单击 'OK' 保存连接别名。

> To connect to HiveServer2, double-click the Hive alias and click 'Connect'.

e.为连接到 HiveServer2，双击 Hive 别名，点击 'Connect'

> When the connection is established you will see errors in the log console and might get a warning that the driver is not JDBC 3.0 compatible. These alerts are due to yet-to-be-implemented parts of the JDBC metadata API and can safely be ignored. To test the connection enter SHOW TABLES in the console and click the run icon.

建立连接后，你将在日志控制台中看到错误，并可能得到一个警告，即 driver 不兼容 JDBC 3.0。

这些警告是由于 JDBC 元数据 API 中尚未实现的部分，可以安全地忽略它们。要测试连接，请在控制台中输入 `SHOW TABLES`，并单击 run 图标。

> Also note that when a query is running, support for the 'Cancel' button is not yet available.

还要注意，当查询运行时，对 'Cancel' 按钮的支持还不可用。

## 6、Integration with SQL Developer

> Integration with Oracle SQLDeveloper is available using JDBC connection.

可以使用 JDBC 连接与 Oracle SQLDeveloper 集成。

[https://community.hortonworks.com/articles/1887/connect-oracle-sql-developer-to-hive.html](https://community.hortonworks.com/articles/1887/connect-oracle-sql-developer-to-hive.html)

## 7、Integration with DbVisSoftware's DbVisualizer

> Download, install and start DbVisualizer free or purchase DbVisualizer Pro from [https://www.dbvis.com/](https://www.dbvis.com/).

> Follow instructions on [github](https://github.com/cyanfr/dbvis_to_hortonworks_hiveserver2/wiki/How-I-Connected-DBVisualizer-9.2.2-on-Windows-to-Hortonwork-HiveServer2).

## 8、Advanced Features for Integration with Other Tools

### 8.1、Supporting Cookie Replay in HTTP Mode

> Version 1.2.0 and later:This option is available starting in [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-9709).

1.2.0 及其往后版本：从 Hive 1.2.0 开始，这个选项开始可用。

> [HIVE-9709](https://issues.apache.org/jira/browse/HIVE-9709) introduced support for the JDBC driver to enable cookie replay. This is turned on by default so that incoming cookies can be sent back to the server for authentication.  

HIVE-9709 引入了对 JDBC driver 的支持，以启用 cookie 重放。默认情况下是打开的，以便传入的 cookie 可以被发送回服务器进行身份验证。

> The JDBC connection URL when enabled should look like this:

当启用时，JDBC 连接 URL 是这样的：

	jdbc:hive2://<host>:<port>/<db>?transportMode=http;httpPath=<http_endpoint>;cookieAuth=true;cookieName=<cookie_name>

> cookieAuth is set to true by default.

- cookieAuth 默认设置为 true。

> cookieName: If any of the incoming cookies' keys match the value of cookieName, the JDBC driver will not send any login credentials/Kerberos ticket to the server. The client will just send the cookie alone back to the server for authentication. The default value of cookieName is hive.server2.auth (this is the HiveServer2 cookie name). 

- cookieName：如果任何传入 cookie 的 keys 与 cookieName 的值匹配，JDBC driver 将不会向服务器发送任何登录凭证/Kerberos凭证。客户端只将 cookie 发送回服务器进行身份验证。cookieName 的默认值是 `hive.server2.auth` (这是 HiveServer2 cookie 的名称)。

> To turn off cookie replay, cookieAuth=false must be used in the JDBC URL.

- 要关闭 cookie 重播，必须在 JDBC URL 中使用 `cookieAuth=false`。

> Important Note: As part of [HIVE-9709](https://issues.apache.org/jira/browse/HIVE-9709), we upgraded Apache http-client and http-core components of Hive to 4.4. To avoid any collision between this upgraded version of HttpComponents and other any versions that might be present in your system (such as the one provided by Apache Hadoop 2.6 which uses http-client and http-core components version of 4.2.5), the client is expected to set CLASSPATH in such a way that Beeline-related jars appear before HADOOP lib jars. This is achieved via setting HADOOP_USER_CLASSPATH_FIRST=true before using hive-jdbc. In fact, in bin/beeline.sh we do this!

- 重要提示:作为 HIVE-9709 的一部分，我们将 Hive 的 Apache http-client 和 http-core 组件升级到 4.4。为了避免在 HttpComponents 升级版本和其他可能存在于你的系统的任何版本之间的碰撞，(如 Apache Hadoop 2.6 提供的一个使用 http-client 和4.2.5版本的 http-core 组件)，客户端将设置类路径指向 HADOOP lib jars 之前出现的 Beeline 相关的 jar。这是通过在使用 hive-jdbc 之前设置`HADOOP_USER_CLASSPATH_FIRST=true`来实现的。事实上，在`bin/beeline.sh`中这样做。

### 8.2、Using 2-way SSL in HTTP Mode 

> Version 1.2.0 and later:This option is available starting in [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-10447).

1.2.0 及其往后版本：从 Hive 1.2.0 开始，这个选项开始可用。

> [HIVE-10447](https://issues.apache.org/jira/browse/HIVE-10447) enabled the JDBC driver to support 2-way SSL in HTTP mode. Please note that HiveServer2 currently does not support 2-way SSL. So this feature is handy when there is an intermediate server such as Knox which requires client to support 2-way SSL.

HIVE-10447 使 JDBC driver 支持 HTTP 模式下的 2-way SSL。请注意，HiveServer2 目前不支持 2-way SSL。因此，当有像 Knox 这样需要客户端支持 2-way SSL 的中间服务器时，这个特性非常方便。

JDBC connection URL:

	jdbc:hive2://<host>:<port>/<db>;ssl=true;twoWay=true;sslTrustStore=<trust_store_path>;trustStorePassword=<trust_store_password>;sslKeyStore=<key_store_path>;keyStorePassword=<key_store_password>?transportMode=http;httpPath=<http_endpoint>

> <trust_store_path> is the path where the client's truststore file lives. This is a mandatory non-empty field.

- `<trust_store_path>` 是客户端 truststore 文件的路径。这是一个强制性的非空字段。

> <trust_store_password> is the password to access the truststore.

- `<trust_store_password>` 是访问 truststore 的密码。

> <key_store_path> is the path where the client's keystore file lives. This is a mandatory non-empty field.

- `<key_store_path>` 是客户端 keystore 文件的路径。这是一个强制性的非空字段。

> <key_store_password> is the password to access the keystore.

- `<key_store_password>`是访问 keystore 的密码。

For versions earlier than 0.14, see the [version note](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-HIVE-6972) above.

### 8.3、Passing HTTP Header Key/Value Pairs via JDBC Driver

> Version 1.2.0 and later:This option is available starting in [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-10339).

1.2.0 及其往后版本：从 Hive 1.2.0 开始，这个选项开始可用。

> [HIVE-10339](https://issues.apache.org/jira/browse/HIVE-10339) introduced an option for clients to provide custom HTTP headers that can be sent to the underlying server (Hive 1.2.0 and later).

HIVE-10339 引入了一个选项，为客户端提供自定义 HTTP headers，可用被发送到底层服务器。

JDBC connection URL:

	jdbc:hive2://<host>:<port>/<db>;transportMode=http;httpPath=<http_endpoint>;http.header.<name1>=<value1>;http.header.<name2>=<value2>

> When the above URL is specified, Beeline will call underlying requests to add an HTTP header set to <name1> and <value1> and another HTTP header set to <name2> and <value2>. This is helpful when the end user needs to send identity in an HTTP header down to intermediate servers such as Knox via Beeline for authentication, for example http.header.USERNAME=<value1>;http.header.PASSWORD=<value2>.

当上述 URL 被指定时，Beeline 将调用底层请求，来添加一个设置为 `<name1>` 和 `<value1>` 的 HTTP header，和另一个设置为 `<name2>` 和 `<value2>`的 HTTP header。

当最终用户需要通过 Beeline 将 HTTP header 中的标识发送到 Knox 等中间服务器进行身份验证时，这是很有帮助的，

例如 `http.header.USERNAME=<value1>;http.header.PASSWORD=<value2>`

For versions earlier than 0.14, see the [version note](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-HIVE-6972) above. 

### 8.4、Passing Custom HTTP Cookie Key/Value Pairs via JDBC Driver

> In Hive version 3.0.0 [HIVE-18447](https://issues.apache.org/jira/browse/HIVE-18447) introduced an option for clients to provide custom HTTP cookies that can be sent to the underlying server. Some authentication mechanisms, like Single Sign On, need the ability to pass a cookie to some intermediate authentication service like Knox via the JDBC driver. 

在 Hive 3.0.0 中，HIVE-18447 为客户端引入了一个选项，来提供可以发送到底层服务器的自定义 HTTP cookie。

一些身份验证机制，如单点登录，需要能够通过 JDBC driver 将 cookie 传递给一些中间身份验证服务，如 Knox。

JDBC connection URL: 

	jdbc:hive2://<host>:<port>/<db>;transportMode=http;httpPath=<http_endpoint>;http.cookie.<name1>=<value1>;http.cookie.<name2>=<value2>

> When the above URL is specified, Beeline will call underlying requests to add HTTP cookie in the request header, and will set it to <name1>=<value1> and <name2>=<value2>. 

当指定了上述 URL 后，Beeline 将调用底层请求，来在请求头中添加 HTTP cookie，并将其设置为 `<name1>=<value1>` 和 `<name2>=<value2>`。