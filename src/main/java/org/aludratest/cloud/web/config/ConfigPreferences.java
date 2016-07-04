package org.aludratest.cloud.web.config;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aludratest.cloud.config.MutablePreferences;
import org.aludratest.cloud.config.Preferences;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;

public class ConfigPreferences implements MutablePreferences, Map<String, Object>, Serializable {

	private static final long serialVersionUID = 6438895490862215794L;

	private Map<String, String> localValues = new HashMap<String, String>();

	private Map<String, ConfigPreferences> childNodes = new HashMap<String, ConfigPreferences>();

	private String originalPath;
	
	private ConfigPreferences parent;

	/* For deserialization only. */
	public ConfigPreferences() {
	}

	private ConfigPreferences(ConfigPreferences parent, String originalPath) {
		this.parent = parent;
		this.originalPath = originalPath;
	}

	public static ConfigPreferences createRootConfigPreferences(Preferences originalRoot) {
		ConfigPreferences prefs = new ConfigPreferences(null, null);
		// copy all values
		copy(originalRoot, prefs);

		return prefs;
	}

	public static void copy(Preferences prefsFrom, MutablePreferences prefsTo) {
		copy(prefsFrom, prefsTo, false);
	}

	public static void copy(Preferences prefsFrom, MutablePreferences prefsTo, boolean deleteNotExisting) {
		// delete all keys and nodes not existing in source
		if (deleteNotExisting) {
			List<String> sourceKeyNames = Arrays.asList(prefsFrom.getKeyNames());
			for (String key : prefsTo.getKeyNames()) {
				if (!sourceKeyNames.contains(key)) {
					prefsTo.removeKey(key);
				}
			}

			List<String> sourceNodeNames = Arrays.asList(prefsFrom.getChildNodeNames());
			for (String node : prefsTo.getChildNodeNames()) {
				if (!sourceNodeNames.contains(node)) {
					prefsTo.removeChildNode(node);
				}
			}
		}

		// copy all keys
		for (String key : prefsFrom.getKeyNames()) {
			prefsTo.setValue(key, prefsFrom.getStringValue(key));
		}

		// copy all nodes
		for (String node : prefsFrom.getChildNodeNames()) {
			copy(prefsFrom.getChildNode(node), prefsTo.createChildNode(node), deleteNotExisting);
		}
	}


	@Override
	public String[] getKeyNames() {
		return localValues.keySet().toArray(new String[0]);
	}

	@Override
	public ConfigPreferences getChildNode(String name) {
		return childNodes.get(name);
	}

	@Override
	public String[] getChildNodeNames() {
		return childNodes.keySet().toArray(new String[0]);
	}

	@Override
	public int size() {
		return localValues.size();
	}

	@Override
	public boolean isEmpty() {
		return getKeyNames().length == 0 && getChildNodeNames().length == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return localValues.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return localValues.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return localValues.get(key);
	}

	@Override
	public Object put(String key, Object value) {
		if (value == null) {
			return localValues.put(key, null);
		}

		try {
			// try to auto-convert
			return localValues.put(key, (String) ConvertUtils.convert(value, String.class));
		}
		catch (ConversionException e) {
			return localValues.put(key, value.toString());
		}
	}

	@Override
	public Object remove(Object key) {
		return localValues.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		localValues.clear();
	}

	@Override
	public Set<String> keySet() {
		return localValues.keySet();
	}

	@Override
	public Collection<Object> values() {
		return Arrays.asList(localValues.values().toArray(new Object[0]));
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySet() {
		throw new UnsupportedOperationException();
	}

	protected String internalGetStringValue(String key) {
		return localValues.get(key);
	}

	@Override
	public void setValue(String key, String value) {
		put(key, value);
	}

	@Override
	public void setValue(String key, boolean value) {
		setValue(key, Boolean.toString(value));
	}

	@Override
	public void setValue(String key, int value) {
		setValue(key, Integer.toString(value));
	}

	@Override
	public void setValue(String key, double value) {
		setValue(key, Double.toString(value));
	}

	@Override
	public void setValue(String key, float value) {
		setValue(key, Float.toString(value));
	}

	@Override
	public void setValue(String key, char value) {
		setValue(key, "" + value);
	}

	@Override
	public MutablePreferences createChildNode(String name) {
		ConfigPreferences node = childNodes.get(name);
		if (node == null) {
			node = new ConfigPreferences(this, originalPath == null ? name : originalPath + "/" + name);
			childNodes.put(name, node);
		}

		return node;
	}

	@Override
	public void removeChildNode(String name) {
		childNodes.remove(name);
	}

	@Override
	public void removeKey(String key) {
		remove(key);
	}

	@Override
	public Preferences getParent() {
		return parent;
	}

	@Override
	public final String getStringValue(String key) {
		if (key.contains("/")) {
			String subnode = key.substring(0, key.indexOf('/'));
			String remainder = key.substring(key.indexOf('/') + 1);
			if ("".equals(remainder)) {
				return null;
			}
			Preferences child = getChildNode(subnode);
			if (child == null) {
				return null;
			}
			return child.getStringValue(remainder);
		}

		String value = internalGetStringValue(key);
		if (value != null) {
			value = resolveVariables(value);
		}
		return value;
	}

	@Override
	public final int getIntValue(String key, int defaultValue) {
		String val = getStringValue(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(val);
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	public final boolean getBooleanValue(String key, boolean defaultValue) {
		String val = getStringValue(key);
		if (val == null) {
			return defaultValue;
		}
		return Boolean.parseBoolean(val);
	}

	@Override
	public final float getFloatValue(String key, float defaultValue) {
		String val = getStringValue(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			return Float.parseFloat(val);
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	public final double getDoubleValue(String key, double defaultValue) {
		String val = getStringValue(key);
		if (val == null) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(val);
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	public final char getCharValue(String key, char defaultValue) {
		String val = getStringValue(key);
		if (val == null || val.length() == 0) {
			return defaultValue;
		}
		return val.charAt(0);
	}

	@Override
	public String getStringValue(String key, String defaultValue) {
		String val = getStringValue(key);
		return val == null ? defaultValue : val;
	}

	@Override
	public int getIntValue(String key) {
		return getIntValue(key, 0);
	}

	@Override
	public float getFloatValue(String key) {
		return getFloatValue(key, 0);
	}

	@Override
	public boolean getBooleanValue(String key) {
		return getBooleanValue(key, false);
	}

	@Override
	public char getCharValue(String key) {
		return getCharValue(key, '\0');
	}

	@Override
	public double getDoubleValue(String key) {
		return getDoubleValue(key, 0);
	}

	private static String resolveVariables(String template) {
		if (template == null) {
			return null;
		}
		String result = template;
		int varStartIndex;
		while ((varStartIndex = result.indexOf("${")) >= 0) {
			int endIndex = result.indexOf('}', varStartIndex + 2);
			String propKey = result.substring(varStartIndex + 2, endIndex);
			result = result.substring(0, varStartIndex) + System.getProperty(propKey) + result.substring(endIndex + 1);
		}
		return result;
	}

}
