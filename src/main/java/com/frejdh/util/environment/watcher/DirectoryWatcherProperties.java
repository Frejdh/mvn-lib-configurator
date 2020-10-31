package com.frejdh.util.environment.watcher;
import com.frejdh.util.environment.ImmutableCollection;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

class DirectoryWatcherProperties {

	public final ImmutableCollection<WatchEvent.Kind<Path>> eventsToWatch;
	public final ImmutableCollection<String> files;
	public final Path directory;
	public final DirectoryWatcher.OnChanged onChanged;

	DirectoryWatcherProperties(ImmutableCollection<String> files,
							   ImmutableCollection<WatchEvent.Kind<Path>> eventsToWatch,
							   String directory,
							   DirectoryWatcher.OnChanged onChanged) {
		this.eventsToWatch = eventsToWatch;
		this.files = files;
		this.directory = FileSystems.getDefault().getPath(directory);
		this.onChanged = onChanged;
	}

	public boolean isWatchingAllFiles() {
		return files.size() == 0;
	}
}
