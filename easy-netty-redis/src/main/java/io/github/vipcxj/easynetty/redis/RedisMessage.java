package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;

public interface RedisMessage {
    RedisType type();
    EasyNettyContext context();
    boolean isComplete();
    JPromise<Void> complete(boolean skip);
    default JPromise<Void> complete() {
        return complete(false);
    }
    JPromise<Void> untilComplete();
    void writeToByteBuf(ByteBuf buf);
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
