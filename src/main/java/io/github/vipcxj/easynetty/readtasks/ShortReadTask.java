package io.github.vipcxj.easynetty.readtasks;

public interface ShortReadTask<Impl extends ShortReadTask<Impl>> extends ReadTask<Short, Impl> {
    @Override
    default int readSize() {
        return 2;
    }
}
