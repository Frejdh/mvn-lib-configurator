package com.frejdh.util.environment.test.helper.tests;

import com.frejdh.util.environment.test.helper.TestProperties;
import com.frejdh.util.environment.test.helper.TestProperty;
import com.frejdh.util.environment.test.helper.TestPropertyRule;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;

public class PropertiesTest {

	private static final String PROPERTY_PATH = "test.property1";
	private static final String ADDITIONAL_PROPERTY_PATH = "test.property2";

	@Rule
	public final TestPropertyRule configRule = new TestPropertyRule();

	@AfterClass
	public static void checkPropertiesRestoredAfterTests() {
		assertNull(System.getProperty(PROPERTY_PATH));
		assertNull(System.getProperty(ADDITIONAL_PROPERTY_PATH));
	}

	@Test
	@TestProperty(key = PROPERTY_PATH, value = "otherValue1")
	@TestProperty(key = ADDITIONAL_PROPERTY_PATH, value = "otherValue2")
	public void ensureThatMultipleTestPropertyAnnotationsWork() {
		assertEquals("otherValue1", System.getProperty(PROPERTY_PATH));
		assertEquals("otherValue2", System.getProperty(ADDITIONAL_PROPERTY_PATH));
	}

	@Test
	@TestProperties({
			@TestProperty(key = PROPERTY_PATH, value = "otherValue3"),
			@TestProperty(key = ADDITIONAL_PROPERTY_PATH, value = "otherValue4")
	})
	public void ensureThatTestPropertiesAnnotationWorks() {
		assertEquals("otherValue3", System.getProperty(PROPERTY_PATH));
		assertEquals("otherValue4", System.getProperty(ADDITIONAL_PROPERTY_PATH));
	}


	@TestProperty(key = ADDITIONAL_PROPERTY_PATH, value = "annotatedClass")
	public static class ClassWithPropertyAnnotation {

		@Rule
		public final TestPropertyRule configRule = new TestPropertyRule();

		@Test
		public void classesCanBeAnnotated() {
			assertEquals("annotatedClass", System.getProperty(ADDITIONAL_PROPERTY_PATH));
		}

		@AfterClass
		public static void checkPropertiesRestoredAfterTests() {
			assertNull(System.getProperty(PROPERTY_PATH));
			assertNull(System.getProperty(ADDITIONAL_PROPERTY_PATH));
		}
	}

}
