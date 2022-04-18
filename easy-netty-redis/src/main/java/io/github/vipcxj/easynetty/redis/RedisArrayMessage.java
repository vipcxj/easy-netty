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
    private List<RedisMessage> messages;

    public RedisArrayMessage(EasyNettyContext context) {
        super(context);
        this.ready = false;
        this.size = -1;
        this.readIndex = 0;
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

    public JPromise<Iterator<JPromise<RedisMessage>>> messageIterator() {
        ready().await();
        if (size < 0) {
            return JPromise.just(null);
        }
        return JPromise.just(new MessageIterator());
    }

    class MessageIterator implements Iterator<JPromise<RedisMessage>> {

        private final ListIterator<RedisMessage> messageIterator = messages.listIterator();
        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public JPromise<RedisMessage> next() {
            if (index++ < readIndex) {
                return JPromise.just(messageIterator.next());
            } else {
                ++readIndex;
                RedisMessage message = RedisMessages.readMessage(context).await();
                messageIterator.add(message);
                return JPromise.just(message);
            }
        }
    }
}
