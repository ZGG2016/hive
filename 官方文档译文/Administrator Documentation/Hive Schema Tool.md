# Hive Schema Tool

[TOC]

## 1、Metastore Schema Verification

> Version. Introduced in Hive 0.12.0. See [HIVE-3764](https://issues.apache.org/jira/browse/HIVE-3764).

在 Hive 0.12.0 中引入。

> Hive now records the schema version in the metastore database and verifies that the metastore schema version is compatible with Hive binaries that are going to accesss the metastore. Note that the Hive properties to implicitly create or alter the existing schema are disabled by default. Hive will not attempt to change the metastore schema implicitly. When you execute a Hive query against an old schema, it will fail to access the metastore:

Hive 现在会在 metastore 数据库中记录 schema 版本，并验证 metastore schema 版本是否与将要访问该 metastore 的 Hive 二进制文件兼容。

注意，默认情况下，用于隐式地创建或更改现有 schema 的 Hive 属性是禁用的。Hive 不会隐式地修改 metastore schema。当你在旧的 schema 执行 Hive 查询时，它将无法访问 metastore:

	$ build/dist/bin/hive -e "show tables"
	FAILED: Execution Error, return code 1 from org.apache.hadoop.hive.ql.exec.DDLTask. java.lang.RuntimeException: Unable to instantiate org.apache.hadoop.hive.metastore.HiveMetaStoreClient

> The log will contain an error about version information not found:

该日志将包含一个关于找不到版本信息的错误:

	...
	Caused by: MetaException(message:Version information not found in metastore. )
	...

> By default the configuration property hive.metastore.schema.verification is false and metastore to implicitly write the schema version if it's not matching. To enable the strict schema verification, you need to set this property to true in hive-site.xml.

默认情况下，配置属性 `hive.metastore.schema.verification` 为 false，如果不匹配，metastore 会隐式地编写 schema 版本。要启用严格的 schema 验证，您需要在 hive-site.xml 中将此属性设置为 true。

> See [Hive Metastore Administration](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+Administration) for general information about the metastore.

## 2、The Hive Schema Tool

> Version. Introduced in Hive 0.12.0. See [HIVE-5301](https://issues.apache.org/jira/browse/HIVE-5301). (Also see [HIVE-5449](https://issues.apache.org/jira/browse/HIVE-5449) for a bug fix.)

在 Hive 0.12.0 中引入。

> The Hive distribution now includes an offline tool for Hive metastore schema manipulation. This tool can be used to initialize the metastore schema for the current Hive version. It can also handle upgrading the schema from an older version to current. It tries to find the current schema from the metastore if it is available. This will be applicable to future upgrades like 0.12.0 to 0.13.0. In case of upgrades from older releases like 0.7.0 or 0.10.0, you can specify the schema version of the existing metastore as a command line option to the tool.

现在，Hive 发行版包含了一个离线工具，用于 Hive metastore schema 操作。

该工具可用于初始化当前 Hive 版本的 metastore schema。它还可以处理将从旧版本的 schema 升级到当前版本。

它尝试从 metastore 中找到当前 schema(如果它可用)。这将适用于未来的升级，如 0.12.0 到 0.13.0。

对于从旧版本(如0.7.0或0.10.0)升级的情况，可以将现有 metastore 的 schema 版本指定为工具的命令行选项。

> The schematool figures out the SQL scripts required to initialize or upgrade the schema and then executes those scripts against the backend database. The metastore DB connection information like JDBC URL, JDBC driver and DB credentials are extracted from the Hive configuration. You can provide alternate DB credentials if needed.

schematool 计算出初始化或升级模式所需的 SQL 脚本，然后针对后端数据库执行这些脚本。metastore DB 连接信息，如 JDBC URL、JDBC 驱动程序和数据库凭证，是从 Hive 配置中提取的。如果需要，你可以提供备用的 DB 凭证。

### 2.1、The schematool Command

> The schematool command invokes the Hive schema tool with these options:

	$ schematool -help
	usage: schemaTool
	 -dbType <databaseType>             Metastore database type
	 -driver <driver>                   Driver name for connection
	 -dryRun                            List SQL scripts (no execute)
	 -help                              Print this message
	 -info                              Show config and schema details
	 -initSchema                        Schema initialization
	 -initSchemaTo <initTo>             Schema initialization to a version
	 -metaDbType <metaDatabaseType>     Used only if upgrading the system catalog for hive
	 -passWord <password>               Override config file password
	 -upgradeSchema                     Schema upgrade
	 -upgradeSchemaFrom <upgradeFrom>   Schema upgrade from a version
	 -url <url>                         Connection url to the database
	 -userName <user>                   Override config file user name
	 -verbose                           Only print SQL statements
	(Additional catalog related options added in Hive 3.0.0 (HIVE-19135] release are below.
	 -createCatalog <catalog>       Create catalog with given name
	 -catalogLocation <location>        Location of new catalog, required when adding a catalog
	 -catalogDescription <description>  Description of new catalog
	 -ifNotExists                       If passed then it is not an error to create an existing catalog
	 -moveDatabase <database>                     Move a database between catalogs.  All tables under it would still be under it as part of new catalog. Argument is the database name. Requires --fromCatalog and --toCatalog parameters as well
	 -moveTable  <table>                Move a table to a different database.  Argument is the table name. Requires --fromCatalog, --toCatalog, --fromDatabase, and --toDatabase 
	 -toCatalog  <catalog>              Catalog a moving database or table is going to.  This is required if you are moving a database or table.
	 -fromCatalog <catalog>             Catalog a moving database or table is coming from.  This is required if you are moving a database or table.
	 -toDatabase  <database>            Database a moving table is going to.  This is required if you are moving a table.
	 -fromDatabase <database>           Database a moving table is coming from.  This is required if you are moving a table.


> The dbType is required and can be one of:

dbType 是要求的选项，可以是如下几项：

	derby|mysql|postgres|oracle|mssql

> Version. The dbType "mssql" was added in Hive 0.13.1 with [HIVE-6862](https://issues.apache.org/jira/browse/HIVE-6862).

dbType "mssql" 是在 Hive 0.13.1 引入。

### 2.2、Usage Examples

- Initialize to current schema for a new Hive setup:

```
$ schematool -dbType derby -initSchema
Metastore connection URL:        jdbc:derby:;databaseName=metastore_db;create=true
Metastore Connection Driver :    org.apache.derby.jdbc.EmbeddedDriver
Metastore connection User:       APP
Starting metastore schema initialization to 0.13.0
Initialization script hive-schema-0.13.0.derby.sql
Initialization script completed
schemaTool completed
```

- Get schema information:

```
$ schematool -dbType derby -info
Metastore connection URL:        jdbc:derby:;databaseName=metastore_db;create=true
Metastore Connection Driver :    org.apache.derby.jdbc.EmbeddedDriver
Metastore connection User:       APP
Hive distribution version:       0.13.0
Metastore schema version:        0.13.0
schemaTool completed
```

- Attempt to get schema information with older metastore:

```
$ schematool -dbType derby -info
Metastore connection URL:        jdbc:derby:;databaseName=metastore_db;create=true
Metastore Connection Driver :    org.apache.derby.jdbc.EmbeddedDriver
Metastore connection User:       APP
Hive distribution version:       0.13.0
org.apache.hadoop.hive.metastore.HiveMetaException: Failed to get schema version.
*** schemaTool failed ***
```

Since the older metastore doesn't store the version information, the tool reports an error retrieving it.

- Upgrade schema from an 0.10.0 release by specifying the 'from' version:

```
$ schematool -dbType derby -upgradeSchemaFrom 0.10.0
Metastore connection URL:        jdbc:derby:;databaseName=metastore_db;create=true
Metastore Connection Driver :    org.apache.derby.jdbc.EmbeddedDriver
Metastore connection User:       APP
Starting upgrade metastore schema from version 0.10.0 to 0.13.0
Upgrade script upgrade-0.10.0-to-0.11.0.derby.sql
Completed upgrade-0.10.0-to-0.11.0.derby.sql
Upgrade script upgrade-0.11.0-to-0.12.0.derby.sql
Completed upgrade-0.11.0-to-0.12.0.derby.sql
Upgrade script upgrade-0.12.0-to-0.13.0.derby.sql
Completed upgrade-0.12.0-to-0.13.0.derby.sql
schemaTool completed
```

- Upgrade dry run can be used to list the required scripts for the given upgrade.

```
$ build/dist/bin/schematool -dbType derby -upgradeSchemaFrom 0.7.0 -dryRun
Metastore Connection Driver :    org.apache.derby.jdbc.EmbeddedDriver
Metastore connection User:       APP
Starting upgrade metastore schema from version 0.7.0 to 0.13.0
Upgrade script upgrade-0.7.0-to-0.8.0.derby.sql
Upgrade script upgrade-0.8.0-to-0.9.0.derby.sql
Upgrade script upgrade-0.9.0-to-0.10.0.derby.sql
Upgrade script upgrade-0.10.0-to-0.11.0.derby.sql
Upgrade script upgrade-0.11.0-to-0.12.0.derby.sql
Upgrade script upgrade-0.12.0-to-0.13.0.derby.sql
schemaTool completed
```

This is useful if you just want to find out all the required scripts for the schema upgrade.

- Moving a database and tables under it from default Hive catalog to a custom spark catalog

```
build/dist/bin/schematool -moveDatabase db1 -fromCatalog hive -toCatalog spark

Moving a table from Hive catalog to Spark Catalog

# Create the desired target database in spark catalog if it doesn't already exist.
beeline ... -e "create database if not exists newdb";
schematool -moveDatabase newdb -fromCatalog hive -toCatalog spark

# Now move the table to target db under the spark catalog.
schematool -moveTable table1 -fromCatalog hive -toCatalog spark  -fromDatabase db1 -toDatabase newdb
```