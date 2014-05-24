package de.jpaw.dp;

public interface DependencyProvider {
	// full generic getters
	/** returns an object of the requested type. */
	Object get(Class<?> type, String name, String qualifier);
	/** returns an object of the requested type. */
	Provider<?> getProvider(Class<?> type, String name, String qualifier);
}
