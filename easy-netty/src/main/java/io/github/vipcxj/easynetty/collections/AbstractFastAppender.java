package io.github.vipcxj.easynetty.collections;

import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractFastAppender<E, T extends FastAppender.Node<E, T>> implements FastAppender<E, T> {

    private final Function<E, T> nodeFactory;
    private final T head;

    protected AbstractFastAppender(Function<E, T> nodeFactory) {
        this.nodeFactory = nodeFactory;
        this.head = this.nodeFactory.apply(null);
        setTail(this.head);
    }

    protected abstract T getTail();
    protected abstract void setTail(T tail);
    protected abstract void weakCasTail(T cmp, T tail);

    protected void append(T newNode, boolean isHead) {
        for (T t = getTail(), p = t;;) {
            T q = p.next();
            if (q == null) {
                T nn = isHead ? newNode.next() : newNode;
                if (p.setNextWhenNull(nn)) {
                    if (isHead && nn == null && (nn = newNode.next()) != null) {
                        if (!p.setNextWhenNull(nn)) {
                            continue;
                        }
                    }
                    if (p != t && nn != null) {
                        weakCasTail(t, nn);
                    }
                    return;
                }
            } else if (q == p) {
                p = (t != (t = getTail())) ? t : head;
            } else {
                p = (p != t && t != (t = getTail())) ? t : q;
            }
        }
    }

    public void append(E data) {
        Objects.requireNonNull(data);
        append(nodeFactory.apply(data), false);
    }

    protected void append(AbstractFastAppender<E, T> appender) {
        append(appender.head, true);
    }

    public void consume(FastAppender.Consumer<E> consumer) {
        T pre = head;
        T cur = head.next();
        while (cur != null) {
            if (consumer.consume(cur.data())) {
                T next = cur.next();
                pre.setNext(next);
                cur.setNext(null);
                cur = next;
            } else {
                pre = cur;
                cur = cur.next();
            }
        }
        setTail(pre);
    }

}
