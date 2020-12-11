package com.frejdh.util.environment;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.frejdh.util.environment.FileHelper.CleanupAction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class PropertyTests extends AbstractTests {

	public static final String FILE_RUNTIME = "runtime.properties";

	@After
	public void cleanup() throws Exception {
		FileHelper.cleanup();
	}

	@Test
	public void happy() {
		assertEquals(50, Config.getInteger("env.test1.test-of-env-int1", 10)); // Should be found
		assertEquals(50, Config.getInteger("property.does.not.exist", 50)); // Correct default value
		assertNull(Config.getInteger("property.does.not.exist"));
		Assert.assertThrows(IllegalArgumentException.class, () -> Config.get("env.test1.test-of-env-int1", PropertyTests.class)); // Should throw

		System.out.println(Config.getPropertiesAsString());
		System.out.println(Config.getLoadedFiles());
		System.out.println(Config.getPropertiesAsMap());
	}

	@Test
	public void runtimeWorks() throws Exception {
		String propertyKey = "runtime.works.test1";
		double propertyValue = 2.03;

		Config.getDouble(propertyKey);
		assertNull(Config.getDouble(propertyKey));
		FileHelper.writeToExistingFile(FILE_RUNTIME, String.format("%s=%s", propertyKey, propertyValue), CleanupAction.EMPTY);
		Thread.sleep(2000);
		assertEquals(propertyValue, Config.getDouble(propertyKey, -1), 0); // Should be found
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
		assertEquals(propertyValue, Config.getString("new_config_file2")); // Should be found
	}

	@Test
	public void arrayPropertiesWork() {
		String propertyKey = "env.test5.array";
		List<Integer> expected = Arrays.asList(50, 100, 150, 200);
		assertEquals(expected, Config.getIntegerList(propertyKey)); // Should be found
	}
}
