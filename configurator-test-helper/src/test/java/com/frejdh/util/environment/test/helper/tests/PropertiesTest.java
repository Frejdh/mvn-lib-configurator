package com.frejdh.util.environment.test.helper.tests;

import com.frejdh.util.environment.test.helper.TestProperties;
import com.frejdh.util.environment.test.helper.TestProperty;
import com.frejdh.util.environment.test.helper.TestPropertyExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(TestPropertyExtension.class)
public class PropertiesTest {

	private static final String PROPERTY_PATH = "test.property1";
	private static final String ADDITIONAL_PROPERTY_PATH = "test.property2";

	@AfterEach
	public void assertPropertiesRestoredAfterTests() {
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

	@ExtendWith(TestPropertyExtension.class)
	@TestProperty(key = ADDITIONAL_PROPERTY_PATH, value = "annotatedClass")
	public static class ClassWithPropertyAnnotationTest {

		@Test
		public void classesCanBeAnnotated() {
			assertEquals("annotatedClass", System.getProperty(ADDITIONAL_PROPERTY_PATH));
		}

		@AfterAll
		public static void checkPropertiesRestoredAfterTests() {
			assertNull(System.getProperty(PROPERTY_PATH));
			assertNull(System.getProperty(ADDITIONAL_PROPERTY_PATH));
		}
	}

}
