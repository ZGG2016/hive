# FileFormats

[TOC]

## 1、File Formats and Compression

### 1.1、File Formats

> Hive supports several file formats:

Hive 支持如下的文件格式：

- Text File
- SequenceFile
- [RCFile](https://cwiki.apache.org/confluence/display/Hive/RCFile)
- [Avro Files](https://cwiki.apache.org/confluence/display/Hive/AvroSerDe)
- [ORC Files](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+ORC)
- [Parquet](https://cwiki.apache.org/confluence/display/Hive/Parquet)
- Custom INPUTFORMAT and OUTPUTFORMAT

> The [hive.default.fileformat](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.default.fileformat) configuration parameter determines the format to use if it is not specified in a [CREATE TABLE](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-CreateTable) or [ALTER TABLE](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-AlterEitherTableorPartition) statement.  Text file is the parameter's default value.

hive.default.fileformat 配置参数决定了使用的格式，如果没有在 CREATE TABLE 或 ALTER TABLE 指定。文本文件是参数的默认值。

> For more information, see the sections [Storage Formats](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-StorageFormats) and [Row Formats & SerDe](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-RowFormats&SerDe) on the DDL page.

更多信息见 DDL 页面的 Storage Formats 和 Row Formats & SerDe。

### 1.2、File Compression

- [Compressed Data Storage](https://cwiki.apache.org/confluence/display/Hive/CompressedStorage)
- [LZO Compression](https://cwiki.apache.org/confluence/display/Hive/CompressedStorage)


