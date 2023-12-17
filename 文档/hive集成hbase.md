# hive集成hbase

[TOC]

该特性允许 Hive QL 语句对 HBase 表进行读(SELECT)和写(INSERT)访问。

【可用于使用hbase更新hive中的数据】

## 配置

1. 确保hive和hbase版本兼容，兼容信息 [点这里](https://cwiki.apache.org/confluence/display/Hive/HBaseIntegration) 查看官网 `Version information` 部分。 

```
以下测试使用的版本是 hbase-1.2.0-cdh5.14.0  hive-1.1.0-cdh5.14.0
```

2. 将 `hive-hbase-handler-x.y.z.jar` 和 HBase、Guava 和 ZooKeeper jars 放到 `lib` 目录下

```
[root@bigdata-cdh01 lib]# pwd
/export/servers/hive/lib
[root@bigdata-cdh01 lib]# ls | grep hbase
hbase-annotations-1.2.0-cdh5.14.0.jar
hbase-common-1.2.0-cdh5.14.0.jar    【√】
hbase-protocol-1.2.0-cdh5.14.0.jar
hive-hbase-handler-1.1.0-cdh5.14.0.jar   【√】
[root@bigdata-cdh01 lib]# ls | grep zookeeper
zookeeper-3.4.5-cdh5.14.0.jar   【√】
[root@bigdata-cdh01 lib]# ls | grep guava
guava-14.0.1.jar   【√】
```

## 使用

```
hive (default)> create table hbase_table_1(key int, value string)
              > stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
              > with serdeproperties("hbase.columns.mapping"=":key,cf1:val")
              > tblproperties("hbase.table.name"="xyz", "hbase.mapred.output.outputtable"="xyz");
OK
Time taken: 8.562 seconds
```

- hbase.columns.mapping 列映射。 `:key`只能有一个； `cf1:val`是在hbase中的列族和列名
- hbase.table.name  在hbase中的表名，不指定就和hive中的表名同名
- hbase.mapred.output.outputtable 控制着往表中插数据

上述语句在hive中执行后，在hbase即可查看到

```
hbase(main):001:0> list
TABLE                                                                                                                                                
tbl_goods                                                                                                                                            
tbl_logs                                                                                                                                             
tbl_orders                                                                                                                                           
tbl_profile                                                                                                                                          
tbl_users                                                                                                                                            
xyz           【√】                                                                                                                                        
6 row(s) in 0.2550 seconds

=> ["tbl_goods", "tbl_logs", "tbl_orders", "tbl_profile", "tbl_users", "xyz"]

hbase(main):003:0> describe "xyz"
Table xyz is ENABLED                                                                                                                                 
xyz                                                                                                                                                  
COLUMN FAMILIES DESCRIPTION                                                                                                                          
{NAME => 'cf1', BLOOMFILTER => 'ROW', VERSIONS => '1', IN_MEMORY => 'false', KEEP_DELETED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'NONE', TTL => 'FOREVER', COMPRESSION => 'NONE', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE => '0'}                            
1 row(s) in 0.0530 seconds
```

往hive表hbase_table_1中插入数据后，在hbase表xyz中也能看到

```
hive (default)> insert into hbase_table_1 values(1,"zhangsan");
hive (default)> select * from hbase_table_1;
OK
hbase_table_1.key	hbase_table_1.value
1	zhangsan
Time taken: 0.195 seconds, Fetched: 1 row(s)
```

```
hbase(main):004:0> scan "xyz"
ROW               COLUMN+CELL                                                                                                   
 1                column=cf1:val, timestamp=1702710174639, value=zhangsan                                                       
1 row(s) in 0.0990 seconds
```

由于 WAL 开销，当插入大量数据时候，速度会慢，可以在插入前执行 `set hive.hbase.wal.enabled=false;` 禁用 WAL.

警告：当 HBase 发生故障时，禁用 WAL 可能会导致数据丢失，只有在有其他恢复策略可用的情况下才使用此功能。

## hive访问已存在的hbase表

```
hbase(main):009:0> create "xyz2","cf1"
0 row(s) in 2.4130 seconds

=> Hbase::Table - xyz2
hbase(main):010:0> put 'xyz2','1','cf1:name','lisi'
0 row(s) in 0.2560 seconds

hbase(main):011:0> scan 'xyz2'
ROW              COLUMN+CELL                                                                                                   
 1               column=cf1:name, timestamp=1702710711996, value=lisi                                                          
1 row(s) in 0.0210 seconds
```

需要创建外部表

```
hive (default)> create external table hbase_table_2(key int, value string)
              > stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
              > with serdeproperties("hbase.columns.mapping" = "cf1:name")
              > TBLPROPERTIES("hbase.table.name" = "xyz2", "hbase.mapred.output.outputtable" = "xyz2");
OK
Time taken: 0.31 seconds
hive (default)> select * from hbase_table_2;
OK
hbase_table_2.key	hbase_table_2.value
1	lisi
Time taken: 0.156 seconds, Fetched: 1 row(s)
```

往hbase表xyz2再插入一条数据，数据会同步过去

```
hbase(main):018:0> put 'xyz2','2','cf1:name','wangwu'
0 row(s) in 0.0070 seconds

hbase(main):019:0> scan 'xyz2'
ROW                    COLUMN+CELL                                                                                                   
 1                     column=cf1:name, timestamp=1702710711996, value=lisi                                                          
 2                     column=cf1:name, timestamp=1702710894577, value=wangwu                                                        
2 row(s) in 0.0420 seconds
```

```
hive (default)> select * from hbase_table_2;
OK
hbase_table_2.key	hbase_table_2.value
1	lisi
2	wangwu
Time taken: 0.136 seconds, Fetched: 2 row(s)
```

往hive表hbase_table_2插入一条数据，数据会同步过去

```
hive (default)> insert into hbase_table_2 values(3,'tom');
hive (default)> select * from hbase_table_2;
OK
hbase_table_2.key	hbase_table_2.value
1	lisi
2	wangwu
3	tom
Time taken: 0.137 seconds, Fetched: 3 row(s)
```

```
hbase(main):020:0> scan 'xyz2'
ROW                    COLUMN+CELL                                                                                                   
 1                     column=cf1:name, timestamp=1702710711996, value=lisi                                                          
 2                     column=cf1:name, timestamp=1702710894577, value=wangwu                                                        
 3                     column=cf1:name, timestamp=1702710975065, value=tom                                                           
3 row(s) in 0.0320 seconds
```

## 删除表

会同步删除

```
hbase(main):025:0> disable 'xyz5'
0 row(s) in 2.4360 seconds

hbase(main):026:0> drop 'xyz5'
0 row(s) in 1.3800 seconds

hive (default)> drop table hbase_table_5;
FAILED: Execution Error, return code 1 from org.apache.hadoop.hive.ql.exec.DDLTask. MetaException(message:org.apache.hadoop.hbase.TableNotFoundException: xyz5
...
```

## 表有多列

在`hbase.columns.mapping`中依次指定列的映射，引号中各项用逗号分隔

```
hive (default)> create table hbase_table_3(key int, name string, age string)
              > stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
              > with serdeproperties("hbase.columns.mapping"=":key,cf1:name,cf2:age")
              > tblproperties("hbase.table.name"="xyz3", "hbase.mapred.output.outputtable"="xyz3");
OK
Time taken: 3.959 seconds
hive (default)> insert into hbase_table_3 values(1,"zhangsan","22");
hive (default)> select * from hbase_table_3;
OK
hbase_table_3.key	hbase_table_3.name	hbase_table_3.age
1	zhangsan	22
Time taken: 0.306 seconds, Fetched: 1 row(s)

```

```
hbase(main):012:0> describe 'xyz3'
Table xyz3 is ENABLED                                                                                                                                
xyz3                                                                                                                                                 
COLUMN FAMILIES DESCRIPTION                                                                                                                          
{NAME => 'cf1', BLOOMFILTER => 'ROW', VERSIONS => '1', IN_MEMORY => 'false', KEEP_DELETED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'NONE', TTL => 'FO
REVER', COMPRESSION => 'NONE', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE => '0'}                            
{NAME => 'cf2', BLOOMFILTER => 'ROW', VERSIONS => '1', IN_MEMORY => 'false', KEEP_DELETED_CELLS => 'FALSE', DATA_BLOCK_ENCODING => 'NONE', TTL => 'FO
REVER', COMPRESSION => 'NONE', MIN_VERSIONS => '0', BLOCKCACHE => 'true', BLOCKSIZE => '65536', REPLICATION_SCOPE => '0'}                            
2 row(s) in 0.6600 seconds

hbase(main):013:0> scan 'xyz3'
ROW                    COLUMN+CELL                                                                                                   
 1                     column=cf1:name, timestamp=1702723621849, value=zhangsan                                                      
 1                     column=cf2:age, timestamp=1702723621849, value=22                                                             
1 row(s) in 0.2250 seconds
```

## 列映射属性

除了`hbase.columns.mapping`列映射属性，还有 `hbase.table.default.storage.type`，指定列是binary还是string，默认是string

如果将列指定为 binary，那么对应的 HBase 单元格中的字节应该是 HBase 的 Bytes 类产生的形式。

```
hive (default)> create table hbase_table_4(key int, name string)
              > stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
              > with serdeproperties("hbase.columns.mapping"=":key,cf1:name", "hbase.table.default.storage.type"="binary")
              > tblproperties("hbase.table.name"="xyz4", "hbase.mapred.output.outputtable"="xyz4");
OK
Time taken: 1.471 seconds
hive (default)> insert into hbase_table_4 values(1,"zhangsan");
```

```
hbase(main):023:0> scan 'xyz4'
ROW                     COLUMN+CELL                                                                                                   
 \x00\x00\x00\x01       column=cf1:name, timestamp=1702711875406, value=zhangsan                                                      
1 row(s) in 0.0290 seconds
```

也可以单独指定 `with serdeproperties("hbase.columns.mapping"=":key,cf1:name#b")`

## 映射时间戳

hive->hbase

```
hive (default)> create table hbase_table_5(key int, name string, start_date bigint)
              > stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
              > with serdeproperties("hbase.columns.mapping"=":key,cf1:name,:timestamp")
              > tblproperties("hbase.table.name"="xyz5", "hbase.mapred.output.outputtable"="xyz5");
OK
Time taken: 2.471 seconds
hive (default)> insert into hbase_table_5 values(1,"zhangsan",1692269092799);

hive (default)> select * from hbase_table_5;
OK
hbase_table_5.key	hbase_table_5.name	hbase_table_5.start_date
1	zhangsan	1692269092799
Time taken: 0.127 seconds, Fetched: 1 row(s)
```

```
hbase(main):034:0> scan 'xyz5'
ROW                    COLUMN+CELL                                                                                                   
 1                     column=cf1:name, timestamp=1692269092799, value=zhangsan                                                      
1 row(s) in 0.0400 seconds
```

hbase->hive

```
-- 前面创建了hbase表xyz2
hive (default)> create external table hbase_table_2_2(key int, value string, ts timestamp)
              > stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
              > with serdeproperties("hbase.columns.mapping" = ":key,cf1:name,:timestamp")
              > TBLPROPERTIES("hbase.table.name" = "xyz2", "hbase.mapred.output.outputtable" = "xyz2");
OK
Time taken: 0.249 seconds
hive (default)> select * from hbase_table_2_2;
OK
hbase_table_2_2.key	hbase_table_2_2.value	hbase_table_2_2.ts
1	lisi	2023-12-16 15:11:51.996
2	wangwu	2023-12-16 15:14:54.577
3	tom	2023-12-16 15:16:15.065
Time taken: 0.191 seconds, Fetched: 3 row(s)
```

使用 SERDEPROPERTIES 选项 `hbase.put.timestamp` 可以覆盖默认的当前时间戳

## 将多个hive表映射到同一个hbase表

```
hbase(main):007:0> create "xyz6","cf1"
0 row(s) in 1.2370 seconds

=> Hbase::Table - xyz6
hbase(main):008:0> put 'xyz6','1','cf1:name','zhangsan'
0 row(s) in 0.1570 seconds

hbase(main):009:0> put 'xyz6','1','cf1:age','30'
0 row(s) in 0.0040 seconds

hbase(main):010:0> scan 'xyz6'
ROW                     COLUMN+CELL                                                                                                   
 1                      column=cf1:age, timestamp=1702714033074, value=30                                                             
 1                      column=cf1:name, timestamp=1702714021735, value=zhangsan                                                      
1 row(s) in 0.0330 seconds
```

```
hive (default)> create external table hbase_table_6_1(key int, name string)
              > stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
              > with serdeproperties("hbase.columns.mapping"=":key,cf1:name")
              > tblproperties("hbase.table.name"="xyz6", "hbase.mapred.output.outputtable"="xyz6");
OK
Time taken: 0.165 seconds
hive (default)> create external table hbase_table_6_2(key int, age int)
              > stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
              > with serdeproperties("hbase.columns.mapping"=":key,cf1:age")
              > tblproperties("hbase.table.name"="xyz6", "hbase.mapred.output.outputtable"="xyz6");
OK
Time taken: 0.201 seconds

hive (default)> select * from hbase_table_6_1;
OK
hbase_table_6_1.key	hbase_table_6_1.name
1	zhangsan
Time taken: 0.129 seconds, Fetched: 1 row(s)
hive (default)> select * from hbase_table_6_2;
OK
hbase_table_6_2.key	hbase_table_6_2.age
1	30
Time taken: 0.122 seconds, Fetched: 1 row(s)
```

## map数据类型映射到列族

```
-- MAP 的键必须具有 string 数据类型，因为它是用来命名 HBase 列的
CREATE TABLE hbase_table_7(value map<string,int>, row_key int) 
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES ("hbase.columns.mapping" = "cf:,:key")
tblproperties("hbase.table.name"="xyz7", "hbase.mapred.output.outputtable"="xyz7");

hive (default)> insert into hbase_table_7 select map('weibo',100),1;
hive (default)> insert into hbase_table_7 select map('zhihu',220),2;
hive (default)> select * from hbase_table_7;
OK
hbase_table_7.value	hbase_table_7.row_key
{"weibo":100}	1
{"zhihu":220}	2
Time taken: 0.159 seconds, Fetched: 2 row(s)
```

```
hbase(main):018:0> scan 'xyz7'
ROW                     COLUMN+CELL                                                                                                   
 1                      column=cf:weibo, timestamp=1702725053184, value=100                                                           
 2                      column=cf:zhihu, timestamp=1702725089005, value=220                                                           
2 row(s) in 0.4510 seconds
```

## map数据类型映射到列前缀

```
hbase(main):022:0> create "xyz8","cf1"
0 row(s) in 2.3450 seconds

=> Hbase::Table - xyz8

hbase(main):014:0> put 'xyz8','1','cf1:class1_math',90
0 row(s) in 0.1760 seconds

hbase(main):015:0> put 'xyz8','2','cf1:class1_english',88
0 row(s) in 0.0120 seconds

hbase(main):016:0> put 'xyz8','3','cf1:class2_math',70
0 row(s) in 0.0070 seconds

hbase(main):017:0> scan 'xyz8'
ROW                     COLUMN+CELL                                                                                                   
 1                      column=cf1:class1_math, timestamp=1702726907472, value=90                                                     
 2                      column=cf1:class1_english, timestamp=1702726925896, value=88                                                  
 3                      column=cf1:class2_math, timestamp=1702726937780, value=70                                                     
3 row(s) in 0.0190 seconds
```

```
-- 需要加点号
hive (default)> create external table hbase_table_8(value map<string,int>, row_key int)
              > stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
              > with serdeproperties("hbase.columns.mapping"="cf1:class1_.*,:key","hbase.columns.mapping.prefix.hide" = "true")
              > tblproperties("hbase.table.name"="xyz8", "hbase.mapred.output.outputtable"="xyz8");
OK
Time taken: 0.175 seconds
hive (default)> select * from hbase_table_8;
OK
hbase_table_8.value	hbase_table_9.row_key
{"class1_math":90}	1
{"class1_english":88}	2
{}	3
Time taken: 0.13 seconds, Fetched: 3 row(s)
```

## hbase组合键

将 HBase 的 row key 映射到 Hive 结构体，并使用 `ROW FORMAT DELIMITED...COLLECTION ITEMS TERMINATED BY`

```
hbase(main):018:0> create "xyz9","cf1"
0 row(s) in 2.4050 seconds

=> Hbase::Table - xyz9

hbase(main):022:0> put 'xyz9','class1,1','cf1:name','zhangsan'
0 row(s) in 0.0240 seconds

hbase(main):023:0> put 'xyz9','class1,2','cf1:name','lisi'
0 row(s) in 0.0090 seconds

hbase(main):024:0> scan 'xyz9'
ROW                  COLUMN+CELL                                                                                                   
 class1,1            column=cf1:name, timestamp=1702728209769, value=zhangsan                                                      
 class1,2            column=cf1:name, timestamp=1702728215094, value=lisi   
```

```
hive (default)> create external table hbase_table_9(row_key struct<f1:string,f2:string>, value string)
              > ROW FORMAT DELIMITED 
              > COLLECTION ITEMS TERMINATED BY ',' 
              > stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
              > with serdeproperties("hbase.columns.mapping"=":key, cf1:name")
              > tblproperties("hbase.table.name"="xyz9", "hbase.mapred.output.outputtable"="xyz9");
OK
Time taken: 0.296 seconds
hive (default)> select * from hbase_table_9;
OK
hbase_table_9.row_key	hbase_table_9.value
{"f1":"class1","f2":"1"}	zhangsan
{"f1":"class1","f2":"2"}	lisi
Time taken: 0.208 seconds, Fetched: 2 row(s)
```

## 映射hbase中的avro列

```
CREATE EXTERNAL TABLE test_hbase_avro
ROW FORMAT SERDE 'org.apache.hadoop.hive.hbase.HBaseSerDe'
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
    "hbase.columns.mapping" = ":key,test_col_fam:test_col",
    "test_col_fam.test_col.serialization.type" = "avro",
    "test_col_fam.test_col.avro.schema.url" = "hdfs://testcluster/tmp/schema.avsc")
TBLPROPERTIES (
    "hbase.table.name" = "hbase_avro_table",
    "hbase.mapred.output.outputtable" = "hbase_avro_table",
    "hbase.struct.autogenerate"="true");
```

## 唯一键问题

HBase 表和 Hive 表的一个细微区别是，HBase 表有一个唯一键，而 Hive 表没有。

当有多个具有相同键的行插入到 HBase 时，只存储其中的一行(选择是任意的，所以不要依赖于 HBase 来选择正确的行)。

```
hive (default)> select * from hbase_table_9;
OK
hbase_table_9.row_key	hbase_table_9.value
{"f1":"class1","f2":"1"}	zhangsan
{"f1":"class1","f2":"2"}	lisi
Time taken: 0.208 seconds, Fetched: 2 row(s)

hive (default)> insert into hbase_table_9 select named_struct("f1","class1","f2","1"),"wangwu";
hive (default)> select * from hbase_table_9;
OK
hbase_table_9.row_key	hbase_table_9.value
{"f1":"class1","f2":"1"}	wangwu
{"f1":"class1","f2":"2"}	lisi
Time taken: 0.178 seconds, Fetched: 2 row(s)
```

```
hbase(main):024:0> scan 'xyz9'
ROW                 COLUMN+CELL                                                                                                   
 class1,1           column=cf1:name, timestamp=1702728209769, value=zhangsan                                                      
 class1,2           column=cf1:name, timestamp=1702728215094, value=lisi                                                          
2 row(s) in 0.0480 seconds

hbase(main):025:0> scan 'xyz9'
ROW                  COLUMN+CELL                                                                                                   
 class1,1            column=cf1:name, timestamp=1702729567383, value=wangwu                                                        
 class1,2            column=cf1:name, timestamp=1702728215094, value=lisi                                                          
2 row(s) in 0.0660 seconds
```

## hive insert overwrite

HBase 表与其他 Hive 表的另一个区别是，使用 INSERT OVERWRITE 时，不会删除表中已有的行。

但是，如果现有行有与新行匹配的键，则会覆盖它们。

```
hive (default)> insert overwrite table hbase_table_9 select named_struct("f1","class1","f2","1"),"mike";
hive (default)> select * from hbase_table_9;
OK
hbase_table_9.row_key	hbase_table_9.value
{"f1":"class1","f2":"1"}	mike
{"f1":"class1","f2":"2"}	tom
Time taken: 0.172 seconds, Fetched: 2 row(s)
```

```
hbase(main):027:0> scan 'xyz9'
ROW                 COLUMN+CELL                                                                                                   
 class1,1           column=cf1:name, timestamp=1702729567383, value=wangwu                                                        
 class1,2           column=cf1:name, timestamp=1702729638672, value=tom                                                           
2 row(s) in 0.0180 seconds

hbase(main):028:0> scan 'xyz9'
ROW                 COLUMN+CELL                                                                                                   
 class1,1           column=cf1:name, timestamp=1702729938391, value=mike                                                          
 class1,2           column=cf1:name, timestamp=1702729638672, value=tom                                                           
2 row(s) in 0.0270 seconds
```

[官网](https://cwiki.apache.org/confluence/display/Hive/HBaseIntegration)

[官网翻译](https://github.com/ZGG2016/hive/blob/6f79bf163b7b1e9ab4366f72ea98886db0c76a42/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20HBase%20Integration.md)