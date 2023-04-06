# LanguageManual DDL BucketedTables

> This is a brief example on creating and populating bucketed tables. (For another example, see [Bucketed Sorted Tables](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-BucketedSortedTables).)

这是一个关于创建和填充分桶表的简单示例。

> Bucketed tables are fantastic in that they allow much more efficient [sampling](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Sampling) than do non-bucketed tables, and they may later allow for time saving operations such as mapside joins. However, the bucketing specified at table creation is not enforced when the table is written to, and so it is possible for the table's metadata to advertise properties which are not upheld by the table's actual layout. This should obviously be avoided. Here's how to do it right.

分桶表的奇妙之处就在于它们**具有比非分桶表表更高效的抽样**，而且它们之后**可能为一些操作缩减时间，比如 mapside join**。

然而，**在往表里写入数据时，创建表时指定的分桶不会被强制执行**，因此表的元数据可能会发布与表的实际布局不一致的属性。

这显然是应该避免的。下面是正确的做法。

> First, [table creation](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-Create/Drop/TruncateTable):

首先，创建表:

```sql
CREATE TABLE user_info_bucketed(user_id BIGINT, firstname STRING, lastname STRING)
COMMENT 'A bucketed copy of user_info'
PARTITIONED BY(ds STRING)
CLUSTERED BY(user_id) INTO 256 BUCKETS;
```

> Note that we specify a column (user_id) to base the bucketing.

注意，我们指定了一个列(user_id)作为分桶的基础。

> Then we populate the table

然后填充表

	(注意:从Hive 2.x开始不需要。)
	set hive.enforce.bucketing = true;  -- (Note: Not needed in Hive 2.x onward)

```sql
FROM user_id
INSERT OVERWRITE TABLE user_info_bucketed
PARTITION (ds='2009-02-25')
SELECT userid, firstname, lastname WHERE ds='2009-02-25';
```

> Version 0.x and 1.x only：The command set hive.enforce.bucketing = true; allows the correct number of reducers and the cluster by column to be automatically selected based on the table. Otherwise, you would need to set the number of reducers to be the same as the number of buckets as in set mapred.reduce.tasks = 256; and have a CLUSTER BY ... clause in the select.

**仅版本 0.x 和 1.x：命令`hive.enforce.bucketing = true`**；

允许根据表自动选择正确的 reducers 数量和 cluster by 列。

否则，你需要在 `mapred.reduce.tasks = 256` 中将 reducers 的数量设置为与桶的数量相同，在 select 中有 `CLUSTER BY ...` 子句。

> How does Hive distribute the rows across the buckets? In general, the bucket number is determined by the expression hash_function(bucketing_column) mod num_buckets. (There's a '0x7FFFFFFF in there too, but that's not that important). The hash_function depends on the type of the bucketing column. For an int, it's easy, hash_int(i) == i. For example, if user_id were an int, and there were 10 buckets, we would expect all user_id's that end in 0 to be in bucket 1, all user_id's that end in a 1 to be in bucket 2, etc. For other datatypes, it's a little tricky. In particular, the hash of a BIGINT is not the same as the BIGINT. And the hash of a string or a complex datatype will be some number that's derived from the value, but not anything humanly-recognizable. For example, if user_id were a STRING, then the user_id's in bucket 1 would probably not end in 0. In general, distributing rows based on the hash will give you a even distribution in the buckets.

Hive 如何在桶间分发行?

通常，桶号是由 `hash_function(bucketing_column)` 对 num_buckets 取模的表达式决定的。(这里也有一个 0x7FFFFFFF，但那不是那么重要)。

hash_function 取决于分桶列的类型。

对于一个 int 类型，`hash_int(i) == i`。例如，如果 user_id 是一个 int 类型，并且有 10 个桶，那么所有以 0 结尾的 user_id 都在桶 1 中，所有以 1 结尾的 user_id 都在桶 2 中，等等。

对于其他数据类型，这有点棘手。特别地，BIGINT 的哈希值与 BIGINT 是不同的。而字符串或复杂数据类型的哈希值将是从值派生的某个数字，但不是人类可识别的任何东西。

例如，如果 user_id 是一个字符串，那么桶 1 中的 user_id 可能不会以 0 结束。

一般来说，根据哈希值来分配行会给你一个均匀的存储桶分布。

> So, what can go wrong? As long as you use the syntax above and set hive.enforce.bucketing = true (for Hive 0.x and 1.x), the tables should be populated properly. Things can go wrong if the bucketing column type is different during the insert and on read, or if you manually cluster by a value that's different from the table definition.

那么，会出现什么问题呢？只要你使用上面的语法设置 `hive.enforce.bucketing = true`(对于Hive 0。x和1.x)时，应该正确填充表。如果在插入和读取时分桶列类型不同，或者你手动聚类不同于表定义的值，那么就可能出现问题。