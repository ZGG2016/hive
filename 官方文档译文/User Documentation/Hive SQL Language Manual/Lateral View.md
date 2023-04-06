# Lateral View

[TOC]

## 1、Lateral View Syntax

	lateralView: LATERAL VIEW udtf(expression) tableAlias AS columnAlias (',' columnAlias)*
	fromClause: FROM baseTable (lateralView)*

## 2、Description

> Lateral view is used in conjunction with user-defined table generating functions such as explode(). As mentioned in [Built-in Table-Generating Functions](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF#LanguageManualUDF-Built-inTable-GeneratingFunctions(UDTF)), a UDTF generates zero or more output rows for each input row. A lateral view first applies the UDTF to each row of base table and then joins resulting output rows to the input rows to form a virtual table having the supplied table alias.

侧视图与 UDTF(如`explode()`)一起使用。

UDTF 为每个输入行生成0或更多个输出行。

**侧视图首先将 UDTF 应用于基表的每一行，然后结果输出行和输入行 join，从而形成具有所提供的表别名的虚拟表**。

> Version:Prior to Hive 0.6.0, lateral view did not support the predicate push-down optimization. In Hive 0.5.0 and earlier, if you used a WHERE clause your query may not have compiled. A workaround was to add set hive.optimize.ppd=false; before your query. The fix was made in Hive 0.6.0; see [https://issues.apache.org/jira/browse/HIVE-1056](https://issues.apache.org/jira/browse/HIVE-1056): Predicate push down does not work with UDTF's.

版本：对于 Hive 0.6.0 之前的版本，侧视图不支持谓词下推优化。

在 Hive 0.5.0 及更早的版本，如果你使用 WHERE 子句，你的查询可能不会编译。一种解决方法是，在你的在查询之前，添加 `set hive.optimize.ppd=false`，修复是在 Hive 0.6.0。见 HIVE-1056：谓词下推不能和 UDTF 一起使用。

> Version:From Hive 0.12.0, column aliases can be omitted. In this case, aliases are inherited from field names of StructObjectInspector which is returned from UTDF.

版本：从 Hive 0.12.0，列别名可以省略。在这种情况下，别名是从 UTDF 返回的 StructObjectInspector 的字段名继承的。

## 3、Example

> Consider the following base table named pageAds. It has two columns: pageid (name of the page) and adid_list (an array of ads appearing on the page):

考虑下列名为 pageAds 的基表。它有两列：pageid(页面id)和adid_list(出现在页面的广告的数组)：

Column Name | Column Type
---|:---
pageid | STRING
adid_list | Array<int>

> An example table with two rows:

表的两列：

pageid | adid_list
---|:---
front_page | [1, 2, 3]
contact_page | [3, 4, 5]

> and the user would like to count the total number of times an ad appears across all pages.

用户想要统计一个广告在所有页面出现的次数。

> A lateral view with [explode()](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF#LanguageManualUDF-explode) can be used to convert adid_list into separate rows using the query:

可以结合使用侧视图和explode()，来将 adid_list 转换成独立的行：

```sql
SELECT pageid, adid FROM pageAds LATERAL VIEW explode(adid_list) adTable AS adid;
```

> The resulting output will be

结果输出就是：

pageid | adid(int)
---|:---
"front_page" | 1
"front_page" | 2
"front_page" | 3
"contact_page" | 3
"contact_page" | 4
"contact_page" | 5

> Then in order to count the number of times a particular ad appears, count/group by can be used:

然后为了统计一个广告出现的次数，可以使用 `count/group by`：

```sql
SELECT adid, count(1)
FROM pageAds LATERAL VIEW explode(adid_list) adTable AS adid
GROUP BY adid;
```

int adid | count(1)
---|:---
1 | 1
2 | 1
3 | 2
4 | 1
5 | 1

## 4、Multiple Lateral Views

> A FROM clause can have multiple LATERAL VIEW clauses. Subsequent LATERAL VIEWS can reference columns from any of the tables appearing to the left of the LATERAL VIEW.

FROM 子句可以有多个 `LATERAL VIEW` 子句。随后的侧视图可以引用侧视图左侧出现的任何表中的列。

> For example, the following could be a valid query:

例如，下面是有效查询：

```sql
SELECT * FROM exampleTable
LATERAL VIEW explode(col1) myTable1 AS myCol1
LATERAL VIEW explode(myCol1) myTable2 AS myCol2;
```

> LATERAL VIEW clauses are applied in the order that they appear. For example with the following base table:

**LATERAL VIEW 子句按出现的顺序执行**。以下面的基表举例：

Array<int> col1 | Array<string> col2
---|:---
[1, 2] | [a", "b", "c"]
[3, 4] | [d", "e", "f"]

> The query:

```sql
SELECT myCol1, col2 FROM baseTable
LATERAL VIEW explode(col1) myTable1 AS myCol1;
```

> Will produce:

产生：

int mycol1 | Array<string> col2
---|:---
1 | [a", "b", "c"]
2 | [a", "b", "c"]
3 | [d", "e", "f"]
4 | [d", "e", "f"]

> A query that adds an additional LATERAL VIEW:

再添加一个 LATERAL VIEW:

```sql
SELECT myCol1, myCol2 FROM baseTable
LATERAL VIEW explode(col1) myTable1 AS myCol1
LATERAL VIEW explode(col2) myTable2 AS myCol2;
```

> Will produce:

产生：

int myCol1 | string myCol2
---|:---
1 | "a"
1 | "b"
1 | "c"
2 | "a"
2 | "b"
2 | "c"
3 | "d"
3 | "e"
3 | "f"
4 | "d"
4 | "e"
4 | "f"

## 5、Outer Lateral Views

> Version：Introduced in Hive version 0.12.0

版本：在 Hive 0.12.0 引入。

> The user can specify the optional OUTER keyword to generate rows even when a LATERAL VIEW usually would not generate a row. This happens when the UDTF used does not generate any rows which happens easily with explode when the column to explode is empty. In this case the source row would never appear in the results. OUTER can be used to prevent that and rows will be generated with NULL values in the columns coming from the UDTF.

用户可以执行可选的 OUTER 关键字，来产生行，即使一个 LATERAL VIEW 通常不会产生一行。

**当要 explode 的列为空时，使用的 UDTF 不生成任何行时**。

在这种情况下，原行永远不会出现在结果中。OUTER 可用于防止这种情况发生，并且**将在来自 UDTF 的列中生成带有 NULL 的行**。

> For example, the following query returns an empty result:

例如，下面的查询返回一个空结果：

```sql
SELEC * FROM src LATERAL VIEW explode(array()) C AS a limit 10;
```

> But with the OUTER keyword

但，添加了 OUTER：

```sql
SELECT * FROM src LATERAL VIEW OUTER explode(array()) C AS a limit 10;
```

> it will produce:

产生：

	238 val_238 NULL
	86 val_86 NULL
	311 val_311 NULL
	27 val_27 NULL
	165 val_165 NULL
	409 val_409 NULL
	255 val_255 NULL
	278 val_278 NULL
	98 val_98 NULL
	...