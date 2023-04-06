# udf-udtf-udaf的简单使用

[TOC]

## 1 UDF

(1)建一个新类，继承GenericUDF，实现initialize、evaluate和getDisplayString方法。

```java
package hive.udf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.StringObjectInspector;

@Description(name="GetLength",
        value="_FUNC_(str) - Returns the length of this string.",
        extended = "Example:\n"
                + " > SELECT _FUNC_('abc') FROM src; \n")
public class GetLengthG extends GenericUDF {

    StringObjectInspector ss;

    // 初始化，可以校验参数个数
    @Override
    public ObjectInspector initialize(ObjectInspector[] arguments) throws UDFArgumentException {
        if(arguments.length>1){
            throw new UDFArgumentLengthException("GetLength Only take one argument:ss");
        }

        ss = (StringObjectInspector) arguments[0];

        return ss;

    }

    // 具体的计算逻辑
    @Override
    public Object evaluate(DeferredObject[] arguments) throws HiveException {
        String s = ss.getPrimitiveJavaObject(arguments[0].get());

        return s.length();
    }

    @Override
    public String getDisplayString(String[] children) {
        return "GetLength";
    }
}

```   

-----------------------------------------

```java
// UDF类被弃用了
package hive.udf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDF;

@Description(name="GetLength",
        value="_FUNC_(str) - Returns the length of this string.",
        extended = "Example:\n"
                + " > SELECT _FUNC_('abc') FROM src; \n")

public class GetLength extends UDF {
    public int evaluate(String s) {
        return s.length();
    }
}

```

------------------------------------------ 

(2)将代码打成 jar 包，并将这个 jar 包添加到 Hive classpath。

```sh
hive> add jar /root/jar/udfgetlength.jar;
Added [/root/jar/udfgetlength.jar] to class path
Added resources: [/root/jar/udfgetlength.jar]
```

(3)注册自定义函数，并使用

```sh
# function是新建的函数名，as后的字符串是主类路径
hive> create temporary function GetLength as 'hive.udf.GetLength';
OK
Time taken: 0.051 seconds
hive> describe function GetLength;
OK
GetLength(str) - Returns the length of this string.
Time taken: 0.028 seconds, Fetched: 1 row(s)
hive> select GetLength("abc");
OK
3
Time taken: 0.415 seconds, Fetched: 1 row(s)
```

(4)在hive的命令行窗口删除函数

    Drop [temporary] function [if exists] [dbname.]function_name;

```sh
hive> Drop temporary function GetLength;
OK
Time taken: 0.018 seconds
```

## 2 UDTF

(1)建一个新类，继承GenericUDTF，实现initialize、process和close方法。

```java
package hive.udtf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDTF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;

import java.util.ArrayList;
import java.util.List;

@Description(name="GetName",
        value="_FUNC_(str) - Returns the name this string contains.",
        extended = "Example:\n"
                + " > SELECT _FUNC_('mike:jackson') FROM src; \n")
public class GetName extends GenericUDTF {

    // 初始化，定义输出列名，也可以初始化参数
    @Override
    public StructObjectInspector initialize(StructObjectInspector argOIs) throws UDFArgumentException {

        List<String> fieldNames = new ArrayList<String>();
        List<ObjectInspector> fieldOIs = new ArrayList<ObjectInspector>();

        fieldNames.add("first_name");
        fieldOIs.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);

        fieldNames.add("last_name");
        fieldOIs.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);

        return ObjectInspectorFactory.getStandardStructObjectInspector(fieldNames, fieldOIs);
    }

    // 具体的处理逻辑
    @Override
    public void process(Object[] args) throws HiveException {
        String name = args[0].toString();
        // 把结果提交给 collector 输出
        forward(name.split(":"));
    }

    @Override
    public void close() throws HiveException {

    }
}

```
(2)打jar包、注册、使用

```sh
hive> add jar /root/jar/getname.jar;
Added [/root/jar/getname.jar] to class path
Added resources: [/root/jar/getname.jar]

hive> create temporary function GetName as 'hive.udtf.GetName';
OK
Time taken: 0.027 seconds

hive> select GetName("mike:jackson");
OK
mike    jackson
Time taken: 4.113 seconds, Fetched: 1 row(s)
```


## 3 UDAF


```java
package udaf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.udf.generic.*;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.IntObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.io.IntWritable;

@Description(name = "CountA",
        value = "_FUNC_(str) - Returns the number of string containing 'a'.",
        extended = "Example:\n"
                + " > SELECT _FUNC_('abc') FROM src; \n")
public class CountA extends AbstractGenericUDAFResolver {

    @Override
    public GenericUDAFEvaluator getEvaluator(GenericUDAFParameterInfo info) throws SemanticException {

        return new CountAEvaluator();
    }

    public static class CountAEvaluator extends GenericUDAFEvaluator{

        private IntWritable result;
        private IntObjectInspector partialCountAAggOI;

        @Override
        public ObjectInspector init(Mode mode, ObjectInspector[] parameters) throws HiveException {
            super.init(mode, parameters);
            if (mode == Mode.PARTIAL2 || mode == Mode.FINAL) {
                partialCountAAggOI = (IntObjectInspector)parameters[0];
            }

            result = new IntWritable(0);
            return PrimitiveObjectInspectorFactory.writableIntObjectInspector;
        }

        static class CountAAgg extends AbstractAggregationBuffer {
            int value;
        }

        // 创建一个用来存储临时聚合结果的对象
        @Override
        public AggregationBuffer getNewAggregationBuffer() throws HiveException {
            CountAAgg countAAgg = new CountAAgg();
            reset(countAAgg);
            return countAAgg;
        }

        @Override
        public void reset(AggregationBuffer agg) throws HiveException {
            ((CountAAgg)agg).value = 0;
        }

        // 处理一个新数据行，并存入聚合缓存
        @Override
        public void iterate(AggregationBuffer agg, Object[] parameters) throws HiveException {
            String item = parameters[0].toString();
            if (item.contains("a")){
                ((CountAAgg)agg).value++;
            }
        }

        // 返回当前聚合的结果
        @Override
        public Object terminatePartial(AggregationBuffer agg) throws HiveException {
            return terminate(agg);
        }

        // 合并由terminatePartial返回的部分聚合结果，到当前聚合操作下
        @Override
        public void merge(AggregationBuffer agg, Object partial) throws HiveException {
            ((CountAAgg)agg).value += partialCountAAggOI.get(partial);
        }

        // 返回聚合的最终结果给hive
        @Override
        public Object terminate(AggregationBuffer agg) throws HiveException {
            result.set(((CountAAgg) agg).value);
            return result;
        }
    }
}

```

创建测试表，并插入数据

```sql
hive (default)> create table test6(name string);

hive (default)> insert into test6 values("a"),("b"),("ac"),("d");
```

测试

```sql
hive (default)> add jar /root/hiveproject-1.0-SNAPSHOT.jar;
Added [/root/hiveproject-1.0-SNAPSHOT.jar] to class path
Added resources: [/root/hiveproject-1.0-SNAPSHOT.jar]

hive (default)> create temporary function CountA as "udaf.CountA";

hive (default)> select CountA(name) from test6;
...
_c0
2
```

Mode 枚举类。来自 [这里](https://www.cnblogs.com/longjshz/p/5567618.html)

```java
public static enum Mode {
    /**
     * 相当于map阶段，调用iterate()和terminatePartial()
     */
    PARTIAL1,
    /**
     * 相当于combiner阶段，调用merge()和terminatePartial()
     */
    PARTIAL2,
    /**
     * 相当于reduce阶段调用merge()和terminate()
     */
    FINAL,
    /**
     * COMPLETE: 相当于没有reduce阶段map，调用iterate()和terminate()
     */
    COMPLETE
  };
```

---------------------------------------------
[点这里](https://github.com/apache/hive/tree/master/ql/src/java/org/apache/hadoop/hive/ql/udf) 查看更多 UDF、UDTF 和 UDAF 用例

[点这里](https://github.com/ZGG2016/hive-website/blob/master/User%20Documentation/Hive%20SQL%20Language%20Manual/GenericUDAFCaseStudy.md) 查看 UDAF 用例讲解