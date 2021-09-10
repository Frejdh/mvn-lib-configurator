package com.frejdh.util.environment.test.helper;

import com.frejdh.util.environment.Config;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import static com.frejdh.util.environment.test.helper.AbstractTests.DYNAMIC_CLASS_PROPERTY_KEY;

@TestProperty(key = DYNAMIC_CLASS_PROPERTY_KEY, value = "child")
public class TestPropertyTests extends AbstractTests {

	private static final String EXAMPLE_KEY = "my-property.example";
	private static final String EXAMPLE_VALUE = "some value";

	@Rule
	public final TestPropertyRule configRule = new TestPropertyRule();

	@Test
	@TestProperty(key = EXAMPLE_KEY, value = EXAMPLE_VALUE)
	public void annotationWorksForMethod() {
		Assert.assertEquals(EXAMPLE_VALUE, Config.getString(EXAMPLE_KEY));
	}

	@Test
	public void annotationWorksForClass() {
		Assert.assertEquals("child", Config.getString(DYNAMIC_CLASS_PROPERTY_KEY));
	}

	@Test
	@TestProperty(key = DYNAMIC_CLASS_PROPERTY_KEY, value = "method")
	public void classAnnotationIsOverriddenByMethodAnnotation() {
		Assert.assertEquals("method", Config.getString(DYNAMIC_CLASS_PROPERTY_KEY));
	}

	@Test
	public void classAnnotationCanBeFetchedFromParent() {
		Assert.assertEquals(FIXED_CLASS_PROPERTY_VALUE, Config.getString(FIXED_CLASS_PROPERTY_KEY));
	}
}
