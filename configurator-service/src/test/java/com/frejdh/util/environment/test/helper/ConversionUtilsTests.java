package com.frejdh.util.environment.test.helper;

import com.frejdh.util.environment.ConversionUtils;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConversionUtilsTests extends AbstractTests {

	@Test
	public void flattenMapWorks() {
		Map<Object, Object> genericMap = new HashMap<>();
		genericMap.put("strKey", "strValue");

		Map<Object, Object> nested1 = new HashMap<>();
		genericMap.put("nested1", nested1);

		Map<Object, Object> nested2 = new HashMap<>();
		nested2.put("value", 50);
		nested1.put("nested2", nested2);

		Map<String, Object> propertiesMap = ConversionUtils.flattenMap(genericMap);
		assertEquals("strValue", propertiesMap.get("strKey"));
		assertEquals(50, propertiesMap.get("nested1.nested2.value"));
	}

}
