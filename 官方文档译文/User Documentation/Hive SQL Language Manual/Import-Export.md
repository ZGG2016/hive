# Import/Export

> Version：The EXPORT and IMPORT commands were added in Hive 0.8.0 (see [HIVE-1918](https://issues.apache.org/jira/browse/HIVE-1918)).Replication extensions to the EXPORT and IMPORT commands were added in Hive 1.2.0 (see [HIVE-7973](https://issues.apache.org/jira/browse/HIVE-7973) and [Hive Replication Development](https://cwiki.apache.org/confluence/display/Hive/HiveReplicationDevelopment)).

版本：

EXPORT 和 IMPORT 命令是在 Hive 0.8.0 中添加的。

对 EXPORT 和 IMPORT 命令的复制扩展在 Hive 1.2.0 中添加。

## 1、Overview

> The EXPORT command exports the data of a table or partition, along with the metadata, into a specified output location. This output location can then be moved over to a different Hadoop or Hive instance and imported from there with the IMPORT command.

**EXPORT 命令将表或分区的数据及其元数据导出到指定的输出位置**。

**然后可以将这个输出位置移动到不同的 Hadoop 或 Hive 实例，并使用 IMPORT 命令从那里导入**。

> When exporting a partitioned table, the original data may be located in different HDFS locations. The ability to export/import a subset of the partition is also supported.

导出分区表时，原始数据可能位于不同的 HDFS 位置。还**支持导出/导入分区的一个子集**。

> Exported metadata is stored in the target directory, and data files are stored in subdirectories.

导出的元数据存放在目标目录中，数据文件存放在子目录中。

> The EXPORT and IMPORT commands work independently of the source and target metastore DBMS used; for example, they can be used between Derby and MySQL databases.

EXPORT 和 IMPORT 命令 work independently of 源和目标 metastore DBMS；例如，它们可以在 Derby 和 MySQL 数据库之间使用。【？？？】

> IMPORT will create target table/partition if it does not exist.  All the table properties/parameters will be that of table that was used in EXPORT to generate the archive.  If target exists, checks are performed that it has appropriate schema, Input/OutputFormat, etc.  If target table exists and is not partitioned, it must be empty.  If target table exists and is partitioned, partitions being imported must not exist in the table.  

如果目标表/分区不存在，IMPORT 将创建它。

所有表属性/参数都是 EXPORT 中用于生成存档的表的属性/参数。

如果目标存在，则检查它是否有适当的模式、输入/输出格式等。如果目标表存在且未分区，则它必须为空。如果目标表存在并已经分区，则表中必须不存在正在导入的分区。

## 2、Export Syntax

```sql
EXPORT TABLE tablename [PARTITION (part_column="value"[, ...])]
  TO 'export_target_path' [ FOR replication('eventid') ]
```

## 3、Import Syntax

```sql
IMPORT [[EXTERNAL] TABLE new_or_original_tablename [PARTITION (part_column="value"[, ...])]]
  FROM 'source_path'
  [LOCATION 'import_target_path']
```

## 4、Replication usage

> The EXPORT and IMPORT commands behave slightly differently when used in the context of replication, and are intended to be used by tools that perform replication between hive warehouses. In most cases, end users will not need to use this additional tag, except when doing a manual bootstrap of a replication-destination warehouse so that an incremental replication tool can take over from that point.

在复制环境中，使用 EXPORT 和 IMPORT 命令时，其行为略有不同，目的是供在 hive 仓库间执行复制的工具使用。

在大多数情况下，终端用户不需要使用这个额外的标记，除非在对复制目标仓库进行手动引导，以便增量复制工具可以从那个点开始接管。

> They make use of a special table property called "repl.last.id" in a table or partition (depending on what object is being replicated) object to make sure that a replication export/import will only update objects if the update is newer than the object it affects. On the export end, it tags the replication export dump with an id that is monotonically increasing on the source warehouse (incrementing each time there is a source warehouse metastore modification). In addition, an export that is tagged as being for replication will not result in an error if it attempts to export an object which does not currently exist. (This is because in the general flow of replication, it is quite possible that by the time an event is acted upon for replication by an external tool, it is possible that the object has been removed, and thus, should not halt the replication pipeline.)

在表或分区(取决于被复制的对象)对象中，使用了一个名为 `repl.last.id` 的特殊表属性，以确保一次复制的导出/导入，只在更新比它所影响的对象更新时，更新对象。

在导出端，它用一个在源仓库上单调递增的 id 标记复制导出转储(在每次源仓库元存储修改时递增)。

此外，如果试图导出当前不存在的对象，标记为用于复制的导出不会产生错误。(这是因为在复制的一般流程中，很有可能当外部工具对复制的事件进行操作时，对象可能已经被删除，因此不应该停止复制管道。)

> On the import side, there is no syntax change, but import is run on an export dump that was generated with the FOR REPLICATION tag, it will check the object it is replicating into if it exists. If that object already exists, it checks the repl.last.id property of that object to determine if what is being imported is newer than the current state of the object in the destination warehouse. If the update is newer, then it replaces the object with the newer information. If the update is older than the object already in place, the update is ignored, and causes no error.

在导入端，没有语法更改。

但是导入是在使用 `FOR REPLICATION` 标记生成的导出转储上运行的，它将检查要复制到的对象(如果它存在的话)。

如果该对象已经存在，它将检查该对象的 `repl.last.id` 属性，以确定要导入的内容是否比目标仓库中对象的当前状态更新。

如果 update 是更新的，那么它将用更新的信息替换对象。如果 update 比已经存在的对象更早，则会忽略更新，并且不会导致错误。

> For those using EXPORT for the first-time manual bootstrapping usecase, users are recommended to use a " FOR replication('bootstrapping') " tag. (Advanced users note : The choice of "bootstrapping" here is arbitrary-ish, and could just as well have been "foo". The real goal is to have a value such that all further incremental replication ids will be greater than this original id. Thus, the integral value of this initial id should be 0, and thus, any string which does not contain numbers is acceptable. Having an initial tag be "123456", however, would be bad, as it could cause further updates which have a repl.last.id < 123456 to not be applied.)

对于那些使用 EXPORT 作为第一次手动引导用例的用户，建议使用 `For replication(‘bootstrapping’)` 标签。

(高级用户注意:这里的 “bootstrapping” 选项是任意的，也可以是“foo”。

真正的目标是有一个值，使所有进一步的增量复制 id 都大于这个原始 id。

因此，这个初始 id 的整数值应该是0，因此，任何不包含数字的字符串都是可以接受的。

然而，初始标记为 “123456” 是不好的，因为它可能导致 `repl.last.id< 123456` 的进一步更新，为了不被应用。)

## 5、Examples

Simple export and import:

```sql
export table department to 'hdfs_exports_location/department';
import from 'hdfs_exports_location/department';
```

Rename table on import:

```sql
export table department to 'hdfs_exports_location/department';
import table imported_dept from 'hdfs_exports_location/department';
```

Export partition and import:

```sql
export table employee partition (emp_country="in", emp_state="ka") to 'hdfs_exports_location/employee';
import from 'hdfs_exports_location/employee';
```

Export table and import partition:

```sql
export table employee to 'hdfs_exports_location/employee';
import table employee partition (emp_country="us", emp_state="tn") from 'hdfs_exports_location/employee';
```

Specify the import location:

```sql
export table department to 'hdfs_exports_location/department';
import table department from 'hdfs_exports_location/department' 
       location 'import_target_location/department';
```

Import as an external table:

```sql
export table department to 'hdfs_exports_location/department';
import external table department from 'hdfs_exports_location/department';
```

----------------------------------------

```sql
hive> select * from zipper_table;
OK
1       lijie   chengdu 20191021        99991231
1       lijie   chongqing       20191020        20191020
2       zhangshan       huoxing 20191021        99991231
2       zhangshan       sz      20191020        20191020
3       lisi    shanghai        20191020        99991231
4       wangwu  lalalala        20191021        99991231
4       wangwu  usa     20191020        20191020
5       xinzeng hehe    20191021        99991231

-- 导出
hive> export table zipper_table to '/in/zipper_table';
OK

-- hdfs上查看
-- EXPORT 命令将表或分区的数据及其元数据导出到指定的输出位置
-- 导出的元数据存放在目标目录中，数据文件存放在子目录中。
[root@zgg ~]# hadoop fs -ls /in/zipper_table
Found 2 items
-rw-r--r--   1 root supergroup       1733 2021-01-12 13:18 /in/zipper_table/_metadata
drwxr-xr-x   - root supergroup          0 2021-01-12 13:18 /in/zipper_table/data

-- 如果直接执行如下，会报错
hive> import from '/in/zipper_table';
FAILED: SemanticException [Error 10119]: Table exists and contains data files

-- 需要这个表的数据目录下没有数据文件才行
[root@zgg ~]# hadoop fs -rm -r /user/hive/warehouse/zipper_table/000000_0

-- 导入
hive> import from '/in/zipper_table';
Copying data from hdfs://zgg:9000/in/zipper_table/data
Copying file: hdfs://zgg:9000/in/zipper_table/data/000000_0
Loading data to table default.zipper_table
OK

-- 查看表 zipper_table
hive> select * from zipper_table;
OK
1       lijie   chengdu 20191021        99991231
1       lijie   chongqing       20191020        20191020
2       zhangshan       huoxing 20191021        99991231
2       zhangshan       sz      20191020        20191020
3       lisi    shanghai        20191020        99991231
4       wangwu  lalalala        20191021        99991231
4       wangwu  usa     20191020        20191020
5       xinzeng hehe    20191021        99991231

-- 也可以创建一个新表，将zipper_table的数据导入到新表中
-- 复制 zipper_table 的表结构，来创建一个表
hive> create table zipper_bk like zipper_table;
OK

-- 导入
hive> import table zipper_bk from '/in/zipper_table';
Copying data from hdfs://zgg:9000/in/zipper_table/data
Copying file: hdfs://zgg:9000/in/zipper_table/data/000000_0
Loading data to table default.zipper_bk
OK

-- 查看表 zipper_bk
hive> select * from zipper_bk;
OK
1       lijie   chengdu 20191021        99991231
1       lijie   chongqing       20191020        20191020
2       zhangshan       huoxing 20191021        99991231
2       zhangshan       sz      20191020        20191020
3       lisi    shanghai        20191020        99991231
4       wangwu  lalalala        20191021        99991231
4       wangwu  usa     20191020        20191020
5       xinzeng hehe    20191021        99991231


-- 也可以直接在import中添加一个未创建过的新表的名称
-- 如果目标表/分区不存在，IMPORT 将创建它。
hive> import table zipper_bk2 from '/in/zipper_table';
Copying data from hdfs://zgg:9000/in/zipper_table/data
Copying file: hdfs://zgg:9000/in/zipper_table/data/000000_0
Loading data to table default.zipper_bk2
OK

-- 查看表 zipper_bk2
hive> select * from zipper_bk2;
OK
1       lijie   chengdu 20191021        99991231
1       lijie   chongqing       20191020        20191020
2       zhangshan       huoxing 20191021        99991231
2       zhangshan       sz      20191020        20191020
3       lisi    shanghai        20191020        99991231
4       wangwu  lalalala        20191021        99991231
4       wangwu  usa     20191020        20191020
5       xinzeng hehe    20191021        99991231

-- -- 分区 ---------

hive> select * from order_table_s;
OK
1       bicycle 1000    201901
2       truck   20000   201901
1       cellphone       2000    201901
2       tv      3000    201901
1       apple   10      201902
2       banana  8       201902
3       milk    70      201902
4       liquor  150     201902

hive> desc order_table_s;
OK
order_id                int                                         
product_name            string                                      
price                   int                                         
deal_day                string                                      
                 
# Partition Information          
# col_name              data_type               comment             
deal_day                string      

-- 导出一个分区
hive> export table order_table_s partition (deal_day='201901') to '/in/order_table_s/';
OK

-- hdfs上查看
[root@zgg ~]# hadoop fs -ls /in/order_table_s
Found 2 items
-rw-r--r--   1 root supergroup       2761 2021-01-12 13:36 /in/order_table_s/_metadata
drwxr-xr-x   - root supergroup          0 2021-01-12 13:36 /in/order_table_s/deal_day=201901

-- 删除这个分区
hive> alter table order_table_s drop partition(deal_day='201901');
Dropped the partition deal_day=201901
OK

-- 导入这个分区的数据
hive> import from '/in/order_table_s/';
Copying data from hdfs://zgg:9000/in/order_table_s/deal_day=201901
Copying file: hdfs://zgg:9000/in/order_table_s/deal_day=201901/000000_0
Loading data to table default.order_table_s partition (deal_day=201901)
OK

-- 查看order_table_s
hive> select * from order_table_s;
OK
1       bicycle 1000    201901
2       truck   20000   201901
1       cellphone       2000    201901
2       tv      3000    201901
1       apple   10      201902
2       banana  8       201902
3       milk    70      201902
4       liquor  150     201902
```