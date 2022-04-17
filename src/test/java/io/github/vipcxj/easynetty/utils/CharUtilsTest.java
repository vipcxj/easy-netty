package io.github.vipcxj.easynetty.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class CharUtilsTest {

    private void prepareBuffer(int i, ByteBuffer buffer) {
        buffer.clear();
        if (i < 0xff) {
            buffer.put((byte) (i & 0xff));
            buffer.putShort((short) 0);
            buffer.put((byte) 0);
        } else if (i < 0xffff) {
            buffer.putShort((short) (i & 0xffff));
            buffer.putShort((short) 0);
        } else if (i < 0xffffff) {
            buffer.putInt(i << 8);
        } else {
            buffer.putInt(i);
        }
        buffer.flip();
    }

    @Test
    public void testReadUtf8CodePoint() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        CharUtils.ByteStreamLike bsBuffer = new CharUtils.ByteBufferByteStream(buffer);
        for (int i = 0; i < Integer.MAX_VALUE; ++i) {
            testChar(i, buffer, bsBuffer);
            if (i % (Integer.MAX_VALUE / 100) == 0) {
                System.out.println("progress: " + Math.round(i * 100.0f / Integer.MAX_VALUE) + "%");
            }
        }
    }

    private void testChar(int i, ByteBuffer buffer, CharUtils.ByteStreamLike bsBuffer) {
        prepareBuffer(i, buffer);
        String s = new String(buffer.array(), StandardCharsets.UTF_8);
        int j = 0;
        while (buffer.hasRemaining()) {
            assertEqualsCpResult(i, s.codePointAt(j++), CharUtils.readUtf8CodePoint(bsBuffer, 0));
        }
    }

    private void assertEqualsCpResult(int i, int cp1, int cp2) {
        cp2 = CharUtils.isRemaining(cp2) ? CharUtils.UNKNOWN : cp2;
        try {
            Assertions.assertEquals(cp1, cp2);
        } catch (AssertionFailedError t) {
            System.out.println(i);
            throw t;
        }
    }
}
