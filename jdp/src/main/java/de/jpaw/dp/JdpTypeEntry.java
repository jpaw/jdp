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
	
	public JdpEntry<T> getProvider(String qualifier) {
		List<JdpEntry<T>> l = (qualifier == null ? unqualifiedEntries : qualifiedEntries.get(qualifier));
		return l != null && l.size() > 0 ? l.get(0) : null;
	}
}
