package udf;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDF;

@Description(name="GetLength",
        value="_FUNC_(str) - Returns the length of this string.",
        extended = "Example:\n"
                + " > SELECT _FUNC_('abc') FROM src; \n")
public class GetLength extends UDF {
    public int evaluate(String s) {
        return s.length();
    }
}
