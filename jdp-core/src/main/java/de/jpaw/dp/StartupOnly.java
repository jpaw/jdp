package de.jpaw.dp;

/** Interface to be implemented by classes which run code upon startup, but not shutdown.
 * The startup priority is determined by the numeric parameter to the @Startup annotation,
 * the shutdown sequence is the reverse of the startup sequence.
 *
 * This offers an alternative to invoking the static onStartup method.
 * The benefit of using the interface is, that reflection code to find the class and invoke the method can
 * be separated from the actual business logic, allowing for clearer stack traces in case of exceptions,
 * and separate exception codes. */
public interface StartupOnly {
    /** Code to be executed at startup time. */
    void onStartup();
}
