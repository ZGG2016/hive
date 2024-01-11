# explain执行计划分析

[TOC]

## explain

```sql
hive (default)> desc emp;
OK
col_name        data_type       comment
empno                   int                                         
ename                   string                                      
job                     string                                      
mgr                     int                                         
hiredate                string                                      
sal                     double                                      
comm                    double                                      
deptno                  int 
```

```sql
hive (default)> explain select deptno, avg(sal) avg_sal from emp group by deptno;
OK
Explain
STAGE DEPENDENCIES:
  Stage-1 is a root stage
  Stage-0 depends on stages: Stage-1   【Stage-0依赖Stage-1，所以先执行Stage-1】

STAGE PLANS:
  Stage: Stage-1
    Map Reduce  【执行mapreduce】
      Map Operator Tree:  【map阶段】
          TableScan  【扫描emp表，读取数据】
            alias: emp  
            Statistics: Num rows: 1 Data size: 6560 Basic stats: COMPLETE Column stats: NONE
            Select Operator 【select出需要的字段】
              expressions: sal (type: double), deptno (type: int)
              outputColumnNames: sal, deptno
              Statistics: Num rows: 1 Data size: 6560 Basic stats: COMPLETE Column stats: NONE
              Group By Operator 【执行分组聚合，统计求平均 需要的 总和 与 数量  map端聚合】
                aggregations: sum(sal), count(sal)
                keys: deptno (type: int)  【分组键】
                mode: hash   【通过计算key的哈希值分组】
                outputColumnNames: _col0, _col1, _col2   【deptno, sum(sal), count(sal)】
                Statistics: Num rows: 1 Data size: 6560 Basic stats: COMPLETE Column stats: NONE
                Reduce Output Operator 【由这个算子把数据划分到不同reducers中】
                  key expressions: _col0 (type: int)
                  sort order: +   【"+"表示按照一列升序排序】
                  Map-reduce partition columns: _col0 (type: int)  【分区键】
                  Statistics: Num rows: 1 Data size: 6560 Basic stats: COMPLETE Column stats: NONE
                  value expressions: _col1 (type: double), _col2 (type: bigint)
      Execution mode: vectorized
      Reduce Operator Tree: 【reduce阶段】
        Group By Operator  【聚合分区间的数据】
          aggregations: sum(VALUE._col0), count(VALUE._col1) 【为什么是count，不是sum？】
          keys: KEY._col0 (type: int)
          mode: mergepartial
          outputColumnNames: _col0, _col1, _col2
          Statistics: Num rows: 1 Data size: 6560 Basic stats: COMPLETE Column stats: NONE
          Select Operator  【计算出平均】
            expressions: _col0 (type: int), (_col1 / _col2) (type: double)
            outputColumnNames: _col0, _col1
            Statistics: Num rows: 1 Data size: 6560 Basic stats: COMPLETE Column stats: NONE
            File Output Operator  【两个阶段间的中间数据放到临时文件】
              compressed: false
              Statistics: Num rows: 1 Data size: 6560 Basic stats: COMPLETE Column stats: NONE
              table:
                  input format: org.apache.hadoop.mapred.SequenceFileInputFormat
                  output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat
                  serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe

  Stage: Stage-0
    Fetch Operator 【获取上个阶段的数据】
      limit: -1
      Processor Tree:
        ListSink

```

## CBO

输出 Calcite 优化器产生的执行计划

```sql
0: jdbc:hive2://localhost:10000> select * from t1;
+--------+----------+---------+------------+
| t1.id  | t1.name  | t1.age  | t1.salary  |
+--------+----------+---------+------------+
| 4      | dd       | 34      | 7000.0     |
| 1      | aa       | 24      | 3000.0     |
| 2      | bb       | 33      | 8000.0     |
| 3      | cc       | 44      | 10000.0    |
| 4      | dd       | 34      | 7000.0     |
+--------+----------+---------+------------+

0: jdbc:hive2://localhost:10000> SELECT * FROM D;
+-------+---------+
| d.id  | d.dept  |
+-------+---------+
| 1     | HR      |
| 2     | IT      |
+-------+---------+

0: jdbc:hive2://localhost:10000> explain cbo select id,avg(salary) from t1 group by id;
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| CBO PLAN:                                          |
| HiveProject(id=[$0], _o__c1=[/($1, $2)])           |
|   HiveAggregate(group=[{0}], agg#0=[sum($3)], agg#1=[count($3)]) |
|     HiveTableScan(table=[[default, t1]], table:alias=[t1]) |
|                                                    |
+----------------------------------------------------+

-- COST 选项打印执行计划和使用 Calcite 的默认成本模型计算的成本信息
0: jdbc:hive2://localhost:10000> explain cbo cost select id,avg(salary) from t1 group by id;
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| CBO PLAN:                                          |
| HiveProject(id=[$0], _o__c1=[/($1, $2)]): rowcount = 1.0, cumulative cost = {7.25 rows, 8.0 cpu, 0.0 io}, id = 642 |
|   HiveAggregate(group=[{0}], agg#0=[sum($3)], agg#1=[count($3)]): rowcount = 1.0, cumulative cost = {6.25 rows, 6.0 cpu, 0.0 io}, id = 657 |
|     HiveTableScan(table=[[default, t1]], table:alias=[t1]): rowcount = 5.0, cumulative cost = {5.0 rows, 6.0 cpu, 0.0 io}, id = 624 |
|                                                    |
+----------------------------------------------------+

-- JOINCOST 选项打印执行计划和使用 Calcite 的用于联接重排的模型计算的成本信息
0: jdbc:hive2://localhost:10000> explain cbo JOINCOST select dept,sum(salary) from t1 join d on t1.id=d.id group by dept;
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| CBO PLAN:                                          |
| HiveAggregate(group=[{3}], agg#0=[sum($1)]): rowcount = 1.9375, cumulative cost = {7.0 rows, 0.0 cpu, 0.0 io}, id = 836 |
|   HiveJoin(condition=[=($0, $2)], joinType=[inner], algorithm=[none], cost=[{7.0 rows, 0.0 cpu, 0.0 io}]): rowcount = 5.0, cumulative cost = {7.0 rows, 0.0 cpu, 0.0 io}, id = 799 |
|     HiveProject(id=[$0], salary=[$3]): rowcount = 5.0, cumulative cost = {0.0 rows, 0.0 cpu, 0.0 io}, id = 792 |
|       HiveFilter(condition=[IS NOT NULL($0)]): rowcount = 5.0, cumulative cost = {0.0 rows, 0.0 cpu, 0.0 io}, id = 790 |
|         HiveTableScan(table=[[default, t1]], table:alias=[t1]): rowcount = 5.0, cumulative cost = {0}, id = 754 |
|     HiveProject(id=[$0], dept=[$1]): rowcount = 2.0, cumulative cost = {0.0 rows, 0.0 cpu, 0.0 io}, id = 797 |
|       HiveFilter(condition=[IS NOT NULL($0)]): rowcount = 2.0, cumulative cost = {0.0 rows, 0.0 cpu, 0.0 io}, id = 795 |
|         HiveTableScan(table=[[default, d]], table:alias=[d]): rowcount = 2.0, cumulative cost = {0}, id = 757 |
|                                                    |
+----------------------------------------------------+
```

## VECTORIZATION

矢量化

```sql
0: jdbc:hive2://localhost:10000> explain VECTORIZATION select id,avg(salary) from t1 group by id;
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| PLAN VECTORIZATION:                                |
|   enabled: true                                    |
|   enabledConditionsMet: [hive.vectorized.execution.enabled IS true] |
|                                                    |
| STAGE DEPENDENCIES:                                |
|   Stage-1 is a root stage                          |
|   Stage-0 depends on stages: Stage-1               |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-1                                   |
|     Tez                                            |
|       DagId: hive_20240111025450_00e2b323-04fb-4448-be4c-fbcbbab91437:10 |
|       Edges:                                       |
|         Reducer 2 <- Map 1 (SIMPLE_EDGE)           |
|       DagName: hive_20240111025450_00e2b323-04fb-4448-be4c-fbcbbab91437:10 |
|       Vertices:                                    |
|         Map 1                                      |
|             Map Operator Tree:                     |
|                 TableScan                          |
|                   alias: t1                        |
|                   Statistics: Num rows: 5 Data size: 60 Basic stats: COMPLETE Column stats: COMPLETE |
|                   Select Operator                  |
|                     expressions: id (type: int), salary (type: double) |
|                     outputColumnNames: id, salary  |
|                     Statistics: Num rows: 5 Data size: 60 Basic stats: COMPLETE Column stats: COMPLETE |
|                     Group By Operator              |
|                       aggregations: sum(salary), count(salary) |
|                       keys: id (type: int)         |
|                       minReductionHashAggr: 0.4    |
|                       mode: hash                   |
|                       outputColumnNames: _col0, _col1, _col2 |
|                       Statistics: Num rows: 3 Data size: 60 Basic stats: COMPLETE Column stats: COMPLETE |
|                       Reduce Output Operator       |
|                         key expressions: _col0 (type: int) |
|                         null sort order: z         |
|                         sort order: +              |
|                         Map-reduce partition columns: _col0 (type: int) |
|                         Statistics: Num rows: 3 Data size: 60 Basic stats: COMPLETE Column stats: COMPLETE |
|                         value expressions: _col1 (type: double), _col2 (type: bigint) |
|             Execution mode: vectorized             |
|             Map Vectorization:                     |
|                 enabled: true                      |
|                 enabledConditionsMet: hive.vectorized.use.vector.serde.deserialize IS true |
|                 inputFormatFeatureSupport: [DECIMAL_64] |
|                 featureSupportInUse: [DECIMAL_64]  |
|                 inputFileFormats: org.apache.hadoop.mapred.TextInputFormat |
|                 allNative: false                   |
|                 usesVectorUDFAdaptor: false        |
|                 vectorized: true                   |
|         Reducer 2                                  |
|             Execution mode: vectorized             |
|             Reduce Vectorization:                  |
|                 enabled: true                      |
|                 enableConditionsMet: hive.vectorized.execution.reduce.enabled IS true, hive.execution.engine tez IN [tez] IS true |
|                 allNative: false                   |
|                 usesVectorUDFAdaptor: false        |
|                 vectorized: true                   |
|             Reduce Operator Tree:                  |
|               Group By Operator                    |
|                 aggregations: sum(VALUE._col0), count(VALUE._col1) |
|                 keys: KEY._col0 (type: int)        |
|                 mode: mergepartial                 |
|                 outputColumnNames: _col0, _col1, _col2 |
|                 Statistics: Num rows: 3 Data size: 60 Basic stats: COMPLETE Column stats: COMPLETE |
|                 Select Operator                    |
|                   expressions: _col0 (type: int), (_col1 / _col2) (type: double) |
|                   outputColumnNames: _col0, _col1  |
|                   Statistics: Num rows: 3 Data size: 36 Basic stats: COMPLETE Column stats: COMPLETE |
|                   File Output Operator             |
|                     compressed: false              |
|                     Statistics: Num rows: 3 Data size: 36 Basic stats: COMPLETE Column stats: COMPLETE |
|                     table:                         |
|                         input format: org.apache.hadoop.mapred.SequenceFileInputFormat |
|                         output format: org.apache.hadoop.hive.ql.io.HiveSequenceFileOutputFormat |
|                         serde: org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe |
|                                                    |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: -1                                    |
|       Processor Tree:                              |
|         ListSink                                   |
|                                                    |
+----------------------------------------------------+
```

## AST

只输出抽象语法树

```sql
0: jdbc:hive2://localhost:10000> explain ast select id,avg(salary) from t1 group by id;
+----------------------------------+
|             Explain              |
+----------------------------------+
| ABSTRACT SYNTAX TREE:            |
|                                  |
| TOK_QUERY                        |
|    TOK_FROM                      |
|       TOK_TABREF                 |
|          TOK_TABNAME             |
|             t1                   |
|    TOK_INSERT                    |
|       TOK_DESTINATION            |
|          TOK_DIR                 |
|             TOK_TMP_FILE         |
|       TOK_SELECT                 |
|          TOK_SELEXPR             |
|             TOK_TABLE_OR_COL     |
|                id                |
|          TOK_SELEXPR             |
|             TOK_FUNCTION         |
|                avg               |
|                TOK_TABLE_OR_COL  |
|                   salary         |
|       TOK_GROUPBY                |
|          TOK_TABLE_OR_COL        |
|             id                   |
|                                  |
+----------------------------------+
```

## ANALYZE

Annotates the plan with actual row counts. Since in Hive 2.2.0 (HIVE-14362)

    Format is: (estimated row count) / (actual row count)

```sql
0: jdbc:hive2://localhost:10000> explain ANALYZE select id,avg(salary) from t1 group by id;
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| Plan optimized by CBO.                             |
|                                                    |
| Vertex dependency in root stage                    |
| Reducer 2 <- Map 1 (SIMPLE_EDGE)                   |
|                                                    |
| Stage-0                                            |
|   Fetch Operator                                   |
|     limit:-1                                       |
|     Stage-1                                        |
|       Reducer 2 vectorized                         |
|       File Output Operator [FS_12]                 |
|         Select Operator [SEL_11] (rows=3/4 width=12) |
|           Output:["_col0","_col1"]                 |
|           Group By Operator [GBY_10] (rows=3/4 width=20) |
|             Output:["_col0","_col1","_col2"],aggregations:["sum(VALUE._col0)","count(VALUE._col1)"],keys:KEY._col0 |
|           <-Map 1 [SIMPLE_EDGE] vectorized         |
|             SHUFFLE [RS_9]                         |
|               PartitionCols:_col0                  |
|               Group By Operator [GBY_8] (rows=3/4 width=20) |
|                 Output:["_col0","_col1","_col2"],aggregations:["sum(salary)","count(salary)"],keys:id |
|                 Select Operator [SEL_7] (rows=5/5 width=12) |
|                   Output:["id","salary"]           |
|                   TableScan [TS_0] (rows=5/5 width=12) |
|                     default@t1,t1,Tbl:COMPLETE,Col:COMPLETE,Output:["id","salary"] |
|                                                    |
+----------------------------------------------------+
```

```sql
EXPLAIN DEPENDENCY
EXPLAIN AUTHORIZATION
EXPLAIN LOCKS
```

更多介绍 [点这里](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/explain%20plan.md)