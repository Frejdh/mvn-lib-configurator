package com.frejdh.util.environment.test.helper;

import com.frejdh.util.environment.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Method;
import java.util.logging.Logger;

@ExtendWith(TestPropertyExtension.class)
@TestProperty(key = "class-property-dynamic", value = "parent")
@TestProperty(key = "class-property-fixed", value = "absolute")
public abstract class AbstractTests {

	public static final String FILE_RUNTIME = "runtime.properties";
	public static final String DYNAMIC_CLASS_PROPERTY_KEY = "class-property-dynamic";
	public static final String FIXED_CLASS_PROPERTY_KEY = "class-property-fixed";
	public static final String FIXED_CLASS_PROPERTY_VALUE = "absolute";

	public static final String MAP_PROPERTY = "some.map";
	public static final String MAP_PROPERTY_WITH_DOT = MAP_PROPERTY + ".";

	protected void restartConfigClass() throws Exception {
		Method method = Config.class.getDeclaredMethod("init");
		method.setAccessible(true);
		method.invoke(null);
		method.setAccessible(false);
	}

	@AfterEach
	public void cleanup() throws Exception {
		TestFileHelper.cleanup();
	}

	@BeforeAll
	public static void initLoggerAndPrintInfo() {
		System.setProperty("handlers", "java.util.logging.ConsoleHandler");
		System.setProperty(".level", "ALL");
		System.setProperty("java.util.logging.ConsoleHandler.level", "ALL");
		System.setProperty("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
		System.setProperty("com.frejdh.util.environment.Config.level", "ALL");
		System.setProperty("com.frejdh.util.environment.Config.handler", "java.util.logging.ConsoleHandler");

		Logger.getGlobal().info("Class {" + AbstractTests.class.getSimpleName() + "} loaded files: " + Config.getLoadedFiles());
	}

}
