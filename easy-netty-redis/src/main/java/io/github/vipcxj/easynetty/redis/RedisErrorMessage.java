package io.github.vipcxj.easynetty.redis;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;

public class RedisErrorMessage extends AbstractRedisMessage {

    private String content;

    public RedisErrorMessage(EasyNettyContext context) {
        super(context);
    }

    @Override
    public RedisType type() {
        return RedisType.ERROR;
    }

    @Override
    public RedisErrorMessage asError() {
        return this;
    }

    public JPromise<String> content() {
        if (content == null) {
            content = context.readUtf8Until('\r', '\n').await();
        }
        return JPromise.just(content);
    }
}
