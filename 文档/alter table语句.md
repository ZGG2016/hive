# alter table语句

[TOC]

**hive-1.1.0-cdh5.14.0**

## Rename Table

```sql
hive (default)> desc formatted users;
OK
col_name	data_type	comment
# col_name            	data_type           	comment             
	 	 
id                  	string              	                    
name                	string              	                    
age                 	string              	                    
	 	 
# Detailed Table Information	 	 
Database:           	default             	 
Owner:              	root                	 
CreateTime:         	Thu Nov 23 21:24:14 CST 2023	 
LastAccessTime:     	UNKNOWN             	 
Protect Mode:       	None                	 
Retention:          	0                   	 
Location:           	hdfs://bigdata-cdh01.itcast.cn:8020/user/hive/warehouse/users	【√】
Table Type:         	MANAGED_TABLE       	 
Table Parameters:	 	 
	COLUMN_STATS_ACCURATE	true                
	numFiles            	1                   
	numRows             	3                   
	rawDataSize         	33                  
	totalSize           	36                  
	transient_lastDdlTime	1700745938          
	 	 
# Storage Information	 	 
SerDe Library:      	org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe	 
InputFormat:        	org.apache.hadoop.mapred.TextInputFormat	 
OutputFormat:       	org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat	 
Compressed:         	No                  	 
Num Buckets:        	-1                  	 
Bucket Columns:     	[]                  	 
Sort Columns:       	[]                  	 
Storage Desc Params:	 	 
	serialization.format	1                   
Time taken: 0.086 seconds, Fetched: 33 row(s)

hive (default)> alter table users rename to user_tb;
OK
Time taken: 0.917 seconds
hive (default)> desc formatted users;
FAILED: SemanticException [Error 10001]: Table not found users
hive (default)> desc formatted user_tb;
OK
col_name	data_type	comment
# col_name            	data_type           	comment             
	 	 
id                  	string              	                    
name                	string              	                    
age                 	string              	                    
	 	 
# Detailed Table Information	 	 
Database:           	default             	 
Owner:              	root                	 
CreateTime:         	Thu Nov 23 21:24:14 CST 2023	 
LastAccessTime:     	UNKNOWN             	 
Protect Mode:       	None                	 
Retention:          	0                   	 
Location:           	hdfs://bigdata-cdh01.itcast.cn:8020/user/hive/warehouse/user_tb	  【√】
Table Type:         	MANAGED_TABLE       	 
Table Parameters:	 	 
	COLUMN_STATS_ACCURATE	true                
	last_modified_by    	root                
	last_modified_time  	1703159980          
	numFiles            	1                   
	numRows             	3                   
	rawDataSize         	33                  
	totalSize           	36                  
	transient_lastDdlTime	1703159980          
	 	 
# Storage Information	 	 
SerDe Library:      	org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe	 
InputFormat:        	org.apache.hadoop.mapred.TextInputFormat	 
OutputFormat:       	org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat	 
Compressed:         	No                  	 
Num Buckets:        	-1                  	 
Bucket Columns:     	[]                  	 
Sort Columns:       	[]                  	 
Storage Desc Params:	 	 
	serialization.format	1                   
Time taken: 0.07 seconds, Fetched: 35 row(s)
```

## Alter Table Properties

```sql
alter table user_tb set tblproperties("comment"="users table", "transactional"="true","EXTERNAL"="TRUE");

hive (default)> desc formatted user_tb;
OK
col_name	data_type	comment
# col_name            	data_type           	comment             
	 	 
id                  	string              	                    
name                	string              	                    
age                 	string              	                    
	 	 
# Detailed Table Information	 	 
Database:           	default             	 
Owner:              	root                	 
CreateTime:         	Thu Dec 21 20:15:12 CST 2023	 
LastAccessTime:     	UNKNOWN             	 
Protect Mode:       	None                	 
Retention:          	0                   	 
Location:           	hdfs://bigdata-cdh01.itcast.cn:8020/user/hive/warehouse/user_tb	 
Table Type:         	EXTERNAL_TABLE      	 【√】 
Table Parameters:	 	 
	COLUMN_STATS_ACCURATE	false               
	EXTERNAL            	TRUE      【√】           
	comment             	users table     【√】     
	last_modified_by    	root                
	last_modified_time  	1703161066          
	numFiles            	1                   
	numRows             	-1                  
	rawDataSize         	-1                  
	totalSize           	36                  
	transactional       	true           【√】       
	transient_lastDdlTime	1703161066          
	 	 
# Storage Information	 	 
SerDe Library:      	org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe	 
InputFormat:        	org.apache.hadoop.mapred.TextInputFormat	 
OutputFormat:       	org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat	 
Compressed:         	No                  	 
Num Buckets:        	-1                  	 
Bucket Columns:     	[]                  	 
Sort Columns:       	[]                  	 
Storage Desc Params:	 	 
	serialization.format	1                   
Time taken: 0.055 seconds, Fetched: 38 row(s)
```

## Add SerDe Properties

```
ALTER TABLE table_name [PARTITION partition_spec] SET SERDE serde_class_name [WITH SERDEPROPERTIES serde_properties];

ALTER TABLE table_name [PARTITION partition_spec] SET SERDEPROPERTIES serde_properties;

serde_properties:
	: (property_name = property_value, property_name = property_value, ... )
```

```sql
alter table user_tb set serde 'org.apache.hive.hcatalog.data.JsonSerDe' with serdeproperties('serialization.encoding'='GBK');
hive (default)> desc formatted user_tb;
OK
col_name	data_type	comment
# col_name            	data_type           	comment             
	 	 
id                  	string              	from deserializer   
name                	string              	from deserializer   
age                 	string              	from deserializer   
	 	 
# Detailed Table Information	 	 
Database:           	default             	 
Owner:              	root                	 
CreateTime:         	Thu Dec 21 20:15:12 CST 2023	 
LastAccessTime:     	UNKNOWN             	 
Protect Mode:       	None                	 
Retention:          	0                   	 
Location:           	hdfs://bigdata-cdh01.itcast.cn:8020/user/hive/warehouse/user_tb	 
Table Type:         	EXTERNAL_TABLE      	 
Table Parameters:	 	 
	COLUMN_STATS_ACCURATE	false               
	EXTERNAL            	TRUE                
	comment             	users table         
	last_modified_by    	root                
	last_modified_time  	1703161374          
	numFiles            	1                   
	numRows             	-1                  
	rawDataSize         	-1                  
	totalSize           	36                  
	transactional       	true                
	transient_lastDdlTime	1703161374          
	 	 
# Storage Information	 	 
SerDe Library:      	org.apache.hive.hcatalog.data.JsonSerDe	   【√】
InputFormat:        	org.apache.hadoop.mapred.TextInputFormat	 
OutputFormat:       	org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat	 
Compressed:         	No                  	 
Num Buckets:        	-1                  	 
Bucket Columns:     	[]                  	 
Sort Columns:       	[]                  	 
Storage Desc Params:	 	 
	serialization.encoding	GBK     【√】             
	serialization.format	1                   
Time taken: 0.06 seconds, Fetched: 39 row(s)
```

删除属性从hive4.0.0开始支持

```sql
alter table user_tb unset serdeproperties('serialization.encoding');
```

## Alter Table Storage Properties

这些语句修改表的物理存储属性。

注意：这些命令只会修改 Hive 的元数据，不会重新组织或重新格式化现有的数据。用户应该确保实际的数据布局与元数据定义一致。

```sql
hive (default)> select * from user_tb;
OK
user_tb.id	user_tb.name	user_tb.age
1	zhangsan	14
2	lisi	14
3	wangwu	23
Time taken: 0.051 seconds, Fetched: 3 row(s)

hive (default)> desc formatted user_tb;
OK
...	 	 
# Storage Information	 	 
SerDe Library:      	org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe	 
InputFormat:        	org.apache.hadoop.mapred.TextInputFormat	 
OutputFormat:       	org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat	 
Compressed:         	No                  	 
Num Buckets:        	-1                  	 
Bucket Columns:     	[]                  	 
Sort Columns:       	[]                  	 
Storage Desc Params:	 	 
	serialization.format	1                   
Time taken: 0.059 seconds, Fetched: 33 row(s)

hive (default)> alter table user_tb clustered by(age) sorted by (id) into 2 buckets;
OK
Time taken: 0.069 seconds

hive (default)> desc formatted user_tb;
OK
...
# Storage Information	 	 
SerDe Library:      	org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe	 
InputFormat:        	org.apache.hadoop.mapred.TextInputFormat	 
OutputFormat:       	org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat	 
Compressed:         	No                  	 
Num Buckets:        	2                   	 
Bucket Columns:     	[age]               	 
Sort Columns:       	[Order(col:id, order:1)]	 
Storage Desc Params:	 	 
	serialization.format	1                   
Time taken: 0.043 seconds, Fetched: 35 row(s)
```

修改前后都是这种组织

```
[root@bigdata-cdh01 datas]# hadoop fs -ls /user/hive/warehouse/user_tb
Found 3 items
-rwxrwxr-x   1 root supergroup         14 2023-12-21 20:33 /user/hive/warehouse/user_tb/000000_0
-rwxrwxr-x   1 root supergroup         10 2023-12-21 20:33 /user/hive/warehouse/user_tb/000000_0_copy_1
-rwxrwxr-x   1 root supergroup         12 2023-12-21 20:34 /user/hive/warehouse/user_tb/000000_0_copy_2
[root@bigdata-cdh01 datas]# hadoop fs -cat /user/hive/warehouse/user_tb/000000_0_copy_1
2lisi14
[root@bigdata-cdh01 datas]# hadoop fs -cat /user/hive/warehouse/user_tb/000000_0_copy_2
3wangwu23
[root@bigdata-cdh01 datas]# hadoop fs -cat /user/hive/warehouse/user_tb/000000_0
1zhangsan14
```

## Alter Table Skewed



## Alter Table Not Skewed



## Alter Table Set Skewed Location



## Alter Table Constraints

TODO

-------------------------------

[官网](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AlterTable)

[官网翻译](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20SQL%20Language%20Manual/DDL%20Statements.md#151alter-table)