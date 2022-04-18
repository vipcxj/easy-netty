package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;

public class Utils {
    public static JPromise<Long> readRedisNumber(EasyNettyContext context) {
        return context.readUtf8Until('\r', '\n').thenMapImmediate(Long::parseLong);
    }
}
