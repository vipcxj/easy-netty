package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

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

    @Override
    public RedisType type() {
        return RedisType.ARRAY;
    }

    @Override
    public RedisArrayMessage asArray() {
        return this;
    }

    private JPromise<Void> ready() {
        if (!ready) {
            size = Math.toIntExact(Utils.readRedisNumber(context).await());
            ready = true;
            if (size >= 0) {
                this.messages = new ArrayList<>(Math.min(size, 1024));
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
                ++readIndex;
                if (mode == StoreMode.STORE) {
                    messageIterator.add(current);
                }
                if (readIndex == size) {
                    current = null;
                    complete = true;
                }
                return JPromise.just(current);
            }
        }
    }
}
