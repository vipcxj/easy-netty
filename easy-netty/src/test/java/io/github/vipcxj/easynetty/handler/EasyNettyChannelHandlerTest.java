package io.github.vipcxj.easynetty.handler;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.EasyNettyHandler;
import io.github.vipcxj.easynetty.client.AbstractSocketClient;
import io.github.vipcxj.easynetty.server.AbstractEasyNettyServer;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

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
            Assertions.assertTrue(one);
            boolean two = context.consumeByte(2).await();
            Assertions.assertFalse(two);
            boolean three = context.consumeByte(3).await();
            Assertions.assertTrue(three);
            return JPromise.empty();
        }));
        sendByte((byte) 1);
        sendByte((byte) 3);
    }

    private void sendShorts() {
        ByteBuf buf1 = Unpooled.buffer(2).writeShort(1);
        channel.writeInbound(buf1);
        ByteBuf buf2 = Unpooled.buffer(2).writeShort(2);
        channel.writeInbound(buf2.slice(0, 1));
        channel.writeInbound(buf2.retainedSlice(1, 1));
    }

    @Test
    void testReadShort() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            short s = context.readShort().await();
            Assertions.assertEquals(1, s);
            s = context.readShort().await();
            Assertions.assertEquals(2, s);
            return JPromise.empty();
        }));
        sendShorts();
    }

    @Test
    void testConsumeShort() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            boolean test;
            test = context.consumeShort(2).await();
            Assertions.assertFalse(test);
            test = context.consumeShort(1).await();
            Assertions.assertTrue(test);
            test = context.consumeShort(1).await();
            Assertions.assertFalse(test);
            test = context.consumeShort(2).await();
            Assertions.assertTrue(test);
            return JPromise.empty();
        }));
        sendShorts();
    }

    private void sendInts() {
        ByteBuf buf1 = Unpooled.buffer(4).writeInt(1);
        channel.writeInbound(buf1);
        ByteBuf buf2 = Unpooled.buffer(4).writeInt(2);
        channel.writeInbound(buf2.slice(0, 1));
        channel.writeInbound(buf2.retainedSlice(1, 3));
        ByteBuf buf3 = Unpooled.buffer(4).writeInt(3);
        channel.writeInbound(buf3.slice(0, 2));
        channel.writeInbound(buf3.retainedSlice(2, 1));
        channel.writeInbound(buf3.retainedSlice(3, 1));
    }

    @Test
    void testReadInt() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            int i = context.readInt().await();
            Assertions.assertEquals(1, i);
            i = context.readInt().await();
            Assertions.assertEquals(2, i);
            i = context.readInt().await();
            Assertions.assertEquals(3, i);
            return JPromise.empty();
        }));
        sendInts();
    }

    @Test
    void testConsumeInt() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            boolean test;
            test = context.consumeInt(3).await();
            Assertions.assertFalse(test);
            test = context.consumeInt(1).await();
            Assertions.assertTrue(test);
            test = context.consumeInt(1).await();
            Assertions.assertFalse(test);
            test = context.consumeInt(2).await();
            Assertions.assertTrue(test);
            test = context.consumeInt(2).await();
            Assertions.assertFalse(test);
            test = context.consumeInt(3).await();
            Assertions.assertTrue(test);
            return JPromise.empty();
        }));
        sendInts();
    }

    private void sendLongs() {
        ByteBuf buf1 = Unpooled.buffer(8).writeLong(1);
        channel.writeInbound(buf1);
        ByteBuf buf2 = Unpooled.buffer(8).writeLong(2);
        channel.writeInbound(buf2.slice(0, 1));
        channel.writeInbound(buf2.retainedSlice(1, 7));
        ByteBuf buf3 = Unpooled.buffer(8).writeLong(3);
        channel.writeInbound(buf3.slice(0, 2));
        channel.writeInbound(buf3.retainedSlice(2, 3));
        channel.writeInbound(buf3.retainedSlice(5, 3));
    }

    @Test
    void testReadLong() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            long i = context.readLong().await();
            Assertions.assertEquals(1, i);
            i = context.readLong().await();
            Assertions.assertEquals(2, i);
            i = context.readLong().await();
            Assertions.assertEquals(3, i);
            return JPromise.empty();
        }));
        sendLongs();
    }

    @Test
    void testConsumeLong() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            boolean test;
            test = context.consumeLong(2).await();
            Assertions.assertFalse(test);
            test = context.consumeLong(1).await();
            Assertions.assertTrue(test);
            test = context.consumeLong(3).await();
            Assertions.assertFalse(test);
            test = context.consumeLong(2).await();
            Assertions.assertTrue(test);
            test = context.consumeLong(1).await();
            Assertions.assertFalse(test);
            test = context.consumeLong(3).await();
            Assertions.assertTrue(test);
            return JPromise.empty();
        }));
        sendLongs();
    }

    private final byte[] b1 = "easy-netty".getBytes(StandardCharsets.UTF_8);
    private final byte[] b2 = "éáșy-netțy".getBytes(StandardCharsets.UTF_8);
    private final byte[] b3 = "简单-netty".getBytes(StandardCharsets.UTF_8);
    private final byte[] b4 = "emoji:\uD83D\uDE01".getBytes(StandardCharsets.UTF_8);

    private void sendUtf8Bytes() {
        channel.writeInbound(Unpooled.wrappedBuffer(b1));
        channel.writeInbound(Unpooled.wrappedBuffer(b1, 0, 6));
        channel.writeInbound(Unpooled.wrappedBuffer(b1, 6, b1.length - 6));
        channel.writeInbound(Unpooled.wrappedBuffer(b2));
        channel.writeInbound(Unpooled.wrappedBuffer(b2, 0, 3));
        channel.writeInbound(Unpooled.wrappedBuffer(b2, 3, b2.length - 3));
        channel.writeInbound(Unpooled.wrappedBuffer(b3));
        channel.writeInbound(Unpooled.wrappedBuffer(b3, 0, 4));
        channel.writeInbound(Unpooled.wrappedBuffer(b3, 4, b3.length - 4));
        channel.writeInbound(Unpooled.wrappedBuffer(b4));
        channel.writeInbound(Unpooled.wrappedBuffer(b4, 0, 8));
        channel.writeInbound(Unpooled.wrappedBuffer(b4, 8, b4.length - 8));
    }

    private JPromise<Void> testReadUtf8String(EasyNettyContext context, byte[] b) {
        String s = new String(b, StandardCharsets.UTF_8);
        int[] cps = s.codePoints().toArray();
        for (int cp : cps) {
            Integer read = context.readUtf8CodePoint().await();
            Assertions.assertEquals(cp, read);
        }
        return JPromise.empty();
    }

    @Test
    void testReadUtf8CodePoint() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            testReadUtf8String(context, b1).await();
            testReadUtf8String(context, b1).await();
            testReadUtf8String(context, b2).await();
            testReadUtf8String(context, b2).await();
            testReadUtf8String(context, b3).await();
            testReadUtf8String(context, b3).await();
            testReadUtf8String(context, b4).await();
            testReadUtf8String(context, b4).await();
            return JPromise.empty();
        }));
        sendUtf8Bytes();
    }

    private JPromise<Void> testConsumeUtf8String(EasyNettyContext context, byte[] b) {
        String s = new String(b, StandardCharsets.UTF_8);
        int[] cps = s.codePoints().toArray();
        for (int cp : cps) {
            Assertions.assertFalse(context.consumeUtf8CodePoint(cp + 1).await());
            Assertions.assertTrue(context.consumeUtf8CodePoint(cp).await());
        }
        return JPromise.empty();
    }

    @Test
    void testConsumeUtf8CodePoint() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            testConsumeUtf8String(context, b1).await();
            testConsumeUtf8String(context, b1).await();
            testConsumeUtf8String(context, b2).await();
            testConsumeUtf8String(context, b2).await();
            testConsumeUtf8String(context, b3).await();
            testConsumeUtf8String(context, b3).await();
            testConsumeUtf8String(context, b4).await();
            testConsumeUtf8String(context, b4).await();
            return JPromise.empty();
        }));
        sendUtf8Bytes();
    }

    private void sendBytes(byte[] input) {
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        channel.writeInbound(Unpooled.wrappedBuffer(input, 0, 2));
        channel.writeInbound(Unpooled.wrappedBuffer(input, 2, 3));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        channel.writeInbound(Unpooled.wrappedBuffer(input, 0, 2));
        channel.writeInbound(Unpooled.wrappedBuffer(input, 2, 3));
    }

    @Test
    void testReadBytes() {
        byte[] input = new byte[] {'a', 'b', 'c', 'd', 'e'};
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            byte[] read = context.readBytes(5).await();
            Assertions.assertArrayEquals(input, read);
            read = context.readBytes(5).await();
            Assertions.assertArrayEquals(input, read);
            Arrays.fill(read, (byte) 0);
            context.readBytes(read).await();
            Assertions.assertArrayEquals(input, read);
            Arrays.fill(read, (byte) 0);
            context.readBytes(read, 0, 3).await();
            context.readBytes(read, 3, 2).await();
            Assertions.assertArrayEquals(input, read);
            return JPromise.empty();
        }));
        sendBytes(input);
    }

    @Test
    void testConsumeBytes() {
        byte[] input = new byte[] {'a', 'b', 'c', 'd', 'e'};
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            byte[] expected = input.clone();
            boolean test;
            expected[3] = 0;
            test = context.consumeBytes(expected).await();
            Assertions.assertFalse(test);
            expected[3] = input[3];
            test = context.consumeBytes(expected).await();
            Assertions.assertTrue(test);
            expected[4] = 0;
            test = context.consumeBytes(expected).await();
            Assertions.assertFalse(test);
            expected[4] = input[4];
            test = context.consumeBytes(expected).await();
            Assertions.assertTrue(test);
            expected[2] = 0;
            test = context.consumeBytes(expected, 0, 3).await();
            Assertions.assertFalse(test);
            test = context.consumeBytes(expected, 2, 3).await();
            Assertions.assertFalse(test);
            expected[2] = input[2];
            test = context.consumeBytes(expected, 0, 3).await();
            Assertions.assertTrue(test);
            test = context.consumeBytes(expected, 3, 2).await();
            Assertions.assertTrue(test);
            expected[3] = 0;
            test = context.consumeBytes(expected, 0, 4).await();
            Assertions.assertFalse(test);
            test = context.consumeBytes(expected, 3, 2).await();
            Assertions.assertFalse(test);
            expected[3] = input[3];
            test = context.consumeBytes(expected, 0, 3).await();
            Assertions.assertTrue(test);
            test = context.consumeBytes(expected, 3, 2).await();
            Assertions.assertTrue(test);
            return JPromise.empty();
        }));
        sendBytes(input);
    }

    @Test
    void testReadUtf8Until1() {
        byte[] input = new byte[] {'a', 'b', 'c', 'd', 'e', '\r', '\n'};
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            String read = context.readUtf8Until('\r', '\n').await();
            Assertions.assertEquals("abcde", read);
            read = context.readUtf8Until('\r', '\n', UntilMode.SKIP).await();
            Assertions.assertEquals("abcde", read);
            read = context.readUtf8Until('\r', '\n', UntilMode.EXCLUDE).await();
            Assertions.assertEquals("abcde", read);
            byte[] bytes = context.readBytes(2).await();
            Assertions.assertArrayEquals(new byte[] {'\r', '\n'}, bytes);
            read = context.readUtf8Until('\r', '\n', UntilMode.INCLUDE).await();
            Assertions.assertEquals("abcde\r\n", read);
            return JPromise.empty();
        }));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
        channel.writeInbound(Unpooled.wrappedBuffer(input));
    }

    @Test
    void testReadUtf8Until2() {
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

    private void sendBigBuf(AbstractSocketClient client, ByteBuf buf) {
        for (int i = 0; i < 16; ++i) {
            client.getChannel().writeAndFlush(buf.retainedSlice(i * M, M / 2));
            client.getChannel().writeAndFlush(buf.retainedSlice(i * M + M / 2, M / 2));
        }
    }

    @Test
    void testReadSomeBuf() throws InterruptedException {
        ByteBuf buf = createBigBuf();
        JPromiseTrigger<Object> completeTrigger = JPromise.createTrigger();
        AbstractEasyNettyServer server = setupServer(context -> {
            int size = 4 * M;
            ByteBuf cache = context.getChannelContext().alloc().buffer(size);
            int remaining = size;
            while (remaining > 0) {
                ByteBuf b = context.readSomeBuf(remaining).await();
                if (b.isReadable()) {
                    remaining -= b.readableBytes();
                    cache.writeBytes(b);
                }
            }
            Assertions.assertEquals(buf.slice(0, size), cache);
            completeTrigger.resolve(null);
            return JPromise.empty();
        });
        AbstractSocketClient client = createClient();
        sendBigBuf(client, buf);
        completeTrigger.getPromise()
                .doFinally(() -> client.close(true))
                .doFinally(server::close).block();
    }

    private JPromise<Void> checkReadSomeBufUntilAny(EasyNettyContext context, UntilBox untilBox, ByteBuf expected) {
        CompositeByteBuf buf = context.getChannelContext().alloc().compositeBuffer();
        untilBox = context.readSomeBufUntilAny(untilBox).await();
        buf.addComponent(true, untilBox.getBuf());
        while (!untilBox.isSuccess()) {
            untilBox = context.readSomeBufUntilAny(untilBox).await();
            buf.addComponent(true, untilBox.getBuf());
        }
        byte[] until;
        switch (untilBox.getMode()) {
            case SKIP:
                Assertions.assertEquals(expected, buf);
                break;
            case EXCLUDE:
                Assertions.assertEquals(expected, buf);
                until = untilBox.getUntils()[untilBox.getMatchedUntil()];
                Assertions.assertTrue(context.consumeBytes(until).await());
                break;
            case INCLUDE:
                CompositeByteBuf buf2 = context.getChannelContext().alloc().compositeBuffer();
                buf2.addComponent(true, expected);
                until = untilBox.getUntils()[untilBox.getMatchedUntil()];
                buf2.writeBytes(until);
                Assertions.assertEquals(buf2, buf);
                break;
            default:
                Assertions.fail();
        }
        return JPromise.empty();
    }

    private void sendBigDataWithDelimiter(AbstractSocketClient client, ByteBuf buf, byte[] delimiter1, byte[] delimiter2, byte[] delimiter3) {
        for (int i = 0; i < 16; ++i) {
            client.getChannel().writeAndFlush(buf.retainedSlice(i * M, M));
            byte[] delimiter;
            if (i % 3 == 0) {
                delimiter = delimiter1;
            } else if (i % 3 == 1) {
                delimiter = delimiter2;
            } else {
                delimiter = delimiter3;
            }
            client.getChannel().writeAndFlush(Unpooled.wrappedBuffer(delimiter, 0, 5));
            client.getChannel().writeAndFlush(Unpooled.wrappedBuffer(delimiter, 5, 5));
            client.getChannel().writeAndFlush(Unpooled.wrappedBuffer(delimiter, 10, delimiter.length - 10));
        }
    }

    @Test
    void testReadSomeBufUntilAny() throws InterruptedException {
        ByteBuf buf = createBigBuf();
        byte[] delimiter1 = ("这是一个分隔符" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        byte[] delimiter2 = ("这是一个分隔符" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        byte[] delimiter3 = ("这是一个分隔符" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        JPromiseTrigger<Object> completeTrigger = JPromise.createTrigger();
        AbstractEasyNettyServer server = setupServer(context -> {
            UntilBox untilBox = new UntilBox(UntilMode.SKIP, delimiter1, delimiter2, delimiter3);
            checkReadSomeBufUntilAny(context, untilBox, buf.slice(0, M)).await();
            checkReadSomeBufUntilAny(context, untilBox, buf.slice(M, M)).await();
            untilBox = new UntilBox(UntilMode.EXCLUDE, delimiter1, delimiter2, delimiter3);
            checkReadSomeBufUntilAny(context, untilBox, buf.slice(2 * M, M)).await();
            checkReadSomeBufUntilAny(context, untilBox, buf.slice(3 * M, M)).await();
            untilBox = new UntilBox(UntilMode.INCLUDE, delimiter1, delimiter2, delimiter3);
            checkReadSomeBufUntilAny(context, untilBox, buf.slice(4 * M, M)).await();
            checkReadSomeBufUntilAny(context, untilBox, buf.slice(5 * M, M)).await();
            completeTrigger.resolve(null);
            return JPromise.empty();
        });
        AbstractSocketClient client = createClient();
        sendBigDataWithDelimiter(client, buf, delimiter1, delimiter2, delimiter3);
        completeTrigger.getPromise().doFinally(() -> client.close(true)).doFinally(server::close).block();
    }

    @Test
    void testWriteTooManyParts() {
        channel.pipeline().addLast(new EasyNettyChannelHandler(context -> {
            for (int i = 0; i < 10000; ++i) {
                context.writeString(Integer.toString(i)).await();
            }
            return context.flush();
        }));
        channel.writeInbound(Unpooled.EMPTY_BUFFER);
        for (int i = 0; i < 10000; ++i) {
            ByteBuf buf = channel.readOutbound();
            Assertions.assertEquals(Integer.toString(i), buf.toString(StandardCharsets.UTF_8));
        }
    }

    private AbstractEasyNettyServer setupServer(EasyNettyHandler handler) throws InterruptedException {
        AbstractEasyNettyServer server = new AbstractEasyNettyServer(11000) {

            @Override
            public JPromise<Void> handle(EasyNettyContext context) {
                return handler.handle(context);
            }
        };
        server.bind().block();
        return server;
    }

    private AbstractSocketClient createClient() throws InterruptedException {
        AbstractSocketClient client = new AbstractSocketClient() {

            @Override
            protected void configure(Bootstrap bootstrap) {
                bootstrap.handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                ReferenceCountUtil.release(msg);
                            }
                        });
                    }
                });
            }
        };
        client.connect("localhost", 11000).block();
        return client;
    }
}
