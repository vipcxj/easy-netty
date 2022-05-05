package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class RedisBulkStringMessage extends AbstractRedisMessage {

    private int length;
    private StoreMode mode;
    private byte[] content;
    private int remaining;

    public RedisBulkStringMessage(EasyNettyContext context) {
        super(context);
        this.mode = StoreMode.UNKNOWN;
        this.length = -2;
    }

    public RedisBulkStringMessage(byte[] content) {
        super(null);
        this.remaining = 0;
        this.mode = StoreMode.STORE;
        if (content == null) {
            this.length = -1;
            this.content = null;
        } else {
            this.length = content.length;
            this.content = content;
        }
        markComplete();
    }

    @Override
    public RedisType type() {
        return RedisType.BULK_STRING;
    }

    @Override
    public RedisBulkStringMessage asBulkString() {
        return this;
    }

    public JPromise<Integer> readLength() {
        if (this.length == -2) {
            long lenRead = Utils.readRedisNumber(context).await();
            if (lenRead > 512 * 1024 * 1024) {
                throw new IllegalArgumentException("The length of bulk string must be less or equal to 512M.");
            }
            this.length = this.remaining = (int) lenRead;
        }
        return JPromise.just(this.length);
    }

    public int getLength() {
        if (length == -2) {
            throw new IllegalStateException("Call readLength().await() first.");
        }
        return length;
    }

    public StoreMode getMode() {
        return mode;
    }

    public byte[] getContent() {
        makeSureCompleted();
        if (mode == StoreMode.DISCARD) {
            throw new UnsupportedOperationException("The method getContent only supported in STORE mode.");
        }
        return content;
    }

    private void triggerMode(boolean store) {
        if (mode == StoreMode.UNKNOWN) {
            mode = store ? StoreMode.STORE : StoreMode.DISCARD;
        } else if (mode == StoreMode.DISCARD) {
            throw new IllegalStateException("Because the current mode is DISCARD mode, so read content operation can only be invoked once.");
        }
    }

    public JPromise<String> readStringContent() {
        return readContent().thenMapImmediate(c -> new String(c, StandardCharsets.UTF_8));
    }

    public JPromise<byte[]> readContent() {
        if (isComplete()) {
            return JPromise.just(content);
        }
        readLength().await();
        if (length == -1) {
            content = null;
            remaining = 0;
            mode = StoreMode.STORE;
            context.consumeBytes(Utils.REDIS_LINE_END).await();
            markComplete();
            return JPromise.empty();
        } else {
            triggerMode(true);
            if (isComplete()) {
                return JPromise.just(content);
            }
            if (content == null) {
                content = context.readBytes(length).await();
                context.consumeBytes(Utils.REDIS_LINE_END).await();
                remaining = 0;
                markComplete();
            } else if (remaining > 0) {
                context.readBytes(content, length - remaining, remaining).await();
                context.consumeBytes(Utils.REDIS_LINE_END).await();
                remaining = 0;
                markComplete();
            } else {
                context.consumeBytes(Utils.REDIS_LINE_END).await();
                markComplete();
            }
            return JPromise.just(content);
        }
    }

    public JPromise<ByteBuf> readSomeContent(boolean store) {
        if (isComplete()) {
            return JPromise.just(Unpooled.EMPTY_BUFFER);
        }
        readLength().await();
        if (length == -1) {
            content = null;
            remaining = 0;
            mode = StoreMode.STORE;
            context.consumeBytes(Utils.REDIS_LINE_END).await();
            markComplete();
            return JPromise.empty();
        } else {
            triggerMode(store);
            if (remaining == 0) {
                context.consumeBytes(Utils.REDIS_LINE_END).await();
                markComplete();
                return JPromise.just(Unpooled.EMPTY_BUFFER);
            } else {
                ByteBuf buf = context.readSomeBuf(remaining).await();
                if (mode == StoreMode.STORE) {
                    int readerIndex = buf.readerIndex();
                    buf.readBytes(content, length - remaining, buf.readableBytes());
                    buf.readerIndex(readerIndex);
                }
                remaining -= buf.readableBytes();
                if (remaining == 0) {
                    context.consumeBytes(Utils.REDIS_LINE_END).await();
                    markComplete();
                }
                return JPromise.just(buf);
            }
        }
    }

    @Override
    public JPromise<Void> complete(boolean skip) {
        if (skip) {
            while (!isComplete()) {
                readStringContent().await();
            }
        } else {
            readContent().await();
        }
        return JPromise.empty();
    }

    @Override
    public void writeToByteBuf(ByteBuf buf) {
        makeSureCompleted();
        if (mode != StoreMode.STORE) {
            throw new UnsupportedOperationException("Only store mode bulk string message support write to byte buf.");
        }
        buf.writeByte(type().sign());
        buf.writeCharSequence(Integer.toString(length), StandardCharsets.UTF_8);
        Utils.writeRedisLineEnd(buf);
        if (content != null) {
            buf.writeBytes(content);
            Utils.writeRedisLineEnd(buf);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RedisBulkStringMessage that = (RedisBulkStringMessage) o;
        return length == that.length && remaining == that.remaining && Arrays.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), length, remaining);
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }
}
