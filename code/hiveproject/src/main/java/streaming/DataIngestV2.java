package streaming;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.streaming.HiveStreamingConnection;
import org.apache.hive.streaming.StreamingConnection;
import org.apache.hive.streaming.StreamingException;
import org.apache.hive.streaming.StrictDelimitedInputWriter;

import java.util.ArrayList;

public class DataIngestV2 {
    public static void main(String[] args) throws StreamingException {
        //-------   MAIN THREAD  ------- //
        String dbName = "default";
        String tblName = "alerts";

        // static partition values
        ArrayList<String> partitionVals = new ArrayList<String>(2);
        partitionVals.add("Asia");
        partitionVals.add("India");
        HiveConf hiveConf = new HiveConf(DataIngestV2.class);
        hiveConf.addResource("thrift://zgg-server:9083");

        hiveConf.setVar(HiveConf.ConfVars.DYNAMICPARTITIONINGMODE, "nonstrict");
        hiveConf.setBoolVar(HiveConf.ConfVars.HIVE_ORC_DELTA_STREAMING_OPTIMIZATIONS_ENABLED, true);
        hiveConf.setBoolVar(HiveConf.ConfVars.METASTORE_CLIENT_CACHE_ENABLED, false);
        hiveConf.setVar(HiveConf.ConfVars.HIVE_TXN_MANAGER, "org.apache.hadoop.hive.ql.lockmgr.DbTxnManager");
        hiveConf.setBoolVar(HiveConf.ConfVars.HIVE_SUPPORT_CONCURRENCY, true);
        hiveConf.setBoolVar(HiveConf.ConfVars.METASTORE_EXECUTE_SET_UGI, true);

        // create delimited record writer whose schema exactly matches table schema
        StrictDelimitedInputWriter writer = StrictDelimitedInputWriter.newBuilder()
                .withFieldDelimiter(',')
                .build();
        // create and open streaming connection (default.src table has to exist already)
        StreamingConnection connection = HiveStreamingConnection.newBuilder()
                .withDatabase(dbName)
                .withTable(tblName)
                .withStaticPartitionValues(partitionVals)
                .withAgentInfo("example-agent-1")
                .withRecordWriter(writer)
                .withHiveConf(hiveConf)
                .connect();
        // begin a transaction, write records and commit 1st transaction
        connection.beginTransaction();
        connection.write("1,val1".getBytes());
        connection.write("2,val2".getBytes());
        connection.commitTransaction();
        // begin another transaction, write more records and commit 2nd transaction
        connection.beginTransaction();
        connection.write("3,val3".getBytes());
        connection.write("4,val4".getBytes());
        connection.commitTransaction();
        // close the streaming connection
        connection.close();

        // dynamic partitioning
//         create delimited record writer whose schema exactly matches table schema
        StrictDelimitedInputWriter dpwriter = StrictDelimitedInputWriter.newBuilder()
                .withFieldDelimiter(',')
                .build();
        // create and open streaming connection (default.src table has to exist already)
        StreamingConnection dpconnection = HiveStreamingConnection.newBuilder()
                .withDatabase(dbName)
                .withTable(tblName)
                .withAgentInfo("example-agent-1")
                .withRecordWriter(dpwriter)
                .withHiveConf(hiveConf)
                .connect();
        // begin a transaction, write records and commit 1st transaction
        dpconnection.beginTransaction();
        // dynamic partition mode where last 2 columns are partition values
        dpconnection.write("11,val11,Asia,China".getBytes());
        dpconnection.write("12,val12,Asia,India".getBytes());
        dpconnection.commitTransaction();
        // begin another transaction, write more records and commit 2nd transaction
        dpconnection.beginTransaction();
        dpconnection.write("13,val13,Europe,Germany".getBytes());
        dpconnection.write("14,val14,Asia,India".getBytes());
        dpconnection.commitTransaction();
        // close the streaming connection
        dpconnection.close();
    }


}
