/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common;

import com.avispl.symphony.dal.util.StringUtils;

/**
 * Utility class for this adapter. This class includes helper methods to extract and convert properties.
 * <p>This class is non-instantiable and provides only static utility methods.</p>
 *
 * @author Harry / Symphony Dev Team
 * @since 1.0.0
 */

public class Util {
	/**
	 * check value is null or empty
	 *
	 * @param value input value
	 * @return value after checking
	 */
	public static String getDefaultValueForNullData(String value) {
		return StringUtils.isNotNullOrEmpty(value) && !Constant.NULL.equalsIgnoreCase(value) ? uppercaseFirstCharacter(value) : Constant.NONE_VALUE;
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
	public static String formatUpTime(String time) {
		int seconds = Integer.parseInt(time);
		if (seconds < 0) {
			return Constant.NONE_VALUE;
		}

		int days = seconds / (24 * 3600);
		seconds %= 24 * 3600;
		int hours = seconds / 3600;
		seconds %= 3600;
		int minutes = seconds / 60;
		seconds %= 60;

		StringBuilder result = new StringBuilder();
		if (days > 0) {
			result.append(days).append(" day(s) ");
		}
		if (hours > 0) {
			result.append(hours).append(" hour(s) ");
		}
		if (minutes > 0) {
			result.append(minutes).append(" minute(s) ");
		}
		if (seconds > 0) {
			result.append(seconds).append(" second(s) ");
		}

		if (result.length() == 0) {
			return "0 second(s)";
		}
		return result.toString().trim();
	}
}
