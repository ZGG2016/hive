# hive抽样

抽样是在 CREATE TABLE 语句的 CLUSTERED BY 子句中指定的列上进行的

	TABLESAMPLE(BUCKET x OUT OF y)

y 必须是该表在创建表时指定的桶数的倍数或除数

桶从0开始编号

```sql
-- 创建分桶
drop table order_table_buckets;
create table order_table_buckets
(
    order_id int,         -- 订单id
    product_name string,  -- 产品名称
    price int             -- 产品价格
)
partitioned by (deal_day string)  -- 交易日期YYYYMM，产品类别
clustered by (order_id) sorted by(price) into 4 buckets
row format delimited
fields terminated by "\t";

-- 导入数据
load data local inpath '/opt/hive/test-data/bucket.txt' into table order_table_buckets partition(deal_day='201901'); 

-- 查看表
0: jdbc:hive2://localhost:10000> select * from order_table_buckets order by order_id;
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
| order_table_buckets.order_id  | order_table_buckets.product_name  | order_table_buckets.price  | order_table_buckets.deal_day  |
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
| 1                             | bicycle                           | 1000                       | 201901                        |
| 2                             | truck                             | 20000                      | 201901                        |
| 3                             | cellphone                         | 2000                       | 201901                        |
| 4                             | tv                                | 3000                       | 201901                        |
| 5                             | apple                             | 10                         | 201901                        |
| 6                             | banana                            | 8                          | 201901                        |
| 7                             | milk                              | 70                         | 201901                        |
| 8                             | liquor                            | 150                        | 201901                        |
| 9                             | book                              | 30                         | 201901                        |
| 10                            | book2                             | 40                         | 201901                        |
| 11                            | book4                             | 45                         | 201901                        |
| 12                            | book5                             | 75                         | 201901                        |
| 13                            | book6                             | 145                        | 201901                        |
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+


hive@8361ca1351e2:/opt/hive/data/warehouse/order_table_buckets/deal_day=201901$ ls
000000_0  000001_0  000002_0  000003_0
hive@8361ca1351e2:/opt/hive/data/warehouse/order_table_buckets/deal_day=201901$ cat 000003_0 
5	apple	10
12	book5	75
hive@8361ca1351e2:/opt/hive/data/warehouse/order_table_buckets/deal_day=201901$ cat 000002_0 
3	cellphone	2000
hive@8361ca1351e2:/opt/hive/data/warehouse/order_table_buckets/deal_day=201901$ cat 000001_0 
9	book	30
11	book4	45
13	book6	145
8	liquor	150
1	bicycle	1000
4	tv	3000
hive@8361ca1351e2:/opt/hive/data/warehouse/order_table_buckets/deal_day=201901$ cat 000000_0 
6	banana	8
10	book2	40
7	milk	70
2	truck	20000
```

从这4个桶里选择第2个桶

```sql
0: jdbc:hive2://localhost:10000> select * from order_table_buckets tablesample(bucket 1 out of 4);
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
| order_table_buckets.order_id  | order_table_buckets.product_name  | order_table_buckets.price  | order_table_buckets.deal_day  |
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
| 6                             | banana                            | 8                          | 201901                        |
| 10                            | book2                             | 40                         | 201901                        |
| 7                             | milk                              | 70                         | 201901                        |
| 2                             | truck                             | 20000                      | 201901                        |
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
```

从这4个桶里选择第1个桶和第3个桶

```sql
0: jdbc:hive2://localhost:10000> select * from order_table_buckets tablesample(bucket 1 out of 2);
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
| order_table_buckets.order_id  | order_table_buckets.product_name  | order_table_buckets.price  | order_table_buckets.deal_day  |
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
| 3                             | cellphone                         | 2000                       | 201901                        |
| 6                             | banana                            | 8                          | 201901                        |
| 10                            | book2                             | 40                         | 201901                        |
| 7                             | milk                              | 70                         | 201901                        |
| 2                             | truck                             | 20000                      | 201901                        |
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
```

取出第1个桶的一半

```sql
0: jdbc:hive2://localhost:10000> select * from order_table_buckets tablesample(bucket 1 out of 8 on order_id);
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
| order_table_buckets.order_id  | order_table_buckets.product_name  | order_table_buckets.price  | order_table_buckets.deal_day  |
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
| 10                            | book2                             | 40                         | 201901                        |
| 7                             | milk                              | 70                         | 201901                        |
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
```