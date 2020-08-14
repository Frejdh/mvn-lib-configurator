package com.frejdh.util.environment.annotation;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import java.lang.annotation.*;


@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER}) // Wanted support for local variables too! But apparently it's not supported...
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SuppressWarnings("InstanceVariableMayNotBeInitialized")
public @interface PropertyValue {
	/**
	 * Property name
	 */
	String value();

	/**
	 * If no property could be found, return the default value
	 */
	String defaultValue() default "";

	/**
	 * If the system properties should be fetched again before setting this value.
	 * Useful in the event of runtime configurations.
	 */
	boolean refreshProperties() default false;

	/**
	 * If the value should be considered a collection. Split by the defined string. If empty, it will be returned as one element.
	 */
	String splitBy() default "";
}
