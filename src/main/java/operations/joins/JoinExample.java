package operations.joins;

import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.operators.base.JoinOperatorBase.JoinHint;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;


public class JoinExample {
    public static void main(String[] args) throws Exception {
        //set up execution environment. ExecutionEnvironemt is a context in which a program is executed. Local environment will call execution in current jvm and remote environment will call execution in remote cluster.
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        final ParameterTool params = ParameterTool.fromArgs(args);

        //make parameters available in the web interface and each node in the cluster
        env.getConfig().setGlobalJobParameters(params);

        //read the person file from given input path and generate tuples out of each string read
        DataSet<Tuple2<Integer, String>> personSet = env.readTextFile(params.get("input1")).
                map(new MapFunction<String, Tuple2<Integer, String>>() { //personSet = tuple of (1 John)
                    @Override
                    public Tuple2<Integer, String> map(String s) throws Exception {
                        String[] words = s.split(",");
                        return new Tuple2<Integer, String>(Integer.parseInt(words[0]), words[1]);
                    }
                });

        //read the location file from given input path and generate tuples out of each string read
        DataSet<Tuple2<Integer, String>> locationSet = env.readTextFile(params.get("input2")).
                map(new MapFunction<String, Tuple2<Integer, String>>() { //personSet = tuple of (1 Delhi)
                    @Override
                    public Tuple2<Integer, String> map(String s) throws Exception {
                        String[] words = s.split(",");
                        return new Tuple2<Integer, String>(Integer.parseInt(words[0]), words[1]);
                    }
                });

        DataSet<Tuple3<Integer, String, String>> joined = null;

        String joinType = params.get("join_type");
        //join datasets on person_id
        //joined format will be <id, person_name, state>
        // Note: JoinHint.OPTIMIZER_CHOOSES is the default. If we don't give this then flink automatically optimizes it by using this. Read about other JoinHint in docs in resources folder
        if (joinType == null || joinType.equalsIgnoreCase("inner")) {
            joined = personSet.join(locationSet, JoinHint.OPTIMIZER_CHOOSES).where(0).equalTo(0).// where(0).equalTo(0) means first field of personSet tuple equals first field of locationSet tuple. default o/p will be like (1,John) (1,DC)
                    with(new JoinFunction<Tuple2<Integer, String>, Tuple2<Integer, String>, Tuple3<Integer, String, String>>() { // first 2 params of Joinfunction is input params and 3rd one is output param
                @Override
                public Tuple3<Integer, String, String> join(Tuple2<Integer, String> person, Tuple2<Integer, String> location) throws Exception {
                    return new Tuple3<>(person.f0, person.f1, location.f1); //f0 means first field
                }
            });
        }//JoinFunction is optional. if not given then output will be new tuple dataset with 2 fields with 1st field being full tuple of 1st input dataset and second field being matching tuple of second input dataset

        else if (joinType.equalsIgnoreCase("leftOuter")) { //fullOuterJoin
            joined = personSet.leftOuterJoin(locationSet).where(0).equalTo(0).// where(0).equalTo(0) means first field of personSet tuple equals first field of locationSet tuple. default o/p will be like (1,John) (1,DC)
                    with(new JoinFunction<Tuple2<Integer, String>, Tuple2<Integer, String>, Tuple3<Integer, String, String>>() { // first 2 params of Joinfunction is input params and 3rd one is output param
                @Override
                public Tuple3<Integer, String, String> join(Tuple2<Integer, String> person, Tuple2<Integer, String> location) throws Exception {
                    if (location == null)
                        return new Tuple3<>(person.f0, person.f1, "NULL");
                    return new Tuple3<>(person.f0, person.f1, location.f1); //f0 means first field
                }
            });
        }


        joined.writeAsCsv(params.get("output"), "\n", ","); // fields separated by coma and line by new line

        env.execute("JoinExample");

    }
}
