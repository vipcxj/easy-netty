package io.github.vipcxj.easynetty.utils;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.EventLoopTaskQueueFactory;
import io.netty.channel.SelectStrategyFactory;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.RejectedExecutionHandler;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

public class PlatformIndependent {

    public static EventLoopGroup createEventLoopGroup() {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup();
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup();
        } else {
            return new NioEventLoopGroup();
        }
    }

    public static EventLoopGroup createEventLoopGroup(int nThreads) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(nThreads);
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(nThreads);
        } else {
            return new NioEventLoopGroup(nThreads);
        }
    }

    public static EventLoopGroup createEventLoopGroup(ThreadFactory threadFactory) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(threadFactory);
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(threadFactory);
        } else {
            return new NioEventLoopGroup(threadFactory);
        }
    }

    public static EventLoopGroup createEventLoopGroup(int nThreads, SelectStrategyFactory selectStrategyFactory) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(nThreads, selectStrategyFactory);
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(nThreads, selectStrategyFactory);
        } else {
            return new NioEventLoopGroup(nThreads);
        }
    }

    public static EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(nThreads, threadFactory);
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(nThreads, threadFactory);
        } else {
            return new NioEventLoopGroup(nThreads, threadFactory);
        }
    }

    public static EventLoopGroup createEventLoopGroup(int nThreads, Executor executor) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(nThreads, executor);
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(nThreads, executor);
        } else {
            return new NioEventLoopGroup(nThreads, executor);
        }
    }

    public static EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory, SelectStrategyFactory selectStrategyFactory) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(nThreads, threadFactory, selectStrategyFactory);
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(nThreads, threadFactory, selectStrategyFactory);
        } else {
            return new NioEventLoopGroup(nThreads, threadFactory);
        }
    }

    public static EventLoopGroup createEventLoopGroup(int nThreads, Executor executor, SelectStrategyFactory selectStrategyFactory) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(nThreads, executor, selectStrategyFactory);
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(nThreads, executor, selectStrategyFactory);
        } else {
            return new NioEventLoopGroup(nThreads, executor);
        }
    }

    public static EventLoopGroup createEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectStrategyFactory selectStrategyFactory) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(nThreads, executor, chooserFactory, selectStrategyFactory);
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(nThreads, executor, chooserFactory, selectStrategyFactory);
        } else {
            return new NioEventLoopGroup(nThreads, executor);
        }
    }

    public static EventLoopGroup createEventLoopGroup(
            int nThreads, Executor executor,
            EventExecutorChooserFactory chooserFactory,
            SelectStrategyFactory selectStrategyFactory,
            RejectedExecutionHandler rejectedExecutionHandler
    ) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(nThreads, executor, chooserFactory, selectStrategyFactory, rejectedExecutionHandler);
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(nThreads, executor, chooserFactory, selectStrategyFactory, rejectedExecutionHandler);
        } else {
            return new NioEventLoopGroup(nThreads, executor);
        }
    }

    public static EventLoopGroup createEventLoopGroup(
            int nThreads, Executor executor,
            EventExecutorChooserFactory chooserFactory,
            SelectStrategyFactory selectStrategyFactory,
            RejectedExecutionHandler rejectedExecutionHandler,
            EventLoopTaskQueueFactory queueFactory
    ) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(
                    nThreads, executor,
                    chooserFactory,
                    selectStrategyFactory,
                    rejectedExecutionHandler,
                    queueFactory
            );
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(
                    nThreads, executor,
                    chooserFactory,
                    selectStrategyFactory,
                    rejectedExecutionHandler,
                    queueFactory
            );
        } else {
            return new NioEventLoopGroup(nThreads, executor);
        }
    }

    public static EventLoopGroup createEventLoopGroup(
            int nThreads, Executor executor,
            EventExecutorChooserFactory chooserFactory,
            SelectStrategyFactory selectStrategyFactory,
            RejectedExecutionHandler rejectedExecutionHandler,
            EventLoopTaskQueueFactory taskQueueFactory,
            EventLoopTaskQueueFactory tailTaskQueueFactory
    ) {
        if (Epoll.isAvailable()) {
            return new EpollEventLoopGroup(
                    nThreads, executor,
                    chooserFactory,
                    selectStrategyFactory,
                    rejectedExecutionHandler,
                    taskQueueFactory,
                    tailTaskQueueFactory
            );
        } else if (KQueue.isAvailable()) {
            return new KQueueEventLoopGroup(
                    nThreads, executor,
                    chooserFactory,
                    selectStrategyFactory,
                    rejectedExecutionHandler,
                    taskQueueFactory,
                    tailTaskQueueFactory
            );
        } else {
            return new NioEventLoopGroup(nThreads, executor);
        }
    }

    public static Class<? extends ServerSocketChannel> getServerSocketChannelClass() {
        if (Epoll.isAvailable()) {
            return EpollServerSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            return KQueueServerSocketChannel.class;
        } else {
            return NioServerSocketChannel.class;
        }
    }

    public static Class<? extends SocketChannel> getSocketChannelClass() {
        if (Epoll.isAvailable()) {
            return EpollSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            return KQueueSocketChannel.class;
        } else {
            return NioSocketChannel.class;
        }
    }
}
