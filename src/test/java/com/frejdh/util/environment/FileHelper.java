package com.frejdh.util.environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class FileHelper {
	public enum CleanupAction {
		REMOVE, EMPTY, NONE
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

	private static final List<FileToCleanup> files = new ArrayList<>();
	private static final String FILENAME_BASE = "test_file_%s.txt";
	private static int filenameCounter = 1;
	private static final String classpath = FileHelper.class.getClassLoader().getResource("").getPath();

	public static String nextFilename() {
		return String.format(FILENAME_BASE, filenameCounter++);
	}

	public static void cleanup() throws Exception {
		for (FileToCleanup file : new ArrayList<>(files)) {
			String fullpath = classpath + file.filename;
			switch (file.cleanupAction) {
				case REMOVE:
					Logger.getGlobal().info("Cleanup - Removing file: " + fullpath);
					new File(fullpath).delete();
					break;
				case EMPTY:
					Logger.getGlobal().info("Cleanup - Emptying file: " + fullpath);
					writeToExistingFile(file.filename, "", CleanupAction.NONE, false);
					break;
				default:
					Logger.getGlobal().info("Cleanup - No action taken for file: " + fullpath);
			}
		}
		files.clear();
	}

	public static void createFile(String filename, CleanupAction cleanupAction) throws Exception {
		String fullpath = classpath + filename;
		Logger.getGlobal().info("Creating file: " + fullpath);
		File file = new File(fullpath);
		if (!file.createNewFile()) {
			throw new IOException("File already exists");
		}
		else {
			files.add(new FileToCleanup(filename, cleanupAction));
		}
	}

	public static void createFile(String filename) throws Exception {
		createFile(filename, CleanupAction.REMOVE);
	}

	private static void writeToExistingFile(String filename, String content, CleanupAction cleanupAction, boolean isLogging) throws Exception {
		String fullpath = classpath + filename;
		if (isLogging)
			Logger.getGlobal().info("Overwriting existing file: " + fullpath);
		FileWriter myWriter = new FileWriter(fullpath);
		myWriter.write(content);
		myWriter.close();

		files.add(new FileToCleanup(filename, cleanupAction));
	}

	public static void writeToExistingFile(String filename, String content, CleanupAction cleanupAction) throws Exception {
		writeToExistingFile(filename, content, cleanupAction, true);
	}

	public static void writeToExistingFile(String filename, String content) throws Exception {
		writeToExistingFile(filename, content, CleanupAction.EMPTY);
	}

	public static void deleteFile(String filename) throws Exception {
		String fullpath = classpath + filename;
		Logger.getGlobal().info("Deleting file: " + fullpath);
		new File(fullpath).delete();
		files.removeIf(file -> file.equals(filename));
	}
}
