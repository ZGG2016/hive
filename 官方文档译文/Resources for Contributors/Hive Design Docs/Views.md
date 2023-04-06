# Views

[TOC]

## 1、Use Cases

> Views ([http://issues.apache.org/jira/browse/HIVE-972](http://issues.apache.org/jira/browse/HIVE-972)) are a standard DBMS feature and their uses are well understood. A typical use case might be to create an interface layer with a consistent entity/attribute naming scheme on top of an existing set of inconsistently named tables, without having to cause disruption due to direct modification of the tables. More advanced use cases would involve predefined filters, joins, aggregations, etc for simplifying query construction by end users, as well as sharing common definitions within ETL pipelines.

视图是一种标准的 DBMS 特性，它们的用途是很容易理解的。

一个典型的用例可能是在现有的一组命名不一致的表上创建一个具有一致实体/属性命名方案的接口层，而不必由于直接修改表而造成中断。

更高级的用例包括预定义的过滤器、连接、聚合等，以简化最终用户的查询构造，以及在 ETL 管道中共享公共定义。

## 2、Scope

> At a minimum, we want to

至少，我们想这样做

> add queryable view support at the SQL language level (specifics of the scoping are under discussion in the Issues section below)

- 在 SQL 语言级别添加可查询视图支持

	- 不支持可更新视图

> updatable views will not be supported (see the [Updatable Views](https://cwiki.apache.org/confluence/display/Hive/UpdatableViews) proposal)
> make sure views and their definitions show up anywhere tables can currently be enumerated/searched/described

- 确保视图及其定义显示在当前可以枚举/搜索/描述表的任何位置

> where relevant, provide additional metadata to allow views to be distinguished from tables

- 在必要的情况下，提供额外的元数据，以便将视图与表区分开来

> Beyond this, we may want to

除此之外，我们可能还想

> expose metadata about view definitions and dependencies (at table-level or column-level) in a way that makes them consumable by metadata-driven tools

- 公开关于视图定义和依赖项的元数据(表级或列级)，使元数据驱动的工具能够使用这些元数据。任何位置

## 3、Syntax

	CREATE VIEW [IF NOT EXISTS] view_name [(column_name [COMMENT column_comment], ...) ]
	[COMMENT table_comment]
	AS SELECT ...
	 
	DROP VIEW view_name

## 4、Implementation Sketch

> The basics of view implementation are very easy due to the fact that Hive already supports subselects in the FROM clause.

由于 Hive 已经在 FROM 子句中支持了子选择，所以基本的视图实现非常简单。

> For CREATE VIEW v AS view-def-select, we extend SemanticAnalyzer to behave similarly to CREATE TABLE t AS select, except that we don't actually execute the query (we stop after plan generation). It's necessary to perform all of plan generation (even though we're not actually going to execute the plan) since currently some validations such as type compatibility-checking are only performed during plan generation. After successful validation, the text of the view is saved in the metastore (the simplest approach snips out the text from the parser's token stream, but this approach introduces problems described in the issues section below).

对于 `CREATE VIEW v AS view-def-select`，我们扩展了 SemanticAnalyzer，使其行为类似于 `CREATE TABLE t AS select`，只是我们实际上不执行查询(在计划生成之后停止)。

有必要执行所有的计划生成(即使我们实际上不打算执行计划)，因为目前一些验证，例如类型兼容性检查只在计划生成期间执行。

在成功验证之后，视图的文本被保存在 metastore 中(最简单的方法从解析器的令牌流中剪掉文本，但是这种方法会引入下面的问题一节中描述的问题)。

> For select ... from view-reference, we detect the view reference in SemanticAnalyzer.getMetaData, load the text of its definition from the metastore, parse it back into an AST, prepare a QBExpr to hold it, and then plug this into the referencing query's QB, resulting in a tree equivalent to select ... from (view-def-select); plan generation can then be carried out on the combined tree.

对 select ... from view-reference，我们在 `SemanticAnalyzer.getMetaData` 中检测视图引用，从 metastore 加载其定义的文本，将其解析回 AST，准备一个 QBExpr 来保存它，然后将其插入到引用查询的 QB 中，生成一个与 `select ... from (view-def-select);` 等价的树，然后可以在合并后的树上执行计划生成。

## 5、Issues

> Some of these are related to functionality/scope; others are related to implementation approaches. Opinions are welcome on all of them.

其中一些与功能/范围有关；其他则与实施方法有关。欢迎对所有这些问题发表意见。

### 5.1、Stored View Definition

> In SQL:200n, a view definition is supposed to be frozen at the time it is created, so that if the view is defined as select * from t, where t is a table with two columns a and b, then later requests to select * from the view should return just columns a and b, even if a new column c is later added to the table. This is implemented correctly by most DBMS products.

在SQL: 200n，视图定义应该在创建时冻结，如果视图定义为 `select * from t`，其中 t 是具有 a 列和 b 列的表，然后请求 `select * from the view` 应该返回列 a 和列 b，即使一个新列 c 是后来添加到表中。大多数DBMS产品都正确地实现了这一点。

> There are similar issues with other kinds of references in the view definition; for example, if a table or function name can be qualified, then the reference should be bound at the time the view is created.

视图定义中的其他类型的引用也有类似的问题；例如，如果表或函数名可以被限定，那么引用应该在创建视图时绑定。

> Implementing this typically requires expanding the view definition into an explicit form rather than storing the original view definition text directly. Doing this could require adding "unparse" support to the AST model (to be applied after object name resolution takes place), something which is not currently present (and which is also useful to have available in general).

实现这一点通常需要将视图定义扩展为显式的形式，而不是直接存储原始视图定义文本。

这样做可能需要向 AST 模型添加 “unparse” 支持(在进行对象名称解析后应用)，这是目前没有的(通常也很有用)。

> However, storing both the expanded form and the original view definition text as well can also be useful for both DESCRIBE readability as well as functionality (see later section on ALTER VIEW v RECOMPILE).

然而，同时存储扩展的形式和原始视图定义文本对于 DESCRIBE 的可读性和功能性也很有用。

> Update 7-Jan-2010: Rather than adding full-blown unparse support to the AST model, I'm taking a parser-dependent shortcut. ANTLR's TokenRewriteStream provides a way to substitute text for token subsequences from the original token stream and then regenerate a transformed version of the parsed text. So, during column resolution, we map an expression such as `"t.*"` to replacement text "t.c1, t.c2, t.c3". Then once all columns have been resolved, we regenerate the view definition using these mapped replacements. Likewise, an unqualified column reference such as "c" gets replaced with the qualified reference "t.c". The rest of the parsed text remains unchanged.

2010年1月7日更新：不是向 AST 模型添加全面的非解析支持，而是采用了依赖于解析器的快捷方式。

ANTLR 的 TokenRewriteStream 提供了一种方法，可以原始令牌流中用令牌子序列的文本替换，然后重新生成已解析文本的转换版本。

因此，在列解析期间，我们将像 `"t.*"` 这样的表达式映射到替换文本 "t.c1, t.c2, t.c3"。

然后，在解析完所有列之后，我们将使用这些映射替换重新生成视图定义。同样，像 “c” 这样的非限定列引用将被限定引用 “t.c” 替换。解析后的其余文本保持不变。

> This approach will break if we ever need to perform more drastic (AST-based) rewrites as part of view expansion in the future.

如果我们将来需要执行更剧烈的(基于AST)重写，作为视图扩展的一部分，这种方法就会失效。

### 5.2、Metastore Modeling

> The metastore model will need to be augmented in order to allow view definitions to be saved. An important issue to be resolved is whether to model this via inheritance, or just shoehorn views in as a special kind of table.

metastore 模型需要扩充，以允许保存视图定义。需要解决的一个重要问题是，是通过继承来进行建模，还是仅仅将视图作为一种特殊的表硬塞进去。

> With an inheritance model, views and base tables would share a common base class (here called ColumnSet following the convention in the Common Warehouse Metamodel for lack of a better term):

使用继承模型，视图和基表将共享一个公共基类

(这里称为ColumnSet，由于缺少更好的术语，遵循公共仓库元模型中的约定):

HiveViewInheritance.png

> For a view, most of the storage descriptor (everything other than the column names and types) would be irrelevant, so this model could be further refined with such discriminations.

对于视图，大多数存储描述符(除了列名和类型之外的所有内容)都是不相关的，因此这个模型可以通过这种区别进一步细化。

> View names and table names share the same namespace with respect to uniqueness (i.e. you can't have a table and a view with the same name), so the name key uniqueness would need to be specified at the base class level.

视图名和表名在唯一性方面共享相同的命名空间(例如，表和视图不能具有相同的名称)，因此名称键唯一性需要在基类级别指定。

> Alternately, if we choose to avoid inheritance, then we could just add a new viewText attribute to the existing Table class (leaving it null for base tables):

或者，如果我们选择避免继承，那么我们可以只添加一个新的 viewText 属性到现有的 Table 类(对基表保留null)：

HiveViewFlat.png

> (Storing the view definition as a table property may not work since property values are limited to VARCHAR(767), and view definitions may be much longer than that, so we'll need to use a LOB.)

(将视图定义存储为表属性可能无法工作，因为属性值被限制为 VARCHAR(767)，而视图定义可能要长得多，因此我们需要使用 LOB。)

> Comparison of the two approaches:

两种方法的比较：

	
- | Inheritance Model   |  Flat Model
---|:---|:---
JDO Support  | Need to investigate how well inheritance works for our purposes【需要调查继承对于我们的目的是如何工作的】 | Nothing special
Metadata queries from existing code/tools | Existing queries for tables will NOT include views in results; those that need to will have to be modified to reference base class instead【对表的现有查询不会在结果中包含视图；那些需要引用基类的则必须修改为引用基类】 | Existing queries for tables WILL include views in results; those that are not supposed to will need to filter them out【对表的现有的查询将在结果中包含视图；那些不应该出现的将需要过滤掉】
Metastore upgrade on deployment  |  Need to test carefully to make sure introducing inheritance doesn't corrupt existing metastore instances【需要仔细测试，以确保引入继承不会破坏现有的metastore实例】  | Nothing special, just adding a new attribute

> Update 30-Dec-2009: Based on a design review meeting, we're going to go with the flat model. Prasad pointed out that in the future, for materialized views, we may need the view definition to be tracked at the partition level as well, so that when we change the view definition, we don't have to discard existing materialized partitions if the new view result can be derived from the old one. So it may make sense to add the view definition as a new attribute of StorageDescriptor (since that is already present at both table and partition level).

2009年12月30日更新：基于设计回顾会议，我们将采用平面模型。Prasad 指出，在未来，对于物化视图，我们可能需要在分区级别跟踪视图定义，为了当我们改变视图定义时，我们不必放弃现有物化分区，如果新视图的结果可以来自旧的。因此，将视图定义添加为 StorageDescriptor 的新属性可能是有意义的(因为它已经在表和分区级别上出现了)。

> Update 20-Jan-2010: After further discussion with Prasad, we decided to put the view definition on the table object instead; for details, see discussion in [HIVE-972](https://issues.apache.org/jira/browse/HIVE-972). Also, per [HIVE-1068](https://issues.apache.org/jira/browse/HIVE-1068), we added an attribute to store the type (view, managed table, external table) for each table descriptor.

2010年1月20日更新：与 Prasad 进一步讨论后，我们决定将视图定义放在表对象上；详细信息请参见 HIVE-972 中的讨论。同样，对于 HIVE-1068，我们添加了一个属性来存储每个表描述符的类型(视图、托管表、外部表)。

### 5.3、Dependency Tracking

> It's necessary to track dependencies from a view to objects it references in the metastore:

从一个视图到它在 metastore 中引用的对象，跟踪依赖关系是必要的:

> tables: this is mandatory if we want DROP TABLE to be able to correctly CASCADE/RESTRICT to a referencing view

- 表：如果我们想要 DROP TABLE 能够正确 CASCADE/RESTRICT 到一个引用视图，这是强制性的。

> other views: same as tables

- 其他视图：与表相同

> columns: this is optional (useful for lineage inspection, but not required for implementing SQL features)

- 列：这是可选的(对于沿袭检查很有用，但是对于实现 SQL 特性不是必需的)

> temporary functions: we should disallow these at view creation unless we also want a concept of temporary view (or if it's OK for the referencing view to become invalid whenever the volatile function registry gets cleared)

- 临时函数：我们应该在创建这些视图禁止，除非我们也想要临时视图的概念(或者引用视图在 volatile 函数注册表被清除时失效)。

> any other objects? (e.g. udt's coming in as part of [http://issues.apache.org/jira/browse/HIVE-779](http://issues.apache.org/jira/browse/HIVE-779))

- 任何其他对象？(例如，udt 作为 [http://issues.apache.org/jira/browse/HIVE-779](http://issues.apache.org/jira/browse/HIVE-779) 的一部分出现)

> (Note that MySQL doesn't actually implement CASCADE/RESTRICT: it just ignores the keyword and drops the table unconditionally, leaving the view dangling.)

(注意，MySQL 实际上并没有实现 CASCADE/RESTRICT：它只是忽略关键字并无条件地删除表，让视图处于悬空状态。)

> Metastore object id's can be used for dependency modeling in order to avoid the need to update dependency records when an object is renamed. However, we'll need to decide what kinds of objects can participate in dependencies. For example, if we restrict it to just tables and views (and assuming we don't introduce inheritance for views), then we can use a model like the one below, in which the dependencies are tracked as (supplier,consumer) table pairs. (In this model, the TableDependency class is acting as an intersection table for implementing a many-to-many relationship between suppliers and consumers).

Metastore 对象 id 可以用于依赖项建模，以避免在重命名对象时需要更新依赖项记录。

然而，我们需要决定哪些类型的对象可以参与依赖关系。

例如，如果我们将其限制为表和视图(并且假设我们不为视图引入继承)，那么我们可以使用如下模型，其中依赖项被跟踪为 (supplier,consumer) 表对。

(在这个模型中，TableDependency 类充当一个交叉表，用于实现 suppliers 和 consumers 之间的多对多关系)。

HiveTableDependency.png

> However, if later we want to introduce persistent functions, or track column dependencies, this model will be insufficient, and we may need to introduce inheritance, with a DependencyParticipant base class from which tables, columns, functions etc all derive. (Again, need to verify that JDO inheritance will actually support what we want here.)

然而，如果以后我们想要引入持久函数，或者跟踪列依赖关系，这个模型将是不够的，我们可能需要引入继承，使用 DependencyParticipant 基类，表、列、函数等都是从这个基类派生的。

(同样，需要验证 JDO 继承是否真正支持我们这里想要的内容。)

> Update 30-Dec-2009: Based on a design review meeting, we'll start with the bare-minimum MySQL approach (with no metastore support for dependency tracking), then if time allows, add dependency analysis and storage, followed by CASCADE support. See HIVE-1073 and HIVE-1074.

2009年12月30日更新：基于一个设计回顾会议，我们将从最低限度的 MySQL 方法开始(没有对依赖跟踪的 metastore 支持)，然后如果时间允许，添加依赖分析和存储，然后是级联支持。

### 5.4、Dependency Invalidation

> What happens when an object is modified underneath a view? For example, suppose a view references a table's column, and then ALTER TABLE is used to drop or replace that column. Note that if the column's datatype changes, the view definition may remain meaningful, but the view's schema may need to be updated to match. Here are two possible options:

当一个对象在视图下面被修改时会发生什么？例如，假设一个视图引用了一个表的列，然后使用 ALTER table 删除或替换该列。

请注意，如果列的数据类型改变，视图定义可能仍然有意义，但视图的模式可能需要更新以匹配。以下是两个可能的选择:

> Strict: prevent operations which would invalidate or change the view in any way (and optionally to provide a CASCADE flag which requests that such views be dropped automatically). This is the approach taken by SQL:200n.

- Strict：防止以任何方式使视图失效或改变视图的操作(也可以提供一个 CASCADE 标志来请求自动删除这些视图)。这就是 SQL:200n 所采用的方法。

> Lenient: allow the update to proceed (and maybe warn the user of the impact), potentially leaving the view in an invalid state. Later, when an invalid view definition is referenced, throw a validation exception for the referencing query. This is the approach taken by MySQL. In the case of datatype changes, derived column datatypes already stored in metastore for referencing views would become stale until those views were recreated.

- Lenient: 允许更新继续进行(可能会警告用户影响)，可能会使视图处于无效状态。稍后，当引用无效的视图定义时，为引用查询抛出验证异常。这就是 MySQL 所采用的方法。在数据类型更改的情况下，已经存储在 metastore 中用于引用视图的派生列数据类型将变得陈旧，直到重新创建这些视图。

> Note that besides table modifications, other operations such as CREATE OR REPLACE VIEW have similar issues (since views can reference other views). The lenient approach provides a reasonable solution for the related issue of external tables whose schemas may be dynamic (not sure if we currently support this).

注意，除了表的修改，其他操作，如 CREATE OR REPLACE VIEW 也有类似的问题(因为视图可以引用其他视图)。

这种宽松的方法为外部表的相关问题提供了一种合理的解决方案，这些表的模式可能是动态的(不确定我们目前是否支持这种模式)。

> Update 30-Dec-2009: Based on a design review meeting, we'll start with the lenient approach, without any support for marking objects invalid in the metastore, then if time allows, follow up with strict support and possibly metastore support for tracking object validity. See HIVE-1077.

2009年12月30日更新：基于一个设计回顾会议，我们将从一个宽松的方法开始，不支持在 metastore 中标记对象无效，然后如果时间允许，接下来是严格的支持和 metastore 对跟踪对象有效性的支持。

### 5.5、View Modification

> In SQL:200n, there's no standard way to update a view definition. MySQL supports both

在 SQL:200n 中，没有更新视图定义的标准方法。MySQL 支持

- CREATE OR REPLACE VIEW v AS new-view-def-select
- ALTER VIEW v AS new-view-def-select

> Note that supporting view modification requires detection of cyclic view definitions, which should be invalid. Whether this detection is carried out at the time of view modification versus reference is dependent on the strict versus lenient approaches to dependency invalidation described above.

注意，支持视图修改需要循环视图定义检测，这应该是无效的。

此检测是否在视图修改和引用时执行，取决于上面描述的依赖无效的严格还是宽松方法。

> Update 30-Dec-2009: Based on a design review meeting, we'll start with an Oracle-style ALTER VIEW v RECOMPILE, which can be used to revalidate a view definition, as well as to re-expand the original definition for clauses such as `select *`. Then if time allows, we'll follow up with CREATE OR REPLACE VIEW support. (The latter is less important since we're going with the lenient invalidation model, making DROP and re-CREATE possible without having to deal with downstream dependencies.) See HIVE-1077 and HIVE-1078.

2009年12月30日更新：基于一次设计评审会议，我们将从 oracle 风格的 ALTER VIEW v RECOMPILE 开始，它可以用来重新验证视图定义，以及重新扩展 `select *` 等子句的原始定义。然后，如果时间允许，我们将继续提供创建或替换视图支持。(后者不那么重要，因为我们将使用宽松的无效模型，使得删除和重新创建成为可能，而不必处理下游的依赖关系。)

### 5.6、Fast Path Execution

> For select * from t, hive supports fast-path execution (skipping Map/Reduce). Is it important for this to work for select * from v as well?

对于 `select * from t`，hive 支持快速路径执行(跳过 Map/Reduce)。这对于 `select * from v` 也很重要吗？

- 2009年12月30日更新：根据 JIRA 的反馈，我们将把这个问题留给基于过滤和预测的快速通道。

- 2010年12月6日更新：Hive 的新 "auto local mode" 功能解决了这个问题。

> Update 30-Dec-2009: Based on feedback in JIRA, we'll leave this as dependent on getting the fast-path working for the underlying filters and projections.

> Update 6-Dec-2010: This one is addressed by Hive's new "auto local mode" feature.

### 5.7、ORDER BY and LIMIT in view definition

> SQL:200n prohibits ORDER BY in a view definition, since a view is supposed to be a virtual (unordered) table, not a query alias. However, many DBMS's ignore this rule; for example, MySQL allows ORDER BY, but ignores it in the case where it is superceded by an ORDER BY in the query. Should we prevent ORDER BY? This question also applies to the LIMIT clause.

SQL:200n 在视图定义中禁止了 ORDER BY，因为视图应该是一个虚拟的(无序的)表，而不是查询别名。

然而，许多 DBMS 忽略了这条规则；例如，MySQL 允许 ORDER BY，但是在查询中被 ORDER BY 取代的情况下忽略它。我们应该阻止订购吗？这个问题也适用于限制条款。

> Update 30-Dec-2009: Based on feedback in JIRA, ORDER BY is important as forward-looking to materialized views. LIMIT may be less important, but we should probably support it too for consistency.

2009年12月30日更新：根据 JIRA 的反馈，ORDER BY 是重要的，因为它具有前瞻性。LIMIT 或许不那么重要，但为了保持一致性，我们也应该支持它。

### 5.8、Underlying Partition Dependencies

> Update 30-Dec-2009: Prasad pointed out that even without supporting materialized views, it may be necessary to provide users with metadata about data dependencies between views and underlying table partitions so that users can avoid seeing inconsistent results during the window when not all partitions have been refreshed with the latest data. One option is to attempt to derive this information automatically (using an overconservative guess in cases where the dependency analysis can't be made smart enough); another is to allow view creators to declare the dependency rules in some fashion as part of the view definition. Based on a design review meeting, we will probably go with the automatic analysis approach once dependency tracking is implemented. The analysis will be performed on-demand, perhaps as part of describing the view or submitting a query job against it. Until this becomes available, users may be able to do their own analysis either via empirical lineage tools or via view->table dependency tracking metadata once it is implemented. See HIVE-1079.

2009年12月30日更新：普拉萨德指出，即使没有支持物化视图，可能需要为用户提供关于数据的元数据视图和底层表分区之间的依赖关系，这样用户可以避免在窗口期间看到不一致的结果，当不是所有分区都刷新最新的数据。

一种选择是尝试自动得出这些信息(在依赖关系分析不够聪明的情况下，使用过于保守的猜测)；另一种方法是允许视图创建者以某种方式声明依赖规则，作为视图定义的一部分。

基于设计评审会议，一旦实现了依赖关系跟踪，我们可能会使用自动分析方法。分析将按需执行，可能作为描述视图或针对视图提交查询作业的一部分。在此之前，用户可以通过经验沿袭工具或视图->表依赖跟踪元数据来进行自己的分析。

> Update 1-Feb-2011: For the latest on this, see [PartitionedViews](https://cwiki.apache.org/confluence/display/Hive/PartitionedViews).

2011年2月1日更新：关于这个的最新更新，请参阅 PartitionedViews。

## 6、Metastore Upgrades

> Since we are adding new columns to the TBLS table in the metastore schema, existing metastore deployments will need to be upgraded. There are two ways this can happen.

因为我们在 metastore 模式的 TBLS 表中添加了新列，所以需要升级现有的 metastore 部署。

这有两种可能。

### 6.1、Automatic ALTER TABLE

> If the following property is set in the Hive configuration file, JDO will notice the difference between the persistent schema and the model and ALTER the tables automatically:

如果在 Hive 配置文件中设置了以下属性，JDO 会注意到持久化模式和模型之间的差异，并自动修改表:

```xml
<property>
	<name>datanucleus.autoCreateSchema</name>
	<value>true</value>
</property>
```

### 6.2、Explicit ALTER TABLE

> However, if the datanucleus.autoCreateSchema property is set to false, then the ALTER statements must be executed explicitly. (An administrator may have set this property for safety in production configurations.)

但是，如果 datanucleus.autoCreateSchema 属性设置为 false，则必须显式执行 ALTER 语句。(管理员可能出于安全考虑在生产配置中设置了此属性。)

> In this case, execute a script such as the following against the metastore database:

在这种情况下，对 metastore 数据库执行如下脚本：

```sql
ALTER TABLE TBLS ADD COLUMN VIEW_ORIGINAL_TEXT MEDIUMTEXT;
ALTER TABLE TBLS ADD COLUMN VIEW_EXPANDED_TEXT MEDIUMTEXT;
ALTER TABLE TBLS ADD COLUMN TBL_TYPE VARCHAR(128);
```

> The syntax here is for MySQL, so you may need to adjust it (particularly for CLOB datatype).

这里的语法是针对 MySQL 的，所以可能需要调整它(特别是针对 CLOB 数据类型)。

> Note that it should be safe to execute this script and continue operations BEFORE upgrading Hive; the old Hive version will simply ignore/nullify the columns it doesn't recognize.

注意，在升级 Hive 之前，执行这个脚本并继续操作应该是安全的；旧的 Hive 版本会简单地忽略/取消它不认识的列。

### 6.3、Existing Row UPDATE

> After the tables are altered, the new columns will contain NULL values for existing rows describing previously created tables. This is correct for VIEW_ORIGINAL_TEXT and VIEW_EXPANDED_TEXT (since views did not previously exist), but is incorrect for the TBL_TYPE column introduced by HIVE-1068. The new Hive code is capable of handling this (automatically filling in the correct value for the new field when a descriptor is retrieved), but it does not "fix" the stored rows. This could be an issue if in the future other tools are used to retrieve information directly from the metastore database rather than accessing the metastore API.

修改表之后，新列将包含描述先前创建的表的现有行的 NULL 值。

这对于 VIEW_ORIGINAL_TEXT 和 VIEW_EXPANDED_TEXT 是正确的(因为视图以前不存在)，但是对于 HIVE-1068 引入的 TBL_TYPE 列是不正确的。

新的 Hive 代码能够处理这个问题(当检索到描述符时，自动为新字段填写正确的值)，但是它不会“修复”存储的行。

如果将来使用其他工具直接从 metastore 数据库检索信息，而不是访问 metastore API，这可能会成为一个问题。

> The script below can be used to fix existing rows after the tables have been altered. It should be run AFTER all Hive instances directly accessing the metastore database have been upgraded (otherwise new null values could slip in and remain forever). For safety, it is view-aware just in case a CREATE VIEW statement has already been executed, meaning it can be rerun any time after the upgrade.

下面的脚本可用于在修改表后修复现有行。

它应该在所有直接访问 metastore 数据库的 Hive 实例升级后运行(否则新的空值可能会插入并永远保留)。

为了安全起见，它是视图感知的，以防已经执行了 CREATE VIEW 语句，这意味着它可以在升级后的任何时间重新运行。

```sql
UPDATE TBLS SET TBL_TYPE='MANAGED_TABLE'
WHERE VIEW_ORIGINAL_TEXT IS NULL
AND NOT EXISTS(
    SELECT * FROM TABLE_PARAMS
    WHERE TABLE_PARAMS.TBL_ID=TBLS.TBL_ID
    AND PARAM_KEY='EXTERNAL'
    AND PARAM_VALUE='TRUE'
);
UPDATE TBLS SET TBL_TYPE='EXTERNAL_TABLE'
WHERE EXISTS(
    SELECT * FROM TABLE_PARAMS
    WHERE TABLE_PARAMS.TBL_ID=TBLS.TBL_ID
    AND PARAM_KEY='EXTERNAL'
    AND PARAM_VALUE='TRUE'
);
```

> For MySQL, note that the "safe updates" feature will need to be disabled since these are full-table updates.

对于 MySQL，注意“安全更新”功能需要禁用，因为这些是全表更新。