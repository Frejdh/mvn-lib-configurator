package com.frejdh.util.environment.exception;

import java.util.Locale;

public class BadIndentationException extends RuntimeException {
	private final String key;

	public BadIndentationException(String key) {
		super(String.format(Locale.getDefault(), "Bad indentation detected for '%s', alternatively the issue lies at the bottom of the file in which the indentation was detected", key));
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
