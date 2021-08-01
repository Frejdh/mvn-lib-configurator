package com.frejdh.util.environment;

import com.frejdh.util.environment.parser.AbstractParser;
import com.frejdh.util.environment.parser.JsonParser;
import com.frejdh.util.environment.parser.PropertiesParser;
import com.frejdh.util.environment.parser.YamlParser;
import java.util.Arrays;

public class ParserSelector {
	public enum FileExtension {
		PROPERTIES("properties"),
		YAML("yml", "yaml"),
		JSON("json", "json5");

		private final String[] extensions;
		FileExtension(String... extensions) {
			this.extensions = extensions;
		}

		public static FileExtension toExtension(String extension) {
			return Arrays.stream(FileExtension.values())
					.filter(extensionEnum -> Arrays.stream(extensionEnum.extensions)
							.anyMatch(extensionString -> extensionString.equalsIgnoreCase(extension))
					).findFirst().orElse(null);
		}
	}

	public static AbstractParser getParser(String filenameOrExtension) throws UnsupportedOperationException {
		if (filenameOrExtension == null) {
			throw new UnsupportedOperationException("The string '" + filenameOrExtension + "' had no supported parser");
		}
		return getParser(FileExtension.toExtension(filenameOrExtension.contains(".") ? filenameOrExtension.substring(filenameOrExtension.lastIndexOf('.') + 1) : filenameOrExtension));
	}

	public static AbstractParser getParser(FileExtension fileExtension) throws UnsupportedOperationException {
		AbstractParser parser = null;
		switch (fileExtension) {
			case PROPERTIES:
				parser = PropertiesParser.getSingletonInstance();
				break;
			case YAML:
				parser = YamlParser.getSingletonInstance();
				break;
			case JSON:
				parser = JsonParser.getSingletonInstance();
				break;
		}

		if (parser == null) {
			throw new UnsupportedOperationException("The FileExtension '" + fileExtension + "' has no supported parser");
		}

		return parser;
	}

}
