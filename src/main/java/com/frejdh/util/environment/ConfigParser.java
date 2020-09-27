package com.frejdh.util.environment;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class ConfigParser {

	private static final boolean JSON_ALLOW_COMMENTS_DEFAULT = false;
	private static final String ARRAY_SEPARATOR_CHARACTER = "\u0001;\u0001";
	private static final List<Class<?>> supportedPrimitiveClasses = initSupportedClasses();

	private static List<Class<?>> initSupportedClasses() {
		return new ArrayList<>(Arrays.asList(
				Boolean.class, Integer.class, Long.class, Double.class, Float.class, Character.class, Short.class, Byte.class
		)); // Must contain valueOf(String) method
	}

	/**
	 * Remaps nested objects for easier access. E.g. the field 'service': {'port': XXXX} will be accessible with 'service.port'
	 * @param jsonString JSON object to parse
	 * @param allowComments If the JSON file has comments, please set this to true.
	 */
	public static Map<String, String> jsonToMap(String jsonString, boolean allowComments) throws IOException {
		JsonReader jsonReader = new JsonReader(new StringReader(allowComments ? ConfigParser.removeJsonComments(jsonString) : jsonString));
		jsonReader.setLenient(true); // Allow trailing commas, etc

		return jsonToMap(new JsonParser().parse(jsonReader).getAsJsonObject());
	}

	/**
	 * Same as ${@link #jsonToMap(String, boolean)} with allowComments = ${@value JSON_ALLOW_COMMENTS_DEFAULT}
	 * @param jsonString JSON object to parse
	 */
	public static Map<String, String> jsonToMap(String jsonString) throws IOException {
		return jsonToMap(jsonString, JSON_ALLOW_COMMENTS_DEFAULT);
	}

	/**
	 * Remaps nested objects for easier access. E.g. the field 'service': {'port': XXXX} will be accessible with 'service.port'
	 * @param jsonObject JSON object to parse
	 */
	public static Map<String, String> jsonToMap(JsonObject jsonObject)  {
		return jsonToMapHelper(jsonObject, "", new HashMap<>());
	}

	private static Map<String, String> jsonToMapHelper(JsonElement jsonElement, String propertyPath, Map<String, String> mapToReturn) {
		if (jsonElement.isJsonObject()) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();

			for (String field : jsonObject.keySet()) {
				String appendedProperty = propertyPath + (propertyPath.isEmpty() ? "" : ".") + field;

				if (jsonObject.get(field).isJsonObject()) {
					jsonToMapHelper(jsonObject.getAsJsonObject(field), appendedProperty, mapToReturn);
				}
				else if (jsonObject.get(field).isJsonArray()){
					jsonToMapHelper(jsonObject.getAsJsonArray(field), appendedProperty, mapToReturn);
				}
				else {
					mapToReturn.put(appendedProperty, jsonObject.get(field).toString());
				}
			}
		}
		else if (jsonElement.isJsonArray()) { // Array!
			JsonArray jsonArray = jsonElement.getAsJsonArray();

			int i = 0;
			StringBuilder arrayValues = new StringBuilder();
			for (JsonElement element : jsonArray) {
				if (element.isJsonPrimitive()) {
					arrayValues.append(element).append(ARRAY_SEPARATOR_CHARACTER);
				}
				else {
					String appendedProperty = propertyPath + "[" + i + "]";
					jsonToMapHelper(element, appendedProperty, mapToReturn);
				}
				i++;
			}

			if (arrayValues.length() != 0) {
				mapToReturn.put(propertyPath, arrayValues.toString());
			}
		}
		return mapToReturn;
	}

	/**
	 * Remove comments from a JSON string. The parsable format of the comments is the styles that exists for Java.
	 * This parser is trailing comma lenient.
	 * @param jsonString String to inspect and remove comments from
	 * @return A new JSON string that is stripped of comments
	 * @throws IOException If the JSON couldn't be parsed
	 */
	public static String removeJsonComments(String jsonString) throws IOException {
		JsonMapper mapper = JsonMapper.builder()
				.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
				.enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
				.build();
		return mapper.writeValueAsString(mapper.readTree(jsonString));
	}

	/**
	 * Convert text in the format of <br>
	 * <code>
	 *     example.property.value1 = Hello 1 <br>
	 *     example.property.value2="Hello 2" <br>
	 * </code> <br>
	 * To a map where <br>
	 * <code>
	 *     Map[example.property.value1] = "Hello 1" <br>
	 *     Map[example.property.value2] = "Hello 2" <br>
	 * </code>
	 *
	 * @param textContent Text to convert to a Map, where each entry is separated by a new line
	 * @return A Map
	 */
	public static Map<String, String> textToMap(String textContent) {
		List<String> lines = ConfigParser.stringToList(textContent, "\n");

		return lines.stream()
				.filter(line -> !line.isEmpty() && !line.matches("\\s*#.*") && line.contains("=")) // Not empty, not comment & has variable assignment
				.collect(Collectors.toMap(
						line -> line.split("=")[0].trim(),
						line -> {
							line = line.split("=").length > 1 ? line.split("=")[1].trim() : "";
							if (line.matches("^\".*\"$")) { // If wrapped by quotes, remove them from the string
								line = line.replaceAll("(^\")|(\"$)", "");
							}
							return line;
						}
		));
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
	static List<String> stringToList(String text, String separatorCharacters) {
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
