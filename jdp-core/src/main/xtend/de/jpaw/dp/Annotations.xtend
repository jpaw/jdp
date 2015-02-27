package de.jpaw.dp;

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.ElementType

// scopes
/** The Singleton scope defines that there is a single instance of this class constructed per JVM.
 * The instance is constructed when it is needed the first time.
 */
annotation Singleton {
}
/** The Dependent scope defines that there is a new instance of this class constructed every time the getter is invoked.
 */
annotation Dependent {
} 
/** The Dependent scope defines that there is a new instance of this class for every thread.
 */
annotation PerThread {
}

// annotation CustomScoped...
/** A provider is a class which returns the correct instance of a type every time the get() methos is invoked.
 * In the current implementation, the built-in providers for Singleton and Dependent are dependent scoped. 
 */
interface Provider<T> {
    def T get();
}

/** Can accompany @Inject, to return a list of types, instead of a single instance. */
annotation Any {
}

/** Can accompany @Inject, to indicate that a missing value is acceptable (i.e. the target field is nullable). */
annotation Optional {
}


interface CustomScope<T> extends Provider<T> {
    def void set(T instance);
    def void close();
}

// qualifiers
/** The Default qualifier defines that the annotated class is choosen before all other implementations, i.e. if multiple
 * bindings exist, the default one will be picked. */
@Retention(RetentionPolicy.RUNTIME)
annotation Default {
}

/** The Fallback qualifier defines that the annotated class is choosen if no other exist, 
 * as a last resort, i.e. it is opposite of Default.
 * Classes annotated with this are usually no operation stubs.
 */
@Retention(RetentionPolicy.RUNTIME)
annotation Fallback {
}

/** The Alternative qualifier defines that the annotated class is not selected for injection, unless explicitly bound. */
@Retention(RetentionPolicy.RUNTIME)
annotation Alternative {
}

/** The Specializes qualifier defines that the annotated class has precedence over all classes it inherits. */
@Retention(RetentionPolicy.RUNTIME)
annotation Specializes {
}

@Retention(RetentionPolicy.RUNTIME)
annotation Named {
    String value;
}

/** Classes annotated with @Startup will be loaded after all other Jdp initalization code for the specified package prefix.
 * The classes must implement a public static void method "onStartup", which will be invoked.
 * In case multiple classes are annotated, the methods are invoked in ascending order of the annotation parameter.
 */
@Retention(RetentionPolicy::RUNTIME)
@Target(ElementType::TYPE)
annotation Startup {
    int value;
}

@Retention(RetentionPolicy::RUNTIME)
@Target(ElementType::TYPE)
annotation ScopeWithCustomProvider {
    Class<? extends CustomScope<?>> value;
}

