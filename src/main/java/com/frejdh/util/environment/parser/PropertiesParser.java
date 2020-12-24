package com.frejdh.util.environment.parser;

import com.frejdh.util.environment.ConversionUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * For .properties files
 */
public class PropertiesParser extends AbstractParser {

	// Found array entries are saved as capture groups.
	// For example: asd[0].otherArray[0]="blabla" has the capture groups 'asd[0]' and 'otherArray[0]'.
	private static final Pattern ARRAY_PATTERN = Pattern.compile("^(.+?\\[\\d+])(?:\\.(.+?\\[\\d+]))*\\s*=");

	private static PropertiesParser singletonInstance;

	protected PropertiesParser() { }

	public static PropertiesParser getSingletonInstance() {
		if (singletonInstance == null) {
			synchronized (PropertiesParser.class) { // Only lock if new instance
				if (singletonInstance == null) { // To avoid race condition
					singletonInstance = new PropertiesParser();
				}
			}
		}

		return singletonInstance;
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
	public Map<String, String> toMap(String textContent) throws IOException {
		List<String> lines = ConversionUtils.stringToList(textContent, "\n")
				.stream()
				.filter(line -> !line.isEmpty() && !line.matches("\\s*#.*") && line.contains("=")) // Not empty, not comment and has variable assignment
				.collect(Collectors.toList());

		Map<String, String> mapToReturn = new HashMap<>();
		for (String line : lines) {
			String fullPath = line.split("\\s*=")[0];
			String value = line.substring(line.lastIndexOf('=') + 1);

			final Matcher arrayMatcher = ARRAY_PATTERN.matcher(line);
			if (arrayMatcher.find()) { // If array
				mapToReturn.put(fullPath, value);
				String arrayFieldWithoutIndex = fullPath.replaceAll("\\[\\d+].*?\\s*$", "");
				String existingArrayString =  mapToReturn.getOrDefault(arrayFieldWithoutIndex, "");
				mapToReturn.put(arrayFieldWithoutIndex, (!existingArrayString.isEmpty() ? existingArrayString + ARRAY_SEPARATOR_CHARACTER : "") + value);
			}
			else {
				mapToReturn.put(fullPath, value);
			}
		}

		return mapToReturn;
	}
}
