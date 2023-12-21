# hive自增序列问题

[TOC]

类似 mysql 中的自增序列, hive 可以通过 `row_number` 或 `UDFRowSequence` 实现。

```sql
create table hive_seq_dst(id int, name string);
create table hive_seq_src(name string);

hive (default)> load data local inpath '/export/datas/seq.txt' into table hive_seq_src;
hive (default)> select * from hive_seq_src;
OK
hive_seq_src.name
zhangsan
lisi
wangwu
tom
mike
Time taken: 0.055 seconds, Fetched: 5 row(s)
```

## row_number

```sql
insert into table hive_seq_dst
select row_number() over(order by name) as id, name from hive_seq_src;
hive (default)> select * from hive_seq_dst;
OK
hive_seq_dst.id	hive_seq_dst.name
1	lisi
2	mike
3	tom
4	wangwu
5	zhangsan
Time taken: 0.061 seconds, Fetched: 5 row(s)
```

此时 hive_seq_dst 表里已有数据，如果再往里追加数据

```sql
-- 首先往hive_seq_src插入一条数据
insert into table hive_seq_src select "alice" as name;
-- 再往hive_seq_dst插入
-- set hive.mapred.mode=nonstrict
insert into table hive_seq_dst
select row_number() over()+t2.maxid as id, t1.name 
from (select * from hive_seq_src where name = "alice") t1 
cross join (select max(id) as maxid from hive_seq_dst) t2;
hive (default)> select * from hive_seq_dst;
OK
hive_seq_dst.id	hive_seq_dst.name
1	lisi
2	mike
3	tom
4	wangwu
5	zhangsan
6	alice
```

也可以连接其他内容

```sql
hive (default)> select concat(to_unix_timestamp(current_date()), row_number() over()) as id, name from hive_seq_src;
id	name
17028288001	mike
17028288002	tom
17028288003	wangwu
17028288004	lisi
17028288005	zhangsan
17028288006	alice
Time taken: 16.125 seconds, Fetched: 6 row(s)
```

## UDFRowSequence

Hive环境要有hive-contrib相关jar包

```
hive (default)> create temporary function row_sequence as 'org.apache.hadoop.hive.contrib.udf.UDFRowSequence';
OK
Time taken: 0.007 seconds

hive (default)> select row_sequence() as id, name from hive_seq_src;
OK
id	name
1	alice
2	zhangsan
3	lisi
4	wangwu
5	tom
6	mike
Time taken: 0.053 seconds, Fetched: 6 row(s)
```

二者区别：

第一种方法row_number

	在一次SQL运行中是全局递增的，只不过再次执行SQL就会重复，如果不想重复我们可以更改start_num的值，把start_num调整到我们认为的不会重复的值开始

	或者拼接上日期或者时间戳等前缀，这样每次执行就不会重复

第二种方法UDFRowSequence

	由于是我们自己定义的函数，而SQL任务是以分布式的运行的，一个SQL并发可能会有多个job执行，每个job可以理解为1个节点或者进程，在每个进程上运营的序列都从起始值开始，所以不能保证序号全局连续唯一。因此我们可以借助第三方存储记录，比如Redis，来保证生产序列的全局连续递增

原文链接：https://blog.csdn.net/lzxlfly/article/details/133627240

## UUID

还可以通过UUID赋一个随机字符串

```sql
hive (default)> select regexp_replace(reflect("java.util.UUID","randomUUID"),"-","") as uuid, name from hive_seq_src;
OK
uuid	name
72a9b056e275462a9cb9921c449ac584	alice
0ef545fe94274aed91e261ad6c69ac18	zhangsan
c318215e26934f639a7e7abbf8f2a892	lisi
245fec4e55f948a2a454321d780349c0	wangwu
7bce569339044eeab72c14f574f2bbc8	tom
7f58ae433884466d95b8762d706e5dcb	mike
Time taken: 0.218 seconds, Fetched: 6 row(s)
```

还可以使用 `uuid()` 函数

```sql
hive (default)> desc function uuid;
OK
tab_name
uuid() - Returns a universally unique identifier (UUID) string.

hive (default)> select uuid() as uuid;
OK
uuid
8e92b8de-acc3-4c65-a977-3c2342601f56

hive (default)> select regexp_replace(uuid(),"-","") as uuid, name from hive_seq_src;
OK
uuid	name
c1a3ea37c0b0490d958bb2fc6419e2e8	alice
f3bf47826ad7498daf8aef2577fb4f3d	zhangsan
cc98355395fd44218f4f873880912a86	lisi
08dc6a2fb39e41efba04cfad72e3aea7	wangwu
2ff5179a7b654964b2b68384f4f01f1d	tom
ad09ccd78b0c4a64abd87d98b1f6f180	mike
Time taken: 0.034 seconds, Fetched: 6 row(s)
```

由于这个 id 不是递增的，此时可以借助 row_number 生成递增的 id 序列。

该函数根据 uuid() 函数生成的唯一ID排序，会造成原数据位置变化。

```sql
hive (default)> select row_number() OVER (ORDER BY uuid()) as uuid, name from hive_seq_src;
OK
uuid	name
1	alice
2	lisi
3	tom
4	zhangsan
5	wangwu
6	mike
Time taken: 17.935 seconds, Fetched: 6 row(s)
```