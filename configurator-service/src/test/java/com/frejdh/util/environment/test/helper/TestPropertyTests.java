package com.frejdh.util.environment.test.helper;

import com.frejdh.util.environment.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.frejdh.util.environment.test.helper.AbstractTests.DYNAMIC_CLASS_PROPERTY_KEY;

@TestProperty(key = DYNAMIC_CLASS_PROPERTY_KEY, value = "child")
public class TestPropertyTests extends AbstractTests {

	private static final String EXAMPLE_KEY = "my-property.example";
	private static final String EXAMPLE_VALUE = "some value";

	@Test
	@TestProperty(key = EXAMPLE_KEY, value = EXAMPLE_VALUE)
	public void annotationWorksForMethod() {
		Assertions.assertEquals(EXAMPLE_VALUE, Config.getString(EXAMPLE_KEY));
	}

	@Test
	public void annotationWorksForClass() {
		Assertions.assertEquals("child", Config.getString(DYNAMIC_CLASS_PROPERTY_KEY));
	}

	@Test
	@TestProperty(key = DYNAMIC_CLASS_PROPERTY_KEY, value = "method")
	public void classAnnotationIsOverriddenByMethodAnnotation() {
		Assertions.assertEquals("method", Config.getString(DYNAMIC_CLASS_PROPERTY_KEY));
	}

	@Test
	public void classAnnotationCanBeFetchedFromParent() {
		Assertions.assertEquals(FIXED_CLASS_PROPERTY_VALUE, Config.getString(FIXED_CLASS_PROPERTY_KEY));
	}
}
