# HiveServer2 Overview

## 1、Introduction

> HiveServer2 (HS2) is a service that enables clients to execute queries against Hive. HiveServer2 is the successor to [HiveServer1](https://cwiki.apache.org/confluence/display/Hive/HiveServer) which has been deprecated. HS2 supports multi-client concurrency and authentication. It is designed to provide better support for open API clients like JDBC and ODBC.

HiveServer2 (HS2)是一个服务，允许客户端对 Hive 执行查询。 HiveServer2 是已被弃用的 HiveServer1 的继承者。

HS2 **支持多客户端并发和身份验证**。它旨在为 JDBC 和 ODBC 等开放 API 客户端提供更好的支持。

HS2 是**作为复合服务运行的单个进程**，包括基于 Thrift 的 Hive 服务(TCP或HTTP)和用于 web UI 的 Jetty web 服务。

> HS2 is a single process running as a composite service, which includes the Thrift-based Hive service (TCP or HTTP) and a [Jetty](http://www.eclipse.org/jetty/) web server for web UI. 

## 2、HS2 Architecture

**基于 Thrift 的 Hive 服务是 HS2 的核心，负责处理 Hive 查询(如，Beeline)**。

**Thrift 是一个用于构建跨平台服务的 RPC 框架。它的栈由4层组成:Server、Transport、 Protocol 和 Processor。**

下面将描述在 HS2 实现中这些层的用法。

> The Thrift-based Hive service is the core of HS2 and responsible for servicing the Hive queries (e.g., from Beeline). [Thrift](https://thrift.apache.org/) is an RPC framework for building cross-platform services. Its stack consists of 4 layers: Server, Transport, Protocol, and Processor. You can find more details about the layers at [https://thrift.apache.org/docs/concepts](https://thrift.apache.org/docs/concepts).

The usage of those layers in the HS2 implementation is described below.

### 2.1、Server

**在 TCP 模式下，HS2 使用 TThreadPoolServer(来自Thrift)，在 HTTP 模式下，使用 Jetty 服务。**

TThreadPoolServer 为每个 TCP 连接分配一个工作线程。每个线程总是与一个连接相关联，即使该连接是空闲的。

因此，由于大量并发连接，导致的大量线程可能会**引起性能问题**。

将来，HS2 可能会切换到另一种服务类型，例如 TThreadedSelectorServer。

> HS2 uses a TThreadPoolServer (from Thrift) for TCP mode, or a Jetty server for the HTTP mode. 

The TThreadPoolServer allocates one worker thread per TCP connection. Each thread is always associated with a connection even if the connection is idle. So there is a potential performance issue resulting from a large number of threads due to a large number of concurrent connections. In the future HS2 might switch to another server type for TCP mode, for example TThreadedSelectorServer. Here is an [article](https://github.com/m1ch1/mapkeeper/wiki/Thrift-Java-Servers-Compared) about a performance comparison between different Thrift Java servers.

### 2.2、Transport

当客户端和服务器之间需要代理时(如，出于负载平衡或安全原因)，需要使用 HTTP 模式。

这就是支持它以及TCP模式的原因。可以通过配置属性`hive.server .transport.mode`指定 Thrift 服务的传输模式。

> HTTP mode is required when a proxy is needed between the client and server (for example, for load balancing or security reasons). That is why it is supported, as well as TCP mode. You can specify the transport mode of the Thrift service through the Hive configuration property [hive.server2.transport.mode](https://cwiki.apache.org/confluence/display/Hive/Configuration+Properties#ConfigurationProperties-hive.server2.transport.mode).

### 2.3、Protocol

Protocol 负责序列化和反序列化。

HS2 目前使用 TBinaryProtocol 作为其序列化的 Thrift 协议。

基于更多的性能评估，在未来可能考虑其他协议，如 TCompactProtocol。

> The Protocol implementation is responsible for serialization and deserialization. HS2 is currently using TBinaryProtocol as its Thrift protocol for serialization. In the future other protocols may be considered, such as TCompactProtocol, based on more performance evaluation.

### 2.4、Processor

**Process 是应用程序处理请求的逻辑**。例如，`ThriftCLIService.ExecuteStatement()`方法实现了编译和执行一个 Hive 查询的逻辑。

> Process implementation is the application logic to handle requests. For example, the ThriftCLIService.ExecuteStatement() method implements the logic to compile and execute a Hive query.

## 3、Dependencies of HS2

- Metastore

metastore 可以嵌入其中(在与HS2相同的进程中)，也可以配置为一个远程服务器(它也是一个基于thrifs的服务)。**HS2 与 metastore 对话，获取查询编译所需的元数据。**

- Hadoop cluster

**HS2 为各种执行引擎(MapReduce/Tez/Spark)准备物理执行计划，并将作业提交到 Hadoop 集群执行。**

> [Metastore](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+Administration)
The metastore can be configured as embedded (in the same process as HS2) or as a remote server (which is a Thrift-based service as well). HS2 talks to the metastore for the metadata required for query compilation.  

> Hadoop cluster
HS2 prepares physical execution plans for various execution engines (MapReduce/Tez/Spark) and submits jobs to the Hadoop cluster for execution.

> You can find a diagram of the interactions between HS2 and its dependencies [here](https://cwiki.apache.org/confluence/display/Hive/Design#Design-HiveArchitecture).

可以在这里找到 HS2 及其依赖关系之间的交互图。

## 4、JDBC Client

> The JDBC driver is recommended for the client side to interact with HS2. Note that there are some use cases (e.g., Hadoop Hue) where the Thrift client is used directly and JDBC is bypassed.

> Here is a sequence of API calls involved to make the first query:

建议在客户端使用 JDBC driver与 HS2 交互。请注意，在一些用例中(如Hadoop Hue)，可以直接使用 Thrift 客户端，而绕开 JDBC。

下面是进行第一个查询所涉及的一系列API调用:

- JDBC 客户端(如，Beeline)通过初始化传输连接(如，TCP连接)，然后通过 OpenSession API 的调用获得一个 SessionHandle 来创建 HiveConnection 。会话是从服务器端创建的。

> The JDBC client (e.g., Beeline) creates a HiveConnection by initiating a transport connection (e.g., TCP connection) followed by an OpenSession API call to get a SessionHandle. The session is created from the server side.

- 执行 HiveStatement(遵循JDBC标准)，并从 Thrift 客户端调用 ExecuteStatement API。在 API 调用中，将 SessionHandle 信息与查询信息一起传递给服务器。

> The HiveStatement is executed (following JDBC standards) and an ExecuteStatement API call is made from the Thrift client. In the API call, SessionHandle information is passed to the server along with the query information.

- HS2 服务器接收请求，并向 driver(它是一个CommandProcessor)请求解析查询和编译。driver 启动一个后台作业，该作业将与 Hadoop 对话，然后立即向客户端返回响应。这是 ExecuteStatement API 的异步设计。该响应包含从服务器端创建的 OperationHandle。

> The HS2 server receives the request and asks the driver (which is a CommandProcessor) for query parsing and compilation. The driver kicks off a background job that will talk to Hadoop and then immediately returns a response to the client. This is an asynchronous design of the ExecuteStatement API. The response contains an OperationHandle created from the server side.

- 客户端使用 OperationHandle 与 HS2 通信，以轮询 查询执行的状态。

> The client uses the OperationHandle to talk to HS2 to poll the status of the query execution.

## 5、Source Code Description

> The following sections help you locate some basic components of HiveServer2 in the source code.

下面几节将帮助您在源代码中找到 HiveServer2 的一些基本组件。

### 5.1、Server Side

- > Thrift IDL file for TCLIService: [https://github.com/apache/hive/blob/master/service-rpc/if/TCLIService.thrift](https://github.com/apache/hive/blob/master/service-rpc/if/TCLIService.thrift).

- > TCLIService.Iface implemented by: 
org.apache.hive.service.cli.thrift.ThriftCLIService class.

- > ThriftCLIService subclassed by: 
org.apache.hive.service.cli.thrift.ThriftBinaryCLIService and org.apache.hive.service.cli.thrift.ThriftHttpCLIService for TCP mode and HTTP mode respectively.

- > org.apache.hive.service.cli.thrift.EmbeddedThriftBinaryCLIService class: Embedded mode for HS2. Don't get confused with embedded metastore, which is a different service (although the embedded mode concept is similar).

- > org.apache.hive.service.cli.session.HiveSessionImpl class: Instances of this class are created on the server side and managed by an org.apache.accumulo.tserver.TabletServer.SessionManager instance.

- > org.apache.hive.service.cli.operation.Operation class: Defines an operation (e.g., a query). Instances of this class are created on the server and managed by an org.apache.hive.service.cli.operation.OperationManager instance.

- > org.apache.hive.service.auth.HiveAuthFactory class: A helper used by both HTTP and TCP mode for authentication. Refer to [Setting Up HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2) for various authentication options, in particular [Authentication/Security Configuration](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2#SettingUpHiveServer2-Authentication/SecurityConfiguration) and [Cookie Based Authentication](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2#SettingUpHiveServer2-CookieBasedAuthentication).

### 5.2、Client Side

- > org.apache.hive.jdbc.HiveConnection class: Implements the java.sql.Connection interface (part of JDBC). An instance of this class holds a reference to a SessionHandle instance which is retrieved when making Thrift API calls to the server.

- > org.apache.hive.jdbc.HiveStatement class: Implements the java.sql.Statement interface (part of JDBC). The client (e.g., Beeline) calls the HiveStatement.execute() method for the query. Inside the execute() method, the Thrift client is used to make API calls.

- > org.apache.hive.jdbc.HiveDriver class: Implements the java.sql.Driver interface (part of JDBC). The core method is connect() which is used by the JDBC client to initiate a SQL connection.

### 5.3、Interaction between Client and Server

- > org.apache.hive.service.cli.SessionHandle class: Session identifier. Instances of this class are returned from the server and used by the client as input for Thrift API calls.

- > org.apache.hive.service.cli.OperationHandle class: Operation identifier. Instances of this class are returned from the server and used by the client to poll the execution status of an operation.

## 6、Resources

How to set up HS2: [Setting Up HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2)

HS2 clients: [HiveServer2 Clients](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients)

User interface:  [Web UI for HiveServer2](https://cwiki.apache.org/confluence/display/Hive/Setting+Up+HiveServer2#SettingUpHiveServer2-WebUIforHiveServer2)

Metrics:  [Hive Metrics](https://cwiki.apache.org/confluence/display/Hive/Hive+Metrics)

Cloudera blog on HS2: [http://blog.cloudera.com/blog/2013/07/how-hiveserver2-brings-security-and-concurrency-to-apache-hive/](http://blog.cloudera.com/blog/2013/07/how-hiveserver2-brings-security-and-concurrency-to-apache-hive/)