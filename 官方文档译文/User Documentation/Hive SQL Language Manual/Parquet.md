# Parquet

> Version:Parquet is supported by a plugin in Hive 0.10, 0.11, and 0.12 and natively in Hive 0.13 and later.

版本：Parquet 在 Hive 0.10、0.11 和 0.12 中作为一个插件支持，在 Hive 0.13 和更高版本中被本地支持。

## 1、Introduction

> Parquet ([http://parquet.io/](http://parquet.io/)) is an ecosystem wide columnar format for Hadoop. Read [Dremel made simple with Parquet](https://blog.twitter.com/2013/dremel-made-simple-with-parquet) for a good introduction to the format while the Parquet project has an [in-depth description of the format](https://github.com/Parquet/parquet-format) including motivations and diagrams. At the time of this writing Parquet supports the follow engines and data description languages:

Parquet 是一种面向 Hadoop 的生态系统宽柱状格式。阅读 Dremel made simple with Parquet 是一个很好的格式介绍，而 Parquet 项目对格式有深入的描述，包括动机和图表。在撰写本文时，Parquet 支持以下引擎和数据描述语言:

Engines

- Apache Hive
- Apache Drill
- Cloudera Impala
- Apache Crunch
- Apache Pig
- Cascading
- Apache Spark

Data description

- Apache Avro
- Apache Thrift
- Google Protocol Buffers

> The latest information on Parquet engine and data description support, please visit the [Parquet-MR projects feature matrix](https://github.com/Parquet/parquet-mr).

最新的 Parquet engine 和 data description 支持信息见 Parquet-MR projects feature matrix。

> Parquet Motivation

> We created Parquet to make the advantages of compressed, efficient columnar data representation available to any project in the Hadoop ecosystem.

创建 Parquet 是为了让 Hadoop 生态系统中的任何项目都能享受到压缩、高效柱状数据表示的优势。

> Parquet is built from the ground up with complex nested data structures in mind, and uses the record shredding and assembly algorithm described in the Dremel paper. We believe this approach is superior to simple flattening of nested name spaces.

Parquet 是基于复杂的嵌套数据结构从头开始构建的，并使用 Dremel 论文中描述的记录分解和组装算法。我们相信这种方法优于简单的嵌套名称空间扁平化。

> Parquet is built to support very efficient compression and encoding schemes. Multiple projects have demonstrated the performance impact of applying the right compression and encoding scheme to the data. Parquet allows compression schemes to be specified on a per-column level, and is future-proofed to allow adding more encodings as they are invented and implemented.

Parquet 支持非常高效的压缩和编码方案。多个项目已经证明了对数据应用正确的压缩和编码方案对性能的影响。Parquet 允许在每个列级别上指定压缩方案，并且具有未来特性，允许在发明和实现更多编码时添加它们。

> Parquet is built to be used by anyone. The Hadoop ecosystem is rich with data processing frameworks, and we are not interested in playing favorites. We believe that an efficient, well-implemented columnar storage substrate should be useful to all frameworks without the cost of extensive and difficult to set up dependencies.

Parquet 是为任何人使用而建造的。Hadoop 生态系统中有丰富的数据处理框架，我们对偏心不感兴趣。我们相信一个有效的，实现良好的柱状存储基底应该是有用的所有框架，而没有广泛的成本和难以建立依赖。

## 2、Native Parquet Support

### 2.1、Hive 0.10, 0.11, and 0.12

> To use Parquet with Hive 0.10-0.12 you must [download](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22parquet-hive-bundle%22) the Parquet Hive package from the Parquet project. You want the [parquet-hive-bundle jar](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22parquet-hive-bundle%22) in Maven Central.

Hive 0.10-0.12 版本中使用 Parquet 必须下载 Parquet Hive 包。parquet-hive-bundle jar 在 maven 中央库。 

### 2.2、Hive 0.13

> Native Parquet support was added ([HIVE-5783](https://issues.apache.org/jira/browse/HIVE-5783)). Please note that not all Parquet data types are supported in this version (see [Versions and Limitations](https://cwiki.apache.org/confluence/display/Hive/Parquet#Parquet-VersionsandLimitations) below).

添加了原生的 Parquet 支持。

注意：并不是所有的 Parquet 数据类型在这个版本都被支持。

## 3、HiveQL Syntax

> A [CREATE TABLE](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTable) statement can specify the Parquet storage format with syntax that depends on the Hive version.

`CREATE TABLE` 语句可以指定 Parquet 存储格式，语法依赖于 Hive 版本。

### 3.1、Hive 0.10 - 0.12

```sql
CREATE TABLE parquet_test (
 id int,
 str string,
 mp MAP<STRING,STRING>,
 lst ARRAY<STRING>,
 strct STRUCT<A:STRING,B:STRING>) 
PARTITIONED BY (part string)
ROW FORMAT SERDE 'parquet.hive.serde.ParquetHiveSerDe'
 STORED AS
 INPUTFORMAT 'parquet.hive.DeprecatedParquetInputFormat'
 OUTPUTFORMAT 'parquet.hive.DeprecatedParquetOutputFormat';
```

### 3.2、Hive 0.13 and later

```sql
CREATE TABLE parquet_test (
 id int,
 str string,
 mp MAP<STRING,STRING>,
 lst ARRAY<STRING>,
 strct STRUCT<A:STRING,B:STRING>) 
PARTITIONED BY (part string)
STORED AS PARQUET;
```

## 4、Versions and Limitations

### 4.1、Hive 0.13.0

> Support was added for Create Table AS SELECT (CTAS -- HIVE-6375](https://issues.apache.org/jira/browse/HIVE-6375)).

添加对 `Create Table AS SELECT` 的支持。

### 4.2、Hive 0.14.0

> Support was added for timestamp ([HIVE-6394](https://issues.apache.org/jira/browse/HIVE-6394)), decimal ([HIVE-6367](https://issues.apache.org/jira/browse/HIVE-6367)), and char and varchar ([HIVE-7735](https://issues.apache.org/jira/browse/HIVE-7735)) data types. Support was also added for column rename with use of the flag parquet.column.index.access ([HIVE-6938](https://issues.apache.org/jira/browse/HIVE-6938)). Parquet column names were previously case sensitive (query had to use column case that matches exactly what was in the metastore), but became case insensitive ([HIVE-7554](https://issues.apache.org/jira/browse/HIVE-7554)).

支持 timestamp、decimal、char 和 varchar 数据类型。

还添加了对列重命名的支持，使用标志 `parquet.column.index.access`。

Parquet 列名以前是区分大小写的(查询必须使用精确匹配 metastore 中的列大小写)，但后来变得不区分大小写。

### 4.3、Hive 1.1.0

> Support was added for binary data types ([HIVE-7073](https://issues.apache.org/jira/browse/HIVE-7073)).

支持二进制数据类型。

### 4.4、Hive 1.2.0

> Support for remaining Parquet data types was added ([HIVE-6384](https://issues.apache.org/jira/browse/HIVE-6384)).

添加了对剩余 Parquet 数据类型的支持。

### 4.5、Resources

- [Parquet Website](http://parquet.io/)
- [Format specification](https://github.com/Parquet/parquet-format)
- [Feature Matrix](https://github.com/Parquet/parquet-mr)
- [The striping and assembly algorithms from the Dremel paper](https://github.com/Parquet/parquet-mr/wiki/The-striping-and-assembly-algorithms-from-the-Dremel-paper)
- [Dremel paper](http://research.google.com/pubs/pub36632.html)
- [Dremel made simple with Parquet](https://blog.twitter.com/2013/dremel-made-simple-with-parquet)

---------------------------------------------------

```sql
create table parquet_test(
    name string,
    favorite_color string
)STORED AS PARQUET;

hive> load data local inpath '/root/data/users.parquet' into table parquet_test;
Loading data to table default.parquet_test
OK

hive> select * from parquet_test;
OK
Alyssa  NULL
Ben     red
```