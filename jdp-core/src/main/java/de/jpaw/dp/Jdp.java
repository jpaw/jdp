package de.jpaw.dp;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jpaw.dp.exceptions.DuplicateStartupSortOrderException;
import de.jpaw.dp.exceptions.MissingOnStartupMethodException;
import de.jpaw.dp.exceptions.NoSuitableImplementationException;

/** JDP - jpaw dependency provider. */

// This dependency provider knows the following JSR 330 stuff:

// Scopes:
// @Singleton (@Eager or @Lazy, by default @Lazy)
// @Dependent
// @PerThread

// @Inject for class fields:
// will change the field to final and perform initialization at construction (before constructor code is run!)
// An active annotation will change the @Inject to = Jdp.get or Jdp.getProvider()

// FEATURE: Can inject objects of any type (Strings, Integers etc)
// FEATURE: Does not need proxy objects, therefore final can be used on classes and methods as usual
// FEATURE: works with new(), does not need getInstance()
// BUT: ATTENTION: This of course cannot work if you have cyclic injections! (But that would be bad application design anyway)

// @Specializes is supported (is default if an annotated class is extended)
// @Alternative
// NOT: @Qualifier
// @Default
// @Named => for Qualifier

// RESTRICTION: All objects for a given type must have the same scope!

public class Jdp {
    private static final Logger LOG = LoggerFactory.getLogger(Jdp.class);
    static private final Map<Class<?>, JdpTypeEntry<?>> typeIndex = new ConcurrentHashMap<Class<?>, JdpTypeEntry<?>>(1000);
    
    // typesafe access methods
    static private <X> JdpTypeEntry<X> getType(Class<X> type) {
        return (JdpTypeEntry<X>) typeIndex.get(type);
    }
    
    
    
    /** returns an object of the requested type. Exception: if the type is an interface, an implementation of it is returned. */
    @Deprecated
    static public <T> T get(Class<T> type) {
        return getOptional(type, null);
    }
    static public <T> T getOptional(Class<T> type) {
        return getOptional(type, null);
    }
    static public <T> T get(Class<T> type, boolean isRequired) {
        return isRequired ? getRequired(type, null) : getOptional(type, null);
    }
    static public <T> T getRequired(Class<T> type) {
        return getRequired(type, null);
    }
    
    
    @Deprecated
    static public <T> T get(Class<T> type, String qualifier) {
        return getOptional(type, qualifier);
    }
    static public <T> T getOptional(Class<T> type, String qualifier) {
        Provider<? extends T> p = getProvider(type, qualifier);
        return p == null ? null : p.get();
    }
    static public <T> T getRequired(Class<T> type, String qualifier) {
        T result = getOptional(type, qualifier);
        if (result == null)
            throw new NoSuitableImplementationException(type, qualifier);
        return result;
    }
    static public <T> T get(Class<T> type, String qualifier, boolean isRequired) {
        return isRequired ? getRequired(type, qualifier) : getOptional(type, qualifier);
    }
    
    

    /** returns an object of the requested type. */
    static public <T> Provider<T> getProvider(Class<T> type) {
        return getProvider(type, null);
    }

    static public <T> Provider<T> getProvider(Class<T> type, String qualifier) {
        JdpTypeEntry<T> te = getType(type);
        return te == null ? null : (Provider<T>)te.getProvider(qualifier);  // Provider<? extends T> is of course also a provider of T 
    }

    /** Destructs all objects which have been created in this thread context. */
    static public void clearThreadContext() {

    }

    /** Get all valid matches. The primary match is returned as the first list element. */
    static public <T> List<T> getAll(Class<T> type) {
        return getAll(type, null);
    }

    /** Get all valid matches. The primary match is returned as the first list element. */
    static public <T> List<T> getAll(Class<T> type, String qualifier) {
        JdpTypeEntry<T> te = getType(type);
        return te == null ? null : te.getAll(qualifier);
    }

    /** Get the scope of the bound class by class name, without instantiating an instance.
     *   
     */
    static public Scopes getScopeForClassname(Class<?> baseClass, String name) {
        return getScopeForClassname(baseClass, name, null);
    }

    /** Get the scope of the bound class by class name, without instantiating an instance.
     *   
     */
    static public Scopes getScopeForClassname(Class<?> baseClass, String classname, String qualifier) {
        JdpTypeEntry<?> te = typeIndex.get(baseClass);
        return te == null ? null : te.getScopeForClassname(classname, qualifier);
    }

    /** Get an instance for a given class name.
     *   
     */
    static public <T> T getInstanceForClassname(Class<T> baseClass, String name) {
        return getInstanceForClassname(baseClass, name, null);
    }
    
    /** perform something on all types */
    static public <T> int forAll(Class<T> baseClass, JdpExecutor<T> lambda) {
        return forAll(baseClass, null, lambda);
    }
    
    /** perform something on all types */
    static public <T> int forAll(Class<T> baseClass, String qualifier, JdpExecutor<T> lambda) {
        JdpTypeEntry<T> te = getType(baseClass);
        return te == null ? 0 : te.runForAll(qualifier, lambda);
    }

    /** perform something on all types */
    static public <T> int forAllEntries(Class<T> baseClass, JdpExecutor<JdpEntry<? extends T>> lambda) {
        return forAllEntries(baseClass, null, lambda);
    }
    
    /** perform something on all types */
    static public <T> int forAllEntries(Class<T> baseClass, String qualifier, JdpExecutor<JdpEntry<? extends T>> lambda) {
        JdpTypeEntry<T> te = getType(baseClass);
        return te == null ? 0 : te.runForAllEntries(qualifier, lambda);
    }

    /** Get the scope of the bound class by class name, without instantiating an instance.
     *   
     */
    static public <T> T getInstanceForClassname(Class<T> baseClass, String classname, String qualifier) {
        JdpTypeEntry<T> te = getType(baseClass);
        return te == null ? null : te.getInstanceForClassname(classname, qualifier);
    }

    /** Bind target to the binding as primary, possibly clearing all other bindings. */
    static public <T> void bindInstanceTo(T target, String qualifier, boolean clearOthers) {
        bindInstanceTo(target, (Class<T>)target.getClass(), qualifier, clearOthers);
    }

    /** Bind target to the binding as primary, possibly clearing all other bindings.
     * fluent xtend use:
     * Mega k = new Mega();
     * k.bindInstanceTo("Mega", null, true);   */
    static public <T> void bindInstanceTo(T target, Class<T> type, String qualifier, boolean clearOthers) {

    }

    /** Bind a singleton class instance to its specific class type only, using no qualifier. */
    static public <T> void bind(T target) {
        JdpEntry<T> newEntry = new JdpEntry<T>(target, null);
        Class<T> cls = (Class<T>) target.getClass();
        register(cls, newEntry);
    }

    /** Bind a singleton class instance to its specific class type only, using an explicit qualifier. */
    static public <T> void bind(T target, String qualifier) {
        JdpEntry<T> newEntry = new JdpEntry<T>(target, qualifier);
        Class<T> cls = (Class<T>) target.getClass();
        register(cls, newEntry);
    }

    private static <T> void register(Class<? super T> forWhat, JdpEntry<T> entry) {
        synchronized (typeIndex) {
            JdpTypeEntry<? super T> e = getType(forWhat);
            if (e == null) {
                typeIndex.put(forWhat, new JdpTypeEntry<T>(entry));
            } else {
                e.addEntry(entry);
            }
        }
    }

    private static <Q> void registerClassAndAllInterfaces(Class<? super Q> cls, JdpEntry<Q> newEntry, Set<Class<?>> classesDone) {
        if (!classesDone.contains(cls)) {
            LOG.debug("    >>> registering also {}", cls.getCanonicalName());
            classesDone.add(cls);
            // register and descend recursion
            register(cls, newEntry);
            for (Class<?> i : cls.getInterfaces()) {    // JAVABUG: Java is not precise enough here. cls.getInterfaces should consist of entries with Q as uperclass only! 
                registerClassAndAllInterfaces((Class<? super Q>)i, newEntry, classesDone);
            }
//            LOG.info("    <<< registered  {}", cls.getCanonicalName());
        }
    }
    /** Registers a class to itself and to all of its directly implemented interfaces and to its superclasses
     * Called internally only. The scope passed from the outside, it is used for autodetection of the classes. */
    private static <T> void registerInternal(Class<T> cls, Scopes scope) {
        LOG.debug(">>> register called for class {}", cls.getCanonicalName());
        Set<Class<?>> classesDone = new HashSet<Class<?>>();
        JdpEntry<T> newEntry = new JdpEntry<T>(cls, scope);
        registerClassAndAllInterfaces(cls, newEntry, classesDone);
        Class<? super T> parent = cls.getSuperclass();
        while (parent != null && parent != Object.class) {
            registerClassAndAllInterfaces(parent, newEntry, classesDone);
            parent = parent.getSuperclass();
        }
//        LOG.info("<<< register done for class {}", cls.getCanonicalName());
    }

    /** Registers a class to itself and to all of its directly implemented interfaces and to its superclasses
     * Called internally only. The scope passed from the outside, it is used for autodetection of the classes. */
    public static <T> void registerWithCustomProvider(Class<T> cls, Provider<T> provider) {
        LOG.debug(">>> register CUSTOM called for class {}", cls.getCanonicalName());
        Set<Class<?>> classesDone = new HashSet<Class<?>>();
        JdpEntry<T> newEntry = new JdpEntry<T>(cls, provider);
        registerClassAndAllInterfaces(cls, newEntry, classesDone);
        Class<? super T> parent = cls.getSuperclass();
        while (parent != null && parent != Object.class) {
            registerClassAndAllInterfaces(parent, newEntry, classesDone);
            parent = parent.getSuperclass();
        }
//        LOG.info("<<< register CUSTOM done for class {}", cls.getCanonicalName());
    }

    static private void initsub(Reflections reflections, Class<? extends Annotation> annotationClass, Scopes scope) {
        Set<Class<?>> instances = reflections.getTypesAnnotatedWith(annotationClass);
        LOG.info("Found {} {}", instances.size(), annotationClass.getSimpleName());

        // bind them (and maybe load them eagerly)
        for (Class<?> s : instances) {
            registerInternal(s, scope);
        }
        
    }
    
    /** Scans the classpath for all (no)DI relevant annotations. */
    static public void init(String prefix) {
        LOG.info("JDP (a no DI framework) scanner running for package prefix {}", prefix);

        Reflections reflections = new Reflections(prefix);
        initsub(reflections, Singleton.class, Scopes.LAZY_SINGLETON);
        initsub(reflections, Dependent.class, Scopes.DEPENDENT);
        initsub(reflections, PerThread.class, Scopes.PER_THREAD);

        Set<Class<?>> startups = reflections.getTypesAnnotatedWith(Startup.class);
        if (startups.size() > 0) {
            Map<Integer, Class<?>> hashedStartups = new HashMap<Integer, Class<?>>(startups.size());
            for (Class<?> cls : startups) {
                // Startup anno = cls.getDeclaredAnnotation(Startup.class); // Java 1.8 only
                Startup anno = cls.getAnnotation(Startup.class); // not working
                Integer sortValue = anno.value();
                Class<?> oldVal = hashedStartups.put(sortValue, cls);
                if (oldVal != null) {
                    throw new DuplicateStartupSortOrderException(oldVal, cls, sortValue);
                }
            }
            // sort the stuff....
            SortedMap<Integer, Class<?>> sortedStartups = new TreeMap<Integer, Class<?>>(hashedStartups);
            // run the methods...
            for (Class<?> cls : sortedStartups.values()) {
                LOG.info("    invoking {}.onStartup()", cls.getCanonicalName());
                try {
                    Method startupMethod = cls.getMethod("onStartup");
                    startupMethod.invoke(cls);
                } catch (Exception e) {
                    throw new MissingOnStartupMethodException(cls, e);
                }
            }
        }
        LOG.info("JDP initialization for {} complete", prefix);
    }
}
