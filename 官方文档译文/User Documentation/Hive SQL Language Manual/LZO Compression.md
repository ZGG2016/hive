# LZO Compression

[TOC]

## 1、General LZO Concepts

> LZO is a lossless data compression library that favors speed over compression ratio. See http://www.oberhumer.com/opensource/lzo and http://www.lzop.org for general information about LZO and see [Compressed Data Storage](https://cwiki.apache.org/confluence/display/Hive/CompressedStorage) for information about compression in Hive.

LZO 是一种无损数据压缩库，更注重速度而不是压缩比。

> Imagine a simple data file that has three columns

包含 3 列的样例数据文件：

	id
	first name
	last name

> Let's populate a data file containing 4 records:

添加数据：

	19630001     john          lennon
	19630002     paul          mccartney
	19630003     george        harrison
	19630004     ringo         starr

> Let's call the data file `/path/to/dir/names.txt`.

数据文件路径为 `/path/to/dir/names.txt`

> In order to make it into an LZO file, we can use the lzop utility and it will create a names.txt.lzo file.

为了压缩成 LZO 文件，使用 lzop 压缩成 names.txt.lzo 文件。

> Now copy the file names.txt.lzo to HDFS.

将 names.txt.lzo 复制到 HDFS。

## 2、Prerequisites

### 2.1、Lzo/Lzop Installations

> lzo and lzop need to be installed on every node in the Hadoop cluster. The details of these installations are beyond the scope of this document.

需要在 Hadoop 集群的每个节点上安装 lzo 和 lzop。

### 2.2、core-site.xml

> Add the following to your core-site.xml:

在 core-site.xml 中添加如下内容：

	com.hadoop.compression.lzo.LzoCodec
	com.hadoop.compression.lzo.LzopCodec

> For example:

```xml
<property>
	<name>io.compression.codecs</name>
	<value>org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.BZip2Codec,com.hadoop.compression.lzo.LzoCodec,com.hadoop.compression.lzo.LzopCodec</value>
</property>

<property>
	<name>io.compression.codec.lzo.class</name>
	<value>com.hadoop.compression.lzo.LzoCodec</value>
</property>
```

> Next we run the command to create an LZO index file:

运行命令创建 LZO 索引文件：

	hadoop jar /path/to/jar/hadoop-lzo-cdh4-0.4.15-gplextras.jar com.hadoop.compression.lzo.LzoIndexer  /path/to/HDFS/dir/containing/lzo/files

> This creates names.txt.lzo on HDFS.

这在 HDFS 上创建 names.txt.lzo

### 2.3、Table Definition

> The following hive -e command creates an LZO-compressed external table:

下面的 `hive -e` 命令创建一个 LZO 压缩的外部表

```sql
hive -e "CREATE EXTERNAL TABLE IF NOT EXISTS hive_table_name (column_1  datatype_1......column_N datatype_N)
         PARTITIONED BY (partition_col_1 datatype_1 ....col_P  datatype_P)
         ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
         STORED AS INPUTFORMAT  \"com.hadoop.mapred.DeprecatedLzoTextInputFormat\"
                   OUTPUTFORMAT \"org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat\";
```

> Note: The double quotes have to be escaped so that the 'hive -e' command works correctly.

注意：双引号必须被转义，这样 `hive -e` 命令才能正常工作。

> See [CREATE TABLE](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTable) and [Hive CLI](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Cli) for information about command syntax.

## 3、Hive Queries

### 3.1、Option 1: Directly Create LZO Files

> Directly create LZO files as the output of the Hive query.

1.直接创建 LZO 文件作为 Hive 查询的输出。

> Use lzop command utility or your custom Java to generate .lzo.index for the .lzo files.

2.使用 lzop 命令或自定义的 Java 来生成 `.lzo` 文件的 `.lzo.index`

**Hive Query Parameters**

	SET mapreduce.output.fileoutputformat.compress.codec=com.hadoop.compression.lzo.LzoCodec
	SET hive.exec.compress.output=true
	SET mapreduce.output.fileoutputformat.compress=true

> For example:

	hive -e "SET mapreduce.output.fileoutputformat.compress.codec=com.hadoop.compression.lzo.LzoCodec; SET hive.exec.compress.output=true;SET mapreduce.output.fileoutputformat.compress=true; <query-string>"
    
> Note: If the data sets are large or number of output files are large , then this option does not work.

注意：如果数据集很大或者输出文件的数量很大，那么这个选项不起作用。

### 3.2、Option 2: Write Custom Java to Create LZO Files

> Create text files as the output of the Hive query.
> Write custom Java code to
> convert Hive query generated text files to .lzo files
> generate .lzo.index files for the .lzo files generated above

- 1.创建文本文件作为 Hive 查询的输出。

- 2.编写自定义 Java 代码

	- a.转换 Hive 查询生成的文本文件为 `.lzo` 文件

	- b.为上面生成的 `.lzo` 文件建立 `.lzo.index `

**Hive Query Parameters**

> Prefix the query string with these parameters:

	SET hive.exec.compress.output=false
	SET mapreduce.output.fileoutputformat.compress=false

> For example:

	hive -e "SET hive.exec.compress.output=false;SET mapreduce.output.fileoutputformat.compress=false;<query-string>"