package de.jpaw.dp;

/** This class is a support class for the PerThread scope.
 * The delegate passed in the constructor is similar to a dependent scope provider, this class caches instances per thread.
 */
public class ThreadScopeWithDelegate<T> extends ThreadLocal<T> implements CustomScope<T> {
    private final Provider<T> delegate;

    public ThreadScopeWithDelegate(Provider<T> delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public T get() {
        T current = super.get();   // query cache (ThreadLocal)
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
