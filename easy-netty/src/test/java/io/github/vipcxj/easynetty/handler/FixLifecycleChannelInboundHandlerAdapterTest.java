package io.github.vipcxj.easynetty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FixLifecycleChannelInboundHandlerAdapterTest {

    static class State {

        private static final int ST_UNUSED = 0;
        private static final int ST_USED = 1;
        private static final int ST_REGISTERED = 2;
        private static final int ST_ACTIVATED = 3;
        private static final int ST_INACTIVE = 4;
        private static final int ST_UNREGISTERED = 5;

        private int state = ST_UNUSED;

        public void toState(int next) {
            switch (state) {
                case ST_UNUSED:
                    Assertions.assertEquals(ST_USED, next);
                    state = ST_USED;
                    break;
                case ST_USED:
                    Assertions.assertEquals(ST_REGISTERED, next);
                    state = ST_REGISTERED;
                    break;
                case ST_REGISTERED:
                    Assertions.assertEquals(ST_ACTIVATED, next);
                    state = ST_ACTIVATED;
                    break;
                case ST_ACTIVATED:
                    Assertions.assertEquals(ST_INACTIVE, next);
                    state = ST_INACTIVE;
                    break;
                case ST_INACTIVE:
                    Assertions.assertEquals(ST_UNREGISTERED, next);
                    state = ST_UNREGISTERED;
                    break;
                case ST_UNREGISTERED:
                    Assertions.assertEquals(ST_UNUSED, next);
                    state = ST_UNUSED;
                    break;
                default:
                    Assertions.fail();
            }
        }
    }

    interface StateHandler extends ChannelHandler {
        int state();
    }

    static class TestHandler extends FixLifecycleChannelInboundHandlerAdapter implements StateHandler {

        private final State state = new State();

        @Override
        public int state() {
            return state.state;
        }

        @Override
        protected void handlerAdded0(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_USED);
            super.handlerAdded0(ctx);
        }

        @Override
        protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_UNUSED);
            super.handlerRemoved0(ctx);
        }

        @Override
        protected void channelRegistered0(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_REGISTERED);
            super.channelRegistered0(ctx);
        }

        @Override
        protected void channelUnregistered0(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_UNREGISTERED);
            super.channelUnregistered0(ctx);
        }

        @Override
        protected void channelActive0(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_ACTIVATED);
            super.channelActive0(ctx);
        }

        @Override
        protected void channelInactive0(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_INACTIVE);
            super.channelInactive0(ctx);
        }
    }

    static class NormalHandler extends ChannelInboundHandlerAdapter implements StateHandler {

        private final State state = new State();

        @Override
        public int state() {
            return state.state;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_USED);
            super.handlerAdded(ctx);
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_UNUSED);
            super.handlerRemoved(ctx);
        }

        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_REGISTERED);
            super.channelRegistered(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_UNREGISTERED);
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_ACTIVATED);
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            state.toState(State.ST_INACTIVE);
            super.channelInactive(ctx);
        }
    }

    @Test
    void testLifecycle1() {
        EmbeddedChannel channel = new EmbeddedChannel();
        TestHandler handler = new TestHandler();
        channel.pipeline().addLast(handler);
        channel.close();
        assertHandlerState(handler);
    }

    @Test
    void testLifecycle2() {
        EmbeddedChannel channel = new EmbeddedChannel();
        StateHandler handler = new TestHandler();
        channel.pipeline().addLast(handler);
        channel.pipeline().remove(handler);
        assertHandlerState(handler);
    }

    @Test
    void testLifecycle3() {
        StateHandler handler = new TestHandler();
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.close();
        assertHandlerState(handler);
    }

    @Test
    void testLifecycle4() {
        StateHandler handler0, handler1;
        EmbeddedChannel channel;
        handler0 = new TestHandler();
        handler1 = new TestHandler();
        channel = new EmbeddedChannel(handler0, handler1);
        channel.close();
        assertHandlerState(handler0);
        assertHandlerState(handler1);

        handler0 = new TestHandler();
        handler1 = new NormalHandler();
        channel = new EmbeddedChannel(handler0, handler1);
        channel.close();
        assertHandlerState(handler0);
        assertHandlerState(handler1);

        handler0 = new NormalHandler();
        handler1 = new TestHandler();
        channel = new EmbeddedChannel(handler0, handler1);
        channel.close();
        assertHandlerState(handler0);
        assertHandlerState(handler1);
    }

    private void assertHandlerState(StateHandler handler) {
        Assertions.assertEquals(State.ST_UNUSED, handler.state());
    }
}
