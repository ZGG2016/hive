# fetch抓取

- hive.fetch.task.conversion

	Default Value: `minimal` in Hive 0.10.0 through 0.13.1, `more`  in Hive 0.14.0 and later

	Added In: Hive 0.10.0 with HIVE-2925; default changed in Hive 0.14.0 with HIVE-7397

	一些select查询转换为单个fetch任务，最小化延迟。

	当前，查询应该是一个数据源，不能有子查询、任意的聚合（会出现ReduceSinkOperator，要求mr任务）、distinct、侧写视图和join

	Supported values are none, minimal and more.

[官方描述 hive.fetch.task.conversion](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties)

```sql
-- year分区列
0: jdbc:hive2://localhost:10000> select * from t3;
+--------+----------+---------+------------+----------+
| t3.id  | t3.name  | t3.age  | t3.salary  | t3.year  |
+--------+----------+---------+------------+----------+
| 1      | aa       | 24      | 3000.0     | 2023     |
| 2      | bb       | 33      | 8000.0     | 2023     |
| 3      | cc       | 44      | 10000.0    | 2023     |
| 4      | dd       | 34      | 7000.0     | 2023     |
| 4      | dd       | 34      | 7000.0     | 2023     |
+--------+----------+---------+------------+----------+

```

## none

Disable `hive.fetch.task.conversion`

```sql
0: jdbc:hive2://localhost:10000> set hive.fetch.task.conversion=none;

0: jdbc:hive2://localhost:10000> select * from t3;
INFO  : Query ID = hive_20240111064304_5083f55b-2f51-4959-abc3-b4c24a2dc4c7
INFO  : Total jobs = 1
INFO  : Launching Job 1 out of 1
...
INFO  : Status: Running (Executing on YARN cluster with App id application_1704955384809_0001)
+--------+----------+---------+------------+----------+
| t3.id  | t3.name  | t3.age  | t3.salary  | t3.year  |
+--------+----------+---------+------------+----------+
| 1      | aa       | 24      | 3000.0     | 2023     |
| 2      | bb       | 33      | 8000.0     | 2023     |
| 3      | cc       | 44      | 10000.0    | 2023     |
| 4      | dd       | 34      | 7000.0     | 2023     |
| 4      | dd       | 34      | 7000.0     | 2023     |
+--------+----------+---------+------------+----------+
```

## minimal

`SELECT *`, `FILTER` on partition columns (`WHERE` and `HAVING` clauses), `LIMIT` only

【`SELECT *`、where分区列、limit、虚拟列】

```sql
set hive.fetch.task.conversion=minimal;

0: jdbc:hive2://localhost:10000> select * from t3;
+--------+----------+---------+------------+----------+
| t3.id  | t3.name  | t3.age  | t3.salary  | t3.year  |
+--------+----------+---------+------------+----------+
| 1      | aa       | 24      | 3000.0     | 2023     |
| 2      | bb       | 33      | 8000.0     | 2023     |
| 3      | cc       | 44      | 10000.0    | 2023     |
| 4      | dd       | 34      | 7000.0     | 2023     |
| 4      | dd       | 34      | 7000.0     | 2023     |
+--------+----------+---------+------------+----------+

0: jdbc:hive2://localhost:10000> select * from t3 where year='2023';
+--------+----------+---------+------------+----------+
| t3.id  | t3.name  | t3.age  | t3.salary  | t3.year  |
+--------+----------+---------+------------+----------+
| 1      | aa       | 24      | 3000.0     | 2023     |
| 2      | bb       | 33      | 8000.0     | 2023     |
| 3      | cc       | 44      | 10000.0    | 2023     |
| 4      | dd       | 34      | 7000.0     | 2023     |
| 4      | dd       | 34      | 7000.0     | 2023     |
+--------+----------+---------+------------+----------+

0: jdbc:hive2://localhost:10000> select * from t3 where id=1;
INFO  : Query ID = hive_20240111065831_905858e9-afdb-4479-be50-12e20b3d010a
INFO  : Total jobs = 1
INFO  : Launching Job 1 out of 1
...
INFO  : Status: Running (Executing on YARN cluster with App id application_1704956114562_0001)

+--------+----------+---------+------------+----------+
| t3.id  | t3.name  | t3.age  | t3.salary  | t3.year  |
+--------+----------+---------+------------+----------+
| 1      | aa       | 24      | 3000.0     | 2023     |
+--------+----------+---------+------------+----------+

0: jdbc:hive2://localhost:10000> select * from t3 limit 1;
+--------+----------+---------+------------+----------+
| t3.id  | t3.name  | t3.age  | t3.salary  | t3.year  |
+--------+----------+---------+------------+----------+
| 1      | aa       | 24      | 3000.0     | 2023     |
+--------+----------+---------+------------+----------+

0: jdbc:hive2://localhost:10000> select name,BLOCK__OFFSET__INSIDE__FILE from t3;
+-------+------------------------------+
| name  | block__offset__inside__file  |
+-------+------------------------------+
| aa    | 0                            |
| bb    | 15                           |
| cc    | 30                           |
| dd    | 46                           |
| dd    | 61                           |
+-------+------------------------------+
```

## more

more:  `SELECT`, `FILTER`, `LIMIT` only (including `TABLESAMPLE`, virtual columns)

"more" can take any kind of expressions in the SELECT clause, including UDFs.(UDTFs and lateral views are not yet supported – see HIVE-5718.)

【SELECT、where、limit、虚拟列、UDF、UDTF、lateral views】
```sql
0: jdbc:hive2://localhost:10000> set hive.fetch.task.conversion=more;
0: jdbc:hive2://localhost:10000> select * from t3 where id=1;
+--------+----------+---------+------------+----------+
| t3.id  | t3.name  | t3.age  | t3.salary  | t3.year  |
+--------+----------+---------+------------+----------+
| 1      | aa       | 24      | 3000.0     | 2023     |
+--------+----------+---------+------------+----------+

0: jdbc:hive2://localhost:10000> select id,substring(name,0,1) from t3 where id=1;
+-----+------+
| id  | _c1  |
+-----+------+
| 1   | a    |
+-----+------+

0: jdbc:hive2://localhost:10000> select * from t3 limit 1;
+--------+----------+---------+------------+----------+
| t3.id  | t3.name  | t3.age  | t3.salary  | t3.year  |
+--------+----------+---------+------------+----------+
| 1      | aa       | 24      | 3000.0     | 2023     |
+--------+----------+---------+------------+----------+

0: jdbc:hive2://localhost:10000> select name,BLOCK__OFFSET__INSIDE__FILE from t3;
+-------+------------------------------+
| name  | block__offset__inside__file  |
+-------+------------------------------+
| aa    | 0                            |
| bb    | 15                           |
| cc    | 30                           |
| dd    | 46                           |
| dd    | 61                           |
+-------+------------------------------+

-- order_table_buckets分桶表
0: jdbc:hive2://localhost:10000> select * from order_table_buckets tablesample(bucket 1 out of 8 on order_id);
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
| order_table_buckets.order_id  | order_table_buckets.product_name  | order_table_buckets.price  | order_table_buckets.deal_day  |
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+
| 10                            | book2                             | 40                         | 201901                        |
| 7                             | milk                              | 70                         | 201901                        |
+-------------------------------+-----------------------------------+----------------------------+-------------------------------+

0: jdbc:hive2://localhost:10000> select * from arraytest;
+---------------+----------------------+
| arraytest.id  |    arraytest.info    |
+---------------+----------------------+
| 1             | ["zhangsan","male"]  |
+---------------+----------------------+

-- udtf
0: jdbc:hive2://localhost:10000> select explode(info) from arraytest;
+-----------+
|    col    |
+-----------+
| zhangsan  |
| male      |
+-----------+

-- lateral views
0: jdbc:hive2://localhost:10000> select * from arraytest lateral view explode(info) arraytest_tmp as tmp;
+---------------+----------------------+--------------------+
| arraytest.id  |    arraytest.info    | arraytest_tmp.tmp  |
+---------------+----------------------+--------------------+
| 1             | ["zhangsan","male"]  | zhangsan           |
| 1             | ["zhangsan","male"]  | male               |
+---------------+----------------------+--------------------+

-- 子查询
0: jdbc:hive2://localhost:10000> select * from t3 where id = (select id from d where id=1);
INFO  : Query ID = hive_20240111072219_6433e612-c3ad-4011-9dc3-d3064aace1a5
INFO  : Total jobs = 1
INFO  : Launching Job 1 out of 1
...
INFO  : Status: Running (Executing on YARN cluster with App id application_1704957315881_0001)

-- distinct
0: jdbc:hive2://localhost:10000> select distinct id from t3;
INFO  : Query ID = hive_20240111072401_2190325a-8377-4b98-b17c-df554361672c
INFO  : Total jobs = 1
INFO  : Launching Job 1 out of 1
...
INFO  : Status: Running (Executing on YARN cluster with App id application_1704957315881_0001)
+-----+
| id  |
+-----+
| 1   |
| 2   |
| 3   |
| 4   |
+-----+

-- join
0: jdbc:hive2://localhost:10000> select name,dept from t3 join d on t3.id=d.id;
INFO  : Query ID = hive_20240111072455_a3171db9-689e-4b0d-b9be-b7ebfeb391bd
INFO  : Total jobs = 1
INFO  : Launching Job 1 out of 1
...
INFO  : Status: Running (Executing on YARN cluster with App id application_1704957315881_0001)
+-------+-------+
| name  | dept  |
+-------+-------+
| aa    | HR    |
| bb    | IT    |
+-------+-------+

```