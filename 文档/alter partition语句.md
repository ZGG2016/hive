# alter partition语句

[TOC]

使用 [分区分桶](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/%E5%88%86%E5%8C%BA%E5%88%86%E6%A1%B6.md) 中的表

```sql
0: jdbc:hive2://localhost:10000> desc order_table_s;
+--------------------------+------------+----------+
|         col_name         | data_type  | comment  |
+--------------------------+------------+----------+
| order_id                 | int        |          |
| product_name             | string     |          |
| price                    | int        |          |
| deal_day                 | string     |          |
|                          | NULL       | NULL     |
| # Partition Information  | NULL       | NULL     |
| # col_name               | data_type  | comment  |
| deal_day                 | string     |          |
+--------------------------+------------+----------+
8 rows selected (0.12 seconds)

0: jdbc:hive2://localhost:10000> select * from order_table_s;
+-------------------------+-----------------------------+----------------------+-------------------------+
| order_table_s.order_id  | order_table_s.product_name  | order_table_s.price  | order_table_s.deal_day  |
+-------------------------+-----------------------------+----------------------+-------------------------+
| 1                       | cellphone                   | 2000                 | 201901                  |
| 2                       | tv                          | 3000                 | 201901                  |
| 3                       | sofa                        | 8000                 | 201901                  |
| 4                       | cabinet                     | 5000                 | 201901                  |
| 5                       | bicycle                     | 1000                 | 201901                  |
| 6                       | truck                       | 2000                 | 201901                  |
| 1                       | apple                       | 10                   | 201902                  |
| 2                       | banana                      | 8                    | 201902                  |
| 3                       | milk                        | 70                   | 201902                  |
| 4                       | liquor                      | 150                  | 201902                  |
+-------------------------+-----------------------------+----------------------+-------------------------+
10 rows selected (0.476 seconds)
```

## Add Partitions

```sql
alter table order_table_s add IF NOT EXISTS 
partition(deal_day='201903')
partition(deal_day='201904');

0: jdbc:hive2://localhost:10000> load data local inpath '/root/data/order-201904.txt' overwrite into table order_table_s partition(deal_day='201904');
0: jdbc:hive2://localhost:10000> select * from order_table_s;
+-------------------------+-----------------------------+----------------------+-------------------------+
| order_table_s.order_id  | order_table_s.product_name  | order_table_s.price  | order_table_s.deal_day  |
+-------------------------+-----------------------------+----------------------+-------------------------+
| 1                       | cellphone                   | 2000                 | 201901                  |
| 2                       | tv                          | 3000                 | 201901                  |
| 3                       | sofa                        | 8000                 | 201901                  |
| 4                       | cabinet                     | 5000                 | 201901                  |
| 5                       | bicycle                     | 1000                 | 201901                  |
| 6                       | truck                       | 2000                 | 201901                  |
| 1                       | apple                       | 10                   | 201902                  |
| 2                       | banana                      | 8                    | 201902                  |
| 3                       | milk                        | 70                   | 201902                  |
| 4                       | liquor                      | 150                  | 201902                  |
| 1                       | book                        | 35                   | 201903                  |
| 2                       | pen                         | 8                    | 201903                  |
| 1                       | cup                         | 77                   | 201904                  |
| 2                       | laptop                      | 800                  | 201904                  |
+-------------------------+-----------------------------+----------------------+-------------------------+

```

在 Hive 0.7 中上述语句会失败，没有错误。并且所有的查询都只会进入 deal_day='201904' 分区，要添加多个分区，只能依次添加。


## Rename Partition

```sql
alter table order_table_s partition(deal_day='201904') 
rename to partition(deal_day='201905');

0: jdbc:hive2://localhost:10000> select * from order_table_s;
+-------------------------+-----------------------------+----------------------+-------------------------+
| order_table_s.order_id  | order_table_s.product_name  | order_table_s.price  | order_table_s.deal_day  |
+-------------------------+-----------------------------+----------------------+-------------------------+
| 1                       | cellphone                   | 2000                 | 201901                  |
| 2                       | tv                          | 3000                 | 201901                  |
| 3                       | sofa                        | 8000                 | 201901                  |
| 4                       | cabinet                     | 5000                 | 201901                  |
| 5                       | bicycle                     | 1000                 | 201901                  |
| 6                       | truck                       | 2000                 | 201901                  |
| 1                       | apple                       | 10                   | 201902                  |
| 2                       | banana                      | 8                    | 201902                  |
| 3                       | milk                        | 70                   | 201902                  |
| 4                       | liquor                      | 150                  | 201902                  |
| 1                       | book                        | 35                   | 201903                  |
| 2                       | pen                         | 8                    | 201903                  |
| 1                       | cup                         | 77                   | 201905                  |
| 2                       | laptop                      | 800                  | 201905                  |
+-------------------------+-----------------------------+----------------------+-------------------------+

0: jdbc:hive2://localhost:10000> show partitions order_table_s;
+------------------+
|    partition     |
+------------------+
| deal_day=201901  |
| deal_day=201902  |
| deal_day=201903  |
| deal_day=201905  |
+------------------+

-- 这里未改变
hive@b6678a23eba8:/opt/hive/data/warehouse/order_table_s$ ls
'deal_day=201901'  'deal_day=201902'  'deal_day=201903'  'deal_day=201904'
```

## Exchange Partition

将一个分区中的数据从一个表移动到另一个具有相同schema但尚未拥有该分区的表

[官网详细描述](https://cwiki.apache.org/confluence/display/Hive/Exchange+Partition)

```sql
create table order_table_s2
(
    order_id int,         -- 订单id
    product_name string,  -- 产品名称
    price int             -- 产品价格
)
partitioned by (deal_day string)  -- 交易日期YYYYMM
row format delimited
fields terminated by "\t";

alter table order_table_s2 exchange partition(deal_day='201903') with table order_table_s;

0: jdbc:hive2://localhost:10000> show partitions order_table_s2;
+------------------+
|    partition     |
+------------------+
| deal_day=201903  |
+------------------+

0: jdbc:hive2://localhost:10000> select * from order_table_s2;
+--------------------------+------------------------------+-----------------------+--------------------------+
| order_table_s2.order_id  | order_table_s2.product_name  | order_table_s2.price  | order_table_s2.deal_day  |
+--------------------------+------------------------------+-----------------------+--------------------------+
| 1                        | book                         | 35                    | 201903                   |
| 2                        | pen                          | 8                     | 201903                   |
+--------------------------+------------------------------+-----------------------+--------------------------+

-- 原表中就没有了
0: jdbc:hive2://localhost:10000> show partitions order_table_s;
+------------------+
|    partition     |
+------------------+
| deal_day=201901  |
| deal_day=201902  |
| deal_day=201904  |
+------------------+
```

## Discover Partitions

TODO

```sql
0: jdbc:hive2://localhost:10000> alter table order_table_s set tblproperties("discover.partitions"="true");
```
 
## Partition Retention

TODO 

```sql
0: jdbc:hive2://localhost:10000> alter table order_table_s set tblproperties("partition.retention.period"="1m");
```

## Recover Partitions (MSCK REPAIR TABLE)

如果新的分区直接添加到 HDFS(比如通过使用 hadoop fs -put命令)或从 HDFS 删除，metastore 不会意识到这些分区信息的变化。所以需要恢复

```
MSCK [REPAIR] TABLE table_name [ADD/DROP/SYNC PARTITIONS];
```

- ADD PARTITIONS: MSC 命令的默认选项。它将把所有存在于 HDFS 上，但不在 metastore 中的分区添加到 metastore中。

- DROP PARTITIONS: 将从 metastore 中删除已经从 HDFS 删除的分区的信息。

- SYNC PARTITIONS: 相当于同时调用 ADD 和 DROP PARTITIONS。

- 不带 REPAIR 选项的 MSCK 命令可用于查找有关元数据不匹配 metastore 的详细信息

示例查看 [这里](https://github.com/ZGG2016/hive/blob/master/%E6%96%87%E6%A1%A3/%E5%88%86%E5%8C%BA%E8%A1%A8%E5%92%8C%E6%95%B0%E6%8D%AE%E4%BA%A7%E7%94%9F%E5%85%B3%E8%81%94.md)

通过设置属性 hive.msck.repair.batch, 可以批量运行 MSCK REPAIR TABLE


## Drop Partitions

这将删除该分区的数据和元数据。如果[配置了 Trash](https://zhuanlan.zhihu.com/p/626290608)，数据实际上会移动到 `.Trash/Current` 目录，除非指定了 PURGE，但是元数据会完全丢失。

```sql
alter table order_table_s drop if exists partition(deal_day="201904") PURGE;
```

## (Un)Archive Partition

将分区文件移动到 Hadoop Archive(HAR)的功能

ARCHIVE can only be performed on managed tables

```sql
alter table order_table_s archive partition(deal_day="201903");
```


-------------------------------

[官网](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AlterPartition)

[官网翻译](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/DDL%20Statements.md#152alter-partition)