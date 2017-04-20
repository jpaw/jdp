package de.jpaw.dp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Alternative qualifier defines that the annotated class is not selected for injection, unless explicitly bound.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Alternative {
}
