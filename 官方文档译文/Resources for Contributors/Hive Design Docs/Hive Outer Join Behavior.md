# Hive Outer Join Behavior

[TOC]

> This document is based on a writeup of [DB2 Outer Join Behavior](http://www.ibm.com/developerworks/data/library/techarticle/purcell/0112purcell.html). The original HTML document is attached to the [Hive Design Docs](https://cwiki.apache.org/confluence/display/Hive/DesignDocs) and can be [downloaded here](https://cwiki.apache.org/confluence/download/attachments/27362075/OuterJoinBehavior.html).

本文档基于对 DB2 Outer Join 行为的总结。原 HTML 文档附在 Hive 设计文档中，可以在这里下载。

## 1、Definitions

| 定义 | 描述 |
|-|-|
|Preserved Row table  | The table in an Outer Join that must return all rows. For left outer joins this is the Left table, for right outer joins it is the Right table, and for full outer joins both tables are Preserved Row tables.【在Outer Join中，必须返回所有行的表。对于left outer joins，这是左表；对于right outer joins，这是右表;对于full outer joins，这两个表都是保留行表。】|
|Null Supplying table | This is the table that has nulls filled in for its columns in unmatched rows.In the non-full outer join case, this is the other table in the Join. For full outer joins both tables are also Null Supplying tables.【这个表在不匹配的行中为其列填充了空。在非full outer join的情况下，这是Join中的另一个表。对于full outer joins，两个表都是提供Null的表。】|
|During Join predicate| A predicate that is in the JOIN ON clause. For example, in 'R1 join R2 on R1.x = 5' the predicate 'R1.x = 5' is a During Join predicate.【位于JOIN ON子句中的谓词。例如，在`R1 join R2 on R1.x = 5`中，谓词`R1.x = 5`是一个During Join predicate。】|
|After Join predicate | A predicate that is in the WHERE clause.【位于WHERE子句中的谓词。】|

## 2、Predicate Pushdown Rules

> The logic can be summarized by these two rules:

规则有：

- During Join predicates 不能推过 Preserved Row tables
- After Join predicates 不能推过 Null Supplying table

> During Join predicates cannot be pushed past Preserved Row tables.

> After Join predicates cannot be pushed past Null Supplying tables.

> This captured in the following table:

这在下表中显示:

|                   |    Preserved Row tables   |    Null Supplying tables|
|---|---|:---|
|Join Predicate      |    Case J1: Not Pushed    |    Case J2: Pushed|
|Where Predicate     |    Case W1: Pushed        |    Case W2: Not Pushed|

See Examples below for illustrations of cases J1, J2, W1, and W2.

### 2.1、Hive Implementation

> Hive enforces the rules by these methods in the SemanticAnalyzer and JoinPPD classes:

Hive 通过在 SemanticAnalyzer 和 JoinPPD 类中的这些方法强制执行规则:

- 规则1:在 Plan Gen 的 QBJoinTree 构造过程中，parseJoinCondition() 逻辑应用此规则。
- 规则2:在 JoinPPD(Join谓词下推)期间，getQualifiedAliases() 逻辑应用此规则

> Rule 1: During QBJoinTree construction in Plan Gen, the parseJoinCondition() logic applies this rule.
> Rule 2: During JoinPPD (Join Predicate PushDown) the getQualifiedAliases() logic applies this rule.

## 3、Examples

> Given Src(Key String, Value String) the following Left Outer Join examples show that Hive has the correct behavior.

给定 Src(Key String, Value String)，下面的 Left Outer Join 示例表明 Hive 有正确的行为。

### 3.1、Case J1: Join Predicate on Preserved Row Table

join 谓词在 Preserved Row Table 上。

```sql
explain 
select s1.key, s2.key 
from src s1 left join src s2 on s1.key > '2';

STAGE DEPENDENCIES:
  Stage-1 is a root stage
  Stage-0 is a root stage

STAGE PLANS:
  Stage: Stage-1
    Map Reduce
      Alias -> Map Operator Tree:
	s1
	  TableScan
	    alias: s1
	    Reduce Output Operator
	      sort order:
	      tag: 0
	      value expressions:
		    expr: key
		    type: string
	s2
	  TableScan
	    alias: s2
	    Reduce Output Operator
	      sort order:
	      tag: 1
	      value expressions:
		    expr: key
		    type: string
      Reduce Operator Tree:
	Join Operator
	  condition map:
	       Left Outer Join0 to 1
	  condition expressions:
	    0 {VALUE._col0}
	    1 {VALUE._col0}
	  filter predicates:
	    0 {(VALUE._col0 > '2')}
	    1
	  handleSkewJoin: false
	  outputColumnNames: _col0, _col4
	  Select Operator
	    expressions:
		  expr: _col0
		  type: string
		  expr: _col4
		  type: string
	    outputColumnNames: _col0, _col1
	    File Output Operator
	      compressed: false
	      GlobalTableId: 0
	      table:
		  input format: org.apache.hadoop.mapred.TextInputFormat
		  output format: org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat
		  serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe

  Stage: Stage-0
    Fetch Operator
      limit: -1
```

### 3.2、Case J2: Join Predicate on Null Supplying Table

join 谓词在 Null Supplying Table 上。

```sql
explain 
select s1.key, s2.key 
from src s1 left join src s2 on s2.key > '2';

STAGE PLANS:
  Stage: Stage-1
    Map Reduce
      Alias -> Map Operator Tree:
	s1
	  TableScan
	    alias: s1
	    Reduce Output Operator
	      sort order:
	      tag: 0
	      value expressions:
		    expr: key
		    type: string
	s2
	  TableScan
	    alias: s2
	    Filter Operator
	      predicate:
		  expr: (key > '2')
		  type: boolean
	      Reduce Output Operator
		sort order:
		tag: 1
		value expressions:
		      expr: key
		      type: string
      Reduce Operator Tree:
	Join Operator
	  condition map:
	       Left Outer Join0 to 1
	  condition expressions:
	    0 {VALUE._col0}
	    1 {VALUE._col0}
	  handleSkewJoin: false
	  outputColumnNames: _col0, _col4
	  Select Operator
	    expressions:
		  expr: _col0
		  type: string
		  expr: _col4
		  type: string
	    outputColumnNames: _col0, _col1
	    File Output Operator
	      compressed: false
	      GlobalTableId: 0
	      table:
		  input format: org.apache.hadoop.mapred.TextInputFormat
		  output format: org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat
		  serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe

  Stage: Stage-0
    Fetch Operator
      limit: -1
```

### 3.3、Case W1: Where Predicate on Preserved Row Table

Where 谓词在 Preserved Row Table 上。

```sql
explain 
select s1.key, s2.key 
from src s1 left join src s2 
where s1.key > '2';

STAGE PLANS:
  Stage: Stage-1
    Map Reduce
      Alias -> Map Operator Tree:
	s1
	  TableScan
	    alias: s1
	    Filter Operator
	      predicate:
		  expr: (key > '2')
		  type: boolean
	      Reduce Output Operator
		sort order:
		tag: 0
		value expressions:
		      expr: key
		      type: string
	s2
	  TableScan
	    alias: s2
	    Reduce Output Operator
	      sort order:
	      tag: 1
	      value expressions:
		    expr: key
		    type: string
      Reduce Operator Tree:
	Join Operator
	  condition map:
	       Left Outer Join0 to 1
	  condition expressions:
	    0 {VALUE._col0}
	    1 {VALUE._col0}
	  handleSkewJoin: false
	  outputColumnNames: _col0, _col4
	  Select Operator
	    expressions:
		  expr: _col0
		  type: string
		  expr: _col4
		  type: string
	    outputColumnNames: _col0, _col1
	    File Output Operator
	      compressed: false
	      GlobalTableId: 0
	      table:
		  input format: org.apache.hadoop.mapred.TextInputFormat
		  output format: org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat
		  serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe

  Stage: Stage-0
    Fetch Operator
      limit: -1
```

### 3.4、Case W2: Where Predicate on Null Supplying Table

Where 谓词在 Null Supplying Table 上。

```sql
explain
select s1.key, s2.key 
from src s1 left join src s2 
where s2.key > '2';

STAGE PLANS:
  Stage: Stage-1
    Map Reduce
      Alias -> Map Operator Tree:
	s1
	  TableScan
	    alias: s1
	    Reduce Output Operator
	      sort order:
	      tag: 0
	      value expressions:
		    expr: key
		    type: string
	s2
	  TableScan
	    alias: s2
	    Reduce Output Operator
	      sort order:
	      tag: 1
	      value expressions:
		    expr: key
		    type: string
      Reduce Operator Tree:
	Join Operator
	  condition map:
	       Left Outer Join0 to 1
	  condition expressions:
	    0 {VALUE._col0}
	    1 {VALUE._col0}
	  handleSkewJoin: false
	  outputColumnNames: _col0, _col4
	  Filter Operator
	    predicate:
		expr: (_col4 > '2')
		type: boolean
	    Select Operator
	      expressions:
		    expr: _col0
		    type: string
		    expr: _col4
		    type: string
	      outputColumnNames: _col0, _col1
	      File Output Operator
		compressed: false
		GlobalTableId: 0
		table:
		    input format: org.apache.hadoop.mapred.TextInputFormat
		    output format: org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat
		    serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe

  Stage: Stage-0
    Fetch Operator
      limit: -1

```

-------------------------------------------------

```sql
-- 建表
hive> create table apps(
    >     id int,
    >     app_name string
    > )ROW FORMAT DELIMITED
    > FIELDS TERMINATED BY ',';
OK

hive> create table access_log(
    >     aid int,
    >     site_id int,
    >     count int
    > )ROW FORMAT DELIMITED
    > FIELDS TERMINATED BY ',';
OK

-- 导入数据
hive> load data local inpath '/root/access_log.txt' OVERWRITE into table access_log;
Loading data to table default.access_log
OK

hive> load data local inpath '/root/apps.txt' into table apps;
Loading data to table default.apps
OK

-- 查看表
hive> select * from access_log;
OK
1       1       45
2       3       100
3       1       230
4       2       10
5       5       205
6       4       13

hive> select * from apps;
OK
1       'QQ APP'
2       'WEIBO APP'
3       'TAOBAO APP'
4       'FACEBOOK APP'
5       'GOOGLE'

-- Case J1: Join Predicate on Preserved Row Table
-- explain 
-- select s1.key, s2.key 
-- from src s1 left join src s2 on s1.key > '2';
hive> select a.id,a.app_name,b.site_id from apps a left join access_log b on a.id>2;
Warning: Map Join MAPJOIN[9][bigTable=?] in task 'Stage-3:MAPRED' is a cross product
....
Total jobs = 1
2021-01-16 18:38:47     Starting to launch local task to process map join;
....
Number of reduce tasks is set to 0 since there is no reduce operator
...
Total MapReduce CPU Time Spent: 2 seconds 930 msec
OK
1       'QQ APP'        NULL
2       'WEIBO APP'     NULL
3       'TAOBAO APP'    1
3       'TAOBAO APP'    3
3       'TAOBAO APP'    1
3       'TAOBAO APP'    2
3       'TAOBAO APP'    5
3       'TAOBAO APP'    4
4       'FACEBOOK APP'  1
4       'FACEBOOK APP'  3
4       'FACEBOOK APP'  1
4       'FACEBOOK APP'  2
4       'FACEBOOK APP'  5
4       'FACEBOOK APP'  4
5       'GOOGLE'        1
5       'GOOGLE'        3
5       'GOOGLE'        1
5       'GOOGLE'        2
5       'GOOGLE'        5
5       'GOOGLE'        4
Time taken: 75.867 seconds, Fetched: 20 row(s)

-- Case J2: Join Predicate on Null Supplying Table
-- explain 
-- select s1.key, s2.key 
-- from src s1 left join src s2 on s2.key > '2';
hive> select a.id,a.app_name,b.site_id from apps a left join access_log b on b.site_id>2;
Warning: Map Join MAPJOIN[11][bigTable=?] in task 'Stage-3:MAPRED' is a cross product
....
Total jobs = 1
....
Number of reduce tasks is set to 0 since there is no reduce operator
....
Total MapReduce CPU Time Spent: 2 seconds 770 msec
OK
1       'QQ APP'        3
1       'QQ APP'        5
1       'QQ APP'        4
2       'WEIBO APP'     3
2       'WEIBO APP'     5
2       'WEIBO APP'     4
3       'TAOBAO APP'    3
3       'TAOBAO APP'    5
3       'TAOBAO APP'    4
4       'FACEBOOK APP'  3
4       'FACEBOOK APP'  5
4       'FACEBOOK APP'  4
5       'GOOGLE'        3
5       'GOOGLE'        5
5       'GOOGLE'        4
Time taken: 77.662 seconds, Fetched: 15 row(s)

-- Where Predicate on Preserved Row Table
-- explain 
-- select s1.key, s2.key 
-- from src s1 left join src s2 
-- where s1.key > '2';
hive> select a.id,a.app_name,b.site_id from apps a left join access_log b where a.id>2;
Warning: Map Join MAPJOIN[11][bigTable=?] in task 'Stage-3:MAPRED' is a cross product
....
Total jobs = 1
....
Number of reduce tasks is set to 0 since there is no reduce operator
....
Total MapReduce CPU Time Spent: 3 seconds 570 msec
OK
3       'TAOBAO APP'    1
3       'TAOBAO APP'    3
3       'TAOBAO APP'    1
3       'TAOBAO APP'    2
3       'TAOBAO APP'    5
3       'TAOBAO APP'    4
4       'FACEBOOK APP'  1
4       'FACEBOOK APP'  3
4       'FACEBOOK APP'  1
4       'FACEBOOK APP'  2
4       'FACEBOOK APP'  5
4       'FACEBOOK APP'  4
5       'GOOGLE'        1
5       'GOOGLE'        3
5       'GOOGLE'        1
5       'GOOGLE'        2
5       'GOOGLE'        5
5       'GOOGLE'        4
Time taken: 67.147 seconds, Fetched: 18 row(s)

-- Where Predicate on Null Supplying Table
-- explain
-- select s1.key, s2.key 
-- from src s1 left join src s2 
-- where s2.key > '2';
hive> select a.id,a.app_name,b.site_id from apps a left join access_log b where b.site_id>2;
Warning: Map Join MAPJOIN[11][bigTable=?] in task 'Stage-3:MAPRED' is a cross product
Query ID = root_20210116185548_a9d06daf-66bd-4a39-bd67-ee5adaeffd97
Total jobs = 1
....
Number of reduce tasks is set to 0 since there is no reduce operator
....
Total MapReduce CPU Time Spent: 3 seconds 680 msec
OK
1       'QQ APP'        3
2       'WEIBO APP'     3
3       'TAOBAO APP'    3
4       'FACEBOOK APP'  3
5       'GOOGLE'        3
1       'QQ APP'        5
2       'WEIBO APP'     5
3       'TAOBAO APP'    5
4       'FACEBOOK APP'  5
5       'GOOGLE'        5
1       'QQ APP'        4
2       'WEIBO APP'     4
3       'TAOBAO APP'    4
4       'FACEBOOK APP'  4
5       'GOOGLE'        4
Time taken: 66.341 seconds, Fetched: 15 row(s)
```