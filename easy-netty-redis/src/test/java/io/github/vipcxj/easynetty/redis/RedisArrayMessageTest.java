package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RedisArrayMessageTest extends AbstractRedisMessageTest {

    @Test
    void testDiscardMode() {
        List<RedisMessage> messages = new ArrayList<>();
        messages.add(new RedisIntegerMessage(1));
        messages.add(new RedisErrorMessage("ERR"));
        messages.add(new RedisSimpleStringMessage("OK"));
        messages.add(new RedisBulkStringMessage("ping".getBytes(StandardCharsets.UTF_8)));
        List<RedisMessage> subMessages = new ArrayList<>();
        subMessages.add(new RedisSimpleStringMessage("NOT OK"));
        messages.add(new RedisArrayMessage(subMessages));
        RedisArrayMessage inputArrayMessages = new RedisArrayMessage(messages);
        prepare(context -> {
            RedisMessage message = RedisMessages.readMessage(context).await();
            Assertions.assertEquals(RedisType.ARRAY, message.type());
            RedisArrayMessage arrayMessage = message.asArray();
            Iterator<JPromise<RedisMessage>> iterator = arrayMessage.messageIterator(false).await();
            int i = 0;
            while (iterator.hasNext()) {
                RedisMessage msg = iterator.next().await();
                msg.complete().await();
                Assertions.assertEquals(messages.get(i++), msg);
            }
            Assertions.assertTrue(arrayMessage.isComplete());
            return JPromise.empty();
        });
        ByteBuf buffer = channel.alloc().buffer();
        inputArrayMessages.writeToByteBuf(buffer);
        channel.writeInbound(buffer);
    }
}
