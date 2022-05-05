package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.redis.bus.RedisClusterMessage;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import org.junit.jupiter.api.Test;

public class RedisClusterMessageTest extends AbstractRedisMessageTest {

    @Test
    void testHeader() {
        prepare(context -> {
            RedisClusterMessage message = new RedisClusterMessage(context);
            message.readHeader().await();
            return JPromise.empty();
        });
    }
}
