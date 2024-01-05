# create table语句

[TOC]

## TBLPROPERTIES ("immutable"="true") or ("immutable"="false")

```sql
create table t5(id int, name string) TBLPROPERTIES ("immutable"="true");

insert into t5 values(1,"zhangsan");

0: jdbc:hive2://zgg-server:10000> select * from t5;
+--------+-----------+
| t5.id  |  t5.name  |
+--------+-----------+
| 1      | zhangsan  |
+--------+-----------+

0: jdbc:hive2://zgg-server:10000> insert into t5 values(2,"lisi");
Error: Error while compiling statement: FAILED: SemanticException [Error 10256]: Inserting into a non-empty immutable table is not allowed t5 (state=42000,code=10256)

```

`INSERT INTO` will append to the table or partition, keeping the existing data intact【完整】. (Note: `INSERT INTO` syntax is only available starting in version 0.8.)

As of Hive 0.13.0, a table can be made immutable【不可变】 by creating it with `TBLPROPERTIES ("immutable"="true")`. The default is `"immutable"="false"`.

**`INSERT INTO` behavior into an immutable table is disallowed if any data is already present, although INSERT INTO still works if the immutable table is empty. The behavior of INSERT OVERWRITE is not affected by the "immutable" table property**.

An immutable table is protected against accidental updates due to a script loading data into it being run multiple times by mistake. The first insert into an immutable table succeeds and successive inserts fail, resulting in only one set of data in the table, instead of silently succeeding with multiple copies of the data in the table. 

## TBLPROPERTIES ("external.table.purge"="true")

```sql
create external table t6(id int, name string) TBLPROPERTIES ("external.table.purge"="true");

insert into t6 values(1,"zhangsan");

0: jdbc:hive2://zgg-server:10000> select * from t6;
+--------+-----------+
| t6.id  |  t6.name  |
+--------+-----------+
| 1      | zhangsan  |
+--------+-----------+

root@zgg-server:/opt# hadoop fs -ls /user/hive/warehouse/t6
Found 1 items
-rw-r--r--   1 root supergroup         11 2024-01-03 13:32 /user/hive/warehouse/t6/000000_0

drop table t6;

-- 也会删除数据
root@zgg-server:/opt# hadoop fs -ls /user/hive/warehouse/t6
ls: `/user/hive/warehouse/t6': No such file or directory
```

## NULL DEFINED AS

A custom NULL format can also be specified using the 'NULL DEFINED AS' clause (default is '\N').

```sql
create table t6_1(id int, name string) 
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
stored as textfile;

create table t6_2(id int, name string) 
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
NULL defined as ''
stored as textfile;

insert into t6_1 values(1,NULL);

insert into t6_2 values(1,NULL);

root@zgg-server:~# hadoop fs -cat /user/hive/warehouse/t6_1/000000_0
1,\N
root@zgg-server:~# hadoop fs -cat /user/hive/warehouse/t6_2/000000_0
1,
```

## ESCAPED BY

Enable escaping for the delimiter characters by using the 'ESCAPED BY' clause (such as ESCAPED BY '\')

Escaping is needed if you want to work with data that can contain these delimiter characters.

```sql
create table t7_1(id int, name string) 
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
ESCAPED BY '\\'
stored as textfile;

create table t7_2(id int, name string) 
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
stored as textfile;

insert into t7_1 values(1,'a,b');
insert into t7_2 values(1,'a,b');

0: jdbc:hive2://zgg-server:10000> select * from t7_1;
+----------+------------+
| t7_1.id  | t7_1.name  |
+----------+------------+
| 1        | a,b        |
+----------+------------+

0: jdbc:hive2://zgg-server:10000> select * from t7_2;
+----------+------------+
| t7_2.id  | t7_2.name  |
+----------+------------+
| 1        | a          |
+----------+------------+
```

## RegexSerDe

对于一行数据，将其划分到各列的方法

```sql
CREATE TABLE logs
(
`host` STRING,
`identity` STRING,
`username` STRING,
`time` STRING,
`request` STRING,
`status` STRING,
`size` STRING,
`referer` STRING,
`agent` STRING
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.RegexSerDe'
WITH SERDEPROPERTIES (
"input.regex" = "([^ ]*) ([^ ]*) ([^ ]*) (\\[.*\\]) (\".*?\") (-|[0-9]*) (-|[0-9]*) (\".*?\") (\".*?\")",
"output.format.string" = "%1$s %2$s %3$s %4$s %5$s %6$s %7$s %8$s %9$s"
)
STORED AS TEXTFILE;

root@zgg-server:~/data# cat nginx.txt
192.168.1.128 - - [09/Jan/2015:12:38:08 +0800] "GET /avatar/helloworld.png HTTP/1.1" 200 1521 "http://write.blog.linuxidc.net/postlist" "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36"
183.60.212.153 - - [19/Feb/2015:10:23:29 +0800] "GET /o2o/media.html?menu=3 HTTP/1.1" 200 16691 "-" "Mozilla/5.0 (compatible; baiduuSpider; +http://www.baiduu.com/search/spider.html)"

0: jdbc:hive2://zgg-server:10000> load data local inpath '/root/data/nginx.txt' into table logs;

0: jdbc:hive2://zgg-server:10000> select * from logs;
+-----------------+----------------+----------------+-------------------------------+----------------------------------------+--------------+------------+--------------------------------------------+----------------------------------------------------+
|    logs.host    | logs.identity  | logs.username  |           logs.time           |              logs.request              | logs.status  | logs.size  |                logs.referer                |                     logs.agent                     |
+-----------------+----------------+----------------+-------------------------------+----------------------------------------+--------------+------------+--------------------------------------------+----------------------------------------------------+
| 192.168.1.128   | -              | -              | [09/Jan/2015:12:38:08 +0800]  | "GET /avatar/helloworld.png HTTP/1.1"  | 200          | 1521       | "http://write.blog.linuxidc.net/postlist"  | "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36" |
| 183.60.212.153  | -              | -              | [19/Feb/2015:10:23:29 +0800]  | "GET /o2o/media.html?menu=3 HTTP/1.1"  | 200          | 16691      | "-"                                        | "Mozilla/5.0 (compatible; baiduuSpider; +http://www.baiduu.com/search/spider.html)" |
+-----------------+----------------+----------------+-------------------------------+----------------------------------------+--------------+------------+--------------------------------------------+----------------------------------------------------+
```

## JsonSerDe

```sql
-- hive3.0.0开始
create table tb_json_1 (
`device` string,
`deviceType` string,
`signal` double,
`time` string
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.JsonSerDe'
STORED AS TEXTFILE;

root@zgg-server:~/data# cat device.json 
{"device":"device_10","deviceType":"kafka","signal":98.0,"time":11111111}

0: jdbc:hive2://zgg-server:10000> load data local inpath '/root/data/device.json' into table tb_json_1;

0: jdbc:hive2://zgg-server:10000> select * from tb_json_1;
+-------------------+-----------------------+-------------------+-----------------+
| tb_json_1.device  | tb_json_1.devicetype  | tb_json_1.signal  | tb_json_1.time  |
+-------------------+-----------------------+-------------------+-----------------+
| device_10         | kafka                 | 98.0              | 11111111        |
+-------------------+-----------------------+-------------------+-----------------+
```

```sql
-- hive4.0.0开始
create table tb_json_2(
`device` string,
`deviceType` string,
`signal` double,
`time` string
)
STORED AS JSONFILE;

0: jdbc:hive2://zgg-server:10000> load data local inpath '/root/data/device.json' into table tb_json_2;

0: jdbc:hive2://zgg-server:10000> select * from tb_json_2;
+-------------------+-----------------------+-------------------+-----------------+
| tb_json_1.device  | tb_json_1.devicetype  | tb_json_1.signal  | tb_json_1.time  |
+-------------------+-----------------------+-------------------+-----------------+
| device_10         | kafka                 | 98.0              | 11111111        |
+-------------------+-----------------------+-------------------+-----------------+
```

## OpenCSVSerde

Default properties for SerDe is Comma-Separated (CSV) file
 
	DEFAULT_ESCAPE_CHARACTER \   CSV文件中使用的转义字符为反斜杠（\）
	
	DEFAULT_QUOTE_CHARACTER  "   CSV文件中使用的引号字符为双引号（"）
	
	DEFAULT_SEPARATOR        ,   CSV文件中使用的分隔符为逗号（,）

```sql
drop table tb_csv_1;
create table tb_csv_1 (
`id` string,
`price` string
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
STORED AS TEXTFILE;

-- DEFAULT_QUOTE_CHARACTER  "
hive@8361ca1351e2:/opt/hive/test-data$ cat tb_csv_1.csv 
1,"10000.00"
2,'30000.00'

0: jdbc:hive2://localhost:10000> load data local inpath '/opt/hive/test-data/tb_csv_1.csv' into table tb_csv_1;
0: jdbc:hive2://localhost:10000> select * from tb_csv_1;
+--------------+-----------------+
| tb_csv_1.id  | tb_csv_1.price  |
+--------------+-----------------+
| 1            | 10000.00        |
| 2            | '30000.00'      |
+--------------+-----------------+

-- DEFAULT_ESCAPE_CHARACTER \
0: jdbc:hive2://localhost:10000> insert into tb_csv_1 values(3,'4000,000.00');
0: jdbc:hive2://localhost:10000> select * from tb_csv_1;
+--------------+-----------------+
| tb_csv_1.id  | tb_csv_1.price  |
+--------------+-----------------+
| 3            | 4000,000.00     |
| 1            | 10000.00        |
| 2            | '30000.00'      |
+--------------+-----------------+

```

```sql
drop table tb_csv_2;
create table tb_csv_2 (
`id` string,
`price` string
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
WITH SERDEPROPERTIES (
   "separatorChar" = "|",
   "quoteChar"     = "'",
   "escapeChar"    = "\\"
) 
STORED AS TEXTFILE; 

-- DEFAULT_QUOTE_CHARACTER  '
hive@8361ca1351e2:/opt/hive/test-data$ cat tb_csv_2.csv 
1|"10000.00"
2|'30000.00'

0: jdbc:hive2://localhost:10000> load data local inpath '/opt/hive/test-data/tb_csv_2.csv' into table tb_csv_2;
0: jdbc:hive2://localhost:10000> select * from tb_csv_2;
+--------------+-----------------+
| tb_csv_2.id  | tb_csv_2.price  |
+--------------+-----------------+
| 1            | "10000.00"      |
| 2            | 30000.00        |
+--------------+-----------------+

-- DEFAULT_ESCAPE_CHARACTER \
0: jdbc:hive2://localhost:10000> insert into tb_csv_2 values(3,'4000|000.00');
0: jdbc:hive2://localhost:10000> select * from tb_csv_2;
+--------------+-----------------+
| tb_csv_2.id  | tb_csv_2.price  |
+--------------+-----------------+
| 1            | "10000.00"      |
| 2            | 30000.00        |
| 3            | 4000|000.00     |
+--------------+-----------------+
```

## Create Table As Select (CTAS)

The table created by CTAS is atomic, meaning that the table is not seen by other users until all the query results are populated. 

So other users will either see the table with the complete results of the query or will not see the table at all.

- The target table cannot be an external table.
- The target table cannot be a list bucketing table.

```sql
drop table tb_ctas_1;
create table tb_ctas_1(
id int,
name string)
partitioned by(deal_day string)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
WITH SERDEPROPERTIES (
   "separatorChar" = "|",
   "quoteChar"     = "'",
   "escapeChar"    = "\\"
) 
STORED AS TEXTFILE
tblproperties("comment"="table......comment"); 

insert into tb_ctas_1 partition(deal_day='20220101') values(1,"zhangsan");

0: jdbc:hive2://localhost:10000> desc formatted tb_ctas_1;
+-------------------------------+----------------------------------------------------+-----------------------------+
|           col_name            |                     data_type                      |           comment           |
+-------------------------------+----------------------------------------------------+-----------------------------+
| id                            | int                                                |                             |
| name                          | string                                             |                             |
|                               | NULL                                               | NULL                        |
| # Partition Information       | NULL                                               | NULL                        |
| # col_name                    | data_type                                          | comment                     |
| deal_day                      | string                                             |                             |
|                               | NULL                                               | NULL                        |
| # Detailed Table Information  | NULL                                               | NULL                        |
| Database:                     | default                                            | NULL                        |
| OwnerType:                    | USER                                               | NULL                        |
| Owner:                        | hive                                               | NULL                        |
| CreateTime:                   | Fri Jan 05 03:25:36 UTC 2024                       | NULL                        |
| LastAccessTime:               | UNKNOWN                                            | NULL                        |
| Retention:                    | 0                                                  | NULL                        |
| Location:                     | file:/opt/hive/data/warehouse/tb_ctas_1            | NULL                        |
| Table Type:                   | EXTERNAL_TABLE                                     | NULL                        |
| Table Parameters:             | NULL                                               | NULL                        |
|                               | COLUMN_STATS_ACCURATE                              | {\"BASIC_STATS\":\"true\"}  |
|                               | EXTERNAL                                           | TRUE                        |
|                               | TRANSLATED_TO_EXTERNAL                             | TRUE                        |
|                               | bucketing_version                                  | 2                           |
|                               | comment                                            | table......comment          |
|                               | external.table.purge                               | TRUE                        |
|                               | numFiles                                           | 1                           |
|                               | numPartitions                                      | 1                           |
|                               | numRows                                            | 1                           |
|                               | rawDataSize                                        | 0                           |
|                               | totalSize                                          | 15                          |
|                               | transient_lastDdlTime                              | 1704425136                  |
|                               | NULL                                               | NULL                        |
| # Storage Information         | NULL                                               | NULL                        |
| SerDe Library:                | org.apache.hadoop.hive.serde2.OpenCSVSerde         | NULL                        |
| InputFormat:                  | org.apache.hadoop.mapred.TextInputFormat           | NULL                        |
| OutputFormat:                 | org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat | NULL                        |
| Compressed:                   | No                                                 | NULL                        |
| Num Buckets:                  | -1                                                 | NULL                        |
| Bucket Columns:               | []                                                 | NULL                        |
| Sort Columns:                 | []                                                 | NULL                        |
| Storage Desc Params:          | NULL                                               | NULL                        |
|                               | escapeChar                                         | \\                          |
|                               | quoteChar                                          | '                           |
|                               | separatorChar                                      | |                           |
|                               | serialization.format                               | 1                           |
+-------------------------------+----------------------------------------------------+-----------------------------+
```
```sql
create table tb_ctas_2 as select * from tb_ctas_1;

0: jdbc:hive2://localhost:10000> desc formatted tb_ctas_2;
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
|           col_name            |                     data_type                      |                      comment                       |
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
| id                            | string                                             |                                                    |
| name                          | string                                             |                                                    |
| deal_day                      | string                                             |                                                    |
|                               | NULL                                               | NULL                                               |
| # Detailed Table Information  | NULL                                               | NULL                                               |
| Database:                     | default                                            | NULL                                               |
| OwnerType:                    | USER                                               | NULL                                               |
| Owner:                        | hive                                               | NULL                                               |
| CreateTime:                   | Fri Jan 05 03:26:02 UTC 2024                       | NULL                                               |
| LastAccessTime:               | UNKNOWN                                            | NULL                                               |
| Retention:                    | 0                                                  | NULL                                               |
| Location:                     | file:/opt/hive/data/warehouse/tb_ctas_2            | NULL                                               |
| Table Type:                   | EXTERNAL_TABLE                                     | NULL                                               |
| Table Parameters:             | NULL                                               | NULL                                               |
|                               | COLUMN_STATS_ACCURATE                              | {\"BASIC_STATS\":\"true\",\"COLUMN_STATS\":{\"deal_day\":\"true\",\"id\":\"true\",\"name\":\"true\"}} |
|                               | EXTERNAL                                           | TRUE                                               |
|                               | TRANSLATED_TO_EXTERNAL                             | TRUE                                               |
|                               | bucketing_version                                  | 2                                                  |
|                               | external.table.purge                               | TRUE                                               |
|                               | numFiles                                           | 1                                                  |
|                               | numRows                                            | 1                                                  |
|                               | rawDataSize                                        | 19                                                 |
|                               | totalSize                                          | 20                                                 |
|                               | transient_lastDdlTime                              | 1704425162                                         |
|                               | NULL                                               | NULL                                               |
| # Storage Information         | NULL                                               | NULL                                               |
| SerDe Library:                | org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe | NULL                                               |
| InputFormat:                  | org.apache.hadoop.mapred.TextInputFormat           | NULL                                               |
| OutputFormat:                 | org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat | NULL                                               |
| Compressed:                   | No                                                 | NULL                                               |
| Num Buckets:                  | -1                                                 | NULL                                               |
| Bucket Columns:               | []                                                 | NULL                                               |
| Sort Columns:                 | []                                                 | NULL                                               |
| Storage Desc Params:          | NULL                                               | NULL                                               |
|                               | serialization.format                               | 1                                                  |
+-------------------------------+----------------------------------------------------+----------------------------------------------------+

```

## Create Table Like

复制表的定义，但不复制数据。除了表名不同，其他都一样。

```sql
create table tb_ctl_1 like tb_ctas_1
tblproperties("comment"="tb_ctl_1......comment"); 

0: jdbc:hive2://localhost:10000> desc formatted tb_ctl_1;
+-------------------------------+----------------------------------------------------+-----------------------------+
|           col_name            |                     data_type                      |           comment           |
+-------------------------------+----------------------------------------------------+-----------------------------+
| id                            | int                                                |                             |
| name                          | string                                             |                             |
|                               | NULL                                               | NULL                        |
| # Partition Information       | NULL                                               | NULL                        |
| # col_name                    | data_type                                          | comment                     |
| deal_day                      | string                                             |                             |
|                               | NULL                                               | NULL                        |
| # Detailed Table Information  | NULL                                               | NULL                        |
| Database:                     | default                                            | NULL                        |
| OwnerType:                    | USER                                               | NULL                        |
| Owner:                        | hive                                               | NULL                        |
| CreateTime:                   | Fri Jan 05 06:06:15 UTC 2024                       | NULL                        |
| LastAccessTime:               | UNKNOWN                                            | NULL                        |
| Retention:                    | 0                                                  | NULL                        |
| Location:                     | file:/opt/hive/data/warehouse/tb_ctl_1             | NULL                        |
| Table Type:                   | EXTERNAL_TABLE                                     | NULL                        |
| Table Parameters:             | NULL                                               | NULL                        |
|                               | COLUMN_STATS_ACCURATE                              | {\"BASIC_STATS\":\"true\"}  |
|                               | EXTERNAL                                           | TRUE                        |
|                               | TRANSLATED_TO_EXTERNAL                             | TRUE                        |
|                               | bucketing_version                                  | 2                           |
|                               | comment                                            | tb_ctl_1......comment       |
|                               | external.table.purge                               | TRUE                        |
|                               | numFiles                                           | 0                           |
|                               | numPartitions                                      | 0                           |
|                               | numRows                                            | 0                           |
|                               | rawDataSize                                        | 0                           |
|                               | totalSize                                          | 0                           |
|                               | transient_lastDdlTime                              | 1704434775                  |
|                               | NULL                                               | NULL                        |
| # Storage Information         | NULL                                               | NULL                        |
| SerDe Library:                | org.apache.hadoop.hive.serde2.OpenCSVSerde         | NULL                        |
| InputFormat:                  | org.apache.hadoop.mapred.TextInputFormat           | NULL                        |
| OutputFormat:                 | org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat | NULL                        |
| Compressed:                   | No                                                 | NULL                        |
| Num Buckets:                  | -1                                                 | NULL                        |
| Bucket Columns:               | []                                                 | NULL                        |
| Sort Columns:                 | []                                                 | NULL                        |
| Storage Desc Params:          | NULL                                               | NULL                        |
|                               | escapeChar                                         | \\                          |
|                               | quoteChar                                          | '                           |
|                               | separatorChar                                      | |                           |
|                               | serialization.format                               | 1                           |
+-------------------------------+----------------------------------------------------+-----------------------------+
```
Before Hive 0.8.0, `CREATE TABLE LIKE view_name` would make a copy of the view. 

In Hive 0.8.0 and later releases, `CREATE TABLE LIKE view_name` creates a table by adopting the schema of view_name (fields and partition columns) using defaults for SerDe and file formats.

```sql
create view view_ctl as select * from tb_ctas_1;

0: jdbc:hive2://localhost:10000> desc formatted view_ctl;
+-------------------------------+----------------------------------------------------+-----------------------+
|           col_name            |                     data_type                      |        comment        |
+-------------------------------+----------------------------------------------------+-----------------------+
| id                            | string                                             |                       |
| name                          | string                                             |                       |
| deal_day                      | string                                             |                       |
|                               | NULL                                               | NULL                  |
| # Detailed Table Information  | NULL                                               | NULL                  |
| Database:                     | default                                            | NULL                  |
| OwnerType:                    | USER                                               | NULL                  |
| Owner:                        | hive                                               | NULL                  |
| CreateTime:                   | Fri Jan 05 06:09:33 UTC 2024                       | NULL                  |
| LastAccessTime:               | UNKNOWN                                            | NULL                  |
| Retention:                    | 0                                                  | NULL                  |
| Table Type:                   | VIRTUAL_VIEW                                       | NULL                  |
| Table Parameters:             | NULL                                               | NULL                  |
|                               | bucketing_version                                  | 2                     |
|                               | transient_lastDdlTime                              | 1704434973            |
|                               | NULL                                               | NULL                  |
| # Storage Information         | NULL                                               | NULL                  |
| SerDe Library:                | null                                               | NULL                  |
| InputFormat:                  | org.apache.hadoop.mapred.TextInputFormat           | NULL                  |
| OutputFormat:                 | org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat | NULL                  |
| Compressed:                   | No                                                 | NULL                  |
| Num Buckets:                  | -1                                                 | NULL                  |
| Bucket Columns:               | []                                                 | NULL                  |
| Sort Columns:                 | []                                                 | NULL                  |
|                               | NULL                                               | NULL                  |
| # View Information            | NULL                                               | NULL                  |
| Original Query:               | select * from tb_ctas_1                            | NULL                  |
| Expanded Query:               | select `tb_ctas_1`.`id`, `tb_ctas_1`.`name`, `tb_ctas_1`.`deal_day` from `default`.`tb_ctas_1` | NULL                  |
+-------------------------------+----------------------------------------------------+-----------------------+

creat table tb_ctl_2 like view_ctl;

0: jdbc:hive2://localhost:10000> desc formatted tb_ctl_2;
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
|           col_name            |                     data_type                      |                      comment                       |
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
| id                            | string                                             |                                                    |
| name                          | string                                             |                                                    |
| deal_day                      | string                                             |                                                    |
|                               | NULL                                               | NULL                                               |
| # Detailed Table Information  | NULL                                               | NULL                                               |
| Database:                     | default                                            | NULL                                               |
| OwnerType:                    | USER                                               | NULL                                               |
| Owner:                        | hive                                               | NULL                                               |
| CreateTime:                   | Fri Jan 05 06:12:14 UTC 2024                       | NULL                                               |
| LastAccessTime:               | UNKNOWN                                            | NULL                                               |
| Retention:                    | 0                                                  | NULL                                               |
| Location:                     | file:/opt/hive/data/warehouse/tb_ctl_2             | NULL                                               |
| Table Type:                   | EXTERNAL_TABLE                                     | NULL                                               |
| Table Parameters:             | NULL                                               | NULL                                               |
|                               | COLUMN_STATS_ACCURATE                              | {\"BASIC_STATS\":\"true\",\"COLUMN_STATS\":{\"deal_day\":\"true\",\"id\":\"true\",\"name\":\"true\"}} |
|                               | EXTERNAL                                           | TRUE                                               |
|                               | TRANSLATED_TO_EXTERNAL                             | TRUE                                               |
|                               | bucketing_version                                  | 2                                                  |
|                               | external.table.purge                               | TRUE                                               |
|                               | numFiles                                           | 0                                                  |
|                               | numRows                                            | 0                                                  |
|                               | rawDataSize                                        | 0                                                  |
|                               | totalSize                                          | 0                                                  |
|                               | transient_lastDdlTime                              | 1704435134                                         |
|                               | NULL                                               | NULL                                               |
| # Storage Information         | NULL                                               | NULL                                               |
| SerDe Library:                | org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe | NULL                                               |
| InputFormat:                  | org.apache.hadoop.mapred.TextInputFormat           | NULL                                               |
| OutputFormat:                 | org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat | NULL                                               |
| Compressed:                   | No                                                 | NULL                                               |
| Num Buckets:                  | -1                                                 | NULL                                               |
| Bucket Columns:               | []                                                 | NULL                                               |
| Sort Columns:                 | []                                                 | NULL                                               |
| Storage Desc Params:          | NULL                                               | NULL                                               |
|                               | serialization.format                               | 1                                                  |
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
```

## Temporary Tables

临时表仅存在当前会话级别。

如果临时表名称与已存在的持久表名相同，那么在这个会话中，访问这个表名时，就会指向临时表

- 不支持分区列
- 不支持创建索引

```sql
-- 已存在tb_csv_2表
create temporary table tb_csv_2(`id` string,`price` string);

0: jdbc:hive2://localhost:10000> insert into tb_csv_2 values('1','11');
0: jdbc:hive2://localhost:10000> select * from tb_csv_2;
+--------------+-----------------+
| tb_csv_2.id  | tb_csv_2.price  |
+--------------+-----------------+
| 1            | 11              |
+--------------+-----------------+
```

## Constraints

Some SQL tools generate more efficient queries when constraints are present. 

Since these constraints are not validated, an upstream system needs to ensure data integrity before it is loaded into Hive.

```sql
create table pk(id int, primary key(id) disable novalidate, name string, age int);

0: jdbc:hive2://localhost:10000> insert into pk values(1,"zhangsan",34),(2,"lisi",56);
0: jdbc:hive2://localhost:10000> select * from pk;
+--------+-----------+---------+
| pk.id  |  pk.name  | pk.age  |
+--------+-----------+---------+
| 1      | zhangsan  | 34      |
| 2      | lisi      | 56      |
+--------+-----------+---------+
0: jdbc:hive2://localhost:10000> desc formatted pk;
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
|           col_name            |                     data_type                      |                      comment                       |
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
...
| # Constraints                 | NULL                                               | NULL                                               |
|                               | NULL                                               | NULL                                               |
| # Primary Key                 | NULL                                               | NULL                                               |
| Table:                        | default.pk                                         | NULL                                               |
| Constraint Name:              | pk_65866204_1704436990270_0                        | NULL                                               |
| Column Name:                  | id                                                 | NULL                                               |
+-------------------------------+----------------------------------------------------+----------------------------------------------------+


create table fk(uid int, constraint c1 foreign key(uid) references pk(id) disable novalidate, amount string);
0: jdbc:hive2://localhost:10000> insert into fk values(1,"555"),(2,"245");
0: jdbc:hive2://localhost:10000> select * from fk;
+---------+------------+
| fk.uid  | fk.amount  |
+---------+------------+
| 1       | 555        |
| 2       | 245        |
+---------+------------+
0: jdbc:hive2://localhost:10000> desc formatted fk;
+-----------------------------------+----------------------------------------------------+----------------------------------------------------+
|             col_name              |                     data_type                      |                      comment                       |
+-----------------------------------+----------------------------------------------------+----------------------------------------------------+
.....
|                                   | NULL                                               | NULL                                               |
| # Constraints                     | NULL                                               | NULL                                               |
|                                   | NULL                                               | NULL                                               |
| # Foreign Keys                    | NULL                                               | NULL                                               |
| Table:                            | default.fk                                         | NULL                                               |
| Constraint Name:                  | c1                                                 | NULL                                               |
| Parent Column Name:default.pk.id  | Column Name:uid                                    | Key Sequence:1                                     |
|                                   | NULL                                               | NULL                                               |
+-----------------------------------+----------------------------------------------------+----------------------------------------------------+

0: jdbc:hive2://localhost:10000> select pk.name, fk.amount from fk, pk where fk.uid=pk.id;
+-----------+------------+
|  pk.name  | fk.amount  |
+-----------+------------+
| zhangsan  | 555        |
| lisi      | 245        |
+-----------+------------+
```

```sql
create table constraints1(
id integer, 
price double, CONSTRAINT c1 CHECK (price > 0 AND price <= 1000)
);

0: jdbc:hive2://localhost:10000> insert into constraints1 values(1, 10000.11);
.....
ERROR : Vertex failed, vertexName=Map 1, vertexId=vertex_1704439523690_0001_2_00, diagnostics=[Task failed, taskId=task_1704439523690_0001_2_00_000000, diagnostics=[TaskAttempt 0 failed, info=[Error: Error while running task ( failure ) : java.lang.RuntimeException: org.apache.hadoop.hive.ql.exec.errors.DataConstraintViolationError: Either CHECK or NOT NULL constraint violated!
```

```sql
--TODO
create table constraints2(
id integer,
price double NOT NULL
);
```

```sql
--TODO
create table constraints3(
id integer, constraint c1 UNIQUE(id) disable novalidate,
price double 
);
```

```sql
--TODO
create table constraints4(
id integer,
usr string DEFAULT current_user()
);
```