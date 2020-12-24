package com.frejdh.util.environment.parser;

import com.frejdh.util.environment.ConversionUtils;
import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static java.util.regex.Pattern.compile;

/**
 * For .properties files
 */
public class YamlParser extends AbstractParser {

	private static YamlParser singletonInstance;

	protected YamlParser() { }

	public static YamlParser getSingletonInstance() {
		if (singletonInstance == null) {
			synchronized (YamlParser.class) { // Only lock if new instance
				if (singletonInstance == null) { // To avoid race condition
					singletonInstance = new YamlParser();
				}
			}
		}

		return singletonInstance;
	}

	/**
	 * Convert text in the format of <br>
	 * <pre> <code>
	 * example.property.value1: Hello 1 <br>
	 * example.property.value2: "Hello 2" <br>
	 * example: <br>
	 *   property: <br>
	 *     value3: "Hello 3" <br>
	 *     value4: "Hello 4" <br>
	 * </code> </pre> <br>
	 *
	 * To a map where <br>
	 * <code>
	 *   Map[example.property.value1] = "Hello 1" <br>
	 *   Map[example.property.value2] = "Hello 2" <br>
	 *   Map[example.property.value3] = "Hello 3" <br>
	 *   Map[example.property.value4] = "Hello 4" <br>
	 * </code> <br>
	 *
	 * This parser follows YML parsing guidelines somewhat. This means that some "illegal" spaces can be combined with colons.
	 * However, the tabbing must still be consistent due to parsing reasons!
	 * @param textContent Text to convert to a Map, where each entry is separated by a new line
	 * @return A Map
	 */
	public Map<String, String> toMap(String textContent) throws IOException {
		List<String> lines = ConversionUtils.stringToList(textContent, "\r\n", false)
				.stream()
				.filter(line -> !StringUtils.isBlank(line) && !line.matches("\\s*[#].*")) // Not empty, not comment and has variable assignment. FIXME: not working? \s*[#].*
				.collect(Collectors.toList());
		Map<String, String> mapToReturn = new HashMap<>();
		String indentation = detectIndentationSequence(lines);
		List<Entry> entriesToProcess = getEntriesToProcess(lines, indentation);

		return toMapRecursive(entriesToProcess, mapToReturn);
	}

	private List<Entry> getEntriesToProcess(List<String> lines, String indentationSequence) {
		Entry previous = null;
		List<Entry> rootEntries = new ArrayList<>();
		for (String line : lines) {
			Entry newEntry = null;
			if (previous == null) {
				newEntry = new Entry(line, 0);
				previous = newEntry;
			}
			else {
				int level = getIndendentionLevel(line, indentationSequence);

				if (level > previous.getDepth()) { // Add as child
					newEntry = new Entry(line, level, previous);
					previous.addChild(newEntry);
					previous = newEntry;
				}
				else if (level == previous.getDepth()) { // Same level, add as another entry
					newEntry = new Entry(line, level, previous.getParent());
					if (previous.hasParent()) {
						previous.getParent().addChild(newEntry);
					}
					previous = newEntry;
				}
				else { // One or more steps above, add as a new entry to a parent
					while (level < previous.getDepth()) {
						previous = previous.getParent();
					}

					newEntry = new Entry(line, level, previous.hasParent() ? previous.getParent() : null);
					if (newEntry.hasParent()) {
						previous.getParent().addChild(newEntry);
					}
				}
			}

			if (!newEntry.hasParent()) {
				rootEntries.add(newEntry);
			}
		}

		return rootEntries;
	}

	private Map<String, String> toMapRecursive(List<Entry> entriesToProcess, Map<String, String> mapToReturn) {
		for (Entry entry : entriesToProcess) {
			if (entry.hasChildren()) {
				toMapRecursive(entry.children, mapToReturn);
			}
			else if (entry.isArrayOfPrimitives()) { // Array with square brackets on the same line (one line)
				// For elements with a defined index
				addChildrenElementsWithoutExistingIndexToMap(entry, mapToReturn);
				addElementToArrayInMap(entry, mapToReturn);
			}
			else if (entry.isArrayElement() && entry.hasIndex()) { // Array element with indexed key (one line)
				mapToReturn.put(entry.getFullKey(), entry.value);
				String arrayFieldWithoutIndex = entry.getFullKey().replaceAll("\\[\\d+].*?\\s*$", "");
				addElementToArrayInMap(entry, mapToReturn, arrayFieldWithoutIndex);
			}
			else if (entry.isArrayElement()) { // Array element with defined with a dash at the start
				addElementToArrayInMap(entry, mapToReturn);
				addElementWithoutExistingIndexToMap(entry, mapToReturn);
			}
			else {
				mapToReturn.put(entry.getFullKey(), entry.value);
			}
		}

		return mapToReturn;
	}

	/**
	 * Adds an element directly to the map. Method for values with a predefined index. For example as 'my.property[0]=hello1'
	 */
	private void addChildrenElementsWithIndexToMap(Entry entry, Map<String, String> map) {
		List<Entry> childrenWithIndex = entry.getChildren().stream().filter(Entry::hasElementIndex).collect(Collectors.toList());
		childrenWithIndex.forEach(child -> {
			map.put(
					entry.getFullKey(),
					entry.getValue()
			);
		});
	}

	/**
	 * Adds an element directly to the map. Method for values without a predefined index. For example as 'my.property[1]=hello2'
	 */
	private void addChildrenElementsWithoutExistingIndexToMap(Entry entry, Map<String, String> map) {
		List<Entry> childrenWithoutIndex = entry.getChildren().stream().filter(child -> !child.hasElementIndex()).collect(Collectors.toList());
		int index = 0;
		for (Entry child : childrenWithoutIndex) {
			index = addElementWithoutExistingIndexToMap(child, map) + 1;
		}
	}

	/**
	 * Add an element directly to the map, that has no predefined index to use.
	 * @return The index that was used to insert the value
	 */
	private int addElementWithoutExistingIndexToMap(Entry entry, Map<String, String> map) {
		return addElementWithoutExistingIndexToMap(entry, map, 0);
	}

	/**
	 * Add an element directly to the map, that has no predefined index to use.
	 * @return The index that was used to insert the value
	 */
	private int addElementWithoutExistingIndexToMap(Entry entry, Map<String, String> map, int startingIndex) {
		int index = startingIndex;
		String fullKey = entry.getFullKey();

		String keyWithIndex = fullKey + "[" + index + "]";
		while (map.getOrDefault(keyWithIndex, null) != null) { // To make sure that no element is replaced
			index++;
			keyWithIndex = fullKey + "[" + index + "]";
		}
		map.put(keyWithIndex, entry.getValue());
		return index;
	}

	/**
	 * Adds an element to the array field in the map. Method for values without a predefined index. For example as 'my.property=[hello1, hello2, hello3]'
	 */
	private void addElementToArrayInMap(Entry entry, Map<String, String> map) {
		String key = entry.getFullKey();
		addElementToArrayInMap(entry, map, key);
	}

	/**
	 * Adds an element to the array field in the map. Method for values without a predefined index. For example as 'my.property=[hello1, hello2, hello3]'
	 */
	private void addElementToArrayInMap(Entry entry, Map<String, String> map, String key) {
		map.put(
				key,
				map.getOrDefault(key, "") + entry.getValue() + ARRAY_SEPARATOR_CHARACTER
		);
	}

	/**
	 * Helper method to detect indention. This method assumes that no lines in the list are empty or comments.
	 * @param lines Lines to detect indention with
	 * @return The sequence for the indentation, could for instance be a sequence of 2 spaces or one tab character.
	 */
	private String detectIndentationSequence(List<String> lines) {
		for (String line : lines) {
			Matcher indentationMatcher = Pattern.compile("^(\\s+).*").matcher(line);

			if (indentationMatcher.find()) {
				return indentationMatcher.group(1);
			}
		}

		return "  ";
	}

	/**
	 * Helper method to detect indention. This method assumes that no lines in the list are empty or comments.
	 * @param line Line to detect the depth for
	 * @param indentationSequence Character sequence for the indentation
	 * @return The level for said indentation.
	 */
	private int getIndendentionLevel(String line, String indentationSequence) {
		return StringUtils.countMatches(line, indentationSequence);
	}


	/**
	 * Entry class for YAML properties. The class assumes that the line is valid and not empty, etc.
	 */
	public static class Entry {
		private final static String OBJECT_PATTERN = "\\s*.+?:.*";
		private final static String OBJECT_PATTERN_WITH_INDEX = "\\s*.+?\\[\\d+]:.*";
		private final static String ARRAY_SAME_LINE_PATTERN = OBJECT_PATTERN + "\\s*\\[.*].*";
		private final static String ARRAY_NEW_LINE_PATTERN = "\\s*-.*";

		private final String line;
		private final String key; // This line only
		private String elementIndex = null;
		private final String value;
		private final int depth;
		private final List<Entry> children;
		private final Entry parent;

		public Entry(String line, int depth) {
			this(line, depth, null);
		}

		public Entry(String line, int depth, Entry parent) {
			this.line = line.trim();
			this.depth = depth;
			this.children = new ArrayList<>();
			this.parent = parent;
			this.key = setKey(line);
			this.value = cleanupValue((line.matches(OBJECT_PATTERN) ? line.substring(line.indexOf(':') + 1) : line.substring(line.indexOf('-') + 1)).trim());

			if (isArrayAndDefinedOnSingleLine()) {
				addChildrenFromSingleLineArray();
			}
			if (key.matches(".+?\\[\\d+]\\s*:.*")) {
				elementIndex = key.substring(key.indexOf("["), key.indexOf("]") - 1);
			}
		}

		private void addChildrenFromSingleLineArray() {
			String[] childrenValues = value.replaceAll("(^\\s*\\[)|(]\\s*$)", "").split(",(?=(?:[^\"']*[\"'][^\"]*[\"'])*[^\"']*$)");
			for (String value : childrenValues) {
				children.add(new Entry("- " + value, this.depth + 1, this));
			}
		}

		private String setKey(String line) {
			if (line.matches(OBJECT_PATTERN)) {
				return line.split("\\s*:")[0].trim();
			}
			return "";
		}

		private String setValue(String line) {
			//
			if (line.matches(OBJECT_PATTERN)) {
				return line.split("\\s*:")[0].trim();
			}
			return "";
		}

		public String getLine() {
			return line;
		}

		public int getDepth() {
			return depth;
		}

		public void addChild(Entry entry) {
			children.add(entry);
		}

		public List<Entry> getChildren() {
			return children;
		}

		public boolean hasChildren() {
			return !children.isEmpty();
		}

		public boolean hasParent() {
			return parent != null;
		}

		public Entry getParent() {
			return parent;
		}

		public String getParentKey() {
			return hasParent() ? parent.key : null;
		}

		/**
		 * New line starting with dash, or containing array index
		 */
		public boolean isArrayElement() {
			return line.matches(ARRAY_NEW_LINE_PATTERN) || line.matches(OBJECT_PATTERN_WITH_INDEX);
		}

		public boolean hasIndex() {
			return key.matches(".+\\[\\d+]");
		}

		public boolean isArrayAndDefinedOnSingleLine() {
			return line.matches(ARRAY_SAME_LINE_PATTERN);
		}

		public boolean isObject() {
			return children.stream().noneMatch(Entry::isArrayElement);
		}

		public boolean isArray() {
			return isArrayOfPrimitives() || isArrayOfObjects();
		}

		public boolean isArrayOfPrimitives() {
			return isArrayAndDefinedOnSingleLine() || (!children.isEmpty() && children.stream().allMatch(entry -> entry.isArrayElement() && !entry.isObject()));
		}

		public boolean isArrayOfObjects() {
			return isArrayAndDefinedOnSingleLine() || (!children.isEmpty() && children.stream().anyMatch(entry -> entry.isArrayElement() && entry.isObject()));
		}

		public String getKey() {
			return key;
		}

		public String getFullKey() {
			StringBuilder fullKey = new StringBuilder(key);
			Entry current = this;
			while (current.hasParent()) {
				fullKey.insert(0, current.parent.key + ".");
				current = current.getParent();
			}

			String fullKeyString = fullKey.toString();
			return fullKeyString.endsWith(".") ? fullKeyString.substring(0, fullKey.lastIndexOf(".")) : fullKeyString;
		}

		public String getValue() {
			return value;
		}

		/**
		 * For arrays only. Checks if the key has an index attached to the used key.
		 */
		public boolean hasElementIndex() {
			return elementIndex != null;
		}

		private String cleanupValue(String value) {
			return removeComment(value).replaceAll("^[\"']|[\"']$", "");
		}

		private String removeComment(String value) {
			if (value.contains("#") || value.matches("(?<!['\"]).*#.*(['\"])")) { // Quoted hashtag
				return value;
			}
			else if (value.contains("#") && !value.matches("^[\"'].*")) {
				return value.substring(0, value.indexOf('#'));
			}
			return value;
		}

		@Override
		public String toString() {
			return "Entry{" +
					"line='" + line + '\'' +
					", key='" + key + '\'' +
					", elementIndex='" + elementIndex + '\'' +
					", value='" + value + '\'' +
					", depth=" + depth +
					", children=" + children.stream().map(Entry::getKey).collect(Collectors.toList()) +
					", parent=" + getParentKey() +
					'}';
		}
	}

}
