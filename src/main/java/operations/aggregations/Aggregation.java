package operations.aggregations;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class Aggregation {
    public static void main(String[] args) throws Exception
    {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<String> data = env.readTextFile("/home/shubham/avg1");

        // month, category,product, profit,
        DataStream<Tuple4<String, String, String, Integer>> mapped = data.map(new Splitter());      // tuple  [June,Category5,Bat,12]
        //       [June,Category4,Perfume,10,1]
        mapped.keyBy(0).sum(3).writeAsText("/home/shubham/out1");

        mapped.keyBy(0).min(3).writeAsText("/home/shubham/out2"); //Note : flink saves only that field which is used in operation. Rest other fields are taken from anywhere say previous tuple, garbage etc. So, here we may find some inconsistency in other fields.

        mapped.keyBy(0).minBy(3).writeAsText("/home/shubham/out3"); // Note: above shortcoming is taken care by this. It keeps state of all the fields . So , here all fields corresponding to min profit will be correct

        mapped.keyBy(0).max(3).writeAsText("/home/shubham/out4");

        mapped.keyBy(0).maxBy(3).writeAsText("/home/shubham/out5");

        //Note: if the input stream is of object type. say class X{private String A; private long B; private String C;}, then we will have ot do something like
        // mapped.keyBy().sum("B"); etc



        // execute program
        env.execute("Aggregation");
    }

    // *************************************************************************
    // USER FUNCTIONS
    // *************************************************************************

    public static class Splitter implements MapFunction<String, Tuple4<String, String, String, Integer>> {
        public Tuple4<String, String, String, Integer> map(String value)         // 01-06-2018,June,Category5,Bat,12
        {
            String[] words = value.split(",");                             // words = [{01-06-2018},{June},{Category5},{Bat}.{12}
            // ignore timestamp, we don't need it for any calculations
            return new Tuple4<String, String, String, Integer>(words[1], words[2], words[3], Integer.parseInt(words[4]));
        }                                                  //    June    Category5      Bat               12
    }
    }
