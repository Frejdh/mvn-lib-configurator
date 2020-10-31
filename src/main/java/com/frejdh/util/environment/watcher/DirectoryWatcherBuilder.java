package com.frejdh.util.environment.watcher;

import com.frejdh.util.environment.ImmutableCollection;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DirectoryWatcherBuilder {

	private DirectoryWatcherBuilder parentBuilder = null;
	private final Set<WatchEvent.Kind<Path>> eventsToWatch = new HashSet<>();
	private final Set<String> filesToLimitTo = new HashSet<>();
	private String directoryToWatch;
	private DirectoryWatcher.OnChanged onChanged = () -> { };

	public DirectoryWatcherBuilder(String directoryToWatch) {
		this.directoryToWatch = directoryToWatch;
	}

	public DirectoryWatcherBuilder() {
		this("");
	}

	private DirectoryWatcherBuilder(String directoryToWatch, DirectoryWatcherBuilder parent) {
		this(directoryToWatch);
		this.parentBuilder = parent;
	}

	/**
	 * Use java.nio.file.StandardWatchEventKinds
	 */
	public DirectoryWatcherBuilder watchEvent(WatchEvent.Kind<Path> event) {
		eventsToWatch.add(event);
		return this;
	}

	/**
	 * Use java.nio.file.StandardWatchEventKinds
	 */
	public DirectoryWatcherBuilder watchEvents(WatchEvent.Kind<Path>... events) {
		eventsToWatch.addAll(Arrays.asList(events));
		return this;
	}

	public DirectoryWatcherBuilder limitToFile(String filename) {
		filesToLimitTo.add(filename);
		return this;
	}

	public DirectoryWatcherBuilder limitToFile(String... filenames) {
		filesToLimitTo.addAll(Arrays.asList(filenames));
		return this;
	}

	public DirectoryWatcherBuilder onChanged(DirectoryWatcher.OnChanged onChanged) {
		this.onChanged = onChanged;
		return this;
	}

	/**
	 * Creates a new Watcher that can be configured. Default is the classpath directory.
	 * @return A new builder instance
	 */
	public DirectoryWatcherBuilder createNext() {
		return new DirectoryWatcherBuilder("", this);
	}

	/**
	 * Creates a new Watcher that can be configured
	 * @param directoryToWatch Directory to watch over
	 * @return A new builder instance
	 */
	public DirectoryWatcherBuilder createNext(String directoryToWatch) {
		return new DirectoryWatcherBuilder(directoryToWatch, this);
	}

	public DirectoryWatcher build() {
		return new DirectoryWatcher(
				build(new ArrayList<>())
		);

	}

	private List<DirectoryWatcherComponent> build(List<DirectoryWatcherComponent> currentComponents) {
		DirectoryWatcherProperties settings = new DirectoryWatcherProperties(
				new ImmutableCollection<>(filesToLimitTo),
				new ImmutableCollection<>(eventsToWatch),
				directoryToWatch,
				onChanged
		);

		DirectoryWatcherComponent watcher = null;
		try {
			watcher = new DirectoryWatcherComponent(settings);
		} catch (IOException e) {
			e.printStackTrace();
		}
		currentComponents.add(watcher);

		if (parentBuilder != null) {
			return parentBuilder.build(currentComponents);
		}
		return currentComponents;
	}
}
