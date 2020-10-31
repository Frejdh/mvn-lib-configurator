package com.frejdh.util.environment.watcher;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;

/**
 *	Handles one specific directory for the directory watcher.
 */
class DirectoryWatcherComponent {

	public final DirectoryWatcherProperties properties;
	public final WatchService watcher;

	DirectoryWatcherComponent(@NotNull DirectoryWatcherProperties properties) throws IOException {
		this.properties = properties;
		this.watcher = FileSystems.getDefault().newWatchService();
	}
}
