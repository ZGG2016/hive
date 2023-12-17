# hive视图

[TOC]

准备数据

```
hive (default)> create table hive_view_tb (id int, name string, province string);
hive (default)> insert into hive_view_tb values
              > (1,"zhangsan","shandong"),
              > (2,"lisi","beijing"),
              > (3,"wangwu","guangdong"),
              > (4,"tom","jiangsu");
hive (default)> select * from hive_view_tb;
OK
hive_view_tb.id	hive_view_tb.name	hive_view_tb.province
1	zhangsan	shandong
2	lisi	beijing
3	wangwu	guangdong
4	tom	jiangsu
```

## create view

视图是一个没有关联存储的纯逻辑对象。

当查询引用视图时，将计算视图的定义，以生成一组行，供查询进一步处理。

(这是一个概念性的描述；事实上，作为查询优化的一部分，Hive可能会将视图的定义和查询的定义结合起来，例如将查询中的过滤器从下推到视图中。)

```
create view if not exists default.hive_view(id, name, province)
comment 'hive view test'
tblproperties("transactional"="true")
as select * from hive_view_tb;

hive (default)> select * from hive_view;
OK
hive_view.id	hive_view.name	hive_view.province
1	zhangsan	shandong
2	lisi	beijing
3	wangwu	guangdong
4	tom	jiangsu
Time taken: 0.07 seconds, Fetched: 4 row(s)
```

```
hive (default)> desc formatted hive_view;
OK
col_name	data_type	comment
# col_name            	data_type           	comment             
	 	 
id                  	int                 	                    
name                	string              	                    
province            	string              	                    
	 	 
# Detailed Table Information	 	 
Database:           	default             	 
Owner:              	root                	 
CreateTime:         	Sun Dec 17 14:54:21 CST 2023	 
LastAccessTime:     	UNKNOWN             	 
Protect Mode:       	None                	 
Retention:          	0                   	 
Table Type:         	VIRTUAL_VIEW        	 
Table Parameters:	 	 
	comment             	hive view test      
	transactional       	true                
	transient_lastDdlTime	1702796061          
	 	 
# Storage Information	 	 
SerDe Library:      	null                	 
InputFormat:        	org.apache.hadoop.mapred.TextInputFormat	 
OutputFormat:       	org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat	 
Compressed:         	No                  	 
Num Buckets:        	-1                  	 
Bucket Columns:     	[]                  	 
Sort Columns:       	[]                  	 
	 	 
# View Information	 	 
View Original Text: 	select * from hive_view_tb	 
View Expanded Text: 	SELECT `id` AS `id`, `name` AS `name`, `province` AS `province` FROM (select `hive_view_tb`.`id`, `hive_view_tb`.`name`, `hive_view_tb`.`province` from `default`.`hive_view_tb`) `default.hive_view`	 
Time taken: 0.06 seconds, Fetched: 31 row(s)
```

## 视图的模式在视图创建时被冻结

视图的模式在视图创建时被冻结，

对底层表的后续更改(例如添加一列)不会反映在视图的schema中。

如果以不兼容的方式删除或更底层表，则后续查询无效视图的尝试将失败。

```
-- 对hive_view_tb新增一列
hive (default)> alter table hive_view_tb add columns(age int);
OK
Time taken: 0.099 seconds
hive (default)> desc hive_view_tb;
OK
col_name	data_type	comment
id                  	int                 	                    
name                	string              	                    
province            	string              	                    
age                 	int                 	                    
Time taken: 0.056 seconds, Fetched: 4 row(s)

hive (default)> desc hive_view;
OK
col_name	data_type	comment
id                  	int                 	                    
name                	string              	                    
province            	string              	                    
Time taken: 0.052 seconds, Fetched: 3 row(s)
```

## 视图是只读的

视图是只读的，不能作为 LOAD/INSERT/ALTER 的目标

`alter view` 修改的是视图元数据，即 `ALTER VIEW [db_name.]view_name SET TBLPROPERTIES table_properties;`

## 视图包含 ORDER BY 和 LIMIT 子句

如果一个引用查询也包含这些子句，那么查询级子句将在视图子句之后(以及查询中的任何其他操作之后)进行计算。

```
create view hive_view_2(id, name, province)
as select id, name, province from hive_view_tb order by id desc limit 3;

hive (default)> select * from hive_view_2;
Query ID = root_20231217151010_1c2df5fc-a243-4eab-aee5-3c966ea546be
Total jobs = 1
hive_view_2.id	hive_view_2.name	hive_view_2.province
4	tom	jiangsu
3	wangwu	guangdong
2	lisi	beijing
Time taken: 21.5 seconds, Fetched: 3 row(s)

hive (default)> select * from hive_view_2 limit 4;
Query ID = root_20231217151212_7ce4cf7e-c82a-49b0-a2cc-0a64e266a5aa
Total jobs = 1
hive_view_2.id	hive_view_2.name	hive_view_2.province
4	tom	jiangsu
3	wangwu	guangdong
2	lisi	beijing
Time taken: 18.702 seconds, Fetched: 3 row(s)
```

## CTE

视图的 select 语句可以包括一个或多个 CTEs

```
create view hive_view_3 as
with q1 as ( select id, name, province from hive_view_tb where id=1)
select * from q1;

hive (default)> select * from hive_view_3;
OK
hive_view_3.id	hive_view_3.name	hive_view_3.province
1	zhangsan	shandong
Time taken: 0.141 seconds, Fetched: 1 row(s)
```

```
create view hive_view_4 as
with t1 as ( select 1 as id, "zhangsan" as name),
     t2 as ( select 1 as id, "beijing"as province)
select t1.id,t1.name,t2.province from t1 join t2 on t1.id=t2.id;

hive (default)> select * from hive_view_4;
hive_view_4.id	hive_view_4.name	hive_view_4.province
1	zhangsan	beijing
Time taken: 17.759 seconds, Fetched: 1 row(s)
```

## 视图引用视图

```
hive (default)> create view hive_view_5 as select * from hive_view_4;
OK
id	name	province
Time taken: 0.552 seconds

hive (default)> select * from hive_view_5;
hive_view_5.id	hive_view_5.name	hive_view_5.province
1	zhangsan	beijing
Time taken: 17.213 seconds, Fetched: 1 row(s)
```

## 删除视图

版本：hive-1.1.0-cdh5.14.0

```
hive (default)> drop view hive_view_6;
OK
Time taken: 0.025 seconds
-- 视图不存在也不会报错
hive (default)> drop view hive_view_6666;
OK
Time taken: 0.033 seconds
hive (default)> drop view if exists hive_view_6666;
OK
Time taken: 0.037 seconds
```

当删除一个被其他视图引用的视图时，不会给出警告(依赖的视图将作为无效视图而被悬空，必须由用户删除或重新创建)。

```
-- hive_view_5根据hive_view_4创建而来
hive (default)> drop view hive_view_4;
OK
Time taken: 0.177 seconds

hive (default)> select * from hive_view_5;
FAILED: SemanticException Line 1:79 Table not found 'hive_view_4' in definition of VIEW hive_view_5 [
select `hive_view_4`.`id`, `hive_view_4`.`name`, `hive_view_4`.`province` from `default`.`hive_view_4`
] used as hive_view_5 at Line 1:14
```

## Alter View

这是用来修改视图属性的

```
ALTER VIEW [db_name.]view_name SET TBLPROPERTIES table_properties;
```

```
create view hive_view_7 as select * from hive_view_tb;

alter view hive_view_7 set TBLPROPERTIES("transactional"="true");
hive (default)> desc formatted hive_view_7;
...       	 
Table Parameters:	 	 
	last_modified_by    	root                
	last_modified_time  	1702799661          
	transactional       	true                
...
```

## Alter View As Select

更改视图的定义（更改视图定义需要删除视图并重新创建它）

该视图必须已经存在，如果视图有分区，它不能被 Alter View As Select 代替。

```
hive (default)> create view hive_view_8 as select * from hive_view_tb;
OK
id	name	province	age
Time taken: 0.126 seconds
hive (default)> desc hive_view_8;
OK
col_name	data_type	comment
id                  	int                 	                    
name                	string              	                    
province            	string              	                    
age                 	int                 	                    
Time taken: 0.142 seconds, Fetched: 4 row(s)

hive (default)> alter view hive_view_8 as select id,name from hive_view_tb;
OK
id	name
Time taken: 0.273 seconds
hive (default)> desc hive_view_8;
OK
col_name	data_type	comment
id                  	int                 	                    
name                	string              	                    
Time taken: 0.135 seconds, Fetched: 2 row(s)
```

## 视图分区

PARTITIONED ON 子句引用的列必须是视图定义中的最后一列，并且它们在 PARTITIONED ON 子句中的顺序必须与它们在视图定义中的顺序相匹配。

```
-- 分区字段在select最后
hive (default)> create view hive_view_9 partitioned on(id) as select name,id from hive_view_tb;
OK
name	id
Time taken: 0.158 seconds
hive (default)> select * from hive_view_9;
OK
hive_view_9.name	hive_view_9.id
zhangsan	1
lisi	2
wangwu	3
tom	4

hive (default)> create view hive_view_9 partitioned on(id) as select id,name from hive_view_tb;
FAILED: SemanticException [Error 10093]: Rightmost columns in view output do not match PARTITIONED ON clause
```

```
hive (default)> create view hive_view_10 partitioned on(id) as select count(name) cnt,id from hive_view_tb group by id;
OK
cnt	id
Time taken: 0.146 seconds
hive (default)> select * from hive_view_10;
hive_view_10.cnt	hive_view_10.id
1	1
1	2
1	3
1	4
Time taken: 19.491 seconds, Fetched: 4 row(s)
```

添加分区

```
alter view hive_view_9 add partition(id=5);
```

删除分区

```
alter view hive_view_9 drop partition(id=5);
```

-------------------------------

更多详细描述见

官网：[视图语法](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-Create/Drop/AlterView) | [视图]() | [视图分区]()

翻译： [视图语法](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/DDL%20Statements.md) | [视图](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/Resources%20for%20Contributors/Hive%20Design%20Docs/Views.md) | [视图分区](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/Resources%20for%20Contributors/Hive%20Design%20Docs/Partitioned%20Views.md)