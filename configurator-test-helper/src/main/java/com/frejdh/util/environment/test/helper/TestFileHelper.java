package com.frejdh.util.environment.test.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


public class TestFileHelper {
	public enum CleanupAction {
		REMOVE, EMPTY, RESTORE, NONE
	}

	private static class FileToCleanup {
		String filename;
		CleanupAction cleanupAction;

		public FileToCleanup(String filename, CleanupAction cleanupAction) {
			this.filename = filename;
			this.cleanupAction = cleanupAction;
		}

		@Override
		public String toString() {
			return "FileToCleanup{filename='" + filename + '\'' + ", cleanupAction=" + cleanupAction + '}';
		}
	}

	private static final List<FileToCleanup> FILES_TO_CLEANUP = new ArrayList<>();
	private static final String FILENAME_BASE = "test_file_%s.txt";
	private static final AtomicInteger FILENAME_COUNTER = new AtomicInteger();
	private static final String CLASSPATH = TestFileHelper.class.getClassLoader().getResource("").getPath();
	private static final Map<String, String> ORIGINAL_FILE_CONTENTS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private static boolean IS_CREATING_BACKUP_FILES = true;

	public static void enableCreationOfBackupFiles(boolean createBackupFiles) {
		IS_CREATING_BACKUP_FILES = createBackupFiles;
	}

	public static boolean isCreatingBackupFiles() {
		return IS_CREATING_BACKUP_FILES;
	}

	public static String nextFilename() {
		return String.format(FILENAME_BASE, FILENAME_COUNTER.getAndIncrement());
	}

	/**
	 * Run the cleanup method for the test files. Removes/restores/empties the relevant files.
	 * Suggestively should be called in either "@After" or "@AfterEach" when using Junit.
	 * @throws IOException When a file could not be restored properly.
	 */
	public static void cleanup() throws IOException {
		for (FileToCleanup file : new ArrayList<>(FILES_TO_CLEANUP)) {
			String fullpath = CLASSPATH + file.filename;
			switch (file.cleanupAction) {
				case REMOVE:
					Logger.getGlobal().info("Cleanup - Removing file: " + fullpath);
					new File(fullpath).delete();
					break;
				case EMPTY:
					Logger.getGlobal().info("Cleanup - Emptying file: " + fullpath);
					writeToExistingFile(file.filename, "", CleanupAction.NONE, false);
					break;
				case RESTORE:
					Logger.getGlobal().info("Cleanup - Restoring file: " + fullpath);
					writeToExistingFile(file.filename, ORIGINAL_FILE_CONTENTS.getOrDefault(file.filename, ""));
					ORIGINAL_FILE_CONTENTS.remove(file.filename);
				default:
					Logger.getGlobal().info("Cleanup - No action taken for file: " + fullpath);
			}
		}
		FILES_TO_CLEANUP.clear();
	}

	public static void createFile(String filename, CleanupAction cleanupAction, String content) throws IOException {
		String fullpath = CLASSPATH + filename;
		Logger.getGlobal().info("Creating file: " + fullpath);
		File file = new File(fullpath);
		if (!file.createNewFile()) {
			throw new IOException("File already exists");
		}
		else {
			if (content != null) {
				writeToExistingFile(fullpath, content, CleanupAction.NONE);
			}
			FILES_TO_CLEANUP.add(new FileToCleanup(filename, cleanupAction));
		}
	}

	public static void createFile(String filename, String content) throws IOException {
		createFile(filename, CleanupAction.REMOVE, content);
	}

	public static void createFile(String filename) throws IOException {
		createFile(filename, (String) null);
	}

	public static void createFile(String filename, CleanupAction cleanupAction) throws IOException {
		createFile(filename, cleanupAction, null);
	}

	public static String readFile(String filename) throws IOException {
		String fullpath = CLASSPATH + filename;
		File file = new File(fullpath);
		if (file.exists()) {
			StringBuilder text = new StringBuilder();
			int read, bufferSize = 1024 * 1024;
			char[] buffer = new char[bufferSize];

			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			do {
				read = br.read(buffer, 0, bufferSize);
				text.append(new String(buffer, 0, read));
			} while (read >= bufferSize);

			return text.toString();
		}
		return null;
	}

	private static void writeToExistingFile(String filename, String content, CleanupAction cleanupAction, boolean isLogging) throws IOException {
		String fullpath = CLASSPATH + filename;
		if (isLogging)
			Logger.getGlobal().info("Overwriting existing file: " + fullpath);
		if (CleanupAction.RESTORE.equals(cleanupAction)) {
			ORIGINAL_FILE_CONTENTS.putIfAbsent(filename, readFile(filename));
		}

		if (IS_CREATING_BACKUP_FILES && !new File(fullpath).exists()) {
			createFile(filename + ".bak", CleanupAction.REMOVE, ORIGINAL_FILE_CONTENTS.get(filename)); // Create backup first
		}

		FileWriter myWriter = new FileWriter(fullpath);
		myWriter.write(content);
		myWriter.close();

		FILES_TO_CLEANUP.add(new FileToCleanup(filename, cleanupAction));
	}

	public static void writeToExistingFile(String filename, String content, CleanupAction cleanupAction) throws IOException {
		writeToExistingFile(filename, content, cleanupAction, true);
	}

	public static void writeToExistingFile(String filename, String content) throws IOException {
		writeToExistingFile(filename, content, CleanupAction.RESTORE);
	}

	public static void deleteFile(String filename, CleanupAction cleanupAction) throws IOException {
		String fullpath = CLASSPATH + filename;
		if (CleanupAction.RESTORE.equals(cleanupAction)) {
			ORIGINAL_FILE_CONTENTS.put(filename, readFile(filename));
			if (IS_CREATING_BACKUP_FILES) {
				createFile(fullpath + ".bak", CleanupAction.REMOVE, ORIGINAL_FILE_CONTENTS.get(filename)); // Create backup first
			}
		}
		FILES_TO_CLEANUP.add(new FileToCleanup(filename, cleanupAction));

		Logger.getGlobal().info("Deleting file: " + fullpath);
		new File(fullpath).delete();
		FILES_TO_CLEANUP.removeIf(file -> (CleanupAction.NONE.equals(cleanupAction) || CleanupAction.REMOVE.equals(cleanupAction))
				&& file.filename.equals(filename));
	}

	public static void deleteFile(String filename) throws Exception {
		deleteFile(filename, CleanupAction.RESTORE);
	}

}
