/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.avispl.symphony.dal.util.StringUtils;

/**
 * Utility class for this adapter. This class includes helper methods to extract and convert properties.
 * <p>This class is non-instantiable and provides only static utility methods.</p>
 *
 * @author Harry / Symphony Dev Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Util {
	private static final Logger LOG = Logger.ofClass(Util.class);

	/**
	 * check value is null or empty
	 *
	 * @param value input value
	 * @return value after checking
	 */
	public static String getDefaultValueForNullData(String value) {
		return getDefaultValueForNullData(value, true);
	}

	public static String getDefaultValueForNullData(String value, boolean isTitleCase) {
		if (value == null) {
			LOG.warn("Skip value mapping: value is null");
			return Constant.NOT_AVAILABLE;
		}
		if (StringUtils.isNullOrEmpty(value, true)) {
			LOG.warn("Skip value mapping: string is null/empty");
			return Constant.NOT_AVAILABLE;
		}
		if (isBoolean(value)) {
			return value.toLowerCase();
		}
		if (isInt(value)) {
			return String.valueOf(Integer.parseInt(value));
		}
		return isTitleCase ? uppercaseFirstCharacter(value) : value;
	}

	/**
	 * capitalize the first character of the string
	 *
	 * @param input input string
	 * @return string after fix
	 */
	public static String uppercaseFirstCharacter(String input) {
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}

	/**
	 * Formats uptime from a string representation "hh:mm:ss" into "X hour(s) Y minute(s)" format.
	 *
	 * @param time the uptime string to format
	 * @return formatted uptime string or "None" if input is invalid
	 */
	public static String formatUpTime(long time) {
		StringBuilder normalizedUptime = new StringBuilder();

		long seconds = time % 60;
		long minutes = time % 3600 / 60;
		long hours = time % 86400 / 3600;
		long days = time / 86400;

		if (days > 0) {
			normalizedUptime.append(days).append(" d ");
		}
		if (hours > 0) {
			normalizedUptime.append(hours).append(" hr ");
		}
		if (minutes > 0) {
			normalizedUptime.append(minutes).append(" min ");
		}
		if (seconds > 0 || normalizedUptime.isEmpty()) {
			normalizedUptime.append(seconds).append(" sec");
		}
		return normalizedUptime.toString().trim();
	}

	private static boolean isBoolean(String value) {
		return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
	}

	private static boolean isInt(String value) {
		if (StringUtils.isNullOrEmpty(value, true)) {
			return false;
		}
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
