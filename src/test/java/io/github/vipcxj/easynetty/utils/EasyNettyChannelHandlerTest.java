package io.github.vipcxj.easynetty.utils;

import io.github.vipcxj.easynetty.EasyNettyChannelHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EasyNettyChannelHandlerTest {

    private EmbeddedChannel channel;

    @BeforeEach
    void setup() {
        channel = new EmbeddedChannel(false, false);
    }

    private void sendByte(byte b) {
        ByteBuf buf = Unpooled.buffer(1).writeByte(b);
        channel.writeInbound(buf);
    }

    @Test
    void testReadByte() throws Exception {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            byte b = context.readByte().await();
            return context.writeByteAndFlush(b);
        }));
        channel.register();
        sendByte((byte) 1);
        ByteBuf outbound = channel.readOutbound();
        Assertions.assertEquals(1, outbound.readByte());
    }

    @Test
    void testConsumeByte() throws Exception {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            boolean one = context.consumeByte((byte) 1).await();
            boolean two = context.consumeByte((byte) 2).await();
            boolean three = context.consumeByte((byte) 3).await();
            context.writeByte((byte) (one ? 1 : 0)).await();
            context.writeByte((byte) (two ? 1 : 0)).await();
            context.writeByte((byte) (three ? 1 : 0)).await();
            return context.flush();
        }));
        channel.register();
        sendByte((byte) 1);
        sendByte((byte) 3);
        ByteBuf outbound = channel.readOutbound();
        Assertions.assertEquals(1, outbound.readByte());
        outbound = channel.readOutbound();
        Assertions.assertEquals(0, outbound.readByte());
        outbound = channel.readOutbound();
        Assertions.assertEquals(1, outbound.readByte());
    }

    @Test
    void testReadShort() throws Exception {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            short s = context.readShort().await();
            return context.writeShortAndFlush(s);
        }));
        channel.register();
        ByteBuf buf = Unpooled.buffer(2).writeShort(1);
        channel.writeInbound(buf);
        ByteBuf outbound = channel.readOutbound();
        Assertions.assertEquals(1, outbound.readShort());
    }

    @Test
    void testReadInt() throws Exception {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            int i = context.readInt().await();
            return context.writeIntAndFlush(i);
        }));
        channel.register();
        ByteBuf buf = Unpooled.buffer(4).writeInt(1);
        channel.writeInbound(buf);
        ByteBuf outbound = channel.readOutbound();
        Assertions.assertEquals(1, outbound.readInt());
    }
}
