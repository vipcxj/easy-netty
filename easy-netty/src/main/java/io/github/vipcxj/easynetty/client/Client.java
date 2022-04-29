package io.github.vipcxj.easynetty.client;

import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.netty.channel.Channel;

public interface Client {
    JPromise<Void> connect(String host, int port);
    JPromise<Void> close(boolean forever);
    JPromise<Void> untilClose();
    Channel getChannel();
}
