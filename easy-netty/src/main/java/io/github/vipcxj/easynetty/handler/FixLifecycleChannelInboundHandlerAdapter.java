package io.github.vipcxj.easynetty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class FixLifecycleChannelInboundHandlerAdapter extends ChannelInboundHandlerAdapter {

    private boolean addCalled;
    private boolean registeredCalled;
    private boolean activeCalled;
    private boolean unregisteredCalled;
    private boolean inactiveCalled;
    private boolean nextRegisteredShouldCalled;
    private boolean nextActiveShouldCalled;
    private ChannelHandlerContext context;

    protected FixLifecycleChannelInboundHandlerAdapter() {
        this.addCalled = false;
        this.registeredCalled = false;
        this.activeCalled = false;
        this.unregisteredCalled = false;
        this.inactiveCalled = false;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        context = createProxy(ctx);
        handlerAdded0(context);
        if (ctx.channel().isRegistered()) {
            channelRegistered(context);
        }
        if (ctx.channel().isActive()) {
            channelActive(context);
        }
        addCalled = true;
    }

    protected void handlerAdded0(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive()) {
            channelInactive(ctx);
        }
        if (ctx.channel().isRegistered()) {
            channelUnregistered(ctx);
        }
        handlerRemoved0(ctx);
        registeredCalled = unregisteredCalled = activeCalled = inactiveCalled = false;
    }

    protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (registeredCalled) {
            if (nextRegisteredShouldCalled) {
                ctx.fireChannelRegistered();
            }
            return;
        }
        channelRegistered0(context);
        if (ctx.channel().isActive()) {
            channelActive(context);
        }
        registeredCalled = true;
    }

    protected void channelRegistered0(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (unregisteredCalled) {
            return;
        }
        unregisteredCalled = true;
        if (ctx.channel().isActive()) {
            channelInactive(ctx);
        }
        channelUnregistered0(ctx);
    }

    protected void channelUnregistered0(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (activeCalled) {
            if (nextActiveShouldCalled) {
                ctx.fireChannelActive();
            }
            return;
        }
        activeCalled = true;
        channelActive0(ctx);
    }

    protected void channelActive0(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (inactiveCalled) {
            return;
        }
        inactiveCalled = true;
        channelInactive0(ctx);
    }

    protected void channelInactive0(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    public ChannelHandlerContext createProxy(ChannelHandlerContext target) {
        return (ChannelHandlerContext) Proxy.newProxyInstance(
                ChannelHandlerContextProxyHandler.class.getClassLoader(),
                new Class[] {ChannelHandlerContext.class},
                new ChannelHandlerContextProxyHandler(target)
        );
    }

    class ChannelHandlerContextProxyHandler implements InvocationHandler {

        private final ChannelHandlerContext target;

        ChannelHandlerContextProxyHandler(ChannelHandlerContext target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object returned;
            if ("fireChannelRegistered".equals(method.getName())) {
                if (addCalled) {
                    returned = target.fireChannelRegistered();
                } else {
                    nextRegisteredShouldCalled = true;
                    returned = proxy;
                }
            } else if ("fireChannelActive".equals(method.getName())) {
                if (registeredCalled) {
                    returned = target.fireChannelActive();
                } else {
                    nextActiveShouldCalled = true;
                    returned = proxy;
                }
            } else {
                returned = method.invoke(target, args);
            }
            if (returned == target) {
                return proxy;
            } else {
                return returned;
            }
        }
    }
}
