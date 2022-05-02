package io.github.vipcxj.easynetty.utils;

import java.util.Objects;

public class Tuple2OwB<T> {

    private final T el0;
    private final boolean el1;

    public Tuple2OwB(T el0, boolean el1) {
        this.el0 = el0;
        this.el1 = el1;
    }

    public static <T> Tuple2OwB<T> of(T el0, boolean el1) {
        return new Tuple2OwB<>(el0, el1);
    }

    public T getEl0() {
        return el0;
    }

    public boolean isEl1() {
        return el1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple2OwB<?> tuple2OwB = (Tuple2OwB<?>) o;
        return el1 == tuple2OwB.el1 && Objects.equals(el0, tuple2OwB.el0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(el0, el1);
    }
}
