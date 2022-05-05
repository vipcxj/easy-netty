package io.github.vipcxj.easynetty;

import io.github.vipcxj.easynetty.handler.UntilBox;
import io.github.vipcxj.easynetty.handler.UntilMode;
import io.github.vipcxj.easynetty.utils.Tuple2OwB;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface EasyNettyContext {

    ChannelHandlerContext getChannelContext();

    void mark();

    void resetMark();

    void cleanMark();

    JPromise<Byte> readByte();

    JPromise<Boolean> consumeByte(int expected);

    JPromise<Short> readShort();

    JPromise<Boolean> consumeShort(int expected);

    JPromise<Integer> readInt();

    JPromise<Boolean> consumeInt(int expected);

    JPromise<Long> readLong();

    JPromise<Boolean> consumeLong(long expected);

    JPromise<Integer> readUtf8CodePoint();

    JPromise<Boolean> consumeUtf8CodePoint(int codePoint);

    JPromise<Void> readUtf8Until(ByteBuf output, int codePoint, UntilMode mode);

    JPromise<Void> readUtf8Until(ByteBuf output, int cp0, int cp1, UntilMode mode);

    JPromise<Void> readUtf8Until(ByteBuf output, int cp0, int cp1, int cp2, UntilMode mode);

    default JPromise<Void> readUtf8Until(ByteBuf output, int codePoint) {
        return readUtf8Until(output, codePoint, UntilMode.SKIP);
    }

    default JPromise<Void> readUtf8Until(ByteBuf output, int cp0, int cp1) {
        return readUtf8Until(output, cp0, cp1, UntilMode.SKIP);
    }

    default JPromise<Void> readUtf8Until(ByteBuf output, int cp0, int cp1, int cp2) {
        return readUtf8Until(output, cp0, cp1, cp2, UntilMode.SKIP);
    }

    default JPromise<String> readUtf8Until(int cp, UntilMode mode) {
        ByteBuf buffer = getChannelContext().alloc().buffer();
        return readUtf8Until(buffer, cp, mode)
                .thenMapImmediate(() -> buffer.toString(StandardCharsets.UTF_8))
                .onFinally((Runnable) buffer::release);
    }

    default JPromise<String> readUtf8Until(int cp0, int cp1, UntilMode mode) {
        ByteBuf buffer = getChannelContext().alloc().buffer();
        return readUtf8Until(buffer, cp0, cp1, mode)
                .thenMapImmediate(() -> buffer.toString(StandardCharsets.UTF_8))
                .onFinally((Runnable) buffer::release);
    }

    default JPromise<String> readUtf8Until(int cp0, int cp1, int cp2, UntilMode mode) {
        ByteBuf buffer = getChannelContext().alloc().buffer();
        return readUtf8Until(buffer, cp0, cp1, cp2, mode)
                .thenMapImmediate(() -> buffer.toString(StandardCharsets.UTF_8))
                .onFinally((Runnable) buffer::release);
    }

    default JPromise<String> readUtf8Until(int cp) {
        return readUtf8Until(cp, UntilMode.SKIP);
    }

    default JPromise<String> readUtf8Until(int cp0, int cp1) {
        return readUtf8Until(cp0, cp1, UntilMode.SKIP);
    }

    default JPromise<String> readUtf8Until(int cp0, int cp1, int cp2) {
        return readUtf8Until(cp0, cp1, cp2, UntilMode.SKIP);
    }

    JPromise<Void> readBytes(byte[] outputBytes);

    JPromise<Void> readBytes(byte[] outputBytes, int offset, int length);

    default JPromise<byte[]> readBytes(int length) {
        byte[] bytes = new byte[length];
        return readBytes(bytes).thenReturn(bytes);
    }

    JPromise<Boolean> consumeBytes(byte[] expectedBytes);

    JPromise<Boolean> consumeBytes(byte[] expectedBytes, int offset, int length);

    JPromise<Void> skip(long length);

//    <T> JPromise<T> readBigData(long length, JAsyncPromiseFunction0<ByteBuf, T> handler);

    JPromise<ByteBuf> readSomeBuf(int maxLen, boolean ignoreEmpty);

    default JPromise<ByteBuf> readSomeBuf(int maxLen) {
        return readSomeBuf(maxLen, true);
    }

    JPromise<UntilBox> readSomeBufUntilAny(UntilBox untilBox);

    JPromise<Void> writeBuffer(ByteBuf buf);

    JPromise<Void> writeBufferAndFlush(ByteBuf buf);

    JPromise<Void> writeByte(int b);

    JPromise<Void> writeByteAndFlush(int b);

    JPromise<Void> writeBytes(byte[] bytes);

    JPromise<Void> writeBytesAndFlush(byte[] bytes);

    JPromise<Void> writeShort(int s);

    JPromise<Void> writeShortAndFlush(int s);

    JPromise<Void> writeInt(int i);

    JPromise<Void> writeIntAndFlush(int i);

    JPromise<Void> writeString(String s, Charset charset);

    JPromise<Void> writeStringAndFlush(String s, Charset charset);

    default JPromise<Void> writeString(String s) {
        return writeString(s, StandardCharsets.UTF_8);
    }

    default JPromise<Void> writeStringAndFlush(String s) {
        return writeStringAndFlush(s, StandardCharsets.UTF_8);
    }

    default JPromise<Void> flush() {
        return writeBufferAndFlush(Unpooled.EMPTY_BUFFER);
    }
}
