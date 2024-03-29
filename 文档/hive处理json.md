# hive处理json

[TOC]

## 内建函数 get_json_object


```sh
hive (default)> desc function get_json_object;
OK
tab_name
get_json_object(json_txt, path) - Extract a json object from path
```

```sql
-- [{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}]
select get_json_object
(
    '{
        "store": {
                "fruit":[{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}],
                "bicycle":{"price":19.95,"color":"red"}
           },
          "email":"amy@only_for_json_udf_test.net",
          "owner":"amy"
     }',
     '$.store.fruit'
);

-- 8
select get_json_object
(
    '{
        "store": {
                "fruit":[{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}],
                "bicycle":{"price":19.95,"color":"red"}
           },
          "email":"amy@only_for_json_udf_test.net",
          "owner":"amy"
     }',
     '$.store.fruit[0].weight'
);


-- red
select get_json_object
(
    '{
        "store": {
                "fruit":[{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}],
                "bicycle":{"price":19.95,"color":"red"}
           },
          "email":"amy@only_for_json_udf_test.net",
          "owner":"amy"
     }',
     '$.store.bicycle.color'
);


-- amy
select get_json_object
(
    '{
        "store": {
                "fruit":[{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}],
                "bicycle":{"price":19.95,"color":"red"}
           },
          "email":"amy@only_for_json_udf_test.net",
          "owner":"amy"
     }',
     '$.owner'
);
```

```sql
-- {"name":"zhangsan","sex":"man","age":"11"}
select get_json_object
(
    '[{"name":"zhangsan","sex":"man","age":"11"},{"name":"lisi","sex":"woman","age":"12"}]',
    '$.[0]'
);

-- zhangsan
select get_json_object
(
    '[{"name":"zhangsan","sex":"man","age":"11"},{"name":"lisi","sex":"woman","age":"12"}]',
    '$.[0].name'
);


-- NULL
select get_json_object('{"name":"zhangsan","sex":"man"}','$.age');
```

## 内建函数 json_tuple

```sh
hive (tags_dat)> desc function json_tuple;
OK
tab_name
json_tuple(jsonStr, p1, p2, ..., pn) - like get_json_object, but it takes multiple names and return a tuple. All the input parameters and output column types are string.
```

```
hive (tags_dat)> select json_tuple
               > (
               >     '{
               >         "store": {
               >                 "fruit":[{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}],
               >                 "bicycle":{"price":19.95,"color":"red"}
               >            },
               >           "email":"amy@only_for_json_udf_test.net",
               >           "owner":"amy"
               >      }',
               >      "store"
               > ) as store;
OK
store
{"fruit":[{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}],"bicycle":{"price":19.95,"color":"red"}}

hive (default)> select json_tuple(store,"fruit", "bicycle") as (fruit,bicycle)
              > from (select json_tuple
              > ('{"store": {"fruit":[{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}],"bicycle":{"price":19.95,"color":"red"}},"email":"amy@only_for_json_udf_test.net","owner":"amy"}',"store","email","owner") as (store,email,owner)) a;
OK
fruit	bicycle
[{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}]	{"price":19.95,"color":"red"}
```
[json_tuple和lateral view配合使用](https://zhuanlan.zhihu.com/p/458213836?utm_id=0)

帮助理解：

```
hive (default)> select * from
              > (select split(regexp_replace(regexp_extract('{"store": {"fruit":[{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}],"bicycle":{"price":19.95,"color":"red"}},"email":"amy@only_for_json_udf_test.net","owner":"amy"}','^(.+)$',1),'\\}\\}\\,','\\}\\}\\-'),'\\-') string_test) a
              > lateral view explode(string_test) tmptb1 as string_test_name;
OK
a.string_test	tmptb1.string_test_name
["{\"store\": {\"fruit\":[{\"weight\":8,\"type\":\"apple\"},{\"weight\":9,\"type\":\"pear\"}],\"bicycle\":{\"price\":19.95,\"color\":\"red\"}}","\"email\":\"amy@only_for_json_udf_test.net\",\"owner\":\"amy\"}"]	{"store": {"fruit":[{"weight":8,"type":"apple"},{"weight":9,"type":"pear"}],"bicycle":{"price":19.95,"color":"red"}}
["{\"store\": {\"fruit\":[{\"weight\":8,\"type\":\"apple\"},{\"weight\":9,\"type\":\"pear\"}],\"bicycle\":{\"price\":19.95,\"color\":\"red\"}}","\"email\":\"amy@only_for_json_udf_test.net\",\"owner\":\"amy\"}"]	"email":"amy@only_for_json_udf_test.net","owner":"amy"}
Time taken: 0.087 seconds, Fetched: 2 row(s)
```

```
[{
	"name": "SSSK001",
	"attr_type": 1,
	"sub_attributes": [{
		"name": "hello world*1",
		"weight": 5
	}, {
		"name": "hello world*2",
		"weight": 5
	}]
}, {
	"name": "SSSK002",
	"attr_type": 2,
	"sub_attributes": [{
		"name": "hello world*3",
		"weight": 8
	}]
}]

select tmptable2.name `手机型号`
       ,tmptable2.attr_type `类型`
       ,tmptable4.remark `参数描述`
       ,tmptable4.weight `重量`
from
(
 select  split(regexp_replace(regexp_extract('[{"name": "SSSK001","attr_type": 1,"sub_attributes": [{"name": "hello world*1","weight": 5},{"name": "hello world*2","weight": 5}]},{"name": "SSSK002","attr_type": 2,"sub_attributes": [{"name": "hello world*3","weight": 8}]}]','^\\[(.+)\\]$',1),'\\}\\]\\}\\,\\{','\\}\\]\\}\\-\\{'),'\\-') string_test
) a LATERAL VIEW explode(string_test) tmptable1 as string_test_name 
LATERAL VIEW json_tuple(string_test_name, 'name', 'attr_type', 'sub_attributes') tmptable2 as name 
, attr_type 
, sub_attributes LATERAL VIEW explode(split(regexp_replace(regexp_extract(sub_attributes, '^\\[(.+)\\]$', 1), '\\}\\,\\{', '\\}\\|\\|\\{'), '\\|\\|')) tmptable3 as string_test_name_2 
LATERAL VIEW json_tuple(string_test_name_2, 'name', 'weight') tmptable4 as remark 
, weight ;
```

## JSONSerde

https://zhuanlan.zhihu.com/p/589579478?utm_id=0

hive3.0后有所不同，见官网描述：https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL

在创建表时，只要指定使用JSONSerde解析表的文件，就会自动将JSON文件中的每一列进行解析。

需要将 hive-hcatalog-core.jar 复制到 lib 目录下，再重启客户端。

```
[root@bigdata-cdh01 lib]# cp ../hcatalog/share/hcatalog/hive-hcatalog-core-1.1.0-cdh5.14.0.jar .

[root@bigdata-cdh01 datas]# cat device.json 
{"device":"device_10","deviceType":"kafka","signal":98.0,"time":11111111}

hive (default)> create table tb_json_test2 (
              >    device string,
              >    deviceType string,
              >    signal double,
              >    `time` string
              >  )
              > ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe'
              > STORED AS TEXTFILE;
OK
Time taken: 1.423 seconds

hive (default)> load data local inpath '/export/datas/device.json' into table tb_json_test2;
Loading data to table default.tb_json_test2
Table default.tb_json_test2 stats: [numFiles=1, totalSize=74]
OK
Time taken: 1.363 seconds

hive (default)> select * from tb_json_test2;
OK
tb_json_test2.device	tb_json_test2.devicetype	tb_json_test2.signal	tb_json_test2.time
device_10	kafka	98.0	11111111
Time taken: 0.622 seconds, Fetched: 1 row(s)

```


