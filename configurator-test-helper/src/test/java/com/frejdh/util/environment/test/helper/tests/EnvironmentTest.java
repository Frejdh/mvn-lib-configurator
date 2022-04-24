package com.frejdh.util.environment.test.helper.tests;

import com.frejdh.util.environment.test.helper.TestPropertyExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Collectors;

@ExtendWith(TestPropertyExtension.class)
public class EnvironmentTest {

	private static final String OVERWRITE_CONFIG_FILE_PATH = "-DvariableOverwritesConfigFile";
	private static final String OVERWRITTEN_BY_PROPERTY_PATH = "-DvariableIsOverwrittenByCommandLine";

	@Test
	public void envVariableCanOverwriteConfigFile() {
		// TODO: Implement!
		System.out.println(System.getenv().entrySet().stream().filter(
				env -> env.getKey().contains("variable")
		).collect(Collectors.toList()));
	}

}
