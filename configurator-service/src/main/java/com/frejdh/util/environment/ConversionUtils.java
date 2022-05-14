package com.frejdh.util.environment;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

public class ConversionUtils {

	private static final List<Class<?>> supportedPrimitiveClasses = initSupportedClasses();

	private static List<Class<?>> initSupportedClasses() {
		return new ArrayList<>(Arrays.asList(
				Boolean.class, Integer.class, Long.class, Double.class, Float.class, Character.class, Short.class, Byte.class
		)); // Must contain valueOf(String) method
	}

	static <T> T convertStringToType(String value, Class<T> returnType) throws IllegalArgumentException {
		List<T> stringList = ConversionUtils.convertListStringToSubType(Collections.singletonList(value), returnType);
		return (stringList != null && !stringList.isEmpty()) ? stringList.get(0) : null;
	}

	@SuppressWarnings("unchecked")
	static <T> List<T> convertListStringToSubType(List<String> values, Class<T> returnType) throws IllegalArgumentException {
		if (returnType.isAssignableFrom(String.class)) {
			return (List<T>) values;
		}
		else if (supportedPrimitiveClasses.stream().noneMatch(supportedClass -> supportedClass.isAssignableFrom(returnType))) {
			throw new IllegalArgumentException("Bad return subtype for list \"" + values + "\". Was [" + returnType + "] but should be one of the following classes: " + supportedPrimitiveClasses);
		}

		for (Class<?> supportedClass : supportedPrimitiveClasses) {
			if (returnType.isAssignableFrom(supportedClass)) {
				try {
					return values.stream()
							.map(value -> {
								try {
									return (T) supportedClass.getDeclaredMethod("valueOf", String.class).invoke(null, value);
								} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
									e.printStackTrace();
									return null;
								}
							})
							.collect(Collectors.toList());
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public static List<String> getStringAsList(String text, String separatorCharacters) {
		return getStringAsList(text, separatorCharacters, true);
	}

	public static List<String> getStringAsList(String text, String separatorCharacters, boolean shouldTrim) {
		if (text == null) {
			return null;
		}
		else if (text.matches("\\s*\\[.*]\\s*")) {
			text = text.substring(text.indexOf("[") + 1, text.lastIndexOf("]"));
		}

		return new ArrayList<>(Arrays.asList(text.split("[" + separatorCharacters + "]"))).stream()
				.filter(element -> element != null && !element.isEmpty())
				.map(str -> shouldTrim ? str.trim() : str)
				.collect(Collectors.toList());
	}

	/**
	 * Converts a generic Map with potentially nested Map values (or normal Object values), to a single Map.
	 * Nested properties are defined with dots.
	 * @param mapToConvert The Map to flatten
	 * @return A new Map instance.
	 */
	@NonNull
	public static <T> Map<String, T> flattenMap(Map<?, T> mapToConvert) {
		final Map<String, T> propertiesMap = new HashMap<>();

		if (isNull(mapToConvert)) {
			return propertiesMap;
		}

		return flattenMap(mapToConvert, propertiesMap, "");
	}

	@SuppressWarnings("unchecked")
	@NonNull
	private static <T> Map<String, T> flattenMap(final Map<?, T> mapToConvert,
												  final Map<String, T> propertiesMap,
												  final String currentKey) {

		mapToConvert.forEach((key, value) -> {
			String nextKey = StringUtils.isBlank(currentKey)
					? Objects.toString(key, "")
					: currentKey + "." + Objects.toString(key, "");

			if (value instanceof Map) {
				Map<Object, T> mapValue = (Map<Object, T>) value;
				if (!mapValue.isEmpty()) {
					flattenMap(mapValue, propertiesMap, nextKey);
				}
			}
			else {
				propertiesMap.put(nextKey, value);
			}
		});

		return propertiesMap;
	}

	public static String toKebabCase(String str) {
		return (str != null)
			? str.trim()
				.replace("_", ".")
				.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase()
			: null;
	}

}
