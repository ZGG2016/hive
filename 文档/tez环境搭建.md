# tez环境搭建

[TOC]

#### 1、下载、解压、重命名

下载：[http://tez.apache.org/releases/index.html](http://tez.apache.org/releases/index.html)

解压：`tar -zxvf apache-tez-0.9.1-bin.tar.gz`

重命名：`mv apache-tez-0.9.1-bin tez-0.9.1`

#### 2、在hive中配置

配置 hive-env.sh 和 hive-site.sh

```sh
[root@zgg conf]# vi hive-env.sh
export TEZ_HOME=/opt/tez-0.9.1  # tez的解压目录
export TEZ_JARS=""
for jar in `ls $TEZ_HOME |grep jar`; do
    export TEZ_JARS=$TEZ_JARS:$TEZ_HOME/$jar
done
for jar in `ls $TEZ_HOME/lib`; do
    export TEZ_JARS=$TEZ_JARS:$TEZ_HOME/lib/$jar
done

export HIVE_AUX_JARS_PATH=/opt/hadoop-2.7.3/share/hadoop/common/hadoop-lzo-0.4.21-SNAPSHOT.jar$TEZ_JAR
```
```sh
[root@zgg conf]# vi hive-site.xml
<property>
    <name>hive.execution.engine</name>
    <value>tez</value>
</property>
```

#### 3、配置tez

在 hive/conf 目录下添加 tez-site.xml 文件，并复制到 `/opt/hadoop-2.7.3/etc/hadoop/`目录下（否则报`org.apache.tez.dag.api.TezUncheckedException: Invalid configuration of tez jars, tez.lib.uris`错误）。

```sh
[root@zgg conf]# vi tez-site.xml
<?xml version="1.0" encoding="UTF-8"?>
<?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
<configuration>
    <property>
        <name>tez.lib.uris</name>
        <value>${fs.defaultFS}/tez/tez-0.9.1,${fs.defaultFS}/tez/tez-0.9.1/lib</value>
    </property>
    <property>
        <name>tez.lib.uris.classpath</name>
        <value>${fs.defaultFS}/tez/tez-0.9.1,${fs.defaultFS}/tez/tez-0.9.1/lib</value>
    </property>
    <property>
        <name>tez.use.cluster.hadoop-libs</name>
        <value>true</value>
    </property>
    <property>
        <name>tez.history.logging.service.class</name>
        <value>org.apache.tez.dag.history.logging.ats.ATSHistoryLoggingService</value>
    </property>
</configuration>
```

#### 4、将tez上传到hdfs

```sh
[root@zgg script]# hadoop fs -mkdir /tez

[root@zgg script]# hadoop fs -ls /      
drwxr-xr-x   - root supergroup          0 2021-02-07 16:21 /tez

[root@zgg opt]# hadoop fs -put tez-0.9.1/ /tez

[root@zgg script]# hadoop fs -ls /tez
drwxr-xr-x   - root supergroup          0 2021-02-07 16:22 /tez/tez-0.9.1
```

#### 5、 测试

```sh
# 启动
bin/hive

# 建表
hive> create table student(
    > id int,
    > name string);
OK
Time taken: 0.971 seconds

# 插入数据，如果没有报错就表示成功了
hive> insert into student values(1,"zhangsan");
Query ID = root_20210926203604_4fde13d1-2955-408c-992a-e266100f9348
Total jobs = 1
Launching Job 1 out of 1


Status: Running (Executing on YARN cluster with App id application_1632659705020_0001)

--------------------------------------------------------------------------------
        VERTICES      STATUS  TOTAL  COMPLETED  RUNNING  PENDING  FAILED  KILLED
--------------------------------------------------------------------------------
Map 1 ..........   SUCCEEDED      1          1        0        0       0       0
--------------------------------------------------------------------------------
VERTICES: 01/01  [==========================>>] 100%  ELAPSED TIME: 12.03 s    
--------------------------------------------------------------------------------
Loading data to table default.student
Table default.student stats: [numFiles=1, numRows=1, totalSize=11, rawDataSize=10]
OK
Time taken: 15.825 seconds

# 查看
hive> select * from student;
OK
1       zhangsan
Time taken: 0.31 seconds, Fetched: 1 row(s)
```

#### 6、出现的问题

```
Exception in thread "main" java.lang.RuntimeException: org.apache.tez.dag.api.SessionNotRunning: TezSession has already shutdown. Application application_1632655765056_0001 failed 2 times due to AM Container for appattempt_1632655765056_0001_000002 exited with  exitCode: -103
```

Container 试图使用过多的内存，被 NodeManager kill 掉

解决方式：

方案一：关掉虚拟内存检查。我们选这个，修改 yarn-site.xml，修改后同步集群配置。

```xml
<property>
    <name>yarn.nodemanager.vmem-check-enabled</name>
    <value>false</value>
</property>
```

方案二：mapred-site.xml 中设置 Map 和 Reduce 任务的内存配置如下：(value中实际配置的内存需要根据自己机器内存大小及应用情况进行修改)

```xml
<property>
　　<name>mapreduce.map.memory.mb</name>
　　<value>1536</value>
</property>
<property>
　　<name>mapreduce.map.java.opts</name>
　　<value>-Xmx1024M</value>
</property>
<property>
　　<name>mapreduce.reduce.memory.mb</name>
　　<value>3072</value>
</property>
<property>
　　<name>mapreduce.reduce.java.opts</name>
　　<value>-Xmx2560M</value>
</property>
```