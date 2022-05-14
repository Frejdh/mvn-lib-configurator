package com.frejdh.util.environment.test.helper;

import com.frejdh.util.environment.Config;
import com.frejdh.util.environment.test.helper.util.ExampleObjectToMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ToMapTests extends AbstractTests {

	private static final String SIMPLE_PROPERTY = "property";
	private static final String NESTED_PROPERTY = "property.nested1.nested2";


	@Test
	@TestProperty(key = "unrelated.property", value = "should be ignored!")
	@TestProperty(key = MAP_PROPERTY_WITH_DOT + SIMPLE_PROPERTY, value = "val1")
	@TestProperty(key = MAP_PROPERTY_WITH_DOT + NESTED_PROPERTY, value = "0")
	@TestProperty(key = MAP_PROPERTY_WITH_DOT + NESTED_PROPERTY, value = "1")
	public void toGenericMultiMapWorks() {
		Map<String, List<Object>> mapFromConfig = Config.getMultiMap(MAP_PROPERTY);

		assertNotNull(mapFromConfig);
		assertEquals(3, mapFromConfig.size());

		assertEquals(1, mapFromConfig.get(SIMPLE_PROPERTY).size());
		assertEquals("val1", mapFromConfig.get(SIMPLE_PROPERTY).get(0));

		assertNull(mapFromConfig.get(MAP_PROPERTY + "property.nested1"));

		assertEquals(2, mapFromConfig.get(NESTED_PROPERTY).size());
		assertEquals("0", mapFromConfig.get(NESTED_PROPERTY).get(0));
		assertEquals("1", mapFromConfig.get(NESTED_PROPERTY).get(1));
	}

	@Test
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_SINGLE, value = "single")
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_LIST, value = "list0")
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_LIST, value = "list1")
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_MAP + ".more.properties", value = "yes!")
	@SuppressWarnings("unchecked")
	public void toObjectMultiMapWorks() {
		Class<ExampleObjectToMap> classToConvertTo = ExampleObjectToMap.class;
		Map<String, List<ExampleObjectToMap>> mapFromConfig = Config.getMultiMap(MAP_PROPERTY, classToConvertTo);

		assertNotNull(mapFromConfig);

		assertNotNull(mapFromConfig.get(ExampleObjectToMap.OBJECT_PROPERTY));
		ExampleObjectToMap mappedObject = mapFromConfig.get(ExampleObjectToMap.OBJECT_PROPERTY).get(0);

		//assertEquals(mappedObject.getMap());
		assertEquals("single", mappedObject.getSingle());
		assertEquals(Arrays.asList("list0", "list1"), mappedObject.getList());

		Map<String, Object> nestedMoreMap = (Map<String, Object>) mappedObject.getMap().get("more");
		assertNotNull(nestedMoreMap);
		String propertiesValue = (String) nestedMoreMap.get("properties");
		assertEquals("yes!", propertiesValue);
	}

	@Test
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_SINGLE, value = "single")
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_LIST, value = "list0")
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_LIST, value = "list1")
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_MAP + ".more.properties", value = "yes!")
	@SuppressWarnings("unchecked")
	public void toObjectSimpleMapWorks() {
		Class<ExampleObjectToMap> classToConvertTo = ExampleObjectToMap.class;
		Map<String, ExampleObjectToMap> mapFromConfig = Config.getSimpleMap(MAP_PROPERTY, classToConvertTo);

		assertNotNull(mapFromConfig);

		assertNotNull(mapFromConfig.get(ExampleObjectToMap.OBJECT_PROPERTY));
		ExampleObjectToMap mappedObject = mapFromConfig.get(ExampleObjectToMap.OBJECT_PROPERTY);

		assertEquals("single", mappedObject.getSingle());
		assertEquals(Arrays.asList("list0", "list1"), mappedObject.getList());

		Map<String, Object> nestedMoreMap = (Map<String, Object>) mappedObject.getMap().get("more");
		assertNotNull(nestedMoreMap);
		String propertiesValue = (String) nestedMoreMap.get("properties");
		assertEquals("yes!", propertiesValue);
	}

	@Test
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_SINGLE, value = "single")
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_LIST, value = "list0")
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_LIST, value = "list1")
	@TestProperty(key = ExampleObjectToMap.FULL_FIELD_PROPERTY_MAP + ".more.properties", value = "yes!")
	public void toFlattenedMultiMapWorks() {
		Map<String, List<Object>> mapFromConfig = Config.getFlattenedPathMultiMap(ExampleObjectToMap.FULL_OBJECT_PROPERTY);

		assertNotNull(mapFromConfig);
		assertEquals(Collections.singletonList("single"), mapFromConfig.get(ExampleObjectToMap.FIELD_NAME_SINGLE));
		assertEquals(Arrays.asList("list0", "list1"), mapFromConfig.get(ExampleObjectToMap.FIELD_NAME_LIST));
		assertEquals(Collections.singletonList("yes!"), mapFromConfig.get(ExampleObjectToMap.FIELD_NAME_MAP + ".more.properties"));
	}

}
