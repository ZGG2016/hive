# Writing UDTFs

[TOC]

## 1、GenericUDTF Interface

> A custom UDTF can be created by extending the GenericUDTF abstract class and then implementing the initialize, process, and possibly close methods. The initialize method is called by Hive to notify the UDTF the argument types to expect. The UDTF must then return an object inspector corresponding to the row objects that the UDTF will generate. Once initialize() has been called, Hive will give rows to the UDTF using the process() method. While in process(), the UDTF can produce and forward rows to other operators by calling forward(). Lastly, Hive will call the close() method when all the rows have passed to the UDTF.

自定义 UDTF 需要继承 GenericUDTF 抽象类，实现 `initialize` 、 `process` 和 `close`方法。

Hive 调用`initialize`方法通知 UDTF 期望的参数类型。

然后，UDTF 必须返回一个对象检查器，它和 UDTF 生成的行对象对应。

一旦调用 `initialize`，Hive 将向 UDTF 提供行，调用 `process` 处理。

在 `process` 中，UDTF 可以通过调用 `forward` 生成行，并将其转发给其他操作符。

最后，当所有行都传递给 UDTF 时，Hive 将调用 `close` 方法。

例子：

```java
package org.apache.hadoop.hive.contrib.udtf.example;
 
import java.util.ArrayList;
 
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDTF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
 
 
/**
 * GenericUDTFCount2 outputs the number of rows seen, twice. It's output twice
 * to test outputting of rows on close with lateral view.
 *
 */
public class GenericUDTFCount2 extends GenericUDTF {
 
  Integer count = Integer.valueOf(0);
  Object forwardObj[] = new Object[1];
 
  @Override
  public void close() throws HiveException {
    forwardObj[0] = count;
    forward(forwardObj);
    forward(forwardObj);
  }
 
  @Override
  public StructObjectInspector initialize(ObjectInspector[] argOIs) throws UDFArgumentException {
    ArrayList<String> fieldNames = new ArrayList<String>();
    ArrayList<ObjectInspector> fieldOIs = new ArrayList<ObjectInspector>();
    fieldNames.add("col1");
    fieldOIs.add(PrimitiveObjectInspectorFactory.javaIntObjectInspector);
    return ObjectInspectorFactory.getStandardStructObjectInspector(fieldNames,
        fieldOIs);
  }
 
  @Override
  public void process(Object[] args) throws HiveException {
    count = Integer.valueOf(count.intValue() + 1);
  }
 
}
```

For reference, here is the abstract class:

```java
package org.apache.hadoop.hive.ql.udf.generic;
 
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
 
/**
 * A Generic User-defined Table Generating Function (UDTF)
 *
 * Generates a variable number of output rows for a single input row. Useful for
 * explode(array)...
 */
 
public abstract class GenericUDTF {
  Collector collector = null;
 
  /**
 * Initialize this GenericUDTF. This will be called only once per instance.
 *
 * @param args
 *          An array of ObjectInspectors for the arguments
 * @return A StructObjectInspector for output. The output struct represents a
 *         row of the table where the fields of the stuct are the columns. The
 *         field names are unimportant as they will be overridden by user
 *         supplied column aliases.
   */
  public abstract StructObjectInspector initialize(ObjectInspector[] argOIs)
      throws UDFArgumentException;
 
  /**
 * Give a set of arguments for the UDTF to process.
 *
 * @param o
 *          object array of arguments
   */
  public abstract void process(Object[] args) throws HiveException;
 
  /**
 * Called to notify the UDTF that there are no more rows to process.
 * Clean up code or additional forward() calls can be made here.
   */
  public abstract void close() throws HiveException;
 
  /**
 * Associates a collector with this UDTF. Can't be specified in the
 * constructor as the UDTF may be initialized before the collector has been
 * constructed.
 *
 * @param collector
   */
  public final void setCollector(Collector collector) {
    this.collector = collector;
  }
 
  /**
 * Passes an output row to the collector.
 *
 * @param o
 * @throws HiveException
   */
  protected final void forward(Object o) throws HiveException {
    collector.collect(o);
  }
 
}

```