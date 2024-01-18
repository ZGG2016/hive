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

[CBO优化]()

[向量化查询]()

## 向量化查询

标准的查询执行系统每次处理一行，

向量化查询执行通过一次处理 1024 行数据的块简化了操作。在块中，每个列都存储为向量(一个基本数据类型的数组)。

首先以 ORC 格式存储数据，然后启用

    set hive.vectorized.execution.enabled = true;
    set hive.vectorized.execution.reduce.enabled = true;

在 explain 展示的执行计划中出现如下内容，就表示使用了向量化查询

```sql
hive (default)> explain select count(id) from vectorizedtable group by state;
....
Execution mode: vectorized
....
```

[点这里](https://github.com/ZGG2016/hive-website/blob/master/Resources%20for%20Contributors/Hive%20Design%20Docs/Vectorized%20Query%20Execution.md) 查看详情

## 列裁剪和分区裁剪

通过配置项 `hive.optimize.cp` 和 `hive.optimize.pruner`，分别进行列裁剪和分区裁剪，避免全列扫描和全表扫描。

`hive.optimize.cp` 在 hive0.13.0 被移除。

## groupby代替distinct去重

`COUNT DISTINCT` 使用先 group by 再 count 的方式替换，可以启用更多的 job 来处理。

但是需要注意 group by 造成的数据倾斜问题。

要确保数据量大到启动 job 的负载远小于计算耗时，才考虑这种方法。

## 避免笛卡尔积

尽量避免笛卡尔积，join 的时候不加 on 条件，或者无效的 on 条件，Hive 只能使用 1 个 reducer 来完成笛卡尔积。

## 严格模式

（1）分区表不使用分区过滤

将 `hive.strict.checks.no.partition.filter` 设置为 true 时，对于分区表，除非 where 语句中含有分区字段过滤条件来限制范围，否则不允许执行。

换句话说，就是用户不允许扫描所有分区。

进行这个限制的原因是，通常分区表都拥有非常大的数据集，而且数据增加迅速。

没有进行分区限制的查询可能会消耗令人不可接受的巨大资源来处理这个表。

（2）使用 order by 没有 limit 过滤

将 `hive.strict.checks.orderby.no.limit` 设置为 true 时，对于使用了 order by 语句的查询，要求必须使用 limit 语句。

因为 order by 为了执行排序过程会将所有的结果数据分发到同一个 Reducer 中进行处理，强制要求用户增加这个 LIMIT 语句可以防止 Reducer 额外执行很长一段时间。

（3）笛卡尔积

将 `hive.strict.checks.cartesian.product` 设置为 true 时，会限制笛卡尔积的查询。

## JVM重用

为了实现任务隔离，hadoop 将每个 task 放到一个单独的 JVM 中执行，而对于执行时间较短的 task ，JVM 启动和关闭的时间将占用很大比例时间，为此，用户可以启用 JVM 重用功能，这样一个 JVM 可连续启动多个同类型的任务。

在`mapred-site.xml`文件中进行配置:

```xml
<property>
  <name>mapreduce.job.jvm.numtasks</name>
  <value>10</value>
  <description>How many tasks to run per jvm. If set to -1, there is no limit. </description>
</property>
```

开启 JVM 重用将一直占用使用到的 task 插槽，以便进行重用，直到任务完成后才能释放。

如果某个"不平衡的" job 中有某几个 reduce task 执行的时间要比其他 reduce task 消耗的时间多的多的话，那么保留的插槽就会一直空闲着却无法被其他的 job 使用，直到所有的 task 都结束了才会释放。

小文件过多时使用。

## 压缩

## 使用 sort by 替代 order by

使用 sort by 替代 order by，这样数据可以分发到多个 reducer 处理。


## 采用合适的存储格式


--------------------------------------

根据以下内容整理 

- [尚硅谷hive教程](https://www.bilibili.com/video/BV1EZ4y1G7iL)
- [Hive/HiveSQL常用优化方法全面总结](https://cloud.tencent.com/developer/article/1453464)
- [Hive优化](https://cloud.tencent.com/developer/article/1700573)