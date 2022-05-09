package io.github.vipcxj.easynetty.redis.command;

import io.github.vipcxj.easynetty.redis.message.RedisMessage;

public interface RedisCommand {
    RedisMessage getMessage();
}
