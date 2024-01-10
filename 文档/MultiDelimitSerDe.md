# MultiDelimitSerDe

使用 MultiDelimitSerDe，可以将字段间的分隔符是多字符

hive4.0.0

```sql
CREATE TABLE tb_mdserde (
 id string,
 hivearray array<string>,
 hivemap map<string,int>) 
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.MultiDelimitSerDe'                  
WITH SERDEPROPERTIES ("field.delim"="[,]","collection.delim"=":","mapkey.delim"="@");

hive@8361ca1351e2:/opt/hive/test-data$ cat tb_mdserde.txt
1[,]zhangsan:male[,]zhangsan@22
2[,]lisi:female[,]lisi@24

0: jdbc:hive2://localhost:10000> load data local inpath '/opt/hive/test-data/tb_mdserde.txt' into table tb_mdserde;

0: jdbc:hive2://localhost:10000> select * from tb_mdserde;
+----------------+-----------------------+---------------------+
| tb_mdserde.id  | tb_mdserde.hivearray  | tb_mdserde.hivemap  |
+----------------+-----------------------+---------------------+
| 1              | ["zhangsan","male"]   | {"zhangsan":22}     |
| 2              | ["lisi","female"]     | {"lisi":24}         |
+----------------+-----------------------+---------------------+
```

[更多信息](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/MultiDelimitSerDe.md)