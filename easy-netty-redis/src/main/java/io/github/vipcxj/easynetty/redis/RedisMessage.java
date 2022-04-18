package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;

public interface RedisMessage {
    RedisType type();
    EasyNettyContext context();
    default RedisSimpleStringMessage asSimpleString() {
        throw new UnsupportedOperationException("Unable to transform to simple string message. The message type is " + type() + ".");
    }
    default RedisErrorMessage asError() {
        throw new UnsupportedOperationException("Unable to transform to error message. The message type is " + type() + ".");
    }
    default RedisIntegerMessage asInteger() {
        throw new UnsupportedOperationException("Unable to transform to integer message. The message type is " + type() + ".");
    }
    default RedisBulkStringMessage asBulkString() {
        throw new UnsupportedOperationException("Unable to transform to bulk string message. The message type is " + type() + ".");
    }
    default RedisArrayMessage asArray() {
        throw new UnsupportedOperationException("Unable to transform to array message. The message type is " + type() + ".");
    }
}
