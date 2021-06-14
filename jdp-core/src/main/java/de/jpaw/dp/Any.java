package de.jpaw.dp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Can accompany @Inject, to return a list of types, instead of a single instance.
 * Can accompany a scope annotated implementation with @Fallback but without qualifier, to indicate that it should be used for any qualifier.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Any {
}
