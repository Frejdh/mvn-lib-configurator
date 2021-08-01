package com.frejdh.util.environment;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	 * Load a file as an InputStream
	 * @param absolutePath The relative path from the resource directory
	 * @return An InputStream or null
	 */
	static InputStream getAbsoluteFileAsStream(String absolutePath) throws IOException {
		return new FileInputStream(absolutePath);
	}

	/**
	 * Load a file as a string.
	 * @param filePath Absolute path or relative path based on the resource directory
	 * @return A string or null if the file couldn't be loaded
	 */
	static String getResourceFile(String filePath) {
		boolean isAbsolutePath = Paths.get(filePath).isAbsolute();

		try (InputStream inputStream = (isAbsolutePath ? getAbsoluteFileAsStream(filePath) : getResourceFileAsStream(filePath))) {
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

}
