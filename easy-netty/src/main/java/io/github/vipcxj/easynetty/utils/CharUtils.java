package io.github.vipcxj.easynetty.utils;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

import static java.lang.Character.MAX_SURROGATE;
import static java.lang.Character.MIN_SURROGATE;

public class CharUtils {

    private static int charLen(byte b) {
        if (b >= 0) {
            return 1;
        } else if (b >= -16) {
            return (b > -12) ? -2 : 4;
        } else if (b >= -32) {
            return 3;
        } else if (b >= -64) {
            return (b <= -63) ? -2 : 2;
        } else {
            return -1;
        }
    }

    private static final int MASK_TWO = 0b00111111;
    private static final int MASK_THREE = 0b00011111;
    private static final int MASK_FOUR = 0b00001111;
    private static final int MASK_REMAINING = 0xff000000;

    public static final int UNKNOWN = '\ufffd';

    public static boolean isRemaining(int cp) {
        return (cp & MASK_REMAINING) == MASK_REMAINING;
    }

    private static byte advanceByte(ByteStreamLike buffer, int lastRemaining, int index) {
        if (isRemaining(lastRemaining)) {
            int realRemaining = lastRemaining & 0xffffff;
            if (realRemaining <= 0xff) {
                if (index == 0) {
                    return (byte) (realRemaining & 0xff);
                } else {
                    return buffer.read();
                }
            } else if (realRemaining <= 0xffff) {
                if (index == 0) {
                    return (byte) ((realRemaining >> 8) & 0xff);
                } else if (index == 1) {
                    return (byte) (realRemaining & 0xff);
                } else {
                    return buffer.read();
                }
            } else {
                if (index == 0) {
                    return (byte) ((realRemaining >> 16) & 0xff);
                } else if (index == 1) {
                    return (byte) ((realRemaining >> 8) & 0xff);
                } else if (index == 2) {
                    return (byte) (realRemaining & 0xff);
                } else {
                    return buffer.read();
                }
            }
        } else {
            return buffer.read();
        }
    }

    private static int createRemaining(byte b) {
        return MASK_REMAINING | (b & 0xff);
    }

    private static int createRemaining(byte b1, byte b2) {
        return MASK_REMAINING | ((b1 & 0xff) << 8) | (b2 & 0xff);
    }

    private static int createRemaining(byte b1, byte b2, byte b3) {
        return MASK_REMAINING | ((b1 & 0xff) << 16) | ((b2 & 0xff) << 8) | (b3 & 0xff);
    }

    public static int readUtf8CodePoint(ByteStreamLike buffer, int lastRemaining) {
        byte b = advanceByte(buffer, lastRemaining, 0);
        byte n1, n2, n3;
        switch (charLen(b)) {
            case 1:
                return b;
            case 2:
                if (buffer.isUnreadable()) {
                    return createRemaining(b);
                }
                n1 = advanceByte(buffer, lastRemaining, 1);
                if (charLen(n1) != -1) {
                    buffer.back();
                    return UNKNOWN;
                }
                return ((b & MASK_TWO) << 6) | n1 & MASK_TWO;
            case 3:
                if (buffer.isUnreadable()) {
                    return createRemaining(b);
                }
                n1 = advanceByte(buffer, lastRemaining, 1);
                if (b == (byte) 0xe0 && (n1 & 0xe0) == 0x80 || n1 >= -64) {
                    buffer.back();
                    return UNKNOWN;
                }
                if (buffer.isUnreadable()) {
                    return createRemaining(b, n1);
                }
                n2 = advanceByte(buffer, lastRemaining, 2);
                if (n2 >= -64) {
                    buffer.back();
                    return UNKNOWN;
                }
                int cp = ((b & MASK_THREE) << 12) | ((n1 & MASK_TWO) << 6) | n2 & MASK_TWO;
                if (cp >= MIN_SURROGATE && cp < (MAX_SURROGATE + 1)) {
                    return UNKNOWN;
                }
                return cp;
            case 4:
                if (buffer.isUnreadable()) {
                    return createRemaining(b);
                }
                n1 = advanceByte(buffer, lastRemaining, 1);
                if ((b == -16 && (n1 < -112)) || (b == -12 && (n1 & 0xf0) != 0x80) || n1 >= -64) {
                    buffer.back();
                    return UNKNOWN;
                }
                if (buffer.isUnreadable()) {
                    return createRemaining(b, n1);
                }
                n2 = advanceByte(buffer, lastRemaining, 2);
                if (n2 >= -64) {
                    buffer.back();
                    return UNKNOWN;
                }
                if (buffer.isUnreadable()) {
                    return createRemaining(b, n1, n2);
                }
                n3 = advanceByte(buffer, lastRemaining, 3);
                if (n3 >= -64) {
                    buffer.back();
                    return UNKNOWN;
                }
                return ((b & MASK_FOUR) << 18) | ((n1 & MASK_TWO) << 12) | ((n2 & MASK_TWO) << 6) | n3 & MASK_THREE;
            default:
                return UNKNOWN;
        }
    }

    public interface ByteStreamLike {
        byte read();
        void back();
        boolean isUnreadable();
    }

    public static class ByteBufferByteStream implements ByteStreamLike {
        private final ByteBuffer buffer;

        public ByteBufferByteStream(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public byte read() {
            return buffer.get();
        }

        @Override
        public void back() {
            buffer.position(Math.max(buffer.position() - 1, 0));
        }

        @Override
        public boolean isUnreadable() {
            return !buffer.hasRemaining();
        }
    }

    public static class ByteBufByteStream implements ByteStreamLike {

        private final ByteBuf buf;

        public ByteBufByteStream(ByteBuf buf) {
            this.buf = buf;
        }

        @Override
        public byte read() {
            return buf.readByte();
        }

        @Override
        public void back() {
            buf.readerIndex(Math.max(buf.readerIndex() - 1, 0));
        }

        @Override
        public boolean isUnreadable() {
            return !buf.isReadable();
        }
    }

    private CharUtils() {}
}
