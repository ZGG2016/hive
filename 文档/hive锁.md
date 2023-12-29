# hive锁

[TOC]

开启关闭锁机制 `set hive.support.concurrency = true/false`

## 锁表与解锁表

```sql
lock table t5 exclusive;

unlock table t5;
```

## show locks

[show locks](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-ShowLocks)

```sql
0: jdbc:hive2://zgg-server:10000> show locks transactions;
+---------+-----------+---------------+---------------------+-------------+---------------+--------------+-----------------+-----------------+----------------+-------+-------------+----------------------------------------------------+
| lockid  | database  |     table     |      partition      | lock_state  |  blocked_by   |  lock_type   | transaction_id  | last_heartbeat  |  acquired_at   | user  |  hostname   |                     agent_info                     |
+---------+-----------+---------------+---------------------+-------------+---------------+--------------+-----------------+-----------------+----------------+-------+-------------+----------------------------------------------------+
| 67.3    | default   | transactions  | NULL                | ACQUIRED    |               | SHARED_READ  | 72              | 0               | 1703775714701  | root  | zgg-server  | root_20231228150146_272b76ef-0442-44e9-af01-d6c0c8643c73 |
| 67.1    | default   | transactions  | tran_date=20170410  | ACQUIRED    |               | EXCL_WRITE   | 72              | 0               | 1703775714701  | root  | zgg-server  | root_20231228150146_272b76ef-0442-44e9-af01-d6c0c8643c73 |
| 67.2    | default   | transactions  | tran_date=20170413  | ACQUIRED    |               | EXCL_WRITE   | 72              | 0               | 1703775714701  | root  | zgg-server  | root_20231228150146_272b76ef-0442-44e9-af01-d6c0c8643c73 |
+---------+-----------+---------------+---------------------+-------------+---------------+--------------+-----------------+-----------------+----------------+-------+-------------+----------------------------------------------------+

0: jdbc:hive2://zgg-server:10000> show locks transactions partition(tran_date='20170410');
+---------+-----------+---------------+---------------------+-------------+---------------+-------------+-----------------+-----------------+----------------+-------+-------------+----------------------------------------------------+
| lockid  | database  |     table     |      partition      | lock_state  |  blocked_by   |  lock_type  | transaction_id  | last_heartbeat  |  acquired_at   | user  |  hostname   |                     agent_info                     |
+---------+-----------+---------------+---------------------+-------------+---------------+-------------+-----------------+-----------------+----------------+-------+-------------+----------------------------------------------------+
| 67.1    | default   | transactions  | tran_date=20170410  | ACQUIRED    |               | EXCL_WRITE  | 72              | 0               | 1703775714701  | root  | zgg-server  | root_20231228150146_272b76ef-0442-44e9-af01-d6c0c8643c73 |
+---------+-----------+---------------+---------------------+-------------+---------------+-------------+-----------------+-----------------+----------------+-------+-------------+----------------------------------------------------+
```

## explain locks

[explain locks](https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Explain#LanguageManualExplain-TheLOCKSClause)

Explore if it's possible to add info about what locks will be asked for to the query plan.

```sql
0: jdbc:hive2://zgg-server:10000> EXPLAIN LOCKS UPDATE transactions SET TranValue = 'value_11' WHERE tran_date='20170410' and ID=1;
+----------------------------------------------------+
|                      Explain                       |
+----------------------------------------------------+
| LOCK INFORMATION:                                  |
| default.transactions.tran_date=20170410 -> SHARED_READ |
| default.transactions -> SHARED_READ                |
| default.transactions.tran_date=20170410 -> EXCL_WRITE |
+----------------------------------------------------+
```

## 其他

[锁配置](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-Locking)

[锁部分理论](https://github.com/ZGG2016/hive/blob/master/%E5%AE%98%E6%96%B9%E6%96%87%E6%A1%A3%E8%AF%91%E6%96%87/User%20Documentation/Hive%20Transactions.md)

[官网](https://cwiki.apache.org/confluence/display/Hive/Locking)

[官网部分翻译]()

[帮助理解](https://cloud.tencent.com/developer/article/1043996)：

```
Hive（CDH4.2.0）的锁处理流程：

1.首先对query进行编译，生成QueryPlan

2.构建读写锁对象（主要两个成员变量：LockObject，Lockmode）
  对于非分区表，直接根据需要构建S或者X锁对象
  对于分区表：(此处是区分input/output)
If S mode:
    直接对Table/related partition 构建S对象
Else：
    If 添加新分区：
        构建S对象
    Else
        构建X对象
End

3.对锁对象进行字符表排序(避免死锁)，对于同一个LockObject，先获取Execlusive

4.遍历锁对象列表，进行锁申请
While trynumber< hive.lock.numretries(default100):
    创建parent（node）目录,mode= CreateMode.PERSISTENT
    创建锁目录，mode=CreateMode.EPHEMERAL_SEQUENTIAL
    For Child：Children
        If Child已经有写锁：
            获取child写锁seqno
        If mode=X 并且 Child 已经有读锁
            获取child读锁seqno
        If childseqno>0并且小于当前seqno
            释放锁
        Trynumber++
    Sleep(hive.lock.sleep.between.retries:default1min)
```