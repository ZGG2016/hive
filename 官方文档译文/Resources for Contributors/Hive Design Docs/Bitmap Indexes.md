# IndexDev Bitmap

[TOC]

## 1、Introduction

> This document explains the proposed design for adding a bitmap index handler ([https://issues.apache.org/jira/browse/HIVE-1803](https://issues.apache.org/jira/browse/HIVE-1803)).

本文档解释了添加位图索引处理程序的建议设计。

> Bitmap indexing ([http://en.wikipedia.org/wiki/Bitmap_index](http://en.wikipedia.org/wiki/Bitmap_index)) is a standard technique for indexing columns with few distinct values, such as gender.

位图索引是一种用于索引具有很少的不同值的列，如性别，的一种标准技术。

## 2、Approach

> We want to develop a bitmap index that can reuse as much of the existing Compact Index code as possible.

我们想要开发一个位图索引，可以重用尽可能多的现有的 Compact Index 代码。

## 3、Proposal

### 3.1、First implementation

> This implementation confers some of the benefits of bitmap indexing and should be easy to implement given the already existing compact index, but it does few of the optimizations such as compression that a really good bitmap index should do.

这种实现提供了位图索引的一些好处，并且在现有的紧凑索引下应该很容易实现，

但是它没有做一些真正好的位图索引应该做的优化，比如压缩。

> Like the complex index, this implementation uses an index table. The index table on a column "key" has four or more columns: first, the columns that are being indexed, then `_bucketname`, `_offset`, and `_bitmaps. _bucketname` is a string pointing to the hadoop file that is storing this block in the table,`_offset` is the block offset of a block, and `_bitmaps` is an uncompressed bitmap encoding (an Array of bytes) of the bitmap for this column value, bucketname, and row offset. Each bit in the bitmap corresponds to one row in the block. The bit is 1 if that row has the value of the values in the columns being indexed, and a 0 if not. If a key value does not appear in a block at all, the value is not stored in the map.

就像复杂索引，该实现使用索引表。

在一个列 “key” 上的索引表有四个或更多的列：首先是被索引的列，然后是 `_bucketname`，`_offset`。

`_bitmaps. _bucketname` 是一个指向存储表中块的 hadoop 文件的字符串，`_offset` 是一个块的块偏移量，`_bitmaps` 是该列值、桶名和行偏移量的未压缩的位图的位图编码(字节数组)。

位图中的每个位对应于块中的一行。如果该行有被索引的列的值，则该位为1，如果没有，则为0。

如果一个键值根本没有出现在块中，那么这个值就不会存储在图中。

> When querying this index, if there are boolean AND or OR operations done on the predicates with bitmap indexes, we can use bitwise operations to try to eliminate blocks as well. We can then eliminate blocks that do not contain the value combinations we are interested in. We can use this data to generate the filename, array of block offsets format that the compact index handler uses and reuse that in the bitmap index query.

当查询这个索引时，如果在具有位图索引的谓词上有布尔 AND 或 OR 操作，我们也可以使用位操作来消除块。

然后，我们可以删除不包含我们感兴趣的值组合的块。我们可以使用这些数据来生成文件名、块偏移格式数组，紧凑索引处理程序在位图索引查询中使用并重用这些格式。

### 3.2、Second iteration

> The basic implementation's only compression is eliminating blocks where all rows are 0s. This is unlikely to happen for larger blocks, so we need a better compression format. What we can do is do byte-aligned bitmap compression, where the bitmap is an array of bytes, and a byte of all 1s or all 0s implies one or more bytes where every value is 0 or 1. Then, we would just need to add another column in the bitmap index table that is an array of Ints that describe how long the gaps are and logic to expand the compression.

基本实现的惟一压缩是消除所有行都为 0 的块。

这种情况不太可能发生在更大的块上，因此我们需要更好的压缩格式。

我们可以做的是字节对齐的位图压缩，位图是一个字节数组，所有 1 或所有 0 的字节意味着一个或多个字节，其中每个值都是0或1。

然后，我们只需要在位图索引表中添加另一列，该列是一个整型数组，用于描述间隙的长度和扩展压缩的逻辑。

## 4、Example

> Suppose we have a bitmap index on a key where, on the first block, value "a" appears in rows 5, 12, and 64, and value "b" appears in rows 7, 8, and 9. Then, for the preliminary implementation, the first entry in the index table will be:

假设我们在一个键上有一个位图索引，在第一个块上，值 “a” 出现在第 5、12 和 64 行，值 “b” 出现在第 7、8 和 9 行。那么，对于初步实现，索引表的第一个条目将是:

![https://issues.apache.org/jira/secure/attachment/12460083/bitmap_index_1.png](https://issues.apache.org/jira/secure/attachment/12460083/bitmap_index_1.png)

[https://issues.apache.org/jira/secure/attachment/12460083/bitmap_index_1.png](https://issues.apache.org/jira/secure/attachment/12460083/bitmap_index_1.png)

> The values in the array represent the bitmap for each block, where each 32-bit BigInt value stores 32 rows.

数组中的值表示每个块的位图，其中每个 32 位的 BigInt 值存储 32 行。

> For the second iteration, the first entry will be:

对于第二次迭代，第一个条目将是:

![https://issues.apache.org/jira/secure/attachment/12460124/bitmap_index_2.png](https://issues.apache.org/jira/secure/attachment/12460124/bitmap_index_2.png)

[https://issues.apache.org/jira/secure/attachment/12460124/bitmap_index_2.png](https://issues.apache.org/jira/secure/attachment/12460124/bitmap_index_2.png)

> This one uses 1-byte array entries, so each value in the array stores 8 rows. If an entry is 0x00 or 0xFF, it represents 1 or more consecutive bytes of zeros, (in this case 5 and 4, respectively)

它使用 1 字节数组项，因此数组中的每个值存储 8 行。

如果一个项是 0x00 或 0xFF，它表示 1 个或多个连续的 0 字节(在本例中分别是5和4)