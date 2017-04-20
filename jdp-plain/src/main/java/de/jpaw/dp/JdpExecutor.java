package de.jpaw.dp;

/** functional interface to allow walking through eligible instances, now compatible with Java 8 Consumer. */
public interface JdpExecutor<T> {
    void accept(T instance);
}
