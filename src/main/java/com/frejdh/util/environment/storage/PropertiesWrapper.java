package com.frejdh.util.environment.storage;

import com.frejdh.util.environment.ConversionUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertiesWrapper extends Properties {

	public PropertiesWrapper() {
		super();
	}

	public PropertiesWrapper(Properties defaults) {
		if (defaults == null) {
			return;
		}

		setProperties(defaults.entrySet());
	}


	@Override
	public synchronized Object setProperty(String key, String value) {
		return super.setProperty(cleanupPropertyKey(key), value);
	}

	public synchronized void setProperties(Collection<Map.Entry<Object, Object>> entries) {
		Set<Map.Entry<Object, Object>> tmpEntries = new HashSet<>(entries);
		tmpEntries.forEach(entry -> setProperty(
				cleanupPropertyKey(entry.getKey()),
				entry.getValue() != null ? entry.getValue().toString() : null
		));
	}

	public synchronized void setProperties(Map<String, String> entries) {
		Map<String, String> tmpEntries = new HashMap<>(entries);
		cleanupPropertyKeys(tmpEntries);
		tmpEntries.forEach(this::setProperty);
	}

	@Override
	public String getProperty(String key) {
		return super.getProperty(cleanupPropertyKey(key));
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return super.getProperty(cleanupPropertyKey(key), defaultValue);
	}


	private String cleanupPropertyKey(Object key) {
		return key != null ? cleanupPropertyKey(key.toString()) : null;
	}

	private String cleanupPropertyKey(String key) {
		return key.replace("_", ".");
	}

	private void cleanupPropertyKeys(Map<String, String> newProperties) {
		Set<Map.Entry<String, String>> set = new HashSet<>(newProperties.entrySet());

		for (Map.Entry<String, String> entry : set) {
			String oldKey = entry.getKey();
			String newKey = cleanupPropertyKey(oldKey);
			if (!oldKey.equals(newKey)) {
				newProperties.put(newKey, Optional.ofNullable(entry.getValue()).orElse(""));
				newProperties.remove(oldKey);
			}
		}
	}

}
