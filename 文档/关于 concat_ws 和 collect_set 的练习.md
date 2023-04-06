# 关于 concat_ws 和 collect_set 的练习

数据源

```sh
root@bigdata101:~# cat person_info.txt
孙悟空,白羊座,A
大海,射手座,A
宋宋,白羊座,B
猪八戒,白羊座,A
凤姐,射手座,A
苍老师,白羊座,B
```

建表

```sql
hive (default)> create table person_info(
              > name string,
              > constellation string,
              > blood_type string)
              > row format delimited fields terminated by ",";
```

导入数据

```sql
hive (default)> load data local inpath "/root/person_info.txt" into table person_info;
```

想要得到这种形式的结果：

```
射手座,A	   大海|凤姐
白羊座,A   孙悟空|猪八戒
白羊座,B   宋宋|苍老师
```

即，把星座和血型一样的人归类到一起

需要将星座和血型拼成一个字段，再根据这个字段分组，将组内的名字用 `|` 拼接。


```sql
hive (default)> select a,
              > concat_ws("|",collect_set(name)) # 先按名字去重汇总成数组，再拼接
              > from
              > (
              > select name,
              >    concat_ws('-',constellation,blood_type) a
              > from person_info
              > ) t
              > group by a;
```

如果不需要拼接，也可以也这样：

```sql
hive (default)> select constellation,blood_type, 
              >   collect_set(name)
              > from person_info
              > group by constellation,blood_type;
 ...
constellation   blood_type      _c2
射手座  A       ["大海","凤姐"]
白羊座  A       ["孙悟空","猪八戒"]
白羊座  B       ["宋宋","苍老师"]             
```

参考：

- [尚硅谷hive教程](https://www.bilibili.com/video/BV1EZ4y1G7iL)
- [官网](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF)