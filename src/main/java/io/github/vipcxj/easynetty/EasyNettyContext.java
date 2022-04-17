package io.github.vipcxj.easynetty;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface EasyNettyContext {

    ChannelHandlerContext getChannelContext();

    JPromise<Byte> readByte();

    JPromise<Boolean> consumeByte(byte expected);

    JPromise<Short> readShort();

    JPromise<Boolean> consumeShort(short expected);

    JPromise<Integer> readInt();

    JPromise<Boolean> consumeInt(int expected);

    JPromise<Integer> readUtf8CodePoint();

    JPromise<Boolean> consumeUtf8CodePoint(int codePoint);

    JPromise<Void> readUtf8Until(ByteBuf output, int codePoint, UntilMode mode);

    JPromise<Void> readUtf8Until(ByteBuf output, int cp0, int cp1, UntilMode mode);

    JPromise<Void> readUtf8Until(ByteBuf output, int cp0, int cp1, int cp2, UntilMode mode);

    JPromise<Void> readBytes(byte[] outputBytes);

    JPromise<Void> readBytes(byte[] outputBytes, int offset, int length);

    default JPromise<byte[]> readBytes(int length) {
        byte[] bytes = new byte[length];
        return readBytes(bytes).thenReturn(bytes);
    }

    JPromise<Boolean> consumeBytes(byte[] expectedBytes);

    JPromise<Boolean> consumeBytes(byte[] expectedBytes, int offset, int length);

    JPromise<Void> writeBuffer(ByteBuf buf);

    JPromise<Void> writeBufferAndFlush(ByteBuf buf);

    JPromise<Void> writeByte(byte b);

    JPromise<Void> writeByteAndFlush(byte b);

    JPromise<Void> writeBytes(byte[] bytes);

    JPromise<Void> writeBytesAndFlush(byte[] bytes);

    JPromise<Void> writeShort(short s);

    JPromise<Void> writeShortAndFlush(short s);

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

    enum UntilMode {
        INCLUDE, EXCLUDE, SKIP
    }
}
