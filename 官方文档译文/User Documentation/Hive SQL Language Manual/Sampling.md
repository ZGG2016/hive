# LanguageManual Sampling

## 1、Sampling Syntax

### 1.1、Sampling Bucketized Table

	table_sample: TABLESAMPLE (BUCKET x OUT OF y [ON colname])

> The TABLESAMPLE clause allows the users to write queries for samples of the data instead of the whole table. The TABLESAMPLE clause can be added to any table in the FROM clause. The buckets are numbered starting from 1. colname indicates the column on which to sample each row in the table. colname can be one of the non-partition columns in the table or rand() indicating sampling on the entire row instead of an individual column. The rows of the table are 'bucketed' on the colname randomly into y buckets numbered 1 through y. Rows which belong to bucket x are returned.

**TABLESAMPLE 子句允许用户为数据抽样(而不是整个表)编写查询**。

TABLESAMPLE 子句**可以添加到 FROM 子句中的任何表中**。

**桶从 1 开始编号**。

**colname 表示对表中每行进行抽样的列**。colname 可以是表中的一个非分区列，或者 **rand() 表示对整个行(而不是单个列)进行抽样**。

表中的行在 colname 上被分桶，随机地放入编号从 1 到 y 的 y 个桶中，返回属于桶 x 的行。

> In the following example the 3rd bucket out of the 32 buckets of the table source. 's' is the table alias.

在下面的例子中，该表源的 32 个桶中的第 3 个桶。's' 是表的别名。

```sql
SELECT *
FROM source TABLESAMPLE(BUCKET 3 OUT OF 32 ON rand()) s;
```

> Input pruning: Typically, TABLESAMPLE will scan the entire table and fetch the sample. But, that is not very efficient. Instead, the table can be created with a CLUSTERED BY clause which indicates the set of columns on which the table is hash-partitioned/clustered on. If the columns specified in the TABLESAMPLE clause match the columns in the CLUSTERED BY clause, TABLESAMPLE scans only the required hash-partitions of the table.

**Input pruning**: 通常，**TABLESAMPLE 会扫描整个表**，并获取样本。但是，这不是很有效。

相反，可以使用 CLUSTERED BY 子句创建表，该子句指明表在这个列上进行哈希分区/聚集。

**如果在 TABLESAMPLE 子句中指定的列与 CLUSTERED BY 子句中的列相匹配，则 TABLESAMPLE 只扫描表中要求的哈希分区**。【就是具体的桶】

Example:

> So in the above example, if table 'source' was created with 'CLUSTERED BY id INTO 32 BUCKETS'

所以在上面的例子中，如果表 source 是用 `CLUSTERED BY id INTO 32 BUCKETS` 创建的。

	TABLESAMPLE(BUCKET 3 OUT OF 16 ON id)

> would pick out the 3rd and 19th clusters as each bucket would be composed of (32/16)=2 clusters.

将挑选出第 3 和第 19 个聚类，因为每个桶将由 (32/16)=2 个聚类组成。

【每个桶有2个聚类，一共64个聚类，取出第 3 和第 19 个聚类】

> On the other hand the tablesample clause

另一方面，tablesample 子句

	TABLESAMPLE(BUCKET 3 OUT OF 64 ON id)

> would pick out half of the 3rd cluster as each bucket would be composed of (32/64)=1/2 of a cluster.

将挑选出第 3 个聚类的一半，因为每个桶将由 (32/64)=1/2 个聚类组成

> For information about creating bucketed tables with the CLUSTERED BY clause, see [Create Table](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTable) (especially [Bucketed Sorted Tables](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-BucketedSortedTables)) and [Bucketed Tables](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL+BucketedTables).

关于使用 CLUSTERED BY 子句创建分桶表的信息见 `Create Table` 和 `Bucketed Tables`。

### 1.2、Block Sampling

> Block sampling is available starting with Hive 0.8. Addressed under JIRA - [https://issues.apache.org/jira/browse/HIVE-2121](https://issues.apache.org/jira/browse/HIVE-2121)

从 Hive 0.8 开始就**可以使用块采样**。

	block_sample: TABLESAMPLE (n PERCENT)

> This will allow Hive to pick up at least n% data size (notice it doesn't necessarily mean number of rows) as inputs. Only CombineHiveInputFormat is supported and some special compression formats are not handled. If we fail to sample it, the input of MapReduce job will be the whole table/partition. We do it in HDFS block level so that the sampling granularity is block size. For example, if block size is 256MB, even if n% of input size is only 100MB, you get 256MB of data.

这将**允许 Hive 获取至少 n% 的数据大小**(注意，它不一定意味着行数)作为输入。

**只支持 CombineHiveInputFormat 格式，不处理一些特殊的压缩格式**。

**如果抽样失败，MapReduce 作业的输入将是整个表/分区**。

我们在 HDFS 块级别中进行抽样，**抽样粒度是块大小**。例如，如果块大小是 256MB，即使 n% 的输入大小只有 100MB，你得到 256MB 的数据。

> In the following example the input size 0.1% or more will be used for the query.

在下面的示例中，查询将使用 0.1% **或更多**的输入大小。

```sql
SELECT *
FROM source TABLESAMPLE(0.1 PERCENT) s;
```

> Sometimes you want to sample the same data with different blocks, you can change this seed number:

有时你想使用不同的块抽样出相同的数据，你可以改变种子号:

	set hive.sample.seednumber=<INTEGER>;

> Or user can specify total length to be read, but it has same limitation with PERCENT sampling. (As of Hive 0.10.0 - [https://issues.apache.org/jira/browse/HIVE-3401](https://issues.apache.org/jira/browse/HIVE-3401))

或者用户可以指定要读取的总长度，但是它有与 PERCENT 抽样相同的限制。

	block_sample: TABLESAMPLE (ByteLengthLiteral)
 
	ByteLengthLiteral : (Digit)+ ('b' | 'B' | 'k' | 'K' | 'm' | 'M' | 'g' | 'G')

> In the following example the input size 100M or more will be used for the query.

在下面的示例中，查询将使用 100M **或更多**的输入大小。

```sql
SELECT *
FROM source TABLESAMPLE(100M) s;
```

> Hive also supports limiting input by row count basis, but it acts differently with above two. First, it does not need CombineHiveInputFormat which means this can be used with non-native tables. Second, the row count given by user is applied to each split. So total row count can be vary by number of input splits. (As of Hive 0.10.0 - [https://issues.apache.org/jira/browse/HIVE-3401](https://issues.apache.org/jira/browse/HIVE-3401))

**Hive 也支持根据行数来限制输入**，但它的作用与以上两个不同。

首先，它**不需要 CombineHiveInputFormat**，这意味着它可以用于 non-native 表。其次，**将用户给出的行计数应用于每个分片**。因此，总的行计数可以根据输入分片的数量而变化。

	block_sample: TABLESAMPLE (n ROWS)

> For example, the following query will take the first 10 rows from each input split.

例如，下面的查询将从每个输入分片中取前 10 行。

```sql
SELECT * FROM source TABLESAMPLE(10 ROWS);
```

-------------------------------------------------------

```sql
hive> select * from base_table;
OK
3       lisi    shanghai        20191020
1       lijie   chengdu 20191021
2       zhangshan       huoxing 20191021
4       wangwu  lalalala        20191021
5       xinzeng hehe    20191021
Time taken: 0.255 seconds, Fetched: 5 row(s)

hive> select * from base_table tablesample(2 rows);
OK
3       lisi    shanghai        20191020
1       lijie   chengdu 20191021
Time taken: 0.118 seconds, Fetched: 2 row(s)
```

-------------------------------------------------------