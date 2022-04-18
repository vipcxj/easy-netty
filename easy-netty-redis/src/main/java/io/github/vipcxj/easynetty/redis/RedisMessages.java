package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;

public class RedisMessages {
    public static JPromise<RedisMessage> readMessage(EasyNettyContext context) {
        char type = (char) (byte) context.readByte().await();
        if (type == RedisType.ARRAY.sign()) {
            return JPromise.just(new RedisArrayMessage(context));
        } else if (type == RedisType.BULK_STRING.sign()) {
            return JPromise.just(new RedisBulkStringMessage(context));
        } else if (type == RedisType.SIMPLE_STRING.sign()) {
            return JPromise.just(new RedisSimpleStringMessage(context));
        } else if (type == RedisType.ERROR.sign()) {
            return JPromise.just(new RedisErrorMessage(context));
        } else if (type == RedisType.INTEGER.sign()) {
            return JPromise.just(new RedisIntegerMessage(context));
        } else {
            throw new IllegalArgumentException("Unsupported redis message type: " + type + ".");
        }
    }
}
