/**
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.bases.BaseProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.AlarmProperty;
import com.avispl.symphony.dal.infrastructure.gateway.audiocodes.mediant.model.dto.Alarms;

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
	/**
	 * Generates a map of property names and their corresponding values.
	 * <p>
	 * Each property name can be optionally prefixed with a group name using a predefined format.
	 * The values are derived using the provided mapping function, with {@link Constant#NOT_AVAILABLE} as a fallback for null results.
	 * </p>
	 *
	 * @param <T>        the enum type that extends {@link BaseProperty}
	 * @param properties the array of enum constants to be processed; if null, an empty map is returned
	 * @param groupName  optional group name used to prefix each property's name; can be null
	 * @param mapper     a function that maps each property to its corresponding string value;
	 *                   if null or if the result is null, {@link Constant#NOT_AVAILABLE} is used as the value
	 * @return a map where keys are (optionally grouped) property names and values are mapped strings or {@link Constant#NOT_AVAILABLE}
	 */
	public static <T extends Enum<T> & BaseProperty> Map<String, String> generateProperties(T[] properties, String groupName, Function<T, String> mapper) {
		if (properties == null || mapper == null) {
			return Collections.emptyMap();
		}
		return Arrays.stream(properties).collect(Collectors.toMap(
				property -> Objects.isNull(groupName) ? property.getName() : String.format(Constant.PROPERTY_FORMAT, groupName, property.getName()),
				property -> Optional.ofNullable(mapper.apply(property)).orElse(Constant.NOT_AVAILABLE)
		));
	}

	/**
	 * Generates a flat monitoring properties map from a list of active alarms.
	 * <p>
	 * A summary group named {@code ActiveAlarms} is always emitted with the total alarm count
	 * ({@code ActiveAlarms#Count}) and the distinct severity levels and sources currently present,
	 * each rendered as a CSV string ({@code ActiveAlarms#Severity}, {@code ActiveAlarms#Sources}).
	 * <p>
	 * Each individual alarm is additionally represented as a group named {@code ActiveAlarms_<id>},
	 * with entries for each {@link AlarmProperty}. Keys follow the format {@code ActiveAlarms_<id>#<PropertyName>}.
	 *
	 * @param alarmsList the list of active alarms to map
	 * @return a map of monitoring properties keyed by alarm group and property name
	 */
	public static Map<String, String> generateActiveAlarmsProperties(List<Alarms> alarmsList) {
		Map<String, String> properties = new LinkedHashMap<>();
		int count = CollectionUtils.isEmpty(alarmsList) ? 0 : alarmsList.size();
		properties.put(String.format(Constant.PROPERTY_FORMAT, Constant.ACTIVE_ALARM, Constant.COUNT), String.valueOf(count));
		properties.put(String.format(Constant.PROPERTY_FORMAT, Constant.ACTIVE_ALARM, Constant.SEVERITY), joinDistinct(alarmsList, Alarms::getSeverity));
		properties.put(String.format(Constant.PROPERTY_FORMAT, Constant.ACTIVE_ALARM, Constant.SOURCES), joinDistinct(alarmsList, Alarms::getSource));

		if (!CollectionUtils.isEmpty(alarmsList)) {
			for (Alarms alarm : alarmsList) {
				String groupName = Constant.ACTIVE_ALARM + Constant.UNDERSCORE + alarm.getId();
				properties.putAll(generateProperties(AlarmProperty.values(), groupName, property -> mapToAlarm(alarm, property)));
			}
		}
		return properties;
	}

	/**
	 * Collects the distinct, title-cased values produced by {@code extractor} across all alarms
	 * and joins them into a CSV string (e.g. {@code "Medium, High"}).
	 *
	 * @param alarmsList the list of active alarms to scan
	 * @param extractor  extracts the raw field (e.g. severity or source) from a single alarm
	 * @return the CSV of distinct values, or {@code Constant.NOT_AVAILABLE} if none are present
	 */
	private static String joinDistinct(List<Alarms> alarmsList, Function<Alarms, String> extractor) {
		if (CollectionUtils.isEmpty(alarmsList)) {
			return Constant.NOT_AVAILABLE;
		}
		String csv = alarmsList.stream()
				.map(extractor)
				.filter(StringUtils::isNotNullOrEmpty)
				.map(Util::toTitleCase)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.joining(Constant.COMMA));
		return StringUtils.isNotNullOrEmpty(csv) ? csv : Constant.NOT_AVAILABLE;
	}

	/**
	 * Maps a specific {@link AlarmProperty} enum value from an {@link Alarms} instance
	 * into its string representation.
	 *
	 * @param alarms The source alarms data object containing raw properties.
	 * @param alarmProperty The target monitoring property enum to retrieve.
	 * @return The string value corresponding to the requested property, or {@code null} if the source data is null.
	 */
	public static String mapToAlarm(Alarms alarms, AlarmProperty alarmProperty) {
		if (alarms == null) {
			return null;
		}
		return switch(alarmProperty) {
			case DESCRIPTION-> mapToValue(alarms.getDescription());
			case SEVERITY -> mapToValue(alarms.getSeverity());
			case SOURCE -> mapToValue(alarms.getSource());
			case DATE -> mapToValue(alarms.getDate());
		};
	}

	/**
	 * Maps the given value to a formatted String:
	 * <ul>
	 *   <li>If the value is a non-empty String, returns it in title case.</li>
	 *   <li>If the value is "true" or "false" (case-insensitive), returns it in lowercase.</li>
	 *   <li>If the value is a Boolean or Integer, returns its string representation.</li>
	 *   <li>Returns {@code Constant.NOT_AVAILABLE} if the value is null or empty.</li>
	 * </ul>
	 *
	 * @param value the input value to map
	 * @return the mapped String value, or {@code Constant.NOT_AVAILABLE} if unavailable
	 */
	public static String mapToValue(Object value) {
		if (value == null) {
			return Constant.NOT_AVAILABLE;
		}
		if (value instanceof String str) {
			if ("true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)) {
				return str.toLowerCase();
			}
			return StringUtils.isNotNullOrEmpty(str) ? toTitleCase(str) : Constant.NOT_AVAILABLE;
		}
		if (value instanceof Boolean || value instanceof Integer) {
			return value.toString();
		}

		return Constant.NOT_AVAILABLE;
	}

	/**
	 * Capitalizes the first character of the input string.
	 * <p>
	 * If the input is {@code null}, empty, or the literal string {@code "null"}, this method returns {@code null}.
	 * If the input is {@code "true"} or {@code "false"}, the method returns the input unchanged.
	 * Otherwise, it returns the input string with its first character converted to uppercase.
	 * </p>
	 *
	 * @param value the input string to convert
	 * @return a string with the first character capitalized, or {@code null} if the input is invalid
	 */
	private static String toTitleCase(String value) {
		if (StringUtils.isNullOrEmpty(value) || value.equals("null")) {
			return null;
		}
		if (value.equals("true") || value.equals("false")) {
			return value;
		}

		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}
}
