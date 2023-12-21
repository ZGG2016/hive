# alter column语句

[TOC]

## Change Column Name/Type/Position/Comment

```
ALTER TABLE table_name [PARTITION partition_spec] CHANGE [COLUMN] col_old_name col_new_name column_type
[COMMENT col_comment] [FIRST|AFTER column_name] [CASCADE|RESTRICT];
```

- `col_old_name col_new_name` 改变名称
- `column_type` 改变类型
- `[COMMENT col_comment]` 改变列注释
- `FIRST` 将列放到第一列
- `AFTER column_name`  将列放到 column_name 列的后面
- `CASCADE`  修改表的元数据的列，并将相同的修改级联到所有分区元数据
- `RESTRICT` 默认值，只限制对表的元数据的列更改

```
hive (default)> desc user_tb;
OK
col_name	data_type	comment
id                  	string              	                    
name                	string              	                    
age                 	string              	                    
Time taken: 0.063 seconds, Fetched: 3 row(s)
```


```sql
alter table user_tb change id uid int comment 'user id' after name;

hive (default)> desc user_tb;
OK
col_name	data_type	comment
name                	string              	                    
uid                 	int                 	user id             
age                 	string              	                    
Time taken: 0.063 seconds, Fetched: 3 row(s)
```

## Add/Replace Columns

```
ALTER TABLE table_name 
  [PARTITION partition_spec]                 -- (Note: Hive 0.14.0 and later)
  ADD|REPLACE COLUMNS (col_name data_type [COMMENT col_comment], ...)
  [CASCADE|RESTRICT]                         -- (Note: Hive 1.1.0 and later)
```

- `ADD` 将新列添加到现有列的末尾，但在分区列之前。

- `REPLACE` 删除所有现有列，并添加一组新的列。这只能对具有原生 SerDe 的表(DynamicSerDe、MetadataTypedColumnsetSerDe、LazySimpleSerDe和columnnarserde)进行。

- `[CASCADE|RESTRICT]` 作用同上 

```sql
alter table user_tb add columns(gender string comment 'user gender');
hive (default)> alter table user_tb add columns(gender string comment 'user gender');
OK
Time taken: 0.091 seconds
hive (default)> desc user_tb;
OK
col_name	data_type	comment
name                	string              	                    
uid                 	int                 	user id             
age                 	string              	                    
gender              	string              	user gender         
Time taken: 0.054 seconds, Fetched: 4 row(s)
```

```sql
-- 删除所有现有列，并添加一组新的列。（也可以用于删除列）
alter table user_tb replace columns(orderid int, sku string);
hive (default)> desc user_tb;
OK
col_name	data_type	comment
orderid             	int                 	                    
sku                 	string              	                    
Time taken: 0.05 seconds, Fetched: 2 row(s)
```

## Partial Partition Specification

为上面的某些更改列语句提供部分分区规范，这与动态分区类似。因此，不必为每个需要更改的分区发出更改列语句

```sql
hive (default)> create table order_table_s
              > (
              >     order_id int,         -- 订单id
              >     product_name string,  -- 产品名称
              >     price int             -- 产品价格
              > )
              > partitioned by (deal_day string)  -- 交易日期YYYYMM
              > row format delimited
              > fields terminated by "\t";
OK
Time taken: 0.055 seconds

hive (default)> desc order_table_s;
OK
col_name	data_type	comment
order_id            	int                 	                    
product_name        	string              	                    
price               	int                 	                    
deal_day            	string              	                    
	 	 
# Partition Information	 	 
# col_name            	data_type           	comment             
	 	 
deal_day            	string              	                    
Time taken: 0.049 seconds, Fetched: 9 row(s)

hive (default)> load data local inpath '/export/datas/order-201901.txt' overwrite into table order_table_s partition(deal_day='201901');
hive (default)> load data local inpath '/export/datas/order-201902.txt' overwrite into table order_table_s partition(deal_day='201902');
```

```sql
ALTER TABLE order_table_s PARTITION (deal_day='201901') CHANGE COLUMN price amount DECIMAL(38,18);

hive (default)> desc order_table_s PARTITION (deal_day='201901');
OK
col_name	data_type	comment
order_id            	int                 	                    
product_name        	string              	                    
amount              	decimal(38,18)      	                    
deal_day            	string              	                    
	 	 
# Partition Information	 	 
# col_name            	data_type           	comment             
	 	 
deal_day            	string              	                    
Time taken: 0.085 seconds, Fetched: 9 row(s)

ive (default)> desc order_table_s PARTITION (deal_day='201902');
OK
col_name	data_type	comment
order_id            	int                 	                    
product_name        	string              	                    
price               	int                 	                    
deal_day            	string              	                    
	 	 
# Partition Information	 	 
# col_name            	data_type           	comment             
	 	 
deal_day            	string              	                    
Time taken: 0.069 seconds, Fetched: 9 row(s)
```

使用一个单独的 ALTER 语句同时更改多个现有分区，类似动态分区

```sql
// hive.exec.dynamic.partition needs to be set to true to enable dynamic partitioning with ALTER PARTITION
SET hive.exec.dynamic.partition = true;
  
// This will alter all existing partitions in the table with ds='2008-04-08' -- be sure you know what you are doing!
ALTER TABLE foo PARTITION (ds='2008-04-08', hr) CHANGE COLUMN dec_column_name dec_column_name DECIMAL(38,18);
 
// This will alter all existing partitions in the table -- be sure you know what you are doing!
ALTER TABLE foo PARTITION (ds, hr) CHANGE COLUMN dec_column_name dec_column_name DECIMAL(38,18);
```

-------------------------------

[官网](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AlterColumn)

[官网翻译](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/DDL%20Statements.md#154alter-column)