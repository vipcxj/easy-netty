package io.github.vipcxj.easynetty.redis.command.spi;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.redis.command.RedisCommand;
import io.github.vipcxj.easynetty.redis.message.RedisMessage;

public interface RedisCommandProvider {

    int DEFAULT_PRIORITY = 0;

    String name();
    RedisCommand create(EasyNettyContext context, RedisMessage message);
    default int priority() {
        return DEFAULT_PRIORITY;
    }
}
