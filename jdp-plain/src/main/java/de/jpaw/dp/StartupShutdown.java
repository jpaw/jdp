package de.jpaw.dp;

/** Interface to be implemented by classes which run code upon startup and shutdown.
 * The startup priority is determined by the numeric parameter to the @Startup annotation,
 * the shutdown sequence is the reverse of the startup sequence. */
public interface StartupShutdown extends StartupOnly {
    void onShutdown();
}
