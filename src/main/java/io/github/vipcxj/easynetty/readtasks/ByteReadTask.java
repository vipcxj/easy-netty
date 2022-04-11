package io.github.vipcxj.easynetty.readtasks;

public interface ByteReadTask<Impl extends ByteReadTask<Impl>> extends ReadTask<Byte, Impl> {
    @Override
    default int readSize() {
        return 1;
    }
}
