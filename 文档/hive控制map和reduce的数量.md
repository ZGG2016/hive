# hive控制map和reduce的数量

- mapreduce.input.fileinputformat.split.maxsize  mapreduce.input.fileinputformat.split.minsize

	map输入的最大/小的分片大小  

	当输入文件很大时，任务逻辑复杂，map 执行非常慢的时候，可以考虑增加 map 数
	
	```
	protected long computeSplitSize(long blockSize, long minSize,
                                   long maxSize) {
      return Math.max(minSize, Math.min(maxSize, blockSize));
    }
    ```

    调整 maxSize ，让 maxSize 最大值低于 blocksize 就可以增加 map 的个数

- hive.input.format

	Default Value:  org.apache.hadoop.hive.ql.io.CombineHiveInputFormat

	The default input format. Set this to `HiveInputFormat` if you encounter problems with `CombineHiveInputFormat`.

	可以在 map 执行前合并小文件，减少 map 数

- hive.merge.mapfiles

	Default Value: true

	Merge small files at the end of a map-only job.

	在 map-only 任务结束时合并小文件

- hive.merge.mapredfiles

	Default Value: false

	Merge small files at the end of a map-reduce job.

	在 map-reduce 任务结束时合并小文件

- hive.merge.size.per.task

	Default Value: 256000000

	Size of merged files at the end of the job.

	在 job 结束时合并的文件大小

- hive.merge.smallfiles.avgsize

	Default Value: 16000000

	When the average output file size of a job is less than this number, Hive will start an additional map-reduce job to merge the output files into bigger files. This is only done for map-only jobs if `hive.merge.mapfiles` is true, and for map-reduce jobs if `hive.merge.mapredfiles` is true.

	当一个 job 的平均输出文件大小小于这个值，hive 将启动一个额外的 mr job，将输出文件合并成一个更大的文件。

	对于 map-only jobs, 如果 `hive.merge.mapfiles` 为 true, 这才能完成。

	对于 map-reduce jobs, 如果 `hive.merge.mapredfiles` 为 true, 这才能完成。

- hive.exec.reducers.bytes.per.reducer

	Default Value: 1,000,000,000 prior to Hive 0.14.0; 256 MB ( 256,000,000 ) in Hive 0.14.0 and later

	Size per reducer. The default in Hive 0.14.0 and earlier is 1 GB, that is, if the input size is 10 GB then 10 reducers will be used. In Hive 0.14.0 and later the default is 256 MB, that is, if the input size is 1 GB then 4 reducers will be used.

	每个 reducer 处理的数据量大小。

	在 Hive 0.14.0 及更早的版本，默认值是 1GB, 如果输入大小是 10GB, 那么就需要使用 10 个 reducer.

	在 Hive 0.14.0 及之后的版本，默认值是 256MB, 如果输入大小是 1GB, 那么就需要使用 4 个 reducer.

- hive.exec.reducers.max

	Default Value: 999 prior to Hive 0.14.0; 1009  in Hive 0.14.0 and later

	Maximum number of reducers that will be used. If the one specified in the configuration property `mapred.reduce.tasks` is negative, Hive will use this as the maximum number of reducers when automatically determining the number of reducers.

	能够使用的 reducers 的最大数量。

	如果将配置属性 `mapred.reduce.tasks` 指定为负数，那么 hive 将使用这个作为能够使用的 reducers 的最大数量。

- mapred.reduce.tasks

	Default Value: -1

	The default number of reduce tasks per job. Typically set to a prime close to the number of available hosts. Ignored when `mapred.job.tracker` is "local". Hadoop set this to 1 by default, whereas Hive uses -1 as its default value. By setting this property to -1, Hive will automatically figure out what should be the number of reducers.

	每个 job 使用的 reduce 任务的默认数量。通常设置为接近可用主机数量的质数。

	当 `mapred.job.tracker` 设为 local 时，忽略它。

	默认情况下，hadoop 将其设置为 1，而 hive 设置为 -1

	hive 将其设置为 -1, 那么就可以自动确定 reducers 的数量。


```sql
create table t4(id int);

insert into t4 values(1);
insert into t4 values(2);
insert into t4 values(3);
insert into t4 values(4);
insert into t4 values(5);
insert into t4 values(6);
insert into t4 values(7);
insert into t4 values(8);

drwxr-xr-x   - root supergroup          0 2024-01-15 12:27 /user/hive/warehouse/t4/.hive-staging_hive_2024-01-15_12-26-07_835_8055213384863131271-2
-rw-r--r--   1 root supergroup          2 2024-01-15 12:27 /user/hive/warehouse/t4/000000_0
-rw-r--r--   1 root supergroup          2 2024-01-15 12:27 /user/hive/warehouse/t4/000000_0_copy_1
-rw-r--r--   1 root supergroup          2 2024-01-15 12:28 /user/hive/warehouse/t4/000000_0_copy_2
-rw-r--r--   1 root supergroup          2 2024-01-15 12:29 /user/hive/warehouse/t4/000000_0_copy_3
-rw-r--r--   1 root supergroup          2 2024-01-15 12:29 /user/hive/warehouse/t4/000000_0_copy_4
-rw-r--r--   1 root supergroup          2 2024-01-15 12:29 /user/hive/warehouse/t4/000000_0_copy_5
-rw-r--r--   1 root supergroup          2 2024-01-15 12:29 /user/hive/warehouse/t4/000000_0_copy_6
-rw-r--r--   1 root supergroup          2 2024-01-15 12:29 /user/hive/warehouse/t4/000000_0_copy_7
-rw-r--r--   1 root supergroup          2 2024-01-15 12:29 /user/hive/warehouse/t4/000000_0_copy_8

0: jdbc:hive2://zgg-server:10000> set hive.input.format;
+----------------------------------------------------+
|                        set                         |
+----------------------------------------------------+
| hive.input.format=org.apache.hadoop.hive.ql.io.CombineHiveInputFormat |
+----------------------------------------------------+

0: jdbc:hive2://zgg-server:10000> select sum(id) from t4;
...
INFO  : Hadoop job information for Stage-1: number of mappers: 1; number of reducers: 1
...


0: jdbc:hive2://zgg-server:10000> set hive.input.format=org.apache.hadoop.hive.ql.io.HiveInputFormat;

0: jdbc:hive2://zgg-server:10000> select sum(id) from t4;
...
INFO  : Hadoop job information for Stage-1: number of mappers: 9; number of reducers: 1
...
```