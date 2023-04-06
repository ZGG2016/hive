# LanguageManual XPathUDF

[TOC]

> Documentation for Built-In User-Defined Functions Related To XPath

内建的和 XPath 相关的 UDFs。

## 1、UDFs

**xpath, xpath_short, xpath_int, xpath_long, xpath_float, xpath_double, xpath_number, xpath_string**

- 使用 XPath 表达式解析 XML 数据的函数
- 从 0.6.0 版本开始

> Functions for parsing XML data using XPath expressions.
> Since version: 0.6.0

### 1.1、Overview

> The xpath family of UDFs are wrappers around the Java XPath library javax.xml.xpath provided by the JDK. The library is based on the XPath 1.0 specification. Please refer to [http://java.sun.com/javase/6/docs/api/javax/xml/xpath/package-summary.html](http://java.sun.com/javase/6/docs/api/javax/xml/xpath/package-summary.html) for detailed information on the Java XPath library.

UDFs 的 xpath 家族是 JDK 提供的 Java XPath 库 javax.xml.xpath 的包装器。

这个库基于 XPath 1.0 规范。有关 Java XPath 库的详细信息，请参阅 http://java.sun.com/javase/6/docs/api/javax/xml/xpath/package-summary.html。

> All functions follow the form: xpath_*(xml_string, xpath_expression_string). The XPath expression string is compiled and cached. It is reused if the expression in the next input row matches the previous. Otherwise, it is recompiled. So, the xml string is always parsed for every input row, but the xpath expression is precompiled and reused for the vast majority of use cases.

所有函数都遵循这种形式：`xpath_*(xml_string, xpath_expression_string)`。

编译并缓存 XPath 表达式字符串。如果下一个输入行中的表达式与前一个匹配，则重用它。否则，它将被重新编译。所以，xml 字符串总是针对每个输入行进行解析，但是 xpath 表达式是预编译的，并在绝大多数用例中重用。

> Backward axes are supported. For example:

```sql
> select xpath ('<a><b id="1"><c/></b><b id="2"><c/></b></a>','/descendant::c/ancestor::b/@id') from t1 limit 1 ;
[1","2]
```

> Each function returns a specific Hive type given the XPath expression:

每个函数根据给定的 XPath 表达式返回一个特定的 Hive 类型：

- xpath returns a Hive array of strings.
- xpath_string returns a string.
- xpath_boolean returns a boolean.
- xpath_short returns a short integer.
- xpath_int returns an integer.
- xpath_long returns a long integer.
- xpath_float returns a floating point number.
- xpath_double,xpath_number returns a double-precision floating point number (xpath_number is an alias for xpath_double).

> The UDFs are schema agnostic - no XML validation is performed. However, malformed xml (e.g., <a><b>1</b></aa>) will result in a runtime exception being thrown.

UDFs 与模式无关：不执行 XML 验证。然而，格式错误的 xml(如，`<a><b>1</b></aa>`)将导致抛出运行时异常。

> Following are specifics on each xpath UDF variant.

下面是关于每个 xpath UDF 变体的详细说明。

### 1.2、xpath

> The xpath() function always returns a hive array of strings. If the expression results in a non-text value (e.g., another xml node) the function will return an empty array. There are 2 primary uses for this function: to get a list of node text values or to get a list of attribute values.

xpath() 函数返回一个字符串 hive 数组。

如果表达式的结果是非文本值(如，另一个xml节点)，函数将返回一个空数组。

这个函数有两个主要用途：获取节点文本值列表或属性值列表。

例如：

> Non-matching XPath expression:

没有匹配 XPath 表达式：

```sql
> select xpath('<a><b>b1</b><b>b2</b></a>','a/*') from src limit 1 ;
[]
```

> Get a list of node text values:

获取节点文本值列表

```sql
> select xpath('<a><b>b1</b><b>b2</b></a>','a/*/text()') from src limit 1 ;
[b1","b2]
```

> Get a list of values for attribute 'id':

获取属性 'id' 值的列表：

```sql
> select xpath('<a><b id="foo">b1</b><b id="bar">b2</b></a>','//@id') from src limit 1 ;
[foo","bar]
```

> Get a list of node texts for nodes where the 'class' attribute equals 'bb':

获得属性 'class' 等于 'bb' 的节点的节点文本列表：

```sql
> SELECT xpath ('<a><b class="bb">b1</b><b>b2</b><b>b3</b><c class="bb">c1</c><c>c2</c></a>', 'a/*[@class="bb"]/text()') FROM src LIMIT 1 ;
[b1","c1]
```

### 1.3、xpath_string

> The xpath_string() function returns the text of the first matching node.

xpath_string() 返回第一个匹配的节点的文本。

> Get the text for node 'a/b':

获得节点 'a/b' 的文本：

```sql
> SELECT xpath_string ('<a><b>bb</b><c>cc</c></a>', 'a/b') FROM src LIMIT 1 ;
bb
```

> Get the text for node 'a'. Because 'a' has children nodes with text, the result is a composite of text from the children.

获得节点 'a' 的文本。因为 'a' 有带有文本的子节点，结果就是子结点的文本的组合。

```sql
> SELECT xpath_string ('<a><b>bb</b><c>cc</c></a>', 'a') FROM src LIMIT 1 ;
bbcc
```

> Non-matching expression returns an empty string:

没有匹配表达式就返回空字符串：

```sql
> SELECT xpath_string ('<a><b>bb</b><c>cc</c></a>', 'a/d') FROM src LIMIT 1 ;
```

> Gets the text of the first node that matches '//b':

获得匹配 '//b' 的第一个节点的文本：

```sql
> SELECT xpath_string ('<a><b>b1</b><b>b2</b></a>', '//b') FROM src LIMIT 1 ;
b1
```

> Gets the second matching node:

获得匹配的第二个节点：

```sql
> SELECT xpath_string ('<a><b>b1</b><b>b2</b></a>', 'a/b[2]') FROM src LIMIT 1 ;
b2
```

> Gets the text from the first node that has an attribute 'id' with value 'b_2':

获得属性 'id' 等于 'b_2' 的第一个节点的文本：

```sql
> SELECT xpath_string ('<a><b>b1</b><b id="b_2">b2</b></a>', 'a/b[@id="b_2"]') FROM src LIMIT 1 ;
b2
```

### 1.4、xpath_boolean

> Returns true if the XPath expression evaluates to true, or if a matching node is found.

如果 XPath 表达式是 true，或者找到匹配的节点，就返回 true。

> Match found:

找到匹配：

```sql
> SELECT xpath_boolean ('<a><b>b</b></a>', 'a/b') FROM src LIMIT 1 ;
true
```

> No match found:

没有找到匹配：

```sql
> SELECT xpath_boolean ('<a><b>b</b></a>', 'a/c') FROM src LIMIT 1 ;
false
```

> Match found:

找到匹配：

```sql
> SELECT xpath_boolean ('<a><b>b</b></a>', 'a/b = "b"') FROM src LIMIT 1 ;
true
```

> No match found:

没有找到匹配：

```sql
> SELECT xpath_boolean ('<a><b>10</b></a>', 'a/b < 10') FROM src LIMIT 1 ;
false
```

### 1.5、xpath_short, xpath_int, xpath_long

> These functions return an integer numeric value, or the value zero if no match is found, or a match is found but the value is non-numeric.

这些函数返回一个整型数值。如果没有找到匹配，或者找到匹配但值是非数字的，则返回 0。

> Mathematical operations are supported. In cases where the value overflows the return type, then the maximum value for the type is returned.

支持数学运算。如果值溢出了返回类型，则返回类型的最大值。

> No match:

没有匹配：

```sql
> SELECT xpath_int ('<a>b</a>', 'a = 10') FROM src LIMIT 1 ;
0
```

> Non-numeric match:

没有数值匹配：

```sql
> SELECT xpath_int ('<a>this is not a number</a>', 'a') FROM src LIMIT 1 ;
0
> SELECT xpath_int ('<a>this 2 is not a number</a>', 'a') FROM src LIMIT 1 ;
0
```

> Adding values:

添加值：

```sql
> SELECT xpath_int ('<a><b class="odd">1</b><b class="even">2</b><b class="odd">4</b><c>8</c></a>', 'sum(a/*)') FROM src LIMIT 1 ;
15
> SELECT xpath_int ('<a><b class="odd">1</b><b class="even">2</b><b class="odd">4</b><c>8</c></a>', 'sum(a/b)') FROM src LIMIT 1 ;
7
> SELECT xpath_int ('<a><b class="odd">1</b><b class="even">2</b><b class="odd">4</b><c>8</c></a>', 'sum(a/b[@class="odd"])') FROM src LIMIT 1 ;
5
```

> Overflow:

溢出：

```sql
> SELECT xpath_int ('<a><b>2000000000</b><c>40000000000</c></a>', 'a/b * a/c') FROM src LIMIT 1 ;
2147483647
```

### 1.6、xpath_float, xpath_double, xpath_number

> Similar to xpath_short, xpath_int and xpath_long but with floating point semantics. Non-matches result in zero. However,
non-numeric matches result in NaN. Note that xpath_number() is an alias for xpath_double().

类似于 xpath_short、xpath_int 和 xpath_long，但具有浮点语义。没有匹配的结果为零。然而，非数值匹配的结果是 NaN。注意，xpath_number( )是 xpath_double() 的别名。

> No match:

没有匹配：

```sql
> SELECT xpath_double ('<a>b</a>', 'a = 10') FROM src LIMIT 1 ;
0.0
```

> Non-numeric match:

没有数值匹配：

```sql
> SELECT xpath_double ('<a>this is not a number</a>', 'a') FROM src LIMIT 1 ;
NaN
```

> A very large number:

一个非常大的数值：

```sql
SELECT xpath_double ('<a><b>2000000000</b><c>40000000000</c></a>', 'a/b * a/c') FROM src LIMIT 1 ;
8.0E19
```

## 2、UDAFs

## 3、UDTFs