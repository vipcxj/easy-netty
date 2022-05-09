package io.github.vipcxj.easynetty.redis.message;

import io.github.vipcxj.easynetty.EasyNettyContext;
import io.github.vipcxj.jasync.ng.spec.JPromise;
import io.github.vipcxj.jasync.ng.spec.JPromiseTrigger;

import java.util.Objects;

public abstract class AbstractRedisMessage implements RedisMessage {

    protected final EasyNettyContext context;
    protected boolean complete;
    protected final JPromiseTrigger<Void> completeTrigger;

    public AbstractRedisMessage(EasyNettyContext context) {
        this.context = context;
        this.complete = false;
        this.completeTrigger = JPromise.createTrigger();
    }

    @Override
    public EasyNettyContext context() {
        return context;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public JPromise<Void> untilComplete() {
        return completeTrigger.getPromise();
    }

    protected void markComplete() {
        complete = true;
        completeTrigger.resolve(null);
    }

    protected void makeSureCompleted() {
        if (!complete) {
            throw new IllegalStateException("Not completed yet.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRedisMessage that = (AbstractRedisMessage) o;
        return complete == that.complete;
    }

    @Override
    public int hashCode() {
        return Objects.hash(complete);
    }
}
