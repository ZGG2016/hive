# hive 优化

[TOC]

[explain](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/explain%E6%89%A7%E8%A1%8C%E8%AE%A1%E5%88%92%E5%88%86%E6%9E%90.md)

[fetch 抓取]()

## 本地模式

当 Hive 的输入数据量是非常小时，触发执行任务消耗的时间可能
会比实际 job 的执行时间要多的多，此时，Hive 可以通过本地模式在单台机器上处理所有的任务。

```
//开启本地模式 mr
set hive.exec.mode.local.auto=true; 

//设置 local mr 的最大输入数据量，当输入数据量小于这个值时采用 local mr 的方式，默认
为 134217728，即 128M
set hive.exec.mode.local.auto.inputbytes.max=50000000;

//设置 local mr 的最大输入文件个数，当输入文件个数小于这个值时采用 local mr 的方式，默
认为 4
set hive.exec.mode.local.auto.input.files.max=10;
```

```sql
hive (default)> select count(*) from emp;
....
Ended Job = job_1662688244068_0008
MapReduce Jobs Launched: 
Stage-Stage-1: Map: 1  Reduce: 1   Cumulative CPU: 5.12 sec   HDFS Read: 14035 HDFS Write: 102 SUCCESS
Total MapReduce CPU Time Spent: 5 seconds 120 msec
OK
_c0
14
Time taken: 33.836 seconds, Fetched: 1 row(s)
```

```sql
hive (default)> set hive.exec.mode.local.auto=true;
hive (default)> select count(*) from emp;
....
Ended Job = job_local1830996877_0001
MapReduce Jobs Launched: 
Stage-Stage-1:  HDFS Read: 1312 HDFS Write: 102 SUCCESS
Total MapReduce CPU Time Spent: 0 msec
OK
_c0
14
Time taken: 6.477 seconds, Fetched: 1 row(s)
```


## 大小表join

执行 mapjoin(MAPJION会把小表全部加载到内存中，), 设置如下属性

```
设置自动选择 Mapjoin
set hive.auto.convert.join = true; 默认为 true

大表小表的阈值设置（默认 25M 以下认为是小表）
set hive.mapjoin.smalltable.filesize = 25000000;

hive.auto.convert.join.noconditionaltask
```

## 大表大表join

- 1. 过滤掉null
- 2. 将null转换
- 3. SMB join: 通过建分桶表实现，并开启如下属性


```
set hive.optimize.bucketmapjoin = true;
set hive.optimize.bucketmapjoin.sortedmerge = true;
set hive.input.format=org.apache.hadoop.hive.ql.io.BucketizedHiveInputFormat;
```

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

## left semi join代替in/exists 操作

```sql
select a.id, a.name from a where a.id in (select b.id from b);

select a.id, a.name from a where exists (select id from b where a.id = b.id);

-- 可以使用 join 来改写：
select a.id, a.name from a join b on a.id = b.id;

-- 应该转换成 left semi join 实现
select a.id, a.name from a left semi join b on a.id = b.id;
```

## 多表join

如果在 join 子句中使用每个表的相同的列，那么 Hive 将多表 join 转成一个 map/reduce job。如下

```sql
SELECT a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key1)
```

下面的语句则是被转成两个 map/reduce job，因为 b 的 key1 列在第一个 join 条件中使用，b 的 key2 列在第二个 join 条件中使用。第一个 map/reduce job 将 a 和 b join，然后在第二个 map/reduce job 中结果再和 c join。

```sql
SELECT a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key2)
```

## join中的表顺序

在 join 的每个 map/reduce 阶段，序列中的最后一个表通过 reducers 流动，而其他表被缓存。

```sql
SELECT a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key1)
```

一个 map/reduce job 中连接所有的三个表，表 a 和表 b 的键的特定值被缓存在 reducers 的内存中。然后，对于从 c 检索到的每一行，使用缓存的行进行计算连接。同样的

```sql
SELECT a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key2)
```

在计算连接时，涉及两个 map/reduce jobs。第一个 job 连接 a 和 b，并缓存 a 的值，同时 b 的值在 reducer 中流动。其中的第二个 job 缓存第一个连接的结果，同时 c 的值在 reducer 中流动。

也可以使用 hint 指定流动的表，那么其他表就被缓存。

```sql
SELECT /*+ STREAMTABLE(a) */ a.val, b.val, c.val FROM a JOIN b ON (a.key = b.key1) JOIN c ON (c.key = b.key1)
```

更多join优化点 [这里](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/Joins.md)

## map端聚合

设置如下参数

```
# 是否在 Map 端进行聚合，默认为 True
set hive.map.aggr = true

# 在 Map 端进行聚合操作的条目数目
set hive.groupby.mapaggr.checkinterval = 100000

# 有数据倾斜的时候进行负载均衡（默认是 false）
set hive.groupby.skewindata = true
```

## groupby代替distinct去重

`COUNT DISTINCT` 使用先 group by 再 count 的方式替换，可以启用更多的 job 来处理。

但是需要注意 group by 造成的数据倾斜问题。

要确保数据量大到启动 job 的负载远小于计算耗时，才考虑这种方法。

## groupby数据倾斜优化

当 group by 时，配置 `hive.groupby.skewindata` ，处理某些 key 对应的数据量过大，发生数据倾斜的情况。

## 避免笛卡尔积

尽量避免笛卡尔积，join 的时候不加 on 条件，或者无效的 on 条件，Hive 只能使用 1 个 reducer 来完成笛卡尔积。

## 列过滤

在 SELECT 中，只拿需要的列，如果有分区，尽量使用分区过滤，少用 `SELECT *`。

## 行过滤（谓词下推）

对表过滤后，再join

对应逻辑优化器是 PredicatePushDown，配置项为 `hive.optimize.ppd`，默认为 true。

[点这里](https://github.com/ZGG2016/knowledgesystem/blob/master/03%20%E5%A4%A7%E6%95%B0%E6%8D%AE/02%20Hive/%E8%B0%93%E8%AF%8D%E4%B8%8B%E6%8E%A8%E6%B5%8B%E8%AF%95.md) 查看详情

## 分区分桶

[点这里](https://github.com/ZGG2016/knowledgesystem/blob/master/03%20%E5%A4%A7%E6%95%B0%E6%8D%AE/02%20Hive/%E5%88%86%E5%8C%BA%E5%88%86%E6%A1%B6.md) 查看详情

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