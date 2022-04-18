package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;

public class RedisBulkStringMessage extends AbstractRedisMessage {

    public RedisBulkStringMessage(EasyNettyContext context) {
        super(context);
    }

    @Override
    public RedisType type() {
        return RedisType.BULK_STRING;
    }

    @Override
    public RedisBulkStringMessage asBulkString() {
        return this;
    }
}
