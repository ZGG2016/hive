# hive归档

[TOC]

- 不能是外部表

	```
	ERROR : FAILED: Execution Error, return code 40000 from org.apache.hadoop.hive.ql.ddl.DDLTask. ARCHIVE can only be performed on managed tables

	0: jdbc:hive2://zgg-server:10000> alter table t2 set tblproperties('EXTERNAL'='FALSE');
	```

- 把 hadoop 目录下的归档相关的 jar 包复制到 hive lib 目录下

	```
	Error: Error while compiling statement: FAILED: Execution Error, return code 40000 from org.apache.hadoop.hive.ql.ddl.DDLTask. org/apache/hadoop/tools/HadoopArchives (state=08S01,code=40000)

	root@zgg-server:/opt/hadoop-3.3.6/share/hadoop/tools/lib# cp hadoop-archives-3.3.6.jar /opt/hive-4.0.0/lib/

	重启服务
	```

```sql
-- 启用
set hive.archive.enabled=true;

0: jdbc:hive2://zgg-server:10000> create table t2(id int, name string) partitioned by(deal_day string);
0: jdbc:hive2://zgg-server:10000> insert into t2 partition(deal_day='20230110') values(1,'aa'),(2,'bb'),(3,'cc'),(4,'dd'),(1,'aa'),(2,'bb'),(3,'cc'),(4,'dd');

root@zgg-server:~# hadoop fs -ls /user/hive/warehouse/t2/deal_day=20230110
Found 1 items
-rw-r--r--   1 root supergroup         40 2023-12-29 07:01 /user/hive/warehouse/t2/deal_day=20230110/000000_0

-- 归档
0: jdbc:hive2://zgg-server:10000> alter table t2 archive partition(deal_day='20230110');

root@zgg-server:/opt/hadoop-3.3.6/share/hadoop/tools/lib# hadoop fs -ls /user/hive/warehouse/t2/deal_day=20230110
Found 1 items
drwxr-xr-x   - root supergroup          0 2023-12-29 06:58 /user/hive/warehouse/t2/deal_day=20230110/data.har

root@zgg-server:~# hadoop fs -ls /user/hive/warehouse/t2/deal_day=20230110/data.har
Found 4 items
-rw-r--r--   1 root supergroup          0 2023-12-29 07:03 /user/hive/warehouse/t2/deal_day=20230110/data.har/_SUCCESS
-rw-r--r--   3 root supergroup        296 2023-12-29 07:03 /user/hive/warehouse/t2/deal_day=20230110/data.har/_index
-rw-r--r--   3 root supergroup         23 2023-12-29 07:03 /user/hive/warehouse/t2/deal_day=20230110/data.har/_masterindex
-rw-r--r--   3 root supergroup         40 2023-12-29 07:02 /user/hive/warehouse/t2/deal_day=20230110/data.har/part-0

-- 恢复
0: jdbc:hive2://zgg-server:10000> alter table t2 unarchive partition(deal_day='20230110');

root@zgg-server:~# hadoop fs -ls /user/hive/warehouse/t2/deal_day=20230110
Found 2 items
drwxr-xr-x   - root supergroup          0 2023-12-29 07:18 /user/hive/warehouse/t2/deal_day=20230110/.hive-staging_hive_2023-12-29_07-17-15_136_2120543675256537905-12
-rw-r--r--   1 root supergroup          40 2023-12-29 07:18 /user/hive/warehouse/t2/deal_day=20230110/000000_0
```

在内部，当一个分区被归档时，使用分区的原始位置(例如/warehouse/table/ds=1)的文件创建一个 HAR。

分区的父目录被指定为与原始位置相同，生成的归档文件名为 data.har。

归档文件被移动到原始目录下(例如/warehouse/table/ds=1/data.har)，并且分区的位置被更改为指向归档文件。

---------------------------

[官网](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Archiving)

[官网翻译](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Archiving.md)