# HiveServer2 Clients

[TOC]

> This page describes the different clients supported by HiveServer2.  Other documentation for [HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2) includes:

本文描述 HiveServer2 支持的不同客户端。其他 HiveServer2 的文献包括：

- [HiveServer2 Overview](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Overview)
- [Setting Up HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2)
- [Hive Configuration Properties:  HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-HiveServer2)

> Version. Introduced in Hive version 0.11. See [HIVE-2935](https://issues.apache.org/jira/browse/HIVE-2935).

版本：在 0.11 中引入。

## 1、Beeline – Command Line Shell

> HiveServer2 supports a command shell Beeline that works with HiveServer2. It's a JDBC client that is based on the SQLLine CLI ([http://sqlline.sourceforge.net/](http://sqlline.sourceforge.net/)). There’s detailed [documentation](http://sqlline.sourceforge.net/#manual) of SQLLine which is applicable to Beeline as well.

HiveServer2 支持与 HiveServer2 一起使用的命令行 shell Beeline。它是一个**基于 SQLLine CLI 的 JDBC 客户端**。关于 SQLLine 的详细文档，它也适用于 Beeline。

> [Replacing the Implementation of Hive CLI Using Beeline](https://cwiki.apache.org/confluence/display/Hive/Replacing+the+Implementation+of+Hive+CLI+Using+Beeline)

使用 Beeline 替换 Hive CLI 的实现

> The Beeline shell works in both embedded mode as well as remote mode. In the embedded mode, it runs an embedded Hive (similar to [Hive CLI](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli)) whereas remote mode is for connecting to a separate HiveServer2 process over Thrift. Starting in [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-7615), when Beeline is used with HiveServer2, it also prints the log messages from HiveServer2 for queries it executes to STDERR. Remote HiveServer2 mode is recommended for production use, as it is more secure and doesn't require direct HDFS/metastore access to be granted for users.

Beeline shell 可以在**嵌入式模式和远程模式**下工作。

在嵌入式模式下，它运行一个嵌入式 Hive(类似于 Hive CLI)，而远程模式是通过 Thrift 连接到一个单独的 HiveServer2 进程。

从 Hive 0.14 开始，当 Beeline 与 HiveServer2 一起使用时，它也会打印来自 HiveServer2 的查询日志消息到 STDERR。

**建议在生产环境下使用远程 HiveServer2 模式**，因为远程 HiveServer2 模式更安全，不需要直接授予用户 HDFS/metastore 访问权限。

> In remote mode HiveServer2 only accepts valid Thrift calls – even in HTTP mode, the message body contains Thrift payloads.

在远程模式下，HiveServer2 只接受有效的 Thrift 调用 -- 即使在 HTTP 模式下，消息主体也包含 Thrift 有效负载。

### 1.1、Beeline Example

```sql
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
```

> You can also specify the connection parameters on command line. This means you can find the command with the connection string from your UNIX shell history. 

还可以**在命令行上指定连接参数**。

这意味着可以从 UNIX shell 历史记录中找到带有连接字符串的命令。

```sh
% beeline -u jdbc:hive2://localhost:10000/default -n scott -w password_file
Hive version 0.11.0-SNAPSHOT by Apache

Connecting to jdbc:hive2://localhost:10000/default
```

> Beeline with NoSASL connection. If you'd like to connect via NOSASL mode, you must specify the authentication mode explicitly:

带有 NoSASL 连接的 Beeline。**如果你想通过 NOSASL 模式连接，必须明确地指定授权模式**：

```sh
% bin/beeline
beeline> !connect jdbc:hive2://<host>:<port>/<db>;auth=noSasl hiveuser pass 
```

### 1.2、Beeline Commands

Command  |  Description
---|:---
`!<SQLLine command>` | List of SQLLine commands available at [http://sqlline.sourceforge.net/](http://sqlline.sourceforge.net/). Example: !quit exits the Beeline client.【可用的SQLLine命令列表。如：`!quit` 退出Beeline客户端 】
!delimiter           | Set the delimiter for queries written in Beeline. Multi-character delimiters are allowed, but quotation marks, slashes, and -- are not allowed. Defaults to ; Usage: !delimiter $$. Version: 3.0.0 ([HIVE-10865](https://issues.apache.org/jira/browse/HIVE-10865))【设置Beeline中查询间的分隔符。允许多字符分隔符，但引号、斜杠和`--`不允许。默认是`;`。用法：`!delimiter $$`。版本：3.0.0】

### 1.3、Beeline Properties

**fetchsize**:

> Standard JDBC enables you to specify the number of rows fetched with each database round-trip for a query, and this number is referred to as the fetch size.

标准 JDBC 允许**指定一个查询的每次数据库往返获取的行数**，这个数字称为 fetch size。

> Setting the fetch size in Beeline overrides the JDBC driver's default fetch size and affects subsequent statements executed in the current session.

**在 Beeline 中，设置 fetch size 会覆盖 JDBC 驱动程序的默认 fetch size，并影响在当前会话中执行的后续语句**。

- 值 -1 指示 Beeline 使用 JDBC 驱动程序的默认 fetch size(默认)

- 对于每条语句，将 0 或更多的值传递给 JDBC 驱动程序

- 任何其他负值都会抛出异常

> A value of -1 instructs Beeline to use the JDBC driver's default fetch size (default)
> A value of zero or more is passed to the JDBC driver for each statement
> Any other negative value will throw an Exception

Usage: `!set fetchsize 200`

Version: 4.0.0 ([HIVE-22853](https://issues.apache.org/jira/browse/HIVE-22853))

### 1.4、Beeline Hive Commands

> Hive specific commands (same as [Hive CLI commands](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveInteractiveShellCommands)) can be run from Beeline, when the Hive JDBC driver is used.

使用 Hive JDBC 驱动程序时，可以直接**在 Beeline 上执行 Hive 的特定命令**(与 Hive 的 CLI 命令相同)。

> Use ";" (semicolon) to terminate commands. Comments in scripts can be specified using the "--" prefix.

使用 `;`(分号)终止命令。脚本中的注释可以使用 `--` 前缀指定。

命令描述表格见：[https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-BeelineHiveCommands](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-BeelineHiveCommands)

### 1.5、Beeline Command Options

> The Beeline CLI supports these command line options:

Beeline CLI 支持这些命令行选项：

选项描述表格见：[https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-BeelineCommandOptions](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-BeelineCommandOptions)

### 1.6、Output Formats

> In Beeline, the result can be displayed in different formats. The format mode can be set with the outputformat option.

在 Beeline 中，结果可以不同的格式展示。可以**使用 outputformat 选项设置格式模式**。

> The following output formats are supported:

支持如下的输出格式：

- HiveServer2 Clients#table
- HiveServer2 Clients#vertical
- HiveServer2 Clients#xmlattr
- HiveServer2 Clients#xmlelements
- HiveServer2 Clients#json
- HiveServer2 Clients#jsonfile
- separated-value formats (csv, tsv, csv2, tsv2, dsv)

#### 1.6.1、table

> The result is displayed in a table. A row of the result corresponds to a row in the table and the values in one row are displayed in separate columns in the table. This is the default format mode.

结果展示在表中。

结果的一行对应表中的一行，一行中的值展示在表中的不同列。

这是默认的格式模式。

> Result of the query `select id, value, comment from test_table`

	+-----+---------+-----------------+
	| id  |  value  |     comment     |
	+-----+---------+-----------------+
	| 1   | Value1  | Test comment 1  |
	| 2   | Value2  | Test comment 2  |
	| 3   | Value3  | Test comment 3  |
	+-----+---------+-----------------+

#### 1.6.2、vertical

> Each row of the result is displayed in a block of key-value format, where the keys are the names of the columns.

结果的每行展示在键值格式的一个块中。键是列名。

> Result of the query `select id, value, comment from test_table`

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

> The result is displayed in an XML format where each row is a "result" element in the XML. The values of a row are displayed as attributes on the "result" element. The names of the attributes are the names of the columns.

结果展示在 XML 格式中，每行是 XML 中的一个结果元素。

行的值在结果元素中以元素形式展示。属性的名字是列名。

> Result of the query `select id, value, comment from test_table`

```xml
<resultset>
  <result id="1" value="Value1" comment="Test comment 1"/>
  <result id="2" value="Value2" comment="Test comment 2"/>
  <result id="3" value="Value3" comment="Test comment 3"/>
</resultset>
```
#### 1.6.4、xmlelements

> The result is displayed in an XML format where each row is a "result" element in the XML. The values of a row are displayed as child elements of the result element.

结果展示在 XML 格式中，每行是 XML 中的一个结果元素。

行的值在结果元素中以子元素的形式展示。

> Result of the query `select id, value, comment from test_table`

```xml
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
```

#### 1.6.5、json

> (Hive 4.0) The result is displayed in JSON format where each row is a "result" element in the JSON array "resultset".

（Hive 4.0）结果展示在 JSON 格式中，每行是 JSON 数组结果集中的一个结果元素。

> Result of the query select `String`, `Int`, `Decimal`, `Bool`, `Null`, `Binary` from test_table

```json
{"resultset":[{"String":"aaa","Int":1,"Decimal":3.14,"Bool":true,"Null":null,"Binary":"SGVsbG8sIFdvcmxkIQ"},{"String":"bbb","Int":2,"Decimal":2.718,"Bool":false,"Null":null,"Binary":"RWFzdGVyCgllZ2cu"}]}

```

#### 1.6.6、jsonfile

> (Hive 4.0) The result is displayed in JSON format where each row is a distinct JSON object.  This matches the expected format for a table created as JSONFILE format.

（Hive 4.0）结果展示在 JSON 格式中，每行是一个去重的 JSON 对象。这与 JSONFILE 格式创建的表的期望格式相匹配。

> Result of the query select `String`, `Int`, `Decimal`, `Bool`, `Null`, `Binary` from test_table

```json
{"String":"aaa","Int":1,"Decimal":3.14,"Bool":true,"Null":null,"Binary":"SGVsbG8sIFdvcmxkIQ"}
{"String":"bbb","Int":2,"Decimal":2.718,"Bool":false,"Null":null,"Binary":"RWFzdGVyCgllZ2cu"}
```

#### 1.6.7、Separated-Value Output Formats

> The values of a row are separated by different delimiters. There are five separated-value output formats available: csv, tsv, csv2, tsv2 and dsv.

一行的值由不同的分隔符分隔。

有五种分隔的值输出格式:csv、tsv、csv2、tsv2 和 dsv。

##### 1.6.7.1、csv2, tsv2, dsv

> Starting with [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-8615) there are improved SV output formats available, namely dsv, csv2 and tsv2. These three formats differ only with the delimiter between cells, which is comma for csv2, tab for tsv2, and configurable for dsv.

从 Hive 0.14 开始，改进的 SV 输出格式可用，即 dsv、csv2 和 tsv2。

这三种格式仅在单元格间的分隔符不同，csv2是逗号，tsv2是制表符，dsv是可配置的。

> For the dsv format, the delimiter can be set with the delimiterForDSV option. The default delimiter is '|'. Please be aware that only single character delimiters are supported.

对于 dsv 格式，可以使用 delimiterForDSV 选项设置分隔符。默认的分隔符是 `|`。

请注意，只支持单字符分隔符。

> Result of the query select id, value, comment from test_table

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

> If quoting is not disabled, double quotes are added around a value if it contains special characters (such as the delimiter or double quote character) or spans multiple lines. Embedded double quotes are escaped with a preceding double quote.

如果未禁用引号，如果值包含特殊字符(如分隔符或双引号字符)或跨多行，则在值周围添加双引号。

嵌入的双引号用前面的双引号进行转义。

> The quoting can be disabled by setting the disable.quoting.for.sv system variable to true. 

可以通过设置 `disable.quoting.for.sv` 为 true 来禁用引用。

> If the quoting is disabled, no double quotes are added around the values (even if they contains special characters) and the embedded double quotes are not escaped. By default, the quoting is disabled.

如果禁用引号，则不会在值周围添加双引号(即使它们包含特殊字符)，嵌入的双引号也不会转义。

默认情况下，禁用引号。

> Result of the query select id, value, comment from test_table

csv2, quoting is enabled

	id,value,comment
	1,"Value,1",Value contains comma
	2,"Value""2",Value contains double quote
	3,Value'3,Value contains single quote

csv2, quoting is disabled

	id,value,comment
	1,Value,1,Value contains comma
	2,Value"2,Value contains double quote
	3,Value'3,Value contains single quote

##### 1.6.7.2、csv, tsv

> These two formats differ only with the delimiter between values, which is comma for csv and tab for tsv. The values are always surrounded with single quote characters, even if the quoting is disabled by the disable.quoting.for.sv system variable. These output formats don't escape the embedded single quotes. Please be aware that these output formats are deprecated and only maintained for backward compatibility.

这两种格式的区别只在于值之间的分隔符，csv是逗号，tsv是tab。

这些值总是被单引号字符包围，即使使用 `disable.quoting.for.sv` 禁用了引号。

这些输出格式不会转义嵌入的单引号。

请注意，这些输出格式已被弃用，维护它们只是为了向后兼容。

> Result of the query select id, value, comment from test_table

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

------------------------------

```sh
# 设置输出格式
0: jdbc:hive2://zgg:10000/default> !set outputformat csv2
0: jdbc:hive2://zgg:10000/default> select * from test;
test.id,test.name
1,zhangsan
```

------------------------------

#### 1.6.8、HiveServer2 Logging

> Starting with Hive 0.14.0, HiveServer2 operation logs are available for Beeline clients. These parameters configure logging:

从 Hive 0.14.0 开始，HiveServer2 操作日志对 Beeline 客户端可用。

这些参数配置日志记录:

- [hive.server2.logging.operation.enabled](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.logging.operation.enabled)
- [hive.server2.logging.operation.log.location](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.logging.operation.log.location)
- [hive.server2.logging.operation.verbose](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.logging.operation.verbose) (Hive 0.14 to 1.1)
- [hive.server2.logging.operation.level](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.logging.operation.level) (Hive 1.2 onward)

> [HIVE-11488](https://issues.apache.org/jira/browse/HIVE-11488) (Hive 2.0.0) adds the support of logging queryId and sessionId to HiveServer2 log file. To enable that, edit/add %X{queryId} and %X{sessionId} to the pattern format string of the logging configuration file.

Hive-11488 增加了记录 queryId 和 sessionId 到 HiveServer2 日志文件中的功能。

要启用该功能，在日志记录配置文件的模式格式字符串中，编辑/添加 `%X{queryId}` 和 `%X{sessionId}` 。

#### 1.6.9、Cancelling the Query

> When a user enters CTRL+C on the Beeline shell, if there is a query which is running at the same time then Beeline attempts to cancel the query while closing the socket connection to HiveServer2. This behavior is enabled only when [hive.server2.close.session.on.disconnect](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.close.session.on.disconnect) is set to true. Starting from Hive 2.2.0 ([HIVE-15626](https://issues.apache.org/jira/browse/HIVE-15626)) Beeline does not exit the command line shell when the running query is being cancelled as a user enters CTRL+C. If the user wishes to exit the shell they can enter CTRL+C for the second time while the query is being cancelled. However, if there is no query currently running, the first CTRL+C will exit the Beeline shell. This behavior is similar to how the Hive CLI handles CTRL+C.

当用户在 Beeline shell 上输入 CTRL+C 时，如果与此同时有一个查询正在运行，那么 Beeline 会尝试取消该查询，同时关闭到 HiveServer2 的套接字连接。

**此行为仅在 `hive.server2.close.session.on.disconnect` 为 true 时启用**。

从 Hive 2.2.0 开始，**当用户输入 CTRL+C 取消正在运行的查询时，Beeline 不会退出命令行 shell。如果用户希望退出 shell，他们可以在取消查询时第二次输入 CTRL+C**。但是，如果当前没有运行查询，第一个 CTRL+C 将退出 Beeline shell。这个行为类似于 Hive CLI 处理 CTRL+C 的方式。

> !quit is the recommended command to exit the Beeline shell.

退出 Beeline shell，建议使用 `!quit` 命令。

#### 1.6.10、Background Query in Terminal Script

> Beeline can be run disconnected from a terminal for batch processing and automation scripts using commands such as nohup and disown.

Beeline 可以在不连接终端的情况下运行，使用 nohup 和 disown 等命令进行批处理和自动化脚本。

> Some versions of Beeline client may require a workaround to allow the nohup command to correctly put the Beeline process in the background without stopping it.  See [HIVE-11717](https://issues.apache.org/jira/browse/HIVE-11717), [HIVE-6758](https://issues.apache.org/jira/browse/HIVE-6758).

一些版本的 Beeline 客户端可能需要一个解决方案，来允许 nohup 命令正确地将 Beeline 进程放在后台而不停止它。

> The following environment variable can be updated:

可以更新的环境变量如下:

	export HADOOP_CLIENT_OPTS="$HADOOP_CLIENT_OPTS -Djline.terminal=jline.UnsupportedTerminal"

> Running with nohangup (nohup) and ampersand (&) will place the process in the background and allow the terminal to disconnect while keeping the Beeline process running. 

使用 nohup 和 & 运行将把进程放置在后台，并允许终端断开连接，同时保持 Beeline 进程运行。 

	nohup beeline --silent=true --showHeader=true --outputformat=dsv -f query.hql </dev/null > /tmp/output.log 2> /tmp/error.log &

## 2、JDBC

> HiveServer2 has a JDBC driver. It supports both embedded and remote access to HiveServer2. Remote HiveServer2 mode is recommended for production use, as it is more secure and doesn't require direct HDFS/metastore access to be granted for users.

HiveServer2 有一个 JDBC 驱动程序。它支持嵌入和远程访问 HiveServer2。

建议在生产环境下使用远程 HiveServer2 模式，因为远程 HiveServer2 模式更安全，不需要直接授予用户 HDFS/metastore 访问权限。

### 2.1、Connection URLs

#### 2.1.1、Connection URL Format

> The HiveServer2 URL is a string with the following syntax:

HiveServer2 URL 是一个具有如下语法的字符串：

	jdbc:hive2://<host1>:<port1>,<host2>:<port2>/dbName;initFile=<file>;sess_var_list?hive_conf_list#hive_var_list

其中：

> <host1>:<port1>,<host2>:<port2> is a server instance or a comma separated list of server instances to connect to (if dynamic service discovery is enabled). If empty, the embedded server will be used.

- `<host1>:<port1>,<host2>:<port2>`：一个要连接的服务器实例，或者一个逗号分隔的服务器实例列表（如果启用动态服务发现）。如果空，使用嵌入式服务。

> dbName is the name of the initial database.

- dbName：初始的数据库名字。

> <file> is the path of init script file (Hive 2.2.0 and later). This script file is written with SQL statements which will be executed automatically after connection. This option can be empty. 

- `<file>`：初始脚本文件的路径（Hive 2.2.0及往后）。这个脚本文件使用 SQL 语句写的，在连接后自动执行。这个选项可以是空。

> sess_var_list is a semicolon separated list of key=value pairs of session variables (e.g., user=foo;password=bar).

- `sess_var_list`：一个分号分隔的会话变量的键值对列表。如，`user=foo;password=bar`

> hive_conf_list is a semicolon separated list of key=value pairs of Hive configuration variables for this session

- `hive_conf_list`：一个分号分隔的 Hive 配置变量的键值对列表。

> hive_var_list is a semicolon separated list of key=value pairs of Hive variables for this session.

- `hive_var_list`：一个分号分隔的针对这个会话的 Hive 变量的键值对列表。

> Special characters in sess_var_list, hive_conf_list, hive_var_list parameter values should be encoded with URL encoding if needed.

`sess_var_list`、`hive_conf_list` 和 `hive_var_list` 参数值中的特殊字符应该使用 URL 编码方式编码。

#### 2.1.2、Connection URL for Remote or Embedded Mode

> The JDBC connection URL format has the prefix jdbc:hive2:// and the Driver class is org.apache.hive.jdbc.HiveDriver. Note that this is different from the old [HiveServer](https://cwiki.apache.org/confluence/display/Hive/HiveServer).

JDBC 连接 URL 格式有 `jdbc:hive2://` 前缀，驱动类为 `org.apache.hive.jdbc.HiveDriver`。

注意，这不同于旧的 HiveServer。

- 对于远程服务，URL 格式是 `jdbc:hive2://<host>:<port>/<db>;initFile=<file>`（HiveServer2 的默认端口是 10000）

- 对于嵌入式服务，URL 格式是 `jdbc:hive2:///;initFile=<file>`（没有主机和端口）

> For a remote server, the URL format is jdbc:hive2://<host>:<port>/<db>;initFile=<file> (default port for HiveServer2 is 10000).

> For an embedded server, the URL format is jdbc:hive2:///;initFile=<file> (no host or port).

> The initFile option is available in [Hive 2.2.0](https://issues.apache.org/jira/browse/HIVE-5867) and later releases.

initFile 选项在 Hive 2.2.0 及往后的版本可用。

#### 2.1.3、Connection URL When HiveServer2 Is Running in HTTP Mode

> JDBC connection URL:  jdbc:hive2://<host>:<port>/<db>;transportMode=http;httpPath=<http_endpoint>, where:

JDBC 连接 URL: `jdbc:hive2://<host>:<port>/<db>;transportMode=http;httpPath=<http_endpoint>`

其中：

- `<http_endpoint>`：hive-site.xml 中配置的相应的 HTTP 端点。默认值为 `cliservice`。

- HTTP 传输模式默认端口为 10001。

> <http_endpoint> is the corresponding HTTP endpoint configured in [hive-site.xml](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Configuration#AdminManualConfiguration-ConfiguringHive). Default value is cliservice.
> Default port for HTTP transport mode is 10001.

> Versions earlier than [0.14](https://issues.apache.org/jira/browse/HIVE-6972). In versions earlier than 0.14 these parameters used to be called [hive.server2.transport.mode](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.transport.mode) and [hive.server2.thrift.http.path](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.thrift.http.path) respectively and were part of the hive_conf_list. These versions have been deprecated in favour of the new versions (which are part of the sess_var_list) but continue to work for now.

在 0.14 之前的版本中，这些参数被分别称为 `hive.server2.transport.mode` 和 `hive.server2.thrift.http.path` ，是 hive_conf_list 的一部分。

为了支持新版本(它们是 sess_var_list 的一部分)，这些版本已经被弃用了，但现在继续起作用。

#### 2.1.4、Connection URL When SSL Is Enabled in HiveServer2

> JDBC connection URL:  `jdbc:hive2://<host>:<port>/<db>;ssl=true;sslTrustStore=<trust_store_path>;trustStorePassword=<trust_store_password>`, where:

JDBC 连接 URL: `jdbc:hive2://<host>:<port>/<db>;ssl=true;sslTrustStore=<trust_store_path>;trustStorePassword=<trust_store_password>`

其中：

- `<trust_store_path>`：客户端 truststore 文件的目录

- `<trust_store_password>`：访问 truststore 的密码

> <trust_store_path> is the path where client's truststore file lives.
> <trust_store_password> is the password to access the truststore.

> In HTTP mode: `jdbc:hive2://<host>:<port>/<db>;ssl=true;sslTrustStore=<trust_store_path>;trustStorePassword=<trust_store_password>;transportMode=http;httpPath=<http_endpoint>`.

在 HTTP 模式下：`jdbc:hive2://<host>:<port>/<db>;ssl=true;sslTrustStore=<trust_store_path>;trustStorePassword=<trust_store_password>;transportMode=http;httpPath=<http_endpoint>`

> For versions earlier than 0.14, see the [version note](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-HIVE-6972) above.

#### 2.1.5、Connection URL When ZooKeeper Service Discovery Is Enabled

> ZooKeeper-based service discovery introduced in Hive 0.14.0 ([HIVE-7935](https://issues.apache.org/jira/browse/HIVE-7395)) enables high availability and rolling upgrade for HiveServer2. A JDBC URL that specifies <zookeeper quorum> needs to be used to make use of these features.

Hive 0.14.0 引入的基于 zookeeper 的服务发现功能，支持 HiveServer2 的高可用性和滚动升级。为了使用这些特性，需要使用指定 `<zookeeper quorum>` 的 JDBC URL。

> With further changes in Hive 2.0.0 and 1.3.0 (unreleased, [HIVE-11581](https://issues.apache.org/jira/browse/HIVE-11581)), none of the additional configuration parameters such as authentication mode, transport mode, or SSL parameters need to be specified, as they are retrieved from the ZooKeeper entries along with the hostname.

Hive 2.0.0 和 1.3.0(未发布，Hive-11581) 的进一步变化是，不需要指定任何额外的配置参数，如认证模式、传输模式或SSL参数，因为它们和主机名一起从 ZooKeeper 条目中检索。

> The JDBC connection URL: `jdbc:hive2://<zookeeper quorum>/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=hiveserver2` .

JDBC 连接 URL：`jdbc:hive2://<zookeeper quorum>/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=hiveserver2`

> The <zookeeper quorum> is the same as the value of hive.zookeeper.quorum configuration parameter in hive-site.xml/hivserver2-site.xml used by HiveServer2.

`<zookeeper quorum>` 与 HiveServer2 使用的 `hive-site.xml/hivserver2-site.xml` 中的 `hive.zookeeper.quorum` 配置参数的值相同。

> Additional runtime parameters needed for querying can be provided within the URL as follows, by appending it as a ?<option> as before.

查询所需的其他运行时参数可以在 URL 中提供，如下所示，方法是像以前一样将其附加为 `?<option>`。

> The JDBC connection URL: `jdbc:hive2://<zookeeper quorum>/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=hiveserver2?tez.queue.name=hive1&hive.server2.thrift.resultset.serialize.in.tasks=true` 

JDBC 连接 URL：`jdbc:hive2://<zookeeper quorum>/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=hiveserver2?tez.queue.name=hive1&hive.server2.thrift.resultset.serialize.in.tasks=true` 

#### 2.1.6、Named Connection URLs

> As of Hive 2.1.0 ([HIVE-13670](https://issues.apache.org/jira/browse/HIVE-13670)), Beeline now also supports named URL connect strings via usage of environment variables. If you try to do a !connect to a name that does not look like a URL, then Beeline will attempt to see if there is an environment variable called BEELINE_URL_<name>. For instance, if you specify !connect blue, it will look for BEELINE_URL_BLUE, and use that to connect. This should make it easier for system administrators to specify environment variables for users, and users need not type in the full URL each time to connect.

从 Hive 2.1.0 开始，**Beeline 现在也通过使用环境变量支持命名的 URL 连接字符串**。

如果你尝试用 `!connect` 连接到一个看起来不像 URL 的名称，那么 Beeline 会尝试查看是否有一个名为 `BEELINE_URL_<name>` 的环境变量。

例如，如果指定了 `!connect blue`，它将查找 BEELINE_URL_BLUE，并使用它来连接。

这将使系统管理员更容易为用户指定环境变量，并且用户不需要在每次连接时输入完整的 URL。

#### 2.1.7、Reconnecting

> Traditionally, !reconnect has worked to refresh a connection that has already been established. It is not able to do a fresh connect after !close has been run. As of Hive 2.1.0 ([HIVE-13670](https://issues.apache.org/jira/browse/HIVE-13670)), Beeline remembers the last URL successfully connected to in a session, and is able to reconnect even after a !close has been run. In addition, if a user does a !save, then this is saved in the beeline.properties file, which then allows !reconnect to connect to this saved last-connected-to URL across multiple Beeline sessions. This also allows the use of  beeline -r  from the command line to do a reconnect on startup.

按照传统，`!reconnect` 的作用是刷新已经建立的连接。

在运行了 `!close` 之后，它无法进行新的连接。

在 Hive 2.1.0 中，Beeline 会记住在会话中成功连接到的最后一个 URL，并且即使在运行了 `!close` 之后也能够重新连接。

此外，如果用户执行了 `!save`，那么这将直接保存在 `beeline.properties` 文件中。然后，它允许跨多个 Beeline 会话执行 `!reconnect` 连接到保存的最后一次连接的 URL。

这也允许在启动时使用命令行中的 `beeline -r` 来重新连接。

#### 2.1.8、Using hive-site.xml to automatically connect to HiveServer2

> As of Hive 2.2.0 ([HIVE-14063](https://issues.apache.org/jira/browse/HIVE-14063)), Beeline adds support to use the hive-site.xml present in the classpath to automatically generate a connection URL based on the configuration properties in hive-site.xml and an additional user configuration file. Not all the URL properties can be derived from hive-site.xml and hence in order to use this feature user must create a configuration file called “beeline-hs2-connection.xml” which is a Hadoop XML format file. This file is used to provide user-specific connection properties for the connection URL. Beeline looks for this configuration file in ${user.home}/.beeline/ (Unix based OS) or ${user.home}\beeline\ directory (in case of Windows). If the file is not found in the above locations Beeline looks for it in ${HIVE_CONF_DIR} location and /etc/hive/conf (check [HIVE-16335](https://issues.apache.org/jira/browse/HIVE-16335) which fixes this location from /etc/conf/hive in Hive 2.2.0) in that order. Once the file is found, Beeline uses beeline-hs2-connection.xml in conjunction with the hive-site.xml in the class path to determine the connection URL.

从 Hive 2.2.0 开始，Beeline 支持使用类路径中 hive-site.xml 的配置属性和附加的用户配置文件自动生成连接 URL。

并不是所有的 URL 属性都可以从 hive-site.xml 派生出来，因此为了使用这个特性，用户必须创建一个名为 beeline-hs2-connection.xml 的配置文件，这是一个 Hadoop XML 格式文件。

此文件为连接 URL 提供特定于用户的连接属性。Beeline 在 `${user.home}/.beeline/`(基于Unix的操作系统) 或 `${user.home}\beeline\ directory`(在Windows情况下) 中查找这个配置文件。

如果文件在上面的位置没有找到，Beeline 会在 `${HIVE_CONF_DIR}` 位置和 `/etc/hive/conf` 中查找。找到文件后，Beeline 将使用 beeline-hs2-connection.xml 与类路径中的 hive-site.xml 一起确定连接 URL。

> The URL connection properties in beeline-hs2-connection.xml must have the prefix “beeline.hs2.connection.” followed by the URL property name. For example in order to provide the property ssl the property key in the beeline-hs2-connection.xml should be “beeline.hs2.connection.ssl”. The sample beeline.hs2.connection.xml below provides the value of user and password for the Beeline connection URL. In this case the rest of the properties like HS2 hostname and port information, Kerberos configuration properties, SSL properties, transport mode, etc., are picked up using the hive-site.xml in the class path. If the password is empty beeline.hs2.connection.password property should be removed. In most cases the below configuration values in beeline-hs2-connection.xml and the correct hive-site.xml in classpath should be sufficient to make the connection to the HiveServer2.

在 beeline-hs2-connection.xml 中的 URL 连接属性必须具有前缀 `beeline.hs2.connection`，前缀后跟 URL 属性名。例如，为了提供属性 ssl，beeline-hs2-connection.xml 中的属性键应该是 ` beeline.hs2.connection.ssl`。

下面的示例 beeline.hs2.connection.xml 提供了 Beeline 连接 URL 的用户和密码值。

在本例中，其他属性(如HS2主机名和端口信息、Kerberos配置属性、SSL属性、传输模式等)是使用类路径中的 hive-site.xml 获取的。如果密码为空，则应该删除 beeline.hs2.connection.password 属性。

在大多数情况下，beeline-hs2-connection.xml 中的以下配置值和类路径中正确的 hive-site.xml 应该足以连接到 HiveServer2。

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

如果属性同时存在于 beeline-hs2-connection.xml 和 hive-site.xml 中，从 beeline-hs2-connection.xml 派生的属性值优先。

例如，下面的 beeline-hs2-connection.xml 文件为支持 Kerberos 环境中的 Beeline 连接提供了 principal 的值。

在本例中，就连接 URL 而言，beeline.hs2.connection.principal 的属性值覆盖了 hive-site.xml 中的 HiveConf.ConfVars.HIVE_SERVER2_KERBEROS_PRINCIPAL 的值。

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

对于属性 beeline.hs2.connection.hosts、beeline.hs2.connection.hiveconf 和 beeline.hs2.connection.hivevar 来说 属性值是以逗号分隔的值列表。

例如，下面的 beeline-hs2-connection.xml 以逗号分隔的格式提供 hiveconf 和 hivevar 值。

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

当存在 beeline-hs2-connection.xml 且不提供其他参数时，Beeline 会自动连接到使用配置文件生成的 URL。

当提供连接参数(-u、-n或-p)时，Beeline 使用它们，而不使用 beeline-hs2-connection.xml 自动连接。

删除或重命名 beeline-hs2-connection.xml 将禁用此特性。

#### 2.1.9、Using beeline-site.xml to automatically connect to HiveServer2

> In addition to the above method of using hive-site.xml and beeline-hs2-connection.xml for deriving the JDBC connection URL to use when connecting to HiveServer2 from Beeline, a user can optionally add beeline-site.xml to their classpath, and within beeline-site.xml, she can specify complete JDBC URLs. A user can also specify multiple named URLs and use beeline -c <named_url> to connect to a specific URL. This is particularly useful when the same cluster has multiple HiveServer2 instances running with different configurations. One of the named URLs is treated as default (which is the URL that gets used when the user simply types beeline). An example beeline-site.xml is shown below:

除了上面使用 hive-site.xml 和 beeline-hs2-connection.xml 的方法来导出从 Beeline 连接到 HiveServer2 时使用的 JDBC 连接 URL 之外，用户还可以可选地将 beeline-site.xml 添加到他们的类路径中，并且在 beeline-site.xml 中，她可以指定完整的 JDBC URLs。

用户也可以指定多个命名 URL，并使用 `beeline -c <named_url>` 连接到特定的 URL。当同一个集群有多个使用不同配置运行的 HiveServer2 实例时，这特别有用。其中一个命名的 URLs 被视为默认 URL(当用户简单地输入 beeline 时使用的URL)。

beeline-site.xml 的示例如下:

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

在上面的例子中，只要输入 beeline 就会打开一个新的到 `JDBC:hive2://localhost:10000/default;user=hive;password=hive` 的 JDBC 连接。

如果类路径中同时存在 beeline-site.xml 和 beeline-hs2-connection.xml，则通过在从 beeline-site.xml 派生的 URL 属性之上应用 beeline-hs2-connection.xml 中指定的属性来创建最终的URL。【覆盖】

例如，考虑下面的 beeline-hs2-connection.xml:

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
Consider the following beeline-site.xml:

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

在上面的例子中，只要输入 beeline 就会打开一个新的到 `JDBC:hive2://localhost:10000/default;user=hive;password=hive` 的 JDBC 连接。

当用户输入 `beeline -c httpUrl` 时，会打开一个到 `jdbc:hive2://localhost:10000/default;transportMode=http;httpPath=cliservice;user=hive;password=hive` 的连接。

#### 2.1.10、Using JDBC

> You can use JDBC to access data stored in a relational database or other tabular format.

你可以使用 JDBC 访问存储在关系数据库或其他表格格式的数据。

> 1.Load the HiveServer2 JDBC driver. As of [1.2.0](https://issues.apache.org/jira/browse/HIVE-7998) applications no longer need to explicitly load JDBC drivers using Class.forName().

1.载入 HiveServer2 JDBC 驱动程序。从 1.2.0 开始，应用程序不再需要明确地使用 `Class.forName()` 载入 JDBC 驱动程序。

	Class.forName("org.apache.hive.jdbc.HiveDriver");

> 2.Connect to the database by creating a Connection object with the JDBC driver. 

2.通过使用 JDBC 驱动程序创建一个 Connection 对象来连接数据库。

	Connection cnct = DriverManager.getConnection("jdbc:hive2://<host>:<port>", "<user>", "<password>");

> The default <port> is 10000. In non-secure configurations, specify a <user> for the query to run as. The <password> field value is ignored in non-secure mode.

默认的 `<port>` 是 10000。在非安全配置下，为查询指定一个 `<user>`。`<password>` 字段值在非安全模式下被忽略。

	Connection cnct = DriverManager.getConnection("jdbc:hive2://<host>:<port>", "<user>", "");

> In Kerberos secure mode, the user information is based on the Kerberos credentials.

在 Kerberos 安全模式下，用户信息基于 Kerberos 凭据。

> 3.Submit SQL to the database by creating a Statement object and using its executeQuery() method. 

3.通过创建一个 Statement 对象，并使用它的 executeQuery() 语句来提交 SQL 到数据库。

	Statement stmt = cnct.createStatement();
	ResultSet rset = stmt.executeQuery("SELECT foo FROM bar");

> 4.Process the result set, if necessary.

4.如有必要，处理结果集。

> These steps are illustrated in the sample code below.

这些步骤在下面的样例代码中阐述。

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

或者，你可以运行以下 bash 脚本，它将在调用客户端之前为数据文件播下种子并构建类路径。该脚本还添加了在嵌入式模式下使用 HiveServer2 所需的所有额外 jars。

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

Hive Type  |  Java Type  |  Specification
---|:---|:---
TINYINT    |    byte     |  signed or unsigned 1-byte integer
SMALLINT   |    short    |  signed 2-byte integer
INT        |    int      |  signed 4-byte integer
BIGINT     |    long     |  signed 8-byte integer
FLOAT      |    double   |  single-precision number (approximately 7 digits)
DOUBLE     |    double   |  double-precision number (approximately 15 digits)
DECIMAL    |java.math.BigDecimal| fixed-precision decimal value
BOOLEAN    |    boolean  |  a single bit (0 or 1)
STRING     |    String   |  character string or variable-length character string
TIMESTAMP  |java.sql.Timestamp|  date and time value
BINARY     |    String   |  binary data
**Complex Types**|       |
ARRAY      |String – json encoded | values of one data type
MAP        |String – json encoded | key-value pairs
STRUCT     |String – json encoded | structured values

### 2.3、JDBC Client Setup for a Secure Cluster

> When connecting to HiveServer2 with Kerberos authentication, the URL format is:

使用 Kerberos 认证连接 HiveServer2 时，URL 格式为:

	jdbc:hive2://<host>:<port>/<db>;principal=<Server_Principal_of_HiveServer2>

> The client needs to have a valid Kerberos ticket in the ticket cache before connecting.

在连接之前，客户端需要在票据缓存中有一个有效的 Kerberos 票据。

> NOTE: If you don't have a "/" after the port number, the jdbc driver does not parse the hostname and ends up running HS2 in embedded mode . So if you are specifying a hostname, make sure you have a "/" or "/<dbname>" after the port number.

注意：如果端口号后面没有 `/`，jdbc 驱动程序就不会解析主机名，最终以嵌入式模式运行 HS2。因此，如果要指定主机名，请确保在端口号后面有 `/` 或 `/<dbname>`。

> In the case of LDAP, CUSTOM or PAM authentication, the client needs to pass a valid user name and password to the JDBC connection API.

对于LDAP、CUSTOM 或 PAM 身份验证，客户端需要将有效的用户名和密码传递给 JDBC 连接 API。

> To use sasl.qop, add the following to the sessionconf part of your Hive JDBC hive connection string, e.g.

为使用 `sasl.qop`，在你的 Hive JDBC hive 连接字符串的 sessionconf 部分添加以下内容，例如:

	jdbc:hive://hostname/dbname;sasl.qop=auth-int

> For more information, see [Setting Up HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2).

#### 2.3.1、Multi-User Scenarios and Programmatic Login to Kerberos KDC

> In the current approach of using Kerberos you need to have a valid Kerberos ticket in the ticket cache before connecting. This entails a static login (using kinit, key tab or ticketcache) and the restriction of one Kerberos user per client. These restrictions limit the usage in middleware systems and other multi-user scenarios, and in scenarios where the client wants to login programmatically to Kerberos KDC.

在使用 Kerberos 的当前方法中，你需要在连接之前在凭据缓存中有一个有效的 Kerberos 凭据。

这需要一个静态登录(使用kinit、key tab或 ticketcache)和每个客户端只能有一个 Kerberos 用户的限制。

这些限制限制了在中间件系统和其他多用户场景以及客户端希望以编程方式登录到 Kerberos KDC 的场景中的使用。

> One way to mitigate the problem of multi-user scenarios is with secure proxy users (see [HIVE-5155](https://issues.apache.org/jira/browse/HIVE-5155)). Starting in Hive 0.13.0, support for secure proxy users has two components:

缓解多用户场景问题的一种方法是使用安全代理用户。从 Hive 0.13.0 开始，对安全代理用户的支持有两个组件:

> Direct proxy access for privileged Hadoop users (HIVE-5155](https://issues.apache.org/jira/browse/HIVE-5155)). This enables a privileged user to directly specify an alternate session user during the connection. If the connecting user has Hadoop level privilege to impersonate the requested userid, then HiveServer2 will run the session as that requested user.

- 对特权 Hadoop 用户的直接代理访问。这使得特权用户可以在连接期间直接指定备用会话用户。如果连接的用户具有模拟请求的 userid 的 Hadoop 级别的特权，那么 HiveServer2 将作为那个请求的用户运行会话。

> Delegation token based connection for Oozie ([OOZIE-1457](https://issues.apache.org/jira/browse/OOZIE-1457)). This is the common mechanism for Hadoop ecosystem components.
Proxy user privileges in the Hadoop ecosystem are associated with both user names and hosts. That is, the privilege is available for certain users from certain hosts.  Delegation tokens in Hive are meant to be used if you are connecting from one authorized (blessed) machine and later you need to make a connection from another non-blessed machine. You get the delegation token from a blessed machine and connect using the delegation token from a non-blessed machine. The primary use case is Oozie, which gets a delegation token from the server machine and then gets another connection from a Hadoop task node.

- 基于授权令牌的 Oozie 连接。这是 Hadoop 生态系统组件的常见机制。

	Hadoop 生态系统中的代理用户特权与用户名和主机都相关。也就是说，该特权适用于来自特定主机的特定用户。

	Hive 中的委托令牌意味着，如果你从一个被授权的机器连接，然后你需要从另一个没有被授权的机器连接，那么你就可以使用委托令牌。

	你从授权的机器获得委托令牌，并使用来自非授权机器的委托令牌进行连接。主要的用例是 Oozie，它从服务器机器获得一个委托令牌，然后从一个 Hadoop 任务节点获得另一个连接。

	如果你只是从一台授权的机器上以特权用户的身份进行 JDBC 连接，那么直接代理访问是更简单的方法。通过使用 `hive.server2.proxy.user=<user>` 参数，你可以在 JDBC URL 中传递需要模拟的用户。

	参见 ProxyAuthTest.java 中的示例。

	使用 HiveServer2 二进制传输模式 `hive.server2.transport.mode` 支持委托令牌模式从 0.13.0 开始可用；Hive-13169 中增加了对 HTTP 传输模式下该特性的支持，这应该是 Hive 2.1.0 的一部分。

> If you are only making a JDBC connection as a privileged user from a single blessed machine, then direct proxy access is the simpler approach. You can just pass the user you need to impersonate in the JDBC URL by using the hive.server2.proxy.user=<user> parameter.

> See examples in [ProxyAuthTest.java](https://github.com/apache/hive/blob/master/beeline/src/test/org/apache/hive/beeline/ProxyAuthTest.java).

> Support for delegation tokens with HiveServer2 binary transport mode [hive.server2.transport.mode](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.transport.mode) has been available starting 0.13.0; support for this feature with HTTP transport mode was added in [HIVE-13169](https://issues.apache.org/jira/browse/HIVE-13169), which should be part of Hive 2.1.0.

> The other way is to use a pre-authenticated Kerberos Subject (see [HIVE-6486](https://issues.apache.org/jira/browse/HIVE-6486)). In this method, starting with Hive 0.13.0 the Hive JDBC client can use a pre-authenticated subject to authenticate to HiveServer2. This enables a middleware system to run queries as the user running the client.

另一种方法是使用预认证的 Kerberos 主题。在这个方法中，从 Hive 0.13.0 开始，Hive JDBC 客户端可以使用一个预认证的主题对 HiveServer2 进行认证。这使得中间件系统可以作为运行客户端的用户运行查询。

##### 2.3.1.1、Using Kerberos with a Pre-Authenticated Subject

> To use a pre-authenticated subject you will need the following changes.

要使用预认证主题，需要进行以下更改。

- 除了常规的 Hive JDBC jars 之外（`commons-configuration-1.6.jar`和`hadoop-core*.jar`不要求），还需要在类路径中添加 `hive-exec*.jar`。

- 除了具有 “principal” URL 属性外，还添加 `auth=kerberos` 和 `kerberosAuthType=fromSubject` JDBC URL 属性。

- 打开 `Subject.doAs()` 中的连接。

> Add hive-exec*.jar to the classpath in addition to the regular Hive JDBC jars (commons-configuration-1.6.jar and hadoop-core*.jar are not required).

> Add auth=kerberos and kerberosAuthType=fromSubject JDBC URL properties in addition to having the “principal" url property.

> Open the connection in Subject.doAs().

> The following code snippet illustrates the usage (refer to [HIVE-6486](https://issues.apache.org/jira/browse/HIVE-6486) for a complete [test case](https://issues.apache.org/jira/secure/attachment/12633984/TestCase_HIVE-6486.java)):

下面的代码片段说明了用法(参考 HIVE-6486 获得完整的测试用例):

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

当客户端需要更多的行时，给 JDBC 驱动程序一个提示，应该从数据库中获取多少行。

可以通过 JDBC 连接字符串指定用于每个语句的默认值。这个默认值随后可以用 JDBC API 覆盖每个语句。

如果在 JDBC 连接字符串中没有指定值，则从 HiveServer2 实例中获取默认的获取大小，作为会话初始化操作的一部分。

	jdbc:hive2://<host>:<port>/<db>;fetchsize=<value>

> Hive Version 4.0

> The Hive JDBC driver will receive a preferred fetch size from the instance of HiveServer2 it has connected to.  This value is specified on the server by the hive.server2.thrift.resultset.default.fetch.size configuration.

Hive JDBC 驱动程序将从它所连接的 HiveServer2 实例接收到一个首选的 fetch size。

这个值在服务器上由 `hive.server2.thrift.resultset.default.fetch.size` 配置指定。

> The JDBC fetch size is only a hint and the server will attempt to respect the client's requested fetch size though with some limits.  HiveServer2 will cap all requests at a maximum value specified by the hive.server2.thrift.resultset.max.fetch.size configuration value regardless of the client's requested fetch size.

JDBC fetch size 只是一个提示，服务器将尝试尊重客户端请求的 fetch size，但有一些限制。

HiveServer2 将以 `hive.server2.thrift.resultset.max.fetch.size`指定的最大值限制所有请求。而不考虑客户端请求的 fetch size。

> While a larger fetch size may limit the number of round-trips between the client and server, it does so at the expense of additional memory requirements on the client and server.

虽然较大的 fetch size 可能会限制客户端和服务器之间的往返次数，但这是以牺牲客户端和服务器上的额外内存需求为代价的。

> The default JDBC fetch size value may be overwritten, per statement, with the JDBC API:

在每条语句中使用 JDBC API 覆盖默认的 JDBC fetch size 值:

> Setting a value of 0 instructs the driver to use the fetch size value preferred by the server

- 将值设置为 0 将指示驱动程序使用服务器首选的 fetch size 值

> Setting a value greater than zero will instruct the driver to fetch that many rows, though the actual number of rows returned may be capped by the server

- 设置大于 0 的值将指示驱动程序获取那么多行，尽管实际返回的行数可能由服务器限制

> If no fetch size value is explicitly set on the JDBC driver's statement then the driver's default value is used

- 如果在 JDBC 驱动程序的语句中没有显式设置 fetch size 值，则使用驱动程序的默认值

	- 如果在 JDBC 连接字符串中指定了 fetch size 值，那么这是默认值

	- 如果在 JDBC 连接字符串中没有 fetch size 值，则使用服务器的首选 fetch size 作为默认值

> If the fetch size value is specified within the JDBC connection string, this is the default value
> If the fetch size value is absent from the JDBC connection string, the server's preferred fetch size is used as the default value

## 3、Python Client

> A Python client driver is available on [github](https://github.com/BradRuderman/pyhs2). For installation instructions, see [Setting Up HiveServer2: Python Client Driver](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2#SettingUpHiveServer2-PythonClientDriver).

## 4、Ruby Client

> A Ruby client driver is available on github at [https://github.com/forward3d/rbhive](https://github.com/forward3d/rbhive).

## 5、Integration with SQuirrel SQL Client 

> 1.Download, install and start the SQuirrel SQL Client from the SQuirrel SQL website.

> 2.Select 'Drivers -> New Driver...' to register Hive's JDBC driver that works with HiveServer2.

> a.Enter the driver name and example URL:

	Name: Hive
	Example URL: jdbc:hive2://localhost:10000/default

> 3.Select 'Extra Class Path -> Add' to add the following jars from your local Hive and Hadoop distribution.

	HIVE_HOME/lib/hive-jdbc-*-standalone.jar
	HADOOP_HOME/share/hadoop/common/hadoop-common-*.jar

> Version information

> Hive JDBC standalone jars are used in Hive 0.14.0 onward (HIVE-538); for previous versions of Hive, use `HIVE_HOME/build/dist/lib/*.jar` instead.

> The hadoop-common jars are for Hadoop 2.0; for previous versions of Hadoop, use `HADOOP_HOME/hadoop-*-core.jar` instead.

> 4.Select 'List Drivers'. This will cause SQuirrel to parse your jars for JDBC drivers and might take a few seconds. From the 'Class Name' input box select the Hive driver for working with HiveServer2:

	org.apache.hive.jdbc.HiveDriver

> 5.Click 'OK' to complete the driver registration. 

> 6.Select 'Aliases -> Add Alias...' to create a connection alias to your HiveServer2 instance.
> a.Give the connection alias a name in the 'Name' input box.
> b.Select the Hive driver from the 'Driver' drop-down.
> c.Modify the example URL as needed to point to your HiveServer2 instance.
> d.Enter 'User Name' and 'Password' and click 'OK' to save the connection alias.
> e.To connect to HiveServer2, double-click the Hive alias and click 'Connect'.

> When the connection is established you will see errors in the log console and might get a warning that the driver is not JDBC 3.0 compatible. These alerts are due to yet-to-be-implemented parts of the JDBC metadata API and can safely be ignored. To test the connection enter SHOW TABLES in the console and click the run icon.

> Also note that when a query is running, support for the 'Cancel' button is not yet available.

## 6、Integration with SQL Developer

> Integration with Oracle SQLDeveloper is available using JDBC connection.

[https://community.hortonworks.com/articles/1887/connect-oracle-sql-developer-to-hive.html](https://community.hortonworks.com/articles/1887/connect-oracle-sql-developer-to-hive.html)

## 7、Integration with DbVisSoftware's DbVisualizer

> 1.Download, install and start DbVisualizer free or purchase DbVisualizer Pro from [https://www.dbvis.com/](https://www.dbvis.com/).

> 2.Follow instructions on [github](https://github.com/cyanfr/dbvis_to_hortonworks_hiveserver2/wiki/How-I-Connected-DBVisualizer-9.2.2-on-Windows-to-Hortonwork-HiveServer2).

## 8、Advanced Features for Integration with Other Tools

### 8.1、Supporting Cookie Replay in HTTP Mode

> Version 1.2.0 and later. This option is available starting in [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-9709).

1.2.0 版本及更高版本。这个选项从 Hive 1.2.0 开始可用。

> HIVE-9709 introduced support for the JDBC driver to enable cookie replay. This is turned on by default so that incoming cookies can be sent back to the server for authentication.  

HIVE-9709 引入了对 JDBC 驱动程序的支持，以启用 cookie 重放。这在默认情况下是打开的，以便可以将传入的 cookie 发送回服务器进行身份验证。

> The JDBC connection URL when enabled should look like this:

在启用时，JDBC 连接 URL 应该是这样的:

	jdbc:hive2://<host>:<port>/<db>?transportMode=http;httpPath=<http_endpoint>;cookieAuth=true;cookieName=<cookie_name>

> cookieAuth is set to true by default.

- cookieAuth 默认为 true。

> cookieName: If any of the incoming cookies' keys match the value of cookieName, the JDBC driver will not send any login credentials/Kerberos ticket to the server. The client will just send the cookie alone back to the server for authentication. The default value of cookieName is hive.server2.auth (this is the HiveServer2 cookie name). 

- cookieName：如果传入的 cookie 的任何键与 cookieName 的值匹配，JDBC 驱动程序将不会向服务器发送任何登录凭据/Kerberos票据。客户端只会将 cookie 单独发送回服务器进行身份验证。cookieName 默认值为 `hive.server2.auth`(这是 HiveServer2 cookie 名)。

> To turn off cookie replay, cookieAuth=false must be used in the JDBC URL.

- 要关闭 cookie 重放，必须在 JDBC URL 中使用 `cookieAuth=false`

> Important Note: As part of [HIVE-9709](https://issues.apache.org/jira/browse/HIVE-9709), we upgraded Apache http-client and http-core components of Hive to 4.4. To avoid any collision between this upgraded version of HttpComponents and other any versions that might be present in your system (such as the one provided by Apache Hadoop 2.6 which uses http-client and http-core components version of 4.2.5), the client is expected to set CLASSPATH in such a way that Beeline-related jars appear before HADOOP lib jars. This is achieved via setting HADOOP_USER_CLASSPATH_FIRST=true before using hive-jdbc. In fact, in bin/beeline.sh we do this!

- 重要提示：作为 Hive-9709 的一部分，我们将 Hive 的 Apache http-client 和 http-core 组件升级到了4.4。避免 HttpComponents 升级版本和其他任何版本之间可能存在于你的系统的冲突(如 Apache Hadoop 2.6 提供的一个使用 ttp-client 和 http-core 组件版本的 4.2.5)，客户端将通过将 Beeline 相关的 jar 出现在 HADOOP lib jars 之前来设置 CLASSPATH。这是通过在使用 hive-jdbc 之前设置 `HADOOP_USER_CLASSPATH_FIRST=true` 来实现的。事实上，在 `bin/beeline.sh` 中我们就这样做了!

### 8.2、Using 2-way SSL in HTTP Mode 

> Version 1.2.0 and later. This option is available starting in [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-10447).

1.2.0 版本及更高版本。这个选项从 Hive 1.2.0 开始就可用。

> HIVE-10447 enabled the JDBC driver to support 2-way SSL in HTTP mode. Please note that HiveServer2 currently does not support 2-way SSL. So this feature is handy when there is an intermediate server such as Knox which requires client to support 2-way SSL.

HIVE-10447 使 JDBC 驱动程序支持 HTTP 模式下的双向 SSL。

请注意 HiveServer2 目前不支持双向 SSL。因此，当如 Knox 这样的中间服务器需要客户端支持双向 SSL 时，这个特性非常方便。

JDBC connection URL:

	jdbc:hive2://<host>:<port>/<db>;ssl=true;twoWay=true;sslTrustStore=<trust_store_path>;trustStorePassword=<trust_store_password>;sslKeyStore=<key_store_path>;keyStorePassword=<key_store_password>?transportMode=http;httpPath=<http_endpoint>

- `<trust_store_path>` 是客户端 truststore 文件所在的路径。这是一个强制的非空字段。

- `<trust_store_password>` 是访问信任库的密码。

- `<key_store_path>` 是客户端 keystore 文件所在的路径。这是一个强制的非空字段。

- `<key_store_password>` 是访问 keystore 的密码。

> <trust_store_path> is the path where the client's truststore file lives. This is a mandatory non-empty field.
> <trust_store_password> is the password to access the truststore.
> <key_store_path> is the path where the client's keystore file lives. This is a mandatory non-empty field.
> <key_store_password> is the password to access the keystore.

> For versions earlier than 0.14, see the [version note](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-HIVE-6972) above.

### 8.3、Passing HTTP Header Key/Value Pairs via JDBC Driver

> Version 1.2.0 and later. This option is available starting in [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-10339).

1.2.0 版本及更高版本。这个选项从 Hive 1.2.0 开始就可用。

> HIVE-10339 introduced an option for clients to provide custom HTTP headers that can be sent to the underlying server (Hive 1.2.0 and later).

Hive-10339 为客户端提引入了提供自定义 HTTP headers 的选项，可以发送到底层服务器。

JDBC connection URL:

	jdbc:hive2://<host>:<port>/<db>;transportMode=http;httpPath=<http_endpoint>;http.header.<name1>=<value1>;http.header.<name2>=<value2>

> When the above URL is specified, Beeline will call underlying requests to add an HTTP header set to <name1> and <value1> and another HTTP header set to <name2> and <value2>. This is helpful when the end user needs to send identity in an HTTP header down to intermediate servers such as Knox via Beeline for authentication, for example http.header.USERNAME=<value1>;http.header.PASSWORD=<value2>.

当指定上述 URL 时，Beeline 将调用底层请求添加一个 HTTP header 设置到 `<name1>` 和 `<value1>`，以及另一个 HTTP header 设置到 `<name2>` 和 `<value2>`。

当终端用户需要通过向中间服务器(如Knox)发送 HTTP header 中的身份验证时，这是很有用的，例如`http.header.USERNAME=<value1>;http.header.PASSWORD=<value2>`

> For versions earlier than 0.14, see the [version note](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=82903124#HiveServer2Clients-HIVE-6972) above. 

### 8.4、Passing Custom HTTP Cookie Key/Value Pairs via JDBC Driver

> In Hive version 3.0.0 [HIVE-18447](https://issues.apache.org/jira/browse/HIVE-18447) introduced an option for clients to provide custom HTTP cookies that can be sent to the underlying server. Some authentication mechanisms, like Single Sign On, need the ability to pass a cookie to some intermediate authentication service like Knox via the JDBC driver. 

在 Hive 3.0.0 版本中，Hive-18447 为客户端引入了一个选项，提供自定义的 HTTP cookie，可以发送到底层服务器。

一些身份验证机制(如单点登录)需要能够通过 JDBC 驱动程序将 cookie 传递给一些中间身份验证服务(如Knox)。

JDBC connection URL: 

	jdbc:hive2://<host>:<port>/<db>;transportMode=http;httpPath=<http_endpoint>;http.cookie.<name1>=<value1>;http.cookie.<name2>=<value2>

> When the above URL is specified, Beeline will call underlying requests to add HTTP cookie in the request header, and will set it to <name1>=<value1> and <name2>=<value2>. 

当指定了上述 URL 后，Beeline 会调用底层请求在请求头中添加 HTTP cookie，并将其设置为 `<name1>=<value1>` 和 `<name2>=<value2>`。