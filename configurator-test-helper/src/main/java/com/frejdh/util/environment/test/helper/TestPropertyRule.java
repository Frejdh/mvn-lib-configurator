package com.frejdh.util.environment.test.helper;

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

public class TestPropertyRule implements InvocationInterceptor  {

	private final Map<String, String> originalProperties = new HashMap<>();
	private static final AtomicBoolean HAS_CONFIG_CLASS = new AtomicBoolean(true);

	public TestPropertyRule() {
		originalProperties.putAll(System.getProperties().entrySet().stream().collect(
				Collectors.toMap(
						entry -> entry.getKey().toString(),
						entry -> entry.getValue().toString()
				)));
	}

	@SneakyThrows
	protected void setPropertiesByAnnotations(List<TestProperty> annotations) {
		annotations.forEach(annotation -> {
			System.setProperty(annotation.key(), annotation.value());

			if (HAS_CONFIG_CLASS.get()) {	// If 'com.frejdh.util.environment.Config' exists (optional), also update the Config class while at it
				setConfigProperty(annotation.key(), annotation.value());
			}
		});
	}

	@NotNull
	@Override
	public Statement apply(@NotNull Statement base, @NotNull Description description) {
		List<TestProperty> annotations = new ArrayList<>();
		Class<?> objectClass = description.getTestClass();

		do {
			addClassDefinedAnnotations(annotations, objectClass);
			objectClass = objectClass.getSuperclass();
		} while (objectClass != null);

		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				addTestSpecificAnnotations(annotations, description);

				try {
					setPropertiesByAnnotations(annotations);
					base.evaluate(); // This will run the test
				} finally {
					annotations.forEach(annotation -> {
						String originalValue = originalProperties.get(annotation.key());
						if (originalValue != null) {
							System.setProperty(annotation.key(), originalValue);
						}
						else {
							System.clearProperty(annotation.key());
						}

						if (HAS_CONFIG_CLASS.get()) {	// If 'com.frejdh.util.environment.Config' exists (optional)
							setConfigProperty(annotation.key(), originalValue);
						}
					});
				}
			}
		};
	}

	private void addClassDefinedAnnotations(@NotNull List<TestProperty> annotations, @NotNull Class<?> objectClass) {
		if (objectClass.isAnnotationPresent(TestProperty.class)) {	// Class properties (single annotation)
			annotations.addAll(0, Arrays.stream(objectClass.getAnnotationsByType(TestProperty.class))
					.collect(Collectors.toList()));
		}
		else if (objectClass.isAnnotationPresent(TestProperties.class)) {	// Class properties (multiple annotations)
			annotations.addAll(0, Arrays.stream(objectClass.getAnnotationsByType(TestProperties.class))
					.flatMap(v -> Arrays.stream(v.value()))
					.collect(Collectors.toList()));
		}
	}

	private void addTestSpecificAnnotations(@NotNull List<TestProperty> annotations, @NotNull Description description) {
		annotations.addAll(description.getAnnotations().stream()
				.filter(annotation -> annotation.annotationType().isAssignableFrom(TestProperty.class))
				.map(annotation -> (TestProperty) annotation)
				.collect(Collectors.toList()));

		annotations.addAll(description.getAnnotations().stream()
				.filter(annotation -> annotation.annotationType().isAssignableFrom(TestProperties.class))
				.flatMap(testAnnotations -> Arrays.stream(((TestProperties) testAnnotations).value()))
				.collect(Collectors.toList()));
	}

	@SuppressWarnings("JavaReflectionMemberAccess")
	private void setConfigProperty(String key, String value) {
		try {
			Class<?> configClass = Class.forName("com.frejdh.util.environment.Config");
			Method method = configClass.getDeclaredMethod("set", String.class, Object.class);
			method.setAccessible(true);
			method.invoke(null, key, value);
			method.setAccessible(false);
		} catch (ClassNotFoundException | NoSuchMethodException ignored) {
			HAS_CONFIG_CLASS.set(false);
		} catch (Exception e) {
			e.printStackTrace();
			HAS_CONFIG_CLASS.set(false);
		}
	}

}
