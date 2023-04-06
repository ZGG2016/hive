# Binary DataType Proposal

[TOC]

## 1、Binary Type in Hive

### 1.1、Motivation:

> Hive is designed to work with big data. Often in such cases, a row in a data might be very wide with hundreds of columns. Sometimes, user is just interested in few of those columns and doesn't want to bother about exact type information for rest of columns. In such cases, he may just declare the types of those columns as binary and Hive will not try to interpret those columns. One important thing to note is this binary type is not modeled after blob type as it exists in other systems.

Hive 是为处理大数据而设计的。通常在这种情况下，数据中的一行可能非常宽，有数百列。

有时，用户只对其中的一些列感兴趣，而不想为其他列的确切类型信息操心。在这种情况下，他可能只声明这些列的类型为二进制，而 Hive 不会尝试解释这些列。

需要注意的一件重要的事情是，这个二进制类型不是按照 blob 类型建模的，因为它存在于其他系统中。

### 1.2、Syntax:

	create table binary_table (a string, b binary);

### 1.3、How is 'binary' represented internally in Hive 

> Binary type in Hive will map to 'binary' data type in thrift. Primitive java object for 'binary' type is ByteArrayRef. PrimitiveWritableObject for 'binary' type is BytesWritable

Hive 中的 Binary 类型将映射到 thrift 中的 'binary' 数据类型。

'binary' 类型的基本 java 对象是 ByteArrayRef

'binary' 类型的 PrimitiveWritableObject 是 BytesWritable

### 1.4、Casting:

> Binary type will not be coerced into any other type implicitly. Even explicit casting will not be supported. 

Binary 类型不会隐式地强制转换为任何其他类型。甚至显式类型转换也不支持。

### 1.5、Serialization:

> String in Hive is serialized by first extracting the underlying bytes of string and then serializing them. Binary type will just piggyback on it and will reuse the same code.

Hive 中的 String 是通过首先提取字符串的底层字节，然后序列化它们来序列化的。

Binary 类型将只是在它的基础上，并重用相同的代码。

### 1.6、Transform Scripts:

> As with other types, binary data will be sent to transform script in String form. byte[] will be first encoded in Base64 format and then a String will be created and sent to the script.   

与其他类型一样，二进制数据将以 String 形式发送到转换脚本。

byte[] 将首先以 Base64 格式编码，然后将创建一个 String，并发送到脚本。

### 1.7、Supported Serde:

- ColumnarSerde
- BinarySortableSerde
- LazyBinaryColumnarSerde  
- LazyBinarySerde
- LazySimpleSerde

> Group-by and unions will be supported on columns with 'binary' type

将在使用 binary 类型的列上支持 group-by 和 unions。

### 1.8、JIRA:

[https://issues.apache.org/jira/browse/HIVE-2380](https://issues.apache.org/jira/browse/HIVE-2380)