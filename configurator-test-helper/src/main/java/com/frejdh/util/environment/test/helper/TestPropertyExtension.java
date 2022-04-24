package com.frejdh.util.environment.test.helper;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TestPropertyExtension implements InvocationInterceptor {

	private final Map<String, String> originalProperties = new HashMap<>();
	private static final List<TestProperty> CLASS_ANNOTATIONS = new ArrayList<>();
	private static final AtomicBoolean HAS_CONFIG_CLASS = new AtomicBoolean(true);

	public TestPropertyExtension() {
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

	@Override
	public <T> T interceptTestClassConstructor(Invocation<T> invocation, ReflectiveInvocationContext<Constructor<T>> invocationContext, ExtensionContext extensionContext) throws Throwable {
		CLASS_ANNOTATIONS.clear();
		Class<?> objectClass = invocationContext.getExecutable().getDeclaringClass();

		do {
			addClassDefinedAnnotations(objectClass);
			objectClass = objectClass.getSuperclass();
		} while (objectClass != null);

		return invocation.proceed();
	}

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
		List<TestProperty> annotations = new ArrayList<>(CLASS_ANNOTATIONS);
		addTestSpecificAnnotations(annotations, invocationContext);

		try {
			setPropertiesByAnnotations(annotations);
			invocation.proceed();
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
					refreshConfig();
				}
			});
		}

	}

	private void addClassDefinedAnnotations(@NotNull Class<?> objectClass) {
		if (objectClass.isAnnotationPresent(TestProperty.class)) {	// Class properties (single annotation)
			CLASS_ANNOTATIONS.addAll(0, Arrays.stream(objectClass.getAnnotationsByType(TestProperty.class))
					.collect(Collectors.toList()));
		}
		else if (objectClass.isAnnotationPresent(TestProperties.class)) {	// Class properties (multiple annotations)
			CLASS_ANNOTATIONS.addAll(0, Arrays.stream(objectClass.getAnnotationsByType(TestProperties.class))
					.flatMap(v -> Arrays.stream(v.value()))
					.collect(Collectors.toList()));
		}
	}

	private void addTestSpecificAnnotations(@NotNull List<TestProperty> annotations, @NotNull ReflectiveInvocationContext<Method> invocationContext) {
		annotations.addAll(Arrays.stream(invocationContext.getExecutable().getAnnotations())
				.filter(annotation -> annotation.annotationType().isAssignableFrom(TestProperty.class))
				.map(annotation -> (TestProperty) annotation)
				.collect(Collectors.toList()));

		annotations.addAll(Arrays.stream(invocationContext.getExecutable().getAnnotations())
				.filter(annotation -> annotation.annotationType().isAssignableFrom(TestProperties.class))
				.flatMap(testAnnotations -> Arrays.stream(((TestProperties) testAnnotations).value()))
				.collect(Collectors.toList()));
	}

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

	private void refreshConfig() {
		try {
			Class<?> configClass = Class.forName("com.frejdh.util.environment.Config");
			Method method = configClass.getDeclaredMethod("refresh", boolean.class);
			method.invoke(null, true);
		} catch (ClassNotFoundException | NoSuchMethodException ignored) {
			HAS_CONFIG_CLASS.set(false);
		} catch (Exception e) {
			e.printStackTrace();
			HAS_CONFIG_CLASS.set(false);
		}
	}

}
