package udaf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.udf.generic.*;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.IntObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.io.IntWritable;

@Description(name = "CountA",
        value = "_FUNC_(str) - Returns the number of string containing 'a'.",
        extended = "Example:\n"
                + " > SELECT _FUNC_('abc') FROM src; \n")
public class CountA extends AbstractGenericUDAFResolver {

    @Override
    public GenericUDAFEvaluator getEvaluator(GenericUDAFParameterInfo info) throws SemanticException {

        return new CountAEvaluator();
    }

    public static class CountAEvaluator extends GenericUDAFEvaluator{

        private IntWritable result;
        private IntObjectInspector partialCountAAggOI;

        @Override
        public ObjectInspector init(Mode mode, ObjectInspector[] parameters) throws HiveException {
            super.init(mode, parameters);
            if (mode == Mode.PARTIAL2 || mode == Mode.FINAL) {
                partialCountAAggOI = (IntObjectInspector)parameters[0];
            }

            result = new IntWritable(0);
            return PrimitiveObjectInspectorFactory.writableIntObjectInspector;
        }

        static class CountAAgg extends AbstractAggregationBuffer {
            int value;
        }

        // 创建一个用来存储临时聚合结果的对象
        @Override
        public AggregationBuffer getNewAggregationBuffer() throws HiveException {
            CountAAgg countAAgg = new CountAAgg();
            reset(countAAgg);
            return countAAgg;
        }

        @Override
        public void reset(AggregationBuffer agg) throws HiveException {
            ((CountAAgg)agg).value = 0;
        }

        // 处理一个新数据行，并存入聚合缓存
        @Override
        public void iterate(AggregationBuffer agg, Object[] parameters) throws HiveException {
            String item = parameters[0].toString();
            if (item.contains("a")){
                ((CountAAgg)agg).value++;
            }
        }

        // 返回当前聚合的结果
        @Override
        public Object terminatePartial(AggregationBuffer agg) throws HiveException {
            return terminate(agg);
        }

        // 合并由terminatePartial返回的部分聚合结果，到当前聚合操作下
        @Override
        public void merge(AggregationBuffer agg, Object partial) throws HiveException {
            ((CountAAgg)agg).value += partialCountAAggOI.get(partial);
        }

        // 返回聚合的最终结果给hive
        @Override
        public Object terminate(AggregationBuffer agg) throws HiveException {
            result.set(((CountAAgg) agg).value);
            return result;
        }
    }
}
