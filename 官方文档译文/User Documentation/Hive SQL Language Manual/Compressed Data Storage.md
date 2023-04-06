# CompressedStorage

[TOC]

## 1、Compressed Data Storage

> Keeping data compressed in Hive tables has, in some cases, been known to give better performance than uncompressed storage; both in terms of disk usage and query performance.

在某些情况下，在 Hive 表中报错压缩的数据要比未压缩的数据具有更高的性能：磁盘使用和查询性能。

> You can import text files compressed with Gzip or Bzip2 directly into a table stored as TextFile. The compression will be detected automatically and the file will be decompressed on-the-fly during query execution. For example:

你可以将使用 `Gzip` 或 `Bzip2` 压缩的文本文件直接导入使用 `TextFile` 格式存储的表中。将会自动检测到压缩，文件将动态地在查询执行期间被解压。

```sql
CREATE TABLE raw (line STRING)
   ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' LINES TERMINATED BY '\n';
 
LOAD DATA LOCAL INPATH '/tmp/weblogs/20090603-access.log.gz' INTO TABLE raw;
```

> The table 'raw' is stored as a TextFile, which is the default storage. However, in this case Hadoop will not be able to split your file into chunks/blocks and run multiple maps in parallel. This can cause underutilization of your cluster's 'mapping' power.

表 raw 存储为 TextFile，这是默认的存储。然而，在这种情况下，Hadoop 不能划分你的文件成块，并行地运行多个 maps。这可能导致集群的“映射”能力未得到充分利用。

> The recommended practice is to insert data into another table, which is stored as a SequenceFile. A SequenceFile can be split by Hadoop and distributed across map jobs whereas a GZIP file cannot be. For example:

推荐的方法是，将数据插入到存储为 SequenceFile 的另一个表。 SequenceFile 可以被 Hadoop 划分，在 map jobs 间分发，而 GZIP 文件不可以。

```sql
CREATE TABLE raw (line STRING)
   ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t' LINES TERMINATED BY '\n';
 
CREATE TABLE raw_sequence (line STRING)
   STORED AS SEQUENCEFILE;
 
LOAD DATA LOCAL INPATH '/tmp/weblogs/20090603-access.log.gz' INTO TABLE raw;
 
SET hive.exec.compress.output=true;
SET io.seqfile.compression.type=BLOCK; -- NONE/RECORD/BLOCK (see below)
INSERT OVERWRITE TABLE raw_sequence SELECT * FROM raw;
```

> The value for io.seqfile.compression.type determines how the compression is performed. Record compresses each value individually while BLOCK buffers up 1MB (default) before doing compression.

`io.seqfile.compression.type` 的值决定了如何执行压缩。在压缩前，当 BLOCK 缓存到 1MB(默认) 时，记录会单独压缩每个值。【？？？】

### 1.1、LZO Compression

> See [LZO Compression](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+LZO) for information about using LZO with Hive.