# hive集成kafka

[TOC]

```sql
CREATE EXTERNAL TABLE kafka_table (
    `timestamp` TIMESTAMP,
    `page` STRING,
    `newPage` BOOLEAN,
    `added` INT, 
    `deleted` BIGINT,
    `delta` DOUBLE)
STORED BY 
  'org.apache.hadoop.hive.kafka.KafkaStorageHandler'
TBLPROPERTIES ( 
  "kafka.topic" = "topic-hive",
  "kafka.bootstrap.servers" = "zgg-server:9092");
```

会添加额外的字段：

- `__key` Kafka record key (byte array)
- `__partition` Kafka record partition identifier (int 32)
- `__offset` Kafka record offset (int 64)
- `__timestamp` Kafka record timestamp (int 64)

```sql
0: jdbc:hive2://zgg-server:10000> desc formatted kafka_table;
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
|           col_name            |                     data_type                      |                      comment                       |
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
| timestamp                     | timestamp                                          | from deserializer                                  |
| page                          | string                                             | from deserializer                                  |
| newpage                       | boolean                                            | from deserializer                                  |
| added                         | int                                                | from deserializer                                  |
| deleted                       | bigint                                             | from deserializer                                  |
| delta                         | double                                             | from deserializer                                  |
| __key                         | binary                                             | from deserializer                                  |
| __partition                   | int                                                | from deserializer                                  |
| __offset                      | bigint                                             | from deserializer                                  |
| __timestamp                   | bigint                                             | from deserializer                                  |
|                               | NULL                                               | NULL                                               |
| # Detailed Table Information  | NULL                                               | NULL                                               |
| Database:                     | default                                            | NULL                                               |
| OwnerType:                    | USER                                               | NULL                                               |
| Owner:                        | root                                               | NULL                                               |
| CreateTime:                   | Sat Jan 06 02:27:42 UTC 2024                       | NULL                                               |
| LastAccessTime:               | Sat Jan 06 02:28:07 UTC 2024                       | NULL                                               |
| Retention:                    | 0                                                  | NULL                                               |
| Location:                     | hdfs://zgg-server:8020/user/hive/warehouse/kafka_table | NULL                                               |
| Table Type:                   | EXTERNAL_TABLE                                     | NULL                                               |
| Table Parameters:             | NULL                                               | NULL                                               |
|                               | COLUMN_STATS_ACCURATE                              | {\"BASIC_STATS\":\"true\",\"COLUMN_STATS\":{\"__key\":\"true\",\"__offset\":\"true\",\"__partition\":\"true\",\"__timestamp\":\"true\",\"added\":\"true\",\"deleted\":\"true\",\"delta\":\"true\",\"newpage\":\"true\",\"page\":\"true\",\"timestamp\":\"true\"}} |
|                               | EXTERNAL                                           | TRUE                                               |
|                               | bucketing_version                                  | 2                                                  |
|                               | hive.kafka.max.retries                             | 6                                                  |
|                               | hive.kafka.metadata.poll.timeout.ms                | 30000                                              |
|                               | hive.kafka.optimistic.commit                       | false                                              |
|                               | hive.kafka.poll.timeout.ms                         | 5000                                               |
|                               | hive.kafka.ssl.credential.keystore                 |                                                    |
|                               | hive.kafka.ssl.key.password                        |                                                    |
|                               | hive.kafka.ssl.keystore.location                   |                                                    |
|                               | hive.kafka.ssl.keystore.password                   |                                                    |
|                               | hive.kafka.ssl.truststore.location                 |                                                    |
|                               | hive.kafka.ssl.truststore.password                 |                                                    |
|                               | kafka.bootstrap.servers                            | zgg-server:9092                                    |
|                               | kafka.serde.class                                  | org.apache.hadoop.hive.serde2.JsonSerDe            |
|                               | kafka.topic                                        | topic-hive                                         |
|                               | kafka.write.semantic                               | AT_LEAST_ONCE                                      |
|                               | numFiles                                           | 0                                                  |
|                               | numRows                                            | 0                                                  |
|                               | rawDataSize                                        | 0                                                  |
|                               | storage_handler                                    | org.apache.hadoop.hive.kafka.KafkaStorageHandler   |
|                               | totalSize                                          | 0                                                  |
|                               | transient_lastDdlTime                              | 1704508087                                         |
|                               | NULL                                               | NULL                                               |
| # Storage Information         | NULL                                               | NULL                                               |
| SerDe Library:                | org.apache.hadoop.hive.kafka.KafkaSerDe            | NULL                                               |
| InputFormat:                  | null                                               | NULL                                               |
| OutputFormat:                 | null                                               | NULL                                               |
| Compressed:                   | No                                                 | NULL                                               |
| Num Buckets:                  | -1                                                 | NULL                                               |
| Bucket Columns:               | []                                                 | NULL                                               |
| Sort Columns:                 | []                                                 | NULL                                               |
| Storage Desc Params:          | NULL                                               | NULL                                               |
|                               | serialization.format                               | 1                                                  |
+-------------------------------+----------------------------------------------------+----------------------------------------------------+
```


可以改变 serializer/deserializer classes

```sql
alter table kafka_table set tblproperties("kafka.serde.class" = "org.apache.hadoop.hive.serde2.OpenCSVSerde");

0: jdbc:hive2://zgg-server:10000> desc formatted kafka_table;
....
|            | kafka.serde.class             | org.apache.hadoop.hive.serde2.OpenCSVSerde    
```

往 kafka 生产者输入数据

```sh
root@zgg-server:/opt/kafka_2.12-3.6.0# kafka-console-producer.sh --broker-list zgg-server:9092 --topic topic-hive
>1704508539,mainpage,false,1,1,1.1
>1704507939,userpage,false,2,2,2.2
>1704507999,mainpage,false,1,1,1.1
```

hive中查看

```sql
0: jdbc:hive2://zgg-server:10000> select * from  kafka_table;
+------------------------+-------------------+----------------------+--------------------+----------------------+--------------------+--------------------+--------------------------+-----------------------+--------------------------+
| kafka_table.timestamp  | kafka_table.page  | kafka_table.newpage  | kafka_table.added  | kafka_table.deleted  | kafka_table.delta  | kafka_table.__key  | kafka_table.__partition  | kafka_table.__offset  | kafka_table.__timestamp  |
+------------------------+-------------------+----------------------+--------------------+----------------------+--------------------+--------------------+--------------------------+-----------------------+--------------------------+
| 1704508539             | mainpage          | false                | 1                  | 1                    | 1.1                | NULL               | 0                        | 0                     | 1704508589264            |
| 1704507939             | userpage          | false                | 2                  | 2                    | 2.2                | NULL               | 0                        | 1                     | 1704508827406            |
| 1704507999             | mainpage          | false                | 1                  | 1                    | 1.1                | NULL               | 0                        | 2                     | 1704508831607            |
+------------------------+-------------------+----------------------+--------------------+----------------------+--------------------+--------------------+--------------------------+-----------------------+--------------------------+
```

将 `kafka-handler-4.0.0-alpha-2.jar` 复制到 `/opt/hadoop-3.3.6/share/hadoop` 目录下

```
root@zgg-server:/opt/hadoop-3.3.6/share/hadoop# cp /opt/hive-4.0.0/lib/kafka-handler-4.0.0-alpha-2.jar common/
```

否则报错

```
Caused by: java.lang.ClassNotFoundException: Class org.apache.hadoop.hive.kafka.KafkaInputSplit not found
```

计算在查询执行时间的10分钟内灌入kafka的记录数量

```sql
SELECT 
  COUNT(*)
FROM 
  kafka_table 
WHERE 
  `__timestamp` >  1000 * TO_UNIX_TIMESTAMP(CURRENT_TIMESTAMP - INTERVAL '10' MINUTES);
...
+------+
| _c0  |
+------+
| 3    |
+------+
```

直接执行命令出现错误 【？ hive本地lib目录已有jar包】【创建目录并上传】

```
ERROR : FAILED: Execution Error, return code 1 from org.apache.hadoop.hive.ql.exec.mr.MapRedTask. File does not exist: hdfs://zgg-server:8020/opt/hive-4.0.0/lib/kafka-handler-4.0.0-alpha-2.jar
```

存储处理器允许这些元数据字段执行过滤下推到kafka. 例如，上述查询仅读取满足过滤谓词的时间戳内的记录。

这种基于时间的过滤（Kafka消费者分区查找）只有在 kafka broker 版本允许基于时间的查找（Kafka 0.11或更高版本）时才可行。

除了基于时间的过滤，存储处理读取器能使用 SQL WHERE 子句进行基于特定分区偏移量的过滤。

当前支持的操作符有 OR AND < <= >= >

```sql
SELECT
  COUNT(*)
FROM 
  kafka_table
WHERE 
  (`__offset` < 10 AND `__offset` >3 AND `__partition` = 0)
  OR 
  (`__offset` < 105 AND `__offset` > 99 AND `__partition` = 0)
  OR (`__offset` = 109);
```

The user can define a view to take of the last 15 minutes and mask what ever column as follows:

定义一个视图，接受过去15分钟的数据

```sql
CREATE VIEW 
  last_15_minutes_of_kafka_table 
AS 
SELECT 
  `timestamp`,
  current_user() as `user`, 
  `delta`, 
  `added` 
FROM 
  kafka_table 
WHERE 
  `__timestamp` >  1000 * TO_UNIX_TIMESTAMP(CURRENT_TIMESTAMP - INTERVAL '15' MINUTES);

0: jdbc:hive2://zgg-server:10000> select * from last_15_minutes_of_kafka_table;
+-------------------------------------------+--------------------------------------+---------------------------------------+---------------------------------------+
| last_15_minutes_of_kafka_table.timestamp  | last_15_minutes_of_kafka_table.user  | last_15_minutes_of_kafka_table.delta  | last_15_minutes_of_kafka_table.added  |
+-------------------------------------------+--------------------------------------+---------------------------------------+---------------------------------------+
| 1704508539                                | root                                 | 1.1                                   | 1                                     |
| 1704507939                                | root                                 | 2.2                                   | 2                                     |
| 1704507999                                | root                                 | 1.1                                   | 1                                     |
+-------------------------------------------+--------------------------------------+---------------------------------------+---------------------------------------+
```

hive 表与 kafka topic 做 join  【事实表与hive中的维度表join】

```sql
CREATE TABLE user_table (
  `user` STRING, 
  `first_name` STRING, 
  `age` INT, 
  `gender` STRING, 
  `comments` STRING ) 
STORED AS ORC;

insert into user_table values('root','aa', 34, 'male', '...');

0: jdbc:hive2://zgg-server:10000> select * from user_table;
+------------------+------------------------+-----------------+--------------------+----------------------+
| user_table.user  | user_table.first_name  | user_table.age  | user_table.gender  | user_table.comments  |
+------------------+------------------------+-----------------+--------------------+----------------------+
| root             | aa                     | 34              | male               | ...                  |
+------------------+------------------------+-----------------+--------------------+----------------------+


SELECT 
  SUM(`added`) AS `added`, 
  AVG(`delta`) AS `delta`, 
  AVG(`age`) AS `avg_age`, 
  `gender` 
FROM 
  last_15_minutes_of_kafka_table t1
JOIN 
  user_table t2 ON 
    t1.`user` = t2.`user`
GROUP BY 
  `gender` 
LIMIT 10;
```

In cases where you want to perform some ad-hoc analysis over the last 15 minutes of topic data, you can join it on itself. In the following example, we show how you can perform classical user retention analysis over the Kafka topic.

```sql
-- Topic join over the view itself
-- The example is adapted from https://www.periscopedata.com/blog/how-to-calculate-cohort-retention-in-sql
-- Assuming l15min_wiki is a view of the last 15 minutes based on the topic's timestamp record metadata

SELECT 
  COUNT(DISTINCT `activity`.`user`) AS `active_users`, 
  COUNT(DISTINCT `future_activity`.`user`) AS `retained_users`
FROM 
  l15min_wiki AS activity
LEFT JOIN 
  l15min_wiki AS future_activity
ON
  activity.`user` = future_activity.`user`
AND 
  activity.`timestamp` = future_activity.`timestamp` - INTERVAL '5' MINUTES;

--  Topic to topic join
-- Assuming wiki_kafka_hive is the entire topic

SELECT 
  FLOOR_HOUR(activity.`timestamp`), 
  COUNT(DISTINCT activity.`user`) AS `active_users`, 
  COUNT(DISTINCT future_activity.`user`) AS retained_users
FROM 
  wiki_kafka_hive AS activity
LEFT JOIN 
  wiki_kafka_hive AS future_activity 
ON
  activity.`user` = future_activity.`user`
AND 
  activity.`timestamp` = future_activity.`timestamp` - INTERVAL '1' HOUR 
GROUP BY 
  FLOOR_HOUR(activity.`timestamp`); 
```

SSL connections

```sql
CREATE EXTERNAL TABLE 
  kafka_ssl (
    `data` STRING
)
STORED BY 
  'org.apache.hadoop.hive.kafka.KafkaStorageHandler'
TBLPROPERTIES ( 
  "kafka.topic" = "test-topic",
  "kafka.bootstrap.servers" = 'localhost:9093',
   'hive.kafka.ssl.credential.keystore'='jceks://hdfs/tmp/test.jceks',
   'hive.kafka.ssl.keystore.password'='keystore.password',
   'hive.kafka.ssl.truststore.password'='truststore.password',
   'kafka.consumer.security.protocol'='SSL',
   'hive.kafka.ssl.keystore.location'='hdfs://cluster/tmp/keystore.jks',
   'hive.kafka.ssl.truststore.location'='hdfs://cluster/tmp/keystore.jks'
);
```

在 hive 中控制 kafka 生产者和消费者的属性

```sql
ALTER TABLE 
  kafka_table 
SET TBLPROPERTIES 
  ("kafka.consumer.max.poll.records" = "5000");
```

-----------------

[更多详细描述](https://github.com/apache/hive/blob/master/kafka-handler/README.md)