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

