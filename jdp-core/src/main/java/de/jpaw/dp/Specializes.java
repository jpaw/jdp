package de.jpaw.dp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Specializes qualifier defines that the annotated class has precedence over all classes it inherits.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Specializes {
}
