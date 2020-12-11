package com.frejdh.util.environment.parser;

import com.frejdh.util.environment.ConversionUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * For .properties files
 */
public class PropertiesParser extends AbstractParser {

	private static AbstractParser singletonInstance;

	protected PropertiesParser() { }

	public static AbstractParser getSingletonInstance() {
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
		List<String> lines = ConversionUtils.stringToList(textContent, "\n");

		return lines.stream()
				.filter(line -> !line.isEmpty() && !line.matches("\\s*#.*") && line.contains("=")) // Not empty, not comment & has variable assignment
				.collect(Collectors.toMap(
						line -> line.split("=")[0].trim().replace("_", "."),
						line -> {
							line = line.split("=").length > 1 ? line.split("=")[1].trim() : "";
							if (line.matches("^\".*\"$")) { // If wrapped by quotes, remove them from the string
								line = line.replaceAll("(^\")|(\"$)", "");
							}
							return line;
						}
				));
	}
}
