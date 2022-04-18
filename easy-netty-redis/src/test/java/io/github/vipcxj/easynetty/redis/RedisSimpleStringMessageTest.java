package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RedisSimpleStringMessageTest extends AbstractRedisMessageTest {

    @Test
    void testContent() throws Exception {
        final String testString = "test simple string";
        prepare(ctx -> {
            RedisMessage message = RedisMessages.readMessage(ctx).await();
            Assertions.assertEquals(RedisType.SIMPLE_STRING, message.type());
            RedisSimpleStringMessage simpleStringMessage = message.asSimpleString();
            String content = simpleStringMessage.content().await();
            Assertions.assertEquals(testString, content);
            return JPromise.empty();
        });
        sendString("+" + testString + "\r\n");
        channel.checkException();
    }
}
