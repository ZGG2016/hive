# explain执行计划分析

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

更多介绍 [点这里](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/explain%20plan.md)