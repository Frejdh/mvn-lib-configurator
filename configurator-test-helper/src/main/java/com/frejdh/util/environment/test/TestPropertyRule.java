package com.frejdh.util.environment.test;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TestPropertyRule implements TestRule {

	private final Map<String, String> originalProperties = new HashMap<>();

	public TestPropertyRule() {
		originalProperties.putAll(System.getProperties().entrySet().stream().collect(
				Collectors.toMap(
						entry -> entry.getKey().toString(),
						entry -> entry.getValue().toString()
				)));
	}

	@SneakyThrows
	protected void setPropertiesByAnnotations(List<TestProperty> annotations) {
		AtomicBoolean hasConfigclass = new AtomicBoolean(true);
		annotations.forEach(annotation -> {
			System.setProperty(annotation.key(), annotation.value());

			if (hasConfigclass.get()) {	// If 'com.frejdh.util.environment.Config' exists (optional), also update the Config class while at it
				try {
					Class<?> configClass = Class.forName("com.frejdh.util.environment.Config");
					Method method = configClass.getDeclaredMethod("set", String.class, Object.class);
					method.setAccessible(true);
					method.invoke(null, annotation.key(), annotation.value());
					method.setAccessible(false);
				} catch (ClassNotFoundException ignored) {
					hasConfigclass.set(false);
				} catch (Exception e) {
					e.printStackTrace();
					hasConfigclass.set(false);
				}
			}
		});
	}

	@NotNull
	@Override
	public Statement apply(@NotNull Statement base, @NotNull Description description) {
		List<TestProperty> annotations = new ArrayList<>();
		Class<?> objectClass = description.getTestClass();
		do {
			if (objectClass.isAnnotationPresent(TestProperty.class)) {	// Class properties (single annotation)
				annotations.addAll(0, Arrays.stream(objectClass.getAnnotationsByType(TestProperty.class))
						.collect(Collectors.toList()));
			}
			else if (objectClass.isAnnotationPresent(TestProperties.class)) {	// Class properties (multiple annotations)
				annotations.addAll(0, Arrays.stream(objectClass.getAnnotationsByType(TestProperties.class))
						.flatMap(v -> Arrays.stream(v.value()))
						.collect(Collectors.toList()));
			}
			objectClass = objectClass.getSuperclass();
		} while (objectClass != null);

		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				annotations.addAll(description.getAnnotations().stream()
						.filter(annotation -> annotation.annotationType().isAssignableFrom(TestProperty.class))
						.map(annotation -> (TestProperty) annotation)
						.collect(Collectors.toList()));

				try {
					setPropertiesByAnnotations(annotations);
					base.evaluate(); // This will run the test
				} finally {
					annotations.forEach(annotation -> {
						System.setProperty(annotation.key(), originalProperties.getOrDefault(annotation.key(), ""));
					});
				}
			}
		};
	}

}
