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

public class JsonParser extends AbstractParser {
	private static final boolean JSON_ALLOW_COMMENTS_DEFAULT = true;
	private static AbstractParser singletonInstance;

	protected JsonParser() { }

	public static AbstractParser getSingletonInstance() {
		if (singletonInstance == null) {
			synchronized (JsonParser.class) { // Only lock if new instance
				if (singletonInstance == null) { // To avoid race condition
					singletonInstance = new JsonParser();
				}
			}
		}

		return singletonInstance;
	}

	/**
	 * Remaps nested objects for easier access. E.g. the field 'service': {'port': XXXX} will be accessible with 'service.port'
	 * @param jsonString JSON object to parse
	 * @param allowComments If the JSON file has comments, please set this to true.
	 */
	public Map<String, String> toMap(String jsonString, boolean allowComments) throws IOException {
		JsonReader jsonReader = new JsonReader(new StringReader(allowComments ? removeJsonComments(jsonString) : jsonString));
		jsonReader.setLenient(true); // Allow trailing commas, etc

		return toMap(new com.google.gson.JsonParser().parse(jsonReader).getAsJsonObject());
	}

	/**
	 * Same as ${@link #toMap(String, boolean)} with allowComments = ${@value JSON_ALLOW_COMMENTS_DEFAULT}
	 * @param jsonString JSON object to parse
	 */
	@Override
	public Map<String, String> toMap(String jsonString) throws IOException {
		return toMap(jsonString, JSON_ALLOW_COMMENTS_DEFAULT);
	}

	/**
	 * Remaps nested objects for easier access. E.g. the field 'service': {'port': XXXX} will be accessible with 'service.port'
	 * @param jsonObject JSON object to parse
	 */
	public Map<String, String> toMap(JsonObject jsonObject)  {
		return jsonToMapHelper(jsonObject, "", new HashMap<>());
	}

	private Map<String, String> jsonToMapHelper(JsonElement jsonElement, String propertyPath, Map<String, String> mapToReturn) {
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
					mapToReturn.put(appendedProperty.replace("_", "."), jsonObject.get(field).toString());
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
				mapToReturn.put(propertyPath.replace("_", "."), arrayValues.toString());
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
	public String removeJsonComments(String jsonString) throws IOException {
		JsonMapper mapper = JsonMapper.builder()
				.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
				.enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
				.build();
		return mapper.writeValueAsString(mapper.readTree(jsonString));
	}

}
