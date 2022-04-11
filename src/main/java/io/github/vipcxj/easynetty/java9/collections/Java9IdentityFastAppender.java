package io.github.vipcxj.easynetty.java9.collections;

import io.github.vipcxj.easynetty.collections.FastAppender;
import io.github.vipcxj.easynetty.java8.collections.Java8FastAppender;

import java.util.function.Function;

public class Java9IdentityFastAppender<T extends FastAppender.Node<T, T>> extends Java8FastAppender<T, T> {
    public Java9IdentityFastAppender() {
        super(Function.identity());
    }
}
