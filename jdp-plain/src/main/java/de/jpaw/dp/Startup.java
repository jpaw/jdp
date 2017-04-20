package de.jpaw.dp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes annotated with @Startup will be loaded after all other Jdp initalization code for the specified package prefix. The classes must implement a public
 * static void method "onStartup", which will be invoked. In case multiple classes are annotated, the methods are invoked in ascending order of the annotation
 * parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Startup {
    public int value();
}
