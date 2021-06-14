package de.jpaw.dp;

import de.jpaw.dp.Provider;

public interface CustomScope<T extends Object> extends Provider<T> {
    public abstract void set(final T instance);

    public abstract void close();
}
