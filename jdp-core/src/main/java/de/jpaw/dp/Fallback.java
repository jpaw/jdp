package de.jpaw.dp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Fallback qualifier defines that the annotated class is choosen if no other exist,
 * as a last resort, i.e. it is opposite of Default.
 * Classes annotated with this are usually no operation stubs.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Fallback {
}
