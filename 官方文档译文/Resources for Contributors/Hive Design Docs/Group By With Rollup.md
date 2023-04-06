# Group By With Rollup

References:

- [Original design doc](https://issues.apache.org/jira/secure/attachment/12437909/dp_design.txt) 
- [HIVE-2397](https://issues.apache.org/jira/browse/HIVE-2397)

## 1、Terminology

> (No) Map Aggr: Shorthand for whether the configuration variable hive.map.aggr is set to true or false, meaning mapside aggregation is allowed or not respectively.

> (No) Skew: Shorthand for whether the configuration variable hive.groupby.skewindata is set to true or false, meaning some columns have a disproportionate number of distinct values.

- (No) Map Aggr：是否配置 `hive.map.aggr` 为 true 或 false，表示允许或不允许 map 端聚合。

- (No) Skew：是否配置 `hive.groupby.skewindata` 为 true 或 false，表示一些列具有不成比例的不同值。

## 2、Design

> Before the rollup option was added to the group by operator, there were 4 different plans based on the 4 possible combinations of (No) Map Aggr and (No) Skew. These were built on and expanded to 6 plans as described below:

在将 rollup 选项添加到 group by 操作符之前，基于 (No) Map Aggr 和 (No) Skew 的 4 种可能组合，有 4 种不同的计划。

这些计划被构建，并扩展到以下 6 种计划:

### 2.1、Map Aggr & No Skew:

> This plan remains the same, only the implementation of the map-side hash-based aggregation operator was modified to handle the extra rows needed for rollup. The plan is as follows:

这个计划保持不变，只修改了 map 端基于哈希的聚合操作符的实现，以处理 rollup 所需的额外行。计划如下：

Mapper:

- Hash-based group by 操作符来执行部分聚合
- Reduce sink 操作符，执行部分聚合

> Hash-based group by operator to perform partial aggregations
> Reduce sink operator, performs some partial aggregations

Reducer:

- MergePartial（基于列表）group by 操作符来执行最终的聚合

> MergePartial (list-based) group by operator to perform final aggregations

### 2.2、Map Aggr & Skew

> Again, this plan remains the same, only the implementation of the map-side hash-based aggregation operator was modified to handle the extra rows needed for rollup. The plan is as follows:

同样，该计划保持不变，只修改了 map 端基于哈希的聚合操作符的实现，以处理 rollup 所需的额外行。计划如下：

Mapper 1:

- Hash-based group by 操作符来执行部分聚合
- Reduce sink operator to spray by the group by and distinct keys (if there is a distinct key) or a random number otherwise

> Hash-based group by operator to perform partial aggregations
> Reduce sink operator to spray by the group by and distinct keys (if there is a distinct key) or a random number otherwise

Reducer 1:

- Partials（基于列表）group by 操作符来执行进一步的聚合

> Partials (list-based) group by operator to perform further partial aggregations

Mapper 2:

- Reduce sink 操作符，执行一些部分聚合

> Reduce sink operator, performs some partial aggregations

Reducer 2:

- Final（基于列表）group by 操作符来执行最终的聚合

> Final (list-based) group by operator to perform final aggregations

> Note that if there are no group by keys or distinct keys, Reducer 1 and Mapper 2 are removed from the plan and the reduce sink operator in Mapper 1 does not spray

注意，如果没有 group by keys 或 distinct keys，Reducer 1 和 Mapper 2 会从计划中删除，Mapper 1 中的 reduce sink 操作符也不会 spray。

### 2.3、No Map Aggr & No Skew & No Rollup

> This plan is the case from pre-rollup version of group by where there is no Map Aggr and No Skew, I included it for completeness as it remains an option if rollup is not used. The plan is as follows:

这个计划是 group by 的预 rollup 版本的情况，其中没有 Map Aggr 和 No Skew，我包含它是为了完整性，因为如果没有使用 rollup，它仍然是一个选项。计划如下:

Mapper:

- Reduce sink 操作符，执行一些部分聚合

> Reduce sink operator, performs some partial aggregations

Reducer:

- Complete（基于列表）group by 操作符来执行所有的聚合

> Complete (list-based) group by operator to perform all aggregations

### 2.4、No Map Aggr & No Skew & With Rollup

> The plan is as follows:

计划如下：

Mapper 1:

- Reduce sink 操作符，不会执行任何部分聚合

> Reduce sink operator, does not perform any partial aggregations

Reducer 1:

- Hash-based group by 操作符，与以前的 mappers 中使用的非常相似

> Hash-based group by operator, much like the one used in the mappers of previous cases

Mapper 2:

- Reduce sink 操作符，执行一些部分聚合

> Reduce sink operator, performs some partial aggregations

Reducer 2:

- MergePartial（基于列表）group by 操作符执行剩余的聚合

> MergePartial (list-based) group by operator to perform remaining aggregations

### 2.5、No Map Aggr & Skew & (No Distinct or No Rollup)

> This plan is the same as was used for the case of No Map Aggr and Skew in the pre-rollup version of group by, for this cads when rollup is not used, or none of the aggregations make use of a distinct key. The implementation of the list-based group by operator was modified to handle the extra rows required for rollup if rollup is being used. The plan is as follows:

此计划与 group by 的预 rollup 版本中 No Map Aggr 和 Skew 时使用的计划相同，对于此cads，如果不使用 rollup，或者没有聚合使用不同的键。修改了基于列表的 group by 操作符的实现，以便在使用 rollup 时处理 rollup 所需的额外行。计划如下:

Mapper 1:

> Reduce sink operator to spray by the group by and distinct keys (if there is a distinct key) or a random number otherwise

Reducer 1:

> Partial1 (list-based) group by operator to perform partial aggregations, it makes use of the new list-based group by operator implementation for rollup if necessary

Mapper 2:

- Reduce sink 操作符，执行一些部分聚合

> Reduce sink operator, performs some partial aggregations

Reducer 2:

- Final（基于列表）group by 操作符执行剩余的聚合

> Final (list-based) group by operator to perform remaining aggregations

### 2.6、No Map Aggr & Skew & Distinct & Rollup

> This plan is used when there is No Map Aggr and Skew and there is an aggregation that involves a distinct key and rollup is being used. The plan is as follows:

当存在 No Map Aggr 和 Skew，且存在包含不同键的聚合并使用 rollup 时，使用此计划。计划如下:

Mapper 1:

> Reduce sink operator to spray by the group by and distinct keys (if there is a distinct key) or a random number otherwise

Reducer 1:

> Hash-based group by operator, much like the one used in the mappers of previous cases

Mapper 2:

> Reduce sink operator to spray by the group by and distinct keys (if there is a distinct key) or a random number otherwise

Reducer 2:

> Partials (list-based) group by operator to perform further partial aggregations

Mapper 3:

> Reduce sink operator, performs some partial aggregations

Reducer 3:

> Final (list-based) group by operator to perform final aggregations

> Note that if there are no group by keys or distinct keys, Reducer 2 and Mapper 3 are removed from the plan and the reduce sink operator in Mapper 2 does not spray. Also, note that the reason for Mapper 2 spraying is that if the skew in the data existed in a column that is not immediately nulled by the rollup (e.g. if we the group by keys are columns g1, g2, g3 in that order, we are concerned with the case where the skew exists in column g1 or g2) the skew may continue to exist after the hash aggregation, so we spray.

注意，如果没有 group by keys 或 distinct keys, Reducer 2 和 Mapper 3 将从计划中删除，Mapper 2 中的 reduce sink 操作符也不会spray。。。。