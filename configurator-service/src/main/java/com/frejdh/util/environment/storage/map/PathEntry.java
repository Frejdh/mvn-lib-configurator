package com.frejdh.util.environment.storage.map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PathEntry<V> {

	private final PathEntry<V> parent;
	private final Map<String, PathEntry<V>> children;
	private final String fullKey;
	private final String entryKey;
	private final List<V> fieldValues;
	private final Class<V> valueClass;
	private final boolean isRootEntry;

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JsonOrgModule()) // To convert org.json classes
			.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

	public PathEntry(PathEntry<V> parent, Map<String, PathEntry<V>> children, String entryKey, List<V> fieldValues, Class<V> valueClass) {
		this.parent = parent;
		this.children = children;
		this.entryKey = entryKey;
		this.fieldValues = fieldValues;
		this.valueClass = valueClass;

		// Custom logic section
		this.fullKey = setFullKey();
		this.isRootEntry = StringUtils.isBlank(fullKey);
	}

	private PathEntryBuilder<V> getDefaultEntryBuilder(String key) {
		if (key.contains(".")) {
			key = key.substring(0, key.indexOf("."));
		}

		return PathEntry.builder(valueClass)
				.withParent(this)
				.withKey(key);
	}

	public PathEntry<V> getParent() {
		return parent;
	}

	public Map<String, PathEntry<V>> getChildren() {
		return children;
	}

	private String setFullKey() {
		return (parent != null && StringUtils.isNotBlank(parent.fullKey) ? parent.fullKey + "." : "") + entryKey;
	}

	public String getFullKey() {
		return fullKey;
	}

	public List<V> getValues() {
		return fieldValues;
	}

	public List<V> getValues(String key) {
		PathEntry<V> entry = getChildEntry(key);
		return entry != null ? entry.getValues() : null;
	}

	public V getFirstValue() {
		return !fieldValues.isEmpty() ? fieldValues.get(0) : null;
	}

	public V getFirstValueOrDefault(V defaultValue) {
		V value = getFirstValue();
		return value != null ? value : defaultValue;
	}

	public V getFirstValue(String key) {
		PathEntry<V> entry = getChildEntry(key);
		List<V> entryFieldValues = entry != null ? entry.fieldValues : null;
		return (entryFieldValues != null && !entryFieldValues.isEmpty()) ? entryFieldValues.get(0) : null;
	}

	public V getFirstValueOrDefault(String key, V defaultValue) {
		V value = getFirstValue(key);
		return value != null ? value : defaultValue;
	}

	public V getLastValue() {
		return !fieldValues.isEmpty() ? fieldValues.get(fieldValues.size() - 1) : null;
	}

	public V getLastValueOrDefault(V defaultValue) {
		V value = getLastValue();
		return value != null ? value : defaultValue;
	}

	public V getLastValue(String key) {
		PathEntry<V> entry = getChildEntry(key);
		List<V> entryFieldValues = entry != null ? entry.fieldValues : null;
		return (entryFieldValues != null && !entryFieldValues.isEmpty()) ? entryFieldValues.get(entryFieldValues.size() - 1) : null;
	}

	public V getLastValueOrDefault(String key, V defaultValue) {
		V value = getLastValue(key);
		return value != null ? value : defaultValue;
	}

	public PathEntry<V> getChildEntry(String fullKey) {
		String nextKey = fullKey.contains(".") ? fullKey.substring(0, fullKey.indexOf(".")) : fullKey;
		if (!children.containsKey(nextKey)) {
			return null;
		}
		return getChildEntry(fullKey, nextKey);
	}

	private PathEntry<V> getChildEntry(String fullKey, String fullKeyForCurrentIteration) {
		String currentChildKey = fullKeyForCurrentIteration.contains(".") && !fullKeyForCurrentIteration.endsWith(".")
				? fullKeyForCurrentIteration.substring(fullKeyForCurrentIteration.lastIndexOf(".") + 1)
				: fullKeyForCurrentIteration;
		PathEntry<V> child = children.get(currentChildKey);
		if (child == null) {
			return null;
		}
		else if (child.fullKey.equalsIgnoreCase(fullKey)) {
			return child;
		}

		String nextChildKey = fullKey.replaceFirst(Pattern.quote(fullKeyForCurrentIteration) + "\\.?", "");
		if (nextChildKey.contains(".")) {
			nextChildKey = nextChildKey.substring(0, nextChildKey.indexOf("."));
		}
		String fullKeyForNextIteration = fullKeyForCurrentIteration + "." + nextChildKey;
		return child.getChildEntry(fullKey, fullKeyForNextIteration);
	}

	/**
	 * Add a value to this entry
	 * @return How many new elements that has been added
	 */
	public int put(String key, V value) {
		return put(key, Collections.singletonList(value));
	}

	/**
	 * Add a value to this entry
	 * @return How many new elements that has been added
	 */
	public int put(String nextKey, List<V> values) {
		if (nextKey == null || nextKey.isEmpty()) {
			return 0;
		}

		if (nextKey.contains(".")) {	// Nested property
			String firstChildKey = nextKey.substring(0, nextKey.indexOf("."));
			nextKey = nextKey.replaceFirst(Pattern.quote(firstChildKey + "."), "");
			PathEntry<V> child = children.getOrDefault(firstChildKey, getDefaultEntryBuilder(firstChildKey).build());
			children.putIfAbsent(firstChildKey, child);
			return child.put(nextKey, values);
		}

		PathEntry<V> lastChild = children.getOrDefault(nextKey, getDefaultEntryBuilder(nextKey).build());
		children.putIfAbsent(nextKey, lastChild);
		int nrOfOldElements = lastChild.fieldValues.size();
		lastChild.fieldValues.addAll(values);
		return lastChild.fieldValues.size() - nrOfOldElements;
	}



	/**
	 * Add a value to this entry
	 * @return How many new elements that has been added
	 */
	public int put(String key, V[] values) {
		return put(key, Arrays.stream(values).collect(Collectors.toList()));
	}

	/**
	 * Remove a whole entry
	 * @return How many new elements that has been removed
	 */
	public int remove(String key) {
		if (key.contains(".")) {
			String firstChildKey = key.substring(0, key.indexOf("."));
			String nextKeys = key.replaceFirst(Pattern.quote(firstChildKey + "."), "");
			PathEntry<V> child = children.get(firstChildKey);
			if (child == null) {
				return 0;
			}

			return child.remove(nextKeys);
		}
		int nrOfElements = fieldValues.size();
		fieldValues.clear();
		return nrOfElements;
	}


	/**
	 * Remove certain values from an entry
	 * @return How many new elements that has been removed
	 */
	public int remove(String key, List<V> values) {
		if (key.contains(".")) {
			String firstChildKey = key.substring(0, key.indexOf("."));
			String nextKeys = key.replaceFirst(Pattern.quote(firstChildKey + "."), "");
			PathEntry<V> child = children.get(firstChildKey);
			if (child == null) {
				return 0;
			}

			return child.remove(nextKeys, values);
		}
		int nrOfElements = fieldValues.size();
		fieldValues.removeAll(values);
		return nrOfElements - fieldValues.size();
	}

	/**
	 * Remove certain values from an entry
	 * @return How many new elements that has been removed
	 */
	public int remove(String key, V value) {
		return remove(key, Collections.singletonList(value));
	}

	public void clear() {
		children.clear();
		fieldValues.clear();
	}

	public Set<String> getKeySet() {
		return children.keySet();
	}

	public Collection<List<V>> getValuesAsCollection() {
		return getMapEntrySet().stream()
				.map(entry -> entry.getValue().getValues())
				.collect(Collectors.toList());
	}

	public Set<Map.Entry<String, PathEntry<V>>> getMapEntrySet() {
		Set<Map.Entry<String, PathEntry<V>>> retval = new HashSet<>();
		recursivelyPutMapEntrySetValues(retval);
		return retval;
	}

	/**
	 * Helper method for adding values to the entry set recursively.
	 * @param entrySet Set to add values to.
	 */
	private void recursivelyPutMapEntrySetValues(Set<Map.Entry<String, PathEntry<V>>> entrySet) {
		entrySet.addAll(children.values().stream()
				.map(entry -> new AbstractMap.SimpleEntry<>(entry.fullKey, entry))
				.collect(Collectors.toSet())
		);
		this.children.values().forEach(child -> child.recursivelyPutMapEntrySetValues(entrySet));
	}

	public boolean hasChildren() {
		return !children.isEmpty();
	}

	public boolean hasFieldValue() {
		return !fieldValues.isEmpty();
	}

	public <T> T toObject(String key, Class<T> toClass) {
		PathEntry<V> entry = getChildEntry(key);
		return entry != null ? toObject(entry, toClass) : null;
	}

	public <T> T toObject(Class<T> toClass) {
		return toObject(this, toClass);
	}

	private <T> T toObject(PathEntry<V> entry, Class<T> toClass) {
		JSONObject jsonObject = toJsonObject(entry);
		return OBJECT_MAPPER.convertValue(jsonObject, toClass);
	}

	public HashMap<String, Object> toHashMap() {
		return new HashMap<>(this.getMapEntrySet().stream().collect(Collectors.toMap(
				Map.Entry::getKey,
				Map.Entry::getValue
		)));
	}

	public HashMap<String, Object> toHashMap(String key) {
		PathEntry<V> child = getChildEntry(key);
		if (child == null) {
			return null;
		}

		HashMap<String, Object> retval = new HashMap<>();

		Object obj = this.getMapEntrySet();

		this.getMapEntrySet().forEach(entry -> {
			List<V> fieldValues = entry.getValue().getValues();
			retval.put(entry.getKey(), fieldValues);
			for (int i = 0; i < fieldValues.size(); i++) {
				if (!entry.getKey().matches(".+\\[\\d+]")) {
					retval.put(entry.getKey() + "[" + i + "]", fieldValues.get(i));
				}
			}
		});
		return retval;
	}

	private JSONObject toJsonObject(PathEntry<V> entry) {
		if (entry == null) {
			return null;
		}
		JSONObject retval = new JSONObject();
		retval.put(entry.entryKey, entry.fieldValues);
		entry.children.forEach((childKey, childValue) -> retval.put(childKey, toJsonObject(childValue)));
		return retval;
	}

	public static <V> PathEntryBuilder<V> builder(Class<V> valueClass) {
		return PathEntryBuilder.builder(valueClass);
	}

	public PathEntryBuilder<V> toBuilder() {
		return PathEntryBuilder.builder(this.valueClass)
				.withParent(this.parent)
				.withChildren(this.children)
				.withKey(this.entryKey)
				.withFieldValues(this.fieldValues);
	}

	public String toString(boolean includeChildren) {
		StringBuilder sb = new StringBuilder("PathEntry{ key = '").append(fullKey).append("', children = [");
		if (includeChildren) {
			sb.append(String.join(", ", children.entrySet().stream().map(children -> children.getValue().toString()).collect(Collectors.joining(", "))));
		}
		else {
			sb.append(String.join(", ", children.keySet()));
		}
		sb.append("], values = [");
		sb.append(fieldValues.stream().map(Object::toString).collect(Collectors.joining(", ")));
		sb.append("] }");
		return sb.toString();
	}

	@Override
	public String toString() {
		return toString(false);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PathEntry)) {
			return false;
		}
		final PathEntry<?> objAsPathEntry = (PathEntry<?>) obj;

		return this.fullKey.equals(objAsPathEntry.fullKey) && this.valueClass.equals(objAsPathEntry.valueClass);
	}

	public static final class PathEntryBuilder<V> {
		private PathEntry<V> parent;
		private final Map<String, PathEntry<V>> children = new HashMap<>();
		private String key = "";	 // The key for the entry itself, and potential sub-keys.
		private final List<V> fieldValues = new ArrayList<>();
		private final Class<V> valueClass;

		private PathEntryBuilder(Class<V> valueClass) {
			this.valueClass = valueClass;
		}

		public static <V> PathEntryBuilder<V> builder(Class<V> valueClass) {
			return new PathEntryBuilder<>(valueClass);
		}

		public PathEntryBuilder<V> withParent(PathEntry<V> parent) {
			this.parent = parent;
			return this;
		}

		public PathEntryBuilder<V> withChildren(Map<String, PathEntry<V>> children) {
			this.children.clear();
			this.children.putAll(children);
			return this;
		}

		public PathEntryBuilder<V> withKey(String key) {
			this.key = key;
			return this;
		}

		public PathEntryBuilder<V> withFieldValues(List<V> fieldValues) {
			this.fieldValues.clear();
			this.fieldValues.addAll(fieldValues);
			return this;
		}

		public PathEntryBuilder<V> withFieldValue(V fieldValue) {
			this.fieldValues.clear();
			this.fieldValues.add(fieldValue);
			return this;
		}

		public PathEntry<V> build() {
			return new PathEntry<>(parent, children, key, fieldValues, valueClass);
		}
	}
}
