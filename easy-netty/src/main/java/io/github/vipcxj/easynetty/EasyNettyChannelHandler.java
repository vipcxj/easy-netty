package io.github.vipcxj.easynetty;

import io.github.vipcxj.easynetty.utils.CharUtils;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JScheduler;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.function.BiConsumer;

@SuppressWarnings({"unchecked", "unused"})
public class EasyNettyChannelHandler extends ChannelInboundHandlerAdapter implements EasyNettyContext {

    private final int capacity;
    private boolean markMode;
    private boolean readComplete;
    private boolean preventRelease;
    private final SendBox<?> readSendBox;
    private final SendBox<?> writeSendBox;
    private JScheduler scheduler;
    private ByteBuf buffer;
    private CompositeByteBuf buffers;
    private CharUtils.ByteStreamLike bsBuffers;
    protected ChannelHandlerContext context;
    private final EasyNettyHandler enHandler;

    public EasyNettyChannelHandler(EasyNettyHandler handler) {
        this(handler, 64 * 1024);
    }

    public EasyNettyChannelHandler(EasyNettyHandler handler, int capacity) {
        this.capacity = capacity;
        this.enHandler = handler;
        this.markMode = false;
        this.readComplete = false;
        this.preventRelease = false;
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
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.context = ctx;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        this.context = null;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        this.scheduler = JScheduler.fromExecutorService(ctx.channel().eventLoop());
        this.buffers = ctx.alloc().compositeBuffer();
        this.bsBuffers = new CharUtils.ByteBufByteStream(this.buffers);
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        this.bsBuffers = null;
        this.buffers.release();
        this.buffers = null;
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.enHandler.handle(this).onError(ctx::fireExceptionCaught).async(JContext.create(scheduler));
        if (!context.channel().config().isAutoRead()) {
            ctx.read();
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (readSendBox.isReady()) {
            readSendBox.cancel();
        }
        if (writeSendBox.isReady()) {
            writeSendBox.cancel();
        }
        super.channelInactive(ctx);
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
        markMode = true;
        buffers.markReaderIndex();
    }

    public void reset() {
        markMode = false;
        buffers.resetReaderIndex();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        buffer = buf.retain();
        try {
            buffers.addComponent(true, buf);
            if (readSendBox.isReady()) {
                readSendBox.handle();
            }
        } finally {
            buffer.release();
            buffer = null;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (readSendBox.isReady()) {
            readComplete = true;
            readSendBox.handle();
            readComplete = false;
        }
        super.channelReadComplete(ctx);
    }

    private void tryBestRelease() {
        if (!markMode) {
            buffers.discardSomeReadBytes();
        } else {
            int index = buffers.readerIndex();
            buffers.resetReaderIndex();
            buffers.discardSomeReadBytes();
            buffers.readerIndex(index);
        }
    }

    private void maybeReadMore() {
        if (!context.channel().config().isAutoRead()) {
            if (buffers.readableBytes() < capacity / 2) {
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
        if (!markMode) {
            buffers.markReaderIndex();
        }
        tryBestRelease();
        maybeReadMore();
    }

    @Override
    public ChannelHandlerContext getChannelContext() {
        return context;
    }

    private final SendBoxHandler<Byte> readByteImpl = (SendBox<Byte> sendBox) -> {
        if (buffers.isReadable()) {
            sendBox.resolve(buffers.readByte());
            return true;
        }
        return false;
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
            if (buffers.getByte(buffers.readerIndex()) == sendBox.byteArg(ARG_EXPECT)) {
                buffers.readByte();
                sendBox.resolve(true);
            } else {
                sendBox.resolve(false);
            }
            return true;
        }
        return false;
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
            sendBox.resolve(buffers.readShort());
            return true;
        }
        return false;
    };

    @Override
    public JPromise<Short> readShort() {
        SendBox<Short> sendBox = (SendBox<Short>) this.readSendBox;
        sendBox.install(readShortImpl);
        return createPromise(sendBox);
    }

    private final SendBoxHandler<Boolean> consumeShortImpl = (SendBox<Boolean> sendBox) -> {
        if (buffers.isReadable(2)) {
            if (buffers.getShort(buffers.readerIndex()) == sendBox.shortArg(ARG_EXPECT)) {
                buffers.readShort();
                sendBox.resolve(true);
            } else {
                sendBox.resolve(false);
            }
            return true;
        }
        return false;
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
            sendBox.resolve(buffers.readInt());
            return true;
        } else {
            return false;
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
            if (buffers.getInt(buffers.readerIndex()) == sendBox.intArg(ARG_EXPECT)) {
                buffers.readInt();
                sendBox.resolve(true);
            } else {
                sendBox.resolve(false);
            }
            return true;
        }
        return false;
    };

    @Override
    public JPromise<Boolean> consumeInt(int expected) {
        SendBox<Boolean> sendBox = (SendBox<Boolean>) this.readSendBox;
        sendBox.install(consumeIntImpl);
        ARG_EXPECT = sendBox.addIntArg(expected);
        return createPromise(sendBox);
    }

    private final SendBoxHandler<Integer> readUtf8CodePointImpl = (SendBox<Integer> sendBox) -> {
        if (!buffers.isReadable()) {
            return false;
        }
        int charRemaining = sendBox.intArg(ARG_CHAR_REMAINING);
        int cp = CharUtils.readUtf8CodePoint(bsBuffers, charRemaining);
        if (!CharUtils.isRemaining(cp)) {
            sendBox.resolve(cp);
            sendBox.setIntArg(ARG_CHAR_REMAINING, 0);
            return true;
        } else {
            sendBox.setIntArg(ARG_CHAR_REMAINING, cp);
            return false;
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
            return false;
        }
        int codePoint = sendBox.intArg(ARG_EXPECT);
        int charRemaining = sendBox.intArg(ARG_CHAR_REMAINING);
        int cp = CharUtils.readUtf8CodePoint(bsBuffers, charRemaining);
        if (!CharUtils.isRemaining(cp)) {
            if (cp == codePoint) {
                sendBox.resolve(true);
            } else {
                sendBox.resolve(false);
            }
            sendBox.setIntArg(ARG_CHAR_REMAINING, 0);
            return true;
        } else {
            sendBox.setIntArg(ARG_CHAR_REMAINING, cp);
            return false;
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
    private int ARG_CP_UNTIL_MODE;
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
            this.preventRelease = true;
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
            this.preventRelease = false;
            sendBox.setIntArg(ARG_CP_UNTIL_TESTED_INDEX, 0);
            sendBox.setIntArg(ARG_CP_UNTIL_TESTED_LEN, 0);
            return false;
        }
    }

    private final SendBoxHandler<Void> readUtf8UntilImpl = (SendBox<Void> sendBox) -> {
        if (!buffers.isReadable()) {
            return false;
        }
        ByteBuf output = sendBox.arg(ARG_BYTE_BUF_OUTPUT);
        int charRemaining = sendBox.intArg(ARG_CHAR_REMAINING);
        UntilMode mode = sendBox.arg(ARG_CP_UNTIL_MODE);
        do {
            int readerIndex = buffers.readerIndex();
            int cp = CharUtils.readUtf8CodePoint(bsBuffers, charRemaining);
            int cpLen = buffers.readerIndex() - readerIndex;
            if (!CharUtils.isRemaining(cp)) {
                if (matchCpUntil(sendBox, cp, cpLen)) {
                    output.writeBytes(buffers, readerIndex, cpLen);
                    if (mode != UntilMode.INCLUDE) {
                        int cpTestedLen = sendBox.intArg(ARG_CP_UNTIL_TESTED_LEN);
                        if (mode == UntilMode.EXCLUDE) {
                            buffers.readerIndex(buffers.readerIndex() - cpTestedLen);
                        } else if (mode != UntilMode.SKIP) {
                            throw new IllegalStateException();
                        }
                        output.writerIndex(output.writerIndex() - cpTestedLen);
                    }
                    sendBox.resolve(null);
                    return true;
                } else {
                    output.writeBytes(buffers, readerIndex, cpLen);
                }
            } else {
                int cpTestedLen = sendBox.intArg(ARG_CP_UNTIL_TESTED_LEN);
                sendBox.setIntArg(ARG_CP_UNTIL_TESTED_LEN, cpTestedLen + cpLen);
                return false;
            }
        } while (buffers.isReadable());
        return false;
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
        ARG_CP_UNTIL_MODE = this.readSendBox.addArg(mode);
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
            sendBox.setIntArg(ARG_BYTES_REMAINING, 0);
            sendBox.resolve(null);
            return true;
        } else {
            buffers.readBytes(outputBytes, currentOffset, readableBytes);
            sendBox.setIntArg(ARG_BYTES_REMAINING, remaining - readableBytes);
            return false;
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
        byte[] expectedBytes = sendBox.arg(ARG_BYTES_EXPECT);
        int offset = sendBox.intArg(ARG_BYTES_OFFSET);
        int length = sendBox.intArg(ARG_BYTES_LENGTH);
        int remaining = sendBox.intArg(ARG_BYTES_REMAINING);
        int currentOffset = offset + length - remaining;
        int readableBytes = buffers.readableBytes();
        int toReads = Math.min(readableBytes, remaining);
        for (int i = currentOffset; i < currentOffset + toReads; ++i) {
            if (buffers.readByte() != expectedBytes[i]) {
                sendBox.resolve(false);
                return true;
            }
        }
        if (readableBytes >= remaining) {
            sendBox.resolve(true);
            return true;
        } else {
            sendBox.setIntArg(ARG_BYTES_REMAINING, remaining - readableBytes);
            return false;
        }
    };

    @Override
    public JPromise<Boolean> consumeBytes(byte[] expectedBytes) {
        return consumeBytes(expectedBytes, 0, expectedBytes.length);
    }

    @Override
    public JPromise<Boolean> consumeBytes(byte[] expectedBytes, int offset, int length) {
        SendBox<Boolean> sendBox = (SendBox<Boolean>) this.readSendBox;
        sendBox.install(consumeByteImpl);
        ARG_BYTES_EXPECT = sendBox.addArg(expectedBytes);
        ARG_BYTES_REMAINING = sendBox.addIntArg(length);
        ARG_BYTES_OFFSET = sendBox.addIntArg(offset);
        ARG_BYTES_LENGTH = sendBox.addIntArg(length);
        return createPromise(sendBox);
    }

    private JPromise<Void> createPromise(ChannelFuture future) {
        return createPromise((thunk, ctx) -> future.addListener(f -> {
            if (f.isSuccess()) {
                thunk.resolve(null, ctx);
            } else if (f.isCancelled()) {
                thunk.cancel();
            } else {
                thunk.reject(f.cause(), ctx);
            }
        }));
    }

    private static int ARG_WRITE_BYTE_BUF;
    private static int ARG_WRITE_FLUSH;
    private final SendBoxHandler<ChannelFuture> writeBufferImpl = (sendBox) -> {
        if (context.channel().isWritable()) {
            ByteBuf buf = sendBox.arg(ARG_WRITE_BYTE_BUF);
            boolean flush = sendBox.intArg(ARG_WRITE_FLUSH) != 0;
            if (flush) {
                sendBox.resolve(context.channel().writeAndFlush(buf));
            } else {
                sendBox.resolve(context.channel().write(buf));
            }
            return true;
        }
        return false;
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
        return createPromise(sendBox).thenImmediate(this::createPromise);
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

    @Override
    public JPromise<Void> writeBytes(byte[] bytes) {
        ByteBuf buf = context.alloc().buffer(bytes.length).writeBytes(bytes);
        return writeBuffer(buf);
    }

    @Override
    public JPromise<Void> writeBytesAndFlush(byte[] bytes) {
        ByteBuf buf = context.alloc().buffer(bytes.length).writeBytes(bytes);
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
        byte[] bytes = s.getBytes(charset);
        return writeBytes(bytes);
    }

    @Override
    public JPromise<Void> writeStringAndFlush(String s, Charset charset) {
        byte[] bytes = s.getBytes(charset);
        return writeBytesAndFlush(bytes);
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
            EasyNettyChannelHandler.this.preventRelease = false;
        }

        private void prepare(JThunk<E> thunk, JContext context) {
            this.thunk = thunk;
            this.context = context;
        }

        public void install(SendBoxHandler<E> handler) {
            if (this.handler != null) {
                throw new IllegalStateException();
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

        public long longArg(int index) {
            return longArgs[index];
        }

        public void resolve(E value) {
            thunk.resolve(value, context);
        }

        public void reject(Throwable t) {
            thunk.reject(t, context);
        }

        public void cancel() {
            thunk.cancel();
        }

        public void handle() {
            accept(thunk, context);
        }

        @Override
        public void accept(JThunk<E> thunk, JContext context) {
            assert handler != null;
            prepare(thunk, context);
            try {
                if (handler.handle(this)) {
                    prepareNextRead();
                    reset();
                } else if (readComplete) {
                    thunk.reject(new IndexOutOfBoundsException(), context);
                    prepareNextRead();
                    reset();
                }
            } catch (Throwable t) {
                thunk.reject(t, context);
                reset();
            }
        }
    }

    interface SendBoxHandler<E> {
        boolean handle(SendBox<E> box);
    }
}
