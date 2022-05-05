package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class RedisIntegerMessage extends AbstractRedisMessage {

    private Long value;

    public RedisIntegerMessage(EasyNettyContext context) {
        super(context);
    }

    public RedisIntegerMessage(long value) {
        super(null);
        this.value = value;
        markComplete();
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
            markComplete();
        }
        return JPromise.just(value);
    }

    public long getValue() {
        makeSureCompleted();
        return value;
    }

    @Override
    public JPromise<Void> complete(boolean skip) {
        return value().thenReturn(null);
    }

    @Override
    public void writeToByteBuf(ByteBuf buf) {
        makeSureCompleted();
        buf.writeByte(type().sign());
        buf.writeCharSequence(Long.toString(value), StandardCharsets.UTF_8);
        Utils.writeRedisLineEnd(buf);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RedisIntegerMessage that = (RedisIntegerMessage) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
