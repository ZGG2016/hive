# Storage Handlers

[TOC]

## 1、Introduction

> This page documents the storage handler support being added to Hive as part of work on [HBaseIntegration](https://cwiki.apache.org/confluence/display/Hive/HBaseIntegration). The motivation is to make it possible to allow Hive to access data stored and managed by other systems in a modular, extensible fashion.

这个页面记录了添加到 Hive 的存储处理器支持作为 HBaseIntegration 工作的一部分。

其动机是**允许 Hive 以模块化、可扩展的方式访问其他系统存储和管理的数据**。

> Besides HBase, a storage handler implementation is also available for [Hypertable](http://code.google.com/p/hypertable/wiki/HiveExtension), and others are being developed for [Cassandra](https://issues.apache.org/jira/browse/HIVE-1434), [Azure Table](https://blogs.msdn.microsoft.com/mostlytrue/2014/04/04/analyzing-azure-table-storage-data-with-hdinsight/), [JDBC](https://cwiki.apache.org/confluence/display/Hive/JdbcStorageHandler) (MySQL and others), [MongoDB](https://github.com/yc-huang/Hive-mongo), [ElasticSearch](https://www.elastic.co/guide/en/elasticsearch/hadoop/current/hive.html), [Phoenix HBase](https://phoenix.apache.org/hive_storage_handler.html?platform=hootsuite), [VoltDB](https://issues.voltdb.com/browse/ENG-10736?page=com.atlassian.jira.plugin.system.issuetabpanels%3Aall-tabpanel) and [Google Spreadsheets](https://github.com/balshor/gdata-storagehandler).  A [Kafka handler](https://github.com/HiveKa/HiveKa) demo is available.

除了 HBase，Hypertable 也有一个存储处理器实现，Cassandra、Azure Table、JDBC (MySQL等)、MongoDB、ElasticSearch、Phoenix HBase、voldb 和 Google Spreadsheets 也在开发中。一个 Kafka handler 示例是可用的。

> Hive storage handler support builds on existing extensibility features in both Hadoop and Hive:

对 Hive 存储处理器的支持建立在 Hadoop 和 Hive 中现有的扩展特性上:

- input formats
- output formats
- serialization/deserialization libraries

> Besides bundling these together, a storage handler can also implement a new metadata hook interface, allowing Hive DDL to be used for managing object definitions in both the Hive metastore and the other system's catalog simultaneously and consistently.

除了将这些绑定在一起，存储处理器还可以实现一个新的元数据钩子接口，**允许 Hive DDL 用于同时且一致地管理 Hive metastore 和其他系统目录中的对象定义**。

## 2、Terminology

> Before storage handlers, Hive already had a concept of managed vs external tables. A managed table is one for which the definition is primarily managed in Hive's metastore, and for whose data storage Hive is responsible. An external table is one whose definition is managed in some external catalog, and whose data Hive does not own (i.e. it will not be deleted when the table is dropped).

在存储处理器之前，Hive 已经有了受管表和外部表的概念。

受管表是在 Hive 的 metastore 中管理的表，Hive 负责表数据的存储。

外部表在外部 catalog 中管理，它的数据不属于 Hive(也就是说，当删除表时，数据不会被删除)。

> Storage handlers introduce a distinction between native and non-native tables. A native table is one which Hive knows how to manage and access without a storage handler; a non-native table is one which requires a storage handler.

存储处理器引入了原生表和非原生表间的区别。

**原生表是 Hive 知道如何在没有存储处理器的情况下管理和访问的表**。

**非原生表是需要存储处理器的表**。

> These two distinctions (managed vs. external and native vs non-native) are orthogonal. Hence, there are four possibilities for base tables:

这两个区别(受管表与外部表，原生表与非原生表)是正交的。因此，基表有四种可能:

> managed native: what you get by default with CREATE TABLE

- 受管原生表：使用 `CREATE TABLE` 默认获得的内容

> external native: what you get with CREATE EXTERNAL TABLE when no STORED BY clause is specified

- 外部原生表：当没有指定 `STORED BY` 子句时，使用 `CREATE external TABLE` 获得的结果

> managed non-native: what you get with CREATE TABLE when a STORED BY clause is specified; Hive stores the definition in its metastore, but does not create any files itself; instead, it calls the storage handler with a request to create a corresponding object structure

- 受管非原生表：当指定了 `STORED BY` 子句时，使用 `CREATE TABLE` 获得的内容；Hive 在它的 metastore 中存储定义，但不创建任何文件；相反，它通过请求调用存储处理器来创建对应的对象结构

> external non-native: what you get with CREATE EXTERNAL TABLE when a STORED BY clause is specified; Hive registers the definition in its metastore and calls the storage handler to check that it matches the primary definition in the other system

- 外部非原生表：当指定了 `STORED BY` 子句时，使用 `CREATE EXTERNAL TABLE`获得的内容；Hive 在它的 metastore 中注册定义，并调用存储处理器来检查它是否与其他系统中的主定义相匹配

> Note that we avoid the term file-based in these definitions, since the form of storage used by the other system is irrelevant.

注意，在这些定义中，我们避免使用术语“基于文件”，因为其他系统使用的存储形式是无关紧要的。

## 3、DDL

> Storage handlers are associated with a table when it is created via the new STORED BY clause, an alternative to the existing ROW FORMAT and STORED AS clause:

当**通过新的 `STORED BY` 子句创建表时，存储处理器与表相关联，该子句替代了现有的 `ROW FORMAT` 和 `STORED AS` 子句**:

```
CREATE [EXTERNAL] TABLE [IF NOT EXISTS] table_name
  [(col_name data_type [COMMENT col_comment], ...)]
  [COMMENT table_comment]
  [PARTITIONED BY (col_name data_type [col_comment], col_name data_type [COMMENT col_comment], ...)]
  [CLUSTERED BY (col_name, col_name, ...) [SORTED BY (col_name, ...)] INTO num_buckets BUCKETS]
  [
   [ROW FORMAT row_format] [STORED AS file_format]
   | STORED BY 'storage.handler.class.name' [WITH SERDEPROPERTIES (...)]
  ]
  [LOCATION hdfs_path]
  [AS select_statement]
```
> When STORED BY is specified, then row_format (DELIMITED or SERDE) and STORED AS cannot be specified. Optional SERDEPROPERTIES can be specified as part of the STORED BY clause and will be passed to the serde provided by the storage handler.

**如果指定了 `STORED BY` ，则不能指定 `row_format (DELIMITED or SERDE)`和 `STORED AS`**。

可选的 `SERDEPROPERTIES` 可以作为 `STORED BY` 子句的一部分指定，并将传递给存储处理器提供的 serde。

> See [CREATE TABLE](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTable) and [Row Format, Storage Format, and SerDe](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-RowFormat,StorageFormat,andSerDe) for more information.

有关更多信息，请参见 CREATE TABLE 和 Row Format, Storage Format, and SerDe。

例如：

```sql
CREATE TABLE hbase_table_1(key int, value string)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" = "cf:string",
"hbase.table.name" = "hbase_table_0"
);
```

> DROP TABLE works as usual, but ALTER TABLE is not yet supported for non-native tables.

`DROP TABLE` 正常工作，但是对于非原生表，还不支持 `ALTER TABLE`。

------------------------------------------------

```sql
-- hive下创建表hbase_table_1
CREATE TABLE hbase_table_1(key int, value string)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" = "cf:string",
"hbase.table.name" = "hbase_table_0"
);

-- hive下查看
hive> show tables;
OK
...
hbase_table_1

-- hbase下查看
hbase(main):001:0> list
TABLE                                                                                                                               
hbase_table_0                                                                                                                       
student                                                                                                                             
2 row(s)
Took 0.5598 seconds                                                                                                                 
=> ["hbase_table_0", "student"]

-- hive下插入一行
hive> insert into table hbase_table_1 values(1,"zhangsan");

-- hive下查看
hive> select * from hbase_table_1;
OK
....
1       zhangsan

-- hbase下查看
hbase(main):004:0> scan "hbase_table_0"
ROW                                COLUMN+CELL                                                                                      
 1                                 column=cf:string, timestamp=1612419399214, value=zhangsan                                        
1 row(s)

-- hbase下插入一行
hbase(main):005:0> put 'hbase_table_0', '2', 'cf:string', 'lisi'
Took 0.0794 seconds                                                                                                                
-- hbase下查看
hbase(main):006:0> scan "hbase_table_0"
ROW                                COLUMN+CELL                                                                                      
 1                                 column=cf:string, timestamp=1612419399214, value=zhangsan                                        
 2  ..... 

-- hive下查看
hive> select * from hbase_table_1;
OK
....
1       zhangsan
2       lisi                   
```

------------------------------------------------

## 4、Storage Handler Interface

> The Java interface which must be implemented by a storage handler is reproduced below; for details, see the Javadoc in the code:

下面再现了必须由存储处理器实现的 Java 接口；详细信息，请参见代码中的 Javadoc：

```java
package org.apache.hadoop.hive.ql.metadata;
 
import java.util.Map;
 
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.hive.metastore.HiveMetaHook;
import org.apache.hadoop.hive.ql.plan.TableDesc;
import org.apache.hadoop.hive.serde2.SerDe;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.OutputFormat;
 
public interface HiveStorageHandler extends Configurable {
  public Class<? extends InputFormat> getInputFormatClass();
  public Class<? extends OutputFormat> getOutputFormatClass();
  public Class<? extends SerDe> getSerDeClass();
  public HiveMetaHook getMetaHook();
  public void configureTableJobProperties(
    TableDesc tableDesc,
    Map<String, String> jobProperties);
}
```

> The HiveMetaHook is optional, and described in the next section. If getMetaHook returns non-null, the returned object's methods will be invoked as part of metastore modification operations.

HiveMetaHook 是可选的，下一节将对此进行描述。
 
如果 getMetaHook 返回非空，返回对象的方法将作为 metastore 修改操作的一部分被调用。

> The configureTableJobProperties method is called as part of planning a job for execution by Hadoop. It is the responsibility of the storage handler to examine the table definition and set corresponding attributes on jobProperties. At execution time, only these jobProperties will be available to the input format, output format, and serde.

configureTableJobProperties 方法作为 Hadoop 执行作业的一部分调用。

存储处理器负责检查表定义，并在 jobProperties 上设置相应的属性。在执行时，只有这些 jobProperties 对输入格式、输出格式和 serde 可用。

> See also [FilterPushdownDev](https://cwiki.apache.org/confluence/display/Hive/FilterPushdownDev) to learn how a storage handler can participate in filter evaluation (to avoid full-table scans).

另请参阅 FilterPushdownDev，了解存储处理器如何参与过滤计算(以避免全表扫描)。

## 5、HiveMetaHook Interface

> The HiveMetaHook interface is reproduced below; for details, see the Javadoc in the code:

HiveMetaHook 接口如下；详细信息，请参见代码中的 Javadoc:

```java
package org.apache.hadoop.hive.metastore;
 
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.Table;
 
public interface HiveMetaHook {
  public void preCreateTable(Table table)
    throws MetaException;
  public void rollbackCreateTable(Table table)
    throws MetaException;
  public void commitCreateTable(Table table)
    throws MetaException;
  public void preDropTable(Table table)
    throws MetaException;
  public void rollbackDropTable(Table table)
    throws MetaException;
  public void commitDropTable(Table table, boolean deleteData)
    throws MetaException;
```

> Note that regardless of whether or not a remote Thrift metastore process is used in the Hive configuration, meta hook calls are always made from the Hive client JVM (never from the Thrift metastore server). This means that the jar containing the storage handler class needs to be available on the client, but not the thrift server.

注意，无论 Hive 配置中是否使用了远程 Thrift metastore 进程，meta hook 调用总是从 Hive 客户端 JVM(从不从 Thrift metastore 服务器)进行。

这意味着包含存储处理器类的 jar 需要在客户端上可用，而不是在 thrift 服务器上可用。

> Also note that there is no facility for two-phase commit in metadata transactions against the Hive metastore and the storage handler. As a result, there is a small window in which a crash during DDL can lead to the two systems getting out of sync.

还要注意的是，元数据事务中没有针对 Hive metastore 和存储处理器的两阶段提交功能。因此，DDL 期间的崩溃可能会导致两个系统不同步。

## 6、Open Issues

> The storage handler class name is currently saved to the metastore via table property storage_handler; this should probably be a first-class attribute on MStorageDescriptor instead

目前，存储处理器类名称通过表属性 storage_handler 保存到 metastore；这应该是 MStorageDescriptor 上的第一类属性。

> Names of helper classes such as input format and output format are saved into the metastore based on what the storage handler returns during CREATE TABLE; it would be better to leave these null in case they are changed later as part of a handler upgrade

辅助类的名称，如输入格式和输出格式，将根据存储处理器在 `CREATE TABLE` 期间返回的内容保存到 metastore 中；

最好将这些值保留为空，以防它们在稍后的处理程序升级过程中发生更改

> A dummy location is currently set for a non-native table (the same as if it were a native table), even though no Hive files are created for it. We would prefer to store null, but first we need to fix the way data source information is passed to input formats (HIVE-1133)

虽然没有为非原生表创建 Hive 文件，但当前会为非原生表设置一个虚拟位置(就像它是原生表一样)。

我们希望存储空值，但是首先我们需要修正将数据源信息传递到输入格式的方式

> Storage handlers are currently set only at the table level. We may want to allow them to be specified per partition, including support for a table spanning different storage handlers.

存储处理器目前仅在表级别设置。我们可能希望允许在每个分区上指定它们，包括支持跨不同存储处理器的表。

> FileSinkOperator should probably be refactored, since non-native tables aren't accessed as files, meaning a lot of the logic is irrelevant for them

可能应该重构 FileSinkOperator，因为非原生表不能作为文件访问，这意味着很多逻辑与它们无关。

> It may be a good idea to support temporary disablement of metastore hooks as part of manual catalog repair operations

作为手工 catalog 修复操作的一部分，支持临时禁用 metastore 钩子可能是个好主意。

> The CREATE TABLE grammar isn't quite as strict as the one given above; some changes are needed in order to prevent STORED BY and row_format both being specified at once

`CREATE TABLE` 语法不像上面给出的那么严格；为了防止 `STORED BY` 和 `row_format` 同时被指定，需要进行一些更改。

> CREATE TABLE AS SELECT is currently prohibited for creating a non-native table. It should be possible to support this, although it may not make sense for all storage handlers. For example, for HBase, it won't make sense until the storage handler is capable of automatically filling in column mappings.

目前，创建一个非原生表，禁止使用 `CREATE TABLE AS SELECT` 。应该可以支持这一点，尽管这可能对所有存储处理器都没有意义。例如，对于 HBase 来说，只有当存储处理器能够自动填充列映射时，它才有意义。