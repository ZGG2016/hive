# hive connector

hive4.0.0开始

```sql
-- TODO
0: jdbc:hive2://zgg-server:10000> select * from student;
Caused by: MetaException(message:Alter table in REMOTE database mysql_hive_testdb is not allowed)
```

--------------------------

创建connector

```sql
CREATE CONNECTOR mysql_local TYPE 'mysql' 
URL 'jdbc:mysql://zgg-server:3306/' 
WITH DCPROPERTIES (
    "hive.sql.dbcp.username"="root", 
    "hive.sql.dbcp.password"="123456"
);

0: jdbc:hive2://zgg-server:10000> show connectors;
+-----------------+
| connector_name  |
+-----------------+
| mysql_local     |
+-----------------+
```

创建数据库，mysql的testdb数据库映射到hive的mysql_hive_testdb数据库

```sql
0: jdbc:hive2://zgg-server:10000> CREATE REMOTE DATABASE mysql_hive_testdb USING mysql_local WITH DBPROPERTIES ("connector.remoteDbName"="testdb");
```

Use the tables in REMOTE database much like the JDBC-storagehandler based tables in hive. 

One big difference is that the metadata for these tables are never persisted in hive. Currently, create/alter/drop table DDLs are not supported in REMOTE databases. 

```sql
0: jdbc:hive2://zgg-server:10000> show databases;
+--------------------+
|   database_name    |
+--------------------+
| default            |
| mysql_hive_testdb  |
+--------------------+

0: jdbc:hive2://zgg-server:10000> USE mysql_hive_testdb;
0: jdbc:hive2://zgg-server:10000> SHOW TABLES;
+-----------+
| tab_name  |
+-----------+
| school    |
| student   |
+-----------+

-- TODO
0: jdbc:hive2://zgg-server:10000> select * from student;
Caused by: MetaException(message:Alter table in REMOTE database mysql_hive_testdb is not allowed)
```

- The `ALTER CONNECTOR ... SET DCPROPERTIES` replaces the existing properties with the new set of properties specified in the ALTER DDL.

- The `ALTER CONNECTOR ... SET URL` replaces the existing URL with a new URL for the remote datasource. Any REMOTE databases that were created using the connector will continue to work as they are associated by name.

- The `ALTER CONNECTOR ... SET OWNER` changes the ownership of the connector object in hive.

```sql
alter CONNECTOR mysql_local set DCPROPERTIES("hive.sql.dbcp.maxActive" = "1");
```

```sql
drop CONNECTOR IF EXISTS mysql_local;
```


-------------------------------

[官网Data Connectors in Hive](https://cwiki.apache.org/confluence/display/Hive/Data+Connectors+in+Hive)

[官网翻译Data Connectors in Hive](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Data%20Connectors%20in%20Hive.md)

[官网](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-Create/Drop/AlterConnector)

[官网翻译](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/DDL%20Statements.md)