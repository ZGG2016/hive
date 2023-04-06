# FilterPushdownDev

[TOC]

## 1、Introduction

> This document explains how we are planning to add support in Hive's optimizer for pushing filters down into physical access methods. This is an important optimization for minimizing the amount of data scanned and processed by an access method (e.g. for an indexed key lookup), as well as reducing the amount of data passed into Hive for further query evaluation.

本文档解释了我们计划如何在 Hive 优化器中添加对下推过滤到物理访问方法的支持。

这是一个重要的优化，可以**最小化访问方法扫描和处理的数据量**(例如索引的键查找)，也可以减少传递到 Hive 中用于进一步查询评估的数据量。

## 2、Use Cases

> Below are the main use cases we are targeting.

下面是我们主要的**目标用例**：

- 将过滤下推到 Hive 的内建存储格式，如，RCFile
- 将过滤下推到存储处理器，如，HBase handler
- 将过滤下推到索引访问计划，一旦索引的框架添加到 Hive

> Pushing filters down into Hive's builtin storage formats such as RCFile
> Pushing filters down into storage handlers such as the [HBase handler](https://cwiki.apache.org/confluence/display/Hive/HBaseIntegration) ([http://issues.apache.org/jira/browse/HIVE-1226](http://issues.apache.org/jira/browse/HIVE-1226))
> Pushing filters down into index access plans once an indexing framework is added to Hive ([http://issues.apache.org/jira/browse/HIVE-417](http://issues.apache.org/jira/browse/HIVE-417))

## 3、Components Involved

> There are a number of different parts to the overall effort.

整个工作有许多不同的部分。

> Propagating the result of Hive's existing predicate pushdown. Hive's optimizer already takes care of the hard work of pushing predicates down through the query plan (controlled via configuration parameter hive.optimize.ppd=true/false). The "last mile" remaining is to send the table-level filters down into the corresponding input formats.

- 传播 Hive 现有谓词下推的结果。

	Hive 优化器通过查询计划已处理了下推谓词的繁重工作(通过配置参数 `hive.optimize.ppd=true/false` 控制)。

	剩下的“最后一步”是将表级的过滤器发送到相应的输入格式中。

> Selection of a primary filter representation to be passed to input formats. This representation needs to be neutral (independent of the access plans which will use it) and loosely coupled with Hive (so that storage handlers can choose to minimize their dependencies on Hive internals).

- 选择要传递到输入格式的主过滤器表示形式。

	这种表示需要是中立的(独立于将要使用它的访问计划)，并且与 Hive 松耦合（这样存储处理器可以选择最小化它们对 Hive 内部的依赖）。

> Helper classes for interpreting the primary representation. Many access plans will need to analyze filters in a similar fashion, e.g. decomposing conjunctions and detecting supported column comparison patterns. Hive should provide sharable utilities for such cases so that they don't need to be duplicated in each access method's code.

- 用于解释主要表示的辅助类。

	许多访问计划将需要以类似的方式分析过滤器，例如，分解连接和检测支持的列比较模式。

	Hive 应该为这种情况提供可共享的实用程序，这样它们就不需要在每个访问方法的代码中重复。

> Converting filters into a form specific to the access method. This part is dependent on the particular access method; e.g. for HBase, it involves converting the filter condition into corresponding calls to set up an [HBase scan](http://hbase.apache.org/docs/r0.20.5/api/org/apache/hadoop/hbase/client/Scan.html) object.

- 将过滤器转换为特定于访问方法的形式。

	这部分依赖于特定的访问方法；

	例如，对于 HBase，它涉及到将过滤条件转换为对应的调用来建立 HBase scan 对象。

## 4、Primary Filter Representation

> To achieve the loosest possible coupling, we are going to use a string as the primary representation for the filter. In particular, the string will be in the form produced when Hive unparses an ExprNodeDesc, e.g.

为了实现最松散的耦合，我们将使用字符串作为过滤器的主要表示形式。

特别地，当 Hive unparses ExprNodeDesc 时，该字符串将产生，例如:

	((key >= 100) and (key < 200))

> In general, this comes out as valid SQL, although it may not always match the original SQL exactly, e.g.

通常，这是有效的 SQL，尽管它可能并不总是与原始 SQL 完全匹配，例如。

	cast(x as int)

成为

	UDFToInteger(x)

> Column names in this string are unqualified references to the columns of the table over which the filter operates, as they are known in the Hive metastore. These column names may be different from those known to the underlying storage; for example, the HBase storage handler maps Hive column names to HBase column names (qualified by column family). Mapping from Hive column names is the responsibility of the code interpreting the filter string.

此字符串中的列名是对过滤器操作的表的列的非限定引用，正如它们在 Hive metastore 中所知的那样。

这些列名可能与底层存储已知的列名不同；例如，HBase 存储处理器将 Hive 列名映射到 HBase 列名(由列族限定)。从 Hive 列名的映射是解释筛选器字符串的代码的责任。

## 5、Other Filter Representations

> As mentioned above, we want to avoid duplication in code which interprets the filter string (e.g. parsing). As a first cut, we will provide access to the ExprNodeDesc tree by passing it along in serialized form as an optional companion to the filter string. In followups, we will provide parsing utilities for the string form.

如上所述，我们希望避免在解释过滤器字符串(例如解析)的代码中出现重复。

作为第一步，我们将提供对 ExprNodeDesc 树的访问，方法是将它以序列化的形式作为过滤器字符串的可选伙伴传递。

在接下来的内容中，我们将提供字符串形式的解析实用程序。

> We will also provide an IndexPredicateAnalyzer class capable of detecting simple [sargable](http://en.wikipedia.org/wiki/Sargable) subexpressions in an ExprNodeDesc tree. In followups, we will provide support for discriminating and combining more complex indexable subexpressions.

我们还将提供一个能够在 ExprNodeDesc 树中检测简单 sargable 子表达式的 IndexPredicateAnalyzer 类。

在后续，我们将提供对更复杂的可索引子表达式的识别和组合的支持。

```java
public class IndexPredicateAnalyzer
{
  public IndexPredicateAnalyzer();
 
  /**
 * Registers a comparison operator as one which can be satisfied
 * by an index search.  Unless this is called, analyzePredicate
 * will never find any indexable conditions.
   *
 * @param udfName name of comparison operator as returned
 * by either {@link GenericUDFBridge#getUdfName} (for simple UDF's)
 * or udf.getClass().getName() (for generic UDF's).
   */
  public void addComparisonOp(String udfName);
 
  /**
 * Clears the set of column names allowed in comparisons.  (Initially, all
 * column names are allowed.)
   */
  public void clearAllowedColumnNames();
 
  /**
 * Adds a column name to the set of column names allowed.
   *
 * @param columnName name of column to be allowed
   */
  public void allowColumnName(String columnName);
 
  /**
 * Analyzes a predicate.
   *
 * @param predicate predicate to be analyzed
   *
 * @param searchConditions receives conditions produced by analysis
   *
 * @return residual predicate which could not be translated to
 * searchConditions
   */
  public ExprNodeDesc analyzePredicate(
    ExprNodeDesc predicate,
    final List<IndexSearchCondition> searchConditions);
 
  /**
 * Translates search conditions back to ExprNodeDesc form (as
 * a left-deep conjunction).
   *
 * @param searchConditions (typically produced by analyzePredicate)
   *
 * @return ExprNodeDesc form of search conditions
   */
  public ExprNodeDesc translateSearchConditions(
    List<IndexSearchCondition> searchConditions);
}
 
public class IndexSearchCondition
{
  /**
 * Constructs a search condition, which takes the form
 * <pre>column-ref comparison-op constant-value</pre>.
   *
 * @param columnDesc column being compared
   *
 * @param comparisonOp comparison operator, e.g. "="
 * (taken from GenericUDFBridge.getUdfName())
   *
 * @param constantDesc constant value to search for
   *
 * @Param comparisonExpr the original comparison expression
   */
  public IndexSearchCondition(
    ExprNodeColumnDesc columnDesc,
    String comparisonOp,
    ExprNodeConstantDesc constantDesc,
    ExprNodeDesc comparisonExpr);
}
```

## 6、Filter Passing

> The approach for passing the filter down to the input format will follow a pattern similar to what is already in place for pushing column projections down.

将过滤器向下传递到输入格式的方法将遵循与下推列投影的模式类似的模式。

> org.apache.hadoop.hive.serde2.ColumnProjectionUtils encapsulates the pushdown communication

- `org.apache.hadoop.hive.serde2.ColumnProjectionUtils` 封装了下推通信

> classes such as HiveInputFormat call ColumnProjectionUtils to set the projection pushdown property (READ_COLUMN_IDS_CONF_STR) on a jobConf before instantiating a RecordReader

- 像 HiveInputFormat 这样的类在实例化 RecordReader 之前调用 ColumnProjectionUtils 在 jobConf 上设置投影下推属性(READ_COLUMN_IDS_CONF_STR)

> the factory method for the RecordReader calls ColumnProjectionUtils to access this property

- RecordReader 的工厂方法调用 ColumnProjectionUtils 来访问此属性。

> For filter pushdown:

对于过滤器下推：

> HiveInputFormat sets properties hive.io.filter.text (string form) and hive.io.filter.expr.serialized (serialized form of ExprNodeDesc) in the job 
conf before calling getSplits as well as before instantiating a record reader

- 在 job 配置中，HiveInputFormat 设置属性 `hive.io.filter.text` (字符串形式)和 `hive.io.filter.expr.serialized` (ExprNodeDesc的序列化形式)。在调用 getSplits 之前，以及在实例化一个记录读取器之前。

> the storage handler's input format reads these properties and processes the filter expression

- 存储处理器的输入格式读取这些属性，并处理过滤器表达式

> there is a separate optimizer interaction for negotiation of filter decomposition (described in a later section)

- 有一个单独的优化器交互，用于协商过滤器分解(将在后面的部分中描述)

> Note that getSplits needs to be involved since the selectivity of the filter may prune away some of the splits which would otherwise be accessed. (In theory column projection could also influence the split boundaries, but we'll leave that for a followup.)

注意，需要使用 getSplits，因为过滤器的选择性可能会删除一些分片，否则可能会访问这些分片。

(理论上，列投影也可能影响切片边界，但我们将在后续讨论这个问题。)

## 7、Filter Collection

> So, where will HiveInputFormat get the filter expression to be passed down? Again, we can start with the pattern for column projections:

那么，HiveInputFormat 将从哪里得到要传递的过滤器表达式呢？同样，我们可以从列投影的模式开始:

> during optimization, org.apache.hadoop.hive.ql.optimizer.ColumnPrunerProcFactory's ColumnPrunerTableScanProc populates the pushdown information in TableScanOperator

- 在优化期间，TableScanOperator 中，`org.apache.hadoop.hive.ql.optimizer.ColumnPrunerProcFactory`的 ColumnPrunerTableScanProc 填充下推信息

> later, HiveInputFormat.initColumnsNeeded retrieves this information from the TableScanOperator

- 后来，`HiveInputFormat.initColumnsNeeded` 从 TableScanOperator 中检索此信息

> For filter pushdown, the equivalent is TableScanPPD in org.apache.hadoop.hive.ql.ppd.OpProcFactory. Currently, it calls createFilter, which collapsed expressions into a single expression called condn, and then sticks that on a new FilterOperator. We can call condn.getExprString() and store the result on TableScanOperator.

对于下推过滤器，等价的是 `org.apache.hadoop.hive.ql.ppd.OpProcFactory` 中的 TableScanPPD。

目前，它调用 createFilter，它将表达式分解为一个名为 condn 的表达式，然后将其插入到一个新的 FilterOperator 上。

我们可以调用 condn.getExprString() 并将结果存储在 TableScanOperator 上。

> Hive configuration parameter hive.optimize.ppd.storage can be used to enable or disable pushing filters down to the storage handler. This will be enabled by default. However, if hive.optimize.ppd is disabled, then this implicitly prevents pushdown to storage handlers as well.

Hive 配置参数 `hive.optimize.ppd.storage` 可用于启用或禁用下推入过滤器存储处理器。这在默认情况下是启用的。

然而，如果禁用了 hive.optimize.ppd，那么这也会隐式地阻止下推到存储处理器。

> We are starting with non-native tables only; we'll revisit this for pushing filters down to indexes and builtin storage formats such as RCFile.

我们只从非原生表开始；为了将过滤器下推到索引和内建的存储格式(如RCFile)，我们将重新讨论这个问题。

## 8、Filter Decomposition

> Consider a filter like

考虑这样的过滤器

	x > 3 AND upper(y) = 'XYZ'

> Suppose a storage handler is capable of implementing the range scanfor x > 3, but does not have a facility for evaluating {{upper(y) =
'XYZ'}}. In this case, the optimal plan would involve decomposing the filter, pushing just the first part down into the storage handler, and
leaving only the remainder for Hive to evaluate via its own executor.

假设一个存储处理器能够实现 x > 3 的范围扫描，但是没有计算 `{{upper(y) =
'XYZ'}}` 的工具。

在这种情况下，最佳计划将包括分解过滤器，将第一部分下推到存储处理器中，以及只留下剩下的部分让 Hive 通过它自己的执行器来计算。

> In order for this to be possible, the storage handler needs to be able to negotiate the decomposition with Hive. This means that Hive gives
the storage handler the entire filter, and the storage handler passes back a "residual": the portion that needs to be evaluated by Hive. A null residual indicates that the storage handler was able to deal with the entire filter on its own (in which case no FilterOperator is needed).

为了实现这一点，存储处理器需要能够与 Hive 协商分解。

这意味着 Hive 给存储处理器整个过滤器，存储处理器返回一个“剩余”：需要由 Hive 计算的部分。

剩余为空表示存储处理器能够单独处理整个过滤器（在这种情况下不需要 FilterOperator）。

> In order to support this interaction, we will introduce a new (optional) interface to be implemented by storage handlers:

为了支持这种交互，我们将引入一个新的(可选的)接口，由存储处理器实现：

```java
public interface HiveStoragePredicateHandler {
  public DecomposedPredicate decomposePredicate(
    JobConf jobConf,
    Deserializer deserializer,
    ExprNodeDesc predicate);
 
  public static class DecomposedPredicate {
    public ExprNodeDesc pushedPredicate;
    public ExprNodeDesc residualPredicate;
  }
}
```

> Hive's optimizer (during predicate pushdown) calls the decomposePredicate method, passing in the full expression and receiving back the decomposition (or null to indicate that no pushdown was possible). The pushedPredicate gets passed back to the storage handler's input format later, and the residualPredicate is attached to the FilterOperator.

Hive 的优化器(在谓词下推过程中)调用 decomposePredicate 方法，传入完整的表达式并接收分解(或null，表示不可能下推)。

pushhedpredicate 稍后被传递回存储处理器的输入格式，residualPredicate 被附加到 FilterOperator。

> It is assumed that storage handlers which are sophisticated enough to implement this interface are suitable for tight coupling to the ExprNodeDesc representation.

它假定能够实现这个接口的足够复杂的存储处理器适合于与 ExprNodeDesc 表示紧密耦合。

> Again, this interface is optional, and pushdown is still possible even without it. If the storage handler does not implement this interface, Hive will always implement the entire expression in the FilterOperator, but it will still provide the expression to the storage handler's input format; the storage handler is free to implement as much or as little as it wants.

同样，这个接口是可选的，即使没有它，下推也是可能的。如果存储处理器没有实现这个接口，Hive 将始终实现 FilterOperator 中的整个表达式，但它仍然会为存储处理器的输入格式提供表达式；存储处理程序可以自由地实现它想要的多少或多少。