package states.broadcaststate;

import java.util.Map;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

//broadcast.txt has complete list of employee records and broadcast_small has those employee records which are going to resign. We need to find which employees would be there in company in next month per department
//program counts employees per department while excluding employees from small file
public class EmpCount
{
    // broadcast state descriptor is specified by Map state descriptor
    public static final MapStateDescriptor<String, String> excludeEmpDescriptor =
            new MapStateDescriptor<String, String>("exclude_employ", BasicTypeInfo.STRING_TYPE_INFO, BasicTypeInfo.STRING_TYPE_INFO); // <String,String> 1st is type of key and 2nd is datatype of value


    public static void main(String[] args) throws Exception
    {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<String> excludeEmp =	env.socketTextStream("localhost", 9090);

        BroadcastStream<String> excludeEmpBroadcast =	excludeEmp.broadcast(excludeEmpDescriptor); // broadcasted to each node

        DataStream<Tuple2<String, Integer>> employees = env.readTextFile("/home/shubham/flink_inputs/broadcast.txt")
                .map(new MapFunction<String, Tuple2<String, String>>()
                {
                    public Tuple2<String, String> map(String value)
                    {
                        // dept, data
                        return new Tuple2<String, String>(value.split(",")[3], value);   // {(Purchase), (AXPM175755,Nana,Developer,Purchase,GH67D)}
                    }
                })
                .keyBy(0)
                .connect(excludeEmpBroadcast)  // will return a BroadcastConnectedStream on whic we do the computations
                .process(new ExcludeEmp());

        employees.writeAsText("/home/shubham/broadcast_out");
        env.execute("Broadcast Exmaple");
    }

    //for non keyed it should inherit BroadcastprocessFunction
    //params are 1- key type of input keyed stream, 2. input type of keyed(non-broadcast) side, 3. input type of broadcast side, 4. output type of operator
    public static class ExcludeEmp extends KeyedBroadcastProcessFunction<String,Tuple2<String, String>, String, Tuple2<String, Integer>>
    {
        private transient ValueState<Integer> countState;

        public void processElement(Tuple2<String, String> value, ReadOnlyContext ctx, Collector<Tuple2<String, Integer>> out)throws Exception //// deals with elements in non broadcasted side
        {
            Integer currCount = countState.value();
            // get card_id of current transaction
            final String cId = value.f1.split(",")[0];

            for (Map.Entry<String, String> cardEntry: ctx.getBroadcastState(excludeEmpDescriptor).immutableEntries())
            {
                final String excludeId = cardEntry.getKey();
                if (cId.equals(excludeId))
                    return;
            }

            countState.update(currCount+1);       // dept    , current sum
            out.collect(new Tuple2<String, Integer>(value.f0, currCount+1));
        }

        public void processBroadcastElement(String empData, Context ctx, Collector<Tuple2<String, Integer>> out) throws Exception // deals with elements in broadcasted side
        {
            String id = empData.split(",")[0];
            ctx.getBroadcastState(excludeEmpDescriptor).put(id, empData);
        }

        public void open(Configuration conf)
        {
            ValueStateDescriptor<Integer> desc = new ValueStateDescriptor<Integer>("", BasicTypeInfo.INT_TYPE_INFO, 0);
            countState = getRuntimeContext().getState(desc);
        }
    }
}
