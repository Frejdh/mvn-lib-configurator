package com.frejdh.util.environment;

import org.junit.After;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import java.util.Arrays;
import java.util.List;
import static com.frejdh.util.environment.FileHelper.CleanupAction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// TODO: More tests, divide into multiple files
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PropertyParserTests extends AbstractParserTests {

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
		Assert.assertThrows(IllegalArgumentException.class, () -> Config.get("env.test1.test-of-env-int1", PropertyParserTests.class)); // Should throw
	}

	@Test
	public void runtimeWorks() throws Exception {
		String propertyKey = "runtime.works.test1";
		double propertyValue = 2.03;

		FileHelper.writeToExistingFile(FILE_RUNTIME, propertyKey + "=0", CleanupAction.EMPTY); // To remove the possibility of failure due to a previous run
		Thread.sleep(1000);
		assertEquals( "Initializing failed", 0, Config.getDouble(propertyKey, -1), 0);
		FileHelper.writeToExistingFile(FILE_RUNTIME, String.format("%s=%s", propertyKey, propertyValue), CleanupAction.EMPTY);
		Thread.sleep(1000);
		assertEquals("Value not updated", propertyValue, Config.getDouble(propertyKey, -1), 0); // Should be found
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
	public void arrayWorksForJson() {
		String propertyKey = "env.test5.array";
		List<Integer> expectedFullArray = Arrays.asList(50, 100, 150, 200);
		assertEquals("Failed to get full array", expectedFullArray, Config.getIntegerList(propertyKey)); // Should be found

		for (int i = 0; i < expectedFullArray.size(); i++) {
			assertEquals("Failed to get element in array", expectedFullArray.get(i), Config.getInteger(propertyKey + "[" + i + "]"));
		}
	}

	@Test
	public void arrayWorksForProperties() {
		String propertyKey = "simple.array-test";
		List<String> expectedFullArray = Arrays.asList("Hello 0", "Hello 1");
		assertEquals("Failed to get full array", expectedFullArray, Config.getStringList(propertyKey)); // Should be found

		for (int i = 0; i < expectedFullArray.size(); i++) {
			assertEquals("Failed to get element in array", expectedFullArray.get(i), Config.getString(propertyKey + "[" + i + "]"));
		}
	}

	@Test
	public void arrayWorksForYaml() {
		List<String> propertyKeys = Arrays.asList("simple.array-test.yml.variant1", "simple.array-test.yml.variant2", "simple.array-test.yml.variant3");
		List<String> expectedFullArray = Arrays.asList("Hello 0", "Hello 1", "Hello 2", "Hello 3");

		for (String propertyKey : propertyKeys) {
			assertEquals("Failed to get full array for " + propertyKey, expectedFullArray, Config.getStringList(propertyKey)); // Should be found

			for (int i = 0; i < expectedFullArray.size(); i++) {
				String keyWithIndex = propertyKey + "[" + i + "]";
				assertEquals("Failed to get element in array for " + keyWithIndex, expectedFullArray.get(i), Config.getString(keyWithIndex));
			}
		}
	}

	@Test
	public void canHandleUppercaseLettersAndDashes() {
		List<String> propertyKeys = Arrays.asList("formatting.test.uppercase-usage-works", "formatting.test.uppercaseUsageWorks");
		String expectedValue = "It works!";

		for (String propertyKey : propertyKeys) {
			assertEquals(expectedValue, Config.getString(propertyKey)); // Should be found
		}
	}
}
