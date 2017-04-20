package de.jpaw.dp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Can accompany @Inject, to indicate that a missing value is acceptable (i.e. the target field is nullable).
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Optional {
}
