/*
 * Copyright (C) 2015 Hamburg Sud and the contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
