# GenericUDAFCaseStudy

[TOC]

## 1、Writing GenericUDAFs: A Tutorial

> User-Defined Aggregation Functions (UDAFs) are an excellent way to integrate advanced data-processing into Hive. Hive allows two varieties of UDAFs: simple and generic. Simple UDAFs, as the name implies, are rather simple to write, but incur performance penalties because of the use of [Java Reflection](http://java.sun.com/docs/books/tutorial/reflect/index.html), and do not allow features such as variable-length argument lists. Generic UDAFs allow all these features, but are perhaps not quite as intuitive to write as Simple UDAFs.

**用户定义的聚合函数(UDAFs)**是一种很好的方式，**将高级数据处理方法集成到 Hive**。

UDAFs **有两类 ：simple 、 generic**。

simple UDAFs 编写起来相当简单，但是由于使用了 Java 反射，会导致性能损失，并且不允许诸如变长参数列表之类的特性。

generic UDAFs 允许所有这些特性，但编写起来可能不如 simple UDAFs 那么直观。

> This tutorial walks through the development of the histogram() UDAF, which computes a histogram with a fixed, user-specified number of bins, using a constant amount of memory and time linear in the input size. It demonstrates a number of features of Generic UDAFs, such as a complex return type (an array of structures), and type checking on the input. The assumption is that the reader wants to write a UDAF for eventual submission to the Hive open-source project, so steps such as modifying the function registry in Hive and writing .q tests are also included. If you just want to write a UDAF, debug and deploy locally, see [this page](http://wiki.apache.org/hadoop/Hive/HivePlugins).

本教程介绍 `histogram()` UDAF 的开发，它使用**固定的、用户指定数量的箱计算直方图，使用恒定的内存，时间和输入大小的线性关系**。

它演示了 Generic UDAFs 的许多特性，例如复杂的返回类型(结构数组)和对输入的类型检查。

假设读者希望编写一个最终提交到 Hive 开源项目的 UDAF，因此还包括了修改 Hive 中的函数注册表和编写`.q`测试等步骤。

如果你只是想编写一个在本地调试和部署的 UDAF，请参阅此页。

> NOTE: In this tutorial, we walk through the creation of a histogram() function. Starting with the 0.6.0 release of Hive, this appears as the built-in function histogram_numeric().

注意:在本教程中，我们介绍了 `histogram()`函数的创建。从 Hive 0.6.0 版本开始，它作为内置函数`histogram_numeric()`出现。

### 1.1、Preliminaries

> Make sure you have the latest Hive trunk by running svn up in your Hive directory. More detailed instructions on downloading and setting up Hive can be found at [Getting Started](http://wiki.apache.org/hadoop/Hive/GettingStarted) . Your local copy of Hive should work by running build/dist/bin/hive from the Hive root directory, and you should have some tables of data loaded into your local instance for testing whatever UDAF you have in mind. For this example, assume that a table called normal exists with a single double column called val, containing a large number of random number drawn from the standard normal distribution.

通过在你的 Hive 目录中运行 `svn up`，确保拥有最新的 Hive trunk。

更多关于下载和设置 Hive 的详细说明可以在 Getting Started 找到。

你的本地 Hive 副本应该在从 Hive 根目录中运行`build/dist/bin/Hive` 来工作，并且应该将一些数据表加载到本地实例中，以便测试想要的UDAF。

对于本例，假设存在一个名为 normal 的表，其中有一个名为 val 的双列，包含服从标准正态分布的大量随机数。

> The files we will be editing or creating are as follows, relative to the Hive root:

我们将编辑或创建的如下文件，Hive root为相对目录:

file | meaning
---|:---
ql/src/java/org/apache/hadoop/hive/ql/udf/generic/GenericUDAFHistogram.java | the main source file, to be created by you. 【由你创建的main源文件】
ql/src/java/org/apache/hadoop/hive/ql/exec/FunctionRegistry.java | the function registry source file, to be edited by you to register our new histogram() UDAF into Hive's built-in function list. 【函数注册源文件，编辑它，以注册新的histogram()到hive内建的函数列表中】
ql/src/test/queries/clientpositive/udaf_histogram.q | a file of sample queries for testing histogram() on sample data, to be created by you.【在样本数据上测试histogram()的样本查询语句】
ql/src/test/results/clientpositive/udaf_histogram.q.out | the expected output from your sample queries, to be created by ant in a later step.【执行样本查询语句的输出】
ql/src/test/results/clientpositive/show_functions.q.out | the expected output from the SHOW FUNCTIONS Hive query. Since we're adding a new histogram() function, this expected output will change to reflect the new function. This file will be modified by ant in a later step. 【执行SHOW FUNCTIONS的输出。因为添加了一个新的histogram()函数，所以预期的输出将会改变以反映新的函数。ant将在后面的步骤中修改该文件。】

### 1.2、Writing the source

> This section gives a high-level outline of how to implement your own generic UDAF. For a concrete example, look at any of the existing UDAF sources present in ql/src/java/org/apache/hadoop/hive/ql/udf/generic/ directory.

这部分给出如何实现你自己的 generic UDAF。

一个具体的例子在 `ql/src/java/org/apache/hadoop/hive/ql/udf/generic/`

#### 1.2.1、Overview

> At a high-level, there are two parts to implementing a Generic UDAF. The first is to write a resolver class, and the second is to create an evaluator class. The resolver handles type checking and operator overloading (if you want it), and helps Hive find the correct evaluator class for a given set of argument types. The evaluator class then actually implements the UDAF logic. Generally, the top-level UDAF class extends the abstract base class org.apache.hadoop.hive.ql.udf.GenericUDAFResolver2, and the evaluator class(es) are written as static inner classes.

实现 Generic UDAF 有两个部分：

	第一个是编写一个 resolver 类

	第二个是创建一个 evaluator 类

resolver 处理**类型检查和操作符重载**(如果需要的话)，并**帮助 Hive 为给定的参数类型找到正确的 evaluator 类**。然后，evaluator 类**实现 UDAF 逻辑**。

通常，顶级 UDAF 类继承抽象基类 `org.apache.hadoop.hive.ql.udf.GenericUDAFResolver2`，

**并且 evaluator 类被编写为静态内部类**。

#### 1.2.2、Writing the resolver

> The resolver handles type checking and operator overloading for UDAF queries. The type checking ensures that the user isn't passing a double expression where an integer is expected, for example, and the operator overloading allows you to have different UDAF logic for different types of arguments.

resolver 处理类型检查和操作符重载。

例如，**类型检查确保用户不会在需要整数的地方传递双精度表达式，而运算符重载允许对不同类型的参数使用不同的 UDAF 逻辑**。

> The resolver class must extend org.apache.hadoop.hive.ql.udf.GenericUDAFResolver2 (see [#Resolver Interface Evolution](https://cwiki.apache.org/confluence/display/Hive/GenericUDAFCaseStudy#GenericUDAFCaseStudy-ResolverInterfaceEvolution) for backwards compatibility information). We recommend that you extend the AbstractGenericUDAFResolver base class in order to insulate your UDAF from future interface changes in Hive.

resolver 类必须继承 `org.apache.hadoop.hive.ql.udf.GenericUDAFResolver2`。

**建议继承 `AbstractGenericUDAFResolver` 基类**，以便将你的 UDAF 与将来在 Hive 中更改的接口隔离开来。

Look at one of the existing UDAFs for the *import*s you will need.

```java
#!Java
public class GenericUDAFHistogramNumeric extends AbstractGenericUDAFResolver {
  static final Log LOG = LogFactory.getLog(GenericUDAFHistogramNumeric.class.getName());
 
  @Override
  public GenericUDAFEvaluator getEvaluator(GenericUDAFParameterInfo info) throws SemanticException {
    // Type-checking goes here!
 
    return new GenericUDAFHistogramNumericEvaluator();
  }
 
  public static class GenericUDAFHistogramNumericEvaluator extends GenericUDAFEvaluator {
    // UDAF logic goes here!
  }
}
```

> The code above shows the basic skeleton of a UDAF. The first line sets up a Log object that you can use to write warnings and errors to be fed into the Hive log. The GenericUDAFResolver class has a single overridden method: getEvaluator, which receives information about how the UDAF is being invoked. Of most interest is info.getParameters(), which provides an array of type information objects corresponding to the SQL types of the invocation parameters. For the histogram UDAF, we want two parameters: the numeric column over which to compute the histogram, and the number of histogram bins requested. The very first thing to do is to check that we have exactly two parameters (lines 3-6 below). Then, we check that the first parameter has a primitive type, and not an array or map, for example (lines 9-13). However, not only do we want it to be a primitive type column, but we also want it to be numeric, which means that we need to throw an exception if a STRING type is given (lines 14-28). BOOLEAN is excluded because the "histogram" estimation problem can be solved with a simple COUNT() query. Lines 30-41 illustrate similar type checking for the second parameter to the histogram() UDAF – the number of histogram bins. In this case, we insist that the number of histogram bins is an integer.

上面的代码显示了 UDAF 的基本框架。

第一行设置了一个 Log 对象，你可以使用它将警告和错误写入 Hive 日志。

`GenericUDAFResolver`类有一个需要被重写的方法:`getEvaluator`，它接收关于如何调用 UDAF 的信息。

`info.getParameters()` 提供了与调用参数的 SQL 类型对应的类型信息对象数组。

对于 histogram UDAF，需要两个参数：用于计算直方图的数值列，和请求的直方图箱的数量。

首先要做的是检查是否恰好有两个参数(下面第3-6行)。

然后，检查第一个参数是否是基本类型，而不是数组或map(例如，第9-13行)。

然而，我们不仅希望它是一个基本类型的列，而且还希望它是数值型的，这意味着如果给定的是字符串类型，我们需要抛出一个异常(第14-28行)。

之所以排除 BOOLEAN，是因为“直方图”估计问题可以通过简单的 COUNT() 查询来解决。

第30-41行演示了对 histogram() UDAF 的第二个参数进行相似类型检查——直方图箱的数量。在本例中，直方图箱的数量是一个整数。

```java
#!Java
  public GenericUDAFEvaluator getEvaluator(GenericUDAFParameterInfo info) throws SemanticException {
    TypeInfo [] parameters = info.getParameters();
    if (parameters.length != 2) {
      throw new UDFArgumentTypeException(parameters.length - 1,
          "Please specify exactly two arguments.");
    }
     
    // validate the first parameter, which is the expression to compute over
    if (parameters[0].getCategory() != ObjectInspector.Category.PRIMITIVE) {
      throw new UDFArgumentTypeException(0,
          "Only primitive type arguments are accepted but "
          + parameters[0].getTypeName() + " was passed as parameter 1.");
    }
    switch (((PrimitiveTypeInfo) parameters[0]).getPrimitiveCategory()) {
    case BYTE:
    case SHORT:
    case INT:
    case LONG:
    case FLOAT:
    case DOUBLE:
      break;
    case STRING:
    case BOOLEAN:
    default:
      throw new UDFArgumentTypeException(0,
          "Only numeric type arguments are accepted but "
          + parameters[0].getTypeName() + " was passed as parameter 1.");
    }
 
    // validate the second parameter, which is the number of histogram bins
    if (parameters[1].getCategory() != ObjectInspector.Category.PRIMITIVE) {
      throw new UDFArgumentTypeException(1,
          "Only primitive type arguments are accepted but "
          + parameters[1].getTypeName() + " was passed as parameter 2.");
    }
    if( ((PrimitiveTypeInfo) parameters[1]).getPrimitiveCategory()
        != PrimitiveObjectInspector.PrimitiveCategory.INT) {
      throw new UDFArgumentTypeException(1,
          "Only an integer argument is accepted as parameter 2, but "
          + parameters[1].getTypeName() + " was passed instead.");
    }
    return new GenericUDAFHistogramNumericEvaluator();
  }
```

> A form of operator overloading could be implemented here. Say you had two completely different histogram construction algorithms – one designed for working with integers only, and the other for double data types. You would then create two separate Evaluator inner classes, and depending on the type of the input expression, would return the correct one as the return value of the Resolver class.

这里可以实现一种操作符重载。假设你有两种完全不同的直方图构造算法：一种仅处理整数，另一种处理 double 数据类型。

然后，将创建两个独立的 Evaluator 内部类，根据输入表达式的类型，将返回正确的一个作为 Resolver 类的返回值。

> CAVEAT: The histogram function is supposed to be used in a manner resembling the following: SELECT histogram_numeric(age, 30) FROM employees;, which means estimate the distribution of employee ages using 30 histogram bins. However, within the resolver class, there is no way of telling if the second argument is a constant or an integer column with a whole bunch of different values. Thus, a pathologically twisted user could write something like: SELECT histogram_numeric(age, age) FROM employees;, assuming that age is an integer column. Of course, this makes no sense whatsoever, but it would validate correctly in the Resolver type-checking code above.

注意:直方图函数应该以如下方式使用:`SELECT histogram_numeric(age, 30) FROM employees;`，

这表示使用 30 个直方图箱来估计雇员年龄的分布。但是，**在 resolver 类中，没有办法知道第二个参数是一个常数列还是一个带有一大堆不同值的整型列**。

因此，病态扭曲的用户可以编写如下内容：`SELECT histogram_numeric(age, age) FROM employees;`，

假设 age 是一个整数列。当然，这没有任何意义，但是它可以在上面的 Resolver 类型检查代码中正确地验证。

> We'll deal with this problem in the Evaluator.

我们将在 Evaluator 中处理这个问题。

#### 1.2.3、Writing the evaluator

> All evaluators must extend from the abstract base class org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator. This class provides a few abstract methods that must be implemented by the extending class. These methods establish the processing semantics followed by the UDAF. The following is the skeleton of an Evaluator class.

**所有 evaluators 都必须继承`org.apache.hadoop.hive.ql.udf.generic.GenericUDAFEvaluator`抽象基类**。这个类提供了一些必须由子类实现的抽象方法。这些方法在 UDAF 后建立处理语义。

下面是 Evaluator 类的框架。

```java
#!Java
  public static class GenericUDAFHistogramNumericEvaluator extends GenericUDAFEvaluator {
 
    // For PARTIAL1 and COMPLETE: ObjectInspectors for original data
    private PrimitiveObjectInspector inputOI;
    private PrimitiveObjectInspector nbinsOI;
 
    // For PARTIAL2 and FINAL: ObjectInspectors for partial aggregations (list of doubles)
    private StandardListObjectInspector loi;
 
 
    @Override
    public ObjectInspector init(Mode m, ObjectInspector[] parameters) throws HiveException {
      super.init(m, parameters);
      // return type goes here
    }
 
    @Override
    public Object terminatePartial(AggregationBuffer agg) throws HiveException {
      // return value goes here
    }
 
    @Override
    public Object terminate(AggregationBuffer agg) throws HiveException {
      // final return value goes here
    }
 
    @Override
    public void merge(AggregationBuffer agg, Object partial) throws HiveException {
    }
 
    @Override
    public void iterate(AggregationBuffer agg, Object[] parameters) throws HiveException {
    }
 
    // Aggregation buffer definition and manipulation methods
    static class StdAgg implements AggregationBuffer {
    };
 
    @Override
    public AggregationBuffer getNewAggregationBuffer() throws HiveException {
    }
 
    @Override
    public void reset(AggregationBuffer agg) throws HiveException {
    }   
  }
```

> What do all these functions do? The following is a brief summary of each function, in (roughly) chronological order of being called. It's very important to remember that the computation of your aggregation must be arbitrarily divisible over the data. Think of it like writing a divide-and-conquer algorithm where the partitioning of the data is completely out of your control and handled by Hive. More formally, given any subset of the input rows, you should be able to compute a partial result, and also be able to merge any pair of partial results into another partial result. This naturally makes it difficult to port over many existing algorithms, but should guarantee researchers jobs for quite some time.

这些函数是做什么的？下面是每个函数的简要介绍，按调用的(大致)时间顺序。

重要的是，聚合的计算必须是可以任意分割的数据。可以把它想象成编写一个分治算法，其中数据的划分完全不受你的控制，而是由 Hive 来处理。

更正式地说，给定输入行的任何子集，你应该能够计算一个部分结果，并且能够将任意一对部分结果合并为另一个部分结果。

这自然使得移植到许多现有算法上变得困难，但应该可以保证研究人员在相当长的一段时间内工作。

Function | Purpose
---|:---
init | Called by Hive to initialize an instance of your UDAF evaluator class.【由Hive调用，初始化一个你的UDAF evaluator类】
getNewAggregationBuffer | Return an object that will be used to store temporary aggregation results.【返回一个用来存储临时聚合结果的对象】
iterate | Process a new row of data into the aggregation buffer 【处理一个新数据行，并存入聚合缓存】
terminatePartial | Return the contents of the current aggregation in a persistable way. Here persistable means the return value can only be built up in terms of Java primitives, arrays, primitive wrappers (e.g. Double), Hadoop Writables, Lists, and Maps. Do NOT use your own classes (even if they implement java.io.Serializable), otherwise you may get strange errors or (probably worse) wrong results.【以一种持久化的方式，返回当前聚合的结果的内容。这里持久化的意思是返回值只能按照Java原语、数组、原语包装器(例如Double)、Hadoop Writables、列表和映射来构建。不要使用你自己的类(即使它们实现了java.io.Serializable)，否则可能会得到奇怪的错误或(可能更糟)错误的结果。】
merge | Merge a partial aggregation returned by terminatePartial into the current aggregation 【合并由terminatePartial返回的部分聚合结果，到当前聚合操作下】
terminate | Return the final result of the aggregation to Hive 【返回聚合的最终结果给hive】

> For writing the histogram() function, the following is the strategy that was adopted.

下面是写 `histogram()` 函数，采用的策略。

##### 1.2.3.1、getNewAggregationBuffer

> The aggregation buffer for a histogram is a list of (x,y) pairs that represent the histogram's bin centers and heights. In addition, the aggregation buffer also stores two integers with the maximum number of bins (a user-specified parameter), and the current number of bins used. The aggregation buffer is initialized to a 'not ready' state with the number of bins set to 0. This is because Hive makes no distinction between a constant parameter supplied to a UDAF and a column from a table; thus, we have no way of knowing how many bins the user wants in their histogram until the first call to iterate().

**直方图的聚合缓冲区是一组 (x,y) 对的列表，它们表示直方图的箱中心和高度**。

此外，聚合缓冲区**还存储两个整数，箱的最大数值(用户指定的参数)和当前使用的箱数**。

聚合缓冲区被初始化为 'not ready' 状态，此时箱数量为0。这是因为 Hive 不区别对提供给 UDAF 的常量参数和表中的列。

因此，我们**无法知道用户希望在其直方图中有多少个箱，直到第一次调用`iterate()`**。

##### 1.2.3.2、iterate

> The first thing we do in iterate() is to check whether the histogram object in our aggregation buffer is initialized. If it is not, we parse our the second argument to iterate(), which is the number of histogram bins requested by the user. We do this exactly once and initialize the histogram object. Note that error checking is performed here – if the user supplied a negative number or zero for the number of histogram bins, a HiveException is thrown at this point and computation terminates.

**在 `iterate()` 中要做的第一件事是检查聚合缓冲区中的直方图对象是否已初始化**。

如果没有，我们将解析第二个参数，第二个参数是用户请求的直方图箱的数量。

我们只执行一次，并初始化直方图对象。

注意，**此处执行错误检查**----如果用户为直方图箱的数量提供了负数或零，则此时抛出 HiveException ，并终止计算。

> Next, we parse out the actual input data item (a number) and add it to our histogram estimation in the aggregation buffer. See the GenericUDAFHistogramNumeric.java file for details on the heuristic used to construct a histogram.

**接下来，我们解析出实际的输入数据项(一个数字)，并将其添加到聚合缓冲区中的直方图估计中**。

##### 1.2.3.3、terminatePartial

> The current histogram approximation is serialized as a list of DoubleWritable objects. The first two doubles in the list indicate the maximum number of histogram bins specified by the user and number of bins current used. The remaining entries are (x,y) pairs from the current histogram approximation.

**当前的直方图近似被序列化为 DoubleWritable 对象的列表**。

列表中的前两个双精度表示：

	用户指定的直方图箱的最大数量

	当前使用的箱的数量

	其余的条目是当前直方图近似的 (x,y) 对

##### 1.2.3.4、merge

> At this point, we have a (possibly uninitialized) histogram estimation, and have been requested to merge it with another estimation performed on a separate subset of the rows. If N is the number of histogram bins specified by the user, the current heuristic first builds a histogram with all 2N bins from both estimations, and then iteratively merges the closest pair of bins until only N bins remain.

在这一点上，我们**有一个(可能未初始化的)直方图估计，并将它与另一个行子集上执行的估计合并**。

如果 N 是用户指定的直方图箱数，则当前首先构建一个直方图，包含所有 2N 个估计的箱，然后迭代地合并最接近的一对箱，直到只剩下 N 个箱。

##### 1.2.3.5、terminate

> The final return type from the histogram() function is an array of (x,y) pairs representing histogram bin centers and heights. These can be {{explode()}}ed into a separate table, or parsed using a script and passed to Gnuplot (for example) to visualize the histogram.

**`histogram()` 函数的最终返回类型是一个 (x,y) 对数组，表示直方图箱中心和高度**。

可以将它们 {{explode()}}ed 单独的表中，或者使用脚本进行解析并传递给 Gnuplot(举例)，以实现直方图的可视化。

### 1.3、Modifying the function registry

> Once the code for the UDAF has been written and the source file placed in ql/src/java/org/apache/hadoop/hive/ql/udf/generic, it's time to modify the function registry and incorporate the new function into Hive's list of functions. This simply involves editing ql/src/java/org/apache/hadoop/hive/ql/exec/FunctionRegistry.java to import your UDAF class and register it's name.

**编写完 UDAF 的代码，并将源文件放入 `ql/src/java/org/apache/hadoop/hive/ql/udf/generic`之后，就该修改函数注册表，并将新函数合并到 Hive 的函数列表中**。

这只需要编辑 `ql/src/java/org/apache/hadoop/hive/ql/exec/FunctionRegistry.java`来导入你的 UDAF 类，并注册它的名称。

> Please note that you will have to run the following command to update the output of the show functions Hive call:

注意：你必须运行如下命令，来更新调用 `show functions` 产生的输出：

	ant test -Dtestcase=TestCliDriver -Dqfile=show_functions.q -Doverwrite=true

### 1.4、Compiling and running

	ant package
	build/dist/bin/hive

### 1.5、Creating the tests

> System-level tests consist of writing some sample queries that operate on sample data, generating the expected output from the queries, and making sure that things don't break in the future in terms of expected output. Note that the expected output is passed through diff with the actual output from Hive, so nondeterministic algorithms will have to compute some sort of statistic and then only keep the most significant digits (for example).

系统级测试包括编写一些对样本数据进行操作的样例查询、从查询中生成预期的输出，以及确保将来在预期输出方面不会中断。

请注意，预期的输出是通过 `diff` 与来自 Hive 的实际输出传递的，因此不确定性算法将不得不计算某种统计数据，然后只保留最有效的数字(举例)。

> These are the simple steps needed for creating test cases for your new UDAF/UDF:

以下是为你的新 UDAF/UDF 创建测试用例所需的简单步骤:

> 1.Create a file in ql/src/test/queries/clientpositive/udaf_XXXXX.q where XXXXX is your UDAF's name.

- 1.在 `ql/src/test/queries/clientpositive/udaf_XXXXX.q` 中创建文件。XXXXX 是你的 UDAF 的名字。

> 2.Put some queries in the .q file – hopefully enough to cover the full range of functionality and special cases.

- 2.在`.q`文件中添加一些查询----希望这些查询能够涵盖所有的功能和特殊情况。

> 3.For sample data, put your own in hive/data/files and load it using LOAD DATA LOCAL INPATH..., or reuse one of the files already there (grep for LOAD in the queries directory to see table names).

- 3.把你自己的样本数据放入在 `hive/data/files`，使用`LOAD DATA LOCAL INPATH...`加载它，或者重用已经存在的文件之一(grep用于在查询目录中加载，以查看表名)。

> 4.touch ql/src/test/results/clientpositive/udaf_XXXX.q.out

- 4.`touch ql/src/test/results/clientpositive/udaf_XXXX.q.out`

> 5.Run the following command to generate the output into the .q.out result file.

- 5.运行以下命令，以在`.q.out`中生成输出结果。

	ant test -Dtestcase=TestCliDriver -Dqfile=udaf_XXXXX.q -Doverwrite=true

> 6.Run the following command to make sure your test runs fine.

- 6.运行以下命令，以确保你的测试正常运行。

	ant test -Dtestcase=TestCliDriver -Dqfile=udaf_XXXXX.q

## 2、Checklist for open source submission

> Create an account on the Hive JIRA (https:--issues.apache.org-jira-browse-HIVE), create an issue for your new patch under the Query Processor component. Solicit discussion, incorporate feedback.

- 在 Hive JIRA 上创建一个帐户(https:--issues.apache.org-jira-browse-HIVE)，在查询处理器组件下为你的新补丁创建一个 issue 。征求讨论，加入反馈。

> Create your UDAF, integrate it into your local Hive copy.

- 创建你的UDAF，并将其集成到你的本地 Hive 副本中。

> Run ant package from the Hive root to compile Hive and your new UDAF.

- Hive 根目录运行 `ant package` 来编译 Hive 和你的新 UDAF。

> Create .q tests and their corresponding .q.out output.

- 创建`.q`测试及其相应的`.q.out`输出。

> Modify the function registry if adding a new function.

- 如果添加新函数，请修改函数注册表。

> Run ant checkstyle and examine build/checkstyle/checkstyle-errors.html, ensure that your source files conform to the Sun Java coding convention (with the 100 character line length exception).

- 运行 `ant checkstyle`，并检查`build/checkstyle/checkstyle-error.html`，确保你的源文件符合 Sun Java 编码约定(有100个字符的行长度异常)。

> Run ant test, ensure that tests pass.

- 运行 `ant test`，确保测试通过。

> Run svn up, ensure no conflicts with the main repository.

- 运行 `svn up`，确保与主存储库没有冲突。

> Run svn add for whatever new files you have created.

- 为创建的任何新文件运行 `svn add`。

> Ensure that you have added .q and .q.out tests.

- 确保添加了 `.q` 和 `.q.out` 测试。

> Ensure that you have run the .q tests for all new functionality.

- 确保已经为所有新功能运行了`.q`测试。

> If adding a new UDAF, ensure that show_functions.q.out has been updated. Run ant test -Dtestcase=TestCliDriver -Dqfile=show_functions.q -Doverwrite=true to do this.

- 如果添加一个新的 UDAF，请确保已更新 `show_functions.q.out`。运行`ant test -Dtestcase=TestCliDriver -Dqfile=show_functions.q -Doverwrite=true`来更新。

> Run svn diff > HIVE-NNNN.1.patch from the Hive root directory, where NNNN is the issue number the JIRA has assigned to you.

- 运行 `svn diff > HIVE-NNNN.1.patch`，其中 NNNN 是 JIRA 分配给你的问题号。

> Attach your file to the JIRA issue, describe your patch in the comments section.

- 将你的文件附加到 JIRA 问题上，在评论部分描述你的补丁。

> Ask for a code review in the comments.

- 要求在注释中进行代码复查。

> Click Submit patch on your issue after you have completed the steps above.

- 完成上述步骤后，单击问题上的 Submit patch。

> It is also advisable to watch your issue to monitor new comments.

- 你也可以观察你的问题，观察新的评论。

## 3、Tips, Tricks, Best Practices

> Hive can have unexpected behavior sometimes. It is best to first run ant clean if you're seeing something weird, ranging from unexplained exceptions to strings being incorrectly double-quoted.

- Hive 有时会有意想不到的行为。如果你看到一些奇怪的情况，从无法解释的异常到字符串被错误地双引号引用，最好首先运行 `ant clean`。

> When serializing the aggregation buffer in a terminatePartial() call, if your UDAF only uses a few variables to represent the buffer (such as average), consider serializing them into a list of doubles, for example, instead of complicated named structures.

- 在一个 `terminatePartial()` 调用中，序列化聚合缓冲区时，如果你的 UDAF 只使用几个变量来表示缓冲区(例如average)，那么可以考虑将它们序列化为一个双精度的列表，而不是复杂的命名结构。

> Strongly cast generics wherever you can.

- 在任何可能的地方强 cast 泛型。

> Abstract core functionality from multiple UDAFs into its own class. Examples are histogram_numeric() and percentile_approx(), which both use the same core histogram estimation functionality.

- 将核心功能从多个 UDAFs 抽象到自己的类中。例如 `histogram_numeric()` 和 `percentile_approx()`，它们都使用相同的核心直方图估计功能。

> If you're stuck looking for an algorithm to adapt to the terminatePartial/merge paradigm, divide-and-conquer and parallel algorithms are predictably good places to start.

- 如果你一直在寻找一种算法来适应 terminatePartial/merge 范例，分治法和并行算法是很好的起点。

> Remember that the tests do a diff on the expected and actual output, and fail if there is any difference at all. An example of where this can fail horribly is a UDAF like ngrams(), where the output is a list of sorted (word,count) pairs. In some cases, different sort implementations might place words with the same count at different positions in the output. Even though the output is correct, the test will fail. In these cases, it's better to output (for example) only the counts, or some appropriate statistic on the counts, like the sum.

- 请记住，测试的输出和实际的输出有差异，如果存在任何差异，则会失败。这样做可能失败的一个例子是像`ngrams()`这样的 UDAF，它的输出是一个有序(单词、计数)对的列表。在某些情况下，不同的排序实现可能会将计数相同的单词放在输出中的不同位置。即使输出是正确的，测试也会失败。在这些情况下，最好只输出(例如)计数，或有关计数的一些适当的统计信息，如总和。

### 3.1、Resolver Interface Evolution

> Old interface org.apache.hadoop.hive.ql.udf.GenericUDAFResolver was deprecated as of the 0.6.0 release. The key difference between GenericUDAFResolver and GenericUDAFResolver2 interface is the fact that the latter allows the evaluator implementation to access extra information regarding the function invocation such as the presence of DISTINCT qualifier or the invocation with the wildcard syntax such as FUNCTION(*). UDAFs that implement the deprecated GenericUDAFResolver interface will not be able to tell the difference between an invocation such as FUNCTION() or FUNCTION(*) since the information regarding specification of the wildcard is not available. Similarly, these implementations will also not be able to tell the difference between FUNCTION(EXPR) vs FUNCTION(DISTINCT EXPR) since the information regarding the presence of the DISTINCT qualifier is also not available.

旧接口 `org.apache.hadoop.hive.ql.udf.GenericUDAFResolver` 在 0.6.0 发行版时就已被弃用。

`GenericUDAFResolver` 和 `GenericUDAFResolver2` 接口之间的关键区别在于：后者允许 evaluator 实现访问关于函数调用的额外信息，比如 DISTINCT 限定符的存在或使用通配符语法(如`FUNCTION(*)`)的调用。

实现弃用的 `GenericUDAFResolver` 接口的 UDAFs 将不能区分调用之间的区别，如 `FUNCTION()` 或 `FUNCTION(*)`，因为关于通配符规范的信息不可用。

类似地，这些实现也不能区分 `FUNCTION(EXPR)` 和 `FUNCTION(DISTINCT EXPR)`之间的区别，因为关于 DISTINCT 限定符的存在的信息也不可用。

> Note that while resolvers which implement the GenericUDAFResolver2 interface are provided the extra information regarding the presence of DISTINCT qualifier of invocation with the wildcard syntax, they can choose to ignore it completely if it is of no significance to them. The underlying data filtering to compute DISTINCT values is actually done by Hive's core query processor and not by the evaluator or resolver; the information is provided to the resolver only for validation purposes. The AbstractGenericUDAFResolver base class offers an easy way to transition previously written UDAF implementations to migrate to the new resolver interface without having to re-write the implementation since the change from implementing GenericUDAFResolver interface to extending AbstractGenericUDAFResolver class is fairly minimal. (There may be issues with implementations that are part of an inheritance hierarchy since it may not be easy to change the base class.)

注意，尽管实现 `GenericUDAFResolver2` 接口的 resolvers 被提供了关于使用通配符语法的 DISTINCT 调用限定符存在的额外信息，但如果它对它们没有意义，它们可以选择完全忽略它。

计算 DISTINCT 值的底层数据过滤实际上是由 Hive 的核心查询处理器完成的，而不是由 evaluator 或 resolver；

提供给 resolver 的信息仅用于验证目的。

`AbstractGenericUDAFResolver` 基类提供了一种简单的方法来转变以前写的 UDAF 实现迁移到新的 resolver 接口，无需重写实现，从实现 `GenericUDAFResolver` 接口到继承 `AbstractGenericUDAFResolver` 类的变化是几乎最小。(作为继承层次结构的一部分的实现可能会有问题，因为更改基类可能不容易。)