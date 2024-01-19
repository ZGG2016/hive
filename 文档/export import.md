# export import

[阅读这里](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Import-Export.md)


```sql
hive> select * from zipper_table;
OK
1       lijie   chengdu 20191021        99991231
1       lijie   chongqing       20191020        20191020
2       zhangshan       huoxing 20191021        99991231
2       zhangshan       sz      20191020        20191020
3       lisi    shanghai        20191020        99991231
4       wangwu  lalalala        20191021        99991231
4       wangwu  usa     20191020        20191020
5       xinzeng hehe    20191021        99991231

-- 导出
hive> export table zipper_table to '/in/zipper_table';
OK

-- hdfs上查看
-- EXPORT 命令将表或分区的数据及其元数据导出到指定的输出位置
-- 导出的元数据存放在目标目录中，数据文件存放在子目录中。
[root@zgg ~]# hadoop fs -ls /in/zipper_table
Found 2 items
-rw-r--r--   1 root supergroup       1733 2021-01-12 13:18 /in/zipper_table/_metadata
drwxr-xr-x   - root supergroup          0 2021-01-12 13:18 /in/zipper_table/data

-- 如果直接执行如下，会报错
hive> import from '/in/zipper_table';
FAILED: SemanticException [Error 10119]: Table exists and contains data files

-- 需要这个表的数据目录下没有数据文件才行
[root@zgg ~]# hadoop fs -rm -r /user/hive/warehouse/zipper_table/000000_0

-- 导入
hive> import from '/in/zipper_table';
Copying data from hdfs://zgg:9000/in/zipper_table/data
Copying file: hdfs://zgg:9000/in/zipper_table/data/000000_0
Loading data to table default.zipper_table
OK

-- 查看表 zipper_table
hive> select * from zipper_table;
OK
1       lijie   chengdu 20191021        99991231
1       lijie   chongqing       20191020        20191020
2       zhangshan       huoxing 20191021        99991231
2       zhangshan       sz      20191020        20191020
3       lisi    shanghai        20191020        99991231
4       wangwu  lalalala        20191021        99991231
4       wangwu  usa     20191020        20191020
5       xinzeng hehe    20191021        99991231

-- 也可以创建一个新表，将zipper_table的数据导入到新表中
-- 复制 zipper_table 的表结构，来创建一个表
hive> create table zipper_bk like zipper_table;
OK

-- 导入
hive> import table zipper_bk from '/in/zipper_table';
Copying data from hdfs://zgg:9000/in/zipper_table/data
Copying file: hdfs://zgg:9000/in/zipper_table/data/000000_0
Loading data to table default.zipper_bk
OK

-- 查看表 zipper_bk
hive> select * from zipper_bk;
OK
1       lijie   chengdu 20191021        99991231
1       lijie   chongqing       20191020        20191020
2       zhangshan       huoxing 20191021        99991231
2       zhangshan       sz      20191020        20191020
3       lisi    shanghai        20191020        99991231
4       wangwu  lalalala        20191021        99991231
4       wangwu  usa     20191020        20191020
5       xinzeng hehe    20191021        99991231


-- 也可以直接在import中添加一个未创建过的新表的名称
-- 如果目标表/分区不存在，IMPORT 将创建它。
hive> import table zipper_bk2 from '/in/zipper_table';
Copying data from hdfs://zgg:9000/in/zipper_table/data
Copying file: hdfs://zgg:9000/in/zipper_table/data/000000_0
Loading data to table default.zipper_bk2
OK

-- 查看表 zipper_bk2
hive> select * from zipper_bk2;
OK
1       lijie   chengdu 20191021        99991231
1       lijie   chongqing       20191020        20191020
2       zhangshan       huoxing 20191021        99991231
2       zhangshan       sz      20191020        20191020
3       lisi    shanghai        20191020        99991231
4       wangwu  lalalala        20191021        99991231
4       wangwu  usa     20191020        20191020
5       xinzeng hehe    20191021        99991231

-- -- 分区 ---------

hive> select * from order_table_s;
OK
1       bicycle 1000    201901
2       truck   20000   201901
1       cellphone       2000    201901
2       tv      3000    201901
1       apple   10      201902
2       banana  8       201902
3       milk    70      201902
4       liquor  150     201902

hive> desc order_table_s;
OK
order_id                int                                         
product_name            string                                      
price                   int                                         
deal_day                string                                      
                 
# Partition Information          
# col_name              data_type               comment             
deal_day                string      

-- 导出一个分区
hive> export table order_table_s partition (deal_day='201901') to '/in/order_table_s/';
OK

-- hdfs上查看
[root@zgg ~]# hadoop fs -ls /in/order_table_s
Found 2 items
-rw-r--r--   1 root supergroup       2761 2021-01-12 13:36 /in/order_table_s/_metadata
drwxr-xr-x   - root supergroup          0 2021-01-12 13:36 /in/order_table_s/deal_day=201901

-- 删除这个分区
hive> alter table order_table_s drop partition(deal_day='201901');
Dropped the partition deal_day=201901
OK

-- 导入这个分区的数据
hive> import from '/in/order_table_s/';
Copying data from hdfs://zgg:9000/in/order_table_s/deal_day=201901
Copying file: hdfs://zgg:9000/in/order_table_s/deal_day=201901/000000_0
Loading data to table default.order_table_s partition (deal_day=201901)
OK

-- 查看order_table_s
hive> select * from order_table_s;
OK
1       bicycle 1000    201901
2       truck   20000   201901
1       cellphone       2000    201901
2       tv      3000    201901
1       apple   10      201902
2       banana  8       201902
3       milk    70      201902
4       liquor  150     201902
```