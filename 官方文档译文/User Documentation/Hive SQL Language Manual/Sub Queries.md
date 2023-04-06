# LanguageManual SubQueries

[TOC]

## 1、Subqueries in the FROM Clause

	SELECT ... FROM (subquery) name ...
	SELECT ... FROM (subquery) AS name ...   (Note: Only valid starting with Hive 0.13.0)

> Hive supports subqueries only in the FROM clause (through Hive 0.12). The subquery has to be given a name because every table in a FROM clause must have a name. Columns in the subquery select list must have unique names. The columns in the subquery select list are available in the outer query just like columns of a table. The subquery can also be a query expression with UNION. Hive supports arbitrary levels of subqueries.

Hive **只支持 FROM 子句中的子查询(到Hive 0.12)**。

**必须给子查询一个名称**，因为 FROM 子句中的每个表都必须有一个名称。

**子查询 select 列表中的列必须具有惟一的名称**。子查询 select 列表中的列在外部查询中可用，就像表中的列一样。

子查询也可以是带有 UNION 的查询表达式。

Hive **支持任意级别的子查询**。

> The optional keyword "AS" can be included before the subquery name in Hive 0.13.0 and later versions ([HIVE-6519](https://issues.apache.org/jira/browse/HIVE-6519)).

在 Hive 0.13.0 及后续版本中，可选关键字 `AS` 可以包含在子查询名称之前。

> Example with simple subquery:

```sql
SELECT col
FROM (
  SELECT a+b AS col
  FROM t1
) t2
```

> Example with subquery containing a UNION ALL:

```sql
SELECT t3.col
FROM (
  SELECT a+b AS col
  FROM t1
  UNION ALL
  SELECT c+d AS col
  FROM t2
) t3
```

## 2、Subqueries in the WHERE Clause

> As of [Hive 0.13](https://issues.apache.org/jira/browse/HIVE-784) some types of subqueries are supported in the WHERE clause. Those are queries where the result of the query can be treated as a constant for IN and NOT IN statements (called uncorrelated subqueries because the subquery does not reference columns from the parent query):

Hive 0.13 中，支持在 WHERE 子句中的一些子查询。

在这些查询中，**查询的结果可以被视为 IN 和 NOT IN 语句中的常量**(称为不相关子查询，因为子查询没有引用父查询的列)

```sql
SELECT *
FROM A
WHERE A.a IN (SELECT foo FROM B);
```

> The other supported types are EXISTS and NOT EXISTS subqueries:

其他支持的类型就是 **EXISTS 和 NOT EXISTS 子查询**：

```sql
SELECT A
FROM T1
WHERE EXISTS (SELECT B FROM T2 WHERE T1.X = T2.Y)
```

> There are a few limitations:

存在一些限制：

- 这些子查询仅支持在表达式的右侧。
- IN/NOT IN 子查询只能选择单个列。
- EXISTS/NOT EXISTS 必须有一个或多个相关谓词。
- 在子查询的 WHERE 子句中，仅支持对父查询的引用。

> These subqueries are only supported on the right-hand side of an expression.
> IN/NOT IN subqueries may only select a single column.
> EXISTS/NOT EXISTS must have one or more correlated predicates.
> References to the parent query are only supported in the WHERE clause of the subquery.