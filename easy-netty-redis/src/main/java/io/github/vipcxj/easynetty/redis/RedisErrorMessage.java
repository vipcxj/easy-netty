package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
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
