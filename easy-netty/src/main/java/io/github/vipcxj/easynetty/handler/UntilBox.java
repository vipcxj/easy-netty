package io.github.vipcxj.easynetty.handler;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class UntilBox {
    private final UntilMode mode;
    private final byte[][] untils;
    private final int[] matched;
    private int matchedUntil;
    private ByteBuf buf;
    private boolean success;

    public UntilBox(UntilMode mode, byte[]... untils) {
        this.mode = mode;
        this.untils = untils;
        this.matched = new int[this.untils.length];
        Arrays.fill(this.matched, 0);
        this.success = false;
        this.buf = null;
    }

    public UntilBox(UntilMode mode, Charset charset, String... untils) {
        this(mode, Arrays.stream(untils).map(s -> s.getBytes(charset)).toArray(byte[][]::new));
    }

    public UntilBox(UntilMode mode, String... untils) {
        this(mode, StandardCharsets.UTF_8, untils);
    }

    public UntilBox(byte[]... untils) {
        this(UntilMode.SKIP, untils);
    }

    public UntilBox(String... untils) {
        this(UntilMode.SKIP, untils);
    }

    public UntilMode getMode() {
        return mode;
    }

    public byte[][] getUntils() {
        return untils;
    }

    public ByteBuf getBuf() {
        return buf;
    }

    void setBuf(ByteBuf buf) {
        this.buf = buf;
    }

    int[] getMatched() {
        return matched;
    }

    public int getMatchedUntil() {
        return matchedUntil;
    }

    void setMatchedUntil(int matchedUntil) {
        this.matchedUntil = matchedUntil;
    }

    public boolean isSuccess() {
        return success;
    }

    void setSuccess(boolean success) {
        this.success = success;
    }
}
