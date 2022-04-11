package io.github.vipcxj.easynetty.java8.collections;

import io.github.vipcxj.easynetty.collections.AbstractFastAppender;
import io.github.vipcxj.easynetty.collections.FastAppender;

import java.util.function.Function;

public class Java8FastAppender<E, T extends FastAppender.Node<E, T>> extends AbstractFastAppender<E, T> {

    private volatile T tail;
    private static final sun.misc.Unsafe UNSAFE;
    private static final long tailOffset;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = Java8FastAppender.class;
            tailOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public Java8FastAppender(Function<E, T> nodeFactory) {
        super(nodeFactory);
    }

    @Override
    protected T getTail() {
        return tail;
    }

    @Override
    protected void setTail(T tail) {
        this.tail = tail;
    }

    @Override
    protected void weakCasTail(T cmp, T tail) {
        UNSAFE.compareAndSwapObject(this, tailOffset, cmp, tail);
    }
}
