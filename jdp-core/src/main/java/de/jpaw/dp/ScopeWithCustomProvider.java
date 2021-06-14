package de.jpaw.dp;

import de.jpaw.dp.CustomScope;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ScopeWithCustomProvider {
    public Class<? extends CustomScope<?>> value();
}
