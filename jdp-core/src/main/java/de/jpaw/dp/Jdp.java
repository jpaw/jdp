package de.jpaw.dp;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
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

import de.jpaw.dp.exceptions.ClassRegisteredTwiceException;
import de.jpaw.dp.exceptions.DuplicateStartupSortOrderException;
import de.jpaw.dp.exceptions.MissingOnStartupMethodException;
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
    private static final Logger LOG = LoggerFactory.getLogger(Jdp.class);
    static private final Map<Class<?>, JdpTypeEntry<?>> typeIndex = new ConcurrentHashMap<Class<?>, JdpTypeEntry<?>>(1000);
    static private Map<Class<?>, JdpEntry<?>> allAutodetectedClasses = new ConcurrentHashMap<Class<?>, JdpEntry<?>>(1000);  // used for determining the scope 
    static private Map<Class<?>, JdpEntry<?>> classesOverriddenBySpecialized = new ConcurrentHashMap<Class<?>, JdpEntry<?>>(1000);  // used to mark classes which are overridden
    
    // typesafe access methods
    static private <X> JdpTypeEntry<X> getType(Class<X> type) {
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
        		JdpEntry<? extends T> candidate = entries.get(0);		// first shot at an result
        		if (entries.size() > 1) {
        			// need to cut down the result set... Filter away all alternatives and ones which have been specialized
        			int countEligible = 0;
        			for (JdpEntry<? extends T> e : entries) {
        				if (!e.isAlternative && classesOverriddenBySpecialized.get(e.actualType) == null) {
        					++countEligible;
        					candidate = e;
        				}
        			}
        			if (countEligible > 1)
        				throw new NonuniqueImplementationException(type, qualifier);
        		}
    			return (Provider<T>)candidate;		// valid because JdpEntry<T> implements Provider<T>
        	}
        }
        return null; 
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
    static private <T> void bindEntryTo(JdpEntry<T> newEntry, Class<T> type, String qualifier, boolean clearOthers) {
        synchronized (typeIndex) {
            JdpTypeEntry<? super T> e = getType(type);
            if (e == null) {
                typeIndex.put(type, new JdpTypeEntry<T>(newEntry));
            } else {
                if (clearOthers)
                    e.clear();
                e.addEntry(newEntry);
            }
        }
    }
    /** Bind source to the binding as primary, possibly clearing all other bindings.
     * This uses the EAGER_SINGLETON type, as the instance does already exist. 
     * fluent xtend use:
     * Mega k = new Mega();
     * k.bindInstanceTo("Mega", null, true);   */
    static private <T> void bindInstanceTo(T source, Class<T> type, String qualifier, boolean clearOthers) {
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
    
    
    /** Bind a singleton class instance to its specific class type only, using no qualifier. */
    static public <T> void bind(T source) {
        JdpEntry<T> newEntry = new JdpEntry<T>(source, null);
        Class<T> cls = (Class<T>) source.getClass();
        register(cls, newEntry);
    }

    /** Bind a singleton class instance to its specific class type only, using an explicit qualifier. */
    static public <T> void bind(T source, String qualifier) {
        JdpEntry<T> newEntry = new JdpEntry<T>(source, qualifier);
        Class<T> cls = (Class<T>) source.getClass();
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
        if (allAutodetectedClasses.put(cls, newEntry) != null) {
        	throw new ClassRegisteredTwiceException(cls);
        }
//        if (classesOverriddenBySpecialized.get(cls) != null) {
//        	newEntry.setOverriddenBySpecialized();
//        }
        registerClassAndAllInterfaces(cls, newEntry, classesDone);
        Class<? super T> parent = cls.getSuperclass();
        while (parent != null && parent != Object.class) {
        	if (newEntry.specializes) {
        		classesOverriddenBySpecialized.put(parent, newEntry);
        	}
            registerClassAndAllInterfaces(parent, newEntry, classesDone);
            parent = parent.getSuperclass();
        }
//        LOG.info("<<< register done for class {}", cls.getCanonicalName());
    }

    /** Registers a class to itself and to all of its directly implemented interfaces and to its superclasses
     * The scope passed from the outside, it is used for autodetection of the classes. */
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
    
    static private final List<StartupShutdown> lifecycleBeans = new ArrayList<StartupShutdown>(40);
    
    /** Scans the classpath for all (no)DI relevant annotations. */
    static public void init(String prefix) {
        LOG.info("JDP (a no DI framework) scanner running for package prefix {}", prefix);

        Reflections reflections = new Reflections(prefix);
        initsub(reflections, Singleton.class, Scopes.LAZY_SINGLETON);
        initsub(reflections, Dependent.class, Scopes.DEPENDENT);
        initsub(reflections, PerThread.class, Scopes.PER_THREAD);
        initsub(reflections, ScopeWithCustomProvider.class, Scopes.CUSTOM);

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
            	// determine if we want the static or the dynamic variant
            	if (StartupOnly.class.isAssignableFrom(cls)) {
            		// dynamic path
                    LOG.info("    invoking {}.onStartup()", cls.getCanonicalName());
                    StartupOnly bean = null;
                    try {
						bean = (StartupOnly)cls.newInstance();
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
            		LOG.info("    invoking static {}.onStartup()", cls.getCanonicalName());
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
        LOG.info("JDP initialization for {} complete", prefix);
    }
    
    /** Runs all shutdown code. */
    static public void shutdown() {
        LOG.info("JDP SHUTDOWN called");
        int i = lifecycleBeans.size();
        while (i > 0) {
        	StartupShutdown bean = lifecycleBeans.get(--i);
            LOG.info("    invoking {}.onShutdown()", bean.getClass().getCanonicalName());
        	try {
        		bean.onShutdown();
        	} catch (Exception e) {
        		// we want to ensure that system level shutdown code is executed even if some business functions had issues
        		LOG.error("Shutdown problem: " + e.getMessage(), e);
        	}
        }
        lifecycleBeans.clear();			// be nice to duplicate calls of the shutdown method
        LOG.info("JDP shutdown complete");
    }
    
    /** Clears all information for a fresh restart. Required in testing environments due to the static data. */
    static public void reset() {
        LOG.info("JDP RESET called");
        lifecycleBeans.clear();
        typeIndex.clear();
        allAutodetectedClasses.clear(); 
        classesOverriddenBySpecialized.clear();
    }
}
