# HCatalog CLI

[TOC]

## 1、Set Up

> The HCatalog command line interface (CLI) can be invoked as HIVE_HOME=hive_home hcat_home/bin/hcat where hive_home is the directory where Hive has been installed and hcat_home is the directory where HCatalog has been installed.

HCatalog 命令行调用方式为 `HIVE_HOME=hive_home` `hcat_home/bin/hcat`，其中 `hive_home` 是 Hive 安装的目录，`hcat_home`是 HCatalog 安装的目录。

> If you are using BigTop's rpms or debs you can invoke the CLI by doing /usr/bin/hcat.

如果你正在使用 BigTop 的 rpm 或 deb，你可以通过执行 `/usr/bin/hcat` 来调用 CLI。

## 2、HCatalog CLI

> The HCatalog CLI supports these command line options:

HCatalog CLI 支持如下命令行选项：

Option |    Usage              |   Description
---|:---|:---
-g  | hcat -g mygroup ...      |   Tells HCatalog that the table which needs to be created must have group "mygroup". 【告诉HCatalog，要创建的表必须具有分组"mygroup"】
-p  | hcat -p rwxr-xr-x ...    |   Tells HCatalog that the table which needs to be created must have permissions "rwxr-xr-x".【告诉HCatalog，要创建的表必须具有"rwxr-xr-x"权限】
-f  | hcat -f myscript.hcatalog ... |  Tells HCatalog that myscript.hcatalog is a file containing DDL commands to execute.【告诉HCatalog，myscript.hcatalog是一个包含要执行的DDL命令的文件】
-e  | hcat -e 'create table mytable(a int);' ... | Tells HCatalog to treat the following string as a DDL command and execute it.【告诉HCatalog，将下面的字符串当作一个DDL命令并执行它】
-D  | hcat -Dkey=value ...     |    Passes the key-value pair to HCatalog as a Java System Property.【将键值对传给HCatalog作为一个java系统属性】
 -  | hcat| Prints a usage message.【打印用法信息】

> Note the following:

注意下列事项：

- `-g` 和 `-p` 选项不是强制性的。

- 只能提供 `-e` 或 `-f` 选项，不能同时提供这两个选项。

- 选项的顺序并不重要；可以以任何顺序指定选项

> The -g and -p options are not mandatory.
> Only one -e or -f option can be provided, not both.
> The order of options is immaterial; you can specify the options in any order.

> If no option is provided, then a usage message is printed:

如果没有提供选项，则会打印一个用法信息:

	Usage:  hcat  { -e "<query>" | -f <filepath> }  [-g <group>] [-p <perms>] [-D<name>=<value>]

### 2.1、Owner Permissions

> When using the HCatalog CLI, you cannot specify a permission string without read permissions for owner, such as -wxrwxr-x, because the string begins with "-". If such a permission setting is desired, you can use the octal version instead, which in this case would be 375. Also, any other kind of permission string where the owner has read permissions (for example r-x----- or r--r--r--) will work fine.

当使用 HCatalog CLI 时，不能为所有者指定没有读权限的权限字符串，例如 `-wxrwxr-x`，因为字符串以 “-” 开头。

如果需要这样的权限设置，则可以使用八进制版本，在本例中为 375。

另外，所有者具有读权限的任何其他类型的权限字符串(例如 `r-x-----` 或 `r--r--r--`)都可以正常工作。

### 2.2、Hive CLI

> Many hcat commands can be issued as hive commands, including all HCatalog DDL commands. The Hive CLI includes some commands that are not available in the HCatalog CLI. Note these differences:

许多 hcat 命令可以作为 hive 命令执行，包括所有 HCatalog DDL 命令。

Hive CLI 中包含了一些 HCatalog CLI 中不可用的命令。注意这些差异:

> "hcat -g" and "hcat -p" for table group and permission settings are only available in the HCatalog CLI.

- 表组和权限设置的 `hcat -g` 和 `hcat -p` 只能在 HCatalog CLI 中使用。

> hcat uses the -p flag for permissions but hive uses it to specify a port number.

- hcat 使用 -p 标志来表示权限，而 hive 使用它来指定端口号。

> hcat uses the -D flag without a space to define key=value pairs but hive uses -d or --define with a space (also --hivevar).For example, "hcat -DA=B" versus "hive -d A=B".

- hcat 使用不带空格的 -D 标志来定义键=值对，而 hive 使用带空格的 -d 或 --define(还有 --hivevar)。例如，`hcat -DA=B` 和 `hive -d A=B`。

> hcat without any flags prints a help message but hive uses the -H flag or --help.

- 不带任何标志的 hcat 打印帮助信息，但 hive 使用 -H 标志或 --help。

> The Hive CLI is documented [here](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli).

## 3、HCatalog DDL

> HCatalog supports all [Hive Data Definition Language](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL) except those operations that require running a MapReduce job. For commands that are supported, any variances are noted below.

除了需要运行 MapReduce job 的操作外，HCatalog 支持所有 Hive 数据定义语言。对于所支持的命令，下面会指出任何差异。

> HCatalog does not support the following Hive DDL and other HiveQL commands:

HCatalog 不支持以下 Hive DDL 和其他 HiveQL 命令：

- ALTER INDEX ... REBUILD
- CREATE TABLE ... AS SELECT
- ALTER TABLE ... CONCATENATE
- ALTER TABLE ARCHIVE/UNARCHIVE PARTITION
- ANALYZE TABLE ... COMPUTE STATISTICS
- IMPORT FROM ...
- EXPORT TABLE

> For information about using WebHCat for DDL commands, see [URL Format](https://cwiki.apache.org/confluence/display/Hive/WebHCat+UsingWebHCat#WebHCatUsingWebHCat-URLFormat) and [WebHCat Reference: DDL Resources](https://cwiki.apache.org/confluence/display/Hive/WebHCat+Reference+AllDDL).

有关 WebHCat 使用 DDL 命令的信息，请参见 URL Format 和 WebHCat Reference: DDL Resources。

### 3.1、Create/Drop/Alter Table

#### 3.1.1、CREATE TABLE

> If you create a table with a CLUSTERED BY clause you will not be able to write to it with Pig or MapReduce. This is because they do not understand how to partition the table, so attempting to write to it would cause data corruption.

如果创建了一个带有 CLUSTERED BY 子句的表，那么你将无法使用 Pig 或 MapReduce 对它进行写入操作。

这是因为他们不了解如何对表进行分区，因此试图对表进行写操作，将导致数据损坏。

#### 3.1.2、CREATE TABLE AS SELECT

> Not supported. Throws an exception with the message "Operation Not Supported".

不支持。抛出一个异常，消息为 "Operation Not Supported"。

#### 3.1.3、DROP TABLE

> Supported. Behavior the same as Hive.

支持。行为类似 Hive中的。

#### 3.1.4、ALTER TABLE

> Supported except for the REBUILD and CONCATENATE options. Behavior the same as Hive.

支持，除了 REBUILD 和 CONCATENATE 选项。行为类似 Hive中的。

### 3.2、Create/Drop/Alter View

> Note: Pig and MapReduce cannot read from or write to views.

注意：Pig 和 MapReduce 从视图读取或写入到视图。

#### 3.2.1、CREATE VIEW

> Supported. Behavior same as Hive.

支持。行为类似 Hive中的。

#### 3.2.2、DROP VIEW

> Supported. Behavior same as Hive.

支持。行为类似 Hive中的。

#### 3.2.3、ALTER VIEW

> Supported. Behavior same as Hive.

支持。行为类似 Hive中的。

### 3.3、Show/Describe

#### 3.3.1、SHOW TABLES

> Supported. Behavior same as Hive.

支持。行为类似 Hive中的。

#### 3.3.2、SHOW PARTITIONS

> Not supported. Throws an exception with message "Operation Not Supported".

不支持。抛出一个异常，消息为 "Operation Not Supported"。

#### 3.3.3、SHOW FUNCTIONS

> Supported. Behavior same as Hive.

支持。行为类似 Hive中的。

#### 3.3.4、DESCRIBE

> Supported. Behavior same as Hive.

支持。行为类似 Hive中的。

### 3.4、Create/Drop Index

> CREATE and DROP INDEX operations are supported.

支持 CREATE 和 DROP INDEX 操作。

> Note: Pig and MapReduce cannot write to a table that has auto rebuild on, because Pig and MapReduce do not know how to rebuild the index.

注意：Pig 和 MapReduce 不能写入到自动重建的表，因为 Pig 和 MapReduce 不知道如何重建索引。

### 3.5、Create/Drop Function

> CREATE and DROP FUNCTION operations are supported, but created functions must still be registered in Pig and placed in CLASSPATH for MapReduce.

支持 CREATE 和 DROP FUNCTION 操作，但创建的函数仍然必须在 Pig 中注册，并放置在 MapReduce 的 CLASSPATH 中。

### 3.6、"dfs" Command and "set" Command

> Supported. Behavior same as Hive.

支持。行为类似 Hive中的。

### 3.7、Other Commands

> Any command not listed above is NOT supported and throws an exception with the message "Operation Not Supported".

不支持上面没有列出的任何命令，并抛出一个异常，消息为 "Operation Not Supported"。

## 4、CLI Errors

### 4.1、Authentication

> If a failure results in a message like "2010-11-03 16:17:28,225 WARN hive.metastore ... - Unable to connect metastore with URI thrift://..." in /tmp/<username>/hive.log, then make sure you have run "kinit <username>@FOO.COM" to get a Kerberos ticket and to be able to authenticate to the HCatalog server.

### 4.2、Error Log

> If other errors occur while using the HCatalog CLI, more detailed messages are written to /tmp/<username>/hive.log.


Navigation Links:

Previous: [Reader and Writer Interfaces](https://cwiki.apache.org/confluence/display/Hive/HCatalog+ReaderWriter)
Next: [Storage Formats](https://cwiki.apache.org/confluence/display/Hive/HCatalog+StorageFormats)

Hive command line interface: [Hive CLI](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli)
Hive DDL commands: [Hive Data Definition Language](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL)
WebHCat DDL resources: [WebHCat Reference: DDL](https://cwiki.apache.org/confluence/display/Hive/WebHCat+Reference+AllDDL)

General: [HCatalog Manual](https://cwiki.apache.org/confluence/display/Hive/HCatalog) – [WebHCat Manual](https://cwiki.apache.org/confluence/display/Hive/WebHCat) – [Hive Wiki Home](https://cwiki.apache.org/confluence/display/Hive/Home) – [Hive Project Site](http://hive.apache.org/)