package io.github.vipcxj.easynetty.redis.message;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.redis.Utils;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class RedisErrorMessage extends AbstractRedisMessage {

    private String content;

    public RedisErrorMessage(EasyNettyContext context) {
        super(context);
    }

    public RedisErrorMessage(String content) {
        super(null);
        this.content = content;
        markComplete();
    }

    @Override
    public RedisType type() {
        return RedisType.ERROR;
    }

    @Override
    public RedisErrorMessage asError() {
        return this;
    }

    public JPromise<String> content() {
        if (content == null) {
            content = context.readUtf8Until('\r', '\n').await();
            markComplete();
        }
        return JPromise.just(content);
    }

    public String getContent() {
        makeSureCompleted();
        return content;
    }

    @Override
    public JPromise<Void> complete(boolean skip) {
        return content().thenReturn(null);
    }

    @Override
    public void writeToByteBuf(ByteBuf buf) {
        makeSureCompleted();
        buf.writeByte(type().sign());
        buf.writeCharSequence(content, StandardCharsets.UTF_8);
        Utils.writeRedisLineEnd(buf);
    }

    @Override
    public JPromise<Void> write(EasyNettyContext outputContext, boolean readIfNeed, boolean storeIfRead) {
        if (!readIfNeed) {
            makeSureCompleted();
        }
        if (!isComplete()) {
            complete(false).await();
        }
        outputContext.writeByte(type().sign()).await();
        outputContext.writeString(content).await();
        return outputContext.writeBytes(Utils.REDIS_LINE_END);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RedisErrorMessage that = (RedisErrorMessage) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), content);
    }
}
