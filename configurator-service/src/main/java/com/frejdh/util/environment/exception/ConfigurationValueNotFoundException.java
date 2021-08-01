package com.frejdh.util.environment.exception;

import java.util.Locale;

public class ConfigurationValueNotFoundException extends RuntimeException {
	private final String key;

	public ConfigurationValueNotFoundException(String key) {
		super(String.format(Locale.getDefault(), "No configuration value was found for '%s'", key));
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
