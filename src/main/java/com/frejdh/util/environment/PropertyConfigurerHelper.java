package com.frejdh.util.environment;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class PropertyConfigurerHelper {

	/**
	 * Remaps nested objects for easier access. E.g. the field 'service': {'port': XXXX} will be accessible with 'service.port'
	 */
	static Map<String, Object> jsonToMap(JsonObject jsonObject) {
		Map<String, Object> remappedSets = new HashMap<>();
		jsonToMap(jsonObject, "", remappedSets);
		return remappedSets;
	}

	private static void jsonToMap(JsonObject jsonObject, String propertyPath, Map<String, Object> mapToReturn) {
		for (String field : jsonObject.keySet()) {
			String appendedProperty = propertyPath + (propertyPath.isEmpty() ? "" : ".") + field;
			try {
				jsonToMap(jsonObject.getAsJsonObject(field), appendedProperty, mapToReturn);
			} catch (ClassCastException ignored) {
				mapToReturn.put(appendedProperty, jsonObject.get(field));
			}
		}
	}

	static String removeJsonComments(String jsonString) throws IOException {
		JsonMapper mapper = JsonMapper.builder()
				.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
				.enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
				.build();
		return mapper.writeValueAsString(mapper.readTree(jsonString));
	}

	static Map<String, Object> textToMap(String textContent) {
		List<String> lines = PropertyConfigurerHelper.stringToList(textContent, "\n");

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

	static List<String> stringToList(String text, String separatorCharacters) {
		return new ArrayList<>(Arrays.asList(text.split("\\s*[" + separatorCharacters + "]\\s*"))); // Mutable
	}
}
