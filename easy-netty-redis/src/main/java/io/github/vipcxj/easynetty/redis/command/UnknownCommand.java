package io.github.vipcxj.easynetty.redis.command;

import io.github.vipcxj.easynetty.redis.message.RedisMessage;

public class UnknownCommand implements RedisCommand {
    private final RedisMessage message;

    public UnknownCommand(RedisMessage message) {
        this.message = message;
    }

    @Override
    public RedisMessage getMessage() {
        return message;
    }


}
