package io.github.vipcxj.easynetty.redis.command;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.easynetty.redis.command.spi.RedisCommandProvider;
import io.github.vipcxj.easynetty.redis.message.*;
import io.github.vipcxj.jasync.ng.spec.JPromise;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

public class RedisCommands {

    private final static Map<String, RedisCommandProvider> providers = getProviders();

    private static Map<String, RedisCommandProvider> getProviders() {
        ServiceLoader<RedisCommandProvider> loader = ServiceLoader.load(RedisCommandProvider.class, RedisCommands.class.getClassLoader());
        Map<String, RedisCommandProvider> providers = new HashMap<>();
        for (RedisCommandProvider provider : loader) {
            String name = provider.name();
            RedisCommandProvider oldProvider = providers.get(name);
            if (oldProvider != null) {
                if (provider.priority() >= oldProvider.priority()) {
                    providers.put(name, provider);
                }
            } else {
                providers.put(name, provider);
            }
        }
        return providers;
    }

    public static JPromise<RedisCommand> readCommand(EasyNettyContext context) {
        RedisMessage message = RedisMessages.readMessage(context).await();
        String name;
        if (message.type() == RedisType.ARRAY) {
            RedisArrayMessage arrayMessage = message.asArray();
            Iterator<JPromise<RedisMessage>> iterator = arrayMessage.messageIterator(true).await();
            if (iterator == null || !iterator.hasNext()) {
                return JPromise.just(new UnknownCommand(message));
            }
            RedisMessage firstMessage = iterator.next().await();
            if (firstMessage.type() == RedisType.BULK_STRING) {
                name = firstMessage.asBulkString().readStringContent().await();
            } else if (firstMessage.type() == RedisType.SIMPLE_STRING) {
                name = firstMessage.asSimpleString().content().await();
            } else {
                return JPromise.just(new UnknownCommand(message));
            }
        } else if (message.type() == RedisType.INLINE) {
            RedisInlineMessage inlineMessage = message.asInline();
            inlineMessage.read().await();
            name = inlineMessage.getName();
        } else {
            return JPromise.just(new UnknownCommand(message));
        }
        RedisCommandProvider provider = providers.get(name);
        if (provider == null) {
            return JPromise.just(new UnknownCommand(message));
        }
        return JPromise.just(provider.create(context, message));
    }
}
