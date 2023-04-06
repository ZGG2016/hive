# Skewed Join Optimization

[TOC]

## 1、Optimizing Skewed Joins

The Problem

> A join of 2 large data tables is done by a set of MapReduce jobs which first sorts the tables based on the join key and then joins them. The Mapper gives all rows with a particular key to the same Reducer.

**两个大的数据表的连接是通过一组 MapReduce jobs 来完成的，这些任务首先根据连接键对表进行排序，然后再进行连接**。

Mapper 将具有特定键的所有行传给同一个 Reducer。

> e.g., Suppose we have table A with a key column, "id" which has values 1, 2, 3 and 4, and table B with a similar column, which has values 1, 2 and 3.

例如，假设表 A 有一个键列 “id”，值为1、2、3 和 4，表 B 有一个类似的列，值为1、2 和 3。

> We want to do a join corresponding to the following query

我们希望执行连接，对应以下的查询：

```sql
select A.id from A join B on A.id = B.id
```

> A set of Mappers read the tables and gives them to Reducers based on the keys. e.g., rows with key 1 go to Reducer R1, rows with key 2 go to Reducer R2 and so on. These Reducers do a cross product of the values from A and B, and write the output. The Reducer R4 gets rows from A, but will not produce any results.

一组 Mappers 读取表，并根据键将它们交给 Reducers。

例如，键为 1 的行转到 Reducer R1，键为 2 的行转到 Reducer R2，以此类推。

这些 Reducers 对 A 和 B 的值做一个向量积，然后写出输出。Reducer R4 从 A 中获取行，但不会产生任何结果。

> Now let's assume that A was highly skewed in favor of id = 1. Reducers R2 and R3 will complete quickly but R1 will continue for a long time, thus becoming the bottleneck. If the user has information about the skew, the bottleneck can be avoided manually as follows:

现在我们假设 A 在 id=1 上具有很高的倾斜。Reducers R2 和 R3 会很快完成，但是 R1 会持续很长时间，从而成为瓶颈。如果用户有关于倾斜的信息，可以手动避免瓶颈，如下:

> Do two separate queries

执行两个单独的查询

```sql
select A.id from A join B on A.id = B.id where A.id <> 1;
select A.id from A join B on A.id = B.id where A.id = 1 and B.id = 1;
```

> The first query will not have any skew, so all the Reducers will finish at roughly the same time. If we assume that B has only few rows with B.id = 1, then it will fit into memory. So the join can be done efficiently by storing the B values in an in-memory hash table. This way, the join can be done by the Mapper itself and the data do not have to go to a Reducer. The partial results of the two queries can then be merged to get the final results.

第一个查询不会有任何倾斜，因此，所有的 Reducers 将在大致相同的时间完成。

如果我们假设 B 只有几行 B.id = 1 的行，那么它将适合内存。因此，通过将 B 值存储在内存中的哈希表中，可以有效地完成连接。通过这种方式，可以由 Mapper 本身完成连接，而数据不必转到 Reducer。然后可以合并这两个查询的部分结果，以获得最终结果。

> Advantages
> If a small number of skewed keys make up for a significant percentage of the data, they will not become bottlenecks.

- 优势

	- 如果少量的倾斜键占数据的很大比例，它们就不会成为瓶颈。

> Disadvantages
> The tables A and B have to be read and processed twice.
> Because of the partial results, the results also have to be read and written twice.
> The user needs to be aware of the skew in the data and manually do the above process.

- 缺点

	- 表 A 和表 B 必须被读取和处理两次。

	- 由于结果不完整，因此还需要对结果进行两次读写。

	- 用户需要意识到数据中的倾斜，并手动执行上述过程。

> We can improve this further by trying to reduce the processing of skewed keys. First read B and store the rows with key 1 in an in-memory hash table. Now run a set of mappers to read A and do the following:

我们可以通过减少对倾斜键的处理来进一步改进这一点。首先读取 B，并将键为 1 的行存储在内存哈希表中。

现在运行一组 mappers 来读取 A，并执行以下操作:

- 如果它有键 1，则使用 B 的哈希版本来计算结果。

- 对于所有其他的键，将它发送给一个执行连接的 reducer。这个 reducer 也会从一个 mapper 中得到 B 的行。

> If it has key 1, then use the hashed version of B to compute the result.
> For all other keys, send it to a reducer which does the join. This reducer will get rows of B also from a mapper.

> This way, we end up reading only B twice. The skewed keys in A are only read and processed by the Mapper, and not sent to the reducer. The rest of the keys in A go through only a single Map/Reduce.

这样，我们最终只读 B 两次。A 中的倾斜键仅由 Mapper 读取和处理，不发送给 reducer。A 中其余的键只通过一个的 Map/Reduce。

> The assumption is that B has few rows with keys which are skewed in A. So these rows can be loaded into the memory.

假设 B 的键在 A 中是倾斜的，所以这些行可以被加载到内存中。

## 2、Hive Enhancements

> Original plan:  The skew data will be obtained from list bucketing (see the [List Bucketing](https://cwiki.apache.org/confluence/display/Hive/ListBucketing) design document). There will be no additions to the Hive grammar.

原方案：倾斜数据从列桶中获取。Hive 语法不会有任何增加。

> Implementation:  Starting in Hive 0.10.0, tables can be created as skewed or altered to be skewed (in which case partitions created after the ALTER statement will be skewed). In addition, skewed tables can use the list bucketing feature by specifying the STORED AS DIRECTORIES option. See the DDL documentation for details: [Create Table](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTable), [Skewed Tables](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-SkewedTables), and [Alter Table Skewed or Stored as Directories](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AlterTableSkewedorStoredasDirectories).

实现：从 Hive 0.10.0 开始，可以创建表为倾斜表，也可以被修改为倾斜表(在这种情况下，ALTER 语句之后创建的分区将被倾斜)。

此外，倾斜表可以通过指定 STORED AS DIRECTORIES 选项来使用列桶特性。详细信息请参阅 DDL 文档。
