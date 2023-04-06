# Archiving

[TOC]

> Note: Archiving should be considered an advanced command due to the caveats involved.

注意:归档应该作为一个高级命令，因为涉及到一些注意事项。

## 1、Archiving for File Count Reduction

### 1.1、Overview

> Due to the design of HDFS, the number of files in the filesystem directly affects the memory consumption in the namenode. While normally not a problem for small clusters, memory usage may hit the limits of accessible memory on a single machine when there are >50-100 million files. In such situations, it is advantageous to have as few files as possible.

由于 HDFS 的设计，文件系统中的文件数量直接影响了 namenode 中的内存消耗。虽然对于小集群来说这通常不是问题，但是当有 `>50-100 `百万个文件时，内存使用可能会达到单台机器上可访问内存的限制。在这种情况下，拥有尽可能少的文件是有利的。

> The use of [Hadoop Archives](http://hadoop.apache.org/docs/stable1/hadoop_archives.html) is one approach to reducing the number of files in partitions. Hive has built-in support to convert files in existing partitions to a Hadoop Archive (HAR) so that a partition that may once have consisted of 100's of files can occupy just `~3` files (depending on settings). However, the trade-off is that queries may be slower due to the additional overhead in reading from the HAR.

Hadoop Archives 的使用是减少分区中文件数量的一种方法。Hive 内部支持将已存在分区中的文件转换到 Hadoop Archive (HAR)，所有，如果一个分区有 100 个文件，那么转换后，仅占用 3 个（取决于配置）。
然而，代价是查询速读变慢了，因为从 HAR 读取会有额外的负载。

> Note that archiving does NOT compress the files – HAR is analogous to the Unix tar command.

注意：**归档不会压缩文件**，HAR 类似于 Unix 的 tar 命令。

### 1.2、Settings

> There are 3 settings that should be configured before archiving is used. (Example values are shown.)

在使用归档前，先配置 3 个配置：

	hive> set hive.archive.enabled=true;
	hive> set hive.archive.har.parentdir.settable=true;
	hive> set har.partfile.size=1099511627776;

> hive.archive.enabled controls whether archiving operations are enabled.

**`hive.archive.enabled` 控制着是否启用归档操作**。

> hive.archive.har.parentdir.settable informs Hive whether the parent directory can be set while creating the archive. In recent versions of Hadoop the -p option can specify the root directory of the archive. For example, if /dir1/dir2/file is archived with /dir1 as the parent directory, then the resulting archive file will contain the directory structure dir2/file. In older versions of Hadoop (prior to 2011), this option was not available and therefore Hive must be configured to accommodate this limitation.

**`hive.archive.har.parentdir.settable` 通知 Hive 在创建归档时，是否设置父目录**。在最近的 Hadoop 版本中，`-p` 选项可以指定归档的根目录。例如，如果归档 `/dir1/dir2/file`，`/dir1` 是父目录，那么结果的归档文件将包含 `dir2/file` 目录结构。在旧的 Hadoop 版本中（2011之前），这个选项是不可用的，因此 Hive 必须配置以适应这个限制。

> har.partfile.size controls the size of the files that make up the archive. The archive will contain size_of_partition/har.partfile.size files, rounded up. Higher values mean fewer files, but will result in longer archiving times due to the reduced number of mappers.

**`har.partfile.size` 控制着组成归档的文件大小**。归档将包含 `size_of_partition/har.partfile.size` 文件，取整。更高的值意味着更少的文件，但会导致更长的归档时间，由于减少了 mappers 的数量。

### 1.3、Usage

#### 1.3.1、Archive

> Once the configuration values are set, a partition can be archived with the command:

一旦设置了配置，可以使用如下命令归档一个分区：

```sql
ALTER TABLE table_name ARCHIVE PARTITION (partition_col = partition_col_value, partition_col = partiton_col_value, ...)
```

例如：

```sql
ALTER TABLE srcpart ARCHIVE PARTITION(ds='2008-04-08', hr='12')
```

> Once the command is issued, a mapreduce job will perform the archiving. Unlike Hive queries, there is no output on the CLI to indicate process.

一旦执行了命令，将执行一个 mapreduce job 来执行归档操作。不同于 Hive 查询，不会在 CLI 显示输出进度。

#### 1.3.2、Unarchive

> The partition can be reverted back to its original files with the unarchive command:

被归档的分区恢复到原始的文件：

```sql
ALTER TABLE srcpart UNARCHIVE PARTITION(ds='2008-04-08', hr='12')
```
### 1.4、Cautions and Limitations

> In some older versions of Hadoop, HAR had a few bugs that could cause data loss or other errors. Be sure that these patches are integrated into your version of Hadoop:

- 在一些旧的 Hadoop 版本，HAR 存在一些 Bugs，回导致数据丢失或其他错误。确保这些 patches 集成到了你的 Hadoop 版本：

	- https://issues.apache.org/jira/browse/HADOOP-6591 (fixed in Hadoop 0.21.0)

	- https://issues.apache.org/jira/browse/MAPREDUCE-1548 (fixed in Hadoop 0.22.0)

	- https://issues.apache.org/jira/browse/MAPREDUCE-2143 (fixed in Hadoop 0.22.0)

	- https://issues.apache.org/jira/browse/MAPREDUCE-1752 (fixed in Hadoop 0.23.0)

> The HarFileSystem class still has a bug that has yet to be fixed:

- HarFileSystem 类仍有一个 bug 未解决：

	- https://issues.apache.org/jira/browse/MAPREDUCE-1877 (moved to https://issues.apache.org/jira/browse/HADOOP-10906 in 2014)

> Hive comes with the HiveHarFileSystem class that addresses some of these issues, and is by default the value for fs.har.impl. Keep this in mind if you're rolling your own version of HarFileSystem:

- Hive 附带了 HiveHarFileSystem 类来解决这些问题，并且默认是 `fs.har.impl` 的值。如果你正在运行自己的 HarFileSystem 版本，请记住这一点:

	- 默认的 `HiveHarFileSystem.getFileBlockLocations()` 没有 locality。这意味着它可能会带来更高的网络负载或降低性能。

	- 不能用 `INSERT OVERWRITE` 覆盖归档的分区。必须首先对分区进行 unarchive。

	- 如果两个进程试图同时归档同一个分区，可能会发生不好的事情。(需要实现并发支持。)

> The default HiveHarFileSystem.getFileBlockLocations() has **no locality**. That means it may introduce higher network loads or reduced performance.

> Archived partitions cannot be overwritten with INSERT OVERWRITE. The partition must be unarchived first.

> If two processes attempt to archive the same partition at the same time, bad things could happen. (Need to implement concurrency support.)

### 1.5、Under the Hood

> Internally, when a partition is archived, a HAR is created using the files from the partition's original location (such as /warehouse/table/ds=1). The parent directory of the partition is specified to be the same as the original location and the resulting archive is named 'data.har'. The archive is moved under the original directory (such as /warehouse/table/ds=1/data.har), and the partition's location is changed to point to the archive.

在内部，当一个分区被归档时，使用分区的原始位置(例如`/warehouse/table/ds=1`)的文件创建一个 HAR。分区的父目录被指定为与原始位置相同，生成的归档文件名为 `data.har`。归档文件被移动到原始目录下(例如`/warehouse/table/ds=1/data.har`)，并且分区的位置被更改为指向归档文件。