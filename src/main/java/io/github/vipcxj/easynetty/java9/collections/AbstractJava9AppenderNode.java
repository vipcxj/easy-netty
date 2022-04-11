package io.github.vipcxj.easynetty.java9.collections;

import io.github.vipcxj.easynetty.collections.FastAppender;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public abstract class AbstractJava9AppenderNode<E, T extends FastAppender.Node<E, T>> implements FastAppender.Node<E, T> {

    private volatile T next;
    private static final VarHandle NEXT;
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            NEXT = lookup.findVarHandle(AbstractJava9AppenderNode.class, "next", FastAppender.Node.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public T next() {
        return next;
    }

    @Override
    public void setNext(T next) {
        this.next = next;
    }

    @Override
    public boolean setNextWhenNull(T next) {
        return NEXT.compareAndSet(this, null, next);
    }
}
