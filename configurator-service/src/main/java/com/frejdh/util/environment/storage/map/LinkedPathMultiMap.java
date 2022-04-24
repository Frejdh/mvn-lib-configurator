package com.frejdh.util.environment.storage.map;

import com.google.common.reflect.TypeToken;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Multi-Map for paths, useful for property paths like:
 * path1.subpath1.subpath11=50
 * where "path1" contains an entry map of "path.subpath1", and "path1.subpath1.subpath11" contains a raw value of 50.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class LinkedPathMultiMap<V> implements Map<String, List<V>> {
	private static final Pattern ARRAY_PATTERN_FOR_KEY = Pattern.compile(".+\\[\\d+]");

	private final Class<V> valueClass;
	private final PathEntry<V> rootEntry;
	private int size;
	private final boolean cleanupKeys;
	private final boolean cleanupStringValues;

	public LinkedPathMultiMap() {
		this(null);
	}

	public LinkedPathMultiMap(Map<String, List<V>> map) {
		this(map, true, true);
	}

	public LinkedPathMultiMap(Map<String, List<V>> map, boolean cleanupKeys, boolean cleanupStringValues) {
		this(cleanupKeys, cleanupStringValues);
		if (map != null) {
			map.forEach(this::put);
		}
	}

	public LinkedPathMultiMap(boolean cleanupKeys, boolean cleanupStringValues) {
		this.cleanupKeys = cleanupKeys;
		this.cleanupStringValues = cleanupStringValues;
		this.valueClass = lookupValueClass();
		this.rootEntry = PathEntry.builder(valueClass).build();
	}

	@SuppressWarnings({"unchecked", "UnstableApiUsage"})
	private Class<V> lookupValueClass() {
		TypeToken<V> typeToken = new TypeToken<V>(getClass()) { };
		return (Class<V>) typeToken.getRawType();
	}

	private PathEntry<V> getDefaultValue() {
		return PathEntry.builder(valueClass).build();
	}

	private String toCleanPropertyKey(Object key) {
		return key != null ? cleanupPropertyKey(key.toString()) : null;
	}

	private String cleanupPropertyKey(String key) {
		return cleanupKeys && key != null
				? key.trim()
					.replace("_", ".")
					.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase()
				: key;
	}

	private String removeArraySuffix(Object key) {
		return key != null ? removeArraySuffix(key.toString()) : null;
	}

	private String removeArraySuffix(String key) {
		return key != null && key.matches(ARRAY_PATTERN_FOR_KEY.pattern()) ? key.replaceAll("\\[\\d+]$", "") : key;
	}

	private Object cleanupStringValue(Object value) {
		if (!cleanupStringValues) {
			return value;
		}

		if (value instanceof String) {
			// If wrapped by quotes, remove them from the string
			return ((String) value).trim().replaceAll("(^\")|(\"$)", "").replaceAll("(^')|('$)", "");
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	private List<V> cleanupStringValue(List<V> values) {
		if (!cleanupStringValues) {
			return values;
		}

		List<V> newList = new ArrayList<>();
		values.forEach(value -> {
			V newValue = value;
			if (newValue instanceof String) {
				// If wrapped by quotes, remove them from the string
				newValue = (V) ((String) value).trim().replaceAll("(^\")|(\"$)", "").replaceAll("(^')|('$)", "");
			}
			newList.add(newValue);
		});
		return newList;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return !rootEntry.hasChildren();
	}

	@Override
	public boolean containsKey(Object key) {
		return getEntry(key) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		value = cleanupStringValue(value);
		return containsValue(value, rootEntry);
	}

	private boolean containsValue(Object value, PathEntry<V> entry) {
		if (entry.getValues().contains(value)) {
			return true;
		}
		else if (entry.hasChildren()) {
			for (PathEntry<V> child : entry.getChildren().values()) {
				if (containsValue(value, child)) {
					return true;
				}
			}
		}
		return false;
	}

	public PathEntry<V> getEntry(Object key) {
		String keyString = key != null ? cleanupPropertyKey(key.toString()) : null;
		if (key == null) {
			return null;
		}
		return rootEntry.getPathEntryByKey(keyString);
	}

	@Override
	public List<V> get(Object key) {
		String keyString = toCleanPropertyKey(key);
		if (key == null) {
			return null;
		}
		return rootEntry.getValues(keyString);
	}

	public List<V> getOrDefault(Object key, List<V> defaultValue) {
		List<V> retval = get(key);
		return retval != null ? retval : defaultValue;
	}

	public V getByIndex(Object key, int index) {
		if (index < 0) {
			return null;
		}

		List<V> retval = get(removeArraySuffix(key));
		return retval != null && retval.size() > index ? retval.get(index) : null;
	}

	public V getFirstOrDefault(Object key, V defaultValue) {
		String keyString = toCleanPropertyKey(key);
		return rootEntry.getFirstValueOrDefault(keyString, defaultValue);
	}

	public V getFirst(Object key) {
		return getFirstOrDefault(key, null);
	}

	public V getLastOrDefault(Object key, V defaultValue) {
		String keyString = toCleanPropertyKey(key);
		return rootEntry.getLastValueOrDefault(keyString, defaultValue);
	}

	public V getLast(Object key) {
		return getLastOrDefault(key, null);
	}

	/**
	 * Puts a value into the map and replaces the existing values completely.
	 * @param key
	 * @param values
	 * @return
	 */
	public List<V> putAndReplace(@NotNull String key, List<V> values) {
		rootEntry.remove(key);
		return this.put(key, values);
	}

	@Nullable
	@Override
	public List<V> put(@NotNull String key, List<V> values) {
		key = cleanupPropertyKey(key);
		values = cleanupStringValue(values);

		PathEntry<V> baseElement = rootEntry.getPathEntryByKey(key);
		List<V> previousValue = baseElement != null ? baseElement.getValues() : null;
		int nrOfElementsAdded = rootEntry.put(key, values);
		size += nrOfElementsAdded;
		return previousValue;
	}

	public List<V> put(@NotNull String key, V value) {
		if (value instanceof List) {
			//noinspection unchecked
			return put(key, (List<V>) value);
		}

		return put(key, Collections.singletonList(value));
	}

	@Override
	public List<V> remove(Object key) {
		String keyString = key != null ? cleanupPropertyKey(key.toString()) : null;
		if (key == null) {
			return null;
		}

		PathEntry<V> baseElement = rootEntry.getPathEntryByKey(keyString);
		List<V> previousValue = baseElement != null ? baseElement.getValues() : null;
		if (baseElement == null) {
			baseElement = getDefaultValue();
		}

		int nrOfElementsRemoved = baseElement.remove(keyString);
		size -= nrOfElementsRemoved;
		return previousValue;
	}

	@Override
	@SuppressWarnings("Java8MapForEach")
	public void putAll(@NotNull Map<? extends String, ? extends List<V>> map) {
		map.entrySet().forEach(entry -> this.put(entry.getKey(), entry.getValue()));
	}

	public void putAllAndReplace(@NotNull Map<? extends String, ? extends List<V>> map) {
		map.keySet().forEach(rootEntry::remove);
		this.putAll(map);
	}

	@Override
	public void clear() {
		this.rootEntry.clear();
		this.size = 0;
	}

	public <T> T toObject(String key, Class<T> toClass) {
		return rootEntry.toObject(key, toClass);
	}

	public <T> T toObject(Class<T> toClass) {
		return rootEntry.toObject(toClass);
	}

	/**
	 * Convert to a HashMap without wrapper classes. This class will no longer be utilized and the property linking ability will be lost.
	 * @return A hashmap
	 */
	public HashMap<String, Object> toHashMap() {
		return rootEntry.toHashMap();
	}

	public <T> LinkedPathMultiMap<T> toMultiMap(String key, Class<T> innerObjectsClass) {
		return rootEntry.toMultiMap(key, innerObjectsClass);
	}

	@NotNull
	@Override
	public Set<String> keySet() {
		return rootEntry.getMapEntrySet().stream().map(Entry::getKey).collect(Collectors.toSet());
	}

	@NotNull
	@Override
	public Collection<List<V>> values() {
		return this.rootEntry.getValuesAsCollection();
	}

	@NotNull
	@Override
	public Set<Entry<String, List<V>>> entrySet() {
		Set<Entry<String, List<V>>> retval = new HashSet<>();
		rootEntry.getMapEntrySet().forEach((entry) -> retval.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getValues())));
		return retval;
	}

	@Override
	public String toString() {
		return rootEntry.toString();
	}

	public String toString(boolean includeChildren) {
		return rootEntry.toString(includeChildren);
	}
}
