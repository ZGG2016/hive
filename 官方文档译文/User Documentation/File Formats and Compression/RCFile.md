# RCFile

[TOC]

> RCFile (Record Columnar File) is a data placement structure designed for MapReduce-based data warehouse systems. Hive added the RCFile format in version 0.6.0.

RCFile (Record Columnar File)是一种为基于 mapreduce 的数据仓库系统设计的数据放置结构。Hive 在 0.6.0 版本中增加了 RCFile 格式。

> RCFile stores table data in a flat file consisting of binary key/value pairs. It first partitions rows horizontally into row splits, and then it vertically partitions each row split in a columnar way. RCFile stores the metadata of a row split as the key part of a record, and all the data of a row split as the value part.

RCFile 将表数据存储在一个由二进制键/值对组成的平面文件中。

它首先水平地将行划分为行分片，然后垂直地以柱状方式划分每个行分片。

RCFile 存储行分片的元数据作为记录的 key 部分，行分片的所有数据作为 value 部分。

> RCFile combines the advantages of both row-store and column-store to satisfy the need for fast data loading and query processing, efficient use of storage space, and adaptability to highly dynamic workload patterns.

RCFile 结合了行存储和列存储的优点，以满足快速数据载入和查询处理、高效使用存储空间和适应高度动态工作负载模式的需求。

- 作为行存储，RCFile 保证同一行中的数据位于同一节点中。

- 作为列存储，RCFile 可以利用列级数据压缩，并跳过不必要的列读取。

> As row-store, RCFile guarantees that data in the same row are located in the same node.
> As column-store, RCFile can exploit column-wise data compression and skip unnecessary column reads.

> A shell utility is available for reading RCFile data and metadata: see [RCFileCat](https://cwiki.apache.org/confluence/display/Hive/RCFileCat).

有一个 shell 实用程序可用于读取 RCFile 数据和元数据：参见 RCFileCat。

> For details about the RCFile format, see:

RCFile 格式请参见:

- Javadoc 的 RCFile.java

- 2011 ICDE 会议论文："RCFile: A Fast and Space-efficient Data Placement Structure in MapReduce-based Warehouse Systems"

> Javadoc for [RCFile.java](http://hive.apache.org/javadocs/r1.0.1/api/org/apache/hadoop/hive/ql/io/RCFile.html)

> the 2011 ICDE conference paper ["RCFile: A Fast and Space-efficient Data Placement Structure in MapReduce-based Warehouse Systems"](http://www.cse.ohio-state.edu/hpcs/WWW/HTML/publications/papers/TR-11-4.pdf) by Yongqiang He, Rubao Lee, Yin Huai, Zheng Shao, Namit Jain, Xiaodong Zhang, and Zhiwei Xu. 