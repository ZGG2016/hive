# DML: Load, Insert, Update, Delete

[TOC]

There are multiple ways to modify data in Hive:

- LOAD

- INSERT

	- into Hive tables from queries

	- into directories from queries

	- into Hive tables from SQL

- UPDATE

- DELETE

- MERGE

EXPORT and IMPORT commands are also available (as of Hive 0.8).

## 1、Loading files into tables

> Hive does not do any transformation while loading data into tables. Load operations are currently pure copy/move operations that move datafiles into locations corresponding to Hive tables.

Hive 在加载数据到表时不做任何转换。加载操作目前是纯粹的复制/移动操作，将数据文件移动到对应的 Hive 表的位置。

### 1.1、Syntax

```sql
LOAD DATA [LOCAL] INPATH 'filepath' [OVERWRITE] INTO TABLE tablename [PARTITION (partcol1=val1, partcol2=val2 ...)]
 
LOAD DATA [LOCAL] INPATH 'filepath' [OVERWRITE] INTO TABLE tablename [PARTITION (partcol1=val1, partcol2=val2 ...)] [INPUTFORMAT 'inputformat' SERDE 'serde'] (3.0 or later)
```

### 1.2、Synopsis

> Load operations prior to Hive 3.0 are pure copy/move operations that move datafiles into locations corresponding to Hive tables.

在 Hive 3.0 之前，Load 操作是纯粹的复制/移动操作，将数据文件移动到 Hive 表对应的位置。

> filepath can be:

文件路径 filepath 可以是:

- 一个相对路径，如 project/data1

- 一个绝对路径，如 /user/hive/project/data1 

- 带有 scheme 和(可选)authority 的完整 URI，如 hdfs://namenode:9000/user/hive/project/data1

> a relative path, such as project/data1
> an absolute path, such as /user/hive/project/data1
> a full URI with scheme and (optionally) an authority, such as hdfs://namenode:9000/user/hive/project/data1

> The target being loaded to can be a table or a partition. If the table is partitioned, then one must specify a specific partition of the table by specifying values for all of the partitioning columns.

- 被加载到的目标可以是一个表或一个分区。如果表是分区的，则必须，为所有分区列指定值，来指定表的特定分区。

> filepath can refer to a file (in which case Hive will move the file into the table) or it can be a directory (in which case Hive will move all the files within that directory into the table). In either case, filepath addresses a set of files.

- 文件路径 filepath 可以指向一个文件(在这种情况下，Hive 会将文件移动到表中)，也可以指向一个目录(在这种情况下，Hive 会将该目录下的所有文件移动到表中)。在这两种情况下，filepath 都指向一组文件。

> If the keyword LOCAL is specified, then:

- 如果指定了关键字 LOCAL，则:

	- load 命令将在本地文件系统中查找文件路径。如果指定了相对路径，它将相对于用户当前的工作目录进行解释。用户也可以为本地文件指定一个完整的 URI：file:///user/hive/project/data1

	- load 命令将尝试将 filepath 下的所有文件复制到目标文件系统。通过查看表的 location 属性来推断目标文件系统。复制的数据文件将被移动到表中。

	- 注意:如果你对 HiveServer2 实例运行这个命令，那么本地路径指的是 HiveServer2 实例上的路径。HiveServer2 必须具有访问该文件的适当权限。

> the load command will look for filepath in the local file system. If a relative path is specified, it will be interpreted relative to the user's current working directory. The user can specify a full URI for local files as well - for example: file:///user/hive/project/data1

> the load command will try to copy all the files addressed by filepath to the target filesystem. The target file system is inferred by looking at the location attribute of the table. The copied data files will then be moved to the table.

> Note: If you run this command against a HiveServer2 instance then the local path refers to a path on the HiveServer2 instance. HiveServer2 must have the proper permissions to access that file.

> If the keyword LOCAL is not specified, then Hive will either use the full URI of filepath, if one is specified, or will apply the following rules:

- 如果没有指定关键字 LOCAL，那么 Hive 将使用 filepath 的完整 URI(如果指定了的话)，或者应用以下规则:

	- 如果没有指定 scheme 或 authority， Hive 将使用 hadoop 配置变量 `fs.default.name` 中的 scheme 和 authority 来指定 Namenode URI。

	- 如果路径不是绝对的，Hive 会将路径解释为，相对于 `/user/<username>` 的路径。

	- Hive 将 filepath 下的文件移动到表(或分区)中。

> If scheme or authority are not specified, Hive will use the scheme and authority from the hadoop configuration variable fs.default.name that specifies the Namenode URI.

> If the path is not absolute, then Hive will interpret it relative to /user/<username>

> Hive will move the files addressed by filepath into the table (or partition)

> If the OVERWRITE keyword is used then the contents of the target table (or partition) will be deleted and replaced by the files referred to by filepath; otherwise the files referred by filepath will be added to the table.

- 如果使用了 OVERWRITE 关键字，那么目标表(或分区)的内容将被删除，并被 filepath 引用的文件所取代；否则，filepath 引用的文件将被添加到表中。

> Additional load operations are supported by Hive 3.0 onwards as Hive internally rewrites the load into an INSERT AS SELECT.

Hive 3.0 以后的版本还支持额外的 load 操作，因为 Hive 内部会将 load 改写为 `INSERT as SELECT`。

> If table has partitions, however, the load command does not have them, the load would be converted into INSERT AS SELECT and assume that the last set of columns are partition columns. It will throw an error if the file does not conform to the expected schema.

- 如果表有分区，然而，load 命令没有分区，那么 load 将被转换为 `INSERT as SELECT`，并假定最后一组列是分区列。如果文件不符合预期的 schema，它将抛出一个错误。

> If table is bucketed then the following rules apply:

- 如果表被分桶，则适用以下规则:

	- 严格模式:作为 `INSERT as SELECT` 作业启动。

	- 在非严格模式:如果文件名称符合命名约定(如果文件属于桶0,它应该命名为`000000 _0` 或 `000000 _0_copy_1`，或如果它属于桶2，名称应该像`000002 _0`或`000002 _0_copy_3`，等等)，那么这将是一个纯粹的复制/移动操作，否则作为 `INSERT as SELECT` 作业启动。

> In strict mode : launches an INSERT AS SELECT job.
> In non-strict mode : if the file names conform to the naming convention (if the file belongs to bucket 0, it should be named 000000_0 or 000000_0_copy_1, or if it belongs to bucket 2 the names should be like 000002_0 or 000002_0_copy_3, etc.) then it will be a pure copy/move operation, else it will launch an INSERT AS SELECT job.

> filepath can contain subdirectories, provided each file conforms to the schema.

- 如果每个文件都符合 schema，filepath 可以包含子目录。

> inputformat can be any Hive input format such as text, ORC, etc.

- inputformat 可以是任何 Hive 的输入格式，比如 text，ORC 等。

> serde can be the associated Hive SERDE.

- serde 可以是关联的 Hive serde。

> Both inputformat and serde are case sensitive.

- inputformat 和 serde 都区分大小写。

> Example of such a schema:

```sql
CREATE TABLE tab1 (col1 int, col2 int) PARTITIONED BY (col3 int) STORED AS ORC;

LOAD DATA LOCAL INPATH 'filepath' INTO TABLE tab1;
```

> Here, partition information is missing which would otherwise give an error, however, if the file(s) located at filepath conform to the table schema such that each row ends with partition column(s) then the load will rewrite into an INSERT AS SELECT job.

在这里，分区信息丢失了，否则就会产生错误，但是，如果位于 filepath 的文件符合表 schema，每一行都以分区列结束，那么加载将作为 `INSERT AS SELECT` 作业。

> The uncompressed data should look like this:

未压缩的数据应该是这样：

	(1,2,3), (2,3,4), (4,5,3) etc.

### 1.3、Notes

> filepath cannot contain subdirectories (except for Hive 3.0 or later, as described above).

- filepath 不能包含子目录(如上所述，Hive 3.0 及以上版本除外)。

> If the keyword LOCAL is not given, filepath must refer to files within the same filesystem as the table's (or partition's) location.

- 如果没有给出关键字 LOCAL，则 filepath 必须引用与表(或分区)位置相同的文件系统中的文件。

> Hive does some minimal checks to make sure that the files being loaded match the target table. Currently it checks that if the table is stored in sequencefile format, the files being loaded are also sequencefiles, and vice versa.

- Hive 会做一些最小的检查，以确保正在加载的文件与目标表匹配。目前，它会检查表是否以 sequencefile 格式存储，所加载的文件是否也是 sequencefile，反之亦然。

> A bug that prevented loading a file when its name includes the "+" character is fixed in release 0.13.0 ([HIVE-6048](https://issues.apache.org/jira/browse/HIVE-6048)).

- 在 0.13.0 版中修复了一个在文件名中包含 “+” 字符时无法加载文件的 bug。

> Please read [CompressedStorage](https://cwiki.apache.org/confluence/display/Hive/CompressedStorage) if your datafile is compressed.

- 如果你的数据文件被压缩，请阅读 CompressedStorage。

## 2、Inserting data into Hive Tables from queries

> Query Results can be inserted into tables by using the insert clause.

可以使用 insert 子句将查询结果插入表中。

### 2.1、Syntax

```sql
-- Standard syntax:
INSERT OVERWRITE TABLE tablename1 [PARTITION (partcol1=val1, partcol2=val2 ...) [IF NOT EXISTS]] select_statement1 FROM from_statement;
INSERT INTO TABLE tablename1 [PARTITION (partcol1=val1, partcol2=val2 ...)] select_statement1 FROM from_statement;
 
-- Hive extension (multiple inserts):
FROM from_statement
INSERT OVERWRITE TABLE tablename1 [PARTITION (partcol1=val1, partcol2=val2 ...) [IF NOT EXISTS]] select_statement1
[INSERT OVERWRITE TABLE tablename2 [PARTITION ... [IF NOT EXISTS]] select_statement2]
[INSERT INTO TABLE tablename2 [PARTITION ...] select_statement2] ...;

FROM from_statement
INSERT INTO TABLE tablename1 [PARTITION (partcol1=val1, partcol2=val2 ...)] select_statement1
[INSERT INTO TABLE tablename2 [PARTITION ...] select_statement2]
[INSERT OVERWRITE TABLE tablename2 [PARTITION ... [IF NOT EXISTS]] select_statement2] ...;
 
-- Hive extension (dynamic partition inserts):
INSERT OVERWRITE TABLE tablename PARTITION (partcol1[=val1], partcol2[=val2] ...) select_statement FROM from_statement;
INSERT INTO TABLE tablename PARTITION (partcol1[=val1], partcol2[=val2] ...) select_statement FROM from_statement;
```

### 2.2、Synopsis

> INSERT OVERWRITE will overwrite any existing data in the table or partition

- INSERT OVERWRITE 将覆盖表或分区中的任何现有数据

	- 除非为分区提供 IF NOT EXISTS(从Hive 0.9.0开始)。
	
	- 从 Hive 2.3.0 开始，如果表有 `TBLPROPERTIES("auto.purge"="true")`，当对表执行 INSERT OVERWRITE 查询时，表之前的数据不会被移到垃圾箱中。此功能仅适用于受管表，并当 "auto.purge " 属性未设置或设置为 false，是关闭的。

> unless IF NOT EXISTS is provided for a partition (as of [Hive 0.9.0](https://issues.apache.org/jira/browse/HIVE-2612)).

> As of Hive 2.3.0 ([HIVE-15880](https://issues.apache.org/jira/browse/HIVE-15880)), if the table has [TBLPROPERTIES](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-listTableProperties) ("auto.purge"="true") the previous data of the table is not moved to Trash when INSERT OVERWRITE query is run against the table. This functionality is applicable only for managed tables (see [managed tables](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-ManagedandExternalTables)) and is turned off when "auto.purge" property is unset or set to false.

> INSERT INTO will append to the table or partition, keeping the existing data intact. (Note: INSERT INTO syntax is only available starting in version 0.8.)

- INSERT INTO 将数据追加到表或分区上，保持现有数据不变。(注意:INSERT INTO 语法只在 0.8 版开始使用。)

> As of [Hive 0.13.0](https://issues.apache.org/jira/browse/HIVE-6406), a table can be made immutable by creating it with [TBLPROPERTIES ("immutable"="true")](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTable). The default is "immutable"="false".

在 Hive 0.13.0 版本中，可以**通过使用 `TBLPROPERTIES("immutable"="true")`创建一个不可变表**。默认值是 "immutable"="false"。

> INSERT INTO behavior into an immutable table is disallowed if any data is already present, although INSERT INTO still works if the immutable table is empty. The behavior of INSERT OVERWRITE is not affected by the "immutable" table property.

如果不可变表中已经存在任何数据，则不允许 INSERT INTO 行为。如果不可变表为空，INSERT INTO 仍然可以工作。NSERT OVERWRITE 的行为不受“不可变”表属性的影响。

> An immutable table is protected against accidental updates due to a script loading data into it being run multiple times by mistake. The first insert into an immutable table succeeds and successive inserts fail, resulting in only one set of data in the table, instead of silently succeeding with multiple copies of the data in the table. 

不可变表受到保护，防止由于错误地多次运行向其加载数据的脚本而导致意外更新。第一次成功插入到不可变表后，后续插入会失败，导致表中只有一组数据，而不是静默地成功插入表中数据的多个副本。

> Inserts can be done to a table or a partition. If the table is partitioned, then one must specify a specific partition of the table by specifying values for all of the partitioning columns. If [hive.typecheck.on.insert](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.typecheck.on.insert) is set to true, these values are validated, converted and normalized to conform to their column types ([Hive 0.12.0]() onward). 

可以对表或分区进行插入。

如果表是被分区的，那么必须通过为所有分区列指定值来指定表的特定分区。

如果 `hive.typecheck.on.insert` 设置为 true，这些值将被验证、转换和规范化，以符合它们的列类型。

> Multiple insert clauses (also known as Multi Table Insert) can be specified in the same query.

- 可以在同一个查询中，指定多个 insert 子句(也称为多表插入)。

> The output of each of the select statements is written to the chosen table (or partition). Currently the OVERWRITE keyword is mandatory and implies that the contents of the chosen table or partition are replaced with the output of corresponding select statement.

- 每个 select 语句的输出都被写入到所选择的表(或分区)中。目前，OVERWRITE 关键字是必须的，它意味着选择的表或分区的内容将被相应的 select 语句的输出所替换。

> The output format and serialization class is determined by the table's metadata (as specified via DDL commands on the table).

- 输出格式和序列化类由表的元数据决定(通过表上的DDL命令指定)。

> As of [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-5317), if a table has an OutputFormat that implements AcidOutputFormat and the system is configured to use a transaction](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions) manager that implements ACID, then INSERT OVERWRITE will be disabled for that table.  This is to avoid users unintentionally overwriting transaction history.  The same functionality can be achieved by using [TRUNCATE TABLE](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-TruncateTable) (for non-partitioned tables) or [DROP PARTITION](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-DropPartitions) followed by INSERT INTO.

- 在 Hive 0.14 中，如果一个表有一个实现了 AcidOutputFormat 的 OutputFormat，并且为使用一个实现了 ACID 的事务管理器配置了系统，那么该表的 INSERT OVERWRITE 将被禁用。这是为了避免用户无意中覆盖事务历史记录。通过在 INSERT INTO 后使用 TRUNCATE TABLE(对于非分区表)或 DROP PARTITION 可以实现相同的功能。

> As of [Hive 1.1.0](https://issues.apache.org/jira/browse/HIVE-9353) the TABLE keyword is optional.

- 从 Hive 1.1.0 开始，TABLE 关键字是可选的。

> As of [Hive 1.2.0](https://issues.apache.org/jira/browse/HIVE-9481) each INSERT INTO T can take a column list like INSERT INTO T (z, x, c1).  See Description of HIVE-9481 for examples.

- 在 Hive 1.2.0 版本中，每个 INSERT INTO T 都可以接受一个列列表，比如 `INSERT INTO T (z, x, c1)`。详细描述见：[https://issues.apache.org/jira/browse/HIVE-9481](https://issues.apache.org/jira/browse/HIVE-9481)

### 2.3、Notes

> Multi Table Inserts minimize the number of data scans required. Hive can insert data into multiple tables by scanning the input data just once (and applying different query operators) to the input data.

- 多表插入可以最小化所需的数据扫描次数。Hive 可以通过扫描一次输入数据(并应用不同的查询操作符)将数据插入到多个表中。

> Starting with [Hive 0.13.0](https://issues.apache.org/jira/browse/HIVE-1180), the select statement can include one or more common table expressions (CTEs) as shown in the [SELECT syntax](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Select#LanguageManualSelect-SelectSyntax). For an example, see [Common Table Expression](https://cwiki.apache.org/confluence/display/Hive/Common+Table+Expression#CommonTableExpression-CTEinViews,CTAS,andInsertStatements).

- 从 Hive 0.13.0 开始，select 语句可以包含一个或多个 cte (common table expression)，如 select 语法所示。

```sql
create table s1 like src;
with q1 as ( select key, value from src where key = '5')
from q1
insert overwrite table s1
select *;
```

### 2.4、Dynamic Partition Inserts

> Version information:This information reflects the situation in Hive 0.12; dynamic partition inserts were added in Hive 0.6.

版本信息：这个信息反映了 Hive 0.12 中的情况；Hive 0.6 中添加了动态分区插入。

> In the dynamic partition inserts, users can give partial partition specifications, which means just specifying the list of partition column names in the PARTITION clause. The column values are optional. If a partition column value is given, we call this a static partition, otherwise it is a dynamic partition. Each dynamic partition column has a corresponding input column from the select statement. This means that the dynamic partition creation is determined by the value of the input column. The dynamic partition columns must be specified last among the columns in the SELECT statement and in the same order in which they appear in the PARTITION() clause. As of Hive 3.0.0 ([HIVE-19083](https://issues.apache.org/jira/browse/HIVE-19083)) there is no need to specify dynamic partition columns. Hive will automatically generate partition specification if it is not specified.

在动态分区插入中，用户可以给出部分分区规范，这意味着**只需在 PARTITION 子句中指定分区列名列表**。

列值是可选的。**如果给出了分区列的值，我们称之为静态分区，否则就是动态分区**。

**每个动态分区列都有一个来自 SELECT 语句的对应输入列**。这意味着动态分区的创建是由输入列的值决定的。

**动态分区列必须在 SELECT 语句的列中最后指定，并且与它们在 PARTITION() 子句中出现的顺序相同**。

从 Hive 3.0.0 开始，不需要指定动态分区列。**如果没有指定分区规格，Hive会自动生成分区规格**。

> Dynamic partition inserts are disabled by default prior to Hive 0.9.0 and enabled by default in Hive 0.9.0 and later. These are the relevant configuration properties for dynamic partition inserts:

在 Hive 0.9.0 之前，默认是禁用动态分区插入的，**在 Hive 0.9.0 及以后的版本中，默认是启用的**。

以下是与动态分区插入相关的配置属性:

Configuration Property                    |  Default  | note
---|:---|:---
hive.exec.dynamic.partition | true | Needs to be set to true to enable dynamic partition inserts 【需要设置为true，来启用动态分区插入】
hive.exec.dynamic.partition.mode  |  strict  | In strict mode, the user must specify at least one static partition in case the user accidentally overwrites all partitions, in nonstrict mode all partitions are allowed to be dynamic【在严格模式下，用户必须执行至少一个静态分区，以防用户不小心覆盖了所有分区，在非严格模式下，所有的分区都允许是动态的。】
hive.exec.max.dynamic.partitions.pernode  |  100   |  Maximum number of dynamic partitions allowed to be created in each mapper/reducer node 【每个mapper/reducer节点允许创建的最大动态分区数】
hive.exec.max.dynamic.partitions  |  1000  |  Maximum number of dynamic partitions allowed to be created in total 【总共允许创建的最大动态分区数】
hive.exec.max.created.files | 100000  | Maximum number of HDFS files created by all mappers/reducers in a MapReduce job 【在一个MapReduce作业中所有mapper/reducers创建的HDFS文件的最大数量】
hive.error.on.empty.partition  |  false  |  Whether to throw an exception if dynamic partition insert generates empty results 【是否在动态分区插入生成空结果时抛出异常】

#### 2.4.1、Example

```sql
FROM page_view_stg pvs
INSERT OVERWRITE TABLE page_view PARTITION(dt='2008-06-08', country)
       SELECT pvs.viewTime, pvs.userid, pvs.page_url, pvs.referrer_url, null, null, pvs.ip, pvs.cnt
```

> Here the country partition will be dynamically created by the last column from the SELECT clause (i.e. pvs.cnt). Note that the name is not used. In nonstrict mode the dt partition could also be dynamically created.

在这里，country 分区将由 SELECT 子句中的最后一列(即pvs.cnt)动态创建。

注意，没有使用该名称。在非严格模式下，也可以动态创建 dt 分区。

#### 2.4.2、Additional Documentation

- [Design Document](https://cwiki.apache.org/confluence/display/Hive/DynamicPartitions)

	- [Original design doc](https://issues.apache.org/jira/secure/attachment/12437909/dp_design.txt)
	- [HIVE-936](https://issues.apache.org/jira/browse/HIVE-936)

- [Tutorial: Dynamic-Partition Insert](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-Dynamic-PartitionInsert)
- [HCatalog Dynamic Partitioning](https://cwiki.apache.org/confluence/display/Hive/HCatalog+DynamicPartitions)

	- [Usage with Pig](https://cwiki.apache.org/confluence/display/Hive/HCatalog+DynamicPartitions#HCatalogDynamicPartitions-UsagewithPig)
	- [Usage from MapReduce](https://cwiki.apache.org/confluence/display/Hive/HCatalog+DynamicPartitions#HCatalogDynamicPartitions-UsagefromMapReduce)

## 3、Writing data into the filesystem from queries

> Query results can be inserted into filesystem directories by using a slight variation of the syntax above:

查询结果可以通过使用上面的语法的细微变化**插入到文件系统目录中**:

### 3.1、Syntax

```sql
-- Standard syntax:
INSERT OVERWRITE [LOCAL] DIRECTORY directory1
  [ROW FORMAT row_format] [STORED AS file_format] (Note: Only available starting with Hive 0.11.0)
  SELECT ... FROM ...
 
-- Hive extension (multiple inserts):
FROM from_statement
INSERT OVERWRITE [LOCAL] DIRECTORY directory1 select_statement1
[INSERT OVERWRITE [LOCAL] DIRECTORY directory2 select_statement2] ...
 
  
-- row_format
  : DELIMITED [FIELDS TERMINATED BY char [ESCAPED BY char]] [COLLECTION ITEMS TERMINATED BY char]
        [MAP KEYS TERMINATED BY char] [LINES TERMINATED BY char]
        [NULL DEFINED AS char] (Note: Only available starting with Hive 0.13)
```

### 3.2、Synopsis

> Directory can be a full URI. If scheme or authority are not specified, Hive will use the scheme and authority from the hadoop configuration variable fs.default.name that specifies the Namenode URI.

- 目录可以是一个完整的 URI。如果没有指定 scheme 或 authority， Hive 将使用 hadoop 配置变量 `fs.default.name` 中的 scheme 和 authority 来指定 Namenode URI。

> If LOCAL keyword is used, Hive will write data to the directory on the local file system.

- 使用 LOCAL 关键字时，Hive 会向本地文件系统的目录写入数据。

> Data written to the filesystem is serialized as text with columns separated by ^A and rows separated by newlines. If any of the columns are not of primitive type, then those columns are serialized to JSON format.

- 写入文件系统的数据序列化为文本，列之间用 `^A`隔开，行之间用换行符隔开。如果任何列不是基本类型，那么这些列将序列化为 JSON 格式。

### 3.3、Notes

> INSERT OVERWRITE statements to directories, local directories, and tables (or partitions) can all be used together within the same query.

- 对目录、本地目录和表(或分区)的 INSERT OVERWRITE 语句都可以在同一个查询中一起使用。

> INSERT OVERWRITE statements to HDFS filesystem directories are the best way to extract large amounts of data from Hive. Hive can write to HDFS directories in parallel from within a map-reduce job.

- HDFS 文件系统目录的 INSERT OVERWRITE 语句是从 Hive 中提取大量数据的最好方法。Hive 可以在 map-reduce 作业中并行写入 HDFS 目录。

> The directory is, as you would expect, OVERWRITten; in other words, if the specified path exists, it is clobbered and replaced with the output.

- 目录被覆盖了；换句话说，如果指定的路径存在，它将被删除，并替换为输出。

> As of Hive [0.11.0](https://issues.apache.org/jira/browse/HIVE-3682) the separator used can be specified; in earlier versions it was always the ^A character (\001). However, custom separators are only supported for LOCAL writes in Hive versions 0.11.0 to 1.1.0 – this bug is fixed in version 1.2.0 (see [HIVE-5672](https://issues.apache.org/jira/browse/HIVE-5672)).

- 从 Hive 0.11.0 开始，可以指定分隔符；在早期版本中，它总是 `^A` 字符(\001)。然而，在 Hive 0.11.0 到 1.1.0 中，自定义分隔符只支持本地写入：这个 bug 在 1.2.0 中已经修复。

> In [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-5317), inserts into [ACID](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions) compliant tables will deactivate vectorization for the duration of the select and insert.  This will be done automatically.  ACID tables that have data inserted into them can still be queried using vectorization.

- 在 Hive 0.14 中，插入到符合 ACID 的表中会在 select 和 insert 期间停止向量化。这将自动完成。仍然可以使用矢量化查询已插入数据的 ACID 表。

## 4、Inserting values into tables from SQL

> The INSERT...VALUES statement can be used to insert data into tables directly from SQL.

`INSERT...VALUES` 语句可以直接从 SQL 中将数据插入到表中。

Version Information:INSERT...VALUES is available starting in [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-5317).

从 Hive 0.14 开始可用。

### 4.1、Syntax

```sql
-- Standard Syntax:
INSERT INTO TABLE tablename [PARTITION (partcol1[=val1], partcol2[=val2] ...)] VALUES values_row [, values_row ...]
	  
-- Where values_row is:
( value [, value ...] )

-- where a value is either null or any valid SQL literal
```

### 4.2、Synopsis

> Each row listed in the VALUES clause is inserted into table tablename.

- VALUES 子句中列出的每一行都被插入到表 tablename 中。

> Values must be provided for every column in the table. The standard SQL syntax that allows the user to insert values into only some columns is not yet supported. To mimic the standard SQL, nulls can be provided for columns the user does not wish to assign a value to.

- **必须为表中的每一列提供值。目前还不支持用户将值插入一些列的标准 SQL 语法**。为了模拟标准 SQL，可以为用户不希望赋值的列提供空值。

> Dynamic partitioning is supported in the same way as for [INSERT...SELECT](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-DynamicPartitionInserts).

- 动态分区的支持方式与 `INSERT…SELECT` 相同。

> If the table being inserted into supports [ACID](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions) and a transaction manager that supports ACID is in use, this operation will be auto-committed upon successful completion.

- 如果要插入的表支持 ACID，并且正在使用支持 ACID 的事务管理器，则该操作将在成功完成后自动提交。

> Hive does not support literals for complex types (array, map, struct, union), so it is not possible to use them in INSERT INTO...VALUES clauses. This means that the user cannot insert data into a complex datatype column using the INSERT INTO...VALUES clause.

- Hive 不支持复杂类型(array, map, struct, union)的字面量，所以不可能在 `INSERT INTO…VALUES` 子句中使用它们。这意味着**用户不能使用 `INSERT INTO…VALUES` 将数据插入复杂数据类型列**。

### 4.3、Examples

```sql
CREATE TABLE students (name VARCHAR(64), age INT, gpa DECIMAL(3, 2))
  CLUSTERED BY (age) INTO 2 BUCKETS STORED AS ORC;
 
INSERT INTO TABLE students
  VALUES ('fred flintstone', 35, 1.28), ('barney rubble', 32, 2.32);
 
 
CREATE TABLE pageviews (userid VARCHAR(64), link STRING, came_from STRING)
  PARTITIONED BY (datestamp STRING) CLUSTERED BY (userid) INTO 256 BUCKETS STORED AS ORC;
 
INSERT INTO TABLE pageviews PARTITION (datestamp = '2014-09-23')
  VALUES ('jsmith', 'mail.com', 'sports.com'), ('jdoe', 'mail.com', null);
 
-- 动态分区 
INSERT INTO TABLE pageviews PARTITION (datestamp)
  VALUES ('tjohnson', 'sports.com', 'finance.com', '2014-09-23'), ('tlee', 'finance.com', null, '2014-09-21');
  
INSERT INTO TABLE pageviews
  VALUES ('tjohnson', 'sports.com', 'finance.com', '2014-09-23'), ('tlee', 'finance.com', null, '2014-09-21');
```

## 5、Update

> Version Information:UPDATE is available starting in [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-5317).Updates can only be performed on tables that support ACID. See [Hive Transactions](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions) for details.

版本信息：

从 Hive 0.14 开始 UPDATE 可用。

只能在支持 ACID 的表上执行更新。

### 5.1、Syntax

	UPDATE tablename SET column = value [, column = value ...] [WHERE expression]

### 5.2、Synopsis

- 被引用的列必须是正在更新的表的列。

- **赋的值必须是 Hive 在 select 子句中支持的表达式。因此，算术运算符、UDFs、casts、literals 等都是支持的。不支持子查询**。

- 只有匹配 WHERE 子句的行才会被更新。

- 无法更新分区列。

- 无法更新桶列。

- 在 Hive 0.14 中，成功完成此操作后，更改将自动提交。

> The referenced column must be a column of the table being updated.
> The value assigned must be an expression that Hive supports in the select clause.  Thus arithmetic operators, UDFs, casts, literals, etc. are supported.  Subqueries are not supported.
> Only rows that match the WHERE clause will be updated.
> Partitioning columns cannot be updated.
> Bucketing columns cannot be updated.
> In Hive 0.14, upon successful completion of this operation the changes will be auto-committed.

### 5.3、Notes

- 对于更新操作，向量化将被关闭。这是自动的，不需要用户做任何操作。非更新操作不受影响。使用矢量化仍然可以查询更新的表。

- 在 0.14 版本中，**当执行更新时，建议设置`hive.optimize.sort.dynamic.partition=false`**。因为这会产生更有效的执行计划。

> Vectorization will be turned off for update operations.  This is automatic and requires no action on the part of the user.  Non-update operations are not affected.  Updated tables can still be queried using vectorization.
> In version 0.14 it is recommended that you set [hive.optimize.sort.dynamic.partition=false](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.optimize.sort.dynamic.partition) when doing updates, as this produces more efficient execution plans.

## 6、Delete

> Version Information:DELETE is available starting in [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-5317).Deletes can only be performed on tables that support ACID. See [Hive Transactions](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions) for details.

版本信息：

在 Hive 0.14 中 DELETE 可用。

DELETE 操作只能在支持 ACID 的表上执行。

### 6.1、Syntax

	DELETE FROM tablename [WHERE expression]

### 6.2、Synopsis

> Only rows that match the WHERE clause will be deleted.

> In Hive 0.14, upon successful completion of this operation the changes will be auto-committed.

- 只有匹配到 WHERE 子句的行才会被删除。

- 在 Hive 0.14 中，成功完成此操作后，更改将自动提交。

### 6.3、Notes

- 对于删除操作，矢量化将被关闭。这是自动的，不需要用户做任何操作。不影响非删除操作。删除数据的表仍然可以使用向量化来查询。

- 在 0.14 版本中，**当执行删除操作时，建议设置 `hive.optimize.sort.dynamic.partition=false`**。因为这会产生更有效的执行计划。

> Vectorization will be turned off for delete operations.  This is automatic and requires no action on the part of the user.  Non-delete operations are not affected.  Tables with deleted data can still be queried using vectorization.

> In version 0.14 it is recommended that you set [hive.optimize.sort.dynamic.partition=false](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.optimize.sort.dynamic.partition) when doing deletes, as this produces more efficient execution plans.

## 7、Merge

> Version Information：MERGE is available starting in [Hive 2.2](https://issues.apache.org/jira/browse/HIVE-10924).Merge can only be performed on tables that support ACID. See [Hive Transactions](https://cwiki.apache.org/confluence/display/Hive/Hive+Transactions) for details.

版本信息:

**MERGE 从 Hive 2.2 开始可用**。

Merge **只能在支持 [ACID](https://baike.baidu.com/item/ACID/10738) 的表上执行**。

### 7.1、Syntax

```sql
MERGE INTO <target table> AS T USING <source expression/table> AS S
ON <boolean expression1>
WHEN MATCHED [AND <boolean expression2>] THEN UPDATE SET <set clause list>
WHEN MATCHED [AND <boolean expression3>] THEN DELETE
WHEN NOT MATCHED [AND <boolean expression4>] THEN INSERT VALUES<value list>
```

### 7.2、Synopsis

> [Merge](https://en.wikipedia.org/wiki/Merge_(SQL)) allows actions to be performed on a target table based on the results of a join with a source table.

Merge **允许根据与源表的连接结果在目标表上执行操作**。

> In Hive 2.2, upon successful completion of this operation the changes will be auto-committed.

在 Hive 2.2 中，成功完成此操作后，更改将自动提交。

### 7.3、Performance Note

> SQL Standard requires that an error is raised if the ON clause is such that more than 1 row in source matches a row in target.  This check is computationally expensive and may affect the overall runtime of a MERGE statement significantly.  [hive.merge.cardinality.check=false](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.merge.cardinality.check) may be used to disable the check at your own risk.  If the check is disabled, but the statement has such a cross join effect, it may lead to data corruption.

SQL Standard 要求，如果 ON 子句中，原表的多行与目标表的一行匹配，则会引发错误。这种检查在计算上是昂贵的，并且可能会显著影响 MERGE 语句的总体运行时间。

**`hive.merge.cardinality.check=false`可能被用于禁用该检查**，风险自负。如果禁用了检查，但语句具有这样的交叉连接效果，则可能导致数据损坏。

### 7.4、Notes

> 1, 2, or 3 WHEN clauses may be present; at most 1 of each type:  UPDATE/DELETE/INSERT.
> WHEN NOT MATCHED must be the last WHEN clause.
> If both UPDATE and DELETE clauses are present, the first one in the statement must include [AND <boolean expression>].
> Vectorization will be turned off for merge operations.  This is automatic and requires no action on the part of the user.  Non-delete operations are not affected.  Tables with deleted data can still be queried using vectorization.

- 可能存在 1 个、2 个或 3 个 WHEN 子句；每一种类型最多有 1 种：UPDATE/DELETE/INSERT。

- WHEN NOT MATCHED必须是最后一个 WHEN 子句。

- 如果同时存在 UPDATE 和 DELETE 子句，则语句的第一个子句必须包含 `[AND <boolean expression>]` 。

- 对于 merge 操作将关闭向量化。这是自动的，不需要用户做任何操作。不影响非删除操作。删除数据的表仍然可以使用向量化来查询。

### 7.5、Examples

See [here](https://community.hortonworks.com/articles/97113/hive-acid-merge-by-example.html).

如下：

首先启用acid

```sh
set hive.support.concurrency = true;
set hive.enforce.bucketing = true;
set hive.exec.dynamic.partition.mode = nonstrict;
set hive.txn.manager = org.apache.hadoop.hive.ql.lockmgr.DbTxnManager;
set hive.compactor.initiator.on = true;
set hive.compactor.worker.threads = 1;
set hive.auto.convert.join=false;
set hive.merge.cardinality.check=false;
```

然后创建两个表，一个作为 merge 的目标表，一个作为 merge 的源表。

请注意，目标表必须被分桶，设置启用事务，并以 orc 格式存储。

```sql
CREATE TABLE transactions(
 ID int,
 TranValue string,
 last_update_user string)
PARTITIONED BY (tran_date string)
CLUSTERED BY (ID) into 5 buckets 
STORED AS ORC TBLPROPERTIES ('transactional'='true');

CREATE TABLE merge_source(
 ID int,
 TranValue string,
 tran_date string)
STORED AS ORC;
```

然后用一些数据填充目标表和源表。

```sql
INSERT INTO transactions PARTITION (tran_date) VALUES
(1, 'value_01', 'creation', '20170410'),
(2, 'value_02', 'creation', '20170410'),
(3, 'value_03', 'creation', '20170410'),
(4, 'value_04', 'creation', '20170410'),
(5, 'value_05', 'creation', '20170413'),
(6, 'value_06', 'creation', '20170413'),
(7, 'value_07', 'creation', '20170413'),
(8, 'value_08', 'creation', '20170413'),
(9, 'value_09', 'creation', '20170413'),
(10, 'value_10','creation', '20170413');

INSERT INTO merge_source VALUES 
(1, 'value_01', '20170410'),
(4, NULL, '20170410'),
(7, 'value_77777', '20170413'),
(8, NULL, '20170413'),
(8, 'value_08', '20170415'),
(11, 'value_11', '20170415');
```

当我们检查这两个表时，我们期望在 Merge 之后，第 1 行保持不变，第 4 行被删除(暗示了一个业务规则:空值表示删除)，第 7 行被更新，第 11 行被插入。

第 8 行涉及到将行从一个分区移动到另一个分区。当前 Merge 不支持动态更改分区值。这需要在旧分区中删除，并在新分区中插入。在实际的用例中，需要根据这个标准构建源表。

然后创建 merge 语句，如下所示。请注意，并不是所有的 3 个 WHEN 合并语句都需要存在，只有 2 个甚至 1 个 WHEN 语句也可以。我们用不同的 last_update_user 标记数据。

```sql
MERGE INTO transactions AS T 
USING merge_source AS S
ON T.ID = S.ID and T.tran_date = S.tran_date
WHEN MATCHED AND (T.TranValue != S.TranValue AND S.TranValue IS NOT NULL) THEN UPDATE SET TranValue = S.TranValue, last_update_user = 'merge_update'
WHEN MATCHED AND S.TranValue IS NULL THEN DELETE
WHEN NOT MATCHED THEN INSERT VALUES (S.ID, S.TranValue, 'merge_insert', S.tran_date);
```

作为 update 子句的一部分，set value 语句不应该包含目标表装饰器“T.”，否则将得到 SQL 编译错误。

一旦合并完成，重新检查数据就会显示数据已经按照预期进行了合并。

第 1 行没有改变；第 4 行被删除；第 7 行被更新，第 11 行被插入。第 8 行被移到了一个新的分区。

-------------------------------------------------------------------------