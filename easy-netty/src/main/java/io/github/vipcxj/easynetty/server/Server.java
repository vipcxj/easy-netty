package io.github.vipcxj.easynetty.server;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.channel.Channel;

public interface Server {
    JPromise<Void> bind();
    JPromise<Void> close();
    JPromise<Void> untilReady();
    JPromise<Void> untilClose();
    Channel getChannel();
}
