package io.github.vipcxj.easynetty.redis.message;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.redis.Utils;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class RedisArrayMessage extends AbstractRedisMessage {

    private boolean ready;
    private int size;
    private int readIndex;
    private StoreMode mode;
    private List<RedisMessage> messages;
    private RedisMessage current;

    public RedisArrayMessage(EasyNettyContext context) {
        super(context);
        this.ready = false;
        this.size = -1;
        this.readIndex = 0;
        this.mode = StoreMode.UNKNOWN;
        this.messages = null;
    }

    public RedisArrayMessage(List<RedisMessage> messages) {
        super(null);
        this.ready = true;
        this.mode = StoreMode.STORE;
        this.messages = messages;
        if (messages == null) {
            this.size = -1;
            this.readIndex = 0;
        } else {
            if (!messages.stream().allMatch(RedisMessage::isComplete)) {
                throw new IllegalArgumentException("All messages must be completed.");
            }
            if (messages.stream().anyMatch(msg -> {
                if (msg.type() == RedisType.ARRAY) {
                    return msg.asArray().getMode() != StoreMode.STORE;
                } else if (msg.type() == RedisType.BULK_STRING) {
                    return msg.asBulkString().getMode() != StoreMode.STORE;
                } else {
                    return false;
                }
            })) {
                throw new IllegalArgumentException("The store mode of all array and bulk string messages must be STORE");
            }
            this.size = messages.size();
            this.readIndex = messages.size();
        }
        markComplete();
    }

    public List<RedisMessage> getMessages() {
        makeSureCompleted();
        if (mode != StoreMode.STORE) {
            throw new UnsupportedOperationException("Only STORE mode support the method getMessages.");
        }
        return messages;
    }

    @Override
    public RedisType type() {
        return RedisType.ARRAY;
    }

    @Override
    public RedisArrayMessage asArray() {
        return this;
    }

    public int getSize() {
        if (!ready) {
            throw new IllegalStateException("Call ready().await() first.");
        }
        return size;
    }

    public StoreMode getMode() {
        return mode;
    }

    public JPromise<Void> ready() {
        if (!ready) {
            size = Math.toIntExact(Utils.readRedisNumber(context).await());
            ready = true;
            if (size >= 0) {
                this.messages = new ArrayList<>(Math.min(size, 256));
            }
        }
        return JPromise.empty();
    }

    /**
     * Provide the iterator of the messages in the array.
     * Get the next message only after reading the current message completely
     * ( Which means <code>message.isComplete() == true<code/> ).
     * After completely read the last message, this array message is completed. <br/>
     * If <code>store<code/> is false, trigger DISCARD mode. The messages have read may be discarded,
     * only the current message is guaranteed to be valid. Or trigger STORE mode, all messages have read are saved.<br/>
     *
     * When this method is invoked at first time, <code>store<code/> parameter decide whether STORE mode or DISCARD mode.<br/>
     * In STORE mode, this method can be called any times, and the follow up <code>store<code/> parameters are ignored.<br/>
     * In DISCARD mode, this method can only be called one time, or a exception will be thrown.
     * @param store true if store the messages, or may discard messages have read
     *              (this means this method can only be safely invoked once)
     * @return the iterator of the messages.
     */
    public JPromise<Iterator<JPromise<RedisMessage>>> messageIterator(boolean store) {
        ready().await();
        if (size < 0) {
            this.mode = StoreMode.STORE;
            this.messages = null;
            markComplete();
            return JPromise.just(null);
        }
        if (mode == StoreMode.UNKNOWN) {
            mode = store ? StoreMode.STORE : StoreMode.DISCARD;
        } else if (mode == StoreMode.DISCARD) {
            throw new IllegalStateException("The method can only be safely called one time when in DISCARD mode.");
        }
        return JPromise.just(new MessageIterator());
    }

    class MessageIterator implements Iterator<JPromise<RedisMessage>> {

        private final ListIterator<RedisMessage> messageIterator = messages.listIterator();
        private int index = 0;

        @Override
        public boolean hasNext() {
            return mode == StoreMode.STORE ? index < size : readIndex < size;
        }

        @Override
        public JPromise<RedisMessage> next() {
            if (mode == StoreMode.STORE && index++ < readIndex) {
                return JPromise.just(messageIterator.next());
            } else {
                if (current != null && !current.isComplete()) {
                    throw new IllegalStateException("Last message is not completed.");
                }
                current = RedisMessages.readMessage(context).await();
                RedisMessage message = current;
                ++readIndex;
                if (mode == StoreMode.STORE) {
                    messageIterator.add(current);
                }
                if (readIndex == size) {
                    current.untilComplete().onSuccess((ctx) -> markComplete()).async();
                    current = null;
                }
                return JPromise.just(message);
            }
        }
    }

    @Override
    public JPromise<Void> complete(boolean skip) {
        if (isComplete()) {
            return JPromise.empty();
        }
        ready().await();
        if (size < 0) {
            this.mode = StoreMode.STORE;
            this.messages = null;
            markComplete();
            return JPromise.empty();
        }
        if (skip || mode == StoreMode.DISCARD) {
            for (int i = readIndex; i < size; ++i) {
                RedisMessage message = RedisMessages.readMessage(context).await();
                message.complete(skip).await();
            }
            this.mode = StoreMode.DISCARD;
            markComplete();
        } else {
            Iterator<JPromise<RedisMessage>> iterator = messageIterator(true).await();
            while (iterator.hasNext()) {
                RedisMessage message = iterator.next().await();
                message.complete(false).await();
            }
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
        buf.writeCharSequence(Integer.toString(size), StandardCharsets.UTF_8);
        Utils.writeRedisLineEnd(buf);
        if (messages != null) {
            for (RedisMessage message : messages) {
                message.writeToByteBuf(buf);
            }
        }
    }

    private boolean equalsMessages(List<RedisMessage> messages) {
        if (this.messages == null || messages == null) {
            return this.messages == messages;
        }
        if (this.messages.size() != messages.size()) {
            return false;
        }
        Iterator<RedisMessage> iter1 = this.messages.iterator();
        Iterator<RedisMessage> iter2 = messages.iterator();
        while (iter1.hasNext()) {
            RedisMessage thisMessage = iter1.next();
            RedisMessage otherMessage = iter2.next();
            if (!Objects.equals(thisMessage, otherMessage)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public JPromise<Void> write(EasyNettyContext outputContext, boolean readIfNeed, boolean storeIfRead) {
        if (!readIfNeed) {
            makeSureCompleted();
        }
        outputContext.writeByte(type().sign()).await();
        ready().await();
        outputContext.writeString(Integer.toString(size)).await();
        outputContext.writeBytes(Utils.REDIS_LINE_END).await();
        Iterator<JPromise<RedisMessage>> iterator = messageIterator(storeIfRead).await();
        if (iterator != null) {
            while (iterator.hasNext()) {
                RedisMessage message = iterator.next().await();
                message.write(outputContext, readIfNeed, storeIfRead).await();
            }
        }
        return JPromise.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RedisArrayMessage that = (RedisArrayMessage) o;
        return ready == that.ready && size == that.size && readIndex == that.readIndex && equalsMessages(that.messages);
    }

    private int hashMessages() {
        if (messages == null) {
            return 0;
        }
        return Objects.hash(messages.toArray());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ready, size, readIndex, hashMessages());
    }
}
