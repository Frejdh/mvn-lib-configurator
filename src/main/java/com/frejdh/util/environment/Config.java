package com.frejdh.util.environment;

import com.frejdh.util.environment.storage.PropertiesWrapper;
import com.frejdh.util.watcher.StorageWatcher;
import com.frejdh.util.watcher.StorageWatcherBuilder;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Handles different environment variables set by different frameworks. Currently handles: <br>
 * - Java (environmental variables) <br>
 * - Spring-boot (application[-profile].properties|yml) <br>
 * - Vertx (conf/config.json & conf/config.json5) <br>
 * - Additionally set configuration files (.properties|yml|json|json5)
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public class Config {
	private static final Logger LOGGER = Logger.getLogger(Config.class.getName());

	private static volatile boolean isInitialized = false;
	private static final PropertiesWrapper environmentVariables = new PropertiesWrapper(System.getProperties());;
	private static final Set<String> filesToLoad = new LinkedHashSet<>();
	private static volatile boolean isRuntimeEnabled;
	private static volatile StorageWatcher storageWatcher = null;

	static {
		LOGGER.setLevel(Level.ALL);
		setDefaultFilesToLoad();
		loadEnvironmentVariables(false);
		loadAdditionalConfigFiles();
		initRuntimeWatcher();
	}

	private static void initRuntimeWatcher() {
		if (get("config.runtime.enabled", false, Boolean.class, false)) {
			long interval = get("config.runtime.interval.value", 10L, Long.class, false);
			String unit = get("config.runtime.interval.unit", TimeUnit.SECONDS.name(), String.class, false);

			Config.storageWatcher = StorageWatcherBuilder.getBuilder()
					.interval(interval, TimeUnit.valueOf(unit.toUpperCase()))
					.watchFiles(filesToLoad)
					.specifyEvents(StandardWatchEventKinds.ENTRY_MODIFY)
					.onChanged((directory, filename) -> {
						LOGGER.info("Detected change in '" + filename + "'. Refreshing...");
						loadVariablesFromFile(directory + File.separator + filename);
					})
					.build();
			Config.storageWatcher.start();
		}
	}

	/**
	 * Set the default files to load
	 */
	private static void setDefaultFilesToLoad() {
		List<String> classpathFiles = new ArrayList<>();

		// Spring
		classpathFiles.add("application.properties");
		classpathFiles.add("application.yml");
		String springProfile = System.getProperty("spring.profiles.active", "");
		if (!springProfile.isEmpty()) {
			classpathFiles.add(String.format("application-%s.properties", springProfile));
			classpathFiles.add(String.format("application-%s.yml", springProfile));
		}

		filesToLoad.addAll(classpathFiles);

		// Vertx
		filesToLoad.add("conf/config.json");
		filesToLoad.add("conf/config.json5");
	}

	private static void loadAdditionalConfigFiles() {
		List<String> additionalFilenames = getAdditionalConfigFilesByEnvName("config.sources");
		additionalFilenames.addAll(getAdditionalConfigFilesByEnvName("spring.additional-files"));

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

	private static List<String> getAdditionalConfigFilesByEnvName(String envName) {
		return ConversionUtils.stringToList(get(envName, "", String.class, false), ",")
				.stream().filter(str -> str != null && !str.isEmpty()).map(String::trim).collect(Collectors.toList());
	}

	public static void loadEnvironmentVariables(boolean force) {
		if (force || !isInitialized()) {
			synchronized (Config.class) {
				if (!force && isInitialized())
					return;

				loadVariablesFromFiles();
				loadVariablesFromProgram();
				isInitialized = true;
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

			Map<String, String> newProperties = ParserSelector.getParser(filename).toMap(fileContent);
			environmentVariables.putAll(newProperties);
			LOGGER.log(Level.FINE, "New properties added from file '" + filename + "'. List of added keys: " + newProperties.keySet());
			return true;
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Couldn't load the file '" + filename + "'. Reason: " + e.getMessage());
		}

		return false;
	}

	private static void loadVariablesFromProgram() {
		Set<String> programProperties = System.getProperties().stringPropertyNames();
		for (String propertyName : programProperties) {
			environmentVariables.put(propertyName, System.getProperty(propertyName));
		}
		LOGGER.fine("New properties added from program. List of added keys: " + programProperties);
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
	private static <T> T get(String key, Class<T> returnType, boolean isRuntimeEnabled) throws IllegalArgumentException {
		waitForInitialization();

		// Check if runtime configuration is enabled, refresh!
		//if (isRuntimeEnabled) {
		//	loadEnvironmentVariables(true);
		//}

		String stringValue = environmentVariables.getProperty(key);
		if (stringValue != null) {
			stringValue = stringValue.replaceAll("(^\")|(\"$)", "");
			LOGGER.fine("Getting configuration '" + key + "'. Got: '" + stringValue + "'");
			return ConversionUtils.convertToType(stringValue, returnType);
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
		return result != null ? result : ConversionUtils.convertToType(defaultValue.toString(), returnType);
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

	/**
	 * Get all of the properties as a string
	 * @return A string
	 */
	public static String getPropertiesAsString() {
		return environmentVariables.entrySet().toString();
	}

	/**
	 * Get all of the properties as a map.
	 * @return A map
	 */
	public static Map<String, String> getPropertiesAsMap() {
		return getPropertiesAsMap(environmentVariables.entrySet());
	}

	/**
	 * Get all of the properties as a map.
	 * @return A map
	 */
	private static Map<String, String> getPropertiesAsMap(Collection<Map.Entry<Object, Object>> entrySet) {
		return new HashMap<>(entrySet.stream().collect(Collectors.toMap(
				entry -> Optional.ofNullable(entry.getKey()).orElse("").toString(),
				entry -> Optional.ofNullable(entry.getValue()).orElse("").toString())
		));
	}

	/**
	 * Get all of the properties as a map.
	 * @return A map
	 */
	private static Map<String, String> getPropertiesAsMap(Map<Object, Object> genericMap) {
		return getPropertiesAsMap(genericMap.entrySet());
	}


	/**
	 * The loaded files by this configurator. Left is loaded first, and the right side is loaded last.
	 * Properties that are loaded by later by a file will have overriden any previously set properties.
	 * @return A list of files. Newer files are last in the list.
	 */
	public static List<String> getLoadedFiles() {
		return new ArrayList<>(filesToLoad);
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
		return ConversionUtils.stringToList(getString(name));
	}

	public static List<String> getStringList(String name, String separator) {
		return ConversionUtils.stringToList(getString(name), separator);
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
		return ConversionUtils.stringToList(getString(name), Boolean.class);
	}

	public static List<Boolean> getBooleanList(String name, String separator) {
		return ConversionUtils.stringToList(getString(name), separator, Boolean.class);
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
		return ConversionUtils.stringToList(getString(name), Integer.class);
	}

	public static List<Integer> getIntegerList(String name, String separator) {
		return ConversionUtils.stringToList(getString(name), separator, Integer.class);
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
		return ConversionUtils.stringToList(getString(name), Long.class);
	}

	public static List<Long> getLongList(String name, String separator) {
		return ConversionUtils.stringToList(getString(name), separator, Long.class);
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
		return ConversionUtils.stringToList(getString(name), Double.class);
	}

	public static List<Double> getDoubleList(String name, String separator) {
		return ConversionUtils.stringToList(getString(name), separator, Double.class);
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
		return ConversionUtils.stringToList(getString(name), Float.class);
	}

	public static List<Float> getFloatList(String name, String separator) {
		return ConversionUtils.stringToList(getString(name), separator, Float.class);
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
		return ConversionUtils.stringToList(getString(name), Character.class);
	}

	public static List<Character> getCharacterList(String name, String separator) {
		return ConversionUtils.stringToList(getString(name), separator, Character.class);
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
		return ConversionUtils.stringToList(getString(name), Short.class);
	}

	public static List<Short> getShortList(String name, String separator) {
		return ConversionUtils.stringToList(getString(name), separator, Short.class);
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
		return ConversionUtils.stringToList(getString(name), Byte.class);
	}

	public static List<Byte> getByteList(String name, String separator) {
		return ConversionUtils.stringToList(getString(name), separator, Byte.class);
	}
}
