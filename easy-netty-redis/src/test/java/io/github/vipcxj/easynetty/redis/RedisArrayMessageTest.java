package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.redis.message.*;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RedisArrayMessageTest extends AbstractRedisMessageTest {

    private RedisArrayMessage testArrayMessage;

    @BeforeEach
    @Override
    void setup() {
        super.setup();
        List<RedisMessage> messages = new ArrayList<>();
        messages.add(new RedisIntegerMessage(1));
        messages.add(new RedisErrorMessage("ERR"));
        messages.add(new RedisSimpleStringMessage("OK"));
        messages.add(new RedisBulkStringMessage("ping".getBytes(StandardCharsets.UTF_8)));
        List<RedisMessage> subMessages = new ArrayList<>();
        subMessages.add(new RedisSimpleStringMessage("NOT OK"));
        messages.add(new RedisArrayMessage(subMessages));
        testArrayMessage = new RedisArrayMessage(messages);
    }

    @Test
    void testDiscardMode() {
        prepare(context -> {
            RedisMessage message = RedisMessages.readMessage(context).await();
            Assertions.assertEquals(RedisType.ARRAY, message.type());
            RedisArrayMessage arrayMessage = message.asArray();
            Iterator<JPromise<RedisMessage>> iterator = arrayMessage.messageIterator(false).await();
            int i = 0;
            while (iterator.hasNext()) {
                RedisMessage msg = iterator.next().await();
                msg.complete().await();
                Assertions.assertEquals(testArrayMessage.getMessages().get(i++), msg);
            }
            Assertions.assertTrue(arrayMessage.isComplete());
            return JPromise.empty();
        });
        ByteBuf buffer = channel.alloc().buffer();
        testArrayMessage.writeToByteBuf(buffer);
        channel.writeInbound(buffer);
    }

    private JPromise<Void> writeToContext() {
        EasyNettyContext context = getContext();
        RedisMessage message = RedisMessages.readMessage(context).await();
        Assertions.assertEquals(RedisType.ARRAY, message.type());
        RedisArrayMessage arrayMessage = message.asArray();
        Iterator<JPromise<RedisMessage>> iterator = arrayMessage.messageIterator(true).await();
        Assertions.assertTrue(iterator.hasNext());
        RedisMessage firstMessage = iterator.next().await();
        firstMessage.complete().await();
        message.write(context, true, false).await();
        context.flush().await();
        return JPromise.empty();
    }

    @Test
    void testWriteToContext() throws InterruptedException {
        prepare(context -> JPromise.empty());
        ByteBuf buffer = channel.alloc().buffer();
        testArrayMessage.writeToByteBuf(buffer);
        channel.writeInbound(buffer.retainedSlice());
        writeToContext().block();
        CompositeByteBuf actual = channel.alloc().compositeBuffer();
        while (true) {
            ByteBuf out = channel.readOutbound();
            if (out != null) {
                actual.addComponent(true, out);
            } else {
                break;
            }
        }
        Assertions.assertEquals(buffer, actual);
        actual.release();
        buffer.release();
    }
}
