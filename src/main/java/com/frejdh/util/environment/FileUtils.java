package com.frejdh.util.environment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class FileUtils {

	/**
	 * Load a file as an InputStream
	 * @param relativePath The relative path from the resource directory
	 * @return An InputStream or null
	 */
	static InputStream getResourceFileAsStream(String relativePath) {
		return FileUtils.class.getResourceAsStream(!relativePath.startsWith("/") ? "/" + relativePath : relativePath);
	}

	/**
	 * Load a file as a string.
	 * @param relativePath The relative path from the resource directory
	 * @return A string or null if the file couldn't be loaded
	 */
	static String getResourceFile(String relativePath) {
		try (InputStream inputStream = getResourceFileAsStream(relativePath)) {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			byte[] buffer = new byte[4 * 0x400]; // 4KB
			int length;
			while ((length = inputStream.read(buffer)) != -1) {
				result.write(buffer, 0, length);
			}

			return result.toString(StandardCharsets.UTF_8.name());
		} catch (NullPointerException | IOException e ) {
			return null;
		}

	}

	public static class DirectoryAndFiles {
		public final Path directory;
		public final Set<String> files = new HashSet<>();

		public DirectoryAndFiles(Path directory, String file) {
			this.directory = directory;
			this.files.add(file);
		}

		public DirectoryAndFiles(Path directory, Collection<String> filenames) {
			this.directory = directory;
			this.files.addAll(filenames);
		}

		public DirectoryAndFiles(String directory, String file) {
			this(FileSystems.getDefault().getPath(directory), file);
		}

		public DirectoryAndFiles(String directory, Collection<String> filenames) {
			this(FileSystems.getDefault().getPath(directory), filenames);
		}

	}
}
