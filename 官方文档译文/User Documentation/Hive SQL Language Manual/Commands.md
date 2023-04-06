# LanguageManual Commands

[TOC]

> Commands are non-SQL statements such as setting a property or adding a resource. They can be used in HiveQL scripts or directly in the [CLI](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli) or [Beeline](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-Beeline%E2%80%93NewCommandLineShell).

命令是非 SQL 语句，比如设置属性或添加资源。

它们可以在 HiveQL 脚本中使用，也可以直接在 CLI 或 Beeline 中使用。

## 1、`quit` `exit`

> Use quit or exit to leave the interactive shell.

离开交互 shell

## 2、`reset`

> Resets the configuration to the default values (as of Hive 0.10: see [HIVE-3202](https://issues.apache.org/jira/browse/HIVE-3202)). Any configuration parameters that were set using the set command or -hiveconf parameter in hive commandline will get reset to default value.

将配置重置为默认值。

使用 set 命令或在 hive 命令行中的 `-hiveconf` 参数设置的任何配置参数将被重置为默认值。

> Note that this does not apply to configuration parameters that were set in set command using the "hiveconf:" prefix for the key name (for historic reasons).

注意，这不适用于在 set 命令中使用 “hiveconf:” 前缀作为键名设置的配置参数(出于历史原因)。

## 3、`set <key>=<value>`

> Sets the value of a particular configuration variable (key). Note: If you misspell the variable name, the CLI will not show an error.

设置特定的配置变量(key)的值。

注意：如果变量名拼写错误，CLI 不会展示错误。

## 4、`set`

> Prints a list of configuration variables that are overridden by the user or Hive.

打印用户或 Hive 覆盖的配置变量的列表。

## 5、`set -v`

> Prints all Hadoop and Hive configuration variables.

打印所有 Hadoop 和 Hive 的配置变量。

## 6、`add FILE[S] <filepath> <filepath>*`  、`add JAR[S] <filepath> <filepath>*` 、 `add ARCHIVE[S] <filepath> <filepath>*`

> Adds one or more files, jars, or archives to the list of resources in the distributed cache. See [Hive Resources](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveResources) for more information.

添加一个或多个文件、jars 或归档文件到分布式缓存中的资源列表。

## 7、`add FILE[S] <ivyurl> <ivyurl>*` 、 `add JAR[S] <ivyurl> <ivyurl>*` 、 `add ARCHIVE[S]<ivyurl> <ivyurl>*`	

> As of [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-9664), adds one or more files, jars or archives to the list of resources in the distributed cache using an [Ivy](http://ant.apache.org/ivy/) URL of the form ivy://group:module:version?query_string. See [Hive Resources](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveResources) for more information.

从 Hive 1.2.0 版本开始，使用形式为 `ivy://group:module:version?query_string` 的 Ivy URL 向分布式缓存中的资源列表中添加一个或多个文件、jars或归档文件。

## 8、`list FILE[S]` 、 `list JAR[S]` 、 `list ARCHIVE[S]`

> Lists the resources already added to the distributed cache. See [Hive Resources](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveResources) for more information.

列出已经添加到分布式缓存中的资源。

## 9、`list FILE[S] <filepath>*` 、 `list JAR[S] <filepath>*` 、 `list ARCHIVE[S] <filepath>*`

> Checks whether the given resources are already added to the distributed cache or not. See [Hive Resources](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveResources) for more information.

检查给定的资源是否已经添加到了分布式缓存。

## 10、`delete FILE[S] <filepath>*` 、 `delete JAR[S] <filepath>*`  、 `delete ARCHIVE[S] <filepath>*`

> Removes the resource(s) from the distributed cache.

移除分布式缓存中的资源

## 11、`delete FILE[S] <ivyurl> <ivyurl>*` 、 `delete JAR[S] <ivyurl> <ivyurl>*` 、 `delete ARCHIVE[S] <ivyurl> <ivyurl>*`	

> As of [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-9664), removes the resource(s) which were added using the <ivyurl> from the distributed cache. See Hive Resources for more information.

从 Hive 1.2.0 版本开始，删除使用 `<ivyurl>` 添加到分布式缓存中的资源。

## 12、`! <command>`

> Executes a shell command from the Hive shell.

在 Hive shell 中执行一个 shell 命令。

## 13、`dfs <dfs command>`

> Executes a dfs command from the Hive shell.

在 Hive shell 中执行一个 dfs 命令。

## 14、`<query string>`

> Executes a Hive query and prints results to standard output.

执行一个 Hive 查询，打印结果到标准输出。

## 15、`source FILE <filepath>`

> Executes a script file inside the CLI.

在 CLI 中执行脚本文件。

## 16、compile `<groovy string>` AS GROOVY NAMED <name>

> This allows inline Groovy code to be compiled and be used as a UDF (as of Hive 0.13.0). For a usage example, see Nov. 2013 Hive Contributors Meetup Presentations – Using Dynamic Compilation with Hive.

这允许编译内联的 Groovy 代码，并作为 UD 使用(从 Hive 0.13.0 开始)。

## 17、Sample Usage:

```sh
hive> set mapred.reduce.tasks=32;
hive> set;
hive> select a.* from tab1;
hive> !ls;
hive> dfs -ls;
```

----------------------------------------------------

```sh
hive> set;
...
env:HADOOP_COMMON_HOME=/opt/hadoop-3.2.1
env:HADOOP_CONF_DIR=/opt/hadoop-3.2.1/etc/hadoop
...
system:user.country=CN
system:user.dir=/root/script
system:user.home=/root
....

####################################################
hive> !ls /root;
anaconda-ks.cfg
data
jar
python_script
run_env
script
sql_script

####################################################
hive> dfs -cat /in/calavg.txt;
A,math,70
B,math,80
C,math,90
A,science,88
B,science,86
C,science,90
```