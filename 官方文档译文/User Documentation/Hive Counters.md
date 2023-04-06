# Hive Counters

> Task counters created by Hive during query execution

在查询执行期间，Hive 创建任务计数器。

> For Tez execution, %context is set to the mapper/reducer name. For other execution engines it is not included in the counter name.

对于 Tez 执行，`%context` 设置为 mapper/reducer 名字。对于其他的执行引擎，它不包含在计数器名字里。

Counter Name | Description
---|:---
RECORDS_IN[_%context]  | Input records read
RECORDS_OUT[_%context] | Output records written
RECORDS_OUT_INTERMEDIATE[_%context]  | Records written as intermediate records to ReduceSink (which become input records to other tasks)
CREATED_FILES          | Number of files created
DESERIALIZE_ERRORS     | Deserialization errors encountered while reading data