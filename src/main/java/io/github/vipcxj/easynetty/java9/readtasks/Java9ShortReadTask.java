package io.github.vipcxj.easynetty.java9.readtasks;

import io.github.vipcxj.easynetty.java9.collections.AbstractJava9AppenderNode;
import io.github.vipcxj.easynetty.readtasks.ShortReadTask;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public class Java9ShortReadTask extends AbstractJava9AppenderNode<Java9ShortReadTask, Java9ShortReadTask> implements ShortReadTask<Java9ShortReadTask> {

    private final JThunk<Short> thunk;

    public Java9ShortReadTask(JThunk<Short> thunk) {
        this.thunk = thunk;
    }

    @Override
    public JThunk<Short> getThunk() {
        return thunk;
    }
}
