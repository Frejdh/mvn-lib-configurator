package com.frejdh.util.environment.storage;

import org.jetbrains.annotations.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;


@SuppressWarnings("unused")
public class ImmutableCollection<T> implements Iterable<T> {

	private final Collection<T> list;

	@SafeVarargs
	public ImmutableCollection(T... elements) {
		this.list = Arrays.asList(elements);
	}

	public ImmutableCollection(@NotNull Collection<T> list) {
		this.list = copyList(list);
	}

	@SuppressWarnings("unchecked")
	private Collection<T> copyList(Collection<T> list) {
		if (list == null)
			return null;

		try {
			return list.getClass().getConstructor(Collection.class).newInstance(list);
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			Logger.getLogger("com.frejdh.util.environment.ImmutableList")
					.warning("Failed to initalize with given List class. Defaulted to ArrayList.");
			return new ArrayList<>(list);
		}
	}

	public int size() {
		return this.list.size();
	}

	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	public boolean contains(T o) {
		return this.list.contains(o);
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public ImmutableCollection<T> clone() {
		return new ImmutableCollection<>(list);
	}

	@SuppressWarnings("unchecked")
	public T[] toArray() {
		return (T[]) this.list.toArray();
	}

	public T[] toArray(T[] a) {
		return this.list.toArray(a);
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override
	public boolean equals(Object o) {
		return this.list.equals(o);
	}

	public int hashCode() {
		return this.list.hashCode();
	}

	@NotNull
	public Iterator<T> iterator() {
		return this.list.iterator();
	}

	public void forEach(Consumer<? super T> action) {
		this.list.forEach(action);
	}

	public boolean containsAll(Collection<?> c) {
		return this.list.containsAll(c);
	}

	public String toString() {
		return this.list.toString();
	}

	public Stream<T> stream() {
		return this.list.stream();
	}

	public Stream<T> parallelStream() {
		return this.list.parallelStream();
	}
}
