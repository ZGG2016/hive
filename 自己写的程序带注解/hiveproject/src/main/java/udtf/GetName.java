package udtf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDTF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;

import java.util.ArrayList;
import java.util.List;

@Description(name="GetName",
        value="_FUNC_(str) - Returns the name this string contains.",
        extended = "Example:\n"
                + " > SELECT _FUNC_('mike:jackson') FROM src; \n")
public class GetName extends GenericUDTF {

    // 初始化，定义输出列名，也可以初始化参数
    @Override
    public StructObjectInspector initialize(StructObjectInspector argOIs) throws UDFArgumentException {

        List<String> fieldNames = new ArrayList<String>();
        List<ObjectInspector> fieldOIs = new ArrayList<ObjectInspector>();

        fieldNames.add("first_name");
        fieldOIs.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);

        fieldNames.add("last_name");
        fieldOIs.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);

        return ObjectInspectorFactory.getStandardStructObjectInspector(fieldNames, fieldOIs);
    }

    // 具体的处理逻辑
    @Override
    public void process(Object[] args) throws HiveException {
        String name = args[0].toString();
        // 把结果提交给 collector 输出
        forward(name.split(":"));
    }

    @Override
    public void close() throws HiveException {

    }
}
