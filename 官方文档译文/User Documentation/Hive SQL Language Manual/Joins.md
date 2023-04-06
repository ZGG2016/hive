# LanguageManual Joins

[TOC]

## 1、Join Syntax

> Hive supports the following syntax for joining tables:

	join_table:
	    table_reference [INNER] JOIN table_factor [join_condition]
	  | table_reference {LEFT|RIGHT|FULL} [OUTER] JOIN table_reference join_condition
	  | table_reference LEFT SEMI JOIN table_reference join_condition
	  | table_reference CROSS JOIN table_reference [join_condition] (as of Hive 0.10)
	 
	table_reference:
	    table_factor
	  | join_table
	 
	table_factor:
	    tbl_name [alias]
	  | table_subquery alias
	  | ( table_references )
	 
	join_condition:
	    ON expression

> See [Select Syntax](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Select#LanguageManualSelect-SelectSyntax) for the context of this join syntax.

> Version 0.13.0+: Implicit join notation

> Implicit join notation is supported starting with Hive 0.13.0 (see [HIVE-5558](https://issues.apache.org/jira/browse/HIVE-5558)). This allows the FROM clause to join a comma-separated list of tables, omitting the JOIN keyword. For example:

从 Hive 0.13.0 开始，**支持隐式连接表示法**。这允许 FROM 子句连接以逗号分隔的表列表，而省略 JOIN 关键字。例如:

```sql
SELECT * 
FROM table1 t1, table2 t2, table3 t3 
WHERE t1.id = t2.id AND t2.id = t3.id AND t1.zipcode = '02535';
```

> Version 0.13.0+: Unqualified column references

> Unqualified column references are supported in join conditions, starting with Hive 0.13.0 (see [HIVE-6393](https://issues.apache.org/jira/browse/HIVE-6393)). Hive attempts to resolve these against the inputs to a Join. If an unqualified column reference resolves to more than one table, Hive will flag it as an ambiguous reference.For example:

从 Hive 0.13.0 开始，**在 join conditions 中支持非限定列引用**。

Hive 试图根据 Join 的输入来解析非限定列引用。如果一个未限定的列引用解析为属于多个表，Hive 会将其标记为一个模糊引用。例如:

```sql
CREATE TABLE a (k1 string, v1 string);
CREATE TABLE b (k2 string, v2 string);

SELECT k1, v1, k2, v2
FROM a JOIN b ON k1 = k2; 
```

> Version 2.2.0+: Complex expressions in ON clause

> Complex expressions in ON clause are supported, starting with Hive 2.2.0 (see [HIVE-15211](https://issues.apache.org/jira/browse/HIVE-15211), [HIVE-15251](https://issues.apache.org/jira/browse/HIVE-15251)). Prior to that, Hive did not support join conditions that are not equality conditions.

从 Hive 2.2.0 开始，**支持 ON 子句中的复杂表达式**。在此之前，Hive 不支持非等号条件的 join conditions。

> In particular, syntax for join conditions was restricted as follows:

特别是，join conditions  的语法受到如下限制:

	join_condition:
	    ON equality_expression ( AND equality_expression )*
	equality_expression:
	    expression = expression

## 2、Examples

> Some salient points to consider when writing join queries are as follows:

在编写连接查询时需要考虑的一些要点如下:

> Complex join expressions are allowed e.g.

- **允许复杂 join 表达式**。如：

```sql
SELECT a.* FROM a JOIN b ON (a.id = b.id)
SELECT a.* FROM a JOIN b ON (a.id = b.id AND a.department = b.department)
SELECT a.* FROM a LEFT OUTER JOIN b ON (a.id <> b.id)
```
是有效 joins。

> are valid joins.

> More than 2 tables can be joined in the same query e.g.

- **在同一查询中，可用连接多于两个表**。如：

```sql
SELECT a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key2)
```

是有效 joins。

> are valid joins.

> Hive converts joins over multiple tables into a single map/reduce job if for every table the same column is used in the join clauses e.g.

- **如果在 join 子句中使用每个表的相同的列，那么 Hive 将多表 join 转成一个 map/reduce job**。

```sql
SELECT a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key1)
```

> is converted into a single map/reduce job as only key1 column for b is involved in the join. On the other hand

被转成一个 map/reduce job，因为连接中只涉及 b 的 key1 列。另一方面。

```sql
SELECT a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key2)
```

> is converted into two map/reduce jobs because key1 column from b is used in the first join condition and key2 column from b is used in the second one. The first map/reduce job joins a with b and the results are then joined with c in the second map/reduce job.

被转成两个 map/reduce job，因为 b 的 key1 列在第一个 join 条件中使用，b 的 key2 列在第二个 join 条件中使用。**第一个 map/reduce job 将 a 和 b join，然后在第二个 map/reduce job 中结果再和 c join**。

> In every map/reduce stage of the join, the last table in the sequence is streamed through the reducers where as the others are buffered. Therefore, it helps to reduce the memory needed in the reducer for buffering the rows for a particular value of the join key by organizing the tables such that the largest tables appear last in the sequence. e.g. in

- **在 join 的每个 map/reduce 阶段，序列中的最后一个表通过 reducers 流动，而其他表被缓存**。

因此，通过组织表，将最大的表出现在序列的最后，它有助于减少 reducer 中为连接键的特定值而缓冲行所需的内存。例如,在

```sql
SELECT a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key1)
```

> all the three tables are joined in a single map/reduce job and the values for a particular value of the key for tables a and b are buffered in the memory in the reducers. Then for each row retrieved from c, the join is computed with the buffered rows. Similarly for

**一个 map/reduce job 中连接所有的三个表，表 a 和表 b 的键的特定值被缓存在 reducers 的内存中。然后，对于从 c 检索到的每一行，使用缓存的行进行计算连接**。同样的

```sql
SELECT a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key2)
```

> there are two map/reduce jobs involved in computing the join. The first of these joins a with b and buffers the values of a while streaming the values of b in the reducers. The second of one of these jobs buffers the results of the first join while streaming the values of c through the reducers.

在计算连接时，涉及两个 map/reduce jobs。第一个 job 连接 a 和 b，并缓存 a 的值，同时 b 的值在 reducer 中流动。其中的第二个 job 缓存第一个连接的结果，同时 c 的值在 reducer 中流动。。

> In every map/reduce stage of the join, the table to be streamed can be specified via a hint. e.g. in

- **在 join 的每个 map/reduce 阶段，流动的表可用通过一个 hint 指定**。如：

```sql
SELECT /*+ STREAMTABLE(a) */ a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key1)
```

> all the three tables are joined in a single map/reduce job and the values for a particular value of the key for tables b and c are buffered in the memory in the reducers. Then for each row retrieved from a, the join is computed with the buffered rows. If the STREAMTABLE hint is omitted, Hive streams the rightmost table in the join.

一个 map/reduce job 中连接所有的三个表，表 b 和表 c 的键的特定值被缓存在 reducers 的内存中。然后，对于从a 检索到的每一行，使用缓存行计算连接。如果省略了 STREAMTABLE hint，Hive 将最右边的表作为流动的表。

> LEFT, RIGHT, and FULL OUTER joins exist in order to provide more control over ON clauses for which there is no match. For example, this query:

- 存在 LEFT、RIGHT 和 FULL OUTER 是为了对在 ON 子句上没有匹配的子句提供更多的控制。例如，这个查询:

```sql
SELECT a.val, b.val FROM a LEFT OUTER JOIN b ON (a.key=b.key)
```

> will return a row for every row in a. This output row will be a.val,b.val when there is a b.key that equals a.key, and the output row will be a.val,NULL when there is no corresponding b.key. Rows from b which have no corresponding a.key will be dropped. The syntax "FROM a LEFT OUTER JOIN b" must be written on one line in order to understand how it works--a is to the LEFT of b in this query, and so all rows from a are kept; a RIGHT OUTER JOIN will keep all rows from b, and a FULL OUTER JOIN will keep all rows from a and all rows from b. OUTER JOIN semantics should conform to standard SQL specs.

将返回为 a 中的每一行。

如果有 b.key 等于 a.key，这个输出行将是 a.val，b.val。如果没有相应的b.key，则输出行为 a.val，NULL。

b 中没有对应的 a.key 的行将被删除。

语法 `FROM a LEFT OUTER JOIN b` 必须写在一行上，以便理解它是如何工作的：在这个查询中，a 在 b 的左边，因此保留来自 a 的所有行；`RIGHT OUTER JOIN` 将保留来自 b 的所有行，而 `FULL OUTER JOIN`将保留来自 a 的所有行和来自 b 的所有行。`OUTER JOIN` 语义应该符合标准的 SQL 规范。

> Joins occur BEFORE WHERE CLAUSES. So, if you want to restrict the OUTPUT of a join, a requirement should be in the WHERE clause, otherwise it should be in the JOIN clause. A big point of confusion for this issue is partitioned tables:

- **join 出现在 WHERE 子句之前**。因此，如果要限制连接的输出，应该在 WHERE 子句中限制，否则应该在 join 子句中。这个问题的一大困惑点是分区表:

```sql
SELECT a.val, b.val FROM a LEFT OUTER JOIN b ON (a.key=b.key)
WHERE a.ds='2009-07-07' AND b.ds='2009-07-07'
```

> will join a on b, producing a list of a.val and b.val. The WHERE clause, however, can also reference other columns of a and b that are in the output of the join, and then filter them out. However, whenever a row from the JOIN has found a key for a and no key for b, all of the columns of b will be NULL, including the ds column. This is to say, you will filter out all rows of join output for which there was no valid b.key, and thus you have outsmarted your LEFT OUTER requirement. In other words, the LEFT OUTER part of the join is irrelevant if you reference any column of b in the WHERE clause. Instead, when OUTER JOINing, use this syntax:

将在 b 上加入 a，生成一个 a.val 和 b.val 的列表。然而，WHERE 子句也可以引用连接输出中的 a 和 b 的其他列，然后过滤掉它们。

然而，当 JOIN 中的一行找到了 a 的键而没有找到 b 的键时，b 的所有列都将为空，包括 ds 列。

也就是说，你将过滤掉所有没有有效的 b.key 的连接输出行，因此你已经超越了 `LEFT OUTER` 要求。

换句话说，如果在 WHERE 子句中引用 b 的任何列，则 `LEFT OUTER ` 部分是不相关的。

相反，当外连接时，使用以下语法:

```sql
SELECT a.val, b.val FROM a LEFT OUTER JOIN b
ON (a.key=b.key AND b.ds='2009-07-07' AND a.ds='2009-07-07')
```
> ..the result is that the output of the join is pre-filtered, and you won't get post-filtering trouble for rows that have a valid a.key but no matching b.key. The same logic applies to RIGHT and FULL joins.

结果是，**连接的输出是预过滤的**，对于具有有效的 a.key 但没有匹配的 b.key 的行，不会出现后过滤问题。同样的逻辑也适用于右连接和完全连接。

> Joins are NOT commutative! Joins are left-associative regardless of whether they are LEFT or RIGHT joins.

- Joins 不是可交换的！**Joins 是左结合，而不管它们是左连接还是右连接**。

```sql
SELECT a.val1, a.val2, b.val, c.val
FROM a
JOIN b ON (a.key = b.key)
LEFT OUTER JOIN c ON (a.key = c.key)
```

> ...first joins a on b, throwing away everything in a or b that does not have a corresponding key in the other table. The reduced table is then joined on c. This provides unintuitive results if there is a key that exists in both a and c but not b: The whole row (including a.val1, a.val2, and a.key) is dropped in the "a JOIN b" step because it is not in b. The result does not have a.key in it, so when it is LEFT OUTER JOINed with c, c.val does not make it in because there is no c.key that matches an a.key (because that row from a was removed). Similarly, if this were a RIGHT OUTER JOIN (instead of LEFT), we would end up with an even weirder effect: NULL, NULL, NULL, c.val, because even though we specified a.key=c.key as the join key, we dropped all rows of a that did not match the first JOIN.
To achieve the more intuitive effect, we should instead do FROM c LEFT OUTER JOIN a ON (c.key = a.key) LEFT OUTER JOIN b ON (c.key = b.key).

首先连接 a 和 b，丢弃 a 或 b 中在另一个表中没有对应键的所有内容。然后，结果表和 c 连接。

这提供了直观的结果，如果有一个键在 a 和 c 都存在，但不存在于 b：在 `a JOIN b` 步骤中整行(包括a.val1、a.val2 和 a.key)被删除，因为它不在 b 中。

结果里不会有 a.key，所有当和 c LEFT OUTER JOIN 时，c.val 不会在里面，因为没有和 a.key 相匹配的 c.key
(因为这一行被删除)。

类似地，如果这是一个 RIGHT OUTER JOIN，我们最终会得到一个更奇怪的效果:NULL、NULL、NULL、c.val，因为即使我们指定了 `a.key=c.key`。我们删除了 a 中与第一个连接不匹配的所有行。

为了达到更直观的效果，我们应该 `FROM c LEFT OUTER JOIN a ON (c.key = a.key) LEFT OUTER JOIN b ON (c.key = b.key)`。

> LEFT SEMI JOIN implements the uncorrelated IN/EXISTS subquery semantics in an efficient way. As of Hive 0.13 the IN/NOT IN/EXISTS/NOT EXISTS operators are supported using [subqueries](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+SubQueries) so most of these JOINs don't have to be performed manually anymore. The restrictions of using LEFT SEMI JOIN are that the right-hand-side table should only be referenced in the join condition (ON-clause), but not in WHERE- or SELECT-clauses etc.

- **LEFT SEMI JOIN 以一种高效的方式实现了不相关的 IN/EXISTS 子查询语义**。

在 Hive 0.13 中，可以使用子查询来支持 IN/NOT IN/EXISTS/NOT EXISTS 操作符，所以大多数 JOINs 操作不再需要手动执行。

使用 LEFT SEMI JOIN 的限制是，右边的表只能在连接条件(on-子句)中引用，而不能在 WHERE- 或 SELECT- 子句中引用。

```sql
SELECT a.key, a.value
FROM a
WHERE a.key in
 (SELECT b.key
  FROM B);
```

> can be rewritten to:

可以重写为：

```sql
SELECT a.key, a.val
FROM a LEFT SEMI JOIN b ON (a.key = b.key)
```

> If all but one of the tables being joined are small, the join can be performed as a map only job. The query

- **如果被连接的表中只有一个表较小，则连接可以作为 map only job 执行**。查询：

```sql
SELECT /*+ MAPJOIN(b) */ a.key, a.value
FROM a JOIN b ON a.key = b.key
```

> does not need a reducer. For every mapper of A, B is read completely. The restriction is that a FULL/RIGHT OUTER JOIN b cannot be performed.

不需要 reducer。对于 A 的每个 mapper，B 被完全读取。限制是不能执行 a FULL/RIGHT OUTER JOIN b。

> If the tables being joined are bucketized on the join columns, and the number of buckets in one table is a multiple of the number of buckets in the other table, the buckets can be joined with each other. If table A has 4 buckets and table B has 4 buckets, the following join

- **如果要连接的表在连接列上进行分桶，并且一个表中的桶数是另一个表中桶数的倍数，那么这些桶可以相互连接**。

如果表 A 有 4 个桶，表 B 有 4 个桶，则连接如下

```sql
SELECT /*+ MAPJOIN(b) */ a.key, a.value
FROM a JOIN b ON a.key = b.key
```

> can be done on the mapper only. Instead of fetching B completely for each mapper of A, only the required buckets are fetched. For the query above, the mapper processing bucket 1 for A will only fetch bucket 1 of B. It is not the default behavior, and is governed by the following parameter

只能在 mapper 上完成。**不是为 A 的每个 mapper 完全获取 B，而是只获取所需的桶**。对于上面的查询，处理 A 的桶 1 的 mapper 只会获取 b 的桶 1。这不是默认行为，由以下参数控制

```sh
set hive.optimize.bucketmapjoin = true
```

> If the tables being joined are sorted and bucketized on the join columns, and they have the same number of buckets, a sort-merge join can be performed. The corresponding buckets are joined with each other at the mapper. If both A and B have 4 buckets,

- **如果要连接的表在连接列上进行了排序和存储，并且它们具有相同数量的桶，则可以执行 sort-merge join**。对应的桶在 mapper 上相互连接。如果 A 和 B 都有 4 个桶，

```sql
SELECT /*+ MAPJOIN(b) */ a.key, a.value
FROM A a JOIN B b ON a.key = b.key
```

> can be done on the mapper only. The mapper for the bucket for A will traverse the corresponding bucket for B. This is not the default behavior, and the following parameters need to be set:

只能在 mapper 上完成。**A 桶的 mapper 将遍历 b 桶的对应桶**。这不是默认行为，需要设置以下参数:

```sh
set hive.input.format=org.apache.hadoop.hive.ql.io.BucketizedHiveInputFormat;
set hive.optimize.bucketmapjoin = true;
set hive.optimize.bucketmapjoin.sortedmerge = true;
```

## 3、MapJoin Restrictions

> If all but one of the tables being joined are small, the join can be performed as a map only job. The query

- 如果所有的表里面有一个表很小，可以执行一个 map join。

```sql
SELECT /*+ MAPJOIN(b) */ a.key, a.value
FROM a JOIN b ON a.key = b.key
```

> does not need a reducer. For every mapper of A, B is read completely.

不需要一个 reducer。对于 A 的每个 mapper，B 都完全地读取。

> The following is not supported.

- 下列特性不支持：

	- Union Followed by a MapJoin
	- Lateral View Followed by a MapJoin
	- Reduce Sink (Group By/Join/Sort By/Cluster By/Distribute By) Followed by MapJoin
	- MapJoin Followed by Union
	- MapJoin Followed by Join
	- MapJoin Followed by MapJoin

> The configuration variable hive.auto.convert.join (if set to true) automatically converts the joins to mapjoins at runtime if possible, and it should be used instead of the mapjoin hint. The mapjoin hint should only be used for the following query.

- 配置变量 `hive.auto.convert.join` （如果设为true）会自动在运行时将 join 转成 mapjoin，应该这么这么使用，而不是使用 mapjoin hint。mapjoin hint 应该禁用来下面的查询：

	- 如果所有的输入都被分桶或排序了，join 应该被转换成一个分桶的 map 端的 join 或分桶的 sort-merge join。

> If all the inputs are bucketed or sorted, and the join should be converted to a bucketized map-side join or bucketized sort-merge join.

> Consider the possibility of multiple mapjoins on different keys:

- 考虑在不同的键上多个 mapjoins 的可能性：

```sql
select /*+MAPJOIN(smallTableTwo)*/ idOne, idTwo, value FROM
  ( select /*+MAPJOIN(smallTableOne)*/ idOne, idTwo, value FROM
    bigTable JOIN smallTableOne on (bigTable.idOne = smallTableOne.idOne)                                                  
  ) firstjoin                                                            
  JOIN                                                                 
  smallTableTwo ON (firstjoin.idTwo = smallTableTwo.idTwo)                      
```

> The above query is not supported. Without the mapjoin hint, the above query would be executed as 2 map-only jobs. If the user knows in advance that the inputs are small enough to fit in memory, the following configurable parameters can be used to make sure that the query executes in a single map-reduce job.

不支持上述查询。如果没有 mapjoin hint，上面的查询将作为 2 个 map-only jobs 执行。如果用户事先知道输入足够小，可以装入内存，那么可以使用以下可配置参数来确保查询在单个 map-reduce job中执行。

- `hive.auto.convert.join.noconditionaltask` ：Hive 是否会根据输入文件大小优化普通 join 到 mapjoin 的转换。如果这个参数是开的，并且，the sum of size for n-1 of the tables/partitions for a n-way join 小于指定的大小，那么该 join 将直接转换为 mapjoin(没有条件任务)。

- `hive.auto.convert.join.noconditionaltask.size`：如果`hive.auto.convert.join.noconditionaltask`是关闭的时，此参数不生效。但是，如果它是开启的，并且the sum of size for n-1 of the tables/partitions for a n-way join 小于这个值，那么该 join 将直接转换为 mapjoin(没有条件任务)。。默认值是 10MB。

> hive.auto.convert.join.noconditionaltask - Whether Hive enable the optimization about converting common join into mapjoin based on the input file size. If this paramater is on, and the sum of size for n-1 of the tables/partitions for a n-way join is smaller than the specified size, the join is directly converted to a mapjoin (there is no conditional task).

> hive.auto.convert.join.noconditionaltask.size - If hive.auto.convert.join.noconditionaltask is off, this parameter does not take affect. However, if it is on, and the sum of size for n-1 of the tables/partitions for a n-way join is smaller than this size, the join is directly converted to a mapjoin(there is no conditional task). The default is 10MB.

## 4、Join Optimization

### 4.1、Predicate Pushdown in Outer Joins

> See [Hive Outer Join Behavior](https://cwiki.apache.org/confluence/display/Hive/OuterJoinBehavior) for information about predicate pushdown in outer joins.

### 4.2\Enhancements in Hive Version 0.11

> See [Join Optimization](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+JoinOptimization) for information about enhancements to join optimization introduced in Hive version 0.11.0. The use of hints is de-emphasized in the enhanced optimizations ([HIVE-3784](https://issues.apache.org/jira/browse/HIVE-3784) and related JIRAs).