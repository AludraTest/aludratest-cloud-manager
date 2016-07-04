package org.aludratest.cloud.web.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MonitoringMap<K, V> extends HashMap<K, V> {

	private static final long serialVersionUID = -1190284235482734396L;

	private Set<K> changedKeys = new HashSet<K>();

	private Set<K> removedKeys = new HashSet<K>();

	private Set<K> addedKeys = new HashSet<K>();

	@Override
	public V put(K key, V value) {
		if (containsKey(key)) {
			changedKeys.add(key);
		}
		else {
			addedKeys.add(key);
			removedKeys.add(key);
		}

		return super.put(key, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		if (containsKey(key)) {
			removedKeys.add((K) key);
			changedKeys.remove(key);
			addedKeys.remove(key);
		}
		return super.remove(key);
	}

	@Override
	public void clear() {
		removedKeys.addAll(keySet());
		changedKeys.clear();
		addedKeys.clear();

		super.clear();
	}

	public Set<K> getChangedKeys() {
		return Collections.unmodifiableSet(changedKeys);
	}

	public Set<K> getAddedKeys() {
		return Collections.unmodifiableSet(addedKeys);
	}

	public Set<K> getRemovedKeys() {
		return Collections.unmodifiableSet(removedKeys);
	}

	public void resetMonitor() {
		changedKeys.clear();
		addedKeys.clear();
		removedKeys.clear();
	}

}
