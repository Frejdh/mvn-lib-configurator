package com.frejdh.util.environment.parser;

import com.frejdh.util.environment.storage.map.LinkedPathMultiMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class AbstractParser {
	public static final Pattern ARRAY_PATTERN_FOR_KEY = Pattern.compile(".+\\[\\d+]");
	public static final Pattern ARRAY_PATTERN_FOR_LINE = Pattern.compile("^(.+?\\[\\d+])(?:\\.(.+?\\[\\d+]))*\\s*=");

	protected LinkedPathMultiMap<String> multiMap = new LinkedPathMultiMap<>();

	protected AbstractParser() { }

	public static AbstractParser getSingletonInstance() {
		return null;
	}

	public abstract Map<String, List<String>> toMultiMap(String content) throws IOException;

}
