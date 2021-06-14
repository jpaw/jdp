package de.jpaw.dp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    JdpTypeEntry(JdpEntry<T> initial) {
        addEntry(initial);
    }

    /* remove all entries */
    final void clear() {
        unqualifiedEntries.clear();
        qualifiedEntries.clear();
    }

    /* remove all entries for a given qualifier */
    final void clear(String qualifier) {
        List<JdpEntry<? extends T>> currentEntries = qualifiedEntries.get(qualifier);
        if (currentEntries != null)
            currentEntries.clear();
    }

    final void addEntry(JdpEntry<? extends T> additional) {
        final List<String> qualifiers = additional.qualifiers;
        if (qualifiers == null || qualifiers.size() == 0)
            unqualifiedEntries.add(additional);
        else {
            synchronized (locker) {
                for (String qualifier: qualifiers) {
                    List<JdpEntry<? extends T>> l = qualifiedEntries.get(qualifier);
                    if (l == null) {
                        qualifiedEntries.put(qualifier, newListWithInitialEntry(additional));
                    } else {
                        l.add(additional);
                    }
                }
            }
        }
    }

    private static <T> void join(StringBuilder b, List<JdpEntry<? extends T>> types) {
        for (int i = 0; i < types.size(); ++i) {
            if (i > 0)
                b.append(", ");
            b.append(types.get(i).actualType.getSimpleName());
        }
        b.append('\n');
    }
    String dump() {
        StringBuilder b = new StringBuilder(1000);
        b.append("- unnamed entries: ");
        join(b, unqualifiedEntries);
        for (Map.Entry<String, List<JdpEntry<? extends T>>> e : qualifiedEntries.entrySet()) {
            b.append(" - qualified by ");
            b.append(e.getKey());
            b.append(": ");
            join(b, e.getValue());

        }
        return b.toString();
    }

    Scopes getScopeForClassname(String classname, String qualifier) {
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

    T getInstanceForClassname(String classname, String qualifier) {
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

    int runForAll(String qualifier, JdpExecutor<T> lambda) {
        List<JdpEntry<? extends T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        int ctr = 0;
        if (baseList != null) {
            for (JdpEntry<? extends T> e : baseList) {
                lambda.accept(e.get());
                ++ctr;
            }
        }
        return ctr;
    }

    int runForAllEntries(String qualifier, JdpExecutor<JdpEntry<? extends T>> lambda) {
        List<JdpEntry<? extends T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        int ctr = 0;
        if (baseList != null) {
            for (JdpEntry<? extends T> e : baseList) {
                lambda.accept(e);
                ++ctr;
            }
        }
        return ctr;
    }

    /** Returns the global fallback, if it exists, else null. */
    Provider<? extends T> getGlobalFallback() {
        for (JdpEntry<? extends T> e : unqualifiedEntries) {
            if (e.isFallback && e.isAny)
                return e;
        }
        return null;
    }

    /** Return the set of qualifiers for which at least one entry exists. */
    Set<String> getQualifiers() {
        return qualifiedEntries.keySet();
    }

    List<JdpEntry<? extends T>> getEntries(String qualifier) {
        return (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
    }

    /** Returns an instance of every matching class for the given qualifier (or entries without a qualifier in case qualifier is null). */
    List<T> getAll(String qualifier) {
        List<JdpEntry<? extends T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        if (baseList == null)
            return null;
        List<T> elementList = new ArrayList<T>(baseList.size());
        for (JdpEntry<? extends T> e : baseList)
            elementList.add(e.get());
        return elementList;
    }

    /** Returns the list of matching classes for the given qualifier (or entries without a qualifier in case qualifier is null). */
    List<Class<? extends T>> getAllClasses(String qualifier) {
        List<JdpEntry<? extends T>> baseList = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
        if (baseList == null)
            return null;
        List<Class<? extends T>> elementList = new ArrayList<Class<? extends T>>(baseList.size());
        for (JdpEntry<? extends T> e : baseList)
            elementList.add(e.actualType);
        return elementList;
    }

    /** Returns an instance of every matching class regardless of qualifiers. */
    Set<T> getAll() {
        Set<T> result = new HashSet<T>();
        for (JdpEntry<? extends T> e: unqualifiedEntries) {
            result.add(e.get());
        }
        for (List<JdpEntry<? extends T>> ql : qualifiedEntries.values()) {
            for (JdpEntry<? extends T> e: ql) {
                result.add(e.get());
            }
        }
        return result;
    }

    /** Returns the set of matching classes regardless of qualifiers. */
    Set<Class<? extends T>> getAllClasses() {
        Set<Class<? extends T>> result = new HashSet<Class<? extends T>>();
        for (JdpEntry<? extends T> e: unqualifiedEntries) {
            result.add(e.actualType);
        }
        for (List<JdpEntry<? extends T>> ql : qualifiedEntries.values()) {
            for (JdpEntry<? extends T> e: ql) {
                result.add(e.actualType);
            }
        }
        return result;
    }
}
