package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;

public abstract class AbstractRedisMessage implements RedisMessage {

    protected final EasyNettyContext context;

    public AbstractRedisMessage(EasyNettyContext context) {
        this.context = context;
    }

    @Override
    public EasyNettyContext context() {
        return context;
    }
}
