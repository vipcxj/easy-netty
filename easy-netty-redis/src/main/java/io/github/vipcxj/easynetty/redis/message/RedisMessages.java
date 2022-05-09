package io.github.vipcxj.easynetty.redis.message;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.handler.UntilBox;
import io.github.vipcxj.jasync.ng.spec.JPromise;

public class RedisMessages {
    public static JPromise<RedisMessage> readMessage(EasyNettyContext context) {
        context.mark();
        char type = (char) (byte) context.readByte().await();
        if (type == RedisType.ARRAY.sign()) {
            context.cleanMark();
            return JPromise.just(new RedisArrayMessage(context));
        } else if (type == RedisType.BULK_STRING.sign()) {
            context.cleanMark();
            return JPromise.just(new RedisBulkStringMessage(context));
        } else if (type == RedisType.SIMPLE_STRING.sign()) {
            context.cleanMark();
            return JPromise.just(new RedisSimpleStringMessage(context));
        } else if (type == RedisType.ERROR.sign()) {
            context.cleanMark();
            return JPromise.just(new RedisErrorMessage(context));
        } else if (type == RedisType.INTEGER.sign()) {
            context.cleanMark();
            return JPromise.just(new RedisIntegerMessage(context));
        } else {
            context.resetMark();
            return JPromise.just(new RedisInlineMessage(context));
        }
    }

    public static JPromise<Boolean> isStandardRedisMessage(EasyNettyContext context) {
        if (context.consumeByte(RedisType.ARRAY.sign()).await()
                || context.consumeByte(RedisType.BULK_STRING.sign()).await()
                || context.consumeByte(RedisType.SIMPLE_STRING.sign()).await()
                || context.consumeByte(RedisType.ERROR.sign()).await()
                || context.consumeByte(RedisType.INTEGER.sign()).await()
        ) {
            return JPromise.just(true);
        } else {
            return JPromise.just(false);
        }
    }
}
