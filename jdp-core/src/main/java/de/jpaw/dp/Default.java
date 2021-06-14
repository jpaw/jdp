package de.jpaw.dp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The Default qualifier defines that the annotated class is choosen before all other implementations, i.e. if multiple
 * bindings exist, the default one will be picked.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Default {
}
