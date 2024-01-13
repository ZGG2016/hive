# hive 优化

[TOC]

[explain](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/explain%E6%89%A7%E8%A1%8C%E8%AE%A1%E5%88%92%E5%88%86%E6%9E%90.md)

[fetch](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/fetch%E6%8A%93%E5%8F%96.md)

[本地模式](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/%E6%9C%AC%E5%9C%B0%E6%A8%A1%E5%BC%8F.md)

[map端聚合](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/map%E7%AB%AF%E8%81%9A%E5%90%88.md)

[大小表join-mapjoin](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/hive%20map%20join.md)

[大大表join-smbjoin](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/hive%20map%20join.md)

[分区分桶](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/%E5%88%86%E5%8C%BA%E5%88%86%E6%A1%B6.md)

[谓词下推]()

列裁剪

[join数据倾斜]()

## join数据倾斜优化

由于 join 出现的数据倾斜，那么请做如下设置：

```
# join 的键对应的记录条数超过这个值则会进行分拆，值根据具体数据量设置
set hive.skewjoin.key=100000;

# 如果是 join 过程出现倾斜应该设置为 true
set hive.optimize.skewjoin=false;
```

如果开启了，在 join 过程中 Hive 会将计数超过阈值 `hive.skewjoin.key`（默认 100000）的倾斜 key 对应的行临时写进文件中，

然后再启动另一个 job 做 map join 生成结果。

通过 `hive.skewjoin.mapjoin.map.tasks` 参数还可以控制第二个 job 的 mapper 数量，默认 10000。

```
set hive.skewjoin.mapjoin.map.tasks=10000;
```


## groupby代替distinct去重

`COUNT DISTINCT` 使用先 group by 再 count 的方式替换，可以启用更多的 job 来处理。

但是需要注意 group by 造成的数据倾斜问题。

要确保数据量大到启动 job 的负载远小于计算耗时，才考虑这种方法。

## groupby数据倾斜优化

当 group by 时，配置 `hive.groupby.skewindata` ，处理某些 key 对应的数据量过大，发生数据倾斜的情况。

## 避免笛卡尔积

尽量避免笛卡尔积，join 的时候不加 on 条件，或者无效的 on 条件，Hive 只能使用 1 个 reducer 来完成笛卡尔积。




## 合理设置map数

（1）复杂文件增加 Map 数

当 input 的文件都很大，任务逻辑复杂，map 执行非常慢的时候，可以考虑增加 Map 数，来使得每个 map 处理的数据量减少，从而提高任务的执行效率。

增加 map 的方法为：

    map输入的最大的分片大小
    mapreduce.input.fileinputformat.split.maxsize [mapred-site.xml]

    map输入的最小的分片大小，默认是0
    mapreduce.input.fileinputformat.split.minsize [mapred-site.xml]

    protected long computeSplitSize(long blockSize, long minSize,
                                   long maxSize) {
      return Math.max(minSize, Math.min(maxSize, blockSize));
    }

调整 maxSize ，让 maxSize 最大值低于 blocksize 就可以增加 map 的个数

    mapreduce.input.fileinputformat.split.maxsize

（2）小文件进行合并

在 map 执行前合并小文件，减少 map 数：CombineHiveInputFormat 具有对小文件进行合
并的功能（系统默认的格式）。

HiveInputFormat 没有对小文件合并功能。

    set hive.input.format=org.apache.hadoop.hive.ql.io.CombineHiveInputFormat;

在 Map-Reduce 的任务结束时合并小文件的设置：

    在 map-only 任务结束时合并小文件，默认 true
    SET hive.merge.mapfiles = true;
    
    在 map-reduce 任务结束时合并小文件，默认 false
    SET hive.merge.mapredfiles = true;
    
    合并文件的大小，默认 256M
    SET hive.merge.size.per.task = 268435456;

    当输出文件的平均大小小于该值时，启动一个独立的 map-reduce 任务进行文件 merge
    SET hive.merge.smallfiles.avgsize = 16777216;

## 合理设置reduce数

（1）调整 reduce 个数方法一

    每个 Reduce 处理的数据量默认是 256MB
    hive.exec.reducers.bytes.per.reducer=256000000
    
    每个任务最大的 reduce 数，默认为 1009
    hive.exec.reducers.max=1009
    
    计算 reducer 数的公式
    N=min(参数 2，总输入数据量/参数 1)

（2）调整 reduce 个数方法二

在 hadoop 的 mapred-default.xml 文件中修改

    设置每个 job 的 Reduce 个数
    set mapreduce.job.reduces = 15;

如果设置为-1，那么就会使用第一个方法设置 reduce 个数

## 并行执行

有些阶段可能并非完全互相依赖的，也就是说有些阶段是可以并行执行的，就可以设置如下参数开启并行执行。

    set hive.exec.parallel=true; //打开任务并行执行
    set hive.exec.parallel.thread.number=16; //同一个 sql 允许最大并行度，默认为8。

当然，得是在系统资源比较空闲的时候才有优势，否则，没资源，并行也起不来。

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

[点这里](https://github.com/ZGG2016/knowledgesystem/blob/master/03%20%E5%A4%A7%E6%95%B0%E6%8D%AE/02%20Hive/hive%20%E5%8E%8B%E7%BC%A9%E4%BD%BF%E7%94%A8.md) 查看详情

## 更换引擎

使用tez或spark

[点这里](https://github.com/ZGG2016/knowledgesystem/blob/master/03%20%E5%A4%A7%E6%95%B0%E6%8D%AE/02%20Hive/tez%E7%8E%AF%E5%A2%83%E6%90%AD%E5%BB%BA.md) 查看详情

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

## 多重模式

存在多条sql语句，它们都从同一个表进行扫描，但是做不同的逻辑，那么就可以一次读取，多次使用。

```sql
insert int t_ptn partition(city=A). select id,name,sex, age from student
where city= A;
insert int t_ptn partition(city=B). select id,name,sex, age from student
where city= B;
insert int t_ptn partition(city=c). select id,name,sex, age from student
where city= c;

修改为：
from student
insert int t_ptn partition(city=A) select id,name,sex, age where city= A
insert int t_ptn partition(city=B) select id,name,sex, age where city= B
```

## CBO优化

Hive 在提供最终执行前，根据统计信息，选择代价最小的执行计划。(hive4.0.0版本开始)

```
set hive.cbo.enable=true;
set hive.compute.query.using.stats=true;
set hive.stats.fetch.column.stats=true;
set hive.stats.fetch.partition.stats=true;
```

```sql
hive> explain cbo cost select customer_key, sum(total_amount) amount from sales group by customer_key;
```

[点击](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/explain%20plan.md#12the-cbo-clause) 查看更多

## 使用 sort by 替代 order by

使用 sort by 替代 order by，这样数据可以分发到多个 reducer 处理。

## 列裁剪和分区裁剪

通过配置项 `hive.optimize.cp` 和 `hive.optimize.pruner`，分别进行列裁剪和分区裁剪，避免全列扫描和全表扫描。

## 优化 SQL 来处理 join 数据倾斜：

- 若不需要空值数据，就提前写where语句过滤掉。需要保留的话，将空值key用随机方式打散。
- 如果倾斜的 key 很少，将它们抽样出来，对应的行单独存入临时表中，然后打上一个较小的随机数前缀（比如0~9），最后再进行聚合。
- 如果小表的数据量也很大，可以利用大表的限制条件，削减小表的数据量，再使用 map join 解决。

## 采用合适的存储格式

[点击](https://github.com/ZGG2016/knowledgesystem/blob/master/03%20%E5%A4%A7%E6%95%B0%E6%8D%AE/02%20Hive/hive%20%E4%B8%AD%E4%BD%BF%E7%94%A8%E4%B8%8D%E5%90%8C%E7%9A%84%E6%96%87%E4%BB%B6%E5%AD%98%E5%82%A8%E6%A0%BC%E5%BC%8F.md) 查看更多

--------------------------------------

根据以下内容整理 

- [尚硅谷hive教程](https://www.bilibili.com/video/BV1EZ4y1G7iL)
- [Hive/HiveSQL常用优化方法全面总结](https://cloud.tencent.com/developer/article/1453464)
- [Hive优化](https://cloud.tencent.com/developer/article/1700573)