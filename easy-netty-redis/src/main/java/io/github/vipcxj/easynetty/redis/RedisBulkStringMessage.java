package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class RedisBulkStringMessage extends AbstractRedisMessage {

    private int length;
    private StoreMode mode;
    private byte[] content;
    private int remaining;

    public RedisBulkStringMessage(EasyNettyContext context) {
        super(context);
        this.length = -2;
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

    private void triggerMode(boolean store) {
        if (mode == StoreMode.UNKNOWN) {
            mode = store ? StoreMode.STORE : StoreMode.DISCARD;
        } else if (mode == StoreMode.DISCARD) {
            throw new IllegalStateException("Because the current mode is DISCARD mode, so read content operation can only be invoked once.");
        }
    }

    public JPromise<String> readStringContent() {
        triggerMode(true);
        if (content == null) {
            content = context.readBytes(length).await();
            remaining = 0;
            complete = true;
        } else if (remaining > 0) {
            context.readBytes(content, length - remaining, remaining).await();
            remaining = 0;
            complete = true;
        }
        return JPromise.just(new String(content, StandardCharsets.UTF_8));
    }

    public JPromise<ByteBuf> readSomeRemainingContent(boolean store) {
        triggerMode(store);
        if (remaining == 0) {
            return JPromise.just(null);
        } else {
            ByteBuf buf = context.readSomeBuf(remaining).await();
            if (mode == StoreMode.STORE) {
                int readerIndex = buf.readerIndex();
                buf.readBytes(content, length - remaining, buf.readableBytes());
                buf.readerIndex(readerIndex);
            }
            remaining -= buf.readableBytes();
            if (remaining == 0) {
                complete = true;
            }
            return JPromise.just(buf);
        }
    }
}
