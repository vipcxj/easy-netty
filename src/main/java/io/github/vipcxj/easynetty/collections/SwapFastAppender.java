package io.github.vipcxj.easynetty.collections;

public interface SwapFastAppender<E, T extends FastAppender.Node<E, T>> extends FastAppender<E, T> {

    @Override
    void append(Object data);
    FastAppender<E, T> swap();
    void restore();
}
