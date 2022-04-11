package io.github.vipcxj.easynetty.java8.collections;

import io.github.vipcxj.easynetty.collections.FastAppender;

public abstract class AbstractJava8AppenderNode<E, T extends FastAppender.Node<E, T>> implements FastAppender.Node<E, T> {

    private volatile T next;
    private static final sun.misc.Unsafe UNSAFE;
    private static final long nextOffset;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            nextOffset = UNSAFE.objectFieldOffset
                    (AbstractJava8AppenderNode.class.getDeclaredField("next"));
        } catch (Exception e) {
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
        return UNSAFE.compareAndSwapObject(this, nextOffset, null, next);
    }
}
