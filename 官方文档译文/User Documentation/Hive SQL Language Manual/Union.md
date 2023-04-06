# Union

[TOC]

## 1、Union Syntax

	select_statement UNION [ALL | DISTINCT] select_statement UNION [ALL | DISTINCT] select_statement ...

> UNION is used to combine the result from multiple SELECT statements into a single result set.

UNION 用于**将多个 SELECT 语句的结果组合为单个结果集**。

> Hive versions prior to [1.2.0](https://issues.apache.org/jira/browse/HIVE-9039) only support UNION ALL (bag union), in which duplicate rows are not eliminated.

- Hive 1.2.0 之前的版本只支持 **UNION ALL (bag UNION)，其中重复的行不会被删除**。

> In Hive 1.2.0 and later, the default behavior for UNION is that duplicate rows are removed from the result. The optional DISTINCT keyword has no effect other than the default because it also specifies duplicate-row removal. With the optional ALL keyword, duplicate-row removal does not occur and the result includes all matching rows from all the SELECT statements.

- 在 Hive 1.2.0 和更高版本中，**UNION 的默认行为是删除结果中的重复行。可选的 DISTINCT** 关键字除了默认值之外没有其他作用，因为它还指定了删除重复行。使用可选的 ALL 关键字，不会删除重复行，结果包括所有 SELECT 语句中所有匹配的行。

> You can mix UNION ALL and UNION DISTINCT in the same query. Mixed UNION types are treated such that a DISTINCT union overrides any ALL union to its left. A DISTINCT union can be produced explicitly by using UNION DISTINCT or implicitly by using UNION with no following DISTINCT or ALL keyword.

可以**在同一个查询中混合使用 UNION ALL 和 UNION DISTINCT**。

混合 UNION 类型的处理方式是，**一个 DISTINCT union 覆盖其左边的任意 ALL union**。可显式地使用 UNION DISTINCT 产生 DISTINCT union，也可隐式地使用不后跟 DISTINCT 或 ALL 关键字的 UNION 产生明显的联合。

> The number and names of columns returned by each select_statement have to be the same. Otherwise, a schema error is thrown.

**每个 select_statement 返回的列的数量和名称必须相同**。否则，将抛出模式错误。

### 1.1、UNION within a FROM Clause

> If some additional processing has to be done on the result of the UNION, the entire statement expression can be embedded in a FROM clause like below:

**如果必须对 UNION 的结果进行一些额外的处理，则可以将整个语句表达式嵌入 FROM 子句中**，如下所示:

```sql
SELECT *
FROM (
  select_statement
  UNION ALL
  select_statement
) unionResult
```

> For example, if we suppose there are two different tables that track which user has published a video and which user has published a comment, the following query joins the results of a UNION ALL with the user table to create a single annotated stream for all the video publishing and comment publishing events:

例如，如果我们有两个不同的表，跟踪哪个用户发布了一个视频，哪个用户发表了评论，下面的查询将连接 user 表和 UNION ALL 的结果，创建了一个带注释的所有视频发布和评论发布事件流:

```sql
SELECT u.id, actions.date
FROM (
    SELECT av.uid AS uid
    FROM action_video av
    WHERE av.date = '2008-06-03'
    UNION ALL
    SELECT ac.uid AS uid
    FROM action_comment ac
    WHERE ac.date = '2008-06-03'
 ) actions JOIN users u ON (u.id = actions.uid)
```

### 1.2、Unions in DDL and Insert Statements

> Unions can be used in views, inserts, and CTAS (create table as select) statements. A query can contain multiple UNION clauses, as shown in the syntax above.

**Unions 可以在视图、insert 和 CTAS(create table as select)语句中使用**。

一个查询可以包含多个 UNION 子句，如上面的语法所示。

### 1.3、Applying Subclauses

> To apply ORDER BY, SORT BY, CLUSTER BY, DISTRIBUTE BY or LIMIT to an individual SELECT, place the clause inside the parentheses that enclose the SELECT:

为了应用 `ORDER BY`、`SORT BY`、`CLUSTER BY`、`DISTRIBUTE BY`或`LIMIT` 到一个独立的 SELECT，将子句放在包含 SELECT 语句的括号内:

```sql
SELECT key FROM (SELECT key FROM src ORDER BY key LIMIT 10)subq1
UNION
SELECT key FROM (SELECT key FROM src1 ORDER BY key LIMIT 10)subq2
```

> To apply an ORDER BY, SORT BY, CLUSTER BY, DISTRIBUTE BY or LIMIT clause to the entire UNION result, place the ORDER BY, SORT BY, CLUSTER BY, DISTRIBUTE BY or LIMIT after the last one. The following example uses both ORDER BY and LIMIT clauses:

为了应用 `ORDER BY`、`SORT BY`、`CLUSTER BY`、`DISTRIBUTE BY`或`LIMIT` 到整个 UNION 结果上，在最后一个后放置这些语句。如：

```sql
SELECT key FROM src
UNION
SELECT key FROM src1 
ORDER BY key LIMIT 10
```

### 1.4、Column Aliases for Schema Matching

> UNION expects the same schema on both sides of the expression list. As a result, the following query may fail with an error message such as "FAILED: SemanticException 4:47 Schema of both sides of union should match."

UNION 要求在两段的表达式列表上具有相同的模式。下面的查询就会失败，产生错误消息：`FAILED: SemanticException 4:47 Schema of both sides of union should match.`

```sql
INSERT OVERWRITE TABLE target_table
  SELECT name, id, category FROM source_table_1
  UNION ALL
  SELECT name, id, "Category159" FROM source_table_2
```

> In such cases, column aliases can be used to force equal schemas:

在这种情况下，可以使用列别名来强制实现相同的模式:

```sql
INSERT OVERWRITE TABLE target_table
  SELECT name, id, category FROM source_table_1
  UNION ALL
  SELECT name, id, "Category159" as category FROM source_table_2
```

### 1.5、Column Type Conversion

> Before [HIVE-14251](https://issues.apache.org/jira/browse/HIVE-14251) in release 2.2.0, Hive tries to perform implicit conversion across Hive type groups. With the change of HIVE-14251, Hive will only perform implicit conversion within each type group including string group, number group or date group, not across groups. In order to union the types from different groups such as a string type and a date type, an explicit cast from string to date or from date to string is needed in the query.

在 Hive-14251 发布 2.2.0 版本之前，Hive 尝试在 Hive 类型分组之间执行隐式转换。

在 Hive-14251 更改后，Hive 只会在每个类型分组内(包括字符串分组、数字分组或日期分组)执行隐式转换，而不会跨组执行。

为了 union 来自不同组(如字符串类型和日期类型)的类型，需要在查询中进行从字符串到日期或从日期到字符串的显式转换。

```sql
SELECT name, id, cast('2001-01-01' as date) d FROM source_table_1
UNION ALL
SELECT name, id, hiredate as d FROM source_table_2
```

### 1.6、Version Information

> Version information

版本信息

> In Hive 0.12.0 and earlier releases, unions can only be used within a subquery such as "SELECT * FROM (select_statement UNION ALL select_statement UNION ALL ...) unionResult".

在 Hive 0.12.0 和更早的版本中，UNION 只能在一个子查询内使用，例如 `SELECT * FROM (select_statement UNION ALL select_statement UNION ALL ...) unionResult`。

> As of Hive 0.13.0, unions can also be used in a top-level query: "select_statement UNION ALL select_statement UNION ALL ...". (See [HIVE-6189](https://issues.apache.org/jira/browse/HIVE-6189).)

在 Hive 0.13.0 版本中，UNION 也可以用于顶级查询，例如 `select_statement UNION ALL select_statement UNION ALL ...`。

> Before Hive 1.2.0, only UNION ALL (bag union) is supported. UNION (or UNION DISTINCT) is supported since Hive 1.2.0. (See [HIVE-9039](https://issues.apache.org/jira/browse/HIVE-6189).)

Hive 1.2.0 之前，只支持 UNION ALL (bag UNION)。自 Hive 1.2.0 以来，支持 UNION(或UNION DISTINCT)。