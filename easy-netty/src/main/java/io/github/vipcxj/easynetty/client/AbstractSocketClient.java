package io.github.vipcxj.easynetty.client;

import io.github.vipcxj.easynetty.utils.PlatformIndependent;
import io.github.vipcxj.easynetty.utils.PromiseUtils;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public abstract class AbstractSocketClient implements Client {

    protected final EventLoopGroup loopGroup;
    protected final boolean ownLoopGroup;
    private Channel channel;
    private boolean foreverClosed;

    public AbstractSocketClient(EventLoopGroup loopGroup) {
        this.loopGroup = loopGroup;
        this.ownLoopGroup = false;
        this.foreverClosed = false;
    }

    public AbstractSocketClient() {
        this.loopGroup = PlatformIndependent.createEventLoopGroup();
        this.ownLoopGroup = true;
        this.foreverClosed = false;
    }

    private Class<? extends SocketChannel> getSocketChannelClass() {
        if (loopGroup instanceof EpollEventLoopGroup) {
            return EpollSocketChannel.class;
        } else if (loopGroup instanceof KQueueEventLoopGroup) {
            return KQueueSocketChannel.class;
        } else {
            return NioSocketChannel.class;
        }
    }

    @Override
    public JPromise<Void> connect(String host, int port) {
        if (foreverClosed) {
            throw new IllegalStateException("The client has been forever closed.");
        }
        Bootstrap bootstrap = new Bootstrap()
                .group(loopGroup)
                .channel(getSocketChannelClass());
        configure(bootstrap);
        ChannelFuture future = bootstrap.connect(host, port);
        this.channel = future.channel();
        channel.closeFuture().addListener(f -> {
            if (ownLoopGroup && foreverClosed) {
                loopGroup.shutdownGracefully();
            }
            channel = null;
        });
        return PromiseUtils.toPromise(future);
    }

    @Override
    public JPromise<Void> close(boolean forever) {
        if (channel != null) {
            foreverClosed = forever;
            return PromiseUtils.toPromise(channel.close());
        } else {
            return JPromise.error(new IllegalStateException("The client has not started yet or has been closed."));
        }
    }

    @Override
    public JPromise<Void> untilClose() {
        if (channel != null) {
            return PromiseUtils.toPromise(channel.closeFuture());
        } else {
            return JPromise.empty();
        }
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    protected abstract void configure(Bootstrap bootstrap);
}
