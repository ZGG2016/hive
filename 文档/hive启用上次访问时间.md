# hive启用LAST_ACCESS_TIME(访问时间)

在 hive-site.xml 中添加如下内容，并重启

```xml
<property>
	<name>hive.security.authorization.sqlstd.confwhitelist</name>
	<value>hive.exec.pre.hooks</value>
</property>

<property>
	<name>hive.exec.pre.hooks</name>
	<value>org.apache.hadoop.hive.ql.hooks.UpdateInputAccessTimeHook$PreExec</value>
</property>
```

```sql
mysql> select * from TBLS;
+--------+-------------+-------+------------------+-------+------------+-----------+-------+---------------+----------------+--------------------+--------------------+----------------------------------------+----------+
| TBL_ID | CREATE_TIME | DB_ID | LAST_ACCESS_TIME | OWNER | OWNER_TYPE | RETENTION | SD_ID | TBL_NAME      | TBL_TYPE       | VIEW_EXPANDED_TEXT | VIEW_ORIGINAL_TEXT | IS_REWRITE_ENABLED                     | WRITE_ID |
+--------+-------------+-------+------------------+-------+------------+-----------+-------+---------------+----------------+--------------------+--------------------+----------------------------------------+----------+
|      2 |  1703401541 |     1 |       1703407601 | root  | USER       |         0 |     2 | order_table_s | MANAGED_TABLE  | NULL               | NULL               | 0x00                                   |        0 |
|     12 |  1703404770 |     1 |       1703407538 | root  | USER       |         0 |    14 | t1            | EXTERNAL_TABLE | NULL               | NULL               | 0x00                                   |        0 |
|     21 |  1703407670 |     1 |       1703408251 | root  | USER       |         0 |    21 | t2            | MANAGED_TABLE  | NULL               | NULL               | 0x00                                   |        0 |
|     22 |  1703407673 |     1 |       1703408241 | root  | USER       |         0 |    22 | t3            | MANAGED_TABLE  | NULL               | NULL               | 0x00                                   |        0 |
+--------+-------------+-------+------------------+-------+------------+-----------+-------+---------------+----------------+--------------------+--------------------+----------------------------------------+----------+
4 rows in set (0.00 sec)

```