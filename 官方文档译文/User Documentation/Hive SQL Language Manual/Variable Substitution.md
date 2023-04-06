# Variable Substitution

[TOC]

## 1、Introduction

> Hive is used for batch and interactive queries. Variable Substitution allows for tasks such as separating environment-specific configuration variables from code.

Hive 用于批量查询和交互式查询。

变量替换允许**将特定于环境的配置变量从代码中分离出来**。

> The Hive variable substitution mechanism was designed to avoid some of the code that was getting baked into the scripting language on top of Hive.

Hive 变量替换机制的设计是为了避免将一些代码融入到 Hive 之上的脚本语言中。

> Examples such as the following shell commands may (inefficiently) be used to set variables within a script:

例如下面的 shell 命令可以(低效地)用于**在脚本中设置变量**:

```sh
$ a=b
$ hive -e " describe $a "
```

---------------------------------

```sh
[root@zgg ~]# cat test.sh
table="hbase_table_1"
hive -e "desc $table"

[root@zgg ~]# sh test.sh
key                     int                                         
value                   string     
```

---------------------------------

> This is frustrating as Hive becomes closely coupled with scripting languages. The Hive startup time of a couple seconds is non-trivial when doing thousands of manipulations such as multiple hive -e invocations.

这是令人沮丧的，因为 Hive 与脚本语言紧密结合。当执行数以千计的操作(比如多次`Hive -e`调用)时，几秒的 Hive 启动时间是非常重要的。

> Hive Variables combine the set capability you know and love with some limited yet powerful substitution ability.

Hive 变量将你所知道和喜爱的集合功能与一些有限但强大的替代功能结合起来。

> The following example:

下面的例子：

```sh
$ bin/hive --hiveconf a=b -e 'set a; set hiveconf:a; \
create table if not exists b (col int); describe ${hiveconf:a}'
```

> results in:

产生：

```s
Hive history file=/tmp/edward/hive_job_log_edward_201011240906_1463048967.txt
a=b
hiveconf:a=b
OK
Time taken: 5.913 seconds
OK
col int
Time taken: 0.754 seconds
```

> For general information about Hive command line options, see [Hive CLI](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli).

Hive 命令行选项的详细信息请参见 Hive CLI。

> Version information. The hiveconf option was added in version 0.7.0 (JIRA [HIVE-1096](https://issues.apache.org/jira/browse/HIVE-1096)). Version 0.8.0 added the options define and hivevar (JIRA [HIVE-2020](https://issues.apache.org/jira/browse/HIVE-2020)), which are equivalent and are not described here. They create custom variables in a namespace that is separate from the hiveconf, system, and env namespaces.

版本信息：

hiveconf 选项在 0.7.0 版中添加。版本 0.8.0 增加了 define 和 hivevar 选项，它们是等价的，这里不描述。它们在与 hiveconf、system 和 env 名称空间分离的名称空间中创建自定义变量。

## 2、Using Variables

> There are three namespaces for variables – hiveconf, system, and env. ([Custom variables](https://issues.apache.org/jira/browse/HIVE-2020) can also be created in a separate namespace with the define or hivevar option in Hive 0.8.0 and later releases.)

变量有三个名称空间：hiveconf、system 和 env。

(在 Hive 0.8.0 及后续版本中，自定义变量也可以通过 define 或 hivevar 选项在单独的命名空间中创建。)

> The hiveconf variables are set as normal:

hiveconf 变量按正常设置:

```sh
set x=myvalue
```

> However they are retrieved using:

然而，它们是通过以下方式获取的:

```sh
${hiveconf:x}
```

> Annotated examples of usage from the test case ql/src/test/queries/clientpositive/set_processor_namespaces.q:

测试用例 `ql/src/test/queries/clientpositive/set_processor_namespaces.q` 中标注的用法示例:

```sh
set zzz=5;
--  sets zzz=5
set zzz;
 
set system:xxx=5;
set system:xxx;
-- sets a system property xxx to 5
 
set system:yyy=${system:xxx};
set system:yyy;
-- sets yyy with value of xxx
 
set go=${hiveconf:zzz};
set go;
-- sets go base on value on zzz
 
set hive.variable.substitute=false;
set raw=${hiveconf:zzz};
set raw;
-- disable substitution set a value to the literal
 
set hive.variable.substitute=true;
 
EXPLAIN SELECT * FROM src where key=${hiveconf:zzz};
SELECT * FROM src where key=${hiveconf:zzz};
--use a variable in a query
 
set a=1;
set b=a;
set c=${hiveconf:${hiveconf:b}};
set c;
--uses nested variables.
 
 
set jar=../lib/derby.jar;
 
add file ${hiveconf:jar};
list file;
delete file ${hiveconf:jar};
list file;
```

---------------------------------------------------

```sh
hive> set zzz=5;
hive> set zzz;
zzz=5
hive> set system:xxx=5;
hive> set system:xxx;
system:xxx=5
hive> set system:yyy=${system:xxx};
hive> set system:yyy;
system:yyy=5
hive> set go=${hiveconf:zzz};
hive> set go;
go=5
hive> set a=1;
hive> set b=a;
hive> set b;
b=a
hive> set c=${hiveconf:${hiveconf:b}};
hive> set c;
c=1
```

---------------------------------------------------

## 3、Substitution During Query Construction

> Hive substitutes the value for a variable when a query is constructed with the variable.

Hive 在使用变量构造查询时，替换该变量的值。

- 如果运行两个不同的 Hive 会话，变量值不会在会话之间混合。

- 如果在同一个 Hive 会话中设置了相同名称的变量，则查询时使用上次设置的值

> If you run two different Hive sessions, variable values will not be mixed across sessions.
> If you set variables with the same name in the same Hive session, a query uses the last set value.

## 4、Disabling Variable Substitution

> Variable substitution is on by default ([hive.variable.substitute](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.variable.substitute)=true). If this causes an issue with an already existing script, disable it using the following command:

默认情况下，变量替换是开启的(hive.variable.substitute=true)。

如果这导致一个已经存在的脚本的问题，使用以下命令禁用它:

	set hive.variable.substitute=false;
