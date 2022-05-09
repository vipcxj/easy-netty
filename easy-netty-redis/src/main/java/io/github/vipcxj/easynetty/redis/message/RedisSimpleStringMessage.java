package io.github.vipcxj.easynetty.redis.message;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.handler.UntilBox;
import io.github.vipcxj.easynetty.handler.UntilMode;
import io.github.vipcxj.easynetty.redis.Utils;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class RedisSimpleStringMessage extends AbstractRedisMessage {

    private String content;

    public RedisSimpleStringMessage(EasyNettyContext context) {
        super(context);
    }

    public RedisSimpleStringMessage(String content) {
        super(null);
        this.content = content;
        markComplete();
    }

    @Override
    public RedisType type() {
        return RedisType.SIMPLE_STRING;
    }

    @Override
    public RedisSimpleStringMessage asSimpleString() {
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
        outputContext.writeByte(type().sign()).await();
        if (!isComplete()) {
            UntilBox untilBox = new UntilBox(UntilMode.INCLUDE, "\r\n");
            CompositeByteBuf byteBuf = storeIfRead ? context.getChannelContext().alloc().compositeBuffer() : null;
            try {
                while (!untilBox.isSuccess()) {
                    context.readSomeBufUntilAny(untilBox).await();
                    if (storeIfRead) {
                        byteBuf.addComponent(true, untilBox.getBuf().retainedSlice());
                    }
                    outputContext.writeBuffer(untilBox.getBuf()).await();
                }
                if (storeIfRead) {
                    content = byteBuf.toString(StandardCharsets.UTF_8);
                }
                markComplete();
            } finally {
                if (byteBuf != null) {
                    byteBuf.release();
                }
            }
            return JPromise.empty();
        } else {
            outputContext.writeString(getContent()).await();
            return outputContext.writeBytes(Utils.REDIS_LINE_END);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RedisSimpleStringMessage that = (RedisSimpleStringMessage) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), content);
    }
}
