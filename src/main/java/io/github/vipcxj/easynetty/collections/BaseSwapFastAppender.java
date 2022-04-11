package io.github.vipcxj.easynetty.collections;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class BaseSwapFastAppender<E, T extends FastAppender.Node<E, T>> implements SwapFastAppender<E, T> {

    private AbstractFastAppender<E, T> current;
    private AbstractFastAppender<E, T> tmp;
    private final Supplier<AbstractFastAppender<E, T>> appenderSupplier;
    private final ReadWriteLock currentLock;

    public BaseSwapFastAppender(Supplier<AbstractFastAppender<E, T>> appenderSupplier) {
        this.appenderSupplier = appenderSupplier;
        this.current = appenderSupplier.get();
        this.currentLock = new ReentrantReadWriteLock();
    }

    @Override
    public void append(Object data) {
        currentLock.readLock().lock();
        try {
            //noinspection unchecked
            current.append((E) data);
        } finally {
            currentLock.readLock().unlock();
        }
    }

    @Override
    public void consume(Consumer<E> consumer) {
        throw new UnsupportedOperationException("Use swap().consume(consumer) instead.");
    }

    @Override
    public FastAppender<E, T> swap() {
        tmp = current;
        AbstractFastAppender<E, T> newAppender = appenderSupplier.get();
        currentLock.writeLock().lock();
        try {
            current = newAppender;
            return tmp;
        } finally {
            currentLock.writeLock().unlock();
        }
    }

    @Override
    public void restore() {
        tmp.append(current);
        currentLock.writeLock().lock();
        try {
            current = tmp;
            tmp = null;
        } finally {
            currentLock.writeLock().unlock();
        }
    }
}
