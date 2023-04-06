# HBaseBulkLoad

[TOC]

> This page explains how to use Hive to bulk load data into a new (empty) HBase table per [HIVE-1295](https://issues.apache.org/jira/browse/HIVE-1295). (If you're not using a build which contains this functionality yet, you'll need to build from source and make sure this patch and HIVE-1321 are both applied.)

这个页面解释了如何使用 Hive 将数据批量加载到一个新的(空的)HBase表中。

(如果你还没有使用包含这个功能的版本，你需要从源代码开始构建，并确保这个补丁和 HIVE-1321 都被应用。)

## 1、Overview

> Ideally, bulk load from Hive into HBase would be part of [HBaseIntegration](https://cwiki.apache.org/confluence/display/Hive/HBaseIntegration), making it as simple as this:

理想情况下，从 Hive 批量加载到 HBase 将是 HBaseIntegration 的一部分，使其简单如下：

```sql
CREATE TABLE new_hbase_table(rowkey string, x int, y int)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES ("hbase.columns.mapping" = ":key,cf:x,cf:y");
 
SET hive.hbase.bulk=true;
 
INSERT OVERWRITE TABLE new_hbase_table
SELECT rowkey_expression, x, y FROM ...any_hive_query...;
```

> However, things aren't quite as straightforward as that yet. Instead, a procedure involving a series of SQL commands is required. It should still be a lot easier and more flexible than writing your own map/reduce program, and over time we hope to enhance Hive to move closer to the ideal.

然而，事情还没有那么简单。相反，要求包含一系列 SQL 命令的 procedure。

它应该仍然比编写自己的 map/reduce 程序更容易和灵活，随着时间的推移，我们希望增强 Hive，使其更接近理想。

> The procedure is based on [underlying HBase recommendations](http://hbase.apache.org/apidocs/org/apache/hadoop/hbase/mapreduce/package-summary.html#bulk), and involves the following steps:

procedure 基于 HBase 底层建议，主要包括以下步骤:

> Decide how you want the data to look once it has been loaded into HBase.

- 1.决定数据加载到 HBase 后的样子。

> Decide on the number of reducers you're planning to use for parallelizing the sorting and HFile creation. This depends on the size of your data as well as cluster resources available.

- 2.决定用于并行排序和 HFile 创建的 reducers 的数量。这取决于数据的大小以及可用的集群资源。

> Run Hive sampling commands which will create a file containing "splitter" keys which will be used for range-partitioning the data during sort.

- 3.执行 Hive sampling 命令将创建一个包含 “splitter” 键的文件，该键将在排序过程中用于数据的范围分区。

> Prepare a staging location in HDFS where the HFiles will be generated.

- 4.在 HDFS 中准备一个将要生成 HFiles 的 staging 位置。

> Run Hive commands which will execute the sort and generate the HFiles.

- 5.使用 Hive 命令将进行排序并生成 HFiles。

> (Optional: if HBase and Hive are running in different clusters, distcp the generated files from the Hive cluster to the HBase cluster.)

- 6.(可选：如果 HBase 和 Hive 运行在不同的集群中，需要将 Hive 集群生成的文件 distcp 到 HBase 集群中。)

> Run HBase script loadtable.rb to move the files into a new HBase table.

- 7.执行 HBase 脚本 loadtable.rb 将文件移动到一个新的 HBase 表中。

> (Optional: register the HBase table as an external table in Hive so you can access it from there.)

- 8.(可选：在 Hive 中将 HBase 表注册为外部表，这样就可以从外部表访问它。)

> The rest of this page explains each step in greater detail.

本页面的其余部分将更详细地解释每一步。

## 2、Decide on Target HBase Schema

> Currently there are a number of constraints here:

目前有一些限制条件:

- 目标表必须是新的(不能批量加载到现有表中)

- 目标表只能有一个列族

- 目标表不能是稀疏的(每一行都有相同的列集)；这个问题应该很容易解决，要么允许从 Hive 中读取 MAP 值，或者允许从 Hive 中以枢轴形式读取行(每个 HBase cell 一行)。

> The target table must be new (you can't bulk load into an existing table)

> The target table can only have a single column family ([HBASE-1861](http://issues.apache.org/jira/browse/HBASE-1861))

> The target table cannot be sparse (every row will have the same set of columns); this should be easy to fix by either allowing a MAP value to be read from Hive, and/or by allowing rows to be read from Hive in pivoted form (one row per HBase cell)

> Besides dealing with these constraints, probably the most important work here is deciding on how you want to assign an HBase row key to each row coming from Hive. To avoid inconsistencies between lexical and binary comparators, it is simplest to design a string row key and use it consistently all the way through. If you want to combine multiple columns into the key, use Hive's string concat expression for this purpose. You can use CREATE VIEW to tack on your rowkey logically without having to update any existing data in Hive.

除了处理这些约束之外，这里最重要的工作可能是决定如何为来自 Hive 的每一行分配一个 HBase 行键。

为了避免词汇比较器和二进制比较器之间的不一致，最简单的方法是设计一个字符串行键，并始终一致地使用它。

如果要将多个列组合到键中，请使用 Hive 的字符串连接表达式。

你可以使用 CREATE VIEW 来逻辑地添加 rowkey，而不需要更新 Hive 中的任何现有数据。

## 3、Estimate Resources Needed

> TBD: provide some example numbers based on Facebook experiments; also reference [Hadoop Terasort](http://www.hpl.hp.com/hosted/sortbenchmark/YahooHadoop.pdf)

## 4、Add necessary JARs

> You will need to add a couple jar files to your path. First, put them in DFS:

需要向路径中添加两个 jar 文件。首先，将它们放入 DFS 中:

```sh
hadoop dfs -put /usr/lib/hive/lib/hbase-VERSION.jar /user/hive/hbase-VERSION.jar
hadoop dfs -put /usr/lib/hive/lib/hive-hbase-handler-VERSION.jar /user/hive/hive-hbase-handler-VERSION.jar
```

> Then add them to your hive-site.xml:

然后添加到你的 hive-site.xml 中：

```xml
<property>
  <name>hive.aux.jars.path</name>
  <value>/user/hive/hbase-VERSION.jar,/user/hive/hive-hbase-handler-VERSION.jar</value>
</property>
```

## 5、Prepare Range Partitioning

> In order to perform a parallel sort on the data, we need to range-partition it. The idea is to divide the space of row keys up into nearly equal-sized ranges, one per reducer which will be used in the parallel sort. The details will vary according to your source data, and you may need to run a number of exploratory Hive queries in order to come up with a good enough set of ranges. Here's one example:

为了对数据执行并行排序，我们需要对其进行范围分区。

其思想是将行键的空间划分为几乎相同大小的范围，每个 reducer 处理一个，用于并行排序。

具体细节将根据源数据而变化，你可能需要运行一些探索性的 Hive 查询，以便获得足够好的范围集合。

```sql
add jar lib/hive-contrib-0.7.0.jar;
set mapred.reduce.tasks=1;

create temporary function row_sequence as
'org.apache.hadoop.hive.contrib.udf.UDFRowSequence';
select transaction_id from
(select transaction_id
from transactions
tablesample(bucket 1 out of 10000 on transaction_id) s
order by transaction_id
limit 10000000) x
where (row_sequence() % 910000)=0
order by transaction_id
limit 11;
```

> This works by ordering all of the rows in a .01% sample of the table (using a single reducer), and then selecting every nth row (here n=910000). The value of n is chosen by dividing the total number of rows in the sample by the desired number of ranges, e.g. 12 in this case (one more than the number of partitioning keys produced by the LIMIT clause). The assumption here is that the distribution in the sample matches the overall distribution in the table; if this is not the case, the resulting partition keys will lead to skew in the parallel sort.

这是通过对表中 0.01% 样本中的所有行进行排序(使用单个reducer)，然后选择每个的第 n 行(这里 n=910000)来实现的。

选择 n 的方法是将样本中的总行数除以所需的范围数，例如在本例中为12(比 LIMIT 子句产生的分区键数多一个)。

这里的假设是，样本中的分布与表中的总体分布相匹配；如果不是这样，那么产生的分区键将导致并行排序中的倾斜。

> Once you have your sampling query defined, the next step is to save its results to a properly formatted file which will be used in a subsequent step. To do this, run commands like the following:

定义了抽样查询之后，下一步是将其结果保存到一个格式正确的文件中，该文件将在后续步骤中使用。为此，运行如下命令:

```sql
create external table hb_range_keys(transaction_id_range_start string)
row format serde
'org.apache.hadoop.hive.serde2.binarysortable.BinarySortableSerDe'
stored as
inputformat
'org.apache.hadoop.mapred.TextInputFormat'
outputformat
'org.apache.hadoop.hive.ql.io.HiveNullValueSequenceFileOutputFormat'
location '/tmp/hb_range_keys';
 
insert overwrite table hb_range_keys
select transaction_id from
(select transaction_id
from transactions
tablesample(bucket 1 out of 10000 on transaction_id) s
order by transaction_id
limit 10000000) x
where (row_sequence() % 910000)=0
order by transaction_id
limit 11;
```

> The first command creates an external table defining the format of the file to be created; be sure to set the serde and inputformat/outputformat exactly as specified.

第一个命令创建一个外部表，它定义了要创建的文件的格式；确保按照指定的方式设置 serde 和 inputformat/outputformat。

> The second command populates it (using the sampling query previously defined). Usage of ORDER BY guarantees that a single file will be produced in directory /tmp/hb_range_keys. The filename is unknown, but it is necessary to reference the file by name later, so run a command such as the following to copy it to a specific name:

第二个命令填充它(使用前面定义的抽样查询)。使用 ORDER BY 可以保证在 `/tmp/hb_range_keys` 目录下生成单个文件。

文件名是未知的，但以后有必要按文件名引用该文件，因此运行如下命令将其复制到特定的名称:

```sh
dfs -cp /tmp/hb_range_keys/* /tmp/hb_range_key_list;
```

## 6、Prepare Staging Location

> The sort is going to produce a lot of data, so make sure you have sufficient space in your HDFS cluster, and choose the location where the files will be staged. We'll use /tmp/hbsort in this example.

排序将产生大量数据，因此请确保在 HDFS 集群中有足够的空间，并选择文件将暂存的位置。

在这个例子中，我们将使用 `/tmp/hbsort`。

> The directory does not actually need to exist (it will be automatically created in the next step), but if it does exist, it should be empty.

这个目录实际上不需要存在(它将在下一步中自动创建)，但是如果它确实存在，它应该是空的。

```sh
dfs -rmr /tmp/hbsort;
dfs -mkdir /tmp/hbsort;
```

## 7、Sort Data

> Now comes the big step: running a sort over all of the data to be bulk loaded. Make sure that your Hive instance has the HBase jars available on its auxpath.

现在是重要的一步：对所有要批量加载的数据进行排序。

确保你的 Hive 实例在它的 auxpath 上有 HBase jar 可用。

```sql
set hive.execution.engine=mr;
set mapred.reduce.tasks=12;
set hive.mapred.partitioner=org.apache.hadoop.mapred.lib.TotalOrderPartitioner;
set total.order.partitioner.path=/tmp/hb_range_key_list;
set hfile.compression=gz;
 
create table hbsort(transaction_id string, user_name string, amount double, ...)
stored as
INPUTFORMAT 'org.apache.hadoop.mapred.TextInputFormat'
OUTPUTFORMAT 'org.apache.hadoop.hive.hbase.HiveHFileOutputFormat'
TBLPROPERTIES ('hfile.family.path' = '/tmp/hbsort/cf');
 
insert overwrite table hbsort
select transaction_id, user_name, amount, ...
from transactions
cluster by transaction_id;
```

> The CREATE TABLE creates a dummy table which controls how the output of the sort is written. Note that it uses HiveHFileOutputFormat to do this, with the table property `hfile.family.path` used to control the destination directory for the output. Again, be sure to set the inputformat/outputformat exactly as specified. In the example above, we select gzip (gz) compression for the result files; if you don't set the `hfile.compression` parameter, no compression will be performed. (The other method available is lzo, which compresses less aggressively but does not require as much CPU power.)

CREATE TABLE 创建一个虚拟表，它控制如何写入排序的输出。

注意，它使用 HiveHFileOutputFormat 来完成这一操作，并使用表属性 `hfile.family.path` 控制输出的目标目录的路径。

同样，确保按照指定的方式设置 inputformat/outputformat。

在上面的例子中，我们为结果文件选择 gzip (gz)压缩；如果不设置 `hfile.compression` 参数，则不会执行压缩。(另一种可用的方法是lzo，它的压缩力度较小，但不需要那么多的CPU功耗。)

> Note that the number of reduce tasks is one more than the number of partitions - this must be true or else you will get a "Wrong number of partitions in keyset" error.

注意，reduce 任务的数量比分区数量多一个，这必须为真，否则您将得到一个 "Wrong number of partitions in keyset" 错误。

> There is a parameter `hbase.hregion.max.filesize` (default 256MB) which affects how HFiles are generated. If the amount of data (pre-compression) produced by a reducer exceeds this limit, more than one HFile will be generated for that reducer. This will lead to unbalanced region files. This will not cause any correctness problems, but if you want to get balanced region files, either use more reducers or set this parameter to a larger value. Note that when compression is enabled, you may see multiple files generated whose sizes are well below the limit; this is because the overflow check is done pre-compression.

有一个参数 `hbase.hregion.max.filesize`(默认为256MB)，影响 HFiles 的生成方式。

如果一个 reducer 产生的数据量(压缩前)超过这个限制，将为该 reducer 生成一个以上的 HFile。这将导致不平衡的 region 文件。这不会导致任何正确性问题，但是如果你想要得到平衡的 region 文件，要么使用更多的 reducers，要么将该参数设置为一个更大的值。

请注意，当启用压缩时，你可能会看到生成的多个文件的大小远低于限制；这是因为溢出检查是在压缩前完成的。

> The cf in the path specifies the name of the column family which will be created in HBase, so the directory name you choose here is important. (Note that we're not actually using an HBase table here; HiveHFileOutputFormat writes directly to files.)

路径中的 cf 指定了将在 HBase 中创建的列族的名称，因此在这里选择的目录名很重要。(注意，这里我们实际上并没有使用 HBase 表；HiveHFileOutputFormat 直接写入文件。)

> The CLUSTER BY clause provides the keys to be used by the partitioner; be sure that it matches the range partitioning that you came up with in the earlier step.

CLUSTER BY 子句提供了分区器使用的键；确保它与你在前面步骤中提出的范围分区相匹配。

> The first column in the SELECT list is interpreted as the rowkey; subsequent columns become cell values (all in a single column family, so their column names are important).

SELECT 列表中的第一列被解释为 rowkey；随后的列变成单元格值(所有列都在一个列族中，因此它们的列名很重要)。

## 8、Run HBase Script

> Once the sort job completes successfully, one final step is required for importing the result files into HBase. Again, we don't know the name of the file, so we copy it over:

一旦排序 job 成功完成，最后一个步骤是将结果文件导入 HBase。

同样，我们不知道文件名，所以我们复制它:

	dfs -copyToLocal /tmp/hbsort/cf/* /tmp/hbout

> If Hive and HBase are running in different clusters, use [distcp](http://hadoop.apache.org/common/docs/current/distcp.html) to copy the files from one to the other.

如果 Hive 和 HBase 运行在不同的集群中，可以使用 distcp 将文件从一个集群复制到另一个集群。

> If you are using HBase 0.90.2 or newer, you can use the [completebulkload](http://hbase.apache.org/bulk-loads.html) utility to load the data into HBase

如果使用的是 HBase 0.90.2 或更新版本，你可以使用 completebulkload 工具将数据加载到 HBase 中

	hadoop jar hbase-VERSION.jar completebulkload [-c /path/to/hbase/config/hbase-site.xml] /tmp/hbout transactions

> In older versions of HBase, use the bin/loadtable.rb script to import them:

在 HBase 的旧版本中，使用 bin/loadtable.rb 脚本导入它们：

	hbase org.jruby.Main loadtable.rb transactions /tmp/hbout

> The first argument (transactions) specifies the name of the new HBase table. For the second argument, pass the staging directory name, not the the column family child directory.

第一个参数(transactions)指定了新的 HBase 表的名称。

对于第二个参数，传递临时目录名，而不是列族子目录。

> After this script finishes, you may need to wait a minute or two for the new table to be picked up by the HBase meta scanner. Use the hbase shell to verify that the new table was created correctly, and do some sanity queries to locate individual cells and make sure they can be found.

在这个脚本完成之后，你可能需要等待一到两分钟，以便 HBase meta scanner 拾取新表。

使用 hbase shell 来验证新表是否正确创建，并执行一些明智的查询来定位单个单元格，并确保能够找到它们。

## 9、Map New Table Back Into Hive

> Finally, if you'd like to access the HBase table you just created via Hive:

最后，你想访问 HBase 表，你可以通过 Hive 创建：

```sql
CREATE EXTERNAL TABLE hbase_transactions(transaction_id string, user_name string, amount double, ...)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES ("hbase.columns.mapping" = ":key,cf:user_name,cf:amount,...")
TBLPROPERTIES("hbase.table.name" = "transactions");
```

## 10、Followups Needed

- Support sparse tables
- Support loading binary data representations once HIVE-1245 is fixed
- Support assignment of timestamps
- Provide control over file parameters such as compression
- Support multiple column families once HBASE-1861 is implemented
- Support loading into existing tables once HBASE-1923 is implemented
- Wrap it all up into the ideal single-INSERT-with-auto-sampling job...