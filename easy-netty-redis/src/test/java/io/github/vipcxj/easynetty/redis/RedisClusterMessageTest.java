package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.redis.bus.RedisClusterMessage;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RedisClusterMessageTest extends AbstractRedisMessageTest {

    @Test
    void testHeader() throws InterruptedException {
        prepare(context -> {
            RedisClusterMessage message = new RedisClusterMessage(context);
            message.readHeader().await();
            return JPromise.empty();
        });
        RedisClusterMessage message = RedisClusterMessage.createGossipMessage(RedisClusterMessage.CLUSTERMSG_TYPE_PING, 3, 0);
        message.setClusterBusPort(16379);
        message.setConfigEpoch(1);
        message.setCurrentEpoch(1);
        message.setMyIp("127.0.0.1");
        message.setPort(6379);
        for (int i = 0; i < 3; ++i) {
            message.setNthIp(i, "192.168.0." + (i + 1));
            message.setNthPort(i, 6380 + i);
        }
        getContext().writeBytes(message.getHeader()).then(() -> getContext().writeBytesAndFlush(message.getData())).block();
        ByteBuf headBuf = channel.readOutbound();
        ByteBuf dataBuf = channel.readOutbound();
        Assertions.assertEquals(Unpooled.wrappedBuffer(message.getHeader()), headBuf);
        Assertions.assertEquals(Unpooled.wrappedBuffer(message.getData()), dataBuf);
    }
}
