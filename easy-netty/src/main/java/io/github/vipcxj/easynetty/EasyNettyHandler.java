package io.github.vipcxj.easynetty;

import io.github.vipcxj.jasync.ng.spec.JPromise;

public interface EasyNettyHandler {
    JPromise<Void> handle(EasyNettyContext context);
}
