package de.jpaw.dp;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    static final Map<Class<?>, JdpTypeEntry> typeIndex = new ConcurrentHashMap<Class<?>, JdpTypeEntry>(1000);
    
	/** returns an object of the requested type. Exception: if the type is an interface, an implementation of it is returned. */
	static public <T> T get(Class<T> type) {
		return get(type, null);
	}
	static public <T> T get(Class<T> type, String qualifier) {
		Provider<T> p = getProvider(type, qualifier);
		return p == null ? null : p.get();
	}
	/** returns an object of the requested type. */
	static public <T> Provider<T> getProvider(Class<T> type) {
		return getProvider(type, null);
	}
	static public <T> Provider<T> getProvider(Class<T> type, String qualifier) {
		JdpTypeEntry te = typeIndex.get(type);
		return te == null ? null : te.getProvider(qualifier);
	}
	
	/** Destructs all objects which have been created in this thread context. */
	static public void clearThreadContext() {
		
	}
	
	/** Get all valid matches. The primary match is returned as the first list element. */
	static public <T> List<T> getAll(Class<T> type, String qualifier) {
		return null;
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
	
	static public <T> void bind(T target, String qualifier) {
		JdpEntry<T> newEntry = new JdpEntry<T>(target, qualifier);
		Class<T> cls = (Class<T>) target.getClass();
		register(cls, cls, Scopes.EAGER_SINGLETON, newEntry);
	}
	
	private static <I,T> void register(Class<T> cls, Class<I> forWhat, Scopes scope, JdpEntry<T> entry) {
		synchronized (typeIndex) {
			JdpTypeEntry<T> e = typeIndex.get(forWhat);
			if (e == null) {
				typeIndex.put(forWhat, new JdpTypeEntry<T>(entry));
			} else {
				e.addEntry(entry);
			}
		}
	}
	
	/** Registers a class to itself and to all of its implemented interfaces. */
	private static <T> void register(Class<T> cls, Scopes scope) {
		JdpEntry<T> newEntry = new JdpEntry<T>(cls, scope);
		register(cls, cls, scope, newEntry);
		for (Class <?> i : cls.getInterfaces()) {
			register(cls, i, scope, newEntry);
		}
	}
	
	/** Scans the classpath for all (no)DI relevant annotations. */
	static public void init(String prefix) {
		LOG.info("JDP (a no DI framework) scanner running for package prefix {}", prefix);
		
		Reflections reflections = new Reflections(prefix);
		
		Set<Class<?>> singletons = reflections.getTypesAnnotatedWith(Singleton.class);
		LOG.info("Found {} singletons", singletons.size());
		
		// bind them (and maybe load them eagerly)
		for (Class<?> s : singletons) {
			Scopes scope = Scopes.LAZY_SINGLETON;
			register(s, scope);
		}
		
		Set<Class<?>> dependents = reflections.getTypesAnnotatedWith(Dependent.class);
		LOG.info("Found {} dependents", dependents.size());
	}
}
