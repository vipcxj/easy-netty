package io.github.vipcxj.easynetty.java8.collections;

import io.github.vipcxj.easynetty.collections.FastAppender;

import java.util.function.Function;

public class Java8IdentityFastAppender<T extends FastAppender.Node<T, T>> extends Java8FastAppender<T, T> {

    public Java8IdentityFastAppender() {
        super(Function.identity());
    }
}
