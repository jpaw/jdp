package de.jpaw.dp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class JdpTypeEntry<T> {
    private static final int INIT_ARRAY_SIZE = 4;
    private static final int INIT_MAP_SIZE = 2;
    private final List<JdpEntry<T>> unqualifiedEntries = new ArrayList<JdpEntry<T>>(INIT_ARRAY_SIZE);
    private final Map<String, List<JdpEntry<T>>> qualifiedEntries = new HashMap<String, List<JdpEntry<T>>>(INIT_MAP_SIZE);
    private final Object locker = new Object();

    private List<JdpEntry<T>> newListWithInitialEntry(JdpEntry<T> e) {
        List<JdpEntry<T>> l = new ArrayList<JdpEntry<T>>(INIT_ARRAY_SIZE);
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

    public void addEntry(JdpEntry<T> additional) {
        if (additional.qualifier == null)
            unqualifiedEntries.add(additional);
        else {
            synchronized (locker) {
                List<JdpEntry<T>> l = qualifiedEntries.get(additional.qualifier);
                if (l == null) {
                    qualifiedEntries.put(additional.qualifier, newListWithInitialEntry(additional));
                } else {
                    l.add(additional);
                }
            }
        }
    }

    public Scopes getScopeForClassname(String classname, String qualifier) {
        List<JdpEntry<T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        if (baseList != null) {
            for (JdpEntry<T> e : baseList) {
                if (e.actualType.getCanonicalName().equals(classname))
                    return e.myScope;
            }
        }
        // not found
        return null;
    }

    public T getInstanceForClassname(String classname, String qualifier) {
        List<JdpEntry<T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        if (baseList != null) {
            for (JdpEntry<T> e : baseList) {
                if (e.actualType.getCanonicalName().equals(classname))
                    return e.get(); // invoke the provider
            }
        }
        // not found
        return null;
    }
    
    public int runForAll(String qualifier, JdpExecutor<T> lambda) {
        List<JdpEntry<T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        int ctr = 0;
        if (baseList != null) {
            for (JdpEntry<T> e : baseList) {
                lambda.apply(e.get());
                ++ctr;
            }
        }
        return ctr;
    }
    
    public int runForAllEntries(String qualifier, JdpExecutor<JdpEntry<T>> lambda) {
        List<JdpEntry<T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        int ctr = 0;
        if (baseList != null) {
            for (JdpEntry<T> e : baseList) {
                lambda.apply(e);
                ++ctr;
            }
        }
        return ctr;
    }
    
    public JdpEntry<T> getProvider(String qualifier) {
        List<JdpEntry<T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        return baseList != null && baseList.size() > 0 ? baseList.get(0) : null;
    }

    public List<T> getAll(String qualifier) {
        List<JdpEntry<T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        if (baseList == null)
            return null;
        List<T> elementList = new ArrayList<T>(baseList.size());
        for (JdpEntry<T> e : baseList)
            elementList.add(e.get());
        return elementList;
    }
}
