package com.frejdh.util.environment.parser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class JsonParser {
	private static final boolean JSON_ALLOW_COMMENTS_DEFAULT = false;
	private static final String ARRAY_SEPARATOR_CHARACTER = "\u0001;\u0001";

	/**
	 * Remaps nested objects for easier access. E.g. the field 'service': {'port': XXXX} will be accessible with 'service.port'
	 * @param jsonString JSON object to parse
	 * @param allowComments If the JSON file has comments, please set this to true.
	 */
	public static Map<String, String> jsonToMap(String jsonString, boolean allowComments) throws IOException {
		JsonReader jsonReader = new JsonReader(new StringReader(allowComments ? JsonParser.removeJsonComments(jsonString) : jsonString));
		jsonReader.setLenient(true); // Allow trailing commas, etc

		return jsonToMap(new com.google.gson.JsonParser().parse(jsonReader).getAsJsonObject());
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

}
