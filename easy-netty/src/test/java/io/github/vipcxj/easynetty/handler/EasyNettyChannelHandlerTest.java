package io.github.vipcxj.easynetty.handler;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class EasyNettyChannelHandlerTest {

    private EmbeddedChannel channel;

    @BeforeEach
    void setup() {
        channel = new EmbeddedChannel();
    }

    private void sendByte(byte b) {
        ByteBuf buf = Unpooled.buffer(1).writeByte(b);
        channel.writeInbound(buf);
    }

    @Test
    void testReadByte() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            byte b = context.readByte().await();
            return context.writeByteAndFlush(b);
        }));
        sendByte((byte) 1);
        ByteBuf outbound = channel.readOutbound();
        Assertions.assertEquals(1, outbound.readByte());
    }

    @Test
    void testConsumeByte() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            boolean one = context.consumeByte(1).await();
            boolean two = context.consumeByte(2).await();
            boolean three = context.consumeByte(3).await();
            context.writeByte(one ? 1 : 0).await();
            context.writeByte(two ? 1 : 0).await();
            context.writeByte(three ? 1 : 0).await();
            return context.flush();
        }));
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
    void testReadShort() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            short s = context.readShort().await();
            return context.writeShortAndFlush(s);
        }));
        ByteBuf buf = Unpooled.buffer(2).writeShort(1);
        channel.writeInbound(buf);
        ByteBuf outbound = channel.readOutbound();
        Assertions.assertEquals(1, outbound.readShort());
    }

    @Test
    void testReadInt() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            int i = context.readInt().await();
            return context.writeIntAndFlush(i);
        }));
        ByteBuf buf = Unpooled.buffer(4).writeInt(1);
        channel.writeInbound(buf);
        ByteBuf outbound = channel.readOutbound();
        Assertions.assertEquals(1, outbound.readInt());
    }

    @Test
    void testReadBytes1() {
        byte[] input = new byte[] {'a', 'b', 'c', 'd', 'e'};
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            byte[] read = context.readBytes(5).await();
            Assertions.assertArrayEquals(input, read);
            return JPromise.empty();
        }));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
    }

    @Test
    void testReadBytes2() {
        byte[] input = new byte[] {'a', 'b', 'c', 'd', 'e'};
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            byte[] read = context.readBytes(5).await();
            Assertions.assertArrayEquals(input, read);
            return JPromise.empty();
        }));
        channel.writeInbound(Unpooled.wrappedBuffer(input, 0, 2));
        channel.writeInbound(Unpooled.wrappedBuffer(input, 2, 3));
    }

    @Test
    void testReadBytes3() {
        byte[] input = new byte[] {'a', 'b', 'c', 'd', 'e'};
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            byte[] read = new byte[5];
            context.readBytes(read).await();
            Assertions.assertArrayEquals(input, read);
            return JPromise.empty();
        }));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
    }

    @Test
    void testReadBytes4() {
        byte[] input = new byte[] {'a', 'b', 'c', 'd', 'e'};
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            byte[] read = new byte[5];
            context.readBytes(read, 0, 2).await();
            context.readBytes(read, 2, 3).await();
            Assertions.assertArrayEquals(input, read);
            return JPromise.empty();
        }));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
    }

    @Test
    void testConsumeBytes1() {
        byte[] input = new byte[] {'a', 'b', 'c', 'd', 'e', '\r', '\n'};
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            String read = context.readUtf8Until('\r', '\n').await();
            Assertions.assertEquals("abcde", read);
            read = context.readUtf8Until('\r', '\n', EasyNettyContext.UntilMode.SKIP).await();
            Assertions.assertEquals("abcde", read);
            read = context.readUtf8Until('\r', '\n', EasyNettyContext.UntilMode.EXCLUDE).await();
            Assertions.assertEquals("abcde", read);
            byte[] bytes = context.readBytes(2).await();
            Assertions.assertArrayEquals(new byte[] {'\r', '\n'}, bytes);
            read = context.readUtf8Until('\r', '\n', EasyNettyContext.UntilMode.INCLUDE).await();
            Assertions.assertEquals("abcde\r\n", read);
            return JPromise.empty();
        }));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
    }

    @Test
    void testConsumeBytes2() {
        byte[] input = new byte[] {'a', 'b', 'c', 'd', 'e', '\r', '\n'};
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            String read = context.readUtf8Until('\r').await();
            Assertions.assertEquals("abcde", read);
            context.skip(1).await();
            read = context.readUtf8Until('c', 'd').await();
            Assertions.assertEquals("ab", read);
            context.skip(3).await();
            read = context.readUtf8Until('d', 'e', '\r').await();
            Assertions.assertEquals("abc", read);
            context.skip(1).await();
            return JPromise.empty();
        }));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
    }

    private final int M = 1024 * 1024;

    private ByteBuf createBigBuf() {
        int size = 16 * M;
        Random random = new Random();
        ByteBuf buf = Unpooled.buffer(size);
        int write = 0;
        while (write < size) {
            buf.writeLong(random.nextLong());
            write += 8;
        }
        return buf;
    }

    @Test
    void testReadSomeBuf() {
        ByteBuf buf = createBigBuf();
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            int size = 4 * M;
            ByteBuf cache = context.getChannelContext().alloc().buffer(size);
            while (cache.readableBytes() < size) {
                ByteBuf b = context.readSomeBuf(size).await();
                cache.writeBytes(b);
            }
            Assertions.assertEquals(buf.slice(0, size), cache);
            return JPromise.empty();
        }));
        for (int i = 0; i < 16; ++i) {
            channel.writeInbound(buf.slice(i * M, M / 2));
            channel.writeInbound(buf.slice(i * M + M / 2, M / 2));
        }
    }
}
