# Common Table Expression

> A Common Table Expression (CTE) is a temporary result set derived from a simple query specified in a WITH clause, which immediately precedes a SELECT or INSERT keyword.  The CTE is defined only within the execution scope of a single statement.  One or more CTEs can be used in a Hive [SELECT](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Select), [INSERT](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DML#LanguageManualDML-InsertingdataintoHiveTablesfromqueries), [CREATE TABLE AS SELECT](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTableAsSelect(CTAS)), or [CREATE VIEW AS SELECT statement](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateView).

CTE 是在 WITH 子句中指定的一个简单查询派生出的临时结果集，该子句紧接在 SELECT 或 INSERT 关键字之前。

CTE 仅在单个语句的执行范围内定义。

在 Hive 的 SELECT、INSERT、CREATE TABLE AS SELECT 或 CREATE VIEW AS SELECT 语句中可以使用一个或多个 CTEs。

> Version:Common Table Expressions are added in Hive 0.13.0 with [HIVE-1180](https://issues.apache.org/jira/browse/HIVE-1180).

## 1、Common Table Expression Syntax

	withClause: cteClause (, cteClause)*
	cteClause: cte_name AS (select statment)

**Additional Grammar Rules**

- 在子查询块中不支持 WITH 子句

- 在 Views、CTAS 和 INSERT 语句中支持 CTEs

- 不支持递归查询

> The WITH clause is not supported within SubQuery Blocks
> CTEs are supported in Views, CTAS and INSERT statements.
> [Recursive Queries](http://wiki.postgresql.org/wiki/CTEReadme#Parsing_recursive_queries) are not supported.

## 2、Examples

CTE 在 Select 语句中

```sql
with q1 as ( select key from src where key = '5')
select *
from q1;
 
-- from style
with q1 as (select * from src where key= '5')
from q1
select *;
  
-- chaining CTEs
with q1 as ( select key from q2 where key = '5'),
q2 as ( select key from src where key = '5')
select * from (select key from q1) a;
  
-- union example
with q1 as (select * from src where key= '5'),
q2 as (select * from src s2 where key = '4')
select * from q1 union all select * from q2;
```

CTE in Views, CTAS, and Insert Statements

```sql
-- insert example
create table s1 like src;
with q1 as ( select key, value from src where key = '5')
from q1
insert overwrite table s1
select *;
 
-- ctas example
create table s2 as
with q1 as ( select key from src where key = '4')
select * from q1;
 
-- view example
create view v1 as
with q1 as ( select key from src where key = '5')
select * from q1;
select * from v1;
  
-- view example, name collision
create view v1 as
with q1 as ( select key from src where key = '5')
select * from q1;
with q1 as ( select key from src where key = '4')
select * from v1;
```

> In the second View example, a query's CTE is different from the CTE used when creating the view. The result will contain rows with key = '5' because in the view's query statement the CTE defined in the view definition takes effect.

在第二个视图示例中，查询的 CTE 与创建视图时使用的 CTE 不同。结果将包含 `key = '5'`的行，因为在视图的查询语句中，视图定义中定义的 CTE 生效。

> Also see this JIRA:

> [HIVE-1180](https://issues.apache.org/jira/browse/HIVE-1180) Support Common Table Expressions (CTEs) in Hive