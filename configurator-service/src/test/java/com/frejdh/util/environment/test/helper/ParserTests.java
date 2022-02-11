package com.frejdh.util.environment.test.helper;

import com.frejdh.util.environment.Config;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ParserTests extends AbstractTests {

	@Test
	public void errorHandlingWorks() {
		assertEquals(50, Config.getInteger("env.test1.test-of-env-int1", 10)); // Should be found
		assertEquals(50, Config.getInteger("property.does.not.exist", 50)); // Correct default value
		assertNull(Config.getInteger("property.does.not.exist"));
		assertThrows(IllegalArgumentException.class, () -> Config.get("env.test1.test-of-env-int1", ParserTests.class)); // Should throw
	}

	@Test
	public void runtimeWorks() throws Exception {
		String propertyKey = "runtime.works.test1";
		double propertyValue = 2.03;

		TestFileHelper.writeToExistingFile(FILE_RUNTIME, propertyKey + "=0", TestFileHelper.CleanupAction.EMPTY); // To remove the possibility of failure due to a previous run
		Thread.sleep(1000);
		assertEquals(0, Config.getDouble(propertyKey, -1), 0, "Initializing failed");
		TestFileHelper.writeToExistingFile(FILE_RUNTIME, String.format("%s=%s", propertyKey, propertyValue), TestFileHelper.CleanupAction.EMPTY);
		Thread.sleep(1000);
		assertEquals(propertyValue, Config.getDouble(propertyKey, -1), 0, "Value not updated"); // Should be found
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

	@Test
	public void yamlPropertiesPickedUp() throws Exception {
		final String propertiesFilename = "application.yml";
		assertTrue(Config.getLoadedFiles().stream().anyMatch(file -> file.contains(propertiesFilename)));
		restartConfigClass();

		final String propertyKey = "env.test5.yaml";
		final String expectedValue = "yes!";
		assertEquals(expectedValue, Config.getString(propertyKey)); // Should be found
		assertTrue(Config.getLoadedFiles().stream().anyMatch(file -> file.contains(propertiesFilename)));
	}

}
