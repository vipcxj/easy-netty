package io.github.vipcxj.easynetty.redis.message;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.handler.UntilBox;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;

import java.nio.charset.StandardCharsets;

public class RedisInlineMessage extends AbstractRedisMessage {

    private String content;

    public RedisInlineMessage(EasyNettyContext context) {
        super(context);
    }

    @Override
    public RedisType type() {
        return RedisType.INLINE;
    }

    @Override
    public JPromise<Void> complete(boolean skip) {
        return read();
    }

    public JPromise<Void> read() {
        if (isComplete()) {
            return JPromise.empty();
        }
        UntilBox untilBox = new UntilBox("\r\n");
        CompositeByteBuf byteBuf = context.getChannelContext().alloc().compositeBuffer();
        try {
            int read = 0;
            while (!untilBox.isSuccess()) {
                context.readSomeBufUntilAny(untilBox).await();
                if (untilBox.getBuf().isReadable()) {
                    read += untilBox.getBuf().readableBytes();
                    if (read <= 64 * 1024) {
                        byteBuf.addComponent(true, untilBox.getBuf());
                    } else {
                        throw new IllegalArgumentException("Too long inline redis command. The max length is 64 * 1024.");
                    }
                }
            }
            content = byteBuf.toString(StandardCharsets.UTF_8);
            markComplete();
        } finally {
            byteBuf.release();
        }
        return JPromise.empty();
    }

    public String getName() {
        markComplete();
        int pos = content.indexOf(' ');
        return pos >= 0 ? content.substring(0, pos) : content;
    }

    @Override
    public JPromise<Void> write(EasyNettyContext outputContext, boolean readIfNeed, boolean storeIfRead) {
        if (!readIfNeed) {
            makeSureCompleted();
        }
        if (!isComplete()) {
            read().await();
        }
        return outputContext.writeString(content);
    }

    @Override
    public void writeToByteBuf(ByteBuf buf) {
        makeSureCompleted();
        buf.writeCharSequence(content, StandardCharsets.UTF_8);
    }

    @Override
    public RedisInlineMessage asInline() {
        return this;
    }
}
