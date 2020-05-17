package states;

import java.util.ArrayList;
import java.util.List;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

public class ManagedOpertorStateDemo implements SinkFunction<Tuple2<String, Integer>>,  CheckpointedFunction
{
    private final int threshold;
    private transient ListState<Tuple2<String, Integer>> checkpointedState;

    private List<Tuple2<String, Integer>> bufferedElements;

    public ManagedOpertorStateDemo(int threshold)
    {
        this.threshold = threshold;
        this.bufferedElements = new ArrayList<Tuple2<String, Integer>>();
    }

    public void invoke(Tuple2<String, Integer> value) throws Exception
    {
        bufferedElements.add(value);
        if (bufferedElements.size() == threshold)
        {
            for (Tuple2<String, Integer> element: bufferedElements)
            {
                // send it to the sink
            }
            bufferedElements.clear();
        }    }

    public void snapshotState(FunctionSnapshotContext context) throws Exception
    {
        checkpointedState.clear();
        for (Tuple2<String, Integer> element : bufferedElements) // adding all elements of java list
        {
            checkpointedState.add(element);
        }    }

    public void initializeState(FunctionInitializationContext context) throws Exception //FunctionInitializationContext provides contet where user functions can inititalize by registering to managed state
    {
        ListStateDescriptor<Tuple2<String, Integer>> descriptor =  new ListStateDescriptor<Tuple2<String, Integer>>(  "buffered-elements",
                TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {}));

        checkpointedState = context.getOperatorStateStore().getListState(descriptor);       // .getUninonListState(descriptor) uses union distribution while getListState(descriptor) uses even split redistribution

        if (context.isRestored()) // using this condition we are checking if we are recovering after failure or not. true means we are recovering
        { //recovery logic. Here we are getting elements from the saved state and adding all those lements in the java list
            for (Tuple2<String, Integer> element : checkpointedState.get())
            {
                bufferedElements.add(element);

            }        }    }
}

