package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RedisErrorMessageTest extends AbstractRedisMessageTest {

    @Test
    void testContent() throws Exception {
        final String testString = "ERR test simple error";
        prepare(context -> {
            RedisMessage message = RedisMessages.readMessage(context).await();
            Assertions.assertEquals(RedisType.ERROR, message.type());
            RedisErrorMessage errorMessage = message.asError();
            String error = errorMessage.content().await();
            Assertions.assertEquals(testString, error);
            return JPromise.empty();
        });
        sendString("-" + testString + "\r\n");
        channel.checkException();
        Thread.sleep(1000);
    }
}
