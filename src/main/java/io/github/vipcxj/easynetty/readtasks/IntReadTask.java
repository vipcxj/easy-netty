package io.github.vipcxj.easynetty.readtasks;

public interface IntReadTask<Impl extends IntReadTask<Impl>> extends ReadTask<Integer, Impl> {
    @Override
    default int readSize() {
        return 4;
    }
}
