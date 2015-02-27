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
}
