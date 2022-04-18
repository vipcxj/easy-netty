package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;

public class RedisIntegerMessage extends AbstractRedisMessage {

    private Long value;

    public RedisIntegerMessage(EasyNettyContext context) {
        super(context);
    }

    @Override
    public RedisType type() {
        return RedisType.INTEGER;
    }

    @Override
    public RedisIntegerMessage asInteger() {
        return this;
    }

    public JPromise<Long> value() {
        if (value == null) {
            value = Utils.readRedisNumber(context).await();
        }
        return JPromise.just(value);
    }
}
