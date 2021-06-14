package de.jpaw.dp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jpaw.dp.exceptions.CannotCreateProviderException;

/** The JdpEntry stores information about a specific class or instance.
 *
 * For every autodetected Class (i,e, class annotated by some scope annotation), there is
 * exactly one instance of JdpEntry.
 * The JdpEntry instances are indexed by the JdpTypeEntry class, which list possible assignments.
 * While a JdpEntry is an (almost) immutable class, the JdpTypeEntry instances can vary over time,
 * as programmatic bindings are performed. (JdpEntry is almost immutable, as the instance field is
 * initialized to null and only assigned later, when required, for classes of scope Singleton.)
 *
 * @author Michael Bischoff
 *
 * @param <T> - the actual class this instance describes.
 */
final public class JdpEntry<T> implements Provider<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdpEntry.class);
    protected final List<String> qualifiers;   // the qualifier - either provided with the constructor or autodetected from @Named annotation
    public final boolean isAlternative; // autodetected, if @Alternative annotation is set, the entry won't be used unless explicitly requested by config files or bind requests
    public final boolean isDefault;     // autodetected, if the @Default annotation is set, the entry will be choosen amount others with higher priority
    public final boolean isAny;         // autodetected, if the @Any     annotation is set and this entry has no qualifier, the entry will be choosen for other qualifiers as well
    public final boolean isFallback;    // autodetected, if the @Fallback annotation is set, the entry will be choosen only if no other exists.
    public final boolean specializes;   // autodetected, if @Specializes annotation is set, the entry will override any parent class
    public final Scopes myScope;
    public final Class<T> actualType; // the requested type (interface for example)
    private T instance = null; // if it's a singleton: the unique instance (not null once it has been called the first time)
    private final Provider<T> customScope;
//    private boolean overriddenBySpecialized = false;

    private static class DelegateProvider<T> implements Provider<T> {
        private final Class<T> cls;
        DelegateProvider(Class<T> cls) {
            this.cls = cls;
        }
        @Override
        public T get() {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Instantiating THREAD instance of {}", cls.getCanonicalName());
                    T instance1 = cls.newInstance();
                    LOGGER.debug("Instantiation of THREAD instance {} done", cls.getCanonicalName());
                    return instance1;
                }
                return cls.newInstance();
            } catch (Exception e) {
                LOGGER.error("Cannot instantiate class {}: {}", cls.getCanonicalName(), e.getMessage());
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
        this.qualifiers = new ArrayList<String>(1);
        this.instance = providedInstance;
        this.customScope = null;
        this.isAlternative = false;
        this.isDefault = false;
        this.isAny = false;
        this.isFallback = false;
        this.specializes = false;
        if (qualifier != null && qualifier.length() > 0)
            qualifiers.add(qualifier);
    }


    private final CustomScope<T> getCustomProvider(Class<T> actualType) {
        Class<? extends CustomScope<?>> myScopeClass = actualType.getAnnotation(ScopeWithCustomProvider.class).value();
        try {
            return (CustomScope<T>) myScopeClass.newInstance();
        } catch (InstantiationException e) {
            throw new CannotCreateProviderException(actualType, myScopeClass, e);
        } catch (IllegalAccessException e) {
            throw new CannotCreateProviderException(actualType, myScopeClass, e);
        }
    }

    private final List<String> getQualifiers(final Class<T> actualType) {
        final Named [] annotations = actualType.getAnnotationsByType(Named.class);
        if (annotations.length == 0)
            return Collections.emptyList();
        final List<String> names = new ArrayList<String>(annotations.length);
        for (Named anno : annotations)
            names.add(anno.value());
        return names;
    }

    /** create a new entry from an autodetected class. This can be any scope, the qualifier is read from annotations. */
    JdpEntry(Class<T> actualType, Scopes myScope) {
        this.myScope        = myScope;
        this.actualType     = actualType;
        this.qualifiers     = getQualifiers(actualType);
        this.isAlternative  = actualType.getAnnotation(Alternative.class) != null;
        this.isAny          = actualType.getAnnotation(Any.class) != null;
        this.isDefault      = actualType.getAnnotation(Default.class) != null;
        this.isFallback     = actualType.getAnnotation(Fallback.class) != null;
        this.specializes    = actualType.getAnnotation(Specializes.class) != null;
        this.customScope    = myScope == Scopes.PER_THREAD
                ? new ThreadScopeWithDelegate(new DelegateProvider(actualType))
                : myScope == Scopes.CUSTOM ? getCustomProvider(actualType) : null;
    }

    /** create a new entry for a manual assignment. */
    JdpEntry(Class<T> actualType, Provider<T> customProvider) {
        this.myScope        = Scopes.CUSTOM;
        this.actualType     = actualType;
        this.qualifiers     = getQualifiers(actualType);
        this.isAlternative  = actualType.getAnnotation(Alternative.class) != null;
        this.isAny          = actualType.getAnnotation(Any.class) != null;
        this.isDefault      = actualType.getAnnotation(Default.class) != null;
        this.isFallback     = actualType.getAnnotation(Fallback.class) != null;
        this.specializes    = actualType.getAnnotation(Specializes.class) != null;
        this.customScope    = customProvider;
    }

    @Override
    public T get() {
        try {
            switch (myScope) {
            case EAGER_SINGLETON:
                return instance;
            case LAZY_SINGLETON:
                if (instance != null)
                    return instance;
                else {
                    // TODO: lock! But then be aware of deadlocks / cycles!
                    LOGGER.debug("Instantiating SINGLETON {}", actualType.getCanonicalName());
                    instance = actualType.newInstance();
                    LOGGER.debug("Instantiation of SINGLETON {} done", actualType.getCanonicalName());
                    return instance;
                }
            case DEPENDENT:
                // always return a new instance
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Instantiating DEPENDENT {}", actualType.getCanonicalName());
                    T instance1 = actualType.newInstance();
                    LOGGER.debug("Instantiation of DEPENDENT {} done", actualType.getCanonicalName());
                    return instance1;
                }
                return actualType.newInstance();
            case PER_THREAD:
            case CUSTOM:
                return customScope.get();
            }
        } catch (Exception e) {
            LOGGER.error("Exception retrieving instance of {} of scope {}: {}: {}",
                        actualType.getCanonicalName(), myScope.name(), e.getClass().getSimpleName(), e.getMessage());
            LOGGER.error("Stack trace of cause is:", e);
            return null;
        }
        return null;
    }

//  public boolean isOverriddenBySpecialized() {
//      return overriddenBySpecialized;
//  }
//
//  public void setOverriddenBySpecialized() {
//      this.overriddenBySpecialized = true;
//  }
}
