# CSV SerDe

[TOC]

## 1、Availability

> Earliest version CSVSerde is available. The CSVSerde is available in [Hive 0.14](https://issues.apache.org/jira/browse/HIVE-7777) and greater.

CSVSerde 在 Hive 0.14 及更高版本可用。

## 2、Background

> The CSV SerDe is based on https://github.com/ogrodnek/csv-serde, and was added to the Hive distribution in HIVE-7777.

> Limitation. This SerDe treats all columns to be of type String. Even if you create a table with non-string column types using this SerDe, the DESCRIBE TABLE output would show string column type. The type information is retrieved from the SerDe. To convert columns to the desired type in a table, you can create a view over the table that does the CAST to the desired type.

限制。此 SerDe 将所有列视为字符串类型。

即使你使用这个 SerDe 创建一个具有非字符串列类型的表，`DESCRIBE TABLE` 的输出也将显示字符串列类型。

从 SerDe 检索类型信息。要将表中的列转换为所需类型，可以在表上创建一个视图，该视图执行 CAST 为所需类型。

## 3、Usage

> This SerDe works for most CSV data, but does not handle embedded newlines. To use the SerDe, specify the fully qualified class name org.apache.hadoop.hive.serde2.OpenCSVSerde.  

该 SerDe 适用于大多数 CSV 数据，但不处理嵌入的换行。

要使用 SerDe，指定完全限定类名`org.apache.hadoop.hive.serde2.OpenCSVSerde`。

> Documentation is based on original documentation at [https://github.com/ogrodnek/csv-serde](https://github.com/ogrodnek/csv-serde).

> Create table, specify CSV properties

创建表，指定 CSV 属性

```sql
CREATE TABLE my_table(a string, b string, ...)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
WITH SERDEPROPERTIES (
   "separatorChar" = "\t",
   "quoteChar"     = "'",
   "escapeChar"    = "\\"
)  
STORED AS TEXTFILE;
```

> Default separator, quote, and escape characters if unspecified

如果未指定，使用默认的分隔符、引号和转义字符

	DEFAULT_ESCAPE_CHARACTER \
	DEFAULT_QUOTE_CHARACTER  "
	DEFAULT_SEPARATOR        ,

> For general information about SerDes, see [Hive SerDe](https://cwiki.apache.org/confluence/display/Hive/DeveloperGuide#DeveloperGuide-HiveSerDe) in the Developer Guide. Also see [SerDe](https://cwiki.apache.org/confluence/display/Hive/SerDe) for details about input and output processing.

## 4、Versions

> The CSVSerde has been built and tested against Hive 0.14 and later, and uses [Open-CSV 2.3](http://opencsv.sourceforge.net/) which is bundled with the Hive distribution.

Hive Version | Open-CSV Version
---|:---
Hive 0.14 and | later	2.3