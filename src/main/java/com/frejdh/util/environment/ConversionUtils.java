package com.frejdh.util.environment;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ConversionUtils {

	private static final List<Class<?>> supportedPrimitiveClasses = initSupportedClasses();
	private static final String ARRAY_SEPARATOR_CHARACTER = "\u0001;\u0001";

	private static List<Class<?>> initSupportedClasses() {
		return new ArrayList<>(Arrays.asList(
				Boolean.class, Integer.class, Long.class, Double.class, Float.class, Character.class, Short.class, Byte.class
		)); // Must contain valueOf(String) method
	}

	@SuppressWarnings("unchecked")
	static <T> T convertToType(String value, Class<T> returnType) throws IllegalArgumentException {
		if (returnType.isAssignableFrom(String.class)) {
			return (T) value;
		}

		for (Class<?> supportedClass : supportedPrimitiveClasses) {
			if (returnType.isAssignableFrom(supportedClass)) {
				try {
					return (T) supportedClass.getDeclaredMethod("valueOf", String.class).invoke(null, value);
				} catch (NumberFormatException e) {
					return null;
				} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}

		throw new IllegalArgumentException("Bad return type for \"" + value + "\". Was [" + returnType + "] but should be one of the following classes: " + supportedPrimitiveClasses);
	}

	/**
	 * Convert a string to list.
	 * @param text Text to convert
	 * @param separatorCharacters Characters to split with. Example string: ",.;|"
	 * @param subType Subtype for the list
	 * @return A mutable list
	 */
	static <T> List<T> stringToList(String text, String separatorCharacters, Class<T> subType) {
		return stringToList(text, separatorCharacters).stream().map(element -> convertToType(element, subType)).collect(Collectors.toList());
	}

	/**
	 * Same as {@link #stringToList(String, String, Class)} but with a predefined separator character.<br>
	 * IMPORTANT: Can only be used with JSON/JSON5 files! A custom separator must be used for other configuration files.
	 * @param text Text to convert
	 * @param subType Subtype for the list
	 * @return A mutable list
	 */
	static <T> List<T> stringToList(String text, Class<T> subType) {
		return stringToList(text, ARRAY_SEPARATOR_CHARACTER, subType);
	}

	/**
	 * Convert a string to list. Not recommended to use for properties set in JSON files.
	 * @param text Text to convert
	 * @param separatorCharacters Characters to split with. Example string: ",.;|"
	 * @return A mutable list
	 */
	public static List<String> stringToList(String text, String separatorCharacters) {
		return text != null ? new ArrayList<>(Arrays.asList(text.split("\\s*[" + separatorCharacters + "]\\s*"))).stream()
				.filter(element -> element != null && !element.isEmpty())
				.map(str -> str.trim().replace(ARRAY_SEPARATOR_CHARACTER, ""))
				.collect(Collectors.toList()) : new ArrayList<>(); // Mutable
	}

	/**
	 * Same as {@link #stringToList(String, String)} but with a predefined separator character.
	 * IMPORTANT: Can only be used with JSON/JSON5 files! A custom separator must be used for other configuration files.
	 * @param text Text to convert
	 * @return A mutable list
	 */
	static List<String> stringToList(String text) {
		return stringToList(text, ARRAY_SEPARATOR_CHARACTER);
	}


}
