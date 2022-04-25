package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;

public abstract class AbstractRedisMessage implements RedisMessage {

    protected final EasyNettyContext context;
    protected boolean complete;

    public AbstractRedisMessage(EasyNettyContext context) {
        this.context = context;
        this.complete = false;
    }

    @Override
    public EasyNettyContext context() {
        return context;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }
}
