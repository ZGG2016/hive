# LanguageManual Select

## 1、Select Syntax

	[WITH CommonTableExpression (, CommonTableExpression)*]    (Note: Only available starting with Hive 0.13.0)
	SELECT [ALL | DISTINCT] select_expr, select_expr, ...
	  FROM table_reference
	  [WHERE where_condition]
	  [GROUP BY col_list]
	  [ORDER BY col_list]
	  [CLUSTER BY col_list
	    | [DISTRIBUTE BY col_list] [SORT BY col_list]
	  ]
	 [LIMIT [offset,] rows]

> A SELECT statement can be part of a [union](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Union) query or a [subquery](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+SubQueries) of another query.

- 一个 SELECT 语句可以是 union 查询的一部分或另一个查询的一个 subquery。

> table_reference indicates the input to the query. It can be a regular table, a [view](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateView), a [join construct](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Joins) or a [subquery](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+SubQueries).

- table_reference 表示要查询的输入。可以是正常的表、视图、join 构造，或子查询。

> Table names and column names are case insensitive.

- 表名和列名是大小写不敏感的。

	- Hive 0.12 及更高版本中，表名和列名只允许包含字母数字和下划线。

	- Hive 0.13 及更高版本中，列名可以包含任何 Unicode 字符。在反引号中指定的任何列名都按字面意义处理。在反引号字符串中，使用双反引号来表示反引号字符。

	- 要恢复到 0.13.0 之前的行为，并将列名限制为字母数字和下划线字符，设置配置属性`hive.support.quoted.identifiers`为 none。在此配置中，反引号的名称被解释为正则表达式。

> In Hive 0.12 and earlier, only alphanumeric and underscore characters are allowed in table and column names.

> In Hive 0.13 and later, column names can contain any [Unicode](http://en.wikipedia.org/wiki/List_of_Unicode_characters) character (see [HIVE-6013](https://issues.apache.org/jira/browse/HIVE-6013)). Any column name that is specified within backticks (反引号) is treated literally. Within a backtick string, use double backticks (反引号) to represent a backtick character.

> To revert to pre-0.13.0 behavior and restrict column names to alphanumeric and underscore characters, set the configuration property [hive.support.quoted.identifiers](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.support.quoted.identifiers) to none. In this configuration, backticked names are interpreted as regular expressions. For details, see [Supporting Quoted Identifiers in Column Names](https://issues.apache.org/jira/secure/attachment/12618321/QuotedIdentifier.html) (attached to [HIVE-6013](https://issues.apache.org/jira/browse/HIVE-6013)). Also see REGEX Column Specification](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Select#LanguageManualSelect-REGEXColumnSpecification) below.

> Simple query. For example, the following query retrieves all columns and all rows from table t1.

- 简单的查询。下面的查询就是从表 t1 接受所有的行和列。

		SELECT * FROM t1 

> Note:As of [Hive 0.13.0](https://issues.apache.org/jira/browse/HIVE-4144), FROM is optional (for example, SELECT 1+1).

注意：Hive 0.13.0 开始，FROM 是可选的。

> To get the current database (as of [Hive 0.13.0](https://issues.apache.org/jira/browse/HIVE-4144)), use the [current_database() function](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF#LanguageManualUDF-Misc.Functions):

- 获取当前数据库：

		SELECT current_database()

> To specify a database, either qualify the table names with database names ("db_name.table_name" starting in [Hive 0.7](https://issues.apache.org/jira/browse/HIVE-1517)) or issue the [USE statement](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-UseDatabase) before the query statement (starting in [Hive 0.6](https://issues.apache.org/jira/browse/HIVE-675)).

- 要指定一个数据库，可以使用数据库名来限定表名(`db_name.table_name`)，或在查询语句之前使用 USE 语句。

> "db_name.table_name" allows a query to access tables in different databases.

`db_name.table_name` 允许查询访问不同数据库中的表。

> USE sets the database for all subsequent HiveQL statements. Reissue it with the keyword "default" to reset to the default database.

USE 为后续所有 HiveQL 语句设置数据库。用关键字 default 重新发布它，以将其重置为默认数据库。

	USE database_name;
	SELECT query_specifications;
	USE default;

### 1.1、WHERE Clause

> The WHERE condition is a [boolean](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types) expression. For example, the following query returns only those sales records which have an amount greater than 10 from the US region. Hive supports a number of [operators and UDFs](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF) in the WHERE clause:

WHERE 条件是一个布尔表达式。例如，下面的查询仅返回满足 region 等于 US 且 amount 大于 10 的记录。

Hive 支持在 WHERE 子句中使用大量的操作符和 UDFs。 

	SELECT * FROM sales WHERE amount > 10 AND region = "US"

> As of Hive 0.13 some types of [subqueries](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+SubQueries) are supported in the WHERE clause.

Hive 0.13 开始，在 WHERE 子句中支持子查询的一些类型。

### 1.2、ALL and DISTINCT Clauses

> The ALL and DISTINCT options specify whether duplicate rows should be returned. If none of these options are given, the default is ALL (all matching rows are returned). DISTINCT specifies removal of duplicate rows from the result set. Note, Hive supports SELECT DISTINCT * starting in release 1.1.0 ([HIVE-9194](https://issues.apache.org/jira/browse/HIVE-9194)).

ALL 和 DISTINCT 选项指定是否返回重复行。如果没有指定，默认是 ALL（所有匹配的行都返回）。DISTINCT 指定移除重复行。

注意：从 1.1.0 开始，支持 `SELECT DISTINCT *`

```sql
hive> SELECT col1, col2 FROM t1
    1 3
    1 3
    1 4
    2 5
hive> SELECT DISTINCT col1, col2 FROM t1
    1 3
    1 4
    2 5
hive> SELECT DISTINCT col1 FROM t1
    1
    2
```

> ALL and DISTINCT can also be used in a UNION clause – see [Union Syntax](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Union#LanguageManualUnion-UnionSyntax) for more information.

ALL 和 DISTINCT 都可以用在 UNION 子句。

### 1.3、Partition Based Queries

> In general, a SELECT query scans the entire table (other than for [sampling](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Sampling)). If a table created using the [PARTITIONED BY](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTable) clause, a query can do partition pruning and scan only a fraction of the table relevant to the partitions specified by the query. Hive currently does partition pruning if the partition predicates are specified in the WHERE clause or the ON clause in a JOIN. For example, if table page_views is partitioned on column date, the following query retrieves rows for just days between 2008-03-01 and 2008-03-31.

通常，一个 SELECT 查询会扫描整个表。

如果使用 PARTITIONED BY 子句创建的表，查询可以作分区修剪，仅扫描查询中指定分区的相关部分。

当前，Hive 在 WHERE 子句或 JOIN 的 ON 子句中指定了分区谓词时，会执行分区修剪。

例如，如果表 page_views 按 date 列分区，那么下面的查询只检索从 2008-03-01 到 2008-03-31 间的天数。

```sql
SELECT page_views.*
FROM page_views
WHERE page_views.date >= '2008-03-01' AND page_views.date <= '2008-03-31'
```

> If a table page_views is joined with another table dim_users, you can specify a range of partitions in the ON clause as follows:

如果表 page_views 和表 dim_users join，你可以在 ON 子句中指定分区的范围：

```sql
SELECT page_views.*
FROM page_views JOIN dim_users
  ON (page_views.user_id = dim_users.id AND page_views.date >= '2008-03-01' AND page_views.date <= '2008-03-31')
```

- See also [Partition Filter Syntax](https://cwiki.apache.org/confluence/display/Hive/Partition+Filter+Syntax).
- See also [Group By](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+GroupBy).
- See also [Sort By / Cluster By / Distribute By / Order By](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+SortBy).

【Partition Filter Syntax：[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Partition%20Filter%20Syntax.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Partition%20Filter%20Syntax.md)】

### 1.4、HAVING Clause

> Hive added support for the HAVING clause in version 0.7.0. In older versions of Hive it is possible to achieve the same effect by using a subquery, e.g:

Hive 在 0.7.0 版本中添加了对 HAVING 子句的支持。在旧的版本中，通过使用子查询来实现。

```sql
SELECT col1 FROM t1 GROUP BY col1 HAVING SUM(col2) > 10
```

> can also be expressed as

```sql
SELECT col1 FROM (SELECT col1, SUM(col2) AS col2sum FROM t1 GROUP BY col1) t2 WHERE t2.col2sum > 10
```

### 1.5、LIMIT Clause

> The LIMIT clause can be used to constrain the number of rows returned by the SELECT statement.

LIMIT 子句可用于约束 SELECT 语句返回的行数。

> LIMIT takes one or two numeric arguments, which must both be non-negative integer constants.

LIMIT 接受一个或两个数字参数，它们都必须是非负整数常量。

> The first argument specifies the offset of the first row to return (as of [Hive 2.0.0](https://issues.apache.org/jira/browse/HIVE-11531)) and the second specifies the maximum number of rows to return.

第一个参数指定要返回的第一行的偏移量，第二个参数指定要返回的最大行数。

> When a single argument is given, it stands for the maximum number of rows and the offset defaults to 0.

当给出一个参数时，它代表最大行数，偏移量默认为0。

 > The following query returns 5 arbitrary customers

下面的查询返回 5 个任意的客户

```sql
SELECT * FROM customers LIMIT 5
```

> The following query returns the first 5 customers to be created

下面的查询返回前 5 个创建的客户

```sql
SELECT * FROM customers ORDER BY create_date LIMIT 5
```

> The following query returns the  3rd to the 7th customers to be created

下面的查询返回第 3 个到 7 个创建的客户

```sql
SELECT * FROM customers ORDER BY create_date LIMIT 2,5
```

### 1.6、REGEX Column Specification

> A SELECT statement can take regex-based column specification in Hive releases prior to 0.13.0, or in 0.13.0 and later releases if the configuration property [hive.support.quoted.identifiers](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.support.quoted.identifiers) is set to none. 

在 0.13.0 之前的 Hive 版本中，或者在 0.13.0 及更高版本中，如果属性 `hive.support.quoted.identifiers` 设置为 none，则 SELECT 语句可以采用基于正则的列规范。

> We use Java regex syntax. Try [http://www.fileformat.info/tool/regex.htm](http://www.fileformat.info/tool/regex.htm) for testing purposes.
> The following query selects all columns except ds and hr.

- 我们使用 Java 正则表达式语法。出于测试目的，请尝试 http://www.fileformat.info/tool/regex.htm。

- 下面的查询选择了除 ds 和 hr 之外的所有列

```sql
SELECT `(ds|hr)?+.+` FROM sales
```

### 1.7、More Select Syntax

> See the following documents for additional syntax and features of SELECT statements:

- GROUP BY:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Group%20By.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Group%20By.md)

- SORT/ORDER/CLUSTER/DISTRIBUTE BY:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Sort%20Distribute%20Cluster%20Order%20By.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Sort%20Distribute%20Cluster%20Order%20By.md)

- UNION:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Union.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Union.md)

- JOIN

	- Hive Joins:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Joins.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Joins.md)
	- Join Optimization:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Join%20Optimization.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Join%20Optimization.md)
	- Outer Join Behavior:[https://github.com/ZGG2016/hive-website/blob/master/Resources%20for%20Contributors/Hive%20Design%20Docs/Hive%20Outer%20Join%20Behavior.md](https://github.com/ZGG2016/hive-website/blob/master/Resources%20for%20Contributors/Hive%20Design%20Docs/Hive%20Outer%20Join%20Behavior.md)

- TABLESAMPLE:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Sampling.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Sampling.md)

- Subqueries:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Sub%20Queries.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Sub%20Queries.md)

- Virtual Columns:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Virtual%20Columns.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Virtual%20Columns.md)

- Operators and UDFs:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Operators%20and%20UDFs.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Operators%20and%20UDFs.md)

- LATERAL VIEW:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Lateral%20View.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Lateral%20View.md)

- Windowing, OVER, and Analytics:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Windowing%20and%20Analytics%20Functions.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Windowing%20and%20Analytics%20Functions.md)

- Common Table Expressions:[https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Common%20Table%20Expression.md](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Common%20Table%20Expression.md)