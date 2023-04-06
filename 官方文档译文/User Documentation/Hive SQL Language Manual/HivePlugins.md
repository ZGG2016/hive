# HivePlugins

[TOC]

## 1、Creating Custom UDFs

> First, you need to create a new class that extends UDF, with one or more methods named evaluate.

首先**建一个新类，继承 UDF，具有一个或多个 evaluate 方法**。

```java
package com.example.hive.udf;
 
import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.io.Text;
 
public final class Lower extends UDF {
  public Text evaluate(final Text s) {
    if (s == null) { return null; }
    return new Text(s.toString().toLowerCase());
  }
}
```

注意：已存在 Lower 这样功能的内建函数。

> (Note that there's already a built-in function for this, it's just an easy example).

> After compiling your code to a jar, you need to add this to the Hive classpath. See the section below on deploying jars.

**将代码打成 jar 包，并将这个 jar 包添加到 Hive classpath。**

> Once Hive is started up with your jars in the classpath, the final step is to register your function as described in [Create Function](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateFunction):

jar 包添加到 Hive classpath，启动 Hive 后，最后一步就是**注册你的函数**：

```sql
create temporary function my_lower as 'com.example.hive.udf.Lower';
```

> Now you can start using it:

开始使用：

```sh
hive> select my_lower(title), sum(freq) from titles group by my_lower(title);
 
...
 
Ended Job = job_200906231019_0006
OK
cmo 13.0
vp  7.0
```

For a more involved example, see [this page](https://cwiki.apache.org/confluence/display/Hive/GenericUDAFCaseStudy).

> As of [Hive 0.13](https://issues.apache.org/jira/browse/HIVE-6047), you can register your function as a permanent UDF either in the current database or in a specified database, as described in [Permanent Functions](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-PermanentFunctions). For example:

Hive 0.13 开始，可以将你的函数注册成一个持久化的 UDF，在当前数据库，或指定数据库。

```sql
create function my_db.my_lower as 'com.example.hive.udf.Lower';
```

## 2、Deploying Jars for User Defined Functions and User Defined SerDes

> In order to start using your UDF, you first need to add the code to the classpath:

为了开始使用你的 UDF，首先要添加代码到 classpath:

```sh
hive> add jar my_jar.jar;
Added my_jar.jar to class path
```

> By default, it will look in the current directory. You can also specify a full path:

默认情况下，查找当前目录下的 jar 包，但也可以指定路径：

```sh
hive> add jar /tmp/my_jar.jar;
Added /tmp/my_jar.jar to class path
```

> Your jar will then be on the classpath for all jobs initiated from that session. To see which jars have been added to the classpath you can use:

然后，你的 jar 包将位于该会话启动的所有 jobs 的 classpath 上。要查看 classpath 中添加了哪些 jar 包，可以使用:

```sh
hive> list jars;
my_jar.jar
```
See [Hive CLI](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli#LanguageManualCli-HiveResources) for full syntax and more examples.

> As of [Hive 0.13](https://issues.apache.org/jira/browse/HIVE-6380), UDFs also have the option of being able to specify required jars in the [CREATE FUNCTION](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateFunction) statement:

Hive 0.13 开始，可以在注册的时候，指定 jar 包的路径：

```sql
CREATE FUNCTION myfunc AS 'myclass' USING JAR 'hdfs:///path/to/jar';
```
这将把 jar 包添加到 classpath 中，就好像在那个 jar 上调用了 `add jar` 一样。

> This will add the jar to the classpath as if ADD JAR had been called on that jar. 


[https://cwiki.apache.org/confluence/display/Hive/HivePlugins](https://cwiki.apache.org/confluence/display/Hive/HivePlugins)