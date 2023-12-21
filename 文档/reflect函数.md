# reflect函数

```sql
hive (default)> desc function reflect;
OK
tab_name
reflect(class,method[,arg1[,arg2..]]) calls method with reflection
```

reflect使用反射实例化，并调用对象的方法，也可以调用静态方法。

它必须返回hive知道如何序列化的基础类型。

1. 调静态方法

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

2. 调非静态方法

TODO


[官网](https://cwiki.apache.org/confluence/display/Hive/ReflectUDF)