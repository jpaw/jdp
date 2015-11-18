package de.jpaw.dp;

import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;

/** Caches scanned packages, because they might be needed multiple times. */
public class ReflectionsPackageCache {
    private static final ConcurrentHashMap<String,Reflections> scannedPackages = new ConcurrentHashMap<String,Reflections>();

    /** Clears all cached entries. Use this after initialization is complete. */
    public static void clear() {
        scannedPackages.clear();
    }

    public static Reflections get(String packagename) {
        Reflections r = scannedPackages.get(packagename);
        if (r == null) {
            r = new Reflections(packagename);
            scannedPackages.putIfAbsent(packagename, r);
        }
        return r;
    }

    /** Scan a list of package names and returns the array of Reflections. */
    public static Reflections [] getAll(String ... packagename) {
        Reflections [] result = new Reflections [packagename.length];
        for (int i = 0; i < packagename.length; ++i) {
            result[i] = get(packagename[i]);
        }
        return result;
    }
}
