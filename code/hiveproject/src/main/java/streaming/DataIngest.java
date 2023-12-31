package streaming;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hive.hcatalog.streaming.*;

import java.util.ArrayList;
/*
create table alerts ( id int , msg string )
partitioned by (continent string, country string)
clustered by (id) into 5 buckets
stored as orc
tblproperties("transactional"="true");
 */
public class DataIngest {
    public static void main(String[] args) throws StreamingException, InterruptedException, ClassNotFoundException {
        //-------   MAIN THREAD  ------- //
        String dbName = "default";
        String tblName = "alerts";
        ArrayList<String> partitionVals = new ArrayList<String>(2);
        partitionVals.add("Asia");
        partitionVals.add("India");
        String serdeClass = "org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe";
        String[] fieldNames = {"id", "msg"};
        HiveEndPoint hiveEP = new HiveEndPoint("thrift://bigdata-cdh01.itcast.cn:9083", dbName, tblName, partitionVals);

        HiveConf hiveConf = new HiveConf();
        hiveConf.setVar(HiveConf.ConfVars.DYNAMICPARTITIONINGMODE, "nonstrict");
        hiveConf.setVar(HiveConf.ConfVars.HIVE_TXN_MANAGER, "org.apache.hadoop.hive.ql.lockmgr.DbTxnManager");
        hiveConf.setVar(HiveConf.ConfVars.HIVE_EXECUTION_ENGINE, "mr");
        hiveConf.setBoolVar(HiveConf.ConfVars.HIVE_SUPPORT_CONCURRENCY, true);
        hiveConf.setBoolVar(HiveConf.ConfVars.METASTORE_EXECUTE_SET_UGI, true);

        //-------   Thread 1  -------//
        StreamingConnection connection = hiveEP.newConnection(true, hiveConf);
        DelimitedInputWriter writer =
                new DelimitedInputWriter(fieldNames,",", hiveEP);
        TransactionBatch txnBatch = connection.fetchTransactionBatch(10, writer);

        ///// Batch 1 - First TXN
        txnBatch.beginNextTransaction();
        txnBatch.write("1,Hello streaming".getBytes());
        txnBatch.write("2,Welcome to streaming".getBytes());
        txnBatch.commit();

        if(txnBatch.remainingTransactions() > 0) {
            ///// Batch 1 - Second TXN
            txnBatch.beginNextTransaction();
            txnBatch.write("3,Roshan Naik".getBytes());
            txnBatch.write("4,Alan Gates".getBytes());
            txnBatch.write("5,Owen O’Malley".getBytes());
            txnBatch.commit();

            txnBatch.close();
            connection.close();
        }

        txnBatch = connection.fetchTransactionBatch(10, writer);

        ///// Batch 2 - First TXN
        txnBatch.beginNextTransaction();
        txnBatch.write("6,David Schorow".getBytes());
        txnBatch.write("7,Sushant Sowmyan".getBytes());
        txnBatch.commit();

        if(txnBatch.remainingTransactions() > 0) {
            ///// Batch 2 - Second TXN
            txnBatch.beginNextTransaction();
            txnBatch.write("8,Ashutosh Chauhan".getBytes());
            txnBatch.write("9,Thejas Nair".getBytes());
            txnBatch.commit();

            txnBatch.close();
        }
        connection.close();

        //-------   Thread 2  -------//
//        StreamingConnection connection2 = hiveEP.newConnection(true);
//        DelimitedInputWriter writer2 =
//                new DelimitedInputWriter(fieldNames,",", hiveEP);
//        TransactionBatch txnBatch2= connection.fetchTransactionBatch(10, writer2);
//
//        ///// Batch 1 - First TXN
//        txnBatch2.beginNextTransaction();
//        txnBatch2.write("21,Venkat Ranganathan".getBytes());
//        txnBatch2.write("22,Bowen Zhang".getBytes());
//        txnBatch2.commit();
//
//        ///// Batch 1 - Second TXN
//        txnBatch2.beginNextTransaction();
//        txnBatch2.write("23,Venkatesh Seetaram".getBytes());
//        txnBatch2.write("24,Deepesh Khandelwal".getBytes());
//        txnBatch2.commit();
//
//        txnBatch2.close();
//        connection.close();
//
//        txnBatch = connection.fetchTransactionBatch(10, writer);
//
//        ///// Batch 2 - First TXN
//        txnBatch.beginNextTransaction();
//        txnBatch.write("26,David Schorow".getBytes());
//        txnBatch.write("27,Sushant Sowmyan".getBytes());
//        txnBatch.commit();
//
//        txnBatch2.close();
//        connection2.close();

    }
}
