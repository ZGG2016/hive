# Hive Developer Guide

[TOC]

## 1、Code Organization and a Brief Architecture

### 1.1、Introduction

> Hive has 3 main components:

Hive 有 3 个主要组件：

> Serializers/Deserializers (trunk/serde) - This component has the framework libraries that allow users to develop serializers and deserializers for their own data formats. This component also contains some builtin serialization/deserialization families.

- 1.Serializers/Deserializers (trunk/serde)

	该组件拥有框架库，允许用户为自己的数据格式开发序列化器和反序列化器。

	这个组件还包含一些内置的序列化/反序列化系列。

> MetaStore (trunk/metastore) - This component implements the metadata server, which is used to hold all the information about the tables and partitions that are in the warehouse.

- 2.MetaStore (trunk/ MetaStore)

	该组件实现了元数据服务，用于保存仓库中关于表和分区的所有信息。

> Query Processor (trunk/ql) - This component implements the processing framework for converting SQL to a graph of map/reduce jobs and the execution time framework to run those jobs in the order of dependencies.

- 3.Query Processor (trunk/ql)

	该组件实现了将 SQL 转换为 map/reduce jobs 图的处理框架，以及按照依赖关系顺序运行这些作业的执行时间框架。

> Apart from these major components, Hive also contains a number of other components. These are as follows:

除了这些主要的组件，Hive 还包含许多其他的组件。这些内容如下:

> Command Line Interface (trunk/cli) - This component has all the java code used by the Hive command line interface.

- 1.Command Line Interface (trunk/cli)

该组件包含 Hive 命令行接口使用的所有 java 代码。

> Hive Server (trunk/service) - This component implements all the APIs that can be used by other clients (such as JDBC drivers) to talk to Hive.

- 2.Hive Server (trunk/service)

这个组件实现了其他客户端(如 JDBC 驱动程序)与 Hive 通信的所有 APIs。

> Common (trunk/common) - This component contains common infrastructure needed by the rest of the code. Currently, this contains all the java sources for managing and passing Hive configurations(HiveConf) to all the other code components.

- 3.Common (trunk/common)

该组件包含其余代码所需的公共基础结构。目前，它包含管理和传递 Hive 配置(HiveConf)给所有其他代码组件的所有 java 源。

> Ant Utilities (trunk/ant) - This component contains the implementation of some ant tasks that are used by the build infrastructure.

- 4.Ant Utilities (trunk/ Ant)

这个组件包含了构建基础结构所使用的一些 Ant 任务的实现。

> Scripts (trunk/bin) - This component contains all the scripts provided in the distribution including the scripts to run the Hive CLI (bin/hive).

- 5.Scripts (trunk/bin)

该组件包含发行版中提供的所有脚本，包括运行 Hive CLI 的脚本(bin/Hive)。

> The following top level directories contain helper libraries, packaged configuration files etc..:

下面的顶层目录包含帮助库、打包的配置文件等：

> trunk/conf - This directory contains the packaged hive-default.xml and hive-site.xml.

- trunk/conf - 这个目录包含打包的 hive-default.xml 和 hive-site.xml。

> trunk/data - This directory contains some data sets and configurations used in the Hive tests.

- trunk/data - 这个目录包含一些 Hive tests 中使用的数据集和配置。

> trunk/ivy - This directory contains the Ivy files used by the build infrastructure to manage dependencies on different Hadoop versions.

- trunk/ivy - 这个目录包含构建基础结构使用的 Ivy 文件，管理对不同 Hadoop 版本的依赖关系。

> trunk/lib - This directory contains the run time libraries needed by Hive.

- trunk/lib - 这个目录包含 Hive 需要的运行时库。

> trunk/testlibs - This directory contains the junit.jar used by the JUnit target in the build infrastructure.

- trunk/testlibs - 这个目录包含 JUnit 目标在构建基础结构中使用的 junit.jar。

> trunk/testutils (Deprecated)

- trunk/testutils (Deprecated)

### 1.2、Hive SerDe

> What is a SerDe?

SerDe 是什么?

- SerDe是 序列化器和反序列化器 的简称。
- Hive 使用 SerDe（和 FileFormat）对表行进行读写。
- HDFS files --> InputFileFormat --> `<key, value>` --> Deserializer --> Row object
- Row object --> Serializer --> `<key, value>` --> OutputFileFormat --> HDFS files

> SerDe is a short name for "Serializer and Deserializer."
> Hive uses SerDe (and FileFormat) to read and write table rows.
> HDFS files --> InputFileFormat --> <key, value> --> Deserializer --> Row object
> Row object --> Serializer --> <key, value> --> OutputFileFormat --> HDFS files

> Note that the "key" part is ignored when reading, and is always a constant when writing. Basically row object is stored into the "value".

注意，“键”部分在读取时被忽略，在写入时始终是一个常量。基本上，行对象存储为“值”。

> One principle of Hive is that Hive does not own the HDFS file format. Users should be able to directly read the HDFS files in the Hive tables using other tools or use other tools to directly write to HDFS files that can be loaded into Hive through "CREATE EXTERNAL TABLE" or can be loaded into Hive through "LOAD DATA INPATH," which just move the file into Hive's table directory.

Hive 的一个原理是 Hive 不拥有 HDFS 文件格式。

用户应该能够使用其他工具直接读取 Hive 表里的 HDFS 文件，或使用其他工具直接写入到 HDFS 文件，这个 HDFS 文件可以通过 `CREATE EXTERNAL TABLE` 或 `LOAD DATA INPATH,` 加载到 Hive，只是将文件移动到 Hive 的表目录。

> Note that org.apache.hadoop.hive.serde is the deprecated old SerDe library. Please look at org.apache.hadoop.hive.serde2 for the latest version.

注意，`org.apache.hadoop.hive.serde` 是已弃用的旧的 serde 库。最新版本查看 `org.apache.hadoop.hive.serde2`。

> Hive currently uses these FileFormat classes to read and write HDFS files:

Hive 目前使用这些 FileFormat 类来读写 HDFS 文件：

- TextInputFormat/HiveIgnoreKeyTextOutputFormat：以纯文本文件格式读写数据。

- SequenceFileInputFormat/SequenceFileOutputFormat：以 Hadoop SequenceFile 格式读写数据。

> TextInputFormat/HiveIgnoreKeyTextOutputFormat: These 2 classes read/write data in plain text file format.

> SequenceFileInputFormat/SequenceFileOutputFormat: These 2 classes read/write data in Hadoop SequenceFile format.

> Hive currently uses these SerDe classes to serialize and deserialize data:

Hive 目前使用这些 SerDe 类来序列化和反序列化数据：

> MetadataTypedColumnsetSerDe: This SerDe is used to read/write delimited records like CSV, tab-separated control-A separated records (sorry, quote is not supported yet).

- MetadataTypedColumnsetSerDe：这个 SerDe 用于读写分隔的记录，如 CSV，制表符分隔的 control-A 分隔的记录(还不支持引号)。

> LazySimpleSerDe: This SerDe can be used to read the same data format as MetadataTypedColumnsetSerDe and TCTLSeparatedProtocol, however, it creates Objects in a lazy way which provides better performance. Starting in [Hive 0.14.0](https://issues.apache.org/jira/browse/HIVE-7142) it also supports read/write data with a specified encode charset, for example:

- LazySimpleSerDe：这个 SerDe 可以用于读取与 MetadataTypedColumnsetSerDe 和 TCTLSeparatedProtocol 相同的数据格式，但是，它以一种惰性的方式创建对象，这提供了更好的性能。从 Hive 0.14.0 开始，它还支持用指定的编码字符集读/写数据，

例如:

	ALTER TABLE person SET SERDEPROPERTIES ('serialization.encoding'='GBK');

> LazySimpleSerDe can treat 'T', 't', 'F', 'f', '1', and '0' as extended, legal boolean literals if the configuration property [hive.lazysimple.extended_boolean_literal](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.lazysimple.extended_boolean_literal) is set to true ([Hive 0.14.0](https://issues.apache.org/jira/browse/HIVE-3635) and later). The default is false, which means only 'TRUE' and 'FALSE' are treated as legal boolean literals.

LazySimpleSerDe 可以将 'T'，'T'，'F'，'F'，'1'和 '0' 作为扩展的合法布尔字面量处理，如果配置属性 `hive.lazysimple.extended_boolean_literal` 设置为 true (Hive 0.14.0 及更高版本)。默认值是 false，这意味着只有 'TRUE' 和 'FALSE' 被视为合法的布尔字面量。

> ThriftSerDe: This SerDe is used to read/write Thrift serialized objects. The class file for the Thrift object must be loaded first.

- ThriftSerDe：这个 SerDe 用于读写 Thrift 序列化对象。必须首先加载 Thrift 对象的类文件。

> DynamicSerDe: This SerDe also read/write Thrift serialized objects, but it understands Thrift DDL so the schema of the object can be provided at runtime. Also it supports a lot of different protocols, including TBinaryProtocol, TJSONProtocol, TCTLSeparatedProtocol (which writes data in delimited records).

- DynamicSerDe：这个 SerDe 也读写 Thrift 序列化对象，但是它理解 Thrift DDL，所以可以在运行时提供对象的模式。它还支持许多不同的协议，包括 TBinaryProtocol、TJSONProtocol、TCTLSeparatedProtocol(在带分隔符的记录中写入数据)。

另外：

> For JSON files, [JsonSerDe](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-JSON) was added in Hive 0.12.0. An Amazon SerDe is available at s3://elasticmapreduce/samples/hive-ads/libs/jsonserde.jar for releases prior to 0.12.0.

- 对于 JSON 文件，在 Hive 0.12.0 中添加了 JsonSerDe。对于 0.12.0 之前的版本，Amazon SerDe 在 s3://elasticmapreduce/samples/hive-ads/libs/jsonserde.jar 可用。

> An [Avro SerDe](https://cwiki.apache.org/confluence/display/Hive/AvroSerDe) was added in Hive 0.9.1.  Starting in Hive 0.14.0 its specification is implicit with the STORED AS AVRO clause.

- 在 Hive 0.9.1 中添加了一个 Avro SerDe。从 Hive 0.14.0 开始，它的规范就隐含在 STORED AS AVRO 子句中。

> A SerDe for the [ORC](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+ORC) file format was added in Hive 0.11.0.

- 在 Hive 0.11.0 中添加了 ORC 文件格式的 SerDe。

> A SerDe for [Parquet](https://cwiki.apache.org/confluence/display/Hive/Parquet) was added via plug-in in Hive 0.10 and natively in Hive 0.13.0.

- Parquet 的 SerDe 在 Hive 0.10 中以插件的方式添加，在 Hive 0.13.0 中以原生的方式添加。

> A SerDe for [CSV](https://cwiki.apache.org/confluence/display/Hive/CSV+Serde) was added in Hive 0.14.

- 在 Hive 0.14 中添加了一个用于 CSV 的 SerDe。

> See [SerDe](https://cwiki.apache.org/confluence/display/Hive/SerDe) for detailed information about input and output processing. Also see [Storage Formats](https://cwiki.apache.org/confluence/display/Hive/HCatalog+StorageFormats) in the [HCatalog manual](https://cwiki.apache.org/confluence/display/Hive/HCatalog), including [CTAS Issue with JSON SerDe](https://cwiki.apache.org/confluence/display/Hive/HCatalog+StorageFormats#HCatalogStorageFormats-CTASIssuewithJSONSerDe). For information about how to create a table with a custom or native SerDe, see [Row Format, Storage Format, and SerDe](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-RowFormat,StorageFormat,andSerDe).

有关输入和输出处理的详细信息，请参阅 SerDe。

也可以在 HCatalog manual 中查看 Storage Formats，包括 CTAS Issue with JSON SerDe。

有关如何使用自定义或原生 SerDe 创建表的信息，请参见 Row Format, Storage Format, and SerDe。

#### 1.2.1、How to Write Your Own SerDe

> In most cases, users want to write a Deserializer instead of a SerDe, because users just want to read their own data format instead of writing to it.

- 在大多数情况下，用户希望编写一个 Deserializer 而不是 SerDe，因为用户只希望读取自己的数据格式，而不是对其进行写入。

> For example, the RegexDeserializer will deserialize the data using the configuration parameter 'regex', and possibly a list of column names (see serde2.MetadataTypedColumnsetSerDe). Please see serde2/Deserializer.java for details.

- 例如，RegexDeserializer 将使用配置参数 'regex' 和可能的列名称列表对数据进行反序列化。

> If your SerDe supports DDL (basically, SerDe with parameterized columns and column types), you probably want to implement a Protocol based on DynamicSerDe, instead of writing a SerDe from scratch. The reason is that the framework passes DDL to SerDe through "Thrift DDL" format, and it's non-trivial to write a "Thrift DDL" parser.

- 如果你的 SerDe 支持 DDL(基本上是带有参数化列和列类型的 SerDe)，那么你可能希望实现基于 DynamicSerDe 的协议，而不是从头编写 SerDe。原因是框架通过 “Thrift DDL” 格式将 DDL 传递给 SerDe，而编写一个 “Thrift DDL” 解析器并非易事。

> For examples, see [SerDe - how to add a new SerDe](https://cwiki.apache.org/confluence/display/Hive/DeveloperGuide#DeveloperGuide-SerDe-howtoaddanewSerDe) below.

- 示例查看 SerDe - how to add a new SerDe

> Some important points about SerDe:

关于 SerDe 的几点要点:

- SerDe 定义表模式，而不是 DDL。一些 SerDe 实现使用 DDL 进行配置，但是 SerDe 也可以覆盖 DDL。

- 列类型可以是任意嵌套的数组、映射和结构。

- ObjectInspector 的回调设计允许使用 CASE/IF 或当使用复杂或嵌套类型时进行延迟反序列化。

> SerDe, not the DDL, defines the table schema. Some SerDe implementations use the DDL for configuration, but the SerDe can also override that.

> Column types can be arbitrarily nested arrays, maps, and structures.

> The callback design of ObjectInspector allows lazy deserialization with CASE/IF or when using complex or nested types.

#### 1.2.2、ObjectInspector

> Hive uses ObjectInspector to analyze the internal structure of the row object and also the structure of the individual columns.

Hive 使用 ObjectInspector 来分析行对象的内部结构以及单个列的结构。

> ObjectInspector provides a uniform way to access complex objects that can be stored in multiple formats in the memory, including:

ObjectInspector 提供了一种统一的方式来访问复杂的对象，这些对象可以以多种格式存储在内存中，包括:

- Java 类的实例(Thrift 或原生 Java)

- 一个标准的 Java 对象(我们使用 java.util.List 来表示 Struct 和 Array，并使用 java.util.Map 表示 Map)

- 一个延迟初始化的对象(例如，存储在单个 Java 字符串对象中的字符串字段结构，每个字段具有初始偏移量)

> Instance of a Java class (Thrift or native Java)
> A standard Java object (we use java.util.List to represent Struct and Array, and use java.util.Map to represent Map)
> A lazily-initialized object (for example, a Struct of string fields stored in a single Java string object with starting offset for each field)

> A complex object can be represented by a pair of ObjectInspector and Java Object. The ObjectInspector not only tells us the structure of the Object, but also gives us ways to access the internal fields inside the Object.

复杂对象可以由一对 ObjectInspector 和 Java Object 表示。

ObjectInspector 不仅告诉我们对象的结构，还提供了访问对象内部字段的方法。

> NOTE: Apache Hive recommends that custom ObjectInspectors created for use with custom SerDes have a no-argument constructor in addition to their normal constructors for serialization purposes. See [HIVE-5380](https://issues.apache.org/jira/browse/HIVE-5380) for more details.

注意：Apache Hive 建议，使用自定义 SerDes 创建的自定义 ObjectInspectors 除了用于序列化的普通构造函数外，还有一个无参数构造函数。

#### 1.2.3、Registration of Native SerDes

> As of [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-5976) a registration mechanism has been introduced for native Hive SerDes.  This allows dynamic binding between a "STORED AS" keyword in place of a triplet of {SerDe, InputFormat, and OutputFormat} specification, in [CreateTable](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTable) statements.

在 Hive 0.14 版本中，原生 Hive SerDes 引入了注册机制。这允许在 CreateTable 语句中动态绑定 “STORED AS” 关键字，而不是 {SerDe，InputFormat 和 OutputFormat} 的三元组规范。

> The following mappings have been added through this registration mechanism:

通过这种注册机制添加了以下映射:

STORED AS AVRO / STORED AS AVROFILE 等价于

	ROW FORMAT SERDE
	  'org.apache.hadoop.hive.serde2.avro.AvroSerDe'
	  STORED AS INPUTFORMAT
	  'org.apache.hadoop.hive.ql.io.avro.AvroContainerInputFormat'
	  OUTPUTFORMAT
	  'org.apache.hadoop.hive.ql.io.avro.AvroContainerOutputFormat'

STORED AS ORC / STORED AS ORCFILE  等价于

	ROW FORMAT SERDE
	  'org.apache.hadoop.hive.ql.io.orc.OrcSerde'
	  STORED AS INPUTFORMAT
	  'org.apache.hadoop.hive.ql.io.orc.OrcInputFormat'
	  OUTPUTFORMAT
	  'org.apache.hadoop.hive.ql.io.orc.OrcOutputFormat'

STORED AS PARQUET / STORED AS PARQUETFILE  等价于

	ROW FORMAT SERDE
	  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe'
	  STORED AS INPUTFORMAT
	  'org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat'
	  OUTPUTFORMAT
	  'org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat'

STORED AS RCFILE  等价于

	STORED AS INPUTFORMAT
	  'org.apache.hadoop.hive.ql.io.RCFileInputFormat'
	  OUTPUTFORMAT
	  'org.apache.hadoop.hive.ql.io.RCFileOutputFormat'

STORED AS SEQUENCEFILE	 等价于

	STORED AS INPUTFORMAT
	  'org.apache.hadoop.mapred.SequenceFileInputFormat'
	  OUTPUTFORMAT
	  'org.apache.hadoop.mapred.SequenceFileOutputFormat'

STORED AS TEXTFILE    等价于	

	STORED AS INPUTFORMAT
	  'org.apache.hadoop.mapred.TextInputFormat'
	  OUTPUTFORMAT
	  'org.apache.hadoop.hive.ql.io.IgnoreKeyTextOutputFormat'

> To add a new native SerDe with STORED AS keyword, follow these steps:

要使用 STORED AS keyword 添加新的原生 SerDe，请遵循以下步骤：

> Create a storage format descriptor class extending from [AbstractStorageFormatDescriptor.java](https://github.com/apache/hive/blob/trunk/ql/src/java/org/apache/hadoop/hive/ql/io/AbstractStorageFormatDescriptor.java) that returns a "stored as" keyword and the names of InputFormat, OutputFormat, and SerDe classes.

- 创建一个存储格式描述符类，继承自 AbstractStorageFormatDescriptor.java，该类返回 "stored as" keyword 以及 InputFormat、OutputFormat 和 SerDe 类的名称。

> Add the name of the storage format descriptor class to the [StorageFormatDescriptor](https://github.com/apache/hive/blob/trunk/ql/src/main/resources/META-INF/services/org.apache.hadoop.hive.ql.io.StorageFormatDescriptor) registration file.

- 将存储格式描述符类的名称添加到 StorageFormatDescriptor 注册文件。

### 1.3、MetaStore

> MetaStore contains metadata regarding tables, partitions and databases. This is used by Query Processor during plan generation.

MetaStore 包含有关表、分区和数据库的元数据。

在计划生成过程中，由 Query Processor  使用。

> Metastore Server - This is the Thrift server (interface defined in metastore/if/hive_metastore.if) that services metadata requests from clients. It delegates most of the requests underlying meta data store and the Hadoop file system which contains data.

- Metastore Server

这是服务于客户端元数据请求的 Thrift 服务(`metastore/if/hive_metastore.if`中定义的接口)。它将大部分请求委托给底层的元数据存储和包含数据的 Hadoop 文件系统。

> Object Store - ObjectStore class handles access to the actual metadata is stored in the SQL store. The current implementation uses JPOX ORM solution which is based of JDA specification. It can be used with any database that is supported by JPOX. New meta stores (file based or xml based) can added by implementing the interface MetaStore. FileStore is a partial implementation of an older version of metastore which may be deprecated soon.

- Object Store

ObjectStore 类处理存储在 SQL 存储中的实际元数据的访问。

当前的实现使用基于 JDA 规范的 JPOX ORM 解决方案。它可以用于 JPOX 支持的任何数据库。通过实现 MetaStore 接口，可以添加新的元存储(基于文件或基于xml)。FileStore 是 metastore 旧版本的部分实现，可能很快就会弃用。

> Metastore Client - There are python, java, php Thrift clients in metastore/src. Java generated client is extended with HiveMetaStoreClient which is used by Query Processor (ql/metadta). This is the main interface to all other Hive components.

- Metastore Client

在 Metastore/src 中有 python、java、php Thrift 客户端。Java 生成的客户端由 Query Processor (ql/metadta)使用的 HiveMetaStoreClient 进行扩展。这是所有其他 Hive 组件的主界面。

### 1.4、Query Processor

> The following are the main components of the Hive Query Processor:

以下是 Hive 查询处理器的主要组件：

> Parse and SemanticAnalysis (ql/parse) - This component contains the code for parsing SQL, converting it into Abstract Syntax Trees, converting the Abstract Syntax Trees into Operator Plans and finally converting the operator plans into a directed graph of tasks which are executed by Driver.java.

- Parse 和 SemanticAnalysis (ql/ Parse)

该组件包含解析 SQL 的代码，将其转换为抽象语法树，将抽象语法树转换为操作符计划，最后将操作符计划转换为由 Driver.java 执行的任务有向图。

> Optimizer (ql/optimizer) - This component contains some simple rule based optimizations like pruning non referenced columns from table scans (column pruning) that the Hive Query Processor does while converting SQL to a series of map/reduce tasks.

- 优化器(ql/ Optimizer)

该组件包含一些简单的基于规则的优化，比如从表扫描中删除未引用的列(列删除)，这是 Hive 查询处理器在将 SQL 转换为一系列 map/reduce 任务时执行的操作。

> Plan Components (ql/plan) - This component contains the classes (which are called descriptors), that are used by the compiler (Parser, SemanticAnalysis and Optimizer) to pass the information to operator trees that is used by the execution code.

- Plan 组件 (ql/ Plan)

该组件包含类(称为描述符)，编译器(解析器、语义分析和优化器)使用这些类将信息传递给执行代码使用的操作符树。

> MetaData Layer (ql/metadata) - This component is used by the query processor to interface with the MetaStore in order to retrieve information about tables, partitions and the columns of the table. This information is used by the compiler to compile SQL to a series of map/reduce tasks.

- MetaData Layer (ql/元数据)

查询处理器使用这个组件和 MetaStore 交互，以便检索有关表、分区和表列的信息。编译器使用此信息将 SQL 编译为一系列 map/reduce 任务。

> Map/Reduce Execution Engine (ql/exec) - This component contains all the query operators and the framework that is used to invoke those operators from within the map/reduces tasks.

- Map/Reduce Execution Engine (ql/exec)

该组件包含所有查询操作符和用于从 Map/Reduce 任务中调用这些操作符的框架。

> Hadoop Record Readers, Input and Output Formatters for Hive (ql/io) - This component contains the record readers and the input, output formatters that Hive registers with a Hadoop Job.

- Hadoop Record Readers, Input and Output Formatters for Hive (ql/io)

这个组件包含了记录读取器，和 Hive 向 Hadoop Job 注册的输入和输出格式化器。

> Sessions (ql/session) - A rudimentary session implementation for Hive.

- session (ql/session)

Hive 的基本会话实现。

> Type interfaces (ql/typeinfo) - This component provides all the type information for table columns that is retrieved from the MetaStore and the SerDes.

- Type interfaces (ql/typeinfo)

该组件为从 MetaStore 和 SerDes 检索的表列提供所有类型信息。

> Hive Function Framework (ql/udf) - Framework and implementation of Hive operators, Functions and Aggregate Functions. This component also contains the interfaces that a user can implement to create user defined functions.

- Hive Function Framework (ql/udf) 

Hive 操作符、函数和聚合函数的框架和实现。该组件还包含用户可以实现的用于创建用户定义函数的接口。

> Tools (ql/tools) - Some simple tools provided by the query processing framework. Currently, this component contains the implementation of the lineage tool that can parse the query and show the source and destination tables of the query.

- Tools (ql/ Tools)

查询处理框架提供的一些简单工具。目前，该组件包含可以解析查询，并显示查询的源表和目标表的沿袭工具的实现。

#### 1.4.1、Compiler

#### 1.4.2、Parser

#### 1.4.3、TypeChecking

#### 1.4.4、Semantic Analysis

#### 1.4.5、Plan generation

#### 1.4.6、Task generation

#### 1.4.7、Execution Engine

#### 1.4.8、Plan

#### 1.4.9、Operators

#### 1.4.10、UDFs and UDAFs

> A helpful overview of the Hive query processor can be found in this [Hive Anatomy slide deck](http://www.slideshare.net/nzhang/hive-anatomy).

Hive 查询处理器可以在 Hive Anatomy slide deck 找到。

## 2、Compiling and Running Hive

> Ant to Maven. As of version [0.13](https://issues.apache.org/jira/browse/HIVE-5107) Hive uses Maven instead of Ant for its build. The following instructions are not up to date.

从 0.13 版本开始，Hive 使用 Maven 代替 Ant 构建。以下说明不是最新的。

> See the [Hive Developer FAQ](https://cwiki.apache.org/confluence/display/Hive/HiveDeveloperFAQ) for updated instructions.

> Hive can be made to compile against different versions of Hadoop.

Hive 可以针对不同版本的 Hadoop 进行编译。

### 2.1、Default Mode

> From the root of the source tree:

从源树的根：

	ant package

> will make Hive compile against Hadoop version 0.19.0. Note that:

将使 Hive 针对 Hadoop 0.19.0 版本进行编译。注意：

> Hive uses Ivy to download the hadoop-0.19.0 distribution. However once downloaded, it's cached and not downloaded multiple times.

Hive 使用 Ivy 下载 hadoop-0.19.0 发行版。然而，一旦下载，它被缓存，不会下载多次。

> This will create a distribution directory in build/dist (relative to the source root) from where one can launch Hive. This distribution should only be used to execute queries against Hadoop branch 0.19. (Hive is not sensitive to minor revisions of Hadoop versions).

这将在 build/dist 中创建一个分发目录(相对于源根目录)，在那里可以启动 Hive。

这个分发版应该只用于执行针对 Hadoop branch 0.19 的查询。(Hive 对 Hadoop 版本的小修改不敏感)。

### 2.2、Advanced Mode

> One can specify a custom distribution directory by using:

可以通过以下方式指定一个自定义的分发目录:

	ant -Dtarget.dir=<my-install-dir> package

> One can specify a version of Hadoop other than 0.19.0 by using (using 0.17.1 as an example):

可以使用如下(以0.17.1为例)指定除 0.19.0 之外的 Hadoop 版本:

	ant -Dhadoop.version=0.17.1 package

> One can also compile against a custom version of the Hadoop tree (only release 0.4 and above). This is also useful if running Ivy is problematic (in disconnected mode for example) - but a Hadoop tree is available. This can be done by specifying the root of the Hadoop source tree to be used, for example:

还可以使用 Hadoop 树的自定义版本(仅0.4及以上版本)进行编译。

如果运行 Ivy 有问题(例如在断开连接的模式下)，这也很有用。但是 Hadoop 树是可用的。这可以通过指定要使用的 Hadoop 源树的根来实现，例如:

	ant -Dhadoop.root=~/src/hadoop-19/build/hadoop-0.19.2-dev -Dhadoop.version=0.19.2-dev

注意:

- Hive 的构建脚本假设 hadoop.root 指向 Hadoop 的分布树，它是通过在 Hadoop 中运行 ant 包创建的。

- hadoop.version 必须与构建 Hadoop 时使用的版本匹配。

> Hive's build script assumes that hadoop.root is pointing to a distribution tree for Hadoop created by running ant package in Hadoop.

> hadoop.version must match the version used in building Hadoop.

> In this particular example - `~/src/hadoop-19` is a checkout of the Hadoop 19 branch that uses 0.19.2-dev as default version and creates a distribution directory in build/hadoop-0.19.2-dev by default.

在这个特定的示例中，`~/src/hadoop-19` 是 Hadoop 19 分支的一个 checkout，该分支使用 0.19.2-dev 作为默认版本，并默认在 build/hadoop-0.19.2-dev 中创建一个分发目录。

> Run Hive from the command line with '$HIVE_HOME/bin/hive', where $HIVE_HOME is typically build/dist under your Hive repository top-level directory.

在命令行中运行 `$HIVE_HOME/bin/hive`，其中 `$HIVE_HOME` 通常是 Hive 存储库顶级目录下的 build/dist 目录。

	$ build/dist/bin/hive

> If Hive fails at runtime, try 'ant very-clean package' to delete the Ivy cache before rebuilding.

如果 Hive 在运行时失败，尝试 `ant very-clean package` 删除 Ivy 缓存，再重新构建。

### 2.3、Running Hive Without a Hadoop Cluster

> From Thejas:

```sh
export HIVE_OPTS='--hiveconf mapred.job.tracker=local --hiveconf fs.default.name=file:///tmp \
    --hiveconf hive.metastore.warehouse.dir=file:///tmp/warehouse \
    --hiveconf javax.jdo.option.ConnectionURL=jdbc:derby:;databaseName=/tmp/metastore_db;create=true'
```

> Then you can run 'build/dist/bin/hive' and it will work against your local file system.

## 3、Unit tests and debugging

见原文：[https://cwiki.apache.org/confluence/display/Hive/DeveloperGuide#DeveloperGuide-Unittestsanddebugging](https://cwiki.apache.org/confluence/display/Hive/DeveloperGuide#DeveloperGuide-Unittestsanddebugging)

### 3.1、Layout of the unit tests

### 3.2、Running unit tests

### 3.3、Adding new unit tests

### 3.4、Debugging Hive Code

> Hive code includes both client-side code (e.g., compiler, semantic analyzer, and optimizer of HiveQL) and server-side code (e.g., operator/task/SerDe implementations). Debugging is different for client-side and server-side code, as described below.

Hive 代码包括客户端代码(例如，HiveQL 的编译器、语义分析器和优化器)和服务器端代码(例如，操作符/任务/SerDe实现)。

客户端代码和服务器端代码的调试是不同的，如下所述。

#### 3.4.1、Debugging Client-Side Code

> The client-side code runs on your local machine so you can easily debug it using Eclipse the same way you debug any regular local Java code. Here are the steps to debug code within a unit test.

客户端代码在本地机器上运行，因此可以轻松地使用 Eclipse 调试它，就像调试任何常规本地 Java 代码一样。下面是在单元测试中调试代码的步骤。

> Make sure that you have run `ant model-jar` in hive/metastore and `ant gen-test` in hive since the last time you ran `ant clean`.

- 确保上次运行 `ant clean` 之后，你已经在 hive/metastore 中运行了 `ant model-jar`，在 hive 中运行了 `ant gen-test`。

> To run all of the unit tests for the CLI:

- 为 CLI 运行所有单元测试:

	- 打开 TestCliDriver.java
	- 单击 Run->Debug Configurations，选择 TestCliDriver，单击 Debug。

> Open up TestCliDriver.java
> Click Run->Debug Configurations, select TestCliDriver, and click Debug.

> To run a single test within TestCliDriver.java:

- 在 TestCliDriver.java 中运行单个测试:

	- 像以前一样开始运行整个 TestCli 套件。

	- 一旦它完成设置并开始执行 JUnit 测试，就停止测试执行。

	- 在 JUnit 窗格中找到所需的测试，

	- 右键单击该测试并选择 Debug。

> Begin running the whole TestCli suite as before.
> Once it finishes the setup and starts executing the JUnit tests, stop the test execution.
> Find the desired test in the JUnit pane,
> Right click on that test and select Debug.

#### 3.4.2、Debugging Server-Side Code

> The server-side code is distributed and runs on the Hadoop cluster, so debugging server-side Hive code is a little bit complicated. In addition to printing to log files using log4j, you can also attach the debugger to a different JVM under unit test (single machine mode). Below are the steps on how to debug on server-side code.

服务器端代码是分布式的，运行在 Hadoop 集群上，所以调试服务器端 Hive 代码有点复杂。

除了使用 log4j 打印日志文件之外，还可以在单元测试(单机模式)下将调试器附加到不同的 JVM 上。

下面是关于如何在服务器端代码上调试的步骤。

> Compile Hive code with javac.debug=on. Under Hive checkout directory:

- 使用 `javac.debug=on` 编译 Hive 代码。在 Hive checkout 目录下:

```sh
> ant -Djavac.debug=on package
```

> If you have already built Hive without javac.debug=on, you can clean the build and then run the above command.

如果已经在没有 `javac.debug=on` 的情况下构建了 Hive，那么你可以清除构建，然后运行上面的命令。

```sh
> ant clean  # not necessary if the first time to compile
> ant -Djavac.debug=on package
```

> Run ant test with additional options to tell the Java VM that is running Hive server-side code to wait for the debugger to attach. First define some convenient macros for debugging. You can put it in your `.bashrc` or `.cshrc`.

- 运行带有附加选项的 ant test，告诉正在运行 Hive 服务器端代码的 Java JVM 等待附加调试器。首先定义一些方便的调试宏。你可以把它放在 `.bashrc` 或 `.cshrc` 中。

```sh
> export HIVE_DEBUG_PORT=8000
> export HIVE_DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,address=${HIVE_DEBUG_PORT},server=y,suspend=y"
```

> In particular HIVE_DEBUG_PORT is the port number that the JVM is listening on and the debugger will attach to. Then run the unit test as follows:

HIVE_DEBUG_PORT 是 JVM 正在监听的端口号，调试器将连接到这个端口号。

然后运行单元测试如下:

```sh
> export HADOOP_OPTS=$HIVE_DEBUG
> ant test -Dtestcase=TestCliDriver -Dqfile=<mytest>.q
```

> The unit test will run until it shows:

单元测试将一直运行，直到它显示:

	[junit] Listening for transport dt_socket at address: 8000

> Now, you can use jdb to attach to port 8000 to debug

- 现在，可以使用 jdb 附加到端口 8000 进行调试

```sh
> jdb -attach 8000
```

> or if you are running Eclipse and the Hive projects are already imported, you can debug with Eclipse. Under Eclipse Run -> Debug Configurations, find "Remote Java Application" at the bottom of the left panel. There should be a MapRedTask configuration already. If there is no such configuration, you can create one with the following property:

或者如果你正在运行 Eclipse 并且已经导入了 Hive 项目，则可以使用 Eclipse 进行调试。在 Eclipse Run -> Debug Configurations 下，在左侧面板底部找到 "Remote Java Application"。应该已经有了一个 MapRedTask 配置。如果没有这样的配置，你可以使用以下属性创建一个:

- Name: any task such as MapRedTask
- Project: the Hive project that you imported.
- Connection Type: Standard (Socket Attach)
- Connection Properties:
	- Host: localhost
	- Port: 8000
		- Then hit the "Debug" button and Eclipse will attach to the JVM listening on port 8000 and continue running till the end. If you define breakpoints in the source code before hitting the "Debug" button, it will stop there. The rest is the same as debugging client-side Hive.

#### 3.4.3、Debugging without Ant (Client and Server Side)

> There is another way of debugging Hive code without going through Ant.
You need to install Hadoop and set the environment variable HADOOP_HOME to that.

还有一种不用 Ant 就可以调试 Hive 代码的方法。

需要安装 Hadoop 并将环境变量 HADOOP_HOME 设置为安装目录。

```sh
> export HADOOP_HOME=<your hadoop home>
```

> Then, start Hive:

然后启动 Hive：

```sh
>  ./build/dist/bin/hive --debug
```

> It will then act similar to the debugging steps outlines in Debugging Hive code. It is faster since there is no need to compile Hive code,
and go through Ant. It can be used to debug both client side and server side Hive.

然后，它的操作将类似于 Debugging Hive code 中概述的调试步骤。它更快，因为不需要编译 Hive 代码，然后通过 Ant。它可以用于调试客户端和服务器端 Hive。

> If you want to debug a particular query, start Hive and perform the steps needed before that query. Then start Hive again in debug to debug that query.

如果你想调试某个特定的查询，启动 Hive 并执行该查询之前所需的步骤。然后在 debug 中再次启动 Hive 来调试该查询。

```sh
>  ./build/dist/bin/hive
>  perform steps before the query
>  ./build/dist/bin/hive --debug
>  run the query
```

> Note that the local file system will be used, so the space on your machine will not be released automatically (unlike debugging via Ant, where the tables created in test are automatically dropped at the end of the test). Make sure to either drop the tables explicitly, or drop the data from /User/hive/warehouse.

注意，将使用本地文件系统，因此机器上的空间不会自动释放(不像通过 Ant 进行的调试，在 test 中创建的表会在测试结束时自动删除)。

确保显式地删除表，或者从 /User/hive/warehouse 删除数据。

## 4、Pluggable interfaces

### 4.1、File Formats

> Please refer to [Hive User Group Meeting August 2009](http://www.slideshare.net/ragho/hive-user-meeting-august-2009-facebook) Page 59-63.

### 4.2、SerDe - how to add a new SerDe

> Please refer to [Hive User Group Meeting August 2009](http://www.slideshare.net/ragho/hive-user-meeting-august-2009-facebook) Page 64-70.

### 4.3、Map-Reduce Scripts

> Please refer to [Hive User Group Meeting August 2009](http://www.slideshare.net/ragho/hive-user-meeting-august-2009-facebook) Page 71-73.

### 4.4、UDFs and UDAFs - how to add new UDFs and UDAFs

> Please refer to [Hive User Group Meeting August 2009](http://www.slideshare.net/ragho/hive-user-meeting-august-2009-facebook) Page 74-87.
