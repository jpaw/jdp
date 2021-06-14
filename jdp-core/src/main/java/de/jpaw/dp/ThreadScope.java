package de.jpaw.dp;

public class ThreadScope<T> extends ThreadLocal<T> implements CustomScope<T> {
    public ThreadScope() {
    }

    @Override
    public void close() {
        remove();
    }
}
