# Hive HBase Integration

[TOC]

> Version information. As of Hive 0.9.0 the HBase integration requires at least HBase 0.92, earlier versions of Hive were working with HBase 0.89/0.90

版本信息。**从 Hive 0.9.0 开始，HBase 集成需要 HBase 版本至少是 0.92**，Hive 的早期版本使用 HBase 0.89/0.90。

> Version information. Hive 1.x will remain compatible with HBase 0.98.x and lower versions. Hive 2.x will be compatible with HBase 1.x and higher. (See [HIVE-10990](https://issues.apache.org/jira/browse/HIVE-10990) for details.) Consumers wanting to work with HBase 1.x using Hive 1.x will need to compile Hive 1.x stream code themselves.

版本信息。Hive 1.x 将保持与 HBase 0.98 和更低版本兼容。Hive 2.x 将兼容 HBase 1.x 和更高版本。消费者想要 Hive 1.x 和 HBase 1.x 一起使用将需要编译 Hive 1.x 流代码本身。

## 1、Introduction

> This page documents the Hive/HBase integration support originally introduced in [HIVE-705](https://issues.apache.org/jira/browse/HIVE-705). This feature allows Hive QL statements to access [HBase](http://hadoop.apache.org/hbase) tables for both read (SELECT) and write (INSERT). It is even possible to combine access to HBase tables with native Hive tables via joins and unions.

本页面介绍了 Hive-705 中开始引入的对 Hive/HBase 集成的支持。

该特性**允许 Hive QL 语句对 HBase 表进行读(SELECT)和写(INSERT)访问**。

甚至可以**通过 join 和 union 将对 HBase 表的访问与原生 Hive 表的访问结合起来**。

> A presentation is available from the [HBase HUG10 Meetup](http://wiki.apache.org/hadoop/Hive/Presentations?action=AttachFile&do=get&target=HiveHBase-HUG10.ppt)

> This feature is a work in progress, and suggestions for its improvement are very welcome.

该功能正在进行中。

## 2、Storage Handlers

> Before proceeding, please read [StorageHandlers](https://cwiki.apache.org/confluence/display/Hive/StorageHandlers) for an overview of the generic storage handler framework on which HBase integration depends.

在继续之前，请阅读 StorageHandlers，了解 HBase 集成所依赖的通用存储处理程序框架的概述。

## 3、Usage

> The storage handler is built as an independent module, hive-hbase-handler-x.y.z.jar, which must be available on the Hive client auxpath, along with HBase, Guava and ZooKeeper jars. It also requires the correct configuration property to be set in order to connect to the right HBase master. See the [HBase documentation](http://hadoop.apache.org/hbase/docs/current/api/overview-summary.html#overview_description) for how to set up an HBase cluster.

存储处理器是作为一个独立模块构建的，**`hive-hbase-handler-x.y.z.jar` 和 HBase、Guava 和 ZooKeeper jars 必须在 Hive 客户端 auxpath 上可用**。

它还需要设置正确的配置属性，以便连接到正确的 HBase master。

> Here's an example using CLI from a source build environment, targeting a single-node HBase server. (Note that the jar locations and names have changed in Hive 0.9.0, so for earlier releases, some changes are needed.)

下面是一个在源构建环境中使用 CLI 的示例，目标是单节点 HBase 服务器。

(请注意，在 Hive 0.9.0 中 jar 的位置和名称已经更改，因此对于更早的版本，需要做一些更改。)

	$HIVE_SRC/build/dist/bin/hive --auxpath $HIVE_SRC/build/dist/lib/hive-hbase-handler-0.9.0.jar,$HIVE_SRC/build/dist/lib/hbase-0.92.0.jar,$HIVE_SRC/build/dist/lib/zookeeper-3.3.4.jar,$HIVE_SRC/build/dist/lib/guava-r09.jar --hiveconf hbase.master=hbase.yoyodyne.com:60000

> Here's an example which instead targets a distributed HBase cluster where a quorum of 3 zookeepers is used to elect the HBase master:

下面是一个针对分布式 HBase 集群的例子，使用 3 个 zookeepers quorum 选举 HBase master:

	$HIVE_SRC/build/dist/bin/hive --auxpath $HIVE_SRC/build/dist/lib/hive-hbase-handler-0.9.0.jar,$HIVE_SRC/build/dist/lib/hbase-0.92.0.jar,$HIVE_SRC/build/dist/lib/zookeeper-3.3.4.jar,$HIVE_SRC/build/dist/lib/guava-r09.jar --hiveconf hbase.zookeeper.quorum=zk1.yoyodyne.com,zk2.yoyodyne.com,zk3.yoyodyne.com

> The handler requires Hadoop 0.20 or higher, and has only been tested with dependency versions hadoop-0.20.x, hbase-0.92.0 and zookeeper-3.3.4. If you are not using hbase-0.92.0, you will need to rebuild the handler with the HBase jar matching your version, and change the --auxpath above accordingly. Failure to use matching versions will lead to misleading connection failures such as MasterNotRunningException since the HBase RPC protocol changes often.

该处理器需要 Hadoop 0.20 或更高版本，并且仅使用依赖版本 Hadoop-0.20、hbase-0.92.0 和 zookeeper-3.3.4 进行过测试。

如果没有使用 hbase-0.92.0，你将需要使用与你的版本匹配的 HBase jar 重新构建处理器，并相应地更改上面的 `--auxpath`。

由于 HBase RPC 协议经常发生变化，如果不能使用匹配的版本，可能会导致连接失败，例如 MasterNotRunningException。

> In order to create a new HBase table which is to be managed by Hive, use the STORED BY clause on CREATE TABLE:

为了**创建一个新的 Hive 管理的 HBase 表，在 CREATE TABLE 上使用 STORED BY 子句**：

```sql
CREATE TABLE hbase_table_1(key int, value string) 
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES ("hbase.columns.mapping" = ":key,cf1:val")
TBLPROPERTIES ("hbase.table.name" = "xyz", "hbase.mapred.output.outputtable" = "xyz");
```

> The hbase.columns.mapping property is required and will be explained in the next section. The hbase.table.name property is optional; it controls the name of the table as known by HBase, and allows the Hive table to have a different name. In this example, the table is known as hbase_table_1 within Hive, and as xyz within HBase. If not specified, then the Hive and HBase table names will be identical. The hbase.mapred.output.outputtable property is optional; it's needed if you plan to insert data to the table (the property is used by hbase.mapreduce.TableOutputFormat)

**`hbase.columns.mapping` 属性是必需的**，下一节将对此进行解释。

**`hbase.table.name` 属性是可选的**，它控制着 HBase 中的表名，允许 Hive 表有不同的名称。

在本例中，Hive 中的表被称为 hbase_table_1, HBase 中的表被称为 xyz。如果不指定，Hive 和 HBase 的表名将完全相同。

**`hbase.mapred.output.outputtable` 属性是可选的，如果你计划向表中插入数据，这是必需的**(该属性由 `hbase.mapreduce.TableOutputFormat` 使用)

> After executing the command above, you should be able to see the new (empty) table in the HBase shell:

在执行了上面的命令之后，你应该能够在 HBase shell 中看到新的空表:

```sh
$ hbase shell
HBase Shell; enter 'help<RETURN>' for list of supported commands.
Version: 0.20.3, r902334, Mon Jan 25 13:13:08 PST 2010
hbase(main):001:0> list
xyz                                                                                                           
1 row(s) in 0.0530 seconds
hbase(main):002:0> describe "xyz"
DESCRIPTION                                                             ENABLED                               
 {NAME => 'xyz', FAMILIES => [{NAME => 'cf1', COMPRESSION => 'NONE', VE true                                  
 RSIONS => '3', TTL => '2147483647', BLOCKSIZE => '65536', IN_MEMORY =>                                       
  'false', BLOCKCACHE => 'true'}]}                                                                            
1 row(s) in 0.0220 seconds
hbase(main):003:0> scan "xyz"
ROW                          COLUMN+CELL                                                                      
0 row(s) in 0.0060 seconds
```

> Notice that even though a column name "val" is specified in the mapping, only the column family name "cf1" appears in the DESCRIBE output in the HBase shell. This is because in HBase, only column families (not columns) are known in the table-level metadata; column names within a column family are only present at the per-row level.

注意，**即使在映射中指定了列名 “val”，在 HBase shell 的 DESCRIBE 命令的输出中也只出现列族名称 “cf1”**。

因为在 HBase 中，表级别的元数据中只有列族(而不是列)是已知的，列族中的列名仅出现在每个行级别。

> Here's how to move data from Hive into the HBase table (see [GettingStarted](https://cwiki.apache.org/confluence/display/Hive/GettingStarted) for how to create the example table pokes in Hive first):

下面是如何**将数据从 Hive 移动到 HBase 表中**：

```sql
INSERT OVERWRITE TABLE hbase_table_1 SELECT * FROM pokes WHERE foo=98;
```

> Use HBase shell to verify that the data actually got loaded:

使用 HBase shell 来验证数据是否被实际加载:

```sh
hbase(main):009:0> scan "xyz"
ROW                          COLUMN+CELL                                                                      
 98                          column=cf1:val, timestamp=1267737987733, value=val_98                            
1 row(s) in 0.0110 seconds
And then query it back via Hive:

hive> select * from hbase_table_1;
Total MapReduce jobs = 1
Launching Job 1 out of 1
...
OK
98	val_98
Time taken: 4.582 seconds
```

> Inserting large amounts of data may be slow due to WAL overhead; if you would like to disable this, make sure you have HIVE-1383 (as of Hive 0.6), and then issue this command before the INSERT:

由于 **WAL 开销**，插入的大量数据可能会比较慢，如果你想禁用这个，确保你有 Hive-1383(从Hive 0.6开始)，然后**在 INSERT 之前发出这个命令:**

	set hive.hbase.wal.enabled=false;

> Warning: disabling WAL may lead to data loss if an HBase failure occurs, so only use this if you have some other recovery strategy available.

警告：当 HBase 发生故障时，禁用 WAL 可能会导致数据丢失，**只有在有其他恢复策略可用的情况下才使用此功能**。

> If you want to give Hive access to an existing HBase table, use CREATE EXTERNAL TABLE:

如果你想**让 Hive 访问一个已经存在的 HBase 表，使用 CREATE EXTERNAL table**：

```sql
CREATE EXTERNAL TABLE hbase_table_2(key int, value string) 
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES ("hbase.columns.mapping" = "cf1:val")
TBLPROPERTIES("hbase.table.name" = "some_existing_table", "hbase.mapred.output.outputtable" = "some_existing_table");
```

> Again, hbase.columns.mapping is required (and will be validated against the existing HBase table's column families), whereas hbase.table.name is optional. The hbase.mapred.output.outputtable is optional.

同样，`hbase.columns.mapping` 是必需的(并且会根据已有的 HBase 表的列族进行验证)，而 `hbase.table.name` 是可选的，`hbase.mapred.output.outputtable` 是可选的。

## 4、Column Mapping

> There are two `SERDEPROPERTIES` that control the mapping of HBase columns to Hive:

有两个 `SERDEPROPERTIES`，它控制 HBase 列到 Hive 的映射:

- `hbase.columns.mapping`

- `hbase.table.default.storage.type`：可以是一个字符串值(默认值)或者二进制值，这个选项从 Hive 0.9 开始才可用，而且字符串行为仅在早期版本中是唯一可用的。

> `hbase.columns.mapping`

> `hbase.table.default.storage.type`: Can have a value of either string (the default) or binary, this option is only available as of Hive 0.9 and the string behavior is the only one available in earlier versions

> The column mapping support currently available is somewhat cumbersome and restrictive:

目前可用的列映射支持有些繁琐和限制性：

> for each Hive column, the table creator must specify a corresponding entry in the comma-delimited `hbase.columns.mapping` string (so for a Hive table with n columns, the string should have n entries); whitespace should not be used in between entries since these will be interperted as part of the column name, which is almost certainly not what you want

- 对于每个 Hive 列，表创建者必须**在以逗号分隔的 `hbase.columns.mapping` 字符串中指定对应的项**(所以对于一个有 n 列的 Hive 表，字符串应该有 n 个项)；不应该在各项间使用空格，因为它们将被解释为列名的一部分，这几乎肯定不是你想要的。

> a mapping entry must be either :`key, :timestamp` or of the form `column-family-name:[column-name][#(binary|string)` (the type specification that delimited by # was added in Hive [0.9.0](https://issues.apache.org/jira/browse/HIVE-1634), earlier versions interpreted everything as strings)

- **映射项必须是：`key, :timestamp` 或 `column-family-name:[column-name][#(binary|string)`**(以 # 分隔的类型规范在 Hive 0.9.0 中添加，在早期版本中将所有内容都解释为字符串)

	- 如果没有给出类型规范，则使用 `hbase.table.default.storage.type` 指定的值

	- 有效值的任何前缀都是有效的(例如，用 `#b` 代替 `#binary`)

	- 如果将列指定为 binary，那么对应的 HBase 单元格中的字节应该是 HBase 的 `Bytes` 类产生的形式。

> If no type specification is given the value from `hbase.table.default.storage.type` will be used

> Any prefixes of the valid values are valid too (i.e. `#b` instead of `#binary`)

> If you specify a column as binary the bytes in the corresponding HBase cells are expected to be of the form that HBase's `Bytes` class yields.

> there must be exactly one `:key` mapping (this can be mapped either to a string or struct column–see [Simple Composite Keys](https://cwiki.apache.org/confluence/display/Hive/HBaseIntegration#HBaseIntegration-SimpleCompositeRowKeys) and [Complex Composite Keys](https://cwiki.apache.org/confluence/display/Hive/HBaseIntegration#HBaseIntegration-ComplexCompositeRowKeysandHBaseKeyFactory))

- **必须恰好有一个 `:key` 映射**(可以映射到字符串或结构体)

> (note that before [HIVE-1228](https://issues.apache.org/jira/browse/HIVE-1228) in Hive 0.6, `:key` was not supported, and the first Hive column implicitly mapped to the key; as of Hive 0.6, it is now strongly recommended that you always specify the key explictly; we will drop support for implicit key mapping in the future)

- (注意，在 Hive 0.6 中的 Hive-1228 之前，不支持 `:key`，并且 Hive 的第一列隐式映射到 key；从 Hive 0.6 开始，强烈建议你明确指定 key；我们将在未来放弃对隐式 key 映射的支持)

> if no column-name is given, then the Hive column will map to all columns in the corresponding HBase column family, and the Hive MAP datatype must be used to allow access to these (possibly sparse) columns

- **如果没有给出列名，那么 Hive 列将映射到对应 HBase 列族中的所有列**，并且必须使用 Hive MAP 数据类型来允许访问这些列(可能是稀疏的)

> Since HBase 1.1 ([HBASE-2828](https://issues.apache.org/jira/browse/HIVE-2828)) there is a way to access the HBase timestamp attribute using the special `:timestamp` mapping. It needs to be either bigint or timestamp.

- 从 HBase 1.1 开始，可以**通过特殊的 `:timestamp` 映射来访问 HBase 的时间戳属性**。它必须是 bigint 或 timestamp。

> it is not necessary to reference every HBase column family, but those that are not mapped will be inaccessible via the Hive table; it's possible to map multiple Hive tables to the same HBase table

- 虽然没有必要引用每个 HBase 列族，但是那些没有被映射的列族将无法通过 Hive 表访问；可以将多个 Hive 表映射到同一个 HBase 表。

> The next few sections provide detailed examples of the kinds of column mappings currently possible.

接下来的几节将提供当前各种列映射的详细示例。

### 4.1、Multiple Columns and Families

> Here's an example with three Hive columns and two HBase column families, with two of the Hive columns (value1 and value2) corresponding to one of the column families (a, with HBase column names b and c), and the other Hive column corresponding to a single column (e) in its own column family (d).

这里的例子是三个 Hive 列和两个 HBase 列族，其中有两个 Hive 列(value1和value2)对应于其中一个列族(列族a包含b列和c列)，另外一个的 Hive 列对应于一个列e，其列族是 d。

```sql
CREATE TABLE hbase_table_1(key int, value1 string, value2 int, value3 int) 
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" = ":key,a:b,a:c,d:e"
);
INSERT OVERWRITE TABLE hbase_table_1 SELECT foo, bar, foo+1, foo+2 
FROM pokes WHERE foo=98 OR foo=100;
```

> Here's how this looks in HBase:

HBase 中表是这样的：

```sh
hbase(main):014:0> describe "hbase_table_1"
DESCRIPTION                                                             ENABLED                               
 {NAME => 'hbase_table_1', FAMILIES => [{NAME => 'a', COMPRESSION => 'N true                                  
 ONE', VERSIONS => '3', TTL => '2147483647', BLOCKSIZE => '65536', IN_M                                       
 EMORY => 'false', BLOCKCACHE => 'true'}, {NAME => 'd', COMPRESSION =>                                        
 'NONE', VERSIONS => '3', TTL => '2147483647', BLOCKSIZE => '65536', IN                                       
 _MEMORY => 'false', BLOCKCACHE => 'true'}]}                                                                  
1 row(s) in 0.0170 seconds
hbase(main):015:0> scan "hbase_table_1"
ROW                          COLUMN+CELL                                                                      
 100                         column=a:b, timestamp=1267740457648, value=val_100                               
 100                         column=a:c, timestamp=1267740457648, value=101                                   
 100                         column=d:e, timestamp=1267740457648, value=102                                   
 98                          column=a:b, timestamp=1267740457648, value=val_98                                
 98                          column=a:c, timestamp=1267740457648, value=99                                    
 98                          column=d:e, timestamp=1267740457648, value=100                                   
2 row(s) in 0.0240 seconds
```

> And when queried back into Hive:

Hive 中进行查询：

```sh
hive> select * from hbase_table_1;
Total MapReduce jobs = 1
Launching Job 1 out of 1
...
OK
100	val_100	101	102
98	val_98	99	100
Time taken: 4.054 seconds
```

### 4.2、Hive MAP to HBase Column Family

> Here's how a Hive MAP datatype can be used to access an entire column family. Each row can have a different set of columns, where the column names correspond to the map keys and the column values correspond to the map values.

下面是如何**使用 Hive MAP 数据类型来访问整个列族**。

每一行可以有一组不同的列，其中列名对应于映射键，列值对应于映射值。

```sql
CREATE TABLE hbase_table_1(value map<string,int>, row_key int) 
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" = "cf:,:key"
);
INSERT OVERWRITE TABLE hbase_table_1 SELECT map(bar, foo), foo FROM pokes 
WHERE foo=98 OR foo=100;
```

> (This example also demonstrates using a Hive column other than the first as the HBase row key.)

(这个例子还演示了使用除第一个列之外的 Hive 列作为 HBase 的 row key。)

> Here's how this looks in HBase (with different column names in different rows):

这是在 HBase 的样子(不同的行有不同的列名):

```sh
hbase(main):012:0> scan "hbase_table_1"
ROW                          COLUMN+CELL                                                                      
 100                         column=cf:val_100, timestamp=1267739509194, value=100                            
 98                          column=cf:val_98, timestamp=1267739509194, value=98                              
2 row(s) in 0.0080 seconds
```

> And when queried back into Hive:

Hive 中进行查询：

```sql
hive> select * from hbase_table_1;
Total MapReduce jobs = 1
Launching Job 1 out of 1
...
OK
{"val_100":100}	100
{"val_98":98}	98
Time taken: 3.808 seconds
```

> Note that the key of the MAP must have datatype string, since it is used for naming the HBase column, so the following table definition will fail:

注意，**MAP 的键必须具有 string 数据类型**，因为它是用来命名 HBase 列的，所以下面的表定义将失败:

```sql
CREATE TABLE hbase_table_1(key int, value map<int,int>) 
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" = ":key,cf:"
);
FAILED: Error in metadata: java.lang.RuntimeException: MetaException(message:org.apache.hadoop.hive.serde2.SerDeException org.apache.hadoop.hive.hbase.HBaseSerDe: hbase column family 'cf:' should be mapped to map<string,?> but is mapped to map<int,int>)
```

### 4.3、Hive MAP to HBase Column Prefix

> Also note that starting with [Hive 0.12](https://issues.apache.org/jira/browse/HIVE-3725), wildcards can also be used to retrieve columns. For instance, if you want to retrieve all columns in HBase that start with the prefix "col_prefix", a query like the following should work:

还要注意，从 Hive 0.12 开始，**通配符也可以用来检索列**。

例如，如果你想检索 HBase 中所有以前缀 “col_prefix” 开头的列，下面这样的查询应该可以工作:

```sql
CREATE TABLE hbase_table_1(value map<string,int>, row_key int) 
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" = "cf:col_prefix.*,:key"
);
```

> The same restrictions apply though. That is, the key of the map should be a string as it maps to the HBase column name and the value can be the type of value that you are retrieving. One other restriction is that all the values under the given prefix should be of the same type. That is, all of them should be of type "int" or type "string" and so on.

同样的限制也适用于此。

也就是说，映射的键应该是一个字符串，因为它映射到 HBase 的列名，值可以是你正在检索的值的类型。

另一个限制是给定前缀下的所有值都应该是相同类型的。也就是说，它们都应该是 “int” 类型或 “string” 类型，等等。

#### 4.3.1、Hiding Column Prefixes

> Starting with [Hive 1.3.0](https://issues.apache.org/jira/browse/HIVE-11329), it is possible to hide column prefixes in select query results.  There is the SerDe boolean property `hbase.columns.mapping.prefix.hide` (false by default), which defines if the prefix should be hidden in keys of Hive map:

从 Hive 1.3.0 开始，**可以在 select 查询结果中隐藏列前缀**。

有一个 SerDe 布尔属性 `hbase.columns.mapping.prefix.hide`(默认为false)，定义在 Hive map 的键中是否应该隐藏前缀:

```sql
CREATE TABLE hbase_table_1(tags map<string,int>, row_key string) 
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" = "cf:tag_.*,:key",
"hbase.columns.mapping.prefix.hide" = "true"
);
```

> Then a value of the column "tags" (`select tags from hbase_table_1`) will be:

然后，列 “tags”(`select tags from hbase_table_1`) 的值将是:

	"x" : 1

> instead of:

而不是:

	"tag_x" : 1

### 4.4、Illegal: Hive Primitive to HBase Column Family

> Table definitions such as the following are illegal because a
Hive column mapped to an entire column family must have MAP type:

就像下面的表定义是非法的，因为映射到整个列族的 Hive 列必须具有 MAP 类型

```sql
CREATE TABLE hbase_table_1(key int, value string) 
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" = ":key,cf:"
);
FAILED: Error in metadata: java.lang.RuntimeException: MetaException(message:org.apache.hadoop.hive.serde2.SerDeException org.apache.hadoop.hive.hbase.HBaseSerDe: hbase column family 'cf:' should be mapped to map<string,?> but is mapped to string)
```

### 4.5、Example with Binary Columns

> Relying on the default value of hbase.table.default.storage.type:

依赖 `hbase.table.default.storage.type` 的默认值:

```sql
CREATE TABLE hbase_table_1 (key int, value string, foobar double)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" = ":key#b,cf:val,cf:foo#b"
);
```

> Specifying hbase.table.default.storage.type:

指定 `hbase.table.default.storage.type`

```sql
CREATE TABLE hbase_table_1 (key int, value string, foobar double)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" = ":key,cf:val#s,cf:foo",
"hbase.table.default.storage.type" = "binary"
);
```

### 4.6、Simple Composite Row Keys

> Version information. As of [Hive 0.13.0](https://issues.apache.org/jira/browse/HIVE-2599)

> Hive can read and write delimited composite keys to HBase by mapping the HBase row key to a Hive struct, and using `ROW FORMAT DELIMITED...COLLECTION ITEMS TERMINATED BY`. Example:

Hive 通过**将 HBase 的 row key 映射到 Hive 结构体，并使用 `ROW FORMAT DELIMITED...COLLECTION ITEMS TERMINATED BY`，可以读写已分隔的组合键到 HBase**。

```sql
-- Create a table with a composite row key consisting of two string fields, delimited by '~'
CREATE EXTERNAL TABLE delimited_example(key struct<f1:string, f2:string>, value string) 
ROW FORMAT DELIMITED 
COLLECTION ITEMS TERMINATED BY '~' 
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler' 
WITH SERDEPROPERTIES (
  'hbase.columns.mapping'=':key,f:c1');
```

### 4.7、Complex Composite Row Keys and HBaseKeyFactory

> As of Hive 0.14.0 with [HIVE-6411](https://issues.apache.org/jira/browse/HIVE-6411) (0.13.0 also supports complex composite keys, but using a different interface–see [HIVE-2599](https://issues.apache.org/jira/browse/HIVE-2599) for that interface)

从 Hive 0.14.0 开始(0.13.0 也支持复杂的组合键，但使用不同的接口)

> For more complex use cases, Hive allows users to specify an HBaseKeyFactory which defines the mapping of a key to fields in a Hive struct. This can be configured using the property "hbase.composite.key.factory" in the SERDEPROPERTIES option:

对于更复杂的用例，Hive 允许用户指定一个 **HBaseKeyFactory，它定义了一个 key 到 Hive 结构体中的字段的映射**。

这可以使用 SERDEPROPERTIES 选项中的属性 `hbase.composite.key.factory` 配置:

```sql
-- Parse a row key with 3 fixed width fields each of width 10
-- Example taken from: https://svn.apache.org/repos/asf/hive/trunk/hbase-handler/src/test/queries/positive/hbase_custom_key2.q
CREATE TABLE hbase_ck_4(key struct<col1:string,col2:string,col3:string>, value string)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
    "hbase.table.name" = "hbase_custom2",
    "hbase.mapred.output.outputtable" = "hbase_custom2",
    "hbase.columns.mapping" = ":key,cf:string",
    "hbase.composite.key.factory"="org.apache.hadoop.hive.hbase.SampleHBaseKeyFactory2");
```

> "hbase.composite.key.factory" should be the fully qualified class name of a class implementing [HBaseKeyFactory](http://hive.apache.org/javadocs/r1.2.0/api/org/apache/hadoop/hive/hbase/HBaseKeyFactory.html). See [SampleHBaseKeyFactory2](http://hive.apache.org/javadocs/r1.2.0/api/org/apache/hadoop/hive/hbase/HBaseKeyFactory.html) for a fixed length example in the same package. This class must be on your classpath in order for the above example to work. TODO: place these in an accessible place; they're currently only in test code.

`hbase.composite.key.factory` 应该是实现 HBaseKeyFactory 的类的全限定类名。

请参阅 SampleHBaseKeyFactory2，以获得同一个包中固定长度的示例。

这个类必须在你的类路径中，才能使上面的示例正常工作。

要做的事：把这些东西放在可访问的地方；它们目前仅在测试代码中。

### 4.8、Avro Data Stored in HBase Columns

> As of Hive 0.14.0 with HIVE-6147

> Hive 0.14.0 onward supports storing and querying Avro objects in HBase columns by making them visible as structs to Hive. This allows Hive to perform ad hoc analysis of HBase data which can be deeply structured. Prior to 0.14.0, the HBase Hive integration only supported querying primitive data types in columns.

Hive 0.14.0 后续版本支持在 HBase 列中存储和查询 Avro 对象，将其作为结构体呈现给 Hive。

这**使得 Hive 可以对 HBase 数据进行 ad hoc 分析**，从而可以进行深度结构化。

在 0.14.0 之前，HBase Hive 集成只支持在列中查询基本数据类型。

> An example HiveQL statement where test_col_fam is the column family and test_col is the column name:

以 HiveQL 语句为例，test_col_fam 是列族，test_col 是列名

```sql
CREATE EXTERNAL TABLE test_hbase_avro
ROW FORMAT SERDE 'org.apache.hadoop.hive.hbase.HBaseSerDe'
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
    "hbase.columns.mapping" = ":key,test_col_fam:test_col",
    "test_col_fam.test_col.serialization.type" = "avro",
    "test_col_fam.test_col.avro.schema.url" = "hdfs://testcluster/tmp/schema.avsc")
TBLPROPERTIES (
    "hbase.table.name" = "hbase_avro_table",
    "hbase.mapred.output.outputtable" = "hbase_avro_table",
    "hbase.struct.autogenerate"="true");
```

> The important properties to note are the following three:

需要注意的重要属性有以下三个：

	"test_col_fam.test_col.serialization.type" = "avro"

> This property tells Hive that the given column under the given column family is an Avro column, so Hive needs to deserialize it accordingly.

这个属性告诉 Hive，给定列族下的给定列是一个 Avro 列，因此 Hive 需要相应地对它进行反序列化。

	"test_col_fam.test_col.avro.schema.url" = "hdfs://testcluster/tmp/schema.avsc"

> Using this property you specify where the reader schema is for the column that will be used to deserialize. This can be on HDFS like mentioned here, or provided inline using something like "`test_col_fam.test_col.avro.schema.literal`" property. If you have a custom store where you store this schema, you can write a custom implementation of [AvroSchemaRetriever](https://github.com/apache/hive/blob/master/serde/src/java/org/apache/hadoop/hive/serde2/avro/AvroSchemaRetriever.java) and plug that in using the "`avro.schema.retriever property`" using a property like "`test_col_fam.test_col.avro.schema.retriever`". You would need to ensure that the jar with this custom class is on the Hive classpath. For a usage discussion and links to other resources, see [HIVE-6147](https://issues.apache.org/jira/browse/HIVE-6147?focusedCommentId=15125117&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-15125117).

使用此属性，你可以指定将用于反序列化的列读取器模式的位置。

这可以在 HDFS 上实现，也可以通过 `test_col_fam.test_col.avro.schema.literal` 这样的内联方式实现。

如果你有一个用于存储该模式的自定义存储，那么你可以编写 AvroSchemaRetriever 的自定义实现，并。。。。。

你需要确保带有这个自定义类的 jar 位于 Hive 的类路径上。

	"hbase.struct.autogenerate" = "true"

> Specifying this property lets Hive auto-deduce the columns and types using the schema that was provided. This allows you to avoid manually creating the columns and types for Avro schemas, which can be complicated and deeply nested.

指定此属性可以让 Hive 使用所提供的模式自动推断列和类型。

这允许你避免为 Avro 模式手动创建列和类型，Avro 模式可能是复杂且深度嵌套的。

## 5、Put Timestamps

> Version information. As of Hive [0.9.0](https://issues.apache.org/jira/browse/HIVE-2781)

> If inserting into a HBase table using Hive the HBase default timestamp is added which is usually the current timestamp. This can be overridden on a per-table basis using the `SERDEPROPERTIES` option `hbase.put.timestamp` which must be a valid timestamp or -1 to reenable the default strategy.

使用 Hive 插入 HBase 中的表时，会添加 HBase 的默认时间戳，通常为当前时间戳。

这可以**使用 `SERDEPROPERTIES` 选项 `hbase.put.timestamp`** 在每个表的基础上覆盖。必须是有效的时间戳或-1，以重新启用默认策略。

## 6、Key Uniqueness

> One subtle difference between HBase tables and Hive tables is that HBase tables have a unique key, whereas Hive tables do not. When multiple rows with the same key are inserted into HBase, only one of them is stored (the choice is arbitrary, so do not rely on HBase to pick the right one). This is in contrast to Hive, which is happy to store multiple rows with the same key and different values.

HBase 表和 Hive 表的一个细微区别是，**HBase 表有一个唯一键，而 Hive 表没有**。

当有多个具有相同键的行插入到 HBase 时，只存储其中的一行(选择是任意的，所以不要依赖于 HBase 来选择正确的行)。

这与 Hive 形成了鲜明的对比，Hive 很乐意存储具有相同键和不同值的多行。

> For example, the pokes table contains rows with duplicate keys. If it is copied into another Hive table, the duplicates are preserved:

例如，poke 表包含具有重复键的行。如果复制到其他 Hive 表中，副本将被保留:

```sql
CREATE TABLE pokes2(foo INT, bar STRING);
INSERT OVERWRITE TABLE pokes2 SELECT * FROM pokes;
-- this will return 3
SELECT COUNT(1) FROM POKES WHERE foo=498;
-- this will also return 3
SELECT COUNT(1) FROM pokes2 WHERE foo=498;
```

> But in HBase, the duplicates are silently eliminated:

但是在 HBase 中，重复项会被悄悄地删除:

```sql
CREATE TABLE pokes3(foo INT, bar STRING)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" = ":key,cf:bar"
);
INSERT OVERWRITE TABLE pokes3 SELECT * FROM pokes;
-- this will return 1 instead of 3
SELECT COUNT(1) FROM pokes3 WHERE foo=498;
```

## 7、Overwrite

> Another difference to note between HBase tables and other Hive tables is that when INSERT OVERWRITE is used, existing rows are not deleted from the table. However, existing rows are overwritten if they have keys which match new rows.

HBase 表与其他 Hive 表的另一个区别是，**使用 INSERT OVERWRITE 时，不会删除表中已有的行**。

但是，如果现有行有与新行匹配的键，则会覆盖它们。

## 8、Potential Followups

> There are a number of areas where Hive/HBase integration could definitely use more love:

Hive/HBase 集成在很多领域都需要更多的爱:

- 更灵活的列映射(HIVE-806, HIVE-1245)
- 在没有给出映射规范的情况下，使用默认的列映射
- 过滤下推和索引(参见FilterPushdownDev和IndexDev)
- 公开timestamp属性，可能还支持将其作为分区键处理
- 允许表 `hbase.master` 配置
- 运行profiler并最小化列映射中的每一行开销
- 用户定义的例程，通过HBase客户端API (HIVE-758和HIVE-791)进行查询和数据加载
- 日志是非常嘈杂的，有很多虚假的异常；调查这些问题，要么解决它们的原因，要么压制它们

> more flexible column mapping (HIVE-806, HIVE-1245)
> default column mapping in cases where no mapping spec is given
> filter pushdown and indexing (see [FilterPushdownDev](https://cwiki.apache.org/confluence/display/Hive/FilterPushdownDev) and [IndexDev](https://cwiki.apache.org/confluence/display/Hive/IndexDev))
> expose timestamp attribute, possibly also with support for treating it as a partition key
> allow per-table hbase.master configuration
> run profiler and minimize any per-row overhead in column mapping
> user defined routines for lookups and data loads via HBase client API (HIVE-758 and HIVE-791)
> logging is very noisy, with a lot of spurious exceptions; investigate these and either fix their cause or squelch them

FilterPushdownDev 原文翻译见：[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/FilterPushdownDev.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/FilterPushdownDev.md)

## 9、Build

存储处理程序的代码位于 hive/trunk/hbase-handler 下。

HBase 和 Zookeeper 依赖是通过 ivy 获取的。

> Code for the storage handler is located under hive/trunk/hbase-handler.

> HBase and Zookeeper dependencies are fetched via ivy.

## 10、Tests

> Class-level unit tests are provided under hbase-handler/src/test/org/apache/hadoop/hive/hbase.

类级单元测试在 `hbase-handler/src/test/org/apache/hadoop/hive/hbase` 下提供。

> Positive QL tests are under hbase-handler/src/test/queries. These use a HBase+Zookeeper mini-cluster for hosting the fixture tables in-process, so no free-standing HBase installation is needed in order to run them. To avoid failures due to port conflicts, don't try to run these tests on the same machine where a real HBase master or zookeeper is running.

正向的 QL 测试在 `hbase-handler/src/test/queries` 下。它们使用一个 HBase+Zookeeper 小集群来在进程中托管 fixture 表，所以不需要独立安装 HBase 来运行它们。为了避免端口冲突导致的失败，不要尝试在运行真正的 HBase master 或 zookeeper 的机器上运行这些测试。

> The QL tests can be executed via ant like this:

QL 测试可以像这样通过 ant 执行:

	ant test -Dtestcase=TestHBaseCliDriver -Dqfile=hbase_queries.q

> An Eclipse launch template remains to be defined.

Eclipse 启动模板仍有待定义。

## 11、Links

- 关于如何从 Hive 批量加载数据到 HBase，请参见 HBaseBulkLoad。

- 另一个项目在 HBase 之上增加了类似 sql 的查询语言支持，请参见HBQL(与Hive无关)。

> For information on how to bulk load data from Hive into HBase, see [HBaseBulkLoad](https://cwiki.apache.org/confluence/display/Hive/HBaseBulkLoad).
> For another project which adds SQL-like query language support on top of HBase, see [HBQL](http://www.hbql.com/) (unrelated to Hive).

HBaseBulkLoad 原文翻译：[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/HBaseBulkLoad.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/HBaseBulkLoad.md)

## 12、Acknowledgements

> Primary credit for this feature goes to Samuel Guo, who did most of the development work in the early drafts of the patch

这个功能主要归功于 Samuel Guo，他在这个补丁的早期草稿中做了大部分的开发工作。

## 13、Open Issues (JIRA)

见原文：[https://cwiki.apache.org/confluence/display/Hive/HBaseIntegration#HBaseIntegration-OpenIssues(JIRA)](https://cwiki.apache.org/confluence/display/Hive/HBaseIntegration#HBaseIntegration-OpenIssues(JIRA))