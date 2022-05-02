package io.github.vipcxj.easynetty.server;

import io.github.vipcxj.easynetty.utils.PlatformIndependent;
import io.github.vipcxj.easynetty.utils.PromiseUtils;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public abstract class AbstractSocketServer implements Server {
    protected final int port;
    protected final EventLoopGroup bossGroup;
    protected final boolean ownBossGroup;
    protected final EventLoopGroup workerGroup;
    protected final boolean ownWorkerGroup;
    protected Channel channel;
    private JPromiseTrigger<Void> readyTrigger;

    protected AbstractSocketServer(int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.port = port;
        this.bossGroup = bossGroup;
        this.ownBossGroup = false;
        this.workerGroup = workerGroup;
        this.ownWorkerGroup = false;
        this.readyTrigger = JPromise.createTrigger();
        this.readyTrigger.start();
    }

    protected AbstractSocketServer(int port) {
        this.port = port;
        this.bossGroup = PlatformIndependent.createEventLoopGroup();
        this.ownBossGroup = true;
        this.workerGroup = PlatformIndependent.createEventLoopGroup();
        this.ownWorkerGroup = true;
        this.readyTrigger = JPromise.createTrigger();
        this.readyTrigger.start();
    }

    private Class<? extends ServerSocketChannel> getServerSocketChannelClass() {
        if (bossGroup instanceof EpollEventLoopGroup) {
            return EpollServerSocketChannel.class;
        } else if (bossGroup instanceof KQueueEventLoopGroup) {
            return KQueueServerSocketChannel.class;
        } else {
            return NioServerSocketChannel.class;
        }
    }

    @Override
    public JPromise<Void> untilReady() {
        return readyTrigger.getPromise();
    }

    @Override
    public JPromise<Void> bind() {
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(getServerSocketChannelClass());
        configure(bootstrap);
        ChannelFuture future = bootstrap.bind(port);
        future.addListener(f -> {
            if (f.isSuccess()) {
                readyTrigger.resolve(null);
            } else if (f.isCancelled()) {
                readyTrigger.cancel();
            } else {
                readyTrigger.reject(f.cause());
            }
        });
        this.channel = future.channel();
        this.channel.closeFuture().addListener(f -> {
            if (ownWorkerGroup) {
                workerGroup.shutdownGracefully();
            }
            if (ownBossGroup) {
                bossGroup.shutdownGracefully();
            }
            this.channel = null;
        });
        return PromiseUtils.toPromise(future);
    }

    @Override
    public JPromise<Void> close() {
        return channel != null
                ? PromiseUtils.toPromise(channel.close())
                : JPromise.error(new IllegalStateException("The server has not started or has been closed."));
    }

    @Override
    public JPromise<Void> untilClose() {
        return channel != null ? PromiseUtils.toPromise(channel.closeFuture()) : JPromise.empty();
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    protected abstract void configure(ServerBootstrap bootstrap);
}
