# hive时间戳

[TOC]

## 时间戳类型

- timestamp 无论本地时区是什么，这些时间戳总是具有相同的值。

- timestamp with local time zone 将根据当地时区调整

```sql
create table t2(id int, ts1 timestamp, ts2 timestamp with local time zone);

0: jdbc:hive2://localhost:10000> insert into t2 values(1, '2023-11-11 11:11:11', '2023-11-11 11:11:11');
0: jdbc:hive2://localhost:10000> select * from t2;
+--------+------------------------+--------------------------------+
| t2.id  |         t2.ts1         |             t2.ts2             |
+--------+------------------------+--------------------------------+
| 1      | 2023-11-11 11:11:11.0  | 2023-11-11 11:11:11.0 Etc/UTC  |
+--------+------------------------+--------------------------------+
```

## 时间戳

```sql
0: jdbc:hive2://localhost:10000> desc function extended unix_timestamp;
+----------------------------------------------------+
|                      tab_name                      |
+----------------------------------------------------+
| unix_timestamp(date[, pattern]) - Converts the time to a number |
| Converts the specified time to number of seconds since 1970-01-01. The unix_timestamp(void) overload is deprecated, use current_timestamp. |
| Function class:org.apache.hadoop.hive.ql.udf.generic.GenericUDFUnixTimeStamp |
| Function type:BUILTIN                              |
+----------------------------------------------------+

0: jdbc:hive2://localhost:10000> select unix_timestamp('2023-11-11 11:11:11');
+-------------+
|     _c0     |
+-------------+
| 1699701071  |
+-------------+

0: jdbc:hive2://localhost:10000> select unix_timestamp('20231111-111111','yyyyMMdd-HHmmss');
+-------------+
|     _c0     |
+-------------+
| 1699701071  |
+-------------+

0: jdbc:hive2://localhost:10000> select unix_timestamp(cast('2023-11-11' as date));
+-------------+
|     _c0     |
+-------------+
| 1699660800  |
+-------------+

0: jdbc:hive2://localhost:10000> select current_timestamp;
+--------------------------+
|           _c0            |
+--------------------------+
| 2024-01-09 07:51:11.132  |
+--------------------------+

0: jdbc:hive2://localhost:10000> desc function extended to_unix_timestamp;
+----------------------------------------------------+
|                      tab_name                      |
+----------------------------------------------------+
| to_unix_timestamp(date[, pattern]) - Returns the UNIX timestamp |
| Converts the specified time to number of seconds since 1970-01-01. |
| Function class:org.apache.hadoop.hive.ql.udf.generic.GenericUDFToUnixTimeStamp |
| Function type:BUILTIN                              |
+----------------------------------------------------+
```

```sql
0: jdbc:hive2://localhost:10000> desc function extended from_unixtime;
+----------------------------------------------------+
|                      tab_name                      |
+----------------------------------------------------+
| from_unixtime(unix_time, format) - returns unix_time in the specified format |
| Example:                                           |
|   > SELECT from_unixtime(0, 'yyyy-MM-dd HH:mm:ss') FROM src LIMIT 1; |
|   '1970-01-01 00:00:00'                            |
| Function class:org.apache.hadoop.hive.ql.udf.generic.GenericUDFFromUnixTime |
| Function type:BUILTIN                              |
+----------------------------------------------------+

0: jdbc:hive2://localhost:10000> select from_unixtime(1699701071);
+----------------------+
|         _c0          |
+----------------------+
| 2023-11-11 11:11:11  |
+----------------------+

0: jdbc:hive2://localhost:10000> select from_unixtime(1699701071, 'yyyyMMdd-HHmmss');
+------------------+
|       _c0        |
+------------------+
| 20231111-111111  |
+------------------+

0: jdbc:hive2://localhost:10000> select from_unixtime(unix_timestamp('20231111111111','yyyyMMddHHmmss')+3600, 'yyyy-MM-dd HH:mm:ss');
+----------------------+
|         _c0          |
+----------------------+
| 2023-11-11 12:11:11  |
+----------------------+

```

```sql
0: jdbc:hive2://localhost:10000> desc function extended from_utc_timestamp;
+----------------------------------------------------+
|                      tab_name                      |
+----------------------------------------------------+
| from_utc_timestamp(timestamp, string timezone) - Assumes given timestamp is UTC and converts to given timezone (as of Hive 0.8.0) |
| Function class:org.apache.hadoop.hive.ql.udf.generic.GenericUDFFromUtcTimestamp |
| Function type:BUILTIN                              |
+----------------------------------------------------+

0: jdbc:hive2://localhost:10000> select from_utc_timestamp(1704788131874, 'Asia/Shanghai');
+--------------------------+
|           _c0            |
+--------------------------+
| 2024-01-09 16:15:31.874  |
+--------------------------+


0: jdbc:hive2://localhost:10000> select from_utc_timestamp(1704788131874, 'America/Los_Angeles');
+--------------------------+
|           _c0            |
+--------------------------+
| 2024-01-09 00:15:31.874  |
+--------------------------+

0: jdbc:hive2://localhost:10000> desc function extended to_utc_timestamp;
+----------------------------------------------------+
|                      tab_name                      |
+----------------------------------------------------+
| to_utc_timestamp(timestamp, string timezone) - Assumes given timestamp is in given timezone and converts to UTC (as of Hive 0.8.0) |
| Function class:org.apache.hadoop.hive.ql.udf.generic.GenericUDFToUtcTimestamp |
| Function type:BUILTIN                              |
+----------------------------------------------------+

0: jdbc:hive2://localhost:10000> select to_utc_timestamp(1704788131874, 'UTC');
+--------------------------+
|           _c0            |
+--------------------------+
| 2024-01-09 08:15:31.874  |
+--------------------------+

0: jdbc:hive2://localhost:10000> select from_utc_timestamp(to_utc_timestamp(1704788131874, 'UTC'), 'Asia/Shanghai');
+--------------------------+
|           _c0            |
+--------------------------+
| 2024-01-09 16:15:31.874  |
+--------------------------+
```

## 对毫秒的处理

```sql
0: jdbc:hive2://localhost:10000> insert into t2 values(3, '2024-01-09 08:15:31.874', '2024-01-09 08:15:31.874');
0: jdbc:hive2://localhost:10000> select * from t2;
+--------+--------------------------+----------------------------------+
| t2.id  |          t2.ts1          |              t2.ts2              |
+--------+--------------------------+----------------------------------+
| 3      | 2024-01-09 08:15:31.874  | 2024-01-09 08:15:31.874 Etc/UTC  |
| 1      | 2023-11-11 11:11:11.0    | 2023-11-11 11:11:11.0 Etc/UTC    |
| 2      | 2023-11-11 11:11:11.0    | 2023-11-11 11:11:11.0 Etc/UTC    |
+--------+--------------------------+----------------------------------+

-- 第二列10位，只能精确到秒（精确到毫秒为13位）
0: jdbc:hive2://localhost:10000> select ts1, unix_timestamp(ts1) from t2 where id=3;
+--------------------------+-------------+
|           ts1            |     _c1     |
+--------------------------+-------------+
| 2024-01-09 08:15:31.874  | 1704788131  |
+--------------------------+-------------+

-- 获取毫秒级别的时间戳
0: jdbc:hive2://localhost:10000> select ts1, concat(unix_timestamp(ts1), substring(ts1,21,23)) from t2 where id=3;
+--------------------------+----------------+
|           ts1            |      _c1       |
+--------------------------+----------------+
| 2024-01-09 08:15:31.874  | 1704788131874  |
+--------------------------+----------------+

-- 毫秒级别的时间戳 转成标准格式
0: jdbc:hive2://localhost:10000> select ts1, to_utc_timestamp(cast(concat(unix_timestamp(ts1), substring(ts1,21,23)) as bigint), 'UTC') from t2 where id=3;
+--------------------------+--------------------------+
|           ts1            |           _c1            |
+--------------------------+--------------------------+
| 2024-01-09 08:15:31.874  | 2024-01-09 08:15:31.874  |
+--------------------------+--------------------------+
```

[官网描述](https://cwiki.apache.org/confluence/display/Hive/Tutorial#Tutorial-TypeSystem)