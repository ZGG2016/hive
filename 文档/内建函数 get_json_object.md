# 内建函数 get_json_object


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