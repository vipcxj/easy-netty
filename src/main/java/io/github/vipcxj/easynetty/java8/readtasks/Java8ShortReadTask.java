package io.github.vipcxj.easynetty.java8.readtasks;

import io.github.vipcxj.easynetty.java8.collections.AbstractJava8AppenderNode;
import io.github.vipcxj.easynetty.readtasks.ShortReadTask;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public class Java8ShortReadTask extends AbstractJava8AppenderNode<Java8ShortReadTask, Java8ShortReadTask> implements ShortReadTask<Java8ShortReadTask> {

    private final JThunk<Short> thunk;

    public Java8ShortReadTask(JThunk<Short> thunk) {
        this.thunk = thunk;
    }

    @Override
    public JThunk<Short> getThunk() {
        return thunk;
    }
}
