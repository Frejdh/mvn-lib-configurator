package com.frejdh.util.environment;

import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * TODO: Not implemented yet
 * Handles different environment variables set by different frameworks. Currently handles:
 * - Java (stock)
 * - Spring / Spring-boot
 * - Vertx
 */
@SuppressWarnings("SameParameterValue")
public class PropertyConfigurer {

	private static volatile boolean isInitialized = false;
	private static final Properties environmentVariables = new Properties(System.getProperties());
	private static final Set<String> filesToLoad = new HashSet<>();

	static {
		setDefaultFilesToLoad();
		loadEnvironmentVariables(false);
	}

	/**
	 * Set the default files to load
	 */
	private static void setDefaultFilesToLoad() {
		String resourceDirectory = null;
		try {
			resourceDirectory = Paths.get(Objects.requireNonNull(PropertyConfigurer.class.getClassLoader().getResource("")).toURI()).toFile().getAbsolutePath();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		// Spring
		filesToLoad.add(resourceDirectory + "/application.properties");

		// Vertx
		filesToLoad.add(resourceDirectory + "/conf/config.json");

		// Vertx, but JSON5 (not an official Vertx file, but I like the support of comments that it provides)
		filesToLoad.add(resourceDirectory + "/conf/config.json5");

		filesToLoad.addAll(PropertyConfigurerHelper.stringToList(System.getProperty("property.sources", ""), ",")
				.stream().filter(str -> str != null && !str.isEmpty()).collect(Collectors.toList()));

	}

	public static void loadEnvironmentVariables(boolean force) {
		if (force || !isInitialized()) {
			synchronized (PropertyConfigurer.class) {
				if (!force && isInitialized())
					return;

				isInitialized = true;
				loadVariablesFromFiles();
				loadVariablesFromProgram();
			}
		}
	}



	private static synchronized boolean shouldLoadEnvVariables() {
		return !isInitialized;
	}

	static synchronized boolean isInitialized() {
		return isInitialized;
	}


	static synchronized void setInitialized(boolean isInitialized) {
		PropertyConfigurer.isInitialized = isInitialized;
	}

	private static void loadVariablesFromFiles() {
		Map<String, Object> properties = new HashMap<>();
		Iterator<String> iter = filesToLoad.iterator();

		while (iter.hasNext()) {
			String file = iter.next();
			try {
				if (!Files.exists(Paths.get(file))) { // If file doesn't exist, remove it
					iter.remove();
				}

				String fileContent = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);

				if (file.matches("(?i).*[.]json[5]*$")) { // Ends with .json or .json5 (case-insensitive)
					JsonReader jsonReader = new JsonReader(new StringReader(file.endsWith("5") ? PropertyConfigurerHelper.removeJsonComments(fileContent) : fileContent));
					jsonReader.setLenient(true); // Allow trailing commas, etc
					properties.putAll(PropertyConfigurerHelper.jsonToMap(new JsonParser().parse(jsonReader).getAsJsonObject()));
				}
				else { // Not json, read values by the format 'variable=true' format, line per line
					properties.putAll(PropertyConfigurerHelper.textToMap(fileContent));
				}
			} catch (IOException e) {
				Logger.getGlobal().log(Level.WARNING, "Couldn't load the file '" + file + "'. Reason: " + e.getMessage());
			}
		}

		properties.keySet().forEach(key -> {
			environmentVariables.setProperty(key, properties.get(key).toString());
		});
	}

	private static void loadVariablesFromProgram() {
		for (String propertyName : System.getProperties().stringPropertyNames()) {
			environmentVariables.setProperty(propertyName, System.getProperty(propertyName));
		}
	}

	public static String getProperty(String name, String defaultValue) {
		if (!isInitialized()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return getProperty(name, defaultValue);
		}

		return environmentVariables.getProperty(name, defaultValue).replaceAll("(^\")|(\"$)", "");
	}

	public static String getPropertiesAsString() {
		return environmentVariables.entrySet().toString();
	}

}
