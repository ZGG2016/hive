# Partition Filter Syntax

> Example: for a table having partition keys country and state, one could construct the following filter:

一个表具有分区键 country 和 state，构造如下过滤：

	country = "USA" AND (state = "CA" OR state = "AZ")

> In particular notice that it is possible to nest sub-expressions within parentheses.

特别要注意，括号内可以嵌套子表达式。

> The following operators are supported when constructing filters for partition columns (derived from [HIVE-1862](https://jira.apache.org/jira/browse/HIVE-1862)):

在为分区列构建过滤器时，支持以下操作符：

- =
- <
- <=
- >
- >=
- <>
- AND
- OR
- LIKE (on keys of type string only, supports literal string template with `.*` wildcard)