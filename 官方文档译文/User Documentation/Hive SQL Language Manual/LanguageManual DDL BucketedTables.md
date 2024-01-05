# LanguageManual DDL BucketedTables

[TOC]

This is a brief example on creating and populating bucketed tables. (For another example, see [Bucketed Sorted Tables](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-BucketedSortedTables).)

Bucketed tables are fantastic in that they allow much more efficient [sampling](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Sampling) than do non-bucketed tables, and they may later allow for time saving operations such as mapside joins. However, the bucketing specified at table creation is not enforced when the table is written to, and so it is possible for the table's metadata to advertise properties which are not upheld by the table's actual layout. This should obviously be avoided. Here's how to do it right.

抽样更高效；对于 map 端 join 操作，更节省时间。

First, [table creation](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-Create/Drop/TruncateTable):

```sql
CREATE TABLE user_info_bucketed(user_id BIGINT, firstname STRING, lastname STRING)
COMMENT 'A bucketed copy of user_info'
PARTITIONED BY(ds STRING)
CLUSTERED BY(user_id) INTO 256 BUCKETS;
```

Note that we specify a column (user_id) to base the bucketing.

Then we populate the table

```sql
set hive.enforce.bucketing = true;  -- (Note: Not needed in Hive 2.x onward)
FROM user_id
INSERT OVERWRITE TABLE user_info_bucketed
PARTITION (ds='2009-02-25')
SELECT userid, firstname, lastname WHERE ds='2009-02-25';
```

Version 0.x and 1.x only: The command `set hive.enforce.bucketing = true;` allows the correct number of reducers and the cluster by column to be automatically selected based on the table. Otherwise, you would need to set the number of reducers to be the same as the number of buckets as in `set mapred.reduce.tasks = 256;` and have a `CLUSTER BY ...` clause in the select.

How does Hive distribute the rows across the buckets? In general, the bucket number is determined by the expression `hash_function(bucketing_column) mod num_buckets`. (There's a '0x7FFFFFFF in there too, but that's not that important). The hash_function depends on the type of the bucketing column. For an int, it's easy, `hash_int(i) == i`. For example, if user_id were an int, and there were 10 buckets, we would expect all user_id's that end in 0 to be in bucket 1, all user_id's that end in a 1 to be in bucket 2, etc. For other datatypes, it's a little tricky. In particular, the hash of a BIGINT is not the same as the BIGINT. And the hash of a string or a complex datatype will be some number that's derived from the value, but not anything humanly-recognizable. For example, if user_id were a STRING, then the user_id's in bucket 1 would probably not end in 0. In general, distributing rows based on the hash will give you a even distribution in the buckets.

hive 是如何将行分发到桶的呢？通常，桶标号是由表达式 `hash_function(bucketing_column) mod num_buckets` 决定的。

hash_function 取决于分桶列的类型。

对于int, `hash_int(i) == i`. 如果 user_id 是 int, 有 10 个桶，那么所有以 0 结尾的 user_id 会进入桶1，所有以 1 结尾的 user_id 会进入桶2。

对于其他数据类型，有点麻烦。特别是，BIGINT 的哈希值和 BIGINT 不相同。 string 或复杂数据类型的哈希值由值决定，但不是人类可识别的。例如，如果 user_id 是 STRING, 那么在桶1的 user_id 可能不会以0结尾。

通常，基于哈希值的行分发会平均分配。

So, what can go wrong? As long as you use the syntax above and `set hive.enforce.bucketing = true` (for Hive 0.x and 1.x), the tables should be populated properly. Things can go wrong if the bucketing column type is different during the insert and on read, or if you manually cluster by a value that's different from the table definition.