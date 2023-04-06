# Hive SerDes

[TOC]

## 1、SerDe Overview

> SerDe is short for Serializer/Deserializer. Hive uses the SerDe interface for IO. The interface handles both serialization and deserialization and also interpreting the results of serialization as individual fields for processing.

SerDe 是序列化器/反序列化器的缩写。

Hive 使用 SerDe 接口进行 IO 操作。

该接口同时处理序列化和反序列化，并将序列化的结果解释为单独的字段进行处理。

> A SerDe allows Hive to read in data from a table, and write it back out to HDFS in any custom format. Anyone can write their own SerDe for their own data formats.

SerDe 允许 Hive 从表中读取数据，并以任何自定义的格式将数据写回 HDFS。

任何人都可以为自己的数据格式编写自己的 SerDe。

> See [Hive SerDe](https://cwiki.apache.org/confluence/display/Hive/DeveloperGuide#DeveloperGuide-HiveSerDe) for an introduction to SerDes.

请参阅 Hive SerDe 了解 SerDes 的介绍。

### 1.1、Built-in and Custom SerDes

> The Hive SerDe library is in org.apache.hadoop.hive.serde2. (The old SerDe library in org.apache.hadoop.hive.serde is deprecated.)

Hive SerDe 库在 `org.apache.hadoop.hive.serde2`。（旧的 Hive SerDe 库在`org.apache.hadoop.hive.serde`，但被弃用了）

#### 1.1.1、Built-in SerDes

- [Avro](https://cwiki.apache.org/confluence/display/Hive/AvroSerDe) (Hive 0.9.1 and later)
- [ORC](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+ORC) (Hive 0.11 and later)
- [RegEx](https://cwiki.apache.org/confluence/display/Hive/GettingStarted#GettingStarted-ApacheWeblogData)
- [Thrift](http://thrift.apache.org/)
- [Parquet](https://cwiki.apache.org/confluence/display/Hive/Parquet) (Hive 0.13 and later)
- [CSV](https://cwiki.apache.org/confluence/display/Hive/CSV+Serde) (Hive 0.14 and later)
- [JsonSerDe](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-RowFormats&SerDe) (Hive 0.12 and later in [hcatalog-core](https://github.com/apache/hive/blob/master/hcatalog/core/src/main/java/org/apache/hive/hcatalog/data/JsonSerDe.java))

> Note: For Hive releases prior to 0.12, Amazon provides a JSON SerDe available at s3://elasticmapreduce/samples/hive-ads/libs/jsonserde.jar.

注意：对于 0.12 之前的 Hive 版本，Amazon 提供了一个可以的 JSON SerDe，在 `s3://elasticmapreduce/samples/hive-ads/libs/jsonserde.jar`

#### 1.1.2、Custom SerDes

> For information about custom SerDes, see [How to Write Your Own SerDe](https://cwiki.apache.org/confluence/display/Hive/DeveloperGuide#DeveloperGuide-HowtoWriteYourOwnSerDe) in the Developer Guide.

自定义 SerDes 的信息见 Developer Guide 中的 How to Write Your Own SerDe。

### 1.2、HiveQL for SerDes

> For the HiveQL statements that specify SerDes and their properties, see [Create Table](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTable) (particularly [Row Formats & SerDe](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-RowFormats&SerDe)) and [Alter Table](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AlterTable) ([Add SerDe Properties](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AddSerDeProperties)).

对于指定 SerDes 及其属性的 HiveQL 语句，请参见 Create Table(特别是Row Formats & SerDe)和 Alter Table(Add SerDe Properties)。

## 2、Input Processing

> Hive's execution engine (referred to as just engine henceforth) first uses the configured InputFormat to read in a record of data (the value object returned by the RecordReader of the InputFormat).

- Hive 的执行引擎(以后仅称为引擎)首先使用配置的 InputFormat 读取数据记录(由 InputFormat 的 RecordReader 返回的值对象)。

> The engine then invokes Serde.deserialize() to perform deserialization of the record. There is no real binding that the deserialized object returned by this method indeed be a fully deserialized one. For instance, in Hive there is a LazyStruct object which is used by the LazySimpleSerDe to represent the deserialized object. This object does not have the bytes deserialized up front but does at the point of access of a field.

- 然后，引擎调用 Serde.deserialize() 来执行记录的反序列化。这个方法返回的反序列化对象并不是完全的反序列化的对象。例如，在 Hive 中，有一个 LazyStruct 对象，它被 LazySimpleSerDe 用来表示反序列化的对象。此对象不预先反序列化字节，但在访问字段时反序列化。

> The engine also gets hold of the ObjectInspector to use by invoking Serde.getObjectInspector(). This has to be a subclass of structObjectInspector since a record representing a row of input data is essentially a struct type.

- 引擎还通过调用 Serde.getObjectInspector() 来获取要使用的 ObjectInspector。这必须是 structObjectInspector 的子类，因为表示一行输入数据的记录本质上是一个 struct 类型。

> The engine passes the deserialized object and the object inspector to all operators for their use in order to get the needed data from the record. The object inspector knows how to construct individual fields out of a deserialized record. For example, StructObjectInspector has a method called getStructFieldData() which returns a certain field in the record. This is the mechanism to access individual fields. For instance ExprNodeColumnEvaluator class which can extract a column from the input row uses this mechanism to get the real column object from the serialized row object. This real column object in turn can be a complex type (like a struct). To access sub fields in such complex typed objects, an operator would use the object inspector associated with that field (The top level StructObjectInspector for the row maintains a list of field level object inspectors which can be used to interpret individual fields).

- 引擎将反序列化对象和对象检查器传递给所有操作符以供它们使用，以便从记录中获得所需的数据。对象检查器知道如何从反序列化的记录中构造单个字段。例如，StructObjectInspector 有一个名为 getStructFieldData() 的方法，它返回记录中的某个字段。这是访问单个字段的机制。例如，ExprNodeColumnEvaluator 类可以从输入行提取列，它使用这种机制从序列化的行对象中获取实际的列对象。这个实际的列对象可以是一个复杂类型(如struct)。要访问这个复杂类型对象中的子字段，操作符将使用与该字段关联的对象检查器(行的顶级 StructObjectInspector 维护字段级的对象检查器列表，可用于解释单个字段)。

> For UDFs the new GenericUDF abstract class provides the ObjectInspector associated with the UDF arguments in the initialize() method. So the engine first initializes the UDF by calling this method. The UDF can then use these ObjectInspectors to interpret complex arguments (for simple arguments, the object handed to the udf is already the right primitive object like LongWritable/IntWritable etc).

对于 UDFs，新的 GenericUDF 抽象类提供了与 initialize() 方法中的 UDF 参数相关联的 ObjectInspector。

因此，引擎首先通过调用这个方法来初始化 UDF。

然后，UDF 可以使用这些 ObjectInspectors 来解释复杂的参数。

(对于简单的参数，传递给 UDF 的对象已经是正确的原始对象，如 LongWritable/IntWritable 等)。

## 3、Output Processing

> Output is analogous to input. The engine passes the deserialized Object representing a record and the corresponding ObjectInspector to Serde.serialize(). In this context serialization means converting the record object to an object of the type expected by the OutputFormat which will be used to perform the write. To perform this conversion, the serialize() method can make use of the passed ObjectInspector to get the individual fields in the record in order to convert the record to the appropriate type.

输出类似于输入。

引擎将表示一条记录的反序列化对象和相对应的 ObjectInspector 传递给 Serde.serialize()。

在此上下文中，序列化意味着将记录对象转换为 OutputFormat 所期望的类型的对象，该类型的对象将用于执行写操作。

为了执行此转换，serialize() 方法可以使用传递的 ObjectInspector 来获取记录中的各个字段，以便将记录转换为适当的类型。

## 4、Additional Notes

> The owner of an object (either a row, a column, a sub field of a column, or the return value of a UDF) is the code that creates it, and the life time of an object expires when the corresponding object for the next row is created. That means several things:

- 一个对象(行、列、列的子字段或 UDF 的返回值)的所有者是创建它的代码，当创建下一行的对象时，对象的生命期就到期了。这意味着几件事:

	- 我们不应该直接缓存任何对象。在 group-by 和 join 中，我们复制对象，然后将其放入 HashMap 中。

	- SerDe、UDF 等可以为不同行的同一列重用同一个对象。这意味着我们可以消除数据管道中的大多数对象创建，这将极大地提高性能。

> We should not directly cache any object. In both group-by and join, we copy the object and then put it into a HashMap.

> SerDe, UDF, etc can reuse the same object for the same column in different rows. That means we can get rid of most of the object creations in the data pipeline, which is a huge performance boost.

> Settable ObjectInspectors (for write and object creation).

- Settable ObjectInspectors(用于写入和对象创建)。

	- ObjectInspector 允许我们“获取”字段，但不允许“设置”字段或“创建”对象。

	- 可设置的 ObjectInspectors 允许这样做。

	- 在 Settable ObjectInspectors 的帮助下，我们可以很容易地将使用 JavaIntObjectInspector 的对象转换为使用 WritableIntObjectInspector 的对象(也就是从整数转换为 IntWritable)。

	- 在 UDFs(非 GenericUDFs)中，我们使用 Java 反射来获取函数的参数/返回值的类型(如 UDFOPPlus 中的 IntWritable)，然后使用 ObjectInspectorUtils.getStandardObjectInspectors 来推断 ObjectInspector。

	- 给定传递给 UDF 的对象的 ObjectInspector，以及 UDF 参数类型的 ObjectInspector，我们将构造 ObjectInspectorConverter，它使用 SettableObjectInspector 接口来转换对象。转换器在 GenericUDF 和 GenericUDAF 中被调用。

> ObjectInspector allows us to "get" fields, but not "set" fields or "create" objects.

> Settable ObjectInspectors allows that.

> We can convert an object with JavaIntObjectInspector to an object with WritableIntObjectInspector (which is, from Integer to IntWritable) easily with the help of Settable ObjectInspectors.

> In UDFs (non-GenericUDFs), we use Java reflection to get the type of the parameters/return values of a function (like IntWritable in case of UDFOPPlus), and then infer the ObjectInspector for that using ObjectInspectorUtils.getStandardObjectInspectors.

> Given the ObjectInspector of an Object that is passed to a UDF, and the ObjectInspector of the type of the parameter of the UDF, we will construct a ObjectInspectorConverter, which uses the SettableObjectInspector interface to convert the object. The converters are called in GenericUDF and GenericUDAF.

> In short, Hive will automatically convert objects so that Integer will be converted to IntWritable (and vice versa) if needed. This allows people without Hadoop knowledge to use Java primitive classes (Integer, etc), while hadoop users/experts can use IntWritable which is more efficient.

简而言之，Hive 会自动转换对象，如果需要，Integer 会被转换为 IntWritable(反之亦然)。

这让没有Hadoop知识的人可以使用 Java 基本类(Integer等)，而 Hadoop 用户/专家可以使用更高效的 IntWritable。

> Between map and reduce, Hive uses LazyBinarySerDe and BinarySortableSerDe 's serialize methods. SerDe can serialize an object that is created by another serde, using ObjectInspector.

在 map 和 reduce 间，Hive 使用 LazyBinarySerDe 和 BinarySortableSerDe 的序列化方法。SerDe 可以使用 ObjectInspector 序列化由另一个 SerDe 创建的对象。