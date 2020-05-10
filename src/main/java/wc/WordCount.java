package wc;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.UnsortedGrouping;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.util.Collector;

import java.util.Arrays;

public class WordCount {
    public static void main(String[] args) throws Exception {
        //set up execution environment. ExecutionEnvironemt is a context in which a program is executed. Local environment will call execution in current jvm and remote environment will call execution in remote cluster.
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        final ParameterTool params = ParameterTool.fromArgs(args);

        //make parameters available in the web interface and each node in the cluster
        env.getConfig().setGlobalJobParameters(params);

        //read the text file from given input path
        DataSet<String> text = env.readTextFile(params.get("input")); //readCsvFile(path) -> returns dataset of tuples. tuples are upto tuple25
        //readFileOfPrimitive(path,Class) - reads each line of file in the form of class mentioned in arguments.
        //readFileOfPrimitives(path,delimiter,Class) - reads each line of file in th form of class mentioned in arguments using a delimiter. e.g
        // 43,45,81
        // 23,45,12
        //readHadoopFile(FileInputFormat,Key,Value,path) - reads hdfs file. Key,Value is type of key and value
        //readSequenceFile(Key,value,path) - reads sequence file

        DataSet<String> filtered = text.filter(new FilterFunction<String>(){

            @Override
            public boolean filter(String s) throws Exception {
                return s.startsWith("n");
            }
        });

        //split up the lines in pairs (2-tuples) containing : (word,1)
        DataSet<Tuple2<String, Integer>> tokenized = filtered.map(new Tokenizer());

        DataSet<Tuple2<String, Integer>> counts = tokenized.groupBy( 0 ).sum(1); // groupBy takes index of tuple on which grouping has to be done
        // OR
//        UnsortedGrouping<Tuple2<String,Integer>> counts1 = tokenized.groupBy(0);
//        DataSet<Tuple2<String,Integer>> counts = counts1.sum(1);


        if (params.has("output"))
        {
            counts.writeAsCsv(params.get("output"), "\n", " ");

            env.execute("WordCount Example"); //if this isn't present job will not run
        }
    }

    public static final class Tokenizer
            implements MapFunction<String, Tuple2<String, Integer>>
    {
        public Tuple2<String, Integer> map(String value)
        {
            return new Tuple2(value, Integer.valueOf(1));
        }
    }

    // when we want multiple outputs from single input use flatmap. say input is Naman,Ruchi,Nayan and we want the o/p as (naman,1) and (nayan,1)
    public static final class FlatMapTokenizer implements FlatMapFunction<String,Tuple2<String,Integer>>{

        @Override
        public void flatMap(String s, Collector<Tuple2<String, Integer>> collector) throws Exception {
            //split the line
            String[] tokens = s.split(",");
            //emit the pairs
//            for(String token : tokens){
//                if(token.length()>0)
//                    collector.collect(new Tuple2<>(token,1)); // (naman,1) and (nayan,1)
//            }
            Arrays.stream(tokens).filter(token -> token.length()>0).forEach(token -> collector.collect(new Tuple2<>(token,1)));
        }
    }


}

