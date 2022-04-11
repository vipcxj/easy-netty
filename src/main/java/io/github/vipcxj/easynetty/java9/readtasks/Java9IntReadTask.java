package io.github.vipcxj.easynetty.java9.readtasks;

import io.github.vipcxj.easynetty.java8.collections.AbstractJava8AppenderNode;
import io.github.vipcxj.easynetty.readtasks.IntReadTask;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public class Java9IntReadTask extends AbstractJava8AppenderNode<Java9IntReadTask, Java9IntReadTask> implements IntReadTask<Java9IntReadTask> {

    private final JThunk<Integer> thunk;

    public Java9IntReadTask(JThunk<Integer> thunk) {
        this.thunk = thunk;
    }

    @Override
    public JThunk<Integer> getThunk() {
        return thunk;
    }
}
