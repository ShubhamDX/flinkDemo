package states;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

public class ListStateDemo {

    public static void main(String[] args) throws Exception
    {
        // set up the streaming execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<String> data = env.socketTextStream("localhost", 9090);

        DataStream<Tuple2<String,Long>> sum = data.map(new MapFunction<String, Tuple2<Long, String>>()
        {
            public Tuple2<Long, String> map(String s)
            {
                String[] words = s.split(",");
                return new Tuple2<Long, String>(Long.parseLong(words[0]), words[1]);
            }
        })
                .keyBy(0)
                .flatMap(new ListStateDemo.StatefulMap());
        sum.writeAsText("/home/shubham/state3");

        // execute program
        env.execute("State");
    }

    //RichFlatMapFunction implements RichFunction interface and provides 4 methods : open,close,getRuntimeContext, setRuntimeContext
    public static class StatefulMap extends RichFlatMapFunction<Tuple2<Long, String>, Tuple2<String,Long>>
    {
        private transient ValueState<Long> count;
        private transient ListState<Long> numbers;

        public void flatMap(Tuple2<Long, String> input, Collector<Tuple2<String,Long>> out)throws Exception //after processing of 10 records sum is emitted to output
        {
            Long currCount = count.value();
            Long currValue = Long.parseLong(input.f1);

            currCount += 1;

            count.update(currCount); //currCount and currSum are updated in state so that we can retrieve them for next tuple
            numbers.add(currValue);
            if (currCount >= 10)
            {
                Long sum = 0L;
                String numberStr = "";
                for(Long number: numbers.get()){
                    numberStr= numberStr + " "+ number;
                    sum = sum+number;
                }
                //emit sum of last 10 elements
                out.collect(new Tuple2<String,Long>(numberStr,sum));
                /* clear value */
                count.clear();
                numbers.clear();
            }
        }
        public void open(Configuration conf)
        {
            ListStateDescriptor<Long> descriptor =new ListStateDescriptor<Long>("numbers", Long.class);
            numbers = getRuntimeContext().getListState(descriptor);

            ValueStateDescriptor<Long> descriptor2 = new ValueStateDescriptor<Long>( "count",  Long.class, 0L);
            count = getRuntimeContext().getState(descriptor2);
        }
    }

}
