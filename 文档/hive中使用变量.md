# hive中使用变量

[TOC]

操作的表：

```sql
hive> select * from hbase_table_1;
OK
1       zhangsan
2       lisi

hive> desc hbase_table_1;
OK
key                     int                                         
value                   string 
```

默认情况下，变量替换是开启的(hive.variable.substitute=true)。

变量可以在 linux 命令行下定义，也可以在 hive 命令行下定义。

## 1 在 hive 命令行下定义

```sh
# 设置hiveconf属性（也就是hiveconf名称空间）
hive> set zzz=5;
hive> set zzz;
zzz=5

# 设置系统属性（也就是system名称空间）
hive> set system:xxx=5;
hive> set system:xxx;
system:xxx=5
hive> set system:yyy=${system:xxx};
hive> set system:yyy;
system:yyy=5
hive> set go=${hiveconf:zzz};
hive> set go;
go=5
hive> set a=1;
hive> set b=a;
hive> set b;
b=a
hive> set c=${hiveconf:${hiveconf:b}};
hive> set c;
c=1
```

## 2 在 linux 命令行下定义

### 2.1 在 shell 脚本中定义变量

在 hive -e 中使用

```sh
[root@zgg ~]# cat test.sh
table="hbase_table_1"
hive -e "desc $table"

[root@zgg ~]# sh test.sh
key                     int                                         
value                   string     
```

### 2.2 通过 --hiveconf 定义变量

在 hive -e 中使用

```sh
[root@zgg ~]# hive --hiveconf a=1 -e 'select * from hbase_table_1 where key=${hiveconf:a};'
....
1       zhangsan
```

在 hive -f 中使用

```sh
[root@zgg ~]# cat test.sql
select * from hbase_table_1 where key=${hiveconf:b};

[root@zgg ~]# hive --hiveconf b=2 -f test.sql
....
2       lisi
```

### 2.3 通过 --hivevar 定义变量

```sh
[root@zgg ~]# hive --hivevar a=1 -e 'select * from hbase_table_1 where key=${hivevar:a};'
....
1       zhangsan
```

```sh
[root@zgg ~]# cat test.sql
select * from hbase_table_1 where key=${hivevar:b};

[root@zgg ~]# hive --hivevar b=2 -f test.sql
....
2       lisi
```

[https://issues.apache.org/jira/browse/HIVE-2020](https://issues.apache.org/jira/browse/HIVE-2020)

### 2.4 通过 -define 定义变量

--define 可以简写为 -d

```sh
[root@zgg ~]# hive -d a=1 -e 'select * from hbase_table_1 where key=${a};'
....
1       zhangsan
```

```sh
[root@zgg ~]# cat test.sql
select * from hbase_table_1 where key=${b};

[root@zgg ~]# hive -d b=2 -f test.sql
....
2       lisi
```

[官网原文](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Variable%20Substitution.md)