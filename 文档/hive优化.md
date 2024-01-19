# hive 优化

[TOC]

[explain](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/explain%E6%89%A7%E8%A1%8C%E8%AE%A1%E5%88%92%E5%88%86%E6%9E%90.md)

[fetch](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/fetch%E6%8A%93%E5%8F%96.md)

[本地模式](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/%E6%9C%AC%E5%9C%B0%E6%A8%A1%E5%BC%8F.md)

[map端聚合](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/map%E7%AB%AF%E8%81%9A%E5%90%88.md)

[大小表join-mapjoin](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/hive%20map%20join.md)

[大大表join-smbjoin](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/hive%20map%20join.md)

[分区分桶](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/%E5%88%86%E5%8C%BA%E5%88%86%E6%A1%B6.md)

[谓词下推](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/%E8%B0%93%E8%AF%8D%E4%B8%8B%E6%8E%A8.md)

[join数据倾斜](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/join%E6%95%B0%E6%8D%AE%E5%80%BE%E6%96%9C.md)

[groupby数据倾斜](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/groupby%E6%95%B0%E6%8D%AE%E5%80%BE%E6%96%9C.md)

[并行执行](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/%E5%B9%B6%E8%A1%8C%E6%89%A7%E8%A1%8C.md)

[多重模式](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/groupby%E5%AD%90%E5%8F%A5.md)

[控制map和reduce的数量](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/hive%E6%8E%A7%E5%88%B6map%E5%92%8Creduce%E7%9A%84%E6%95%B0%E9%87%8F.md)

[CBO优化](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/cbo%E4%BC%98%E5%8C%96.md)

[向量化查询](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/%E5%90%91%E9%87%8F%E5%8C%96%E6%9F%A5%E8%AF%A2.md)

[严格模式](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/%E4%B8%A5%E6%A0%BC%E6%A8%A1%E5%BC%8F.md)

[相关性优化器]()

## 列裁剪和分区裁剪

配置 `hive.optimize.cp` 和 `hive.optimize.pruner`，分别进行列裁剪和分区裁剪，避免全列扫描和全表扫描。

`hive.optimize.cp` 在 hive0.13.0 被移除。

## groupby代替distinct

```sql
-- 两个job
0: jdbc:hive2://zgg-server:10000> select count(uid) from (select uid from ratings group by uid) t;
...
INFO  : MapReduce Jobs Launched: 
INFO  : Stage-Stage-1: Map: 3  Reduce: 3   Cumulative CPU: 32.97 sec   HDFS Read: 678326977 HDFS Write: 348 HDFS EC Read: 0 SUCCESS
INFO  : Stage-Stage-2: Map: 1  Reduce: 1   Cumulative CPU: 3.26 sec   【HDFS Read: 9185】 HDFS Write: 106 HDFS EC Read: 0 SUCCESS
INFO  : Total MapReduce CPU Time Spent: 36 seconds 230 msec
...
```

```sql
0: jdbc:hive2://zgg-server:10000> select count(distinct uid) from ratings;
...
INFO  : MapReduce Jobs Launched: 
INFO  : Stage-Stage-1: Map: 3  Reduce: 1   Cumulative CPU: 30.1 sec   【HDFS Read: 678315013】 HDFS Write: 106 HDFS EC Read: 0 SUCCESS
INFO  : Total MapReduce CPU Time Spent: 30 seconds 100 msec
...
```

使用先 group by/count 的方式替换 COUNT DISTINCT

但是 group by 容易产生数据倾斜问题。同时，要确保数据量大到启动 job 的负载远小于计算耗时，才考虑这种方法。

--------------------------------------

参考：

- [尚硅谷hive教程](https://www.bilibili.com/video/BV1EZ4y1G7iL)
- [Hive/HiveSQL常用优化方法全面总结](https://cloud.tencent.com/developer/article/1453464)
- [Hive优化](https://cloud.tencent.com/developer/article/1700573)