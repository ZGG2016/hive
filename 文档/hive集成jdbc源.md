# hive集成jdbc源

[TOC]

## 准备数据

```sql
mysql> use testdb;
Database changed
mysql> create table student(name varchar(20), age int, gpa double);
Query OK, 0 rows affected (0.06 sec)

mysql> insert into student values('zhangsan', 13, 87.22),('lisi', 14, 93.23);
Query OK, 2 rows affected (0.02 sec)
Records: 2  Duplicates: 0  Warnings: 0

mysql> select * from student;
+----------+------+-------+
| name     | age  | gpa   |
+----------+------+-------+
| zhangsan |   13 | 87.22 |
| lisi     |   14 | 93.23 |
+----------+------+-------+
```

## 建表

```sql
CREATE EXTERNAL TABLE student_jdbc_1
(
  name string,
  age int,
  gpa double
)
STORED BY 'org.apache.hive.storage.jdbc.JdbcStorageHandler'
TBLPROPERTIES (
    "hive.sql.database.type" = "MYSQL",
    "hive.sql.jdbc.driver" = "com.mysql.cj.jdbc.Driver",
    "hive.sql.jdbc.url" = "jdbc:mysql://zgg-server:3306/testdb",  -- testdb是student表所在的数据库
    "hive.sql.dbcp.username" = "root",
    "hive.sql.dbcp.password" = "123456",
    "hive.sql.table" = "student",
    "hive.sql.dbcp.maxActive" = "1"
);

0: jdbc:hive2://zgg-server:10000> select * from student_jdbc_1;
+----------------------+---------------------+---------------------+
| student_jdbc_1.name  | student_jdbc_1.age  | student_jdbc_1.gpa  |
+----------------------+---------------------+---------------------+
| zhangsan             | 13                  | 87.22               |
| lisi                 | 14                  | 93.23               |
+----------------------+---------------------+---------------------+
```

- hive.sql.query
- 外部表定义必须和jdbc数据表结构相同，但是，列名和列类型可以不同

```sql
CREATE EXTERNAL TABLE student_jdbc_2
(
  name string,
  score double
)
STORED BY 'org.apache.hive.storage.jdbc.JdbcStorageHandler'
TBLPROPERTIES (
    "hive.sql.database.type" = "MYSQL",
    "hive.sql.jdbc.driver" = "com.mysql.cj.jdbc.Driver",
    "hive.sql.jdbc.url" = "jdbc:mysql://zgg-server:3306/testdb", 
    "hive.sql.dbcp.username" = "root",
    "hive.sql.dbcp.password" = "123456",
    "hive.sql.query" = "select name, gpa from student",
    "hive.sql.dbcp.maxActive" = "1"
);

0: jdbc:hive2://zgg-server:10000> select * from student_jdbc_2;
+----------------------+-----------------------+
| student_jdbc_2.name  | student_jdbc_2.score  |
+----------------------+-----------------------+
| zhangsan             | 87.22                 |
| lisi                 | 93.23                 |
+----------------------+-----------------------+
```

## 密码安全

密码存储在 HDFS 上的 Java keystore 文件中

```sql
-- 创建keystore文件
root@zgg-server:/opt# hadoop credential create zggserver.password -provider jceks://hdfs/user/root/test.jceks -v 123456

root@zgg-server:/opt# hadoop fs -ls /user/root
Found 1 items
-rw-------   1 root supergroup        505 2024-01-05 13:52 /user/root/test.jceks

CREATE EXTERNAL TABLE student_jdbc_3
(
  name string,
  score double
)
STORED BY 'org.apache.hive.storage.jdbc.JdbcStorageHandler'
TBLPROPERTIES (
    "hive.sql.database.type" = "MYSQL",
    "hive.sql.jdbc.driver" = "com.mysql.cj.jdbc.Driver",
    "hive.sql.jdbc.url" = "jdbc:mysql://zgg-server:3306/testdb", 
    "hive.sql.dbcp.username" = "root",
    "hive.sql.dbcp.password.keystore" = "jceks://hdfs/user/root/test.jceks",
    "hive.sql.dbcp.password.key" = "zggserver.password",    
    "hive.sql.query" = "select name, gpa from student",
    "hive.sql.dbcp.maxActive" = "1"
);

0: jdbc:hive2://zgg-server:10000> select * from student_jdbc_3;
+----------------------+-----------------------+
| student_jdbc_3.name  | student_jdbc_3.score  |
+----------------------+-----------------------+
| zhangsan             | 87.22                 |
| lisi                 | 93.23                 |
+----------------------+-----------------------+

```

## 分区

```sql
mysql> insert into student values('wangwu', 23, 87.22),('lisi', 30, 93.23);
Query OK, 2 rows affected (0.01 sec)
Records: 2  Duplicates: 0  Warnings: 0

mysql> select * from student;
+----------+------+-------+
| name     | age  | gpa   |
+----------+------+-------+
| zhangsan |   13 | 87.22 |
| lisi     |   14 | 93.23 |
| wangwu   |   23 | 87.22 |
| lisi     |   30 | 93.23 |
+----------+------+-------+


CREATE EXTERNAL TABLE student_jdbc_4
(
  name string,
  age int,
  score double
)
STORED BY 'org.apache.hive.storage.jdbc.JdbcStorageHandler'
TBLPROPERTIES (
    "hive.sql.database.type" = "MYSQL",
    "hive.sql.jdbc.driver" = "com.mysql.cj.jdbc.Driver",
    "hive.sql.jdbc.url" = "jdbc:mysql://zgg-server:3306/testdb", 
    "hive.sql.dbcp.username" = "root",
    "hive.sql.dbcp.password.keystore" = "jceks://hdfs/user/root/test.jceks",
    "hive.sql.dbcp.password.key" = "zggserver.password",
    "hive.sql.dbcp.maxActive" = "1",
    "hive.sql.table" = "student",
    "hive.sql.partitionColumn" = "age",  -- 分片列
    "hive.sql.numPartitions" = "3",  -- 分3片
    "hive.sql.lowerBound" = "10",   -- 下界。 未指定的话，就使用数据源的最大最小值。 null指进第一个分片。
    "hive.sql.upperBound" = "40"    -- 上界
);
```

查看日志

```sh
root@zgg-server:/opt# vim /tmp/root/hive.log
2024-01-05T13:58:16,513  INFO [HiveServer2-Background-Pool: Thread-103] jdbc.JdbcInputFormat: Num input splits created 3
2024-01-05T13:58:16,513  INFO [HiveServer2-Background-Pool: Thread-103] jdbc.JdbcInputFormat: split:interval:age[,20)
2024-01-05T13:58:16,514  INFO [HiveServer2-Background-Pool: Thread-103] jdbc.JdbcInputFormat: split:interval:age[20,30)
2024-01-05T13:58:16,514  INFO [HiveServer2-Background-Pool: Thread-103] jdbc.JdbcInputFormat: split:interval:age[30,)
2024-01-05T13:58:16,745  INFO [HiveServer2-Background-Pool: Thread-103] dao.GenericJdbcDatabaseAccessor: Query to execute is [SELECT `name`, `age`, `gpa` FROM   (SELECT * FROM student WHERE age < 20 OR age IS NULL) student  ]
2024-01-05T13:58:16,970  INFO [HiveServer2-Background-Pool: Thread-103] dao.GenericJdbcDatabaseAccessor: Query to execute is [SELECT `name`, `age`, `gpa` FROM   (SELECT * FROM student WHERE age >= 20 AND age < 30) student  ]
2024-01-05T13:58:17,198  INFO [HiveServer2-Background-Pool: Thread-103] dao.GenericJdbcDatabaseAccessor: Query to execute is [SELECT `name`, `age`, `gpa` FROM   (SELECT * FROM student WHERE age >= 30) student  ]
```

## 计算下推

计算下推仅会发生在通过 “hive.sql.table” 指定 jdbc 表时。

```sql
create table school(name varchar(20), school varchar(20));
Query OK, 0 rows affected (0.06 sec)

mysql> insert into school values('zhangsan', 'S1'),('lisi', 'S2'),('wangwu', 'S3');
Query OK, 2 rows affected (0.02 sec)

mysql> select * from school;
+----------+--------+
| name     | school |
+----------+--------+
| zhangsan | S1     |
| lisi     | S2     |
| wangwu   | S3     |
+----------+--------+


CREATE EXTERNAL TABLE school_jdbc
(
  name string,
  school string
)
STORED BY 'org.apache.hive.storage.jdbc.JdbcStorageHandler'
TBLPROPERTIES (
    "hive.sql.database.type" = "MYSQL",
    "hive.sql.jdbc.driver" = "com.mysql.cj.jdbc.Driver",
    "hive.sql.jdbc.url" = "jdbc:mysql://zgg-server:3306/testdb", 
    "hive.sql.dbcp.username" = "root",
    "hive.sql.dbcp.password.keystore" = "jceks://hdfs/user/root/test.jceks",
    "hive.sql.dbcp.password.key" = "zggserver.password",
    "hive.sql.dbcp.maxActive" = "1",
    "hive.sql.table" = "school"
);

0: jdbc:hive2://zgg-server:10000> select t1.*, t2.school from student_jdbc_4 t1 join school_jdbc t2 on t1.name=t2.name;
+-----------+---------+---------+------------+
|  t1.name  | t1.age  | t1.gpa  | t2.school  |
+-----------+---------+---------+------------+
| zhangsan  | 13      | 87.22   | S1         |
| lisi      | 14      | 93.23   | S2         |
| wangwu    | 23      | 87.22   | S3         |
| lisi      | 30      | 93.23   | S2         |
+-----------+---------+---------+------------+

0: jdbc:hive2://zgg-server:10000> explain select t1.*, t2.school from student_jdbc_4 t1 join school_jdbc t2 on t1.name=t2.name;
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| STAGE DEPENDENCIES:                                |
|   Stage-0 is a root stage                          |
|                                                    |
| STAGE PLANS:                                       |
|   Stage: Stage-0                                   |
|     Fetch Operator                                 |
|       limit: -1                                    |
|       Processor Tree:                              |
|         TableScan                                  |
|           alias: t1                                |
|           properties:                              |
|             hive.sql.query SELECT `t0`.`name`, `t0`.`age`, `t0`.`gpa`, `t2`.`school` |
| FROM (SELECT `name`, `age`, `gpa`                  |
| FROM `student`                                     |
| WHERE `name` IS NOT NULL) AS `t0`                  |
| INNER JOIN (SELECT `name`, `school`                |
| FROM `school`                                      |
| WHERE `name` IS NOT NULL) AS `t2` ON `t0`.`name` = `t2`.`name` |
|             hive.sql.query.fieldNames name,age,gpa,school |
|             hive.sql.query.fieldTypes string,int,double,string |
|             hive.sql.query.split false             |
|           Statistics: Num rows: 1 Data size: 380 Basic stats: COMPLETE Column stats: NONE |
|           Select Operator                          |
|             expressions: name (type: string), age (type: int), gpa (type: double), school (type: string) |
|             outputColumnNames: _col0, _col1, _col2, _col3 |
|             Statistics: Num rows: 1 Data size: 380 Basic stats: COMPLETE Column stats: NONE |
|             ListSink                               |
|                                                    |
+----------------------------------------------------+
```

--------------------

[官网](https://cwiki.apache.org/confluence/display/Hive/JDBC+Storage+Handler)

[官网翻译](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/Resources%20for%20Contributors/Hive%20Design%20Docs/JDBC%20Storage%20Handler.md)