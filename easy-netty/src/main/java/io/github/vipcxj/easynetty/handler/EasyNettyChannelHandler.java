package io.github.vipcxj.easynetty.handler;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.EasyNettyHandler;
import io.github.vipcxj.easynetty.buffer.StreamBuffer;
import io.github.vipcxj.easynetty.utils.*;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JScheduler;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.function.BiConsumer;

@SuppressWarnings({"unchecked"})
public class EasyNettyChannelHandler extends FixLifecycleChannelInboundHandlerAdapter implements EasyNettyContext {

    protected final int capacity;
    protected final SendBox<?> readSendBox;
    protected final SendBox<?> writeSendBox;
    protected JScheduler scheduler;
    protected StreamBuffer buffers;
    protected CharUtils.ByteStreamLike bsBuffers;
    protected ChannelHandlerContext context;
    protected final EasyNettyHandler enHandler;
    protected JPromise<Void> promise;
    protected final Object UNRESOLVED = new Object();

    public EasyNettyChannelHandler(EasyNettyHandler handler) {
        this(handler, 64 * 1024);
    }

    public EasyNettyChannelHandler(EasyNettyHandler handler, int capacity) {
        this.capacity = capacity;
        this.enHandler = handler;
        this.buffers = new StreamBuffer();
        this.readSendBox = new SendBox<>()
                .withObjectArgs(2)
                .withByteArgs(1)
                .withShortArgs(1)
                .withIntArgs(7)
                .withLongArgs(1);
        this.writeSendBox = new SendBox<>()
                .withObjectArgs(1)
                .withIntArgs(1);
    }

    @Override
    public void handlerAdded0(ChannelHandlerContext ctx) {
        this.context = ctx;
    }

    @Override
    public void handlerRemoved0(ChannelHandlerContext ctx) {}

    @Override
    public void channelRegistered0(ChannelHandlerContext ctx) throws Exception {
        this.scheduler = JScheduler.fromExecutorService(ctx.channel().eventLoop());
        this.bsBuffers = new CharUtils.StreamBufferByteStream(this.buffers);
        super.channelRegistered0(ctx);
    }

    @Override
    public void channelUnregistered0(ChannelHandlerContext ctx) throws Exception {
        readSendBox.cancel();
        writeSendBox.cancel();
        this.bsBuffers = null;
        this.buffers.release();
        super.channelUnregistered0(ctx);
    }

    @Override
    public void channelActive0(ChannelHandlerContext ctx) throws Exception {
        promise = this.enHandler.handle(this).onError(ctx::fireExceptionCaught);
        promise.async(JContext.create(scheduler));
        maybeReadMore();
        super.channelActive0(ctx);
    }

    @Override
    public void channelInactive0(ChannelHandlerContext ctx) throws Exception {
        if (promise != null) {
            promise.cancel();
        }
        if (readSendBox.isReady()) {
            readSendBox.cancel();
        }
        if (writeSendBox.isReady()) {
            writeSendBox.cancel();
        }
        super.channelInactive0(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (readSendBox.isReady()) {
            readSendBox.reject(cause);
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (writeSendBox.isReady() && ctx.channel().isWritable()) {
            writeSendBox.handle();
        }
        super.channelWritabilityChanged(ctx);
    }

    public void mark() {
        buffers.mark();
    }

    public void resetMark() {
        buffers.resetMark();
    }

    public void cleanMark() {
        buffers.cleanMark();
    }

    protected void mark0() {
        buffers.mark0();
    }

    protected void resetMark0() {
        buffers.resetMark0();
    }

    protected void cleanMark0() {
        buffers.cleanMark0();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        buffers.consumeBuffer(buf);
        if (readSendBox.isReady()) {
            readSendBox.handle();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        maybeReadMore();
        super.channelReadComplete(ctx);
    }

    private void tryBestRelease() {
        buffers.freeSomeBytes();
    }

    private void maybeReadMore() {
        if (!context.channel().config().isAutoRead()) {
            if (buffers.readableBytes() <= capacity / 2) {
                context.read();
            }
        }
    }

    private <T> JPromise<T> createPromise(BiConsumer<JThunk<T>, JContext> handler) {
        if (context.channel().eventLoop().inEventLoop()) {
            return JPromise.generate(handler);
        } else {
            return JPromise.wrapScheduler(JPromise.create(handler), scheduler);
        }
    }

    private void prepareNextRead() {
        tryBestRelease();
        maybeReadMore();
    }

    @Override
    public ChannelHandlerContext getChannelContext() {
        return context;
    }

    private final SendBoxHandler<Byte> readByteImpl = (SendBox<Byte> sendBox) -> {
        if (buffers.isReadable()) {
            return buffers.readByte();
        }
        return UNRESOLVED;
    };

    @Override
    public JPromise<Byte> readByte() {
        SendBox<Byte> sendBox = (SendBox<Byte>) this.readSendBox;
        sendBox.install(readByteImpl);
        return createPromise(sendBox);
    }

    private int ARG_EXPECT;
    private int ARG_CHAR_REMAINING;
    private final SendBoxHandler<Boolean> consumeByteImpl = (SendBox<Boolean> sendBox) -> {
        if (buffers.isReadable()) {
            if (buffers.getByte() == sendBox.byteArg(ARG_EXPECT)) {
                buffers.readByte();
                return true;
            } else {
                return false;
            }
        }
        return UNRESOLVED;
    };

    @Override
    public JPromise<Boolean> consumeByte(int expected) {
        SendBox<Boolean> sendBox = (SendBox<Boolean>) this.readSendBox;
        sendBox.install(consumeByteImpl);
        ARG_EXPECT = sendBox.addByteArg((byte) (expected & 0xff));
        return createPromise(sendBox);
    }

    private final SendBoxHandler<Short> readShortImpl = (SendBox<Short> sendBox) -> {
        if (buffers.isReadable(2)) {
            return buffers.readShort();
        }
        return UNRESOLVED;
    };

    @Override
    public JPromise<Short> readShort() {
        SendBox<Short> sendBox = (SendBox<Short>) this.readSendBox;
        sendBox.install(readShortImpl);
        return createPromise(sendBox);
    }

    private final SendBoxHandler<Boolean> consumeShortImpl = (SendBox<Boolean> sendBox) -> {
        if (buffers.isReadable(2)) {
            if (buffers.getShort() == sendBox.shortArg(ARG_EXPECT)) {
                buffers.readShort();
                return true;
            } else {
                return false;
            }
        }
        return UNRESOLVED;
    };

    @Override
    public JPromise<Boolean> consumeShort(int expected) {
        SendBox<Boolean> sendBox = (SendBox<Boolean>) this.readSendBox;
        sendBox.install(consumeShortImpl);
        ARG_EXPECT = sendBox.addShortArg((short) (expected & 0xffff));
        return createPromise(sendBox);
    }

    private final SendBoxHandler<Integer> readIntImpl = (SendBox<Integer> sendBox) -> {
        if (buffers.isReadable(4)) {
            return buffers.readInt();
        } else {
            return UNRESOLVED;
        }
    };

    @Override
    public JPromise<Integer> readInt() {
        SendBox<Integer> sendBox = (SendBox<Integer>) this.readSendBox;
        sendBox.install(readIntImpl);
        return createPromise(sendBox);
    }

    private final SendBoxHandler<Boolean> consumeIntImpl = (SendBox<Boolean> sendBox) -> {
        if (buffers.isReadable(4)) {
            if (buffers.getInt() == sendBox.intArg(ARG_EXPECT)) {
                buffers.readInt();
                return true;
            } else {
                return false;
            }
        }
        return UNRESOLVED;
    };

    @Override
    public JPromise<Boolean> consumeInt(int expected) {
        SendBox<Boolean> sendBox = (SendBox<Boolean>) this.readSendBox;
        sendBox.install(consumeIntImpl);
        ARG_EXPECT = sendBox.addIntArg(expected);
        return createPromise(sendBox);
    }

    private final SendBoxHandler<Long> readLongImpl = (sendBox) -> {
        if (buffers.isReadable(8)) {
            return buffers.readLong();
        }
        return UNRESOLVED;
    };

    @Override
    public JPromise<Long> readLong() {
        SendBox<Long> sendBox = (SendBox<Long>) this.readSendBox;
        sendBox.install(readLongImpl);
        return createPromise(sendBox);
    }

    private final SendBoxHandler<Boolean> consumeLongImpl = (sendBox) -> {
        if (buffers.isReadable(8)) {
            if (buffers.getLong() == sendBox.longArg(ARG_EXPECT)) {
                buffers.readLong();
                return true;
            } else {
                return false;
            }
        }
        return UNRESOLVED;
    };

    @Override
    public JPromise<Boolean> consumeLong(long expect) {
        SendBox<Boolean> sendBox = (SendBox<Boolean>) this.readSendBox;
        sendBox.install(consumeLongImpl);
        ARG_EXPECT = sendBox.addLongArg(expect);
        return createPromise(sendBox);
    }

    private final SendBoxHandler<Integer> readUtf8CodePointImpl = (SendBox<Integer> sendBox) -> {
        if (!buffers.isReadable()) {
            return UNRESOLVED;
        }
        int charRemaining = sendBox.intArg(ARG_CHAR_REMAINING);
        int cp = CharUtils.readUtf8CodePoint(bsBuffers, charRemaining);
        if (!CharUtils.isRemaining(cp)) {
            return cp;
        } else {
            sendBox.setIntArg(ARG_CHAR_REMAINING, cp);
            return UNRESOLVED;
        }
    };

    @Override
    public JPromise<Integer> readUtf8CodePoint() {
        SendBox<Integer> sendBox = (SendBox<Integer>) this.readSendBox;
        sendBox.install(readUtf8CodePointImpl);
        ARG_CHAR_REMAINING = sendBox.addIntArg(0);
        return createPromise(sendBox);
    }

    private final SendBoxHandler<Boolean> consumeUtf8CodePointImpl = (SendBox<Boolean> sendBox) -> {
        if (!buffers.isReadable()) {
            return UNRESOLVED;
        }
        int codePoint = sendBox.intArg(ARG_EXPECT);
        int charRemaining = sendBox.intArg(ARG_CHAR_REMAINING);
        if (charRemaining == 0) {
            mark0();
        }
        int cp = CharUtils.readUtf8CodePoint(bsBuffers, charRemaining);
        if (!CharUtils.isRemaining(cp)) {
            if (cp == codePoint) {
                cleanMark0();
                return true;
            } else {
                resetMark0();
                return false;
            }
        } else {
            sendBox.setIntArg(ARG_CHAR_REMAINING, cp);
            return UNRESOLVED;
        }
    };

    @Override
    public JPromise<Boolean> consumeUtf8CodePoint(int codePoint) {
        SendBox<Boolean> sendBox = (SendBox<Boolean>) this.readSendBox;
        sendBox.install(consumeUtf8CodePointImpl);
        ARG_EXPECT = sendBox.addIntArg(codePoint);
        ARG_CHAR_REMAINING = sendBox.addIntArg(0);
        return createPromise(sendBox);
    }

    private int ARG_BYTE_BUF_OUTPUT;
    private int ARG_CP_UNTIL0;
    private int ARG_CP_UNTIL1;
    private int ARG_CP_UNTIL2;
    private int ARG_CP_UNTIL_NUM;
    private int ARG_UNTIL_MODE;
    private int ARG_CP_UNTIL_TESTED_LEN;
    private int ARG_CP_UNTIL_TESTED_INDEX;

    private boolean matchCpUntil(SendBox<?> sendBox, int cp, int cpLen) {
        int cpTestedIndex = sendBox.intArg(ARG_CP_UNTIL_TESTED_INDEX);
        int cpTest;
        if (cpTestedIndex == 0) {
            cpTest = sendBox.intArg(ARG_CP_UNTIL0);
        } else if (cpTestedIndex == 1) {
            cpTest = sendBox.intArg(ARG_CP_UNTIL1);
        } else if (cpTestedIndex == 2) {
            cpTest = sendBox.intArg(ARG_CP_UNTIL2);
        } else {
            throw new IllegalStateException();
        }
        if (cp == cpTest) {
            ++cpTestedIndex;
            int cpNum = sendBox.intArg(ARG_CP_UNTIL_NUM);
            int cpTestedLen = sendBox.intArg(ARG_CP_UNTIL_TESTED_LEN);
            sendBox.setIntArg(ARG_CP_UNTIL_TESTED_LEN, cpTestedLen + cpLen);
            if (cpTestedIndex == cpNum) {
                return true;
            } else if (cpTestedIndex < cpNum) {
                sendBox.setIntArg(ARG_CP_UNTIL_TESTED_INDEX, cpTestedIndex);
                return false;
            } else {
                throw new IllegalStateException();
            }
        } else {
            sendBox.setIntArg(ARG_CP_UNTIL_TESTED_INDEX, 0);
            sendBox.setIntArg(ARG_CP_UNTIL_TESTED_LEN, 0);
            return false;
        }
    }

    private final SendBoxHandler<Void> readUtf8UntilImpl = (SendBox<Void> sendBox) -> {
        if (!buffers.isReadable()) {
            return UNRESOLVED;
        }
        ByteBuf output = sendBox.arg(ARG_BYTE_BUF_OUTPUT);
        int charRemaining = sendBox.intArg(ARG_CHAR_REMAINING);
        UntilMode mode = sendBox.arg(ARG_UNTIL_MODE);
        do {
            int readerIndex = buffers.readerIndex();
            int cp = CharUtils.readUtf8CodePoint(bsBuffers, charRemaining);
            int cpLen = buffers.readerIndex() - readerIndex;
            if (!CharUtils.isRemaining(cp)) {
                buffers.readerIndex(readerIndex);
                if (matchCpUntil(sendBox, cp, cpLen)) {
                    buffers.readBuf(output, cpLen);
                    if (mode != UntilMode.INCLUDE) {
                        int cpTestedLen = sendBox.intArg(ARG_CP_UNTIL_TESTED_LEN);
                        if (mode == UntilMode.EXCLUDE) {
                            buffers.readerIndex(buffers.readerIndex() - cpTestedLen);
                        } else if (mode != UntilMode.SKIP) {
                            throw new IllegalStateException();
                        }
                        output.writerIndex(output.writerIndex() - cpTestedLen);
                    }
                    return null;
                } else {
                    buffers.readBuf(output, cpLen);
                }
            } else {
                int cpTestedLen = sendBox.intArg(ARG_CP_UNTIL_TESTED_LEN);
                sendBox.setIntArg(ARG_CP_UNTIL_TESTED_LEN, cpTestedLen + cpLen);
                return UNRESOLVED;
            }
        } while (buffers.isReadable());
        return UNRESOLVED;
    };

    private void prepareUtf8Until(ByteBuf buf, int cpNum, UntilMode mode, int cp0, int cp1, int cp2) {
        assert cpNum == 1 || cpNum == 2 || cpNum == 3;
        ARG_BYTE_BUF_OUTPUT = this.readSendBox.addArg(buf);
        ARG_CHAR_REMAINING = this.readSendBox.addIntArg(0);
        ARG_CP_UNTIL_NUM = this.readSendBox.addIntArg(cpNum);
        ARG_CP_UNTIL0 = this.readSendBox.addIntArg(cp0);
        if (cpNum > 1) {
            ARG_CP_UNTIL1 = this.readSendBox.addIntArg(cp1);
        }
        if (cpNum > 2) {
            ARG_CP_UNTIL2 = this.readSendBox.addIntArg(cp2);
        }
        ARG_UNTIL_MODE = this.readSendBox.addArg(mode);
        ARG_CP_UNTIL_TESTED_INDEX = this.readSendBox.addIntArg(0);
        ARG_CP_UNTIL_TESTED_LEN = this.readSendBox.addIntArg(0);
    }

    @Override
    public JPromise<Void> readUtf8Until(ByteBuf output, int codePoint, UntilMode mode) {
        SendBox<Void> sendBox = (SendBox<Void>) this.readSendBox;
        sendBox.install(readUtf8UntilImpl);
        prepareUtf8Until(output, 1, mode, codePoint, 0, 0);
        return createPromise(sendBox);
    }

    @Override
    public JPromise<Void> readUtf8Until(ByteBuf output, int cp0, int cp1, UntilMode mode) {
        SendBox<Void> sendBox = (SendBox<Void>) this.readSendBox;
        sendBox.install(readUtf8UntilImpl);
        prepareUtf8Until(output, 2, mode, cp0, cp1, 0);
        return createPromise(sendBox);
    }

    @Override
    public JPromise<Void> readUtf8Until(ByteBuf output, int cp0, int cp1, int cp2, UntilMode mode) {
        SendBox<Void> sendBox = (SendBox<Void>) this.readSendBox;
        sendBox.install(readUtf8UntilImpl);
        prepareUtf8Until(output, 3, mode, cp0, cp1, cp2);
        return createPromise(sendBox);
    }

    private int ARG_BYTES_OUTPUT;
    private int ARG_BYTES_REMAINING;
    private int ARG_BYTES_OFFSET;
    private int ARG_BYTES_LENGTH;
    private final SendBoxHandler<Void> readBytesImpl = (SendBox<Void> sendBox) -> {
        byte[] outputBytes = sendBox.arg(ARG_BYTES_OUTPUT);
        int remaining = sendBox.intArg(ARG_BYTES_REMAINING);
        int offset = sendBox.intArg(ARG_BYTES_OFFSET);
        int length = sendBox.intArg(ARG_BYTES_LENGTH);
        int currentOffset = offset + length - remaining;
        int readableBytes = buffers.readableBytes();
        if (readableBytes >= remaining) {
            buffers.readBytes(outputBytes, currentOffset, remaining);
            return null;
        } else if (readableBytes > 0){
            buffers.readBytes(outputBytes, currentOffset, readableBytes);
            sendBox.setIntArg(ARG_BYTES_REMAINING, remaining - readableBytes);
            return UNRESOLVED;
        } else {
            return UNRESOLVED;
        }
    };

    @Override
    public JPromise<Void> readBytes(byte[] outputBytes) {
        return readBytes(outputBytes, 0, outputBytes.length);
    }

    @Override
    public JPromise<Void> readBytes(byte[] outputBytes, int offset, int length) {
        SendBox<Void> sendBox = (SendBox<Void>) this.readSendBox;
        sendBox.install(readBytesImpl);
        ARG_BYTES_OUTPUT = sendBox.addArg(outputBytes);
        ARG_BYTES_REMAINING = sendBox.addIntArg(length);
        ARG_BYTES_OFFSET = sendBox.addIntArg(offset);
        ARG_BYTES_LENGTH = sendBox.addIntArg(length);
        return createPromise(sendBox);
    }

    private int ARG_BYTES_EXPECT;
    private final SendBoxHandler<Boolean> consumeBytesImpl = (SendBox<Boolean> sendBox) -> {
        int length = sendBox.intArg(ARG_BYTES_LENGTH);
        if (length == 0) {
            return true;
        }
        int remaining = sendBox.intArg(ARG_BYTES_REMAINING);
        if (length == remaining) {
            mark0();
        }
        byte[] expectedBytes = sendBox.arg(ARG_BYTES_EXPECT);
        int offset = sendBox.intArg(ARG_BYTES_OFFSET);
        int currentOffset = offset + length - remaining;
        int readableBytes = buffers.readableBytes();
        int toReads = Math.min(readableBytes, remaining);
        for (int i = currentOffset; i < currentOffset + toReads; ++i) {
            if (buffers.readByte() != expectedBytes[i]) {
                resetMark0();
                return false;
            }
        }
        if (readableBytes >= remaining) {
            cleanMark0();
            return true;
        } else {
            sendBox.setIntArg(ARG_BYTES_REMAINING, remaining - readableBytes);
            return UNRESOLVED;
        }
    };

    @Override
    public JPromise<Boolean> consumeBytes(byte[] expectedBytes) {
        return consumeBytes(expectedBytes, 0, expectedBytes.length);
    }

    @Override
    public JPromise<Boolean> consumeBytes(byte[] expectedBytes, int offset, int length) {
        SendBox<Boolean> sendBox = (SendBox<Boolean>) this.readSendBox;
        sendBox.install(consumeBytesImpl);
        ARG_BYTES_EXPECT = sendBox.addArg(expectedBytes);
        ARG_BYTES_REMAINING = sendBox.addIntArg(length);
        ARG_BYTES_OFFSET = sendBox.addIntArg(offset);
        ARG_BYTES_LENGTH = sendBox.addIntArg(length);
        return createPromise(sendBox);
    }

    private int ARG_BUF_MAX_LEN;
    private int ARG_IGNORE_EMPTY;
    private final SendBoxHandler<ByteBuf> readSomeBufImpl = (SendBox<ByteBuf> sendBox) -> {
        int maxLen = sendBox.intArg(ARG_BUF_MAX_LEN);
        int ignoreEmpty = sendBox.intArg(ARG_IGNORE_EMPTY);
        int toRead = Math.max(0, Math.min(maxLen, buffers.readableBytes()));
        if (toRead > 0) {
            return buffers.readBuf(context.alloc(), toRead);
        } else if (ignoreEmpty == 0) {
            return Unpooled.EMPTY_BUFFER;
        } else {
            return UNRESOLVED;
        }
    };

    @Override
    public JPromise<ByteBuf> readSomeBuf(int maxLen, boolean ignoreEmpty) {
        SendBox<ByteBuf> sendBox = (SendBox<ByteBuf>) this.readSendBox;
        sendBox.install(readSomeBufImpl);
        ARG_BUF_MAX_LEN = sendBox.addIntArg(maxLen);
        ARG_IGNORE_EMPTY = sendBox.addIntArg(ignoreEmpty ? 1 : 0);
        return createPromise(sendBox);
    }

    private UntilBox matchUntil(UntilBox untilBox, byte[] until, int index, int start) {
        untilBox.setMatchedUntil(index);
        untilBox.setSuccess(true);
        int end = buffers.readerIndex();
        buffers.readerIndex(start);
        switch (untilBox.getMode()) {
            case SKIP:
                untilBox.setBuf(buffers.readBuf(context.alloc(), end - until.length - start));
                buffers.readerIndex(end);
                break;
            case INCLUDE:
                untilBox.setBuf(buffers.readBuf(context.alloc(), end - start));
                break;
            case EXCLUDE:
                untilBox.setBuf(buffers.readBuf(context.alloc(), end - until.length - start));
                break;
            default:
                throw new IllegalStateException("This is impossible.");
        }
        return untilBox;
    }

    private int ARG_UNTIL_BOX;
    private final SendBoxHandler<UntilBox> readSomeBufUntilAnyImpl = (sendBox) -> {
        UntilBox untilBox = sendBox.arg(ARG_UNTIL_BOX);
        if (buffers.isReadable()) {
            byte[][] untils = untilBox.getUntils();
            int[] matched = untilBox.getMatched();
            Arrays.fill(matched, 0);
            int start = buffers.readerIndex();
            do {
                for (int i = 0; i < untils.length; ++i) {
                    byte[] until = untils[i];
                    int index = matched[i];
                    if (index == until.length) {
                        return matchUntil(untilBox, until, i, start);
                    }
                    if (until[index] == buffers.getByte()) {
                        matched[i]++;
                    } else {
                        matched[i] = 0;
                    }
                }
                buffers.readByte();
            } while (buffers.isReadable());
            for (int i = 0; i < untils.length; ++i) {
                byte[] until = untils[i];
                int index = matched[i];
                if (index == until.length) {
                    return matchUntil(untilBox, until, i, start);
                }
            }
            int len = 0;
            for (int i = 0; i < matched.length; ++i) {
                int m = matched[i];
                matched[i] = 0;
                len = Math.max(m, len);
            }
            int end = buffers.readerIndex() - len;
            buffers.readerIndex(start);
            untilBox.setBuf(buffers.readBuf(context.alloc(), end - start));
        } else {
            untilBox.setBuf(Unpooled.EMPTY_BUFFER);
        }
        untilBox.setSuccess(false);
        return untilBox;
    };

    @Override
    public JPromise<UntilBox> readSomeBufUntilAny(UntilBox untilBox) {
        SendBox<UntilBox> sendBox = (SendBox<UntilBox>) this.readSendBox;
        sendBox.install(readSomeBufUntilAnyImpl);
        ARG_UNTIL_BOX = sendBox.addArg(untilBox);
        return createPromise(sendBox);
    }

    private final SendBoxHandler<Void> skipImpl = (sendBox) -> {
        long remaining = sendBox.longArg(ARG_BYTES_REMAINING);
        int readableBytes = buffers.readableBytes();
        if (readableBytes >= remaining) {
            buffers.readerIndex(buffers.readerIndex() + (int) remaining);
            return null;
        } else {
            buffers.readerIndex(buffers.readerIndex() + readableBytes);
            sendBox.setLongArg(ARG_BYTES_REMAINING, remaining - readableBytes);
            return UNRESOLVED;
        }
    };

    @Override
    public JPromise<Void> skip(long length) {
        SendBox<Void> sendBox = (SendBox<Void>) this.readSendBox;
        sendBox.install(skipImpl);
        ARG_BYTES_REMAINING = sendBox.addLongArg(length);
        return createPromise(sendBox);
    }

    private static int ARG_WRITE_BYTE_BUF;
    private static int ARG_WRITE_FLUSH;
    private final SendBoxHandler<ChannelFuture> writeBufferImpl = (sendBox) -> {
        if (!context.channel().isActive()) {
            sendBox.cancel();
        }
        if (context.channel().isWritable()) {
            ByteBuf buf = sendBox.arg(ARG_WRITE_BYTE_BUF);
            boolean flush = sendBox.intArg(ARG_WRITE_FLUSH) != 0;
            if (flush) {
                return context.channel().writeAndFlush(buf);
            } else {
                return context.channel().write(buf);
            }
        }
        return UNRESOLVED;
    };

    @Override
    public JPromise<Void> writeBuffer(ByteBuf buf) {
        SendBox<ChannelFuture> sendBox = (SendBox<ChannelFuture>) this.writeSendBox;
        sendBox.install(writeBufferImpl);
        ARG_WRITE_BYTE_BUF = sendBox.addArg(buf);
        ARG_WRITE_FLUSH = sendBox.addIntArg(0);
        return createPromise(sendBox).thenReturn(null);
    }

    @Override
    public JPromise<Void> writeBufferAndFlush(ByteBuf buf) {
        SendBox<ChannelFuture> sendBox = (SendBox<ChannelFuture>) this.writeSendBox;
        sendBox.install(writeBufferImpl);
        ARG_WRITE_BYTE_BUF = sendBox.addArg(buf);
        ARG_WRITE_FLUSH = sendBox.addIntArg(1);
        return createPromise(sendBox).thenImmediate(PromiseUtils::toPromise);
    }

    @Override
    public JPromise<Void> writeByte(int b) {
        ByteBuf buf = context.alloc().buffer(1).writeByte(b);
        return writeBuffer(buf);
    }

    @Override
    public JPromise<Void> writeByteAndFlush(int b) {
        ByteBuf buf = context.alloc().buffer(1).writeByte(b);
        return writeBufferAndFlush(buf);
    }

    private void checkBytesRange(byte[] bytes, int offset, int length) {
        if (offset < 0 || offset + length > bytes.length || length < 0) {
            throw new IndexOutOfBoundsException("bytes length: " + bytes.length + ", offset: " + offset + ", length: " + length + ".");
        }
    }

    @Override
    public JPromise<Void> writeBytes(byte[] bytes, int offset, int length) {
        checkBytesRange(bytes, offset, length);
        ByteBuf buf = context.alloc().buffer(length).writeBytes(bytes, offset, length);
        return writeBuffer(buf);
    }

    @Override
    public JPromise<Void> writeBytesAndFlush(byte[] bytes, int offset, int length) {
        checkBytesRange(bytes, offset, length);
        ByteBuf buf = context.alloc().buffer(length).writeBytes(bytes, offset, length);
        return writeBufferAndFlush(buf);
    }

    @Override
    public JPromise<Void> writeShort(int s) {
        ByteBuf buf = context.alloc().buffer(2).writeShort(s);
        return writeBuffer(buf);
    }

    @Override
    public JPromise<Void> writeShortAndFlush(int s) {
        ByteBuf buf = context.alloc().buffer(2).writeShort(s);
        return writeBufferAndFlush(buf);
    }

    @Override
    public JPromise<Void> writeInt(int i) {
        ByteBuf buf = context.alloc().buffer(4).writeInt(i);
        return writeBuffer(buf);
    }

    @Override
    public JPromise<Void> writeIntAndFlush(int i) {
        ByteBuf buf = context.alloc().buffer(4).writeInt(i);
        return writeBufferAndFlush(buf);
    }

    @Override
    public JPromise<Void> writeString(String s, Charset charset) {
        ByteBuf buf = BufUtils.fromString(s, charset);
        return writeBuffer(buf);
    }

    @Override
    public JPromise<Void> writeStringAndFlush(String s, Charset charset) {
        ByteBuf buf = BufUtils.fromString(s, charset);
        return writeBufferAndFlush(buf);
    }

    class SendBox<E> implements BiConsumer<JThunk<E>, JContext> {

        private SendBoxHandler<E> handler;
        private JThunk<E> thunk;
        private JContext context;
        private Object[] args;
        private int argUsed;
        private byte[] byteArgs;
        private int byteArgUsed;
        private short[] shortArgs;
        private int shortArgUsed;
        private int[] intArgs;
        private int intArgUsed;
        private long[] longArgs;
        private int longArgUsed;

        SendBox() {}

        public SendBox<E> withObjectArgs(int argLen) {
            this.args = new Object[argLen];
            this.argUsed = 0;
            return this;
        }

        public SendBox<E> withByteArgs(int argLen) {
            this.byteArgs = new byte[argLen];
            this.byteArgUsed = 0;
            return this;
        }

        public SendBox<E> withShortArgs(int argLen) {
            this.shortArgs = new short[argLen];
            this.shortArgUsed = 0;
            return this;
        }

        public SendBox<E> withIntArgs(int argLen) {
            this.intArgs = new int[argLen];
            this.intArgUsed = 0;
            return this;
        }

        public SendBox<E> withLongArgs(int argLen) {
            this.longArgs = new long[argLen];
            this.longArgUsed = 0;
            return this;
        }

        public boolean isReady() {
            return this.thunk != null && this.context != null;
        }

        private void reset() {
            this.handler = null;
            this.thunk = null;
            this.context = null;
            Arrays.fill(args, null);
            this.argUsed = 0;
            if (this.byteArgs != null) {
                Arrays.fill(byteArgs, (byte) 0);
                this.byteArgUsed = 0;
            }
            if (this.shortArgs != null) {
                Arrays.fill(this.shortArgs, (short) 0);
                this.shortArgUsed = 0;
            }
            if (this.intArgs != null) {
                Arrays.fill(this.intArgs, 0);
                this.intArgUsed = 0;
            }
            if (this.longArgs != null) {
                Arrays.fill(this.longArgs, 0);
                this.longArgUsed = 0;
            }
        }

        private void prepare(JThunk<E> thunk, JContext context) {
            this.thunk = thunk;
            this.context = context;
        }

        public void install(SendBoxHandler<E> handler) {
            if (this.handler != null) {
                throw new IllegalStateException("Unable to install " + handlerName(handler) + ", The handler " + handlerName(this.handler) + " has been installed.");
            }
            this.handler = handler;
        }

        public int addArg(Object arg) {
            args[argUsed] = arg;
            return argUsed++;
        }

        public void setArg(int index, Object arg) {
            args[index] = arg;
        }

        public <T> T arg(int index) {
            //noinspection unchecked
            return (T) args[index];
        }

        public int addByteArg(byte arg) {
            byteArgs[byteArgUsed] = arg;
            return byteArgUsed++;
        }

        public byte byteArg(int index) {
            return byteArgs[index];
        }

        public int addShortArg(short arg) {
            shortArgs[shortArgUsed] = arg;
            return shortArgUsed++;
        }

        public short shortArg(int index) {
            return shortArgs[index];
        }

        public int addIntArg(int arg) {
            intArgs[intArgUsed] = arg;
            return intArgUsed++;
        }

        public void setIntArg(int index, int arg) {
            intArgs[index] = arg;
        }

        public int intArg(int index) {
            return intArgs[index];
        }

        public int addLongArg(long arg) {
            longArgs[longArgUsed] = arg;
            return longArgUsed++;
        }

        public void setLongArg(int index, long arg) {
            longArgs[index] = arg;
        }

        public long longArg(int index) {
            return longArgs[index];
        }

        public void reject(Throwable t) {
            thunk.reject(t, context);
        }

        public void cancel() {
            if (thunk != null) {
                thunk.cancel();
            }
        }

        public void handle() {
            accept(thunk, context);
        }

        @Override
        public void accept(JThunk<E> thunk, JContext context) {
            assert handler != null;
            prepare(thunk, context);
            try {
                Object resolved = handler.handle(this);
                if (resolved != UNRESOLVED) {
                    prepareNextRead();
                    reset();
                    thunk.resolve((E) resolved, context);
                }
            } catch (Throwable t) {
                prepareNextRead();
                reset();
                thunk.reject(t, context);
            }
        }
    }

    interface SendBoxHandler<E> {
        Object handle(SendBox<E> box);
    }

    private String handlerName(SendBoxHandler<?> handler) {
        if (handler == consumeByteImpl) {
            return "consumeByteImpl";
        } else if (handler == consumeBytesImpl) {
            return "consumeBytesImpl";
        } else if (handler == consumeIntImpl) {
            return "consumeIntImpl";
        } else if (handler == consumeLongImpl) {
            return "consumeLongImpl";
        } else if (handler == consumeShortImpl) {
            return "consumeShortImpl";
        } else if (handler == consumeUtf8CodePointImpl) {
            return "consumeUtf8CodePointImpl";
        } else if (handler == readByteImpl) {
            return "readByteImpl";
        } else if (handler == readBytesImpl) {
            return "readBytesImpl";
        } else if (handler == readIntImpl) {
            return "readIntImpl";
        } else if (handler == readLongImpl) {
            return "readLongImpl";
        } else if (handler == readShortImpl) {
            return "readShortImpl";
        } else if (handler == readSomeBufImpl) {
            return "readSomeBufImpl";
        } else if (handler == readSomeBufUntilAnyImpl) {
            return "readSomeBufUntilAnyImpl";
        } else if (handler == readUtf8CodePointImpl) {
            return "readUtf8CodePointImpl";
        } else if (handler == readUtf8UntilImpl) {
            return "readUtf8UntilImpl";
        } else if (handler == skipImpl) {
            return "skipImpl";
        } else if (handler == writeBufferImpl) {
            return "writeBufferImpl";
        } else {
            return "unknown";
        }
    }
}
