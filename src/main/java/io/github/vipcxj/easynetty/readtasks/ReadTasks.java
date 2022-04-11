package io.github.vipcxj.easynetty.readtasks;

import io.github.vipcxj.easynetty.java8.readtasks.Java8ByteReadTask;
import io.github.vipcxj.easynetty.java8.readtasks.Java8IntReadTask;
import io.github.vipcxj.easynetty.java8.readtasks.Java8ShortReadTask;
import io.github.vipcxj.easynetty.java9.readtasks.Java9ByteReadTask;
import io.github.vipcxj.easynetty.java9.readtasks.Java9IntReadTask;
import io.github.vipcxj.easynetty.java9.readtasks.Java9ShortReadTask;
import io.github.vipcxj.easynetty.utils.Constants;
import io.github.vipcxj.jasync.ng.spec.JThunk;

public class ReadTasks {

    public static ByteReadTask<?> readByte(JThunk<Byte> thunk) {
        return Constants.JAVA9 ? new Java9ByteReadTask(thunk) : new Java8ByteReadTask(thunk);
    }

    public static ShortReadTask<?> readShort(JThunk<Short> thunk) {
        return Constants.JAVA9 ? new Java9ShortReadTask(thunk) : new Java8ShortReadTask(thunk);
    }

    public static IntReadTask<?> readInt(JThunk<Integer> thunk) {
        return Constants.JAVA9 ? new Java9IntReadTask(thunk) : new Java8IntReadTask(thunk);
    }
}
