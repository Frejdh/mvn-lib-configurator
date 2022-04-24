package com.frejdh.util.environment;

import com.frejdh.util.environment.storage.map.LinkedPathMultiMap;
import com.frejdh.util.watcher.StorageWatcher;
import com.frejdh.util.watcher.StorageWatcherBuilder;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import static com.frejdh.util.environment.parser.AbstractParser.ARRAY_PATTERN_FOR_KEY;

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
	private static final String ADDITIONAL_CONFIG_FILES = "config.sources";
	private static final String ADDITIONAL_CONFIG_FILES_SPRING = "spring.additional-files";

	private static volatile boolean isInitialized = false;
	//private static final PropertiesWrapper environmentVariables = new PropertiesWrapper(System.getProperties());
	private static final LinkedPathMultiMap<String> properties = new LinkedPathMultiMap<>();
	private static final Set<String> filesToLoad = new LinkedHashSet<>();
	private static volatile boolean isRuntimeEnabled;
	private static volatile StorageWatcher storageWatcher = null;

	static {
		init();
	}

	private static void init() {
		LOGGER.setLevel(Level.ALL);
		setDefaultFilesToLoad();
		refresh(true);
		initRuntimeWatcher();
	}

	private static void initRuntimeWatcher() {
		if (get("config.runtime.enabled", false, Boolean.class, false)) {
			long interval = get("config.runtime.interval.value", 10L, Long.class, false);
			String unit = get("config.runtime.interval.unit", TimeUnit.SECONDS.name(), String.class, false);

			if (Config.storageWatcher != null) {  // Only needed for the tests as this method can be called multiple times there.
				Config.storageWatcher.stop();
			}

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
		// Spring
		filesToLoad.add("application.properties");
		filesToLoad.add("application.yml");
		filesToLoad.addAll(getSpringProfileFilenames());

		// Vertx
		filesToLoad.add("conf/config.json");
		filesToLoad.add("conf/config.json5");
	}

	private static List<String> getSpringProfileFilenames() {
		List<String> filenames = new ArrayList<>();
		String springProfileArgument = System.getProperty("spring.profiles.active", "");
		List<String> springProfiles = springProfileArgument.contains(",")
				? Arrays.asList(springProfileArgument.split("\\s*,\\s*"))
				: Collections.singletonList(springProfileArgument);

		springProfiles.forEach(profile -> {
			filenames.add(String.format("application-%s.properties", profile));
			filenames.add(String.format("application-%s.yml", profile));
		});

		return filenames;
	}

	private static void loadVariablesFromAdditionalFiles() {
		List<String> additionalFilenames = getAdditionalConfigFilesByEnvName(ADDITIONAL_CONFIG_FILES);
		additionalFilenames.addAll(getAdditionalConfigFilesByEnvName(ADDITIONAL_CONFIG_FILES_SPRING));

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
		return ConversionUtils.getStringAsList(get(envName, "", String.class, false, false), ",")
				.stream().filter(str -> str != null && !str.isEmpty()).map(String::trim).collect(Collectors.toList());
	}

	public static void refresh(boolean force) {
		if (force || !isInitialized()) {
			synchronized (Config.class) {
				if (!force && isInitialized())
					return;

				properties.clear();
				loadVariablesFromFiles();
				loadVariablesFromAdditionalFiles();
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
		try {
			String fileContent = FileUtils.getResourceFile(filename);
			if (fileContent == null) { // If file doesn't exist, remove it
				return false;
			}

			Map<String, List<String>> newProperties = ParserSelector.getParser(filename).toMultiMap(fileContent);
			// Config.environmentVariables.setProperties(newProperties);
			Config.properties.putAll(newProperties);
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
//			environmentVariables.setProperty(propertyName, System.getProperty(propertyName));
			properties.put(propertyName, System.getProperty(propertyName));
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

	private static <T> T get(String key, Class<T> returnType, boolean isRuntimeEnabled, boolean waitForInitialization) throws IllegalArgumentException {
		if (waitForInitialization) {
			waitForInitialization();
		}

//		String stringValue = environmentVariables.getProperty(key);
		String keyArrayIndex = key.matches(ARRAY_PATTERN_FOR_KEY.pattern()) ? key.substring(key.lastIndexOf("[") + 1, key.lastIndexOf("]")) : null;
		String stringValue = keyArrayIndex != null ? properties.getByIndex(key, Integer.parseInt(keyArrayIndex)) : properties.getLast(key);
		if (stringValue != null) {
//			stringValues = stringValues.stream().map(val -> val.replaceAll("(^\")|(\"$)", "")).collect(Collectors.toList());
			LOGGER.fine("Getting configuration '" + key + "'. Got: '" + stringValue + "'");
			return ConversionUtils.convertStringToType(stringValue, returnType);
		}

		return null;
	}

	// Internal. Set if the property should be fetched runtime or not.
	private static <T> T get(String key, Class<T> returnType, boolean isRuntimeEnabled) throws IllegalArgumentException {
		return get(key, returnType, isRuntimeEnabled, true);
	}

	/**
	 * Get a property by a key and return type.
	 * @param key Name of the property
	 * @param returnType The class that the property shall be returned as
	 * @param <T> The supplied type
	 * @return The property or null if none was found
	 * @throws IllegalArgumentException If the class was not supported
	 */
	public static <T> T get(String key, Class<T> returnType) throws IllegalArgumentException {
		return get(key, returnType, Config.isRuntimeEnabled);
	}

	// Internal. Set if the property should be fetched runtime or not.
	private static <T> T get(String key, T defaultValue, Class<T> returnType, boolean isRuntimeEnabled, boolean waitForInitialization) throws IllegalArgumentException {
		T result = get(key, returnType, isRuntimeEnabled, waitForInitialization);
		return result != null ? result : ConversionUtils.convertStringToType(defaultValue.toString(), returnType);
	}

	// Internal. Set if the property should be fetched runtime or not.
	private static <T> T get(String key, T defaultValue, Class<T> returnType, boolean isRuntimeEnabled) throws IllegalArgumentException {
		return get(key, defaultValue, returnType, isRuntimeEnabled, true);
	}

	/**
	 * Get a property by a key and return type.
	 * @param key Name of the property
	 * @param defaultValue The default value to return.
	 * @param returnType The class that the property shall be returned as
	 * @param <T> The supplied type
	 * @return The property or null if none was found
	 * @throws IllegalArgumentException If the class was not supported
	 */
	public static <T> T get(String key, T defaultValue, Class<T> returnType) throws IllegalArgumentException {
		return get(key, defaultValue, returnType, Config.isRuntimeEnabled);
	}

	/**
	 * Get optional property by a key and return type.
	 * @param key Name of the property
	 * @param returnType The class that the property shall be returned as
	 * @param <T> The supplied type
	 * @return The optional property
	 * @throws IllegalArgumentException If the class was not supported
	 */
	public static <T> Optional<T> getOptional(String key, Class<T> returnType) throws IllegalArgumentException {
		return Optional.ofNullable(get(key, returnType, Config.isRuntimeEnabled));
	}

	/**
	 * Gets properties by a key and sub-type.
	 * @param key Name of the property
	 * @param subType The class that the property shall be returned as
	 * @param <T> The supplied type
	 * @return The properties or empty list if none was found
	 */
	public static <T> List<T> getList(String key, Class<T> subType) {
		List<String> stringValues = properties.get(key);
		if (stringValues != null && !stringValues.isEmpty()) {
			LOGGER.fine("Getting configuration '" + key + "'. Got: '" + stringValues + "'");
			return ConversionUtils.convertListStringToSubType(stringValues, subType);
		}
		return new ArrayList<>();
	}

	/**
	 * Get optional properties by a key and return type.
	 * @param key Name of the property
	 * @param subType The class that the property elements shall be returned as
	 * @param <T> The supplied type
	 * @return The optional property. Empty lists will be reported as "not present" in the Optional object.
	 * @throws IllegalArgumentException If the class was not supported
	 */
	public static <T> Optional<List<T>> getOptionalList(String key, Class<T> subType) {
		List<T> list = getList(key, subType);
		return Optional.ofNullable(!list.isEmpty() ? list : null);
	}

	// TODO: Implement, again
//	/**
//	 * Get all of the properties as a HashMap.
//	 * @param key Name of the property
//	 * @return A HashMap
//	 */
//	public static Map<String, List<Object>> getHashMap(String key) {
//		return properties.toMultiMap(key);
//	}

	/**
	 * Get all of the properties as a HashMap.
	 * @param key Name of the property
	 * @return A HashMap
	 */
	public static Map<String, List<Object>> getMultiMap(String key) {
		return properties.toMultiMap(key, Object.class);
	}

	/**
	 * Get all properties as a MultiMap.
	 * @param key Name of the property
	 * @return A HashMap, or null of nothing was found
	 */
	public static <T> Map<String, List<T>> getMultiMap(String key, Class<T> innerObjectsClass) {
		return properties.toMultiMap(key, innerObjectsClass);
	}

	/**
	 * Get all properties as an optional MultiMap.
	 * @param key Name of the property
	 * @return An optional that could contain a found MultiMap
	 */
	public static Optional<Map<String, List<Object>>> getOptionalMultiMap(String key) {
		return Optional.ofNullable(getMultiMap(key));
	}

	/**
	 * Get all of the properties as a string
	 * @return A string
	 */
	public static String getPropertiesAsString() {
//		return environmentVariables.entrySet().toString();
		return properties.toString();
	}

	/**
	 * Get the implemented map. Uses wrapper methods.
	 * @param key Name of the property
	 * @return The implementation map
	 */
	public static <T> T getObject(String key, Class<T> toClass) {
		return properties.toObject(key, toClass);
	}

	/**
	 * Get the implemented map. Uses wrapper methods.
	 * @param key Name of the property
	 * @return An optional implementation map
	 */
	public static <T> Optional<T> getOptionalObject(String key, Class<T> toClass) {
		return Optional.ofNullable(getObject(key, toClass));
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
	 * Fetch a property with a given key as a String (works for all existing values).
	 * @param key Name of the property
	 * @return The instance or null if none was found
	 */
	public static String getString(String key) {
		return get(key, String.class);
	}

	/**
	 * Fetch a property with a given key as an optional String (works for all existing values).
	 * @param key Name of the property
	 * @return The optional with the found value
	 */
	public static Optional<String> getOptionalString(String key) {
		return Optional.ofNullable(getString(key));
	}

	/**
	 * Fetch a property with a given key as a String (works for all existing values).
	 * @param key Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value if none was found
	 */
	public static String getString(String key, @NotNull String defaultValue) {
		return get(key, defaultValue, String.class);
	}

	public static List<String> getStringList(String key) {
		return getList(key, String.class);
	}

	/**
	 * See {@link #getOptionalList(String, Class)}
	 */
	public static Optional<List<String>> getOptionalStringList(String key) {
		return getOptionalList(key, String.class);
	}

	public static List<String> getStringAsList(String key, String separator) {
		return ConversionUtils.getStringAsList(getString(key), separator);
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @return The instance or null if none was found
	 */
	public static Boolean getBoolean(String key) {
		return get(key, Boolean.class);
	}

	/**
	 * Fetch a property with a given key as an optional Boolean (works for all existing values).
	 * @param key Name of the property
	 * @return The optional with the found value
	 */
	public static Optional<Boolean> getOptionalBoolean(String key) {
		return Optional.ofNullable(getBoolean(key));
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static boolean getBoolean(String key, boolean defaultValue) {
		return get(key, defaultValue, Boolean.class);
	}


	public static List<Boolean> getBooleanList(String key) {
		return getList(key, Boolean.class);
	}

	/**
	 * See {@link #getOptionalList(String, Class)}
	 */
	public static Optional<List<Boolean>> getOptionalBooleanList(String key) {
		return getOptionalList(key, Boolean.class);
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @return The instance or null if none was found
	 */
	public static Integer getInteger(String key) {
		return get(key, Integer.class);
	}

	/**
	 * Fetch a property with a given key as an optional Integer (works for all existing values).
	 * @param key Name of the property
	 * @return The optional with the found value
	 */
	public static Optional<Integer> getOptionalInteger(String key) {
		return Optional.ofNullable(getInteger(key));
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static int getInteger(String key, int defaultValue) {
		return get(key, defaultValue, Integer.class);
	}

	public static List<Integer> getIntegerList(String key) {
		return getList(key, Integer.class);
	}

	/**
	 * See {@link #getOptionalList(String, Class)}
	 */
	public static Optional<List<Integer>> getOptionalIntegerList(String key) {
		return getOptionalList(key, Integer.class);
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @return The instance or null if none was found
	 */
	public static Long getLong(String key) {
		return get(key, Long.class);
	}

	/**
	 * Fetch a property with a given key as an optional Long (works for all existing values).
	 * @param key Name of the property
	 * @return The optional with the found value
	 */
	public static Optional<Long> getOptionalLong(String key) {
		return Optional.ofNullable(getLong(key));
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static long getLong(String key, long defaultValue) {
		return get(key, defaultValue, Long.class);
	}

	public static List<Long> getLongList(String key) {
		return getList(key, Long.class);
	}

	/**
	 * See {@link #getOptionalList(String, Class)}
	 */
	public static Optional<List<Long>> getOptionalLongList(String key) {
		return getOptionalList(key, Long.class);
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @return The instance or null if none was found
	 */
	public static Double getDouble(String key) {
		return get(key, Double.class);
	}

	/**
	 * Fetch a property with a given key as an optional Double (works for all existing values).
	 * @param key Name of the property
	 * @return The optional with the found value
	 */
	public static Optional<Double> getOptionalDouble(String key) {
		return Optional.ofNullable(getDouble(key));
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static double getDouble(String key, double defaultValue) {
		return get(key, defaultValue, Double.class);
	}

	public static List<Double> getDoubleList(String key) {
		return getList(key, Double.class);
	}

	/**
	 * See {@link #getOptionalList(String, Class)}
	 */
	public static Optional<List<Double>> getOptionalDoubleList(String key) {
		return getOptionalList(key, Double.class);
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @return The instance or null if none was found
	 */
	public static Float getFloat(String key) {
		return get(key, Float.class);
	}

	/**
	 * Fetch a property with a given key as an optional Float (works for all existing values).
	 * @param key Name of the property
	 * @return The optional with the found value
	 */
	public static Optional<Float> getOptionalFloat(String key) {
		return Optional.ofNullable(getFloat(key));
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static float getFloat(String key, float defaultValue) {
		return get(key, defaultValue, Float.class);
	}

	public static List<Float> getFloatList(String key) {
		return getList(key, Float.class);
	}

	/**
	 * See {@link #getOptionalList(String, Class)}
	 */
	public static Optional<List<Float>> getOptionalFloatList(String key) {
		return getOptionalList(key, Float.class);
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @return The instance or null if none was found
	 */
	public static Character getCharacter(String key) {
		return get(key, Character.class);
	}

	/**
	 * Fetch a property with a given key as an optional Character (works for all existing values).
	 * @param key Name of the property
	 * @return The optional with the found value
	 */
	public static Optional<Character> getOptionalCharacter(String key) {
		return Optional.ofNullable(getCharacter(key));
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static char getCharacter(String key, char defaultValue) {
		return get(key, defaultValue, Character.class);
	}

	public static List<Character> getCharacterList(String key) {
		return getList(key, Character.class);
	}

	/**
	 * See {@link #getOptionalList(String, Class)}
	 */
	public static Optional<List<Character>> getOptionalCharacterList(String key) {
		return getOptionalList(key, Character.class);
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @return The instance or null if none was found
	 */
	public static Short getShort(String key) {
		return get(key, Short.class);
	}

	/**
	 * Fetch a property with a given key as an optional Short (works for all existing values).
	 * @param key Name of the property
	 * @return The optional with the found value
	 */
	public static Optional<Short> getOptionalShort(String key) {
		return Optional.ofNullable(getShort(key));
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static short getShort(String key, short defaultValue) {
		return get(key, defaultValue, Short.class);
	}

	public static List<Short> getShortList(String key) {
		return getList(key, Short.class);
	}

	/**
	 * See {@link #getOptionalList(String, Class)}
	 */
	public static Optional<List<Short>> getOptionalShortList(String key) {
		return getOptionalList(key, Short.class);
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @return The instance or null if none was found
	 */
	public static Byte getByte(String key) {
		return get(key, Byte.class);
	}

	/**
	 * Fetch a property with a given key as an optional Byte (works for all existing values).
	 * @param key Name of the property
	 * @return The optional with the found value
	 */
	public static Optional<Byte> getOptionalByte(String key) {
		return Optional.ofNullable(getByte(key));
	}

	/**
	 * Fetch a property with a given key.
	 * @param key Name of the property
	 * @param defaultValue Value to use in case the property wasn't found
	 * @return The instance or the default value (null-safe)
	 */
	public static byte getByte(String key, byte defaultValue) {
		return get(key, defaultValue, Byte.class);
	}

	public static List<Byte> getByteList(String key) {
		return getList(key, Byte.class);
	}

	/**
	 * See {@link #getOptionalList(String, Class)}
	 */
	public static Optional<List<Byte>> getOptionalByteList(String key) {
		return getOptionalList(key, Byte.class);
	}

	private static String cleanupPropertyKey(String key) {
		return key != null
				? key.trim()
				.replace("_", ".")
				.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase()
				: null;
	}

	/**
	 * Private setter method that is hidden for common usage
	 */
	private static void set(String key, Object value) {
		if (key == null) {
			return;
		}
		key = cleanupPropertyKey(key);
		properties.put(key, value != null ? value.toString() : null);
		if (ADDITIONAL_CONFIG_FILES.equals(key) || ADDITIONAL_CONFIG_FILES_SPRING.equals(key)) {
			loadVariablesFromAdditionalFiles();
			loadVariablesFromProgram();
		}
	}

}
