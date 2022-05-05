package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;

public class Utils {

    public static final byte[] REDIS_LINE_END = new byte[] {'\r', '\n'};

    public static JPromise<Long> readRedisNumber(EasyNettyContext context) {
        return context.readUtf8Until('\r', '\n').thenMapImmediate(Long::parseLong);
    }

    public static void writeRedisLineEnd(ByteBuf buf) {
        buf.writeByte('\r');
        buf.writeByte('\n');
    }
}
