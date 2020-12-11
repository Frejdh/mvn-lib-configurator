package com.frejdh.util.environment;

import org.junit.BeforeClass;


public abstract class AbstractTests {

	@BeforeClass
	public static void setLogger() {
		System.setProperty("handlers", "java.util.logging.ConsoleHandler");
		System.setProperty(".level", "ALL");
		System.setProperty("java.util.logging.ConsoleHandler.level", "ALL");
		System.setProperty("java.util.logging.ConsoleHandler.formatter", "java.util.logging.SimpleFormatter");
		System.setProperty("com.frejdh.util.environment.Config.level", "ALL");
		System.setProperty("com.frejdh.util.environment.Config.handler", "java.util.logging.ConsoleHandler");
	}

}
