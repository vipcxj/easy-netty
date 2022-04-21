package io.github.vipcxj.easynetty.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BufUtils {

    public static ByteBuf fromString(String str) {
        return fromString(str, StandardCharsets.UTF_8);
    }

    public static ByteBuf fromString(String str, Charset charset) {
        byte[] bytes = str.getBytes(charset);
        return Unpooled.wrappedBuffer(bytes);
    }
}
