package com.frejdh.util.environment.test;

import com.frejdh.util.environment.Config;
import org.junit.Assert;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

import static com.frejdh.util.environment.test.TestFileHelper.CleanupAction;
import static org.junit.Assert.*;

public class ParserTests extends AbstractTests {

	@Test
	public void errorHandlingWorks() {
		Assert.assertEquals(50, Config.getInteger("env.test1.test-of-env-int1", 10)); // Should be found
		assertEquals(50, Config.getInteger("property.does.not.exist", 50)); // Correct default value
		assertNull(Config.getInteger("property.does.not.exist"));
		Assert.assertThrows(IllegalArgumentException.class, () -> Config.get("env.test1.test-of-env-int1", ParserTests.class)); // Should throw
	}

	@Test
	public void runtimeWorks() throws Exception {
		String propertyKey = "runtime.works.test1";
		double propertyValue = 2.03;

		TestFileHelper.writeToExistingFile(FILE_RUNTIME, propertyKey + "=0", CleanupAction.EMPTY); // To remove the possibility of failure due to a previous run
		Thread.sleep(1000);
		assertEquals( "Initializing failed", 0, Config.getDouble(propertyKey, -1), 0);
		TestFileHelper.writeToExistingFile(FILE_RUNTIME, String.format("%s=%s", propertyKey, propertyValue), CleanupAction.EMPTY);
		Thread.sleep(1000);
		assertEquals("Value not updated", propertyValue, Config.getDouble(propertyKey, -1), 0); // Should be found
	}

	@Test
	public void springProfileWorks() throws Exception {
		final String propertiesFilename = "application-profile.properties";
		assertTrue(Config.getLoadedFiles().stream().noneMatch(file -> file.contains(propertiesFilename)));
		System.setProperty("spring.profiles.active", "profile, profile-without-file");
		restartConfigClass();

		final String propertyKey = "test.of.profile.property";
		final String expectedValue = "Hello profile!";
		assertEquals(expectedValue, Config.getString(propertyKey)); // Should be found
		assertTrue(Config.getLoadedFiles().stream().anyMatch(file -> file.contains(propertiesFilename)));
	}

	@Test
	public void additionalSourcesWorks() {
		String propertyValue = "works!";
		assertEquals(propertyValue, Config.getString("new.config.file1")); // Should be found
		assertEquals(propertyValue, Config.getString("new_config_file2")); // Should be found
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
