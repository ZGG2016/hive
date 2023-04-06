# LanguageManual VirtualColumns

[TOC]

## 1、Virtual Columns

> Hive 0.8.0 provides support for two virtual columns:

Hive 0.8.0 提供了对两个虚拟列的支持：

- INPUT__FILE__NAME：一个 mapper 任务的输入文件的名字。

- BLOCK__OFFSET__INSIDE__FILE：当前全局文件的位置。对于块压缩文件，它是当前块的文件偏移量，即当前块的第一个字节的文件偏移量。

> One is INPUT__FILE__NAME, which is the input file's name for a mapper task.

> the other is BLOCK__OFFSET__INSIDE__FILE, which is the current global file position.

> For block compressed file, it is the current block's file offset, which is the current block's first byte's file offset.

> Since Hive 0.8.0 the following virtual columns have been added:

从 Hive 0.8.0 开始，添加了如下的虚拟列：

- ROW__OFFSET__INSIDE__BLOCK

- RAW__DATA__SIZE

- ROW__ID

- GROUPING__ID

> It is important to note, that all of the virtual columns listed here cannot be used for any other purpose (i.e. table creation with columns having a virtual column will fail with "SemanticException Error 10328: Invalid column name..")

重要的是，要注意：这里列出的所有虚拟列不能用于任何其他目的

(例如，创建具有虚拟列的表将失败，产生"SemanticException Error 10328: Invalid column name..")

### 1.1、Simple Examples

```sql
select INPUT__FILE__NAME, key, BLOCK__OFFSET__INSIDE__FILE from src;

select key, count(INPUT__FILE__NAME) from src group by key order by key;

select * from src where BLOCK__OFFSET__INSIDE__FILE > 12000 order by key;
```


[https://github.com/ZGG2016/knowledgesystem/blob/master/03%20%E5%A4%A7%E6%95%B0%E6%8D%AE/02%20Hive/%E8%99%9A%E6%8B%9F%E5%88%97.md](https://github.com/ZGG2016/knowledgesystem/blob/master/03%20%E5%A4%A7%E6%95%B0%E6%8D%AE/02%20Hive/%E8%99%9A%E6%8B%9F%E5%88%97.md)
