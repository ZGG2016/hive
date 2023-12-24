# reflect和reflect2函数

[TOC]

## reflect函数

[源码](https://github.com/apache/hive/blob/master/ql/src/java/org/apache/hadoop/hive/ql/udf/generic/GenericUDFReflect.java)

```sql
hive (default)> desc function reflect;
OK
tab_name
reflect(class,method[,arg1[,arg2..]]) calls method with reflection
```

reflect使用反射实例化，并调用静态方法

前两个参数必须是字符串类型

它必须返回hive知道如何序列化的基础类型。

```sql
select 
    reflect("java.lang.String","valueOf", 1) a,
    reflect("java.lang.String","isEmpty") b,
    reflect("java.lang.Math","max",2,3) c,
    reflect("java.lang.Math","round",2.5) d,
    reflect("java.lang.Math","floor",1.9) e,
    java_method("java.lang.Math","floor",1.9) f
;
OK
a	b	c	d	e	f
1	true	3	3	1.0	1.0
```

```
-- 源码参考理解
select reflect("java.util.ArrayList","add","1");
add方法的参数是泛型E，返回的类型是Object，不是基础类型
```

## reflect2函数

[源码](https://github.com/apache/hive/blob/master/ql/src/java/org/apache/hadoop/hive/ql/udf/generic/GenericUDFReflect2.java)

```sql
0: jdbc:hive2://localhost:10000> desc function reflect2;
+----------------------------------------------------+
|                      tab_name                      |
+----------------------------------------------------+
| reflect2(arg0,method[,arg1[,arg2..]]) calls method of arg0 with reflection |
+----------------------------------------------------+
1 row selected (0.622 seconds)
```

- 第一个参数必须是基础类型
- 方法名需要是字符串常量，不能是字符串的hashcode方法
- 返回类型也是基础类型

```sql
0: jdbc:hive2://localhost:10000> select reflect2("abc","toUpperCase");
+------+
| _c0  |
+------+
| ABC  |
+------+
1 row selected (0.322 seconds)

0: jdbc:hive2://localhost:10000> select reflect2("abc","concat","def");
+---------+
|   _c0   |
+---------+
| abcdef  |
+---------+
1 row selected (0.325 seconds)
```


------------------------

[官网](https://cwiki.apache.org/confluence/display/Hive/ReflectUDF)