package de.jpaw.dp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The JdpEntry stores information about a specific class or instance.
 * The combination of actualType and qualifier should be unique in the system.
 * The JdpEntry instances are indexed by the JdpTypeEntry class. 
 *  
 * @author Michael Bischoff
 *
 * @param <T>
 */
final public class JdpEntry<T> implements Provider<T> {
    private static final Logger LOG = LoggerFactory.getLogger(JdpEntry.class);
    public final String qualifier;
    public final Scopes myScope;
    public final Class<T> actualType; // the requested type (interface for example)
    private T instance = null; // if it's a singleton: the unique instance (not null once it has been called the first time)
    private final Provider<T> customScope; 
    
    private static class DelegateProvider<T> implements Provider<T> {
        private final Class<T> cls;
        DelegateProvider(Class<T> cls) {
            this.cls = cls;
        }
        @Override
        public T get() {
            try {
                return cls.newInstance();
            } catch (Exception e) {
                LOG.error("Cannot instantiate class {}", cls.getCanonicalName());
                return null;
            }
        }
        
    }
    /** create a new entry from a provided instance without a qualifier - this is a singleton. */
    JdpEntry(T providedInstance) {
        this(providedInstance, null);
        instance = providedInstance;
    }

    /** create a new entry from a provided instance with a qualifier - this is a singleton. */
    JdpEntry(T providedInstance, String qualifier) {
        this.myScope = Scopes.EAGER_SINGLETON;
        this.actualType = (Class<T>) providedInstance.getClass();
        this.qualifier = qualifier;
        this.instance = providedInstance;
        this.customScope = null;
    }

    /** create a new entry from an autodetected class. This can be any scope, the qualifier is read from annotations. */
    public JdpEntry(Class<T> actualType, Scopes myScope) {
        this.myScope = myScope;
        this.actualType = actualType;
        Named anno = actualType.getAnnotation(Named.class);
        this.qualifier = (anno == null ? null : anno.value());
        this.customScope = myScope == Scopes.PER_THREAD ? new ThreadScopeWithDelegate(new DelegateProvider(actualType)) : null;
    }

    /** create a new entry from an autodetected class. This can be any scope, the qualifier is read from annotations. */
    public JdpEntry(Class<T> actualType, Provider<T> customProvider) {
        this.myScope = Scopes.CUSTOM;
        this.actualType = actualType;
        Named anno = actualType.getAnnotation(Named.class);
        this.qualifier = (anno == null ? null : anno.value());
        this.customScope = customProvider;
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
            case PER_THREAD:
            case CUSTOM:
                return customScope.get();
            }
        } catch (Exception e) {
            LOG.error("Exception retrieving instance of {} with qualifier {}: {} ", actualType.getCanonicalName(), qualifier, e);
            return null;
        }
        return null;
    }
}
