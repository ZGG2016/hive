# hive 压缩使用

[TOC]

hive 的压缩可以在 mr 层面控制，也可以在 hive 层面控制。

## 1 mr

在 core-site.xml 中配置指定输入压缩格式

```
hive (default)>set io.compression.codecs=org.apache.hadoop.io.compress.SnappyCodec;
```

开启 mapreduce 中 map 输出压缩功能

```
hive (default)>set mapreduce.map.output.compress=true;
```

设置 mapreduce 中 map 输出数据的压缩方式

```
hive (default)>set mapreduce.map.output.compress.codec = org.apache.hadoop.io.compress.SnappyCodec;
```

开启 mapreduce 最终输出数据压缩

```
hive (default)>set mapreduce.output.fileoutputformat.compress=true;
```

设置 mapreduce 最终数据输出压缩方式

```
hive (default)> set mapreduce.output.fileoutputformat.compress.codec =org.apache.hadoop.io.compress.SnappyCodec;
```

设置 mapreduce 最终数据输出压缩为块压缩

```
hive (default)> set mapreduce.output.fileoutputformat.compress.type=BLOCK;
```

## 2 hive

开启 hive 生成的中间文件在多个 mr job 间传输数据的压缩功能

```
hive (default)>set hive.exec.compress.intermediate=true;
```


开启一个 hive 查询的最终输出（输出到hive表、本地文件、hdfs文件）的压缩功能

```
hive (default)>set hive.exec.compress.output=true;
```

测试一下输出结果是否是压缩文件

```
hive (default)> insert overwrite local directory '/opt/module/data/distribute-result' select * from emp distribute by deptno sort by empno desc;
```

--------------------------
参考

- [尚硅谷hive教程](https://www.bilibili.com/video/BV1EZ4y1G7iL)
- [官网配置](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties)