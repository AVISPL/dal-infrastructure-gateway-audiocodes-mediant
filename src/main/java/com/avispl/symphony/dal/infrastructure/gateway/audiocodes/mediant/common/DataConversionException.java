/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common;

/**
 * Signals that a device response was received but could not be deserialized into the expected
 * shape (malformed or unexpected JSON payload).
 * <p>
 *
 * @author Symphony Dev Team
 * @since 1.0.0
 */
public class DataConversionException extends Exception {
	public DataConversionException(String message, Throwable cause) {
		super(message, cause);
	}
}
