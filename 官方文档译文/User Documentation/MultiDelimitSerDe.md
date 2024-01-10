# MultiDelimitSerDe

[TOC]

## Introduction

Introduced in [HIVE-5871](https://issues.apache.org/jira/browse/HIVE-5871), MultiDelimitSerDe allows user to specify multiple-character string as the field delimiter when creating a table.

## Version

Hive 0.14.0 and later.

## Hive QL Syntax

You can use MultiDelimitSerDe in a create table statement like this:

```sql
CREATE TABLE test (
 id string,
 hivearray array<binary>,
 hivemap map<string,int>) 
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.MultiDelimitSerDe'                  
WITH SERDEPROPERTIES ("field.delim"="[,]","collection.delim"=":","mapkey.delim"="@");
```
where `field.delim` is the field delimiter, `collection.delim` and `mapkey.delim` is the delimiter for collection items and key value pairs, respectively. 

`field.delim`： 字段分隔符

`collection.delim`： 集合项分隔符

`mapkey.delim`： 键值对分隔符

[HIVE-20619](https://issues.apache.org/jira/browse/HIVE-20619) moved MultiDelimitSerDe to `hive.serde2` in release 4.0.0, so user won't have to install hive-contrib JAR into the HiveServer2 auxiliary directory.

## Limitations

- Among the delimiters, `field.delim` is mandatory and can be of multiple characters, while `collection.delim` and `mapkey.delim` is optional and only support single character.

	`field.delim` 是强制的，可以是多字符。

	`collection.delim` 和 `mapkey.delim` 是可选的，仅支持单个字符

- Nested complex type is not supported, e.g. an `Array<Array>`.

- To use MultiDelimitSerDe prior to Hive release 4.0.0, you have to add the hive-contrib jar to the class path, e.g. with the add jar command.