package de.jpaw.dp;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Repeatable(Named.List.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Named {
    public String value();

    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        Named[] value();
    }
}
