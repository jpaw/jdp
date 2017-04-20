package de.jpaw.dp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import de.jpaw.dp.CustomScope;
import de.jpaw.dp.Provider;

public class JdpThreadLocal<T> implements CustomScope<T>, AutoCloseable {
    final protected ConcurrentMap<Long, T> instances = new ConcurrentHashMap<Long, T>(31);

    private final Provider<T> factory;
    private final boolean allowOverwrite;
    private final boolean allowNullGets;

    public JdpThreadLocal(Provider<T> factory, boolean allowOverwrite, boolean allowNullGets) {
        this.factory = factory;
        this.allowOverwrite = allowOverwrite;
        this.allowNullGets = allowNullGets;
    }

    protected final Long threadId() {
        return Thread.currentThread().getId();
    }

    @Override
    public T get() {
        final Long id =  threadId();
        T value = instances.get(id);
        if (value == null) {
            if (factory == null) {
                if (allowNullGets)
                    return null;
                throw new RuntimeException("No value and no factory defined for " + getClass().getCanonicalName()); // not allowed to get a null
            }
            value = factory.get();
            instances.put(id, value);
        }
        return value;
    }

    @Override
    public void set(T instance) {
        T oldvalue = instances.put(threadId(), instance);
        if (!allowOverwrite && oldvalue != null)
            throw new RuntimeException("Cannot overwrite a non-null value for " + getClass().getCanonicalName()); // not allowed to overwrite a value
    }

    @Override
    public void close() {
        instances.remove(threadId()); // no error if no instance existed - multiple close ops are allowed!
    }
}
