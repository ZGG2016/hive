# 关于 select count 的问题

[TOC]

建表

```sql
create table test(id int);
```

查看元数据

```sql
hive (default)> desc formatted test;
...
numFiles                0                   
numRows                 0 
...
```

## insert

插入数据，文件数是1，行数是1

```sql
hive (default)> insert into test values(1);

hive (default)> desc formatted test;
...
numFiles                1                   
numRows                 1 
...
```

此时查看记录数量，是1，发现没有走 mapreduce

```sql
hive (default)> select count(1) from test;
OK
_c0
1
```


## 2 hadoop fs put

插入数据

```sh
root@bigdata101:~# hadoop fs -put test.txt /user/hive/warehouse/test
```

查看元数据，文件数是0，行数是0

```sql
hive (default)> desc formatted test;
...
numFiles                0                   
numRows                 0 
...
```

此时查看记录数量，是0，发现没有走 mapreduce

```sql
hive (default)> select count(1) from test;
OK
_c0
0
```

## 3 load data


插入数据

```sql
hive (default)> load data local inpath '/root/test.txt' into table test;
```

查看元数据，文件数是1，行数是0

```sql
hive (default)> desc formatted test;
...
numFiles                1                   
numRows                 0 
...
```

此时查看记录数量，是1，发现走了 mapreduce

```sql
hive (default)> select count(1) from test;
Query ID = root_20220906091549_2a5d25e8-42ce-47f7-9037-deca97e2a1ae
Total jobs = 1
Launching Job 1 out of 1
.......
OK
_c0
1
```



所以，

- insert 方式插入数据，会更新元数据中的 numFiles 字段和 numRows 字段，当统计 count(1) 时，不执行 mapreduce，直接使用元数据中的 numRows 字段。

- hadoop fs put 命令的方式插入数据，不会更新元数据中的 numFiles 字段和 numRows 字段，当统计 count(1) 时，不执行 mapreduce，直接使用元数据中的 numRows 字段，所以就会是0。

- load data 的方式插入数据只会更新元数据中的 numFiles 字段，不会更新 numRows 字段，所以 numRows 等于0，当统计 count(1) 时，通过执行 mapreduce 计算得到结果

