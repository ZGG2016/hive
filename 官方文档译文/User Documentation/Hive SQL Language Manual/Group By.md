# LanguageManual GroupBy

[TOC]

## 1、Group By Syntax

	groupByClause: GROUP BY groupByExpression (, groupByExpression)*
	 
	groupByExpression: expression
	 
	groupByQuery: SELECT expression (, expression)* FROM src groupByClause?

> In groupByExpression columns are specified by name, not by position number. However in [Hive 0.11.0](https://issues.apache.org/jira/browse/HIVE-581) and later, columns can be specified by position when configured as follows:

**在 groupByExpression 中，列由名称指定，而不是由位置号码。然而，在 Hive 0.11.0 及更高版本中，当配置了如下内容，可以由位置指定**：

- 对于 Hive 0.11.0 到 2.1.x 版本，设置 `hive.groupby.orderby.position.alias` 为 true。（默认是false）

- 对于 Hive 2.2.0 及更高版本，设置 `hive.groupby.position.alias` 为 true。（默认是false）

> For Hive 0.11.0 through 2.1.x, set [hive.groupby.orderby.position.alias](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.groupby.orderby.position.alias) to true (the default is false).

> For Hive 2.2.0 and later, set [hive.groupby.position.alias](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.groupby.position.alias) to true (the default is false).

### 1.1、Simple Examples

> In order to count the number of rows in a table:

为了统计表中的行数：

```sql
SELECT COUNT(*) FROM table2;
```

> Note that for versions of Hive which don't include [HIVE-287](https://issues.apache.org/jira/browse/HIVE-287), you'll need to use `COUNT(1)` in place of `COUNT(*)`.

注意，**对于不包含 Hive-287 的 Hive 版本，你需要用 `COUNT(1)` 来代替 `COUNT(*)`**。

> In order to count the number of distinct users by gender one could write the following query:

为了按性别计算去重的用户的数量，可以编写以下查询:

```sql
INSERT OVERWRITE TABLE pv_gender_sum
SELECT pv_users.gender, count (DISTINCT pv_users.userid)
FROM pv_users
GROUP BY pv_users.gender;
```

> Multiple aggregations can be done at the same time, however, no two aggregations can have different DISTINCT columns. For example, the following is possible because count(DISTINCT) and sum(DISTINCT) specify the same column:

**多个聚合操作可以同时完成，然而，两个聚合不能有不同的 DISTINCT 列**。例如，下面的是可能的，因为 `count(DISTINCT)` 和 `sum(DISTINCT)` 指定了相同的列：

```sql
INSERT OVERWRITE TABLE pv_gender_agg
SELECT pv_users.gender, count(DISTINCT pv_users.userid), count(*), sum(DISTINCT pv_users.userid)
FROM pv_users
GROUP BY pv_users.gender;
```

> Note that for versions of Hive which don't include HIVE-287, you'll need to use `COUNT(1)` in place of `COUNT(*)`.

注意，对于不包含 Hive-287 的 Hive 版本，你需要用 `COUNT(1)` 来代替 `COUNT(*)`。

> However, the following query is not allowed. We don't allow multiple DISTINCT expressions in the same query.

然而，下面的查询是不允许的。我们**不允许在相同的查询有多个 DISTINCT 表达式**。

```sql
INSERT OVERWRITE TABLE pv_gender_agg
SELECT pv_users.gender, count(DISTINCT pv_users.userid), count(DISTINCT pv_users.ip)
FROM pv_users
GROUP BY pv_users.gender;
```

### 1.2、Select statement and group by clause

> When using group by clause, the select statement can only include columns included in the group by clause. Of course, you can have as many aggregation functions (e.g. count) in the select statement as well.

**当使用 group by 子句时，select 语句只能包含 group by 子句中的列**。

当然，在 select 语句中也可以有很多聚合函数(如 count)。

> Let's take a simple example

```sql
CREATE TABLE t1(a INTEGER, b INTGER);
```

> A group by query on the above table could look like:

在上述表的一个 group by 查询可以是这样的：

```sql
SELECT
   a,
   sum(b)
FROM
   t1
GROUP BY
   a;
```

> The above query works because the select clause contains a (the group by key) and an aggregation function (sum(b)).

上述查询是可以的，因为 select 子句包含了 a(the group by key)，和一个聚合函数(sum(b))。

> However, the query below DOES NOT work:

然而，下面的查询不可以：

```sql
SELECT
   a,
   b
FROM
   t1
GROUP BY
   a;
```

> This is because the select clause has an additional column (b) that is not included in the group by clause (and it's not an aggregation function either). This is because, if the table t1 looked like:

这是因为 select 子句有一个额外的列 b，它是不包含在 group by 子句中的（它也不是一个聚合函数）。

这是因为，如果表 t1 是这样的:

	a    b
	------
	100  1
	100  2
	100  3

> Since the grouping is only done on a, what value of b should Hive display for the group a=100? One can argue that it should be the first value or the lowest value but we all agree that there are multiple possible options. Hive does away with this guessing by making it invalid SQL (HQL, to be precise) to have a column in the select clause that is not included in the group by clause.

既然分组只在 a 上进行，那么对于组 a=100, Hive 应该显示什么 b 值呢？

有人可能会说它应该是第一个值或最小值，但我们都同意存在多种可能的选择。

Hive 通过将 select 子句中没有包含在 group by 子句中的列设置为无效 SQL(准确地说，是 HQL)来消除这种猜测。

## 2、Advanced Features

### 2.1、Multi-Group-By Inserts

> The output of the aggregations or simple selects can be further sent into multiple tables or even to hadoop dfs files (which can then be manipulated using hdfs utilitites). e.g. if along with the gender breakdown, one needed to find the breakdown of unique page views by age, one could accomplish that with the following query:

聚合或简单选择的输出可以进一步发送到多个表中，甚至发送到 hadoop dfs 文件中(然后可以使用 hdfs 实用程序操作这些文件)。

例如，如果在性别分类的同时，你需要找到按年龄划分的唯一页面浏览量，你可以通过以下查询来完成:

```sql
-- 分别按照年龄和性别分组，求出每组去重后的用户数量，分别去往表和HDFS
FROM pv_users
INSERT OVERWRITE TABLE pv_gender_sum
  SELECT pv_users.gender, count(DISTINCT pv_users.userid)
  GROUP BY pv_users.gender
INSERT OVERWRITE DIRECTORY '/user/facebook/tmp/pv_age_sum'
  SELECT pv_users.age, count(DISTINCT pv_users.userid)
  GROUP BY pv_users.age;

SELECT pv_users.age, count(DISTINCT pv_users.userid)
GROUP BY pv_users.age;

```

### 2.2、Map-side Aggregation for Group By

> hive.map.aggr controls how we do aggregations. The default is false. If it is set to true, Hive will do the first-level aggregation directly in the map task.
This usually provides better efficiency, but may require more memory to run successfully.

`hive.map.aggr` 控制着如何进行聚合操作。默认是 false。

如果设为 true，Hive 将在 map task 中直接作第一级的聚合。

这通常提供更好的效率，但要求更多的内存。

	set hive.map.aggr=true;
	SELECT COUNT(*) FROM table2;

> Note that for versions of Hive which don't include [HIVE-287]https://issues.apache.org/jira/browse/HIVE-287(), you'll need to use `COUNT(1)` in place of `COUNT(*)`.

注意，对于不包含 Hive-287 的 Hive 版本，你需要用 `COUNT(1)` 来代替 `COUNT(*)`。

### 2.3、Grouping Sets, Cubes, Rollups, and the GROUPING__ID Function

> Version:Grouping sets, CUBE and ROLLUP operators, and the GROUPING__ID function were added in Hive release 0.10.0.

版本：`Grouping sets`、`CUBE` 、`ROLLUP` 操作符和 `GROUPING__ID` 函数在 Hive 0.10.0 添加。

> See [Enhanced Aggregation, Cube, Grouping and Rollup](https://cwiki.apache.org/confluence/display/Hive/Enhanced+Aggregation%2C+Cube%2C+Grouping+and+Rollup) for information about these aggregation operators.

关于这些聚合操作符的更多信息见 Enhanced Aggregation, Cube, Grouping and Rollup。

Also see the JIRAs:

- [HIVE-2397](https://issues.apache.org/jira/browse/HIVE-2397) Support with rollup option for group by
- [HIVE-3433](https://issues.apache.org/jira/browse/HIVE-3433) Implement CUBE and ROLLUP operators in Hive
- [HIVE-3471](https://issues.apache.org/jira/browse/HIVE-3471) Implement grouping sets in Hive
- [HIVE-3613](https://issues.apache.org/jira/browse/HIVE-3613) Implement grouping_id function

New in Hive release 0.11.0:

- [HIVE-3552](https://issues.apache.org/jira/browse/HIVE-3552) HIVE-3552 performant manner for performing cubes/rollups/grouping sets for a high number of grouping set keys