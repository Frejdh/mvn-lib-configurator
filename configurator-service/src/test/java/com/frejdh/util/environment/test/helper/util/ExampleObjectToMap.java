package com.frejdh.util.environment.test.helper.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

import static com.frejdh.util.environment.test.helper.AbstractTests.MAP_PROPERTY_WITH_DOT;

@NoArgsConstructor
@Getter
@Setter
public class ExampleObjectToMap {
	public static final String OBJECT_PROPERTY = "object-to-map";
	public static final String FULL_OBJECT_PROPERTY = MAP_PROPERTY_WITH_DOT + OBJECT_PROPERTY + ".";

	public static final String FIELD_NAME_SINGLE = "single";
	public static final String FULL_FIELD_PROPERTY_SINGLE = FULL_OBJECT_PROPERTY + FIELD_NAME_SINGLE;

	public static final String FIELD_NAME_LIST =  "list";
	public static final String FULL_FIELD_PROPERTY_LIST = FULL_OBJECT_PROPERTY + FIELD_NAME_LIST;

	public static final String FIELD_NAME_MAP = "map";
	public static final String FULL_FIELD_PROPERTY_MAP = FULL_OBJECT_PROPERTY + FIELD_NAME_MAP;

	private String single;
	private List<String> list;
	private Map<String, String> map;
}