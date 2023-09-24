package io.github.vipcxj.easynetty.utils;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.util.concurrent.Future;

public class PromiseUtils {

    private PromiseUtils() {}

    public static <T> JPromise<T> toPromise(Future<T> future) {
        return JPromise.generate((thunk, context) -> {
            future.addListener(f -> {
                if (f.isSuccess()) {
                    //noinspection unchecked
                    thunk.resolve((T) f.getNow(), context);
                } else if (f.isCancelled()) {
                    thunk.interrupt(context);
                } else {
                    thunk.reject(f.cause(), context);
                }
            });
        });
    }
}
