package udf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.StringObjectInspector;

@Description(name = "GetLength",
        value = "_FUNC_(str) - Returns the length of this string.",
        extended = "Example:\n"
                + " > SELECT _FUNC_('abc') FROM src; \n")
public class GetLengthG extends GenericUDF {

    StringObjectInspector ss;

    // 初始化，可以校验参数个数
    @Override
    public ObjectInspector initialize(ObjectInspector[] arguments) throws UDFArgumentException {
        if (arguments.length > 1) {
            throw new UDFArgumentLengthException("GetLength Only take one argument");
        }

        ss = (StringObjectInspector) arguments[0];

        return ss;

    }

    // 具体的计算逻辑
    @Override
    public Object evaluate(DeferredObject[] arguments) throws HiveException {
        String s = ss.getPrimitiveJavaObject(arguments[0].get());

        if (s == null) {
            return 0;
        }

        return s.length();
    }

    @Override
    public String getDisplayString(String[] children) {
        return "GetLength";
    }
}
