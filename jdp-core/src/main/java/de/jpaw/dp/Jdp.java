package de.jpaw.dp;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import de.jpaw.dp.exceptions.ClassRegisteredTwiceException;
import de.jpaw.dp.exceptions.DuplicateStartupSortOrderException;
import de.jpaw.dp.exceptions.MissingOnStartupMethodException;
import de.jpaw.dp.exceptions.MultipleDefaultsException;
import de.jpaw.dp.exceptions.MultipleFallbacksException;
import de.jpaw.dp.exceptions.NoSuitableImplementationException;
import de.jpaw.dp.exceptions.NoSuitableProviderException;
import de.jpaw.dp.exceptions.NonuniqueImplementationException;
import de.jpaw.dp.exceptions.StartupBeanInstantiationException;
import de.jpaw.dp.exceptions.StartupMethodExecutionException;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(Jdp.class);
    private static final ConcurrentMap<String, Boolean> doNotRegisterFor     = new ConcurrentHashMap<String, Boolean>(32);   // can be used to avoid registering injections also for java.lang.*
    private static final ConcurrentMap<String, Boolean> onlyRegisterFor      = new ConcurrentHashMap<String, Boolean>(32);   // can be used to limit subtypes to listed packages

    private static final Map<Class<?>, JdpTypeEntry<?>> typeIndex            = new ConcurrentHashMap<Class<?>, JdpTypeEntry<?>>(1000);
    private static Map<Class<?>, JdpEntry<?>> allAutodetectedClasses         = new ConcurrentHashMap<Class<?>, JdpEntry<?>>(1000);  // used for determining the scope
    private static Map<Class<?>, JdpEntry<?>> classesOverriddenBySpecialized = new ConcurrentHashMap<Class<?>, JdpEntry<?>>(1000);  // used to mark classes which are overridden
    static public boolean registerAbstractClasses = false;  // normally, abstract classes should not be registered.

    // typesafe access methods
    private static <X> JdpTypeEntry<X> getType(Class<X> type) {
        return (JdpTypeEntry<X>) typeIndex.get(type);
    }

    public static String dump(Class<?> type) {
        JdpTypeEntry<?> te = getType(type);
        if (te == null)
            return "No entry for type " + type.getSimpleName();
        else
            return "Registered entries for type "  + type.getSimpleName() + " are\n" + te.dump();
    }

    public static String dump() {
        StringBuilder b = new StringBuilder(2000);
        b.append("Full Jdp type dump:\n");
        for (Map.Entry<Class<?>, JdpTypeEntry<?>> e : typeIndex.entrySet()) {
            b.append("Registered entries for type "  + e.getKey().getSimpleName() + " are\n" + e.getValue().dump());
        }
        return b.toString();
    }

    /** Superclass and interfaces are only registered if they do not start with one of the registered prefixes.
     * @param exclusion Package prefix to exclude, for example "java."
     */
    public static void excludePackagePrefix(String exclusion) {
        doNotRegisterFor.putIfAbsent(exclusion, Boolean.TRUE);
    }

    /** If at least one package prefix is provided by this method, then superclass and interfaces are only
     * registered if they start with one of the explicitly registered prefixes.
     * @param inclusion
     */
    public static void includePackagePrefix(String inclusion) {
        onlyRegisterFor.putIfAbsent(inclusion, Boolean.TRUE);
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

    /** Replaced by getOptional(). */
    @Deprecated
    static public <T> T get(Class<T> type, String qualifier) {
        return getOptional(type, qualifier);
    }
    static public <T> T getRequired(Class<T> type, String qualifier) {
        T result = getOptional(type, qualifier);
        if (result == null)
            throw new NoSuitableImplementationException(type, qualifier);
        return result;
    }
    static public <T> T getOptional(Class<T> type, String qualifier) {
        Provider<? extends T> p = getOptionalProvider(type, qualifier);
        return p == null ? null : p.get();
    }
    static public <T> T get(Class<T> type, String qualifier, boolean isRequired) {
        return isRequired ? getRequired(type, qualifier) : getOptional(type, qualifier);
    }



    /** returns a provider for the requested type. */
    static public <T> Provider<T> getProvider(Class<T> type) {
        return getProvider(type, null);
    }
    static public <T> Provider<T> getProvider(Class<T> type, String qualifier) {
        Provider<T> firstEntry = getOptionalProvider(type, qualifier);
        if (firstEntry == null) {
            throw new NoSuitableProviderException(type, qualifier);
        }
        return firstEntry;
    }
    static public <T> Provider<T> getOptionalProvider(Class<T> type, String qualifier) {
        JdpTypeEntry<T> te = getType(type);
        if (te != null) {
            List<JdpEntry<? extends T>> entries = te.getEntries(qualifier);
            if (entries != null && entries.size() > 0) {
                JdpEntry<? extends T> candidate = entries.get(0);       // first shot at an result
                if (entries.size() > 1) {
                    // perform a second scanning with detailed prioritization of the candidates
                    JdpEntry<? extends T> myFallback = null;
                    JdpEntry<? extends T> myDefault = null;
                    // need to cut down the result set... Filter away all alternatives, fallbacks and ones which have been specialized
                    int countEligible = 0;
                    for (JdpEntry<? extends T> e : entries) {
                        if (!e.isAlternative && classesOverriddenBySpecialized.get(e.actualType) == null) {
                            if (e.isDefault) {
                                if (myDefault != null)
                                    throw new MultipleDefaultsException(type, qualifier);
                                myDefault = e;
                            } else if (e.isFallback) {
                                if (myFallback != null)
                                    throw new MultipleFallbacksException(type, qualifier);
                                myFallback = e;
                            } else {
                                ++countEligible;
                                candidate = e;
                            }
                        }
                    }
                    if (myDefault != null)
                        return (Provider<T>)myDefault;  // one has been specified to be priority
                    if (countEligible > 1) {
                        // too many possibilities
                        throw new NonuniqueImplementationException(type, qualifier);
                    }
                    if (countEligible == 0) {
                        // throw new NoSuitableImplementationException(type, qualifier);
                        // fall through, use the initial entry (which at this point has turned out to be an Alternative)
                    }
                }
                return (Provider<T>)candidate;      // valid because JdpEntry<T> implements Provider<T>
            } else if (qualifier != null) {
                // was looking for a specific qualifier, but none found. See if a global fallback exists
                return (Provider<T>) te.getGlobalFallback();
            }
        }
        return null;
    }


    /** Destructs all objects which have been created in this thread context. */
    static public void clearThreadContext() {
    }

    /** Returns all qualifiers for which an implemenation has been found. */
    static public <T> Set<String> getQualifiers(Class<T> type) {
        JdpTypeEntry<T> te = getType(type);
        return te == null ? ImmutableSet.<String>of() : ImmutableSet.copyOf(te.getQualifiers());
    }

    /** Returns one instance per non-null qualifier, using the usual resolution rules. */
    static public <T> List<T> getOneInstancePerQualifier(Class<T> type) {
        JdpTypeEntry<T> te = getType(type);
        if (te == null)
            return ImmutableList.<T>of();
        Set<String> qualifiers = te.getQualifiers();
        if (qualifiers == null || qualifiers.size() == 0)
            return ImmutableList.<T>of();
        List<T> list = new ArrayList<T>(qualifiers.size() * 2);
        for (String qualifier : qualifiers) {
            list.add(getRequired(type, qualifier));
        }
        return list;
    }

    /** Returns one instance per non-null qualifier, using the usual resolution rules. */
    static public <T> Map<String, T> getInstanceMapPerQualifier(Class<T> type) {
        JdpTypeEntry<T> te = getType(type);
        if (te == null)
            return ImmutableMap.<String, T>of();
        Set<String> qualifiers = te.getQualifiers();
        if (qualifiers == null || qualifiers.size() == 0)
            return ImmutableMap.<String, T>of();
        Map<String, T> map = new HashMap<String, T>(qualifiers.size());
        for (String qualifier : qualifiers) {
            map.put(qualifier, getRequired(type, qualifier));
        }
        return map;
    }


    /** Get all valid matches regardless of qualifier, or null if the type is not known. */
    static public <T> Set<T> getAllAnyQualifier(Class<T> type) {
        JdpTypeEntry<T> te = getType(type);
        return te == null ? null : te.getAll();
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

    /** Get all valid matches regardless of qualifier, or null if the type is not known. */
    static public <T> Set<Class<? extends T>> getAllClassesAnyQualifier(Class<T> type) {
        JdpTypeEntry<T> te = getType(type);
        return te == null ? null : te.getAllClasses();
    }

    /** Get all valid matches. The primary match is returned as the first list element. */
    static public <T> List<Class<? extends T>> getAllClasses(Class<T> type) {
        return getAllClasses(type, null);
    }

    /** Get all valid matches. The primary match is returned as the first list element. */
    static public <T> List<Class<? extends T>> getAllClasses(Class<T> type, String qualifier) {
        JdpTypeEntry<T> te = getType(type);
        return te == null ? null : te.getAllClasses(qualifier);
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

    //*************************************************************************************************************************
    //
    //  Dynamic binding methods
    //
    //
    //*************************************************************************************************************************

    /** Bind source to its own class as the new goal, with a qualifier. */
    static public <T> void bindInstanceTo(T source, String qualifier) {
        bindInstanceTo(source, (Class<T>)source.getClass(), qualifier, true);
    }
    /** Bind source to its own class as the new goal for no qualifier.
     * Difference to bind() is that this one uses the main class only and does no recursion. */
    static public <T> void bindInstanceTo(T source) {
        bindInstanceTo(source, (Class<T>)source.getClass(), null, true);
    }

    /** Bind source to type as if it had the specified qualifier. */
    static public <T> void bindClassToQualifier(Class<? extends T> source, Class<T> type, String qualifier) {
        // the class must have been registered via autodetection without a qualifier before...
        JdpEntry<?> entry = allAutodetectedClasses.get(source);
        if (entry == null)
            throw new NoSuitableImplementationException(source);
        bindEntryTo((JdpEntry<T>)entry, type, qualifier, true);
    }
    /** Bind source to type as if it had no qualifier.
     * Useful for selection of a couple of alternatives via config file. */
    static public <T> void bindClassWithoutQualifier(Class<? extends T> source, Class<T> type) {
        // the class must have been registered via autodetection without a qualifier before...
        JdpEntry<?> entry = allAutodetectedClasses.get(source);
        if (entry == null)
            throw new NoSuitableImplementationException(source);
        bindEntryTo((JdpEntry<T>)entry, type, null, true);
    }


    /** internal subroutine: register an existing entry for a type. */
    private static <T> void bindEntryTo(JdpEntry<T> newEntry, Class<T> type, String qualifier, boolean clearOthers) {
        synchronized (typeIndex) {
            JdpTypeEntry<? super T> e = getType(type);
            if (e == null) {
                typeIndex.put(type, new JdpTypeEntry<T>(newEntry));
            } else {
                if (clearOthers) {
                    if (qualifier != null)
                        e.clear(qualifier);     // clear only entries for this qualifier
                    else
                        e.clear();              // clear all
                }
                e.addEntry(newEntry);
            }
        }
    }

    /** Bind source to the binding as primary, possibly clearing all other bindings.
     * This uses the EAGER_SINGLETON type, as the instance does already exist.
     * fluent xtend use:
     * Mega k = new Mega();
     * k.bindInstanceTo("Mega", null, true);   */
    private static <T> void bindInstanceTo(T source, Class<T> type, String qualifier, boolean clearOthers) {
        JdpEntry<T> newEntry = new JdpEntry<T>(source, qualifier);
        bindEntryTo(newEntry, type, qualifier, clearOthers);
    }
    // Java, give me default parameters please...
    static public <T> void bindInstanceTo(T source, Class<T> type, String qualifier) {
        JdpEntry<T> newEntry = new JdpEntry<T>(source, qualifier);
        bindEntryTo(newEntry, type, qualifier, true);
    }
    static public <T> void bindInstanceTo(T source, Class<T> type) {
        JdpEntry<T> newEntry = new JdpEntry<T>(source, null);
        bindEntryTo(newEntry, type, null, true);
    }


    /** Bind a singleton class instance to its specific class type only, using no qualifier.
     * Works for any class or interface, ignoring any exclusion prefix. */
    static public <T> void bind(T source) {
        JdpEntry<T> newEntry = new JdpEntry<T>(source, null);
        Class<T> cls = (Class<T>) source.getClass();
        register(cls, newEntry);
    }

    /** Bind a singleton class instance to its specific class type only, using an explicit qualifier.
     * Works for any class or interface, ignoring any exclusion prefix. */
    static public <T> void bind(T source, String qualifier) {
        JdpEntry<T> newEntry = new JdpEntry<T>(source, qualifier);
        Class<T> cls = (Class<T>) source.getClass();
        register(cls, newEntry);
    }

    static public <T> void bindByQualifierWithFallback(Class<T> interfaceClass, String qualifier) {
        T bean = Jdp.getRequired(interfaceClass, qualifier);
        if (qualifier != null) {
            // check for success or fallback selection
            Named anno = bean.getClass().getAnnotation(Named.class);
            if (anno == null || !qualifier.equals(anno.value())) {
                LOGGER.error("No {} implementation found for qualifier {}, using fallback", interfaceClass.getSimpleName(), qualifier);
            }
        }
        Jdp.bindInstanceTo(bean, interfaceClass); // set preference
    }

    /** Registers a class. Worker - no checks. */
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

    private static <Q> void registerClassAndAllInterfaces(Class<? super Q> cls, JdpEntry<Q> newEntry, Set<Class<?>> classesDone, boolean skipCheck) {
        if (!skipCheck) {
            if (onlyRegisterFor.size() > 0) {
                boolean includedPrefix = false;
                for (String inclusion : onlyRegisterFor.keySet()) {
                    if (cls.getCanonicalName().startsWith(inclusion)) {
                        includedPrefix = true;
                        break;
                    }
                }
                if (!includedPrefix) {
                    LOGGER.debug("    not registering for {}: not in inclusion list", cls.getCanonicalName());
                    return;
                }
            }
            for (String exclusion : doNotRegisterFor.keySet()) {
                if (cls.getCanonicalName().startsWith(exclusion)) {
                    LOGGER.debug("    not registering for {}: exclusion list entry {}", cls.getCanonicalName(), exclusion);
                    return;
                }
            }
        }
        if (!classesDone.contains(cls)) {
            LOGGER.debug("    registering for {}", cls.getCanonicalName());
            classesDone.add(cls);
            // register and descend recursion
            register(cls, newEntry);
            for (Class<?> i : cls.getInterfaces()) {    // JAVABUG: Java is not precise enough here. cls.getInterfaces should consist of entries with Q as superclass only!
                registerClassAndAllInterfaces((Class<? super Q>)i, newEntry, classesDone, false);
            }
        }
    }
    /** Registers a class to itself and to all of its directly implemented interfaces and to its superclasses
     * Called internally only. The scope passed from the outside, it is used for autodetection of the classes.
     * Registration is normally rejected for abstract classes. */
    private static <T> void registerInternal(Class<T> cls, Scopes scope) {
        if (cls.isInterface()) {
            LOGGER.debug("    not registering class {}: it's an interface!", cls.getCanonicalName());
            return;
        }
        if (!registerAbstractClasses && Modifier.isAbstract(cls.getModifiers())) {
            LOGGER.debug("    not registering class {}: it's abstract! (Set Jdp.registerAbstractClasses to true if you want that.)", cls.getCanonicalName());
            return;
        }
        
        LOGGER.debug("register({})", cls.getCanonicalName());
        JdpEntry<T> newEntry = new JdpEntry<T>(cls, scope);
        if (allAutodetectedClasses.put(cls, newEntry) != null) {
            throw new ClassRegisteredTwiceException(cls);
        }
        registerSub(cls, newEntry, false, true);
//        LOGGER.info("<<< register done for class {}", cls.getCanonicalName());
    }

    /** Registers a class to itself and to all of its directly implemented interfaces and to its superclasses
     * The scope passed from the outside, it is used for autodetection of the classes.
     * Here, abstract classes are possible, because the custom provider could return a non abstract subclass of it. */
    public static <T> void registerWithCustomProvider(Class<T> cls, Provider<T> provider) {
        LOGGER.debug("register({}) CUSTOM", cls.getCanonicalName());
        JdpEntry<T> newEntry = new JdpEntry<T>(cls, provider);
        registerSub(cls, newEntry, true, false);
//      LOGGER.info("<<< register CUSTOM done for class {}", cls.getCanonicalName());
    }

    private static <T> void registerSub(Class<T> cls, JdpEntry<T> newEntry, boolean skipCheck, boolean withSpecializes) { 
        Set<Class<?>> classesDone = new HashSet<Class<?>>();
        registerClassAndAllInterfaces(cls, newEntry, classesDone, skipCheck);
        Class<? super T> parent = cls.getSuperclass();
        while (parent != null && parent != Object.class) {
            if (withSpecializes && newEntry.specializes) {
                classesOverriddenBySpecialized.put(parent, newEntry);
            }
            registerClassAndAllInterfaces(parent, newEntry, classesDone, false);
            parent = parent.getSuperclass();
        }
    }

    private static void initsub(Reflections reflections, Class<? extends Annotation> annotationClass, Scopes scope) {
        Set<Class<?>> instances = reflections.getTypesAnnotatedWith(annotationClass);
        LOGGER.info("Found {} {}", instances.size(), annotationClass.getSimpleName());

        // bind them (and maybe load them eagerly)
        for (Class<?> s : instances) {
            registerInternal(s, scope);
        }

    }


    static public void scanClasses(String prefix) {
        LOGGER.info("Jdp (a no DI framework) scanner running for package prefix {}", prefix);

        scanClasses(ReflectionsPackageCache.get(prefix));
    }

    /** Scan classes for the provided reflections parameters. */
    static public void scanClasses(Reflections ... reflections) {
        for (int i = 0; i < reflections.length; ++i) {
            initsub(reflections[i], Singleton.class, Scopes.LAZY_SINGLETON);
            initsub(reflections[i], Dependent.class, Scopes.DEPENDENT);
            initsub(reflections[i], PerThread.class, Scopes.PER_THREAD);
            initsub(reflections[i], ScopeWithCustomProvider.class, Scopes.CUSTOM);
        }
    }

    private static final List<StartupShutdown> lifecycleBeans = new ArrayList<StartupShutdown>(40);
    private static final Set<Class<?>> lifecycleBeanSkips = new HashSet<Class<?>>(40);

    static public void runStartups(String prefix) {
        LOGGER.info("Jdp startup phase for {} begins", prefix);

        runStartups(ReflectionsPackageCache.get(prefix));

        LOGGER.info("Jdp startup phase for {} complete", prefix);
    }

    static public void runStartups(Reflections ... reflections) {
        for (int i = 0; i < reflections.length; ++i) {
            Set<Class<?>> startups = reflections[i].getTypesAnnotatedWith(Startup.class);
            if (startups.size() > 0) {
                Map<Integer, Class<?>> hashedStartups = new HashMap<Integer, Class<?>>(startups.size());
                for (Class<?> cls : startups) {
                    // check for exclusion list
                    if (!lifecycleBeanSkips.contains(cls)) {
                        // Startup anno = cls.getDeclaredAnnotation(Startup.class); // Java 1.8 only
                        Startup anno = cls.getAnnotation(Startup.class); // not working
                        Integer sortValue = anno.value();
                        Class<?> oldVal = hashedStartups.put(sortValue, cls);
                        if (oldVal != null) {
                            throw new DuplicateStartupSortOrderException(oldVal, cls, sortValue);
                        }
                    }
                }
                // sort the stuff....
                SortedMap<Integer, Class<?>> sortedStartups = new TreeMap<Integer, Class<?>>(hashedStartups);
                // run the methods...
                for (Map.Entry<Integer, Class<?>> se: sortedStartups.entrySet()) {
                    final Class<?> cls = se.getValue();
                    // determine if we want the static or the dynamic variant
                    final boolean byInstance = StartupOnly.class.isAssignableFrom(cls);
                    LOGGER.info("Startup stage {}: invoking {} {}.onStartup()", se.getKey(), byInstance ? "dynamic" : "static", cls.getCanonicalName());

                    if (byInstance) {
                        // dynamic path
                        StartupOnly bean = null;
                        try {
                            bean = (StartupOnly) cls.newInstance();
                        } catch (Exception e) {
                            // convert the RuntimeException
                            throw new StartupBeanInstantiationException(cls, e);
                        }
                        // invoke then method
                        bean.onStartup();

                        // Test if we want shutdown as well. In that case, register the bean.
                        if (bean instanceof StartupShutdown)
                            lifecycleBeans.add((StartupShutdown) bean);
                    } else {
                        // combined reflection code with invoke
                        Method startupMethod = null;
                        try {
                            startupMethod = cls.getMethod("onStartup");
                        } catch (Exception e) {
                            throw new MissingOnStartupMethodException(cls, e);
                        }
                        try {
                            startupMethod.invoke(cls);
                        } catch (Exception e) {
                            throw new StartupMethodExecutionException(cls, e);
                        }
                    }
                }

            }
        }
    }

    /** Combined scan / startup along the classpath for all (no)DI relevant annotations. */
    static public void init(String prefix) {
        scanClasses(prefix);
        runStartups(prefix);
    }

    /** Init for prescanned reflections parameters. */
    static public void init(Reflections ... reflections) {
        scanClasses(reflections);
        runStartups(reflections);
    }

    static public void skipStartupClass(Class<?> cls) {
        lifecycleBeanSkips.add(cls);
    }

    /** Runs all shutdown code. */
    static public void shutdown() {
        LOGGER.info("Jdp shutdown called");
        int i = lifecycleBeans.size();
        while (i > 0) {
            StartupShutdown bean = lifecycleBeans.get(--i);
            LOGGER.info("Shutdown: invoking {}.onShutdown()", bean.getClass().getCanonicalName());
            try {
                bean.onShutdown();
            } catch (Exception e) {
                // we want to ensure that system level shutdown code is executed even if some business functions had issues
                LOGGER.error("Shutdown problem: " + e.getMessage(), e);
            }
        }
        lifecycleBeans.clear();         // be nice to duplicate calls of the shutdown method
        LOGGER.info("Jdp shutdown complete");
    }

    /** Clears all information for a fresh restart. Required in testing environments due to the static data. */
    static public void reset() {
        LOGGER.info("Jdp reset called");
        doNotRegisterFor.clear();
        onlyRegisterFor.clear();
        lifecycleBeans.clear();
        lifecycleBeanSkips.clear();
        typeIndex.clear();
        allAutodetectedClasses.clear();
        classesOverriddenBySpecialized.clear();
    }
}
