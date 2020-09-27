package com.frejdh.util.environment;

import org.junit.Assert;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// TODO: Do actual tests...
public class PropertyTests {

	private void setRuntimeEnabled(boolean value) {
		System.setProperty("property.runtime.enabled", Boolean.toString(value));
		Config.loadEnvironmentVariables(true);
	}

	@Test
	public void happy() {
		assertEquals(50, Config.getInteger("env.test1.test-of-env-int1", 10)); // Should be found
		assertEquals(50, Config.getInteger("property.does.not.exist", 50)); // Correct default value
		assertNull(Config.getInteger("property.does.not.exist"));
		Assert.assertThrows(IllegalArgumentException.class, () -> Config.get("env.test1.test-of-env-int1", PropertyTests.class)); // Should throw

		System.out.println(Config.getPropertiesAsString());
	}

	@Test
	public void runtimeWorks() {
		setRuntimeEnabled(true);
		String propertyKey = "runtime.works.test";
		double propertyValue = 2.09;

		assertNull(Config.getDouble(propertyKey));
		System.setProperty(propertyKey, Double.toString(propertyValue));
		assertEquals(propertyValue, Config.getDouble(propertyKey, 10), 0); // Should be found
		setRuntimeEnabled(false);
	}

	/* @Test
	public void springProfileWorks() {
		/*String propertyKey = "runtime.works.test";
		double propertyValue = 2.09;

		assertNull(Config.getDouble(propertyKey));
		System.setProperty(propertyKey, Double.toString(propertyValue));
		assertEquals(propertyValue, Config.getDouble(propertyKey, 10), 0); // Should be found
	}*/

	@Test
	public void additionalSourcesWorks() {
		String propertyValue = "works!";
		assertEquals(propertyValue, Config.getString("new.config.file1")); // Should be found
		assertEquals(propertyValue, Config.getString("new.config.file2")); // Should be found
	}

	@Test
	public void arrayPropertiesWork() {
		String propertyKey = "env.test5.array";
		List<Integer> expected = Arrays.asList(50, 100, 150, 200);
		assertEquals(expected, Config.getIntegerList(propertyKey)); // Should be found
	}
}
