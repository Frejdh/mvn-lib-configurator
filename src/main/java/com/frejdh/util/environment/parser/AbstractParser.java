package com.frejdh.util.environment.parser;

import java.io.IOException;
import java.util.Map;

public abstract class AbstractParser {
	protected static final String ARRAY_SEPARATOR_CHARACTER = "\u0001;\u0001";

	protected AbstractParser() { }

	public static AbstractParser getSingletonInstance() {
		return null;
	}

	public abstract Map<String, String> toMap(String content) throws IOException;
}
