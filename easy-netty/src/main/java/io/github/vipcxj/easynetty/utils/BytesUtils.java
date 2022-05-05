package io.github.vipcxj.easynetty.utils;

import java.nio.charset.StandardCharsets;

public class BytesUtils {

    public static byte getByte(byte[] memory, int index) {
        return memory[index];
    }

    public static short getShort(byte[] memory, int index) {
        return (short) (memory[index] << 8 | memory[index + 1] & 0xFF);
    }

    public static void setShort(byte[] memory, int index, int s) {
        memory[index] = (byte) ((s >> 8) & 0xff);
        memory[index + 1] = (byte) (s & 0xff);
    }

    public static short getShortLE(byte[] memory, int index) {
        return (short) (memory[index] & 0xff | memory[index + 1] << 8);
    }

    public static int getUnsignedShort(byte[] memory, int index) {
        return getShort(memory, index) & 0xFFFF;
    }

    public static int getUnsignedShortLE(byte[] memory, int index) {
        return getShortLE(memory, index) & 0xFFFF;
    }

    public static int getMedium(byte[] memory, int index) {
        int value = getUnsignedMedium(memory, index);
        if ((value & 0x800000) != 0) {
            value |= 0xff000000;
        }
        return value;
    }

    public static int getMediumLE(byte[] memory, int index) {
        int value = getUnsignedMediumLE(memory, index);
        if ((value & 0x800000) != 0) {
            value |= 0xff000000;
        }
        return value;
    }

    public static int getUnsignedMedium(byte[] memory, int index) {
        return  (memory[index]     & 0xff) << 16 |
                (memory[index + 1] & 0xff) <<  8 |
                memory[index + 2] & 0xff;
    }

    public static int getUnsignedMediumLE(byte[] memory, int index) {
        return  memory[index]     & 0xff         |
                (memory[index + 1] & 0xff) <<  8 |
                (memory[index + 2] & 0xff) << 16;
    }

    public static int getInt(byte[] memory, int index) {
        return  (memory[index]     & 0xff) << 24 |
                (memory[index + 1] & 0xff) << 16 |
                (memory[index + 2] & 0xff) <<  8 |
                memory[index + 3] & 0xff;
    }

    public static int getIntLE(byte[] memory, int index) {
        return  memory[index]      & 0xff        |
                (memory[index + 1] & 0xff) << 8  |
                (memory[index + 2] & 0xff) << 16 |
                (memory[index + 3] & 0xff) << 24;
    }

    public static long getUnsignedInt(byte[] memory, int index) {
        return getInt(memory, index) & 0xFFFFFFFFL;
    }

    public static long getUnsignedIntLE(byte[] memory, int index) {
        return getIntLE(memory, index) & 0xFFFFFFFFL;
    }

    public static long getLong(byte[] memory, int index) {
        return  ((long) memory[index]     & 0xff) << 56 |
                ((long) memory[index + 1] & 0xff) << 48 |
                ((long) memory[index + 2] & 0xff) << 40 |
                ((long) memory[index + 3] & 0xff) << 32 |
                ((long) memory[index + 4] & 0xff) << 24 |
                ((long) memory[index + 5] & 0xff) << 16 |
                ((long) memory[index + 6] & 0xff) <<  8 |
                (long) memory[index + 7] & 0xff;
    }

    public static long getLongLE(byte[] memory, int index) {
        return  (long) memory[index]      & 0xff        |
                ((long) memory[index + 1] & 0xff) <<  8 |
                ((long) memory[index + 2] & 0xff) << 16 |
                ((long) memory[index + 3] & 0xff) << 24 |
                ((long) memory[index + 4] & 0xff) << 32 |
                ((long) memory[index + 5] & 0xff) << 40 |
                ((long) memory[index + 6] & 0xff) << 48 |
                ((long) memory[index + 7] & 0xff) << 56;
    }

    public static char getChar(byte[] memory, int index) {
        return (char) getShort(memory, index);
    }

    public static float getFloat(byte[] memory, int index) {
        return Float.intBitsToFloat(getInt(memory, index));
    }

    public static double getDouble(byte[] memory, int index) {
        return Double.longBitsToDouble(getLong(memory, index));
    }

    public static String getString(byte[] memory, int index, int maxLen) {
        int end = index + maxLen;
        for (int i = index; i < end; ++i) {
            if (memory[i] == 0) {
                end = i;
            }
        }
        return new String(memory, index, end - index, StandardCharsets.UTF_8);
    }

    public static void setString(byte[] memory, int index, int maxLen, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= maxLen) {
            throw new IllegalArgumentException("Too long string.");
        }
        System.arraycopy(bytes, 0, memory, index, bytes.length);
        for (int i = bytes.length; i < maxLen; ++i) {
            memory[index + i] = 0;
        }
    }

    private BytesUtils() {}
}
