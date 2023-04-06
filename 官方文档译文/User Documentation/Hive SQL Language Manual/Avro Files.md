# AvroSerDe

[TOC]

## 1、Availability

> Earliest version AvroSerde is available. The AvroSerde is available in Hive 0.9.1 and greater.

AvroSerde 在 Hive 0.9.1 及更高版本中可用。

## 2、Overview – Working with Avro from Hive

> The AvroSerde allows users to read or write [Avro data](http://avro.apache.org/) as Hive tables. The AvroSerde's bullet points:

AvroSerde 允许用户以 Hive 表的形式读写 Avro 数据。

AvroSerde 的要点如下:

> Infers the schema of the Hive table from the Avro schema. Starting in [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-6806), the Avro schema can be inferred from the Hive table schema.

> Reads all Avro files within a table against a specified schema, taking advantage of Avro's backwards compatibility abilities

> Supports arbitrarily nested schemas.

- 根据 Avro 模式推断 Hive 表的模式。从 Hive 0.14 开始，Avro 模式可以从 Hive 表模式推断出来。

- 根据指定的模式，读取表中的所有 Avro 文件，利用 Avro 的向后兼容能力。

- 支持任意嵌套的模式。

> Translates all Avro data types into equivalent Hive types. Most types map exactly, but some Avro types don't exist in Hive and are automatically converted by the AvroSerde.

> Understands compressed Avro files.

> Transparently converts the Avro idiom of handling nullable types as Union[T, null] into just T and returns null when appropriate.

- 将所有 Avro 数据类型转换为等价的 Hive 类型。大多数类型都是精确映射的，但有些 Avro 类型在 Hive 中不存在，由 AvroSerde 自动转换。

- 理解压缩的 Avro 文件。

- 将 Avro 将可空类型 Union[T, null] 转换为 T，并在适当的时候返回 null。

> Writes any Hive table to Avro files.

> Has worked reliably against our most convoluted Avro schemas in our ETL process.

> Starting in [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-7446), columns can be added to an Avro backed Hive table using the [Alter Table](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-Add/ReplaceColumns) statement.

- 将任意的 Hive 表写到 Avro 文件。

- 在我们的 ETL 进程中，可以可靠地针对最复杂的 Avro 模式工作。

- 从 Hive 0.14 开始，可以使用 Alter Table 语句将列添加到 Avro 支持的 Hive 表中。

> For general information about SerDes, see [Hive SerDe](https://cwiki.apache.org/confluence/display/Hive/DeveloperGuide#DeveloperGuide-HiveSerDe) in the Developer Guide. Also see [SerDe](https://cwiki.apache.org/confluence/display/Hive/SerDe) for details about input and output processing.

有关 SerDes 的一般信息，请参见 Developer Guide 中的 Hive SerDe。有关输入和输出处理的详细信息，请参见 SerDe。

## 3、Requirements

> The AvroSerde has been built and tested against Hive 0.9.1 and later, and uses Avro 1.7.5 as of Hive 0.13 and 0.14.

AvroSerde 已经在 Hive 0.9.1 及更高版本上进行了构建和测试，并使用了 Hive 0.13 和 0.14 版本的 Avro 1.7.5。

Hive Version                | Avro Version
---|:---
Hive 0.9.1	                | Avro 1.5.3
Hive 0.10, 0.11, and 0.12	| Avro 1.7.1
Hive 0.13 and 0.14	        | Avro 1.7.5


## 4、Avro to Hive type conversion

> While most Avro types convert directly to equivalent Hive types, there are some which do not exist in Hive and are converted to reasonable equivalents. Also, the AvroSerde special cases unions of null and another type, as described below:

虽然大多数 Avro 类型直接转换为等价的 Hive 类型，但有一些类型在 Hive 中不存在，而是被转换为合理的等价类型。

另外，AvroSerde 的特殊情况是 null 和另一种类型的合并，如下所述:

AvroType   |  Becomes Hive Type  | Note
---|:---|:---
null       |      void           |
boolean    |     boolean         |
int        |       int           |
long       |      bigint         |
float      |       float         |
double     |      double         |
bytes      |      binary         | Bytes are converted to Array[smallint] prior to Hive 0.12.0.【Hive 0.12.0之前，Bytes转换为Array[smallint]】
string     |      string         |
record     |      struct         |
map        |      map            |
list       |      array          |
union      |      union          | Unions of [T, null] transparently convert to nullable T, other types translate directly to Hive's unions of those types. However, unions were introduced in Hive 7 and are not currently able to be used in where/group-by statements. They are essentially look-at-only. Because the AvroSerde transparently converts [T,null], to nullable T, this limitation only applies to unions of multiple types or unions not of a single type and null.【[T, null]的合并透明地转换为可为空的T，其他类型直接转换为Hive的这些类型的合并。然而，合并是在Hive 7中引入的，目前还不能在where/group-by语句中使用。它们基本上都是仅看的。因为AvroSerde透明地将[T,null]转换为可为空的T，所以这个限制只适用于多种类型的合并，或者不适用单一类型和null的合并。】
enum       |      string         | Hive has no concept of enums.【hive没有enums类型】
fixed      |      binary         | Fixeds are converted to Array[smallint] prior to Hive 0.12.0.【Hive 0.12.0之前，fixed转换为Array[smallint]】

## 5、Creating Avro-backed Hive tables

> Avro-backed tables can be created in Hive using AvroSerDe.

可以在 Hive 中使用 AvroSerDe 创建 Avro-backed 表。

### 5.1、All Hive versions

> To create an Avro-backed table, specify the serde as org.apache.hadoop.hive.serde2.avro.AvroSerDe, specify the inputformat as org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat, and the outputformat as org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat. Also provide a location from which the AvroSerde will pull the most current schema for the table. For example:

要创建一个 Avro-backed 表，请指定 serde 为 `org.apache.hadoop.hive.serde2.avro.AvroSerDe`，指定 inputformat 为 `org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat`，指定 outputformat 为 `org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat` 

还要提供一个位置，AvroSerde 将从中提取表的最新模式。例如:

```sql
CREATE TABLE kst
  PARTITIONED BY (ds string)
  ROW FORMAT SERDE
  'org.apache.hadoop.hive.serde2.avro.AvroSerDe'
  STORED AS INPUTFORMAT
  'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat'
  OUTPUTFORMAT
  'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat'
  TBLPROPERTIES (
    'avro.schema.url'='http://schema_provider/kst.avsc');
```

> In this example we're pulling the source-of-truth reader schema from a webserver. Other options for providing the schema are described below.

在本例中，我们从一个 webserver 获取 source-of-truth 阅读模式。下面描述了提供给模式的其他选项。

> Add the Avro files to the database (or create an external table) using standard Hive operations ([http://wiki.apache.org/hadoop/Hive/LanguageManual/DML](http://wiki.apache.org/hadoop/Hive/LanguageManual/DML)).

使用标准的 Hive 操作将 Avro 文件添加到数据库(或创建一个外部表)。

> This table might result in a description as below:

该表可能会产生如下描述:

```sql
hive> describe kst;
OK
string1 string  from deserializer
string2 string  from deserializer
int1    int     from deserializer
boolean1        boolean from deserializer
long1   bigint  from deserializer
float1  float   from deserializer
double1 double  from deserializer
inner_record1   struct<int_in_inner_record1:int,string_in_inner_record1:string> from deserializer
enum1   string  from deserializer
array1  array<string>   from deserializer
map1    map<string,string>      from deserializer
union1  uniontype<float,boolean,string> from deserializer
fixed1  binary  from deserializer
null1   void    from deserializer
unionnullint    int     from deserializer
bytes1  binary  from deserializer
```

> At this point, the Avro-backed table can be worked with in Hive like any other table.

在这一点上，支持 Avro 的表可以像其他表一样在 Hive 中工作。

### 5.2、Hive 0.14 and later versions

> Starting in Hive 0.14, Avro-backed tables can simply be created by using "STORED AS AVRO" in a DDL statement. AvroSerDe takes care of creating the appropriate Avro schema from the Hive table schema, a big win in terms of Avro usability in Hive.

从 Hive 0.14 开始，在 DDL 语句中使用 `STORED AS AVRO` 可以简单地创建支持 AVRO 的表。

AvroSerDe 负责从 Hive 表模式中创建合适的 Avro 模式，这是 Hive 中 Avro 可用性方面的一大胜利。

```sql
CREATE TABLE kst (
    string1 string,
    string2 string,
    int1 int,
    boolean1 boolean,
    long1 bigint,
    float1 float,
    double1 double,
    inner_record1 struct<int_in_inner_record1:int,string_in_inner_record1:string>,
    enum1 string,
    array1 array<string>,
    map1 map<string,string>,
    union1 uniontype<float,boolean,string>,
    fixed1 binary,
    null1 void,
    unionnullint int,
    bytes1 binary)
  PARTITIONED BY (ds string)
  STORED AS AVRO;
```

> This table might result in a description as below:

该表可能会产生如下描述:

```sql
hive> describe kst;
OK
string1 string  from deserializer
string2 string  from deserializer
int1    int     from deserializer
boolean1        boolean from deserializer
long1   bigint  from deserializer
float1  float   from deserializer
double1 double  from deserializer
inner_record1   struct<int_in_inner_record1:int,string_in_inner_record1:string> from deserializer
enum1   string  from deserializer
array1  array<string>   from deserializer
map1    map<string,string>      from deserializer
union1  uniontype<float,boolean,string> from deserializer
fixed1  binary  from deserializer
null1   void    from deserializer
unionnullint    int     from deserializer
bytes1  binary  from deserializer
```

## 6、Writing tables to Avro files

> The AvroSerde can serialize any Hive table to Avro files. This makes it effectively an any-Hive-type to Avro converter. In order to write a table to an Avro file, you must first create an appropriate Avro schema (except in Hive 0.14.0 and later, as described below). Create as select type statements are not currently supported.

AvroSerde 可以将任何 Hive 表序列化为 Avro 文件。

为了向 Avro 文件写入一个表，你必须首先创建一个合适的 Avro 模式(除了 Hive 0.14.0 和之后的版本，如下所述)。目前不支持 `Create as select type` 语句。

> Types translate as detailed in the table above. For types that do not translate directly, there are a few items to keep in mind:

类型的转换如上表中详细描述的那样。对于不能直接转换的类型，需要记住以下几点:

> Types that may be null must be defined as a union of that type and Null within Avro. A null in a field that is not so defined will result in an exception during the save. No changes need be made to the Hive schema to support this, as all fields in Hive can be null.

- 可能为 null 的类型必须在 Avro 中定义为该类型和 Null 的并集。如果字段中的空值不是这样定义的，将在保存期间导致异常。不需要修改 Hive 模式来支持这个功能，因为 Hive 中的所有字段都可以是空的。

> Avro Bytes type should be defined in Hive as lists of tiny ints. The AvroSerde will convert these to Bytes during the saving process.

- Avro Bytes 类型应该在 Hive 中定义为 tiny int 的列表。AvroSerde 将在保存过程中将这些转换为 Bytes。

> Avro Fixed type should be defined in Hive as lists of tiny ints. The AvroSerde will convert these to Fixed during the saving process.

- Avro Fixed 类型应该在 Hive 中定义为 tiny int 的列表。AvroSerde 将在保存过程中将这些转换为 Fixed。

> Avro Enum type should be defined in Hive as strings, since Hive doesn't have a concept of enums. Ensure that only valid enum values are present in the table – trying to save a non-defined enum will result in an exception.

- Avro Enum 类型应该在 Hive 中定义为字符串，因为 Hive 中没有 Enum 的概念。确保表中只有有效的 enum 值 - 试图保存未定义的 enum 将导致异常。

> Hive is very forgiving about types: it will attempt to store whatever value matches the provided column in the equivalent column position in the new table. No matching is done on column names, for instance. Therefore, it is incumbent on the query writer to make sure the target column types are correct. If they are not, Avro may accept the type or it may throw an exception; this is dependent on the particular combination of types.

Hive 对于类型是非常宽容的：它会尝试在新表的相同列位置存储与提供的列相匹配的任何值。例如，没有对列名进行匹配。

因此，查询编写器有责任确保目标列类型是正确的。如果不是，Avro 可以接受该类型，或者抛出异常；这取决于特定的类型组合。

### 6.1、Example

> Consider the following Hive table, which covers all types of Hive data types, making it a good example:

考虑下面的 Hive 表，它涵盖了所有的 Hive 数据类型：

```sql
CREATE TABLE test_serializer(string1 STRING,
                             int1 INT,
                             tinyint1 TINYINT,
                             smallint1 SMALLINT,
                             bigint1 BIGINT,
                             boolean1 BOOLEAN,
                             float1 FLOAT,
                             double1 DOUBLE,
                             list1 ARRAY<STRING>,
                             map1 MAP<STRING,INT>,
                             struct1 STRUCT<sint:INT,sboolean:BOOLEAN,sstring:STRING>,
                             union1 uniontype<FLOAT, BOOLEAN, STRING>,
                             enum1 STRING,
                             nullableint INT,
                             bytes1 BINARY,
                             fixed1 BINARY)
 ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' COLLECTION ITEMS TERMINATED BY ':' MAP KEYS TERMINATED BY '#' LINES TERMINATED BY '\n'
 STORED AS TEXTFILE;
```

> If the table were backed by a csv file such as:

如果该表支持 csv 文件，例如:

|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|-|
| why hello there | 42 | 3 | 100 | 1412341 | true | 42.43 | 85.23423424 | alpha:beta:gamma | Earth#42:Control#86:Bob#31 | 17:true:Abe Linkedin | 0:3.141459 | BLUE | 72 | ^A^B^C | ^A^B^C
another record | 98 | 4 | 101 | 9999999 | false | 99.89 | 0.00000009 | beta | Earth#101 | 1134:false:wazzup | 1:true | RED | NULL | ^D^E^F^G | ^D^E^F
third record | 45 | 5 | 102 | 999999999 | true | 89.99 | 0.00000000000009 | alpha:gamma| Earth#237:Bob#723 | 102:false:BNL | 2:Time to go home | GREEN | NULL | ^H | ^G^H^I

> then you could write it out to Avro as described below.

然后你可以像下面描述的那样把它写到 Avro。

### 6.2、All Hive versions

> To save this table as an Avro file, create an equivalent Avro schema (the namespace and actual name of the record are not important):

为了将这个表保存为一个 Avro 文件，创建一个等价的 Avro 模式(记录的命名空间和实际名称并不重要):

	{
	  "namespace": "com.linkedin.haivvreo",
	  "name": "test_serializer",
	  "type": "record",
	  "fields": [
	    { "name":"string1", "type":"string" },
	    { "name":"int1", "type":"int" },
	    { "name":"tinyint1", "type":"int" },
	    { "name":"smallint1", "type":"int" },
	    { "name":"bigint1", "type":"long" },
	    { "name":"boolean1", "type":"boolean" },
	    { "name":"float1", "type":"float" },
	    { "name":"double1", "type":"double" },
	    { "name":"list1", "type":{"type":"array", "items":"string"} },
	    { "name":"map1", "type":{"type":"map", "values":"int"} },
	    { "name":"struct1", "type":{"type":"record", "name":"struct1_name", "fields": [
	          { "name":"sInt", "type":"int" }, { "name":"sBoolean", "type":"boolean" }, { "name":"sString", "type":"string" } ] } },
	    { "name":"union1", "type":["float", "boolean", "string"] },
	    { "name":"enum1", "type":{"type":"enum", "name":"enum1_values", "symbols":["BLUE","RED", "GREEN"]} },
	    { "name":"nullableint", "type":["int", "null"] },
	    { "name":"bytes1", "type":"bytes" },
	    { "name":"fixed1", "type":{"type":"fixed", "name":"threebytes", "size":3} }
	  ] }

> Then you can write it out to Avro with:

然后你可以将它写入到 Avro

```sql
CREATE TABLE as_avro
  ROW FORMAT SERDE
  'org.apache.hadoop.hive.serde2.avro.AvroSerDe'
  STORED as INPUTFORMAT
  'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat'
  OUTPUTFORMAT
  'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat'
  TBLPROPERTIES (
    'avro.schema.url'='file:///path/to/the/schema/test_serializer.avsc');
  
INSERT OVERWRITE TABLE as_avro SELECT * FROM test_serializer;
```

### 6.3、Hive 0.14 and later

> In Hive versions 0.14 and later, you do not need to create the Avro schema manually. The procedure shown above to save a table as an Avro file reduces to just a DDL statement followed by an insert into the table.

在 Hive 0.14 及以后的版本中，不需要手动创建 Avro 模式。上面所示的将表保存为 Avro 文件的过程简化为一个 DDL 语句，然后插入到表中。

```sql
CREATE TABLE as_avro(string1 STRING,
                     int1 INT,
                     tinyint1 TINYINT,
                     smallint1 SMALLINT,
                     bigint1 BIGINT,
                     boolean1 BOOLEAN,
                     float1 FLOAT,
                     double1 DOUBLE,
                     list1 ARRAY<STRING>,
                     map1 MAP<STRING,INT>,
                     struct1 STRUCT<sint:INT,sboolean:BOOLEAN,sstring:STRING>,
                     union1 uniontype<FLOAT, BOOLEAN, STRING>,
                     enum1 STRING,
                     nullableint INT,
                     bytes1 BINARY,
                     fixed1 BINARY)
STORED AS AVRO;
INSERT OVERWRITE TABLE as_avro SELECT * FROM test_serializer;
```

### 6.4、Avro file extension

> The files that are written by the Hive job are valid Avro files, however, MapReduce doesn't add the standard .avro extension. If you copy these files out, you'll likely want to rename them with .avro.

Hive job 写的文件是有效的 Avro 文件，但是，MapReduce没有添加标准的 `.avro` 扩展名。如果你把这些文件复制出来，你可能需要用 `.avro` 重命名它们。

## 7、Specifying the Avro schema for a table

> There are three ways to provide the reader schema for an Avro table, all of which involve parameters to the serde. As the schema evolves, you can update these values by updating the parameters in the table.

有三种方法可以为 Avro 表提供读取器模式，所有这些方法都涉及到 serde 参数。随着模式的发展，可以通过更新表中的参数来更新这些值。

### 7.1、Use avro.schema.url

> Specifies a URL to access the schema from. For http schemas, this works for testing and small-scale clusters, but as the schema will be accessed at least once from each task in the job, this can quickly turn the job into a DDOS attack against the URL provider (a web server, for instance). Use caution when using this parameter for anything other than testing.

指定一个 URL，来访问模式。对于 http 模式，这适用于测试和小规模集群，但由于 job 中的每个任务至少要访问模式一次，这可能会迅速将 job 转变为针对 URL 提供者(例如web服务器)的 DDOS 攻击。当使用该参数进行测试以外的任何事情时，请谨慎使用。

> The schema can also point to a location on HDFS, for instance: hdfs://your-nn:9000/path/to/avsc/file. The AvroSerde will then read the file from HDFS, which should provide resiliency against many reads at once. Note that the serde will read this file from every mapper, so it's a good idea to turn the replication of the schema file to a high value to provide good locality for the readers. The schema file itself should be relatively small, so this does not add a significant amount of overhead to the process.

这个模式也可以指向 HDFS 上的一个位置，例如：`hdfs://your-nn:9000/path/to/avsc/file`。

然后，AvroSerde 将从 HDFS 读取文件，这将提供一次读取多个文件的弹性。

请注意，serde 将从每个 mapper 读取该文件，因此最好将模式文件的副本数设为一个高值，以便为读者提供良好的局域性。

模式文件本身应该相对较小，因此这不会给流程增加大量的开销。

### 7.2、Use schema.literal and embed the schema in the create statement

> You can embed the schema directly into the create statement. This works if the schema doesn't have any single quotes (or they are appropriately escaped), as Hive uses this to define the parameter value. For instance:

可以将模式直接嵌入到 create 语句中。

如果模式中没有单引号(或者它们被适当地转义了)，这个方法就可以工作，因为 Hive 使用它来定义参数值。

例如:

```sql
CREATE TABLE embedded
  COMMENT "just drop the schema right into the HQL"
  ROW FORMAT SERDE
  'org.apache.hadoop.hive.serde2.avro.AvroSerDe'
  STORED AS INPUTFORMAT
  'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat'
  OUTPUTFORMAT
  'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat'
  TBLPROPERTIES (
    'avro.schema.literal'='{
      "namespace": "com.howdy",
      "name": "some_schema",
      "type": "record",
      "fields": [ { "name":"string1","type":"string"}]
    }');
```

> Note that the value is enclosed in single quotes and just pasted into the create statement.

注意，该值用单引号括起来，并粘贴到 create 语句中。

### 7.3、Use avro.schema.literal and pass the schema into the script

> Hive can do simple variable substitution and you can pass the schema embedded in a variable to the script. Note that to do this, the schema must be completely escaped (carriage returns converted to \n, tabs to \t, quotes escaped, etc). An example:

Hive 可以做简单的变量替换，你可以将嵌入变量中的模式传递给脚本。

注意，要做到这一点，模式必须完全转义(回车转换成\n，制表符转换成\t，引号转义，等等)。

```sql
set hiveconf:schema;
DROP TABLE example;
CREATE TABLE example
  ROW FORMAT SERDE
  'org.apache.hadoop.hive.serde2.avro.AvroSerDe'
  STORED AS INPUTFORMAT
  'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat'
  OUTPUTFORMAT
  'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat'
  TBLPROPERTIES (
    'avro.schema.literal'='${hiveconf:schema}');
```

> To execute this script file, assuming $SCHEMA has been defined to be the escaped schema value:

为了执行这个脚本文件，假设 $SCHEMA 已经被定义为转义的模式值:

	hive --hiveconf schema="${SCHEMA}" -f your_script_file.sql

> Note that $SCHEMA is interpolated into the quotes to correctly handle spaces within the schema.

注意，$SCHEMA 被插入到引号中，以正确处理模式中的空格。

### 7.4、Use none to ignore either avro.schema.literal or avro.schema.url

> Hive does not provide an easy way to unset or remove a property. If you wish to switch from using URL or schema to the other, set the to-be-ignored value to none and the AvroSerde will treat it as if it were not set.

Hive没有提供一种简单的方法来取消设置或删除属性。

如果希望从使用 URL 或模式切换到另一种方式，则将要忽略的值设置为 none, AvroSerde 会将其视为未设置。

## 8、HBase Integration

> Hive 0.14.0 onward supports storing and querying Avro objects in HBase columns by making them visible as structs to Hive. This allows Hive to perform ad hoc analysis of HBase data which can be deeply structured. Prior to 0.14.0, the HBase Hive integration only supported querying primitive data types in columns. See [Avro Data Stored in HBase Columns](https://cwiki.apache.org/confluence/display/Hive/HBaseIntegration#HBaseIntegration-AvroDataStoredinHBaseColumns) for details.

Hive 0.14.0 后续版本支持在 HBase 列中存储和查询 Avro 对象，将其作为结构体呈现给 Hive。

这使得 Hive 可以对 HBase 数据进行 ad hoc 分析，从而可以进行深度结构化。

在 0.14.0 之前，HBase Hive integration 只支持在列中查询基本数据类型。

## 9、If something goes wrong

> Hive tends to swallow exceptions from the AvroSerde that occur before job submission. To force Hive to be more verbose, it can be started with `*hive --hiveconf hive.root.logger=INFO,console*`, which will spit orders of magnitude more information to the console and will likely include any information the AvroSerde is trying to get you about what went wrong. If the AvroSerde encounters an error during MapReduce, the stack trace will be provided in the failed task log, which can be examined from the JobTracker's web interface. The AvroSerde only emits the AvroSerdeException; look for these. Please include these in any bug reports. The most common is expected to be exceptions while attempting to serializing an incompatible type from what Avro is expecting.

Hive倾向于在 job 提交之前接受来自 AvroSerde 的异常。

为了使 Hive 更详细，可以用 `*hive --hiveconf hive.root.logger=INFO,console*`，这将吐出更多数量级的信息到控制台，并可能包括任何 AvroSerde 试图让你知道哪里出了问题的信息。

如果 AvroSerde 在 MapReduce 过程中遇到错误，堆栈跟踪将在失败的任务日志中提供，可以从 JobTracker 的 web 界面检查。

AvroSerde 只会触发 AvroSerdeException；寻找这些。请在任何错误报告中包括这些。最常见的是当试图从 Avro 所期望的序列化不兼容类型时出现异常。

## 10、FAQ

> Why do I get error-error-error-error-error-error-error and a message to check avro.schema.literal and avro.schema.url when describing a table or running a query against a table?

为什么我得到 error-error-error-error-error-error-error -error-error-error 和一条消息来检查 avro.schema.literal 和 avro.schema.url，当描述一个表或对一个表运行查询时？

> The AvroSerde returns this message when it has trouble finding or parsing the schema provided by either the avro.schema.literal or avro.avro.schema.url value. It is unable to be more specific because Hive expects all calls to the serde config methods to be successful, meaning we are unable to return an actual exception. By signaling an error via this message, the table is left in a good state and the incorrect value can be corrected with a call to `alter table T set TBLPROPERTIES`.

当 AvroSerde 在查找或解析 `avro.schema.literal` 或 `avro.avro.schema.url` 提供的模式时，它会返回此消息。

不能说得更具体，因为 Hive 希望所有对 serde 配置方法的调用都能成功，这意味着我们无法返回实际的异常。

通过此消息发出错误信号，表将保持良好状态，并且可以调用 `alter table T set TBLPROPERTIES` 来纠正不正确的值。