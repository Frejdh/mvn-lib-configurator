package com.frejdh.util.environment.parser;

import java.io.IOException;
import java.util.Map;

/**
 * For .properties files
 */
public class YamlParser extends AbstractParser {

	private static AbstractParser singletonInstance;

	protected YamlParser() { }

	public static AbstractParser getSingletonInstance() {
		if (singletonInstance == null) {
			synchronized (YamlParser.class) { // Only lock if new instance
				if (singletonInstance == null) { // To avoid race condition
					singletonInstance = new YamlParser();
				}
			}
		}

		return singletonInstance;
	}

	@Override
	public Map<String, String> toMap(String content) throws IOException {
		return null;
	}
}
