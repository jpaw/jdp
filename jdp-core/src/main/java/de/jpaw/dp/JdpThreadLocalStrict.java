package de.jpaw.dp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Specialized class of JdpThreadLocal. */
public class JdpThreadLocalStrict<T> implements CustomScope<T>, AutoCloseable {
    final protected ConcurrentMap<Long, T> instances = new ConcurrentHashMap<Long, T>(31);

    protected final Long threadId() {
        return Thread.currentThread().getId();
    }

    @Override
    public T get() {
        T value = instances.get(threadId());
        if (value == null) {
            throw new RuntimeException("No value and no factory defined for " + getClass().getCanonicalName()); // not allowed to get a null
        }
        return value;
    }

    @Override
    public void set(T instance) {
        T oldvalue = instances.put(threadId(), instance);
        if (oldvalue != null)
            throw new RuntimeException("Cannot overwrite a non-null value for " + getClass().getCanonicalName()); // not allowed to overwrite a value
    }

    @Override
    public void close() {
        instances.remove(threadId()); // no error if no instance existed - multiple close ops are allowed!
    }
}
