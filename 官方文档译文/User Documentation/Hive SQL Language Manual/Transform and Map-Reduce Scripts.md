# Transform and Map-Reduce Scripts

[TOC]

## 1、Transform/Map-Reduce Syntax

> Users can also plug in their own custom mappers and reducers in the data stream by using features natively supported in the Hive language. e.g. in order to run a custom mapper script - map_script - and a custom reducer script - reduce_script - the user can issue the following command which uses the TRANSFORM clause to embed the mapper and the reducer scripts.

用户还可以使用 Hive 语言本地支持的特性，在数据流中插入自己的自定义 mappers 和 reducers。

例如，为了运行一个自定义 mapper 脚本(map_script) 和一个自定义 reducer 脚本(reduce_script)。用户可以通过**使用 TRANSFORM 子句来嵌入 mapper 和 reducer 脚本**。

> By default, columns will be transformed to STRING and delimited by TAB before feeding to the user script; similarly, all NULL values will be converted to the literal string \N in order to differentiate NULL values from empty strings. The standard output of the user script will be treated as TAB-separated STRING columns, any cell containing only \N will be re-interpreted as a NULL, and then the resulting STRING column will be cast to the data type specified in the table declaration in the usual way. User scripts can output debug information to standard error which will be shown on the task detail page on hadoop. These defaults can be overridden with ROW FORMAT ....

**默认情况下，列将被转换为 STRING，并在提供给用户脚本之前由 TAB 分隔**；类似地，所有的 NULL 值都将被转换为字面字符串`\N`，以便将 NULL 值与空字符串区分开来。

**用户脚本的标准输出将被视为 TAB 分隔的 STRING 列**，任何只包含 `\N` 的单元格将被重新解释为 NULL，

**然后生成的 STRING 列转换为表声明中指定的数据类型**。

用户脚本可以将调试信息输出到标准错误，这些错误将显示在 hadoop 上的 task detail 页面上。

这些默认值可以用 `ROW FORMAT ....` 覆盖。

> In windows, use "cmd /c your_script" instead of just "your_script"

在 windows 下，使用 `cmd /c your_script` 而不是 `your_script`

> Warning:It is your responsibility to sanitize any STRING columns prior to transformation. If your STRING column contains tabs, an identity transformer will not give you back what you started with! To help with this, see [REGEXP_REPLACE](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+UDF#LanguageManualUDF-StringFunctions) and replace the tabs with some other character on their way into the TRANSFORM() call.

警告：你有责任在转换之前对任何 STRING 列进行清理。

**如果 STRING 列包含 tabs，标识转换器将不会返回你开始使用的值**！要对此有所帮助，请参阅 REGEXP_REPLACE，并在 tabs 进入 TRANSFORM() 调用时将它们替换为其他字符。

> Warning:Formally, MAP ... and REDUCE ... are syntactic transformations of SELECT TRANSFORM ( ... ). In other words, they serve as comments or notes to the reader of the query. BEWARE: Use of these keywords may be dangerous as (e.g.) typing "REDUCE" does not force a reduce phase to occur and typing "MAP" does not force a new map phase!

警告：**`MAP ...` 和 `REDUCE ...` 是 `SELECT TRANSFORM(…)`的语法转换**。【等价】

换句话说，它们可以作为查询读者的评论或注释。

注意：使用这些关键字可能是危险的(例如)**键入 `REDUCE` 并不会迫使一个 REDUCE 阶段发生，键入 MAP 也不会迫使一个新的 MAP 阶段发生**!

> Please also see [Sort By/Cluster By/Distribute By](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+SortBy) and Larry Ogrodnek's [blog post](http://dev.bizo.com/2009/10/hive-map-reduce-in-java.html).

请参见 Sort By/Cluster By/Distribute By 和 Larry Ogrodnek 的博客文章。

	clusterBy: CLUSTER BY colName (',' colName)*
	distributeBy: DISTRIBUTE BY colName (',' colName)*
	sortBy: SORT BY colName (ASC | DESC)? (',' colName (ASC | DESC)?)*
	 
	rowFormat
	  : ROW FORMAT
	    (DELIMITED [FIELDS TERMINATED BY char]
	               [COLLECTION ITEMS TERMINATED BY char]
	               [MAP KEYS TERMINATED BY char]
	               [ESCAPED BY char]
	               [LINES SEPARATED BY char]
	     |
	     SERDE serde_name [WITH SERDEPROPERTIES
	                            property_name=property_value,
	                            property_name=property_value, ...])
	 
	outRowFormat : rowFormat
	inRowFormat : rowFormat
	outRecordReader : RECORDREADER className
	 
	query:
	  FROM (
	    FROM src
	    MAP expression (',' expression)*
	    (inRowFormat)?
	    USING 'my_map_script'
	    ( AS colName (',' colName)* )?
	    (outRowFormat)? (outRecordReader)?
	    ( clusterBy? | distributeBy? sortBy? ) src_alias
	  )
	  REDUCE expression (',' expression)*
	    (inRowFormat)?
	    USING 'my_reduce_script'
	    ( AS colName (',' colName)* )?
	    (outRowFormat)? (outRecordReader)?
	 
	  FROM (
	    FROM src
	    SELECT TRANSFORM '(' expression (',' expression)* ')'
	    (inRowFormat)?
	    USING 'my_map_script'
	    ( AS colName (',' colName)* )?
	    (outRowFormat)? (outRecordReader)?
	    ( clusterBy? | distributeBy? sortBy? ) src_alias
	  )
	  SELECT TRANSFORM '(' expression (',' expression)* ')'
	    (inRowFormat)?
	    USING 'my_reduce_script'
	    ( AS colName (',' colName)* )?
	    (outRowFormat)? (outRecordReader)?

### 1.1、SQL Standard Based Authorization Disallows TRANSFORM

> The TRANSFORM clause is disallowed when [SQL standard based authorization](https://cwiki.apache.org/confluence/display/Hive/SQL+Standard+Based+Hive+Authorization) is configured in Hive 0.13.0 and later releases ([HIVE-6415](https://issues.apache.org/jira/browse/HIVE-6415)).

在 Hive 0.13.0 及后续版本中，当配置基于 SQL 标准的授权时，TRANSFORM 子句是不允许的。

### 1.2、TRANSFORM Examples

> Example #1:

```sql
FROM (
  FROM pv_users
  MAP pv_users.userid, pv_users.date
  USING 'map_script'
  AS dt, uid
  CLUSTER BY dt) map_output
INSERT OVERWRITE TABLE pv_users_reduced
  REDUCE map_output.dt, map_output.uid
  USING 'reduce_script'
  AS date, count;

FROM (
  FROM pv_users
  SELECT TRANSFORM(pv_users.userid, pv_users.date)
  USING 'map_script'
  AS dt, uid
  CLUSTER BY dt) map_output
INSERT OVERWRITE TABLE pv_users_reduced
  SELECT TRANSFORM(map_output.dt, map_output.uid)
  USING 'reduce_script'
  AS date, count;
```

> Example #2

```sql
FROM (
  FROM src
  SELECT TRANSFORM(src.key, src.value) ROW FORMAT SERDE 'org.apache.hadoop.hive.contrib.serde2.TypedBytesSerDe'
  USING '/bin/cat'
  AS (tkey, tvalue) ROW FORMAT SERDE 'org.apache.hadoop.hive.contrib.serde2.TypedBytesSerDe'
  RECORDREADER 'org.apache.hadoop.hive.contrib.util.typedbytes.TypedBytesRecordReader'
) tmap
INSERT OVERWRITE TABLE dest1 SELECT tkey, tvalue
```

## 2、Schema-less Map-reduce Scripts

> If there is no AS clause after USING my_script, Hive assumes that the output of the script contains 2 parts: key which is before the first tab, and value which is the rest after the first tab. Note that this is different from specifying AS key, value because in that case, value will only contain the portion between the first tab and the second tab if there are multiple tabs.

如果在 `USING my_script` 后没有 AS 子句，Hive 假设脚本输出包含两个部分：key 是在第一个制表符之前的部分，value 是在第一个制表符之后的部分。

注意，这与指定 `AS key, value` 不同，因为在这种情况下，如果有多个制表符，value 将只包含第一个制表符和第二个制表符之间的部分。

> Note that we can directly do CLUSTER BY key without specifying the output schema of the scripts.

注意，我们可以直接执行 `CLUSTER BY key `，而不需要指定脚本的输出模式。

```sql
FROM (
  FROM pv_users
  MAP pv_users.userid, pv_users.date
  USING 'map_script'
  CLUSTER BY key) map_output
INSERT OVERWRITE TABLE pv_users_reduced
  REDUCE map_output.key, map_output.value
  USING 'reduce_script'
  AS date, count;
```

## 3、Typing the output of TRANSFORM

> The output fields from a script are typed as strings by default; for example in

**脚本的输出字段默认为字符串类型**。

```sql
SELECT TRANSFORM(stuff)
USING 'script'
AS thing1, thing2
```

> They can be immediately casted with the syntax:

它们**可以立即被转换**：

```sql
SELECT TRANSFORM(stuff)
USING 'script'
AS (thing1 INT, thing2 INT)
```

帮助理解：[https://www.cnblogs.com/aquastone/p/hive-transform.html](https://www.cnblogs.com/aquastone/p/hive-transform.html)