package de.jpaw.dp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JdpEntry<T> implements Provider<T> {
    private static final Logger LOG = LoggerFactory.getLogger(JdpEntry.class);
	public final String qualifier;
	public final Scopes   myScope;
	public final Class<T> actualType;				// the requested type (interface for example)
	public T instance = null;						// if it's a singleton: the unique instance (not null once it has been called the first time)
	
	
	private String autodetectQualifier() {
		Named anno = actualType.getAnnotation(Named.class);
		return anno == null ? null : anno.value();
	}
	// create a new entry from a provided instance - this is a singleton 
	JdpEntry(T providedInstance) {
		this((Class<T> )providedInstance.getClass(), Scopes.EAGER_SINGLETON);
		instance = providedInstance;
	}
	
	// create a new entry from a provided instance with qualifier - this is a singleton 
	JdpEntry(T providedInstance, String qualifier) {
		this.myScope = Scopes.EAGER_SINGLETON;
		this.actualType = (Class<T> )providedInstance.getClass();
		this.qualifier = qualifier;
		instance = providedInstance;
	}
	
	public JdpEntry(Class<T> actualType, Scopes myScope) {
		this.myScope = myScope;
		this.actualType = actualType;
		this.qualifier = autodetectQualifier();
	}
	
	public T get() {
		try {
			switch (myScope) {
			case EAGER_SINGLETON:
				return instance;
			case LAZY_SINGLETON:
				if (instance != null)
					return instance;
				else {
					// TODO: lock!
					instance = actualType.newInstance();
					return instance;
				}
			case DEPENDENT:
				// always return a new instance
				return actualType.newInstance();
			}
		} catch (Exception e) {
			LOG.error("Exception retrieving instance of {} with qualifier {}: {} ",
					actualType.getCanonicalName(),
					qualifier,
					e);
			return null;
		}
		return null;
	}
}
