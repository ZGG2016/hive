# hive宏

Ability to create functions in HQL as a substitute for creating them in Java.

可以将频繁使用的语句封装到宏。

但是宏是会话级别的 (Macros exist for the duration of the current session.)

创建宏

	CREATE TEMPORARY MACRO macro_name([col_name col_type, ...]) expression;

```sql
0: jdbc:hive2://zgg-server:10000> create temporary macro simply_add(x int, y int) x+y;

0: jdbc:hive2://zgg-server:10000> select simply_add(1,2);
+------+
| _c0  |
+------+
| 3    |
+------+
```

```sql
create temporary macro concat_string(x string, y string) concat_ws("|", x, y);

0: jdbc:hive2://zgg-server:10000> select concat_string(order_id,product_name) s from order_table_s;
+--------------+
|      s       |
+--------------+
| 1|cellphone  |
| 2|tv         |
| 3|sofa       |
| 4|cabinet    |
| 5|bicycle    |
| 6|truck      |
+--------------+

```

删除宏

```
drop temporary macro concat_string;
```