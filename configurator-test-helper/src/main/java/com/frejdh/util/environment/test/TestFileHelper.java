package com.frejdh.util.environment.test;

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

	public static String nextFilename() {
		return String.format(FILENAME_BASE, FILENAME_COUNTER.getAndIncrement());
	}

	public static void cleanup() throws Exception {
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

	public static void createFile(String filename, CleanupAction cleanupAction) throws Exception {
		String fullpath = CLASSPATH + filename;
		Logger.getGlobal().info("Creating file: " + fullpath);
		File file = new File(fullpath);
		if (!file.createNewFile()) {
			throw new IOException("File already exists");
		}
		else {
			FILES_TO_CLEANUP.add(new FileToCleanup(filename, cleanupAction));
		}
	}

	public static void createFile(String filename) throws Exception {
		createFile(filename, CleanupAction.REMOVE);
	}

	public static String readFile(String filename) throws Exception {
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

	private static void writeToExistingFile(String filename, String content, CleanupAction cleanupAction, boolean isLogging) throws Exception {
		String fullpath = CLASSPATH + filename;
		if (isLogging)
			Logger.getGlobal().info("Overwriting existing file: " + fullpath);
		if (CleanupAction.RESTORE.equals(cleanupAction))
			ORIGINAL_FILE_CONTENTS.putIfAbsent(filename, readFile(filename));

		FileWriter myWriter = new FileWriter(fullpath);
		myWriter.write(content);
		myWriter.close();

		FILES_TO_CLEANUP.add(new FileToCleanup(filename, cleanupAction));
	}

	public static void writeToExistingFile(String filename, String content, CleanupAction cleanupAction) throws Exception {
		writeToExistingFile(filename, content, cleanupAction, true);
	}

	public static void writeToExistingFile(String filename, String content) throws Exception {
		writeToExistingFile(filename, content, CleanupAction.EMPTY);
	}

	public static void deleteFile(String filename) throws Exception {
		String fullpath = CLASSPATH + filename;
		Logger.getGlobal().info("Deleting file: " + fullpath);
		new File(fullpath).delete();
		FILES_TO_CLEANUP.removeIf(file -> file.equals(filename));
	}
}
