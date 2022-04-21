package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyChannelHandler;
import io.github.vipcxj.easynetty.EasyNettyHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;

import java.nio.charset.StandardCharsets;

public class AbstractRedisMessageTest {

    protected EmbeddedChannel channel;

    @BeforeEach
    void setup() {
        channel = new EmbeddedChannel();
    }

    void prepare(EasyNettyHandler handler) throws Exception {
        channel.pipeline().addLast(new EasyNettyChannelHandler(handler));
        // channel.register();
    }

    void sendString(String msg) {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = channel.alloc().buffer(bytes.length);
        buf.writeBytes(bytes);
        channel.writeInbound(buf);
    }
}
