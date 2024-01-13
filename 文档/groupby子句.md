# groupby子句

hive4.0

在 groupby 子句中使用位置编号，而不是名称

```sql
0: jdbc:hive2://localhost:10000> select * from movies_data;
+----------------------+----------------------+---------------------+------------------------+
| movies_data.user_id  | movies_data.item_id  | movies_data.rating  | movies_data.rate_time  |
+----------------------+----------------------+---------------------+------------------------+
| 196                  | 242                  | 3                   | 881250949              |
| 186                  | 302                  | 3                   | 891717742              |
| 22                   | 377                  | 1                   | 878887116              |
| 244                  | 51                   | 2                   | 880606923              |
| 166                  | 346                  | 1                   | 886397596              |
| 298                  | 474                  | 4                   | 884182806              |
| 115                  | 265                  | 2                   | 881171488              |
| 253                  | 465                  | 5                   | 891628467              |
| 305                  | 451                  | 3                   | 886324817              |
| 6                    | 86                   | 3                   | 883603013              |
| 62                   | 257                  | 2                   | 879372434              |
+----------------------+----------------------+---------------------+------------------------+

0: jdbc:hive2://localhost:10000> set hive.groupby.position.alias;
+------------------------------------+
|                set                 |
+------------------------------------+
| hive.groupby.position.alias=false  |
+------------------------------------+

0: jdbc:hive2://localhost:10000> select item_id, avg(rating) from movies_data group by 1;
Error: Error while compiling statement: FAILED: SemanticException [Error 10025]: Line 1:7 Expression not in GROUP BY key 'item_id' (state=42000,code=10025)

0: jdbc:hive2://localhost:10000> set hive.groupby.position.alias=true;

0: jdbc:hive2://localhost:10000> select item_id, avg(rating) from movies_data group by 1;
+----------+------+
| item_id  | _c1  |
+----------+------+
| 51       | 2.0  |
| 86       | 3.0  |
| 242      | 3.0  |
| 257      | 2.0  |
| 265      | 2.0  |
| 302      | 3.0  |
| 346      | 1.0  |
| 377      | 1.0  |
| 451      | 3.0  |
| 465      | 5.0  |
| 474      | 4.0  |
+----------+------+
```

多个聚合操作可以同时完成，然而，两个聚合不能有不同的 DISTINCT 列  【hive4.0下可以】

```sql
0: jdbc:hive2://localhost:10000> SELECT user_id, count(DISTINCT user_id), count(DISTINCT item_id) FROM movies_data GROUP BY user_id;
+----------+------+------+
| user_id  | _c1  | _c2  |
+----------+------+------+
| 6        | 1    | 1    |
| 22       | 1    | 1    |
| 62       | 1    | 1    |
| 115      | 1    | 1    |
| 166      | 1    | 1    |
| 186      | 1    | 1    |
| 196      | 1    | 1    |
| 244      | 1    | 1    |
| 253      | 1    | 1    |
| 298      | 1    | 1    |
| 305      | 1    | 1    |
+----------+------+------+

```

当使用 group by 子句时，select 语句只能包含 group by 子句中的列，除了聚合项，如上述语句。

但下列语句则不可以。

因为，如果user_id对应多个item_id，那么最后取哪个值就是不确定的了。

```sql
0: jdbc:hive2://localhost:10000> SELECT user_id, item_id FROM movies_data GROUP BY user_id;
Error: Error while compiling statement: FAILED: SemanticException [Error 10025]: Line 1:16 Expression not in GROUP BY key 'item_id' (state=42000,code=10025)
```

如果 group by 子句中的列不在 select 中，也是不正确的

```sql
0: jdbc:hive2://localhost:10000> SELECT * FROM movies_data GROUP BY user_id;
Error: Error while compiling statement: FAILED: SemanticException [Error 10025]: Expression not in GROUP BY key item_id (state=42000,code=10025)

0: jdbc:hive2://localhost:10000> SELECT item_id, avg(rating) FROM movies_data GROUP BY user_id;
Error: Error while compiling statement: FAILED: SemanticException [Error 10025]: Line 1:7 Expression not in GROUP BY key 'item_id' (state=42000,code=10025)
```

聚合或简单选择的输出可以进一步发送到多个表中，甚至发送到 hadoop dfs 文件中(然后可以使用 hdfs 实用程序操作这些文件)。

```sql
0: jdbc:hive2://localhost:10000> create table t1(id int, name string, age int, salary double);

0: jdbc:hive2://localhost:10000> insert into t1 values(1,"aa","24", 3000.00),(2,"bb","33", 8000.00),(3,"cc","44", 10000.00);  

0: jdbc:hive2://localhost:10000> select * from t1;
+--------+----------+---------+------------+
| t1.id  | t1.name  | t1.age  | t1.salary  |
+--------+----------+---------+------------+
| 1      | aa       | 24      | 3000.0     |
| 2      | bb       | 33      | 8000.0     |
| 3      | cc       | 44      | 10000.0    |
+--------+----------+---------+------------+

0: jdbc:hive2://localhost:10000> create table t1_1(id int, cnt int);
0: jdbc:hive2://localhost:10000> create table t1_2(name string, salary double);
```

```sql
from t1
insert overwrite table t1_1
select t1.id, count(distinct t1.name) cnt
 group by t1.id
-- insert overwrite DIRECTORY '/user/hive/warehouse/t1_2'
insert overwrite table t1_2
select t1.name,sum(t1.salary) salary
 group by t1.name;

0: jdbc:hive2://localhost:10000> select * from t1_1;
+----------+-----------+
| t1_1.id  | t1_1.cnt  |
+----------+-----------+
| 1        | 1         |
| 2        | 1         |
| 3        | 1         |
+----------+-----------+

0: jdbc:hive2://localhost:10000> select * from t1_2;
+------------+--------------+
| t1_2.name  | t1_2.salary  |
+------------+--------------+
| aa         | 3000.0       |
| bb         | 8000.0       |
| cc         | 10000.0      |
+------------+--------------+
```

[官网](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+GroupBy)

[官网译文](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Group%20By.md)