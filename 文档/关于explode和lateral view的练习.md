# 关于 explode 和 lateral view 的练习

[TOC]

首先阅读 [官网描述](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/Lateral%20View.md)

## 1 explode

EXPLODE(col)：将 hive 一列中复杂的 Array 或者 Map 结构拆分成多行

```sql
hive (default)> select * from test2;
OK
test2.name      test2.friends   test2.children  test2.address
songsong        ["bingbing","lili"]     {"xiao song":18,"xiaoxiao song":19}     {"street":"hui long guan","city":"beijing"}
yangyang        ["caicai","susu"]       {"xiao yang":18,"xiaoxiao yang":19}     {"street":"chao yang","city":"beijing"}

-- 拆 array
hive (default)> select explode(friends) from test2;
OK
col
bingbing
lili
caicai
susu

-- 拆 map
hive (default)> select explode(children) from test2;
OK
key     value
xiao song       18
xiaoxiao song   19
xiao yang       18
xiaoxiao yang   19
```


## 2 lateral view

用法：

	LATERAL VIEW udtf(expression) tableAlias AS columnAlias (',' columnAlias)*

用于和 split, explode 等 UDTF 一起使用，它能够将一列数据拆成多行数据，在此基础上可以对拆分后的数据进行查询或聚合。

具体的含义参考练习理解。

## 3 练习

数据源

```sh
root@bigdata101:~# cat movie_info.txt
《疑犯追踪》    悬疑,动作,科幻,剧情
《Lie to me》   悬疑,警匪,动作,心理,剧情
《战狼 2》      战争,动作,灾难
```

需求：将电影分类中的数组数据展开，结果如下

```
《疑犯追踪》 悬疑
《疑犯追踪》 动作
《疑犯追踪》 科幻
《疑犯追踪》 剧情
《Lie to me》 悬疑
《Lie to me》 警匪
《Lie to me》 动作
《Lie to me》 心理
《Lie to me》 剧情
《战狼 2》 战争
《战狼 2》 动作
《战狼 2》 灾难
```

建表

```sql
hive (default)> create table movie_info(
              > movie string,
              > category string)
              > row format delimited fields terminated by "\t";
OK
```

导入数据

```sql
hive (default)> load data local inpath "/root/movie_info.txt" into table movie_info;
Loading data to table default.movie_info
OK
```

对 movie_info 表的每行应用 explode 函数，然后将得到的结果和原表连接，最后就可以根据连接的结果再 select 或聚合。

先看下连接的结果是怎样的

```sql
hive (default)> select
              > *
              > from movie_info
              > lateral view explode(split(category,',')) movie_info_tmp as category_name;
OK
movie_info.movie        movie_info.category     movie_info_tmp.category_name
《疑犯追踪》    悬疑,动作,科幻,剧情     悬疑
《疑犯追踪》    悬疑,动作,科幻,剧情     动作
《疑犯追踪》    悬疑,动作,科幻,剧情     科幻
《疑犯追踪》    悬疑,动作,科幻,剧情     剧情
《Lie to me》   悬疑,警匪,动作,心理,剧情        悬疑
《Lie to me》   悬疑,警匪,动作,心理,剧情        警匪
《Lie to me》   悬疑,警匪,动作,心理,剧情        动作
《Lie to me》   悬疑,警匪,动作,心理,剧情        心理
《Lie to me》   悬疑,警匪,动作,心理,剧情        剧情
《战狼 2》      战争,动作,灾难  战争
《战狼 2》      战争,动作,灾难  动作
《战狼 2》      战争,动作,灾难  灾难
```


解决需求

```sql
hive (default)> select
              >    movie,
              >    category_name
              > from movie_info
              > lateral view explode(split(category,',')) movie_info_tmp as category_name;
OK
movie   category_name
《疑犯追踪》    悬疑
《疑犯追踪》    动作
《疑犯追踪》    科幻
《疑犯追踪》    剧情
《Lie to me》   悬疑
《Lie to me》   警匪
《Lie to me》   动作
《Lie to me》   心理
《Lie to me》   剧情
《战狼 2》      战争
《战狼 2》      动作
《战狼 2》      灾难
```

也可以再聚合，比如统计每个电影有几个标签

```sql
hive (default)> select movie, count(category_name)
              > from movie_info
              > lateral view explode(split(category,',')) movie_info_tmp as category_name
              > group by movie;
.....
OK
movie   _c1
《战狼 2》      3
《疑犯追踪》    4
《Lie to me》   5              
```

使用多个 lateral view 示例

```sql
hive (default)> create table movie_info_2(
              > movie string,
              > category string,
              > actor string)
              > row format delimited fields terminated by "\t";
OK
Time taken: 0.052 seconds

hive (default)> load data local inpath '/export/datas/movie_info_2.txt' into table movie_info_2;
Loading data to table default.movie_info_2
Table default.movie_info_2 stats: [numFiles=1, numRows=0, totalSize=148, rawDataSize=0]
OK
Time taken: 0.3 seconds

hive (default)> select * from movie_info_2;
OK
movie_info_2.movie	movie_info_2.category	movie_info_2.actor
《疑犯追踪》	悬疑,动作,科幻,剧情	a,b,c
《Lie to me》	悬疑,警匪,动作,心理,剧情	a,c
《战狼 2》	战争,动作,灾难	a,b
Time taken: 0.052 seconds, Fetched: 3 row(s)

hive (default)> select movie,c1 from movie_info_2 lateral view explode(split(category,",")) ctb as c1;
OK
movie	c1
《疑犯追踪》	悬疑
《疑犯追踪》	动作
《疑犯追踪》	科幻
《疑犯追踪》	剧情
《Lie to me》	悬疑
《Lie to me》	警匪
《Lie to me》	动作
《Lie to me》	心理
《Lie to me》	剧情
《战狼 2》	战争
《战狼 2》	动作
《战狼 2》	灾难
Time taken: 0.05 seconds, Fetched: 12 row(s)

hive (default)> select movie,c1,c2 from movie_info_2 
              > lateral view explode(split(category,",")) ctb as c1
              > lateral view explode(split(actor,",")) atb as c2;
OK
movie	c1	c2
《疑犯追踪》	悬疑	a
《疑犯追踪》	悬疑	b
《疑犯追踪》	悬疑	c
《疑犯追踪》	动作	a
《疑犯追踪》	动作	b
《疑犯追踪》	动作	c
《疑犯追踪》	科幻	a
《疑犯追踪》	科幻	b
《疑犯追踪》	科幻	c
《疑犯追踪》	剧情	a
《疑犯追踪》	剧情	b
《疑犯追踪》	剧情	c
《Lie to me》	悬疑	a
《Lie to me》	悬疑	c
《Lie to me》	警匪	a
《Lie to me》	警匪	c
《Lie to me》	动作	a
《Lie to me》	动作	c
《Lie to me》	心理	a
《Lie to me》	心理	c
《Lie to me》	剧情	a
《Lie to me》	剧情	c
《战狼 2》	战争	a
《战狼 2》	战争	b
《战狼 2》	动作	a
《战狼 2》	动作	b
《战狼 2》	灾难	a
《战狼 2》	灾难	b
Time taken: 0.052 seconds, Fetched: 28 row(s)

```

使用 OUTER 关键字的示例

```sql
hive (default)> select movie,category from movie_info_2 lateral view outer explode(array()) C as a;
OK
movie	category
《疑犯追踪》	悬疑,动作,科幻,剧情
《Lie to me》	悬疑,警匪,动作,心理,剧情
《战狼 2》	战争,动作,灾难
Time taken: 0.046 seconds, Fetched: 3 row(s)

hive (default)> select movie,category from movie_info_2 lateral view explode(array()) C as a;
OK
movie	category
Time taken: 0.049 seconds
```
