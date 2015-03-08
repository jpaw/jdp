package de.jpaw.dp;

public class ThreadScopeWithDelegate<T> extends ThreadLocal<T> implements CustomScope<T> {
    private final Provider<T> delegate;

    public ThreadScopeWithDelegate(Provider<T> delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public T get() {
        T current = get();
        if (current != null)
            return current;
        // no value is stored, but we have a delegate
        current = delegate.get();
        set(current);
        return current;
    }

    @Override
    public void close() {
        remove();
    }
}
