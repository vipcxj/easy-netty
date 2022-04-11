package io.github.vipcxj.easynetty.java9.readtasks;

import io.github.vipcxj.easynetty.java9.collections.AbstractJava9AppenderNode;
import io.github.vipcxj.easynetty.readtasks.ByteReadTask;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public class Java9ByteReadTask extends AbstractJava9AppenderNode<Java9ByteReadTask, Java9ByteReadTask> implements ByteReadTask<Java9ByteReadTask> {

    private final JThunk<Byte> thunk;

    public Java9ByteReadTask(JThunk<Byte> thunk) {
        this.thunk = thunk;
    }

    @Override
    public JThunk<Byte> getThunk() {
        return thunk;
    }
}
