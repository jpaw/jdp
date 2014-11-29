package de.jpaw.dp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The JdpTypeEntry stores the lists of qualified and unqualified entries for a given interface (or base class).
 * The generics type parameter T is the precise class of the interface / base class. The lists are supertypes (which extend T) */
final class JdpTypeEntry<T> {
    private static final int INIT_ARRAY_SIZE = 4;
    private static final int INIT_MAP_SIZE = 2;
    private final List<JdpEntry<? extends T>> unqualifiedEntries = new ArrayList<JdpEntry<? extends T>>(INIT_ARRAY_SIZE);
    private final Map<String, List<JdpEntry<? extends T>>> qualifiedEntries = new HashMap<String, List<JdpEntry<? extends T>>>(INIT_MAP_SIZE);
    private final Object locker = new Object();

    private List<JdpEntry<? extends T>> newListWithInitialEntry(JdpEntry<? extends T> e) {
        List<JdpEntry<? extends T>> l = new ArrayList<JdpEntry<? extends T>>(INIT_ARRAY_SIZE);
        l.add(e);
        return l;
    }

    public JdpTypeEntry(JdpEntry<T> initial) {
        if (initial.qualifier == null)
            unqualifiedEntries.add(initial);
        else {
            qualifiedEntries.put(initial.qualifier, newListWithInitialEntry(initial));
        }
    }

    public void addEntry(JdpEntry<? extends T> additional) {
        if (additional.qualifier == null)
            unqualifiedEntries.add(additional);
        else {
            synchronized (locker) {
                List<JdpEntry<? extends T>> l = qualifiedEntries.get(additional.qualifier);
                if (l == null) {
                    qualifiedEntries.put(additional.qualifier, newListWithInitialEntry(additional));
                } else {
                    l.add(additional);
                }
            }
        }
    }

    public Scopes getScopeForClassname(String classname, String qualifier) {
        List<JdpEntry<? extends T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        if (baseList != null) {
            for (JdpEntry<? extends T> e : baseList) {
                if (e.actualType.getCanonicalName().equals(classname))
                    return e.myScope;
            }
        }
        // not found
        return null;
    }

    public T getInstanceForClassname(String classname, String qualifier) {
        List<JdpEntry<? extends T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        if (baseList != null) {
            for (JdpEntry<? extends T> e : baseList) {
                if (e.actualType.getCanonicalName().equals(classname))
                    return e.get(); // invoke the provider
            }
        }
        // not found
        return null;
    }
    
    public int runForAll(String qualifier, JdpExecutor<T> lambda) {
        List<JdpEntry<? extends T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        int ctr = 0;
        if (baseList != null) {
            for (JdpEntry<? extends T> e : baseList) {
                lambda.apply(e.get());
                ++ctr;
            }
        }
        return ctr;
    }
    
    public int runForAllEntries(String qualifier, JdpExecutor<JdpEntry<? extends T>> lambda) {
        List<JdpEntry<? extends T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        int ctr = 0;
        if (baseList != null) {
            for (JdpEntry<? extends T> e : baseList) {
                lambda.apply(e);
                ++ctr;
            }
        }
        return ctr;
    }
    
    public JdpEntry<? extends T> getProvider(String qualifier) {
        List<JdpEntry<? extends T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        return baseList != null && baseList.size() > 0 ? baseList.get(0) : null;
    }

    public List<T> getAll(String qualifier) {
        List<JdpEntry<? extends T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        if (baseList == null)
            return null;
        List<T> elementList = new ArrayList<T>(baseList.size());
        for (JdpEntry<? extends T> e : baseList)
            elementList.add(e.get());
        return elementList;
    }
}
