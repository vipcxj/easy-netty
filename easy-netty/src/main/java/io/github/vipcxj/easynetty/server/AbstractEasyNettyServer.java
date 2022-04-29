package io.github.vipcxj.easynetty.server;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.EasyNettyHandler;
import io.github.vipcxj.easynetty.handler.EasyNettyChannelHandler;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;

public abstract class AbstractEasyNettyServer extends AbstractSocketServer implements EasyNettyHandler {

    public AbstractEasyNettyServer(int port) {
        super(port);
    }

    public AbstractEasyNettyServer(int port, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        super(port, bossGroup, workerGroup);
    }

    @Override
    protected void configure(ServerBootstrap bootstrap) {
        bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                preInitChannel(ch);
                ch.pipeline().addLast(new EasyNettyChannelHandler(AbstractEasyNettyServer.this));
                postInitChannel(ch);
            }
        });
    }

    protected void preInitChannel(Channel channel) {};
    protected void postInitChannel(Channel channel) {};

    @Override
    public abstract JPromise<Void> handle(EasyNettyContext context);
}
