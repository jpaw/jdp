package de.jpaw.dp;

/**
 * A provider is a class which returns the correct instance of a type every time the get() methos is invoked. In the current implementation, the built-in
 * providers for Singleton and Dependent are dependent scoped.
 */
public interface Provider<T extends Object> {
    public abstract T get();
}
