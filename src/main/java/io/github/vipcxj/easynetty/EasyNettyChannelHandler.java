package io.github.vipcxj.easynetty;

import io.github.vipcxj.easynetty.collections.ReadTasksAppender;
import io.github.vipcxj.easynetty.readtasks.ReadTasks;
import io.github.vipcxj.jasync.ng.spec.JContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JThunk;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayDeque;
import java.util.Deque;

public class EasyNettyChannelHandler extends ChannelInboundHandlerAdapter {

    private final int capacity;
    private int pointer;
    private int mark;
    private int target;
    private final ReadTasksAppender<?> readTasks;
    private final Deque<ByteBuf> buffers;

    public EasyNettyChannelHandler(int capacity) {
        this.capacity = capacity;
        this.readTasks = new ReadTasksAppender<>();
        this.buffers = new ArrayDeque<>();
        this.pointer = 0;
        this.mark = this.target = -1;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    private void readByteImpl(JThunk<Byte> thunk, JContext context) {
        readTasks.append(ReadTasks.readByte(thunk));
    }

    public JPromise<Byte> readByte() {
        return JPromise.generate(this::readByteImpl);
    }

    private void readShortImpl(JThunk<Short> thunk, JContext context) {
        readTasks.append(ReadTasks.readShort(thunk));
    }

    public JPromise<Short> readShort() {
        return JPromise.generate(this::readShortImpl);
    }

    private void readIntImpl(JThunk<Integer> thunk, JContext context) {
        readTasks.append(ReadTasks.readInt(thunk));
    }

    public JPromise<Integer> readInt() {
        return JPromise.generate(this::readIntImpl);
    }

}
