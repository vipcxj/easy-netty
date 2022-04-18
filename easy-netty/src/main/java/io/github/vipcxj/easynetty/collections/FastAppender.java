package io.github.vipcxj.easynetty.collections;

public interface FastAppender<E, T extends FastAppender.Node<E, T>> {

    void append(E data);

    void consume(Consumer<E> consumer);

    interface Node<E, T> {
        E data();
        T next();
        void setNext(T next);
        boolean setNextWhenNull(T next);
    }

    interface Consumer<E> {
        boolean consume(E data);
    }
}
