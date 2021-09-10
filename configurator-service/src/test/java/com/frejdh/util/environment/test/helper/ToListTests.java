package com.frejdh.util.environment.test.helper;

import static org.junit.Assert.assertEquals;

public class ToListTests extends AbstractTests {

//	@Test
//	public void arrayWorksForJson() {
//		String propertyKey = "env.test5.array";
//		List<Integer> expectedFullArray = Arrays.asList(50, 100, 150, 200);
//		assertEquals("Failed to get full array", expectedFullArray, Config.getIntegerList(propertyKey)); // Should be found
//		for (int i = 0; i < expectedFullArray.size(); i++) {
//			assertEquals("Failed to get element in array", expectedFullArray.get(i), Config.getInteger(propertyKey + "[" + i + "]"));
//		}
//	}
//
//	@Test
//	public void arrayWorksForProperties() {
//		String propertyKey = "simple.array-test";
//		List<String> expectedFullArray = Arrays.asList("Hello 0", "Hello 1");
//		assertEquals("Failed to get full array", expectedFullArray, Config.getStringList(propertyKey)); // Should be found
//
//		for (int i = 0; i < expectedFullArray.size(); i++) {
//			assertEquals("Failed to get element in array", expectedFullArray.get(i), Config.getString(propertyKey + "[" + i + "]"));
//		}
//	}
//
//	@Test
//	public void nestedArrayWorksForProperties() {
//		String propertyKey = "nested.array-test[%s].another[%s]";
//		int nrOfArrays = 3;
//		int nrOfElementsPerArray = 2;
//		String expectedValue = "Hi %s.%s";
//
//		for (int i = 0; i < nrOfArrays; i++) {
//			for (int j = 0; j < nrOfElementsPerArray; j++) {
//				String elementValue = Config.getString(String.format(propertyKey, i, j));
//				assertEquals("Values doesn't match", String.format(expectedValue, i, j), elementValue);
//			}
//		}
//	}
//
//	@Test
//	public void arrayWorksForYaml() {
//		List<String> propertyKeys = Arrays.asList("simple.array-test.yml.variant1", "simple.array-test.yml.variant2", "simple.array-test.yml.variant3");
//		List<String> expectedFullArray = Arrays.asList("Hello 0", "Hello 1", "Hello 2", "Hello 3");
//
//		for (String propertyKey : propertyKeys) {
//			assertEquals("Failed to get full array for " + propertyKey, expectedFullArray, Config.getStringList(propertyKey)); // Should be found
//
//			for (int i = 0; i < expectedFullArray.size(); i++) {
//				String keyWithIndex = propertyKey + "[" + i + "]";
//				assertEquals("Failed to get element in array for " + keyWithIndex, expectedFullArray.get(i), Config.getString(keyWithIndex));
//			}
//		}
//	}

}
