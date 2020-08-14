package com.frejdh.util.environment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frejdh.util.environment.annotation.PropertyValue;
import org.junit.Test;
import java.util.logging.Logger;

public class PropertyTests {

	private Logger logger = Logger.getLogger("PropertyTests");

	@PropertyValue("env.test3.normal-comment")
	private String test1;

	@PropertyValue("env.test2.test-of-env-str1")
	private ObjectMapper test3;

	@Test
	public void happy() {
		logger.info("test1: " + test1);
	}
}
