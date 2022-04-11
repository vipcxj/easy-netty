package io.github.vipcxj.easynetty.java8.readtasks;

import io.github.vipcxj.easynetty.java8.collections.AbstractJava8AppenderNode;
import io.github.vipcxj.easynetty.readtasks.ByteReadTask;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public class Java8ByteReadTask extends AbstractJava8AppenderNode<Java8ByteReadTask, Java8ByteReadTask> implements ByteReadTask<Java8ByteReadTask> {

    private final JThunk<Byte> thunk;

    public Java8ByteReadTask(JThunk<Byte> thunk) {
        this.thunk = thunk;
    }

    @Override
    public JThunk<Byte> getThunk() {
        return thunk;
    }
}
