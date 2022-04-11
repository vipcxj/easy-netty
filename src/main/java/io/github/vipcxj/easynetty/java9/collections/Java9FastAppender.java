package io.github.vipcxj.easynetty.java9.collections;

import io.github.vipcxj.easynetty.collections.AbstractFastAppender;
import io.github.vipcxj.easynetty.collections.FastAppender;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

public class Java9FastAppender<E, T extends FastAppender.Node<E, T>> extends AbstractFastAppender<E, T> {

    private volatile T tail;
    private static final VarHandle TAIL;
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            TAIL = lookup.findVarHandle(Java9FastAppender.class, "tail", FastAppender.Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public Java9FastAppender(Function<E, T> nodeFactory) {
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
        TAIL.weakCompareAndSet(this, cmp, tail);
    }
}
