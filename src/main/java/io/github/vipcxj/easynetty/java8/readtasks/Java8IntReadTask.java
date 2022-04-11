package io.github.vipcxj.easynetty.java8.readtasks;

import io.github.vipcxj.easynetty.java8.collections.AbstractJava8AppenderNode;
import io.github.vipcxj.easynetty.readtasks.IntReadTask;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public class Java8IntReadTask extends AbstractJava8AppenderNode<Java8IntReadTask, Java8IntReadTask> implements IntReadTask<Java8IntReadTask> {

    private final JThunk<Integer> thunk;

    public Java8IntReadTask(JThunk<Integer> thunk) {
        this.thunk = thunk;
    }

    @Override
    public JThunk<Integer> getThunk() {
        return thunk;
    }
}
