package com.frejdh.util.environment;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

}
