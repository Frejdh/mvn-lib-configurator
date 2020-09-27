package com.frejdh.util.environment;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Handles different environment variables set by different frameworks. Currently handles: <br>
 * - Java (environmental variables) <br>
 * - Spring-boot (application[-profile].properties) <br>
 * - Vertx (conf/config.json & conf/config.json5) <br>
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public class Config {
	private static final Logger LOGGER = Logger.getLogger(Config.class.getName());

	private static volatile boolean isInitialized = false;
	private static final java.util.Properties environmentVariables = new java.util.Properties(System.getProperties());
	private static final Set<String> filesToLoad = new HashSet<>();
	private static volatile boolean isRuntimeEnabled;

	static {
		setDefaultFilesToLoad();
		loadEnvironmentVariables(false);
		loadAdditionalConfigFiles();
	}

	/**
	 * Set the default files to load
	 */
	private static void setDefaultFilesToLoad() {
		// Spring
		String springProfile = System.getProperty("spring.profiles.active", "");
		filesToLoad.add("application" + (!springProfile.isEmpty() ? "-" + springProfile : "") + ".properties");

		// Vertx
		filesToLoad.add("conf/config.json");

		// Vertx, but JSON5 (not an official Vertx file, but I like the additional support of comments)
		filesToLoad.add("conf/config.json5");
	}

	private static void loadAdditionalConfigFiles() {
		List<String> additionalFilenames = ConfigParser.stringToList(get("config.sources", "", String.class, false), ",")
				.stream().filter(str -> str != null && !str.isEmpty()).map(String::trim).collect(Collectors.toList());

		Iterator<String> iter = additionalFilenames.iterator();
		while (iter.hasNext()) {
			String filename = iter.next();

			if (!loadVariablesFromFile(filename)) {
				LOGGER.log(Level.WARNING, "Couldn't load the configuration file '" + filename + "'.");
				iter.remove();
			}
		}

		filesToLoad.addAll(additionalFilenames);

	}

	public static void loadEnvironmentVariables(boolean force) {
		if (force || !isInitialized()) {
			synchronized (Config.class) {
				if (!force && isInitialized())
					return;

				loadVariablesFromFiles();
				loadVariablesFromProgram();
				isInitialized = true;
				setRuntimeEnabled(get("property.runtime.enabled", false, Boolean.class, false));
			}
		}
	}

	private static synchronized boolean isInitialized() {
		return isInitialized;
	}

	private static synchronized void setInitialized(boolean isInitialized) {
		Config.isInitialized = isInitialized;
	}

	public static synchronized boolean isRuntimeEnabled() {
		return isRuntimeEnabled;
	}

	private static synchronized void setRuntimeEnabled(boolean isRuntimeEnabled) {
		Config.isRuntimeEnabled = isRuntimeEnabled;
	}

	private static void loadVariablesFromFiles() {
		// Load files. Also remove the file from the list if it doesn't exist.
		filesToLoad.removeIf(filename -> !loadVariablesFromFile(filename));
	}

	private static boolean loadVariablesFromFile(String filename) {
		Map<String, String> properties = new HashMap<>();

		try {
			String fileContent = FileUtils.getResourceFile(filename);
			if (fileContent == null) { // If file doesn't exist, remove it
				return false;
			}

			if (filename.matches("(?i).*[.]json[5]*$")) { // Ends with .json or .json5 (case-insensitive)
				properties.putAll(ConfigParser.jsonToMap(fileContent, true));
			}
			else { // Not json, read values by the format 'variable=true' format, line per line
				properties.putAll(ConfigParser.textToMap(fileContent));
			}
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Couldn't load the file '" + filename + "'. Reason: " + e.getMessage());
		}
		properties.keySet().forEach(key -> environmentVariables.setProperty(key, properties.get(key)));

		return true;
	}

	private static void loadVariablesFromProgram() {
		for (String propertyName : System.getProperties().stringPropertyNames()) {
			environmentVariables.setProperty(propertyName, System.getProperty(propertyName));
		}
	}

	private static void waitForInitialization() {
		// Check that everything is initialized first!
		if (!isInitialized()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			waitForInitialization();
		}
	}

	// Internal. Set if the property should be fetched runtime or not.
	private static <T> T get(String name, Class<T> returnType, boolean isRuntimeEnabled) throws IllegalArgumentException {
		waitForInitialization();

		// Check if runtime configuration is enabled, refresh!
		if (isRuntimeEnabled) {
			loadEnvironmentVariables(true);
		}

		String stringValue = environmentVariables.getProperty(name);
		if (stringValue != null) {
			stringValue = stringValue.replaceAll("(^\")|(\"$)", "");
			return ConfigParser.convertToType(stringValue, returnType);
		}

		return null;
	}

	/**
	 * Get a property by a name and return type.
	 * @param name Name of the property
	 * @param returnType The class that the property shall be returned as
	 * @param <T> The supplied type
	 * @return The property or null if none was found
	 * @throws IllegalArgumentException If the class was not supported
	 */
	public static <T> T get(String name, Class<T> returnType) throws IllegalArgumentException {
		return get(name, returnType, Config.isRuntimeEnabled);
	}

	// Internal. Set if the property should be fetched runtime or not.
	private static <T> T get(String name, T defaultValue, Class<T> returnType, boolean isRuntimeEnabled) throws IllegalArgumentException {
		T result = get(name, returnType, isRuntimeEnabled);
		return result != null ? result : ConfigParser.convertToType(defaultValue.toString(), returnType);
	}

	/**
	 * Get a property by a name and return type.
	 * @param name Name of the property
	 * @param defaultValue The default value to return.
	 * @param returnType The class that the property shall be returned as
	 * @param <T> The supplied type
	 * @return The property or null if none was found
	 * @throws IllegalArgumentException If the class was not supported
	 */
	public static <T> T get(String name, T defaultValue, Class<T> returnType) throws IllegalArgumentException {
		return get(name, defaultValue, returnType, Config.isRuntimeEnabled);
	}

	public static String getPropertiesAsString() {
		return environmentVariables.entrySet().toString();
	}


	//
	// Property getters
	//
	/**
	 * Fetch a property with a given name as a String (works for all existing values).
	 * @param name Name of the property
	 * @return The instance or null if none was found
	 */
	public static String getString(String name) {
		return get(name, String.class);
	}

	/**
	 * Fetch a property with a given name as a String (works for all existing values).
	 * @param name Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value if none was found
	 */
	public static String getString(String name, @NotNull String defaultValue) {
		return get(name, defaultValue, String.class);
	}

	public static List<String> getStringList(String name) {
		return ConfigParser.stringToList(getString(name));
	}

	public static List<String> getStringList(String name, String separator) {
		return ConfigParser.stringToList(getString(name), separator);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @return The instance or null if none was found
	 */
	public static Boolean getBoolean(String name) {
		return get(name, Boolean.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static boolean getBoolean(String name, boolean defaultValue) {
		return get(name, defaultValue, Boolean.class);
	}


	public static List<Boolean> getBooleanList(String name) {
		return ConfigParser.stringToList(getString(name), Boolean.class);
	}

	public static List<Boolean> getBooleanList(String name, String separator) {
		return ConfigParser.stringToList(getString(name), separator, Boolean.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @return The instance or null if none was found
	 */
	public static Integer getInteger(String name) {
		return get(name, Integer.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static int getInteger(String name, int defaultValue) {
		return get(name, defaultValue, Integer.class);
	}

	public static List<Integer> getIntegerList(String name) {
		return ConfigParser.stringToList(getString(name), Integer.class);
	}

	public static List<Integer> getIntegerList(String name, String separator) {
		return ConfigParser.stringToList(getString(name), separator, Integer.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @return The instance or null if none was found
	 */
	public static Long getLong(String name) {
		return get(name, Long.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static long getLong(String name, long defaultValue) {
		return get(name, defaultValue, Long.class);
	}

	public static List<Long> getLongList(String name) {
		return ConfigParser.stringToList(getString(name), Long.class);
	}

	public static List<Long> getLongList(String name, String separator) {
		return ConfigParser.stringToList(getString(name), separator, Long.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @return The instance or null if none was found
	 */
	public static Double getDouble(String name) {
		return get(name, Double.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static double getDouble(String name, double defaultValue) {
		return get(name, defaultValue, Double.class);
	}

	public static List<Double> getDoubleList(String name) {
		return ConfigParser.stringToList(getString(name), Double.class);
	}

	public static List<Double> getDoubleList(String name, String separator) {
		return ConfigParser.stringToList(getString(name), separator, Double.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @return The instance or null if none was found
	 */
	public static Float getFloat(String name) {
		return get(name, Float.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static float getFloat(String name, float defaultValue) {
		return get(name, defaultValue, Float.class);
	}

	public static List<Float> getFloatList(String name) {
		return ConfigParser.stringToList(getString(name), Float.class);
	}

	public static List<Float> getFloatList(String name, String separator) {
		return ConfigParser.stringToList(getString(name), separator, Float.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @return The instance or null if none was found
	 */
	public static Character getCharacter(String name) {
		return get(name, Character.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static char getCharacter(String name, char defaultValue) {
		return get(name, defaultValue, Character.class);
	}

	public static List<Character> getCharacterList(String name) {
		return ConfigParser.stringToList(getString(name), Character.class);
	}

	public static List<Character> getCharacterList(String name, String separator) {
		return ConfigParser.stringToList(getString(name), separator, Character.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @return The instance or null if none was found
	 */
	public static Short getShort(String name) {
		return get(name, Short.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static short getShort(String name, short defaultValue) {
		return get(name, defaultValue, Short.class);
	}

	public static List<Short> getShortList(String name) {
		return ConfigParser.stringToList(getString(name), Short.class);
	}

	public static List<Short> getShortList(String name, String separator) {
		return ConfigParser.stringToList(getString(name), separator, Short.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @return The instance or null if none was found
	 */
	public static Byte getByte(String name) {
		return get(name, Byte.class);
	}

	/**
	 * Fetch a property with a given name.
	 * @param name Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static byte getByte(String name, byte defaultValue) {
		return get(name, defaultValue, Byte.class);
	}

	public static List<Byte> getByteList(String name) {
		return ConfigParser.stringToList(getString(name), Byte.class);
	}

	public static List<Byte> getByteList(String name, String separator) {
		return ConfigParser.stringToList(getString(name), separator, Byte.class);
	}
}
