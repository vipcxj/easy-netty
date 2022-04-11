package io.github.vipcxj.easynetty.readtasks;

import io.github.vipcxj.easynetty.collections.FastAppender;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public interface ReadTask<T, Impl extends ReadTask<T, Impl>> extends FastAppender.Node<Impl, Impl> {
    JThunk<T> getThunk();
    int readSize();
    @SuppressWarnings("unchecked")
    @Override
    default Impl data() {
        return (Impl) this;
    }
}
