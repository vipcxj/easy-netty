package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.redis.message.RedisIntegerMessage;
import io.github.vipcxj.easynetty.redis.message.RedisMessage;
import io.github.vipcxj.easynetty.redis.message.RedisMessages;
import io.github.vipcxj.easynetty.redis.message.RedisType;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RedisIntegerMessageTest extends AbstractRedisMessageTest {

    @Test
    void testValue() throws Exception {
        final long target = 12345678987654321L;
        prepare(context -> {
            RedisMessage message = RedisMessages.readMessage(context).await();
            Assertions.assertEquals(RedisType.INTEGER, message.type());
            RedisIntegerMessage integerMessage = message.asInteger();
            Assertions.assertEquals(target, integerMessage.value().await());
            return JPromise.empty();
        });
        sendString(":" + target + "\r\n");
        channel.checkException();
    }
}
